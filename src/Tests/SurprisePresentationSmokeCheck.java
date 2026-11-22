package Tests;

import java.awt.Window;
import java.lang.reflect.Constructor;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** פותח את חלון ההפתעות באופן אוטומטי, ללא משחק ידני. */
public final class SurprisePresentationSmokeCheck {
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println("SKIP - Swing display is not available");
            return;
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                Class<?> cls = Class.forName("View.SurpriseDialog");
                Constructor<?> c = cls.getDeclaredConstructor(Window.class,
                        String.class, String.class, String.class);
                c.setAccessible(true);
                javax.swing.JDialog dialog = (javax.swing.JDialog) c.newInstance(
                        null, "OPPONENT DETOUR!", "OPPONENT MOVEMENT",
                        "Rolled 5; opponent moves back 3 squares.");
                Timer closeTimer = new Timer(300, e -> dialog.dispose());
                closeTimer.setRepeats(false);
                closeTimer.start();
                dialog.setVisible(true);
                if (dialog.isDisplayable()) throw new AssertionError("Dialog not closed");
                System.out.println("PASS - surprise dialog opened and closed");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("All surprise presentation checks passed.");
    }
}