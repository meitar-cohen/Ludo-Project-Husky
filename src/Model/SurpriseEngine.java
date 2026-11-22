package Model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * אחראי להגרלת סוג ההפתעה ולהפעלת ה-Strategy המתאימה.
 * ה-Random מוזרק מבחוץ כדי שאפשר יהיה לכתוב בדיקות דטרמיניסטיות.
 */
public final class SurpriseEngine {
    private final Random random;
    private final Map<SurpriseType, SurpriseStrategy> strategies;

    public SurpriseEngine() {
        this(new Random());
    }

    public SurpriseEngine(Random random) {
        this.random = Objects.requireNonNull(random, "Random generator is required");
        EnumMap<SurpriseType, SurpriseStrategy> map = new EnumMap<>(SurpriseType.class);
        register(map, new SurpriseStrategies.FinishPiece());
        register(map, new SurpriseStrategies.MoveOpponent());
        register(map, new SurpriseStrategies.ReturnToCastle());
        register(map, new SurpriseStrategies.MoveBackThree());
        register(map, new SurpriseStrategies.SkipNextTurn());
        register(map, new SurpriseStrategies.ExtraTurnUltraBoost());
        strategies = Map.copyOf(map);

        int total = 0;
        for (SurpriseType type : SurpriseType.values()) {
            total += type.getProbabilityPercent();
            if (!strategies.containsKey(type)) {
                throw new IllegalStateException("No strategy for " + type);
            }
        }
        if (total != 100) {
            throw new IllegalStateException("Surprise probabilities must total 100");
        }
    }

    private static void register(Map<SurpriseType, SurpriseStrategy> map,
                                 SurpriseStrategy strategy) {
        if (map.put(strategy.getType(), strategy) != null) {
            throw new IllegalStateException("Duplicate strategy: " + strategy.getType());
        }
    }

    // הגרלה אחת בטווח 0..99; כל סוג מקבל טווח לפי האחוזים שלו.
    public SurprisePlan draw(SurpriseContext context) {
        Objects.requireNonNull(context, "context");
        int roll = random.nextInt(100);
        int cumulative = 0;
        for (SurpriseType type : SurpriseType.values()) {
            cumulative += type.getProbabilityPercent();
            if (roll < cumulative) {
                return resolve(type, context);
            }
        }
        throw new IllegalStateException("No surprise selected for roll " + roll);
    }

    // פתרון מפורש משמש לבדיקות ולחיבור עתידי של ה-Controller.
    public SurprisePlan resolve(SurpriseType type, SurpriseContext context) {
        if (type == null) {
            throw new IllegalArgumentException("Surprise type is required");
        }
        if (context == null) {
            throw new IllegalArgumentException("Surprise context is required");
        }
        SurpriseStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No strategy for " + type);
        }
        return strategy.createPlan(context);
    }
}