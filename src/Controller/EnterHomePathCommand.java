package Controller;

import Model.Game;

/** Command: a piece leaves the shared path and enters the private home path of its player. */
public final class EnterHomePathCommand extends AbstractGameCommand {
    private final int pieceIndex, sharedRouteSize;
    private final String player;

    public EnterHomePathCommand(Game game, MovementController movement,
                                int pieceIndex, int sharedRouteSize) {
        super(game, movement);
        this.pieceIndex = pieceIndex;
        this.sharedRouteSize = sharedRouteSize;
        this.player = game.getCurrentPlayerName();
    }

    @Override
    protected boolean doExecute() {
        return movement.tryEnterHomePath(pieceIndex, sharedRouteSize);
    }

    @Override
    public String getDescription() {
        return player + " moves piece " + (pieceIndex + 1) + " into the home path";
    }
}
