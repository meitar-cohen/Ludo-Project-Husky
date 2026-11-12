package tests;

import model.Player;

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
}
