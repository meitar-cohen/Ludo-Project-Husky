package Tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import Model.Question;
import Model.QuestionBank;
import Model.QuestionRecord;
import Model.QuestionRepository;

/**
 * בדיקות רגרסיה למערכת השאלות, ללא פתיחת חלונות משחק.
 * בדיקות CRUD ואימות מנהל משתמשות בקבצים זמניים בלבד.
 */
public final class QuestionSystemRegressionCheck {

    private QuestionSystemRegressionCheck() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkQuestionBank() throws Exception {
        // טעינת הנתונים מתוך אותו קובץ המשמש את המשחק ואת ממשק המנהל.
        List<QuestionRecord> records = QuestionRepository.loadRecordsFromCsv(
                QuestionRepository.getProjectCsvPath());
        List<Question> gameQuestions = QuestionRepository.loadProjectQuestions();

        check(records.size() == 30, "Expected 30 records, found " + records.size());
        check(gameQuestions.size() == records.size(),
                "Game and administrator loaders returned different counts");

        Set<Integer> ids = new HashSet<>();
        Set<String> texts = new HashSet<>();
        int easy = 0;
        int medium = 0;
        int hard = 0;

        for (QuestionRecord record : records) {
            check(ids.add(record.getId()), "Duplicate question ID: " + record.getId());

            Question question = record.getQuestion();
            String normalizedText = question.getText().trim().toLowerCase(Locale.ROOT);
            check(texts.add(normalizedText), "Duplicate question text: " + question.getText());

            String[] options = question.getOptions();
            check(options.length == 4, "Question must have four answers: " + record.getId());
            for (String answer : options) {
                check(answer != null && !answer.isBlank(),
                        "Empty answer at question " + record.getId());
            }
            check(question.getCorrectIndex() >= 0 && question.getCorrectIndex() < 4,
                    "Invalid correct answer index: " + record.getId());

            switch (question.getDifficulty().trim().toLowerCase(Locale.ROOT)) {
                case "easy" -> easy++;
                case "medium" -> medium++;
                case "hard" -> hard++;
                default -> throw new AssertionError(
                        "Unsupported question difficulty: " + question.getDifficulty());
            }
        }

        check(easy == 10 && medium == 10 && hard == 10,
                "Expected 10 questions in each difficulty; found "
                        + easy + "/" + medium + "/" + hard);

        QuestionBank bank = new QuestionBank(gameQuestions);
        check(bank.getTotalCount() == 30, "QuestionBank has incorrect total");
        for (String level : new String[]{"easy", "medium", "hard"}) {
            check(bank.getCountForDifficulty(level) == 10,
                    "QuestionBank has incorrect count for " + level);
        }
        System.out.println("PASS - CSV and QuestionBank: 30 unique questions (10 per level)");
        System.out.println("PASS - identifiers, four answers and correct answer indexes");
    }

    private static byte[] readIfExists(Path path) throws Exception {
        return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
    }

    public static void main(String[] args) throws Exception {
        // צילום מצב הנתונים האמיתיים לפני הבדיקה, כדי לזהות שינוי לא רצוי.
        Path csv = QuestionRepository.getProjectCsvPath();
        check(Files.isRegularFile(csv), "Question CSV is missing: " + csv);

        Path credentials = Path.of(System.getProperty("user.home"),
                ".ludo", "admin-auth.properties");
        byte[] csvBefore = readIfExists(csv);
        byte[] credentialsBefore = readIfExists(credentials);

        try {
            System.out.println("=== QUESTION SYSTEM REGRESSION ===");
            checkQuestionBank();

            // שימוש חוזר בבדיקות הקיימות במקום שכפול של תרחישים.
            QuestionEventCreatorCheck.main(new String[0]);
            GameControllerQuestionFactoryIntegrationCheck.main(new String[0]);
            QuestionManagementCrudCheck.main(new String[0]);
            AdminAuthenticationCheck.main(new String[0]);

            System.out.println("PASS - question selection, Factory Method, CRUD and admin authentication");
        } finally {
            // גם אם בדיקה נכשלה, נוודא שהקבצים האמיתיים לא השתנו.
            check(Arrays.equals(csvBefore, readIfExists(csv)),
                    "The real questions_Husky.csv changed during regression!");
            check(Arrays.equals(credentialsBefore, readIfExists(credentials)),
                    "The real administrator credentials changed during regression!");
            System.out.println("PASS - production CSV and administrator credentials were not modified");
        }

        System.out.println("All question system regression checks passed.");
    }
}
