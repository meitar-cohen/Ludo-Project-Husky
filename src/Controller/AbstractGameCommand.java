package Controller;

import Enums.PieceState;
import Enums.Stage;
import Model.Game;
import Model.Piece;

/**
 * Base of the concrete commands. Before a command changes the game it keeps a snapshot of the
 * parts of the model that an action can change (Memento), so {@link #undo()} is the same for all
 * commands. A concrete command only writes {@link #doExecute()} and {@link #getDescription()}.
 */
abstract class AbstractGameCommand implements GameCommand {

    protected final Game game;
    protected final MovementController movement;

    private Snapshot before;
    private boolean executed;

    AbstractGameCommand(Game game, MovementController movement) {
        if (game == null || movement == null) {
            throw new IllegalArgumentException("A command needs the game and its controller");
        }
        this.game = game;
        this.movement = movement;
    }

    /** The real action, done through the receiver. */
    protected abstract boolean doExecute();

    @Override
    public final boolean execute() {
        if (executed) {
            return false;
        }
        Snapshot snapshot = new Snapshot(game);
        if (!doExecute()) {
            snapshot.restore(game); // a refused action must leave no trace
            return false;
        }
        before = snapshot;
        executed = true;
        return true;
    }

    @Override
    public final boolean undo() {
        if (!executed) {
            return false;
        }
        before.restore(game);
        executed = false;
        return true;
    }

    /** The state of every piece, castle counter and turn marker of the game. */
    private static final class Snapshot {
        private final PieceState[] states;
        private final int[] steps;
        private final boolean[] inner, middle;
        private final int[] castle;
        private final boolean[] finishedPlayers;
        private final Stage stage;
        private final int currentPlayer, selectedPiece;

        Snapshot(Game game) {
            int players = game.getPlayerCount();
            int count = players * Game.PIECES_PER_PLAYER;
            states = new PieceState[count];
            steps = new int[count];
            inner = new boolean[count];
            middle = new boolean[count];
            castle = new int[players];
            finishedPlayers = new boolean[players];
            for (int p = 0; p < players; p++) {
                castle[p] = game.getPiecesInCastle(p);
                finishedPlayers[p] = game.isPlayerFinished(p);
                for (int i = 0; i < Game.PIECES_PER_PLAYER; i++) {
                    Piece piece = game.getPlayer(p).getPiece(i);
                    int k = p * Game.PIECES_PER_PLAYER + i;
                    states[k] = piece.getState();
                    steps[k] = piece.getSteps();
                    inner[k] = piece.isInInnerPath();
                    middle[k] = piece.isInMiddlePath();
                }
            }
            stage = game.getStage();
            currentPlayer = game.getCurrentPlayer();
            selectedPiece = game.getSelectedPieceIndex();
        }

        void restore(Game game) {
            for (int p = 0; p < castle.length; p++) {
                game.setPiecesInCastle(p, castle[p]);
                if (game.isPlayerFinished(p) && !finishedPlayers[p]) {
                    game.setPlayerFinished(p, false);
                }
                for (int i = 0; i < Game.PIECES_PER_PLAYER; i++) {
                    Piece piece = game.getPlayer(p).getPiece(i);
                    int k = p * Game.PIECES_PER_PLAYER + i;
                    piece.setState(states[k]);
                    piece.setSteps(steps[k]);
                    piece.setInInnerPath(false);
                    piece.setInMiddlePath(false);
                    if (inner[k]) {
                        piece.setInInnerPath(true);
                    } else if (middle[k]) {
                        piece.setInMiddlePath(true);
                    }
                }
            }
            game.setCurrentPlayer(currentPlayer);
            game.selectPiece(selectedPiece);
            game.setStage(stage);
        }
    }
}
