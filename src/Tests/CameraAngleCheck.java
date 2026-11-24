package Tests;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;

import Controller.GameController;
import Controller.MovementController;
import Model.Game;
import Model.Player;
import View.BoardPanel;

/**
 * The first view of the 3D board is a high, broadcast-style angle (like the classic football-game
 * camera), not the low camera right behind the piece. Double-click (reset view) goes back to it.
 * The View classes are package-private, so they are used through reflection.
 */
public final class CameraAngleCheck {
    private CameraAngleCheck() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static double number(Object target, String name) throws Exception {
        return ((Number) field(target, name)).doubleValue();
    }

    private static void paint(JPanel panel) {
        BufferedImage image = new BufferedImage(900, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
    }

    public static void main(String[] args) throws Exception {
        Game game = new Game(new Player[]{new Player("Ann", "Green"), new Player("Bob", "Red")}, "Easy");
        GameController controller = GameController.forTesting(game, new MovementController(game));
        BoardPanel board = new BoardPanel(controller);
        board.setSize(700, 700);

        Class<?> themes = Class.forName("View.Themes");
        Method all = themes.getDeclaredMethod("all");
        all.setAccessible(true);
        List<?> list = (List<?>) all.invoke(null);
        Class<?> themeClass = Class.forName("View.WorldTheme");
        Constructor<?> constructor = Class.forName("View.Board3DPanel").getDeclaredConstructor(BoardPanel.class, themeClass);
        constructor.setAccessible(true);

        // the first view in every world
        for (Object theme : list) {
            JPanel panel = (JPanel) constructor.newInstance(board, theme);
            panel.setSize(900, 640);
            panel.doLayout();
            double elevation = Math.toDegrees(number(panel, "orbitElevation"));
            check(elevation >= 38 && elevation <= 46, "the first view is a high angle (about 42 degrees), not " + elevation);
            check(number(panel, "zoom") == 1.0, "the first view has the default zoom");
            paint(panel);                                         // paints and places the camera
            Object cam = field(panel, "cam");
            double height = number(cam, "posY") - 0.45;
            double pitch = Math.toDegrees(number(cam, "pitch"));
            check(height >= 10, "the camera is high above the board: " + height);
            check(pitch >= 30 && pitch <= 45, "the camera looks down from above: " + pitch);
        }
        System.out.println("PASS - the first view of every world is a high broadcast-style angle");

        // dragging changes the angle, the reset goes back to the first view
        Object theme = list.get(0);
        JPanel panel = (JPanel) constructor.newInstance(board, theme);
        panel.setSize(900, 640);
        panel.doLayout();
        double first = number(panel, "orbitElevation");
        Field elevation = panel.getClass().getDeclaredField("orbitElevation");
        elevation.setAccessible(true);
        elevation.setDouble(panel, Math.toRadians(15));
        Field zoom = panel.getClass().getDeclaredField("zoom");
        zoom.setAccessible(true);
        zoom.setDouble(panel, 1.7);
        Method reset = panel.getClass().getDeclaredMethod("resetView");
        reset.setAccessible(true);
        reset.invoke(panel);
        check(number(panel, "orbitElevation") == first && number(panel, "zoom") == 1.0,
                "the reset goes back to the first view");
        System.out.println("PASS - reset view returns to the first view");

        // the picture is still painted (not an empty sky)
        paint(panel);
        BufferedImage image = new BufferedImage(900, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        panel.paint(g);
        g.dispose();
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < 640; y += 4) {
            for (int x = 0; x < 900; x += 4) {
                colors.add(image.getRGB(x, y));
            }
        }
        check(colors.size() > 300, "the board is painted from the new angle (" + colors.size() + " colours)");
        System.out.println("PASS - the board is painted from the new angle");
        System.out.println("All camera angle checks passed.");
        System.exit(0);
    }
}
