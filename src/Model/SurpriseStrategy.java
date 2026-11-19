package Model;

/**
 * Strategy: כל סוג הפתעה מתכנן את ההשפעה שלו באופן עצמאי.
 */
public interface SurpriseStrategy {
    SurpriseType getType();
    SurprisePlan createPlan(SurpriseContext context);
}