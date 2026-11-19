package View;

import Enums.SquareType;
import View.Mesh3D.Poly;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * The whole game in 2.5D with a "runner" camera: the camera flies right behind the piece that is
 * about to move (or is moving), low above the board, so the path ahead stretches from the
 * bottom centre to the horizon. Around it: a floating sky island, four brick castles, trees and
 * glossy 3D pawns that hop from square to square, leaving a glowing trail.
 *
 * <p>This panel only <b>draws</b>. All game state (where the pieces are, which ones can be
 * clicked) is read from the {@link BoardPanel}, and a click is passed back to it, so the rules
 * in the Controller and the flow in {@link GameScreen} are exactly the same as in the 2D game.
 *
 * <p>Performance: the sky gradient, sun, cloud and rock sprites and the pawn sprites are
 * pre-rendered once in the {@link AssetCache}; the 3D scene itself is drawn every frame with
 * near-plane clipping and screen culling.
 */
class Board3DPanel extends JPanel {

    private static final double BOARD_TOP = 0.45;       // height of the board surface
    private static final double PAWN_HEIGHT = 1.35;
    private static final double PITCH_DEGREES = 16;     // low camera right behind the piece
    private static final double CHASE_BACK = 7.0;       // distance behind the piece
    private static final double CHASE_HEIGHT = 3.4;     // height above the board
    private static final double SUN_AZIMUTH = 0.9;
    private static final int TRACK = 1, CASTLE = 2, CENTER = 3;
    private static final Color HAZE = new Color(214, 232, 250);
    private static final double HOP_SECONDS = 0.17;
    private static final double TRAIL_SECONDS = 0.55;
    private static final BasicStroke THIN = new BasicStroke(0.9f);
    private static final Shape PEBBLE = new Ellipse2D.Double(-0.5, -0.38, 1.0, 0.76);

    /** Test hook: forces a detail level (0 full, 1 reduced, 2 low); -1 = automatic. */
    static int forcedQuality = -1;
    private final BoardPanel board;
    private final MinimapPanel minimap;
    private final Camera3D cam = new Camera3D();
    private final AssetCache cache = new AssetCache();
    private final int n;
    private final int rings;
    private final double half;
    private final double platformHalf;

    // --- static scene (built once) ----------------------------------------------
    private final int[][] kind;
    private final List<Poly> statics = new ArrayList<>();
    private final List<Poly> soil = new ArrayList<>();
    private final List<Tree> trees = new ArrayList<>();
    private final List<Caster> casters = new ArrayList<>();
    private final List<double[]> flags = new ArrayList<>();            // x, z, y, pole height, corner
    private final List<double[]> grassPatches = new ArrayList<>();     // x, z, radius, shade
    private final List<double[]> flowers = new ArrayList<>();          // x, z, colour index
    private final List<SkyObject> skyObjects = new ArrayList<>();
    private final Map<String, Shape> glyphCache = new HashMap<>();
    private final Shape crack;

    // --- animation state -------------------------------------------------------
    private final PieceVisual[] visuals = new PieceVisual[16];
    private final List<Puff> puffs = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Timer timer = new Timer(16, e -> tick());
    private final int[] lastFocus = {-1, -1, -1, -1};
    private long lastNanos = System.nanoTime();
    private double time;
    private int focusKey = -1;
    private double chaseX, chaseZ, chaseHeading;
    private boolean chaseInit;
    // mouse orbit: drag = look around the piece, wheel = zoom, double-click = reset
    private static final double BASE_DISTANCE = Math.hypot(CHASE_BACK, CHASE_HEIGHT);
    private static final double DEFAULT_ELEVATION = Math.atan2(CHASE_HEIGHT, CHASE_BACK);
    private double orbitYaw;
    private double orbitElevation = DEFAULT_ELEVATION;
    private double zoom = 1;
    private Point dragFrom;
    private Point dragLast;
    private boolean dragged;
    private double skyShift;

    // --- on-screen buttons -------------------------------------------------------
    private Shape goBounds = new Rectangle2D.Double();
    private Shape prevBounds = new Rectangle2D.Double();
    private Shape nextBounds = new Rectangle2D.Double();
    private boolean goHover;
    private Runnable rollAction;
    private BooleanSupplier rollEnabled;
    private BufferedImage vignette, sunScaled, cornerMask;
    // adaptive detail: 0 = full, 1 = reduced, 2 = low. Chosen from the measured time of the frames.
    private int quality;
    private double frameMillis = 12;
    private int frameCount;

    // --- per-frame values ------------------------------------------------------
    private final double[] tmpA = new double[3];
    private final double[] tmpB = new double[3];
    private final double[] camPos = new double[3];
    private int panelW, panelH;
    private double lightX, lightY, lightZ;
    private double shadowX, shadowZ;

    Board3DPanel(BoardPanel board) {
        this.board = board;
        this.n = board.getBoardSize();
        this.rings = board.getRings();
        this.half = n / 2.0;
        this.platformHalf = half + 3.4;
        setOpaque(false);
        setLayout(null);
        kind = new int[n][n];
        Path2D c = new Path2D.Double();
        c.moveTo(-0.30, -0.12);
        c.lineTo(-0.06, 0.04);
        c.lineTo(0.08, -0.06);
        c.lineTo(0.30, 0.16);
        crack = new BasicStroke(0.018f).createStrokedShape(c);
        classifyCells();
        buildCastles();
        buildTrees();
        buildIsland();
        double lx = 0.55, ly = 0.80, lz = -0.25, ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lightX = lx / ll;
        lightY = ly / ll;
        lightZ = lz / ll;
        shadowX = -lx / ly;
        shadowZ = -lz / ly;
        Random random = new Random(21);
        for (int i = 0; i < 110; i++) {
            double[] p = randomMarginPoint(random, 0.5, 0.2);
            grassPatches.add(new double[]{p[0], p[1], 0.35 + random.nextDouble() * 1.0, random.nextDouble()});
        }
        for (int i = 0; i < 170; i++) {
            double[] p = randomMarginPoint(random, 0.55, 0.3);
            flowers.add(new double[]{p[0], p[1], random.nextInt(4)});
        }
        skyObjects.add(new SkyObject(0, 0, SUN_AZIMUTH, 0.17, 1.0, 1f, 0));
        skyObjects.add(new SkyObject(2, 0, 2.2, 0.27, 0.5, 0.9f, 0));
        skyObjects.add(new SkyObject(2, 0, 5.1, 0.22, 0.4, 0.9f, 0));
        skyObjects.add(new SkyObject(2, 0, 3.7, 0.31, 0.45, 0.9f, 0));
        for (int i = 0; i < 9; i++) {           // clouds above the horizon
            skyObjects.add(new SkyObject(1, random.nextInt(6), random.nextDouble() * 6.28,
                    0.04 + random.nextDouble() * 0.30, 0.35 + random.nextDouble() * 0.45,
                    0.45f + random.nextFloat() * 0.4f, 0.01 + random.nextDouble() * 0.02));
        }
        for (int i = 0; i < 12; i++) {          // sea of clouds round the island
            skyObjects.add(new SkyObject(1, random.nextInt(6), random.nextDouble() * 6.28,
                    0.38 + random.nextDouble() * 0.60, 0.4 + random.nextDouble() * 0.6,
                    0.45f + random.nextFloat() * 0.4f, 0.006 + random.nextDouble() * 0.012));
        }
        for (int i = 0; i < visuals.length; i++) {
            visuals[i] = new PieceVisual();
        }
        minimap = new MinimapPanel(board);
        add(minimap);

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragFrom = e.getPoint();
                dragLast = e.getPoint();
                dragged = false;
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragFrom == null) {
                    return;
                }
                if (!dragged && e.getPoint().distance(dragFrom) > 5) {
                    dragged = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                if (dragged) {
                    orbitYaw -= (e.getX() - dragLast.x) * 0.008;
                    orbitElevation = Math.max(Math.toRadians(8), Math.min(Math.toRadians(68),
                            orbitElevation + (e.getY() - dragLast.y) * 0.006));
                }
                dragLast = e.getPoint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (!dragged) {
                    if (e.getClickCount() >= 2 && hitAt(e.getPoint()) == null && !goBounds.contains(e.getPoint())) {
                        resetView();
                    } else {
                        handleClick(e.getPoint());
                    }
                }
                dragFrom = null;
                dragged = false;
                setCursor(Cursor.getDefaultCursor());
            }
            @Override public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                zoom = Math.max(0.55, Math.min(1.9, zoom * Math.pow(1.1, e.getWheelRotation())));
            }
            @Override public void mouseMoved(MouseEvent e) {
                boolean overGo = goBounds.contains(e.getPoint());
                goHover = overGo;
                boolean hand = overGo || prevBounds.contains(e.getPoint()) || nextBounds.contains(e.getPoint())
                        || hitAt(e.getPoint()) != null;
                setCursor(hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        bindKey("LEFT", () -> cycleFocus(-1));
        bindKey("RIGHT", () -> cycleFocus(1));
        bindKey("ENTER", this::go);
    }

    private void bindKey(String key, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /**
     * What the green "Go!" button does when no piece can be moved: roll the dice.
     *
     * @param action  rolls the dice
     * @param enabled tells whether rolling is possible right now
     */
    void setRollAction(Runnable action, BooleanSupplier enabled) {
        this.rollAction = action;
        this.rollEnabled = enabled;
    }

    // ------------------------------------------------------------------
    // Life cycle
    // ------------------------------------------------------------------

    void start() {
        lastNanos = System.nanoTime();
        timer.start();
    }

    void stop() {
        timer.stop();
    }

    @Override public void addNotify() {
        super.addNotify();
        start();
    }

    @Override public void removeNotify() {
        stop();
        super.removeNotify();
    }

    @Override public void doLayout() {
        minimap.layoutIn(getWidth(), getHeight());
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - lastNanos) / 1e9);
        lastNanos = now;
        time += dt;
        for (SkyObject o : skyObjects) {
            o.az += o.speed * dt * 0.1;
        }
        updatePieces();
        updateFocus();
        updateChase(dt);
        for (int i = puffs.size() - 1; i >= 0; i--) {
            if (time - puffs.get(i).born > 0.5) {
                puffs.remove(i);
            }
        }
        repaint();
    }

    // ------------------------------------------------------------------
    // Focus piece and chase camera
    // ------------------------------------------------------------------

    private List<Integer> selectableKeys(int turn) {
        List<Integer> keys = new ArrayList<>();
        for (int idx = 0; idx < 4; idx++) {
            PieceVisual v = visuals[turn * 4 + idx];
            if (v.init && v.visible && isSelectable(turn, idx, turn, v)) {
                keys.add(turn * 4 + idx);
            }
        }
        return keys;
    }

    private void updateFocus() {
        int turn = board.turnCorner();
        int moving = -1;
        for (int idx = 0; idx < 4; idx++) {
            PieceVisual v = visuals[turn * 4 + idx];
            if (v.init && v.visible && v.hopping) {
                moving = turn * 4 + idx;
            }
        }
        List<Integer> selectable = selectableKeys(turn);
        if (moving >= 0) {
            focusKey = moving;
        } else if (!selectable.isEmpty()) {
            if (!selectable.contains(focusKey)) {
                focusKey = selectable.contains(lastFocus[turn]) ? lastFocus[turn] : selectable.get(0);
            }
        } else if (focusKey < 0 || focusKey / 4 != turn || !visuals[focusKey].visible) {
            int fallback = lastFocus[turn];
            if (fallback < 0 || !visuals[fallback].visible) {
                fallback = -1;
                for (int idx = 0; idx < 4 && fallback < 0; idx++) {
                    if (visuals[turn * 4 + idx].init && visuals[turn * 4 + idx].visible
                            && visuals[turn * 4 + idx].onBoard) {
                        fallback = turn * 4 + idx;
                    }
                }
                for (int idx = 0; idx < 4 && fallback < 0; idx++) {
                    if (visuals[turn * 4 + idx].init && visuals[turn * 4 + idx].visible) {
                        fallback = turn * 4 + idx;
                    }
                }
            }
            focusKey = fallback;
        }
        if (focusKey >= 0) {
            lastFocus[focusKey / 4] = focusKey;
        }
    }

    /** Back to the default camera behind the piece. */
    private void resetView() {
        orbitYaw = 0;
        orbitElevation = DEFAULT_ELEVATION;
        zoom = 1;
    }

    private void cycleFocus(int direction) {
        List<Integer> keys = selectableKeys(board.turnCorner());
        if (keys.size() < 2) {
            return;
        }
        Collections.sort(keys);
        int at = Math.max(0, keys.indexOf(focusKey));
        focusKey = keys.get(Math.floorMod(at + direction, keys.size()));
    }

    /** Moves the camera behind the focus piece, turning to look along its path. */
    private void updateChase(double dt) {
        if (focusKey < 0 || !visuals[focusKey].init) {
            return;
        }
        PieceVisual v = visuals[focusKey];
        int corner = focusKey / 4, idx = focusKey % 4;
        double dx = 0, dz = 0;
        if (v.onBoard) {
            Point a = board.pieceCell(corner, idx);
            Point b = board.routeCell(corner, idx, 3);
            if (a != null && b != null) {
                dx = b.x - a.x;
                dz = -(b.y - a.y);
                if (dx == 0 && dz == 0) {
                    b = board.routeCell(corner, idx, -3);
                    dx = a.x - b.x;
                    dz = -(a.y - b.y);
                }
            }
        } else {
            // a piece waiting in its castle: look towards the castle from the board side, so the
            // camera is not inside the keep and the castle towers up in the background
            double[] corner0 = castlePoint(corner, 0, 0);
            dx = corner0[0] - v.curX;
            dz = corner0[1] - v.curZ;
        }
        double targetHeading = chaseHeading;
        if (dx != 0 || dz != 0) {
            targetHeading = Math.atan2(dx, dz);
        }
        if (!chaseInit) {
            chaseInit = true;
            chaseX = v.curX;
            chaseZ = v.curZ;
            chaseHeading = targetHeading;
            return;
        }
        double diff = Math.atan2(Math.sin(targetHeading - chaseHeading), Math.cos(targetHeading - chaseHeading));
        chaseHeading += diff * (1 - Math.exp(-4.5 * dt));
        double k = 1 - Math.exp(-7.0 * dt);
        chaseX += (v.curX - chaseX) * k;
        chaseZ += (v.curZ - chaseZ) * k;
    }

    // ------------------------------------------------------------------
    // Scene set-up (done once)
    // ------------------------------------------------------------------

    private void classifyCells() {
        for (int ring = 0; ring < rings; ring++) {
            for (Point p : board.ringPath(ring)) {
                kind[p.x][p.y] = TRACK;
            }
        }
        for (int c = 0; c < 15; c++) {
            for (int r = 0; r < 15; r++) {
                boolean vertical = c >= 6 && c <= 8 && (r < 6 || r > 8);
                boolean horizontal = r >= 6 && r <= 8 && (c < 6 || c > 8);
                if (vertical || horizontal) {
                    Point p = board.inner(c, r);
                    kind[p.x][p.y] = TRACK;
                }
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            Point o = board.castleOrigin(corner);
            for (int dx = 0; dx < 6; dx++) {
                for (int dy = 0; dy < 6; dy++) {
                    kind[o.x + dx][o.y + dy] = CASTLE;
                }
            }
        }
        for (int c = 6; c <= 8; c++) {
            for (int r = 6; r <= 8; r++) {
                Point p = board.inner(c, r);
                kind[p.x][p.y] = CENTER;
            }
        }
    }

    /** Local castle coordinates (u, v from the outer corner of the 6x6 block) to world x / z. */
    private double[] castlePoint(int corner, double u, double v) {
        Point o = board.castleOrigin(corner);
        boolean right = corner == 1 || corner == 2;
        boolean bottom = corner >= 2;
        double gx = (right ? o.x + 6 : o.x) + (right ? -u : u);
        double gy = (bottom ? o.y + 6 : o.y) + (bottom ? -v : v);
        return new double[]{gx - half, half - gy};
    }

    /** Standing place of a piece waiting in its castle. */
    private double[] padPosition(int corner, int slot) {
        return castlePoint(corner, 3.25 + 1.45 * (slot % 2), 3.25 + 1.45 * (slot / 2));
    }

    private double[] randomMarginPoint(Random random, double fromFrame, double fromEdge) {
        while (true) {
            double x = (random.nextDouble() * 2 - 1) * (platformHalf - fromEdge);
            double z = (random.nextDouble() * 2 - 1) * (platformHalf - fromEdge);
            if (Math.abs(x) > half + 0.45 + fromFrame || Math.abs(z) > half + 0.45 + fromFrame) {
                if (Math.abs(x) + Math.abs(z) < platformHalf * 2 - 3.6) {
                    return new double[]{x, z};
                }
            }
        }
    }

    private void buildCastles() {
        Mesh3D mesh = new Mesh3D();
        for (int corner = 0; corner < 4; corner++) {
            Color color = BoardPanel.cornerColor(corner);
            Color wall = Mesh3D.mix(color, new Color(206, 186, 160), 0.62);
            Color wallDark = Mesh3D.mix(wall, Color.BLACK, 0.20);
            Color roof = Mesh3D.mix(color, Color.BLACK, 0.06);
            double y = BOARD_TOP;
            // keep with turret
            localBox(mesh, corner, 0.3, 2.6, 0.3, 2.6, y, y + 3.3, wall, true, 2);
            mesh.addDoorsToLastBox(0.40, 0.60, 0.40);
            mesh.addBannersToLastBox(color, 0.14, 0.86);
            rectCaster(corner, 0.3, 2.6, 0.3, 2.6, 0, 3.3);
            localBattlements(mesh, corner, 0.3, 2.6, 0.3, 2.6, y + 3.3, 0.36, wallDark);
            double[] c = castlePoint(corner, 1.45, 1.45);
            mesh.addPrism(c[0], c[1], 0.74, y + 3.3, y + 4.8, 10, wall, true, 3);
            mesh.addPrism(c[0], c[1], 0.88, y + 4.8, y + 5.05, 10, wallDark, false, 0);
            mesh.addPyramid(c[0], c[1], 1.0, y + 5.05, 2.3, 10, roof);
            circleCaster(c[0], c[1], 0.74, 3.3, 4.8, 1.0, BOARD_TOP);
            circleCaster(c[0], c[1], 1.0, 5.05, 7.35, 0.03, BOARD_TOP);
            flags.add(new double[]{c[0], c[1], y + 7.35, 1.15, corner});
            // outer walls with arched wooden gates and banners
            localBox(mesh, corner, 2.6, 5.5, 0.15, 0.62, y, y + 1.3, wall, true, 0);
            mesh.addDoorsToLastBox(0.42, 0.58, 0.62);
            mesh.addBannersToLastBox(color, 0.14, 0.86);
            localBattlements(mesh, corner, 2.6, 5.5, 0.15, 0.62, y + 1.3, 0.3, wallDark);
            rectCaster(corner, 2.6, 5.5, 0.15, 0.62, 0, 1.6);
            localBox(mesh, corner, 0.15, 0.62, 2.6, 5.5, y, y + 1.3, wall, true, 0);
            mesh.addDoorsToLastBox(0.42, 0.58, 0.62);
            mesh.addBannersToLastBox(color, 0.14, 0.86);
            localBattlements(mesh, corner, 0.15, 0.62, 2.6, 5.5, y + 1.3, 0.3, wallDark);
            rectCaster(corner, 0.15, 0.62, 2.6, 5.5, 0, 1.6);
            // round towers at the two far ends of the walls
            double[][] towers = {{5.3, 0.55}, {0.55, 5.3}};
            for (double[] t : towers) {
                double[] p = castlePoint(corner, t[0], t[1]);
                mesh.addPrism(p[0], p[1], 0.6, y, y + 2.9, 10, wall, true, 3);
                mesh.addPrism(p[0], p[1], 0.72, y + 2.9, y + 3.14, 10, wallDark, false, 0);
                mesh.addPyramid(p[0], p[1], 0.84, y + 3.14, 1.75, 10, roof);
                circleCaster(p[0], p[1], 0.6, 0, 2.9, 1.0, BOARD_TOP);
                circleCaster(p[0], p[1], 0.84, 3.14, 4.89, 0.03, BOARD_TOP);
                flags.add(new double[]{p[0], p[1], y + 4.89, 0.9, corner});
            }
        }
        statics.addAll(mesh.polys());
    }

    private void localBox(Mesh3D mesh, int corner, double u0, double u1, double v0, double v1,
                          double y0, double y1, Color color, boolean brick, int windows) {
        double[] a = castlePoint(corner, u0, v0);
        double[] b = castlePoint(corner, u1, v1);
        mesh.addBox(Math.min(a[0], b[0]), Math.max(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[1], b[1]),
                y0, y1, color, brick, windows);
    }

    private void localBattlements(Mesh3D mesh, int corner, double u0, double u1, double v0, double v1,
                                  double y, double height, Color color) {
        double[] a = castlePoint(corner, u0, v0);
        double[] b = castlePoint(corner, u1, v1);
        mesh.addBattlements(Math.min(a[0], b[0]), Math.max(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[1], b[1]),
                y, height, color);
    }

    private void buildTrees() {
        Random random = new Random(5);
        for (int i = 0; i < 18; i++) {
            double[] p = randomMarginPoint(random, 1.6, 0.9);
            double s = 0.6 + random.nextDouble() * 0.4;
            Color leaf = new Color(46 + random.nextInt(30), 134 + random.nextInt(45), 62 + random.nextInt(25));
            trees.add(new Tree(p[0], p[1], s, leaf, false));
            circleCaster(p[0], p[1], 0.35 * s, 0, 2.2 * s, 2.6, 0);
        }
        for (int i = 0; i < 16; i++) {
            double[] p = randomMarginPoint(random, 0.5, 0.7);
            double s = 0.5 + random.nextDouble() * 0.3;
            Color leaf = new Color(70 + random.nextInt(30), 150 + random.nextInt(40), 70 + random.nextInt(25));
            trees.add(new Tree(p[0], p[1], s, leaf, true));
        }
    }

    // --- shadow casters ---------------------------------------------------------

    private static final class Caster {
        final double[] x, z;
        final double baseH, topScale, topH, groundY;
        Caster(double[] x, double[] z, double baseH, double topScale, double topH, double groundY) {
            this.x = x;
            this.z = z;
            this.baseH = baseH;
            this.topScale = topScale;
            this.topH = topH;
            this.groundY = groundY;
        }
    }

    private void rectCaster(int corner, double u0, double u1, double v0, double v1, double h0, double h1) {
        double[] a = castlePoint(corner, u0, v0), b = castlePoint(corner, u1, v1);
        double x0 = Math.min(a[0], b[0]), x1 = Math.max(a[0], b[0]);
        double z0 = Math.min(a[1], b[1]), z1 = Math.max(a[1], b[1]);
        casters.add(new Caster(new double[]{x0, x1, x1, x0}, new double[]{z0, z0, z1, z1}, h0, 1.0, h1, BOARD_TOP));
    }

    private void circleCaster(double cx, double cz, double r, double h0, double h1, double topScale, double groundY) {
        int seg = 10;
        double[] xs = new double[seg], zs = new double[seg];
        for (int i = 0; i < seg; i++) {
            double a = 2 * Math.PI * i / seg;
            xs[i] = cx + r * Math.cos(a);
            zs[i] = cz + r * Math.sin(a);
        }
        casters.add(new Caster(xs, zs, h0, topScale, h1, groundY));
    }

    // --- the floating island --------------------------------------------------------

    private void buildIsland() {
        double hs = platformHalf, c = 2.6;
        double[][] oct = {{-hs + c, hs}, {hs - c, hs}, {hs, hs - c}, {hs, -hs + c},
                {hs - c, -hs}, {-hs + c, -hs}, {-hs, -hs + c}, {-hs, hs - c}};
        List<double[]> rim = new ArrayList<>();
        int sub = 5;
        for (int i = 0; i < oct.length; i++) {
            double[] a = oct[i], b = oct[(i + 1) % oct.length];
            for (int j = 0; j < sub; j++) {
                rim.add(new double[]{a[0] + (b[0] - a[0]) * j / sub, a[1] + (b[1] - a[1]) * j / sub});
            }
        }
        double[] ys = {0, -1.3, -3.8, -9.5};
        double[] shrink = {1.0, 0.975, 0.80, 0.22};
        Color[] colors = {new Color(158, 142, 120), new Color(124, 90, 60), new Color(96, 76, 60)};
        Random random = new Random(9);
        int m = rim.size();
        double[][] jitter = new double[4][m];
        for (int level = 0; level < 4; level++) {
            for (int i = 0; i < m; i++) {
                jitter[level][i] = level < 2 ? 1.0 : level == 2 ? 0.92 + 0.16 * random.nextDouble()
                        : 0.7 + 0.6 * random.nextDouble();
            }
        }
        for (int level = 0; level < 3; level++) {
            for (int i = 0; i < m; i++) {
                int j = (i + 1) % m;
                double s0 = shrink[level], s1 = shrink[level + 1];
                double[] xs = {rim.get(i)[0] * s0 * jitter[level][i], rim.get(j)[0] * s0 * jitter[level][j],
                        rim.get(j)[0] * s1 * jitter[level + 1][j], rim.get(i)[0] * s1 * jitter[level + 1][i]};
                double[] zs = {rim.get(i)[1] * s0 * jitter[level][i], rim.get(j)[1] * s0 * jitter[level][j],
                        rim.get(j)[1] * s1 * jitter[level + 1][j], rim.get(i)[1] * s1 * jitter[level + 1][i]};
                double[] yv = {ys[level], ys[level], ys[level + 1], ys[level + 1]};
                double mx = (rim.get(i)[0] + rim.get(j)[0]) / 2, mz = (rim.get(i)[1] + rim.get(j)[1]) / 2;
                double len = Math.hypot(mx, mz);
                double ny = level == 0 ? 0.1 : level == 1 ? -0.1 : -0.45;
                double hl = Math.sqrt(1 - ny * ny);
                double vary = 0.88 + 0.24 * random.nextDouble();
                Color col = new Color(clamp(colors[level].getRed() * vary), clamp(colors[level].getGreen() * vary),
                        clamp(colors[level].getBlue() * vary));
                soil.add(new Poly(xs, yv, zs, mx / len * hl, ny, mz / len * hl, col, false));
            }
        }
    }

    // ------------------------------------------------------------------
    // Piece animation
    // ------------------------------------------------------------------

    private static final class PieceVisual {
        boolean init, hopping, visible = true;
        double curX, curZ, curH, srcX, srcZ, tgtX, tgtZ, dur, hopH, scale = 1;
        double t0;
        boolean onBoard, finished;
        final List<double[]> trail = new ArrayList<>();      // x, y, z, time
    }

    private static final class Puff {
        final double x, z, born;
        Puff(double x, double z, double born) {
            this.x = x;
            this.z = z;
            this.born = born;
        }
    }

    private void updatePieces() {
        // pieces sharing a square are placed side by side
        Map<Integer, List<Integer>> sharers = new HashMap<>();
        Point[] cells = new Point[16];
        for (int corner = 0; corner < 4; corner++) {
            if (!board.isActiveCorner(corner)) {
                continue;
            }
            for (int idx = 0; idx < 4; idx++) {
                Point cell = board.pieceCell(corner, idx);
                cells[corner * 4 + idx] = cell;
                if (cell != null) {
                    sharers.computeIfAbsent(cell.x * 1000 + cell.y, k -> new ArrayList<>()).add(corner * 4 + idx);
                }
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            if (!board.isActiveCorner(corner)) {
                continue;
            }
            for (int idx = 0; idx < 4; idx++) {
                int key = corner * 4 + idx;
                PieceVisual v = visuals[key];
                double tx, tz;
                boolean finished = board.pieceFinished(corner, idx);
                Point cell = cells[key];
                if (finished) {
                    tx = 0;
                    tz = 0;
                } else if (cell != null) {
                    List<Integer> group = sharers.get(cell.x * 1000 + cell.y);
                    double[] off = shareOffset(group.indexOf(key), group.size());
                    tx = cell.x + 0.5 - half + off[0];
                    tz = half - (cell.y + 0.5) + off[1];
                } else {
                    double[] pad = padPosition(corner, idx);
                    tx = pad[0];
                    tz = pad[1];
                }
                v.onBoard = cell != null;
                v.finished = finished;
                if (!v.init) {
                    v.init = true;
                    v.curX = tx;
                    v.curZ = tz;
                    v.tgtX = tx;
                    v.tgtZ = tz;
                    v.visible = !finished;
                    continue;
                }
                if (Math.abs(tx - v.tgtX) > 1e-6 || Math.abs(tz - v.tgtZ) > 1e-6) {
                    v.srcX = v.curX;
                    v.srcZ = v.curZ;
                    v.tgtX = tx;
                    v.tgtZ = tz;
                    double dist = Math.hypot(tx - v.srcX, tz - v.srcZ);
                    boolean step = dist <= 1.7;
                    v.dur = step ? HOP_SECONDS : Math.min(1.0, 0.4 + 0.045 * dist);
                    v.hopH = step ? 0.85 : Math.min(2.6, 1.0 + 0.1 * dist);
                    v.t0 = time;
                    v.hopping = true;
                }
                if (v.hopping) {
                    double u = (time - v.t0) / v.dur;
                    if (u >= 1) {
                        u = 1;
                        v.hopping = false;
                        puffs.add(new Puff(v.tgtX, v.tgtZ, time));
                        if (finished) {
                            v.visible = false;
                        }
                    }
                    v.curX = v.srcX + (v.tgtX - v.srcX) * u;
                    v.curZ = v.srcZ + (v.tgtZ - v.srcZ) * u;
                    v.curH = Math.sin(Math.PI * u) * v.hopH;
                    v.scale = finished ? 1 - 0.7 * u : 1;
                    v.trail.add(new double[]{v.curX, BOARD_TOP + v.curH + 0.5 * PAWN_HEIGHT * v.scale, v.curZ, time});
                } else {
                    v.curH = 0;
                }
                while (!v.trail.isEmpty() && time - v.trail.get(0)[3] > TRAIL_SECONDS) {
                    v.trail.remove(0);
                }
            }
        }
    }

    private static double[] shareOffset(int index, int count) {
        if (count <= 1) {
            return new double[]{0, 0};
        }
        double[][] spots = {{-0.24, 0.22}, {0.24, 0.22}, {-0.24, -0.22}, {0.24, -0.22}};
        if (count == 2) {
            spots = new double[][]{{-0.24, 0}, {0.24, 0}};
        }
        return spots[Math.max(0, index) % spots.length];
    }

    // ------------------------------------------------------------------
    // Clicking
    // ------------------------------------------------------------------

    private static final class Hit {
        final int corner, index;
        final boolean castle;
        final Rectangle2D bounds;
        final double depth;
        Hit(int corner, int index, boolean castle, Rectangle2D bounds, double depth) {
            this.corner = corner;
            this.index = index;
            this.castle = castle;
            this.bounds = bounds;
            this.depth = depth;
        }
    }

    private Hit hitAt(Point p) {
        Hit best = null;
        for (Hit h : hits) {
            if (h.bounds.contains(p) && (best == null || h.depth < best.depth)) {
                best = h;
            }
        }
        return best;
    }

    private void handleClick(Point p) {
        if (goBounds.contains(p)) {
            go();
            return;
        }
        if (prevBounds.contains(p)) {
            cycleFocus(-1);
            return;
        }
        if (nextBounds.contains(p)) {
            cycleFocus(1);
            return;
        }
        Hit hit = hitAt(p);
        if (hit != null) {
            focusKey = hit.corner * 4 + hit.index;
            fire(hit.corner, hit.index);
        }
    }

    /** The green "Go!" button: move the focused piece, or roll the dice when nothing can move yet. */
    private void go() {
        int turn = board.turnCorner();
        if (selectableKeys(turn).contains(focusKey)) {
            fire(focusKey / 4, focusKey % 4);
        } else if (rollAction != null && rollEnabled != null && rollEnabled.getAsBoolean()) {
            rollAction.run();
        }
    }

    private void fire(int corner, int idx) {
        if (board.pieceInCastle(corner, idx) && board.castleHighlighted()) {
            board.fireCastleClick(idx);
        } else if (board.boardHighlighted()) {
            board.fireBoardClick(idx);
        }
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        GameScreen.quality(g);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth(), h = getHeight();
        if (w < 10 || h < 10) {
            g.dispose();
            return;
        }
        long frameStart = System.nanoTime();
        panelW = w;
        panelH = h;
        g.setClip(0, 0, w, h);      // a plain rectangle keeps every draw call on the fast path
        setupCamera(w, h);

        g.setColor(new Color(18, 76, 192));
        g.fillRect(0, 0, w, h);
        if (skyShift < 0) {
            g.setColor(new Color(96, 156, 232));
            g.fillRect(0, (int) (h + skyShift) - 1, w, (int) (-skyShift) + 2);
        }
        g.drawImage(cache.sky(w, h), 0, (int) Math.round(skyShift), null);
        drawSkyObjects(g, w, h);
        if (quality >= 2) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        drawIsland(g);
        drawBoardSlab(g);
        drawBoardTiles(g);
        drawShadows(g);
        hits.clear();
        drawGroundEffects(g);
        drawObjects(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawFlags(g);
        if (quality < 2) {
            drawVignette(g, w, h);
        }
        drawHud(g, w, h);
        double zoom = minimap.zoomProgress();
        if (zoom > 0.01) {
            // the scene is dimmed while the map is held open
            g.setColor(new Color(6, 12, 34, (int) (130 * zoom)));
            g.fillRect(0, 0, w, h);
        }
        drawCornerMasks(g, w, h);
        g.dispose();
        adaptQuality((System.nanoTime() - frameStart) / 1e6);
    }

    /** Raises or lowers the level of detail so the animation stays smooth on slower computers. */
    private void adaptQuality(double millis) {
        if (forcedQuality >= 0) {
            quality = forcedQuality;
            return;
        }
        frameMillis += (millis - frameMillis) * 0.08;
        if (++frameCount % 40 != 0) {
            return;
        }
        if (frameMillis > 20 && quality < 2) {
            quality++;
            frameMillis = 12;
        } else if (frameMillis < 7 && quality > 0) {
            quality--;
            frameMillis = 12;
        }
    }

    /** Rounded corners without a (slow) rounded clip: four small white corner pieces. */
    private void drawCornerMasks(Graphics2D g, int w, int h) {
        final int r = 22;
        if (cornerMask == null) {
            cornerMask = new BufferedImage(r, r, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = cornerMask.createGraphics();
            cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D area = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            area.append(new Rectangle2D.Double(0, 0, r, r), false);
            area.append(new Ellipse2D.Double(0, 0, 2 * r, 2 * r), false);
            cg.setColor(Color.WHITE);
            cg.fill(area);
            cg.dispose();
        }
        g.drawImage(cornerMask, 0, 0, null);
        g.drawImage(cornerMask, w, 0, -r, r, null);
        g.drawImage(cornerMask, 0, h, r, -r, null);
        g.drawImage(cornerMask, w, h, -r, -r, null);
    }

    private void setupCamera(int w, int h) {
        double focal = Math.min(0.80 * w, 1.05 * h);
        double distance = BASE_DISTANCE * zoom;
        double heading = chaseHeading + orbitYaw;
        cam.focal = focal;
        // keep aiming so the piece stays at the same place on screen, whatever the elevation
        cam.pitch = Math.max(Math.toRadians(2), orbitElevation - 0.38 * DEFAULT_ELEVATION);
        cam.centerX = w / 2.0;
        // the optical axis stays where it is for the default view; the horizon moves with the pitch
        cam.centerY = h * AssetCache.HORIZON + focal * Math.tan(0.62 * DEFAULT_ELEVATION);
        cam.heading = heading;
        cam.posX = chaseX - Math.sin(heading) * distance * Math.cos(orbitElevation);
        cam.posZ = chaseZ - Math.cos(heading) * distance * Math.cos(orbitElevation);
        cam.posY = BOARD_TOP + distance * Math.sin(orbitElevation);
        cam.update();
        camPos[0] = cam.posX;
        camPos[1] = cam.posY;
        camPos[2] = cam.posZ;
        skyShift = (cam.centerY - focal * Math.tan(cam.pitch)) - h * AssetCache.HORIZON;
    }

    // --- sky ---------------------------------------------------------------------------

    private void drawSkyObjects(Graphics2D g, int w, int h) {
        double ref = Math.max(0.6, w / 1000.0);
        int index = 0;
        for (SkyObject o : skyObjects) {
            index++;
            if (quality > 0 && o.type == 1 && (index & 1) == 0) {
                continue;
            }
            double da = Math.atan2(Math.sin(o.az - cam.heading), Math.cos(o.az - cam.heading));
            if (Math.abs(da) > 1.25) {
                continue;
            }
            double x = cam.centerX + cam.focal * Math.tan(da);
            double y = h * o.yFrac + skyShift;
            BufferedImage sprite;
            double size;
            if (o.type == 0) {
                size = w * 0.75;
                if (sunScaled == null || sunScaled.getWidth() != (int) size) {
                    sunScaled = new BufferedImage((int) size, (int) size, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D sg = sunScaled.createGraphics();
                    sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    sg.drawImage(cache.sun(), 0, 0, (int) size, (int) size, null);
                    sg.dispose();
                }
                sprite = sunScaled;
            } else if (o.type == 2) {
                sprite = cache.islet();
                size = 150 * ref * o.scale * 2.2;
            } else {
                sprite = cache.cloud(o.sprite);
                size = sprite.getWidth() * o.scale * ref * 0.8;
            }
            double ratio = sprite.getHeight() / (double) sprite.getWidth();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, o.alpha));
            g.drawImage(sprite, (int) (x - size / 2), (int) (y - size * ratio / 2), (int) size, (int) (size * ratio), null);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    // --- island, board slab ------------------------------------------------------------

    private void drawIsland(Graphics2D g) {
        List<Poly> faces = new ArrayList<>();
        for (Poly p : soil) {
            if (facing(p)) {
                faces.add(p);
            }
        }
        faces.sort(Comparator.comparingDouble((Poly p) -> -cam.depthOf(p.cx, p.cy, p.cz)));
        for (Poly p : faces) {
            drawPoly(g, p, quality == 0);
        }
        double hs = platformHalf, c = 2.6;
        double[] xs = {-hs + c, hs - c, hs, hs, hs - c, -hs + c, -hs, -hs};
        double[] zs = {hs, hs, hs - c, -hs + c, -hs, -hs, -hs + c, hs - c};
        Path2D top = flatPath(xs, zs, 0);
        if (top != null) {
            Rectangle2D b = top.getBounds2D();
            g.setPaint(new GradientPaint(0, (float) Math.max(b.getMaxY(), b.getMinY() + 2), new Color(150, 212, 104),
                    0, (float) b.getMinY(), new Color(90, 170, 74)));
            g.fill(top);
            g.setColor(new Color(70, 52, 36, 200));
            g.setStroke(new BasicStroke(3f));
            g.draw(top);
        }
        for (double[] patch : grassPatches) {
            if (quality > 0 || cam.depthOf(patch[0], 0, patch[1]) > 40) {
                continue;
            }
            Path2D disc = discPath(patch[0], patch[1], 0.01, patch[2], 14);
            if (disc != null) {
                g.setColor(patch[3] < 0.5 ? new Color(150, 214, 100, 110) : new Color(50, 130, 56, 80));
                g.fill(disc);
            }
        }
        Color[] petals = {new Color(255, 255, 255), new Color(255, 224, 80), new Color(255, 150, 190), new Color(190, 170, 255)};
        for (double[] f : flowers) {
            if (quality > 0 || !cam.project(f[0], 0.02, f[1], tmpA) || tmpA[2] > 36 || tmpA[0] < -10 || tmpA[0] > panelW + 10) {
                continue;
            }
            double r = Math.max(1.0, 0.075 * cam.focal / tmpA[2]);
            g.setColor(petals[(int) f[2]]);
            g.fill(new Ellipse2D.Double(tmpA[0] - r, tmpA[1] - r * 0.7, r * 2, r * 1.4));
        }
    }

    private void drawBoardSlab(Graphics2D g) {
        double o = half + 0.45;
        double[][] corners = {{-o, -o}, {o, -o}, {o, o}, {-o, o}};
        Color stone = new Color(150, 122, 98);
        for (int i = 0; i < 4; i++) {
            double[] a = corners[i], b = corners[(i + 1) % 4];
            double mx = (a[0] + b[0]) / 2, mz = (a[1] + b[1]) / 2;
            double len = Math.hypot(mx, mz);
            Poly side = new Poly(new double[]{a[0], b[0], b[0], a[0]}, new double[]{0, 0, BOARD_TOP, BOARD_TOP},
                    new double[]{a[1], b[1], b[1], a[1]}, mx / len, 0, mz / len, stone, false);
            if (facing(side)) {
                drawPoly(g, side, quality == 0);
            }
        }
        Path2D frame = flatPath(new double[]{-o, o, o, -o}, new double[]{-o, -o, o, o}, BOARD_TOP);
        if (frame != null) {
            g.setColor(new Color(120, 88, 64));
            g.fill(frame);
            g.setColor(new Color(70, 48, 34));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(frame);
        }
        double t = half + 0.12;
        Path2D trim = flatPath(new double[]{-t, t, t, -t}, new double[]{-t, -t, t, t}, BOARD_TOP + 0.01);
        if (trim != null) {
            g.setColor(new Color(222, 184, 100));
            g.setStroke(new BasicStroke(2f));
            g.draw(trim);
        }
        Path2D base = flatPath(new double[]{-half, half, half, -half}, new double[]{-half, -half, half, half}, BOARD_TOP);
        if (base != null) {
            g.setColor(new Color(160, 148, 134));
            g.fill(base);
        }
    }

    // --- board tiles ---------------------------------------------------------------------

    private void drawBoardTiles(Graphics2D g) {
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (kind[x][y] == TRACK) {
                    drawTrackCell(g, x, y);
                }
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            drawCastleFloor(g, corner);
        }
        drawCenter(g);
    }

    private void drawTrackCell(Graphics2D g, int x, int y) {
        double wx = x + 0.5 - half, wz = half - (y + 0.5);
        double depth = cam.depthOf(wx, BOARD_TOP, wz);
        if (depth < -1.0 || depth > 46) {
            return;
        }
        depth += 7 * quality;       // less detail on slower computers
        Point p = new Point(x, y);
        Color home = board.homeCellColor(p);
        SquareType type = board.specialSquareType(p);
        Color special = board.specialSquareColor(p);
        Color base = ((x + y) & 1) == 0 ? new Color(238, 234, 226) : new Color(224, 219, 209);
        if (home != null) {
            base = Mesh3D.mix(home, Color.WHITE, 0.60);
        }
        if (type != null) {
            switch (type) {
                case START:
                case ARROW:
                    base = Mesh3D.mix(special, Color.WHITE, 0.72);
                    break;
                case STAR:
                    base = new Color(255, 241, 196);
                    break;
                case QUESTION:
                    base = Mesh3D.mix(special, Color.WHITE, 0.6);
                    break;
                case SURPRISE:
                    base = new Color(234, 223, 255);
                    break;
                default:
                    break;
            }
        }
        Path2D outer = cellPath(x, y, 0.025);
        if (outer == null || !onScreen(outer.getBounds2D())) {
            return;
        }
        // distance shortcuts only for plain squares: special squares and home columns always keep
        // their colour and symbol, so the 3D board matches the map exactly
        boolean plain = type == null && home == null;
        boolean far = plain && depth > 13;
        if (far) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        g.setColor(Mesh3D.mix(base, Color.BLACK, 0.34));
        g.fill(outer);
        if (plain && depth > 20) {
            fillCell(g, x, y, 0.07, base);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            return;
        }
        if (far) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        if (plain && depth > 11) {
            fillCell(g, x, y, 0.08, Mesh3D.mix(base, Color.WHITE, 0.12));
        } else {
            fillCell(g, x, y, 0.055, Mesh3D.mix(base, Color.WHITE, 0.30));
            fillCellShaded(g, x, y, 0.10, base);
        }
        if (plain && depth < 8) {
            speckle(g, x, y);
        }
        if (type != null) {
            drawSpecialGlyph(g, p, type, special, wx, wz);
        } else if (home != null) {
            double dx = (half - (x + 0.5)), dy = (half - (y + 0.5));
            double ax, az;
            if (Math.abs(dx) > Math.abs(dy)) {
                ax = Math.signum(dx);
                az = 0;
            } else {
                ax = 0;
                az = -Math.signum(dy);
            }
            drawShapeAt(g, chevron(), wx, wz, ax, az, -az, ax, 0.9, Mesh3D.mix(home, Color.BLACK, 0.2));
        }
    }

    private void speckle(Graphics2D g, int x, int y) {
        int hsh = x * 73856093 ^ y * 19349663;
        hsh ^= hsh >>> 13;
        hsh *= 0x5bd1e995;
        hsh ^= hsh >>> 15;
        double wx = x + 0.5 - half, wz = half - (y + 0.5);
        Color dot = new Color(60, 50, 40, 46);
        for (int k = 0; k < 3; k++) {
            int b = (hsh >>> (k * 8)) & 255;
            double ox = ((b & 15) / 15.0 - 0.5) * 0.6, oz = ((b >> 4) / 15.0 - 0.5) * 0.6;
            drawShapeAt(g, PEBBLE, wx + ox, wz + oz, 1, 0, 0, 1, 0.045 + 0.04 * ((b >> 2) & 3) / 3.0, dot);
        }
        if ((hsh & 7) == 0) {
            drawShapeAt(g, crack, wx, wz, 1, 0, 0, 1, 1.0, new Color(50, 40, 34, 70));
        }
    }

    private void drawSpecialGlyph(Graphics2D g, Point p, SquareType type, Color color, double wx, double wz) {
        double[] right = cam.rightOnGround();
        double[] fwd = cam.forwardOnGround();
        switch (type) {
            case START:
                fillCell(g, p.x, p.y, 0.2, color);
                drawShapeAt(g, glyph("#"), wx, wz, right[0], right[1], -fwd[0], -fwd[1], 0.72, Color.WHITE);
                break;
            case STAR:
                drawShapeAt(g, star(0.4), wx, wz, right[0], right[1], -fwd[0], -fwd[1], 1.0, new Color(251, 181, 37));
                break;
            case ARROW: {
                int d = board.specialSquareDirection(p);
                double[][] dirs = {{1, 0}, {0, -1}, {-1, 0}, {0, 1}};   // grid directions in world space (z is flipped)
                double[] dir = dirs[Math.floorMod(d, 4)];
                drawShapeAt(g, arrow(), wx, wz, dir[0], dir[1], -dir[1], dir[0], 1.0, color.darker());
                break;
            }
            case QUESTION: {
                Color fill = color.equals(BoardPanel.Q_EASY) ? new Color(48, 167, 111)
                        : color.equals(BoardPanel.Q_MEDIUM) ? new Color(220, 157, 32) : new Color(221, 69, 90);
                fillCell(g, p.x, p.y, 0.2, fill);
                drawShapeAt(g, glyph("?"), wx, wz, right[0], right[1], -fwd[0], -fwd[1], 0.72, Color.WHITE);
                break;
            }
            case SURPRISE:
                fillCell(g, p.x, p.y, 0.2, new Color(134, 76, 211));
                drawShapeAt(g, glyph("!"), wx, wz, right[0], right[1], -fwd[0], -fwd[1], 0.72, Color.WHITE);
                break;
            default:
                break;
        }
    }

    private void drawCastleFloor(Graphics2D g, int corner) {
        Color color = BoardPanel.cornerColor(corner);
        double[] a = castlePoint(corner, 0, 0), b = castlePoint(corner, 6, 6);
        double x0 = Math.min(a[0], b[0]), x1 = Math.max(a[0], b[0]);
        double z0 = Math.min(a[1], b[1]), z1 = Math.max(a[1], b[1]);
        Path2D outer = flatPath(new double[]{x0 + 0.03, x1 - 0.03, x1 - 0.03, x0 + 0.03},
                new double[]{z0 + 0.03, z0 + 0.03, z1 - 0.03, z1 - 0.03}, BOARD_TOP);
        if (outer == null || !onScreen(outer.getBounds2D())) {
            return;
        }
        g.setColor(Mesh3D.mix(color, Color.WHITE, 0.48));
        g.fill(outer);
        g.setColor(Mesh3D.mix(color, Color.BLACK, 0.1));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(outer);
        double[] c0 = castlePoint(corner, 2.65, 2.65), c1 = castlePoint(corner, 5.9, 5.9);
        double yx0 = Math.min(c0[0], c1[0]), yx1 = Math.max(c0[0], c1[0]);
        double yz0 = Math.min(c0[1], c1[1]), yz1 = Math.max(c0[1], c1[1]);
        Path2D yard = flatPath(new double[]{yx0, yx1, yx1, yx0}, new double[]{yz0, yz0, yz1, yz1}, BOARD_TOP + 0.01);
        if (yard != null) {
            g.setColor(new Color(236, 230, 218, 240));
            g.fill(yard);
            g.setColor(new Color(120, 108, 96, 70));
            g.setStroke(new BasicStroke(1f));
            for (int i = 1; i < 4; i++) {
                double fx = yx0 + (yx1 - yx0) * i / 4, fz = yz0 + (yz1 - yz0) * i / 4;
                if (cam.project(fx, BOARD_TOP + 0.012, yz0, tmpA) && cam.project(fx, BOARD_TOP + 0.012, yz1, tmpB)) {
                    g.draw(new Line2D.Double(tmpA[0], tmpA[1], tmpB[0], tmpB[1]));
                }
                if (cam.project(yx0, BOARD_TOP + 0.012, fz, tmpA) && cam.project(yx1, BOARD_TOP + 0.012, fz, tmpB)) {
                    g.draw(new Line2D.Double(tmpA[0], tmpA[1], tmpB[0], tmpB[1]));
                }
            }
        }
        for (int slot = 0; slot < 4; slot++) {
            double[] pad = padPosition(corner, slot);
            Path2D disc = discPath(pad[0], pad[1], BOARD_TOP + 0.02, 0.62, 18);
            if (disc != null) {
                g.setColor(Mesh3D.mix(color, Color.WHITE, 0.35));
                g.fill(disc);
                g.setColor(color.darker());
                g.setStroke(new BasicStroke(1.8f));
                g.draw(disc);
            }
        }
    }

    private void drawCenter(Graphics2D g) {
        double y = BOARD_TOP + 0.02;
        double[][][] tri = {
                {{-1.5, 1.5}, {-1.5, -1.5}, {0, 0}},    // left, green
                {{-1.5, 1.5}, {1.5, 1.5}, {0, 0}},      // top, yellow
                {{1.5, 1.5}, {1.5, -1.5}, {0, 0}},      // right, blue
                {{-1.5, -1.5}, {1.5, -1.5}, {0, 0}}};   // bottom, red
        for (int corner = 0; corner < 4; corner++) {
            Path2D path = flatPath(new double[]{tri[corner][0][0], tri[corner][1][0], tri[corner][2][0]},
                    new double[]{tri[corner][0][1], tri[corner][1][1], tri[corner][2][1]}, y);
            if (path != null) {
                g.setColor(BoardPanel.cornerColor(corner));
                g.fill(path);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.6f));
                g.draw(path);
            }
        }
        Path2D disc = discPath(0, 0, y + 0.01, 0.58, 22);
        if (disc != null) {
            g.setColor(Color.WHITE);
            g.fill(disc);
            g.setColor(new Color(35, 53, 105));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(disc);
        }
        double[] right = cam.rightOnGround();
        double[] fwd = cam.forwardOnGround();
        drawShapeAt(g, glyph("GOAL"), 0, 0, right[0], right[1], -fwd[0], -fwd[1], 0.62, new Color(35, 53, 105));
    }

    // --- shadows ---------------------------------------------------------------------------

    /** Shadows of castles and trees, merged into one shape so overlaps do not get darker. */
    private void drawShadows(Graphics2D g) {
        Path2D all = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (Caster c : casters) {
            int m = c.x.length;
            double cx = 0, cz = 0;
            for (int i = 0; i < m; i++) {
                cx += c.x[i] / m;
                cz += c.z[i] / m;
            }
            if (cam.depthOf(cx, c.groundY, cz) > 60) {
                continue;
            }
            double[][] pts = new double[m * 2][2];
            for (int i = 0; i < m; i++) {
                pts[i][0] = c.x[i] + shadowX * c.baseH;
                pts[i][1] = c.z[i] + shadowZ * c.baseH;
                pts[m + i][0] = cx + (c.x[i] - cx) * c.topScale + shadowX * c.topH;
                pts[m + i][1] = cz + (c.z[i] - cz) * c.topScale + shadowZ * c.topH;
            }
            double[][] hull = convexHull(pts);
            if (hull.length < 3) {
                continue;
            }
            double[] hx = new double[hull.length], hz = new double[hull.length], hy = new double[hull.length];
            for (int i = 0; i < hull.length; i++) {
                hx[i] = hull[i][0];
                hz[i] = hull[i][1];
                hy[i] = c.groundY + 0.015;
            }
            int count = cam.projectPolygon(hx, hy, hz, hull.length);
            if (count < 3) {
                continue;
            }
            all.moveTo(cam.sx[0], cam.sy[0]);
            for (int i = 1; i < count; i++) {
                all.lineTo(cam.sx[i], cam.sy[i]);
            }
            all.closePath();
        }
        g.setColor(new Color(0, 10, 30, 70));
        g.fill(all);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0, 10, 30, 26));
        g.draw(all);
    }

    private static double[][] convexHull(double[][] input) {
        double[][] pts = input.clone();
        Arrays.sort(pts, (a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        int k = 0;
        double[][] hull = new double[pts.length * 2][];
        for (double[] p : pts) {
            while (k >= 2 && cross(hull[k - 2], hull[k - 1], p) <= 0) {
                k--;
            }
            hull[k++] = p;
        }
        for (int i = pts.length - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && cross(hull[k - 2], hull[k - 1], pts[i]) <= 0) {
                k--;
            }
            hull[k++] = pts[i];
        }
        return Arrays.copyOf(hull, Math.max(0, k - 1));
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    /** Pawn shadows (angled plus a soft contact shadow), selection rings and landing dust. */
    private void drawGroundEffects(Graphics2D g) {
        int turn = board.turnCorner();
        for (Puff puff : puffs) {
            double age = (time - puff.born) / 0.5;
            Path2D ring = discPath(puff.x, puff.z, BOARD_TOP + 0.03, 0.25 + 0.65 * age, 18);
            if (ring != null) {
                g.setColor(new Color(255, 255, 255, (int) (150 * (1 - age))));
                g.setStroke(new BasicStroke(2.2f));
                g.draw(ring);
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            if (!board.isActiveCorner(corner)) {
                continue;
            }
            for (int idx = 0; idx < 4; idx++) {
                PieceVisual v = visuals[corner * 4 + idx];
                if (!v.init || !v.visible || cam.depthOf(v.curX, BOARD_TOP, v.curZ) < 0.5) {
                    continue;
                }
                double hop = Math.min(1, v.curH);
                double r = 0.36 * v.scale;
                double[][] pts = new double[16][2];
                for (int i = 0; i < 8; i++) {
                    double a = 2 * Math.PI * i / 8;
                    pts[i][0] = v.curX + shadowX * v.curH + r * Math.cos(a);
                    pts[i][1] = v.curZ + shadowZ * v.curH + r * Math.sin(a);
                    pts[8 + i][0] = v.curX + shadowX * (v.curH + PAWN_HEIGHT * v.scale) + 0.12 * Math.cos(a);
                    pts[8 + i][1] = v.curZ + shadowZ * (v.curH + PAWN_HEIGHT * v.scale) + 0.12 * Math.sin(a);
                }
                double[][] hull = convexHull(pts);
                double[] hx = new double[hull.length], hz = new double[hull.length];
                for (int i = 0; i < hull.length; i++) {
                    hx[i] = hull[i][0];
                    hz[i] = hull[i][1];
                }
                Path2D shadow = flatPath(hx, hz, BOARD_TOP + 0.02);
                if (shadow != null) {
                    g.setColor(new Color(0, 0, 0, 70));
                    g.fill(shadow);
                }
                Path2D contact = discPath(v.curX, v.curZ, BOARD_TOP + 0.025, 0.48 * (1 - 0.4 * hop) * v.scale, 16);
                if (contact != null) {
                    g.setColor(new Color(0, 0, 0, (int) (110 * (1 - 0.6 * hop))));
                    g.fill(contact);
                }
                if (isSelectable(corner, idx, turn, v)) {
                    boolean focus = corner * 4 + idx == focusKey;
                    double pulse = 0.5 + 0.5 * Math.sin(time * 6);
                    Path2D ring = discPath(v.curX, v.curZ, BOARD_TOP + 0.03, 0.66 + 0.1 * pulse + (focus ? 0.12 : 0), 24);
                    if (ring != null) {
                        g.setColor(new Color(255, 178, 67, (int) ((focus ? 120 : 70) + 90 * pulse)));
                        g.fill(ring);
                        g.setColor(focus ? Color.WHITE : new Color(255, 171, 56));
                        g.setStroke(new BasicStroke(focus ? 3.2f : 2.4f));
                        g.draw(ring);
                    }
                }
            }
        }
    }

    // --- objects (castles, trees, pawns), painter's algorithm ------------------------------------

    private static final class Item {
        final double depth;
        final Poly poly;
        final Tree tree;
        final int pawnKey;
        final double[] sx, sy;
        Item(double depth, Poly poly, Tree tree, int pawnKey, double[] sx, double[] sy) {
            this.depth = depth;
            this.poly = poly;
            this.tree = tree;
            this.pawnKey = pawnKey;
            this.sx = sx;
            this.sy = sy;
        }
    }

    private static final class Tree {
        final double x, z, scale;
        final Color leaf;
        final boolean bush;
        Tree(double x, double z, double scale, Color leaf, boolean bush) {
            this.x = x;
            this.z = z;
            this.scale = scale;
            this.leaf = leaf;
            this.bush = bush;
        }
    }

    private void drawObjects(Graphics2D g) {
        List<Item> items = new ArrayList<>(900);
        for (Poly p : statics) {
            if (!facing(p)) {
                continue;
            }
            double depth = cam.depthOf(p.cx, p.cy, p.cz);
            if (depth < 0.4 || depth > 70 || (p.small && depth > 15)) {
                continue;
            }
            int count = cam.projectPolygon(p.x, p.y, p.z, p.x.length);
            if (count < 3) {
                continue;
            }
            double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
            for (int i = 0; i < count; i++) {
                minX = Math.min(minX, cam.sx[i]);
                maxX = Math.max(maxX, cam.sx[i]);
                minY = Math.min(minY, cam.sy[i]);
                maxY = Math.max(maxY, cam.sy[i]);
            }
            if (maxX < 0 || minX > panelW || maxY < 0 || minY > panelH || (maxX - minX) * (maxY - minY) < 2.5) {
                continue;
            }
            items.add(new Item(depth, p, null, -1, Arrays.copyOf(cam.sx, count), Arrays.copyOf(cam.sy, count)));
        }
        for (Tree t : trees) {
            double depth = cam.depthOf(t.x, 0, t.z);
            if (depth < 1.0 || depth > 60) {
                continue;
            }
            items.add(new Item(depth, null, t, -1, null, null));
        }
        for (int corner = 0; corner < 4; corner++) {
            if (!board.isActiveCorner(corner)) {
                continue;
            }
            for (int idx = 0; idx < 4; idx++) {
                PieceVisual v = visuals[corner * 4 + idx];
                if (!v.init || !v.visible) {
                    continue;
                }
                double depth = cam.depthOf(v.curX, BOARD_TOP, v.curZ);
                if (depth < 0.8) {
                    continue;
                }
                items.add(new Item(depth - 0.25, null, null, corner * 4 + idx, null, null));
            }
        }
        items.sort((a, b) -> Double.compare(b.depth, a.depth));
        int turn = board.turnCorner();
        for (Item item : items) {
            if (item.poly != null) {
                fillPoly(g, item.poly, item.sx, item.sy, item.sx.length, item.depth, quality == 0);
            } else if (item.tree != null) {
                drawTree(g, item.tree);
            } else {
                drawPawnItem(g, item.pawnKey / 4, item.pawnKey % 4, turn, visuals[item.pawnKey], item.depth);
            }
        }
    }

    private void drawTree(Graphics2D g, Tree t) {
        double height = (t.bush ? 1.0 : 2.8) * t.scale;
        if (!cam.project(t.x, 0, t.z, tmpA) || !cam.project(t.x, height, t.z, tmpB)) {
            return;
        }
        double x = tmpA[0], ground = tmpA[1], ph = ground - tmpB[1];
        if (x < -ph || x > panelW + ph) {
            return;
        }
        double fog = fogAt(tmpA[2]);
        Color leaf = fog > 0 ? Mesh3D.mix(t.leaf, HAZE, fog) : t.leaf;
        if (!t.bush) {
            g.setPaint(new GradientPaint((float) (x - ph * 0.04), 0f, new Color(92, 64, 44),
                    (float) (x + ph * 0.04), 0f, new Color(140, 100, 66)));
            g.fill(new Rectangle2D.Double(x - ph * 0.04, ground - ph * 0.46, ph * 0.08, ph * 0.46));
        }
        double[][] blobs = t.bush
                ? new double[][]{{-0.22, -0.34, 0.30}, {0.22, -0.34, 0.30}, {0, -0.5, 0.34}}
                : new double[][]{{0, -0.50, 0.30}, {-0.20, -0.62, 0.25}, {0.20, -0.62, 0.25},
                        {-0.12, -0.84, 0.22}, {0.14, -0.80, 0.21}, {0, -0.68, 0.28}};
        for (double[] b : blobs) {
            double r = ph * b[2], cx = x + ph * b[0], cy = ground + ph * b[1];
            if (quality > 0) {
                g.setColor(leaf);
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                continue;
            }
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cx + r * 0.35, cy - r * 0.40), (float) (r * 1.5),
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{Mesh3D.mix(leaf, Color.WHITE, 0.38), leaf, Mesh3D.mix(leaf, Color.BLACK, 0.45)}));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
    }

    // --- pawns ------------------------------------------------------------------------------

    private boolean isSelectable(int corner, int idx, int turn, PieceVisual v) {
        if (corner != turn || v.hopping) {
            return false;
        }
        if (board.castleHighlighted() && board.pieceInCastle(corner, idx)) {
            return true;
        }
        return board.boardHighlighted() && v.onBoard && !v.finished;
    }

    private void drawPawnItem(Graphics2D g, int corner, int idx, int turn, PieceVisual v, double depth) {
        boolean selectable = isSelectable(corner, idx, turn, v);
        double bob = selectable ? 0.14 * (0.5 + 0.5 * Math.sin(time * 7 + idx)) : 0;
        double lift = v.curH + bob;
        if (!cam.project(v.curX, BOARD_TOP + lift, v.curZ, tmpA)
                || !cam.project(v.curX, BOARD_TOP + lift + PAWN_HEIGHT * v.scale, v.curZ, tmpB)) {
            return;
        }
        double groundY = tmpA[1], x = tmpA[0], ph = groundY - tmpB[1];
        if (ph < 4 || x < -ph || x > panelW + ph) {
            return;
        }
        drawTrail(g, v, BoardPanel.cornerColor(corner));
        double squash = 0;
        if (!v.hopping && v.dur > 0 && time - (v.t0 + v.dur) < 0.12) {
            squash = Math.max(0, 1 - (time - (v.t0 + v.dur)) / 0.12);
        }
        boolean small = ph < 70;
        BufferedImage sprite = small ? cache.pawnHalf(BoardPanel.cornerColor(corner)) : cache.pawn(BoardPanel.cornerColor(corner));
        double scale = ph / AssetCache.PAWN_BODY;
        double dw = AssetCache.PAWN_W * scale, dh = AssetCache.PAWN_H * scale;
        AffineTransform keep = g.getTransform();
        g.translate(x, groundY);
        g.scale(1 + 0.16 * squash, 1 - 0.2 * squash);
        g.translate(-x, -groundY);
        g.drawImage(sprite, (int) Math.round(x - dw / 2), (int) Math.round(groundY - AssetCache.PAWN_GROUND * scale),
                (int) Math.round(dw), (int) Math.round(dh), null);
        g.setTransform(keep);
        if (selectable) {
            double w = ph * 0.75;
            hits.add(new Hit(corner, idx, board.pieceInCastle(corner, idx),
                    new Rectangle2D.Double(x - w / 2, groundY - ph * 1.05, w, ph * 1.2), depth));
        }
    }

    /** Glowing trail the piece leaves while it hops. */
    private void drawTrail(Graphics2D g, PieceVisual v, Color color) {
        List<double[]> trail = v.trail;
        if (trail.size() < 2) {
            return;
        }
        Color core = Mesh3D.mix(color, Color.WHITE, 0.75), glow = Mesh3D.mix(color, Color.WHITE, 0.25);
        double[] prev = null;
        double prevDepth = 0;
        for (double[] p : trail) {
            if (!cam.project(p[0], p[1], p[2], tmpA)) {
                prev = null;
                continue;
            }
            if (prev != null) {
                double age = (time - p[3]) / TRAIL_SECONDS;
                double fade = Math.max(0, 1 - age);
                double width = Math.max(2, 0.16 * cam.focal / tmpA[2]) * (0.35 + 0.65 * fade);
                Line2D line = new Line2D.Double(prev[0], prev[1], tmpA[0], tmpA[1]);
                g.setStroke(new BasicStroke((float) (width * 2.4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), (int) (34 * fade)));
                g.draw(line);
                g.setStroke(new BasicStroke((float) (width * 1.3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), (int) (80 * fade)));
                g.draw(line);
                g.setStroke(new BasicStroke((float) (width * 0.5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(core.getRed(), core.getGreen(), core.getBlue(), (int) (210 * fade)));
                g.draw(line);
            }
            prev = new double[]{tmpA[0], tmpA[1]};
            prevDepth = tmpA[2];
        }
    }

    // --- flags (animated, drawn every frame) ---------------------------------------------------

    private void drawFlags(Graphics2D g) {
        double[] right = cam.rightOnGround();
        for (double[] f : flags) {
            if (!cam.project(f[0], f[2], f[1], tmpA) || !cam.project(f[0], f[2] + f[3], f[1], tmpB)
                    || tmpA[2] > 60) {
                continue;
            }
            Color color = BoardPanel.cornerColor((int) f[4]);
            g.setColor(new Color(70, 52, 42));
            g.setStroke(new BasicStroke(Math.max(1.4f, (float) (tmpA[1] - tmpB[1]) * 0.05f)));
            g.draw(new Line2D.Double(tmpA[0], tmpA[1], tmpB[0], tmpB[1]));
            int segments = 7;
            double len = 1.0, top = f[2] + f[3], flagH = 0.52;
            double phase = f[0] * 1.7 + f[1] * 2.3;
            double[][] upper = new double[segments + 1][2], lower = new double[segments + 1][2];
            boolean ok = true;
            for (int i = 0; i <= segments && ok; i++) {
                double t = i / (double) segments;
                double wave = Math.sin(time * 5.5 + phase - t * 4.2) * 0.10 * t;
                double px = f[0] + right[0] * len * t, pz = f[1] + right[1] * len * t;
                if (!cam.project(px, top - 0.03 + wave, pz, tmpA)) {
                    ok = false;
                    break;
                }
                upper[i][0] = tmpA[0];
                upper[i][1] = tmpA[1];
                if (!cam.project(px, top - flagH * (1 - 0.35 * t) + wave, pz, tmpA)) {
                    ok = false;
                    break;
                }
                lower[i][0] = tmpA[0];
                lower[i][1] = tmpA[1];
            }
            if (!ok) {
                continue;
            }
            Path2D ribbon = new Path2D.Double();
            ribbon.moveTo(upper[0][0], upper[0][1]);
            for (int i = 1; i <= segments; i++) {
                ribbon.lineTo(upper[i][0], upper[i][1]);
            }
            for (int i = segments; i >= 0; i--) {
                ribbon.lineTo(lower[i][0], lower[i][1]);
            }
            ribbon.closePath();
            g.setPaint(new GradientPaint((float) upper[0][0], 0f, Mesh3D.mix(color, Color.WHITE, 0.22),
                    (float) upper[segments][0] + 0.01f, 0f, Mesh3D.mix(color, Color.BLACK, 0.25)));
            g.fill(ribbon);
            g.setColor(new Color(0, 0, 0, 70));
            g.setStroke(new BasicStroke(1f));
            g.draw(ribbon);
        }
    }

    // ------------------------------------------------------------------
    // Geometry helpers
    // ------------------------------------------------------------------

    private boolean facing(Poly p) {
        if (p.twoSided) {
            return true;
        }
        double vx = camPos[0] - p.cx, vy = camPos[1] - p.cy, vz = camPos[2] - p.cz;
        return p.nx * vx + p.ny * vy + p.nz * vz > 0;
    }

    private boolean onScreen(Rectangle2D b) {
        return b.getMaxX() >= 0 && b.getMinX() <= panelW && b.getMaxY() >= 0 && b.getMinY() <= panelH;
    }

    private double fogAt(double depth) {
        return Math.max(0, Math.min(0.5, (depth - 24) / 40.0));
    }

    private static Color shade(Color c, double f) {
        return new Color(clamp(c.getRed() * f), clamp(c.getGreen() * f), clamp(c.getBlue() * f));
    }

    private void drawPoly(Graphics2D g, Poly p, boolean details) {
        int count = cam.projectPolygon(p.x, p.y, p.z, p.x.length);
        if (count < 3) {
            return;
        }
        double depth = cam.depthOf(p.cx, p.cy, p.cz);
        fillPoly(g, p, Arrays.copyOf(cam.sx, count), Arrays.copyOf(cam.sy, count), count, depth, details);
    }

    private void fillPoly(Graphics2D g, Poly p, double[] sx, double[] sy, int count, double depth, boolean details) {
        // small polygons are filled without anti-aliasing: it is much faster and hardly visible
        double w0 = 0, h0 = 0;
        double lx = 1e9, hx = -1e9, ly = 1e9, hy = -1e9;
        for (int i = 0; i < count; i++) {
            lx = Math.min(lx, sx[i]);
            hx = Math.max(hx, sx[i]);
            ly = Math.min(ly, sy[i]);
            hy = Math.max(hy, sy[i]);
        }
        w0 = hx - lx;
        h0 = hy - ly;
        boolean smallPoly = w0 * h0 < 260;
        if (smallPoly) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        fillPolyImpl(g, p, sx, sy, count, depth, details);
        if (smallPoly) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    private void fillPolyImpl(Graphics2D g, Poly p, double[] sx, double[] sy, int count, double depth, boolean details) {
        double light = p.twoSided ? 0.95 : 0.46 + 0.62 * Math.max(0, p.nx * lightX + p.ny * lightY + p.nz * lightZ);
        double fog = fogAt(depth);
        Color lit = shade(p.color, light);
        Path2D path = new Path2D.Double();
        path.moveTo(sx[0], sy[0]);
        for (int i = 1; i < count; i++) {
            path.lineTo(sx[i], sy[i]);
        }
        path.closePath();
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        for (int i = 0; i < count; i++) {
            minX = Math.min(minX, sx[i]);
            maxX = Math.max(maxX, sx[i]);
            minY = Math.min(minY, sy[i]);
            maxY = Math.max(maxY, sy[i]);
        }
        double area0 = (maxX - minX) * (maxY - minY);
        // gradients and outlines are only worth their cost on polygons that are big on screen
        boolean shaded = details && area0 > 700 && maxY - minY > 3
                && ((p.x.length == 4 && Math.abs(p.ny) < 0.6) || p.x.length == 3);
        if (shaded) {
            // lighter towards the top, darker towards the foot (ambient occlusion)
            Color top = shade(lit, 1.12), bottom = shade(lit, 0.70);
            if (fog > 0) {
                top = Mesh3D.mix(top, HAZE, fog);
                bottom = Mesh3D.mix(bottom, HAZE, fog);
            }
            g.setPaint(new GradientPaint(0, (float) minY, top, 0, (float) maxY, bottom));
        } else {
            g.setColor(fog > 0 ? Mesh3D.mix(lit, HAZE, fog) : lit);
        }
        g.fill(path);
        if (!details || count != p.x.length || area0 < 150) {
            return;
        }
        g.setColor(new Color(0, 0, 0, 52));
        g.setStroke(THIN);
        g.draw(path);
        double area = area0;
        if (count == 3 && p.brickRows > 0 && area > 250) {
            // roof tiles
            Path2D rows = new Path2D.Double();
            for (int r = 1; r < p.brickRows; r++) {
                double t = r / (double) p.brickRows;
                rows.moveTo(lerp(sx[0], sx[2], t), lerp(sy[0], sy[2], t));
                rows.lineTo(lerp(sx[1], sx[2], t), lerp(sy[1], sy[2], t));
            }
            g.setColor(new Color(0, 0, 0, (int) (46 * (1 - fog))));
            g.setStroke(THIN);
            g.draw(rows);
            return;
        }
        if (count != 4 || area < 500) {
            return;
        }
        if (p.brickRows > 0) {
            // all mortar joints go into one path: one stroke instead of dozens
            Path2D mortar = new Path2D.Double();
            for (int r = 1; r < p.brickRows; r++) {
                double t = r / (double) p.brickRows;
                mortar.moveTo(lerp(sx[0], sx[3], t), lerp(sy[0], sy[3], t));
                mortar.lineTo(lerp(sx[1], sx[2], t), lerp(sy[1], sy[2], t));
            }
            if (area > 1500 && p.widthWorld > 0) {
                int cols = Math.max(2, (int) Math.round(p.widthWorld / 0.5));
                for (int r = 0; r < p.brickRows; r++) {
                    double v0 = r / (double) p.brickRows, v1 = (r + 1) / (double) p.brickRows;
                    for (int k = 0; k < cols; k++) {
                        double u = (k + 0.5 * (r & 1)) / cols;
                        if (u <= 0.001) {
                            continue;
                        }
                        double[] a0 = face(sx, sy, u, v0), c0 = face(sx, sy, u, v1);
                        mortar.moveTo(a0[0], a0[1]);
                        mortar.lineTo(c0[0], c0[1]);
                    }
                }
            }
            g.setColor(new Color(0, 0, 0, (int) (36 * (1 - fog))));
            g.setStroke(THIN);
            g.draw(mortar);
        }
        if (p.windows != null) {
            for (double[] win : p.windows) {
                Path2D w = arch(sx, sy, win[0], win[1], win[2], win[3]);
                g.setColor(Mesh3D.mix(p.windowColor, HAZE, fog));
                g.fill(w);
                g.setColor(new Color(255, 255, 255, (int) (80 * (1 - fog))));
                g.setStroke(new BasicStroke(1f));
                g.draw(w);
            }
        }
        if (p.doors != null) {
            for (double[] d : p.doors) {
                Path2D door = arch(sx, sy, d[0], d[1], d[2], d[3]);
                g.setColor(Mesh3D.mix(new Color(46, 28, 20), HAZE, fog));
                g.fill(door);
                Path2D plank = arch(sx, sy, d[0] + 0.012, d[1] - 0.012, d[2], d[3] - 0.015);
                g.setColor(Mesh3D.mix(new Color(112, 72, 42), HAZE, fog));
                g.fill(plank);
                g.setColor(new Color(40, 24, 16, 160));
                g.setStroke(new BasicStroke(1f));
                for (int k = 1; k < 4; k++) {
                    double u = d[0] + (d[1] - d[0]) * k / 4.0;
                    double[] a = face(sx, sy, u, d[2]), c = face(sx, sy, u, d[3] - 0.04);
                    g.draw(new Line2D.Double(a[0], a[1], c[0], c[1]));
                }
                g.setColor(new Color(30, 20, 14));
                g.draw(door);
            }
        }
        if (p.banners != null && p.bannerColor != null) {
            for (double[] bn : p.banners) {
                double u0 = bn[0], u1 = bn[1], v0 = bn[2], v1 = bn[3];
                double uc = (u0 + u1) / 2;
                Path2D banner = new Path2D.Double();
                double[][] pts = {{u0, v1}, {u1, v1}, {u1, v0}, {uc, v0 + (v1 - v0) * 0.2}, {u0, v0}};
                for (int i = 0; i < pts.length; i++) {
                    double[] q = face(sx, sy, pts[i][0], pts[i][1]);
                    if (i == 0) {
                        banner.moveTo(q[0], q[1]);
                    } else {
                        banner.lineTo(q[0], q[1]);
                    }
                }
                banner.closePath();
                g.setColor(Mesh3D.mix(shade(p.bannerColor, 0.55 + 0.45 * light), HAZE, fog));
                g.fill(banner);
                g.setColor(new Color(0, 0, 0, 80));
                g.draw(banner);
            }
        }
    }

    /** Bilinear point on a projected quad (bottom-left, bottom-right, top-right, top-left). */
    private static double[] face(double[] sx, double[] sy, double u, double v) {
        double bx = lerp(sx[0], sx[1], u), by = lerp(sy[0], sy[1], u);
        double tx = lerp(sx[3], sx[2], u), ty = lerp(sy[3], sy[2], u);
        return new double[]{lerp(bx, tx, v), lerp(by, ty, v)};
    }

    private static Path2D arch(double[] sx, double[] sy, double u0, double u1, double v0, double v1) {
        double vm = v0 + (v1 - v0) * 0.72, uc = (u0 + u1) / 2;
        double[][] pts = {{u0, v0}, {u1, v0}, {u1, vm}, {uc, v1}, {u0, vm}};
        Path2D path = new Path2D.Double();
        for (int i = 0; i < pts.length; i++) {
            double[] q = face(sx, sy, pts[i][0], pts[i][1]);
            if (i == 0) {
                path.moveTo(q[0], q[1]);
            } else {
                path.lineTo(q[0], q[1]);
            }
        }
        path.closePath();
        return path;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    /** Projects a flat polygon lying at the given height (clipped at the camera); null if not visible. */
    private Path2D flatPath(double[] xs, double[] zs, double y) {
        double[] ys = new double[xs.length];
        Arrays.fill(ys, y);
        int count = cam.projectPolygon(xs, ys, zs, xs.length);
        if (count < 3) {
            return null;
        }
        Path2D path = new Path2D.Double();
        path.moveTo(cam.sx[0], cam.sy[0]);
        for (int i = 1; i < count; i++) {
            path.lineTo(cam.sx[i], cam.sy[i]);
        }
        path.closePath();
        return path;
    }

    private Path2D discPath(double cx, double cz, double y, double radius, int segments) {
        double[] xs = new double[segments], zs = new double[segments];
        for (int i = 0; i < segments; i++) {
            double a = 2 * Math.PI * i / segments;
            xs[i] = cx + radius * Math.cos(a);
            zs[i] = cz + radius * Math.sin(a);
        }
        return flatPath(xs, zs, y);
    }

    private Path2D cellPath(int x, int y, double inset) {
        double x0 = x + inset - half, x1 = x + 1 - inset - half;
        double z0 = half - (y + 1 - inset), z1 = half - (y + inset);
        return flatPath(new double[]{x0, x1, x1, x0}, new double[]{z0, z0, z1, z1}, BOARD_TOP);
    }

    private void fillCell(Graphics2D g, int x, int y, double inset, Color color) {
        Path2D path = cellPath(x, y, inset);
        if (path != null) {
            g.setColor(color);
            g.fill(path);
        }
    }

    private void fillCellShaded(Graphics2D g, int x, int y, double inset, Color color) {
        Path2D path = cellPath(x, y, inset);
        if (path == null) {
            return;
        }
        Rectangle2D b = path.getBounds2D();
        if (b.getHeight() < 1.5) {
            g.setColor(color);
        } else {
            g.setPaint(new GradientPaint(0, (float) b.getMaxY(), shade(color, 0.90),
                    0, (float) b.getMinY(), shade(color, 1.05)));
        }
        g.fill(path);
    }

    /**
     * Draws a shape that lies flat on the board. Local x runs along (ax, az), local y along
     * (bx, bz) (both world directions), one local unit is one board square.
     */
    private void drawShapeAt(Graphics2D g, Shape shape, double wx, double wz,
                             double ax, double az, double bx, double bz, double size, Color color) {
        double y = BOARD_TOP + 0.03;
        if (!cam.project(wx, y, wz, tmpA) || !cam.project(wx + ax, y, wz + az, tmpB)) {
            return;
        }
        double ox = tmpA[0], oy = tmpA[1], ex = tmpB[0], ey = tmpB[1];
        if (!cam.project(wx + bx, y, wz + bz, tmpB)) {
            return;
        }
        AffineTransform at = new AffineTransform(ex - ox, ey - oy, tmpB[0] - ox, tmpB[1] - oy, ox, oy);
        Graphics2D gg = (Graphics2D) g.create();
        gg.transform(at);
        gg.scale(size, size);
        gg.setColor(color);
        gg.fill(shape);
        gg.dispose();
    }

    private Shape glyph(String text) {
        Shape cached = glyphCache.get(text);
        if (cached != null) {
            return cached;
        }
        GlyphVector gv = new Font("Segoe UI", Font.BOLD, 100)
                .createGlyphVector(new FontRenderContext(null, true, true), text);
        Rectangle2D b = gv.getVisualBounds();
        AffineTransform at = new AffineTransform();
        at.scale(0.01, 0.01);
        at.translate(-b.getCenterX(), -b.getCenterY());
        Shape shape = at.createTransformedShape(gv.getOutline());
        glyphCache.put(text, shape);
        return shape;
    }

    private Shape star(double radius) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? radius : radius * 0.46;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            if (i == 0) {
                star.moveTo(Math.cos(a) * r, Math.sin(a) * r);
            } else {
                star.lineTo(Math.cos(a) * r, Math.sin(a) * r);
            }
        }
        star.closePath();
        return star;
    }

    private Shape arrow() {
        double l = 0.37;
        Path2D a = new Path2D.Double();
        a.moveTo(-l, -l * 0.27);
        a.lineTo(0, -l * 0.27);
        a.lineTo(0, -l * 0.69);
        a.lineTo(l, 0);
        a.lineTo(0, l * 0.69);
        a.lineTo(0, l * 0.27);
        a.lineTo(-l, l * 0.27);
        a.closePath();
        return a;
    }

    private Shape chevron() {
        Path2D c = new Path2D.Double();
        c.moveTo(-0.25, -0.30);
        c.lineTo(-0.07, -0.30);
        c.lineTo(0.22, 0);
        c.lineTo(-0.07, 0.30);
        c.lineTo(-0.25, 0.30);
        c.lineTo(0.04, 0);
        c.closePath();
        return c;
    }

    // ------------------------------------------------------------------
    // Vignette and HUD
    // ------------------------------------------------------------------

    private void drawVignette(Graphics2D g, int w, int h) {
        if (vignette == null || vignette.getWidth() != w || vignette.getHeight() != h) {
            vignette = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D vg = vignette.createGraphics();
            double r = Math.max(w, h) * 0.78;
            vg.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h * 0.52), (float) r,
                    new float[]{0f, 0.62f, 1f},
                    new Color[]{new Color(10, 20, 50, 0), new Color(10, 20, 50, 0), new Color(10, 20, 50, 90)}));
            vg.fillRect(0, 0, w, h);
            vg.dispose();
        }
        g.drawImage(vignette, 0, 0, null);
    }

    private void drawHud(Graphics2D g, int w, int h) {
        int corner = board.turnCorner();
        Color color = BoardPanel.cornerColor(corner);
        String name = board.turnName();
        if (name.length() > 14) {
            name = name.substring(0, 13) + ".";
        }
        double px = 52, py = 18, pw = 262, ph = 46;
        Path2D plate = new Path2D.Double();
        plate.moveTo(px, py);
        plate.lineTo(px + pw, py);
        plate.lineTo(px + pw - 16, py + ph);
        plate.lineTo(px, py + ph);
        plate.closePath();
        g.setPaint(new GradientPaint(0, (float) py, new Color(34, 52, 120), 0, (float) (py + ph), new Color(14, 26, 74)));
        g.fill(plate);
        g.setColor(new Color(150, 190, 255, 200));
        g.setStroke(new BasicStroke(2f));
        g.draw(plate);
        g.setFont(new Font("Segoe UI", Font.BOLD, 19));
        g.setColor(Color.WHITE);
        String label = "Turn: ";
        g.drawString(label, (float) (px + 54), (float) (py + 22));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Mesh3D.mix(color, Color.WHITE, 0.2));
        g.drawString(name, (float) (px + 54 + fm.stringWidth(label)), (float) (py + 22));
        int home = board.finishedCounts()[corner];
        g.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g.setColor(new Color(255, 200, 110));
        g.drawString("PIECES HOME  " + home + " / 4", (float) (px + 54), (float) (py + 38));
        double bcx = 38, bcy = 41, br = 31;
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(bcx - br + 2, bcy - br + 4, br * 2, br * 2));
        g.setColor(new Color(12, 22, 62));
        g.fill(new Ellipse2D.Double(bcx - br, bcy - br, br * 2, br * 2));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < 4; k++) {
            g.setColor(BoardPanel.cornerColor(k));
            g.draw(new Arc2D.Double(bcx - br + 3, bcy - br + 3, 2 * br - 6, 2 * br - 6, 90 - k * 90 - 80, 70, Arc2D.OPEN));
        }
        g.setColor(new Color(10, 16, 44));
        g.fill(new Ellipse2D.Double(bcx - br + 8, bcy - br + 8, 2 * br - 16, 2 * br - 16));
        BufferedImage sprite = cache.pawnHalf(color);
        double scale = 36.0 / (AssetCache.PAWN_BODY / 2.0);
        g.drawImage(sprite, (int) (bcx - sprite.getWidth() * scale / 2),
                (int) (bcy + 18 - (AssetCache.PAWN_GROUND / 2.0) * scale),
                (int) (sprite.getWidth() * scale), (int) (sprite.getHeight() * scale), null);
        drawControls(g, w, h);
        if (time < 14) {
            String hint = "Drag: look around   ·   Wheel: zoom   ·   Double-click: reset view";
            float alpha = (float) Math.max(0, Math.min(1, (14 - time) / 3.0));
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics hm = g.getFontMetrics();
            int hw = hm.stringWidth(hint) + 24;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(10, 18, 48, 170));
            g.fill(new RoundRectangle2D.Double(18, h - 46, hw, 28, 16, 16));
            g.setColor(Color.WHITE);
            g.drawString(hint, 30, h - 27);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    /** Piece switcher and the green "Go!" button in the bottom right corner. */
    private void drawControls(Graphics2D g, int w, int h) {
        int turn = board.turnCorner();
        List<Integer> selectable = selectableKeys(turn);
        Collections.sort(selectable);
        boolean canMove = selectable.contains(focusKey);
        boolean canRoll = rollEnabled != null && rollEnabled.getAsBoolean();
        boolean active = canMove || canRoll;

        double bw = 132, bh = 62, bx = w - bw - 24, by = h - bh - 24;
        goBounds = new RoundRectangle2D.Double(bx, by, bw, bh, 22, 22);
        double lift = goHover && active ? 2 : 0;
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(bx + 2, by + 6, bw, bh, 22, 22));
        Color top = active ? new Color(124, 226, 96) : new Color(150, 176, 150);
        Color bottom = active ? new Color(34, 148, 52) : new Color(96, 120, 100);
        g.setPaint(new GradientPaint(0, (float) (by - lift), top, 0, (float) (by + bh), bottom));
        g.fill(new RoundRectangle2D.Double(bx, by - lift, bw, bh, 22, 22));
        g.setColor(new Color(24, 96, 38));
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Double(bx + 1.5, by - lift + 1.5, bw - 3, bh - 3, 20, 20));
        g.setColor(new Color(255, 255, 255, active ? 110 : 60));
        g.fill(new RoundRectangle2D.Double(bx + 8, by - lift + 5, bw - 16, bh * 0.38, 16, 16));
        g.setFont(new Font("Segoe UI", Font.BOLD, 30));
        FontMetrics fm = g.getFontMetrics();
        String text = "Go!";
        double tx = bx + (bw - fm.stringWidth(text)) / 2, ty = by - lift + (bh - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(new Color(10, 70, 24, 200));
        g.drawString(text, (float) tx + 1.5f, (float) ty + 2f);
        g.setColor(Color.WHITE);
        g.drawString(text, (float) tx, (float) ty);

        prevBounds = new Rectangle2D.Double();
        nextBounds = new Rectangle2D.Double();
        if (selectable.size() > 1) {
            double d = 44, cy = by + (bh - d) / 2;
            double nx = bx - d - 14, pvx = nx - d - 10;
            prevBounds = new Ellipse2D.Double(pvx, cy, d, d);
            nextBounds = new Ellipse2D.Double(nx, cy, d, d);
            drawRoundButton(g, pvx, cy, d, false);
            drawRoundButton(g, nx, cy, d, true);
            int at = Math.max(0, selectable.indexOf(focusKey)) + 1;
            String label = "PIECE " + at + " / " + selectable.size();
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            fm = g.getFontMetrics();
            double lx = pvx + (2 * d + 10 - fm.stringWidth(label)) / 2;
            g.setColor(new Color(10, 18, 48, 150));
            g.fill(new RoundRectangle2D.Double(lx - 8, cy - 26, fm.stringWidth(label) + 16, 20, 14, 14));
            g.setColor(Color.WHITE);
            g.drawString(label, (float) lx, (float) (cy - 11));
        }
    }

    private void drawRoundButton(Graphics2D g, double x, double y, double d, boolean right) {
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(x + 1.5, y + 4, d, d));
        g.setPaint(new GradientPaint(0, (float) y, new Color(86, 112, 232), 0, (float) (y + d), new Color(34, 52, 150)));
        g.fill(new Ellipse2D.Double(x, y, d, d));
        g.setColor(new Color(190, 210, 255));
        g.setStroke(new BasicStroke(2.4f));
        g.draw(new Ellipse2D.Double(x + 1, y + 1, d - 2, d - 2));
        double cx = x + d / 2, cy = y + d / 2, s = d * 0.2;
        Path2D arrowHead = new Path2D.Double();
        double dir = right ? 1 : -1;
        arrowHead.moveTo(cx - dir * s * 0.7, cy - s);
        arrowHead.lineTo(cx + dir * s * 0.8, cy);
        arrowHead.lineTo(cx - dir * s * 0.7, cy + s);
        arrowHead.closePath();
        g.setColor(Color.WHITE);
        g.fill(arrowHead);
    }

    private static final class SkyObject {
        final int type;          // 0 sun, 1 cloud, 2 rock
        final int sprite;
        double az;               // direction in the sky (radians)
        final double yFrac, scale, speed;
        final float alpha;
        SkyObject(int type, int sprite, double az, double yFrac, double scale, float alpha, double speed) {
            this.type = type;
            this.sprite = sprite;
            this.az = az;
            this.yFrac = yFrac;
            this.scale = scale;
            this.alpha = alpha;
            this.speed = speed;
        }
    }
}
