package tests;
import View.BoardPanel;

public class HomeInnerRouteAcceptanceCheck {

    public static void main(String[] args) {

        String[] levels = {"Easy", "Medium", "Hard"};
        String[] colors = {"Green", "Yellow", "Blue", "Red"};

        for (String level : levels) {

            for (String color : colors) {

                BoardPanel board = new BoardPanel(level, colors);
                board.setActivePlayerColor(color);

                // הוצאת כלי מהטירה אל הלוח
                check(board.placeActivePieceOnStart(0),
                        level + " - castle exit");

                // בדיקת אורך המסלול הראשוני
                int expectedRoute = "Easy".equals(level) ? 52
                        : "Medium".equals(level) ? 64 : 72;

                check(board.getActiveRouteSize(0) == expectedRoute,
                        level + " - initial route");

                // ברמות Medium ו-Hard מעבירים למסלול הפנימי
                if (!"Easy".equals(level)) {

                    check(board.showActivePieceInInnerPath(0, 50),
                            level + " - inner path entry");

                    check(board.isActivePieceInInnerPath(0),
                            level + " - inner path state");

                    check(board.getActiveRouteSize(0) == 52,
                            level + " - inner path size");
                }

                // מעבר ממסלול משותף למסלול הבית
                check(board.showActivePieceInHomePath(0, 1),
                        level + " - home entry");

                check(board.isActivePieceInHomePath(0),
                        level + " - home path state");

                check(board.getActiveRouteSize(0) == 6,
                        level + " - home path size");

                check(board.getActivePieceStep() == 1,
                        level + " - home position");
            }

            System.out.println("PASS - " + level
                    + " inner and home path checks");
        }

        System.out.println("PASS - all tests");
    }

    private static void check(boolean condition, String message) {

        if (!condition) {
            throw new AssertionError("FAIL - " + message);
        }
    }
}