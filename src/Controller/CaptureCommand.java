package Controller;

import Model.Game;

/** Command: a piece of another player is captured and goes back to its castle (SRS 3.2.5.3.1). */
public final class CaptureCommand extends AbstractGameCommand {
    private final int victimPlayer, victimPiece;
    private final String victim;

    public CaptureCommand(Game game, MovementController movement,
                          int victimPlayer, int victimPiece) {
        super(game, movement);
        this.victimPlayer = victimPlayer;
        this.victimPiece = victimPiece;
        this.victim = victimPlayer >= 0 && victimPlayer < game.getPlayerCount()
                ? game.getPlayer(victimPlayer).getName() : "?";
    }

    @Override
    protected boolean doExecute() {
        return movement.sendPieceToCastle(victimPlayer, victimPiece);
    }

    @Override
    public String getDescription() {
        return victim + " loses piece " + (victimPiece + 1) + " (captured, back to the castle)";
    }
}
