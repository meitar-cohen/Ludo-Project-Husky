package Controller;

import Model.Game;

/** Command: a piece reaches the goal with exactly the rolled number of steps. */
public final class FinishPieceCommand extends AbstractGameCommand {
    private final int pieceIndex, routeSize;
    private final String player;

    public FinishPieceCommand(Game game, MovementController movement,
                              int pieceIndex, int routeSize) {
        super(game, movement);
        this.pieceIndex = pieceIndex;
        this.routeSize = routeSize;
        this.player = game.getCurrentPlayerName();
    }

    @Override
    protected boolean doExecute() {
        return movement.tryFinishPiece(pieceIndex, routeSize);
    }

    @Override
    public String getDescription() {
        return player + " finishes piece " + (pieceIndex + 1) + " (reaches the goal)";
    }
}
