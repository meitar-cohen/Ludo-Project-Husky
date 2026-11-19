package Model;

/**
 * תוצאת התכנון של Strategy: סוג הפעולה ויעד תנועה במקרה הרלוונטי.
 * targetStep הוא -1 כאשר התוצאה אינה תנועה רגילה לאחור.
 * ביצוע ההשפעה עצמה יתבצע דרך Controller בשלב החיבור למשחק.
 */
public final class SurprisePlan {
    private final SurpriseType type;
    private final int targetStep;

    public SurprisePlan(SurpriseType type, int targetStep) {
        if (type == null) {
            throw new IllegalArgumentException("Surprise type is required");
        }
        if (type == SurpriseType.MOVE_BACK_THREE) {
            if (targetStep < 0) {
                throw new IllegalArgumentException("Backward move needs a valid target");
            }
        } else if (targetStep != -1) {
            throw new IllegalArgumentException("Only backward movement has a numeric target");
        }
        this.type = type;
        this.targetStep = targetStep;
    }

    public SurpriseType getType() {
        return type;
    }

    public int getTargetStep() {
        return targetStep;
    }
}