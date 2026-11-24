package Tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;
import Model.Game;
import Model.Player;
import View.BoardPanel;

/**
 * בדיקת אינטגרציה: בחירת שאלה, חישוב תנועה,
 * עדכון המודל ואנימציית הכלי על הלוח.
 */
public final class QuestionMovementIntegrationCheck {

    private QuestionMovementIntegrationCheck() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // תרחיש הבדיקה כולל את המודל, הבקרים ולוח המשחק האמיתי.
    private static void checkQuestionMovement(
            Game game, GameController controller,
            MovementController movement, BoardPanel board,
            String difficulty, boolean shouldAnswerCorrectly,
            int landingStep) throws Exception {

        // הצבת הכלי בנקודת הנחיתה על משבצת השאלה
        game.selectPiece(0);
        game.getSelectedPiece().setSteps(landingStep);
        game.setStage(Stage.BUSY);

        check("QUESTION".equals(board.getSquareEventAtStep(0, landingStep)),
                "Expected a question square");
        check(difficulty.equals(
                board.getQuestionDifficultyAtStep(0, landingStep)),
                "Incorrect landing square difficulty");

        // בחירת שאלה אמיתית מהמאגר לפי המשבצת
        controller.selectQuestion(difficulty);
        check(difficulty.equalsIgnoreCase(controller.getQuestionDifficulty()),
                "Question difficulty does not match the square");

        // בדיקת תשובה נכונה או שגויה לפי התרחיש
        int correctIndex = controller.getCorrectQuestionAnswerIndex();
        int selectedIndex = shouldAnswerCorrectly
                ? correctIndex : (correctIndex + 1) % 4;
        boolean correct = controller.isCorrectQuestionAnswer(selectedIndex);
        check(correct == shouldAnswerCorrectly,
                "Answer correctness is wrong");

        // חישוב היעד על פי רמת השאלה
        int questionSteps = controller.getCurrentQuestionMoveSteps();
        int expectedSteps = switch (difficulty) {
            case "easy" -> 1;
            case "medium" -> 2;
            case "hard" -> 3;
            default -> throw new AssertionError("Unknown difficulty");
        };
        check(questionSteps == expectedSteps, "Incorrect movement distance");

        int expectedTarget = landingStep
                + (correct ? questionSteps : -questionSteps);
        int target = movement.getAnswerMoveTarget(
                correct, questionSteps, board.getActiveRouteSize(0));
        check(target == expectedTarget, "Incorrect movement target");

        // ביצוע המהלך במודל והנפשת אותו המהלך בלוח
        check(movement.applyEventMovement(
                0, target, board.getActiveRouteSize(0)),
                "Model rejected the question movement");
        check(game.getSelectedPiece().getSteps() == expectedTarget,
                "Model did not update the piece position");

        CountDownLatch animationFinished = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() ->
                board.animatePiece(0, landingStep, target,
                        animationFinished::countDown));

        check(animationFinished.await(8, TimeUnit.SECONDS),
                "Board animation did not finish");
        final int[] shownStep = new int[1];
        SwingUtilities.invokeAndWait(() ->
                shownStep[0] = board.getActivePieceStep());
        check(shownStep[0] == expectedTarget,
                "Board view is not synchronized with the model");

        System.out.println("PASS - " + difficulty + ": "
                + (correct ? "correct" : "wrong")
                + ", " + landingStep + " -> " + expectedTarget);
    }

    public static void main(String[] args) throws Exception {

        // עקב מיקום המשבצות האקראי, ננסה לוח חדש אם אין
        // משבצת מכל רמה במרחק בטוח משני קצוות המסלול.
        for (int attempt = 0; attempt < 20; attempt++) {
            Player[] players = {
                    new Player("A", "Green"),
                    new Player("B", "Blue")
            };
            Game game = new Game(players, "Hard");
            MovementController movement = new MovementController(game);
            GameController controller = GameController.forTesting(game, movement);
            final BoardPanel[] boardHolder = new BoardPanel[1];
            SwingUtilities.invokeAndWait(() ->
                    boardHolder[0] = new BoardPanel(controller));
            BoardPanel board = boardHolder[0];
            final boolean[] placed = new boolean[1];
            SwingUtilities.invokeAndWait(() ->
                    placed[0] = board.placeActivePieceOnStart(0));
            check(placed[0], "Could not place a piece on the board");
            game.selectPiece(0);
            game.getSelectedPiece().setOnBoard(true);

            String[] difficulties = {"easy", "medium", "hard"};
            int[] positions = {-1, -1, -1};
            int routeSize = board.getActiveRouteSize(0);

            // איתור משבצת שאלה מכל רמה במרחק בטוח מהגבולות
            for (int step = 4; step < routeSize - 4; step++) {
                if (!"QUESTION".equals(board.getSquareEventAtStep(0, step))) {
                    continue;
                }
                String difficulty =
                        board.getQuestionDifficultyAtStep(0, step);
                for (int i = 0; i < difficulties.length; i++) {
                    if (positions[i] < 0
                            && difficulties[i].equals(difficulty)) {
                        positions[i] = step;
                    }
                }
            }

            boolean hasAllLevels = true;
            for (int position : positions) {
                if (position < 0) {
                    hasAllLevels = false;
                }
            }
            if (!hasAllLevels) {
                continue;
            }

            for (int i = 0; i < difficulties.length; i++) {
                checkQuestionMovement(game, controller, movement, board,
                        difficulties[i], true, positions[i]);
                checkQuestionMovement(game, controller, movement, board,
                        difficulties[i], false, positions[i]);
            }

            System.out.println(
                    "All question movement integration checks passed.");
            return;
        }
        throw new AssertionError(
                "Could not find suitable question squares after 20 boards");
    }
}
