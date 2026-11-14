package Tests;
import View.BoardPanel;

public class HomePathViewCheck {

    public static void main(String[] args) {

        String[] colors = {"Green", "Yellow", "Blue", "Red"};
        String[] levels = {"Easy", "Medium", "Hard"};

        // בדיקת המעבר למסלול הבית בכל רמה ולכל צבע
        for (String level : levels) {

            BoardPanel board = new BoardPanel(level, colors);

            for (String color : colors) {

                board.setActivePlayerColor(color);

                // הכנסת כלי ללוח לצורך הבדיקה
                if (!board.placeActivePieceOnStart(0)) {
                    throw new AssertionError(
                        level + " " + color + ": start failed"
                    );
                }

                // בתחילה הכלי אינו במסלול הבית
                if (board.isActivePieceInHomePath(0)) {
                    throw new AssertionError(
                        "Piece started in home path"
                    );
                }

                // בדיקת עדכון מצב הכלי למסלול הבית
                if (!board.showActivePieceInHomePath(0)) {
                    throw new AssertionError(
                        level + " " + color + ": transition failed"
                    );
                }

                // אימות שהמסלול החדש מכיל 6 משבצות
                if (!board.isActivePieceInHomePath(0)
                        || board.getActiveRouteSize(0) != 6) {
                    throw new AssertionError(
                        level + " " + color + ": home path failed"
                    );
                }
            }

            System.out.println(
                "PASS - " + level + ": home path for all colors"
            );
        }
    }
}