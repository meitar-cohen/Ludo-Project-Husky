package tests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import controller.GameController;
import enums.RollAction;
import enums.Stage;
import model.Game;
import view.GameView;

public class StepZeroAcceptanceCheck {

    // השוואת התוצאה בפועל לתוצאה הצפויה
    private static void expect(
            Object expected, Object actual, String name) {

        if (!expected.equals(actual)) {
            throw new AssertionError(name
                    + ": expected " + expected
                    + ", actual " + actual);
        }
    }

    // יצירת משחק חדש לצורך בדיקה
    private static Game newGame(String level) {

        return new Game(
                new String[]{"A", "B"},
                new String[]{"Red", "Blue"},
                level
        );
    }

    // גישה לשדות פרטיים לצורך בדיקת המסך בלבד
    private static Object field(Object obj, String name)
            throws Exception {

        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    // בדיקת הנוסחאות בשלוש רמות הקושי
    private static void testFormulas() {

        expect(4,
                new GameController(newGame("Easy"))
                        .calculateSteps(4, 0),
                "Easy");

        expect(8,
                new GameController(newGame("Medium"))
                        .calculateSteps(4, 3),
                "Medium Boost 3");

        expect(0,
                new GameController(newGame("Medium"))
                        .calculateSteps(5, 1),
                "Medium Boost 1");

        expect(30,
                new GameController(newGame("Hard"))
                        .calculateSteps(6, 6),
                "Hard UltraBoost 6");

        expect(0,
                new GameController(newGame("Hard"))
                        .calculateSteps(3, 1),
                "Hard UltraBoost 1");

        System.out.println(
                "PASS - formula in Easy, Medium and Hard");
    }

    // בדיקה שתוצאה 0 מונעת תנועה במסלול המשותף
    private static void testZeroNoBoardMove(String level) {

        Game game = newGame(level);

        game.selectPiece(0);
        game.setPieceOnBoard(true);

        GameController controller = new GameController(game);
        controller.registerRollAndCheckThirdSix(4);

        expect(RollAction.NEED_SIX,
                controller.finishRoll(4, 1),
                level + " zero on shared route");

        expect(0, game.getPendingSteps(),
                level + " zero stored");

        System.out.println(
                "PASS - " + level + " zero blocks shared path");
    }

    // בדיקה שתוצאה 0 מאפשרת תנועה במסלול הבית
    private static void testZeroHomeMove(String level) {

        Game game = newGame(level);

        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setPieceInHomePath(true);
        game.setPieceStep(1);

        GameController controller = new GameController(game);
        controller.registerRollAndCheckThirdSix(4);

        expect(RollAction.MOVE,
                controller.finishRoll(4, 1),
                level + " home allowed");

        expect(5,
                controller.validatePieceMove(0, 6),
                level + " target 5");

        expect(true,
                controller.movePiece(0, 5, 6),
                level + " apply home move");

        expect(5, game.getPieceStep(),
                level + " home position");

        System.out.println(
                "PASS - " + level + " zero permits home path");
    }

    // בדיקה שלא ניתן לחרוג ממסלול הבית
    private static void testZeroHomeBlocked(String level) {

        Game game = newGame(level);

        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setPieceInHomePath(true);
        game.setPieceStep(4);

        GameController controller = new GameController(game);
        controller.registerRollAndCheckThirdSix(4);

        expect(RollAction.NEED_SIX,
                controller.finishRoll(4, 1),
                level + " no legal home move");

        System.out.println(
                "PASS - " + level + " zero blocks overshoot");
    }

    // בדיקה שקובייה 6 מאפשרת יציאה מהטירה גם כשהמכפיל נותן 0
    private static void testZeroAndSixExitsCastle(String level) {

        Game game = newGame(level);

        GameController controller = new GameController(game);
        controller.registerRollAndCheckThirdSix(6);

        expect(RollAction.TAKE_OUT,
                controller.finishRoll(6, 1),
                level + " six can exit castle");

        expect(Stage.TAKE_OUT, game.getStage(),
                level + " take out stage");

        System.out.println(
                "PASS - " + level
                + " 6 exits castle with zero Boost");
    }

    // בדיקה שהתור עובר אוטומטית במסך המשחק
    private static void testAutomaticPass(
            String level, boolean hasSharedPiece) throws Exception {

        final Throwable[] problem = new Throwable[1];

        SwingUtilities.invokeAndWait(() -> {

            GameView view = null;

            try {

                view = new GameView(
                        new String[]{"A", "B"},
                        new String[]{"Red", "Blue"},
                        level
                );

                GameController controller =
                        (GameController) field(view, "controller");

                Game game =
                        (Game) field(controller, "game");

                // בדיקה גם עם כלי במסלול המשותף
                if (hasSharedPiece) {
                    game.selectPiece(0);
                    game.setPieceOnBoard(true);
                }

                // הפעלת סיום הטלה עם תוצאה 0
                Method finish = GameView.class.getDeclaredMethod(
                        "finishRoll", int.class, int.class);

                finish.setAccessible(true);
                finish.invoke(view, 4, 1);

                // בדיקה שהתור עבר לשחקן הבא
                expect(1, game.getCurrentPlayer(),
                        level + " next player");

                expect(Stage.ROLL, game.getStage(),
                        level + " roll stage");

                // בדיקה שכפתור ההטלה מוכן לשחקן הבא
                JButton button =
                        (JButton) field(view, "rollButton");

                expect("Roll Dice", button.getText(),
                        level + " roll button");

                expect(true, button.isEnabled(),
                        level + " roll enabled");

                // בדיקת הודעה מתאימה לשחקן
                JTextArea status =
                        (JTextArea) field(view, "statusArea");

                if (!status.getText()
                        .contains("Boost result is 0")) {

                    throw new AssertionError(
                            level + " missing zero status");
                }

            } catch (Throwable error) {
                problem[0] = error;

            } finally {

                // עצירת שעון הבדיקה
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
                    level + " GUI", problem[0]);
        }

        System.out.println(
                "PASS - " + level
                + " zero passes turn automatically "
                + (hasSharedPiece
                        ? "with shared piece"
                        : "with all pieces in castle"));
    }

    public static void main(String[] args) throws Exception {

        testFormulas();

        for (String level : new String[]{"Medium", "Hard"}) {

            testZeroNoBoardMove(level);
            testZeroHomeMove(level);
            testZeroHomeBlocked(level);
            testZeroAndSixExitsCastle(level);

            testAutomaticPass(level, false);
            testAutomaticPass(level, true);
        }

        System.out.println(
                "PASS - SRS 3.2.3.3.3 acceptance tests");

        System.exit(0);
    }
}
