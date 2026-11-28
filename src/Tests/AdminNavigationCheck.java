package Tests;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import View.AdminLoginScreen;
import View.MainMenuScreen;

/**
 * בדיקת ניווט לממשק הכניסה ללא שימוש בסיסמת מנהל אמיתית.
 * אינה שומרת נתונים ואינה משנה את קובץ השאלות.
 */
public final class AdminNavigationCheck {

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static JButton findButton(Container container, String label) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button
                    && label.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child, label);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Window> T visibleWindow(Class<T> type) {
        for (Window window : Window.getWindows()) {
            if (type.isInstance(window) && window.isVisible()) {
                return type.cast(window);
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                MainMenuScreen menu = new MainMenuScreen();
                menu.setVisible(true);
                JButton manage = findButton(menu.getContentPane(),
                        "Question Management");
                check(manage != null && manage.isVisible(),
                        "Question Management is not visible in menu");
                manage.doClick();
                check(visibleWindow(AdminLoginScreen.class) != null,
                        "Admin login did not open");
                check(!menu.isDisplayable(),
                        "Original menu did not close");
                System.out.println("PASS - menu opens administrator login");

                AdminLoginScreen login = visibleWindow(AdminLoginScreen.class);
                JButton back = findButton(login.getContentPane(),
                        "Back to Main Menu");
                check(back != null, "Back button missing");
                back.doClick();
                check(visibleWindow(MainMenuScreen.class) != null,
                        "Back button did not open main menu");
                check(!login.isDisplayable(), "Login window remains open");
                System.out.println("PASS - back button returns to main menu");

                MainMenuScreen returned = visibleWindow(MainMenuScreen.class);
                JButton next = findButton(returned.getContentPane(),
                        "Question Management");
                next.doClick();
                AdminLoginScreen second = visibleWindow(AdminLoginScreen.class);
                check(second != null, "Second login did not open");
                second.dispatchEvent(new WindowEvent(second,
                        WindowEvent.WINDOW_CLOSING));
                check(visibleWindow(MainMenuScreen.class) != null,
                        "Window X did not return to main menu");
                System.out.println("PASS - closing login returns to main menu");
                System.out.println("All admin navigation checks passed.");
            } finally {
                for (Window window : Window.getWindows()) {
                    window.dispose();
                }
            }
        });
    }
}
