import view.BoardPanel;

public class TurnSquareEventCheck {

    private static void same(
            String expected, String actual, String description) {

        if (!expected.equals(actual)) {
            throw new AssertionError(
                    description + ": expected " + expected
                    + ", got " + actual);
        }
    }

    public static void main(String[] args) {

        String[] colors = {
                "Green", "Yellow", "Blue", "Red"
        };

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            BoardPanel board = new BoardPanel(level, colors);
            board.setActivePlayerColor("Green");

            if (!board.placeActivePieceOnStart(0)) {
                throw new AssertionError(
                        level + ": cannot place piece");
            }

            // משבצת רגילה אינה מפעילה אירוע
            same("NONE",
                    board.getSquareEventAtStep(0, 1),
                    level + " normal square");

            // מיקום לא חוקי לא מפעיל אירוע
            same("NONE",
                    board.getSquareEventAtStep(0, -1),
                    level + " invalid position");

            if ("Easy".equals(level)) {

                same("NONE",
                        board.getSquareEventAtStep(0, 3),
                        level + " no question");

            } else {

                // בדיקת המשבצות בלוח הקיים
                same("QUESTION",
                        board.getSquareEventAtStep(0, 3),
                        level + " question square");

                same("SURPRISE",
                        board.getSquareEventAtStep(0, 5),
                        level + " surprise square");
            }

            // במסלול הבית לא מפעילים אירועי שאלה והפתעה
            if (!board.showActivePieceInHomePath(0, 1)) {
                throw new AssertionError(
                        level + ": cannot enter home path");
            }

            same("NONE",
                    board.getSquareEventAtStep(0, 1),
                    level + " home path");

            System.out.println(
                    "PASS - " + level + " square event detection");
        }

        System.out.println("PASS - turn square event tests");
    }
}