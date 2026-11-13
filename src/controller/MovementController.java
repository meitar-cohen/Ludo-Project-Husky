package controller;

import java.awt.Color;
import enums.Stage;
import Model.Game;
import Model.Piece;
import Model.Player;

/**
 * בקר פעולות וחוקי תנועה של כלים.
 */
public class MovementController {
    private final Game game;

    public MovementController(Game game) {
        this.game = game;
    }

    public boolean canTakeOutActivePiece(int slot) {
        return game.getStage() == Stage.TAKE_OUT
                && slot >= 0
                && slot < Game.PIECES_PER_PLAYER
                && game.hasPieceInCastle()
                && !game.isPieceOnBoard(game.getCurrentPlayer(), slot);
    }

    public boolean takeOutActivePiece(int slot) {
        if (!canTakeOutActivePiece(slot) || !game.selectPiece(slot)) {
            return false;
        }
        game.setPiecesInCastle(game.getPiecesInCastle() - 1);
        game.setPieceOnBoard(true);
        game.setPieceStep(0);
        game.setPieceInInnerPath(false);
        game.setPieceInHomePath(false);
        game.setStage(Stage.ROLL);
        return true;
    }

    public boolean canChooseBoardMoveInsteadOfTakeOut() {
        return game.getStage() == Stage.TAKE_OUT
                && game.getLastStepDie() == 6
                && game.hasAnyPieceOnBoard()
                && (game.getPendingSteps() > 0
                    || hasAvailableHomePathMove(game.getLastStepDie()));
    }

    public boolean chooseBoardMoveInsteadOfTakeOut() {
        if (!canChooseBoardMoveInsteadOfTakeOut()) {
            return false;
        }
        game.setStage(Stage.MOVE);
        return true;
    }

    public boolean hasPieceInCastle() {
        return game.hasPieceInCastle();
    }

    public boolean hasPieceOnBoard() {
        return game.hasAnyPieceOnBoard();
    }

    public int getActivePieceStep() {
        return game.getPieceStep();
    }

    public int getPieceStep(int pieceIndex) {
        if (!game.selectPiece(pieceIndex)) {
            return -1;
        }
        return game.getPieceStep();
    }

    public int getAnswerMoveTarget(boolean correct, int routeSize) {
        int change = correct ? 1 : -1;
        int target = getClockwiseTarget(game.getPieceStep(), change);
        return target >= 0 && target < routeSize
                ? target : game.getPieceStep();
    }

    public int getSurpriseMoveTarget(int routeSize) {
        int target = game.getPieceStep() - 3;
        return target >= 0 ? target : 0;
    }

    public int getMoveStepsForSelectedPiece() {
        return game.isPieceInHomePath()
                ? game.getLastStepDie() : game.getPendingSteps();
    }

    public boolean isPieceSelectionLegal(int pieceIndex) {
        if (game.getStage() != Stage.MOVE
                || !game.selectPiece(pieceIndex)
                || !game.hasPieceOnBoard()
                || game.isPieceFinished(game.getCurrentPlayer(), pieceIndex)) {
            return false;
        }
        return getMoveStepsForSelectedPiece() > 0;
    }

    public int validatePieceMove(int pieceIndex, int routeSize) {
        if (!isPieceSelectionLegal(pieceIndex)) {
            return -1;
        }
        int currentStep = game.getPieceStep();
        int steps = getMoveStepsForSelectedPiece();
        return isClockwiseMoveAllowed(currentStep, steps, routeSize)
                ? getClockwiseTarget(currentStep, steps) : -1;
    }

    public boolean movePiece(int pieceIndex, int targetStep, int routeSize) {
        if (!isPieceSelectionLegal(pieceIndex)
                || targetStep < 0 || targetStep >= routeSize) {
            return false;
        }
        game.setPieceStep(targetStep);
        game.setStage(Stage.BUSY);
        return true;
    }

    public boolean applyEventMovement(int pieceIndex, int targetStep,
                                      int routeSize) {
        if (game.getStage() != Stage.BUSY
                || !game.selectPiece(pieceIndex)
                || !game.hasPieceOnBoard()
                || targetStep < 0 || targetStep >= routeSize) {
            return false;
        }
        game.setPieceStep(targetStep);
        return true;
    }

    public boolean tryFinishPiece(int pieceIndex, int routeSize) {
        if (game.getStage() != Stage.MOVE
                || !game.selectPiece(pieceIndex)
                || !game.hasPieceOnBoard()
                || game.isPieceFinished(game.getCurrentPlayer(), pieceIndex)) {
            return false;
        }

        boolean inHome = game.isPieceInHomePath();
        int steps = inHome ? game.getLastStepDie() : game.getPendingSteps();
        int current = game.getPieceStep();
        if (steps <= 0 || current < 0) {
            return false;
        }

        if (inHome) {
            if (routeSize != 6 || current >= 6 || current + steps != 6) {
                return false;
            }
        } else if (routeSize != 52 || current >= routeSize
                || (!isEasyGame() && !game.isPieceInInnerPath())
                || current + steps != routeSize + 6) {
            return false;
        }

        if (!finishSelectedPiece()) {
            return false;
        }
        game.setStage(Stage.BUSY);
        return true;
    }

    private boolean finishSelectedPiece() {
        Player player = game.getPlayer(game.getCurrentPlayer());
        Piece piece = player.getPiece(game.getSelectedPieceIndex());
        if (!piece.isOnBoard() || piece.isFinished()) {
            return false;
        }

        piece.finish();
        if (player.getFinishedPieceCount() == Game.PIECES_PER_PLAYER) {
            game.setPlayerFinished(game.getCurrentPlayer(), true);
        }
        return true;
    }

    public boolean isCurrentPlayerFinished() {
        return game.isPlayerFinished(game.getCurrentPlayer());
    }

    public boolean isClockwiseMoveAllowed(int currentStep, int steps,
                                          int routeSize) {
        return currentStep >= 0 && steps >= 0
                && currentStep + steps < routeSize;
    }

    public int getClockwiseTarget(int currentStep, int steps) {
        return currentStep + steps;
    }

    public boolean isMatchingArrow(Color arrowColor, Color pieceColor) {
        return arrowColor != null && pieceColor != null
                && arrowColor.equals(pieceColor);
    }

    public boolean enterInnerPathFromArrow(Color arrowColor, Color pieceColor,
                                           int innerPathStartStep) {
        if (isEasyGame() || !isMatchingArrow(arrowColor, pieceColor)
                || !game.hasPieceOnBoard() || innerPathStartStep < 0) {
            return false;
        }
        game.setPieceInInnerPath(true);
        game.setPieceInHomePath(false);
        game.setPieceStep(innerPathStartStep);
        return true;
    }

    public boolean hasAnyLegalMove(int[] routeSizes) {
        if ((game.getStage() != Stage.MOVE
                && game.getStage() != Stage.TAKE_OUT)
                || routeSizes == null
                || routeSizes.length != Game.PIECES_PER_PLAYER) {
            return false;
        }

        int player = game.getCurrentPlayer();
        for (int piece = 0; piece < Game.PIECES_PER_PLAYER; piece++) {
            if (!game.isPieceOnBoard(player, piece)
                    || game.isPieceFinished(player, piece)) {
                continue;
            }
            boolean inHome = game.isPieceInHomePath(player, piece);
            int steps = inHome ? game.getLastStepDie() : game.getPendingSteps();
            int current = game.getPieceStep(player, piece);
            int routeSize = routeSizes[piece];
            if (steps <= 0 || current < 0 || routeSize <= 0
                    || current >= routeSize) {
                continue;
            }
            int destination = current + steps;
            if (inHome && destination <= 6) {
                return true;
            }
            if (!inHome && destination < routeSize) {
                return true;
            }
            if (!inHome && routeSize == 52
                    && (isEasyGame() || game.isPieceInInnerPath(player, piece))
                    && destination <= routeSize + 6) {
                return true;
            }
        }
        return false;
    }

    public boolean tryEnterHomePath(int pieceIndex, int sharedRouteSize) {
        if (game.getStage() != Stage.MOVE || sharedRouteSize <= 0
                || !game.selectPiece(pieceIndex) || !game.hasPieceOnBoard()
                || game.isPieceInHomePath()) {
            return false;
        }
        if (!isEasyGame() && !game.isPieceInInnerPath()) {
            return false;
        }
        int steps = game.getPendingSteps();
        int destination = game.getPieceStep() + steps;
        if (steps <= 0 || destination < sharedRouteSize
                || destination >= sharedRouteSize + 6) {
            return false;
        }
        game.setPieceInHomePath(true);
        game.setPieceStep(destination - sharedRouteSize);
        game.setStage(Stage.BUSY);
        return true;
    }

    public boolean hasAvailableHomePathMove(int stepDie) {
        int player = game.getCurrentPlayer();
        for (int pieceIndex = 0; pieceIndex < Game.PIECES_PER_PLAYER;
                pieceIndex++) {
            if (game.isPieceOnBoard(player, pieceIndex)
                    && game.isPieceInHomePath(player, pieceIndex)
                    && !game.isPieceFinished(player, pieceIndex)
                    && game.getPieceStep(player, pieceIndex) + stepDie <= 6) {
                return true;
            }
        }
        return false;
    }

    private boolean isEasyGame() {
        return "Easy".equals(game.getDifficulty());
    }
}
