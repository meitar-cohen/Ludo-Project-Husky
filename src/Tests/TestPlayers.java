package Tests;

import Controller.GameController;
import Model.Player;

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

    static GameController gameControllerForBoard(
            String difficulty, String[] colors) {
        return gameController(colors, colors, difficulty);
    }
}
