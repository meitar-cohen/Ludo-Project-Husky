package View;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Flyweight cache for everything in the 3D view that never changes between frames: the sky,
 * the cloud sprites, the glossy pawn sprites (one per colour) and the pre-rendered island
 * scene for each camera direction. The painting code only has to draw these images, so a frame
 * stays cheap even on integrated graphics.
 */
final class AssetCache {

    /** Sprite geometry: the pawn stands on {@code PAWN_GROUND} and is {@code PAWN_BODY} px tall. */
    static final int PAWN_W = 160, PAWN_H = 256, PAWN_GROUND = 240, PAWN_BODY = 224;
    static final double HORIZON = 0.37;               // horizon as a fraction of the height

    private BufferedImage sky, sun, islet;
    private final BufferedImage[] clouds = new BufferedImage[6];
    private final Map<Integer, BufferedImage> pawns = new HashMap<>();
    private final Map<String, BufferedImage> images = new HashMap<>();

    // ------------------------------------------------------------------
    // Sky (gradient only; sun, clouds and rocks are sprites that move with the camera heading)
    // ------------------------------------------------------------------

    BufferedImage sky(int w, int h) {
        if (sky != null && sky.getWidth() == w && sky.getHeight() == h) {
            return sky;
        }
        sky = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sky.createGraphics();
        quality(g);
        // deep azure at the top, warm sunlit haze at the horizon, soft blue below the island
        g.setPaint(new LinearGradientPaint(0, 0, 0, h,
                new float[]{0f, (float) (HORIZON * 0.55), (float) HORIZON, 0.62f, 1f},
                new Color[]{new Color(18, 76, 192), new Color(60, 140, 232), new Color(252, 244, 226),
                        new Color(150, 200, 246), new Color(96, 156, 232)}));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return sky;
    }

    /** Soft glowing sun, 600 x 600. */
    BufferedImage sun() {
        if (sun != null) {
            return sun;
        }
        int size = 600;
        sun = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sun.createGraphics();
        quality(g);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(size / 2.0, size / 2.0), size / 2f,
                new float[]{0f, 0.10f, 0.32f, 1f},
                new Color[]{new Color(255, 255, 240, 255), new Color(255, 246, 205, 200),
                        new Color(255, 232, 170, 80), new Color(255, 232, 170, 0)}));
        g.fill(new Ellipse2D.Double(0, 0, size, size));
        g.dispose();
        return sun;
    }

    /** A small far-away floating rock, 200 x 200. */
    BufferedImage islet() {
        if (islet != null) {
            return islet;
        }
        int size = 200;
        islet = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = islet.createGraphics();
        quality(g);
        Color haze = new Color(190, 214, 242);
        double x = size / 2.0, y = 70, r = 80;
        Path2D rock = new Path2D.Double();
        rock.moveTo(x - r, y);
        rock.lineTo(x + r, y);
        rock.lineTo(x + r * 0.45, y + r * 0.9);
        rock.lineTo(x + r * 0.05, y + r * 1.5);
        rock.lineTo(x - r * 0.4, y + r * 0.8);
        rock.closePath();
        g.setColor(blend(new Color(104, 88, 76), haze, 0.3));
        g.fill(rock);
        g.setColor(blend(new Color(120, 190, 100), haze, 0.25));
        g.fill(new Ellipse2D.Double(x - r, y - r * 0.28, r * 2, r * 0.56));
        g.dispose();
        return islet;
    }

    // ------------------------------------------------------------------
    // Cloud sprites
    // ------------------------------------------------------------------

    BufferedImage cloud(int index) {
        index = Math.floorMod(index, clouds.length);
        if (clouds[index] != null) {
            return clouds[index];
        }
        int w = 440, h = 210;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        quality(g);
        Random random = new Random(100 + index * 31);
        int puffs = 7 + random.nextInt(4);
        double[][] p = new double[puffs][3];
        for (int i = 0; i < puffs; i++) {
            double t = puffs == 1 ? 0.5 : i / (double) (puffs - 1);
            p[i][0] = 70 + t * (w - 140) + random.nextDouble() * 20 - 10;
            p[i][2] = 44 + 38 * Math.sin(Math.PI * t) + random.nextDouble() * 16;
            p[i][1] = h * 0.62 - p[i][2] * 0.55 + random.nextDouble() * 10;
        }
        // shaded underside first, bright top second
        for (int pass = 0; pass < 2; pass++) {
            for (double[] c : p) {
                double r = c[2];
                double cy = c[1] + (pass == 0 ? r * 0.22 : 0);
                Color core = pass == 0 ? new Color(170, 192, 228, 230) : new Color(255, 255, 255, 245);
                Color mid = pass == 0 ? new Color(176, 198, 232, 150) : new Color(250, 252, 255, 190);
                g.setPaint(new RadialGradientPaint(new Point2D.Double(c[0] - r * 0.15, cy - r * 0.2), (float) r,
                        new float[]{0f, 0.6f, 1f}, new Color[]{core, mid, new Color(255, 255, 255, 0)}));
                g.fill(new Ellipse2D.Double(c[0] - r, cy - r, r * 2, r * 2));
            }
        }
        g.dispose();
        clouds[index] = img;
        return img;
    }

    // ------------------------------------------------------------------
    // Pawn sprites
    // ------------------------------------------------------------------

    /** Half-size copy of the pawn sprite, used when the pawn is small on screen (avoids jagged edges). */
    BufferedImage pawnHalf(WorldTheme theme, Color color, int corner) {
        Integer key = (theme.id().hashCode() * 31 + color.getRGB()) ^ 0x1000000;
        BufferedImage cached = pawns.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage big = pawn(theme, color, corner);
        BufferedImage img = new BufferedImage(PAWN_W / 2, PAWN_H / 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(big, 0, 0, PAWN_W / 2, PAWN_H / 2, null);
        g.dispose();
        pawns.put(key, img);
        return img;
    }

    /** The piece of the given world and colour (painted once by the world's strategy, then reused). */
    BufferedImage pawn(WorldTheme theme, Color color, int corner) {
        Integer key = theme.id().hashCode() * 31 + color.getRGB();
        BufferedImage cached = pawns.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage img = new BufferedImage(PAWN_W, PAWN_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        quality(g);
        theme.paintPawn(g, color, corner);
        g.dispose();
        pawns.put(key, img);
        return img;
    }

    /** The standard glossy pawn; worlds call this and add their own details around it. */
    static void paintGlossyPawn(Graphics2D g, Color color) {
        double x = PAWN_W / 2.0, ground = PAWN_GROUND, ph = PAWN_BODY;
        double bw = ph * 0.60, neck = ph * 0.20, neckY = ground - ph * 0.60;
        double hr = ph * 0.21, hy = ground - ph * 0.80;
        Color dark = blend(color, Color.BLACK, 0.50), deep = blend(color, Color.BLACK, 0.30);
        Color light = blend(color, Color.WHITE, 0.45), glow = blend(color, Color.WHITE, 0.72);
        // base disc
        g.setPaint(new GradientPaint((float) x, (float) (ground - ph * 0.09), blend(color, Color.BLACK, 0.12),
                (float) x, (float) (ground + ph * 0.05), dark));
        g.fill(new Ellipse2D.Double(x - bw * 0.62, ground - ph * 0.09, bw * 1.24, ph * 0.18));
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke((float) (ph * 0.012)));
        g.draw(new java.awt.geom.Arc2D.Double(x - bw * 0.62, ground - ph * 0.09, bw * 1.24, ph * 0.18, 20, 140, java.awt.geom.Arc2D.OPEN));
        // body
        Path2D body = new Path2D.Double();
        body.moveTo(x - bw / 2, ground - ph * 0.03);
        body.curveTo(x - bw / 2, ground - ph * 0.22, x - neck / 2 - ph * 0.05, ground - ph * 0.38, x - neck / 2, neckY);
        body.lineTo(x + neck / 2, neckY);
        body.curveTo(x + neck / 2 + ph * 0.05, ground - ph * 0.38, x + bw / 2, ground - ph * 0.22, x + bw / 2, ground - ph * 0.03);
        body.closePath();
        // sun comes from the top right: dark on the left, bright on the right
        g.setPaint(new LinearGradientPaint((float) (x - bw / 2), 0f, (float) (x + bw / 2), 0f,
                new float[]{0f, 0.25f, 0.62f, 0.88f, 1f},
                new Color[]{dark, deep, color, light, blend(color, Color.WHITE, 0.2)}));
        g.fill(body);
        // ambient occlusion towards the foot
        g.setPaint(new GradientPaint(0f, (float) (neckY + ph * 0.1), new Color(0, 0, 0, 0),
                0f, (float) (ground - ph * 0.03), new Color(0, 0, 0, 80)));
        g.fill(body);
        g.setColor(new Color(0, 0, 0, 80));
        g.setStroke(new BasicStroke((float) Math.max(1.2, ph * 0.014)));
        g.draw(body);
        // specular streak on the sunlit side
        Path2D streak = new Path2D.Double();
        streak.moveTo(x + bw * 0.14, ground - ph * 0.12);
        streak.curveTo(x + bw * 0.16, ground - ph * 0.28, x + neck * 0.35, ground - ph * 0.40, x + neck * 0.28, neckY + ph * 0.05);
        streak.curveTo(x + neck * 0.05, ground - ph * 0.40, x + bw * 0.02, ground - ph * 0.27, x + bw * 0.02, ground - ph * 0.12);
        streak.closePath();
        g.setPaint(new LinearGradientPaint(0f, (float) (neckY), 0f, (float) (ground - ph * 0.1),
                new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 170), new Color(255, 255, 255, 40)}));
        g.fill(streak);
        // rim light on the right edge
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke((float) Math.max(1.2, ph * 0.016), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D rim = new Path2D.Double();
        rim.moveTo(x + bw / 2 - ph * 0.005, ground - ph * 0.05);
        rim.curveTo(x + bw / 2, ground - ph * 0.22, x + neck / 2 + ph * 0.05, ground - ph * 0.38, x + neck / 2, neckY + ph * 0.02);
        g.draw(rim);
        // lower band
        g.setColor(new Color(255, 255, 255, 105));
        g.fill(new Ellipse2D.Double(x - bw * 0.46, ground - ph * 0.17, bw * 0.92, ph * 0.055));
        // collar
        g.setPaint(new LinearGradientPaint((float) (x - bw * 0.3), 0f, (float) (x + bw * 0.3), 0f,
                new float[]{0f, 0.6f, 1f}, new Color[]{deep, light, glow}));
        g.fill(new Ellipse2D.Double(x - bw * 0.30, neckY - ph * 0.045, bw * 0.60, ph * 0.10));
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke((float) Math.max(1, ph * 0.010)));
        g.draw(new Ellipse2D.Double(x - bw * 0.30, neckY - ph * 0.045, bw * 0.60, ph * 0.10));
        // head: multi-stop radial gradient, highlight towards the sun
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x + hr * 0.38, hy - hr * 0.42), (float) (hr * 1.75),
                new float[]{0f, 0.18f, 0.5f, 0.82f, 1f},
                new Color[]{Color.WHITE, glow, color, deep, dark}));
        g.fill(new Ellipse2D.Double(x - hr, hy - hr, hr * 2, hr * 2));
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke((float) Math.max(1.2, ph * 0.013)));
        g.draw(new Ellipse2D.Double(x - hr, hy - hr, hr * 2, hr * 2));
        // specular highlight
        g.setColor(new Color(255, 255, 255, 215));
        g.fill(new Ellipse2D.Double(x + hr * 0.10, hy - hr * 0.78, hr * 0.62, hr * 0.42));
        g.setColor(new Color(255, 255, 255, 255));
        g.fill(new Ellipse2D.Double(x + hr * 0.30, hy - hr * 0.66, hr * 0.22, hr * 0.14));
        // rim light on the head
        g.setColor(new Color(255, 255, 255, 90));
        g.setStroke(new BasicStroke((float) Math.max(1.2, ph * 0.014)));
        g.draw(new java.awt.geom.Arc2D.Double(x - hr * 0.94, hy - hr * 0.94, hr * 1.88, hr * 1.88, -60, 70, java.awt.geom.Arc2D.OPEN));
    }

    // ------------------------------------------------------------------
    // Generic cached pictures (backdrops, panoramas, signs)
    // ------------------------------------------------------------------

    /** Paints into a picture of the given size. */
    interface ImagePainter {
        void paint(Graphics2D g, int width, int height);
    }

    /** A picture that is painted once for every key and size, then reused. */
    BufferedImage image(String key, int width, int height, ImagePainter painter) {
        String full = key + "@" + width + "x" + height;
        BufferedImage cached = images.get(full);
        if (cached != null) {
            return cached;
        }
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        quality(g);
        painter.paint(g, img.getWidth(), img.getHeight());
        g.dispose();
        images.put(full, img);
        return img;
    }

    // ------------------------------------------------------------------

    static Color blend(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}
