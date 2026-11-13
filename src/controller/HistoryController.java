package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import Model.Game;
import Model.GameRecord;
import Model.Player;

/**
 * SRS 3.2.8 - controller of the game history.
 * Saves a finished game to the history file and loads the saved games for the history screen.
 */
public class HistoryController {

    public static final String HISTORY_FILE_NAME =
            "History-DataBase Tempalte - Game.csv";
    public static final String HISTORY_HEADER =
            "gameID,difficulty,timestamp(DD/MM/YYYY; HH:MM:SS),"
            + "1st_place_name,2nd_place_name,3rd_place_name,4rd_place_name,"
            + "game_duration(HH:MM:SS)";

    private final File historyFile;
    private int skippedRows;
    private String lastError;

    public HistoryController() {
        this(defaultHistoryFile());
    }

    public HistoryController(File historyFile) {
        if (historyFile == null) {
            throw new IllegalArgumentException("History file cannot be null");
        }
        this.historyFile = historyFile;
    }

    /**
     * SRS 3.2.8.3.2 - 3.2.8.3.3 - adds the finished game to the history file.
     * A game is saved only once, and only if it was actually played (at least one roll).
     *
     * @return null when the game was saved or did not need saving, otherwise an error message
     */
    public String saveFinishedGame(Game game, TurnController turns) {
        if (game == null || turns == null || game.isSavedToHistory()
                || !turns.isGameClockStarted()) {
            return null;
        }
        Player[] ranking = turns.getRanking();
        String[] names = new String[GameRecord.MAX_PLAYERS];
        for (int i = 0; i < names.length; i++) {
            names[i] = i < ranking.length ? ranking[i].getName() : "";
        }
        try {
            List<GameRecord> history = readGames();
            int nextId = getNextGameId(history);
            GameRecord record = new GameRecord(
                    nextId,
                    toFileDifficulty(game.getDifficulty()),
                    formatTimestamp(new Date()),
                    names,
                    formatDuration(turns.getElapsedSeconds()));
            appendRecord(record);
            game.markSavedToHistory();
            return null;
        } catch (IOException | RuntimeException e) {
            return             "The game could not be saved to the history file ("
            + historyFile.getName() + "): " + e.getMessage();
        }
    }

    /** SRS 3.2.8.3.4 - the saved games, newest first, skipping rows that cannot be read. */
    public List<GameRecord> loadHistory() {
        lastError = null;
        try {
            List<GameRecord> games = new ArrayList<>(readGames());
            Collections.reverse(games);
            return games;
        } catch (IOException | RuntimeException e) {
            skippedRows = 0;
            lastError = "The history file could not be read: " + e.getMessage();
            return new ArrayList<>();
        }
    }

    public String getHistoryFileName() {
        return historyFile.getName();
    }

    public int getSkippedRows() {
        return skippedRows;
    }

    public String getLastError() {
        return lastError;
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

    private static int getNextGameId(List<GameRecord> games) {
        int nextId = 1;
        for (GameRecord game : games) {
            nextId = Math.max(nextId, game.getGameId() + 1);
        }
        return nextId;
    }

    public List<GameRecord> readGames() throws IOException {
        List<GameRecord> games = new ArrayList<>();
        skippedRows = 0;
        if (!historyFile.exists()) {
            return games;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(historyFile), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    if (line.trim().toLowerCase().startsWith("gameid")) {
                        continue;
                    }
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                GameRecord record = parseRow(line);
                if (record == null) {
                    skippedRows++;
                } else {
                    games.add(record);
                }
            }
        }
        return games;
    }

    public void appendRecord(GameRecord record) throws IOException {
        ensureFileExists(historyFile);
        StringBuilder row = new StringBuilder();
        if (!endsWithLineBreak(historyFile)) {
            row.append("\r\n");
        }
        row.append(record.getGameId()).append(',')
                .append(escape(record.getDifficulty())).append(',')
                .append(escape(record.getTimestamp()));
        for (String name : record.getPlaceNames()) {
            row.append(',').append(escape(name));
        }
        row.append(',').append(escape(record.getDuration())).append("\r\n");

        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(historyFile, true), StandardCharsets.UTF_8)) {
            writer.write(row.toString());
        }
    }

    private static GameRecord parseRow(String line) {
        List<String> fields = splitCsv(line);
        if (fields.size() != 8) {
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(fields.get(0).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
        String difficulty = fields.get(1).trim();
        if (difficulty.isEmpty() || fields.get(3).trim().isEmpty()) {
            return null;
        }
        String[] places = {
            fields.get(3), fields.get(4), fields.get(5), fields.get(6)
        };
        return new GameRecord(id, difficulty, fields.get(2),
                places, fields.get(7));
    }

    private static void ensureFileExists(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        File folder = file.getAbsoluteFile().getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            throw new IOException("Cannot create folder " + folder);
        }
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(HISTORY_HEADER + "\r\n");
        }
    }

    private static boolean endsWithLineBreak(File file) throws IOException {
        if (file.length() == 0) {
            return true;
        }
        try (RandomAccessFile randomAccessFile =
                     new RandomAccessFile(file, "r")) {
            randomAccessFile.seek(file.length() - 1);
            return randomAccessFile.read() == '\n';
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (quoted) {
                if (character == '"' && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (character == '"') {
                    quoted = false;
                } else {
                    current.append(character);
                }
            } else if (character == '"') {
                quoted = true;
            } else if (character == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private static File defaultHistoryFile() {
        File base = new File(System.getProperty("user.dir"));
        try {
            File code = new File(HistoryController.class
                    .getProtectionDomain().getCodeSource()
                    .getLocation().toURI());
            if (code.isFile()) {
                base = code.getParentFile();
            }
        } catch (Exception exception) {
            // שימוש בתיקיית העבודה כברירת מחדל
        }
        return new File(new File(base, "data"),
                HISTORY_FILE_NAME);
    }

}
