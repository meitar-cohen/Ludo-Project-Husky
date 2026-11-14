package Tests;

import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Controller.GameController;
import Enums.SquareType;
import View.BoardPanel;

/**
 * SRS 3.2.4.3.2 - the arrows of the Medium and Hard boards, checked against the sample
 * boards of the project document (page 2):
 * - the arrow of a colour is on the ring, 7 squares before the colour's corner, and points inward;
 * - the square an arrow points to is the first square of the next path;
 * - Hard has a "staircase" of three arrows of the same colour (outer, middle, inner);
 * - every path is continuous, so a piece never jumps to another part of the board.
 * Cells are (column, row) in the board grid (17x17 for Medium, 19x19 for Hard).
 */
public class BoardArrowGeometryCheck {

    private static final String[] COLORS = {"Green", "Yellow", "Blue", "Red"};
    // directions of the arrows: 0 right, 1 down, 2 left, 3 up (one per corner)
    private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    // expected arrow cells per corner (top-left, top-right, bottom-right, bottom-left)
    private static final int[][] MEDIUM_OUTER = {{0, 7}, {9, 0}, {16, 9}, {7, 16}};
    private static final int[][] MEDIUM_INNER = {{1, 8}, {8, 1}, {15, 8}, {8, 15}};
    private static final int[][] HARD_OUTER = {{0, 7}, {11, 0}, {18, 11}, {7, 18}};
    private static final int[][] HARD_MIDDLE = {{1, 8}, {10, 1}, {17, 10}, {8, 17}};
    private static final int[][] HARD_INNER = {{2, 9}, {9, 2}, {16, 9}, {9, 16}};

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static Object field(Object source, String name) throws Exception {
        Field f = source.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(source);
    }

    @SuppressWarnings("unchecked")
    private static List<Point> route(BoardPanel board, String method, int corner) throws Exception {
        Method m = BoardPanel.class.getDeclaredMethod(method, int.class);
        m.setAccessible(true);
        return (List<Point>) m.invoke(board, corner);
    }

    private static int distance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private static Point pointedAt(Point arrow, int dir) {
        return new Point(arrow.x + DIRS[dir][0], arrow.y + DIRS[dir][1]);
    }

    private static void checkArrow(GameController squares, Point cell, int[] expected, int corner,
                                   String what) {
        check(cell.x == expected[0] && cell.y == expected[1],
                what + ": arrow cell (" + cell.x + "," + cell.y + ") expected ("
                        + expected[0] + "," + expected[1] + ")");
        check(squares.getSpecialSquareType(cell) == SquareType.ARROW,
                what + ": the square is not an arrow");
        check(BoardPanel.colorOf(COLORS[corner]).equals(squares.getSpecialSquareColor(cell)),
                what + ": arrow colour");
        check(squares.getSpecialSquareDirection(cell) == corner,
                what + ": arrow direction " + squares.getSpecialSquareDirection(cell));
    }

    /**
     * Consecutive squares must touch. The 52-square track of the basic board turns diagonally
     * around the four corners of the centre, so the inner path allows a diagonal step there.
     */
    private static boolean touching(Point a, Point b, boolean allowDiagonal) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        return allowDiagonal ? Math.max(dx, dy) == 1 : dx + dy == 1;
    }

    private static void checkContinuous(List<Point> cells, int count, String what,
                                        boolean allowDiagonal) {
        for (int i = 1; i < count; i++) {
            check(touching(cells.get(i - 1), cells.get(i), allowDiagonal),
                    what + ": jump between step " + (i - 1) + " and " + i);
        }
    }

    private static void checkLevel(String level) throws Exception {
        boolean hard = "Hard".equals(level);
        BoardPanel board = new BoardPanel(TestPlayers.gameControllerForBoard(level, COLORS));
        GameController squares = (GameController) field(board, "turns");
        int outerArrowStep = hard ? 65 : 57;
        int[][] outerExpected = hard ? HARD_OUTER : MEDIUM_OUTER;
        int[][] innerExpected = hard ? HARD_INNER : MEDIUM_INNER;

        for (int corner = 0; corner < 4; corner++) {
            String what = level + " " + COLORS[corner];
            board.setActivePlayerColor(COLORS[corner]);
            @SuppressWarnings("unchecked")
            List<Point> outer = (List<Point>) field(board, "activePath");
            @SuppressWarnings("unchecked")
            List<Point> homePath = (List<Point>) field(board, "activeHomePath");
            List<Point> middle = route(board, "middleRouteForCorner", corner);
            List<Point> inner = route(board, "innerRouteForCorner", corner);

            // start square on the corner of the ring
            check(squares.getSpecialSquareType(outer.get(0)) == SquareType.START
                    && BoardPanel.colorOf(COLORS[corner]).equals(
                            squares.getSpecialSquareColor(outer.get(0))),
                    what + ": the route must start on the colour's start square");

            // outer arrow: position, direction, and the square it points to
            checkArrow(squares, outer.get(outerArrowStep), outerExpected[corner], corner,
                    what + " outer arrow");
            checkContinuous(outer, outerArrowStep + 1, what + " outer path", false);
            Point next = hard ? middle.get(0) : inner.get(0);
            check(pointedAt(outer.get(outerArrowStep), corner).equals(next),
                    what + ": the outer arrow must point to the first square of the next path");

            if (hard) {
                // middle path: 64 squares, ends on the middle arrow, which points to the inner path
                check(middle.size() == 64 && new HashSet<>(middle).size() == 64,
                        what + ": middle path must have 64 different squares");
                checkContinuous(middle, middle.size(), what + " middle path", false);
                checkArrow(squares, middle.get(63), HARD_MIDDLE[corner], corner,
                        what + " middle arrow");
                check(pointedAt(middle.get(63), corner).equals(inner.get(0)),
                        what + ": the middle arrow must point to the first square of the inner path");
                // the staircase: the arrows of the three paths are on consecutive rows or columns
                Point o = outer.get(outerArrowStep);
                Point m = middle.get(63);
                Point in = inner.get(51);
                check(distance(o, m) == 2 && distance(m, in) == 2,
                        what + ": the three arrows must form a staircase");
            }

            // inner path: 52 squares, one full circuit, ends on the inner arrow next to the home path
            check(inner.size() == 52 && new HashSet<>(inner).size() == 52,
                    what + ": inner path must have 52 different squares");
            checkContinuous(inner, inner.size(), what + " inner path", true);
            check(touching(inner.get(51), inner.get(0), true),
                    what + ": the inner path must be a closed circuit");
            checkArrow(squares, inner.get(51), innerExpected[corner], corner, what + " inner arrow");
            check(pointedAt(inner.get(51), corner).equals(homePath.get(0)),
                    what + ": the inner arrow must point to the first square of the home path");

            // the colour has exactly one arrow on each ring it uses
            Set<Point> own = new HashSet<>();
            for (Point position : squares.getSpecialSquarePositions()) {
                if (squares.getSpecialSquareType(position) == SquareType.ARROW
                        && BoardPanel.colorOf(COLORS[corner]).equals(
                                squares.getSpecialSquareColor(position))) {
                    own.add(position);
                }
            }
            check(own.size() == (hard ? 3 : 2), what + ": arrows of the colour: " + own.size());
        }
        System.out.println("PASS - " + level + " arrows: position, direction and continuous paths");
    }

    public static void main(String[] args) throws Exception {
        checkLevel("Medium");
        checkLevel("Hard");
        System.out.println("All board arrow geometry checks passed.");
    }
}
