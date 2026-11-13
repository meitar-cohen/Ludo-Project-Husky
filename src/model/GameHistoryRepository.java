package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SRS 3.2.8.3.2 - 3.2.8.3.4 - reads and writes the game history file.
 *
 * The file is the CSV template supplied by the course
 * ("History-DataBase Tempalte - Game.csv"). The column names and their order
 * are never changed: new games are only appended as new rows.
 */
public class GameHistoryRepository {

    /** The name of the course template file. */
    public static final String FILE_NAME = "History-DataBase Tempalte - Game.csv";

    /** The header row of the course template, unchanged. */
    public static final String HEADER = "gameID,difficulty,timestamp(DD/MM/YYYY; HH:MM:SS),"
            + "1st_place_name,2nd_place_name,3rd_place_name,4rd_place_name,game_duration(HH:MM:SS)";

    private static final int COLUMNS = 8;
    private static final String LINE_END = "\r\n";   // same line ending as the course template

    private final File file;

    public GameHistoryRepository(File file) {
        if (file == null) {
            throw new IllegalArgumentException("History file cannot be null");
        }
        this.file = file;
    }

    /**
     * The history file in the "data" folder of the project.
     * When the game runs from a JAR file, the folder next to the JAR is used;
     * otherwise (for example in Eclipse) the working folder of the project is used.
     */
    public static GameHistoryRepository atDefaultLocation() {
        File base = new File(System.getProperty("user.dir"));
        try {
            File code = new File(GameHistoryRepository.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (code.isFile()) {
                base = code.getParentFile();
            }
        } catch (Exception ignored) {
            // keep the working folder
        }
        return new GameHistoryRepository(new File(new File(base, "data"), FILE_NAME));
    }

    public File getFile() {
        return file;
    }

    // ------------------------------------------------------------------
    // Reading - SRS 3.2.8.3.4
    // ------------------------------------------------------------------

    /**
     * Reads all games from the file. A missing file means no games yet.
     * Rows that cannot be read are skipped and counted.
     */
    public synchronized ReadResult readGames() throws IOException {
        List<GameRecord> games = new ArrayList<>();
        int skipped = 0;
        if (!file.exists()) {
            return new ReadResult(games, skipped);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    if (line.trim().toLowerCase().startsWith("gameid")) {
                        continue;   // header row
                    }
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                GameRecord record = parseRow(line);
                if (record == null) {
                    skipped++;
                } else {
                    games.add(record);
                }
            }
        }
        return new ReadResult(games, skipped);
    }

    private static GameRecord parseRow(String line) {
        List<String> fields = splitCsv(line);
        if (fields.size() != COLUMNS) {
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(fields.get(0).trim());
        } catch (NumberFormatException e) {
            return null;
        }
        String difficulty = fields.get(1).trim();
        if (difficulty.isEmpty() || fields.get(3).trim().isEmpty()) {
            return null;   // every saved game has a difficulty and a winner
        }
        String[] places = {fields.get(3), fields.get(4), fields.get(5), fields.get(6)};
        return new GameRecord(id, difficulty, fields.get(2), places, fields.get(7));
    }

    // ------------------------------------------------------------------
    // Writing - SRS 3.2.8.3.2 - 3.2.8.3.3
    // ------------------------------------------------------------------

    /**
     * Appends one finished game as a new row. Creates the file with the template
     * header when it does not exist. The game ID is the next running number.
     */
    public synchronized GameRecord appendGame(String difficulty, String timestamp,
                                              String[] placeNames, String duration) throws IOException {
        ensureFileExists();
        int nextId = 1;
        for (GameRecord game : readGames().getGames()) {
            nextId = Math.max(nextId, game.getGameId() + 1);
        }
        GameRecord record = new GameRecord(nextId, difficulty, timestamp, placeNames, duration);

        StringBuilder row = new StringBuilder();
        if (!endsWithLineBreak()) {
            row.append(LINE_END);   // the course template ends without a line break
        }
        row.append(record.getGameId()).append(',')
           .append(escape(record.getDifficulty())).append(',')
           .append(escape(record.getTimestamp()));
        for (String name : record.getPlaceNames()) {
            row.append(',').append(escape(name));
        }
        row.append(',').append(escape(record.getDuration())).append(LINE_END);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
            writer.write(row.toString());
        }
        return record;
    }

    private void ensureFileExists() throws IOException {
        if (file.exists()) {
            return;
        }
        File folder = file.getAbsoluteFile().getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            throw new IOException("Cannot create folder " + folder);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(HEADER + LINE_END);
        }
    }

    private boolean endsWithLineBreak() throws IOException {
        if (file.length() == 0) {
            return true;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(file.length() - 1);
            return raf.read() == '\n';
        }
    }

    // ------------------------------------------------------------------
    // CSV helpers
    // ------------------------------------------------------------------

    /** Quotes a value only when it contains a comma, a quote or a line break. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /** The games read from the file and the number of rows that were skipped. */
    public static final class ReadResult {
        private final List<GameRecord> games;
        private final int skippedRows;

        ReadResult(List<GameRecord> games, int skippedRows) {
            this.games = Collections.unmodifiableList(new ArrayList<>(games));
            this.skippedRows = skippedRows;
        }

        public List<GameRecord> getGames() {
            return games;
        }

        public int getSkippedRows() {
            return skippedRows;
        }
    }
}
