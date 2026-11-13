package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Modern Ludo game setup screen. Pure Java Swing; no external libraries. */
public class GameSetupView extends JFrame {

    // Keep the original combo-box API: selection cards synchronize with these models.
    private final JComboBox<Integer> playerCountComboBox =
            new JComboBox<>(new Integer[]{2, 3, 4});
    private final JComboBox<String> difficultyComboBox =
            new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
    private JButton continueButton;
    private JButton backButton;

    private ChoiceCard[] playerCards;
    private ChoiceCard[] difficultyCards;

    private static final Color NAVY = new Color(12, 20, 49);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 123, 151);
    private static final Color TEXT_LIGHT = new Color(203, 213, 248);

    public GameSetupView() {
        setTitle("Ludo - New Game");
        setSize(1120, 690);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = new GradientBackground();
        background.setLayout(new GridBagLayout());

        JPanel layout = new JPanel(new GridLayout(1, 2, 28, 0));
        layout.setOpaque(false);
        layout.setPreferredSize(new Dimension(1020, 565));
        layout.add(new SetupHero());
        layout.add(buildSetupCard());

        background.add(layout);
        setContentPane(background);

        // Keep selection state in sync, including external use of the original getters.
        playerCountComboBox.addActionListener(e -> refreshSelectedCards());
        difficultyComboBox.addActionListener(e -> refreshSelectedCards());
        refreshSelectedCards();
    }

    private JPanel buildSetupCard() {
        JPanel card = new RoundedCard();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(30, 35, 22, 35));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("STEP 01  /  GAME SETUP");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        eyebrow.setForeground(BLUE);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(eyebrow);
        form.add(Box.createVerticalStrut(9));

        JLabel heading = new JLabel("Make it your game");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 29));
        heading.setForeground(NAVY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(heading);
        form.add(Box.createVerticalStrut(4));

        JLabel subheading = new JLabel("A few quick choices, then you're off!");
        subheading.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subheading.setForeground(MUTED);
        subheading.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(subheading);
        form.add(Box.createVerticalStrut(20));

        form.add(fieldTitle("01", "HOW MANY PLAYERS?"));
        form.add(Box.createVerticalStrut(11));

        JPanel playerOptions = createChoiceRow();
        playerCards = new ChoiceCard[]{
            new ChoiceCard("2", "DUEL"),
            new ChoiceCard("3", "TRIO"),
            new ChoiceCard("4", "PARTY")
        };
        for (int i = 0; i < playerCards.length; i++) {
            final int count = i + 2;
            playerCards[i].addActionListener(e -> playerCountComboBox.setSelectedItem(count));
            playerCards[i].getAccessibleContext().setAccessibleName(count + " players");
            playerCards[i].getAccessibleContext().setAccessibleDescription("Choose a " + count + "-player game");
            playerOptions.add(playerCards[i]);
        }
        form.add(playerOptions);
        form.add(Box.createVerticalStrut(19));

        form.add(fieldTitle("02", "CHOOSE YOUR DIFFICULTY"));
        form.add(Box.createVerticalStrut(11));

        JPanel difficultyOptions = createChoiceRow();
        difficultyCards = new ChoiceCard[]{
            new ChoiceCard("Easy", "RELAXED"),
            new ChoiceCard("Medium", "BALANCED"),
            new ChoiceCard("Hard", "CHALLENGE")
        };
        String[] difficulties = {"Easy", "Medium", "Hard"};
        for (int i = 0; i < difficultyCards.length; i++) {
            final String choice = difficulties[i];
            difficultyCards[i].addActionListener(e -> difficultyComboBox.setSelectedItem(choice));
            difficultyCards[i].getAccessibleContext().setAccessibleName(choice + " difficulty");
            difficultyCards[i].getAccessibleContext().setAccessibleDescription("Choose " + choice + " game difficulty");
            difficultyOptions.add(difficultyCards[i]);
        }
        form.add(difficultyOptions);

        card.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));

        continueButton = new ActionButton("Continue", true);
        continueButton.addActionListener(e -> openPlayerDetails());

        backButton = new ActionButton("Back to Menu", false);
        backButton.addActionListener(e -> {
            new MainMenuView().setVisible(true);
            dispose();
        });

        actions.add(continueButton);
        actions.add(Box.createVerticalStrut(11));
        actions.add(backButton);
        actions.add(Box.createVerticalStrut(10));

        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    /**
     * SRS 3.2.1.3.1 -
     * Opens the player-details screen using the selected player count
     * and difficulty.
     */
    private void openPlayerDetails() {
        Integer count = (Integer) playerCountComboBox.getSelectedItem();
        String difficulty = (String) difficultyComboBox.getSelectedItem();
        if (count == null || difficulty == null) return;

        JFrame detailsFrame = new JFrame("Ludo - Player Details");
        detailsFrame.setSize(700, 600);
        detailsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        detailsFrame.setLocationRelativeTo(null);
        detailsFrame.setResizable(false);
        detailsFrame.setContentPane(new PlayerDetailsScreen(count, difficulty));
        detailsFrame.setVisible(true);
        dispose();
    }

    private void refreshSelectedCards() {
        if (playerCards == null || difficultyCards == null) return;
        Object count = playerCountComboBox.getSelectedItem();
        Object difficulty = difficultyComboBox.getSelectedItem();
        for (int i = 0; i < playerCards.length; i++) {
            playerCards[i].setSelectedState(Integer.valueOf(i + 2).equals(count));
        }
        String[] options = {"Easy", "Medium", "Hard"};
        for (int i = 0; i < difficultyCards.length; i++) {
            difficultyCards[i].setSelectedState(options[i].equals(difficulty));
        }
    }

    private static JPanel createChoiceRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        row.setPreferredSize(new Dimension(416, 86));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JComponent fieldTitle(String number, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        JLabel step = new JLabel(number);
        step.setFont(new Font("Segoe UI", Font.BOLD, 12));
        step.setForeground(BLUE);
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(42, 51, 81));
        row.add(step);
        row.add(label);
        return row;
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static class GradientBackground extends JPanel {
        @Override protected void paintComponent(Graphics graphics) {
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


    // Visual illustration in the same style as the new main menu.
    private static class SetupHero extends JPanel {
        SetupHero() { setOpaque(false); }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);

            g.setColor(new Color(177, 186, 240));
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.drawString("THE FUN STARTS HERE", 39, 75);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 58));
            g.drawString("Your game.", 35, 148);
            g.drawString("Your rules.", 35, 211);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 17));
            g.setColor(TEXT_LIGHT);
            g.drawString("Pick your crew, set the challenge,", 39, 257);
            g.drawString("and get ready to roll!", 39, 284);

            // Draw a mini Ludo-inspired board; no external assets needed.
            int bx = 110, by = 320, size = 210, half = size / 2;
            g.setColor(new Color(4, 9, 29, 65));
            g.fillRoundRect(bx + 9, by + 12, size, size, 26, 26);
            g.setColor(Color.WHITE);
            g.fillRoundRect(bx - 6, by - 6, size + 12, size + 12, 23, 23);
            g.setColor(new Color(255, 94, 117));
            g.fillRoundRect(bx, by, half, half, 13, 13);
            g.setColor(new Color(83, 136, 255));
            g.fillRoundRect(bx + half, by, half, half, 13, 13);
            g.setColor(new Color(68, 205, 159));
            g.fillRoundRect(bx, by + half, half, half, 13, 13);
            g.setColor(new Color(255, 195, 78));
            g.fillRoundRect(bx + half, by + half, half, half, 13, 13);

            drawHome(g, bx, by, half);
            drawHome(g, bx + half, by, half);
            drawHome(g, bx, by + half, half);
            drawHome(g, bx + half, by + half, half);

            g.setColor(new Color(248, 249, 255));
            g.fillRoundRect(bx + half - 27, by + half - 27, 54, 54, 17, 17);
            g.setColor(new Color(92, 101, 185));
            int mx = bx + half, my = by + half;
            Polygon diamond = new Polygon(
                new int[]{mx, mx + 13, mx, mx - 13},
                new int[]{my - 16, my, my + 16, my}, 4);
            g.fillPolygon(diamond);
            g.dispose();
        }

        private void drawHome(Graphics2D g, int x, int y, int square) {
            g.setColor(new Color(255, 255, 255, 215));
            g.fillRoundRect(x + 17, y + 17, square - 34, square - 34, 17, 17);
            g.setColor(new Color(255, 255, 255));
            int radius = 9;
            for (int dx = 0; dx < 2; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    int cx = x + 39 + dx * 27, cy = y + 39 + dy * 27;
                    g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                }
            }
            g.setColor(new Color(205, 211, 230));
            g.setStroke(new BasicStroke(1));
            for (int dx = 0; dx < 2; dx++) {
                for (int dy = 0; dy < 2; dy++) {
                    int cx = x + 39 + dx * 27, cy = y + 39 + dy * 27;
                    g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
                }
            }
        }
    }

    private static class RoundedCard extends JPanel {
        RoundedCard() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(6, 11, 31, 45));
            g.fill(new RoundRectangle2D.Double(6, 10, w - 12, h - 19, 38, 38));
            g.setColor(new Color(250, 251, 255));
            g.fill(new RoundRectangle2D.Double(2, 2, w - 12, h - 19, 38, 38));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class ChoiceCard extends JButton {
        private final String label;
        private final String caption;
        private boolean selectedState;
        private boolean hovered;

        ChoiceCard(String label, String caption) {
            this.label = label;
            this.caption = caption;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Select " + label);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        void setSelectedState(boolean selectedState) {
            this.selectedState = selectedState;
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            g.setColor(selectedState ? new Color(234, 239, 255) :
                    hovered ? new Color(244, 246, 253) : new Color(248, 249, 253));
            g.fillRoundRect(2, 2, w - 5, h - 5, 19, 19);
            g.setColor(selectedState ? BLUE : new Color(225, 230, 242));
            g.setStroke(new BasicStroke(selectedState ? 2.3f : 1.2f));
            g.drawRoundRect(3, 3, w - 7, h - 7, 18, 18);

            g.setColor(selectedState ? BLUE : NAVY);
            Font mainFont = new Font("Segoe UI", Font.BOLD, label.length() >= 5 ? 19 : 25);
            g.setFont(mainFont);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, (w - fm.stringWidth(label)) / 2, 53);
            g.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g.setColor(MUTED);
            fm = g.getFontMetrics();
            g.drawString(caption, (w - fm.stringWidth(caption)) / 2, 69);
            if (isFocusOwner()) {
                g.setColor(new Color(75, 110, 248, 130));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(6, 6, w - 13, h - 13, 16, 16);
            }
            g.dispose();
        }
    }

    private static class ActionButton extends JButton {
        private final boolean primary;
        private boolean hovered;

        ActionButton(String label, boolean primary) {
            super(label);
            this.primary = primary;
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setPreferredSize(new Dimension(400, primary ? 52 : 46));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, primary ? 52 : 46));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            if (primary) {
                g.setPaint(new GradientPaint(0, 0,
                        hovered ? new Color(107, 112, 255) : BLUE,
                        w, h, hovered ? new Color(150, 92, 246) : new Color(118, 76, 226)));
            } else {
                g.setColor(hovered ? new Color(226, 234, 255) : new Color(239, 243, 253));
            }
            g.fillRoundRect(2, 2, w - 5, h - 5, 18, 18);
            if (isFocusOwner()) {
                g.setColor(new Color(80, 130, 240));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(4, 4, w - 9, h - 9, 15, 15);
            }
            String text = getText();
            g.setFont(getFont());
            g.setColor(primary ? Color.WHITE : new Color(40, 53, 91));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (w - fm.stringWidth(text)) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
            if (primary) {
                int cx = w - 27, cy = h / 2;
                g.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(cx - 6, cy - 5, cx, cy);
                g.drawLine(cx, cy, cx - 6, cy + 5);
            }
            g.dispose();
        }
    }

    public JComboBox<Integer> getPlayerCountComboBox() { return playerCountComboBox; }
    public JComboBox<String> getDifficultyComboBox() { return difficultyComboBox; }
    public JButton getContinueButton() { return continueButton; }
    public JButton getBackButton() { return backButton; }
}
