package Tests;

import java.awt.Color;

import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;
import Model.Game;
import View.BoardPanel;

public class InnerPathArrowCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        testMatchingArrow();
        testNonMatchingArrow();
        testBoardArrowLookup();
        testHardGoesThroughMiddlePath();
        testInnerPathArrowIgnored();
        System.out.println("PASS - matching arrows enter the inner path");
    }

    private static void testMatchingArrow() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Medium");
        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);

        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setStage(Stage.MOVE);

        check(controllerPieces.enterInnerPathFromArrow(
                        BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Matching arrow was rejected");
        check(game.isPieceInInnerPath(),
                "Piece did not enter the inner path");
        check(game.getPieceStep() == 0,
                "Piece did not start at the inner path entry");
    }

    private static void testNonMatchingArrow() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Medium");
        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);

        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setStage(Stage.MOVE);

        check(!controllerPieces.enterInnerPathFromArrow(
                        BoardPanel.RED, BoardPanel.GREEN, 0),
                "Non-matching arrow was accepted");
        check(!game.isPieceInInnerPath(),
                "Piece entered the inner path from a non-matching arrow");
    }

    // SRS 3.2.4.3.2 - ברמה הקשה: 72 -> 64 -> 52
    private static void testHardGoesThroughMiddlePath() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Hard");
        MovementController pieces = new MovementController(game);
        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setPieceStep(9);
        game.setStage(Stage.MOVE);

        check(pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Hard: arrow on the outer path was rejected");
        check(game.isPieceInMiddlePath() && !game.isPieceInInnerPath(),
                "Hard: the first arrow must lead to the middle path (64), not the inner path");
        check(game.getPieceStep() == 0, "Hard: piece must start the middle path at step 0");

        check(pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Hard: arrow on the middle path was rejected");
        check(game.isPieceInInnerPath() && !game.isPieceInMiddlePath(),
                "Hard: the second arrow must lead to the inner path (52)");

        check(!pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Hard: an arrow on the inner path must not move the piece again");
    }

    // SRS 3.2.4.3.2 - ברמה הבינונית: 64 -> 52, ובמסלול הפנימי החץ לא מחזיר את הכלי להתחלה
    private static void testInnerPathArrowIgnored() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Medium");
        MovementController pieces = new MovementController(game);
        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setPieceInInnerPath(true);
        game.setPieceStep(50);
        game.setStage(Stage.MOVE);
        check(!pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0)
                && game.getPieceStep() == 50,
                "Medium: an arrow on the inner path must not reset the piece");
        check(!game.isPieceInMiddlePath(), "Medium has no middle path");
    }

    private static void testBoardArrowLookup() {
        BoardPanel board = new BoardPanel(
                "Medium", new String[]{"Green", "Yellow"});
        board.setActivePlayerColor("Green");
        check(board.placeActivePieceOnStart(0),
                "Could not place piece on the board");

        int arrowStep = -1;
        Color arrowColor = null;
        for (int step = 0; step < board.getActiveRouteSize(0); step++) {
            Color candidate = board.getArrowColorAtStep(0, step);
            if (candidate != null) {
                arrowStep = step;
                arrowColor = candidate;
                break;
            }
        }

        check(arrowStep >= 0 && arrowColor != null,
                "Board did not expose an arrow destination");
    }
}
