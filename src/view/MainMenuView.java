package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Modern Ludo main menu - pure Java Swing, no external libraries. */
public class MainMenuView extends JFrame {

    private JButton newGameButton;
    private JButton gameHistoryButton;
    private JButton questionManagementButton;
    private JButton exitButton;

    private static final Color NAVY = new Color(12, 20, 49);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 123, 151);

    public MainMenuView() {
        setTitle("Ludo - Play. Learn. Win.");
        setSize(1120, 690);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = new GradientBackground();
        background.setLayout(new GridBagLayout());

        JPanel content = new JPanel(new GridLayout(1, 2, 28, 0));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(1020, 565));

        content.add(new HeroPanel());
        content.add(createMenuCard());
        background.add(content);
        setContentPane(background);

        // שמירה על הניווט הקיים של הפרויקט
        newGameButton.addActionListener(e -> {
            new GameSetupView().setVisible(true);
            dispose();
        });

        gameHistoryButton.addActionListener(e -> {
            new GameHistoryView().setVisible(true);
            dispose();
        });

        questionManagementButton.addActionListener(e -> {
            AdminLoginView login = new AdminLoginView(() ->
                new QuestionManagementView().setVisible(true)
            );
            login.setVisible(true);
            dispose();
        });

        exitButton.addActionListener(e -> dispose());
    }

    private JPanel createMenuCard() {
        JPanel card = new RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(48, 45, 38, 45));

        JLabel eyebrow = new JLabel("YOUR NEXT ADVENTURE STARTS HERE");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        eyebrow.setForeground(BLUE);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Ready to play?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Choose where you want to go.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        newGameButton = createButton("New Game", 0, 0);
        gameHistoryButton = createButton("Game History", 1, 1);
        // SRS 3.2.8.3.4 - the game history screen reads the saved games from the history file.
        questionManagementButton = createButton("Question Management", 1, 2);
        questionManagementButton.setVisible(false); // Hidden from the main menu for iteration 1.
        exitButton = createButton("Exit", 2, 3);

        card.add(eyebrow);
        card.add(Box.createVerticalStrut(11));
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(32));
        card.add(newGameButton);
        card.add(Box.createVerticalStrut(13));
        card.add(gameHistoryButton);
        card.add(Box.createVerticalStrut(13));
        card.add(questionManagementButton);
        card.add(Box.createVerticalStrut(13));
        card.add(exitButton);
        card.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("A little luck. A lot of fun.");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footer.setForeground(MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(footer);
        return card;
    }

    private JButton createButton(String text, int style, int iconType) {
        return new MenuButton(text, style, iconType);
    }

    public JButton getNewGameButton() { return newGameButton; }
    public JButton getGameHistoryButton() { return gameHistoryButton; }
    public JButton getQuestionManagementButton() { return questionManagementButton; }
    public JButton getExitButton() { return exitButton; }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // רקע מדורג עם צורות צבעוניות שקופות
    private static class GradientBackground extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setPaint(new GradientPaint(0, 0, new Color(14, 22, 54),
                    getWidth(), getHeight(), new Color(43, 39, 100)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(110, 103, 231, 45));
            g.fillOval(-180, -210, 650, 650);
            g.setColor(new Color(72, 177, 246, 24));
            g.fillOval(460, 320, 580, 580);
            g.setColor(new Color(244, 91, 146, 22));
            g.fillOval(890, -230, 440, 440);
            g.dispose();
        }
    }

    // אזור המיתוג: קוביות Ludo מאוירות בקוד
    private static class HeroPanel extends JPanel {
        HeroPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);

            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.setColor(new Color(177, 186, 240));
            g.drawString("THE BOARD GAME, REIMAGINED", 39, 78);

            g.setFont(new Font("Segoe UI", Font.BOLD, 98));
            g.setColor(Color.WHITE);
            g.drawString("LUDO", 30, 195);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 25));
            g.setColor(new Color(207, 215, 248));
            g.drawString("Play. Learn. Win.", 39, 241);

            g.setColor(new Color(151, 167, 218));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            g.drawString("Roll the dice. Make your move.", 40, 279);

            drawDie(g, 55, 341, 100, new Color(255, 89, 118), 4, -0.12);
            drawDie(g, 203, 318, 111, new Color(84, 137, 255), 5, 0.11);
            drawDie(g, 105, 445, 95, new Color(255, 197, 77), 2, 0.13);
            drawDie(g, 291, 438, 99, new Color(68, 205, 159), 6, -0.13);
            g.dispose();
        }

        private void drawDie(Graphics2D g, int x, int y, int size,
                             Color color, int dots, double tilt) {
            AffineTransform old = g.getTransform();
            g.rotate(tilt, x + size / 2.0, y + size / 2.0);
            g.setColor(new Color(3, 8, 28, 65));
            g.fillRoundRect(x + 6, y + 11, size, size, 25, 25);
            g.setPaint(new GradientPaint(x, y, color.brighter(), x + size,
                    y + size, color.darker()));
            g.fillRoundRect(x, y, size, size, 25, 25);
            g.setColor(new Color(255, 255, 255, 75));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x + 3, y + 3, size - 6, size - 6, 23, 23);
            g.setColor(Color.WHITE);
            int a = x + size / 4;
            int b = x + size / 2;
            int c = x + size * 3 / 4;
            int t = y + size / 4;
            int m = y + size / 2;
            int d = y + size * 3 / 4;
            int r = Math.max(5, size / 12);
            if (dots == 1 || dots == 3 || dots == 5) dot(g, b, m, r);
            if (dots >= 2) { dot(g, a, t, r); dot(g, c, d, r); }
            if (dots >= 4) { dot(g, c, t, r); dot(g, a, d, r); }
            if (dots == 6) { dot(g, a, m, r); dot(g, c, m, r); }
            g.setTransform(old);
        }

        private void dot(Graphics2D g, int cx, int cy, int radius) {
            g.fill(new Ellipse2D.Double(cx - radius, cy - radius,
                    radius * 2, radius * 2));
        }
    }

    // כרטיס עם פינות מעוגלות וצל
    private static class RoundedCard extends JPanel {
        RoundedCard() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(6, 11, 31, 45));
            g.fill(new RoundRectangle2D.Double(6, 10, w - 12, h - 19, 38, 38));
            g.setColor(new Color(250, 251, 255));
            g.fill(new RoundRectangle2D.Double(2, 2, w - 12, h - 19, 38, 38));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    // כפתור מעוצב עם Hover ואייקון מצויר, ללא ספרייה חיצונית
    private static class MenuButton extends JButton {
        private final int style;
        private final int iconType;
        private boolean hovered;

        MenuButton(String label, int style, int iconType) {
            super(label);
            this.style = style;
            this.iconType = iconType;
            setFont(new Font("Segoe UI", Font.BOLD, 17));
            setForeground(style == 0 ? Color.WHITE : NAVY);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setPreferredSize(new Dimension(350, 61));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 61));
            setMinimumSize(new Dimension(200, 61));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            int w = getWidth();
            int h = getHeight();
            int x = 2, y = 2, bw = w - 5, bh = h - 5;

            Color fg;
            if (style == 0) {
                g.setPaint(new GradientPaint(0, 0,
                        hovered ? new Color(107, 112, 255) : BLUE,
                        w, h, hovered ? new Color(150, 92, 246) : new Color(118, 76, 226)));
                fg = Color.WHITE;
            } else if (style == 2) {
                g.setColor(hovered ? new Color(255, 221, 228) : new Color(255, 237, 241));
                fg = new Color(188, 56, 87);
            } else {
                g.setColor(hovered ? new Color(226, 234, 255) : new Color(239, 243, 253));
                fg = new Color(36, 49, 90);
            }
            g.fillRoundRect(x, y, bw, bh, 19, 19);
            if (isFocusOwner()) {
                g.setColor(new Color(60, 100, 224));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(x + 1, y + 1, bw - 2, bh - 2, 18, 18);
            }
            drawIcon(g, iconType, 27, h / 2, fg);
            g.setColor(fg);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), 56, (h - fm.getHeight()) / 2 + fm.getAscent());
            if (style != 2) {
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(w - 28, h / 2 - 5, w - 23, h / 2);
                g.drawLine(w - 23, h / 2, w - 28, h / 2 + 5);
            }
            g.dispose();
        }

        private void drawIcon(Graphics2D g, int type, int cx, int cy, Color color) {
            g.setColor(color);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case 0: // משחק חדש
                    Path2D play = new Path2D.Double();
                    play.moveTo(cx - 6, cy - 9);
                    play.lineTo(cx + 8, cy);
                    play.lineTo(cx - 6, cy + 9);
                    play.closePath();
                    g.fill(play);
                    break;
                case 1: // היסטוריה
                    g.drawOval(cx - 10, cy - 10, 20, 20);
                    g.drawLine(cx, cy, cx, cy - 6);
                    g.drawLine(cx, cy, cx + 5, cy + 3);
                    break;
                case 2: // ניהול
                    g.drawRoundRect(cx - 10, cy - 8, 20, 16, 4, 4);
                    g.drawLine(cx - 6, cy - 3, cx + 6, cy - 3);
                    g.drawLine(cx - 6, cy + 3, cx + 2, cy + 3);
                    break;
                default: // יציאה
                    g.drawArc(cx - 9, cy - 9, 18, 18, 45, 270);
                    g.drawLine(cx, cy - 12, cx, cy + 1);
            }
        }
    }
}
