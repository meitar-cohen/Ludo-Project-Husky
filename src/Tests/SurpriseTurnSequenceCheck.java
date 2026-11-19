package Tests;

import Controller.MovementController;
import Controller.TurnController;
import Model.Game;
import Model.Player;

/** בודק רצפי תורות והפתעות ללא תצוגה גרפית. */
public final class SurpriseTurnSequenceCheck {

    private SurpriseTurnSequenceCheck() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // שני שחקנים שונים מפסידים תור: העונשים נצרכים פעם אחת.
        Game game = new Game(new Player[]{
                new Player("A", "Green"),
                new Player("B", "Red"),
                new Player("C", "Blue")
        }, "Medium");
        TurnController turns = new TurnController(game, new MovementController(game));
        turns.markSkipNextTurn(1);
        turns.markSkipNextTurn(2);
        turns.passTurn();
        check(game.getCurrentPlayer() == 0
                && !game.isSkipNextTurnPending(1)
                && !game.isSkipNextTurnPending(2),
                "Two skipped turns were not consumed correctly");
        turns.passTurn();
        check(game.getCurrentPlayer() == 1,
                "Player B did not return after the skipped turn");
        System.out.println("PASS - consecutive skipped turns are consumed once");

        // אם שחקן חייב להפסיד תור וזכאי לתור נוסף בגלל 6, התור הנוסף מתבטל.
        Game sixGame = new Game(new Player[]{
                new Player("A", "Green"), new Player("B", "Red")
        }, "Medium");
        TurnController sixTurns = new TurnController(sixGame,
                new MovementController(sixGame));
        sixTurns.markSkipNextTurn(0);
        sixGame.setLastStepDie(6);
        check(!sixTurns.hasExtraTurn(), "Skipped bonus turn should not be granted");
        sixTurns.passTurn();
        check(sixGame.getCurrentPlayer() == 1
                && !sixGame.isSkipNextTurnPending(0),
                "Skipped bonus turn was not consumed");
        System.out.println("PASS - skipped turn cancels a normal six bonus");

        // UltraBoost חל על הטלה אחת, מאפשר טווח 1..6 ולא מפעיל עונש של שלוש שישיות.
        Game boostGame = new Game(new Player[]{
                new Player("A", "Green"), new Player("B", "Red")
        }, "Medium");
        TurnController boostTurns = new TurnController(boostGame,
                new MovementController(boostGame));
        check(boostTurns.getBoostDieFaces() == 3,
                "Medium game should normally have a three-face boost die");
        boostTurns.requestSurpriseExtraTurn();
        check(boostTurns.hasExtraTurn(), "Surprise bonus was not requested");
        boostTurns.grantExtraTurn();
        check(boostTurns.isUltraBoostRollPending()
                && boostTurns.getBoostDieFaces() == 6,
                "Temporary UltraBoost did not activate");
        boostGame.setConsecutiveSixes(2);
        check(!boostTurns.registerRollAndCheckThirdSix(6),
                "UltraBoost roll incorrectly caused a third-six penalty");
        boostTurns.finishRoll(4, 5);
        check(boostGame.getPendingSteps() == 16
                && !boostTurns.isUltraBoostRollPending()
                && boostTurns.getBoostDieFaces() == 3,
                "UltraBoost did not expire after one roll");
        System.out.println("PASS - UltraBoost is temporary and ignores third-six penalty");

        System.out.println("All surprise turn sequence checks passed.");
    }
}