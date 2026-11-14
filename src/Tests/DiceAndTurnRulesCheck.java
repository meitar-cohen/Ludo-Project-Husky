package Tests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;
import Enums.SquareType;
import Model.Game;
import View.BoardPanel;
import View.GameScreen;

/**
 * SRS 3.2.3.3.2 - dice of each level, SRS 3.2.3.3.5 - extra turn after a 6 and
 * cancelling the third 6 in a row, SRS 3.2.1.3.3 - game clock.
 */
public class DiceAndTurnRulesCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static Object field(Object source, String name) throws Exception {
        Field f = source.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(source);
    }

    private static GameController controllerFor(Game game) {
        return new GameController(game, new MovementController(game));
    }

    private static Game newGame(String level) {
        return new Game(TestPlayers.of(new String[]{"A", "B"},
                new String[]{"Red", "Blue"}), level);
    }

    // SRS 3.2.3.3.2 - קוביית צעדים 1-6, Boost 1-3 בבינונית, UltraBoost 1-6 בקשה
    private static void testDiceRanges() {
        String[] levels = {"Easy", "Medium", "Hard"};
        int[] faces = {0, 3, 6};
        for (int l = 0; l < levels.length; l++) {
            GameController controller = controllerFor(newGame(levels[l]));
            check(controller.getTurnController().getBoostDieFaces() == faces[l], levels[l] + ": bonus die faces");
            boolean[] stepSeen = new boolean[7];
            boolean[] boostSeen = new boolean[7];
            for (int i = 0; i < 20000; i++) {
                int step = controller.getTurnController().rollStepDie();
                int boost = controller.getTurnController().rollBoostDie();
                check(step >= 1 && step <= 6, levels[l] + ": step die out of range: " + step);
                if (faces[l] == 0) {
                    check(boost == 0, levels[l] + ": no bonus die");
                } else {
                    check(boost >= 1 && boost <= faces[l],
                            levels[l] + ": bonus die out of range: " + boost);
                    boostSeen[boost] = true;
                }
                stepSeen[step] = true;
            }
            for (int v = 1; v <= 6; v++) {
                check(stepSeen[v], levels[l] + ": step die never showed " + v);
            }
            for (int v = 1; v <= faces[l]; v++) {
                check(boostSeen[v], levels[l] + ": bonus die never showed " + v);
            }
        }
        System.out.println("PASS - dice ranges in Easy, Medium and Hard");
    }

    // SRS 3.2.3.3.5 - תור נוסף אחרי 6, וביטול השישייה השלישית ברצף
    private static void testSixesRule() {
        Game game = newGame("Medium");
        GameController controller = controllerFor(game);
        check(!controller.getTurnController().registerRollAndCheckThirdSix(6) && controller.getTurnController().hasExtraTurn(),
                "first 6 gives an extra turn");
        check(!controller.getTurnController().registerRollAndCheckThirdSix(6) && game.getConsecutiveSixes() == 2,
                "second 6 in a row is counted");
        check(controller.getTurnController().registerRollAndCheckThirdSix(6), "third 6 in a row is cancelled");
        check(game.getConsecutiveSixes() == 0, "counter restarts after the cancelled roll");
        check(!controller.getTurnController().registerRollAndCheckThirdSix(6), "the next 6 is a first 6 again");
        check(!controller.getTurnController().registerRollAndCheckThirdSix(4) && !controller.getTurnController().hasExtraTurn()
                && game.getConsecutiveSixes() == 0, "a roll that is not 6 resets the counter");
        game.setStage(Stage.BUSY);
        controller.getTurnController().grantExtraTurn();
        check(game.getStage() == Stage.ROLL, "extra turn lets the same player roll");
        controller.getTurnController().registerRollAndCheckThirdSix(6);
        controller.getTurnController().passTurn();
        check(game.getConsecutiveSixes() == 0 && game.getCurrentPlayer() == 1,
                "passing the turn resets the counter for the next player");
        System.out.println("PASS - extra turn and third 6 in a row");
    }

    // SRS 3.2.1.3.3 - השעון מתחיל בהטלה הראשונה ונעצר בסוף המשחק
    private static void testClock() throws Exception {
        GameController controller = controllerFor(newGame("Easy"));
        check(!controller.isGameClockStarted() && controller.getElapsedSeconds() == 0
                && "00:00:00".equals(controller.getElapsedTime()), "clock waits for the first roll");
        controller.startGameClock();
        check(controller.isGameClockStarted(), "clock starts");
        Thread.sleep(1100);
        controller.stopGameClock();
        long stopped = controller.getElapsedSeconds();
        check(stopped >= 1, "clock counts seconds");
        Thread.sleep(1100);
        check(controller.getElapsedSeconds() == stopped, "clock stops at the end of the game");
        check(controller.getElapsedTime().matches("\\d{2}:\\d{2}:\\d{2}"), "clock format HH:MM:SS");
        System.out.println("PASS - game clock");
    }

    // מסך המשחק: שלוש שישיות ברצף מעבירות את התור, ותור נוסף אחרי הזזת כלי עם 6
    private static void testScreen() throws Exception {
        final GameScreen[] view = new GameScreen[1];
        final Throwable[] error = {null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                view[0] = new GameScreen(TestPlayers.gameController(
                        new String[]{"A", "B"}, new String[]{"Red", "Blue"}, "Easy"), "Easy");
                Game game = (Game) field(field(view[0], "turns"), "game");
                Object sidebar = field(view[0], "sidebar");
                Method finishRoll = GameScreen.class.getDeclaredMethod("finishRoll", int.class, int.class);
                finishRoll.setAccessible(true);
                Method castle = GameScreen.class.getDeclaredMethod("onCastlePieceClicked", int.class);
                castle.setAccessible(true);
                BoardPanel board = (BoardPanel) field(view[0], "board");
                board.setActivePlayerColor("Red");

                finishRoll.invoke(view[0], 6, 0);
                castle.invoke(view[0], 0);
                check(game.getCurrentPlayer() == 0 && game.getStage() == Stage.ROLL,
                        "screen: extra turn after taking a piece out with a 6");
                game.setConsecutiveSixes(2);
                finishRoll.invoke(view[0], 6, 0);
                check(game.getCurrentPlayer() == 1 && game.getStage() == Stage.ROLL,
                        "screen: third 6 passes the turn");
                String status = ((JTextArea) field(sidebar, "statusArea")).getText();
                check(status.contains("Third 6"), "screen: message about the cancelled roll");
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError("third six screen", error[0]);
        }

        // תור נוסף אחרי הזזת כלי על הלוח עם 6
        final Game[] game = new Game[1];
        final Object[] sidebar = new Object[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                ((Timer) field(view[0], "gameTimer")).stop();
                view[0] = new GameScreen(TestPlayers.gameController(
                        new String[]{"A", "B"}, new String[]{"Red", "Blue"}, "Easy"), "Easy");
                game[0] = (Game) field(field(view[0], "turns"), "game");
                sidebar[0] = field(view[0], "sidebar");
                BoardPanel board = (BoardPanel) field(view[0], "board");
                board.setActivePlayerColor("Red");
                check(board.placeActivePieceOnStart(0), "setup piece");
                removeEventSquares(board);
                game[0].selectPiece(0);
                game[0].setPieceOnBoard(true);
                game[0].setPiecesInCastle(3);
                game[0].setPieceStep(0);
                game[0].setLastStepDie(6);
                game[0].setPendingSteps(6);
                game[0].setStage(Stage.MOVE);
                Method click = GameScreen.class.getDeclaredMethod("onBoardPieceClicked", int.class);
                click.setAccessible(true);
                click.invoke(view[0], 0);
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError("extra turn screen", error[0]);
        }
        Thread.sleep(220L * 8 + 600);
        SwingUtilities.invokeAndWait(() -> {
            try {
                JButton roll = (JButton) field(sidebar[0], "rollButton");
                check(game[0].getCurrentPlayer() == 0 && game[0].getStage() == Stage.ROLL
                        && roll.isEnabled() && "Roll Again".equals(roll.getText()),
                        "screen: moving a piece with a 6 gives another roll");
                ((Timer) field(view[0], "gameTimer")).stop();
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        if (error[0] != null) {
            throw new AssertionError("extra turn screen", error[0]);
        }
        System.out.println("PASS - game screen: third 6 and extra turn");
    }

    private static void removeEventSquares(BoardPanel board) throws Exception {
        GameController turns = (GameController) field(board, "turns");
        turns.removeSpecialSquares(
                SquareType.QUESTION, SquareType.SURPRISE);
    }

    public static void main(String[] args) throws Exception {
        testDiceRanges();
        testSixesRule();
        testClock();
        testScreen();
        System.out.println("All dice and turn rule checks passed.");
    }
}
