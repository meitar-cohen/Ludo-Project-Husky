
import view.BoardPanel;

public class TurnSquareEventCheck {

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {

        String[] colors = {
                "Green", "Yellow", "Blue", "Red"
        };

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            // בדיקה בעשרה לוחות שונים
            for (int gameNumber = 0; gameNumber < 10; gameNumber++) {

                BoardPanel board = new BoardPanel(level, colors);
                board.setActivePlayerColor("Green");

                check(board.placeActivePieceOnStart(0),
                        level + ": cannot place piece");

                int questions = 0;
                int surprises = 0;

                // בודקים את כל המסלול, כי מיקומי האירועים אקראיים
                for (int step = 0;
                        step < board.getActiveRouteSize(0); step++) {

                    String event =
                            board.getSquareEventAtStep(0, step);

                    if ("QUESTION".equals(event)) {
                        questions++;
                    } else if ("SURPRISE".equals(event)) {
                        surprises++;
                    } else {
                        check("NONE".equals(event),
                                level + ": unknown event " + event);
                    }
                }

                // המסלול החיצוני כולל שש שאלות ושתי הפתעות
                check(questions == ("Easy".equals(level) ? 0 : 6),
                        level + ": question detection count");

                check(surprises == ("Easy".equals(level) ? 0 : 2),
                        level + ": surprise detection count");

                check("NONE".equals(
                        board.getSquareEventAtStep(0, -1)),
                        level + ": invalid step");

                // במסלול הבית לא מפעילים שאלה או הפתעה
                check(board.showActivePieceInHomePath(0, 1),
                        level + ": cannot enter home path");

                check("NONE".equals(
                        board.getSquareEventAtStep(0, 1)),
                        level + ": home path must not trigger events");
            }

            System.out.println(
                    "PASS - " + level + " landing events");
        }

        System.out.println(
                "PASS - dynamic square event detection");
    }
}
