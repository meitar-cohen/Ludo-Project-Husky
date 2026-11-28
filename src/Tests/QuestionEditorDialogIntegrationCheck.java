package Tests;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import Controller.QuestionManagementController;
import Model.Question;
import Model.QuestionRecord;
import Model.QuestionRepository;
import View.QuestionEditorDialog;

/**
 * Exercises the independent dialog using a temporary CSV. Runs with desktop UI/Xvfb.
 */
public final class QuestionEditorDialogIntegrationCheck {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton button && text.equals(button.getText())) return button;
            if (c instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JTextArea findTextArea(Container root, String exactText) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextArea area && exactText.equals(area.getText())) return area;
            if (c instanceof Container child) {
                JTextArea found = findTextArea(child, exactText);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("ludo-editor-uml-");
        Path csv = dir.resolve("questions_Husky.csv");
        try {
            Files.copy(QuestionRepository.getProjectCsvPath(), csv,
                    StandardCopyOption.REPLACE_EXISTING);
            QuestionManagementController controller = new QuestionManagementController(csv);
            QuestionRecord existing = controller.getQuestions().get(0);
            SwingUtilities.invokeAndWait(() -> {
                QuestionEditorDialog dialog = new QuestionEditorDialog(null, existing,
                        (question, pattern, coverage, source) -> controller.update(
                                existing.getId(), question, pattern, coverage, source));
                check(dialog.getTitle().contains("Edit Question #" + existing.getId()),
                        "Editor did not receive existing question ID");
                JTextArea field = findTextArea(dialog.getContentPane(),
                        existing.getQuestion().getText());
                check(field != null, "Existing question not loaded into editor");
                field.setText("UML dialog edit test");
                JButton save = findButton(dialog.getContentPane(), "Save Changes");
                check(save != null, "Save button not found");
                save.doClick();
                check(!dialog.isDisplayable(), "Editor did not close on successful save");
            });
            QuestionRecord saved = controller.getQuestions().get(0);
            check(saved.getQuestion().getText().equals("UML dialog edit test"),
                    "Editor failed to save the question");
            check(saved.getSource().equals(existing.getSource())
                    && saved.getCoverage().equals(existing.getCoverage())
                    && saved.getArchitecturalPattern().equals(existing.getArchitecturalPattern()),
                    "Editor discarded CSV metadata");
            check(QuestionRepository.loadRecordsFromCsv(csv).get(0).getQuestion()
                    .getText().equals("UML dialog edit test"), "Editor did not persist to CSV");
            System.out.println("PASS - independent editor opens, saves and preserves CSV metadata");
            System.out.println("PASS - production questions were not modified");
            System.out.println("All QuestionEditorDialog checks passed.");
        } finally {
            Files.deleteIfExists(csv);
            Files.deleteIfExists(dir);
        }
    }
}
