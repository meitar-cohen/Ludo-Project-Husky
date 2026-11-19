package Tests;

import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;
import Model.Game;
import Model.Player;
import Model.SurpriseType;
import View.BoardPanel;

/** בדיקת סנכרון מודל-לוח להפתעות שלא דורשות משחק ידני. */
public final class SurpriseBoardSyncCheck {
    private static void check(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }

    private static final class Setup {
        final Game game = new Game(new Player[]{new Player("A", "Green"),
                new Player("B", "Red")}, "Medium");
        final MovementController movement = new MovementController(game);
        final GameController controller = new GameController(game, movement);
        final BoardPanel board = new BoardPanel(controller);

        Setup() {
            game.setStage(Stage.TAKE_OUT);
            check(board.placeActivePieceOnStart(0), "View failed to take out piece");
            check(movement.takeOutActivePiece(0), "Model failed to take out piece");
            game.setStage(Stage.BUSY);
        }
    }

    public static void main(String[] args) {
        Setup castle = new Setup();
        castle.controller.getSurpriseController().apply(
                castle.controller.getSurpriseController().resolveForPiece(
                        SurpriseType.RETURN_TO_CASTLE, 0, 64), 0, 64, -1, 0);
        check(castle.board.showActivePieceReturnedToCastle(0),
                "View could not return piece to castle");
        check(castle.board.hasPieceInCastle()
                && castle.game.getPiecesInCastle() == 4,
                "Castle state out of sync");
        System.out.println("PASS - castle model and board synchronization");

        Setup finish = new Setup();
        finish.controller.getSurpriseController().apply(
                finish.controller.getSurpriseController().resolveForPiece(
                        SurpriseType.FINISH_PIECE, 0, 64), 0, 64, -1, 0);
        check(finish.board.showActivePieceFinished(0),
                "View could not finish piece");
        check(finish.board.isActivePieceFinished(0)
                && finish.game.getPlayer(0).getPiece(0).isFinished(),
                "Finish state out of sync");
        System.out.println("PASS - finish model and board synchronization");
        System.out.println("All surprise board synchronization checks passed.");
    }
}