package Controller;

import Model.Game;

/** Command: the current player takes a piece out of the castle onto its start square. */
public final class TakeOutPieceCommand extends AbstractGameCommand {
    private final int slot;
    private final String player;

    public TakeOutPieceCommand(Game game, MovementController movement, int slot) {
        super(game, movement);
        this.slot = slot;
        this.player = game.getCurrentPlayerName();
    }

    @Override
    protected boolean doExecute() {
        return movement.takeOutActivePiece(slot);
    }

    @Override
    public String getDescription() {
        return player + " takes piece " + (slot + 1) + " out of the castle";
    }
}
