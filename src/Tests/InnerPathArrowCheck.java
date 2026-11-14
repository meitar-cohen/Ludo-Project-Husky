package Tests;

import java.awt.Color;

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
        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);
        game.setStage(Stage.MOVE);

        check(controllerPieces.enterInnerPathFromArrow(
                        BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Matching arrow was rejected");
        check(game.getSelectedPiece().isInInnerPath(),
                "Piece did not enter the inner path");
        check(game.getSelectedPiece().getSteps() == 0,
                "Piece did not start at the inner path entry");
    }

    private static void testNonMatchingArrow() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Medium");
        MovementController controllerPieces = new MovementController(game);
        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);
        game.setStage(Stage.MOVE);

        check(!controllerPieces.enterInnerPathFromArrow(
                        BoardPanel.RED, BoardPanel.GREEN, 0),
                "Non-matching arrow was accepted");
        check(!game.getSelectedPiece().isInInnerPath(),
                "Piece entered the inner path from a non-matching arrow");
    }

    // SRS 3.2.4.3.2 - ברמה הקשה: 72 -> 64 -> 52
    private static void testHardGoesThroughMiddlePath() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Hard");
        MovementController pieces = new MovementController(game);
        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setSteps(9);
        game.setStage(Stage.MOVE);

        check(pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Hard: arrow on the outer path was rejected");
        check(game.getSelectedPiece().isInMiddlePath() && !game.getSelectedPiece().isInInnerPath(),
                "Hard: the first arrow must lead to the middle path (64), not the inner path");
        check(game.getSelectedPiece().getSteps() == 0, "Hard: piece must start the middle path at step 0");

        check(pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Hard: arrow on the middle path was rejected");
        check(game.getSelectedPiece().isInInnerPath() && !game.getSelectedPiece().isInMiddlePath(),
                "Hard: the second arrow must lead to the inner path (52)");

        check(!pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0),
                "Hard: an arrow on the inner path must not move the piece again");
    }

    // SRS 3.2.4.3.2 - ברמה הבינונית: 64 -> 52, ובמסלול הפנימי החץ לא מחזיר את הכלי להתחלה
    private static void testInnerPathArrowIgnored() {
        Game game = new Game(TestPlayers.of(new String[]{"Green", "Yellow"}, new String[]{"Green", "Yellow"}), "Medium");
        MovementController pieces = new MovementController(game);
        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setInInnerPath(true);
        game.getSelectedPiece().setSteps(50);
        game.setStage(Stage.MOVE);
        check(!pieces.enterInnerPathFromArrow(BoardPanel.GREEN, BoardPanel.GREEN, 0)
                && game.getSelectedPiece().getSteps() == 50,
                "Medium: an arrow on the inner path must not reset the piece");
        check(!game.getSelectedPiece().isInMiddlePath(), "Medium has no middle path");
    }

    private static void testBoardArrowLookup() {
        BoardPanel board = new BoardPanel(
                TestPlayers.gameControllerForBoard(
                        "Medium", new String[]{"Green", "Yellow"}));
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
