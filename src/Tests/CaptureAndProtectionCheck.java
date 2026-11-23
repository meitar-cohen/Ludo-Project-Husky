package Tests;

import java.awt.Point;

import Controller.GameController;
import Controller.MovementController;
import Enums.SquareType;
import Model.Game;
import Model.Piece;
import Model.Player;

/** SRS 3.2.5.3.1 - 3.2.5.3.2: capturing an opponent piece and protected squares. */
public final class CaptureAndProtectionCheck {
    private static void check(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }

    private static final class Setup {
        final Game game;
        final GameController controller;

        Setup(String difficulty) {
            game = new Game(new Player[]{new Player("A", "Green"),
                    new Player("B", "Red")}, difficulty);
            controller = GameController.forTesting(game, new MovementController(game));
        }

        void place(int player, int piece, int steps) {
            Piece model = game.getPlayer(player).getPiece(piece);
            model.setOnBoard(true);
            model.setSteps(steps);
            game.setPiecesInCastle(player, game.getPiecesInCastle(player) - 1);
        }
    }

    // on the Easy board Green starts at index 0 and Red at index 39 of the 52-square path
    private static Setup sameSquare() {
        Setup setup = new Setup("Easy");
        setup.place(0, 0, 10);
        setup.place(1, 0, 23);
        check(setup.controller.getPieceBoardPosition(0, 0)
                .equals(setup.controller.getPieceBoardPosition(1, 0)),
                "Pieces should share a square");
        return setup;
    }

    public static void main(String[] args) {
        Setup capture = sameSquare();
        int[] captured = capture.controller.captureOpponentAt(0, 0);
        check(captured != null && captured[0] == 1 && captured[1] == 0,
                "Opponent piece should be captured");
        Piece victim = capture.game.getPlayer(1).getPiece(0);
        check(!victim.isOnBoard() && victim.getSteps() == 0
                && capture.game.getPiecesInCastle(1) == 4,
                "Captured piece should return to its castle");
        check(capture.game.getPlayer(0).getPiece(0).isOnBoard()
                && capture.game.getPiecesInCastle(0) == 3,
                "The capturing piece should stay on the board");
        System.out.println("PASS - single opponent piece is captured");

        for (SquareType type : new SquareType[]{
                SquareType.STAR, SquareType.START, SquareType.SURPRISE}) {
            Setup protectedSquare = sameSquare();
            Point position = protectedSquare.controller.getPieceBoardPosition(0, 0);
            protectedSquare.controller.addSpecialSquare(position, type, null, 0);
            check(protectedSquare.controller.captureOpponentAt(0, 0) == null
                    && protectedSquare.game.getPlayer(1).getPiece(0).isOnBoard(),
                    type + " square must protect the piece");
        }
        System.out.println("PASS - star, start and surprise squares are protected");

        Setup other = sameSquare();
        other.controller.addSpecialSquare(
                other.controller.getPieceBoardPosition(0, 0), SquareType.ARROW, null, 0);
        check(other.controller.captureOpponentAt(0, 0) != null,
                "A square that is not protected allows capture");

        Setup block = sameSquare();
        block.place(1, 1, 23);
        check(block.controller.captureOpponentAt(0, 0) == null,
                "Two pieces of one player are not a single piece");

        Setup own = new Setup("Easy");
        own.place(0, 0, 10);
        own.place(0, 1, 10);
        check(own.controller.captureOpponentAt(0, 0) == null
                && own.game.getPlayer(0).getPiece(1).isOnBoard(),
                "A piece never captures its own pieces");

        Setup apart = new Setup("Easy");
        apart.place(0, 0, 10);
        apart.place(1, 0, 24);
        check(apart.controller.captureOpponentAt(0, 0) == null,
                "No capture on a different square");

        Setup home = new Setup("Easy");
        home.place(0, 0, 10);
        home.place(1, 0, 3);
        home.game.getPlayer(1).getPiece(0).setInHomePath(true);
        check(home.controller.getPieceBoardPosition(1, 0) == null
                && home.controller.captureOpponentAt(0, 0) == null,
                "A piece on the home path cannot be captured");
        System.out.println("PASS - block, own, distant and home-path pieces");

        // Medium: the 64-square outer ring, Red corner is 3 sides (48 squares) after Green
        Setup medium = new Setup("Medium");
        medium.place(0, 0, 5);
        medium.place(1, 0, 21);
        int[] mediumCapture = medium.controller.captureOpponentAt(1, 0);
        check(mediumCapture != null && mediumCapture[0] == 0,
                "Outer ring positions should match for capture");
        System.out.println("PASS - capture on the outer ring");
        System.out.println("All capture and protection checks passed.");
    }
}
