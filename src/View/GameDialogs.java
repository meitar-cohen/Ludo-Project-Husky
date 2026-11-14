package View;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class GameDialogs {

    private static final Color INK = new Color(31, 42, 98);
    private static final Color PRIMARY = new Color(76, 104, 241);
    private static final Color BORDER = new Color(208, 214, 255);
    private static final Color WHITE = Color.WHITE;

    private GameDialogs() {
    }

    /**
     * Shows the choice after rolling a 6.
     * Returns 0 for castle, 1 for an existing board piece, -1 when closed.
     */
    public static int chooseMove(Component parent) {
        final JDialog dialog = createDialog(parent, 640, 276);
        final int[] choice = {-1};
        JPanel card = createCard(dialog, "Choose your move");
        card.add(messageRow("6", "You rolled a 6! Choose your move:", false), BorderLayout.CENTER);
        JPanel buttons = buttonRow();
        JButton takeOut = new PillButton("Take out a piece", true);
        takeOut.addActionListener(e -> {
            choice[0] = 0;
            dialog.dispose();
        });
        JButton moveOnBoard = new PillButton("Move a piece on board", false);
        moveOnBoard.addActionListener(e -> {
            choice[0] = 1;
            dialog.dispose();
        });
        buttons.add(takeOut);
        buttons.add(moveOnBoard);
        card.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(card);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return choice[0];
    }

    /** Returns true only when the user explicitly confirms leaving. */
    public static boolean confirmLeave(Component parent) {
        final JDialog dialog = createDialog(parent, 590, 286);
        final boolean[] confirmed = {false};
        JPanel card = createCard(dialog, "Leave game");
        card.add(messageRow("!", "<html>Leave the game? An unfinished game<br>is not saved to the history.</html>", true),
                BorderLayout.CENTER);
		JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
		buttons.setOpaque(false);

        JButton yes = new PillButton("Yes, leave game", true);
		yes.addActionListener(e -> {
		    confirmed[0] = true;
		    dialog.dispose();
		});

        JButton no = new PillButton("No, keep playing", false);
		no.addActionListener(e -> dialog.dispose());
		buttons.add(yes);
		buttons.add(no);
		card.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(card);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return confirmed[0];
    }

    private static JDialog createDialog(Component parent, int width, int height) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        final JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        dialog.setResizable(false);
        try {
            dialog.setBackground(new Color(0, 0, 0, 0));
        } catch (UnsupportedOperationException ignored) {
            dialog.setBackground(WHITE);
        }
        dialog.getRootPane().registerKeyboardAction(
                (ActionEvent e) -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        return dialog;
    }

    private static JPanel createCard(JDialog dialog, String title) {
        JPanel card = new JPanel(new BorderLayout(4, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                antiAlias(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(17, 26, 75, 45));
                g2.fillRoundRect(9, 10, w - 14, h - 15, 35, 35);
                g2.setPaint(new GradientPaint(4, 4, WHITE, w, h, new Color(244, 245, 255)));
                g2.fillRoundRect(3, 3, w - 13, h - 15, 34, 34);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(3, 3, w - 13, h - 15, 34, 34);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(25, 35, 30, 39));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setForeground(INK);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 27));
        header.add(heading, BorderLayout.WEST);
        JButton close = new JButton("\u00d7");
        close.setForeground(new Color(100, 111, 162));
        close.setFont(new Font("Segoe UI", Font.PLAIN, 29));
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setPreferredSize(new Dimension(36, 34));
        close.addActionListener(e -> dialog.dispose());
        header.add(close, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private static JPanel messageRow(String symbol, String text, boolean warning) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 17, 7));
        row.setOpaque(false);
        row.add(new RoundBadge(symbol, warning));
        JLabel message = new JLabel(text);
        message.setForeground(INK);
        message.setFont(new Font("Segoe UI", Font.BOLD, 17));
        row.add(message);
        return row;
    }

    private static JPanel buttonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);
        return row;
    }

    private static void antiAlias(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static final class RoundBadge extends JComponent {
        private final String symbol;
        private final boolean warning;
        RoundBadge(String symbol, boolean warning) {
            this.symbol = symbol;
            this.warning = warning;
            setPreferredSize(new Dimension(66, 66));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antiAlias(g2);
            Color background = warning ? new Color(255, 236, 187) : new Color(229, 233, 255);
            Color foreground = warning ? new Color(184, 117, 21) : PRIMARY;
            g2.setColor(background);
            g2.fillOval(2, 2, 62, 62);
            g2.setColor(foreground);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 34));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(symbol)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(symbol, x, y);
            g2.dispose();
        }
    }

    private static final class PillButton extends JButton {
        private final boolean primary;
        PillButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setPreferredSize(new Dimension(250, 50));
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setForeground(primary ? WHITE : INK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antiAlias(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(60, 72, 142, 20));
            g2.fillRoundRect(3, 4, w - 6, h - 4, 22, 22);
            if (primary) {
                g2.setPaint(new GradientPaint(0, 0,
                        getModel().isRollover() ? new Color(65, 135, 254) : new Color(58, 130, 252),
                        0, h, new Color(75, 87, 231)));
            } else {
                g2.setPaint(new GradientPaint(0, 0, WHITE, 0, h,
                        getModel().isRollover() ? new Color(229, 232, 255) : new Color(239, 241, 255)));
            }
            g2.fillRoundRect(2, 1, w - 6, h - 6, 21, 21);
            g2.setStroke(new BasicStroke(1.6f));
            g2.setColor(primary ? new Color(92, 151, 255) : new Color(153, 151, 250));
            g2.drawRoundRect(2, 1, w - 6, h - 6, 21, 21);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
