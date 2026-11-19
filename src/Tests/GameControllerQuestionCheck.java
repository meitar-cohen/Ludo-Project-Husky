
package Tests;

import java.util.List;

import Controller.GameController;
import Controller.MovementController;
import Model.Game;
import Model.Player;
import Model.Question;
import Model.QuestionRepository;

/**
 * בדיקת החיבור בין בקר המשחק למאגר השאלות הקיים.
 */
public class GameControllerQuestionCheck {

    // בדיקת תנאי והצגת שגיאה במקרה של כישלון
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {

        // טעינת מאגר השאלות האמיתי לצורך השוואה
        List<Question> questions =
                QuestionRepository.loadProjectQuestions();

        // יצירת משחק בדיקה עם שני משתתפים
        Player[] players = {
                new Player("A", "Red"),
                new Player("B", "Blue")
        };

        Game game = new Game(players, "Easy");

        GameController controller = new GameController(
                game, new MovementController(game));

        // בדיקה שהשאלה הראשונית מגיעה ממאגר ה-CSV
        check(questions.stream().anyMatch(q ->
                q.getText().equals(controller.getQuestionText())),
                "Initial question was not loaded from CSV");

        System.out.println("PASS - Initial question comes from CSV");

        // בדיקת בחירת שאלות בכל שלוש רמות הקושי
        String[] difficulties = {"easy", "medium", "hard"};

        for (String difficulty : difficulties) {

            // בחירת 100 שאלות בכל רמת קושי
            for (int i = 0; i < 100; i++) {

                controller.selectQuestion(difficulty);

                // איתור השאלה שנבחרה במאגר המקורי
                Question matching = questions.stream()
                        .filter(q -> q.getText()
                                .equals(controller.getQuestionText()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Selected question not found in CSV"));

                // אימות שרמת השאלה מתאימה לרמה המבוקשת
                check(matching.getDifficulty()
                                .equalsIgnoreCase(difficulty),
                        "Incorrect question difficulty: " + difficulty);

                // אימות מספר אפשרויות התשובה
                check(controller.getQuestionOptions().length == 4,
                        "Each question must have four options");

                // אימות אינדקס התשובה הנכונה מול הקובץ
                check(controller.getCorrectQuestionAnswerIndex()
                                == matching.getCorrectIndex(),
                        "Correct answer index does not match CSV");

                // אימות שתשובה נכונה מזוהה כתקינה
                check(controller.isCorrectQuestionAnswer(
                                matching.getCorrectIndex()),
                        "Correct answer was rejected");

                // אימות שאינדקסים לא חוקיים נדחים
                check(!controller.isCorrectQuestionAnswer(-1)
                                && !controller.isCorrectQuestionAnswer(4),
                        "Out-of-range answer was accepted");
            }
        }

        System.out.println(
                "PASS - 300 selections match CSV, level and answer");

        System.out.println(
                "All game controller question checks passed.");
    }
}
