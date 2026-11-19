package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Screen shown after the player chose "3D LUDO": a list of the worlds (see {@link WorldTheme}).
 * Clicking a world closes the screen and the game starts in that world.
 */
public class ThemeChoiceDialog extends JDialog {

    private static final Color NAVY = new Color(12, 20, 49);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 123, 151);
    private static final int PREVIEW_W = 210, PREVIEW_H = 84;

    private WorldTheme chosen;

    private ThemeChoiceDialog(Window owner) {
        super(owner, "Ludo - Choose Your World", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        MainMenuScreen.applyAppIcon(this);

        JPanel background = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                GameScreen.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(14, 22, 54), getWidth(), getHeight(), new Color(43, 39, 100)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(110, 103, 231, 45));
                g.fillOval(-120, -150, 420, 420);
                g.setColor(new Color(72, 177, 246, 24));
                g.fillOval(480, 300, 380, 380);
                g.dispose();
            }
        };

        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                GameScreen.quality(g);
                g.setColor(new Color(6, 11, 31, 55));
                g.fill(new RoundRectangle2D.Double(5, 9, getWidth() - 10, getHeight() - 14, 34, 34));
                g.setColor(new Color(250, 251, 255));
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 6, getHeight() - 10, 34, 34));
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 24, 16, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("STEP 04  /  CHOOSE YOUR WORLD");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        eyebrow.setForeground(BLUE);
        JLabel title = new JLabel("Which 3D Ludo do you want to play?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(NAVY);
        JLabel subtitle = new JLabel("Pick a world and the game starts. The rules are the same in every world.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        header.add(eyebrow);
        header.add(Box.createVerticalStrut(5));
        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(subtitle);
        header.setPreferredSize(new Dimension(670, 86));
        card.add(header, BorderLayout.NORTH);

        List<WorldTheme> themes = Themes.all();
        JPanel list = new JPanel(new GridLayout(themes.size(), 1, 0, 8));
        list.setOpaque(false);
        for (int i = 0; i < themes.size(); i++) {
            WorldTheme theme = themes.get(i);
            ThemeRow row = new ThemeRow(theme, i + 1);
            row.addActionListener(e -> {
                chosen = theme;
                dispose();
            });
            row.getAccessibleContext().setAccessibleName(theme.title());
            list.add(row);
        }
        card.add(list, BorderLayout.CENTER);

        JButton back = new JButton("Back") {
            private boolean hovered;
            {
                setFont(new Font("Segoe UI", Font.BOLD, 15));
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(0, 44));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                GameScreen.quality(g);
                g.setColor(hovered ? new Color(226, 234, 255) : new Color(239, 243, 253));
                g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g.setFont(getFont());
                g.setColor(new Color(40, 53, 91));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g.dispose();
            }
        };
        back.addActionListener(e -> dispose());
        card.add(back, BorderLayout.SOUTH);

        card.setPreferredSize(new Dimension(720, 150 + themes.size() * 98 + 60));
        background.add(card);
        background.setPreferredSize(new Dimension(780, 210 + themes.size() * 98 + 60));
        setContentPane(background);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Shows the screen.
     *
     * @return the chosen world, or {@code null} if the player pressed "Back"
     */
    static WorldTheme choose(Window owner) {
        ThemeChoiceDialog dialog = new ThemeChoiceDialog(owner);
        dialog.setVisible(true);
        return dialog.chosen;
    }

    /** One row of the list: picture of the world, name and a short description. */
    private static final class ThemeRow extends JButton {
        private final WorldTheme theme;
        private final int number;
        private final BufferedImage preview;
        private boolean hovered;

        ThemeRow(WorldTheme theme, int number) {
            this.theme = theme;
            this.number = number;
            this.preview = new BufferedImage(PREVIEW_W, PREVIEW_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = preview.createGraphics();
            GameScreen.quality(pg);
            pg.setClip(new RoundRectangle2D.Double(0, 0, PREVIEW_W, PREVIEW_H, 16, 16));
            theme.paintPreview(pg, PREVIEW_W, PREVIEW_H);
            pg.dispose();
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Play in: " + theme.title());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            GameScreen.quality(g);
            int w = getWidth(), h = getHeight();
            g.setColor(hovered ? new Color(236, 241, 255) : new Color(247, 249, 254));
            g.fillRoundRect(2, 2, w - 5, h - 5, 22, 22);
            g.setColor(hovered ? theme.accent() : new Color(225, 230, 242));
            g.setStroke(new BasicStroke(hovered ? 2.6f : 1.3f));
            g.drawRoundRect(3, 3, w - 7, h - 7, 21, 21);
            g.drawImage(preview, 12, (h - PREVIEW_H) / 2, null);
            // number badge on the picture
            g.setColor(theme.accent());
            g.fillOval(18, (h - PREVIEW_H) / 2 + 6, 24, 24);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            String n = String.valueOf(number);
            g.drawString(n, 30 - fm.stringWidth(n) / 2, (h - PREVIEW_H) / 2 + 6 + 12 + (fm.getAscent() - fm.getDescent()) / 2);
            int tx = 12 + PREVIEW_W + 20;
            g.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g.setColor(hovered ? theme.accent().darker() : NAVY);
            g.drawString(theme.title(), tx, h / 2 - 4);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g.setColor(MUTED);
            drawWrapped(g, theme.tagline(), tx, h / 2 + 16, w - tx - 50);
            // arrow
            g.setColor(hovered ? theme.accent() : new Color(160, 170, 200));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int ax = w - 28, ay = h / 2;
            g.drawLine(ax - 6, ay - 8, ax + 2, ay);
            g.drawLine(ax + 2, ay, ax - 6, ay + 8);
            g.dispose();
        }

        private static void drawWrapped(Graphics2D g, String text, int x, int y, int width) {
            FontMetrics fm = g.getFontMetrics();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int lineY = y;
            for (String word : words) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(test) > width && line.length() > 0) {
                    g.drawString(line.toString(), x, lineY);
                    lineY += fm.getHeight();
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            g.drawString(line.toString(), x, lineY);
        }
    }
}
