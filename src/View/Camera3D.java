package View;

/**
 * Small perspective camera for the 3D board (no external 3D library).
 *
 * <p>World space: x = right, y = up, z = "up the grid" (away from the bottom edge of the flat
 * board). The camera stands at {@code (posX, posY, posZ)}, looks along the horizontal direction
 * given by {@code heading} (0 = along +z, growing clockwise seen from above) and is tilted down
 * by {@code pitch}. Polygons are clipped against the near plane, so nothing pops away when the
 * camera flies close to a wall.
 */
final class Camera3D {

    static final double NEAR = 0.6;

    double posX, posY = 3, posZ;
    double heading;                 // radians
    double pitch = Math.toRadians(13);
    double focal = 700;             // pixels
    double centerX, centerY;        // screen position of the point the camera looks at

    /** Output of {@link #projectPolygon}: screen x, y and depth of the clipped polygon. */
    final double[] sx = new double[40], sy = new double[40], sd = new double[40];

    private double rightX = 1, rightZ, fwdX, fwdZ = 1, cosP = 1, sinP;

    /** Call after changing any field. */
    void update() {
        rightX = Math.cos(heading);
        rightZ = -Math.sin(heading);
        fwdX = Math.sin(heading);
        fwdZ = Math.cos(heading);
        cosP = Math.cos(pitch);
        sinP = Math.sin(pitch);
    }

    /**
     * Projects a world point. {@code out} receives {screenX, screenY, depth}.
     *
     * @return false if the point is too close to (or behind) the camera
     */
    boolean project(double wx, double wy, double wz, double[] out) {
        double dx = wx - posX, dz = wz - posZ, ry = wy - posY;
        double rx = dx * rightX + dz * rightZ;
        double rz = dx * fwdX + dz * fwdZ;
        double depth = -ry * sinP + rz * cosP;
        if (depth < NEAR) {
            return false;
        }
        double up = ry * cosP + rz * sinP;
        out[0] = centerX + focal * rx / depth;
        out[1] = centerY - focal * up / depth;
        out[2] = depth;
        return true;
    }

    /** Depth of a point along the viewing direction (can be negative). */
    double depthOf(double wx, double wy, double wz) {
        double dx = wx - posX, dz = wz - posZ, ry = wy - posY;
        double rz = dx * fwdX + dz * fwdZ;
        return -ry * sinP + rz * cosP;
    }

    /**
     * Projects a polygon, clipping it against the near plane. The result is stored in
     * {@link #sx}, {@link #sy}, {@link #sd}.
     *
     * @return number of vertices (0 if nothing is visible)
     */
    int projectPolygon(double[] xs, double[] ys, double[] zs, int n) {
        double[] cx = tmp[0], cy = tmp[1], cd = tmp[2];
        for (int i = 0; i < n; i++) {
            double dx = xs[i] - posX, dz = zs[i] - posZ, ry = ys[i] - posY;
            double rx = dx * rightX + dz * rightZ;
            double rz = dx * fwdX + dz * fwdZ;
            cx[i] = rx;
            cd[i] = -ry * sinP + rz * cosP;
            cy[i] = ry * cosP + rz * sinP;
        }
        int count = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            boolean inI = cd[i] >= NEAR, inJ = cd[j] >= NEAR;
            if (inI) {
                put(count++, cx[i], cy[i], cd[i]);
            }
            if (inI != inJ) {
                double t = (NEAR - cd[i]) / (cd[j] - cd[i]);
                put(count++, cx[i] + (cx[j] - cx[i]) * t, cy[i] + (cy[j] - cy[i]) * t, NEAR);
            }
        }
        return count;
    }

    private final double[][] tmp = new double[3][40];

    private void put(int k, double rx, double up, double depth) {
        sx[k] = centerX + focal * rx / depth;
        sy[k] = centerY - focal * up / depth;
        sd[k] = depth;
    }

    /** Horizontal world direction that appears as "screen right". */
    double[] rightOnGround() {
        return new double[]{rightX, rightZ};
    }

    /** Horizontal world direction that appears as "away from the viewer". */
    double[] forwardOnGround() {
        return new double[]{fwdX, fwdZ};
    }

    /** Converts a direction given in view space (x right, z forward) to world space. */
    double[] viewToWorld(double vx, double vz) {
        return new double[]{vx * rightX + vz * fwdX, vx * rightZ + vz * fwdZ};
    }
}
