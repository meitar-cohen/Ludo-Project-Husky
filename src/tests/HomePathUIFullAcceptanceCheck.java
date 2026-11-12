package tests;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import controller.GameController;
import enums.Stage;
import model.Game;
import view.GameView;
import view.BoardPanel;

public class HomePathUIFullAcceptanceCheck {

    // קריאת שדות לצורך בדיקת האינטגרציה בלבד
    private static Object getField(Object object, String name)
            throws Exception {

        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    // בדיקת תנאי והצגת שגיאה אם הוא נכשל
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {

        String[] levels = {"Easy", "Medium", "Hard"};

        for (String level : levels) {

            final GameView[] view = new GameView[1];
            final Game[] game = new Game[1];
            final BoardPanel[] board = new BoardPanel[1];
            final Throwable[] error = new Throwable[1];

            SwingUtilities.invokeAndWait(() -> {
                try {
                    // יצירת מסך משחק לפי רמת הקושי
                    view[0] = new GameView(TestPlayers.of(new String[]{"A", "B"}, new String[]{"Red", "Blue"}), level);

                    GameController controller =
                            (GameController) getField(
                                    view[0], "controller");

                    game[0] = (Game) getField(controller, "game");
                    board[0] = (BoardPanel) getField(view[0], "board");

                    board[0].setActivePlayerColor("Red");

                    // הכנת כלי בסוף המסלול המשותף
                    check(board[0].placeActivePieceOnStart(0),
                            level + ": castle exit");

                    game[0].setPieceOnBoard(true);
                    game[0].setPieceStep(50);

                    // ברמות Medium ו-Hard הכלי נמצא במסלול הפנימי
                    if (!"Easy".equals(level)) {
                        game[0].setPieceInInnerPath(true);

                        check(board[0].showActivePieceInInnerPath(0, 50),
                                level + ": inner path");
                    }

                    check(board[0].getActiveRouteSize(0) == 52,
                            level + ": shared route size");

                    game[0].setPendingSteps(3);
                    game[0].setStage(Stage.MOVE);
                    controller.registerRollAndCheckThirdSix(3);

                    // הפעלת בחירת הכלי מתוך GameView
                    Method click = GameView.class.getDeclaredMethod(
                            "onBoardPieceClicked", int.class);

                    click.setAccessible(true);
                    click.invoke(view[0], 0);

                    // בדיקת כניסה למסלול הבית במודל ובתצוגה
                    check(game[0].isPieceInHomePath(0, 0),
                            level + ": model home entry");

                    check(game[0].getPieceStep(0, 0) == 1,
                            level + ": model home position");

                    check(board[0].isActivePieceInHomePath(0),
                            level + ": board home entry");

                    check(board[0].getActivePieceStep() == 1,
                            level + ": board home position");

                    // החזרת התור לשחקן הראשון לצורך הבדיקה
                    game[0].setCurrentPlayer(0);
                    board[0].setActivePlayerColor("Red");

                    // כלי נוסף נשאר במסלול המשותף
                    check(board[0].placeActivePieceOnStart(1),
                            level + ": second piece");

                    game[0].selectPiece(1);
                    game[0].setPieceOnBoard(true);
                    game[0].setPieceStep(0);

                    // קובייה רגילה 4, מכפיל שנותן 0
                    game[0].setPendingSteps(0);
                    game[0].setStage(Stage.MOVE);
                    controller.registerRollAndCheckThirdSix(4);

                    // כלי במסלול המשותף אינו יכול לזוז
                    click.invoke(view[0], 1);

                    check(game[0].getStage() == Stage.MOVE,
                            level + ": shared move rejected");

                    check(game[0].getPieceStep(0, 1) == 0,
                            level + ": shared piece unchanged");

                    // כלי במסלול הבית מתקדם ארבע משבצות
                    click.invoke(view[0], 0);

                    check(game[0].getPieceStep(0, 0) == 5,
                            level + ": home movement in model");

                } catch (Throwable t) {
                    error[0] = t;
                }
            });

            if (error[0] != null) {
                throw new AssertionError(level, error[0]);
            }

            // המתנה להשלמת האנימציה
            Thread.sleep(1500);

            SwingUtilities.invokeAndWait(() -> {
                try {
                    check(board[0].getActivePieceStep() == 5,
                            level + ": animated home position");

                    // בדיקה שהתור עבר בלי לפתוח חלון שאלה
                    check(game[0].getCurrentPlayer() == 1,
                            level + ": turn passed");

                    ((Timer) getField(view[0], "gameTimer")).stop();

                } catch (Throwable t) {
                    error[0] = t;
                }
            });

            if (error[0] != null) {
                throw new AssertionError(level, error[0]);
            }

            System.out.println("PASS - " + level
                    + ": home entry and movement");
        }

        System.out.println("PASS - All home path tests");
        System.exit(0);
    }
}