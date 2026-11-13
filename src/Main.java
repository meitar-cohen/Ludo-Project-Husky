import javax.swing.SwingUtilities;
import View.MainMenuView;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenuView mainMenuView = new MainMenuView();
            mainMenuView.setVisible(true);
        });
    }
}