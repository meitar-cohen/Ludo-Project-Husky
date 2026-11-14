package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import Controller.GameController;
import Enums.SquareType;

/**
 * Board component for the game screen.
 * Draws the board, special squares, castles, and pieces.
 */
public class BoardPanel extends JPanel {

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

    private final int rings;
    private final int size;
    private int activeCorner;
    private final boolean[] activeCorners = new boolean[4];
    private final int[] piecesInCastle = new int[4];
    private final boolean[][] castlePieces = new boolean[4][4];

    private final List<Point> activePath = new ArrayList<>();
    private final List<Point> activeHomePath = new ArrayList<>();

    private final List<List<Point>> sharedRoutes = new ArrayList<>();
    private final Map<Point, GameController.SpecialSquareInfo> squares =
            new HashMap<>();
    private final Map<Point, Color> homeCells = new HashMap<>();

    /**
     * SRS 3.2.4.3.1 -
     */
    private final boolean[][] piecesOnBoard = new boolean[4][4];
    private final int[][] pieceSteps = new int[4][4];
    private final boolean[][] piecesInHomePath = new boolean[4][4];
    private final boolean[][] finishedPieces = new boolean[4][4];
    private final boolean[][] piecesInInnerPath = new boolean[4][4];
    // SRS 3.2.4.3.2 - Hard level: piece on the middle path (64 squares)
    private final boolean[][] piecesInMiddlePath = new boolean[4][4];
    private int selectedPieceIndex;

    private boolean highlightCastle = false;
    private boolean highlightBoard = false;

    private IntConsumer castlePieceClickListener;
    private IntConsumer boardPieceClickListener;
    private Timer animation;
    private Timer highlightPulse;
    private boolean pulsePhase;
    private final PiecePanel piecePanel = new PiecePanel();
    private final BoardDrawingPanel drawingPanel = new BoardDrawingPanel();

    public BoardPanel(String difficulty, String[] playerColorNames) {
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
        randomizeSquares();
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

    public void setCastlePieceClickListener(IntConsumer listener) {
        this.castlePieceClickListener = listener;
    }

    public void setBoardPieceClickListener(IntConsumer listener) {
        this.boardPieceClickListener = listener;
    }

    // ------------------------------------------------------------------
    // ------------------------------------------------------------------

    Point inner(int c, int r) {
        return new Point(c + rings, r + rings);
    }

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

    List<Point> ringPath(int ring) {
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
        sharedRoutes.clear();
        sharedRoutes.add(inner);
        if (rings >= 1) {
            sharedRoutes.add(ringPath(rings == 1 ? 0 : 1));
        }
        if (rings == 2) {
            sharedRoutes.add(ringPath(0));
        }
        int[] startIdx = {0, 13, 26, 39};
        for (int i = 1; i <= 5; i++) {
            homeCells.put(inner(i, 7), GREEN);
            homeCells.put(inner(7, i), YELLOW);
            homeCells.put(inner(14 - i, 7), BLUE);
            homeCells.put(inner(7, 14 - i), RED);
        }
        for (int c = 0; c < 4; c++) {
            Point star = inner.get((startIdx[c] + 8) % 52);
            squares.put(star, GameController.createSpecialSquare(SquareType.STAR, null, 0));
            Point arrow = inner.get((startIdx[c] + 50) % 52);
            squares.put(arrow, GameController.createSpecialSquare(SquareType.ARROW, CORNER_COLORS[c], c));
            if (rings == 0) {
                squares.put(inner.get(startIdx[c]), GameController.createSpecialSquare(SquareType.START, CORNER_COLORS[c], 0));
            }
        }
        for (int i = 1; i <= 6; i++) {
            switch (activeCorner) {
                case 0: activeHomePath.add(inner(i, 7)); break;
                case 1: activeHomePath.add(inner(7, i)); break;
                case 2: activeHomePath.add(inner(14 - i, 7)); break;
                default: activeHomePath.add(inner(7, 14 - i)); break;
            }
        }
        if (rings == 0) {
            int s = startIdx[activeCorner];
            for (int i = 0; i < inner.size(); i++) {
                activePath.add(inner.get((s + i) % inner.size()));
            }
        }
        if (rings > 0) {
            List<Point> outer = ringPath(0);
            int k = activeCorner;
            Color[] qLevels = {Q_EASY, Q_MEDIUM, Q_HARD, Q_EASY};
            for (int i = 0; i < 4; i++) {
                int c = (k + i) % 4;
                squares.put(ringCell(outer, c, 0), GameController.createSpecialSquare(SquareType.START, CORNER_COLORS[c], 0));
                squares.put(ringCell(outer, c, 3), GameController.createSpecialSquare(SquareType.QUESTION, qLevels[i], 0));
                squares.put(ringCell(outer, c, 11), GameController.createSpecialSquare(SquareType.STAR, null, 0));
                int mid = (size - 1) / 2;
                squares.put(ringCell(outer, c, mid), GameController.createSpecialSquare(SquareType.ARROW, CORNER_COLORS[c], (c + 1) % 4));
            }
            squares.put(ringCell(outer, (k + 1) % 4, 12), GameController.createSpecialSquare(SquareType.QUESTION, Q_MEDIUM, 0));
            squares.put(ringCell(outer, (k + 2) % 4, 12), GameController.createSpecialSquare(SquareType.QUESTION, Q_HARD, 0));
            squares.put(ringCell(outer, k, 5), GameController.createSpecialSquare(SquareType.SURPRISE, SURPRISE, 0));
            squares.put(ringCell(outer, k, 7), GameController.createSpecialSquare(SquareType.SURPRISE, SURPRISE, 0));
            if (rings == 2) {
                List<Point> second = ringPath(1);
                Color[] q2 = {Q_EASY, Q_MEDIUM, Q_HARD};
                for (int i = 0; i < 4; i++) {
                    int c = (k + i) % 4;
                    squares.put(ringCell(second, c, 11), GameController.createSpecialSquare(SquareType.STAR, null, 0));
                    squares.put(ringCell(second, c, (size - 3) / 2), GameController.createSpecialSquare(SquareType.ARROW, CORNER_COLORS[c], (c + 1) % 4));
                    if (i > 0) {
                        squares.put(ringCell(second, c, 3), GameController.createSpecialSquare(SquareType.QUESTION, q2[i - 1], 0));
                    }
                }
                squares.put(ringCell(second, (k + 2) % 4, 10), GameController.createSpecialSquare(SquareType.SURPRISE, SURPRISE, 0));
            }
            for (int i = 0; i < outer.size(); i++) {
                activePath.add(ringCell(outer, k, i));
            }
        }
    }

    private void randomizeSquares() {
        squares.entrySet().removeIf(entry ->
                entry.getValue().getType() == SquareType.STAR
                || entry.getValue().getType() == SquareType.QUESTION
                || entry.getValue().getType() == SquareType.SURPRISE);
        placeRandomSpecialsOnRoute(sharedRoutes.get(0), 4, 0, 0, 0, 0);
        if (rings == 1) {
            placeRandomSpecialsOnRoute(sharedRoutes.get(1), 4, 2, 2, 2, 2);
        }
        if (rings == 2) {
            placeRandomSpecialsOnRoute(sharedRoutes.get(1), 4, 1, 1, 1, 1);
            placeRandomSpecialsOnRoute(sharedRoutes.get(2), 4, 2, 2, 2, 2);
        }
    }

    private void placeRandomSpecialsOnRoute(List<Point> route,
            int stars, int easyQuestions, int mediumQuestions,
            int hardQuestions, int surprises) {
        List<Point> available = new ArrayList<>();
        for (Point cell : route) {
            if (!squares.containsKey(cell) && !homeCells.containsKey(cell)) {
                available.add(cell);
            }
        }
        int needed = stars + easyQuestions + mediumQuestions
                + hardQuestions + surprises;
        if (available.size() < needed) {
            throw new IllegalStateException(
                    "Not enough free squares for special cells");
        }
        java.util.Collections.shuffle(available);
        int index = 0;
        for (int i = 0; i < stars; i++) {
            squares.put(available.get(index++),
                    GameController.createSpecialSquare(SquareType.STAR, null, 0));
        }
        for (int i = 0; i < easyQuestions; i++) {
            squares.put(available.get(index++),
                    GameController.createSpecialSquare(SquareType.QUESTION, Q_EASY, 0));
        }
        for (int i = 0; i < mediumQuestions; i++) {
            squares.put(available.get(index++),
                    GameController.createSpecialSquare(SquareType.QUESTION, Q_MEDIUM, 0));
        }
        for (int i = 0; i < hardQuestions; i++) {
            squares.put(available.get(index++),
                    GameController.createSpecialSquare(SquareType.QUESTION, Q_HARD, 0));
        }
        for (int i = 0; i < surprises; i++) {
            squares.put(available.get(index++),
                    GameController.createSpecialSquare(SquareType.SURPRISE, SURPRISE, 0));
        }
    }

    // ------------------------------------------------------------------
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
        piecesInInnerPath[activeCorner][slot] = false;
        piecesInMiddlePath[activeCorner][slot] = false;
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

    public boolean showActivePieceInHomePath(int pieceIndex) {
        return showActivePieceInHomePath(pieceIndex, 0);
    }

    public boolean showActivePieceInHomePath(int pieceIndex, int homeStep) {
        if (pieceIndex < 0 || pieceIndex >= 4
                || !piecesOnBoard[activeCorner][pieceIndex]
                || homeStep < 0
                || homeStep >= activeHomePath.size()) {
            return false;
        }
        piecesInHomePath[activeCorner][pieceIndex] = true;
        pieceSteps[activeCorner][pieceIndex] = homeStep;
        selectedPieceIndex = pieceIndex;
        repaint();
        return true;
    }

    public boolean showActivePieceInInnerPath(int pieceIndex, int innerStep) {
        if (rings == 0
                || pieceIndex < 0
                || pieceIndex >= 4
                || !piecesOnBoard[activeCorner][pieceIndex]
                || piecesInHomePath[activeCorner][pieceIndex]
                || innerStep < 0
                || innerStep >= innerPath().size()) {
            return false;
        }
        piecesInInnerPath[activeCorner][pieceIndex] = true;
        piecesInMiddlePath[activeCorner][pieceIndex] = false;
        pieceSteps[activeCorner][pieceIndex] = innerStep;
        selectedPieceIndex = pieceIndex;
        repaint();
        return true;
    }

    /** SRS 3.2.4.3.2 - Hard level: shows a piece on the middle path (64 squares). */
    public boolean showActivePieceInMiddlePath(int pieceIndex, int middleStep) {
        if (rings != 2
                || pieceIndex < 0
                || pieceIndex >= 4
                || !piecesOnBoard[activeCorner][pieceIndex]
                || piecesInHomePath[activeCorner][pieceIndex]
                || piecesInInnerPath[activeCorner][pieceIndex]
                || middleStep < 0
                || middleStep >= middleRouteForCorner(activeCorner).size()) {
            return false;
        }
        piecesInMiddlePath[activeCorner][pieceIndex] = true;
        pieceSteps[activeCorner][pieceIndex] = middleStep;
        selectedPieceIndex = pieceIndex;
        repaint();
        return true;
    }

    public boolean isActivePieceInMiddlePath(int pieceIndex) {
        return pieceIndex >= 0
                && pieceIndex < 4
                && piecesInMiddlePath[activeCorner][pieceIndex];
    }

    /**
     * SRS 3.2.4.3.2 - the step of the piece's own-colour arrow on its current path,
     * or -1 when there is none (home path, inner path or Easy level).
     */
    public int getOwnArrowStep(int pieceIndex) {
        if (rings == 0
                || pieceIndex < 0 || pieceIndex >= 4
                || !piecesOnBoard[activeCorner][pieceIndex]
                || piecesInHomePath[activeCorner][pieceIndex]
                || piecesInInnerPath[activeCorner][pieceIndex]) {
            return -1;
        }
        List<Point> route = currentPieceRoute(pieceIndex);
        Color own = CORNER_COLORS[activeCorner];
        for (int step = 0; step < route.size(); step++) {
            GameController.SpecialSquareInfo special =
                    squares.get(route.get(step));
            if (special != null && special.getType() == SquareType.ARROW
                    && own.equals(special.getColor())) {
                return step;
            }
        }
        return -1;
    }

    public boolean showActivePieceFinished(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= 4
                || !piecesOnBoard[activeCorner][pieceIndex]
                || finishedPieces[activeCorner][pieceIndex]) {
            return false;
        }
        finishedPieces[activeCorner][pieceIndex] = true;
        piecesOnBoard[activeCorner][pieceIndex] = false;
        selectedPieceIndex = pieceIndex;
        clearHighlight();
        repaint();
        return true;
    }

    public int getActiveFinishedPieceCount() {
        int count = 0;
        for (int piece = 0; piece < 4; piece++) {
            if (finishedPieces[activeCorner][piece]) {
                count++;
            }
        }
        return count;
    }

    public boolean isActivePieceFinished(int pieceIndex) {
        return pieceIndex >= 0
                && pieceIndex < 4
                && finishedPieces[activeCorner][pieceIndex];
    }

    public boolean isActivePieceInInnerPath(int pieceIndex) {
        return pieceIndex >= 0
                && pieceIndex < 4
                && piecesInInnerPath[activeCorner][pieceIndex];
    }

    public boolean isActivePieceInHomePath(int pieceIndex) {
        return pieceIndex >= 0 && pieceIndex < 4
                && piecesInHomePath[activeCorner][pieceIndex];
    }
    public int getActiveRouteSize() {
        return getActiveRouteSize(selectedPieceIndex);
    }

    public int getActiveRouteSize(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= 4) {
            return 0;
        }
        return currentPieceRoute(pieceIndex).size();    }
    public String getSquareEventAtStep(int pieceIndex, int step) {
        if (pieceIndex < 0 || pieceIndex >= 4
                || !piecesOnBoard[activeCorner][pieceIndex]
                || piecesInHomePath[activeCorner][pieceIndex]) {
            return "NONE";
        }
        List<Point> route = currentPieceRoute(pieceIndex);
        if (step < 0 || step >= route.size()) {
            return "NONE";
        }
        GameController.SpecialSquareInfo special =
                squares.get(route.get(step));
        if (special == null) {
            return "NONE";
        }
        if (special.getType() == SquareType.QUESTION) {
            return "QUESTION";
        }
        if (special.getType() == SquareType.SURPRISE) {
            return "SURPRISE";
        }
        return "NONE";
    }

    /**
     */
    public Color getArrowColorAtStep(int pieceIndex, int step) {
        if (pieceIndex < 0 || pieceIndex >= 4
                || !piecesOnBoard[activeCorner][pieceIndex]
                || piecesInHomePath[activeCorner][pieceIndex]) {
            return null;
        }
        List<Point> route = currentPieceRoute(pieceIndex);
        if (step < 0 || step >= route.size()) {
            return null;
        }
        GameController.SpecialSquareInfo special =
                squares.get(route.get(step));
        return special != null && special.getType() == SquareType.ARROW
                ? special.getColor() : null;
    }

    public int getActiveHomePathSize() {
        return activeHomePath.size();
    }

    public int getSharedRouteCount() {
        return sharedRoutes.size();
    }

    public int getSharedRouteSize(int routeIndex) {
        return sharedRoutes.get(routeIndex).size();
    }
    public void setActivePlayerColor(String colorName) {
        activeCorner = cornerOf(colorName);
        rebuildActivePath();
    }

    private void rebuildActivePath() {
        List<Point> inner = innerPath();
        int[] startIdx = {0, 13, 26, 39};
        activeHomePath.clear();
        for (int i = 1; i <= 6; i++) {
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
            for (int i = 0; i < inner.size(); i++) {
                activePath.add(inner.get((s + i) % inner.size()));
            }
        } else {
            List<Point> outer = ringPath(0);
            for (int i = 0; i < outer.size(); i++) {
                activePath.add(ringCell(outer, activeCorner, i));
            }
        }
        repaint();
    }

    private List<Point> innerRouteForCorner(int corner) {
        List<Point> base = innerPath();
        List<Point> result = new ArrayList<>();
        int first = corner * 13;
        for (int i = 0; i < base.size(); i++) {
            result.add(base.get((first + i) % base.size()));
        }
        return result;
    }

    /** SRS 3.2.4.3.2 - Hard level: the middle ring (64 squares), from the player's corner. */
    private List<Point> middleRouteForCorner(int corner) {
        List<Point> middle = ringPath(1);
        List<Point> result = new ArrayList<>();
        for (int i = 0; i < middle.size(); i++) {
            result.add(ringCell(middle, corner, i));
        }
        return result;
    }

    private List<Point> currentPieceRoute(int pieceIndex) {
        if (piecesInHomePath[activeCorner][pieceIndex]) {
            return activeHomePath;
        }
        if (piecesInInnerPath[activeCorner][pieceIndex]) {
            return innerRouteForCorner(activeCorner);
        }
        if (piecesInMiddlePath[activeCorner][pieceIndex]) {
            return middleRouteForCorner(activeCorner);
        }
        return activePath;
    }

    /**
     * SRS 3.2.4.3.1 -
     *
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

    int cellSize() {
        return Math.max(10, Math.min(getWidth(), getHeight()) / size);
    }

    int originX() {
        return (getWidth() - cellSize() * size) / 2;
    }

    int originY() {
        return (getHeight() - cellSize() * size) / 2;
    }

    Point castleOrigin(int corner) {
        int c = (corner == 1 || corner == 2) ? 9 : 0;
        int r = (corner == 2 || corner == 3) ? 9 : 0;
        return inner(c, r);
    }

    double[] slotCenter(int corner, int slot) {
        Point o = castleOrigin(corner);
        double[][] offsets = {{2, 2}, {4, 2}, {2, 4}, {4, 4}};
        int cs = cellSize();
        return new double[]{
            originX() + (o.x + offsets[slot][0]) * cs,
            originY() + (o.y + offsets[slot][1]) * cs
        };
    }

    private void handleClick(Point p) {
        if (castlePieceClickListener == null
                && boardPieceClickListener == null) {
            return;
        }
        int cs = cellSize();
        if (highlightCastle) {
            for (int s = 0; s < 4; s++) {
                if (!castlePieces[activeCorner][s]) continue;
                double[] c = slotCenter(activeCorner, s);
                if (p.distance(c[0], c[1]) <= cs * 0.8) {
                    if (castlePieceClickListener != null) {
                        castlePieceClickListener.accept(s);
                    }
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
                    if (boardPieceClickListener != null) {
                        boardPieceClickListener.accept(pieceIndex);
                    }
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    private List<Point> routeForPlayerPiece(int corner, int pieceIndex) {
        if (corner == activeCorner) {
            return currentPieceRoute(pieceIndex);
        }
        List<Point> route = new ArrayList<>();
        if (piecesInHomePath[corner][pieceIndex]) {
            for (int i = 1; i <= 6; i++) {
                switch (corner) {
                    case 0: route.add(inner(i, 7)); break;
                    case 1: route.add(inner(7, i)); break;
                    case 2: route.add(inner(14 - i, 7)); break;
                    default: route.add(inner(7, 14 - i)); break;
                }
            }
        }
        else if (piecesInInnerPath[corner][pieceIndex]) {
            route.addAll(innerRouteForCorner(corner));
        }
        else if (piecesInMiddlePath[corner][pieceIndex]) {
            route.addAll(middleRouteForCorner(corner));
        }
        else if (rings == 0) {
            List<Point> innerRoute = innerPath();
            int[] startIdx = {0, 13, 26, 39};
            for (int i = 0; i < innerRoute.size(); i++) {
                route.add(innerRoute.get(
                    (startIdx[corner] + i) % innerRoute.size()
                ));
            }
        }
        else {
            List<Point> outerRoute = ringPath(0);
            for (int i = 0; i < outerRoute.size(); i++) {
                route.add(ringCell(outerRoute, corner, i));
            }
        }
        return route;
    }

    int getRings() {
        return rings;
    }

    int getBoardSize() {
        return size;
    }

    boolean isActiveCorner(int corner) {
        return activeCorners[corner];
    }

    Color homeCellColor(Point point) {
        return homeCells.get(point);
    }

    Iterable<Map.Entry<Point, GameController.SpecialSquareInfo>>
            squareEntries() {
        return squares.entrySet();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int cs = cellSize();
        int ox = originX();
        int oy = originY();
        drawingPanel.drawBoard(g2, this);
        piecePanel.drawFinishedPieceCounters(
            g2, cs, ox, oy, finishedPieceCounts(), CORNER_COLORS
        );
        // Pieces in castles
        for (int corner = 0; corner < 4; corner++) {
            for (int s = 0; s < 4; s++) {
                if (!castlePieces[corner][s]) continue;
                double[] c = slotCenter(corner, s);
                boolean hl = highlightCastle && corner == activeCorner;
                piecePanel.drawPiece(
                    g2, c[0], c[1], cs, CORNER_COLORS[corner], hl, pulsePhase
                );
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            if (!activeCorners[corner]) {
                continue;
            }
            for (int pieceIndex = 0; pieceIndex < 4; pieceIndex++) {
                if (!piecesOnBoard[corner][pieceIndex]) {
                    continue;
                }
                List<Point> route = routeForPlayerPiece(
                    corner, pieceIndex
                );
                if (route.isEmpty()) {
                    continue;
                }
                int step = Math.max(0,
                    Math.min(pieceSteps[corner][pieceIndex],
                             route.size() - 1));
                Point cell = route.get(step);
                boolean highlighted =
                    highlightBoard && corner == activeCorner;
                piecePanel.drawPiece(
                    g2,
                    ox + (cell.x + 0.5) * cs,
                    oy + (cell.y + 0.5) * cs,
                    cs,
                    CORNER_COLORS[corner],
                    highlighted,
                    pulsePhase
                );
            }
        }
        g2.dispose();
    }

    private int[] finishedPieceCounts() {
        int[] counts = new int[4];
        for (int corner = 0; corner < 4; corner++) {
            for (int piece = 0; piece < 4; piece++) {
                if (finishedPieces[corner][piece]) {
                    counts[corner]++;
                }
            }
        }
        return counts;
    }

}
