package Tests;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;
import Enums.SurpriseEventType;
import Model.Game;
import Model.Player;
import View.BoardPanel;
import View.GameScreen;

/**
 * Verifies the Track 2 Observer Design Pattern (SRS 3.5):
 * Game (Model Subject) -> GameController (MVC bridge) -> View Observers.
 */
public final class ObserverSyncCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    public static void main(String[] args) throws Exception {
        // 1. Model Subject -> GameController bridge -> Observer contract
        Game game = new Game(new Player[]{
                new Player("Ann", "Green"),
                new Player("Bob", "Red")
        }, "Medium");
        MovementController movement = new MovementController(game);
        GameController controller = GameController.forTesting(game, movement);

        List<PropertyChangeEvent> received = new ArrayList<>();
        PropertyChangeListener observer = received::add;
        controller.addObserver(observer);
        controller.addObserver(observer); // duplicate registration must be ignored
        controller.addObserver(null);

        game.setStage(Stage.TAKE_OUT);
        check(received.size() == 1, "Observer should be notified once per change");
        check(received.get(0).getSource() == controller,
                "Event source must be GameController (no direct Model exposure to View)");

        controller.removeObserver(observer);
        game.setStage(Stage.ROLL);
        check(received.size() == 1, "Removed observer must no longer receive events");
        System.out.println("PASS - observer registration, deduplication, removal, and MVC boundary");

        // 2. Automatic BoardPanel synchronization on take-out, undo, and surprise effects
        final Throwable[] uiError = {null};
        SwingUtilities.invokeAndWait(() -> {
            try {
                Game syncGame = new Game(new Player[]{
                        new Player("Ann", "Green"),
                        new Player("Bob", "Red")
                }, "Medium");
                GameController syncController =
                        GameController.forTesting(syncGame, new MovementController(syncGame));
                BoardPanel board = new BoardPanel(syncController);

                syncGame.setStage(Stage.TAKE_OUT);
                check(syncController.takeOutPiece(0), "Controller takes piece out");
                check(board.hasPieceOnBoard() && board.getActivePieceStep() == 0,
                        "BoardPanel automatically synced take-out from model");

                check(syncController.getCommandInvoker().undoLast(), "Undo take-out");
                check(!board.hasPieceOnBoard() && board.hasPieceInCastle(),
                        "BoardPanel automatically synced undo back to castle");

                syncGame.setStage(Stage.TAKE_OUT);
                check(syncController.takeOutPiece(0), "Take piece out again");
                syncGame.setStage(Stage.BUSY);
                syncController.getSurpriseController().apply(
                        syncController.getSurpriseController().resolveForPiece(
                                SurpriseEventType.GO_TO_FINISH, 0, 64),
                        0, 64, -1, 0);
                check(board.isActivePieceFinished(0) && !board.hasPieceOnBoard(),
                        "BoardPanel automatically synced surprise finish without manual show call");

                board.removeObservers();
            } catch (Throwable t) {
                uiError[0] = t;
            }
        });
        if (uiError[0] != null) {
            throw new AssertionError("BoardPanel observer sync failed", uiError[0]);
        }
        System.out.println("PASS - automatic BoardPanel synchronization on commands, undo, and surprises");

        // 3. Automatic GameSidebarPanel synchronization on turn pass and game completion
        SwingUtilities.invokeAndWait(() -> {
            GameScreen screen = null;
            try {
                Game screenGame = new Game(new Player[]{
                        new Player("Ann", "Green"),
                        new Player("Bob", "Red")
                }, "Easy");
                GameController screenController =
                        GameController.forTesting(screenGame, new MovementController(screenGame));
                screen = new GameScreen(screenController, "Easy");

                Object sidebar = field(screen, "sidebar");
                JLabel turnLabel = (JLabel) field(sidebar, "turnLabel");
                JButton endGameButton = (JButton) field(sidebar, "endGameButton");
                JButton rollButton = (JButton) field(sidebar, "rollButton");

                check("Ann's turn".equals(turnLabel.getText()), "Initial turn label");
                check(!endGameButton.isEnabled(), "End Game disabled initially");

                screenController.getTurnController().passTurn();
                check("Bob's turn".equals(turnLabel.getText()),
                        "Sidebar automatically updated active player on passTurn");

                screenGame.setPlayerFinished(1, true);
                screenController.getTurnController().passTurn();
                check(screenController.isGameOver(), "Game reached DONE");
                check(endGameButton.isEnabled() && !rollButton.isEnabled(),
                        "Sidebar automatically enabled End Game and disabled Roll on DONE");

                // Verify cleanup unregisters View observers
                screen.getClass().getDeclaredMethod("cleanupObservers");
                java.lang.reflect.Method cleanup =
                        GameScreen.class.getDeclaredMethod("cleanupObservers");
                cleanup.setAccessible(true);
                cleanup.invoke(screen);

                int beforeCleanupCount = ((java.beans.PropertyChangeSupport)
                        field(screenController, "stateObservers")).getPropertyChangeListeners().length;
                check(beforeCleanupCount == 0,
                        "All GameScreen observers unregistered on cleanup, remaining: " + beforeCleanupCount);
            } catch (Throwable t) {
                uiError[0] = t;
            } finally {
                if (screen != null) {
                    try {
                        ((Timer) field(screen, "gameTimer")).stop();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        if (uiError[0] != null) {
            throw new AssertionError("Sidebar/GameScreen observer sync failed", uiError[0]);
        }
        System.out.println("PASS - automatic GameSidebarPanel sync and observer cleanup");
        System.out.println("All Observer pattern checks passed.");
        System.exit(0);
    }
}
