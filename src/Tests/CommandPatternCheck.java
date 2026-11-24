package Tests;

import java.util.List;

import Controller.CommandInvoker;
import Controller.GameCommand;
import Controller.GameController;
import Controller.MovePieceCommand;
import Controller.MovementController;
import Enums.PieceState;
import Enums.Stage;
import Model.Game;
import Model.Piece;
import Model.Player;

/**
 * Command design pattern (SCRUM 3.5): every action of a piece is an object that can be executed
 * and undone, and the invoker keeps the log of the game.
 */
public final class CommandPatternCheck {
    private static void check(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }

    private static final class Setup {
        final Game game;
        final GameController controller;
        final MovementController movement;

        Setup(String difficulty) {
            game = new Game(new Player[]{new Player("Ann", "Green"),
                    new Player("Bob", "Red")}, difficulty);
            movement = new MovementController(game);
            controller = GameController.forTesting(game, movement);
        }

        Piece piece(int player, int index) {
            return game.getPlayer(player).getPiece(index);
        }

        void place(int player, int index, int steps) {
            piece(player, index).setOnBoard(true);
            piece(player, index).setSteps(steps);
            game.setPiecesInCastle(player, game.getPiecesInCastle(player) - 1);
        }
    }

    public static void main(String[] args) {
        // --- take a piece out of the castle, and undo it
        Setup out = new Setup("Easy");
        out.game.setStage(Stage.TAKE_OUT);
        check(out.controller.takeOutPiece(1), "take-out command runs");
        check(out.piece(0, 1).isOnBoard() && out.game.getPiecesInCastle(0) == 3
                && out.game.getStage() == Stage.ROLL, "the piece is on the board");
        check(out.controller.getCommandInvoker().undoLast(), "the take-out can be undone");
        check(!out.piece(0, 1).isOnBoard() && out.game.getPiecesInCastle(0) == 4
                && out.game.getStage() == Stage.TAKE_OUT, "undo puts the piece back in the castle");
        System.out.println("PASS - take-out command and its undo");

        // --- move a piece, and undo it
        Setup move = new Setup("Easy");
        move.place(0, 0, 3);
        move.game.setStage(Stage.MOVE);
        move.game.setPendingSteps(4);
        check(move.controller.movePiece(0, 7, 52), "move command runs");
        check(move.piece(0, 0).getSteps() == 7 && move.game.getStage() == Stage.BUSY,
                "the piece moved");
        check(move.controller.getCommandInvoker().undoLast(), "the move can be undone");
        check(move.piece(0, 0).getSteps() == 3 && move.game.getStage() == Stage.MOVE,
                "undo returns the piece and the stage");
        System.out.println("PASS - move command and its undo");

        // --- an illegal command changes nothing and is not logged
        Setup bad = new Setup("Easy");
        bad.place(0, 0, 3);
        bad.game.setStage(Stage.MOVE);
        bad.game.setPendingSteps(4);
        check(!bad.controller.movePiece(0, 99, 52), "a move outside the path is refused");
        check(!bad.controller.movePiece(2, 7, 52), "a piece that is not on the board cannot move");
        check(bad.piece(0, 0).getSteps() == 3 && bad.game.getStage() == Stage.MOVE,
                "a refused command leaves no trace");
        check(bad.controller.getActionLog().isEmpty(), "a refused command is not logged");
        System.out.println("PASS - a refused command changes nothing");

        // --- enter the home path and finish, with undo
        Setup home = new Setup("Easy");
        home.place(0, 0, 50);
        home.game.setStage(Stage.MOVE);
        home.game.setPendingSteps(4);
        check(home.controller.enterHomePath(0, 52), "home-path command runs");
        check(home.piece(0, 0).isInHomePath() && home.piece(0, 0).getSteps() == 2,
                "the piece is on the home path");
        home.controller.getCommandInvoker().undoLast();
        check(!home.piece(0, 0).isInHomePath() && home.piece(0, 0).getSteps() == 50
                && home.piece(0, 0).getState() == PieceState.BOARD,
                "undo takes the piece back to the shared path");

        Setup finish = new Setup("Easy");
        for (int i = 0; i < 3; i++) {
            finish.place(0, i, 0);
            finish.piece(0, i).finish();
        }
        finish.place(0, 3, 51);
        finish.game.setStage(Stage.MOVE);
        finish.game.setPendingSteps(7);
        check(finish.controller.finishPiece(3, 52), "finish command runs");
        check(finish.piece(0, 3).isFinished() && finish.game.isPlayerFinished(0),
                "the last piece finished the player");
        finish.controller.getCommandInvoker().undoLast();
        check(!finish.piece(0, 3).isFinished() && !finish.game.isPlayerFinished(0)
                && finish.game.getFinishOrder().isEmpty() && finish.piece(0, 3).getSteps() == 51,
                "undo of a finish restores the piece and the ranking");
        System.out.println("PASS - home-path and finish commands with undo");

        // --- capture is a command too
        Setup capture = new Setup("Easy");
        capture.place(0, 0, 10);
        capture.place(1, 0, 23);
        int[] captured = capture.controller.captureOpponentAt(0, 0);
        check(captured != null && !capture.piece(1, 0).isOnBoard(), "capture works through a command");
        check(capture.controller.getActionLog().size() == 1
                && capture.controller.getActionLog().get(0).contains("captured"),
                "the capture is in the log");
        check(capture.controller.getCommandInvoker().undoLast()
                && capture.piece(1, 0).isOnBoard() && capture.piece(1, 0).getSteps() == 23
                && capture.game.getPiecesInCastle(1) == 3, "undo of a capture brings the piece back");
        System.out.println("PASS - capture command with undo");

        // --- the invoker: order, log, undo, clear
        Setup log = new Setup("Easy");
        log.place(0, 0, 3);
        log.place(0, 1, 5);
        log.game.setStage(Stage.MOVE);
        log.game.setPendingSteps(2);
        CommandInvoker invoker = new CommandInvoker();
        GameCommand first = new MovePieceCommand(log.game, log.movement, 0, 5, 52);
        check(invoker.execute(first), "first command");
        check(!first.execute(), "a command cannot run twice");
        log.game.setStage(Stage.MOVE);
        check(invoker.execute(new MovePieceCommand(log.game, log.movement, 1, 7, 52)), "second command");
        List<String> lines = invoker.getLog();
        check(invoker.size() == 2 && lines.size() == 2, "two commands logged");
        check(lines.get(0).equals("Ann moves piece 1 from step 3 to step 5"),
                "the log text is readable: " + lines.get(0));
        check(invoker.undoLast() && log.piece(0, 1).getSteps() == 5, "last command is undone first");
        check(invoker.undoLast() && log.piece(0, 0).getSteps() == 3, "then the one before");
        check(!invoker.canUndo() && !invoker.undoLast(), "nothing more to undo");
        check(invoker.getLog().size() == 4 && invoker.getLog().get(2).startsWith("Undo: "),
                "undo is written in the log");
        invoker.clear();
        check(invoker.getLog().isEmpty() && invoker.size() == 0, "clear empties the invoker");
        boolean rejected = false;
        try {
            invoker.execute(null);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "a null command is rejected");
        System.out.println("PASS - the invoker keeps the log and undoes in reverse order");
        System.out.println("All command pattern checks passed.");
    }
}
