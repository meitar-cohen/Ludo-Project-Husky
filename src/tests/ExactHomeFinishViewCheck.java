package tests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Model.Game;
import enums.Stage;
import View.GameView;
import View.BoardPanel;

public class ExactHomeFinishViewCheck {

    private static Object field(Object obj, String name)
            throws Exception {

        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void testLevel(String level) throws Exception {

        final Throwable[] problem = {null};

        SwingUtilities.invokeAndWait(() -> {

            GameView view = null;

            try {

                // יצירת מסך המשחק
                view = new GameView(TestPlayers.of(new String[]{"A", "B"}, new String[]{"Red", "Blue"}), level);

                Game game = (Game) field(view, "game");

                BoardPanel board =
                        (BoardPanel) field(view, "board");

                board.setActivePlayerColor("Red");

                // הוצאת כלי ללוח לצורך הכנת הבדיקה
                check(board.placeActivePieceOnStart(0),
                        level + ": start");

                // הכנת הכלי במשבצת 4 במסלול הבית
                game.setPieceOnBoard(true);
                game.setPieceInHomePath(true);
                game.setPieceStep(4);
                game.setLastStepDie(3);
                game.setStage(Stage.MOVE);

                check(board.showActivePieceInHomePath(0, 4),
                        level + ": home setup");

                // הפעלת בחירת הכלי דרך מסך המשחק
                Method click = GameView.class.getDeclaredMethod(
                        "onBoardPieceClicked", int.class
                );

                click.setAccessible(true);

                // תוצאה 3 גדולה מדי - הכלי לא מסיים
                click.invoke(view, 0);

                check(game.getPieceStep(0, 0) == 4
                        && !game.isPieceFinished(0, 0),
                        level + ": overshoot rejected");

                check(game.getCurrentPlayer() == 0,
                        level + ": player can choose another piece");

                // תוצאה 2 מדויקת - הכלי מסיים
                game.setLastStepDie(2);
                click.invoke(view, 0);

                check(game.isPieceFinished(0, 0)
                        && game.getPieceStep(0, 0) == 6,
                        level + ": exact finish");

                // בדיקת תצוגת הסיום
                board.setActivePlayerColor("Red");

                check(board.isActivePieceFinished(0)
                        && board.getActiveFinishedPieceCount() == 1,
                        level + ": finish displayed");

                check(!board.hasPieceOnBoard(),
                        level + ": piece removed from movable path");

                // לאחר הסיום התור עובר לשחקן הבא
                check(game.getCurrentPlayer() == 1,
                        level + ": turn passed");

                // בדיקת ציור הלוח ללא שגיאות
                board.setSize(600, 600);

                BufferedImage image = new BufferedImage(
                        600, 600, BufferedImage.TYPE_INT_RGB
                );

                java.awt.Graphics2D graphics = image.createGraphics();
                board.paint(graphics);
                graphics.dispose();

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
            throw new AssertionError(level, problem[0]);
        }

        System.out.println(
                "PASS - " + level + " exact finish in GameView");
    }

    public static void main(String[] args) throws Exception {

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            testLevel(level);
        }

        System.out.println(
                "PASS - SRS 3.2.4.3.4 screen acceptance");

        System.exit(0);
    }
}
