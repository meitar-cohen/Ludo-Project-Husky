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
                && !game.getPlayer(game.getCurrentPlayer()).getPiece(slot).isOnBoard()
                // a piece that already reached the finish is not in the castle
                && !game.getPlayer(game.getCurrentPlayer()).getPiece(slot).isFinished();
    }

    public boolean takeOutActivePiece(int slot) {
        if (!canTakeOutActivePiece(slot) || !game.selectPiece(slot)) {
            return false;
        }
        game.setPiecesInCastle(game.getPiecesInCastle() - 1);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setSteps(0);
        game.getSelectedPiece().setInInnerPath(false);
        game.getSelectedPiece().setInMiddlePath(false);
        game.getSelectedPiece().setInHomePath(false);
        game.setStage(Stage.ROLL);
        return true;
    }

    public boolean canChooseBoardMoveInsteadOfTakeOut() {
        return game.getStage() == Stage.TAKE_OUT
                && game.getLastStepDie() == 6
                && game.getPlayer(game.getCurrentPlayer()).hasPieceOnBoard()
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
        return game.getPlayer(game.getCurrentPlayer()).hasPieceOnBoard();
    }

    public int getActivePieceStep() {
        return game.getSelectedPiece().getSteps();
    }

    public int getPieceStep(int pieceIndex) {
        if (!game.selectPiece(pieceIndex)) {
            return -1;
        }
        return game.getSelectedPiece().getSteps();
    }

    public int getAnswerMoveTarget(boolean correct, int routeSize) {
        int change = correct ? 1 : -1;
        int target = getClockwiseTarget(game.getSelectedPiece().getSteps(), change);
        return target >= 0 && target < routeSize
                ? target : game.getSelectedPiece().getSteps();
    }

    public int getSurpriseMoveTarget(int routeSize) {
        int target = game.getSelectedPiece().getSteps() - 3;
        return target >= 0 ? target : 0;
    }

    public int getMoveStepsForSelectedPiece() {
        return game.getSelectedPiece().isInHomePath()
                ? game.getLastStepDie() : game.getPendingSteps();
    }

    public boolean isPieceSelectionLegal(int pieceIndex) {
        if (game.getStage() != Stage.MOVE
                || !game.selectPiece(pieceIndex)
                || !game.getSelectedPiece().isOnBoard()
                || game.getSelectedPiece().isFinished()) {
            return false;
        }
        return getMoveStepsForSelectedPiece() > 0;
    }

    public int validatePieceMove(int pieceIndex, int routeSize) {
        if (!isPieceSelectionLegal(pieceIndex)) {
            return -1;
        }
        int currentStep = game.getSelectedPiece().getSteps();
        int steps = getMoveStepsForSelectedPiece();
        return isClockwiseMoveAllowed(currentStep, steps, routeSize)
                ? getClockwiseTarget(currentStep, steps) : -1;
    }

    public boolean movePiece(int pieceIndex, int targetStep, int routeSize) {
        if (!isPieceSelectionLegal(pieceIndex)
                || targetStep < 0 || targetStep >= routeSize) {
            return false;
        }
        game.getSelectedPiece().setSteps(targetStep);
        game.setStage(Stage.BUSY);
        return true;
    }

    public boolean applyEventMovement(int pieceIndex, int targetStep,
                                      int routeSize) {
        if (game.getStage() != Stage.BUSY
                || !game.selectPiece(pieceIndex)
                || !game.getSelectedPiece().isOnBoard()
                || targetStep < 0 || targetStep >= routeSize) {
            return false;
        }
        game.getSelectedPiece().setSteps(targetStep);
        return true;
    }

    public boolean tryFinishPiece(int pieceIndex, int routeSize) {
        if (game.getStage() != Stage.MOVE
                || !game.selectPiece(pieceIndex)
                || !game.getSelectedPiece().isOnBoard()
                || game.getSelectedPiece().isFinished()) {
            return false;
        }

        boolean inHome = game.getSelectedPiece().isInHomePath();
        int steps = inHome ? game.getLastStepDie() : game.getPendingSteps();
        int current = game.getSelectedPiece().getSteps();
        if (steps <= 0 || current < 0) {
            return false;
        }

        if (inHome) {
            if (routeSize != 6 || current >= 6 || current + steps != 6) {
                return false;
            }
        } else if (routeSize != 52 || current >= routeSize
                || (!isEasyGame() && !game.getSelectedPiece().isInInnerPath())
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

    private boolean isClockwiseMoveAllowed(int currentStep, int steps, int routeSize) {
        return currentStep >= 0 && steps >= 0
                && currentStep + steps < routeSize;
    }

    public int getClockwiseTarget(int currentStep, int steps) {
        return currentStep + steps;
    }

    private boolean isMatchingArrow(Color arrowColor, Color pieceColor) {
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
                || !game.getSelectedPiece().isOnBoard() || game.getSelectedPiece().isInHomePath()
                || game.getSelectedPiece().isInInnerPath() || innerPathStartStep < 0) {
            return false;
        }
        if (isHardGame() && !game.getSelectedPiece().isInMiddlePath()) {
            game.getSelectedPiece().setInMiddlePath(true);
        } else {
            game.getSelectedPiece().setInInnerPath(true);
        }
        game.getSelectedPiece().setInHomePath(false);
        game.getSelectedPiece().setSteps(innerPathStartStep);
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
                && game.getSelectedPiece().isOnBoard()
                && !game.getSelectedPiece().isInHomePath()
                && !game.getSelectedPiece().isInInnerPath();
    }

    public boolean isPieceInMiddlePath(int pieceIndex) {
        return game.selectPiece(pieceIndex) && game.getSelectedPiece().isInMiddlePath();
    }

    public boolean isPieceInInnerPath(int pieceIndex) {
        return game.selectPiece(pieceIndex) && game.getSelectedPiece().isInInnerPath();
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
        return arrowWithinReach(game.getSelectedPiece().getSteps(),
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
        return arrowWithinReach(game.getSelectedPiece().getSteps(), remainingSteps, ownArrowStep);
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
        return hasAnyLegalMove(routeSizes, null);
    }

    /**
     * SRS 3.2.4.3.2 - a move that reaches the piece's own arrow is legal even if the full
     * number of steps would pass the end of the current ring, because the piece continues
     * on the next path with the steps that are left.
     *
     * @param ownArrowSteps for every piece, the step of its own arrow on its current path,
     *                      or -1 when there is none (may be null)
     */
    public boolean hasAnyLegalMove(int[] routeSizes, int[] ownArrowSteps) {
        if ((game.getStage() != Stage.MOVE
                && game.getStage() != Stage.TAKE_OUT)
                || routeSizes == null
                || routeSizes.length != Game.PIECES_PER_PLAYER) {
            return false;
        }

        int player = game.getCurrentPlayer();
        for (int piece = 0; piece < Game.PIECES_PER_PLAYER; piece++) {
            Piece currentPiece = game.getPlayer(player).getPiece(piece);
            if (!currentPiece.isOnBoard() || currentPiece.isFinished()) {
                continue;
            }
            boolean inHome = currentPiece.isInHomePath();
            int steps = inHome ? game.getLastStepDie() : game.getPendingSteps();
            int current = currentPiece.getSteps();
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
                    && (isEasyGame() || currentPiece.isInInnerPath())
                    && destination <= routeSize + 6) {
                return true;
            }
            if (!inHome && !isEasyGame() && !currentPiece.isInInnerPath()
                    && ownArrowSteps != null && ownArrowSteps.length == Game.PIECES_PER_PLAYER
                    && arrowWithinReach(current, steps, ownArrowSteps[piece]) >= 0) {
                return true;
            }
        }
        return false;
    }

    public boolean tryEnterHomePath(int pieceIndex, int sharedRouteSize) {
        if (game.getStage() != Stage.MOVE || sharedRouteSize <= 0
                || !game.selectPiece(pieceIndex) || !game.getSelectedPiece().isOnBoard()
                || game.getSelectedPiece().isInHomePath()) {
            return false;
        }
        if (!isEasyGame() && !game.getSelectedPiece().isInInnerPath()) {
            return false;
        }
        int steps = game.getPendingSteps();
        int destination = game.getSelectedPiece().getSteps() + steps;
        if (steps <= 0 || destination < sharedRouteSize
                || destination >= sharedRouteSize + 6) {
            return false;
        }
        game.getSelectedPiece().setInHomePath(true);
        game.getSelectedPiece().setSteps(destination - sharedRouteSize);
        game.setStage(Stage.BUSY);
        return true;
    }

    public boolean hasAvailableHomePathMove(int stepDie) {
        int player = game.getCurrentPlayer();
        for (int pieceIndex = 0; pieceIndex < Game.PIECES_PER_PLAYER;
                pieceIndex++) {
            Piece piece = game.getPlayer(player).getPiece(pieceIndex);
            if (piece.isOnBoard()
                    && piece.isInHomePath()
                    && !piece.isFinished()
                    && piece.getSteps() + stepDie <= 6) {
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
