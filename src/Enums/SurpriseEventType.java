package Enums;

/**
 * ששת סוגי ההפתעות וההסתברויות שנקבעו בהוראות המשחק.
 */
public enum SurpriseEventType {
    GO_TO_FINISH(10),
    MOVE_OPPONENT_BACK(10),
    BACK_TO_CASTLE(10),
    BACK_3_SQUARES(30),
    LOSE_NEXT_TURN(30),
    EXTRA_ULTRABOOST_TURN(10);

    private final int probabilityPercent;

    SurpriseEventType(int probabilityPercent) {
        this.probabilityPercent = probabilityPercent;
    }

    public int getProbabilityPercent() {
        return probabilityPercent;
    }
}