
package Model;

import java.io.IOException;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * אחראית לקריאת שאלות מקובץ ה-CSV הקיים בפרויקט.
 * המחלקה שייכת לשכבת Model ואינה אחראית להצגת שאלות.
 */
public final class QuestionRepository {

    private QuestionRepository() {
        // אין צורך ליצור מופע של מחלקת שירות זו.
    }

    // טעינת כל השאלות מהקובץ
    public static List<Question> loadFromCsv(Path file)
            throws IOException {

        List<List<String>> rows;

        // קריאת הקובץ בקידוד UTF-8
        try (PushbackReader reader = new PushbackReader(
                Files.newBufferedReader(
                        file, StandardCharsets.UTF_8), 1)) {

            rows = parseCsv(reader);
        }

        if (rows.isEmpty()) {
            throw new IOException(
                    "Question CSV is empty: " + file);
        }

        // קריאת כותרות העמודות
        List<String> header = rows.get(0);

        if (!header.isEmpty()) {
            header.set(0,
                    header.get(0).replace("\uFEFF", ""));
        }

        String[] required = {
                "Difficulty",
                "Question",
                "Answer 1",
                "Answer 2",
                "Answer 3",
                "Answer 4",
                "Correct answer (1-4)"
        };

        // בדיקה שהקובץ מכיל את כל עמודות החובה
        for (String column : required) {
            if (!header.contains(column)) {
                throw new IOException(
                        "Missing required CSV column: " + column);
            }
        }

        List<Question> questions = new ArrayList<>();

        // מעבר על כל רשומות השאלות, ללא שורת הכותרת
        for (int rowIndex = 1;
                rowIndex < rows.size(); rowIndex++) {

            List<String> row = rows.get(rowIndex);

            if (row.size() != header.size()) {
                throw new IOException(
                        "Invalid number of columns at CSV record "
                                + (rowIndex + 1));
            }

            try {
                // חילוץ ארבע אפשרויות התשובה
                String[] options = {
                        get(row, header, "Answer 1"),
                        get(row, header, "Answer 2"),
                        get(row, header, "Answer 3"),
                        get(row, header, "Answer 4")
                };

                // המרה ממספור 1-4 באקסל לאינדקסים 0-3 ב-Java
                int correctNumber = Integer.parseInt(
                        get(row, header,
                                "Correct answer (1-4)").trim());

                if (correctNumber < 1 || correctNumber > 4) {
                    throw new IllegalArgumentException(
                            "Answer must be 1-4");
                }

                // יצירת אובייקט שאלה מתוך נתוני הרשומה
                Question question = new Question(
                        get(row, header, "Question"),
                        options,
                        correctNumber - 1,
                        get(row, header, "Difficulty")
                );

                questions.add(question);

            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "Invalid question in CSV record "
                                + (rowIndex + 1),
                        exception);
            }
        }

        return questions;
    }

    // שליפת נתון לפי שם העמודה והסרת רווחים מיותרים
    private static String get(
            List<String> row,
            List<String> header,
            String column) {

        return row.get(header.indexOf(column)).trim();
    }

    /**
     * פענוח CSV תוך תמיכה בפסיקים בתוך טקסט,
     * מירכאות כפולות וירידות שורה בתוך שדות.
     */
    private static List<List<String>> parseCsv(
            PushbackReader reader) throws IOException {

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

                        if (next != -1) {
                            reader.unread(next);
                        }
                    }

                } else if (cell.length() == 0) {
                    insideQuotes = true;

                } else {
                    throw new IOException(
                            "Unexpected quote in CSV field");
                }

            } else if (c == ',' && !insideQuotes) {
                currentRow.add(cell.toString());
                cell.setLength(0);

            } else if ((c == '\n' || c == '\r')
                    && !insideQuotes) {

                if (c == '\r') {
                    int next = reader.read();

                    if (next != '\n' && next != -1) {
                        reader.unread(next);
                    }
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

        if (insideQuotes) {
            throw new IOException(
                    "Unclosed quoted field in CSV");
        }

        if (hasData) {
            currentRow.add(cell.toString());
            rows.add(currentRow);
        }

        return rows;
    }
}
