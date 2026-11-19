package Tests;

import Controller.MovementController;
import Controller.TurnController;
import Model.Game;
import Model.Player;

/** בדיקת תור נוסף, דילוג על תור ו-UltraBoost. */
public final class SurpriseTurnRulesCheck {
    private static void check(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }

    public static void main(String[] args) {
        Game game = new Game(new Player[]{new Player("A", "Green"),
                new Player("B", "Red"), new Player("C", "Blue")}, "Medium");
        TurnController turns = new TurnController(game, new MovementController(game));
        turns.markSkipNextTurn(1);
        turns.passTurn();
        check(game.getCurrentPlayer() == 2 && !game.isSkipNextTurnPending(1),
                "Player B must be skipped exactly once");
        turns.passTurn();
        check(game.getCurrentPlayer() == 0, "Player A expected");
        turns.passTurn();
        check(game.getCurrentPlayer() == 1, "Player B must return after skip");
        System.out.println("PASS - skipped turn is consumed exactly once");

        turns.requestSurpriseExtraTurn();
        check(turns.hasExtraTurn(), "Extra turn is not queued");
        turns.grantExtraTurn();
        check(turns.isUltraBoostRollPending() && turns.getBoostDieFaces() == 6,
                "UltraBoost must be 1..6");
        game.setConsecutiveSixes(2);
        check(!turns.registerRollAndCheckThirdSix(6),
                "Surprise turn must ignore consecutive six limit");
        turns.finishRoll(4, 6);
        check(game.getPendingSteps() == 20 && !turns.isUltraBoostRollPending(),
                "Medium game should use temporary UltraBoost then reset");
        System.out.println("PASS - temporary UltraBoost and third-six exemption");
        System.out.println("All surprise turn rule checks passed.");
    }
}