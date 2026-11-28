package Controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import Model.Question;
import Model.QuestionRecord;
import Model.QuestionRepository;

/**
 * פעולות הניהול של מאגר השאלות. אין כאן רכיבי ממשק גרפי.
 * כל פעולה נשמרת ל-CSV לפני עדכון המצב בזיכרון.
 */
public final class QuestionManagementController {
    private static final int MIN_QUESTIONS = 30;

    private final Path csvPath;
    private final List<QuestionRecord> records;

    public QuestionManagementController() throws IOException {
        this(QuestionRepository.getProjectCsvPath());
    }

    public QuestionManagementController(Path csvPath) throws IOException {
        if (csvPath == null) {
            throw new IllegalArgumentException("CSV path is required");
        }
        this.csvPath = csvPath;
        this.records = new ArrayList<>(
                QuestionRepository.loadRecordsFromCsv(csvPath));
    }

    public List<QuestionRecord> getQuestions() {
        return List.copyOf(records);
    }

    public int add(Question question, String pattern,
            String coverage, String source) throws IOException {
        List<QuestionRecord> updated = new ArrayList<>(records);
        int id = records.stream().mapToInt(QuestionRecord::getId)
                .max().orElse(0) + 1;
        updated.add(new QuestionRecord(id, question, pattern, coverage, source));
        saveAndApply(updated);
        return id;
    }

    public void update(int id, Question question, String pattern,
            String coverage, String source) throws IOException {
        List<QuestionRecord> updated = new ArrayList<>(records);
        int index = indexOf(id);
        updated.set(index, new QuestionRecord(id, question, pattern, coverage, source));
        saveAndApply(updated);
    }

    public void delete(int id) throws IOException {
        if (records.size() <= MIN_QUESTIONS) {
            throw new IllegalStateException(
                    "At least 30 questions are required. Add a new question before deleting one.");
        }
        List<QuestionRecord> updated = new ArrayList<>(records);
        updated.remove(indexOf(id));
        saveAndApply(updated);
    }

    /** שומר את הרשומות הקיימות לקובץ ה-CSV הפעיל (גם כשלא בוצעה עריכה). */
    public void save() throws IOException {
        QuestionRepository.saveRecordsToCsv(csvPath, List.copyOf(records));
    }

    private int indexOf(int id) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == id) return i;
        }
        throw new IllegalArgumentException("Question ID not found: " + id);
    }

    private void saveAndApply(List<QuestionRecord> updated) throws IOException {
        Set<String> unique = new HashSet<>();
        for (QuestionRecord record : updated) {
            String normalized = record.getQuestion().getText()
                    .trim().toLowerCase(Locale.ROOT);
            if (!unique.add(normalized)) {
                throw new IllegalArgumentException("A question with this text already exists.");
            }
        }
        for (String level : new String[]{"easy", "medium", "hard"}) {
            boolean found = updated.stream().anyMatch(r ->
                    level.equalsIgnoreCase(r.getQuestion().getDifficulty()));
            if (!found) {
                throw new IllegalStateException(
                        "At least one question is required at " + level + " difficulty.");
            }
        }
        QuestionRepository.saveRecordsToCsv(csvPath, updated);
        records.clear();
        records.addAll(updated);
    }
}
