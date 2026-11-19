package Controller;

import java.util.Random;

import Enums.RollAction;
import Enums.Stage;
import Model.Die;
import Model.Game;

/** Manages dice rolls, turn phases, and transitions between players. */
public class TurnController {
    private final Game game;
    private final MovementController pieces;
    private final Random random = new Random();
    private final Die stepDie = new Die(6);
    private final Die boostDie;
    private final Die ultraBoostDie = new Die(6);
    private boolean surpriseExtraTurnPending;
    private boolean ultraBoostOnNextRoll;

    // בקשה לתור נוסף בעקבות הפתעה
    public void requestSurpriseExtraTurn() {
        surpriseExtraTurnPending = true;
    }

    // האם ההטלה הבאה משתמשת ב-UltraBoost
    public boolean isUltraBoostRollPending() {
        return ultraBoostOnNextRoll;
    }

    // סימון הפסד התור הבא
    public void markSkipNextTurn(int playerIndex) {
        game.markSkipNextTurn(playerIndex);
    }

    public TurnController(Game game, MovementController pieces) {
        this.game = game;
        this.pieces = pieces;
        if ("Hard".equals(game.getDifficulty())) {
            this.boostDie = new Die(6);
        } else if ("Medium".equals(game.getDifficulty())) {
            this.boostDie = new Die(3);
        } else {
            this.boostDie = null;
        }
    }

    public boolean canRoll() {
        return game.getStage() == Stage.ROLL;
    }

    public boolean consumeFirstTurnPulse() {
        if (!game.isFirstTurnPulsePending()) {
            return false;
        }
        game.setFirstTurnPulsePending(false);
        return true;
    }

    public int rollStepDie() {
        return rollDie(stepDie);
    }

    public int rollBoostDie() {
        if (ultraBoostOnNextRoll) {
            return rollDie(ultraBoostDie);
        }
        return boostDie == null ? 0 : rollDie(boostDie);
    }

    public int getBoostDieFaces() {
        if (ultraBoostOnNextRoll) {
            return 6;
        }
        return boostDie == null ? 0 : boostDie.getFaces();
    }

    private int rollDie(Die die) {
        die.setValue(random.nextInt(die.getFaces()) + 1);
        return die.getValue();
    }

    public boolean registerRollAndCheckThirdSix(int stepDie) {
        game.setLastStepDie(stepDie);

        // תור UltraBoost אינו נפסל בגלל שלוש שישיות
        if (ultraBoostOnNextRoll) {
            game.setConsecutiveSixes(0);
            return false;
        }

        if (stepDie != 6) {
            game.setConsecutiveSixes(0);
            return false;
        }
        game.setConsecutiveSixes(game.getConsecutiveSixes() + 1);
        if (game.getConsecutiveSixes() >= 3) {
            game.setConsecutiveSixes(0);
            return true;
        }
        return false;
    }

    public boolean hasExtraTurn() {
        if (game.isSkipNextTurnPending(game.getCurrentPlayer())) {
            return false;
        }

        return surpriseExtraTurnPending
                || game.getLastStepDie() == 6;
    }

    public void grantExtraTurn() {
        if (surpriseExtraTurnPending) {
            surpriseExtraTurnPending = false;
            ultraBoostOnNextRoll = true;
            game.setConsecutiveSixes(0);
        }

        game.setStage(Stage.ROLL);
    }

    public RollAction finishRoll(int stepDie, int boostDie) {
        game.setPendingSteps(calculateSteps(stepDie, boostDie));

        // UltraBoost תקף להטלה אחת בלבד
        ultraBoostOnNextRoll = false;
        if (stepDie == 6 && game.hasPieceInCastle()) {
            game.setStage(Stage.TAKE_OUT);
            return RollAction.TAKE_OUT;
        }
        if ((game.getPendingSteps() > 0 && game.getPlayer(game.getCurrentPlayer()).hasPieceOnBoard())
                || pieces.hasAvailableHomePathMove(stepDie)) {
            game.setStage(Stage.MOVE);
            return RollAction.MOVE;
        }
        game.setStage(Stage.ROLL);
        return RollAction.NEED_SIX;
    }

    public int calculateSteps(int stepDie, int boostDie) {
        if ("Easy".equals(game.getDifficulty())) {
            return stepDie;
        }
        return stepDie * (boostDie - 1);
    }

    public void passTurn() {

        // צריכת עונש דילוג על תור נוסף שנובע משישייה
        if (game.getLastStepDie() == 6
                && game.isSkipNextTurnPending(game.getCurrentPlayer())) {
            game.consumeSkippedTurn(game.getCurrentPlayer());
        }

        game.setConsecutiveSixes(0);
        surpriseExtraTurnPending = false;
        ultraBoostOnNextRoll = false;

        int total = game.getPlayerCount();
        int active = 0;

        for (int player = 0; player < total; player++) {
            if (!game.isPlayerFinished(player)) {
                active++;
            }
        }

        if (active <= 1) {
            game.setStage(Stage.DONE);
            return;
        }

        // חיפוש השחקן הבא תוך דילוג על שחקנים עם עונש
        for (int offset = 1; offset <= total; offset++) {
            int candidate =
                    (game.getCurrentPlayer() + offset) % total;

            if (!game.isPlayerFinished(candidate)) {
                if (game.consumeSkippedTurn(candidate)) {
                    continue;
                }

                game.setCurrentPlayer(candidate);
                game.setStage(Stage.ROLL);
                return;
            }
        }

        // כאשר כולם דילגו, חוזרים לשחקן הזמין הראשון
        for (int offset = 1; offset <= total; offset++) {
            int candidate =
                    (game.getCurrentPlayer() + offset) % total;

            if (!game.isPlayerFinished(candidate)) {
                game.setCurrentPlayer(candidate);
                game.setStage(Stage.ROLL);
                return;
            }
        }
    }
}
