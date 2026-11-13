package controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import model.Game;
import model.GameHistoryRepository;
import model.GameRecord;
import model.Player;

/**
 * SRS 3.2.8 - controller of the game history.
 * Saves a finished game to the history file and loads the saved games for the history screen.
 */
public class HistoryController {

    private final GameHistoryRepository repository;

    public HistoryController() {
        this(GameHistoryRepository.atDefaultLocation());
    }

    public HistoryController(GameHistoryRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Repository cannot be null");
        }
        this.repository = repository;
    }

    /**
     * SRS 3.2.8.3.2 - 3.2.8.3.3 - adds the finished game to the history file.
     * A game is saved only once, and only if it was actually played (at least one roll).
     *
     * @return null when the game was saved or did not need saving, otherwise an error message
     */
    public String saveFinishedGame(Game game) {
        if (game == null || game.isSavedToHistory() || !game.isClockStarted()) {
            return null;
        }
        Player[] ranking = game.getRanking();
        String[] names = new String[GameRecord.MAX_PLAYERS];
        for (int i = 0; i < names.length; i++) {
            names[i] = i < ranking.length ? ranking[i].getName() : "";
        }
        try {
            repository.appendGame(
                    toFileDifficulty(game.getDifficulty()),
                    formatTimestamp(new Date()),
                    names,
                    formatDuration(game.getElapsedSeconds()));
            game.markSavedToHistory();
            return null;
        } catch (IOException | RuntimeException e) {
            return "The game could not be saved to the history file ("
                    + repository.getFile().getName() + "): " + e.getMessage();
        }
    }

    /** SRS 3.2.8.3.4 - the saved games, newest first, skipping rows that cannot be read. */
    public HistoryResult loadHistory() {
        try {
            GameHistoryRepository.ReadResult result = repository.readGames();
            List<GameRecord> games = new ArrayList<>(result.getGames());
            Collections.reverse(games);
            return new HistoryResult(games, result.getSkippedRows(), null);
        } catch (IOException | RuntimeException e) {
            return new HistoryResult(new ArrayList<>(), 0,
                    "The history file could not be read: " + e.getMessage());
        }
    }

    public String getHistoryFileName() {
        return repository.getFile().getName();
    }

    /** "Easy" -> "easy" - the file uses lower case, as in the course template. */
    static String toFileDifficulty(String difficulty) {
        return difficulty == null ? "" : difficulty.trim().toLowerCase();
    }

    /** "hard" -> "Hard" - for the screens. */
    public static String toDisplayDifficulty(String difficulty) {
        if (difficulty == null || difficulty.trim().isEmpty()) {
            return "";
        }
        String d = difficulty.trim().toLowerCase();
        return Character.toUpperCase(d.charAt(0)) + d.substring(1);
    }

    /** Timestamp format of the template: DD/MM/YYYY;HH:MM:SS. */
    static String formatTimestamp(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy;HH:mm:ss").format(date);
    }

    /** Duration format of the template: HH:MM:SS. */
    static String formatDuration(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    /** The games for the history screen, with information for the messages. */
    public static final class HistoryResult {
        private final List<GameRecord> games;
        private final int skippedRows;
        private final String error;

        HistoryResult(List<GameRecord> games, int skippedRows, String error) {
            this.games = Collections.unmodifiableList(games);
            this.skippedRows = skippedRows;
            this.error = error;
        }

        public List<GameRecord> getGames() {
            return games;
        }

        public int getSkippedRows() {
            return skippedRows;
        }

        /** Null when the file was read (or does not exist yet). */
        public String getError() {
            return error;
        }

        public boolean isEmpty() {
            return games.isEmpty();
        }
    }
}
