package Tests;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;
import Enums.SquareType;
import Model.Game;
import View.BoardPanel;
import View.GameScreen;

public class TurnFlowAcceptanceCheck {

    private static Object field(Object source, String name)
            throws Exception {

        Field f = source.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(source);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void removeEventSquares(BoardPanel board) throws Exception {
        GameController turns = (GameController) field(board, "turns");
        turns.removeSpecialSquares(
                SquareType.QUESTION, SquareType.SURPRISE);
    }

    // בדיקת עדכון המודל לאחר אירוע
    private static void testEventModel() {

        Game game = new Game(TestPlayers.of(new String[]{"A", "B"}, new String[]{"Green", "Red"}), "Medium");

        MovementController controllerPieces = new MovementController(game);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setSteps(5);
        game.setStage(Stage.BUSY);

        check(controllerPieces.applyEventMovement(0, 2, 64),
                "Event updates model");

        check(game.getSelectedPiece().getSteps() == 2,
                "Event position saved");

        check(game.getStage() == Stage.BUSY,
                "Event does not end turn early");

        check(!controllerPieces.applyEventMovement(0, -1, 64),
                "Negative target rejected");

        check(!controllerPieces.applyEventMovement(0, 64, 64),
                "Out-of-range target rejected");

        game.setStage(Stage.ROLL);

        check(!controllerPieces.applyEventMovement(0, 1, 64),
                "Event rejected outside movement");

        check(game.getSelectedPiece().getSteps() == 2,
                "Invalid move does not change position");

        System.out.println("PASS - event model updates");
    }

    // בדיקת תור רגיל דרך מסך המשחק
    private static void testTurn(String level) throws Exception {

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

                check(board[0].placeActivePieceOnStart(0),
                        level + ": setup piece");

                game[0].getSelectedPiece().setOnBoard(true);
                game[0].getSelectedPiece().setSteps(0);
                game[0].setStage(Stage.MOVE);
                game[0].setLastStepDie(1);
                game[0].setPendingSteps(1);

                // משבצת שאלה/הפתעה אקראית בצעד 1 פותחת חלון ומעכבת את התור,
                // לכן הבדיקה של תור רגיל מסירה אותן מהלוח
                removeEventSquares(board[0]);

                // הדמיית בחירת כלי לאחר הטלת קובייה
                Method click = GameScreen.class.getDeclaredMethod(
                        "onBoardPieceClicked", int.class);

                click.setAccessible(true);
                click.invoke(view[0], 0);

                // התור עדיין לא אמור לעבור בזמן האנימציה
                check(game[0].getStage() == Stage.BUSY,
                        level + ": waiting for movement");

            } catch (Throwable t) {
                error[0] = t;
            }
        });

        if (error[0] != null) {
            throw new AssertionError(level, error[0]);
        }

        // המתנה להשלמת האנימציה בלי לחסום את מסך המשחק
        Thread.sleep(1200);

        SwingUtilities.invokeAndWait(() -> {
            try {

                check(game[0].getCurrentPlayer() == 1,
                        level + ": turn passed");

                check(game[0].getStage() == Stage.ROLL,
                        level + ": next player can roll");

                check(game[0].getPlayer(0).getPiece(0).getSteps() == 1,
                        level + ": model position");

                board[0].setActivePlayerColor("Green");

                check(board[0].getActivePieceStep() == 1,
                        level + ": board position");

                ((Timer) field(view[0], "gameTimer")).stop();

            } catch (Throwable t) {
                error[0] = t;
            }
        });

        if (error[0] != null) {
            throw new AssertionError(level, error[0]);
        }

        System.out.println(
                "PASS - " + level + " movement and turn passing");
    }

    public static void main(String[] args) throws Exception {

        testEventModel();

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            testTurn(level);
        }

        System.out.println("PASS - basic turn flow");
        System.exit(0);
    }
}