
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import controller.GameController;
import enums.RollAction;
import enums.Stage;
import model.Game;
import view.BoardPanel;
import view.GameView;

public class CastleViewAcceptanceCheck {

    // קריאת שדה לצורך בדיקת אינטגרציה בלבד
    private static Object field(Object object, String name)
            throws Exception {

        Field f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void test(String level) throws Exception {

        final Throwable[] problem = {null};

        SwingUtilities.invokeAndWait(() -> {

            GameView view = null;

            try {

                // יצירת מסך המשחק
                view = new GameView(
                        new String[]{"A", "B"},
                        new String[]{"Red", "Blue"},
                        level);

                GameController controller =
                        (GameController) field(view, "controller");

                Game game =
                        (Game) field(controller, "game");

                BoardPanel board =
                        (BoardPanel) field(view, "board");

                JButton button =
                        (JButton) field(view, "rollButton");

                Method castleClick =
                        GameView.class.getDeclaredMethod(
                                "onCastlePieceClicked", int.class);

                castleClick.setAccessible(true);

                board.setActivePlayerColor("Red");

                // הטלת 6 - עם Boost 1 ברמות הרלוונטיות
                int bonus = "Easy".equals(level) ? 0 : 1;

                controller.registerRollAndCheckThirdSix(6);

                check(controller.finishRoll(6, bonus)
                        == RollAction.TAKE_OUT,
                        level + ": take-out stage");

                // לחיצה על הכלי בטירה
                castleClick.invoke(view, 0);

                // בדיקת מצב הכלי במודל
                check(game.isPieceOnBoard(0, 0)
                        && game.getPieceStep(0, 0) == 0,
                        level + ": model position");

                // בדיקת הופעת הכלי בלוח
                check(board.hasPieceOnBoard()
                        && board.getActivePieceStep() == 0,
                        level + ": board position");

                // בדיקת מספר הכלים בטירות
                check(game.getPiecesInCastle(0) == 3
                        && game.getPiecesInCastle(1) == 4,
                        level + ": castle counts");

                // בדיקת אפשרות להטלה נוספת
                check(game.getStage() == Stage.ROLL
                        && button.isEnabled()
                        && "Roll Dice".equals(button.getText()),
                        level + ": extra roll");

                // לחיצה חוזרת אינה מוציאה שוב את אותו הכלי
                game.setStage(Stage.TAKE_OUT);
                castleClick.invoke(view, 0);

                check(game.getPiecesInCastle(0) == 3,
                        level + ": duplicate click rejected");

            } catch (Throwable t) {
                problem[0] = t;

            } finally {

                // עצירת שעון המשחק של הבדיקה
                if (view != null) {
                    try {
                        ((Timer) field(view, "gameTimer")).stop();
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        if (problem[0] != null) {
            throw new AssertionError(level, problem[0]);
        }

        System.out.println(
                "PASS - " + level + " castle screen");
    }

    public static void main(String[] args) throws Exception {

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            test(level);
        }

        System.out.println(
                "PASS - SRS 3.2.3.3.4 view acceptance");

        System.exit(0);
    }
}
