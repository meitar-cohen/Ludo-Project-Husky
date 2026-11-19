package Tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import Controller.QuestionManagementController;
import Model.Question;
import Model.QuestionRecord;
import Model.QuestionRepository;

/**
 * בדיקת הוספה, עריכה, מחיקה ושמירה ב-CSV.
 * כל הבדיקות מתבצעות על עותק זמני - לא משנות את קובץ הפרויקט!
 */
public final class QuestionManagementCrudCheck {

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path temporaryFolder = Files.createTempDirectory("ludo-question-check-");
        Path temporaryCsv = temporaryFolder.resolve("questions_Husky.csv");
        try {
            // השאלות המקוריות נשמרות בקובץ הפרויקט ללא שינוי.
            Files.copy(QuestionRepository.getProjectCsvPath(), temporaryCsv,
                    StandardCopyOption.REPLACE_EXISTING);
            QuestionManagementController controller =
                    new QuestionManagementController(temporaryCsv);

            check(controller.getQuestions().size() == 30,
                    "Expected 30 original questions");
            System.out.println("PASS - loaded 30 questions from temporary CSV");

            QuestionRecord original = controller.getQuestions().get(0);
            int originalId = original.getId();
            Question oldQuestion = original.getQuestion();
            String updatedText = oldQuestion.getText() + " [management edit check]";
            Question updatedQuestion = new Question(updatedText,
                    oldQuestion.getOptions(), oldQuestion.getCorrectIndex(),
                    oldQuestion.getDifficulty());

            // עריכה אמורה לשמר את המזהה ואת כל שדות המטא-דאטה.
            controller.update(originalId, updatedQuestion,
                    original.getArchitecturalPattern(), original.getCoverage(),
                    original.getSource());
            QuestionRecord edited = QuestionRepository.loadRecordsFromCsv(temporaryCsv)
                    .stream().filter(r -> r.getId() == originalId)
                    .findFirst().orElseThrow();
            check(edited.getQuestion().getText().equals(updatedText),
                    "Edited question was not saved");
            check(edited.getSource().equals(original.getSource())
                    && edited.getCoverage().equals(original.getCoverage())
                    && edited.getArchitecturalPattern().equals(original.getArchitecturalPattern()),
                    "Metadata was not preserved");
            System.out.println("PASS - edit saved and metadata preserved");

            // בדיקת פסיקים, מירכאות וירידת שורה בתוך CSV.
            String complexAnswer = "First, \"quoted\"?\nSecond line";
            Question addedQuestion = new Question("Temporary test question, \"CSV\"",
                    new String[]{complexAnswer, "Option two", "Option three", "Option four"},
                    0, "hard");
            int addedId = controller.add(addedQuestion,
                    "Temporary pattern", "Temporary coverage", "Temporary source");
            check(controller.getQuestions().size() == 31,
                    "Added question not counted");
            QuestionRecord reloaded = QuestionRepository.loadRecordsFromCsv(temporaryCsv)
                    .stream().filter(r -> r.getId() == addedId)
                    .findFirst().orElseThrow();
            check(reloaded.getQuestion().getText().equals(addedQuestion.getText())
                    && reloaded.getQuestion().getOptions()[0].equals(complexAnswer),
                    "Comma / quotes / newline did not survive CSV save");
            System.out.println("PASS - add and CSV special characters");

            // שאלות כפולות נדחות ללא שינוי קובץ.
            expectInvalid(() -> {
                try {
                    controller.add(addedQuestion, "", "", "");
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }, "Duplicate question was accepted");
            check(controller.getQuestions().size() == 31,
                    "Rejected duplicate changed bank size");
            System.out.println("PASS - duplicate question rejected");

            controller.delete(addedId);
            check(controller.getQuestions().size() == 30,
                    "Deleted question is still present");
            check(QuestionRepository.loadFromCsv(temporaryCsv).size() == 30,
                    "Game CSV loader cannot read saved file");
            System.out.println("PASS - delete and game CSV compatibility");

            // אין לרדת מתחת ל-30 שאלות.
            expectInvalid(() -> {
                try {
                    controller.delete(originalId);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }, "Deleting below 30 questions was accepted");
            System.out.println("PASS - minimum 30 questions enforced");
            System.out.println("All question management CRUD checks passed.");
        } finally {
            Files.deleteIfExists(temporaryCsv);
            Files.deleteIfExists(temporaryFolder);
        }
    }
}
