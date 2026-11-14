import javax.swing.SwingUtilities;
import View.MainMenuScreen;
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenuScreen mainMenuView = new MainMenuScreen();
            mainMenuView.setVisible(true);
        });
    }
}