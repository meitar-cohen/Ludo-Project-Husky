package Controller;

import Enums.SurpriseEventType;

/** תוצאת ביצוע הפתעה; מיועדת לסנכרון ה-View עם המודל. */
public final class SurpriseResolution {
    private final SurpriseEventType type;
    private final int affectedPlayer;
    private final int affectedPiece;
    private final int beforeStep;
    private final int afterStep;
    private final String title;
    private final String description;

    public SurpriseResolution(SurpriseEventType type, int affectedPlayer, int affectedPiece,
            int beforeStep, int afterStep, String title, String description) {
        this.type = type;
        this.affectedPlayer = affectedPlayer;
        this.affectedPiece = affectedPiece;
        this.beforeStep = beforeStep;
        this.afterStep = afterStep;
        this.title = title;
        this.description = description;
    }

    public SurpriseEventType getType() { return type; }
    public int getAffectedPlayer() { return affectedPlayer; }
    public int getAffectedPiece() { return affectedPiece; }
    public int getBeforeStep() { return beforeStep; }
    public int getAfterStep() { return afterStep; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean hasAnimatedMovement() {
        return beforeStep >= 0 && afterStep >= 0 && beforeStep != afterStep;
    }
}