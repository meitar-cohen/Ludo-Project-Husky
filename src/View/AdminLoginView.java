package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import Controller.AdminAuthController;

/**
 * כניסה למנהל: בהרצה ראשונה הגדרת סיסמה, ובהרצות הבאות אימות.
 * אין סיסמת דמו קבועה בקוד, וסיסמה לא נשמרת ב-GitHub.
 */
public final class AdminLoginView extends JFrame {

    private static final Color NAVY = new Color(18, 27, 61);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 125, 153);
    private static final Color RED = new Color(198, 63, 82);

    private final AdminAuthController auth;
    private final boolean setupMode;
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmationField = new JPasswordField();
    private final JLabel message = new JLabel(" ");

    public AdminLoginView() {
        this(new AdminAuthController());
    }

    // בנאי לבדיקה, מאפשר שימוש במאגר אימות זמני.
    public AdminLoginView(AdminAuthController auth) {
        if (auth == null) {
            throw new IllegalArgumentException("Authentication controller is required");
        }
        this.auth = auth;
        this.setupMode = auth.requiresSetup();

        setTitle("Ludo - Admin Access");
        MainMenuScreen.applyAppIcon(this);
        setSize(1050, 680);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                returnToMenu();
            }
        });

        JPanel background = new GradientPanel();
        background.setLayout(new GridBagLayout());
        background.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel columns = new JPanel(new GridLayout(1, 2, 28, 0));
        columns.setOpaque(false);
        columns.setPreferredSize(new Dimension(960, 560));
        columns.add(new IntroPanel());
        columns.add(buildLoginCard());
        background.add(columns);
        setContentPane(background);
    }

    private JPanel buildLoginCard() {
        JPanel card = new RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(37, 34, 32, 34));

        JLabel badge = label("RESTRICTED ACCESS  /  ADMIN", 12, Font.BOLD, BLUE);
        JLabel title = label(setupMode ? "Set admin access" : "Welcome back",
                30, Font.BOLD, NAVY);
        JLabel subtitle = label(setupMode
                ? "Choose a password for managing questions."
                : "Sign in to manage game questions.", 14, Font.PLAIN, MUTED);
        JLabel account = label("ACCOUNT  /  ADMINISTRATOR", 12, Font.BOLD, MUTED);
        JLabel passwordLabel = label(setupMode ? "CREATE PASSWORD" : "ADMIN PASSWORD",
                12, Font.BOLD, NAVY);

        stylePasswordField(passwordField);
        stylePasswordField(confirmationField);
        char defaultEcho = passwordField.getEchoChar();

        JPanel passwordBox = fieldPanel(passwordField);
        JPanel confirmBox = fieldPanel(confirmationField);
        JLabel confirmLabel = label("CONFIRM PASSWORD", 12, Font.BOLD, NAVY);

        JButton showPassword = new JButton("Show password");
        showPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        showPassword.setForeground(BLUE);
        showPassword.setBorderPainted(false);
        showPassword.setContentAreaFilled(false);
        showPassword.setFocusPainted(false);
        showPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPassword.addActionListener(event -> {
            boolean visible = passwordField.getEchoChar() == (char) 0;
            passwordField.setEchoChar(visible ? defaultEcho : (char) 0);
            confirmationField.setEchoChar(visible ? defaultEcho : (char) 0);
            showPassword.setText(visible ? "Show password" : "Hide password");
        });

        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        message.setForeground(RED);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JButton enter = actionButton(setupMode
                ? "Create password & Continue" : "Login to Dashboard", true);
        JButton back = actionButton("Back to Main Menu", false);
        enter.addActionListener(event -> submit());
        back.addActionListener(event -> returnToMenu());
        getRootPane().setDefaultButton(enter);

        card.add(badge);
        card.add(Box.createVerticalStrut(18));
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));
        card.add(account);
        card.add(Box.createVerticalStrut(19));
        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(passwordBox);
        if (setupMode) {
            card.add(Box.createVerticalStrut(16));
            card.add(confirmLabel);
            card.add(Box.createVerticalStrut(8));
            card.add(confirmBox);
        }
        card.add(Box.createVerticalStrut(3));
        card.add(showPassword);
        card.add(message);
        card.add(Box.createVerticalStrut(3));
        card.add(enter);
        card.add(Box.createVerticalStrut(10));
        card.add(back);
        card.add(Box.createVerticalGlue());
        JLabel hint = label(setupMode
                ? "Use at least 10 characters. Keep your password safe."
                : "Access is limited to the local administrator.",
                12, Font.PLAIN, MUTED);
        card.add(hint);
        return card;
    }

    private void submit() {
        char[] password = passwordField.getPassword();
        char[] confirmation = setupMode ? confirmationField.getPassword() : new char[0];
        try {
            if (setupMode) {
                auth.setupPassword(password, confirmation);
            } else if (!auth.authenticate(password)) {
                message.setText("Incorrect password. Please try again.");
                return;
            }

            // גישה למסך הניהול רק אחרי הגדרת סיסמה או אימות מוצלח.
            QuestionManagementView management = new QuestionManagementView();
            management.setVisible(true);
            dispose();
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            message.setText("<html>" + escapeHtml(exception.getMessage()) + "</html>");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            passwordField.setText("");
            confirmationField.setText("");
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "Unable to open question management";
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void returnToMenu() {
        new MainMenuScreen().setVisible(true);
        dispose();
    }

    private static JPanel fieldPanel(JPasswordField field) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 220, 245), 1, true),
                new EmptyBorder(0, 4, 0, 4)));
        wrapper.add(field, BorderLayout.CENTER);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        wrapper.setPreferredSize(new Dimension(365, 49));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    private static void stylePasswordField(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        field.setForeground(NAVY);
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(10, 12, 10, 12));
    }

    private static JLabel label(String value, int size, int weight, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", weight, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton actionButton(String value, boolean primary) {
        JButton button = new JButton(value);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(primary ? Color.WHITE : BLUE);
        button.setBackground(primary ? BLUE : new Color(240, 244, 255));
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49));
        button.setPreferredSize(new Dimension(365, 49));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private static void quality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static final class GradientPanel extends JPanel {
        @Override protected void paintComponent(Graphics canvas) {
            super.paintComponent(canvas);
            Graphics2D graphics = (Graphics2D) canvas.create();
            quality(graphics);
            graphics.setPaint(new GradientPaint(0, 0, new Color(13, 23, 58),
                    getWidth(), getHeight(), new Color(44, 38, 104)));
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.setColor(new Color(120, 112, 246, 43));
            graphics.fillOval(-140, -170, 580, 580);
            graphics.fillOval(650, 310, 480, 480);
            graphics.dispose();
        }
    }

    private static final class IntroPanel extends JPanel {
        private IntroPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics canvas) {
            super.paintComponent(canvas);
            Graphics2D graphics = (Graphics2D) canvas.create();
            quality(graphics);
            graphics.setColor(new Color(168, 183, 247));
            graphics.setFont(new Font("Segoe UI", Font.BOLD, 13));
            graphics.drawString("LUDO  /  ADMIN ACCESS", 20, 95);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("Segoe UI", Font.BOLD, 43));
            graphics.drawString("Keep the", 18, 186);
            graphics.drawString("game smart.", 18, 238);
            graphics.setColor(new Color(210, 220, 250));
            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 17));
            graphics.drawString("Your questions. Your rules.", 20, 285);
            graphics.drawString("Manage a better game.", 20, 318);
            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 120));
            graphics.setColor(new Color(145, 168, 248));
            graphics.drawString("\u25a3", 95, 488);
            graphics.dispose();
        }
    }

    private static final class RoundedCard extends JPanel {
        private RoundedCard() { setOpaque(false); }
        @Override protected void paintComponent(Graphics canvas) {
            Graphics2D graphics = (Graphics2D) canvas.create();
            quality(graphics);
            graphics.setColor(Color.WHITE);
            graphics.fillRoundRect(0, 0, getWidth() - 2,
                    getHeight() - 2, 30, 30);
            graphics.dispose();
            super.paintComponent(canvas);
        }
    }
}
