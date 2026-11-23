package Tests;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

import Controller.CaptureEvent;
import Controller.CaptureListener;
import Controller.GameController;
import Controller.MovementController;
import Enums.SquareType;
import Model.Game;
import Model.Piece;
import Model.Player;
import View.BoardPanel;

/**
 * The kick animation of a capture: the controller tells its observers about every capture
 * (Observer), the board plays the kick and lets the game go on only afterwards, every world has its
 * own look of the kick (Strategy), and a kick never happens when the rules do not allow a capture.
 */
public final class CaptureKickCheck {
    private CaptureKickCheck() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Setup {
        final Game game = new Game(new Player[]{new Player("Ann", "Green"), new Player("Bob", "Red")}, "Easy");
        final GameController controller = GameController.forTesting(game, new MovementController(game));

        /** Ann's piece 0 and Bob's piece 0 on the same square (Green step s is Red step s + 13 on Easy). */
        Point meet() {
            Piece a = game.getPlayer(0).getPiece(0);
            a.setOnBoard(true);
            int step = 20;
            for (;; step++) {                                  // a square without a special mark
                a.setSteps(step);
                if (!controller.hasSpecialSquare(controller.getPieceBoardPosition(0, 0))) break;
            }
            game.setPiecesInCastle(0, 3);
            Piece v = game.getPlayer(1).getPiece(0);
            v.setOnBoard(true);
            v.setSteps(step + 13);
            game.setPiecesInCastle(1, 3);
            Point square = controller.getPieceBoardPosition(0, 0);
            check(square.equals(controller.getPieceBoardPosition(1, 0)), "the pieces share a square");
            return square;
        }
    }

    public static void main(String[] args) throws Exception {
        // --- Observer: the controller tells its listeners about a capture
        Setup s = new Setup();
        Point square = s.meet();
        List<CaptureEvent> heard = new ArrayList<>();
        CaptureListener listener = heard::add;
        s.controller.addCaptureListener(listener);
        s.controller.addCaptureListener(listener);                  // the same listener only once
        check(s.controller.captureOpponentAt(0, 0) != null, "the capture happens");
        check(heard.size() == 1, "the listener is told once");
        CaptureEvent event = heard.get(0);
        check(event.getAttackerPlayer() == 0 && event.getAttackerPiece() == 0
                && event.getVictimPlayer() == 1 && event.getVictimPiece() == 0
                && event.getSquare().equals(square), "the event says who captured whom, and where");
        event.getSquare().translate(5, 5);
        check(event.getSquare().equals(square), "the event cannot be changed from outside");
        System.out.println("PASS - the controller tells its observers about a capture");

        Setup none = new Setup();
        none.meet();
        List<CaptureEvent> silent = new ArrayList<>();
        none.controller.addCaptureListener(silent::add);
        none.controller.addSpecialSquare(none.controller.getPieceBoardPosition(0, 0), SquareType.STAR, null, 0);
        check(none.controller.captureOpponentAt(0, 0) == null && silent.isEmpty(),
                "a protected square: no capture and nobody is told");
        Setup block = new Setup();
        block.meet();
        block.game.getPlayer(1).getPiece(1).setOnBoard(true);
        block.game.getPlayer(1).getPiece(1).setSteps(block.game.getPlayer(1).getPiece(0).getSteps());
        List<CaptureEvent> blocked = new ArrayList<>();
        block.controller.addCaptureListener(blocked::add);
        check(block.controller.captureOpponentAt(0, 0) == null && blocked.isEmpty(),
                "a Double Block is not captured and nobody is told");
        Setup removed = new Setup();
        removed.meet();
        List<CaptureEvent> late = new ArrayList<>();
        CaptureListener l2 = late::add;
        removed.controller.addCaptureListener(l2);
        removed.controller.removeCaptureListener(l2);
        removed.controller.captureOpponentAt(0, 0);
        check(late.isEmpty(), "a listener that was removed is not told");
        System.out.println("PASS - no news without a capture, and listeners can leave");

        // --- the board plays the kick, and the game goes on only after it
        Setup g = new Setup();
        BoardPanel board = new BoardPanel(g.controller);       // it places the special squares first
        g.meet();
        board.setSize(700, 700);
        CountDownLatch finished = new CountDownLatch(1);
        long[] startedAt = new long[1], endedAt = new long[1];
        String[] message = new String[1];
        SwingUtilities.invokeAndWait(() -> {
            startedAt[0] = System.nanoTime();
            message[0] = board.resolveCapture(0, 0, () -> {
                endedAt[0] = System.nanoTime();
                finished.countDown();
            });
        });
        check(message[0] != null && message[0].contains("kicked"), "the players get a message about the kick");
        check(board.isKickPlaying(), "the kick is playing");
        check(finished.getCount() == 1, "the game does not go on at once");

        // painting during the kick works and changes the picture
        Set<Integer> pictures = new HashSet<>();
        for (int i = 0; i < 6 && finished.getCount() > 0; i++) {
            BufferedImage image = new BufferedImage(700, 700, BufferedImage.TYPE_INT_ARGB);
            SwingUtilities.invokeAndWait(() -> {
                Graphics2D g2 = image.createGraphics();
                board.paint(g2);
                g2.dispose();
            });
            pictures.add(java.util.Arrays.hashCode(image.getRGB(0, 0, 700, 700, null, 0, 700)));
            Thread.sleep(260);
        }
        check(pictures.size() >= 4, "the picture changes while the kick plays (" + pictures.size() + " different)");
        check(finished.await(5, TimeUnit.SECONDS), "the kick ends and the game goes on");
        double seconds = (endedAt[0] - startedAt[0]) / 1e9;
        check(seconds >= BoardPanel.kickDurationSeconds() - 0.1 && seconds < BoardPanel.kickDurationSeconds() + 1.0,
                "the kick lasts about " + BoardPanel.kickDurationSeconds() + " seconds, took " + seconds);
        check(!board.isKickPlaying(), "the kick is over");
        System.out.println("PASS - the board plays the kick and the game goes on after it");

        // --- no kick when the rules do not allow a capture
        Setup safe = new Setup();
        BoardPanel safeBoard = new BoardPanel(safe.controller);
        safe.meet();
        safe.controller.addSpecialSquare(safe.controller.getPieceBoardPosition(0, 0), SquareType.START, null, 0);
        boolean[] called = {false};
        String[] safeMessage = new String[1];
        SwingUtilities.invokeAndWait(() -> safeMessage[0] = safeBoard.resolveCapture(0, 0, () -> called[0] = true));
        Thread.sleep(150);
        check(safeMessage[0] == null && !safeBoard.isKickPlaying() && !called[0],
                "a protected square: no kick, the caller just carries on");
        System.out.println("PASS - no kick without a capture");

        // --- every world has its own look of the kick (Strategy)
        Class<?> themes = Class.forName("View.Themes");
        Method all = themes.getDeclaredMethod("all");
        all.setAccessible(true);
        List<?> list = (List<?>) all.invoke(null);
        Class<?> themeClass = Class.forName("View.WorldTheme");
        Method kickStyle = themeClass.getDeclaredMethod("kickStyle");
        kickStyle.setAccessible(true);
        Set<String> words = new HashSet<>();
        for (Object theme : list) {
            Object style = kickStyle.invoke(theme);
            check(style != null, "every world has a kick style");
            Field word = style.getClass().getDeclaredField("word");
            word.setAccessible(true);
            words.add((String) word.get(style));
        }
        check(words.size() == list.size(), "the worlds have different words: " + words);
        System.out.println("PASS - every world has its own kick look: " + words);
        System.out.println("All capture kick checks passed.");
        System.exit(0);
    }
}
