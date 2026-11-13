package controller;

import java.awt.Color;
import enums.RollAction;
import enums.Stage;
import Model.Game;
import Model.Player;

/**
 * מתאם תאימות לקוד קיים. מסך המשחק עובד ישירות מול הבקרים הייעודיים.
 */
@Deprecated
public class GameController {
    private final Game game;
    private final HistoryController history;
    private final MovementController pieces;
    private final TurnController turns;

    public GameController(Game game) {
        this(game, new HistoryController());
    }

    public GameController(Game game, HistoryController history) {
        if (game == null || history == null) {
            throw new IllegalArgumentException("Game and history are required");
        }
        this.game = game;
        this.history = history;
        this.pieces = new MovementController(game);
        this.turns = new TurnController(game, pieces);
    }

    public String saveGameToHistory() {
        turns.stopGameClock();
        return history.saveFinishedGame(game, turns);
    }

    public boolean isGameSavedToHistory() {
        return game.isSavedToHistory();
    }

    public Player[] getRankedPlayers() {
        return turns.getRanking();
    }

    public Stage getStage() {
        return game.getStage();
    }

    public int getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public int getPendingSteps() {
        return game.getPendingSteps();
    }

    public boolean consumeFirstTurnPulse() {
        return turns.consumeFirstTurnPulse();
    }

    public boolean canRoll() {
        return turns.canRoll();
    }

    public boolean isEasyGame() {
        return "Easy".equals(game.getDifficulty());
    }

    public boolean canTakeOutActivePiece(int slot) {
        return pieces.canTakeOutActivePiece(slot);
    }

    public boolean takeOutActivePiece(int slot) {
        return pieces.takeOutActivePiece(slot);
    }

    public boolean canChooseBoardMoveInsteadOfTakeOut() {
        return pieces.canChooseBoardMoveInsteadOfTakeOut();
    }

    public boolean chooseBoardMoveInsteadOfTakeOut() {
        return pieces.chooseBoardMoveInsteadOfTakeOut();
    }

    public boolean hasPieceInCastle() {
        return pieces.hasPieceInCastle();
    }

    public boolean hasPieceOnBoard() {
        return pieces.hasPieceOnBoard();
    }

    public int getPieceStep(int pieceIndex) {
        return pieces.getPieceStep(pieceIndex);
    }

    public int getMoveStepsForSelectedPiece() {
        return pieces.getMoveStepsForSelectedPiece();
    }

    public boolean isPieceSelectionLegal(int pieceIndex) {
        return pieces.isPieceSelectionLegal(pieceIndex);
    }

    public int validatePieceMove(int pieceIndex, int routeSize) {
        return pieces.validatePieceMove(pieceIndex, routeSize);
    }

    public boolean movePiece(int pieceIndex, int targetStep, int routeSize) {
        return pieces.movePiece(pieceIndex, targetStep, routeSize);
    }

    public boolean applyEventMovement(int pieceIndex, int targetStep,
                                      int routeSize) {
        return pieces.applyEventMovement(pieceIndex, targetStep, routeSize);
    }

    public boolean tryFinishPiece(int pieceIndex, int routeSize) {
        return pieces.tryFinishPiece(pieceIndex, routeSize);
    }

    public boolean isCurrentPlayerFinished() {
        return pieces.isCurrentPlayerFinished();
    }

    public boolean isMatchingArrow(Color arrowColor, Color pieceColor) {
        return pieces.isMatchingArrow(arrowColor, pieceColor);
    }

    public boolean enterInnerPathFromArrow(Color arrowColor, Color pieceColor,
                                           int innerPathStartStep) {
        return pieces.enterInnerPathFromArrow(
                arrowColor, pieceColor, innerPathStartStep);
    }

    public boolean hasAnyLegalMove(int[] routeSizes) {
        return pieces.hasAnyLegalMove(routeSizes);
    }

    public boolean tryEnterHomePath(int pieceIndex, int sharedRouteSize) {
        return pieces.tryEnterHomePath(pieceIndex, sharedRouteSize);
    }

    public int rollStepDie() {
        return turns.rollStepDie();
    }

    public int rollBoostDie() {
        return turns.rollBoostDie();
    }

    public int getBoostDieFaces() {
        return turns.getBoostDieFaces();
    }

    public boolean registerRollAndCheckThirdSix(int stepDie) {
        return turns.registerRollAndCheckThirdSix(stepDie);
    }

    public RollAction finishRoll(int stepDie, int boostDie) {
        return turns.finishRoll(stepDie, boostDie);
    }

    public int calculateSteps(int stepDie, int boostDie) {
        return turns.calculateSteps(stepDie, boostDie);
    }

    public boolean hasExtraTurn() {
        return turns.hasExtraTurn();
    }

    public void grantExtraTurn() {
        turns.grantExtraTurn();
    }

    public void startGameClock() {
        turns.startGameClock();
    }

    public String getElapsedTime() {
        return turns.getElapsedTime();
    }

    public long getElapsedSeconds() {
        return turns.getElapsedSeconds();
    }

    public void passTurn() {
        turns.passTurn();
    }
}
