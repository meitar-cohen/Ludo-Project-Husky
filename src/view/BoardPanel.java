package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Board component for the Ludo game screen.
 * Draws the board for the selected difficulty (Easy 15x15, Medium with an extra
 * ring of 64 squares, Hard with extra rings of 64 and 72 squares), the special
 * squares, the castles and the active player's pieces.
 */
public class BoardPanel extends JPanel {

    // Corner order: 0 = top-left, 1 = top-right, 2 = bottom-right, 3 = bottom-left
    public static final String[] CORNER_COLOR_NAMES = {"Green", "Yellow", "Blue", "Red"};

    public static final Color RED = new Color(220, 55, 55);
    public static final Color BLUE = new Color(45, 95, 215);
    public static final Color GREEN = new Color(40, 160, 75);
    public static final Color YELLOW = new Color(235, 185, 20);
    private static final Color[] CORNER_COLORS = {GREEN, YELLOW, BLUE, RED};

    public static final Color Q_EASY = new Color(120, 200, 120);
    public static final Color Q_MEDIUM = new Color(250, 215, 80);
    public static final Color Q_HARD = new Color(235, 100, 100);
    public static final Color SURPRISE = new Color(150, 90, 200);

    private static final Color BOARD_BG = new Color(243, 246, 255);
    private static final Color CELL_BORDER = new Color(201, 209, 228);
    private static final Color HIGHLIGHT = new Color(255, 171, 56);

    private enum Type { START, STAR, ARROW, QUESTION, SURPRISE }

    private static class Special {
        Type type;
        Color color;
        int dir; // arrows: 0 right, 1 down, 2 left, 3 up
        Special(Type type, Color color, int dir) {
            this.type = type;
            this.color = color;
            this.dir = dir;
        }
    }

    public interface PieceClickListener {
        void castlePieceClicked(int slot);
        void boardPieceClicked(int pieceIndex);
    }

    private final String difficulty;
    private final int rings;   // 0 easy, 1 medium, 2 hard
    private final int size;    // board size in cells
    private int activeCorner;
    private final boolean[] activeCorners = new boolean[4];
    private final int[] piecesInCastle = new int[4];
    private final boolean[][] castlePieces = new boolean[4][4];

    private final List<Point> activePath = new ArrayList<>();
    private final List<Point> activeHomePath = new ArrayList<>();
    private final Map<Point, Special> specials = new HashMap<>();
    private final Map<Point, Color> homeCells = new HashMap<>();

    /**
     * SRS 3.2.4.3.1 (Maor Mizrahi) -
     * Stores the visual state of each piece for every player color.
     */
    private final boolean[][] piecesOnBoard = new boolean[4][4];
    private final int[][] pieceSteps = new int[4][4];
    private final boolean[][] piecesInHomePath = new boolean[4][4];
    private int selectedPieceIndex;

    private boolean highlightCastle = false;
    private boolean highlightBoard = false;

    private PieceClickListener listener;
    private Timer animation;
    private Timer highlightPulse;
    private boolean pulsePhase;

    public BoardPanel(String difficulty, String[] playerColorNames) {
        this.difficulty = difficulty;
        if ("Hard".equals(difficulty)) {
            rings = 2;
        } else if ("Medium".equals(difficulty)) {
            rings = 1;
        } else {
            rings = 0;
        }
        size = 15 + 2 * rings;

        for (String name : playerColorNames) {
            int c = cornerOf(name);
            activeCorners[c] = true;
            piecesInCastle[c] = 4;
            for (int slot = 0; slot < 4; slot++) {
                castlePieces[c][slot] = true;
            }
        }
        activeCorner = cornerOf(playerColorNames[0]);

        buildBoard();

        setBackground(new Color(244, 247, 255));
        setPreferredSize(new Dimension(700, 700));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getPoint());
            }
        });
    }

    public static int cornerOf(String colorName) {
        for (int i = 0; i < 4; i++) {
            if (CORNER_COLOR_NAMES[i].equalsIgnoreCase(colorName)) {
                return i;
            }
        }
        return 0;
    }

    public static Color colorOf(String colorName) {
        return CORNER_COLORS[cornerOf(colorName)];
    }

    public void setPieceClickListener(PieceClickListener listener) {
        this.listener = listener;
    }

    // ------------------------------------------------------------------
    // Board structure
    // ------------------------------------------------------------------

    private Point inner(int c, int r) {
        return new Point(c + rings, r + rings);
    }

    /** The 52 squares of the shared path of the basic board, clockwise. */
    private List<Point> innerPath() {
        List<Point> p = new ArrayList<>();
        for (int c = 1; c <= 5; c++) p.add(inner(c, 6));
        for (int r = 5; r >= 0; r--) p.add(inner(6, r));
        p.add(inner(7, 0));
        p.add(inner(8, 0));
        for (int r = 1; r <= 5; r++) p.add(inner(8, r));
        for (int c = 9; c <= 14; c++) p.add(inner(c, 6));
        p.add(inner(14, 7));
        p.add(inner(14, 8));
        for (int c = 13; c >= 9; c--) p.add(inner(c, 8));
        for (int r = 9; r <= 14; r++) p.add(inner(8, r));
        p.add(inner(7, 14));
        p.add(inner(6, 14));
        for (int r = 13; r >= 9; r--) p.add(inner(6, r));
        for (int c = 5; c >= 0; c--) p.add(inner(c, 8));
        p.add(inner(0, 7));
        p.add(inner(0, 6));
        return p;
    }

    /** Perimeter squares of an outer ring, clockwise from its top-left corner. */
    private List<Point> ringPath(int ring) {
        int n = size - 2 * ring;
        int o = ring;
        List<Point> p = new ArrayList<>();
        for (int c = 0; c < n; c++) p.add(new Point(o + c, o));
        for (int r = 1; r < n; r++) p.add(new Point(o + n - 1, o + r));
        for (int c = n - 2; c >= 0; c--) p.add(new Point(o + c, o + n - 1));
        for (int r = n - 2; r >= 1; r--) p.add(new Point(o, o + r));
        return p;
    }

    private Point ringCell(List<Point> ring, int corner, int step) {
        int sideLen = ring.size() / 4;
        int idx = (corner * sideLen + step) % ring.size();
        if (idx < 0) idx += ring.size();
        return ring.get(idx);
    }

    private void buildBoard() {
        List<Point> inner = innerPath();
        int[] startIdx = {0, 13, 26, 39};

        // Home paths (5 coloured squares leading to the centre)
        for (int i = 1; i <= 5; i++) {
            homeCells.put(inner(i, 7), GREEN);
            homeCells.put(inner(7, i), YELLOW);
            homeCells.put(inner(14 - i, 7), BLUE);
            homeCells.put(inner(7, 14 - i), RED);
        }

        // Inner board special squares (all levels)
        for (int c = 0; c < 4; c++) {
            Point star = inner.get((startIdx[c] + 8) % 52);
            specials.put(star, new Special(Type.STAR, null, 0));
            Point arrow = inner.get((startIdx[c] + 50) % 52);
            specials.put(arrow, new Special(Type.ARROW, CORNER_COLORS[c], c));
            if (rings == 0) {
                specials.put(inner.get(startIdx[c]), new Special(Type.START, CORNER_COLORS[c], 0));
            }
        }

        for (int i = 1; i <= 5; i++) {
            switch (activeCorner) {
                case 0: activeHomePath.add(inner(i, 7)); break;
                case 1: activeHomePath.add(inner(7, i)); break;
                case 2: activeHomePath.add(inner(14 - i, 7)); break;
                default: activeHomePath.add(inner(7, 14 - i)); break;
            }
        }

        if (rings == 0) {
            int s = startIdx[activeCorner];
            for (int i = 0; i <= 20; i++) {
                activePath.add(inner.get((s + i) % 52));
            }
        }

        if (rings > 0) {
            // Configured positions for Medium and Hard board layouts.
            List<Point> outer = ringPath(0);
            int k = activeCorner;
            Color[] qLevels = {Q_EASY, Q_MEDIUM, Q_HARD, Q_EASY};
            for (int i = 0; i < 4; i++) {
                int c = (k + i) % 4;
                specials.put(ringCell(outer, c, 0), new Special(Type.START, CORNER_COLORS[c], 0));
                specials.put(ringCell(outer, c, 3), new Special(Type.QUESTION, qLevels[i], 0));
                specials.put(ringCell(outer, c, 11), new Special(Type.STAR, null, 0));
                int mid = (size - 1) / 2;
                specials.put(ringCell(outer, c, mid), new Special(Type.ARROW, CORNER_COLORS[c], (c + 1) % 4));
            }
            specials.put(ringCell(outer, (k + 1) % 4, 12), new Special(Type.QUESTION, Q_MEDIUM, 0));
            specials.put(ringCell(outer, (k + 2) % 4, 12), new Special(Type.QUESTION, Q_HARD, 0));
            specials.put(ringCell(outer, k, 5), new Special(Type.SURPRISE, SURPRISE, 0));
            specials.put(ringCell(outer, k, 7), new Special(Type.SURPRISE, SURPRISE, 0));

            if (rings == 2) {
                List<Point> second = ringPath(1);
                Color[] q2 = {Q_EASY, Q_MEDIUM, Q_HARD};
                for (int i = 0; i < 4; i++) {
                    int c = (k + i) % 4;
                    specials.put(ringCell(second, c, 11), new Special(Type.STAR, null, 0));
                    specials.put(ringCell(second, c, (size - 3) / 2), new Special(Type.ARROW, CORNER_COLORS[c], (c + 1) % 4));
                    if (i > 0) {
                        specials.put(ringCell(second, c, 3), new Special(Type.QUESTION, q2[i - 1], 0));
                    }
                }
                specials.put(ringCell(second, (k + 2) % 4, 10), new Special(Type.SURPRISE, SURPRISE, 0));
            }

            for (int i = 0; i <= 20; i++) {
                activePath.add(ringCell(outer, k, i));
            }
        }
    }

    // ------------------------------------------------------------------
    // Active player piece control
    // ------------------------------------------------------------------

    public void highlightCastlePieces() {
        highlightCastle = true;
        highlightBoard = false;
        startHighlightPulse(true);
    }

    public void highlightBoardPiece() {
        highlightBoard = true;
        highlightCastle = false;
        startHighlightPulse(true);
    }

    public void highlightCastlePieces(boolean pulse) {
        highlightCastle = true;
        highlightBoard = false;
        startHighlightPulse(pulse);
    }

    public void highlightBoardPiece(boolean pulse) {
        highlightBoard = true;
        highlightCastle = false;
        startHighlightPulse(pulse);
    }

    public void clearHighlight() {
        highlightCastle = false;
        highlightBoard = false;
        if (highlightPulse != null) {
            highlightPulse.stop();
            highlightPulse = null;
        }
        repaint();
    }

    private void startHighlightPulse(boolean pulse) {
        if (highlightPulse != null) {
            highlightPulse.stop();
        }
        pulsePhase = false;
        if (!pulse) {
            highlightPulse = null;
            repaint();
            return;
        }
        highlightPulse = new Timer(420, e -> {
            pulsePhase = !pulsePhase;
            repaint();
        });
        highlightPulse.start();
        repaint();
    }

    public boolean placeActivePieceOnStart(int slot) {
        if (slot < 0 || slot >= 4 || !castlePieces[activeCorner][slot]) return false;
        castlePieces[activeCorner][slot] = false;
        piecesInCastle[activeCorner]--;
        piecesOnBoard[activeCorner][slot] = true;
        pieceSteps[activeCorner][slot] = 0;
        piecesInHomePath[activeCorner][slot] = false;
        selectedPieceIndex = slot;
        clearHighlight();
        repaint();
        return true;
    }

    public int getActivePieceStep() {
        return pieceSteps[activeCorner][selectedPieceIndex];
    }

    public boolean hasPieceInCastle() {
        return piecesInCastle[activeCorner] > 0;
    }

    public boolean hasPieceOnBoard() {
        for (int pieceIndex = 0; pieceIndex < 4; pieceIndex++) {
            if (piecesOnBoard[activeCorner][pieceIndex]) {
                return true;
            }
        }
        return false;
    }

    public int getActiveRouteSize() {
        return getActiveRouteSize(selectedPieceIndex);
    }

    public int getActiveRouteSize(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= 4) {
            return 0;
        }

        return piecesInHomePath[activeCorner][pieceIndex] ? activeHomePath.size() : activePath.size();
    }

    public void setActivePlayerColor(String colorName) {
        activeCorner = cornerOf(colorName);
        rebuildActivePath();
    }

    private void rebuildActivePath() {
        List<Point> inner = innerPath();
        int[] startIdx = {0, 13, 26, 39};
        activeHomePath.clear();
        for (int i = 1; i <= 5; i++) {
            switch (activeCorner) {
                case 0: activeHomePath.add(inner(i, 7)); break;
                case 1: activeHomePath.add(inner(7, i)); break;
                case 2: activeHomePath.add(inner(14 - i, 7)); break;
                default: activeHomePath.add(inner(7, 14 - i)); break;
            }
        }
        activePath.clear();
        if (rings == 0) {
            int s = startIdx[activeCorner];
            for (int i = 0; i <= 20; i++) {
                activePath.add(inner.get((s + i) % 52));
            }
        } else {
            List<Point> outer = ringPath(0);
            for (int i = 0; i <= 20; i++) {
                activePath.add(ringCell(outer, activeCorner, i));
            }
        }
        repaint();
    }

    /** Checks that the selected active-player move stays within the current route. */
    public boolean canMoveActivePiece(int steps) {
        if (!piecesOnBoard[activeCorner][selectedPieceIndex] || steps < 0) return false;
        List<Point> route = currentPieceRoute(selectedPieceIndex);
        return pieceSteps[activeCorner][selectedPieceIndex] + steps < route.size();
    }

    private List<Point> currentPieceRoute(int pieceIndex) {
        return piecesInHomePath[activeCorner][pieceIndex] ? activeHomePath : activePath;
    }

    public void animateActivePiece(
            int startStep,
            int targetStep,
            Runnable onDone) {
        animatePiece(selectedPieceIndex, startStep, targetStep, onDone);
    }

    /**
     * SRS 3.2.4.3.1 (Maor Mizrahi) -
     * Visually animates an active piece after the movement
     * has already been validated by GameController.
     *
     * This method performs presentation only and does not
     * validate or apply game rules.
     */
    public void animatePiece(
            int pieceIndex,
            int startStep,
            int targetStep,
            Runnable onDone) {
        if (pieceIndex < 0 || pieceIndex >= 4) {
            return;
        }
        clearHighlight();
        selectedPieceIndex = pieceIndex;

        if (animation != null && animation.isRunning()) {
            animation.stop();
        }

        pieceSteps[activeCorner][pieceIndex] = startStep;

        final boolean forward = targetStep >= startStep;
        final int[] remaining = {
            Math.abs(targetStep - startStep)
        };

        animation = new Timer(220, null);

        animation.addActionListener(e -> {
            if (remaining[0] == 0) {
                animation.stop();

                if (onDone != null) {
                    onDone.run();
                }

                return;
            }

            if (forward) {
                pieceSteps[activeCorner][pieceIndex]++;
            } else {
                pieceSteps[activeCorner][pieceIndex]--;
            }

            remaining[0]--;
            repaint();
        });

        animation.start();
    }

    // ------------------------------------------------------------------
    // Mouse handling
    // ------------------------------------------------------------------

    private int cellSize() {
        return Math.max(10, Math.min(getWidth(), getHeight()) / size);
    }

    private int originX() {
        return (getWidth() - cellSize() * size) / 2;
    }

    private int originY() {
        return (getHeight() - cellSize() * size) / 2;
    }

    private Point castleOrigin(int corner) {
        int c = (corner == 1 || corner == 2) ? 9 : 0;
        int r = (corner == 2 || corner == 3) ? 9 : 0;
        return inner(c, r);
    }

    private double[] slotCenter(int corner, int slot) {
        Point o = castleOrigin(corner);
        double[][] offsets = {{2, 2}, {4, 2}, {2, 4}, {4, 4}};
        int cs = cellSize();
        return new double[]{
            originX() + (o.x + offsets[slot][0]) * cs,
            originY() + (o.y + offsets[slot][1]) * cs
        };
    }

    private void handleClick(Point p) {
        if (listener == null) return;
        int cs = cellSize();
        if (highlightCastle) {
            for (int s = 0; s < 4; s++) {
                if (!castlePieces[activeCorner][s]) continue;
                double[] c = slotCenter(activeCorner, s);
                if (p.distance(c[0], c[1]) <= cs * 0.8) {
                    listener.castlePieceClicked(s);
                    return;
                }
            }
        }
        if (highlightBoard) {
            for (int pieceIndex = 0; pieceIndex < 4; pieceIndex++) {
                if (!piecesOnBoard[activeCorner][pieceIndex]) {
                    continue;
                }

                List<Point> route = currentPieceRoute(pieceIndex);
                int step = Math.min(pieceSteps[activeCorner][pieceIndex], route.size() - 1);
                Point cell = route.get(step);
                Rectangle bounds = new Rectangle(originX() + cell.x * cs, originY() + cell.y * cs, cs, cs);

                if (bounds.contains(p)) {
                    selectedPieceIndex = pieceIndex;
                    listener.boardPieceClicked(pieceIndex);
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cs = cellSize();
        int ox = originX();
        int oy = originY();

        // Soft frame around the board, matching the navy-and-lavender UI.
        g2.setColor(new Color(31, 47, 95, 18));
        g2.fillRoundRect(ox - 7, oy - 5, cs * size + 14, cs * size + 14, 18, 18);
        g2.setColor(BOARD_BG);
        g2.fillRect(ox, oy, cs * size, cs * size);

        // Outer rings
        for (int ring = 0; ring < rings; ring++) {
            for (Point p : ringPath(ring)) {
                drawCell(g2, p, Color.WHITE);
            }
        }

        // Track and home path of the basic board
        for (int c = 0; c < 15; c++) {
            for (int r = 0; r < 15; r++) {
                boolean vertical = c >= 6 && c <= 8 && (r < 6 || r > 8);
                boolean horizontal = r >= 6 && r <= 8 && (c < 6 || c > 8);
                if (vertical || horizontal) {
                    Point p = inner(c, r);
                    Color home = homeCells.get(p);
                    drawCell(g2, p, home != null ? lighter(home) : Color.WHITE);
                }
            }
        }

        // Castles
        for (int corner = 0; corner < 4; corner++) {
            drawCastle(g2, corner);
        }

        // Centre (finish)
        drawCentre(g2);

        // Special squares
        for (Map.Entry<Point, Special> e : specials.entrySet()) {
            drawSpecial(g2, e.getKey(), e.getValue());
        }

        // Pieces in castles
        for (int corner = 0; corner < 4; corner++) {
            for (int s = 0; s < 4; s++) {
                if (!castlePieces[corner][s]) continue;
                double[] c = slotCenter(corner, s);
                boolean hl = highlightCastle && corner == activeCorner;
                drawPiece(g2, c[0], c[1], cs, CORNER_COLORS[corner], hl);
            }
        }

        // Active player's moving piece
        for (int pieceIndex = 0; pieceIndex < 4; pieceIndex++) {
            if (!piecesOnBoard[activeCorner][pieceIndex]) {
                continue;
            }

            List<Point> route = currentPieceRoute(pieceIndex);
            Point cell = route.get(Math.min(pieceSteps[activeCorner][pieceIndex], route.size() - 1));
            boolean highlighted = highlightBoard;
            drawPiece(g2, ox + (cell.x + 0.5) * cs, oy + (cell.y + 0.5) * cs, cs,
                CORNER_COLORS[activeCorner], highlighted);
        }

        g2.setColor(new Color(126, 140, 173));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRect(ox, oy, cs * size, cs * size);
        g2.dispose();
    }

    // Track cells remain in the exact same positions, so click detection is unchanged.
    private void drawCell(Graphics2D g2, Point p, Color fill) {
        int cs = cellSize();
        int x = originX() + p.x * cs;
        int y = originY() + p.y * cs;
        g2.setColor(fill);
        g2.fillRect(x, y, cs, cs);
        g2.setColor(CELL_BORDER);
        g2.setStroke(new BasicStroke(0.9f));
        g2.drawRect(x, y, cs, cs);
    }

    // Pastel castles with a clean inner area; piece positions are untouched.
    private void drawCastle(Graphics2D g2, int corner) {
        int cs = cellSize();
        Point o = castleOrigin(corner);
        int x = originX() + o.x * cs;
        int y = originY() + o.y * cs;
        int side = cs * 6;
        Color color = CORNER_COLORS[corner];
        Color base = activeCorners[corner] ? lighter(color) : lighter(lighter(color));
        Color glow = activeCorners[corner] ? new Color(246, 250, 255) : new Color(250, 251, 255);

        g2.setPaint(new GradientPaint(x, y, base, x + side, y + side,
            activeCorners[corner] ? lighter(lighter(color)) : new Color(245, 248, 254)));
        g2.fillRoundRect(x + 1, y + 1, side - 2, side - 2, Math.max(12, cs), Math.max(12, cs));
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.7f));
        g2.drawRoundRect(x + 1, y + 1, side - 2, side - 2, Math.max(12, cs), Math.max(12, cs));

        g2.setColor(glow);
        g2.fillRoundRect(x + cs, y + cs, cs * 4, cs * 4, Math.max(12, cs), Math.max(12, cs));
        g2.setColor(new Color(255, 255, 255, 210));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(x + cs, y + cs, cs * 4, cs * 4, Math.max(12, cs), Math.max(12, cs));

        for (int s = 0; s < 4; s++) {
            double[] c = slotCenter(corner, s);
            double r = cs * 0.61;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
            g2.fill(new Ellipse2D.Double(c[0] - r, c[1] - r, r * 2, r * 2));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 85));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Ellipse2D.Double(c[0] - r, c[1] - r, r * 2, r * 2));
        }
    }

    private void drawCentre(Graphics2D g2) {
        int cs = cellSize();
        double x0 = originX() + (6 + rings) * cs;
        double y0 = originY() + (6 + rings) * cs;
        double x1 = x0 + 3 * cs;
        double y1 = y0 + 3 * cs;
        double cx = (x0 + x1) / 2;
        double cy = (y0 + y1) / 2;
        fillTriangle(g2, GREEN, x0, y0, x0, y1, cx, cy);
        fillTriangle(g2, YELLOW, x0, y0, x1, y0, cx, cy);
        fillTriangle(g2, BLUE, x1, y0, x1, y1, cx, cy);
        fillTriangle(g2, RED, x0, y1, x1, y1, cx, cy);

        // The finish marker stays within the middle 3x3 cells.
        int d = Math.max(18, (int) (cs * 1.04));
        int x = (int) cx - d / 2;
        int y = (int) cy - d / 2;
        g2.setColor(new Color(22, 34, 77, 50));
        g2.fillOval(x + 2, y + 3, d, d);
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y, d, d);
        g2.setColor(new Color(35, 53, 105));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawOval(x, y, d, d);
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(9, (int) (cs * 0.29))));
        FontMetrics fm = g2.getFontMetrics();
        String goal = "GOAL";
        g2.drawString(goal, (int) cx - fm.stringWidth(goal)/2,
            (int) cy + (fm.getAscent()-fm.getDescent())/2);
    }

    private void fillTriangle(Graphics2D g2, Color c, double ax, double ay,
                              double bx, double by, double cx, double cy) {
        Path2D t = new Path2D.Double();
        t.moveTo(ax, ay);
        t.lineTo(bx, by);
        t.lineTo(cx, cy);
        t.closePath();
        g2.setColor(c);
        g2.fill(t);
        g2.setColor(Color.WHITE);
        g2.draw(t);
    }

    /**
     * SRS 3.2.2.3.4 (Maor Mizrahi) -
     * Draws a distinct visual marking and color for each special
     * square type, including start, protection, arrow, question
     * and surprise squares.
     */
    private void drawSpecial(Graphics2D g2, Point p, Special s) {
        int cs = cellSize();
        int x = originX() + p.x * cs;
        int y = originY() + p.y * cs;
        int pad = Math.max(2, (int) (cs * 0.09));
        int tile = cs - pad * 2;

        switch (s.type) {
            case START:
                drawCell(g2, p, lighter(lighter(s.color)));
                g2.setColor(s.color);
                g2.fillRoundRect(x + pad, y + pad, tile, tile, Math.max(6, cs / 4), Math.max(6, cs / 4));
                drawCenteredText(g2, "#", x, y, cs, Color.WHITE, 0.68);
                break;
            case STAR:
                drawCell(g2, p, new Color(255, 241, 196));
                g2.setColor(new Color(255, 255, 255, 170));
                g2.fillOval(x + pad, y + pad, tile, tile);
                drawStar(g2, x + cs / 2.0, y + cs / 2.0, cs * 0.39);
                break;
            case ARROW:
                drawCell(g2, p, lighter(lighter(s.color)));
                g2.setColor(new Color(255, 255, 255, 150));
                g2.fillRoundRect(x + pad, y + pad, tile, tile, Math.max(6, cs / 4), Math.max(6, cs / 4));
                drawArrow(g2, x, y, cs, s.dir, s.color.darker());
                break;
            case QUESTION:
                Color questionFill = s.color.equals(Q_EASY) ? new Color(48, 167, 111)
                    : (s.color.equals(Q_MEDIUM) ? new Color(220, 157, 32) : new Color(221, 69, 90));
                drawCell(g2, p, lighter(lighter(s.color)));
                g2.setColor(questionFill);
                g2.fillRoundRect(x + pad, y + pad, tile, tile, Math.max(6, cs / 4), Math.max(6, cs / 4));
                drawCenteredText(g2, "?", x, y, cs, Color.WHITE, 0.72);
                break;
            case SURPRISE:
                drawCell(g2, p, new Color(234, 223, 255));
                g2.setColor(new Color(134, 76, 211));
                g2.fillRoundRect(x + pad, y + pad, tile, tile, Math.max(6, cs / 4), Math.max(6, cs / 4));
                drawCenteredText(g2, "!", x, y, cs, Color.WHITE, 0.72);
                break;
            default:
                break;
        }
    }

    private void drawCenteredText(Graphics2D g2, String text, int x, int y,
                                  int cs, Color color, double scale) {
        int fontSize = Math.max(12, (int) (cs * scale));
        g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(color);
        g2.drawString(text, x + (cs - fm.stringWidth(text)) / 2,
            y + (cs + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void drawStar(Graphics2D g2, double cx, double cy, double r) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double radius = (i % 2 == 0) ? r : r * 0.46;
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(angle) * radius;
            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.closePath();
        g2.setColor(new Color(251, 181, 37));
        g2.fill(star);
        g2.setColor(new Color(158, 107, 19));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(star);
    }

    private void drawArrow(Graphics2D g2, int x, int y, int cs, int dir, Color color) {
        Graphics2D a = (Graphics2D) g2.create();
        a.translate(x + cs / 2.0, y + cs / 2.0);
        a.rotate(dir * Math.PI / 2);
        double l = cs * 0.37;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(-l, -l * 0.27);
        arrow.lineTo(0, -l * 0.27);
        arrow.lineTo(0, -l * 0.69);
        arrow.lineTo(l, 0);
        arrow.lineTo(0, l * 0.69);
        arrow.lineTo(0, l * 0.27);
        arrow.lineTo(-l, l * 0.27);
        arrow.closePath();
        a.setColor(color);
        a.fill(arrow);
        a.dispose();
    }

    private void drawPiece(Graphics2D g2, double cx, double cy, int cs, Color color, boolean highlighted) {
        double r = cs * 0.42;
        if (highlighted) {
            int glowAlpha = pulsePhase ? 180 : 75;
            double glowSize = pulsePhase ? 0.28 : 0.21;
            g2.setColor(new Color(255, 178, 67, glowAlpha));
            g2.fill(new Ellipse2D.Double(cx - r - cs * glowSize, cy - r - cs * glowSize,
                2 * (r + cs * glowSize), 2 * (r + cs * glowSize)));
            g2.setColor(HIGHLIGHT);
            g2.setStroke(new BasicStroke(Math.max(2f, cs * 0.10f)));
            g2.draw(new Ellipse2D.Double(cx - r - cs * 0.12, cy - r - cs * 0.12,
                2 * (r + cs * 0.12), 2 * (r + cs * 0.12)));
        }
        g2.setColor(new Color(25, 32, 69, 55));
        g2.fill(new Ellipse2D.Double(cx - r + cs * 0.055, cy - r + cs * 0.10, r * 2, r * 2));
        g2.setPaint(new GradientPaint((float) (cx - r), (float) (cy - r), lighter(color),
            (float) (cx + r), (float) (cy + r), color));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g2.setColor(new Color(255, 255, 255, 185));
        g2.fill(new Ellipse2D.Double(cx - r * 0.53, cy - r * 0.62, r * 0.76, r * 0.76));
    }

    private static Color lighter(Color c) {
        return new Color(
            (c.getRed() + 255) / 2,
            (c.getGreen() + 255) / 2,
            (c.getBlue() + 255) / 2);
    }

    public String getDifficulty() {
        return difficulty;
    }
}
