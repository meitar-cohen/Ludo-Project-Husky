package View;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/** Renders pieces and the finished-piece counters for the board. */
public class PiecePanel extends JPanel {

    private static final Color HIGHLIGHT = new Color(255, 171, 56);

    public PiecePanel() {
        setOpaque(false);
    }

    public void drawFinishedPieceCounters(
            Graphics2D g2, int cs, int ox, int oy,
            int[] finishedCounts, Color[] cornerColors) {
        int[] cols = {6, 8, 8, 6};
        int[] rows = {6, 6, 8, 8};
        for (int corner = 0; corner < finishedCounts.length; corner++) {
            int count = finishedCounts[corner];
            if (count == 0) {
                continue;
            }
            int x = ox + (int) ((cols[corner] + 0.5) * cs);
            int y = oy + (int) ((rows[corner] + 0.5) * cs);
            int diameter = Math.max(15, cs / 2);
            g2.setColor(cornerColors[corner]);
            g2.fillOval(
                    x - diameter / 2,
                    y - diameter / 2,
                    diameter,
                    diameter
            );
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(
                    "Dialog", Font.BOLD,
                    Math.max(10, diameter / 2)
            ));
            String text = String.valueOf(count);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(
                    text,
                    x - fm.stringWidth(text) / 2,
                    y + (fm.getAscent() - fm.getDescent()) / 2
            );
        }
    }

    public void drawPiece(
            Graphics2D g2, double cx, double cy, int cs,
            Color color, boolean highlighted, boolean pulsePhase) {
        double r = cs * 0.42;
        if (highlighted) {
            int glowAlpha = pulsePhase ? 180 : 75;
            double glowSize = pulsePhase ? 0.28 : 0.21;
            g2.setColor(new Color(255, 178, 67, glowAlpha));
            g2.fill(new Ellipse2D.Double(
                    cx - r - cs * glowSize,
                    cy - r - cs * glowSize,
                    2 * (r + cs * glowSize),
                    2 * (r + cs * glowSize)
            ));
            g2.setColor(HIGHLIGHT);
            g2.setStroke(new BasicStroke(Math.max(2f, cs * 0.10f)));
            g2.draw(new Ellipse2D.Double(
                    cx - r - cs * 0.12,
                    cy - r - cs * 0.12,
                    2 * (r + cs * 0.12),
                    2 * (r + cs * 0.12)
            ));
        }
        g2.setColor(new Color(25, 32, 69, 55));
        g2.fill(new Ellipse2D.Double(
                cx - r + cs * 0.055,
                cy - r + cs * 0.10,
                r * 2,
                r * 2
        ));
        g2.setPaint(new GradientPaint(
                (float) (cx - r), (float) (cy - r), lighter(color),
                (float) (cx + r), (float) (cy + r), color
        ));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g2.setColor(new Color(255, 255, 255, 185));
        g2.fill(new Ellipse2D.Double(
                cx - r * 0.53,
                cy - r * 0.62,
                r * 0.76,
                r * 0.76
        ));
    }

    private static Color lighter(Color color) {
        return new Color(
                (color.getRed() + 255) / 2,
                (color.getGreen() + 255) / 2,
                (color.getBlue() + 255) / 2
        );
    }
}
