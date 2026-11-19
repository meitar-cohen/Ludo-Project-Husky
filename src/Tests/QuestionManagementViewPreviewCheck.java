package Tests;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.swing.SwingUtilities;

import Controller.QuestionManagementController;
import Model.QuestionRepository;
import View.QuestionManagementView;

/**
 * פתיחת ממשק הניהול על עותק CSV זמני בלבד.
 * לצורכי בדיקת הממשק עד לחיבור מסך הזדהות המנהל בשלב 20.
 */
public final class QuestionManagementViewPreviewCheck {
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("ludo-admin-preview-");
        Path copy = folder.resolve("questions_Husky.csv");
        Files.copy(QuestionRepository.getProjectCsvPath(), copy,
                StandardCopyOption.REPLACE_EXISTING);
        QuestionManagementController controller =
                new QuestionManagementController(copy);
        System.out.println("PASS - admin preview uses a temporary CSV copy");
        SwingUtilities.invokeLater(() -> {
            QuestionManagementView window = new QuestionManagementView(controller);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e) {
                    try {
                        Files.deleteIfExists(copy);
                        Files.deleteIfExists(folder);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            window.setVisible(true);
        });
    }
}
