package tests;
import View.BoardPanel;

public class HomePathPositionCheck {

    public static void main(String[] args) {

        String[] levels = {"Easy", "Medium", "Hard"};
        String[] colors = {"Green", "Yellow", "Blue", "Red"};

        for (String level : levels) {

            BoardPanel board = new BoardPanel(level, colors);

            for (String color : colors) {

                board.setActivePlayerColor(color);

                // הוצאת כלי ללוח לצורך הבדיקה
                if (!board.placeActivePieceOnStart(0)) {
                    throw new AssertionError(
                        "Start failed: " + level + "/" + color
                    );
                }

                // מעבר למסלול הבית במשבצת מספר 2
                if (!board.showActivePieceInHomePath(0, 2)
                        || !board.isActivePieceInHomePath(0)
                        || board.getActivePieceStep() != 2
                        || board.getActiveRouteSize(0) != 6) {

                    throw new AssertionError(
                        "Home position failed: " + level + "/" + color
                    );
                }

                // בדיקת דחיית מיקומים לא חוקיים
                if (!board.placeActivePieceOnStart(1)) {
                    throw new AssertionError("Second piece failed");
                }

                if (board.showActivePieceInHomePath(1, -1)
                        || board.showActivePieceInHomePath(1, 6)
                        || board.isActivePieceInHomePath(1)) {

                    throw new AssertionError(
                        "Invalid home step accepted"
                    );
                }
            }

            System.out.println(
                "PASS - " + level
                + ": home positions and boundaries for all colors"
            );
        }
    }
}