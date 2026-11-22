package View;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;
import java.awt.GradientPaint;

/**
 * Sensible defaults for a {@link WorldTheme}, so a concrete world only writes what is different.
 */
abstract class AbstractTheme implements WorldTheme {

    private final String id, title, tagline;
    private final Color accent;

    AbstractTheme(String id, String title, String tagline, Color accent) {
        this.id = id;
        this.title = title;
        this.tagline = tagline;
        this.accent = accent;
    }

    @Override public String id() { return id; }
    @Override public String title() { return title; }
    @Override public String tagline() { return tagline; }
    @Override public Color accent() { return accent; }

    @Override public IslandStyle island() { return new IslandStyle(); }
    @Override public double ambientLight() { return 0.46; }
    @Override public double directLight() { return 0.62; }
    @Override public Color fogColor() { return new Color(214, 232, 250); }
    @Override public void paintAnimated(Animated scene) { }
    @Override public double restLift() { return 0; }
    @Override public double restTilt() { return 0; }

    @Override
    public void paintPawn(Graphics2D g, Color color, int corner) {
        AssetCache.paintGlossyPawn(g, color);
    }

    /** Standing places in a 2 x 2 group (spacing 2 units, like the squares of the board). */
    @Override
    public double[] padLocal(int slot) {
        return new double[]{3.4 + 2 * (slot % 2), 3.4 + 2 * (slot / 2)};
    }

    // ------------------------------------------------------------------ helpers for the strategies

    static Color mix(Color a, Color b, double t) {
        return Mesh3D.mix(a, b, t);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** Soft round glow. */
    static void glow(Graphics2D g, double cx, double cy, double radius, Color color, int maxAlpha) {
        if (radius < 1) {
            return;
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) radius,
                new float[]{0f, 1f}, new Color[]{alpha(color, maxAlpha), alpha(color, 0)}));
        g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    /** Simple cartoon pawn used by the small pictures on the choice screen. */
    static void miniPawn(Graphics2D g, double x, double ground, double h, Color color) {
        Color dark = mix(color, Color.BLACK, 0.4), light = mix(color, Color.WHITE, 0.5);
        Path2D body = new Path2D.Double();
        body.moveTo(x - h * 0.3, ground);
        body.curveTo(x - h * 0.3, ground - h * 0.25, x - h * 0.1, ground - h * 0.4, x - h * 0.1, ground - h * 0.6);
        body.lineTo(x + h * 0.1, ground - h * 0.6);
        body.curveTo(x + h * 0.1, ground - h * 0.4, x + h * 0.3, ground - h * 0.25, x + h * 0.3, ground);
        body.closePath();
        g.setPaint(new GradientPaint((float) (x - h * 0.3), 0f, light, (float) (x + h * 0.3), 0f, dark));
        g.fill(body);
        g.setPaint(new GradientPaint((float) (x - h * 0.15), (float) (ground - h * 0.9), light,
                (float) (x + h * 0.15), (float) (ground - h * 0.6), color));
        g.fill(new Ellipse2D.Double(x - h * 0.17, ground - h * 0.9, h * 0.34, h * 0.34));
    }
}
