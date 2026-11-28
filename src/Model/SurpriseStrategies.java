package Model;

import java.util.Objects;

import Enums.SurpriseEventType;

/**
 * המימושים הקונקרטיים של Strategy לכל אחד מסוגי ההפתעות.
 */
public final class SurpriseStrategies {

    private SurpriseStrategies() {
    }

    // מאיץ את הכלי למשבצת הסיום. ביצוע הסיום יטופל בבקר.
    public static final class FinishPiece implements SurpriseStrategy {
        @Override
        public SurpriseEventType getType() {
            return SurpriseEventType.GO_TO_FINISH;
        }

        @Override
        public SurprisePlan createPlan(SurpriseContext context) {
            Objects.requireNonNull(context, "context");
            return new SurprisePlan(getType(), -1);
        }
    }

    // בוחר באפקט הזזת יריב; הבקר יבחר כלי יריב ויטיל קובייה רגילה.
    public static final class MoveOpponent implements SurpriseStrategy {
        @Override
        public SurpriseEventType getType() {
            return SurpriseEventType.MOVE_OPPONENT_BACK;
        }

        @Override
        public SurprisePlan createPlan(SurpriseContext context) {
            Objects.requireNonNull(context, "context");
            return new SurprisePlan(getType(), -1);
        }
    }

    // מחזיר את הכלי הפעיל לטירה; הבקר יעדכן את מצב הכלי והמונה.
    public static final class ReturnToCastle implements SurpriseStrategy {
        @Override
        public SurpriseEventType getType() {
            return SurpriseEventType.BACK_TO_CASTLE;
        }

        @Override
        public SurprisePlan createPlan(SurpriseContext context) {
            Objects.requireNonNull(context, "context");
            return new SurprisePlan(getType(), -1);
        }
    }

    // מחשב יעד אמיתי לתנועה אחורה בלי לחרוג מתחילת המסלול.
    public static final class MoveBackThree implements SurpriseStrategy {
        @Override
        public SurpriseEventType getType() {
            return SurpriseEventType.BACK_3_SQUARES;
        }

        @Override
        public SurprisePlan createPlan(SurpriseContext context) {
            Objects.requireNonNull(context, "context");
            int targetStep = Math.max(0, context.getCurrentStep() - 3);
            return new SurprisePlan(getType(), targetStep);
        }
    }

    // מסמן הפסד התור הבא, לא הפסד התור הנוכחי.
    public static final class SkipNextTurn implements SurpriseStrategy {
        @Override
        public SurpriseEventType getType() {
            return SurpriseEventType.LOSE_NEXT_TURN;
        }

        @Override
        public SurprisePlan createPlan(SurpriseContext context) {
            Objects.requireNonNull(context, "context");
            return new SurprisePlan(getType(), -1);
        }
    }

    // מסמן תור נוסף מיידי עם קובייה רגילה ו-UltraBoost.
    public static final class ExtraTurnUltraBoost implements SurpriseStrategy {
        @Override
        public SurpriseEventType getType() {
            return SurpriseEventType.EXTRA_ULTRABOOST_TURN;
        }

        @Override
        public SurprisePlan createPlan(SurpriseContext context) {
            Objects.requireNonNull(context, "context");
            return new SurprisePlan(getType(), -1);
        }
    }
}