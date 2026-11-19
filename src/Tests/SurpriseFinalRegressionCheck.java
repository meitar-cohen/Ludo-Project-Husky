package Tests;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import Model.QuestionRepository;

/** רגרסיה מרכזית למערכות ההפתעות והשאלות, ללא משחק ידני. */
public final class SurpriseFinalRegressionCheck {

    private SurpriseFinalRegressionCheck() { }

    private static byte[] readIfExists(Path path) throws Exception {
        return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
    }

    public static void main(String[] args) throws Exception {
        // אימות שהבדיקות לא משנות את מאגר השאלות או פרטי המנהל.
        Path questions = QuestionRepository.getProjectCsvPath();
        Path admin = Path.of(System.getProperty("user.home"),
                ".ludo", "admin-auth.properties");
        byte[] questionsBefore = readIfExists(questions);
        byte[] adminBefore = readIfExists(admin);

        try {
            System.out.println("=== SURPRISE SYSTEM REGRESSION ===");
            SurpriseStage3RegressionCheck.main(new String[0]);
            SurpriseBoundaryCasesCheck.main(new String[0]);
            SurpriseTurnSequenceCheck.main(new String[0]);
            SurpriseBoardSyncCheck.main(new String[0]);
            SurprisePresentationSmokeCheck.main(new String[0]);

            System.out.println("=== QUESTION SYSTEM REGRESSION ===");
            QuestionSystemRegressionCheck.main(new String[0]);
            System.out.println("PASS - surprise and question checks completed");
        } finally {
            if (!Arrays.equals(questionsBefore, readIfExists(questions))) {
                throw new AssertionError("Production question CSV was modified!");
            }
            if (!Arrays.equals(adminBefore, readIfExists(admin))) {
                throw new AssertionError("Administrator credentials were modified!");
            }
            System.out.println("PASS - production data was not modified");
        }

        System.out.println("All surprise final regression checks passed.");
    }
}