package Tests;
import java.awt.Point;
import java.util.List;

import Controller.GameController;
import Controller.MovementController;
import Enums.SquareType;
import Model.Game;
import View.BoardPanel;

public class BoardRouteStructureCheck {

    public static void main(String[] args) {

        String[] colors = {"Red", "Blue", "Green", "Yellow"};

        // בדיקה של כל המסלולים בכל רמת קושי
        check("Easy", new int[]{52}, colors);
        check("Medium", new int[]{52, 64}, colors);
        check("Hard", new int[]{52, 64, 72}, colors);
        checkModelOwnership(colors);
    }

    private static void check(
            String difficulty, int[] expected, String[] colors) {

        // יצירת לוח ברמת הקושי הנבדקת
        BoardPanel board = new BoardPanel(
                TestPlayers.gameControllerForBoard(difficulty, colors));

        // בדיקת מספר המסלולים
        if (board.getSharedRouteCount() != expected.length) {
            throw new AssertionError(
                difficulty + ": incorrect number of routes"
            );
        }

        // בדיקת מספר המשבצות בכל מסלול
        for (int i = 0; i < expected.length; i++) {

            int actual = board.getSharedRouteSize(i);

            if (actual != expected[i]) {
                throw new AssertionError(
                    difficulty + " route " + i
                    + ": expected " + expected[i]
                    + ", got " + actual
                );
            }
        }

        System.out.println("PASS - " + difficulty);
    }

    private static void checkModelOwnership(String[] colors) {
        Game game = new Game(TestPlayers.of(colors, colors), "Medium");
        GameController controller =
                new GameController(game, new MovementController(game));
        GameController other =
                TestPlayers.gameControllerForBoard("Medium", colors);

        List<List<Point>> routes = controller.getBoardSharedRoutes();
        Point original = new Point(routes.get(0).get(0));
        routes.get(0).get(0).translate(50, 50);
        routes.get(1).clear();
        if (!controller.getBoardSharedRoutes().get(0).get(0).equals(original)
                || controller.getBoardSharedRoutes().get(1).size() != 64) {
            throw new AssertionError("Board routes must be independent copies");
        }

        Point position = new Point(original);
        controller.addSpecialSquare(position, SquareType.STAR, null, 0);
        position.translate(50, 50);
        if (game.getBoard().getSpecialSquare(original) == null
                || controller.getSpecialSquareType(original) != SquareType.STAR
                || other.hasSpecialSquare(original)) {
            throw new AssertionError("Each game must own its board squares");
        }
        controller.getSpecialSquarePositions().get(0).translate(50, 50);
        if (!controller.hasSpecialSquare(original)) {
            throw new AssertionError("Square positions must be independent copies");
        }
        System.out.println("PASS - board model ownership and defensive copies");
    }
}