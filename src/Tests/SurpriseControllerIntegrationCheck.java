package Tests;

import Controller.MovementController;
import Controller.SurpriseController;
import Controller.SurpriseResolution;
import Controller.TurnController;
import Enums.Stage;
import Model.Game;
import Model.Piece;
import Model.Player;
import Model.SurprisePlan;
import Model.SurpriseType;

/** בדיקות אינטגרציה של שש ההפתעות ללא הפעלת משחק גרפי. */
public final class SurpriseControllerIntegrationCheck {
    private static void assertThat(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class Setup {
        Game game = new Game(new Player[]{new Player("A", "Green"),
                new Player("B", "Red")}, "Medium");
        MovementController move = new MovementController(game);
        TurnController turns = new TurnController(game, move);
        SurpriseController surprises = new SurpriseController(game, move, turns);
        Setup() {
            game.getPlayer(0).getPiece(0).setOnBoard(true);
            game.getPlayer(0).getPiece(0).setSteps(8);
            game.setPiecesInCastle(3);
            game.setStage(Stage.BUSY);
        }
        SurprisePlan plan(SurpriseType type) {
            return surprises.resolveForPiece(type, 0, 52);
        }
        SurpriseResolution apply(SurpriseType type) {
            return surprises.apply(plan(type), 0, 52, -1, 0);
        }
    }

    public static void main(String[] args) {
        Setup back = new Setup();
        SurpriseResolution backResult = back.apply(SurpriseType.MOVE_BACK_THREE);
        assertThat(back.game.getPlayer(0).getPiece(0).getSteps() == 5,
                "Move-back strategy was not applied");
        assertThat(backResult.getAfterStep() == 5, "Wrong view position");
        System.out.println("PASS - move back 3 squares and view target");

        Setup castle = new Setup();
        castle.apply(SurpriseType.RETURN_TO_CASTLE);
        Piece returned = castle.game.getPlayer(0).getPiece(0);
        assertThat(!returned.isOnBoard() && castle.game.getPiecesInCastle() == 4,
                "Castle count/state is out of sync");
        System.out.println("PASS - return to castle and piece count");

        Setup finish = new Setup();
        finish.apply(SurpriseType.FINISH_PIECE);
        assertThat(finish.game.getPlayer(0).getPiece(0).isFinished(),
                "Direct finish was not applied");
        System.out.println("PASS - direct finish");

        Setup skip = new Setup();
        skip.apply(SurpriseType.SKIP_NEXT_TURN);
        assertThat(skip.game.isSkipNextTurnPending(0), "Skip was not recorded");
        skip.turns.passTurn(); // שחקן B מקבל תור
        assertThat(skip.game.getCurrentPlayer() == 1, "Expected player B");
        skip.turns.passTurn(); // שחקן A מדולג, B שוב
        assertThat(skip.game.getCurrentPlayer() == 1
                && !skip.game.isSkipNextTurnPending(0), "Next turn was not skipped once");
        System.out.println("PASS - skip next turn exactly once");

        Setup bonus = new Setup();
        bonus.apply(SurpriseType.EXTRA_TURN_ULTRABOOST);
        assertThat(bonus.turns.hasExtraTurn(), "Bonus turn was not requested");
        bonus.turns.grantExtraTurn();
        assertThat(bonus.turns.isUltraBoostRollPending()
                && bonus.turns.getBoostDieFaces() == 6, "UltraBoost is not active");
        assertThat(!bonus.turns.registerRollAndCheckThirdSix(6),
                "Bonus roll was wrongly rejected");
        bonus.turns.finishRoll(6, 3);
        assertThat(!bonus.turns.isUltraBoostRollPending(), "Bonus did not expire");
        System.out.println("PASS - immediate UltraBoost roll bypasses third-six penalty");

        Setup enemy = new Setup();
        enemy.game.getPlayer(1).getPiece(1).setOnBoard(true);
        enemy.game.getPlayer(1).getPiece(1).setSteps(10);
        assertThat(enemy.surprises.eligibleOpponents().contains(1),
                "Eligible opponent was not found");
        SurpriseResolution enemyResult = enemy.surprises.apply(
                enemy.plan(SurpriseType.MOVE_OPPONENT), 0, 52, 1, 5);
        assertThat(enemy.game.getPlayer(1).getPiece(1).getSteps() == 7
                && enemyResult.getAfterStep() == 7,
                "Opponent should move back ceil(5/2)=3 squares");
        assertThat(enemy.game.getCurrentPlayer() == 0, "Opponent changed active player");
        System.out.println("PASS - opponent moves ceil(die/2) backward");

        Setup noEnemy = new Setup();
        SurpriseResolution noTarget = noEnemy.apply(SurpriseType.MOVE_OPPONENT);
        assertThat(noTarget.getTitle().contains("NO TARGET"),
                "No-opponent edge case not handled");
        System.out.println("PASS - no opponent on the board");
        System.out.println("All surprise controller integration checks passed.");
    }
}