package Model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** קריאה ושמירה של שאלות בקובץ CSV, ללא תלות ב-Swing. */
public final class QuestionRepository {

    private static final String[] COLUMNS = {
            "QuestionID", "Difficulty", "Architectural pattern", "Question",
            "Answer 1", "Answer 2", "Answer 3", "Answer 4",
            "Correct answer (1-4)", "Advantages / disadvantages covered", "Source"
    };

    private QuestionRepository() { }

    /** נתיב הקובץ הפעיל - תמיכה גם ב-Eclipse וגם בהרצה מקובץ JAR. */
    public static Path getProjectCsvPath() {
        Path csvPath = Path.of(System.getProperty("user.dir"),
                "data", "questions_Husky.csv");
        try {
            Path codeLocation = Path.of(QuestionRepository.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            Path baseDirectory = codeLocation.getParent();
            if (baseDirectory != null) {
                Path candidate = baseDirectory.resolve("data")
                        .resolve("questions_Husky.csv");
                if (Files.isRegularFile(candidate)) {
                    csvPath = candidate;
                }
            }
        } catch (Exception ignored) {
            // אם לא ניתן לזהות את מיקום ההרצה, משתמשים בתיקיית העבודה.
        }
        return csvPath;
    }

    public static List<Question> loadProjectQuestions() throws IOException {
        return loadFromCsv(getProjectCsvPath());
    }

    /** השיטה המקורית נשארת זמינה לכל מחלקות המשחק והבדיקות. */
    public static List<Question> loadFromCsv(Path file) throws IOException {
        List<List<String>> rows = readRows(file);
        List<String> header = rows.get(0);
        requireColumns(header, "Difficulty", "Question", "Answer 1",
                "Answer 2", "Answer 3", "Answer 4",
                "Correct answer (1-4)");
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            checkRowSize(row, header, i + 1);
            questions.add(toQuestion(row, header, i + 1));
        }
        return questions;
    }

    /** קריאת כל 11 העמודות לצורך עריכה ללא איבוד שדות נוספים. */
    public static List<QuestionRecord> loadRecordsFromCsv(Path file)
            throws IOException {
        List<List<String>> rows = readRows(file);
        List<String> header = rows.get(0);
        requireColumns(header, COLUMNS);
        List<QuestionRecord> records = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            checkRowSize(row, header, i + 1);
            try {
                int id = Integer.parseInt(get(row, header, "QuestionID"));
                if (!ids.add(id)) {
                    throw new IllegalArgumentException("Duplicate QuestionID: " + id);
                }
                Question question = toQuestion(row, header, i + 1);
                records.add(new QuestionRecord(id, question,
                        get(row, header, "Architectural pattern"),
                        get(row, header, "Advantages / disadvantages covered"),
                        get(row, header, "Source")));
            } catch (IllegalArgumentException ex) {
                throw new IOException("Invalid CSV record " + (i + 1), ex);
            }
        }
        return records;
    }

    /**
     * שמירה בטוחה: כתיבה לקובץ זמני והחלפת הקובץ המקורי רק בסיום.
     * כך כישלון בכתיבה לא משאיר CSV שנכתב חלקית.
     */
    public static void saveRecordsToCsv(Path file, List<QuestionRecord> records)
            throws IOException {
        if (file == null || records == null || records.isEmpty()) {
            throw new IllegalArgumentException("File and non-empty question list are required");
        }
        Set<Integer> ids = new HashSet<>();
        Set<String> texts = new HashSet<>();
        for (QuestionRecord record : records) {
            if (record == null || !ids.add(record.getId())) {
                throw new IllegalArgumentException("Invalid or duplicate question ID");
            }
            String normalized = record.getQuestion().getText()
                    .trim().toLowerCase(Locale.ROOT);
            if (!texts.add(normalized)) {
                throw new IllegalArgumentException("Duplicate question text");
            }
        }

        Path absolute = file.toAbsolutePath();
        Path directory = absolute.getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            throw new IOException("Question data directory does not exist");
        }
        Path temporary = Files.createTempFile(directory, "questions_", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    temporary, StandardCharsets.UTF_8)) {
                // BOM תומך גם בפתיחת UTF-8 בעברית באמצעות Excel.
                writer.write('\uFEFF');
                writeCsvRow(writer, List.of(COLUMNS));
                for (QuestionRecord record : records) {
                    Question q = record.getQuestion();
                    String[] answers = q.getOptions();
                    writeCsvRow(writer, List.of(
                            Integer.toString(record.getId()),
                            q.getDifficulty(), record.getArchitecturalPattern(),
                            q.getText(), answers[0], answers[1],
                            answers[2], answers[3],
                            Integer.toString(q.getCorrectIndex() + 1),
                            record.getCoverage(), record.getSource()));
                }
            }
            // בדיקת קריאה מחדש לפני החלפת קובץ השאלות.
            if (loadRecordsFromCsv(temporary).size() != records.size()) {
                throw new IOException("Question CSV verification failed");
            }
            try {
                Files.move(temporary, absolute,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, absolute,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeCsvRow(BufferedWriter writer, List<String> fields)
            throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i != 0) writer.write(',');
            String value = fields.get(i) == null ? "" : fields.get(i);
            if (value.contains(",") || value.contains("\"")
                    || value.contains("\n") || value.contains("\r")) {
                writer.write('"');
                writer.write(value.replace("\"", "\"\""));
                writer.write('"');
            } else {
                writer.write(value);
            }
        }
        writer.newLine();
    }

    private static List<List<String>> readRows(Path file) throws IOException {
        List<List<String>> rows;
        try (PushbackReader reader = new PushbackReader(
                Files.newBufferedReader(file, StandardCharsets.UTF_8), 1)) {
            rows = parseCsv(reader);
        }
        if (rows.isEmpty()) {
            throw new IOException("Question CSV is empty: " + file);
        }
        List<String> header = rows.get(0);
        if (!header.isEmpty()) {
            header.set(0, header.get(0).replace("\uFEFF", ""));
        }
        return rows;
    }

    private static void requireColumns(List<String> header, String... required)
            throws IOException {
        for (String column : required) {
            if (!header.contains(column)) {
                throw new IOException("Missing required CSV column: " + column);
            }
        }
    }

    private static void checkRowSize(List<String> row, List<String> header,
            int recordNumber) throws IOException {
        if (row.size() != header.size()) {
            throw new IOException("Invalid number of columns at CSV record "
                    + recordNumber);
        }
    }

    private static Question toQuestion(List<String> row, List<String> header,
            int recordNumber) throws IOException {
        try {
            String[] options = {
                    get(row, header, "Answer 1"), get(row, header, "Answer 2"),
                    get(row, header, "Answer 3"), get(row, header, "Answer 4")
            };
            int answer = Integer.parseInt(
                    get(row, header, "Correct answer (1-4)"));
            if (answer < 1 || answer > 4) {
                throw new IllegalArgumentException("Answer must be 1-4");
            }
            return new Question(get(row, header, "Question"), options,
                    answer - 1, get(row, header, "Difficulty"));
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid question in CSV record "
                    + recordNumber, ex);
        }
    }

    private static String get(List<String> row, List<String> header,
            String column) {
        return row.get(header.indexOf(column)).trim();
    }

    /** מפענח CSV כולל פסיקים, מירכאות וירידות שורה בתוך שדה. */
    private static List<List<String>> parseCsv(PushbackReader reader)
            throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean insideQuotes = false;
        boolean hasData = false;
        int c;
        while ((c = reader.read()) != -1) {
            hasData = true;
            if (c == '"') {
                if (insideQuotes) {
                    int next = reader.read();
                    if (next == '"') {
                        cell.append('"');
                    } else {
                        insideQuotes = false;
                        if (next != -1) reader.unread(next);
                    }
                } else if (cell.length() == 0) {
                    insideQuotes = true;
                } else {
                    throw new IOException("Unexpected quote in CSV field");
                }
            } else if (c == ',' && !insideQuotes) {
                currentRow.add(cell.toString());
                cell.setLength(0);
            } else if ((c == '\n' || c == '\r') && !insideQuotes) {
                if (c == '\r') {
                    int next = reader.read();
                    if (next != '\n' && next != -1) reader.unread(next);
                }
                currentRow.add(cell.toString());
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                cell.setLength(0);
                hasData = false;
            } else {
                cell.append((char) c);
            }
        }
        if (insideQuotes) throw new IOException("Unclosed quoted field in CSV");
        if (hasData) {
            currentRow.add(cell.toString());
            rows.add(currentRow);
        }
        return rows;
    }
}
