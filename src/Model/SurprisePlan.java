package Model;

import Enums.SurpriseEventType;

/**
 * תוצאת התכנון של Strategy: סוג הפעולה ויעד תנועה במקרה הרלוונטי.
 * targetStep הוא -1 כאשר התוצאה אינה תנועה רגילה לאחור.
 * ביצוע ההשפעה עצמה יתבצע דרך Controller בשלב החיבור למשחק.
 */
public final class SurprisePlan {
    private final SurpriseEventType type;
    private final int targetStep;

    public SurprisePlan(SurpriseEventType type, int targetStep) {
        if (type == null) {
            throw new IllegalArgumentException("Surprise type is required");
        }
        if (type == SurpriseEventType.BACK_3_SQUARES) {
            if (targetStep < 0) {
                throw new IllegalArgumentException("Backward move needs a valid target");
            }
        } else if (targetStep != -1) {
            throw new IllegalArgumentException("Only backward movement has a numeric target");
        }
        this.type = type;
        this.targetStep = targetStep;
    }

    public SurpriseEventType getType() {
        return type;
    }

    public int getTargetStep() {
        return targetStep;
    }
}