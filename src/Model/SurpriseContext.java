package Model;

/**
 * נתוני הכלי הנוכחי שנדרשים לתכנון השפעת ההפתעה.
 * המחלקה אינה משנה את מצב המשחק.
 */
public final class SurpriseContext {
    private final int currentStep;
    private final int routeSize;

    public SurpriseContext(int currentStep, int routeSize) {
        if (routeSize <= 0) {
            throw new IllegalArgumentException("Route size must be positive");
        }
        if (currentStep < 0 || currentStep >= routeSize) {
            throw new IllegalArgumentException("Current step is outside route");
        }
        this.currentStep = currentStep;
        this.routeSize = routeSize;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getRouteSize() {
        return routeSize;
    }
}