package View;

import java.awt.Point;

/**
 * One capture shown as a kick: the attacker pulls its boot back, kicks, and the captured piece
 * flies over the board to its own castle. The board creates it when the controller reports a
 * capture (see {@link BoardPanel#onCapture}), and both the 2D and the 3D board draw it from the
 * same timeline.
 */
final class KickEvent {
    /** Seconds the attacker winds up before the kick lands. */
    static final double WINDUP = 0.50;
    /** Seconds the captured piece flies. */
    static final double FLIGHT = 1.10;
    /** Seconds of landing (squash and dust) before the game goes on. */
    static final double LAND = 0.30;
    static final double TOTAL = WINDUP + FLIGHT + LAND;

    final int attackerCorner, attackerSlot, victimCorner, victimSlot;
    /** Board square where it happened (cell coordinates of the board). */
    final Point cell;
    final long startNanos;

    KickEvent(int attackerCorner, int attackerSlot, int victimCorner, int victimSlot,
              Point cell, long startNanos) {
        this.attackerCorner = attackerCorner;
        this.attackerSlot = attackerSlot;
        this.victimCorner = victimCorner;
        this.victimSlot = victimSlot;
        this.cell = new Point(cell);
        this.startNanos = startNanos;
    }

    boolean isAttacker(int corner, int slot) {
        return corner == attackerCorner && slot == attackerSlot;
    }

    boolean isVictim(int corner, int slot) {
        return corner == victimCorner && slot == victimSlot;
    }

    /** 0..1 ease in and out. */
    static double smooth(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }
}
