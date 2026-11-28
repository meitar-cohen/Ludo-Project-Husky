package Tests;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import Enums.SurpriseEventType;
import Model.SurpriseContext;
import Model.SurpriseEngine;

/**
 * בדיקת הסתברויות מדויקת ללא תלות במזל:
 * מריצים את כל 100 ערכי ההגרלה האפשריים, פעם אחת כל ערך.
 */
public final class SurpriseProbabilityCheck {
    private SurpriseProbabilityCheck() {}

    private static final class SequentialHundredRolls extends Random {
        private static final long serialVersionUID = 1L;
        private int next;

        @Override
        public int nextInt(int bound) {
            if (bound != 100) {
                throw new AssertionError("Surprise selection must use 0..99");
            }
            return next++ % 100;
        }
    }

    public static void main(String[] args) {
        SurpriseEngine engine = new SurpriseEngine(new SequentialHundredRolls());
        SurpriseContext context = new SurpriseContext(10, 52);
        Map<SurpriseEventType, Integer> counts = new EnumMap<>(SurpriseEventType.class);
        for (SurpriseEventType type : SurpriseEventType.values()) counts.put(type, 0);

        for (int i = 0; i < 100; i++) {
            SurpriseEventType type = engine.draw(context).getType();
            counts.put(type, counts.get(type) + 1);
        }

        int total = 0;
        for (SurpriseEventType type : SurpriseEventType.values()) {
            int actual = counts.get(type);
            int expected = type.getProbabilityPercent();
            if (actual != expected) {
                throw new AssertionError(type + ": expected " + expected + ", got " + actual);
            }
            total += actual;
            System.out.println("PASS - " + type + ": " + actual + "%");
        }
        if (total != 100) {
            throw new AssertionError("Expected exactly 100 outcomes");
        }
        System.out.println("All surprise probability checks passed.");
    }
}