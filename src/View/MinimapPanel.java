package View;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Square 2D map with a wooden frame in the top right corner of the 3D board.
 *
 * <p>It paints the real {@link BoardPanel} scaled down, so the pieces always show the current
 * positions. While the mouse button is held down on it, the map grows with a short animation so
 * the squares, special squares and question squares can be read; when the button is released it
 * shrinks back to its place.
 */
class MinimapPanel extends JComponent {

    static final int SIZE = 160;
    private static final int MARGIN = 8;               // room for the shadow and the glow
    private static final double SECONDS_GROW = 0.26;
    private static final double SECONDS_SHRINK = 0.20;

    private final BoardPanel board;
    private final Timer timer = new Timer(16, e -> step());
    private double progress;                            // 0 = small, 1 = big
    private double target;
    private long lastNanos;
    private BufferedImage cached;
    private int cachedSignature;
    private int cachedSize;

    MinimapPanel(BoardPanel board) {
        this.board = board;
        setOpaque(false);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setSize(SIZE, SIZE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Hold the mouse button to enlarge the map");
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                grow(true);
            }
            @Override public void mouseReleased(MouseEvent e) {
                grow(false);
            }
        };
        addMouseListener(mouse);
    }

    /** 0 when the map is small, 1 when it is fully enlarged (used to dim the scene behind it). */
    double zoomProgress() {
        return ease(progress);
    }

    private void grow(boolean on) {
        target = on ? 1 : 0;
        lastNanos = System.nanoTime();
        timer.start();
    }

    private void step() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - lastNanos) / 1e9);
        lastNanos = now;
        if (target > progress) {
            progress = Math.min(target, progress + dt / SECONDS_GROW);
        } else {
            progress = Math.max(target, progress - dt / SECONDS_SHRINK);
        }
        Container parent = getParent();
        if (parent != null) {
            layoutIn(parent.getWidth(), parent.getHeight());
            parent.repaint();
        }
        if (progress == target) {
            timer.stop();
        }
    }

    private static double ease(double p) {
        return p * p * (3 - 2 * p);
    }

    /** Places the map in the top right corner of its parent; its size depends on the animation. */
    void layoutIn(int parentWidth, int parentHeight) {
        int big = (int) Math.max(300, Math.min(Math.min(parentWidth * 0.62, parentHeight * 0.92), 520));
        int size = (int) Math.round(SIZE + (big - SIZE) * ease(progress)) + 2 * MARGIN;
        setBounds(parentWidth - size - 2, 0, size, size);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (board.getWidth() <= 0 || board.getHeight() <= 0) {
            return;
        }
        int size = getWidth();
        int signature = board.stateSignature();
        if (cached == null || signature != cachedSignature || size != cachedSize) {
            cached = render(size);
            cachedSignature = signature;
            cachedSize = size;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        GameScreen.quality(g);
        g.drawImage(cached, 0, 0, null);
        double e = ease(progress);
        if (e > 0.01) {
            // warm glow around the frame while the map is held open
            double f = Math.max(10, size * 0.07);
            RoundRectangle2D frame = new RoundRectangle2D.Double(MARGIN, MARGIN, size - 2 * MARGIN, size - 2 * MARGIN, f * 1.6, f * 1.6);
            for (int i = 0; i < 4; i++) {
                g.setStroke(new BasicStroke(2f + i * 2.4f));
                g.setColor(new Color(255, 214, 120, (int) (70 * e / (i + 1))));
                g.draw(frame);
            }
        }
        g.dispose();
    }

    private BufferedImage render(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        GameScreen.quality(g);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int s = size - 2 * MARGIN;                       // the frame itself
        double x = MARGIN, y = MARGIN;
        double arc = Math.max(14, s * 0.12);
        // shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 6, s, s, arc, arc));
        // wooden frame
        g.setPaint(new GradientPaint((float) x, (float) y, new Color(184, 130, 74),
                (float) (x + s), (float) (y + s), new Color(78, 46, 24)));
        g.fill(new RoundRectangle2D.Double(x, y, s, s, arc, arc));
        g.setColor(new Color(40, 22, 10));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, s, s, arc, arc));
        g.setColor(new Color(255, 214, 140, 120));
        g.draw(new RoundRectangle2D.Double(x + 3, y + 3, s - 6, s - 6, arc - 4, arc - 4));
        // inner window with the real board
        double frameWidth = Math.max(8, s * 0.055);
        double inner = s - 2 * frameWidth;
        double ix = x + frameWidth, iy = y + frameWidth;
        g.setPaint(new GradientPaint((float) ix, (float) iy, new Color(60, 36, 18),
                (float) (ix + inner), (float) (iy + inner), new Color(124, 84, 46)));
        g.fill(new RoundRectangle2D.Double(ix - 2, iy - 2, inner + 4, inner + 4, arc * 0.5, arc * 0.5));
        Graphics2D in = (Graphics2D) g.create();
        in.clip(new RoundRectangle2D.Double(ix, iy, inner, inner, arc * 0.4, arc * 0.4));
        in.setColor(new Color(250, 251, 255));
        in.fillRect((int) ix, (int) iy, (int) Math.ceil(inner), (int) Math.ceil(inner));
        in.translate(ix, iy);
        board.paintScaled(in, (int) Math.round(inner));
        in.dispose();
        // "MAP" plaque
        double pw = Math.max(54, s * 0.34), ph = Math.max(20, s * 0.13);
        double px = x + (s - pw) / 2, py = y - ph * 0.36;
        g.setPaint(new GradientPaint(0, (float) py, new Color(160, 108, 60), 0, (float) (py + ph), new Color(86, 52, 28)));
        g.fill(new RoundRectangle2D.Double(px, py, pw, ph, ph * 0.6, ph * 0.6));
        g.setColor(new Color(40, 22, 10));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new RoundRectangle2D.Double(px, py, pw, ph, ph * 0.6, ph * 0.6));
        g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(12, (int) (ph * 0.62))));
        g.setColor(new Color(255, 236, 200));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("MAP", (float) (px + (pw - fm.stringWidth("MAP")) / 2),
                (float) (py + (ph - fm.getHeight()) / 2 + fm.getAscent()));
        g.dispose();
        return img;
    }
}
