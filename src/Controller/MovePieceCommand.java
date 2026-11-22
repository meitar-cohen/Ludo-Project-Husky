package Controller;

import Model.Game;

/** Command: the current player moves one piece along the shared path to a new step. */
public final class MovePieceCommand extends AbstractGameCommand {
    private final int pieceIndex, targetStep, routeSize;
    private final String player;
    private final int fromStep;

    public MovePieceCommand(Game game, MovementController movement,
                            int pieceIndex, int targetStep, int routeSize) {
        super(game, movement);
        this.pieceIndex = pieceIndex;
        this.targetStep = targetStep;
        this.routeSize = routeSize;
        this.player = game.getCurrentPlayerName();
        this.fromStep = pieceIndex >= 0 && pieceIndex < Game.PIECES_PER_PLAYER
                ? game.getPlayer(game.getCurrentPlayer()).getPiece(pieceIndex).getSteps() : 0;
    }

    @Override
    protected boolean doExecute() {
        return movement.movePiece(pieceIndex, targetStep, routeSize);
    }

    @Override
    public String getDescription() {
        return player + " moves piece " + (pieceIndex + 1)
                + " from step " + fromStep + " to step " + targetStep;
    }
}
