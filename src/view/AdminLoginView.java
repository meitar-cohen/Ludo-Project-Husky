package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

/** Administrator entry screen. */
public class AdminLoginView extends JFrame {

    private static final Color NAVY = new Color(17, 27, 65);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(107, 120, 149);
    private static final Color ERROR = new Color(195, 48, 75);

    public AdminLoginView(Runnable onLoginSuccess) {
        setTitle("Ludo - Admin Access");
        setSize(1020, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = new GradientBackground();
        background.setLayout(new GridBagLayout());
        background.setBorder(new EmptyBorder(26, 26, 26, 26));

        JPanel layout = new JPanel(new GridLayout(1, 2, 30, 0));
        layout.setOpaque(false);
        layout.setPreferredSize(new Dimension(950, 570));
        layout.add(new HeroPanel());
        layout.add(createLoginCard(onLoginSuccess));

        background.add(layout);
        setContentPane(background);
    }

    private JPanel createLoginCard(Runnable onLoginSuccess) {
        JPanel card = new RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(39, 38, 36, 42));

        LockBadge lockBadge = new LockBadge();
        lockBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        lockBadge.setPreferredSize(new Dimension(60, 60));
        lockBadge.setMaximumSize(new Dimension(60, 60));

        JLabel eyebrow = label("RESTRICTED ACCESS", 12, Font.BOLD, BLUE);
        JLabel title = label("Welcome back", 32, Font.BOLD, NAVY);
        JLabel subtitle = label("Sign in to manage game questions.", 15, Font.PLAIN, MUTED);

        JLabel passwordLabel = label("ADMIN PASSWORD", 12, Font.BOLD, NAVY);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        passwordField.setForeground(NAVY);
        passwordField.setOpaque(false);
        passwordField.setBorder(new EmptyBorder(8, 12, 8, 5));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        char defaultEcho = passwordField.getEchoChar();

        JButton showPassword = new JButton("Show");
        showPassword.setFont(new Font("Segoe UI", Font.BOLD, 12));
        showPassword.setForeground(BLUE);
        showPassword.setContentAreaFilled(false);
        showPassword.setFocusPainted(false);
        showPassword.setBorder(new EmptyBorder(6, 8, 6, 12));
        showPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPassword.addActionListener(e -> {
            boolean hidden = passwordField.getEchoChar() != (char) 0;
            passwordField.setEchoChar(hidden ? (char) 0 : defaultEcho);
            showPassword.setText(hidden ? "Hide" : "Show");
            passwordField.requestFocusInWindow();
        });

        JPanel fieldBox = new RoundedFieldPanel();
        fieldBox.setLayout(new BorderLayout());
        fieldBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        fieldBox.setPreferredSize(new Dimension(360, 52));
        fieldBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldBox.add(passwordField, BorderLayout.CENTER);
        fieldBox.add(showPassword, BorderLayout.EAST);

        JLabel errorLabel = label(" ", 13, Font.PLAIN, ERROR);
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        RoundedButton loginButton = new RoundedButton("Login to Dashboard", true);
        RoundedButton backButton = new RoundedButton("Back to Main Menu", false);

        loginButton.addActionListener(e -> {
            char[] enteredPassword = passwordField.getPassword();
            if (enteredPassword.length == 0) {
                errorLabel.setText("Please enter a password.");
            } else {
                if (onLoginSuccess != null) {
                    onLoginSuccess.run();
                }
                dispose();
            }
            Arrays.fill(enteredPassword, '\0');
        });

        backButton.addActionListener(e -> {
            new MainMenuView().setVisible(true);
            dispose();
        });

        // Enter submits the password, including when the password field has focus.
        getRootPane().setDefaultButton(loginButton);

        JLabel demoLabel = label("ADMIN ACCESS", 11, Font.BOLD, BLUE);
        JLabel hintLabel = label("Enter your password to continue.", 14, Font.PLAIN, MUTED);
        JPanel hint = new RoundedHintPanel();
        hint.setLayout(new BoxLayout(hint, BoxLayout.Y_AXIS));
        hint.setBorder(new EmptyBorder(13, 16, 13, 16));
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.add(demoLabel);
        hint.add(Box.createVerticalStrut(4));
        hint.add(hintLabel);

        card.add(lockBadge);
        card.add(Box.createVerticalStrut(17));
        card.add(eyebrow);
        card.add(Box.createVerticalStrut(7));
        card.add(title);
        card.add(Box.createVerticalStrut(7));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldBox);
        card.add(Box.createVerticalStrut(8));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(13));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(11));
        card.add(backButton);
        card.add(Box.createVerticalStrut(22));
        card.add(hint);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private static JLabel label(String text, int fontSize, int fontStyle, Color color) {
        JLabel result = new JLabel(text);
        result.setFont(new Font("Segoe UI", fontStyle, fontSize));
        result.setForeground(color);
        result.setAlignmentX(Component.LEFT_ALIGNMENT);
        return result;
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static class GradientBackground extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setPaint(new GradientPaint(0, 0, new Color(14, 22, 54),
                    getWidth(), getHeight(), new Color(43, 39, 100)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(116, 106, 236, 39));
            g.fillOval(-200, -190, 650, 650);
            g.setColor(new Color(75, 177, 245, 25));
            g.fillOval(570, 310, 560, 560);
            g.dispose();
        }
    }

    private static class HeroPanel extends JPanel {
        HeroPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(168, 182, 245));
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.drawString("LUDO  /  ADMIN AREA", 28, 81);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 43));
            g.drawString("Keep the", 26, 157);
            g.drawString("game smart.", 26, 209);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 17));
            g.setColor(new Color(205, 216, 245));
            g.drawString("Your question bank, one place.", 30, 258);
            g.drawString("Create. Review. Improve.", 30, 286);

            // A padlock illustration, without external images or logos.
            int x = 97, y = 326;
            g.setColor(new Color(4, 10, 29, 50));
            g.fillRoundRect(x + 14, y + 95, 185, 139, 40, 40);
            g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(192, 208, 255));
            g.drawArc(x + 54, y, 104, 136, 0, 180);
            g.setColor(new Color(105, 134, 255));
            g.fillRoundRect(x + 12, y + 95, 185, 133, 36, 36);
            g.setColor(new Color(255, 255, 255, 40));
            g.fillRoundRect(x + 18, y + 101, 172, 40, 30, 30);
            g.setColor(Color.WHITE);
            g.fillOval(x + 94, y + 134, 24, 24);
            g.fillRoundRect(x + 101, y + 150, 10, 30, 7, 7);
            g.dispose();
        }
    }

    private static class RoundedCard extends JPanel {
        RoundedCard() { setOpaque(false); }
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(4, 8, 30, 58));
            g.fill(new RoundRectangle2D.Double(6, 11, getWidth() - 12, getHeight() - 19, 38, 38));
            g.setColor(new Color(251, 252, 255));
            g.fill(new RoundRectangle2D.Double(1, 2, getWidth() - 12, getHeight() - 19, 38, 38));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class RoundedFieldPanel extends JPanel {
        RoundedFieldPanel() { setOpaque(false); }
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(242, 245, 253));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.setColor(new Color(209, 219, 241));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class RoundedHintPanel extends JPanel {
        RoundedHintPanel() { setOpaque(false); }
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(237, 241, 255));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class LockBadge extends JComponent {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(new Color(231, 236, 255));
            g.fillRoundRect(0, 0, 58, 58, 17, 17);
            g.setColor(BLUE);
            g.setStroke(new BasicStroke(3.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(19, 13, 20, 23, 0, 180);
            g.fillRoundRect(17, 27, 25, 20, 6, 6);
            g.setColor(Color.WHITE);
            g.fillOval(28, 33, 5, 5);
            g.dispose();
        }
    }

    private static class RoundedButton extends JButton {
        private final boolean primary;
        private boolean hovered;

        RoundedButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setForeground(primary ? Color.WHITE : BLUE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
            setPreferredSize(new Dimension(360, 49));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            if (primary) {
                g.setPaint(new GradientPaint(0, 0,
                        hovered ? new Color(91, 126, 255) : BLUE,
                        getWidth(), getHeight(), new Color(89, 78, 213)));
            } else {
                g.setColor(hovered ? new Color(229, 235, 254) : new Color(239, 242, 250));
            }
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
