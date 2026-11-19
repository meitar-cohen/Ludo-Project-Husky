package Model;

/**
 * ששת סוגי ההפתעות וההסתברויות שנקבעו בהוראות המשחק.
 */
public enum SurpriseType {
    FINISH_PIECE(10),
    MOVE_OPPONENT(10),
    RETURN_TO_CASTLE(10),
    MOVE_BACK_THREE(30),
    SKIP_NEXT_TURN(30),
    EXTRA_TURN_ULTRABOOST(10);

    private final int probabilityPercent;

    SurpriseType(int probabilityPercent) {
        this.probabilityPercent = probabilityPercent;
    }

    public int getProbabilityPercent() {
        return probabilityPercent;
    }
}