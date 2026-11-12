package tests;

import java.awt.Color;

import controller.GameController;
import enums.Stage;
import model.Game;
import view.BoardPanel;

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
        System.out.println("PASS - matching arrows enter the inner path");
    }

    private static void testMatchingArrow() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Medium");
        GameController controller = new GameController(game);

        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setStage(Stage.MOVE);

        check(controller.enterInnerPathFromArrow(
                        BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Matching arrow was rejected");
        check(game.isPieceInInnerPath(),
                "Piece did not enter the inner path");
        check(game.getPieceStep() == 0,
                "Piece did not start at the inner path entry");
    }

    private static void testNonMatchingArrow() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Medium");
        GameController controller = new GameController(game);

        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setStage(Stage.MOVE);

        check(!controller.enterInnerPathFromArrow(
                        BoardPanel.RED, BoardPanel.GREEN, 0),
                "Non-matching arrow was accepted");
        check(!game.isPieceInInnerPath(),
                "Piece entered the inner path from a non-matching arrow");
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
