
package Model;

/**
 * מימוש תבנית Factory Method.
 * כל יוצר קונקרטי אחראי ליצור אירוע שאלה
 * ברמת קושי מסוימת מתוך המאגר הקיים.
 */
public abstract class QuestionEventCreator {

    protected final QuestionBank bank;

    protected QuestionEventCreator(QuestionBank bank) {

        if (bank == null) {
            throw new IllegalArgumentException(
                    "Question bank is required");
        }

        this.bank = bank;
    }

    // פעולה משותפת ליצירת אירוע
    public final QuestionEvent create() {
        return createEvent();
    }

    // Factory Method - ממומשת במחלקות היורשות
    protected abstract QuestionEvent createEvent();

    // יוצר אירועי שאלות קלות
    public static final class Easy
            extends QuestionEventCreator {

        public Easy(QuestionBank bank) {
            super(bank);
        }

        @Override
        protected QuestionEvent createEvent() {
            return new QuestionEvent.Easy(
                    bank.drawQuestion("easy"));
        }
    }

    // יוצר אירועי שאלות בינוניות
    public static final class Medium
            extends QuestionEventCreator {

        public Medium(QuestionBank bank) {
            super(bank);
        }

        @Override
        protected QuestionEvent createEvent() {
            return new QuestionEvent.Medium(
                    bank.drawQuestion("medium"));
        }
    }

    // יוצר אירועי שאלות קשות
    public static final class Hard
            extends QuestionEventCreator {

        public Hard(QuestionBank bank) {
            super(bank);
        }

        @Override
        protected QuestionEvent createEvent() {
            return new QuestionEvent.Hard(
                    bank.drawQuestion("hard"));
        }
    }
}
