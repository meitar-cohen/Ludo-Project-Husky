package Tests;

/** בדיקת סיכום לשלב 3 ללא צורך במשחק ידני. */
public final class SurpriseStage3RegressionCheck {
    public static void main(String[] args) throws Exception {
        SurpriseStrategyCheck.main(args);
        SurpriseProbabilityCheck.main(args);
        SurpriseTurnRulesCheck.main(args);
        SurpriseMovementRuleCheck.main(args);
        SurpriseControllerIntegrationCheck.main(args);
        System.out.println("All surprise stage 3 regression checks passed.");
    }
}