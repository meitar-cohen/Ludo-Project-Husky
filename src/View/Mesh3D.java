package View;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A small collection of lit, flat-shaded polygons (boxes, prisms, pyramids) used to build
 * the castles and trees of the 3D board. Everything is plain geometry, no assets.
 */
final class Mesh3D {

    /** One convex polygon with an outward normal. */
    static final class Poly {
        final double[] x, y, z;
        final double nx, ny, nz;
        final Color color;
        final boolean twoSided;
        /** Windows as {u0, u1, v0, v1} in face coordinates (only for quads: bl, br, tr, tl). */
        List<double[]> windows;
        Color windowColor = new Color(34, 26, 36);
        /** Number of mortar rows (0 = none). For triangles: number of roof tile rows. */
        int brickRows;
        /** Width of the face in world units (used for the brick pattern). */
        double widthWorld;
        /** Arched doors and cloth banners as {u0, u1, v0, v1} in face coordinates. */
        List<double[]> doors;
        List<double[]> banners;
        Color bannerColor;
        /** Tiny decoration (battlement block): not drawn when far away. */
        boolean small;

        final double cx, cy, cz;

        Poly(double[] x, double[] y, double[] z, double nx, double ny, double nz, Color color, boolean twoSided) {
            this.cx = avg(x);
            this.cy = avg(y);
            this.cz = avg(z);
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.color = color;
            this.twoSided = twoSided;
        }

        double centerX() { return cx; }
        double centerY() { return cy; }
        double centerZ() { return cz; }

        private static double avg(double[] v) {
            double s = 0;
            for (double d : v) {
                s += d;
            }
            return s / v.length;
        }
    }

    private final List<Poly> polys = new ArrayList<>();

    List<Poly> polys() {
        return polys;
    }

    /** Axis-aligned box. {@code brick} adds mortar lines, {@code windowsPerFace} dark window quads. */
    void addBox(double x0, double x1, double z0, double z1, double y0, double y1,
                Color color, boolean brick, int windowsPerFace) {
        double h = y1 - y0;
        int rows = brick ? Math.max(2, (int) Math.round(h / 0.32)) : 0;
        // -z (near)
        Poly f = new Poly(new double[]{x0, x1, x1, x0}, new double[]{y0, y0, y1, y1},
                new double[]{z0, z0, z0, z0}, 0, 0, -1, color, false);
        decorate(f, rows, windowsPerFace, x1 - x0);
        polys.add(f);
        // +z
        f = new Poly(new double[]{x1, x0, x0, x1}, new double[]{y0, y0, y1, y1},
                new double[]{z1, z1, z1, z1}, 0, 0, 1, color, false);
        decorate(f, rows, windowsPerFace, x1 - x0);
        polys.add(f);
        // -x
        f = new Poly(new double[]{x0, x0, x0, x0}, new double[]{y0, y0, y1, y1},
                new double[]{z1, z0, z0, z1}, -1, 0, 0, color, false);
        decorate(f, rows, windowsPerFace, z1 - z0);
        polys.add(f);
        // +x
        f = new Poly(new double[]{x1, x1, x1, x1}, new double[]{y0, y0, y1, y1},
                new double[]{z0, z1, z1, z0}, 1, 0, 0, color, false);
        decorate(f, rows, windowsPerFace, z1 - z0);
        polys.add(f);
        // top
        polys.add(new Poly(new double[]{x0, x1, x1, x0}, new double[]{y1, y1, y1, y1},
                new double[]{z0, z0, z1, z1}, 0, 1, 0, color, false));
    }

    /** Row of merlons (battlements) on top of a rectangle. */
    void addBattlements(double x0, double x1, double z0, double z1, double y, double height, Color color) {
        double step = 0.5;
        int nx = Math.max(2, (int) Math.round((x1 - x0) / step));
        int nz = Math.max(2, (int) Math.round((z1 - z0) / step));
        double w = 0.28;
        int from = polys.size();
        for (int i = 0; i < nx; i++) {
            double cx = x0 + (i + 0.5) * (x1 - x0) / nx;
            addBox(cx - w / 2, cx + w / 2, z0, z0 + w, y, y + height, color, false, 0);
            addBox(cx - w / 2, cx + w / 2, z1 - w, z1, y, y + height, color, false, 0);
        }
        for (int j = 1; j < nz - 1; j++) {
            double cz = z0 + (j + 0.5) * (z1 - z0) / nz;
            addBox(x0, x0 + w, cz - w / 2, cz + w / 2, y, y + height, color, false, 0);
            addBox(x1 - w, x1, cz - w / 2, cz + w / 2, y, y + height, color, false, 0);
        }
        for (int i = from; i < polys.size(); i++) {
            polys.get(i).small = true;
        }
    }

    /** Upright prism with a regular polygon as base (a round tower). */
    void addPrism(double cx, double cz, double radius, double y0, double y1, int sides,
                  Color color, boolean brick, int windowFacets) {
        double rows = brick ? Math.max(2, Math.round((y1 - y0) / 0.32)) : 0;
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            double mid = (a0 + a1) / 2;
            Poly p = new Poly(
                    new double[]{cx + radius * Math.cos(a0), cx + radius * Math.cos(a1),
                            cx + radius * Math.cos(a1), cx + radius * Math.cos(a0)},
                    new double[]{y0, y0, y1, y1},
                    new double[]{cz + radius * Math.sin(a0), cz + radius * Math.sin(a1),
                            cz + radius * Math.sin(a1), cz + radius * Math.sin(a0)},
                    Math.cos(mid), 0, Math.sin(mid), color, false);
            p.brickRows = (int) rows;
            p.widthWorld = 2 * radius * Math.sin(Math.PI / sides);
            if (windowFacets > 0 && i % Math.max(1, sides / windowFacets) == 1) {
                p.windows = new ArrayList<>();
                p.windows.add(new double[]{0.32, 0.68, 0.42, 0.78});
            }
            polys.add(p);
        }
        double[] tx = new double[sides], tz = new double[sides], ty = new double[sides];
        for (int i = 0; i < sides; i++) {
            double a = 2 * Math.PI * i / sides;
            tx[i] = cx + radius * Math.cos(a);
            tz[i] = cz + radius * Math.sin(a);
            ty[i] = y1;
        }
        polys.add(new Poly(tx, ty, tz, 0, 1, 0, color, false));
    }

    /** Cone / pyramid roof. */
    void addPyramid(double cx, double cz, double radius, double y0, double height, int sides, Color color) {
        double len = Math.sqrt(height * height + radius * radius);
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            double mid = (a0 + a1) / 2;
            Poly roof = new Poly(
                    new double[]{cx + radius * Math.cos(a0), cx + radius * Math.cos(a1), cx},
                    new double[]{y0, y0, y0 + height},
                    new double[]{cz + radius * Math.sin(a0), cz + radius * Math.sin(a1), cz},
                    height * Math.cos(mid) / len, radius / len, height * Math.sin(mid) / len,
                    color, false);
            roof.brickRows = Math.max(3, (int) Math.round(height / 0.28));
            polys.add(roof);
        }
    }

    /** Adds an arched door to the four side faces of the box that was added last. */
    void addDoorsToLastBox(double u0, double u1, double height) {
        for (int i = polys.size() - 5; i < polys.size() - 1; i++) {
            Poly p = polys.get(i);
            p.doors = new ArrayList<>();
            p.doors.add(new double[]{u0, u1, 0.0, height});
        }
    }

    /** Adds cloth banners to the four side faces of the box that was added last. */
    void addBannersToLastBox(Color color, double... positions) {
        for (int i = polys.size() - 5; i < polys.size() - 1; i++) {
            Poly p = polys.get(i);
            p.bannerColor = color;
            p.banners = new ArrayList<>();
            for (double u : positions) {
                p.banners.add(new double[]{u - 0.035, u + 0.035, 0.12, 0.88});
            }
        }
    }

    private static void decorate(Poly face, int brickRows, int windows, double width) {
        face.widthWorld = width;
        face.brickRows = brickRows;
        if (windows > 0 && width > 1.2) {
            face.windows = new ArrayList<>();
            for (int i = 0; i < windows; i++) {
                double c = (i + 1.0) / (windows + 1.0);
                face.windows.add(new double[]{c - 0.07, c + 0.07, 0.42, 0.78});
            }
        }
    }

    static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
