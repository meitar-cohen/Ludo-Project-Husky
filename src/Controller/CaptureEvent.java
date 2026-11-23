package Controller;

import java.awt.Point;

/**
 * What the views are told when a piece is captured (SRS 3.2.5.3.1): who captured whom, and on
 * which board square it happened. It is a small read-only value, so a view can use it later
 * (for example for an animation) without asking the model again.
 */
public final class CaptureEvent {
    private final int attackerPlayer, attackerPiece, victimPlayer, victimPiece;
    private final Point square;

    public CaptureEvent(int attackerPlayer, int attackerPiece,
                        int victimPlayer, int victimPiece, Point square) {
        this.attackerPlayer = attackerPlayer;
        this.attackerPiece = attackerPiece;
        this.victimPlayer = victimPlayer;
        this.victimPiece = victimPiece;
        this.square = new Point(square);
    }

    public int getAttackerPlayer() { return attackerPlayer; }

    public int getAttackerPiece() { return attackerPiece; }

    public int getVictimPlayer() { return victimPlayer; }

    public int getVictimPiece() { return victimPiece; }

    /** The board square where the capture happened (a copy, the event cannot be changed). */
    public Point getSquare() { return new Point(square); }
}
