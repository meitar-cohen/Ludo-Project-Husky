
package Tests;

import java.io.IOException;
import java.util.List;

import Model.Question;
import Model.QuestionRepository;

/**
 * בדיקות לטעינת מאגר השאלות מקובץ CSV.
 */
public class QuestionRepositoryCheck {

    // אימות תנאי בדיקה
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws IOException {

    	List<Question> questions =
    	        QuestionRepository.loadProjectQuestions();

        // בדיקה שכל השאלות הקיימות נטענו
        check(questions.size() >= 11,
                "Expected at least 11 questions");

        System.out.println("PASS - Questions loaded: "
                + questions.size());

        int easy = 0;
        int medium = 0;
        int hard = 0;

        // בדיקת תקינות כל השאלות
        for (Question question : questions) {

            check(!question.getText().isBlank(),
                    "Question text is empty");

            check(question.getOptions().length == 4,
                    "Question must have four options");

            check(question.getCorrectIndex() >= 0
                    && question.getCorrectIndex() < 4,
                    "Invalid correct answer index");

            // ספירת שאלות לפי רמת קושי
            switch (question.getDifficulty().toLowerCase()) {
                case "easy" -> easy++;
                case "medium" -> medium++;
                case "hard" -> hard++;
                default -> throw new AssertionError(
                        "Unknown difficulty: "
                                + question.getDifficulty());
            }
        }

        // אימות כמות השאלות בכל רמה במאגר הנוכחי
        check(easy >= 3, "Missing easy questions");
        check(medium >= 4, "Missing medium questions");
        check(hard >= 4, "Missing hard questions");

        System.out.println("PASS - Easy: " + easy);
        System.out.println("PASS - Medium: " + medium);
        System.out.println("PASS - Hard: " + hard);

        // בדיקת המרת תשובה נכונה מ-1 ל-0
        check(questions.get(0).getCorrectIndex() == 0,
                "Incorrect answer index conversion");

        // בדיקת המרה נוספת: תשובה מספר 2 הופכת לאינדקס 1
        check(questions.get(3).getCorrectIndex() == 1,
                "Incorrect answer index for question 4");

        System.out.println("PASS - Answer index conversion");

        // בדיקה שפסיקים בתוך תשובות לא פגעו בפענוח
        check(questions.get(1).getOptions()[0].contains(","),
                "CSV quoted field was not parsed correctly");

        System.out.println("PASS - CSV quoted fields");

        System.out.println(
                "All question repository checks passed.");
    }
}
