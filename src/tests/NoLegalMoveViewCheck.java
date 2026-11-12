package tests;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import controller.GameController;
import enums.Stage;
import model.Game;
import view.BoardPanel;
import view.GameView;

public class NoLegalMoveViewCheck {

    private static Object field(Object source, String name)
            throws Exception {

        Field f = source.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(source);
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private static void test(String level, int situation)
            throws Exception {

        final Throwable[] problem = {null};

        SwingUtilities.invokeAndWait(() -> {

            GameView view = null;

            try {
                view = new GameView(TestPlayers.of(new String[]{"A", "B", "C"}, new String[]{"Red", "Blue", "Green"}), level);

                GameController controller =
                        (GameController) field(view, "controller");

                Game game =
                        (Game) field(controller, "game");

                BoardPanel board =
                        (BoardPanel) field(view, "board");

                JButton rollButton =
                        (JButton) field(view, "rollButton");

                JTextArea status =
                        (JTextArea) field(view, "statusArea");

                int die = 4;
                int boost = "Easy".equals(level) ? 0 : 2;

                if (situation == 1) {

                    // תוצאה חיובית אבל הכלי אינו יכול להתקדם
                    board.setActivePlayerColor("Red");
                    check(board.placeActivePieceOnStart(0),
                            "Board setup");

                    game.selectPiece(0);
                    game.setPieceOnBoard(true);
                    game.setPieceInHomePath(true);
                    game.setPieceStep(5);

                    board.showActivePieceInHomePath(0, 5);

                } else if (situation == 2) {

                    // המשתתף הבא סיים ולכן צריך לדלג עליו
                    game.setPlayerFinished(1, true);

                } else if (situation == 3) {

                    // תוצאת Boost אפס עם מהלך חוקי במסלול הבית
                    board.setActivePlayerColor("Red");
                    check(board.placeActivePieceOnStart(0),
                            "Board setup");

                    game.selectPiece(0);
                    game.setPieceOnBoard(true);
                    game.setPieceInHomePath(true);
                    game.setPieceStep(1);

                    board.showActivePieceInHomePath(0, 1);
                    boost = 1;

                } else if (situation == 4) {

                    // נשאר משתתף פעיל אחד בלבד
                    game.setPlayerFinished(1, true);
                    game.setPlayerFinished(2, true);

                } else if (situation == 5) {

                    // הטלת 6 נותנת תור נוסף גם ללא מהלך חוקי
                    game.setPiecesInCastle(0);
                    board.setActivePlayerColor("Red");

                    for (int piece = 0; piece < 4; piece++) {

                        check(board.placeActivePieceOnStart(piece),
                                "All pieces setup");

                        game.selectPiece(piece);
                        game.setPieceOnBoard(true);
                        game.setPieceInHomePath(true);
                        game.setPieceStep(5);

                        board.showActivePieceInHomePath(piece, 5);
                    }

                    die = 6;
                    boost = "Easy".equals(level) ? 0 : 1;

                } else if (situation == 6) {

                    // שישייה שלישית מבוטלת והתור עובר
                    game.setConsecutiveSixes(2);
                    die = 6;
                    boost = "Easy".equals(level) ? 0 : 1;
                }

                // הפעלת סיום ההטלה מתוך מסך המשחק
                Method finish = GameView.class.getDeclaredMethod(
                        "finishRoll", int.class, int.class);

                finish.setAccessible(true);
                finish.invoke(view, die, boost);

                if (situation == 3) {

                    // מהלך חוקי במסלול הבית שומר על התור
                    check(game.getCurrentPlayer() == 0,
                            level + " keeps legal move");

                    check(game.getStage() == Stage.MOVE,
                            level + " move stage");

                    check(status.getText().contains("home-path")
                                    || status.getText().contains(
                                            "Select a piece."),
                            level + " home status");

                } else if (situation == 5) {

                    // קבלת תור נוסף אחרי שישייה
                    check(game.getCurrentPlayer() == 0,
                            level + " six extra turn");

                    check(game.getStage() == Stage.ROLL,
                            level + " extra roll stage");

                    check("Roll Again".equals(rollButton.getText()),
                            level + " extra roll button");

                } else if (situation == 6) {

                    // ביטול השישייה השלישית
                    check(game.getCurrentPlayer() == 1,
                            level + " third six passes turn");

                    check(game.getStage() == Stage.ROLL,
                            level + " third six roll stage");

                    check(status.getText().contains("Third 6 in a row"),
                            level + " third six status");

                } else if (situation == 4) {

                    // אין תור חדש אחרי שנותר משתתף אחד
                    check(game.getStage() == Stage.DONE,
                            level + " done stage");

                    check(!rollButton.isEnabled(),
                            level + " disabled roll");

                } else {

                    // בכל יתר המקרים התור עובר אוטומטית
                    int expectedPlayer =
                            situation == 2 ? 2 : 1;

                    check(game.getCurrentPlayer() == expectedPlayer,
                            level + " automatic next player");

                    check(game.getStage() == Stage.ROLL,
                            level + " roll stage");

                    check(rollButton.isEnabled(),
                            level + " roll enabled");

                    check("Roll Dice".equals(rollButton.getText()),
                            level + " roll text");

                    check(status.getText().contains("has no legal move"),
                            level + " message");
                }

            } catch (Throwable t) {

                problem[0] = t;

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
                    level + " case " + situation,
                    problem[0]);
        }
    }

    public static void main(String[] args) throws Exception {

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            // שבעה תרחישי קבלה בכל רמת קושי
            for (int situation = 0; situation <= 6; situation++) {
                test(level, situation);
            }

            System.out.println("PASS - " + level
                    + " no-move screen and turn skipping");
        }

        System.out.println(
                "PASS - SRS 3.2.3.3.6 GameView tests");

        System.exit(0);
    }
}
