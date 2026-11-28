package Model;

import Enums.SurpriseEventType;

/**
 * Strategy: כל סוג הפתעה מתכנן את ההשפעה שלו באופן עצמאי.
 */
public interface SurpriseStrategy {
    SurpriseEventType getType();
    SurprisePlan createPlan(SurpriseContext context);
}