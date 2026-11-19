
package Tests;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import Model.Question;
import Model.QuestionRepository;

/**
 * בדיקת תקינות מאגר 30 השאלות.
 * ללא צורך בפתיחת משחק או ממשק גרפי.
 */
public final class QuestionBank30ValidationCheck {

    private QuestionBank30ValidationCheck() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {

        // טעינת המאגר באותו מנגנון שבו משתמש המשחק
        List<Question> questions =
                QuestionRepository.loadProjectQuestions();

        // אימות הכמות הכוללת
        check(questions.size() == 30,
                "Expected 30 questions, found " + questions.size());

        int easy = 0;
        int medium = 0;
        int hard = 0;

        Set<String> uniqueQuestions = new HashSet<>();

        for (Question question : questions) {

            // בדיקה שאין שאלות כפולות
            String normalizedText = question.getText()
                    .trim().toLowerCase(Locale.ROOT);

            check(uniqueQuestions.add(normalizedText),
                    "Duplicate question: " + question.getText());

            // בדיקת ארבע אפשרויות תשובה
            String[] options = question.getOptions();

            check(options.length == 4,
                    "Question must have exactly four answers");

            // בדיקת מיקום התשובה הנכונה
            check(question.getCorrectIndex() >= 0
                    && question.getCorrectIndex() < 4,
                    "Invalid correct answer index");

            // ספירת שאלות לפי רמת קושי
            String difficulty = question.getDifficulty()
                    .trim().toLowerCase(Locale.ROOT);

            switch (difficulty) {
                case "easy" -> easy++;
                case "medium" -> medium++;
                case "hard" -> hard++;
                default -> throw new AssertionError(
                        "Unsupported difficulty: " + difficulty);
            }
        }

        // אימות חלוקה שווה בין רמות הקושי
        check(easy == 10, "Expected 10 easy questions");
        check(medium == 10, "Expected 10 medium questions");
        check(hard == 10, "Expected 10 hard questions");

        System.out.println("PASS - 30 questions loaded");
        System.out.println("PASS - 10 easy questions");
        System.out.println("PASS - 10 medium questions");
        System.out.println("PASS - 10 hard questions");
        System.out.println("PASS - unique questions and valid answers");
        System.out.println("All question bank validation checks passed.");
    }
}
