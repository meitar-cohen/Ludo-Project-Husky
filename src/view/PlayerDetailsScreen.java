package view;

import controller.GameSetupController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

/**
 * Player details screen for game setup.
 * Uses only Java Swing; keeps all original validation and game navigation.
 */
public class PlayerDetailsScreen extends JPanel {

    private final int numPlayers;
    private final String difficulty;
    private final JTextField[] nameFields;
    private final JComboBox<String>[] colorSelectors;
    private JLabel errorLabel;

    private static final Color NAVY = new Color(12, 20, 49);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 123, 151);
    private static final Color[] PLAYER_COLORS = {
        new Color(255, 99, 124), new Color(77, 126, 250),
        new Color(46, 191, 147), new Color(255, 190, 75)
    };
    private static final String[] COLOR_NAMES = {"Red", "Blue", "Green", "Yellow"};

    public PlayerDetailsScreen(int selectedPlayers, String difficulty) {
        this.numPlayers = Math.max(2, Math.min(4, selectedPlayers));
        this.difficulty = difficulty == null ? "Easy" : difficulty;
        this.nameFields = new JTextField[this.numPlayers];
        @SuppressWarnings("unchecked")
        JComboBox<String>[] selectors = (JComboBox<String>[]) new JComboBox<?>[this.numPlayers];
        this.colorSelectors = selectors;

        setLayout(new GridBagLayout());
        setOpaque(false);

        JPanel content = new JPanel(new GridLayout(1, 2, 28, 0));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(1020, 574));
        content.add(new PlayerHero());
        content.add(buildDetailsCard());
        add(content);
    }

    public PlayerDetailsScreen(int selectedPlayers) {
        this(selectedPlayers, "Easy");
    }

    // GameSetupView currently creates a 700x600 host window. Enlarge it for this
    // matching two-column layout without requiring edits to GameSetupView.java.
    @Override
    public void addNotify() {
        super.addNotify();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame && window.getWidth() < 1100) {
            window.setSize(1120, 690);
            window.setLocationRelativeTo(null);
        }
    }

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

    private JPanel buildDetailsCard() {
        JPanel card = new RoundedCard();
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(new EmptyBorder(23, 28, 17, 28));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel eyebrow = new JLabel("STEP 02  /  PLAYER DETAILS");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        eyebrow.setForeground(BLUE);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(eyebrow);
        header.add(Box.createVerticalStrut(7));

        JLabel title = new JLabel("Create Your Players");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(5));

        JLabel subtitle = new JLabel("Pick names & colors for your crew.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitle);
        card.add(header, BorderLayout.NORTH);

        JPanel players = new JPanel();
        players.setOpaque(false);
        players.setLayout(new BoxLayout(players, BoxLayout.Y_AXIS));

        for (int i = 0; i < numPlayers; i++) {
            JPanel row = buildPlayerRow(i);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            players.add(row);
            if (i < numPlayers - 1) players.add(Box.createVerticalStrut(8));
        }
        card.add(players, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(new Color(211, 65, 90));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(errorLabel);
        footer.add(Box.createVerticalStrut(6));

        JPanel actions = new JPanel(new GridLayout(1, 2, 11, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 51));
        actions.setPreferredSize(new Dimension(430, 51));

        JButton backButton = new ActionButton("Back", false);
        backButton.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            new GameSetupView().setVisible(true);
            if (window != null) window.dispose();
        });

        JButton startButton = new ActionButton("Start Game", true);
        startButton.addActionListener(e -> handleStartGame());
        actions.add(backButton);
        actions.add(startButton);
        footer.add(actions);
        footer.add(Box.createVerticalStrut(10));

        JLabel note = new JLabel("Each player needs a unique name and color.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setForeground(MUTED);
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(note);

        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    /**
     * SRS 3.2.1.3.1 -
     * Creates the name field and color selection controls for one player.
     */
    private JPanel buildPlayerRow(int index) {
        JPanel row = new PlayerCard();
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(new EmptyBorder(8, 12, 8, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 83));
        row.setPreferredSize(new Dimension(430, 83));

        JLabel number = new JLabel(String.format("%02d", index + 1), SwingConstants.CENTER);
        number.setFont(new Font("Segoe UI", Font.BOLD, 17));
        number.setForeground(PLAYER_COLORS[index].darker());
        number.setPreferredSize(new Dimension(38, 55));
        row.add(number, BorderLayout.WEST);

        JPanel nameBlock = new JPanel(new BorderLayout(0, 5));
        nameBlock.setOpaque(false);
        JLabel playerLabel = new JLabel("PLAYER " + (index + 1));
        playerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        playerLabel.setForeground(new Color(45, 53, 82));
        nameBlock.add(playerLabel, BorderLayout.NORTH);
        RoundedTextField field = new RoundedTextField("Enter name...");
        nameFields[index] = field;
        nameBlock.add(field, BorderLayout.CENTER);
        row.add(nameBlock, BorderLayout.CENTER);

        JPanel colorBlock = new JPanel(new BorderLayout(0, 5));
        colorBlock.setOpaque(false);
        colorBlock.setPreferredSize(new Dimension(148, 55));
        JLabel colorLabel = new JLabel("COLOR  /  " + COLOR_NAMES[index].toUpperCase());
        colorLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        colorLabel.setForeground(MUTED);
        colorBlock.add(colorLabel, BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(1, 4, 4, 0));
        choices.setOpaque(false);
        ColorChip[] chips = new ColorChip[COLOR_NAMES.length];
        JComboBox<String> selector = new JComboBox<>(COLOR_NAMES);
        selector.setSelectedIndex(index);
        colorSelectors[index] = selector;
        for (int c = 0; c < COLOR_NAMES.length; c++) {
            final String color = COLOR_NAMES[c];
            ColorChip chip = new ColorChip(PLAYER_COLORS[c], color);
            chip.addActionListener(e -> selector.setSelectedItem(color));
            choices.add(chip);
            chips[c] = chip;
        }
        selector.addActionListener(e -> {
            String selected = (String) selector.getSelectedItem();
            colorLabel.setText("COLOR  /  " + selected.toUpperCase());
            for (int c = 0; c < chips.length; c++) {
                chips[c].setSelectedColor(COLOR_NAMES[c].equals(selected));
            }
        });
        chips[index].setSelectedColor(true);
        colorBlock.add(choices, BorderLayout.CENTER);
        row.add(colorBlock, BorderLayout.EAST);
        return row;
    }

    /**
     * SRS 3.2.1.3.1 -
     * Collects the selected game settings and starts the game after
     * validation by GameSetupController.
     */
    private void handleStartGame() {
        errorLabel.setForeground(new Color(211, 65, 90));
        String[] names = new String[numPlayers];
        String[] colors = new String[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            names[i] = nameFields[i].getText().trim();
            colors[i] = (String) colorSelectors[i].getSelectedItem();
        }

        GameSetupController.ValidationError error = GameSetupController.validatePlayers(names, colors);
        if (error != null) {
            // SRS 3.2.1.3.2 - show the message and move the cursor to the wrong name field.
            errorLabel.setText(error.getMessage());
            if (error.getField() == GameSetupController.Field.NAME) {
                nameFields[error.getPlayerIndex()].requestFocusInWindow();
            }
            return;
        }

        errorLabel.setForeground(new Color(25, 150, 112));
        errorLabel.setText("Success! Loading game...");

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            frame.setTitle("Ludo - Game");
            frame.setContentPane(new GameView(names, colors, difficulty));
            frame.setSize(1150, 800);
            frame.setLocationRelativeTo(null);
            frame.revalidate();
            frame.repaint();
        }
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static class RoundedCard extends JPanel {
        RoundedCard() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(0, 0, 0, 28));
            g.fillRoundRect(4, 8, getWidth() - 7, getHeight() - 10, 28, 28);
            g.setColor(new Color(249, 250, 255));
            g.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 8, 27, 27);
            g.dispose();
        }
    }

    private static class PlayerCard extends JPanel {
        PlayerCard() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(239, 243, 254));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 17, 17);
            g.setColor(new Color(220, 227, 245));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 17, 17);
            g.dispose();
        }
    }

    private static class RoundedTextField extends JTextField {
        private final String placeholder;
        RoundedTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setForeground(NAVY);
            setCaretColor(BLUE);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 11, 11);
            g.setColor(isFocusOwner() ? BLUE : new Color(216, 224, 242));
            g.setStroke(new BasicStroke(isFocusOwner() ? 1.7f : 1f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 11, 11);
            g.dispose();
            super.paintComponent(graphics);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D hint = (Graphics2D) graphics.create();
                hint.setColor(new Color(154, 164, 183));
                hint.setFont(getFont());
                FontMetrics metrics = hint.getFontMetrics();
                hint.drawString(placeholder, 11, (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                hint.dispose();
            }
        }
    }

    private static class ColorChip extends JButton {
        private final Color color;
        private boolean selectedColor;
        private boolean hovered;
        ColorChip(Color color, String name) {
            this.color = color;
            setToolTipText(name);
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
        void setSelectedColor(boolean selected) {
            selectedColor = selected;
            repaint();
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            int w = getWidth(), h = getHeight();
            int diameter = Math.min(w, h) - 8;
            if (selectedColor || hovered) {
                g.setColor(selectedColor ? new Color(223, 231, 255) : new Color(233, 238, 251));
                g.fillRoundRect(1, 1, w - 2, h - 2, 12, 12);
            }
            g.setColor(color);
            g.fill(new Ellipse2D.Double((w - diameter) / 2.0,
                    (h - diameter) / 2.0, diameter, diameter));
            if (selectedColor) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2, cy = h / 2;
                g.drawLine(cx - 6, cy, cx - 1, cy + 5);
                g.drawLine(cx - 1, cy + 5, cx + 7, cy - 6);
            }
            if (isFocusOwner()) {
                g.setColor(BLUE);
                g.setStroke(new BasicStroke(1.6f));
                g.drawRoundRect(2, 2, w - 5, h - 5, 10, 10);
            }
            g.dispose();
        }
    }

    private static class ActionButton extends JButton {
        private final boolean primary;
        private boolean hovered;
        ActionButton(String text, boolean primary) {
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
            quality(g);
            int w = getWidth(), h = getHeight();
            if (primary) {
                g.setPaint(new GradientPaint(0, 0,
                        hovered ? new Color(103, 121, 255) : BLUE,
                        w, h, hovered ? new Color(157, 94, 246) : new Color(118, 76, 226)));
            } else {
                g.setColor(hovered ? new Color(229, 234, 253) : new Color(236, 241, 252));
            }
            g.fillRoundRect(1, 1, w - 2, h - 2, 17, 17);
            g.setColor(primary ? Color.WHITE : new Color(44, 57, 89));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    private class PlayerHero extends JPanel {
        PlayerHero() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(180, 193, 246));
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.drawString("YOUR CREW, YOUR STORY", 39, 80);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 44));
            g.drawString("The best games", 37, 139);
            g.drawString("start together.", 37, 196);

            g.setColor(new Color(206, 216, 251));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            g.drawString("Choose your teammates' names", 39, 247);
            g.drawString("and make every color count.", 39, 273);

            // Four Ludo pawns arranged as a team (vector drawing, no images).
            int[][] spots = {{145, 400}, {268, 381}, {190, 472}, {326, 481}};
            for (int i = 0; i < Math.min(4, numPlayers); i++) {
                int cx = spots[i][0], cy = spots[i][1];
                g.setColor(new Color(0, 0, 0, 30));
                g.fillOval(cx - 42, cy + 34, 84, 22);
                g.setColor(PLAYER_COLORS[i]);
                g.fillOval(cx - 22, cy - 46, 44, 44);
                g.fillRoundRect(cx - 17, cy - 14, 34, 58, 18, 18);
                g.fillRoundRect(cx - 36, cy + 28, 72, 21, 14, 14);
                g.setColor(new Color(255, 255, 255, 85));
                g.fillOval(cx - 14, cy - 39, 15, 10);
                g.setColor(new Color(255, 255, 255, 135));
                g.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String n = "" + (i + 1);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(n, cx - fm.stringWidth(n)/2, cy + 20);
            }
            g.dispose();
        }
    }
}
