package View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * "Ludo at the Pool": a blue sky without clouds and a big smiling sun with sunglasses. The house
 * of every player is a swimming pool with a lifeguard; waiting pieces lie on sunbeds.
 */
final class PoolTheme extends AbstractTheme {

    private static final Color WATER = new Color(72, 206, 232);
    private static final Color DECK = new Color(244, 236, 218);

    PoolTheme() {
        super("pool", "Ludo at the Pool", "Cloudless sky, a sun with sunglasses, pools, sunbeds and lifeguards.",
                new Color(40, 190, 225));
    }

    // ------------------------------------------------------------------ light and island

    @Override public double ambientLight() { return 0.56; }
    @Override public double directLight() { return 0.56; }
    @Override public Color fogColor() { return new Color(214, 238, 255); }
    @Override public double restLift() { return 0.5; }
    @Override public double restTilt() { return -1.15; }

    @Override
    public IslandStyle island() {
        IslandStyle s = new IslandStyle();
        s.topNear = new Color(244, 222, 160);
        s.topFar = new Color(220, 190, 124);
        s.rim = new Color(150, 110, 62, 210);
        s.soil = new Color[]{new Color(226, 196, 136), new Color(200, 164, 108), new Color(172, 136, 88)};
        s.patchLight = new Color(255, 240, 180, 120);
        s.patchDark = new Color(206, 172, 104, 100);
        s.petals = new Color[]{new Color(232, 60, 80), new Color(255, 140, 190), new Color(255, 160, 60), Color.WHITE};
        s.frame = new Color(214, 172, 120);
        s.trim = new Color(255, 255, 255);
        s.boardBase = new Color(206, 228, 234);
        s.tileLight = new Color(252, 250, 244);
        s.tileDark = new Color(238, 242, 240);
        return s;
    }

    // ------------------------------------------------------------------ backdrop: a clear sky and a happy sun

    @Override
    public void paintBackdrop(Backdrop b) {
        double h = AssetCache.HORIZON;
        b.gradient("pool-sky", new float[]{0f, (float) (h * 0.5), (float) h, 0.62f, 1f},
                new Color[]{new Color(22, 108, 232), new Color(70, 170, 246), new Color(206, 238, 255),
                        new Color(140, 208, 250), new Color(96, 178, 246)});
        Graphics2D g = b.g;
        int size = (int) (Math.min(b.width, b.height) * 0.62);
        double sx = b.width * 0.58 + Math.sin(b.heading) * b.width * 0.03;
        double sy = b.height * 0.24 + b.skyShift * 0.35;
        if (b.quality == 0) {
            int glowSize = (int) (size * 1.9);
            BufferedImage halo = b.cache.image("pool-sun-glow", glowSize, glowSize, (ig, w, hh) -> glow(ig, w / 2.0, hh / 2.0, w / 2.0, new Color(255, 244, 170), 150));
            b.sprite(halo, sx, sy, glowSize, 1f);
        }
        // rays turn slowly behind the face
        double spin = b.time * 0.12;
        g.setColor(new Color(255, 214, 70, 235));
        for (int i = 0; i < 18; i++) {
            double a = spin + i * Math.PI * 2 / 18;
            double r0 = size * 0.36, r1 = size * (i % 2 == 0 ? 0.56 : 0.47), half = 0.07;
            Path2D ray = new Path2D.Double();
            ray.moveTo(sx + Math.cos(a - half) * r0, sy + Math.sin(a - half) * r0);
            ray.lineTo(sx + Math.cos(a) * r1, sy + Math.sin(a) * r1);
            ray.lineTo(sx + Math.cos(a + half) * r0, sy + Math.sin(a + half) * r0);
            ray.closePath();
            g.fill(ray);
        }
        BufferedImage face = b.cache.image("pool-sun-face", size, size, (ig, w, hh) -> paintSunFace(ig, w));
        b.sprite(face, sx, sy, size, 1f);
    }

    private static void paintSunFace(Graphics2D g, int size) {
        double c = size / 2.0, r = size * 0.36;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(c - r * 0.2, c - r * 0.3), (float) (r * 1.4),
                new float[]{0f, 0.6f, 1f},
                new Color[]{new Color(255, 250, 150), new Color(255, 214, 60), new Color(255, 160, 24)}));
        g.fill(new Ellipse2D.Double(c - r, c - r, 2 * r, 2 * r));
        g.setColor(new Color(240, 120, 10));
        g.setStroke(new BasicStroke((float) (size * 0.008)));
        g.draw(new Ellipse2D.Double(c - r, c - r, 2 * r, 2 * r));
        // rosy cheeks
        g.setColor(new Color(255, 120, 100, 110));
        g.fill(new Ellipse2D.Double(c - r * 0.82, c + r * 0.12, r * 0.34, r * 0.22));
        g.fill(new Ellipse2D.Double(c + r * 0.48, c + r * 0.12, r * 0.34, r * 0.22));
        // sunglasses
        double ly = c - r * 0.28, lw = r * 0.74, lh = r * 0.42;
        Color frame = new Color(18, 18, 26);
        for (int side = -1; side <= 1; side += 2) {
            double lx = c + side * r * 0.42 - lw / 2;
            RoundRectangle2D lens = new RoundRectangle2D.Double(lx, ly, lw, lh, lh * 0.7, lh * 0.7);
            g.setPaint(new GradientPaint((float) lx, (float) ly, new Color(60, 70, 96), (float) lx, (float) (ly + lh), frame));
            g.fill(lens);
            g.setColor(frame);
            g.setStroke(new BasicStroke((float) (size * 0.012)));
            g.draw(lens);
            g.setColor(new Color(255, 255, 255, 170));
            g.setStroke(new BasicStroke((float) (size * 0.01), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(lx + lw * 0.18, ly + lh * 0.3, lx + lw * 0.42, ly + lh * 0.14));
        }
        g.setColor(frame);
        g.setStroke(new BasicStroke((float) (size * 0.016), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(c - r * 0.06, ly + lh * 0.25, c + r * 0.06, ly + lh * 0.25));
        g.draw(new Line2D.Double(c - r * 0.8, ly + lh * 0.2, c - r * 0.98, ly + lh * 0.05));
        g.draw(new Line2D.Double(c + r * 0.8, ly + lh * 0.2, c + r * 0.98, ly + lh * 0.05));
        // big smile
        g.setColor(new Color(150, 50, 10));
        g.setStroke(new BasicStroke((float) (size * 0.02), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(c - r * 0.5, c + r * 0.02, r, r * 0.62, 195, 150, Arc2D.OPEN));
        g.setColor(new Color(255, 250, 240));
        g.fill(new Arc2D.Double(c - r * 0.46, c + r * 0.06, r * 0.92, r * 0.52, 200, 140, Arc2D.CHORD));
        g.setColor(new Color(150, 50, 10));
        g.draw(new Arc2D.Double(c - r * 0.46, c + r * 0.06, r * 0.92, r * 0.52, 200, 140, Arc2D.CHORD));
    }

    // ------------------------------------------------------------------ props

    @Override
    public void createProps(PropPlanner p) {
        Random random = new Random(909);
        for (int i = 0; i < 14; i++) {
            double[] spot = p.marginPoint(1.4, 0.9);
            double s = 0.9 + random.nextDouble() * 0.5;
            p.add("palm", spot[0], spot[1], 4.6 * s, new Color(40 + random.nextInt(30), 150 + random.nextInt(50), 60));
            p.shadow(spot[0], spot[1], 0.3 * s, 3.4 * s);
        }
        Color[] balls = {new Color(236, 70, 80), new Color(250, 200, 50), new Color(60, 150, 240), new Color(70, 200, 120)};
        for (int i = 0; i < 8; i++) {
            double[] spot = p.marginPoint(0.5, 0.8);
            p.add("ball", spot[0], spot[1], 1.0, balls[i % balls.length]);
        }
    }

    @Override
    public void paintProp(PropView v) {
        Graphics2D g = v.g;
        double x = v.x, ground = v.ground, ph = v.ph;
        String kind = v.prop.kind;
        if ("ball".equals(kind)) {
            double r = ph * 0.5;
            Ellipse2D ball = new Ellipse2D.Double(x - r, ground - 2 * r, 2 * r, 2 * r);
            Color c = v.prop.color;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x - r * 0.3, ground - r * 1.4), (float) (r * 1.6),
                    new float[]{0f, 0.6f, 1f}, new Color[]{Color.WHITE, c, mix(c, Color.BLACK, 0.4)}));
            g.fill(ball);
            g.setColor(new Color(255, 255, 255, 190));
            g.setStroke(new BasicStroke((float) Math.max(1.2, r * 0.18)));
            g.draw(new Line2D.Double(x - r * 0.9, ground - r, x + r * 0.9, ground - r * 0.9));
            return;
        }
        if ("lifeguard".equals(kind)) {
            paintLifeguard(g, x, ground, ph, v.prop.color);
            return;
        }
        // palm tree
        Color leaf = v.fog > 0 ? mix(v.prop.color, v.fogColor, v.fog) : v.prop.color;
        double topX = x + ph * 0.12, topY = ground - ph * 0.82;
        Path2D trunk = new Path2D.Double();
        trunk.moveTo(x - ph * 0.035, ground);
        trunk.quadTo(x + ph * 0.02, ground - ph * 0.45, topX - ph * 0.025, topY);
        trunk.lineTo(topX + ph * 0.025, topY);
        trunk.quadTo(x + ph * 0.08, ground - ph * 0.45, x + ph * 0.035, ground);
        trunk.closePath();
        g.setPaint(new GradientPaint((float) (x - ph * 0.04), 0f, new Color(150, 110, 70), (float) (x + ph * 0.08), 0f, new Color(96, 66, 40)));
        g.fill(trunk);
        for (int i = 0; i < 9; i++) {
            double a = -Math.PI * 0.95 + i * Math.PI * 1.9 / 8 * 0.5 - Math.PI * 0.05;
            double len = ph * (0.34 + 0.06 * (i % 2));
            double ex = topX + Math.cos(a) * len, ey = topY + Math.sin(a) * len * 0.55 + len * 0.45;
            Path2D frond = new Path2D.Double();
            frond.moveTo(topX, topY);
            frond.quadTo(topX + Math.cos(a) * len * 0.6, topY - len * 0.35, ex, ey);
            frond.quadTo(topX + Math.cos(a) * len * 0.55, topY - len * 0.12, topX, topY);
            frond.closePath();
            g.setPaint(new GradientPaint((float) topX, (float) topY, mix(leaf, Color.WHITE, 0.25), (float) ex, (float) ey, mix(leaf, Color.BLACK, 0.3)));
            g.fill(frond);
        }
        g.setColor(new Color(110, 72, 40));
        g.fill(new Ellipse2D.Double(topX - ph * 0.03, topY, ph * 0.04, ph * 0.04));
        g.fill(new Ellipse2D.Double(topX + ph * 0.005, topY + ph * 0.01, ph * 0.04, ph * 0.04));
    }

    private void paintLifeguard(Graphics2D g, double x, double ground, double ph, Color tint) {
        Color skin = new Color(238, 190, 150), red = new Color(224, 40, 44);
        // legs hanging from the chair
        g.setColor(skin);
        g.fill(new RoundRectangle2D.Double(x - ph * 0.17, ground - ph * 0.34, ph * 0.12, ph * 0.34, 6, 6));
        g.fill(new RoundRectangle2D.Double(x + ph * 0.05, ground - ph * 0.34, ph * 0.12, ph * 0.34, 6, 6));
        // red shorts and tank top
        g.setColor(red);
        g.fill(new RoundRectangle2D.Double(x - ph * 0.2, ground - ph * 0.46, ph * 0.4, ph * 0.18, 6, 6));
        g.setPaint(new GradientPaint((float) (x - ph * 0.2), 0f, new Color(255, 90, 90), (float) (x + ph * 0.2), 0f, red));
        g.fill(new RoundRectangle2D.Double(x - ph * 0.2, ground - ph * 0.8, ph * 0.4, ph * 0.38, 10, 10));
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(x - ph * 0.03, ground - ph * 0.7, ph * 0.06, ph * 0.18));
        g.fill(new Rectangle2D.Double(x - ph * 0.09, ground - ph * 0.64, ph * 0.18, ph * 0.06));
        // arms
        g.setColor(skin);
        g.fill(new RoundRectangle2D.Double(x - ph * 0.3, ground - ph * 0.78, ph * 0.1, ph * 0.3, 6, 6));
        g.fill(new RoundRectangle2D.Double(x + ph * 0.2, ground - ph * 0.78, ph * 0.1, ph * 0.3, 6, 6));
        // head, hat and sunglasses, whistle
        double hr = ph * 0.15, hy = ground - ph * 0.9;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x + hr * 0.3, hy - hr * 0.3), (float) (hr * 1.6),
                new float[]{0f, 1f}, new Color[]{new Color(255, 220, 180), skin}));
        g.fill(new Ellipse2D.Double(x - hr, hy - hr, hr * 2, hr * 2));
        g.setColor(new Color(250, 236, 150));
        g.fill(new Ellipse2D.Double(x - hr * 1.5, hy - hr * 0.7, hr * 3, hr * 0.7));
        g.setColor(tint);
        g.fill(new Rectangle2D.Double(x - hr, hy - hr * 0.95, hr * 2, hr * 0.28));
        g.setColor(new Color(240, 220, 120));
        g.fill(new Arc2D.Double(x - hr * 0.95, hy - hr * 1.5, hr * 1.9, hr * 1.5, 0, 180, Arc2D.CHORD));
        g.setColor(new Color(20, 20, 28));
        g.fill(new RoundRectangle2D.Double(x - hr * 0.85, hy - hr * 0.2, hr * 0.75, hr * 0.38, 6, 6));
        g.fill(new RoundRectangle2D.Double(x + hr * 0.1, hy - hr * 0.2, hr * 0.75, hr * 0.38, 6, 6));
        g.setColor(new Color(250, 250, 250));
        g.fill(new Ellipse2D.Double(x - hr * 0.2, hy + hr * 0.9, hr * 0.4, hr * 0.32));
    }

    // ------------------------------------------------------------------ the pool houses

    private static double[] sunbed(int slot) {
        double[][] beds = {{1.9, 2.0}, {1.9, 4.8}, {1.9, 7.6}, {1.9, 10.4}};
        return beds[slot];
    }

    @Override
    public double[] padLocal(int slot) {
        return sunbed(slot);
    }

    @Override
    public void buildHouse(HouseBuilder h) {
        Color color = h.color();
        Color cream = new Color(250, 244, 228), chrome = new Color(200, 208, 214), towel = mix(color, Color.WHITE, 0.1);
        // coping: the raised edge around the water
        double u0 = 3.8, u1 = 11.2, w = 0.36;
        h.box(u0 - w, u1 + w, u0 - w, u0, 0, 0.3, cream, false, 0);
        h.box(u0 - w, u1 + w, u1, u1 + w, 0, 0.3, cream, false, 0);
        h.box(u0 - w, u0, u0, u1, 0, 0.3, cream, false, 0);
        h.box(u1, u1 + w, u0, u1, 0, 0.3, cream, false, 0);
        // pool ladder
        int tiny = h.mark();
        h.cylinder(false, u0 - 0.1, 5.2, 0.9, 0.07, 1.8, 8, chrome);
        h.cylinder(false, u0 - 0.1, 5.9, 0.9, 0.07, 1.8, 8, chrome);
        h.cylinder(false, u0 - 0.1, 5.55, 1.75, 0.07, 0.7, 8, chrome);
        h.tinySince(tiny);
        h.decor("pool", 7.5, 7.5, 0.04, 7.4);
        // sunbeds with a towel in the colour of the player
        for (int slot = 0; slot < 4; slot++) {
            double[] c = sunbed(slot);
            boolean alongV = true;
            double lu = alongV ? 0.65 : 1.35, lv = alongV ? 1.35 : 0.65;       // half sizes along u and v
            double step = 0.15;
            h.box(c[0] - lu, c[0] + lu, c[1] - lv, c[1] + lv, 0.22, 0.42, cream, false, 0);
            // towel stripe
            if (alongV) {
                h.box(c[0] - lu, c[0] + lu, c[1] - 0.25, c[1] + 0.6, 0.42, 0.46, towel, false, 0);
                h.frustum(c[0] - lu, c[0] + lu, c[1] - lv, c[1] - lv + 0.9, 0.42,
                        c[0] - lu, c[0] + lu, c[1] - lv - 0.45, c[1] - lv - 0.25, 1.15, cream);
            } else {
                h.box(c[0] - 0.25, c[0] + 0.6, c[1] - lv, c[1] + lv, 0.42, 0.46, towel, false, 0);
                h.frustum(c[0] - lu, c[0] - lu + 0.9, c[1] - lv, c[1] + lv, 0.42,
                        c[0] - lu - 0.45, c[0] - lu - 0.25, c[1] - lv, c[1] + lv, 1.15, cream);
            }
            double legU = lu - step, legV = lv - step;
            int legs = h.mark();
            h.box(c[0] - lu, c[0] - legU, c[1] - lv, c[1] - legV, 0, 0.22, chrome, false, 0);
            h.box(c[0] + legU, c[0] + lu, c[1] + legV, c[1] + lv, 0, 0.22, chrome, false, 0);
            h.tinySince(legs);
            h.shadowRect(c[0] - lu, c[0] + lu, c[1] - lv, c[1] + lv, 0, 0.45);
        }
        // beach umbrellas
        double[][] umbrellas = {{5.6, 1.9}, {9.0, 1.9}};
        for (double[] um : umbrellas) {
            h.prism(um[0], um[1], 0.05, 0, 2.7, 6, chrome, false, 0);
            h.pyramidStriped(um[0], um[1], 1.5, 2.6, 0.65, 8, color, Color.WHITE);
            h.shadowCircle(um[0], um[1], 1.5, 2.6, 3.2, 0.2);
        }
        // lifeguard tower with a lifeguard in the chair
        double tu = 11.2, tv = 11.2;
        double[][] legs = {{-0.5, -0.5}, {0.5, -0.5}, {-0.5, 0.5}, {0.5, 0.5}};
        int towerLegs = h.mark();
        for (double[] l : legs) {
            h.box(tu + l[0] - 0.07, tu + l[0] + 0.07, tv + l[1] - 0.07, tv + l[1] + 0.07, 0, 2.3, new Color(250, 250, 250), false, 0);
        }
        h.tinySince(towerLegs);
        h.box(tu - 0.7, tu + 0.7, tv - 0.7, tv + 0.7, 2.3, 2.42, new Color(236, 60, 60), false, 0);
        h.box(tu - 0.55, tu + 0.55, tv + 0.45, tv + 0.55, 2.42, 3.4, new Color(250, 250, 250), false, 0);
        h.pyramidStriped(tu, tv, 1.2, 3.9, 0.55, 8, new Color(236, 60, 60), Color.WHITE);
        h.prism(tu, tv - 0.55, 0.05, 2.42, 3.95, 6, chrome, false, 0);
        h.prop("lifeguard", tu, tv - 0.15, 2.42, 1.6, color);
        h.shadowRect(tu - 0.7, tu + 0.7, tv - 0.7, tv + 0.7, 0, 3.6);
        // a palm in the corner
        h.prop("palm", 0.9, 0.9, 0, 3.4, new Color(60, 170, 80));
    }

    @Override
    public void paintHouseFloor(HouseFloor f) {
        Color color = f.color();
        f.quad(0.05, 11.95, 0.05, 11.95, DECK);
        if (f.detailed()) {
            double[] joints = new double[7 * 2 * 4];
            for (int i = 1; i < 8; i++) {
                double t = i * 1.5;
                joints[(i - 1) * 8] = t; joints[(i - 1) * 8 + 1] = 0.05; joints[(i - 1) * 8 + 2] = t; joints[(i - 1) * 8 + 3] = 11.95;
                joints[(i - 1) * 8 + 4] = 0.05; joints[(i - 1) * 8 + 5] = t; joints[(i - 1) * 8 + 6] = 11.95; joints[(i - 1) * 8 + 7] = t;
            }
            f.lines(joints, new Color(190, 176, 150, 120), 1f);
        }
        Color edge = mix(color, Color.WHITE, 0.2);
        f.lines(new double[]{0.05, 0.05, 11.95, 0.05, 11.95, 0.05, 11.95, 11.95, 11.95, 11.95, 0.05, 11.95, 0.05, 11.95, 0.05, 0.05},
                edge, 2.6f);
        // the water: darker at the edge, lighter in the middle, with moving ripples
        Color water = mix(WATER, color, 0.28);
        f.quad(3.8, 11.2, 3.8, 11.2, mix(water, Color.BLACK, 0.12));
        f.quad(4.3, 10.7, 4.3, 10.7, water);
        f.quad(5.2, 9.8, 5.2, 9.8, mix(water, Color.WHITE, 0.2));
        if (f.detailed()) {
            double[] ripples = new double[7 * 8];
            for (int i = 0; i < 7; i++) {
                double v = 4.6 + i * 1.0;
                double shift = Math.sin(f.time() * 1.6 + i) * 0.5;
                ripples[i * 8] = 4.5 + shift; ripples[i * 8 + 1] = v; ripples[i * 8 + 2] = 6.0 + shift; ripples[i * 8 + 3] = v + 0.1;
                ripples[i * 8 + 4] = 7.4 - shift; ripples[i * 8 + 5] = v + 0.4; ripples[i * 8 + 6] = 9.2 - shift; ripples[i * 8 + 7] = v + 0.3;
            }
            f.lines(ripples, new Color(255, 255, 255, 110), 1.5f);
        }
        f.line(4.3, 7.5, 10.7, 7.5, new Color(30, 90, 150, 70), 2f);
    }

    // ------------------------------------------------------------------ sparkles on the water

    @Override
    public void paintAnimated(Animated s) {
        Graphics2D g = s.g();
        double[] p = new double[3];
        for (Decor d : s.decors()) {
            if (!"pool".equals(d.kind)) {
                continue;
            }
            for (int i = 0; i < 12; i++) {
                double t = (s.time() * 0.7 + i * 0.13 + d.corner * 0.31) % 1.0;
                double a = Math.sin(Math.PI * t);
                double u = (((i * 0.618) % 1.0) - 0.5) * d.size * 0.8, v = (((i * 0.382 + 0.2) % 1.0) - 0.5) * d.size * 0.8;
                if (!s.project(d.x + u, d.y + 0.05, d.z + v, p) || p[2] > 60) {
                    continue;
                }
                double r = Math.max(2, 0.28 * s.focal() / p[2]) * a;
                g.setColor(new Color(255, 255, 255, (int) (220 * a)));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Line2D.Double(p[0] - r, p[1], p[0] + r, p[1]));
                g.draw(new Line2D.Double(p[0], p[1] - r, p[0], p[1] + r));
                g.fill(new Ellipse2D.Double(p[0] - r * 0.25, p[1] - r * 0.25, r * 0.5, r * 0.5));
            }
        }
    }

    // ------------------------------------------------------------------ pieces with a straw hat and sunglasses

    @Override
    public void paintPawn(Graphics2D g, Color color, int corner) {
        AssetCache.paintGlossyPawn(g, color);
        double x = AssetCache.PAWN_W / 2.0, ph = AssetCache.PAWN_BODY, ground = AssetCache.PAWN_GROUND;
        double hr = ph * 0.21, hy = ground - ph * 0.80;
        // sunglasses on the side of the head
        g.setColor(new Color(18, 18, 26));
        g.fill(new RoundRectangle2D.Double(x + hr * 0.02, hy - hr * 0.18, hr * 0.8, hr * 0.42, 10, 10));
        g.fill(new RoundRectangle2D.Double(x - hr * 0.86, hy - hr * 0.18, hr * 0.8, hr * 0.42, 10, 10));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x - hr * 0.06, hy - hr * 0.02, x + hr * 0.02, hy - hr * 0.02));
        g.setColor(new Color(255, 255, 255, 160));
        g.draw(new Line2D.Double(x + hr * 0.14, hy - hr * 0.08, x + hr * 0.34, hy - hr * 0.14));
        // straw hat with a ribbon in the colour of the player
        double brimW = hr * 2.9, brimH = hr * 0.62, top = hy - hr * 0.72;
        g.setPaint(new GradientPaint((float) (x - brimW / 2), 0f, new Color(232, 196, 110), (float) (x + brimW / 2), 0f, new Color(250, 224, 150)));
        g.fill(new Ellipse2D.Double(x - brimW / 2, top - brimH / 2 + hr * 0.2, brimW, brimH));
        g.setPaint(new GradientPaint((float) (x - hr), (float) (top - hr * 0.9), new Color(250, 226, 150), (float) (x + hr), (float) top, new Color(222, 184, 100)));
        g.fill(new Arc2D.Double(x - hr * 0.92, top - hr * 1.15, hr * 1.84, hr * 1.7, 0, 180, Arc2D.CHORD));
        g.setColor(color);
        g.fill(new Rectangle2D.Double(x - hr * 0.9, top - hr * 0.12, hr * 1.8, hr * 0.26));
        g.setColor(new Color(150, 110, 50, 150));
        g.setStroke(new BasicStroke(1.3f));
        for (int i = -4; i <= 4; i++) {
            g.draw(new Line2D.Double(x + i * hr * 0.3, top + hr * 0.18, x + i * hr * 0.42, top + hr * 0.38));
        }
    }

    // ------------------------------------------------------------------ preview

    @Override
    public void paintPreview(Graphics2D g, int w, int h) {
        smooth(g);
        g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 0.65f, 1f},
                new Color[]{new Color(30, 120, 240), new Color(190, 232, 255), new Color(244, 222, 160)}));
        g.fillRect(0, 0, w, h);
        double sx = w * 0.78, sy = h * 0.3, r = h * 0.2;
        g.setColor(new Color(255, 214, 70));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(sx + Math.cos(a) * r * 1.2, sy + Math.sin(a) * r * 1.2, sx + Math.cos(a) * r * 1.6, sy + Math.sin(a) * r * 1.6));
        }
        g.setColor(new Color(255, 200, 40));
        g.fill(new Ellipse2D.Double(sx - r, sy - r, 2 * r, 2 * r));
        g.setColor(new Color(20, 20, 28));
        g.fill(new RoundRectangle2D.Double(sx - r * 0.8, sy - r * 0.3, r * 0.7, r * 0.4, 6, 6));
        g.fill(new RoundRectangle2D.Double(sx + r * 0.1, sy - r * 0.3, r * 0.7, r * 0.4, 6, 6));
        g.setColor(new Color(150, 50, 10));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Arc2D.Double(sx - r * 0.5, sy, r, r * 0.6, 200, 140, Arc2D.OPEN));
        // pool
        g.setColor(new Color(250, 244, 228));
        g.fill(new RoundRectangle2D.Double(w * 0.06, h * 0.62, w * 0.62, h * 0.32, 8, 8));
        g.setPaint(new GradientPaint(0, (float) (h * 0.66), new Color(110, 220, 240), 0, (float) (h * 0.9), new Color(40, 170, 215)));
        g.fill(new RoundRectangle2D.Double(w * 0.1, h * 0.67, w * 0.54, h * 0.22, 6, 6));
        miniPawn(g, w * 0.27, h * 0.95, h * 0.4, new Color(60, 190, 220));
    }
}
