package Tests;

import java.awt.Point;

import Controller.GameController;
import Controller.MovementController;
import Enums.SquareType;
import Enums.Stage;
import Model.Game;
import Model.Piece;
import Model.Player;

/**
 * SRS 3.2.5.3.3 - 3.2.5.3.4: the Double Block. Two pieces of one player on one square block it
 * in both directions; an opponent cannot stop on it or pass through it, a piece that meets a
 * block stops on the square before it, forward only if no other piece can make the whole move,
 * and the block is cancelled when one of its pieces moves.
 */
public final class DoubleBlockCheck {
    private static void check(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }

    private static final int[] SIZES = {52, 52, 52, 52};

    private static final class Setup {
        final Game game;
        final GameController controller;
        final MovementController movement;

        Setup(String difficulty) {
            game = new Game(new Player[]{new Player("A", "Green"),
                    new Player("B", "Red")}, difficulty);
            movement = new MovementController(game);
            controller = new GameController(game, movement);
        }

        void place(int player, int piece, int steps) {
            Piece model = game.getPlayer(player).getPiece(piece);
            model.setOnBoard(true);
            model.setSteps(steps);
            game.setPiecesInCastle(player, game.getPiecesInCastle(player) - 1);
        }

        /** Turn of Red with the given rolled steps, ready to choose a piece. */
        void redMoves(int steps) {
            game.setCurrentPlayer(1);
            game.setPendingSteps(steps);
            game.setLastStepDie(steps);
            game.setStage(Stage.MOVE);
        }
    }

    // Easy board: Green step 10 is the same square as Red step 23 (Green starts at 0, Red at 39)
    private static Setup greenBlock() {
        Setup setup = new Setup("Easy");
        setup.place(0, 0, 10);
        setup.place(0, 1, 10);
        return setup;
    }

    public static void main(String[] args) {
        // --- 3.2.5.3.3: what a block is
        Setup s = greenBlock();
        Point square = s.controller.getPieceBoardPosition(0, 0);
        check(s.controller.isDoubleBlock(square), "two pieces of one player are a block");
        check(s.controller.getDoubleBlockOwnerAt(square) == 0, "the block belongs to its owner");
        check(s.controller.getDoubleBlockPositions().size() == 1
                && s.controller.getDoubleBlockPositions().contains(square),
                "the view gets the square of the block");

        Setup single = new Setup("Easy");
        single.place(0, 0, 10);
        check(!single.controller.isDoubleBlock(single.controller.getPieceBoardPosition(0, 0)),
                "one piece is not a block");
        Setup mixed = new Setup("Easy");
        mixed.place(0, 0, 10);
        mixed.place(1, 0, 23);
        check(mixed.controller.getDoubleBlockPositions().isEmpty(),
                "pieces of two players on one square are not a block");
        check(single.controller.getDoubleBlockOwnerAt(null) == -1, "no square, no block");
        System.out.println("PASS - a block is two pieces of the same player on one square");

        // --- 3.2.5.3.3: not passed and not stood on, forward
        s.place(1, 0, 20);
        check(s.controller.getStepsBeforeBlock(1, 0, 20, 25) == 22,
                "forward: the piece stops before the block, it does not pass it");
        check(s.controller.getStepsBeforeBlock(1, 0, 20, 23) == 22,
                "forward: the piece cannot stop on the block");
        check(s.controller.getStepsBeforeBlock(1, 0, 20, 22) == 22,
                "a move that ends before the block is free");
        check(s.controller.getStepsBeforeBlock(1, 0, 20, 20) == 20, "no move, no change");
        System.out.println("PASS - an opponent cannot pass or stop on the block (forward)");

        // --- 3.2.5.3.4: backward
        s.game.getPlayer(1).getPiece(0).setSteps(26);
        check(s.controller.getStepsBeforeBlock(1, 0, 26, 21) == 24,
                "backward: the piece stops on the square after the block");
        check(s.controller.getStepsBeforeBlock(1, 0, 26, 24) == 24,
                "a backward move that ends before the block is free");
        check(s.controller.getBlockingPlayer(1, 0, 26, 21) == 0, "the owner of the block is known");
        check(s.controller.getBlockingPlayer(1, 0, 26, 24) == -1, "a free move has no blocker");
        System.out.println("PASS - an opponent cannot pass or stop on the block (backward)");

        // --- own pieces, protected squares, home path
        s.place(0, 2, 5);
        check(s.controller.getStepsBeforeBlock(0, 2, 5, 14) == 14,
                "the owner is not stopped by its own block");
        s.controller.addSpecialSquare(square, SquareType.STAR, null, 0);
        s.game.getPlayer(1).getPiece(0).setSteps(20);
        check(s.controller.getStepsBeforeBlock(1, 0, 20, 25) == 22,
                "a block on a protected square still blocks");
        Setup home = greenBlock();
        home.place(1, 0, 3);
        home.game.getPlayer(1).getPiece(0).setInHomePath(true);
        check(home.controller.getStepsBeforeBlock(1, 0, 3, 5) == 5,
                "the private home path holds no block");
        System.out.println("PASS - own pieces, protected squares and the home path");

        // --- the block is cancelled when one of its pieces moves
        Setup cancel = greenBlock();
        cancel.place(1, 0, 20);
        check(cancel.controller.getStepsBeforeBlock(1, 0, 20, 25) == 22, "block stands");
        cancel.game.getPlayer(0).getPiece(1).setSteps(11);
        check(cancel.controller.getDoubleBlockPositions().isEmpty(),
                "the block is gone when one piece moves");
        check(cancel.controller.getStepsBeforeBlock(1, 0, 20, 25) == 25,
                "after the block is cancelled the way is free");
        System.out.println("PASS - the block is cancelled when one of its pieces moves");

        // --- a piece right behind a block cannot move at all
        Setup stuck = greenBlock();
        stuck.place(1, 0, 22);
        stuck.redMoves(2);
        check(stuck.controller.getPiecesStuckBehindBlock()[0], "piece in front of the block is stuck");
        check(!stuck.controller.getPiecesStuckBehindBlock()[1], "a piece that is in the castle is not");
        check(!stuck.movement.hasAnyLegalMove(SIZES, null, stuck.controller.getPiecesStuckBehindBlock()),
                "when the only piece is stuck there is no legal move");
        check(stuck.movement.hasAnyLegalMove(SIZES, null),
                "without the block rule the move would look legal");
        System.out.println("PASS - a stuck piece gives no legal move");

        // --- forward is allowed only if no other piece can make the whole move
        Setup choice = greenBlock();
        choice.place(1, 0, 20);
        choice.redMoves(5);
        check(!choice.controller.hasOtherPieceWithFreeMove(0, SIZES, null),
                "with no other piece the cut-short move is allowed");
        choice.place(1, 1, 5);
        check(choice.controller.hasOtherPieceWithFreeMove(0, SIZES, null),
                "another piece with a free move: the cut-short move is not allowed");
        choice.game.getPlayer(1).getPiece(1).setSteps(19);
        check(!choice.controller.hasOtherPieceWithFreeMove(0, SIZES, null),
                "the other piece is blocked by the same block, so it is no free move");
        System.out.println("PASS - forward move up to a block only if there is no other move");

        // --- a block on the start square keeps the other players' pieces in the castle
        Setup start = new Setup("Easy");
        start.place(0, 0, 39);
        start.place(0, 1, 39);
        start.game.setCurrentPlayer(1);
        check(start.controller.isTakeOutBlocked(0), "block on the start square: no take-out");
        start.game.setCurrentPlayer(0);
        check(!start.controller.isTakeOutBlocked(2), "the block does not stop its own owner");
        Setup free = new Setup("Easy");
        free.game.setCurrentPlayer(1);
        check(!free.controller.isTakeOutBlocked(0), "free start square: take-out is possible");
        System.out.println("PASS - a block on the start square stops the take-out");

        // --- Medium: the 64-square outer ring
        Setup medium = new Setup("Medium");
        medium.place(0, 0, 5);
        medium.place(0, 1, 5);
        medium.place(1, 0, 15);
        check(medium.controller.getStepsBeforeBlock(1, 0, 15, 25) == 20,
                "the block works on the outer ring of Medium");
        System.out.println("PASS - the block on the outer ring");
        System.out.println("All Double Block checks passed.");
    }
}
