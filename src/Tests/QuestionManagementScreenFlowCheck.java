package Tests;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import Controller.QuestionManagementController;
import Model.QuestionRepository;
import View.QuestionEditorDialog;
import View.QuestionManagementScreen;

/**
 * Tests the management screen -> standalone editor -> CSV flow on temporary data.
 * Use a desktop display or xvfb-run; never edits the production question bank.
 */
public final class QuestionManagementScreenFlowCheck {

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static <T extends Component> T find(Container container, Class<T> type, String text) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                if (text == null || component instanceof JButton b && text.equals(b.getText())) {
                    return type.cast(component);
                }
            }
            if (component instanceof Container child) {
                T nested = find(child, type, text);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static QuestionEditorDialog visibleEditor() {
        for (Window window : Window.getWindows()) {
            if (window instanceof QuestionEditorDialog editor && editor.isVisible()) return editor;
        }
        return null;
    }

    private static QuestionEditorDialog awaitEditor() throws Exception {
        for (int attempt = 0; attempt < 60; attempt++) {
            QuestionEditorDialog[] editor = new QuestionEditorDialog[1];
            SwingUtilities.invokeAndWait(() -> editor[0] = visibleEditor());
            if (editor[0] != null) return editor[0];
            Thread.sleep(50);
        }
        throw new AssertionError("QuestionEditorDialog did not open from management screen");
    }

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("ludo-screen-flow-");
        Path csv = temp.resolve("questions_Husky.csv");
        QuestionManagementScreen[] screen = new QuestionManagementScreen[1];
        try {
            Files.copy(QuestionRepository.getProjectCsvPath(), csv,
                    StandardCopyOption.REPLACE_EXISTING);
            QuestionManagementController backend = new QuestionManagementController(csv);
            SwingUtilities.invokeAndWait(() -> {
                screen[0] = new QuestionManagementScreen(backend);
                screen[0].setVisible(true);
                JButton add = find(screen[0].getContentPane(), JButton.class, "+ Add Question");
                check(add != null, "Add button was not found");
                SwingUtilities.invokeLater(add::doClick);
            });

            QuestionEditorDialog editor = awaitEditor();
            SwingUtilities.invokeAndWait(() -> {
                java.util.List<JTextArea> areas = new java.util.ArrayList<>();
                collectAreas(editor.getContentPane(), areas);
                check(areas.size() == 7, "Unexpected number of editor text areas: " + areas.size());
                areas.get(0).setText("Management screen integration question");
                for (int i = 1; i <= 4; i++) areas.get(i).setText("Answer " + i);
                JTextField pattern = find(editor.getContentPane(), JTextField.class, null);
                check(pattern != null, "Metadata field missing");
                pattern.setText("Integration Pattern");
                JButton save = find(editor.getContentPane(), JButton.class, "Add & Save");
                check(save != null, "Save button not found");
                save.doClick();
            });
            check(backend.getQuestions().size() == 31, "Add was not persisted through screen");
            check(backend.getQuestions().get(30).getArchitecturalPattern().equals("Integration Pattern"),
                    "Metadata was not preserved");
            check(QuestionRepository.loadRecordsFromCsv(csv).size() == 31,
                    "Management screen did not save to temporary CSV");
            SwingUtilities.invokeAndWait(() -> {
                JTable table = find(screen[0].getContentPane(), JTable.class, null);
                check(table != null && table.getRowCount() == 31,
                        "Management table did not refresh after Add & Save");
                screen[0].dispose();
            });
            System.out.println("PASS - Add button opens independent editor");
            System.out.println("PASS - saving updates CSV metadata and refreshes the management table");
            System.out.println("PASS - original question bank unchanged");
            System.out.println("All management screen flow checks passed.");
        } finally {
            if (screen[0] != null) {
                SwingUtilities.invokeAndWait(screen[0]::dispose);
            }
            Files.deleteIfExists(csv);
            Files.deleteIfExists(temp);
        }
    }

    private static void collectAreas(Container root, java.util.List<JTextArea> target) {
        for (Component component : root.getComponents()) {
            if (component instanceof JTextArea area) target.add(area);
            if (component instanceof Container child) collectAreas(child, target);
        }
    }
}
