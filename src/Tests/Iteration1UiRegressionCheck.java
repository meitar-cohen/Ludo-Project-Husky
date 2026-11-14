package Tests;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Controller.GameController;
import Controller.MovementController;
import Enums.RollAction;
import Model.Game;
import View.BoardPanel;
import View.GameScreen;

public class Iteration1UiRegressionCheck {

    // בדיקת תנאי והצגת שגיאה במקרה של כישלון
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // קריאת שדה פרטי לצורך בדיקה בלבד
    private static Object field(Object source, String name)
            throws Exception {

        Field f = source.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(source);
    }

    // בדיקה שתוצאות הקוביות אינן נעלמות לאחר מעבר תור
    private static void testDiceResult() throws Exception {

        final Throwable[] problem = {null};

        SwingUtilities.invokeAndWait(() -> {

            GameScreen view = null;

            try {
                view = new GameScreen(TestPlayers.gameController(
                        new String[]{"A", "B"}, new String[]{"Red", "Blue"}, "Medium"),
                        "Medium");

                Object sidebar = field(view, "sidebar");
                Object dice = field(sidebar, "dice");

                Method show = dice.getClass().getDeclaredMethod(
                        "show", int.class, int.class);

                show.setAccessible(true);
                show.invoke(dice, 2, 2);

                Method passTurn =
                        GameScreen.class.getDeclaredMethod("passTurn");

                passTurn.setAccessible(true);
                passTurn.invoke(view);

                int step = (Integer) field(dice, "stepValue");
                int boost = (Integer) field(dice, "boostValue");

                check(step == 2 && boost == 2,
                        "Dice results vanished after passing the turn");

            } catch (Throwable t) {
                problem[0] = t;

            } finally {
                if (view != null) {
                    try {
                        ((Timer) field(view, "gameTimer")).stop();
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        if (problem[0] != null) {
            throw new AssertionError(
                    "Dice display regression", problem[0]);
        }

        System.out.println(
                "PASS - Dice values remain after turn passing");
    }

    // בדיקת התחלה אחת לכל צבע וכמות חצים תקינה
    private static void testStartAndArrowCounts() throws Exception {

        String[] colors = {
                "Green", "Yellow", "Blue", "Red"
        };

        String[] levels = {
                "Easy", "Medium", "Hard"
        };

        int[] arrowsExpected = {4, 8, 12};

        for (int i = 0; i < levels.length; i++) {

            BoardPanel board = new BoardPanel(levels[i], colors);

            Map<?, ?> squares =
                    (Map<?, ?>) field(board, "squares");

            int starts = 0;
            int arrows = 0;

            Map<Color, Integer> startColors = new HashMap<>();

            for (Object special : squares.values()) {

                String kind = TestPlayers.specialSquareType(special);

                if ("START".equals(kind)) {

                    starts++;

                    Color color = TestPlayers.specialSquareColor(special);

                    startColors.put(color,
                            startColors.getOrDefault(color, 0) + 1);

                } else if ("ARROW".equals(kind)) {
                    arrows++;
                }
            }

            check(starts == 4,
                    levels[i] + ": expected 4 start squares");

            check(startColors.size() == 4,
                    levels[i] + ": expected one start per color");

            for (int count : startColors.values()) {
                check(count == 1,
                        levels[i] + ": duplicate start square");
            }

            check(arrows == arrowsExpected[i],
                    levels[i] + ": incorrect arrow count");
        }

        System.out.println(
                "PASS - One start per color and correct arrow counts");
    }

    // בדיקת התקדמות ב-Medium לאחר הוצאת כלי עם 6
    private static void testMediumMoveAfterCastle() {

        String[] names = {"A", "B"};
        String[] colors = {"Red", "Blue"};

        Game game = new Game(TestPlayers.of(names, colors), "Medium");
        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);
        BoardPanel board = new BoardPanel("Medium", colors);

        board.setActivePlayerColor("Red");

        // הטלת 6 ראשונה
        check(!controller.registerRollAndCheckThirdSix(6),
                "First six cancelled unexpectedly");

        check(controller.finishRoll(6, 2) == RollAction.TAKE_OUT,
                "Six did not allow castle exit");

        // הוצאת הכלי מהטירה
        check(board.placeActivePieceOnStart(0),
                "Could not place piece on start");

        controllerPieces.takeOutActivePiece(0);

        check(game.isPieceOnBoard(0, 0),
                "Piece missing from model after castle exit");

        check(controller.canRoll(),
                "Cannot roll again after castle exit");

        // הטלה שנייה: 2 בקובייה ו-2 ב-Boost
        check(!controller.registerRollAndCheckThirdSix(2),
                "Second roll cancelled unexpectedly");

        check(controller.finishRoll(2, 2) == RollAction.MOVE,
                "Medium 2 with Boost 2 must allow movement");

        int routeSize = board.getActiveRouteSize(0);

        check(routeSize > 2,
                "Invalid shared route size");

        // בדיקת חישוב יעד התנועה
        int target =
                controllerPieces.validatePieceMove(0, routeSize);

        check(target == 2,
                "Expected target 2, got " + target);

        // ביצוע התנועה במודל
        check(controllerPieces.movePiece(0, target, routeSize),
                "Controller rejected legal movement");

        check(game.getPieceStep(0, 0) == 2,
                "Piece position was not updated");

        System.out.println(
                "PASS - Medium: exit with 6, then move with 2 and Boost 2");
    }

    public static void main(String[] args) throws Exception {

        testDiceResult();
        testStartAndArrowCounts();
        testMediumMoveAfterCastle();

        System.out.println(
                "PASS - Iteration 1 UI regression checks");

        System.exit(0);
    }
}