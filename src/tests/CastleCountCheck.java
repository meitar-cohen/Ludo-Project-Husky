package tests;

import controller.GameController;
import enums.Stage;
import model.Game;

public class CastleCountCheck {

    public static void main(String[] args) {

        // יצירת משחק עם שני שחקנים
        Game game = new Game(TestPlayers.of(new String[] {"Blue", "Red"}, new String[] {"Blue", "Red"}), "Easy");

        GameController controller = new GameController(game);

        // בתחילת המשחק לכל שחקן ארבעה כלים בטירה
        check(game, 0, 4);
        check(game, 1, 4);

        // הוצאת כלי כחול מהטירה
        game.setCurrentPlayer(0);
        game.setStage(Stage.TAKE_OUT);
        controller.takeOutActivePiece(0);

        check(game, 0, 3);
        check(game, 1, 4);

        // הוצאת כלי אדום מהטירה
        game.setCurrentPlayer(1);
        game.setStage(Stage.TAKE_OUT);
        controller.takeOutActivePiece(0);

        check(game, 0, 3);
        check(game, 1, 3);

        // חזרה לתור הכחול
        game.setCurrentPlayer(0);

        if (game.getPiecesInCastle() != 3) {
            throw new AssertionError("Blue castle count changed");
        }

        System.out.println(
            "PASS - independent castle counts for both players"
        );
    }

    private static void check(Game game, int player, int expected) {

        int actual = game.getPiecesInCastle(player);

        if (actual != expected) {
            throw new AssertionError(
                "Player " + player + ": expected "
                + expected + ", got " + actual
            );
        }
    }
}
