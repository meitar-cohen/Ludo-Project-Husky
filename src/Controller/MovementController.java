package Controller;

import java.awt.Color;
import Enums.Stage;
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
                && !game.isPieceOnBoard(game.getCurrentPlayer(), slot)
                // a piece that already reached the finish is not in the castle
                && !game.isPieceFinished(game.getCurrentPlayer(), slot);
    }

    public boolean takeOutActivePiece(int slot) {
        if (!canTakeOutActivePiece(slot) || !game.selectPiece(slot)) {
            return false;
        }
        game.setPiecesInCastle(game.getPiecesInCastle() - 1);
        game.setPieceOnBoard(true);
        game.setPieceStep(0);
        game.setPieceInInnerPath(false);
        game.setPieceInMiddlePath(false);
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

    /**
     * SRS 3.2.4.3.2 - a piece on an arrow of its own colour moves to the next inner path:
     * Medium: outer path (64) -> inner path (52).
     * Hard:   outer path (72) -> middle path (64) -> inner path (52).
     * On the inner path the arrow does nothing (the piece continues to its home path).
     */
    public boolean enterInnerPathFromArrow(Color arrowColor, Color pieceColor,
                                           int innerPathStartStep) {
        if (isEasyGame() || !isMatchingArrow(arrowColor, pieceColor)
                || !game.hasPieceOnBoard() || game.isPieceInHomePath()
                || game.isPieceInInnerPath() || innerPathStartStep < 0) {
            return false;
        }
        if (isHardGame() && !game.isPieceInMiddlePath()) {
            game.setPieceInMiddlePath(true);
        } else {
            game.setPieceInInnerPath(true);
        }
        game.setPieceInHomePath(false);
        game.setPieceStep(innerPathStartStep);
        return true;
    }

    /** SRS 3.2.4.3.2 - the same rule for a given piece of the current player. */
    public boolean enterNextPathFromArrow(int pieceIndex, Color arrowColor,
                                          Color pieceColor) {
        return game.selectPiece(pieceIndex)
                && enterInnerPathFromArrow(arrowColor, pieceColor, 0);
    }

    /** True when the piece is on a path that still has an arrow to the next path. */
    public boolean canUseArrows(int pieceIndex) {
        return !isEasyGame()
                && game.selectPiece(pieceIndex)
                && game.hasPieceOnBoard()
                && !game.isPieceInHomePath()
                && !game.isPieceInInnerPath();
    }

    public boolean isPieceInMiddlePath(int pieceIndex) {
        return game.selectPiece(pieceIndex) && game.isPieceInMiddlePath();
    }

    public boolean isPieceInInnerPath(int pieceIndex) {
        return game.selectPiece(pieceIndex) && game.isPieceInInnerPath();
    }

    /**
     * SRS 3.2.4.3.2 - the arrow is used when the piece passes over it OR stops on it.
     * Returns the step of the piece's own arrow if this move reaches it, otherwise -1.
     *
     * @param ownArrowStep step of the piece's own-colour arrow on its current path (-1 if none)
     */
    public int findArrowOnMove(int pieceIndex, int ownArrowStep) {
        if (game.getStage() != Stage.MOVE || !canUseArrows(pieceIndex)) {
            return -1;
        }
        return arrowWithinReach(game.getPieceStep(),
                getMoveStepsForSelectedPiece(), ownArrowStep);
    }

    /**
     * SRS 3.2.4.3.2 - after moving to a new path the piece continues with the steps
     * that are left. Returns the step of the next own arrow if these steps reach it
     * (Hard: the arrow of the middle path), otherwise -1.
     */
    public int findArrowOnRemainingSteps(int pieceIndex, int remainingSteps,
                                         int ownArrowStep) {
        if (!canUseArrows(pieceIndex)) {
            return -1;
        }
        return arrowWithinReach(game.getPieceStep(), remainingSteps, ownArrowStep);
    }

    private static int arrowWithinReach(int currentStep, int steps, int arrowStep) {
        if (steps <= 0 || arrowStep < 0 || currentStep < 0) {
            return -1;
        }
        return arrowStep > currentStep && arrowStep <= currentStep + steps
                ? arrowStep : -1;
    }

    /**
     * SRS 3.2.4.3.2 - continues the current move on the new path after an arrow
     * (the turn is still in progress).
     */
    public boolean continueMoveAfterArrow(int pieceIndex, int targetStep,
                                          int routeSize) {
        return applyEventMovement(pieceIndex, targetStep, routeSize);
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

    private boolean isHardGame() {
        return "Hard".equals(game.getDifficulty());
    }
}
