package View;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * STRATEGY pattern: one "world" of the 3D version of the game.
 *
 * <p>{@link Board3DPanel} (the context) knows the board, the pieces and the camera, but it does
 * not know how a world looks. Everything that makes the worlds different (sky, island, the
 * houses of the players, the props, the look of the pieces) is behind this interface. Each
 * world is a separate strategy class ({@link SkyTheme}, {@link VolcanoTheme}, {@link PoolTheme},
 * {@link SalonTheme}, {@link RacingTheme}), and {@link Themes} creates them. A new world is
 * added by writing one new class, without changing the board panel.
 */
interface WorldTheme {

    /** Short technical name, for example {@code "sky"}. */
    String id();

    /** Name shown on the choice screen. */
    String title();

    /** One line that describes the world on the choice screen. */
    String tagline();

    /** Main colour of the world, used for the card on the choice screen. */
    Color accent();

    /** Small picture of the world for the choice screen. */
    void paintPreview(Graphics2D g, int width, int height);

    // ------------------------------------------------------------------ the world around the board

    /** Paints everything behind the island: sky, sun, clouds, mountains, grandstands... */
    void paintBackdrop(Backdrop backdrop);

    /** Colours of the island and of the board frame. */
    IslandStyle island();

    /** Brightness of the unlit side of the objects (0..1). */
    double ambientLight();

    /** How much the sun adds to the faces that look at it (0..1). */
    double directLight();

    /** Colour that far away objects fade into. */
    Color fogColor();

    /** Places trees, rocks, tyres... around the board. */
    void createProps(PropPlanner planner);

    /** Draws one prop as a picture standing in the world. */
    void paintProp(PropView view);

    // ------------------------------------------------------------------ the houses of the players

    /** Builds the house of one player (castle, volcano, pool, salon, car) out of simple shapes. */
    void buildHouse(HouseBuilder house);

    /** Where the {@code slot}-th waiting piece of a player stands, in the local coordinates of the house. */
    double[] padLocal(int slot);

    /** Paints the flat ground of the player's block (floor, water, lava, asphalt) and the standing places. */
    void paintHouseFloor(HouseFloor floor);

    /** Paints the moving parts every frame: flags, lava, water sparkles, smoke... */
    void paintAnimated(Animated scene);

    // ------------------------------------------------------------------ the pieces

    /** Paints one piece on the sprite canvas (see {@link AssetCache#PAWN_W} and friends). */
    void paintPawn(Graphics2D g, Color color, int corner);

    /** How high a waiting piece is lifted (for example onto a sunbed). */
    double restLift();

    /** How far a waiting piece is tilted (radians), for example when it lies on a sunbed. */
    double restTilt();

    /** How the kick animation of a capture looks in this world (Strategy: the world decides). */
    KickStyle kickStyle();

    // ======================================================================================
    // Small helper types the strategies work with
    // ======================================================================================

    /** What a strategy gets to paint the background. */
    final class Backdrop {
        final Graphics2D g;
        final int width, height;
        final double skyShift, heading, focal, centerX, time;
        final AssetCache cache;
        /** 0 = full detail, 1 and 2 = reduced (slow computer). */
        final int quality;

        Backdrop(Graphics2D g, int width, int height, double skyShift, double heading, double focal,
                 double centerX, double time, AssetCache cache, int quality) {
            this.quality = quality;
            this.g = g;
            this.width = width;
            this.height = height;
            this.skyShift = skyShift;
            this.heading = heading;
            this.focal = focal;
            this.centerX = centerX;
            this.time = time;
            this.cache = cache;
        }

        /** Screen height of the horizon. */
        double horizonY() {
            return height * AssetCache.HORIZON + skyShift;
        }

        /** Screen x of something that lies in the given direction of the sky, NaN if it is not in view. */
        double screenX(double azimuth) {
            double da = Math.atan2(Math.sin(azimuth - heading), Math.cos(azimuth - heading));
            return Math.abs(da) > 1.3 ? Double.NaN : centerX + focal * Math.tan(da);
        }

        /** Vertical gradient over the whole panel; the horizon is at {@link AssetCache#HORIZON}. */
        void gradient(String key, float[] fractions, Color[] colors) {
            BufferedImage image = cache.image(key, width, height, (ig, w, h) -> {
                ig.setPaint(new java.awt.LinearGradientPaint(0, 0, 0, h, fractions, colors));
                ig.fillRect(0, 0, w, h);
            });
            g.setColor(colors[0]);
            g.fillRect(0, 0, width, height);
            if (skyShift < 0) {
                g.setColor(colors[colors.length - 1]);
                g.fillRect(0, (int) (height + skyShift) - 1, width, (int) (-skyShift) + 2);
            }
            g.drawImage(image, 0, (int) Math.round(skyShift), null);
        }

        /** Draws a picture centred at (cx, cy) with the given width. */
        void sprite(BufferedImage image, double cx, double cy, double spriteWidth, float alpha) {
            double ratio = image.getHeight() / (double) image.getWidth();
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
            g.drawImage(image, (int) (cx - spriteWidth / 2), (int) (cy - spriteWidth * ratio / 2),
                    (int) spriteWidth, (int) (spriteWidth * ratio), null);
            g.setComposite(java.awt.AlphaComposite.SrcOver);
        }
    }

    /** Colours of the island, the board frame and the plain squares. */
    final class IslandStyle {
        Color topNear = new Color(150, 212, 104);
        Color topFar = new Color(90, 170, 74);
        Color rim = new Color(70, 52, 36, 200);
        Color[] soil = {new Color(158, 142, 120), new Color(124, 90, 60), new Color(96, 76, 60)};
        Color patchLight = new Color(150, 214, 100, 110);
        Color patchDark = new Color(50, 130, 56, 80);
        /** Flowers on the grass; {@code null} = none. */
        Color[] petals = {Color.WHITE, new Color(255, 224, 80), new Color(255, 150, 190), new Color(190, 170, 255)};
        Color frame = new Color(120, 88, 64);
        Color trim = new Color(222, 184, 100);
        Color boardBase = new Color(160, 148, 134);
        Color tileLight = new Color(238, 234, 226);
        Color tileDark = new Color(224, 219, 209);
        /** Two colours for kerb stones along the edge of the island; {@code null} = none. */
        Color[] kerbs;
    }

    /** One prop (tree, rock, tyre stack...) standing on the island. */
    final class Prop {
        final String kind;
        final double x, y, z, height, seed;
        final Color color;

        Prop(String kind, double x, double y, double z, double height, Color color, double seed) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.height = height;
            this.color = color;
            this.seed = seed;
        }
    }

    /** What a strategy gets to paint one prop. {@code ph} is the screen height of the prop in pixels. */
    final class PropView {
        Graphics2D g;
        Prop prop;
        double x, ground, ph, fog, time;
        int quality;            // 0 = full detail, 1 and 2 = reduced
        Color fogColor;
    }

    /** Lets a strategy place props. */
    interface PropPlanner {
        Random random();

        /** A random spot on the island outside the board frame: {x, z}. */
        double[] marginPoint(double fromFrame, double fromEdge);

        void add(String kind, double x, double z, double height, Color color);

        /** A soft shadow on the ground next to a prop. */
        void shadow(double x, double z, double radius, double height);
    }

    /**
     * Builds the house of one player in the local coordinates of the player's block: {@code u} and
     * {@code v} are distances in world units from the outer corner of the block along its two
     * edges, {@code h} is the height above the board. The block is 12 x 12 units.
     */
    interface HouseBuilder {
        int corner();

        /** Colour of the player. */
        Color color();

        Mesh3D mesh();

        /** World position {x, z} of a local point. */
        double[] point(double u, double v);

        /** Height of the board surface in world units. */
        double baseY();

        void box(double u0, double u1, double v0, double v1, double h0, double h1, Color c, boolean brick, int windows);

        void battlements(double u0, double u1, double v0, double v1, double h, double height, Color c);

        void prism(double u, double v, double r, double h0, double h1, int sides, Color c, boolean brick, int windowFacets);

        void pyramid(double u, double v, double r, double h0, double height, int sides, Color c);

        void pyramidStriped(double u, double v, double r, double h0, double height, int sides, Color c1, Color c2);

        void roundFrustum(double u, double v, double r0, double r1, double h0, double h1, int sides, Color c);

        /** Box with a different top rectangle (all in local coordinates). */
        void frustum(double bu0, double bu1, double bv0, double bv1, double h0,
                     double tu0, double tu1, double tv0, double tv1, double h1, Color c);

        /** Cylinder lying along the u axis ({@code alongU}) or the v axis. */
        void cylinder(boolean alongU, double u, double v, double h, double radius, double length, int sides, Color c);

        /** Adds arched doors to the box that was added last. */
        void doors(double u0, double u1, double height);

        /** Adds cloth banners to the box that was added last. */
        void banners(Color c, double... positions);

        /** Colour of the windows of the box that was added last. */
        void windowColor(Color c);

        /** Colour of the doors of the box that was added last. */
        void doorColor(Color c);

        /** Paints text on the sides of the box that was added last. */
        void label(String text, Color c);

        /** Starts a group of shapes that glow. */
        int mark();

        void glowSince(int mark);

        /** Marks the shapes added since {@code mark} as tiny details that are skipped when far away. */
        void tinySince(int mark);

        /** Shadow of a rectangular part. */
        void shadowRect(double u0, double u1, double v0, double v1, double h0, double h1);

        /** Shadow of a round part; {@code topScale} is the size at the top relative to the bottom. */
        void shadowCircle(double u, double v, double r, double h0, double h1, double topScale);

        /** Registers something that is animated every frame (flag, lava vent...). */
        void decor(String kind, double u, double v, double h, double size);

        /** Registers a prop (lifeguard, palm, bottle...) standing at a local point, {@code h} above the board. */
        void prop(String kind, double u, double v, double h, double height, Color c);
    }

    /** What a strategy gets to paint the flat ground of one block. */
    interface HouseFloor {
        Graphics2D g();

        int corner();

        Color color();

        double time();

        /** Flat rectangle in local coordinates. */
        void quad(double u0, double u1, double v0, double v1, Color c);

        void disc(double u, double v, double r, Color c);

        void ring(double u, double v, double r, Color c, float width);

        void line(double u0, double v0, double u1, double v1, Color c, float width);

        /** Many lines at once: {@code segments} = u0, v0, u1, v1, u0, v0, ... (much cheaper than single lines). */
        void lines(double[] segments, Color c, float width);

        /** False when the block is far away or the computer is slow: skip small decorations. */
        boolean detailed();
    }

    /** What a strategy gets to paint the moving parts. */
    interface Animated {
        Graphics2D g();

        double time();

        double focal();

        /** Horizontal world direction that points to the right of the screen. */
        double[] right();

        /** Projects a world point; {@code out} = {screenX, screenY, depth}; false if not visible. */
        boolean project(double x, double y, double z, double[] out);

        List<Decor> decors();
    }

    /** A registered animated part of a house. */
    final class Decor {
        final String kind;
        final int corner;
        final Color color;
        final double x, y, z, size;

        Decor(String kind, int corner, Color color, double x, double y, double z, double size) {
            this.kind = kind;
            this.corner = corner;
            this.color = color;
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
        }
    }
}
