package Model;

/**
 * רשומה מלאה מקובץ השאלות, כולל נתוני המקור והנושא.
 * שימור שדות אלה מונע אובדן מידע בעת עריכת שאלה.
 */
public final class QuestionRecord {
    private final int id;
    private final Question question;
    private final String architecturalPattern;
    private final String coverage;
    private final String source;

    public QuestionRecord(int id, Question question,
            String architecturalPattern, String coverage, String source) {
        if (id <= 0) {
            throw new IllegalArgumentException("Question ID must be positive");
        }
        if (question == null) {
            throw new IllegalArgumentException("Question is required");
        }
        if (question.getOptions().length != 4) {
            throw new IllegalArgumentException("Exactly four answers are required");
        }
        String level = question.getDifficulty();
        if (!"easy".equalsIgnoreCase(level)
                && !"medium".equalsIgnoreCase(level)
                && !"hard".equalsIgnoreCase(level)) {
            throw new IllegalArgumentException("Difficulty must be easy, medium or hard");
        }
        this.id = id;
        this.question = question;
        this.architecturalPattern = clean(architecturalPattern);
        this.coverage = clean(coverage);
        this.source = clean(source);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public int getId() { return id; }
    public Question getQuestion() { return question; }
    public String getArchitecturalPattern() { return architecturalPattern; }
    public String getCoverage() { return coverage; }
    public String getSource() { return source; }
}
