package View;

import java.awt.BasicStroke;
import java.awt.AlphaComposite;
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
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * "Ludo Nail Salon": pink skies, houses that look like nail salons and pieces with long hair.
 */
final class SalonTheme extends AbstractTheme {

    private static final Color PINK = new Color(255, 120, 190);
    private static final Color[] HAIR = {new Color(96, 56, 34), new Color(250, 226, 150),
            new Color(34, 30, 46), new Color(196, 74, 32)};          // corner 0..3: brown, blonde, black, ginger

    private final double[][] hearts = new double[16][];            // azimuth, speed, size, phase
    private final double[][] clouds = new double[16][];            // azimuth, y fraction, scale, sprite
    private final double[][] stars = new double[40][];

    SalonTheme() {
        super("salon", "Ludo Nail Salon", "Pink skies, nail salons instead of houses, and pieces with long hair.",
                new Color(255, 105, 180));
        Random random = new Random(1717);
        for (int i = 0; i < hearts.length; i++) {
            hearts[i] = new double[]{random.nextDouble() * 6.28, 0.02 + random.nextDouble() * 0.04, 0.5 + random.nextDouble() * 0.9, random.nextDouble()};
        }
        for (int i = 0; i < clouds.length; i++) {
            clouds[i] = new double[]{random.nextDouble() * 6.28, 0.04 + random.nextDouble() * 0.7, 0.35 + random.nextDouble() * 0.5, random.nextInt(6)};
        }
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new double[]{random.nextDouble() * 6.28, 0.02 + random.nextDouble() * 0.3, random.nextDouble() * 6.28};
        }
    }

    // ------------------------------------------------------------------ light and island

    @Override public double ambientLight() { return 0.74; }
    @Override public double directLight() { return 0.38; }
    @Override public Color fogColor() { return new Color(255, 222, 238); }

    @Override
    public IslandStyle island() {
        IslandStyle s = new IslandStyle();
        s.topNear = new Color(255, 218, 236);
        s.topFar = new Color(242, 176, 210);
        s.rim = new Color(204, 94, 154, 210);
        s.soil = new Color[]{new Color(244, 180, 210), new Color(216, 144, 186), new Color(182, 112, 158)};
        s.patchLight = new Color(255, 240, 246, 130);
        s.patchDark = new Color(232, 150, 192, 100);
        s.petals = new Color[]{Color.WHITE, new Color(255, 120, 180), new Color(255, 200, 232), new Color(255, 250, 160)};
        s.frame = new Color(232, 124, 176);
        s.trim = new Color(255, 236, 150);
        s.boardBase = new Color(250, 222, 238);
        s.tileLight = new Color(255, 249, 252);
        s.tileDark = new Color(250, 234, 244);
        return s;
    }

    // ------------------------------------------------------------------ backdrop: pink sky, clouds, hearts

    @Override
    public void paintBackdrop(Backdrop b) {
        double h = AssetCache.HORIZON;
        b.gradient("salon-sky", new float[]{0f, (float) (h * 0.5), (float) h, 0.62f, 1f},
                new Color[]{new Color(255, 96, 178), new Color(255, 156, 206), new Color(255, 230, 240),
                        new Color(246, 176, 226), new Color(212, 140, 224)});
        Graphics2D g = b.g;
        for (double[] s : stars) {
            double x = b.screenX(s[0]);
            if (Double.isNaN(x)) {
                continue;
            }
            double tw = 0.5 + 0.5 * Math.sin(b.time * 2.4 + s[2]);
            sparkle(g, x, b.height * s[1] + b.skyShift, 2 + 4 * tw, new Color(255, 255, 255, (int) (110 + 120 * tw)));
        }
        double ref = Math.max(0.6, b.width / 1000.0);
        for (double[] c : clouds) {
            double x = b.screenX(c[0] + b.time * 0.002);
            if (Double.isNaN(x)) {
                continue;
            }
            int idx = (int) c[3];
            BufferedImage base = b.cache.cloud(idx);
            BufferedImage pink = b.cache.image("salon-cloud-" + idx, base.getWidth(), base.getHeight(), (g2, w, hh) -> {
                g2.drawImage(base, 0, 0, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.55f));
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 190, 226), 0, hh, new Color(255, 120, 196)));
                g2.fillRect(0, 0, w, hh);
            });
            b.sprite(pink, x, b.height * c[1] + b.skyShift, pink.getWidth() * c[2] * ref * 0.8, 0.8f);
        }
        for (double[] hh : hearts) {
            double t = (b.time * hh[1] + hh[3]) % 1.0;
            double x = b.screenX(hh[0] + Math.sin(b.time * 0.5 + hh[3] * 8) * 0.04);
            if (Double.isNaN(x)) {
                continue;
            }
            double y = b.height * (0.95 - 0.9 * t) + b.skyShift * 0.4;
            heart(g, x, y, 14 * hh[2] * ref, new Color(255, 255, 255, (int) (200 * Math.sin(Math.PI * t))));
        }
    }

    private static void sparkle(Graphics2D g, double x, double y, double r, Color c) {
        g.setColor(c);
        Path2D star = new Path2D.Double();
        star.moveTo(x, y - r);
        star.quadTo(x, y, x + r, y);
        star.quadTo(x, y, x, y + r);
        star.quadTo(x, y, x - r, y);
        star.quadTo(x, y, x, y - r);
        g.fill(star);
    }

    private static void heart(Graphics2D g, double x, double y, double s, Color c) {
        Path2D heart = new Path2D.Double();
        heart.moveTo(x, y + s * 0.9);
        heart.curveTo(x - s * 1.6, y - s * 0.1, x - s * 0.9, y - s * 1.2, x, y - s * 0.35);
        heart.curveTo(x + s * 0.9, y - s * 1.2, x + s * 1.6, y - s * 0.1, x, y + s * 0.9);
        heart.closePath();
        g.setColor(c);
        g.fill(heart);
    }

    // ------------------------------------------------------------------ props

    @Override
    public void createProps(PropPlanner p) {
        Random random = new Random(2121);
        for (int i = 0; i < 14; i++) {
            double[] spot = p.marginPoint(1.5, 0.9);
            double s = 0.8 + random.nextDouble() * 0.5;
            p.add("blossom", spot[0], spot[1], 3.0 * s, new Color(255, 150 + random.nextInt(50), 200 + random.nextInt(40)));
            p.shadow(spot[0], spot[1], 0.35 * s, 2.3 * s);
        }
        Color[] polish = {new Color(255, 60, 140), new Color(150, 90, 240), new Color(40, 190, 220), new Color(255, 180, 40), new Color(240, 40, 70)};
        for (int i = 0; i < 12; i++) {
            double[] spot = p.marginPoint(0.5, 0.8);
            p.add("polish", spot[0], spot[1], 1.1, polish[i % polish.length]);
        }
    }

    @Override
    public void paintProp(PropView v) {
        Graphics2D g = v.g;
        double x = v.x, ground = v.ground, ph = v.ph;
        if ("polish".equals(v.prop.kind)) {
            Color c = v.prop.color;
            double w = ph * 0.46;
            g.setPaint(new GradientPaint((float) (x - w / 2), 0f, mix(c, Color.WHITE, 0.45), (float) (x + w / 2), 0f, mix(c, Color.BLACK, 0.25)));
            g.fill(new RoundRectangle2D.Double(x - w / 2, ground - ph * 0.62, w, ph * 0.62, w * 0.4, w * 0.4));
            g.setColor(new Color(34, 30, 40));
            g.fill(new RoundRectangle2D.Double(x - w * 0.22, ground - ph, w * 0.44, ph * 0.4, 4, 4));
            g.setColor(new Color(255, 255, 255, 160));
            g.fill(new RoundRectangle2D.Double(x - w * 0.32, ground - ph * 0.55, w * 0.1, ph * 0.4, 4, 4));
            return;
        }
        // cherry blossom tree
        Color leaf = v.fog > 0 ? mix(v.prop.color, v.fogColor, v.fog) : v.prop.color;
        g.setPaint(new GradientPaint((float) (x - ph * 0.04), 0f, new Color(120, 78, 70), (float) (x + ph * 0.04), 0f, new Color(80, 50, 46)));
        g.fill(new Rectangle2D.Double(x - ph * 0.04, ground - ph * 0.5, ph * 0.08, ph * 0.5));
        double[][] blobs = {{0, -0.62, 0.32}, {-0.22, -0.52, 0.25}, {0.22, -0.52, 0.25}, {-0.12, -0.86, 0.22}, {0.14, -0.82, 0.21}};
        for (double[] bl : blobs) {
            double r = ph * bl[2], cx = x + ph * bl[0], cy = ground + ph * bl[1];
            if (v.quality > 0) {
                g.setColor(leaf);
            } else {
                g.setPaint(new RadialGradientPaint(new Point2D.Double(cx + r * 0.3, cy - r * 0.4), (float) (r * 1.5),
                        new float[]{0f, 0.55f, 1f}, new Color[]{mix(leaf, Color.WHITE, 0.55), leaf, mix(leaf, new Color(190, 60, 130), 0.35)}));
            }
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
    }

    // ------------------------------------------------------------------ the salons

    @Override
    public double[] padLocal(int slot) {
        return new double[]{3.0 + 2 * (slot % 2), 8.4 + 2 * (slot / 2)};
    }

    @Override
    public void buildHouse(HouseBuilder h) {
        Color color = h.color();
        Color wall = mix(new Color(255, 218, 236), color, 0.16);
        Color trim = mix(color, Color.WHITE, 0.15);
        // the shop with large windows and a glass door
        h.box(0.4, 6.4, 0.4, 5.2, 0, 3.6, wall, false, 2);
        h.windowColor(new Color(176, 226, 255));
        h.doors(0.43, 0.57, 0.5);
        h.doorColor(new Color(255, 236, 246));
        h.shadowRect(0.4, 6.4, 0.4, 5.2, 0, 3.8);
        // roof edge in the colour of the player
        h.box(0.25, 6.55, 0.25, 5.35, 3.6, 3.9, trim, false, 0);
        // striped awning over the entrance
        int stripes = 6;
        double u0 = 0.9, width = 5.0 / stripes;
        for (int i = 0; i < stripes; i++) {
            h.frustum(u0 + i * width, u0 + (i + 1) * width, 5.2, 5.28, 2.75,
                    u0 + i * width, u0 + (i + 1) * width, 6.7, 6.78, 2.05, i % 2 == 0 ? color : Color.WHITE);
        }
        h.shadowRect(0.9, 5.9, 5.2, 6.8, 0, 2.6);
        // "NAILS" sign above the awning
        int mark = h.mark();
        h.box(1.9, 4.9, 5.2, 5.5, 3.0, 3.6, new Color(255, 70, 160), false, 0);
        h.label("NAILS", Color.WHITE);
        h.glowSince(mark);
        // the giant bottle of nail polish on the roof
        double bu = 3.4, bv = 2.8;
        h.prism(bu, bv, 0.85, 3.9, 5.5, 12, mix(color, Color.WHITE, 0.05), false, 0);
        h.prism(bu, bv, 0.5, 5.5, 5.85, 12, new Color(222, 224, 232), false, 0);
        h.prism(bu, bv, 0.4, 5.85, 7.1, 10, new Color(34, 30, 40), false, 0);
        h.shadowCircle(bu, bv, 0.85, 3.9, 5.5, 0.6);
        h.shadowCircle(bu, bv, 0.4, 5.85, 7.1, 0.6);
        h.decor("bottle", bu, bv, 7.4, 1.4);
        // pastel decoration around the entrance
        h.prop("blossom", 7.8, 1.3, 0, 3.4, new Color(255, 170, 214));
        h.prop("polish", 6.9, 6.1, 0, 1.0, color);
        h.prop("polish", 7.7, 6.4, 0, 1.0, new Color(255, 120, 190));
    }

    @Override
    public void paintHouseFloor(HouseFloor f) {
        Color color = f.color();
        Color pink = mix(color, new Color(255, 214, 232), 0.7), white = new Color(255, 248, 252);
        f.quad(0.05, 11.95, 0.05, 11.95, pink);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (((i + j) & 1) == 0) {
                    f.quad(i * 1.5, (i + 1) * 1.5, j * 1.5, (j + 1) * 1.5, white);
                }
            }
        }
        // pink carpet from the door
        f.quad(2.7, 4.1, 5.3, 8.3, mix(color, Color.WHITE, 0.2));
        Color edge = mix(color, Color.WHITE, 0.1);
        f.line(0.05, 0.05, 11.95, 0.05, edge, 2.6f);
        f.line(11.95, 0.05, 11.95, 11.95, edge, 2.6f);
        f.line(11.95, 11.95, 0.05, 11.95, edge, 2.6f);
        f.line(0.05, 11.95, 0.05, 0.05, edge, 2.6f);
        for (int slot = 0; slot < 4; slot++) {
            double[] pad = padLocal(slot);
            f.disc(pad[0], pad[1], 0.8, mix(color, Color.WHITE, 0.55));
            f.disc(pad[0], pad[1], 0.58, mix(color, new Color(255, 120, 190), 0.4));
            f.ring(pad[0], pad[1], 0.8, Color.WHITE, 2.4f);
        }
    }

    @Override
    public void paintAnimated(Animated s) {
        Graphics2D g = s.g();
        double[] p = new double[3];
        for (Decor d : s.decors()) {
            if (!"bottle".equals(d.kind) || !s.project(d.x, d.y, d.z, p) || p[2] > 70) {
                continue;
            }
            double scale = s.focal() / p[2];
            glow(g, p[0], p[1] + scale * 0.5, scale * 3.2, mix(d.color, PINK, 0.5), 90);
            for (int i = 0; i < 6; i++) {
                double t = (s.time() * 0.8 + i * 0.17 + d.corner * 0.2) % 1.0;
                double a = i * 1.05 + s.time() * 0.6;
                double r = scale * (1.0 + 0.5 * Math.sin(i + s.time()));
                double alpha = Math.sin(Math.PI * t);
                sparkle(g, p[0] + Math.cos(a) * r * 1.8, p[1] + Math.sin(a) * r * 1.3 - scale * 0.2, Math.max(2, scale * 0.35 * alpha),
                        new Color(255, 255, 255, (int) (230 * alpha)));
            }
        }
    }

    // ------------------------------------------------------------------ pieces with long hair

    @Override
    public void paintPawn(Graphics2D g, Color color, int corner) {
        AssetCache.paintGlossyPawn(g, color);
        double x = AssetCache.PAWN_W / 2.0, ph = AssetCache.PAWN_BODY, ground = AssetCache.PAWN_GROUND;
        double hr = ph * 0.21, hy = ground - ph * 0.80;
        Color hair = mix(HAIR[Math.floorMod(corner, 4)], color, 0.3);
        Color light = mix(hair, Color.WHITE, 0.45), dark = mix(hair, Color.BLACK, 0.4);
        Path2D mane = new Path2D.Double();
        mane.moveTo(x - hr * 1.02, hy + hr * 0.05);
        mane.curveTo(x - hr * 1.12, hy - hr * 1.35, x + hr * 1.12, hy - hr * 1.35, x + hr * 1.02, hy + hr * 0.05);
        mane.curveTo(x + hr * 1.45, hy + hr * 1.6, x + hr * 1.55, ground - ph * 0.64, x + hr * 1.18, ground - ph * 0.52);
        mane.quadTo(x + hr * 0.7, ground - ph * 0.46, x + hr * 0.3, ground - ph * 0.54);
        mane.quadTo(x - hr * 0.2, ground - ph * 0.6, x - hr * 0.6, ground - ph * 0.5);
        mane.quadTo(x - hr * 1.0, ground - ph * 0.46, x - hr * 1.2, ground - ph * 0.54);
        mane.curveTo(x - hr * 1.6, ground - ph * 0.64, x - hr * 1.5, hy + hr * 1.6, x - hr * 1.02, hy + hr * 0.05);
        mane.closePath();
        g.setPaint(new LinearGradientPaint((float) (x - hr * 1.5), 0f, (float) (x + hr * 1.5), 0f,
                new float[]{0f, 0.35f, 0.75f, 1f}, new Color[]{dark, hair, light, hair}));
        g.fill(mane);
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke(2f));
        g.draw(mane);
        // strands and a shiny streak
        g.setColor(new Color(0, 0, 0, 55));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -3; i <= 3; i++) {
            Path2D strand = new Path2D.Double();
            strand.moveTo(x + i * hr * 0.22, hy - hr * 0.8);
            strand.curveTo(x + i * hr * 0.45, hy + hr * 0.4, x + i * hr * 0.55, hy + hr * 1.8, x + i * hr * 0.42, ground - ph * 0.56);
            g.draw(strand);
        }
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D shine = new Path2D.Double();
        shine.moveTo(x + hr * 0.45, hy - hr * 0.7);
        shine.curveTo(x + hr * 0.9, hy - hr * 0.2, x + hr * 1.05, hy + hr * 0.9, x + hr * 0.95, hy + hr * 1.7);
        g.draw(shine);
        // pink bow at the side of the head
        double bx = x - hr * 0.78, by = hy - hr * 0.7;
        g.setColor(mix(color, new Color(255, 120, 190), 0.25));
        Path2D bow = new Path2D.Double();
        bow.moveTo(bx, by);
        bow.lineTo(bx - hr * 0.5, by - hr * 0.35);
        bow.lineTo(bx - hr * 0.5, by + hr * 0.35);
        bow.closePath();
        bow.moveTo(bx, by);
        bow.lineTo(bx + hr * 0.5, by - hr * 0.35);
        bow.lineTo(bx + hr * 0.5, by + hr * 0.35);
        bow.closePath();
        g.fill(bow);
        g.setColor(new Color(255, 160, 210));
        g.fill(new Ellipse2D.Double(bx - hr * 0.13, by - hr * 0.13, hr * 0.26, hr * 0.26));
    }

    // ------------------------------------------------------------------ preview

    @Override
    public void paintPreview(Graphics2D g, int w, int h) {
        smooth(g);
        g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 0.65f, 1f},
                new Color[]{new Color(255, 100, 180), new Color(255, 214, 234), new Color(250, 190, 224)}));
        g.fillRect(0, 0, w, h);
        heart(g, w * 0.82, h * 0.22, h * 0.1, new Color(255, 255, 255, 220));
        heart(g, w * 0.7, h * 0.38, h * 0.06, new Color(255, 255, 255, 180));
        double sx = w * 0.3, base = h * 0.9;
        g.setColor(new Color(255, 190, 220));
        g.fill(new RoundRectangle2D.Double(sx, base - h * 0.5, w * 0.4, h * 0.5, 8, 8));
        g.setColor(new Color(255, 70, 160));
        g.fill(new RoundRectangle2D.Double(sx + w * 0.07, base - h * 0.45, w * 0.26, h * 0.12, 6, 6));
        for (int i = 0; i < 6; i++) {
            g.setColor(i % 2 == 0 ? new Color(255, 90, 170) : Color.WHITE);
            g.fill(new Rectangle2D.Double(sx + i * w * 0.0667, base - h * 0.3, w * 0.0667, h * 0.1));
        }
        g.setColor(new Color(34, 30, 40));
        g.fill(new RoundRectangle2D.Double(sx + w * 0.16, base - h * 0.66, w * 0.07, h * 0.12, 4, 4));
        g.setColor(new Color(255, 90, 170));
        g.fill(new RoundRectangle2D.Double(sx + w * 0.14, base - h * 0.55, w * 0.11, h * 0.1, 8, 8));
        miniPawn(g, w * 0.14, h * 0.94, h * 0.45, new Color(255, 110, 150));
        g.setColor(new Color(250, 226, 150));
        g.fill(new Ellipse2D.Double(w * 0.14 - h * 0.1, h * 0.94 - h * 0.43, h * 0.2, h * 0.12));
    }
}
