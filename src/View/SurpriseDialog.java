package View;

import javax.swing.*;
import java.awt.*;

/** Modal dialog shown when a piece lands on a surprise square. */
class SurpriseDialog extends JDialog {

    SurpriseDialog(Window owner) {
        super(owner, "Ludo - Surprise", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setSize(510, 390);
        setResizable(false);

        JPanel background = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(
                        0, 0, new Color(29, 31, 78),
                        getWidth(), getHeight(), new Color(72, 57, 130)
                ));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GameScreen.quality(g2);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(446, 334));
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel tag = centeredLabel(
                "SURPRISE SQUARE", 13, Font.BOLD,
                new Color(108, 78, 198)
        );
        JLabel title = centeredLabel(
                "A LITTLE DETOUR!", 28, Font.BOLD,
                new Color(30, 37, 79)
        );
        JLabel subtitle = centeredLabel(
                "You've landed on a surprise square.", 14, Font.PLAIN,
                new Color(103, 111, 136)
        );

        JPanel eventCard = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GameScreen.quality(g2);
                g2.setColor(new Color(242, 237, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        eventCard.setOpaque(false);
        eventCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        eventCard.setPreferredSize(new Dimension(386, 88));
        eventCard.setMaximumSize(new Dimension(386, 88));
        JPanel eventText = new JPanel();
        eventText.setOpaque(false);
        eventText.setLayout(new BoxLayout(eventText, BoxLayout.Y_AXIS));
        eventText.add(centeredLabel(
                "-3 SQUARES", 27, Font.BOLD,
                new Color(105, 70, 199)
        ));
        eventText.add(Box.createVerticalStrut(4));
        eventText.add(centeredLabel(
                "Move your piece three squares back", 13, Font.PLAIN,
                new Color(91, 95, 119)
        ));
        eventCard.add(eventText);

        JButton continueButton = new JButton("CONTINUE GAME") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GameScreen.quality(g2);
                g2.setColor(getModel().isRollover()
                        ? new Color(86, 77, 218)
                        : new Color(77, 104, 241));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        continueButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        continueButton.setForeground(Color.WHITE);
        continueButton.setFocusPainted(false);
        continueButton.setOpaque(false);
        continueButton.setContentAreaFilled(false);
        continueButton.setBorderPainted(false);
        continueButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueButton.setPreferredSize(new Dimension(288, 46));
        continueButton.setMaximumSize(new Dimension(288, 46));
        continueButton.addActionListener(e -> dispose());

        card.add(tag);
        card.add(Box.createVerticalStrut(12));
        card.add(title);
        card.add(Box.createVerticalStrut(7));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(18));
        card.add(eventCard);
        card.add(Box.createVerticalStrut(22));
        card.add(continueButton);
        background.add(card);
        setContentPane(background);
        getRootPane().setDefaultButton(continueButton);
        setLocationRelativeTo(owner);
    }

    private JLabel centeredLabel(
            String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }
}
