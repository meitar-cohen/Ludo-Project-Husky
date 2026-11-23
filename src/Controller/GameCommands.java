package Controller;

import Enums.PieceState;
import Enums.Stage;
import Model.Game;
import Model.Piece;

/**
 * Command design pattern - the concrete commands of the game, grouped in one file (like the
 * surprise strategies are): take a piece out, move, enter the home path, finish, and capture.
 *
 * <p>All of them extend {@link AbstractCommand}, which keeps a snapshot of the model before the
 * action (Memento) and makes {@code execute} and {@code undo} the same for every command, so a
 * concrete command only writes {@code doExecute()} and {@code getDescription()}.
 * The receiver that does the real work is {@link MovementController}.
 */
public final class GameCommands {
    private GameCommands() { }

    /** Base of the commands: snapshot before the action, restore on undo or when it is refused. */
    abstract static class AbstractCommand implements GameCommand {
        protected final Game game;
        protected final MovementController movement;

        private Snapshot before;
        private boolean executed;

        AbstractCommand(Game game, MovementController movement) {
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
    }

    /** The current player takes a piece out of the castle onto its start square. */
    public static final class TakeOutPiece extends AbstractCommand {
        private final int slot;
        private final String player;

        public TakeOutPiece(Game game, MovementController movement, int slot) {
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

    /** The current player moves one piece along the shared path to a new step. */
    public static final class MovePiece extends AbstractCommand {
        private final int pieceIndex, targetStep, routeSize, fromStep;
        private final String player;

        public MovePiece(Game game, MovementController movement, int pieceIndex, int targetStep, int routeSize) {
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

    /** A piece leaves the shared path and enters the private home path of its player. */
    public static final class EnterHomePath extends AbstractCommand {
        private final int pieceIndex, sharedRouteSize;
        private final String player;

        public EnterHomePath(Game game, MovementController movement, int pieceIndex, int sharedRouteSize) {
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

    /** A piece reaches the goal with exactly the rolled number of steps. */
    public static final class FinishPiece extends AbstractCommand {
        private final int pieceIndex, routeSize;
        private final String player;

        public FinishPiece(Game game, MovementController movement, int pieceIndex, int routeSize) {
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

    /** A piece of another player is captured and goes back to its castle (SRS 3.2.5.3.1). */
    public static final class Capture extends AbstractCommand {
        private final int victimPlayer, victimPiece;
        private final String victim;

        public Capture(Game game, MovementController movement, int victimPlayer, int victimPiece) {
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

    /** The state of every piece, castle counter and turn marker of the game (Memento). */
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
