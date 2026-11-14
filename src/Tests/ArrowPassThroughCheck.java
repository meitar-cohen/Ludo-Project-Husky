package Tests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;
import Enums.SquareType;
import Model.Game;
import View.BoardPanel;
import View.GameScreen;

/**
 * SRS 3.2.4.3.2 - a piece moves to the next inner path when it passes over
 * OR stops on an arrow of its own colour.
 * Medium: outer (64) -> inner (52). Hard: outer (72) -> middle (64) -> inner (52).
 */
public class ArrowPassThroughCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static Object field(Object source, String name) throws Exception {
        Field f = source.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(source);
    }

    private static Game gameAt(String level, int step, int pendingSteps) {
        Game game = new Game(TestPlayers.of(new String[]{"A", "B"},
                new String[]{"Green", "Red"}), level);
        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setPieceStep(step);
        game.setPendingSteps(pendingSteps);
        game.setLastStepDie(5);
        game.setStage(Stage.MOVE);
        return game;
    }

    // 1. החוק בבקר: מעבר דרך החץ, עצירה עליו, ומקרים שבהם החץ לא חל
    private static void testControllerRule() {
        // Medium: own arrow at step 8 of the outer path
        Game passing = gameAt("Medium", 5, 6);
        MovementController pieces = new MovementController(passing);
        check(pieces.findArrowOnMove(0, 8) == 8, "Medium: passing over the arrow is detected");

        Game landing = gameAt("Medium", 5, 3);
        check(new MovementController(landing).findArrowOnMove(0, 8) == 8,
                "Medium: stopping on the arrow is detected");

        Game shortMove = gameAt("Medium", 2, 3);
        check(new MovementController(shortMove).findArrowOnMove(0, 8) == -1,
                "Medium: a move that does not reach the arrow ignores it");

        Game easy = gameAt("Easy", 5, 6);
        check(new MovementController(easy).findArrowOnMove(0, 8) == -1,
                "Easy: there are no arrows between paths");

        Game inner = gameAt("Medium", 45, 6);
        inner.setPieceInInnerPath(true);
        check(new MovementController(inner).findArrowOnMove(0, 50) == -1,
                "Medium: the arrow on the inner path does not move the piece again");

        // Hard: after the outer arrow, the remaining steps can reach the middle arrow
        Game hard = gameAt("Hard", 7, 30);
        MovementController hardPieces = new MovementController(hard);
        check(hardPieces.findArrowOnMove(0, 9) == 9, "Hard: outer arrow detected");
        check(hardPieces.movePiece(0, 9, 72), "Hard: move to the outer arrow");
        check(hardPieces.enterNextPathFromArrow(0, BoardPanel.GREEN, BoardPanel.GREEN)
                && hard.isPieceInMiddlePath(), "Hard: outer arrow leads to the middle path");
        int remaining = 30 - (9 - 7);
        check(hardPieces.findArrowOnRemainingSteps(0, remaining, 8) == 8,
                "Hard: remaining steps reach the middle arrow");
        check(hardPieces.continueMoveAfterArrow(0, 8, 64), "Hard: move to the middle arrow");
        check(hardPieces.enterNextPathFromArrow(0, BoardPanel.GREEN, BoardPanel.GREEN)
                && hard.isPieceInInnerPath() && !hard.isPieceInMiddlePath(),
                "Hard: middle arrow leads to the inner path");
        check(hardPieces.findArrowOnRemainingSteps(0, remaining - 8, 8) == -1,
                "Hard: no arrow is used on the inner path");
        check(hardPieces.continueMoveAfterArrow(0, remaining - 8, 52)
                && hard.getPieceStep() == 20, "Hard: the rest of the move is on the inner path");

        System.out.println("PASS - arrow rule in the controller");
    }

    // 2. מיקום החצים על הלוח: 8 ב-64, 9 ב-72, ו-8 במסלול האמצעי
    private static void testBoardArrowSteps() {
        BoardPanel medium = new BoardPanel(
                TestPlayers.gameControllerForBoard(
                        "Medium", new String[]{"Green", "Red"}));
        medium.setActivePlayerColor("Green");
        check(medium.placeActivePieceOnStart(0), "Medium: place piece");
        check(medium.getActiveRouteSize(0) == 64 && medium.getOwnArrowStep(0) == 8,
                "Medium: own arrow at step 8 of the 64 path");

        BoardPanel hard = new BoardPanel(
                TestPlayers.gameControllerForBoard(
                        "Hard", new String[]{"Green", "Red"}));
        hard.setActivePlayerColor("Green");
        check(hard.placeActivePieceOnStart(0), "Hard: place piece");
        check(hard.getActiveRouteSize(0) == 72 && hard.getOwnArrowStep(0) == 9,
                "Hard: own arrow at step 9 of the 72 path");
        check(hard.showActivePieceInMiddlePath(0, 0)
                && hard.getActiveRouteSize(0) == 64 && hard.getOwnArrowStep(0) == 8,
                "Hard: middle path has 64 squares and its own arrow at step 8");
        check(hard.showActivePieceInInnerPath(0, 0)
                && !hard.isActivePieceInMiddlePath(0)
                && hard.getActiveRouteSize(0) == 52 && hard.getOwnArrowStep(0) == -1,
                "Hard: inner path has 52 squares and no arrow to use");

        BoardPanel easy = new BoardPanel(
                TestPlayers.gameControllerForBoard(
                        "Easy", new String[]{"Green", "Red"}));
        easy.setActivePlayerColor("Green");
        easy.placeActivePieceOnStart(0);
        check(!easy.showActivePieceInMiddlePath(0, 0), "Easy: there is no middle path");
        System.out.println("PASS - arrow positions on the board");
    }

    // 3. מסך המשחק: לחיצה על כלי שעובר את החץ
    private static void testScreen(String level, int startStep, int pendingSteps,
                                   int expectedStep, boolean expectInner) throws Exception {
        final GameScreen[] view = new GameScreen[1];
        final Game[] game = new Game[1];
        final BoardPanel[] board = new BoardPanel[1];
        final Throwable[] error = {null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                view[0] = new GameScreen(TestPlayers.gameController(
                        new String[]{"A", "B"}, new String[]{"Green", "Red"}, level), level);
                game[0] = (Game) field(field(view[0], "turns"), "game");
                board[0] = (BoardPanel) field(view[0], "board");
                board[0].setActivePlayerColor("Green");
                check(board[0].placeActivePieceOnStart(0), level + ": setup piece");
                game[0].selectPiece(0);
                game[0].setPieceOnBoard(true);
                game[0].setPieceStep(startStep);
                game[0].setStage(Stage.MOVE);
                game[0].setLastStepDie(5);
                game[0].setPendingSteps(pendingSteps);
                board[0].animatePiece(0, startStep, startStep, null);
                removeEventSquares(board[0]);
                Method click = GameScreen.class.getDeclaredMethod("onBoardPieceClicked", int.class);
                click.setAccessible(true);
                click.invoke(view[0], 0);
                check(game[0].getStage() == Stage.BUSY, level + ": move in progress");
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError(level, error[0]);
        }
        Thread.sleep(220L * (pendingSteps + 4) + 800);
        SwingUtilities.invokeAndWait(() -> {
            try {
                check(game[0].isPieceInInnerPath(0, 0) == expectInner,
                        level + ": piece reached the inner path = " + expectInner);
                check(!game[0].isPieceInMiddlePath(0, 0),
                        level + ": piece is not left on the middle path");
                check(game[0].getPieceStep(0, 0) == expectedStep,
                        level + ": model step " + game[0].getPieceStep(0, 0) + " expected " + expectedStep);
                check(game[0].getCurrentPlayer() == 1, level + ": turn passed");
                board[0].setActivePlayerColor("Green");
                check(board[0].isActivePieceInInnerPath(0) == expectInner
                        && board[0].getActivePieceStep() == expectedStep,
                        level + ": board shows the same position as the model");
                ((Timer) field(view[0], "gameTimer")).stop();
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError(level, error[0]);
        }
        System.out.println("PASS - " + level + " screen: " + pendingSteps
                + " steps from step " + startStep);
    }

    /** Question and surprise squares open dialogs; remove them so the move is deterministic. */
    private static void removeEventSquares(BoardPanel board) throws Exception {
        GameController turns = (GameController) field(board, "turns");
        turns.removeSpecialSquares(
                SquareType.QUESTION, SquareType.SURPRISE);
    }

    public static void main(String[] args) throws Exception {
        testControllerRule();
        testBoardArrowSteps();
        // Medium: from step 6, 4 steps -> arrow at 8, 2 steps left -> inner step 2
        testScreen("Medium", 6, 4, 2, true);
        // Medium: stopping exactly on the arrow -> inner step 0
        testScreen("Medium", 5, 3, 0, true);
        // Hard: from step 7, 30 steps -> outer arrow 9 (28 left) -> middle arrow 8 (20 left) -> inner step 20
        testScreen("Hard", 7, 30, 20, true);
        // Hard: from step 7, 5 steps -> outer arrow 9 (3 left) -> middle step 3
        testMiddleStop();
        System.out.println("All arrow pass-through checks passed.");
    }

    // Hard: the piece stays on the middle path when the steps do not reach the middle arrow
    private static void testMiddleStop() throws Exception {
        final GameScreen[] view = new GameScreen[1];
        final Game[] game = new Game[1];
        final BoardPanel[] board = new BoardPanel[1];
        final Throwable[] error = {null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                view[0] = new GameScreen(TestPlayers.gameController(
                        new String[]{"A", "B"}, new String[]{"Green", "Red"}, "Hard"), "Hard");
                game[0] = (Game) field(field(view[0], "turns"), "game");
                board[0] = (BoardPanel) field(view[0], "board");
                board[0].setActivePlayerColor("Green");
                board[0].placeActivePieceOnStart(0);
                game[0].selectPiece(0);
                game[0].setPieceOnBoard(true);
                game[0].setPieceStep(7);
                game[0].setStage(Stage.MOVE);
                game[0].setLastStepDie(5);
                game[0].setPendingSteps(5);
                board[0].animatePiece(0, 7, 7, null);
                removeEventSquares(board[0]);
                Method click = GameScreen.class.getDeclaredMethod("onBoardPieceClicked", int.class);
                click.setAccessible(true);
                click.invoke(view[0], 0);
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError("Hard middle", error[0]);
        }
        Thread.sleep(220L * 9 + 800);
        SwingUtilities.invokeAndWait(() -> {
            try {
                check(game[0].isPieceInMiddlePath(0, 0) && !game[0].isPieceInInnerPath(0, 0),
                        "Hard: piece is on the middle path");
                check(game[0].getPieceStep(0, 0) == 3, "Hard: middle step 3");
                board[0].setActivePlayerColor("Green");
                check(board[0].isActivePieceInMiddlePath(0) && board[0].getActivePieceStep() == 3,
                        "Hard: board shows the piece on the middle path");
                ((Timer) field(view[0], "gameTimer")).stop();
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError("Hard middle", error[0]);
        }
        System.out.println("PASS - Hard screen: piece stops on the middle path");
    }
}
