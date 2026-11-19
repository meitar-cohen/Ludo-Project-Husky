
package Tests;

import java.util.List;
import java.util.Random;

import Model.Question;
import Model.QuestionBank;
import Model.QuestionEvent;
import Model.QuestionEventCreator;
import Model.QuestionRepository;

/**
 * בדיקות לתבנית Factory Method
 * באמצעות מאגר השאלות הקיים של הפרויקט.
 */
public final class QuestionEventCreatorCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // אימות שקלט לא תקין נדחה
    private static void expectInvalid(
            Runnable action, String message) {

        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }

        throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {

        // טעינת השאלות מקובץ ה-CSV הקיים
        List<Question> questions =
                QuestionRepository.loadProjectQuestions();

        QuestionBank bank =
                new QuestionBank(questions, new Random(2026));

        // שלושת היוצרים הקונקרטיים של התבנית
        QuestionEventCreator[] creators = {
                new QuestionEventCreator.Easy(bank),
                new QuestionEventCreator.Medium(bank),
                new QuestionEventCreator.Hard(bank)
        };

        String[] difficulties = {"easy", "medium", "hard"};

        // יצירת 100 אירועים מכל רמת קושי
        for (int level = 0; level < creators.length; level++) {

            for (int i = 0; i < 100; i++) {

                QuestionEvent event = creators[level].create();
                Question question = event.getQuestion();

                int correctIndex = question.getCorrectIndex();

                // בדיקת התאמה לרמת הקושי
                check(question.getDifficulty().equalsIgnoreCase(
                        difficulties[level]),
                        "Wrong event difficulty");

                // בדיקת מספר צעדי התנועה
                check(event.getMoveSteps() == level + 1,
                        "Wrong movement distance");

                // בדיקת תשובה נכונה ושגויה
                check(event.isCorrectAnswer(correctIndex),
                        "Correct answer rejected");

                check(!event.isCorrectAnswer(
                        (correctIndex + 1) % 4),
                        "Wrong answer accepted");

                // אימות שנוצר סוג האירוע המתאים
                check(switch (level) {
                    case 0 -> event instanceof QuestionEvent.Easy;
                    case 1 -> event instanceof QuestionEvent.Medium;
                    case 2 -> event instanceof QuestionEvent.Hard;
                    default -> false;
                }, "Wrong event class");
            }

            System.out.println(
                    "PASS - " + difficulties[level]
                            + ": 100 question events");
        }

        // בדיקות של קלט לא תקין
        expectInvalid(
                () -> new QuestionEventCreator.Easy(null),
                "Null question bank accepted");

        Question easyQuestion = questions.stream()
                .filter(q -> "easy".equalsIgnoreCase(
                        q.getDifficulty()))
                .findFirst()
                .orElseThrow();

        expectInvalid(
                () -> new QuestionEvent.Hard(easyQuestion),
                "Mismatched event difficulty accepted");

        System.out.println("PASS - Factory validation");

        System.out.println(
                "All question event factory checks passed.");
    }
}
