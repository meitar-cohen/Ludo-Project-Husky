
package Model;

/**
 * אירוע שאלה במשחק.
 * מכיל שאלה שנבחרה ואת מספר צעדי התנועה
 * בהתאם לרמת הקושי שלה.
 */
public abstract class QuestionEvent {

    private final Question question;

    protected QuestionEvent(
            Question question, String expectedDifficulty) {

        if (question == null) {
            throw new IllegalArgumentException(
                    "Question cannot be null");
        }

        // מניעת יצירת אירוע ברמה שאינה תואמת לשאלה
        if (!expectedDifficulty.equalsIgnoreCase(
                question.getDifficulty())) {

            throw new IllegalArgumentException(
                    "Question difficulty does not match event type");
        }

        this.question = question;
    }

    public final Question getQuestion() {
        return question;
    }

    // בדיקת נכונות התשובה שנבחרה
    public final boolean isCorrectAnswer(int selectedIndex) {
        return selectedIndex == question.getCorrectIndex();
    }

    // כל אירוע מגדיר את מספר צעדי התנועה שלו
    public abstract int getMoveSteps();

    // אירוע שאלה קלה
    public static final class Easy extends QuestionEvent {

        public Easy(Question question) {
            super(question, "easy");
        }

        @Override
        public int getMoveSteps() {
            return 1;
        }
    }

    // אירוע שאלה בינונית
    public static final class Medium extends QuestionEvent {

        public Medium(Question question) {
            super(question, "medium");
        }

        @Override
        public int getMoveSteps() {
            return 2;
        }
    }

    // אירוע שאלה קשה
    public static final class Hard extends QuestionEvent {

        public Hard(Question question) {
            super(question, "hard");
        }

        @Override
        public int getMoveSteps() {
            return 3;
        }
    }
}
