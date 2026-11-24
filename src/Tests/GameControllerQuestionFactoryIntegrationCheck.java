package Tests;

import Controller.GameController;
import Controller.MovementController;
import Model.Game;
import Model.Player;

/**
 * בדיקת אינטגרציה בין GameController לבין מנגנון אירועי השאלות.
 * בודקת את החוזה הציבורי של הבקר לאחר שילוב Factory Method.
 */
public final class GameControllerQuestionFactoryIntegrationCheck {

    private GameControllerQuestionFactoryIntegrationCheck() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectInvalid(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void verifyQuestion(GameController controller,
            String difficulty, int expectedSteps) {

        // בחירת אירוע חדש דרך הבקר וחוזה ה-Factory Method
        controller.selectQuestion(difficulty);

        check(difficulty.trim().equalsIgnoreCase(
                controller.getQuestionDifficulty()),
                "Incorrect question difficulty: " + difficulty);
        check(controller.getCurrentQuestionMoveSteps() == expectedSteps,
                "Incorrect movement steps: " + difficulty);
        check(controller.getQuestionText() != null
                && !controller.getQuestionText().isBlank(),
                "Question text is missing");

        String[] options = controller.getQuestionOptions();
        check(options != null && options.length == 4,
                "Expected four answer options");

        int correctIndex = controller.getCorrectQuestionAnswerIndex();
        check(correctIndex >= 0 && correctIndex < options.length,
                "Correct answer index is out of range");

        // רק התשובה הנכונה צריכה להתקבל
        for (int i = 0; i < options.length; i++) {
            check(controller.isCorrectQuestionAnswer(i) == (i == correctIndex),
                    "Incorrect answer validation at index " + i);
        }
        check(!controller.isCorrectQuestionAnswer(-1),
                "Negative answer index was accepted");
        check(!controller.isCorrectQuestionAnswer(options.length),
                "Out-of-range answer index was accepted");

        System.out.println("PASS - " + difficulty.trim().toLowerCase()
                + ": question, answer and " + expectedSteps + " steps");
    }

    public static void main(String[] args) {
        // יצירת משחק אמיתי עם שני שחקנים
        Player[] players = {
                new Player("A", "Red"),
                new Player("B", "Blue")
        };
        Game game = new Game(players, "Hard");
        GameController controller = GameController.forTesting(
                game, new MovementController(game));

        // בבנאי נוצר אוטומטית אירוע שאלה קלה
        check("easy".equalsIgnoreCase(controller.getQuestionDifficulty()),
                "Initial question should be easy");
        check(controller.getCurrentQuestionMoveSteps() == 1,
                "Initial question should move one step");
        System.out.println("PASS - initial question event");

        verifyQuestion(controller, "easy", 1);
        verifyQuestion(controller, "medium", 2);
        verifyQuestion(controller, "hard", 3);

        // רווחים ואותיות גדולות לא משנים את בחירת הרמה
        verifyQuestion(controller, "  MEDIUM  ", 2);

        // קלט שגוי חייב להידחות ללא החלפת האירוע הנוכחי
        String questionBefore = controller.getQuestionText();
        expectInvalid(() -> controller.selectQuestion(null),
                "Null difficulty was accepted");
        expectInvalid(() -> controller.selectQuestion(" "),
                "Blank difficulty was accepted");
        expectInvalid(() -> controller.selectQuestion("unknown"),
                "Unsupported difficulty was accepted");
        check(controller.getQuestionText().equals(questionBefore)
                && controller.getCurrentQuestionMoveSteps() == 2,
                "Invalid difficulty replaced the current event");
        System.out.println("PASS - invalid difficulty handling");

        System.out.println(
                "All game controller question factory integration checks passed.");
    }
}
