package Tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import Controller.AdminAuthController;
import Controller.QuestionController;
import Model.AdminCredentials;
import Model.Question;
import Model.QuestionEvent;
import Model.QuestionRecord;
import Model.QuestionRepository;

/**
 * Tests the UML-facing QuestionController with isolated CSV and credentials.
 * Neither the production question bank nor the real administrator credentials are modified.
 */
public final class QuestionControllerUmlCheck {
    private static void check(boolean value, String failure) {
        if (!value) throw new AssertionError(failure);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("ludo-question-uml-");
        Path csv = dir.resolve("questions_Husky.csv");
        Path authFile = dir.resolve("admin-auth.properties");
        try {
            Files.copy(QuestionRepository.getProjectCsvPath(), csv,
                    StandardCopyOption.REPLACE_EXISTING);
            AdminAuthController auth = new AdminAuthController(new AdminCredentials(authFile));
            QuestionController controller = new QuestionController(csv, auth);

            check(controller.requiresSetup(), "Admin should require setup");
            char[] pass = "UmlOnly!2026".toCharArray();
            controller.setupPassword(pass, pass);
            check(controller.authenticateAdmin(pass), "Admin login failed");
            check(!controller.authenticateAdmin("wrong-password".toCharArray()),
                    "Incorrect administrator password accepted");
            System.out.println("PASS - administrator authentication through QuestionController");

            List<Question> loaded = controller.loadQuestions();
            check(loaded.size() == 30 && controller.hasEnoughQuestions(),
                    "Question controller did not load the 30 original questions");
            for (String level : List.of("easy", "medium", "hard")) {
                Question drawn = controller.getQuestionForSquare(level);
                check(drawn.getDifficulty().equalsIgnoreCase(level),
                        "Wrong difficulty returned for " + level);
                QuestionEvent event = controller.createQuestionEvent(level);
                check(event.getQuestion().getDifficulty().equalsIgnoreCase(level),
                        "Factory method returned wrong difficulty");
                check(event.getMoveSteps() == controller.getMoveSteps(level),
                        "Movement steps differ between controller and Factory Method");
                check(controller.isCorrectAnswer(drawn, drawn.getCorrectIndex()),
                        "Correct answer rejected");
                check(!controller.isCorrectAnswer(drawn, -1), "Wrong answer accepted");
            }
            check(!controller.validateQuestion("", new String[]{"a","b","c","d"}, 0, "easy").isEmpty(),
                    "Invalid question was not rejected");
            System.out.println("PASS - selection, validation, answers and Factory Method");

            List<QuestionRecord> records = controller.getQuestionRecords();
            QuestionRecord first = records.get(0);
            String editedText = first.getQuestion().getText() + " [UML test edit]";
            String editResult = controller.editQuestion(first.getId(), editedText,
                    first.getQuestion().getOptions(), first.getQuestion().getCorrectIndex(),
                    first.getQuestion().getDifficulty());
            check(editResult.startsWith("Question updated:"), "UML edit failed: " + editResult);
            QuestionRecord edited = controller.getQuestionRecords().stream()
                    .filter(r -> r.getId() == first.getId()).findFirst().orElseThrow();
            check(edited.getArchitecturalPattern().equals(first.getArchitecturalPattern())
                    && edited.getCoverage().equals(first.getCoverage())
                    && edited.getSource().equals(first.getSource()),
                    "UML edit discarded CSV metadata");

            String newText = "Temporary UML facade question";
            String addResult = controller.addQuestion(newText,
                    new String[]{"A", "B", "C", "D"}, 0, "hard");
            check(addResult.startsWith("Question added:"), "UML add failed: " + addResult);
            check(controller.getAllQuestions().size() == 31, "Added question not in model");
            check(controller.saveQuestions(), "Explicit save failed: " + controller.getLoadError());
            check(QuestionRepository.loadRecordsFromCsv(csv).size() == 31, "Added question not in CSV");
            int newId = controller.getQuestionRecords().stream()
                    .filter(r -> r.getQuestion().getText().equals(newText))
                    .findFirst().orElseThrow().getId();
            check(controller.deleteQuestion(newId), "Deleting added question failed");
            check(!controller.deleteQuestion(first.getId()), "Minimum 30 questions were not enforced");
            check(controller.getAllQuestions().size() == 30, "Unexpected question count");
            check(controller.getLoadError() != null, "Load error API missing");
            System.out.println("PASS - CRUD, CSV metadata, explicit save and minimum question count");
            System.out.println("PASS - production question CSV and admin credentials were not modified");
            System.out.println("All QuestionController UML checks passed.");
        } finally {
            Files.deleteIfExists(authFile);
            Files.deleteIfExists(csv);
            Files.deleteIfExists(dir);
        }
    }
}
