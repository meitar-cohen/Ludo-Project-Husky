package Tests;

import Controller.GameController;
import View.BoardPanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;

/**
 * Checks the worlds of the 3D version (Strategy pattern, see {@code View.WorldTheme}):
 * the catalogue is complete, every world can paint the board, its pieces and its choice-screen
 * picture, and every world gives different pictures.
 *
 * <p>The View classes are package-private, so they are used through reflection.
 */
public final class ThemeCatalogCheck {
    private ThemeCatalogCheck() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object call(Object target, String method, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getMethod(method, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Object callNoArgs(Object target, String method) throws Exception {
        Class<?> type = Class.forName("View.WorldTheme");
        Method m = type.getDeclaredMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static int distinctColors(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < image.getHeight(); y += 3) {
            for (int x = 0; x < image.getWidth(); x += 3) {
                colors.add(image.getRGB(x, y));
            }
        }
        return colors.size();
    }

    public static void main(String[] args) throws Exception {
        Class<?> themes = Class.forName("View.Themes");
        Method all = themes.getDeclaredMethod("all");
        all.setAccessible(true);
        List<?> list = (List<?>) all.invoke(null);

        String[] expected = {"sky", "volcano", "pool", "salon", "racing"};
        check(list.size() == expected.length, "five worlds expected, found " + list.size());
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < expected.length; i++) {
            String id = (String) callNoArgs(list.get(i), "id");
            check(expected[i].equals(id), "world " + i + " should be " + expected[i] + " but is " + id);
            check(ids.add(id), "duplicate id " + id);
            String title = (String) callNoArgs(list.get(i), "title");
            String tagline = (String) callNoArgs(list.get(i), "tagline");
            check(title != null && !title.trim().isEmpty(), "title of " + id);
            check(tagline != null && tagline.length() > 10, "tagline of " + id);
        }
        System.out.println("PASS - the catalogue has the five worlds");

        Method byId = themes.getDeclaredMethod("byId", String.class);
        byId.setAccessible(true);
        Method defaultTheme = themes.getDeclaredMethod("defaultTheme");
        defaultTheme.setAccessible(true);
        check(byId.invoke(null, "pool") == list.get(2), "byId finds a world");
        check(byId.invoke(null, "nope") == defaultTheme.invoke(null), "unknown id gives the default world");
        check(defaultTheme.invoke(null) == list.get(0), "the default world is Ludo in the Sky");
        System.out.println("PASS - lookup by id and default world");

        Class<?> panelClass = Class.forName("View.Board3DPanel");
        Class<?> themeClass = Class.forName("View.WorldTheme");
        Constructor<?> constructor = panelClass.getDeclaredConstructor(BoardPanel.class, themeClass);
        constructor.setAccessible(true);
        Set<Integer> fingerprints = new HashSet<>();
        for (Object theme : list) {
            String id = (String) callNoArgs(theme, "id");
            GameController controller = GameController.createNewGame(
                    new String[]{"a", "b"}, new String[]{"Red", "Blue"}, "Medium");
            BoardPanel board = new BoardPanel(controller);
            board.setSize(700, 700);
            JPanel panel = (JPanel) constructor.newInstance(board, theme);
            panel.setSize(800, 640);
            panel.doLayout();
            BufferedImage image = new BufferedImage(800, 640, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            panel.paint(g);
            g.dispose();
            check(distinctColors(image) > 200, id + ": the 3D board is painted (" + distinctColors(image) + " colours)");
            fingerprints.add(image.getRGB(100, 100) * 31 + image.getRGB(700, 100));

            BufferedImage preview = new BufferedImage(210, 84, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = preview.createGraphics();
            Method paintPreview = themeClass.getDeclaredMethod("paintPreview", Graphics2D.class, int.class, int.class);
            paintPreview.setAccessible(true);
            paintPreview.invoke(theme, pg, 210, 84);
            pg.dispose();
            check(distinctColors(preview) > 30, id + ": the choice-screen picture is painted");
        }
        check(fingerprints.size() >= 4, "the worlds do not look different");
        System.out.println("PASS - every world paints the board, the houses, the pieces and its preview");

        // the choice screen needs a window system; nothing to check without one
        Class<?> dialog = Class.forName("View.ThemeChoiceDialog");
        Method choose = dialog.getDeclaredMethod("choose", Window.class);
        check(choose != null, "the choice screen exists");
        System.out.println("PASS - the choice screen exists");
        System.out.println("All theme catalogue checks passed.");
        System.exit(0);
    }
}
