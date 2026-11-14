package Tests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Controller.GameController;
import Controller.MovementController;
import Enums.SquareType;
import Enums.Stage;
import Model.Game;
import View.BoardPanel;
import View.GameScreen;

/**
 * SRS 3.2.4.3.2 - a piece moves to the next path when it passes over OR stops on an arrow
 * of its own colour. The arrows are 7 squares before the colour's corner:
 * Medium: outer ring (arrow at step 57) -> inner path (52).
 * Hard:   outer ring (arrow at step 65) -> middle ring (64, arrow at step 63) -> inner path (52).
 * Stopping on the arrow puts the piece on the first square of the next path;
 * each step that is left is then taken on the next path.
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
        Game passing = gameAt("Medium", 54, 6);
        check(new MovementController(passing).findArrowOnMove(0, 57) == 57,
                "Medium: passing over the arrow is detected");
        Game landing = gameAt("Medium", 54, 3);
        check(new MovementController(landing).findArrowOnMove(0, 57) == 57,
                "Medium: stopping on the arrow is detected");
        Game shortMove = gameAt("Medium", 50, 4);
        check(new MovementController(shortMove).findArrowOnMove(0, 57) == -1,
                "Medium: a move that does not reach the arrow ignores it");
        Game easy = gameAt("Easy", 50, 12);
        check(new MovementController(easy).findArrowOnMove(0, 57) == -1,
                "Easy: there are no arrows between paths");
        Game inner = gameAt("Medium", 45, 6);
        inner.setPieceInInnerPath(true);
        check(new MovementController(inner).findArrowOnMove(0, 51) == -1,
                "Medium: the arrow on the inner path does not move the piece again");

        // Hard: outer arrow (65) -> middle path, middle arrow (63) -> inner path
        Game hard = gameAt("Hard", 63, 5);
        MovementController hardPieces = new MovementController(hard);
        check(hardPieces.findArrowOnMove(0, 65) == 65, "Hard: outer arrow detected");
        check(hardPieces.movePiece(0, 65, 72), "Hard: move to the outer arrow");
        check(hardPieces.enterNextPathFromArrow(0, BoardPanel.GREEN, BoardPanel.GREEN)
                && hard.isPieceInMiddlePath() && !hard.isPieceInInnerPath(),
                "Hard: the outer arrow leads to the middle path, not to the inner path");
        check(hardPieces.continueMoveAfterArrow(0, 3, 64) && hard.getPieceStep() == 3,
                "Hard: the 3 steps that are left are taken on the middle path");

        Game middle = gameAt("Hard", 60, 5);
        middle.setPieceInMiddlePath(true);
        MovementController middlePieces = new MovementController(middle);
        check(middlePieces.findArrowOnMove(0, 63) == 63, "Hard: middle arrow detected");
        check(middlePieces.movePiece(0, 63, 64), "Hard: move to the middle arrow");
        check(middlePieces.enterNextPathFromArrow(0, BoardPanel.GREEN, BoardPanel.GREEN)
                && middle.isPieceInInnerPath() && !middle.isPieceInMiddlePath(),
                "Hard: the middle arrow leads to the inner path");
        check(middlePieces.continueMoveAfterArrow(0, 2, 52) && middle.getPieceStep() == 2,
                "Hard: the 2 steps that are left are taken on the inner path");
        check(!middlePieces.enterNextPathFromArrow(0, BoardPanel.GREEN, BoardPanel.GREEN),
                "Hard: an arrow on the inner path does nothing");

        System.out.println("PASS - arrow rule in the controller");
    }

    // 2. חוק המהלך החוקי: מהלך שמגיע לחץ חוקי גם כשמספר הצעדים חורג מסוף הטבעת
    private static void testLegalMoveReachesArrow() {
        // 10 steps from step 55 would pass the end of the 64-square ring, but the arrow is reached
        Game game = gameAt("Medium", 55, 10);
        MovementController pieces = new MovementController(game);
        int[] sizes = {64, 64, 64, 64};
        int[] arrows = {57, -1, -1, -1};
        check(!pieces.hasAnyLegalMove(sizes),
                "without arrow information the move looks like it leaves the ring");
        check(pieces.hasAnyLegalMove(sizes, arrows),
                "a move that reaches the arrow is legal");
        // a move that stops short of the arrow, but is still on the ring, is legal too
        Game normal = gameAt("Medium", 30, 10);
        check(new MovementController(normal).hasAnyLegalMove(sizes, arrows),
                "a normal move on the ring is legal");
        System.out.println("PASS - a move that reaches the arrow is legal");
    }

    // 3. מיקום החצים והמסלולים על הלוח
    private static void testBoardArrowSteps() {
        BoardPanel medium = new BoardPanel(
                TestPlayers.gameControllerForBoard(
                        "Medium", new String[]{"Green", "Red"}));
        medium.setActivePlayerColor("Green");
        check(medium.placeActivePieceOnStart(0), "Medium: place piece");
        check(medium.getActiveRouteSize(0) == 64 && medium.getOwnArrowStep(0) == 57,
                "Medium: own arrow at step 57 of the 64 ring");
        check(medium.showActivePieceInInnerPath(0, 0)
                && medium.getActiveRouteSize(0) == 52 && medium.getOwnArrowStep(0) == -1,
                "Medium: inner path has 52 squares and no arrow to use");

        BoardPanel hard = new BoardPanel(
                TestPlayers.gameControllerForBoard(
                        "Hard", new String[]{"Green", "Red"}));
        hard.setActivePlayerColor("Green");
        check(hard.placeActivePieceOnStart(0), "Hard: place piece");
        check(hard.getActiveRouteSize(0) == 72 && hard.getOwnArrowStep(0) == 65,
                "Hard: own arrow at step 65 of the 72 ring");
        check(hard.showActivePieceInMiddlePath(0, 0)
                && hard.getActiveRouteSize(0) == 64 && hard.getOwnArrowStep(0) == 63,
                "Hard: middle path has 64 squares and its own arrow at step 63");
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

    // 4. מסך המשחק: לחיצה על כלי שעובר את החץ או עוצר עליו
    private static void testScreen(String level, boolean startOnMiddle, int startStep,
                                   int pendingSteps, String expectedPath,
                                   int expectedStep) throws Exception {
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
                removeEventSquares(board[0]);
                game[0].selectPiece(0);
                game[0].setPieceOnBoard(true);
                if (startOnMiddle) {
                    check(board[0].showActivePieceInMiddlePath(0, startStep),
                            level + ": setup middle path");
                    game[0].setPieceInMiddlePath(true);
                } else {
                    board[0].animatePiece(0, startStep, startStep, null);
                }
                game[0].setPieceStep(startStep);
                game[0].setStage(Stage.MOVE);
                game[0].setLastStepDie(5);
                game[0].setPendingSteps(pendingSteps);
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
                String where = level + " " + (startOnMiddle ? "middle " : "outer ") + startStep
                        + " + " + pendingSteps;
                boolean inner = "inner".equals(expectedPath);
                boolean middle = "middle".equals(expectedPath);
                check(game[0].isPieceInInnerPath(0, 0) == inner
                        && game[0].isPieceInMiddlePath(0, 0) == middle,
                        where + ": the piece must be on the " + expectedPath + " path");
                check(game[0].getPieceStep(0, 0) == expectedStep,
                        where + ": model step " + game[0].getPieceStep(0, 0)
                                + " expected " + expectedStep);
                check(game[0].getCurrentPlayer() == 1, where + ": turn passed");
                board[0].setActivePlayerColor("Green");
                check(board[0].isActivePieceInInnerPath(0) == inner
                        && board[0].isActivePieceInMiddlePath(0) == middle
                        && board[0].getActivePieceStep() == expectedStep,
                        where + ": board shows the same position as the model");
                ((Timer) field(view[0], "gameTimer")).stop();
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError(level, error[0]);
        }
        System.out.println("PASS - " + level + " screen: " + (startOnMiddle ? "middle " : "outer ")
                + startStep + " + " + pendingSteps + " steps -> " + expectedPath + " step " + expectedStep);
    }

    /** Question and surprise squares open dialogs; remove them so the move is deterministic. */
    private static void removeEventSquares(BoardPanel board) throws Exception {
        GameController turns = (GameController) field(board, "turns");
        turns.removeSpecialSquares(
                SquareType.QUESTION, SquareType.SURPRISE);
    }

    public static void main(String[] args) throws Exception {
        testControllerRule();
        testLegalMoveReachesArrow();
        testBoardArrowSteps();
        // Medium: from step 55, 4 steps -> arrow at 57 (2 steps), 2 left -> inner step 2
        testScreen("Medium", false, 55, 4, "inner", 2);
        // Medium: stopping exactly on the arrow -> first square of the inner path
        testScreen("Medium", false, 54, 3, "inner", 0);
        // Medium: a move that does not reach the arrow stays on the ring
        testScreen("Medium", false, 50, 4, "outer", 54);
        // Hard: from step 63, 5 steps -> outer arrow at 65 (2 steps), 3 left -> middle step 3
        testScreen("Hard", false, 63, 5, "middle", 3);
        // Hard: a long move: 30 steps from step 62 -> arrow at 65 (3 steps), 27 left on the middle ring
        testScreen("Hard", false, 62, 30, "middle", 27);
        // Hard: from the middle ring, step 60, 5 steps -> middle arrow at 63, 2 left -> inner step 2
        testScreen("Hard", true, 60, 5, "inner", 2);
        System.out.println("All arrow pass-through checks passed.");
    }
}
