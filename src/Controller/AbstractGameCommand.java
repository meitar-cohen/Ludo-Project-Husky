package Controller;

import Enums.PieceState;
import Enums.Stage;
import Model.Game;
import Model.Piece;

/**
 * Base of the concrete commands (Command design pattern). Before a command changes the game it
 * keeps a snapshot of the parts of the model that an action can change (Memento), so
 * {@link #undo()} is the same for all commands. A concrete command only writes
 * {@link #doExecute()} and {@link #getDescription()}.
 */
abstract class AbstractGameCommand implements GameCommand {

    protected final Game game;
    protected final MovementController movement;

    private boolean executed;

    // snapshot of the game before the action: every piece, castle counters and turn markers
    private PieceState[] states;
    private int[] steps;
    private boolean[] inner, middle;
    private int[] castle;
    private boolean[] finishedPlayers;
    private Stage stage;
    private int currentPlayer, selectedPiece;

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
        takeSnapshot();
        if (!doExecute()) {
            restoreSnapshot(); // a refused action must leave no trace
            return false;
        }
        executed = true;
        return true;
    }

    @Override
    public final boolean undo() {
        if (!executed) {
            return false;
        }
        restoreSnapshot();
        executed = false;
        return true;
    }

    private void takeSnapshot() {
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

    private void restoreSnapshot() {
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
        game.notifyObservers("undo");   // the views must redraw after an undo (Observer of the Model)
    }
}
