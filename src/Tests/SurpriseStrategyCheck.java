package Tests;

import Model.SurpriseContext;
import Model.SurpriseEngine;
import Model.SurprisePlan;
import Model.SurpriseType;

/** בדיקות יחידה לכל שש אסטרטגיות ההפתעה. */
public final class SurpriseStrategyCheck {
    private SurpriseStrategyCheck() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectInvalid(Runnable task, String message) {
        try {
            task.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    public static void main(String[] args) {
        SurpriseEngine engine = new SurpriseEngine();
        SurpriseContext context = new SurpriseContext(12, 52);
        for (SurpriseType type : SurpriseType.values()) {
            SurprisePlan plan = engine.resolve(type, context);
            check(plan.getType() == type, "Wrong strategy for " + type);
            if (type != SurpriseType.MOVE_BACK_THREE) {
                check(plan.getTargetStep() == -1,
                        "Unexpected numeric target for " + type);
            }
            System.out.println("PASS - strategy: " + type);
        }

        // תנועה אחורה בקצה המסלול: אסור לקבל אינדקס שלילי.
        check(engine.resolve(SurpriseType.MOVE_BACK_THREE,
                new SurpriseContext(12, 52)).getTargetStep() == 9,
                "Backward movement should use three steps");
        check(engine.resolve(SurpriseType.MOVE_BACK_THREE,
                new SurpriseContext(1, 52)).getTargetStep() == 0,
                "Backward movement should stop at zero");
        check(engine.resolve(SurpriseType.MOVE_BACK_THREE,
                new SurpriseContext(0, 52)).getTargetStep() == 0,
                "Backward movement should stay at zero");

        expectInvalid(() -> new SurpriseContext(-1, 52), "Negative step accepted");
        expectInvalid(() -> new SurpriseContext(52, 52), "Out-of-range step accepted");
        expectInvalid(() -> new SurpriseContext(0, 0), "Zero-sized route accepted");
        expectInvalid(() -> engine.resolve(null, context), "Null surprise type accepted");

        System.out.println("PASS - backward movement and invalid context");
        System.out.println("All surprise strategy checks passed.");
    }
}