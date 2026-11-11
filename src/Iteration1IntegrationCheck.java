import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import controller.GameController;
import enums.RollAction;
import model.Game;
import view.BoardPanel;

public class Iteration1IntegrationCheck {

    // בדיקת תנאי והצגת הודעת שגיאה אם אינו מתקיים
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // בדיקת שילוב רכיבי המשחק ברמת קושי מסוימת
    private static void testDifficulty(
            String level,
            int[] expectedRoutes,
            int expectedBoostFaces) throws Exception {

        String[] names = {"Player1", "Player2"};
        String[] colors = {"Red", "Blue"};

        // יצירת המודל, הבקר והלוח
        Game game = new Game(names, colors, level);
        GameController controller = new GameController(game);
        BoardPanel board = new BoardPanel(level, colors);

        // 3.2.1 - מצב התחלתי של משחק
        check(controller.getCurrentPlayer() == 0,
                level + ": wrong starting player");

        check(controller.canRoll(),
                level + ": first player cannot roll");

        check(controller.hasPieceInCastle(),
                level + ": no pieces in castle");

        // 3.2.2 - מבנה המסלולים בהתאם לרמת הקושי
        check(board.getSharedRouteCount() == expectedRoutes.length,
                level + ": wrong number of routes");

        for (int i = 0; i < expectedRoutes.length; i++) {
            check(board.getSharedRouteSize(i) == expectedRoutes[i],
                    level + ": wrong route size at index " + i);
        }

        // 3.2.3 - סוגי הקוביות וטווחי התוצאות
        check(controller.getBoostDieFaces() == expectedBoostFaces,
                level + ": wrong Boost die configuration");

        for (int i = 0; i < 30; i++) {

            int stepDie = controller.rollStepDie();
            int boostDie = controller.rollBoostDie();

            check(stepDie >= 1 && stepDie <= 6,
                    level + ": invalid step die result");

            if (expectedBoostFaces == 0) {
                check(boostDie == 0,
                        level + ": Easy must not use Boost");
            } else {
                check(boostDie >= 1
                        && boostDie <= expectedBoostFaces,
                        level + ": invalid Boost result");
            }
        }

        // בדיקת הפעלת שעון המשחק
        controller.startGameClock();

        check(controller.getElapsedTime() != null
                && !controller.getElapsedTime().isEmpty(),
                level + ": game clock not available");

        // 3.2.3 - הטלת 6 מאפשרת הוצאת כלי מהטירה
        int boostForTest = expectedBoostFaces == 0 ? 0 : 2;

        check(!controller.registerRollAndCheckThirdSix(6),
                level + ": first six cancelled");

        check(controller.finishRoll(6, boostForTest)
                == RollAction.TAKE_OUT,
                level + ": castle exit not offered");

        // הצבת הכלי על הלוח בתצוגה
        SwingUtilities.invokeAndWait(() -> {
            check(board.placeActivePieceOnStart(0),
                    level + ": board castle exit failed");
        });

        // עדכון המודל באמצעות הבקר
        controller.takeOutActivePiece(0);

        check(controller.hasPieceOnBoard(),
                level + ": model castle exit failed");

        // 3.2.4 - הטלה רגילה ובחירת כלי לתנועה
        check(!controller.registerRollAndCheckThirdSix(2),
                level + ": normal roll cancelled");

        check(controller.finishRoll(2, boostForTest)
                == RollAction.MOVE,
                level + ": move stage not reached");

        int routeSize = board.getActiveRouteSize(0);

        int targetStep = controller.validatePieceMove(
                0, routeSize);

        check(targetStep == 2,
                level + ": wrong movement target");

        // שמירת התנועה במודל
        check(controller.movePiece(0, targetStep, routeSize),
                level + ": model movement failed");

        check(controller.getPieceStep(0) == targetStep,
                level + ": model position not updated");

        // הפעלת אנימציית התנועה בלוח
        CountDownLatch animationDone = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            board.animatePiece(
                    0,
                    0,
                    targetStep,
                    animationDone::countDown);
        });

        // ממתינים לאנימציה בלי לחסום את תהליכון הממשק
        check(animationDone.await(5, TimeUnit.SECONDS),
                level + ": board animation did not finish");

        final int[] boardStep = new int[1];

        SwingUtilities.invokeAndWait(() -> {
            boardStep[0] = board.getActivePieceStep();
        });

        // בדיקת התאמה בין מיקום הכלי במודל ובלוח
        check(boardStep[0] == targetStep,
                level + ": board position not updated");

        // 3.2.3 - העברת תור לאחר סיום המהלך
        controller.passTurn();

        check(controller.getCurrentPlayer() == 1,
                level + ": next player not selected");

        check(controller.canRoll(),
                level + ": next player cannot roll");

        System.out.println(
                "PASS - " + level + " integration");
    }

    public static void main(String[] args) throws Exception {

        // Easy: מסלול משותף אחד של 52 משבצות
        testDifficulty("Easy", new int[]{52}, 0);

        // Medium: מסלולים של 52 ו-64 משבצות
        testDifficulty("Medium", new int[]{52, 64}, 3);

        // Hard: מסלולים של 52, 64 ו-72 משבצות
        testDifficulty("Hard", new int[]{52, 64, 72}, 6);

        System.out.println(
                "PASS - Iteration 1 integration smoke test");

        System.exit(0);
    }
}