package Tests;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;

import View.GameScreen;
import View.BoardPanel;
import Model.Game;
import Enums.Stage;

public class HomeEntryScreenCheck {

    // גישה לרכיב לצורך בדיקת אינטגרציה בלבד
    private static Object getField(Object obj, String name)
            throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }

    public static void main(String[] args) throws Exception {

        final Throwable[] error = {null};

        SwingUtilities.invokeAndWait(() -> {
            try {
                // יצירת מסך משחק ברמה קלה
                GameScreen view = new GameScreen(TestPlayers.of(new String[]{"Player1", "Player2"}, new String[]{"Red", "Blue"}), "Easy");

                BoardPanel board = (BoardPanel)
                    getField(view, "board");

                Game game = (Game) getField(view, "game");

                // הכנת כלי לקראת סוף המסלול המשותף
                board.placeActivePieceOnStart(0);
                game.setPieceOnBoard(true);
                game.setPieceStep(50);
                game.setPendingSteps(3);
                game.setStage(Stage.MOVE);

                // הדמיית לחיצה על הכלי במסך המשחק
                Method click = GameScreen.class.getDeclaredMethod(
                    "onBoardPieceClicked", int.class
                );
                click.setAccessible(true);
                click.invoke(view, 0);

                // בדיקת כניסה למסלול הבית במודל
                if (!game.isPieceInHomePath(0, 0)
                        || game.getPieceStep(0, 0) != 1) {
                    throw new AssertionError(
                        "Model home entry failed"
                    );
                }

                // בדיקת התאמה בין המודל לתצוגת הלוח
                board.setActivePlayerColor("Red");

                if (!board.isActivePieceInHomePath(0)
                        || board.getActivePieceStep() != 1) {
                    throw new AssertionError(
                        "Board home entry failed"
                    );
                }

                System.out.println(
                    "PASS - Home entry synchronized"
                );

            } catch (Throwable t) {
                error[0] = t;
            }
        });

        if (error[0] != null) {
            throw new AssertionError(
                "Home entry integration failed", error[0]
            );
        }

        System.exit(0);
    }
}