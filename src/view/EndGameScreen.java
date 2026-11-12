package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * End-of-game screen for the current game.
 */
public class EndGameScreen extends JPanel {

    private static final Color NAVY = new Color(20, 25, 68);
    private static final Color BLUE = new Color(74, 107, 245);
    private static final Color INK = new Color(31, 41, 78);
    private static final Color MUTED = new Color(115, 124, 151);
    private static final int CARD_WIDTH = 630;
    private static final int CONTENT_WIDTH = 555;

    public EndGameScreen(String[] names, String[] colors, String difficulty, String duration) {
        setLayout(new GridBagLayout());
        setOpaque(true);

        String winner = (names != null && names.length > 0) ? names[0] : "Player 1";
        String winnerColor = (colors != null && colors.length > 0) ? colors[0] : "Blue";
        String level = difficulty == null ? "Easy" : difficulty;
        String elapsed = duration == null ? "00:00:00" : duration;

        RoundedPanel card = new RoundedPanel(Color.WHITE, 28);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 35, 19, 35));
        card.setPreferredSize(new Dimension(CARD_WIDTH, 518));
        card.setMaximumSize(new Dimension(CARD_WIDTH, 518));

        JLabel kicker = label("YOUR MATCH RESULTS", 11, Font.BOLD, BLUE);
        kicker.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(kicker);
        card.add(Box.createVerticalStrut(3));

        JLabel heading = label("GAME OVER", 34, Font.BOLD, INK);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(heading);

        JLabel subtitle = label("Great game! Here is how everyone placed.", 12, Font.PLAIN, MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(13));

        WinnerPanel winnerPanel = new WinnerPanel(winner, winnerColor);
        sized(winnerPanel, CONTENT_WIDTH, 76);
        card.add(winnerPanel);
        card.add(Box.createVerticalStrut(11));

        JPanel stats = new JPanel(new GridLayout(1, 2, 10, 0));
        stats.setOpaque(false);
        stats.add(new StatPanel("DIFFICULTY", level.toUpperCase()));
        stats.add(new StatPanel("GAME TIME", elapsed));
        sized(stats, CONTENT_WIDTH, 43);
        card.add(stats);
        card.add(Box.createVerticalStrut(12));

        JLabel rankingTitle = label("FINAL RANKING", 12, Font.BOLD, INK);
        rankingTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(rankingTitle);
        card.add(Box.createVerticalStrut(6));

        String[] places = {"1ST", "2ND", "3RD", "4TH"};
        int count = names == null ? 0 : Math.min(places.length, names.length);
        for (int i = 0; i < count; i++) {
            String c = (colors != null && i < colors.length) ? colors[i] : "Blue";
            RankingRow row = new RankingRow(places[i], names[i], c, i == 0);
            sized(row, CONTENT_WIDTH, 31);
            card.add(row);
            if (i < count - 1) card.add(Box.createVerticalStrut(4));
        }

        card.add(Box.createVerticalGlue());
        JLabel note = label("Game history is not available yet.", 10, Font.PLAIN, MUTED);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(note);
        card.add(Box.createVerticalStrut(10));

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        RoundedButton historyButton = new RoundedButton("View Game History", BLUE);
        RoundedButton menuButton = new RoundedButton("Back to Main Menu", new Color(104, 115, 144));
        actions.add(historyButton);
        actions.add(menuButton);
        sized(actions, CONTENT_WIDTH, 43);
        card.add(actions);

        historyButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            new GameHistoryView().setVisible(true);
            if (window != null) window.dispose();
        });
        menuButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            new MainMenuView().setVisible(true);
            if (window != null) window.dispose();
        });
        add(card);
    }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel result = new JLabel(text);
        result.setFont(new Font("Segoe UI", style, size));
        result.setForeground(color);
        return result;
    }

    private static void sized(JComponent component, int width, int height) {
        Dimension d = new Dimension(width, height);
        component.setPreferredSize(d);
        component.setMinimumSize(d);
        component.setMaximumSize(d);
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
    }

    private static Color colorOf(String name) {
        return BoardPanel.colorOf(name == null ? "Blue" : name);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, NAVY, getWidth(), getHeight(), new Color(63, 51, 122)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(117, 124, 244, 24));
        g2.fillOval(getWidth() - 260, -160, 410, 410);
        g2.setColor(new Color(139, 113, 246, 23));
        g2.fillOval(-210, getHeight() - 280, 440, 440);
        g2.dispose();
    }

    private static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int arc;

        RoundedPanel(Color bg, int arc) {
            this.bg = bg;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class WinnerPanel extends JPanel {
        WinnerPanel(String name, String playerColor) {
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setBorder(new EmptyBorder(10, 16, 10, 17));

            JPanel icon = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D a = (Graphics2D) g.create();
                    a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    a.setColor(new Color(255, 218, 121));
                    a.fillOval(3, 3, 49, 49);
                    a.setColor(new Color(170, 111, 17));
                    a.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    // Trophy body and handles, drawn directly so no external icons are needed.
                    a.setColor(new Color(235, 167, 42));
                    a.fillRoundRect(15, 12, 25, 20, 7, 7);
                    a.setColor(new Color(190, 120, 24));
                    a.drawArc(8, 12, 17, 20, 100, 180);
                    a.drawArc(31, 12, 17, 20, -100, 180);
                    a.setStroke(new BasicStroke(4f));
                    a.drawLine(27, 32, 27, 39);
                    a.drawLine(19, 41, 35, 41);
                    a.dispose();
                }
            };
            icon.setOpaque(false);
            icon.setPreferredSize(new Dimension(55, 55));
            add(icon, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            JLabel title = label("CHAMPION", 10, Font.BOLD, new Color(80, 100, 176));
            JLabel who = label(name == null ? "Player" : name, 22, Font.BOLD, INK);
            who.setToolTipText(name);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            who.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(Box.createVerticalGlue());
            text.add(title);
            text.add(Box.createVerticalStrut(2));
            text.add(who);
            text.add(Box.createVerticalGlue());
            add(text, BorderLayout.CENTER);

            JLabel flag = label("1ST PLACE", 11, Font.BOLD, colorOf(playerColor).darker());
            add(flag, BorderLayout.EAST);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D a = (Graphics2D) g.create();
            a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            a.setPaint(new GradientPaint(0, 0, new Color(228, 236, 255), getWidth(), getHeight(), new Color(243, 238, 255)));
            a.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            a.setColor(new Color(205, 216, 253));
            a.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            a.dispose();
            super.paintComponent(g);
        }
    }

    private static class StatPanel extends JPanel {
        StatPanel(String title, String value) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, 14, 5, 14));
            JLabel key = label(title, 10, Font.BOLD, MUTED);
            JLabel val = label(value, 15, Font.BOLD, INK);
            add(key, BorderLayout.NORTH);
            add(val, BorderLayout.SOUTH);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D a = (Graphics2D) g.create();
            a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            a.setColor(new Color(246, 248, 253));
            a.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            a.dispose();
            super.paintComponent(g);
        }
    }

    private static class RankingRow extends JPanel {
        private final boolean first;

        RankingRow(String rank, String name, String playerColor, boolean first) {
            this.first = first;
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setBorder(new EmptyBorder(2, 12, 2, 12));

            JLabel number = label(rank, 11, Font.BOLD, first ? BLUE : MUTED);
            number.setPreferredSize(new Dimension(40, 22));
            add(number, BorderLayout.WEST);

            JLabel player = label(name == null ? "Player" : name, 13, first ? Font.BOLD : Font.PLAIN, INK);
            player.setToolTipText(name);
            add(player, BorderLayout.CENTER);

            JPanel flag = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
            flag.setOpaque(false);
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D a = (Graphics2D) g.create();
                    a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    a.setColor(colorOf(playerColor));
                    a.fill(new Ellipse2D.Float(1, 4, 13, 13));
                    a.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setPreferredSize(new Dimension(16, 22));
            JLabel colorName = label(playerColor, 11, Font.PLAIN, MUTED);
            flag.add(dot);
            flag.add(colorName);
            add(flag, BorderLayout.EAST);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D a = (Graphics2D) g.create();
            a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            a.setColor(first ? new Color(235, 240, 255) : new Color(248, 249, 253));
            a.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            a.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private final Color bg;
        private boolean hover;

        RoundedButton(String text, Color bg) {
            super(text);
            this.bg = bg;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D a = (Graphics2D) g.create();
            a.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color active = hover ? bg.brighter() : bg;
            a.setColor(active);
            a.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            a.dispose();
            super.paintComponent(g);
        }
    }
}
