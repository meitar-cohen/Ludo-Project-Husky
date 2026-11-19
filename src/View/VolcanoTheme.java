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
import java.util.Random;

/**
 * "Ludo in the Dark": a night sky with erupting volcanoes in the distance. The house of every
 * player is a volcano with lava in the colour of the player.
 */
final class VolcanoTheme extends AbstractTheme {

    private static final double[][] PROFILE = {{3.6, 0}, {2.6, 1.6}, {1.6, 3.3}, {1.0, 4.5}};   // radius, height
    private static final int VOLCANOES = 8;
    private static final int STARS = 70;

    private final double[][] volcano = new double[VOLCANOES][];      // azimuth, width factor, phase
    private final double[][] stars = new double[STARS][];            // azimuth, y fraction, phase
    private final double[][] embers = new double[46][];              // x, speed, size, phase

    VolcanoTheme() {
        super("volcano", "Ludo in the Dark", "A night of fire: erupting volcanoes, and every house is a volcano.",
                new Color(210, 70, 40));
        Random random = new Random(404);
        for (int i = 0; i < VOLCANOES; i++) {
            volcano[i] = new double[]{i * (2 * Math.PI / VOLCANOES) + random.nextDouble() * 0.3,
                    0.55 + random.nextDouble() * 0.5, random.nextDouble() * 6.28};
        }
        for (int i = 0; i < STARS; i++) {
            stars[i] = new double[]{random.nextDouble() * 6.28, 0.02 + random.nextDouble() * 0.26, random.nextDouble() * 6.28};
        }
        for (int i = 0; i < embers.length; i++) {
            embers[i] = new double[]{random.nextDouble(), 0.03 + random.nextDouble() * 0.06, 1 + random.nextDouble() * 2.2,
                    random.nextDouble()};
        }
    }

    // ------------------------------------------------------------------ light and island

    @Override public double ambientLight() { return 0.38; }
    @Override public double directLight() { return 0.62; }
    @Override public Color fogColor() { return new Color(70, 26, 30); }

    @Override
    public IslandStyle island() {
        IslandStyle s = new IslandStyle();
        s.topNear = new Color(76, 62, 64);
        s.topFar = new Color(40, 32, 36);
        s.rim = new Color(255, 120, 40, 230);
        s.soil = new Color[]{new Color(66, 52, 54), new Color(46, 36, 38), new Color(34, 26, 28)};
        s.patchLight = new Color(180, 70, 30, 120);
        s.patchDark = new Color(18, 14, 16, 120);
        s.petals = null;
        s.frame = new Color(62, 42, 42);
        s.trim = new Color(255, 150, 60);
        s.boardBase = new Color(84, 70, 70);
        s.tileLight = new Color(228, 214, 206);
        s.tileDark = new Color(212, 196, 188);
        return s;
    }

    // ------------------------------------------------------------------ backdrop

    @Override
    public void paintBackdrop(Backdrop b) {
        double h = AssetCache.HORIZON;
        b.gradient("volcano-sky", new float[]{0f, (float) (h * 0.5), (float) h, 0.62f, 1f},
                new Color[]{new Color(6, 5, 20), new Color(58, 18, 52), new Color(222, 92, 32),
                        new Color(72, 20, 22), new Color(22, 7, 9)});
        Graphics2D g = b.g;
        double horizon = b.horizonY();
        // stars
        for (double[] s : stars) {
            double x = b.screenX(s[0]);
            if (Double.isNaN(x)) {
                continue;
            }
            double tw = 0.5 + 0.5 * Math.sin(b.time * 2 + s[2]);
            g.setColor(new Color(255, 240, 230, (int) (60 + 120 * tw)));
            g.fill(new Ellipse2D.Double(x, b.height * s[1] + b.skyShift, 1.8, 1.8));
        }
        // dark smoke drifting in the sky
        for (int i = 0; i < 7; i++) {
            double x = b.screenX(i * 0.9 + b.time * 0.01);
            if (Double.isNaN(x)) {
                continue;
            }
            double y = horizon - b.height * (0.10 + 0.07 * (i % 3));
            double r = b.width * (0.16 + 0.04 * (i % 2));
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) r, new float[]{0f, 1f},
                    new Color[]{new Color(40, 14, 20, 150), new Color(40, 14, 20, 0)}));
            g.fill(new Ellipse2D.Double(x - r, y - r * 0.5, r * 2, r));
        }
        // the volcanoes
        for (double[] v : volcano) {
            double x = b.screenX(v[0]);
            if (Double.isNaN(x)) {
                continue;
            }
            paintFarVolcano(b, x, horizon + 26, b.width * 0.62 * v[1], v[2]);
        }
        // floating embers
        for (double[] e : embers) {
            double t = (b.time * e[1] + e[3]) % 1.0;
            double x = b.width * ((e[0] + 0.05 * Math.sin(b.time * 0.7 + e[3] * 9)) % 1.0);
            double y = b.height * (0.95 - t * 0.9) + b.skyShift * 0.3;
            int a = (int) (180 * Math.sin(Math.PI * t));
            g.setColor(new Color(255, 150 + (int) (60 * Math.sin(b.time * 5 + e[3] * 20)), 50, Math.max(0, a)));
            g.fill(new Ellipse2D.Double(x, y, e[2], e[2]));
        }
    }

    private void paintFarVolcano(Backdrop b, double cx, double base, double width, double phase) {
        Graphics2D g = b.g;
        double height = width * 0.62;
        double top = base - height, craterW = width * 0.13;
        Path2D cone = new Path2D.Double();
        cone.moveTo(cx - width / 2, base);
        cone.curveTo(cx - width * 0.28, base - height * 0.25, cx - craterW * 1.6, top + height * 0.12, cx - craterW, top);
        cone.lineTo(cx + craterW, top);
        cone.curveTo(cx + craterW * 1.6, top + height * 0.12, cx + width * 0.28, base - height * 0.25, cx + width / 2, base);
        cone.closePath();
        g.setPaint(new LinearGradientPaint(0, (float) top, 0, (float) base, new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(70, 24, 24), new Color(30, 16, 22), new Color(46, 20, 24)}));
        g.fill(cone);
        // glowing lava veins
        g.setStroke(new BasicStroke(Math.max(1.5f, (float) (width * 0.012)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 5; i++) {
            double f = (i - 2) / 2.0;
            double glow = 0.55 + 0.45 * Math.sin(b.time * 1.6 + phase + i);
            g.setColor(new Color(255, (int) (90 + 70 * glow), 30, (int) (120 + 110 * glow)));
            Path2D vein = new Path2D.Double();
            vein.moveTo(cx + f * craterW * 0.8, top + 3);
            vein.quadTo(cx + f * craterW * 2.2 + Math.sin(i * 2.1) * 8, top + height * 0.45,
                    cx + f * width * 0.34, base - height * 0.05);
            g.draw(vein);
        }
        // eruption: fountain of lava, glow and smoke
        double pulse = 0.6 + 0.4 * Math.sin(b.time * 1.3 + phase);
        glow(g, cx, top, width * 0.55, new Color(255, 110, 30), (int) (140 * pulse));
        int count = 20;
        for (int i = 0; i < count; i++) {
            double t = (b.time * 0.6 + i / (double) count + phase) % 1.0;
            double a = (i * 0.618) % 1.0 - 0.5;                           // launch direction
            double px = cx + a * width * 0.55 * t;
            double py = top - (height * 0.9 * t - height * 0.9 * t * t) - 2;
            double size = Math.max(2, width * 0.018 * (1 - t * 0.5));
            g.setColor(new Color(255, (int) (140 + 90 * (1 - t)), 40, (int) (230 * (1 - t))));
            g.fill(new Ellipse2D.Double(px - size / 2, py - size / 2, size, size));
        }
        for (int i = 0; i < 5; i++) {
            double t = (b.time * 0.12 + i * 0.2 + phase) % 1.0;
            double r = width * (0.07 + 0.12 * t);
            double sx = cx + Math.sin(phase + i) * width * 0.08 * t + t * width * 0.1;
            double sy = top - height * 0.2 - t * height * 1.1;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), (float) r, new float[]{0f, 1f},
                    new Color[]{new Color(38, 20, 24, (int) (150 * (1 - t))), new Color(38, 20, 24, 0)}));
            g.fill(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));
        }
    }

    // ------------------------------------------------------------------ props

    @Override
    public void createProps(PropPlanner p) {
        Random random = new Random(77);
        for (int i = 0; i < 14; i++) {
            double[] spot = p.marginPoint(1.2, 0.9);
            double s = 0.8 + random.nextDouble() * 1.0;
            p.add("rock", spot[0], spot[1], 1.2 * s, new Color(52, 42, 44));
            p.shadow(spot[0], spot[1], 0.5 * s, 1.0 * s);
        }
        for (int i = 0; i < 12; i++) {
            double[] spot = p.marginPoint(1.4, 0.9);
            p.add("deadtree", spot[0], spot[1], 2.4 + random.nextDouble() * 1.2, new Color(28, 22, 24));
        }
    }

    @Override
    public void paintProp(PropView v) {
        Graphics2D g = v.g;
        double x = v.x, ground = v.ground, ph = v.ph;
        Color base = v.fog > 0 ? mix(v.prop.color, v.fogColor, v.fog) : v.prop.color;
        if ("rock".equals(v.prop.kind)) {
            Random r = new Random((long) (v.prop.seed * 1e6));
            Path2D rock = new Path2D.Double();
            int pts = 8;
            for (int i = 0; i < pts; i++) {
                double a = Math.PI * i / (pts - 1);
                double rad = ph * (0.62 + 0.25 * r.nextDouble());
                double px = x - Math.cos(a) * rad, py = ground - Math.sin(a) * ph * (0.9 + 0.2 * r.nextDouble());
                if (i == 0) {
                    rock.moveTo(px, ground);
                    rock.lineTo(px, py);
                } else {
                    rock.lineTo(px, py);
                }
            }
            rock.lineTo(x + ph * 0.7, ground);
            rock.closePath();
            g.setPaint(new GradientPaint((float) (x - ph * 0.6), (float) (ground - ph), mix(base, Color.WHITE, 0.25),
                    (float) (x + ph * 0.6), (float) ground, mix(base, Color.BLACK, 0.5)));
            g.fill(rock);
            glow(g, x, ground, ph * 0.9, new Color(255, 110, 30), 70);
            g.setColor(new Color(255, 130, 40, 190));
            g.setStroke(new BasicStroke(Math.max(1f, (float) ph * 0.04f)));
            g.draw(new Line2D.Double(x - ph * 0.3, ground - ph * 0.15, x - ph * 0.05, ground - ph * 0.55));
            g.draw(new Line2D.Double(x - ph * 0.05, ground - ph * 0.55, x + ph * 0.25, ground - ph * 0.35));
            return;
        }
        // charred dead tree
        g.setColor(base);
        g.setStroke(new BasicStroke(Math.max(1.5f, (float) (ph * 0.07)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x, ground, x, ground - ph * 0.72));
        g.setStroke(new BasicStroke(Math.max(1f, (float) (ph * 0.04)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x, ground - ph * 0.45, x - ph * 0.28, ground - ph * 0.75));
        g.draw(new Line2D.Double(x, ground - ph * 0.55, x + ph * 0.3, ground - ph * 0.9));
        g.draw(new Line2D.Double(x, ground - ph * 0.72, x - ph * 0.1, ground - ph));
        g.draw(new Line2D.Double(x - ph * 0.28, ground - ph * 0.75, x - ph * 0.4, ground - ph * 0.92));
    }

    // ------------------------------------------------------------------ the volcano houses

    @Override
    public void buildHouse(HouseBuilder h) {
        Color color = h.color();
        Color rock = new Color(58, 46, 50), rockDark = new Color(40, 31, 35);
        Color lava = mix(color, new Color(255, 200, 90), 0.12);
        double cu = 3.7, cv = 3.7;
        // the cone, three sloping rings
        for (int i = 0; i < PROFILE.length - 1; i++) {
            h.roundFrustum(cu, cv, PROFILE[i][0], PROFILE[i + 1][0], PROFILE[i][1], PROFILE[i + 1][1], 14,
                    i % 2 == 0 ? rock : rockDark);
        }
        // crater rim and the glowing lava pool
        h.roundFrustum(cu, cv, 1.0, 1.15, 4.5, 4.75, 14, rockDark);
        int mark = h.mark();
        h.roundFrustum(cu, cv, 0.92, 0.92, 4.62, 4.66, 14, mix(lava, Color.WHITE, 0.2));
        // lava streams running down the slopes, in the colour of the player
        int streams = 6;
        for (int s = 0; s < streams; s++) {
            double angle = s * (2 * Math.PI / streams) + 0.4;
            double width = 0.34;
            double[] radii = {1.05, 1.6, 2.2, 2.9, 3.4};
            for (int k = 0; k < radii.length - 1; k++) {
                double r0 = radii[k] + 0.04 * Math.sin(s * 2 + k), r1 = radii[k + 1];
                double h0 = profileHeight(r0) + 0.05, h1 = profileHeight(r1) + 0.05;
                double w0 = width * (0.6 + 0.2 * k), w1 = width * (0.8 + 0.25 * k);
                double[] p0a = h.point(cu + Math.cos(angle) * r0 - Math.sin(angle) * w0, cv + Math.sin(angle) * r0 + Math.cos(angle) * w0);
                double[] p0b = h.point(cu + Math.cos(angle) * r0 + Math.sin(angle) * w0, cv + Math.sin(angle) * r0 - Math.cos(angle) * w0);
                double[] p1a = h.point(cu + Math.cos(angle) * r1 - Math.sin(angle) * w1, cv + Math.sin(angle) * r1 + Math.cos(angle) * w1);
                double[] p1b = h.point(cu + Math.cos(angle) * r1 + Math.sin(angle) * w1, cv + Math.sin(angle) * r1 - Math.cos(angle) * w1);
                double[] axis = h.point(cu, cv);
                h.mesh().addConvex(new double[]{p0a[0], p0b[0], p1b[0], p1a[0]},
                        new double[]{h.baseY() + h0, h.baseY() + h0, h.baseY() + h1, h.baseY() + h1},
                        new double[]{p0a[1], p0b[1], p1b[1], p1a[1]}, lava, axis[0], h.baseY() + 1, axis[1]);
            }
        }
        h.glowSince(mark);
        h.shadowCircle(cu, cv, 3.6, 0, 4.7, 0.3);
        h.decor("vent", cu, cv, 4.7, 1.0);
        // glowing lava rocks next to the volcano
        double[][] rocks = {{7.4, 1.4, 0.55}, {1.2, 7.6, 0.5}, {7.7, 5.3, 0.4}};
        for (double[] r : rocks) {
            h.roundFrustum(r[0], r[1], r[2], r[2] * 0.5, 0, r[2] * 1.2, 7, rockDark);
            int m = h.mark();
            h.roundFrustum(r[0], r[1], r[2] * 0.5, r[2] * 0.35, r[2] * 1.2, r[2] * 1.25, 7, lava);
            h.glowSince(m);
        }
    }

    private static double profileHeight(double radius) {
        for (int i = 0; i < PROFILE.length - 1; i++) {
            if (radius <= PROFILE[i][0] && radius >= PROFILE[i + 1][0]) {
                double t = (PROFILE[i][0] - radius) / (PROFILE[i][0] - PROFILE[i + 1][0]);
                return PROFILE[i][1] + (PROFILE[i + 1][1] - PROFILE[i][1]) * t;
            }
        }
        return radius > PROFILE[0][0] ? 0 : PROFILE[PROFILE.length - 1][1];
    }

    @Override
    public double[] padLocal(int slot) {
        return new double[]{7.4 + 2 * (slot % 2), 7.4 + 2 * (slot / 2)};
    }

    @Override
    public void paintHouseFloor(HouseFloor f) {
        Color color = f.color();
        Color lava = mix(color, new Color(255, 200, 90), 0.15);
        f.quad(0.05, 11.95, 0.05, 11.95, new Color(54, 44, 48));
        f.quad(0.4, 11.6, 0.4, 11.6, new Color(46, 37, 41));
        // glowing cracks
        Random random = new Random(f.corner() * 31 + 5);
        double pulse = 0.55 + 0.35 * Math.sin(f.time() * 1.8 + f.corner());
        double[] cracks = new double[14 * 3 * 4];
        int n = 0;
        for (int i = 0; i < 14; i++) {
            double u = 0.8 + random.nextDouble() * 10.4, v = 0.8 + random.nextDouble() * 10.4;
            for (int k = 0; k < 3; k++) {
                double nu = Math.max(0.5, Math.min(11.5, u + (random.nextDouble() - 0.5) * 3.2));
                double nv = Math.max(0.5, Math.min(11.5, v + (random.nextDouble() - 0.5) * 3.2));
                cracks[n++] = u; cracks[n++] = v; cracks[n++] = nu; cracks[n++] = nv;
                u = nu;
                v = nv;
            }
        }
        f.lines(cracks, alpha(mix(lava, Color.BLACK, 0.35), (int) (110 + 110 * pulse)), 3.4f);
        f.lines(cracks, alpha(lava, (int) (150 + 100 * pulse)), 1.4f);
        f.lines(new double[]{0.05, 0.05, 11.95, 0.05, 11.95, 0.05, 11.95, 11.95, 11.95, 11.95, 0.05, 11.95, 0.05, 11.95, 0.05, 0.05},
                alpha(lava, 220), 2.4f);
        for (int slot = 0; slot < 4; slot++) {
            double[] pad = padLocal(slot);
            f.disc(pad[0], pad[1], 0.78, new Color(30, 24, 27));
            f.disc(pad[0], pad[1], 0.62, mix(lava, Color.BLACK, 0.45));
            f.ring(pad[0], pad[1], 0.78, alpha(lava, (int) (190 + 60 * pulse)), 3f);
        }
    }

    // ------------------------------------------------------------------ eruptions

    @Override
    public void paintAnimated(Animated s) {
        Graphics2D g = s.g();
        double[] out = new double[3], p = new double[3];
        for (Decor d : s.decors()) {
            if (!"vent".equals(d.kind) || !s.project(d.x, d.y, d.z, out) || out[2] > 70) {
                continue;
            }
            double pulse = 0.6 + 0.4 * Math.sin(s.time() * 2.2 + d.corner);
            Color hot = mix(d.color, new Color(255, 230, 140), 0.4);
            glow(g, out[0], out[1], s.focal() * 2.6 / out[2], hot, (int) (150 * pulse));
            int count = 26;
            for (int i = 0; i < count; i++) {
                double t = (s.time() * 0.7 + i / (double) count + d.corner * 0.17) % 1.0;
                double a = 2 * Math.PI * ((i * 0.618) % 1.0);
                double speed = 0.6 + 2.2 * ((i * 0.37) % 1.0);
                double vy = 5.5 + 3.5 * ((i * 0.73) % 1.0);
                double life = t * 1.7;
                double wx = d.x + Math.cos(a) * speed * life, wz = d.z + Math.sin(a) * speed * life;
                double wy = d.y + vy * life - 4.6 * life * life;
                if (wy < d.y - 3 || !s.project(wx, wy, wz, p)) {
                    continue;
                }
                double size = Math.max(1.5, 0.16 * s.focal() / p[2]) * (1 - t * 0.5);
                g.setColor(new Color(255, (int) (150 + 80 * (1 - t)), 50, (int) (240 * (1 - t))));
                g.fill(new Ellipse2D.Double(p[0] - size / 2, p[1] - size / 2, size, size));
                glow(g, p[0], p[1], size * 2.5, hot, (int) (70 * (1 - t)));
            }
            for (int i = 0; i < 5; i++) {
                double t = (s.time() * 0.18 + i * 0.2 + d.corner * 0.3) % 1.0;
                double wy = d.y + 1.0 + t * 9, wx = d.x + t * 3.2, wz = d.z + Math.sin(i + d.corner) * t * 2;
                if (!s.project(wx, wy, wz, p)) {
                    continue;
                }
                double r = (0.9 + 1.8 * t) * s.focal() / p[2];
                g.setPaint(new RadialGradientPaint(new Point2D.Double(p[0], p[1]), (float) Math.max(2, r), new float[]{0f, 1f},
                        new Color[]{new Color(34, 18, 22, (int) (130 * (1 - t))), new Color(34, 18, 22, 0)}));
                g.fill(new Ellipse2D.Double(p[0] - r, p[1] - r, r * 2, r * 2));
            }
        }
    }

    // ------------------------------------------------------------------ preview

    @Override
    public void paintPreview(Graphics2D g, int w, int h) {
        smooth(g);
        g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 0.65f, 1f},
                new Color[]{new Color(8, 6, 22), new Color(150, 50, 40), new Color(30, 12, 14)}));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 240, 230, 200));
        for (int i = 0; i < 12; i++) {
            g.fill(new Ellipse2D.Double((i * 53 % w), (i * 29 % (h / 2)), 2, 2));
        }
        double cx = w * 0.58, base = h * 0.9, vh = h * 0.62, bw = w * 0.5;
        Path2D cone = new Path2D.Double();
        cone.moveTo(cx - bw / 2, base);
        cone.lineTo(cx - bw * 0.12, base - vh);
        cone.lineTo(cx + bw * 0.12, base - vh);
        cone.lineTo(cx + bw / 2, base);
        cone.closePath();
        g.setColor(new Color(44, 28, 32));
        g.fill(cone);
        glow(g, cx, base - vh, w * 0.3, new Color(255, 120, 40), 190);
        g.setColor(new Color(255, 140, 40));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new Line2D.Double(cx - bw * 0.06, base - vh, cx - bw * 0.28, base - 2));
        g.draw(new Line2D.Double(cx + bw * 0.05, base - vh, cx + bw * 0.2, base - 2));
        for (int i = 0; i < 9; i++) {
            double a = (i - 4) * 0.2;
            g.fill(new Ellipse2D.Double(cx + a * bw * 0.5 - 2, base - vh - (6 + (i % 3) * 8), 4, 4));
        }
        miniPawn(g, w * 0.16, h * 0.93, h * 0.45, new Color(230, 70, 80));
    }
}
