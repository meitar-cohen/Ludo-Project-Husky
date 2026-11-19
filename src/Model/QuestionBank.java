
package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * מאגר שאלות בזיכרון.
 * מאפשר לבחור שאלה אקראית לפי רמת קושי.
 */
public final class QuestionBank {

    private final List<Question> questions;
    private final Random random;

    // יצירת מאגר מתוך רשימת שאלות קיימת
    public QuestionBank(List<Question> questions) {
        this(questions, new Random());
    }

    // מאפשר שליטה באקראיות לצורך בדיקות
    public QuestionBank(List<Question> questions, Random random) {

        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Question list cannot be empty");
        }

        if (random == null) {
            throw new IllegalArgumentException(
                    "Random generator is required");
        }

        // שמירת עותק שאינו ניתן לשינוי מבחוץ
        this.questions = List.copyOf(questions);
        this.random = random;
    }

    // בחירת שאלה אקראית ברמת הקושי המבוקשת
    public Question drawQuestion(String difficulty) {

        List<Question> matching =
                findByDifficulty(difficulty);

        if (matching.isEmpty()) {
            throw new IllegalStateException(
                    "No questions available for difficulty: "
                            + difficulty);
        }

        return matching.get(
                random.nextInt(matching.size()));
    }

    // החזרת מספר השאלות הכולל
    public int getTotalCount() {
        return questions.size();
    }

    // החזרת מספר השאלות ברמה מסוימת
    public int getCountForDifficulty(String difficulty) {
        return findByDifficulty(difficulty).size();
    }

    // סינון השאלות לפי רמת הקושי
    private List<Question> findByDifficulty(
            String difficulty) {

        if (difficulty == null || difficulty.isBlank()) {
            throw new IllegalArgumentException(
                    "Difficulty is required");
        }

        String requested = difficulty.trim();
        List<Question> matching = new ArrayList<>();

        for (Question question : questions) {
            if (question.getDifficulty()
                    .equalsIgnoreCase(requested)) {

                matching.add(question);
            }
        }

        return matching;
    }
}
