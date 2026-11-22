
package Tests;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Model.Question;
import Model.QuestionBank;
import Model.QuestionRepository;

/**
 * בדיקות למאגר השאלות ולבחירת שאלות לפי רמת קושי.
 */
public class QuestionBankCheck {

    // אימות תנאי בדיקה
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // בדיקה שפעולה לא חוקית זורקת את החריגה המתאימה
    private static void expectException(
            Class<? extends RuntimeException> expected,
            Runnable action,
            String message) {

        try {
            action.run();
        } catch (RuntimeException exception) {
            check(expected.isInstance(exception), message);
            return;
        }

        throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {

        // טעינת השאלות האמיתיות מקובץ המאגר
        Path file = Path.of("data", "questions_Husky.csv");
        List<Question> questions =
                QuestionRepository.loadFromCsv(file);

        // יצירת מאגר עם אקראיות קבועה לצורך בדיקה
        QuestionBank bank =
                new QuestionBank(questions, new Random(2026));

        // בדיקת מספר השאלות הכולל
        check(bank.getTotalCount() == questions.size(),
                "Incorrect total question count");

        check(bank.getTotalCount() >= 11,
                "Expected at least 11 questions");

        System.out.println("PASS - Total questions: "
                + bank.getTotalCount());

        // בדיקת מספר השאלות בכל רמה
        check(bank.getCountForDifficulty("easy") >= 3,
                "Incorrect easy question count");

        check(bank.getCountForDifficulty("medium") >= 4,
                "Incorrect medium question count");

        check(bank.getCountForDifficulty("hard") >= 4,
                "Incorrect hard question count");

        System.out.println("PASS - Difficulty counts");

        // בדיקה שהבחירה מחזירה רק שאלות מהרמה המבוקשת
        String[] difficulties = {"easy", "medium", "hard"};

        for (String difficulty : difficulties) {

            // ביצוע 100 בחירות מכל רמת קושי
            for (int i = 0; i < 100; i++) {

                Question selected =
                        bank.drawQuestion(difficulty);

                check(selected != null,
                        "Selected question is null");

                check(selected.getDifficulty()
                                .equalsIgnoreCase(difficulty),
                        "Selected wrong difficulty: "
                                + difficulty);
            }
        }

        System.out.println(
                "PASS - 300 difficulty-based selections");

        // בדיקת תמיכה באותיות גדולות וברווחים
        Question selected =
                bank.drawQuestion(" EASY ");

        check(selected.getDifficulty()
                        .equalsIgnoreCase("easy"),
                "Difficulty normalization failed");

        System.out.println("PASS - Difficulty normalization");

        // בדיקה של רמה שאינה קיימת במאגר
        expectException(
                IllegalStateException.class,
                () -> bank.drawQuestion("unknown"),
                "Unknown difficulty should fail");

        // בדיקת קלט ריק או null
        expectException(
                IllegalArgumentException.class,
                () -> bank.drawQuestion(" "),
                "Blank difficulty should fail");

        expectException(
                IllegalArgumentException.class,
                () -> bank.drawQuestion(null),
                "Null difficulty should fail");

        System.out.println("PASS - Invalid difficulty handling");

        // בדיקת מניעת יצירת מאגר ריק
        expectException(
                IllegalArgumentException.class,
                () -> new QuestionBank(List.of()),
                "Empty question bank should fail");

        // בדיקת מניעת מחולל אקראי חסר
        expectException(
                IllegalArgumentException.class,
                () -> new QuestionBank(questions, null),
                "Null random generator should fail");

        System.out.println("PASS - Constructor validation");

        // בדיקה שהמאגר שומר עותק עצמאי של הרשימה
        List<Question> original = new ArrayList<>(questions);

        QuestionBank snapshot =
                new QuestionBank(original);

        original.clear();

        check(snapshot.getTotalCount() == questions.size(),
                "External changes affected question bank");

        System.out.println("PASS - Defensive copy");

        System.out.println(
                "All QuestionBank checks passed.");
    }
}
