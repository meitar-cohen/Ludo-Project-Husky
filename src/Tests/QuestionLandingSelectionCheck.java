
package Tests;

import Controller.GameController;
import View.BoardPanel;

/**
 * בדיקת בחירת שאלות לפי רמת המשבצת
 * שעליה כלי המשחק נמצא.
 */
public class QuestionLandingSelectionCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // בדיקת אירועי שאלה ברמת משחק נתונה
    private static void checkGame(
            String gameLevel, int expectedQuestions) {

        GameController controller =
                GameController.createNewGame(
                        new String[]{"A", "B"},
                        new String[]{"Green", "Blue"},
                        gameLevel);

        BoardPanel board = new BoardPanel(controller);
        board.setActivePlayerColor("Green");

        // הצבת כלי על המסלול לצורך בדיקה
        check(board.placeActivePieceOnStart(0),
                "Cannot place test piece");

        int questionCount = 0;

        // מעבר על המשבצות במסלול הפעיל
        for (int step = 0;
                step < board.getActiveRouteSize(0); step++) {

            String event =
                    board.getSquareEventAtStep(0, step);

            String difficulty =
                    board.getQuestionDifficultyAtStep(0, step);

            if ("QUESTION".equals(event)) {

                // לכל משבצת שאלה חייבת להיות רמת קושי
                check(difficulty != null,
                        "Question square has no difficulty");

                // בחירת שאלה מהמאגר לפי רמת המשבצת
                controller.selectQuestion(difficulty);

                check(difficulty.equalsIgnoreCase(
                                controller.getQuestionDifficulty()),
                        "Selected question has wrong difficulty");

                check(controller.getQuestionOptions().length == 4,
                        "Question must have four options");

                questionCount++;

            } else {
                // למשבצת שאינה שאלת ידע אין רמת שאלה
                check(difficulty == null,
                        "Non-question square has question difficulty");
            }
        }

        check(questionCount == expectedQuestions,
                "Incorrect number of question squares");

        // בדיקת מיקום לא חוקי
        check(board.getQuestionDifficultyAtStep(0, -1) == null,
                "Invalid step must not return difficulty");

        // במסלול הבית אין להפעיל שאלות
        check(board.showActivePieceInHomePath(0, 1),
                "Cannot enter home path");

        check(board.getQuestionDifficultyAtStep(0, 1) == null,
                "Home path must not return difficulty");

        System.out.println("PASS - " + gameLevel
                + ": " + questionCount + " question squares");
    }

    public static void main(String[] args) {

        // חזרה על הבדיקה עם מיקומים אקראיים שונים
        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            for (int i = 0; i < 5; i++) {
                checkGame(level,
                        "Easy".equals(level) ? 0 : 6);
            }
        }

        System.out.println(
                "All question landing selection checks passed.");
    }
}
