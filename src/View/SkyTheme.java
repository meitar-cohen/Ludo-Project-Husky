package View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** "Ludo in the Sky": the original world with a bright sky, clouds and brick castles. */
final class SkyTheme extends AbstractTheme {

    private static final double SUN_AZIMUTH = 0.9;

    // sky objects: type (1 cloud, 2 rock), sprite, azimuth, y fraction, scale, alpha, speed
    private final double[][] objects;

    SkyTheme() {
        super("sky", "Ludo in the Sky", "The classic world: bright sky, drifting clouds and brick castles.",
                new Color(75, 130, 240));
        Random random = new Random(21);
        objects = new double[3 + 9 + 12][];
        int i = 0;
        objects[i++] = new double[]{2, 0, 2.2, 0.27, 0.5, 0.9, 0};
        objects[i++] = new double[]{2, 0, 5.1, 0.22, 0.4, 0.9, 0};
        objects[i++] = new double[]{2, 0, 3.7, 0.31, 0.45, 0.9, 0};
        for (int k = 0; k < 9; k++) {
            objects[i++] = new double[]{1, random.nextInt(6), random.nextDouble() * 6.28,
                    0.04 + random.nextDouble() * 0.30, 0.35 + random.nextDouble() * 0.45,
                    0.45 + random.nextDouble() * 0.4, 0.01 + random.nextDouble() * 0.02};
        }
        for (int k = 0; k < 12; k++) {
            objects[i++] = new double[]{1, random.nextInt(6), random.nextDouble() * 6.28,
                    0.38 + random.nextDouble() * 0.60, 0.4 + random.nextDouble() * 0.6,
                    0.45 + random.nextDouble() * 0.4, 0.006 + random.nextDouble() * 0.012};
        }
    }

    // ------------------------------------------------------------------ backdrop

    @Override
    public void paintBackdrop(Backdrop b) {
        double h = AssetCache.HORIZON;
        b.gradient("sky", new float[]{0f, (float) (h * 0.55), (float) h, 0.62f, 1f},
                new Color[]{new Color(18, 76, 192), new Color(60, 140, 232), new Color(252, 244, 226),
                        new Color(150, 200, 246), new Color(96, 156, 232)});
        double ref = Math.max(0.6, b.width / 1000.0);
        double sunX = b.screenX(SUN_AZIMUTH);
        if (!Double.isNaN(sunX)) {
            int size = (int) (b.width * 0.75);
            BufferedImage sun = b.cache.image("sky-sun", size, size, (g, w, ph) -> g.drawImage(b.cache.sun(), 0, 0, w, ph, null));
            b.sprite(sun, sunX, b.height * 0.17 + b.skyShift, size, 1f);
        }
        int index = 0;
        for (double[] o : objects) {
            index++;
            double x = b.screenX(o[2] + o[6] * b.time * 0.1);
            if (Double.isNaN(x)) {
                continue;
            }
            double y = b.height * o[3] + b.skyShift;
            if (o[0] == 2) {
                b.sprite(b.cache.islet(), x, y, 150 * ref * o[4] * 2.2, (float) o[5]);
            } else {
                BufferedImage cloud = b.cache.cloud((int) o[1]);
                b.sprite(cloud, x, y, cloud.getWidth() * o[4] * ref * 0.8, (float) o[5]);
            }
        }
    }

    // ------------------------------------------------------------------ island and props

    @Override
    public void createProps(PropPlanner p) {
        Random random = new Random(5);
        for (int i = 0; i < 18; i++) {
            double[] spot = p.marginPoint(1.6, 0.9);
            double s = 0.6 + random.nextDouble() * 0.4;
            Color leaf = new Color(46 + random.nextInt(30), 134 + random.nextInt(45), 62 + random.nextInt(25));
            p.add("tree", spot[0], spot[1], 2.8 * s, leaf);
            p.shadow(spot[0], spot[1], 0.35 * s, 2.2 * s);
        }
        for (int i = 0; i < 16; i++) {
            double[] spot = p.marginPoint(0.5, 0.7);
            double s = 0.5 + random.nextDouble() * 0.3;
            p.add("bush", spot[0], spot[1], 1.0 * s, new Color(70 + random.nextInt(30), 150 + random.nextInt(40), 70 + random.nextInt(25)));
        }
    }

    @Override
    public void paintProp(PropView v) {
        Graphics2D g = v.g;
        boolean bush = "bush".equals(v.prop.kind);
        double x = v.x, ground = v.ground, ph = v.ph;
        Color leaf = v.fog > 0 ? mix(v.prop.color, v.fogColor, v.fog) : v.prop.color;
        if (!bush) {
            g.setPaint(new GradientPaint((float) (x - ph * 0.04), 0f, new Color(92, 64, 44),
                    (float) (x + ph * 0.04), 0f, new Color(140, 100, 66)));
            g.fill(new Rectangle2D.Double(x - ph * 0.04, ground - ph * 0.46, ph * 0.08, ph * 0.46));
        }
        double[][] blobs = bush
                ? new double[][]{{-0.22, -0.34, 0.30}, {0.22, -0.34, 0.30}, {0, -0.5, 0.34}}
                : new double[][]{{0, -0.50, 0.30}, {-0.20, -0.62, 0.25}, {0.20, -0.62, 0.25},
                        {-0.12, -0.84, 0.22}, {0.14, -0.80, 0.21}, {0, -0.68, 0.28}};
        for (double[] b : blobs) {
            double r = ph * b[2], cx = x + ph * b[0], cy = ground + ph * b[1];
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cx + r * 0.35, cy - r * 0.40), (float) (r * 1.5),
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{mix(leaf, Color.WHITE, 0.38), leaf, mix(leaf, Color.BLACK, 0.45)}));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
    }

    // ------------------------------------------------------------------ the castles

    @Override
    public void buildHouse(HouseBuilder h) {
        Color color = h.color();
        Color wall = mix(color, new Color(206, 186, 160), 0.62);
        Color wallDark = mix(wall, Color.BLACK, 0.20);
        Color roof = mix(color, Color.BLACK, 0.06);
        // keep with turret
        h.box(0.3, 2.6, 0.3, 2.6, 0, 3.3, wall, true, 2);
        h.doors(0.40, 0.60, 0.40);
        h.banners(color, 0.14, 0.86);
        h.shadowRect(0.3, 2.6, 0.3, 2.6, 0, 3.3);
        h.battlements(0.3, 2.6, 0.3, 2.6, 3.3, 0.36, wallDark);
        h.prism(1.45, 1.45, 0.74, 3.3, 4.8, 10, wall, true, 3);
        h.prism(1.45, 1.45, 0.88, 4.8, 5.05, 10, wallDark, false, 0);
        h.pyramid(1.45, 1.45, 1.0, 5.05, 2.3, 10, roof);
        h.shadowCircle(1.45, 1.45, 0.74, 3.3, 4.8, 1.0);
        h.shadowCircle(1.45, 1.45, 1.0, 5.05, 7.35, 0.03);
        h.decor("flag", 1.45, 1.45, 7.35, 1.15);
        // outer walls with arched wooden gates and banners
        h.box(2.6, 5.5, 0.15, 0.62, 0, 1.3, wall, true, 0);
        h.doors(0.42, 0.58, 0.62);
        h.banners(color, 0.14, 0.86);
        h.battlements(2.6, 5.5, 0.15, 0.62, 1.3, 0.3, wallDark);
        h.shadowRect(2.6, 5.5, 0.15, 0.62, 0, 1.6);
        h.box(0.15, 0.62, 2.6, 5.5, 0, 1.3, wall, true, 0);
        h.doors(0.42, 0.58, 0.62);
        h.banners(color, 0.14, 0.86);
        h.battlements(0.15, 0.62, 2.6, 5.5, 1.3, 0.3, wallDark);
        h.shadowRect(0.15, 0.62, 2.6, 5.5, 0, 1.6);
        // round towers at the two far ends of the walls
        double[][] towers = {{5.3, 0.55}, {0.55, 5.3}};
        for (double[] t : towers) {
            h.prism(t[0], t[1], 0.6, 0, 2.9, 10, wall, true, 3);
            h.prism(t[0], t[1], 0.72, 2.9, 3.14, 10, wallDark, false, 0);
            h.pyramid(t[0], t[1], 0.84, 3.14, 1.75, 10, roof);
            h.shadowCircle(t[0], t[1], 0.6, 0, 2.9, 1.0);
            h.shadowCircle(t[0], t[1], 0.84, 3.14, 4.89, 0.03);
            h.decor("flag", t[0], t[1], 4.89, 0.9);
        }
    }

    @Override
    public void paintHouseFloor(HouseFloor f) {
        Color color = f.color();
        f.quad(0.05, 11.95, 0.05, 11.95, mix(color, Color.WHITE, 0.48));
        Color edge = mix(color, Color.BLACK, 0.1);
        f.line(0.05, 0.05, 11.95, 0.05, edge, 2.2f);
        f.line(11.95, 0.05, 11.95, 11.95, edge, 2.2f);
        f.line(11.95, 11.95, 0.05, 11.95, edge, 2.2f);
        f.line(0.05, 11.95, 0.05, 0.05, edge, 2.2f);
        f.quad(2.65, 6.15, 2.65, 6.15, new Color(236, 230, 218));
        Color joint = new Color(120, 108, 96, 70);
        for (int i = 1; i < 4; i++) {
            double t = 2.65 + 3.5 * i / 4;
            f.line(t, 2.65, t, 6.15, joint, 1f);
            f.line(2.65, t, 6.15, t, joint, 1f);
        }
        for (int slot = 0; slot < 4; slot++) {
            double[] pad = padLocal(slot);
            f.disc(pad[0], pad[1], 0.62, mix(color, Color.WHITE, 0.35));
            f.ring(pad[0], pad[1], 0.62, color.darker(), 1.8f);
        }
    }

    // ------------------------------------------------------------------ animated flags

    @Override
    public void paintAnimated(Animated s) {
        Graphics2D g = s.g();
        double[] right = s.right();
        double[] a = new double[3], b = new double[3], c = new double[3];
        for (Decor f : s.decors()) {
            if (!"flag".equals(f.kind) || !s.project(f.x, f.y, f.z, a) || !s.project(f.x, f.y + f.size, f.z, b) || a[2] > 60) {
                continue;
            }
            g.setColor(new Color(70, 52, 42));
            g.setStroke(new BasicStroke(Math.max(1.4f, (float) (a[1] - b[1]) * 0.05f)));
            g.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
            int segments = 7;
            double top = f.y + f.size, flagH = 0.52;
            double phase = f.x * 1.7 + f.z * 2.3;
            double[][] upper = new double[segments + 1][2], lower = new double[segments + 1][2];
            boolean ok = true;
            for (int i = 0; i <= segments && ok; i++) {
                double t = i / (double) segments;
                double wave = Math.sin(s.time() * 5.5 + phase - t * 4.2) * 0.10 * t;
                double px = f.x + right[0] * t, pz = f.z + right[1] * t;
                if (!s.project(px, top - 0.03 + wave, pz, c)) {
                    ok = false;
                    break;
                }
                upper[i][0] = c[0];
                upper[i][1] = c[1];
                if (!s.project(px, top - flagH * (1 - 0.35 * t) + wave, pz, c)) {
                    ok = false;
                    break;
                }
                lower[i][0] = c[0];
                lower[i][1] = c[1];
            }
            if (!ok) {
                continue;
            }
            Path2D ribbon = new Path2D.Double();
            ribbon.moveTo(upper[0][0], upper[0][1]);
            for (int i = 1; i <= segments; i++) {
                ribbon.lineTo(upper[i][0], upper[i][1]);
            }
            for (int i = segments; i >= 0; i--) {
                ribbon.lineTo(lower[i][0], lower[i][1]);
            }
            ribbon.closePath();
            g.setPaint(new GradientPaint((float) upper[0][0], 0f, mix(f.color, Color.WHITE, 0.22),
                    (float) upper[segments][0] + 0.01f, 0f, mix(f.color, Color.BLACK, 0.25)));
            g.fill(ribbon);
            g.setColor(new Color(0, 0, 0, 70));
            g.setStroke(new BasicStroke(1f));
            g.draw(ribbon);
        }
    }

    // ------------------------------------------------------------------ preview

    @Override
    public void paintPreview(Graphics2D g, int w, int h) {
        smooth(g);
        g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(40, 110, 220), new Color(190, 226, 255), new Color(130, 200, 120)}));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 220));
        g.fill(new Ellipse2D.Double(w * 0.08, h * 0.16, w * 0.22, h * 0.2));
        g.fill(new Ellipse2D.Double(w * 0.18, h * 0.08, w * 0.2, h * 0.22));
        g.fill(new Ellipse2D.Double(w * 0.68, h * 0.12, w * 0.24, h * 0.2));
        // castle
        double cx = w * 0.56, base = h * 0.82, u = h * 0.12;
        Color wall = new Color(190, 140, 130), roof = new Color(220, 70, 70);
        g.setColor(wall);
        g.fill(new Rectangle2D.Double(cx - u * 2.2, base - u * 2.2, u * 4.4, u * 2.2));
        g.fill(new Rectangle2D.Double(cx - u * 3.2, base - u * 3.2, u * 1.2, u * 3.2));
        g.fill(new Rectangle2D.Double(cx + u * 2.0, base - u * 3.2, u * 1.2, u * 3.2));
        g.setColor(roof);
        for (double tx : new double[]{cx - u * 2.6, cx + u * 2.6}) {
            Path2D cone = new Path2D.Double();
            cone.moveTo(tx - u * 0.9, base - u * 3.2);
            cone.lineTo(tx + u * 0.9, base - u * 3.2);
            cone.lineTo(tx, base - u * 4.9);
            cone.closePath();
            g.fill(cone);
        }
        g.setColor(new Color(60, 40, 40));
        g.fill(new Rectangle2D.Double(cx - u * 0.45, base - u * 1.2, u * 0.9, u * 1.2));
        miniPawn(g, w * 0.2, h * 0.92, h * 0.45, new Color(230, 70, 80));
    }
}
