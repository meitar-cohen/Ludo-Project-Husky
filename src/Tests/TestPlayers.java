package Tests;

import Controller.GameController;
import Model.Player;
import java.awt.Color;
import java.lang.reflect.Method;

final class TestPlayers {
    private TestPlayers() {
    }

    static Player[] of(String[] names, String[] colors) {
        Player[] players = new Player[names.length];
        for (int i = 0; i < names.length; i++) {
            players[i] = new Player(names[i], colors[i]);
        }
        return players;
    }

    static GameController gameController(
            String[] names, String[] colors, String difficulty) {
        return GameController.createNewGame(names, colors, difficulty);
    }

    static String specialSquareType(Object square) throws Exception {
        return invokeSquareGetter(square, "getType").toString();
    }

    static Color specialSquareColor(Object square) throws Exception {
        return (Color) invokeSquareGetter(square, "getColor");
    }

    private static Object invokeSquareGetter(Object square, String getter)
            throws Exception {
        Method method = square.getClass().getMethod(getter);
        return method.invoke(square);
    }
}
