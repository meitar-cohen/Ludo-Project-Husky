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
        Color doorColor = new Color(112, 72, 42);
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
        /** Glowing surface (lava, neon, lights): drawn without shading. */
        boolean emissive;
        /** Text painted on the face (signs), only for quads. */
        String label;
        Color labelColor = Color.WHITE;

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

    // ------------------------------------------------------------------
    // More shapes for the themed houses
    // ------------------------------------------------------------------

    /** Remembers the current end of the mesh, to change the polygons added after it. */
    int mark() {
        return polys.size();
    }

    /** Marks everything added since {@code mark} as tiny: it is not drawn when far away. */
    void smallSince(int mark) {
        for (int i = mark; i < polys.size(); i++) {
            polys.get(i).small = true;
        }
    }

    /** Makes everything added since {@code mark} glow (no shading). */
    void emissiveSince(int mark) {
        for (int i = mark; i < polys.size(); i++) {
            polys.get(i).emissive = true;
        }
    }

    /** Sets the colour of the window decals of the box that was added last. */
    void windowColorOfLastBox(Color color) {
        for (int i = polys.size() - 5; i < polys.size() - 1; i++) {
            polys.get(i).windowColor = color;
        }
    }

    /** Sets the colour of the doors of the box that was added last. */
    void doorColorOfLastBox(Color color) {
        for (int i = polys.size() - 5; i < polys.size() - 1; i++) {
            polys.get(i).doorColor = color;
        }
    }

    /** Paints a text label on the four side faces of the box that was added last. */
    void addLabelToLastBox(String text, Color color) {
        for (int i = polys.size() - 5; i < polys.size() - 1; i++) {
            polys.get(i).label = text;
            polys.get(i).labelColor = color;
        }
    }

    /** Convex polygon; the normal is computed and turned away from the given inside point. */
    Poly addConvex(double[] x, double[] y, double[] z, Color color, double ix, double iy, double iz) {
        int n = x.length;
        double nx = 0, ny = 0, nz = 0, cx = 0, cy = 0, cz = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            nx += (y[i] - y[j]) * (z[i] + z[j]);
            ny += (z[i] - z[j]) * (x[i] + x[j]);
            nz += (x[i] - x[j]) * (y[i] + y[j]);
            cx += x[i] / n;
            cy += y[i] / n;
            cz += z[i] / n;
        }
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) {
            len = 1;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        if (nx * (cx - ix) + ny * (cy - iy) + nz * (cz - iz) < 0) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }
        Poly p = new Poly(x, y, z, nx, ny, nz, color, false);
        polys.add(p);
        return p;
    }

    /** Box whose top rectangle differs from the bottom one (hoods, cabins, awnings). Axis-aligned rectangles. */
    void addFrustum(double bx0, double bx1, double bz0, double bz1, double by,
                    double tx0, double tx1, double tz0, double tz1, double ty, Color color) {
        double[][] b = {{bx0, bz0}, {bx1, bz0}, {bx1, bz1}, {bx0, bz1}};
        double[][] t = {{tx0, tz0}, {tx1, tz0}, {tx1, tz1}, {tx0, tz1}};
        double ix = (bx0 + bx1 + tx0 + tx1) / 4, iz = (bz0 + bz1 + tz0 + tz1) / 4, iy = (by + ty) / 2;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            addConvex(new double[]{b[i][0], b[j][0], t[j][0], t[i][0]}, new double[]{by, by, ty, ty},
                    new double[]{b[i][1], b[j][1], t[j][1], t[i][1]}, color, ix, iy, iz);
        }
        addConvex(new double[]{t[0][0], t[1][0], t[2][0], t[3][0]}, new double[]{ty, ty, ty, ty},
                new double[]{t[0][1], t[1][1], t[2][1], t[3][1]}, color, ix, iy, iz);
    }

    /** Upright cone with a flat top (volcano, tower with sloping sides). */
    void addRoundFrustum(double cx, double cz, double r0, double r1, double y0, double y1, int sides, Color color) {
        double h = y1 - y0;
        double len = Math.sqrt(h * h + (r0 - r1) * (r0 - r1));
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            double mid = (a0 + a1) / 2;
            polys.add(new Poly(
                    new double[]{cx + r0 * Math.cos(a0), cx + r0 * Math.cos(a1), cx + r1 * Math.cos(a1), cx + r1 * Math.cos(a0)},
                    new double[]{y0, y0, y1, y1},
                    new double[]{cz + r0 * Math.sin(a0), cz + r0 * Math.sin(a1), cz + r1 * Math.sin(a1), cz + r1 * Math.sin(a0)},
                    Math.cos(mid) * h / len, (r0 - r1) / len, Math.sin(mid) * h / len, color, false));
        }
        if (r1 > 0.01) {
            double[] tx = new double[sides], tz = new double[sides], ty = new double[sides];
            for (int i = 0; i < sides; i++) {
                double a = 2 * Math.PI * i / sides;
                tx[i] = cx + r1 * Math.cos(a);
                tz[i] = cz + r1 * Math.sin(a);
                ty[i] = y1;
            }
            polys.add(new Poly(tx, ty, tz, 0, 1, 0, color, false));
        }
    }

    /** Cylinder lying along the x or the z axis (wheels, pipes, rails). */
    void addAxisCylinder(boolean alongX, double cx, double cy, double cz, double radius, double length,
                         int sides, Color color) {
        double h = length / 2;
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides, mid = (a0 + a1) / 2;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            if (alongX) {
                polys.add(new Poly(new double[]{cx - h, cx + h, cx + h, cx - h},
                        new double[]{cy + radius * c0, cy + radius * c0, cy + radius * c1, cy + radius * c1},
                        new double[]{cz + radius * s0, cz + radius * s0, cz + radius * s1, cz + radius * s1},
                        0, Math.cos(mid), Math.sin(mid), color, false));
            } else {
                polys.add(new Poly(new double[]{cx + radius * s0, cx + radius * s0, cx + radius * s1, cx + radius * s1},
                        new double[]{cy + radius * c0, cy + radius * c0, cy + radius * c1, cy + radius * c1},
                        new double[]{cz - h, cz + h, cz + h, cz - h},
                        Math.sin(mid), Math.cos(mid), 0, color, false));
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            double[] px = new double[sides], py = new double[sides], pz = new double[sides];
            for (int i = 0; i < sides; i++) {
                double a = 2 * Math.PI * i / sides;
                if (alongX) {
                    px[i] = cx + side * h;
                    py[i] = cy + radius * Math.cos(a);
                    pz[i] = cz + radius * Math.sin(a);
                } else {
                    px[i] = cx + radius * Math.sin(a);
                    py[i] = cy + radius * Math.cos(a);
                    pz[i] = cz + side * h;
                }
            }
            polys.add(new Poly(px, py, pz, alongX ? side : 0, 0, alongX ? 0 : side, color, false));
        }
    }

    /** Cone roof whose facets alternate between two colours (beach umbrella, awning). */
    void addPyramidStriped(double cx, double cz, double radius, double y0, double height, int sides,
                           Color c1, Color c2) {
        double len = Math.sqrt(height * height + radius * radius);
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            double mid = (a0 + a1) / 2;
            polys.add(new Poly(
                    new double[]{cx + radius * Math.cos(a0), cx + radius * Math.cos(a1), cx},
                    new double[]{y0, y0, y0 + height},
                    new double[]{cz + radius * Math.sin(a0), cz + radius * Math.sin(a1), cz},
                    height * Math.cos(mid) / len, radius / len, height * Math.sin(mid) / len,
                    (i & 1) == 0 ? c1 : c2, false));
        }
    }

    static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
