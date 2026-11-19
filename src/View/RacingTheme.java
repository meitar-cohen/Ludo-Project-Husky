package View;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
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
 * "Ludo Grand Prix": a Formula 1 race. The houses are sports cars, the pieces are racing drivers
 * and the island floats in the middle of a stadium with grandstands, crowds and passing cars.
 */
final class RacingTheme extends AbstractTheme {

    private static final String[] SPONSORS = {"SPEED", "TURBO", "NITRO", "RACE", "GRIP", "BOOST", "GEAR"};

    RacingTheme() {
        super("racing", "Ludo Grand Prix", "Sports cars instead of houses, racing drivers and a Formula 1 stadium.",
                new Color(225, 40, 50));
    }

    // ------------------------------------------------------------------ light and island

    @Override public double ambientLight() { return 0.52; }
    @Override public double directLight() { return 0.62; }
    @Override public Color fogColor() { return new Color(200, 216, 236); }

    @Override
    public IslandStyle island() {
        IslandStyle s = new IslandStyle();
        s.topNear = new Color(92, 94, 106);
        s.topFar = new Color(60, 62, 72);
        s.rim = new Color(240, 240, 244, 230);
        s.soil = new Color[]{new Color(76, 78, 88), new Color(56, 58, 68), new Color(42, 44, 52)};
        s.patchLight = new Color(116, 118, 130, 110);
        s.patchDark = new Color(30, 32, 40, 130);
        s.petals = null;
        s.frame = new Color(34, 36, 44);
        s.trim = new Color(255, 255, 255);
        s.boardBase = new Color(72, 74, 84);
        s.tileLight = new Color(238, 240, 244);
        s.tileDark = new Color(218, 222, 228);
        s.kerbs = new Color[]{new Color(222, 40, 52), new Color(250, 250, 252)};
        return s;
    }

    // ------------------------------------------------------------------ backdrop: the stadium

    @Override
    public void paintBackdrop(Backdrop b) {
        double h = AssetCache.HORIZON;
        b.gradient("racing-sky", new float[]{0f, (float) (h * 0.5), (float) h, 0.64f, 1f},
                new Color[]{new Color(52, 132, 228), new Color(120, 184, 244), new Color(214, 232, 248),
                        new Color(92, 100, 118), new Color(38, 42, 54)});
        Graphics2D g = b.g;
        double horizon = b.horizonY();
        double ref = Math.max(0.6, b.width / 1000.0);
        for (int i = 0; i < 7; i++) {
            double x = b.screenX(i * 0.9 + b.time * 0.004);
            if (!Double.isNaN(x)) {
                BufferedImage cloud = b.cache.cloud(i);
                b.sprite(cloud, x, b.height * (0.08 + 0.05 * (i % 4)) + b.skyShift, cloud.getWidth() * 0.45 * ref, 0.8f);
            }
        }
        // the stadium all around, painted once as one long picture
        int panoH = (int) (b.height * 0.74);
        int panoW = (int) Math.min(9000, Math.max(1200, 2 * Math.PI * b.focal));
        BufferedImage pano = b.cache.image("racing-pano", panoW, panoH, (pg, w, ph) -> paintStadium(pg, w, ph));
        double top = horizon - panoH * 0.42;
        double offset = b.centerX - b.focal * (((b.heading % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI));
        for (int k = -1; k <= 1; k++) {
            double x = offset + k * panoW;
            if (x < b.width && x + panoW > 0) {
                g.drawImage(pano, (int) Math.round(x), (int) Math.round(top), null);
            }
        }
        // cars racing past in front of the grandstands
        double lane = horizon + panoH * 0.2;
        for (int i = 0; i < 4; i++) {
            double x = b.screenX(b.time * (0.32 + 0.05 * i) * (i % 2 == 0 ? 1 : -1) + i * 1.7);
            if (Double.isNaN(x)) {
                continue;
            }
            paintRaceCar(g, x, lane + (i % 2) * panoH * 0.035, b.width * 0.085, i % 2 == 0, new Color[]{
                    new Color(226, 40, 50), new Color(250, 200, 40), new Color(50, 120, 240), new Color(250, 250, 250)}[i]);
        }
    }

    private static void paintStadium(Graphics2D g, int w, int h) {
        double y0 = h * 0.42;                                       // the horizon
        Random random = new Random(1234);
        int segments = Math.max(24, w / 160);
        double segW = w / (double) segments;
        // lower wall and the race track in front of the stands
        g.setPaint(new GradientPaint(0, (float) (y0 + h * 0.1), new Color(112, 118, 134), 0, h, new Color(40, 44, 56)));
        g.fill(new Rectangle2D.Double(0, y0 + h * 0.08, w, h));
        g.setColor(new Color(58, 60, 70));
        g.fill(new Rectangle2D.Double(0, y0 + h * 0.18, w, h * 0.13));
        g.setColor(new Color(255, 255, 255, 190));
        for (double x = 0; x < w; x += 70) {
            g.fill(new Rectangle2D.Double(x, y0 + h * 0.245, 36, 3));
        }
        // grandstands with the crowd
        for (int i = 0; i < segments; i++) {
            double x0 = i * segW;
            double roofTop = y0 - h * (0.17 + 0.03 * ((i * 7) % 3));
            g.setPaint(new GradientPaint(0, (float) roofTop, new Color(150, 156, 172), 0, (float) (y0 + h * 0.2), new Color(84, 90, 108)));
            g.fill(new Rectangle2D.Double(x0, roofTop, segW * 0.94, y0 + h * 0.2 - roofTop));
            // crowd rows
            int rows = 7;
            for (int r = 0; r < rows; r++) {
                double ry = y0 - h * 0.09 + r * h * 0.038;
                g.setColor(new Color(60, 64, 80));
                g.fill(new Rectangle2D.Double(x0, ry + h * 0.026, segW * 0.94, h * 0.008));
                for (double dx = 4; dx < segW * 0.94 - 4; dx += 10) {
                    Color c = new Color[]{new Color(240, 60, 60), new Color(250, 220, 60), new Color(70, 140, 250),
                            new Color(250, 250, 250), new Color(80, 200, 120), new Color(250, 140, 40)}[random.nextInt(6)];
                    g.setColor(new Color(236, 190, 150));
                    g.fill(new Ellipse2D.Double(x0 + dx, ry, 5.5, 5.5));
                    g.setColor(c);
                    g.fill(new RoundRectangle2D.Double(x0 + dx - 1, ry + 5, 7.5, h * 0.022, 3, 3));
                }
            }
            // roof edge in red and white
            g.setColor(new Color(46, 50, 62));
            g.fill(new Rectangle2D.Double(x0 - 2, roofTop - h * 0.02, segW * 0.94 + 4, h * 0.022));
            for (double sx = 0; sx < segW * 0.94; sx += 14) {
                g.setColor(((int) (sx / 14) & 1) == 0 ? new Color(224, 40, 52) : Color.WHITE);
                g.fill(new Rectangle2D.Double(x0 + sx, roofTop, 14, h * 0.012));
            }
            // advertising board
            g.setColor(new Color[]{new Color(224, 40, 52), new Color(40, 90, 210), new Color(250, 200, 40), new Color(30, 30, 36)}[i % 4]);
            g.fill(new Rectangle2D.Double(x0, y0 + h * 0.16, segW * 0.94, h * 0.045));
            g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(10, (int) (h * 0.034))));
            FontMetrics fm = g.getFontMetrics();
            String word = SPONSORS[i % SPONSORS.length];
            g.setColor(i % 4 == 2 ? new Color(30, 30, 36) : Color.WHITE);
            g.drawString(word, (float) (x0 + (segW * 0.94 - fm.stringWidth(word)) / 2), (float) (y0 + h * 0.16 + fm.getAscent() * 0.85));
        }
        // floodlight towers
        for (int i = 0; i < segments; i += 6) {
            double x = i * segW + segW * 0.45;
            g.setColor(new Color(70, 74, 90));
            g.fill(new Rectangle2D.Double(x, y0 - h * 0.36, 7, h * 0.56));
            g.setColor(new Color(60, 64, 78));
            g.fill(new RoundRectangle2D.Double(x - 26, y0 - h * 0.4, 60, 24, 6, 6));
            g.setColor(new Color(255, 252, 230));
            for (int k = 0; k < 4; k++) {
                g.fill(new Ellipse2D.Double(x - 20 + k * 14, y0 - h * 0.4 + 6, 9, 9));
            }
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x + 4, y0 - h * 0.385), 60f, new float[]{0f, 1f},
                    new Color[]{new Color(255, 250, 220, 130), new Color(255, 250, 220, 0)}));
            g.fill(new Ellipse2D.Double(x - 56, y0 - h * 0.385 - 60, 120, 120));
        }
    }

    private static void paintRaceCar(Graphics2D g, double x, double y, double length, boolean right, Color color) {
        double dir = right ? 1 : -1, s = length / 10;
        Color dark = mix(color, Color.BLACK, 0.4);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(x - length * 0.5, y - s * 0.2, length, s * 1.2));
        Path2D body = new Path2D.Double();
        body.moveTo(x - dir * s * 5, y - s * 0.9);
        body.lineTo(x - dir * s * 5, y - s * 1.8);
        body.lineTo(x - dir * s * 3.4, y - s * 1.8);
        body.lineTo(x - dir * s * 2.4, y - s * 1.1);
        body.lineTo(x - dir * s * 0.6, y - s * 2.1);
        body.lineTo(x + dir * s * 0.8, y - s * 1.3);
        body.lineTo(x + dir * s * 5, y - s * 1.0);
        body.lineTo(x + dir * s * 5, y - s * 0.7);
        body.lineTo(x - dir * s * 5, y - s * 0.6);
        body.closePath();
        g.setPaint(new GradientPaint(0, (float) (y - s * 2), mix(color, Color.WHITE, 0.3), 0, (float) y, dark));
        g.fill(body);
        g.setColor(new Color(20, 20, 26));
        g.fill(new Ellipse2D.Double(x - dir * s * 3.4 - s * 0.9, y - s * 1.3, s * 1.8, s * 1.8));
        g.fill(new Ellipse2D.Double(x + dir * s * 3.2 - s * 0.9, y - s * 1.3, s * 1.8, s * 1.8));
        g.setColor(new Color(200, 204, 214));
        g.fill(new Ellipse2D.Double(x - dir * s * 3.4 - s * 0.35, y - s * 0.75, s * 0.7, s * 0.7));
        g.fill(new Ellipse2D.Double(x + dir * s * 3.2 - s * 0.35, y - s * 0.75, s * 0.7, s * 0.7));
        g.setColor(new Color(20, 24, 34));
        g.fill(new Rectangle2D.Double(Math.min(x - dir * s * 5.2, x - dir * s * 4.6), y - s * 2.3, s * 0.6, s * 1.0));
    }

    // ------------------------------------------------------------------ props

    @Override
    public void createProps(PropPlanner p) {
        Random random = new Random(2468);
        for (int i = 0; i < 12; i++) {
            double[] spot = p.marginPoint(1.4, 0.9);
            p.add("tyres", spot[0], spot[1], 1.3, new Color(24, 24, 28));
            p.shadow(spot[0], spot[1], 0.6, 1.2);
        }
        for (int i = 0; i < 14; i++) {
            double[] spot = p.marginPoint(0.6, 0.8);
            p.add("cone", spot[0], spot[1], 0.9, new Color(255, 120, 20));
        }
    }

    @Override
    public void paintProp(PropView v) {
        Graphics2D g = v.g;
        double x = v.x, ground = v.ground, ph = v.ph;
        if ("cone".equals(v.prop.kind)) {
            Path2D cone = new Path2D.Double();
            cone.moveTo(x - ph * 0.4, ground);
            cone.lineTo(x - ph * 0.12, ground - ph);
            cone.lineTo(x + ph * 0.12, ground - ph);
            cone.lineTo(x + ph * 0.4, ground);
            cone.closePath();
            g.setPaint(new GradientPaint((float) (x - ph * 0.4), 0f, new Color(255, 160, 60), (float) (x + ph * 0.4), 0f, new Color(220, 80, 10)));
            g.fill(cone);
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Double(x - ph * 0.27, ground - ph * 0.55, ph * 0.54, ph * 0.16));
            g.setColor(new Color(40, 40, 46));
            g.fill(new RoundRectangle2D.Double(x - ph * 0.5, ground - ph * 0.08, ph, ph * 0.1, 4, 4));
            return;
        }
        // stack of tyres with white lettering stripe
        int tyres = 3;
        double th = ph / tyres, w = ph * 0.62;
        for (int i = 0; i < tyres; i++) {
            double ty = ground - (i + 1) * th;
            g.setPaint(new GradientPaint((float) (x - w), 0f, new Color(70, 70, 78), (float) (x + w), 0f, new Color(14, 14, 18)));
            g.fill(new RoundRectangle2D.Double(x - w, ty, w * 2, th * 0.96, th * 0.7, th * 0.7));
            g.setColor((i & 1) == 0 ? new Color(250, 250, 252) : new Color(222, 40, 52));
            g.fill(new RoundRectangle2D.Double(x - w * 0.55, ty + th * 0.35, w * 1.1, th * 0.2, 4, 4));
        }
    }

    // ------------------------------------------------------------------ the sports cars

    @Override
    public double[] padLocal(int slot) {
        return new double[]{3.6 + 2 * (slot % 2), 7.0 + 2 * (slot / 2)};
    }

    @Override
    public void buildHouse(HouseBuilder h) {
        Color color = h.color();
        Color glass = new Color(34, 44, 66), black = new Color(26, 26, 32), silver = new Color(204, 208, 216);
        Color dark = mix(color, Color.BLACK, 0.45);
        // body, with the cabin and a sloping nose
        h.box(0.9, 6.7, 1.5, 4.1, 0.35, 0.9, color, false, 0);
        h.frustum(5.1, 6.7, 1.5, 4.1, 0.9, 6.35, 6.7, 1.95, 3.65, 0.58, color);
        h.frustum(2.4, 4.9, 1.7, 3.9, 0.9, 3.0, 4.35, 2.15, 3.45, 1.58, glass);
        h.frustum(0.9, 2.4, 1.6, 4.0, 0.9, 1.0, 2.4, 1.9, 3.7, 1.15, color);
        // racing stripes
        h.box(0.9, 6.5, 2.65, 2.85, 0.9, 0.94, Color.WHITE, false, 0);
        h.box(0.9, 6.5, 2.95, 3.05, 0.9, 0.94, Color.WHITE, false, 0);
        // wheels with silver hub caps
        double[] us = {1.7, 5.5};
        double[] vs = {1.2, 4.4};
        for (double u : us) {
            for (double v : vs) {
                h.cylinder(false, u, v, 0.44, 0.44, 0.5, 12, black);
                h.cylinder(false, u, v + (v < 2.8 ? -0.03 : 0.03), 0.44, 0.24, 0.52, 10, silver);
            }
        }
        // rear wing
        h.box(0.85, 1.0, 1.5, 1.62, 0.9, 1.9, dark, false, 0);
        h.box(0.85, 1.0, 3.98, 4.1, 0.9, 1.9, dark, false, 0);
        h.box(0.7, 1.4, 1.4, 4.2, 1.75, 1.92, dark, false, 0);
        h.box(0.7, 0.85, 1.4, 4.2, 1.92, 2.0, color, false, 0);
        // headlights that glow
        int mark = h.mark();
        h.box(6.68, 6.74, 1.7, 2.25, 0.5, 0.7, new Color(255, 245, 190), false, 0);
        h.box(6.68, 6.74, 3.35, 3.9, 0.5, 0.7, new Color(255, 245, 190), false, 0);
        h.glowSince(mark);
        h.shadowRect(0.7, 6.8, 1.2, 4.4, 0, 1.6);
        // chequered flag, tyres and a pit board
        h.decor("flag", 0.7, 6.0, 0, 2.7);
        h.prop("tyres", 8.2, 2.2, 0, 1.3, black);
        h.prop("tyres", 8.2, 3.4, 0, 1.3, black);
        h.box(7.6, 7.75, 5.2, 7.4, 0, 1.9, new Color(250, 250, 252), false, 0);
        h.label("PIT", new Color(40, 40, 50));
    }

    @Override
    public void paintHouseFloor(HouseFloor f) {
        Color color = f.color();
        f.quad(0.05, 11.95, 0.05, 11.95, new Color(62, 64, 74));
        f.quad(0.2, 11.8, 0.2, 11.8, new Color(68, 70, 80));
        // painted box around the car and the pit lane line
        Color white = new Color(255, 255, 255, 220);
        f.line(0.5, 1.0, 7.4, 1.0, white, 2f);
        f.line(7.4, 1.0, 7.4, 4.6, white, 2f);
        f.line(7.4, 4.6, 0.5, 4.6, white, 2f);
        f.line(0.5, 4.6, 0.5, 1.0, white, 2f);
        for (double u = 0.4; u < 11.6; u += 1.6) {
            f.line(u, 5.8, u + 0.8, 5.8, white, 2f);
        }
        // kerb stones in the colour of the player around the block
        f.quad(0, 12, 0.05, 0.4, Color.WHITE);
        f.quad(0, 12, 11.6, 11.95, Color.WHITE);
        f.quad(0.05, 0.4, 0, 12, Color.WHITE);
        f.quad(11.6, 11.95, 0, 12, Color.WHITE);
        for (int i = 0; i < 12; i += 2) {
            f.quad(i, i + 1, 0.05, 0.4, color);
            f.quad(i, i + 1, 11.6, 11.95, color);
            f.quad(0.05, 0.4, i, i + 1, color);
            f.quad(11.6, 11.95, i, i + 1, color);
        }
        // starting grid boxes for the waiting pieces
        for (int slot = 0; slot < 4; slot++) {
            double[] pad = padLocal(slot);
            f.quad(pad[0] - 0.8, pad[0] + 0.8, pad[1] - 0.9, pad[1] + 0.9, white);
            f.quad(pad[0] - 0.7, pad[0] + 0.7, pad[1] - 0.8, pad[1] + 0.8, mix(color, new Color(62, 64, 74), 0.7));
        }
    }

    // ------------------------------------------------------------------ chequered flags

    @Override
    public void paintAnimated(Animated s) {
        Graphics2D g = s.g();
        double[] right = s.right();
        double[] a = new double[3], b = new double[3], c = new double[3];
        for (Decor f : s.decors()) {
            if (!"flag".equals(f.kind) || !s.project(f.x, f.y, f.z, a) || !s.project(f.x, f.y + f.size, f.z, b) || a[2] > 60) {
                continue;
            }
            g.setColor(new Color(220, 224, 232));
            g.setStroke(new BasicStroke(Math.max(1.6f, (float) (a[1] - b[1]) * 0.045f)));
            g.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
            int cols = 8, rows = 3;
            double top = f.y + f.size, len = 1.7, hgt = 1.0;
            for (int i = 0; i < cols; i++) {
                for (int j = 0; j < rows; j++) {
                    double[][] q = new double[4][2];
                    boolean ok = true;
                    for (int k = 0; k < 4 && ok; k++) {
                        double t = (i + (k == 1 || k == 2 ? 1 : 0)) / (double) cols;
                        double vv = (j + (k >= 2 ? 1 : 0)) / (double) rows;
                        double wave = Math.sin(s.time() * 5 + f.x * 1.3 - t * 4.5) * 0.12 * t;
                        if (!s.project(f.x + right[0] * len * t, top - 0.05 - hgt * vv + wave, f.z + right[1] * len * t, c)) {
                            ok = false;
                        }
                        q[k][0] = c[0];
                        q[k][1] = c[1];
                    }
                    if (!ok) {
                        continue;
                    }
                    Path2D quad = new Path2D.Double();
                    quad.moveTo(q[0][0], q[0][1]);
                    quad.lineTo(q[1][0], q[1][1]);
                    quad.lineTo(q[2][0], q[2][1]);
                    quad.lineTo(q[3][0], q[3][1]);
                    quad.closePath();
                    g.setColor(((i + j) & 1) == 0 ? Color.WHITE : new Color(20, 20, 24));
                    g.fill(quad);
                }
            }
        }
    }

    // ------------------------------------------------------------------ racing drivers

    @Override
    public void paintPawn(Graphics2D g, Color color, int corner) {
        AssetCache.paintGlossyPawn(g, color);
        double x = AssetCache.PAWN_W / 2.0, ph = AssetCache.PAWN_BODY, ground = AssetCache.PAWN_GROUND;
        double bw = ph * 0.60, hr = ph * 0.21, hy = ground - ph * 0.80, neckY = ground - ph * 0.60;
        // race suit: white stripes and a number on the back
        g.setColor(new Color(255, 255, 255, 235));
        g.fill(new RoundRectangle2D.Double(x - bw * 0.46, ground - ph * 0.46, bw * 0.92, ph * 0.055, 6, 6));
        g.fill(new RoundRectangle2D.Double(x - bw * 0.40, ground - ph * 0.38, bw * 0.80, ph * 0.03, 6, 6));
        g.setColor(new Color(20, 22, 28));
        g.fill(new RoundRectangle2D.Double(x - bw * 0.4, ground - ph * 0.2, bw * 0.8, ph * 0.035, 6, 6));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(x - ph * 0.085, ground - ph * 0.40, ph * 0.17, ph * 0.17));
        g.setColor(new Color(20, 22, 28));
        g.setFont(new Font("Segoe UI", Font.BOLD, (int) (ph * 0.12)));
        FontMetrics fm = g.getFontMetrics();
        String number = String.valueOf((corner + 1) * 7);
        g.drawString(number, (float) (x - fm.stringWidth(number) / 2.0), (float) (ground - ph * 0.40 + ph * 0.125));
        // helmet: white, with a stripe and a dark visor
        double hx = x - hr * 1.08, hyy = hy - hr * 1.1, hw = hr * 2.16, hh = hr * 2.2;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x + hr * 0.4, hy - hr * 0.5), (float) (hr * 2.0),
                new float[]{0f, 0.5f, 1f}, new Color[]{Color.WHITE, new Color(236, 238, 244), new Color(150, 156, 172)}));
        g.fill(new Ellipse2D.Double(hx, hyy, hw, hh));
        g.setColor(color);
        g.fill(new Rectangle2D.Double(x - hr * 0.2, hyy + 2, hr * 0.4, hh * 0.52));
        g.setColor(Mesh3D.mix(color, Color.WHITE, 0.4));
        g.fill(new Rectangle2D.Double(x - hr * 0.05, hyy + 2, hr * 0.1, hh * 0.52));
        g.setPaint(new GradientPaint((float) (x + hr * 0.1), (float) (hy - hr * 0.3), new Color(60, 70, 100), (float) (x + hr * 0.9), (float) (hy + hr * 0.3), new Color(10, 12, 20)));
        Path2D visor = new Path2D.Double();
        visor.moveTo(x + hr * 0.12, hy - hr * 0.4);
        visor.curveTo(x + hr * 0.7, hy - hr * 0.55, x + hr * 1.05, hy - hr * 0.2, x + hr * 1.02, hy + hr * 0.12);
        visor.curveTo(x + hr * 0.7, hy + hr * 0.3, x + hr * 0.3, hy + hr * 0.28, x + hr * 0.12, hy + hr * 0.1);
        visor.closePath();
        g.fill(visor);
        g.setColor(new Color(255, 255, 255, 170));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x + hr * 0.3, hy - hr * 0.3, x + hr * 0.7, hy - hr * 0.38));
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Ellipse2D.Double(hx, hyy, hw, hh));
        // shoulder patches
        g.setColor(new Color(20, 22, 28));
        g.fill(new Ellipse2D.Double(x - bw * 0.42, neckY + ph * 0.06, ph * 0.07, ph * 0.07));
        g.fill(new Ellipse2D.Double(x + bw * 0.42 - ph * 0.07, neckY + ph * 0.06, ph * 0.07, ph * 0.07));
    }

    // ------------------------------------------------------------------ preview

    @Override
    public void paintPreview(Graphics2D g, int w, int h) {
        smooth(g);
        g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[]{0f, 0.5f, 0.52f, 1f},
                new Color[]{new Color(60, 140, 232), new Color(200, 226, 248), new Color(90, 96, 112), new Color(44, 48, 60)}));
        g.fillRect(0, 0, w, h);
        Random random = new Random(9);
        for (int r = 0; r < 3; r++) {
            for (int i = 0; i < w / 7; i++) {
                g.setColor(new Color[]{new Color(240, 60, 60), new Color(250, 220, 60), new Color(70, 140, 250), Color.WHITE}[random.nextInt(4)]);
                g.fill(new Ellipse2D.Double(i * 7 + 2, h * 0.33 + r * 7, 4, 4));
            }
        }
        g.setColor(new Color(224, 40, 52));
        g.fill(new Rectangle2D.Double(0, h * 0.28, w, 4));
        g.setColor(new Color(60, 62, 74));
        g.fill(new Rectangle2D.Double(0, h * 0.62, w, h * 0.38));
        paintRaceCar(g, w * 0.62, h * 0.88, w * 0.44, true, new Color(226, 40, 50));
        miniPawn(g, w * 0.14, h * 0.94, h * 0.45, new Color(250, 250, 250));
    }
}
