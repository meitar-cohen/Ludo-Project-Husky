package View;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Draws the kick of a capture: the attacker pulls back and kicks with a boot, a comic burst
 * appears, and the captured piece spins over the board to its castle. {@link #draw} is the flat
 * (2D) board; the 3D board uses the same parts ({@link #drawBootSide}, {@link #drawBurst},
 * {@link #drawSparkle}) with its own camera. Only draws: the rules were already decided by the
 * controller.
 */
final class KickPainter {
    private KickPainter() { }

    /**
     * @param cs     size of a board square in pixels
     * @param cx,cy  centre of the square where the capture happened
     * @param tx,ty  centre of the castle place where the captured piece lands
     * @param t      seconds since the kick started
     */
    static void draw(Graphics2D g2, PiecePanel pieces, int cs, double cx, double cy, double tx, double ty,
                     Color attacker, Color victim, KickStyle style, double t) {
        double dx = tx - cx, dy = ty - cy, dist = Math.hypot(dx, dy);
        double ux = dist < 1 ? 0 : dx / dist, uy = dist < 1 ? -1 : dy / dist;   // direction of the kick
        double angle = Math.atan2(uy, ux);
        double w = KickEvent.WINDUP, f = KickEvent.FLIGHT, l = KickEvent.LAND;

        // ---- attacker: steps back, stretches, then lunges forward
        double back = t < w ? -0.40 * KickEvent.smooth(t / w)
                : 0.10 * Math.max(0, 1 - (t - w) / 0.40) - 0.40 * Math.max(0, 1 - (t - w) / 0.12);
        double stretch = t < w ? 1 - 0.14 * KickEvent.smooth(t / w)
                : 1 + 0.3 * Math.max(0, 1 - (t - w) / 0.25);
        double ax = cx + ux * back * cs, ay = cy + uy * back * cs;
        // both pieces stood on the same square: they step apart before the kick
        double apart = 0.22 * KickEvent.smooth(t / 0.2);
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(ax, ay);
        g.rotate(angle);
        g.scale(stretch, 1 / Math.sqrt(stretch));
        g.rotate(-angle);
        g.translate(-ax, -ay);
        pieces.drawPiece(g, ax, ay, cs, attacker, false, false);
        g.dispose();

        // ---- the captured piece
        if (t < w) {
            double shake = Math.sin(t * 46) * 0.025 * cs * KickEvent.smooth(t / w);
            pieces.drawPiece(g2, cx + ux * apart * cs - uy * shake, cy + uy * apart * cs + ux * shake,
                    cs, victim, false, false);
        } else if (t < w + f) {
            double u = (t - w) / f;
            drawFlight(g2, pieces, cs, cx + ux * 0.22 * cs, cy + uy * 0.22 * cs, tx, ty, victim, style, u);
        } else {
            double lt = Math.min(1, (t - w - f) / l);
            drawLanding(g2, pieces, cs, tx, ty, victim, style, lt);
        }

        // ---- the burst at the moment of the kick (behind the boot)
        double s = burstProgress(t);
        if (s >= 0 && s < 1) {
            drawBurst(g2, cs, cx + ux * 0.12 * cs, cy + uy * 0.12 * cs - 0.18 * cs, style, s);
        }

        // ---- the boot, on top
        Graphics2D gb = (Graphics2D) g2.create();
        gb.translate(ax, ay);
        gb.rotate(angle);
        paintBoot(gb, cs, bootReach(t), bootAlpha(t), attacker, style);
        gb.dispose();
    }

    // ------------------------------------------------------------------ shared parts

    /** 0..1 progress of the burst, negative before it starts. */
    static double burstProgress(double t) {
        return (t - (KickEvent.WINDUP - 0.02)) / 0.50;
    }

    /** Boot position along the kick axis (in board squares): swings back, snaps forward, rests. */
    static double bootReach(double t) {
        double w = KickEvent.WINDUP;
        if (t < w - 0.12) {
            return -0.10 - 0.55 * KickEvent.smooth(t / (w - 0.12));
        } else if (t < w + 0.06) {
            return -0.65 + 1.50 * KickEvent.smooth((t - (w - 0.12)) / 0.18);
        }
        return 0.85 - 0.95 * KickEvent.smooth((t - (w + 0.06)) / 0.40);
    }

    static double bootAlpha(double t) {
        if (t < 0.04) {
            return 0;
        }
        double w = KickEvent.WINDUP;
        return t > w + 0.46 ? Math.max(0, 1 - (t - (w + 0.46)) / 0.12) : 1;
    }

    /**
     * The boot for the 3D board: drawn upright on the screen, kicking to the right when
     * {@code sign} is +1 and to the left when it is -1.
     *
     * @param size height of a pawn on the screen, in pixels
     */
    static void drawBootSide(Graphics2D g2, double ax, double ay, double size, double sign, Color attacker,
                             KickStyle style, double t) {
        double alpha = bootAlpha(t);
        if (alpha <= 0) {
            return;
        }
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(ax, ay);
        g.scale(sign < 0 ? -1 : 1, 1);
        paintBoot(g, size, bootReach(t), alpha, attacker, style);
        g.dispose();
    }

    /** Leg and boot, drawn in a frame where +x is the kick direction and the origin is the attacker. */
    private static void paintBoot(Graphics2D g, double cs, double reach, double alpha, Color attacker,
                                  KickStyle style) {
        if (alpha <= 0) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        double bx = reach * cs, by = 0.24 * cs;
        // leg
        g.setStroke(new BasicStroke((float) (cs * 0.17), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(attacker.darker());
        g.draw(new Line2D.Double(0.05 * cs, by * 0.4, bx, by));
        // boot: sole, then the leather
        double bw = 0.66 * cs, bh = 0.36 * cs;
        g.setColor(style.sole);
        g.fill(new RoundRectangle2D.Double(bx - bw * 0.35, by + bh * 0.30, bw, bh * 0.45, bh * 0.4, bh * 0.4));
        g.setColor(style.boot);
        Path2D boot = new Path2D.Double();
        boot.moveTo(bx - bw * 0.35, by - bh * 0.55);
        boot.lineTo(bx + bw * 0.05, by - bh * 0.55);
        boot.lineTo(bx + bw * 0.10, by + bh * 0.05);
        boot.curveTo(bx + bw * 0.45, by + bh * 0.02, bx + bw * 0.70, by + bh * 0.16, bx + bw * 0.62, by + bh * 0.42);
        boot.lineTo(bx - bw * 0.35, by + bh * 0.42);
        boot.closePath();
        g.fill(boot);
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke((float) Math.max(1, cs * 0.025)));
        g.draw(new Line2D.Double(bx - bw * 0.2, by - bh * 0.30, bx + bw * 0.05, by - bh * 0.30));
    }

    private static void drawFlight(Graphics2D g2, PiecePanel pieces, int cs, double sx, double sy, double tx,
                                   double ty, Color victim, KickStyle style, double u) {
        double dist = Math.hypot(tx - sx, ty - sy);
        double arcH = Math.max(1.6 * cs, Math.min(4.2 * cs, dist * 0.28));
        // afterimages first, then the piece
        for (int i = 4; i >= 0; i--) {
            double ui = Math.max(0, u - 0.045 * i);
            double lift = arcH * 4 * ui * (1 - ui);
            double x = sx + (tx - sx) * ui, y = sy + (ty - sy) * ui - lift;
            double scale = 1 + 0.55 * 4 * ui * (1 - ui);
            Graphics2D g = (Graphics2D) g2.create();
            if (i == 0) {
                // ground shadow
                double sh = 1 - 0.55 * 4 * ui * (1 - ui);
                g.setColor(new Color(25, 32, 69, (int) (70 * sh)));
                double gx = sx + (tx - sx) * ui, gy = sy + (ty - sy) * ui + cs * 0.22;
                g.fill(new Ellipse2D.Double(gx - cs * 0.36 * sh, gy - cs * 0.12 * sh, cs * 0.72 * sh, cs * 0.24 * sh));
            } else {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.28 - 0.055 * i)));
            }
            g.translate(x, y);
            g.rotate(ui * Math.PI * 6);
            g.scale(scale, scale);
            g.translate(-x, -y);
            pieces.drawPiece(g, x, y, cs, victim, false, false);
            g.dispose();
        }
        // sparkles around the flying piece
        double lift = arcH * 4 * u * (1 - u);
        double x = sx + (tx - sx) * u, y = sy + (ty - sy) * u - lift;
        for (int i = 0; i < 4; i++) {
            double a = u * 9 + i * Math.PI / 2;
            double r = cs * (0.62 + 0.12 * Math.sin(u * 14 + i));
            drawSparkle(g2, x + Math.cos(a) * r, y + Math.sin(a) * r * 0.8, cs * 0.12, style.burst);
        }
    }

    private static void drawLanding(Graphics2D g2, PiecePanel pieces, int cs, double tx, double ty, Color victim,
                                    KickStyle style, double lt) {
        double squash = (1 - lt) * Math.abs(Math.cos(lt * Math.PI * 2.5)) * 0.32;
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(tx, ty + cs * 0.4);
        g.scale(1 + squash * 0.6, 1 - squash);
        g.translate(-tx, -(ty + cs * 0.4));
        pieces.drawPiece(g, tx, ty, cs, victim, false, false);
        g.dispose();
        // dust ring
        double r = cs * (0.4 + 0.9 * lt);
        g2.setStroke(new BasicStroke((float) Math.max(1.5, cs * 0.07 * (1 - lt))));
        g2.setColor(new Color(style.burst.getRed(), style.burst.getGreen(), style.burst.getBlue(), (int) (200 * (1 - lt))));
        g2.draw(new Ellipse2D.Double(tx - r, ty + cs * 0.1 - r * 0.45, 2 * r, r * 0.9));
    }

    static void drawSparkle(Graphics2D g2, double x, double y, double r, Color color) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4, rr = i % 2 == 0 ? r : r * 0.32;
            double px = x + Math.cos(a) * rr, py = y + Math.sin(a) * rr;
            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.closePath();
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fill(star);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(star);
    }

    /** The comic burst with the word of the world ("POW!", "BOOM!"...); {@code s} runs from 0 to 1. */
    static void drawBurst(Graphics2D g2, double cs, double x, double y, KickStyle style, double s) {
        double grow = 1 - Math.pow(1 - Math.min(1, s * 2.2), 3);        // quick pop
        double scale = (0.45 + 0.85 * grow) * (1 + 0.10 * s);
        float alpha = (float) Math.max(0, Math.min(1, 1.25 - s * 1.25));
        double outer = cs * 0.80 * scale, inner = outer * 0.52;
        Path2D burst = new Path2D.Double();
        int spikes = 13;
        for (int i = 0; i < spikes * 2; i++) {
            double a = i * Math.PI / spikes + 0.3;
            double r = i % 2 == 0 ? outer : inner;
            double px = x + Math.cos(a) * r, py = y + Math.sin(a) * r;
            if (i == 0) burst.moveTo(px, py); else burst.lineTo(px, py);
        }
        burst.closePath();
        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) Math.max(1, outer),
                new float[]{0f, 1f}, new Color[]{style.burst, style.burstRim}));
        g.fill(burst);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke((float) Math.max(1.5, cs * 0.05)));
        g.draw(burst);
        // the word
        Font font = new Font("Arial Black", Font.BOLD | Font.ITALIC, Math.max(8, (int) (cs * 0.40 * scale)));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        double tx = x - fm.stringWidth(style.word) / 2.0, ty = y + (fm.getAscent() - fm.getDescent()) / 2.0;
        g.rotate(-0.12, x, y);
        g.setColor(new Color(60, 20, 20));
        g.drawString(style.word, (float) (tx + 2), (float) (ty + 2));
        g.setColor(Color.WHITE);
        g.drawString(style.word, (float) tx, (float) ty);
        g.dispose();
    }
}
