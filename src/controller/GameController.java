package controller;

import java.awt.Color;
import java.util.Random;
import enums.RollAction;
import enums.Stage;
import model.Game;

public class GameController {
    private final Game game;
    private final Random random = new Random();

    public GameController(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null");
        }
        this.game = game;
    }

    public Stage getStage() {
        return game.getStage();
    }

    public void setStage(Stage stage) {
        game.setStage(stage);
    }

    public int getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public int getPendingSteps() {
        return game.getPendingSteps();
    }

    public boolean consumeFirstTurnPulse() {
        if (!game.isFirstTurnPulsePending()) return false;
        game.setFirstTurnPulsePending(false);
        return true;
    }

    public boolean canRoll() {
        return game.getStage() == Stage.ROLL;
    }

    public boolean isInTakeOutStage() {
        return game.getStage() == Stage.TAKE_OUT;
    }

    public boolean isInMoveStage() {
        return game.getStage() == Stage.MOVE;
    }

    public boolean isEasyGame() {
        return "Easy".equals(game.getDifficulty());
    }

    public boolean canTakeOutActivePiece(int slot) {
        return slot >= 0 && slot < 4 && game.hasPieceInCastle() && game.getStage() == Stage.TAKE_OUT;
    }

    public void takeOutActivePiece(int slot) {
        if (!canTakeOutActivePiece(slot)) return;
        takePieceOutOfCastle();
        game.setPiecesInCastle(game.hasPieceInCastle()
            ? game.getPiecesInCastle() - 1 : 0);
        game.setPieceOnBoard(true);
        game.setPieceStep(0);
        game.setPieceInHomePath(false);
        game.setStage(Stage.ROLL);
    }

    public boolean hasPieceInCastle() {
        return game.hasPieceInCastle();
    }

    public boolean hasPieceOnBoard() {
        return game.hasPieceOnBoard();
    }

    public int getActivePieceStep() {
        return game.getPieceStep();
    }

    public int getMoveTarget(int routeSize) {
        return isClockwiseMoveAllowed(game.getPieceStep(), game.getPendingSteps(), routeSize)
            ? getClockwiseTarget(game.getPieceStep(), game.getPendingSteps())
            : -1;
    }

    public boolean moveActivePiece(int targetStep, int routeSize) {
        game.setStage(Stage.BUSY);
        if (!game.hasPieceOnBoard() || targetStep < 0 || targetStep >= routeSize) return false;
        game.setPieceStep(targetStep);
        return true;
    }

    public int getAnswerMoveTarget(boolean correct, int routeSize) {
        int change = correct ? 1 : -1;
        return getClockwiseTarget(game.getPieceStep(), change) >= 0
            && getClockwiseTarget(game.getPieceStep(), change) < routeSize
                ? getClockwiseTarget(game.getPieceStep(), change) : game.getPieceStep();
    }

    public int getSurpriseMoveTarget(int routeSize) {
        int target = game.getPieceStep() - 3;
        return target >= 0 ? target : 0;
    }

    public void finishGame() {
        game.setStage(Stage.DONE);
    }

    public String getCurrentPlayerName() {
        return game.getCurrentPlayerName();
    }

    public String getCurrentPlayerColor() {
        return game.getCurrentPlayerColor();
    }

    public boolean isLegalPieceSelection(boolean pieceOnBoard, boolean pieceInCastle) {
        return pieceOnBoard || (pieceInCastle && game.getPendingSteps() == 6);
    }

    public boolean isClockwiseMoveAllowed(int currentStep, int steps, int routeSize) {
        return currentStep >= 0 && steps >= 0 && currentStep + steps < routeSize;
    }

    public int getClockwiseTarget(int currentStep, int steps) {
        return currentStep + steps;
    }

    public boolean isMatchingArrow(Color color, Color pieceColor) {
        return color != null && color.equals(pieceColor);
    }

    public void setPendingSteps(int pendingSteps) {
        game.setPendingSteps(pendingSteps);
    }

    public int rollStepDie() {
        return random.nextInt(6) + 1;
    }

    /**
     * SRS 3.2.3.3.2 (Daniel Azulay) - the bonus die of the level:
     * Easy - none (0), Medium - Boost 1-3, Hard - UltraBoost 1-6.
     */
    public int rollBoostDie() {
        int faces = getBoostDieFaces();
        return faces == 0 ? 0 : random.nextInt(faces) + 1;
    }

    /** SRS 3.2.3.3.2 - number of faces of the bonus die: 0 (Easy), 3 (Boost), 6 (UltraBoost). */
    public int getBoostDieFaces() {
        if ("Hard".equals(game.getDifficulty())) return 6;
        if ("Medium".equals(game.getDifficulty())) return 3;
        return 0;
    }

    /**
     * SRS 3.2.3.3.5 (Daniel Azulay) - records the step die of a roll and counts sixes in a row.
     *
     * @return true when this roll is the third 6 in a row - the roll is cancelled
     *         and the turn should pass
     */
    public boolean registerRollAndCheckThirdSix(int stepDie) {
        game.setLastStepDie(stepDie);
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

    /** SRS 3.2.3.3.5 (Daniel Azulay) - a 6 on the step die gives the same player another turn. */
    public boolean hasExtraTurn() {
        return game.getLastStepDie() == 6;
    }

    /** SRS 3.2.1.3.3 (Daniel Azulay) - game clock as HH:MM:SS. */
    public String getElapsedTime() {
        long seconds = game.getElapsedSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    public long getElapsedSeconds() {
        return game.getElapsedSeconds();
    }

    public RollAction finishRoll(int stepDie, int boostDie) {
        calculateSteps();
        game.setPendingSteps(stepDie);
        if (game.getPendingSteps() == 6 && game.hasPieceInCastle()) {
            game.setStage(Stage.TAKE_OUT);
            return RollAction.TAKE_OUT;
        }
        if (game.hasPieceOnBoard()) {
            game.setStage(Stage.MOVE);
            return RollAction.MOVE;
        }
        game.setStage(Stage.ROLL);
        return RollAction.NEED_SIX;
    }

    public void calculateSteps() {
        return;
    }

    public void takePieceOutOfCastle() {
        return;
    }

    public void handleNoLegalMove() {
        return;
    }

    public void enterHomePath() {
        return;
    }

    public void takeOutPiece() {
        takePieceOutOfCastle();
        game.setStage(Stage.ROLL);
    }

    public void passTurn() {
        int nextPlayer = (game.getCurrentPlayer() + 1) % game.getPlayerCount();
        game.setCurrentPlayer(nextPlayer);
        game.setStage(Stage.ROLL);
    }
}
