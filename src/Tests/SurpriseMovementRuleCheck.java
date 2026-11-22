package Tests;

import Controller.MovementController;
import Enums.Stage;
import Model.Game;
import Model.Player;

/** בדיקת שינוי מצבו של כלי שהופעל עליו אפקט הפתעה. */
public final class SurpriseMovementRuleCheck {
    private static void check(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }

    public static void main(String[] args) {
        Game castle = new Game(new Player[]{new Player("A", "Green"),
                new Player("B", "Red")}, "Medium");
        MovementController movement = new MovementController(castle);
        castle.getPlayer(0).getPiece(0).setOnBoard(true);
        castle.getPlayer(0).getPiece(0).setSteps(11);
        castle.setPiecesInCastle(3);
        castle.setStage(Stage.BUSY);
        check(movement.returnPieceToCastleFromSurprise(0), "Return failed");
        check(castle.getPiecesInCastle() == 4 &&
                !castle.getPlayer(0).getPiece(0).isOnBoard(), "Castle desync");
        System.out.println("PASS - piece returns to castle with correct count");

        Game finish = new Game(new Player[]{new Player("A", "Green"),
                new Player("B", "Red")}, "Hard");
        MovementController finishing = new MovementController(finish);
        finish.getPlayer(0).getPiece(0).setOnBoard(true);
        finish.setStage(Stage.BUSY);
        check(finishing.finishPieceFromSurprise(0), "Direct finish failed");
        check(finish.getPlayer(0).getPiece(0).isFinished(), "Piece not finished");
        check(!finishing.finishPieceFromSurprise(0), "Already finished piece finished twice");
        System.out.println("PASS - direct finish is applied once");
        System.out.println("All surprise movement rule checks passed.");
    }
}