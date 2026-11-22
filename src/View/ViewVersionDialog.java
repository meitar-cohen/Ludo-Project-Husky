package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Small modal window shown after "Continue" on the game setup screen.
 * The player chooses between the classic 2D board and the 3D prototype (2.5D action view).
 */
public class ViewVersionDialog extends JDialog {

    private static final Color NAVY = new Color(12, 20, 49);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 123, 151);

    private boolean threeD;
    private boolean confirmed;
    private final VersionCard card2D;
    private final VersionCard card3D;

    private ViewVersionDialog(Window owner) {
        super(owner, "Ludo - Choose Version", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        MainMenuScreen.applyAppIcon(this);

        JPanel background = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                GameScreen.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(14, 22, 54),
                        getWidth(), getHeight(), new Color(43, 39, 100)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(110, 103, 231, 45));
                g.fillOval(-120, -150, 420, 420);
                g.setColor(new Color(72, 177, 246, 24));
                g.fillOval(400, 200, 380, 380);
                g.dispose();
            }
        };

        JPanel panel = new RoundedCard();
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(22, 26, 18, 26));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("STEP 03  /  GAME VERSION");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        eyebrow.setForeground(BLUE);
        JLabel title = new JLabel("Which Ludo do you want?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("The rules are identical. Only the look of the board changes.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        header.add(eyebrow);
        header.add(Box.createVerticalStrut(6));
        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(subtitle);
        panel.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));
        cards.setOpaque(false);
        card2D = new VersionCard("2D LUDO", "Classic top-down board", null, false);
        card3D = new VersionCard("3D LUDO", "Whole game on a 2.5D board", "PROTOTYPE", true);
        card2D.addActionListener(e -> select(false));
        card3D.addActionListener(e -> select(true));
        card2D.getAccessibleContext().setAccessibleName("2D Ludo");
        card3D.getAccessibleContext().setAccessibleName("3D Ludo prototype");
        cards.add(card2D);
        cards.add(card3D);
        panel.add(cards, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(0, 46));
        JButton cancel = new PillButton("Back", false);
        cancel.addActionListener(e -> dispose());
        JButton ok = new PillButton("Start Game", true);
        ok.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        actions.add(cancel);
        actions.add(ok);
        panel.add(actions, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(560, 380));
        background.add(panel);
        background.setPreferredSize(new Dimension(620, 440));
        setContentPane(background);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
        select(false);
    }

    private void select(boolean threeD) {
        this.threeD = threeD;
        card2D.setChosen(!threeD);
        card3D.setChosen(threeD);
    }

    /**
     * Shows the dialog.
     *
     * @return {@code Boolean.TRUE} for 3D, {@code Boolean.FALSE} for 2D, {@code null} if the
     *         player went back
     */
    public static Boolean choose(Window owner) {
        ViewVersionDialog dialog = new ViewVersionDialog(owner);
        dialog.setVisible(true);
        return dialog.confirmed ? Boolean.valueOf(dialog.threeD) : null;
    }

    // ------------------------------------------------------------------
    // Components
    // ------------------------------------------------------------------

    private static final class RoundedCard extends JPanel {
        RoundedCard() {
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            GameScreen.quality(g);
            g.setColor(new Color(6, 11, 31, 55));
            g.fill(new RoundRectangle2D.Double(5, 9, getWidth() - 10, getHeight() - 14, 34, 34));
            g.setColor(new Color(250, 251, 255));
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 6, getHeight() - 10, 34, 34));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class VersionCard extends JButton {
        private final String label;
        private final String caption;
        private final String badge;
        private final boolean threeD;
        private boolean chosen;
        private boolean hovered;

        VersionCard(String label, String caption, String badge, boolean threeD) {
            this.label = label;
            this.caption = caption;
            this.badge = badge;
            this.threeD = threeD;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        void setChosen(boolean chosen) {
            this.chosen = chosen;
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            GameScreen.quality(g);
            int w = getWidth(), h = getHeight();
            g.setColor(chosen ? new Color(234, 239, 255) : hovered ? new Color(244, 246, 253) : new Color(248, 249, 253));
            g.fillRoundRect(2, 2, w - 5, h - 5, 22, 22);
            g.setColor(chosen ? BLUE : new Color(225, 230, 242));
            g.setStroke(new BasicStroke(chosen ? 2.4f : 1.2f));
            g.drawRoundRect(3, 3, w - 7, h - 7, 21, 21);

            // illustration
            int ix = 16, iy = 16, iw = w - 32, ih = (int) (h * 0.52);
            Shape oldClip = g.getClip();
            g.clip(new RoundRectangle2D.Double(ix, iy, iw, ih, 16, 16));
            if (threeD) {
                paintThreeD(g, ix, iy, iw, ih);
            } else {
                paintTwoD(g, ix, iy, iw, ih);
            }
            g.setClip(oldClip);

            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.setColor(chosen ? BLUE : NAVY);
            FontMetrics fm = g.getFontMetrics();
            int ty = iy + ih + 30;
            g.drawString(label, (w - fm.stringWidth(label)) / 2, ty);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(MUTED);
            fm = g.getFontMetrics();
            g.drawString(caption, (w - fm.stringWidth(caption)) / 2, ty + 20);
            if (badge != null) {
                g.setFont(new Font("Segoe UI", Font.BOLD, 9));
                fm = g.getFontMetrics();
                int bw = fm.stringWidth(badge) + 14;
                g.setColor(new Color(255, 190, 75));
                g.fillRoundRect(w - bw - 24, iy + 8, bw, 18, 18, 18);
                g.setColor(NAVY);
                g.drawString(badge, w - bw - 17, iy + 21);
            }
            g.dispose();
        }

        private static void paintTwoD(Graphics2D g, int x, int y, int w, int h) {
            g.setPaint(new GradientPaint(x, y, new Color(236, 240, 252), x, y + h, new Color(222, 228, 246)));
            g.fillRect(x, y, w, h);
            int size = Math.min(h - 18, w - 40), bx = x + (w - size) / 2, by = y + (h - size) / 2, half = size / 2;
            Color[] colors = {new Color(255, 94, 117), new Color(83, 136, 255),
                    new Color(68, 205, 159), new Color(255, 195, 78)};
            for (int i = 0; i < 4; i++) {
                int qx = bx + (i % 2) * half, qy = by + (i / 2) * half;
                g.setColor(colors[i]);
                g.fillRoundRect(qx + 1, qy + 1, half - 2, half - 2, 8, 8);
                g.setColor(new Color(255, 255, 255, 200));
                g.fillRoundRect(qx + half / 5, qy + half / 5, half - 2 * (half / 5), half - 2 * (half / 5), 8, 8);
            }
            g.setColor(new Color(248, 249, 255));
            g.fillRoundRect(bx + half - 10, by + half - 10, 20, 20, 8, 8);
        }

        private static void paintThreeD(Graphics2D g, int x, int y, int w, int h) {
            double horizon = y + h * 0.42;
            g.setPaint(new GradientPaint(x, y, new Color(40, 104, 214), x, (float) horizon, new Color(176, 220, 255)));
            g.fill(new java.awt.geom.Rectangle2D.Double(x, y, w, horizon - y));
            g.setPaint(new GradientPaint(x, (float) horizon, new Color(140, 214, 120), x, y + h, new Color(48, 140, 70)));
            g.fill(new java.awt.geom.Rectangle2D.Double(x, horizon, w, y + h - horizon));
            g.setColor(new Color(255, 255, 255, 220));
            g.fill(new Ellipse2D.Double(x + w * 0.12, y + h * 0.12, 34, 12));
            g.fill(new Ellipse2D.Double(x + w * 0.18, y + h * 0.07, 26, 14));
            // road
            double cx = x + w / 2.0;
            Path2D road = new Path2D.Double();
            road.moveTo(cx - 4, horizon);
            road.lineTo(cx + 4, horizon);
            road.lineTo(cx + w * 0.30, y + h);
            road.lineTo(cx - w * 0.30, y + h);
            road.closePath();
            g.setColor(new Color(238, 234, 224));
            g.fill(road);
            g.setColor(new Color(60, 50, 80, 120));
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 1; i <= 4; i++) {
                double t = 1 - Math.pow(0.6, i);
                double ly = horizon + (y + h - horizon) * t;
                double half = 4 + (w * 0.30 - 4) * t;
                g.draw(new java.awt.geom.Line2D.Double(cx - half, ly, cx + half, ly));
            }
            // castles
            drawCastle(g, cx - w * 0.30, horizon + 8, 22, new Color(255, 94, 117));
            drawCastle(g, cx + w * 0.30, horizon + 14, 28, new Color(83, 136, 255));
            // piece, seen from behind
            double px = cx, py = y + h - 8;
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new Ellipse2D.Double(px - 14, py - 3, 28, 7));
            g.setColor(new Color(68, 205, 159));
            Path2D body = new Path2D.Double();
            body.moveTo(px - 11, py);
            body.curveTo(px - 11, py - 12, px - 4, py - 16, px - 3.5, py - 22);
            body.lineTo(px + 3.5, py - 22);
            body.curveTo(px + 4, py - 16, px + 11, py - 12, px + 11, py);
            body.closePath();
            g.fill(body);
            g.fill(new Ellipse2D.Double(px - 6, py - 33, 12, 12));
        }

        private static void drawCastle(Graphics2D g, double cx, double ground, double s, Color color) {
            g.setColor(new Color(226, 220, 205));
            g.fill(new java.awt.geom.Rectangle2D.Double(cx - s * 0.6, ground - s * 0.9, s * 1.2, s * 0.9));
            for (int t = -1; t <= 1; t += 2) {
                double tx = cx + t * s * 0.6;
                g.setColor(new Color(212, 205, 190));
                g.fill(new java.awt.geom.Rectangle2D.Double(tx - s * 0.2, ground - s * 1.4, s * 0.4, s * 1.4));
                Path2D roof = new Path2D.Double();
                roof.moveTo(tx - s * 0.28, ground - s * 1.4);
                roof.lineTo(tx + s * 0.28, ground - s * 1.4);
                roof.lineTo(tx, ground - s * 1.8);
                roof.closePath();
                g.setColor(color);
                g.fill(roof);
            }
        }
    }

    private static final class PillButton extends JButton {
        private final boolean primary;
        private boolean hovered;

        PillButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            GameScreen.quality(g);
            int w = getWidth(), h = getHeight();
            if (primary) {
                g.setPaint(new GradientPaint(0, 0, hovered ? new Color(107, 112, 255) : BLUE,
                        w, h, hovered ? new Color(150, 92, 246) : new Color(118, 76, 226)));
            } else {
                g.setColor(hovered ? new Color(226, 234, 255) : new Color(239, 243, 253));
            }
            g.fillRoundRect(1, 1, w - 3, h - 3, 18, 18);
            g.setFont(getFont());
            g.setColor(primary ? Color.WHITE : new Color(40, 53, 91));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
