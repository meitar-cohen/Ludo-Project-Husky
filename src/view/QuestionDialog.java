package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;


public class QuestionDialog extends JDialog {

    static final String QUESTION =
        "Which document defines the software requirements according to IEEE Std 830?";
    static final String[] ANSWERS = {
        "SRS - Software Requirements Specification",
        "SDP - Software Development Plan",
        "STD - Software Test Document",
        "SDD - Software Design Description"
    };
    static final int CORRECT_INDEX = 0;

    static final Color NAVY = new Color(18, 27, 61);
    static final Color PRIMARY = new Color(75, 110, 248);
    static final Color MUTED = new Color(111, 125, 153);
    static final Color SUCCESS = new Color(27, 148, 107);
    static final Color ERROR = new Color(207, 70, 87);

    private int selected = -1;
    private boolean correct = false;
    private final AnswerButton[] answerButtons = new AnswerButton[ANSWERS.length];

    public QuestionDialog(Window owner) {
        super(owner, "Ludo - Question Challenge", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        JPanel root = new QuestionBackgroundPanel();
        root.setLayout(new GridBagLayout());
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setPreferredSize(new Dimension(710, 680));
        QuestionCardPanel card = new QuestionCardPanel(Color.WHITE, 27);
        card.setLayout(new BorderLayout(0, 0));
        card.setPreferredSize(new Dimension(650, 640));
        card.setBorder(new EmptyBorder(20, 31, 16, 31));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel topLine = new JPanel(new BorderLayout());
        topLine.setOpaque(false);
        topLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tag = new JLabel("LUDO   /   QUESTION SQUARE");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tag.setForeground(PRIMARY);
        topLine.add(tag, BorderLayout.WEST);
        JLabel level = new JLabel("  EASY  ");
        level.setOpaque(true);
        level.setBackground(new Color(225, 247, 235));
        level.setForeground(SUCCESS);
        level.setFont(new Font("Segoe UI", Font.BOLD, 12));
        level.setBorder(new EmptyBorder(4, 8, 4, 8));
        topLine.add(level, BorderLayout.EAST);
        content.add(topLine);
        content.add(Box.createVerticalStrut(9));
        JLabel title = new JLabel("Question time!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 31));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(5));
        JLabel subtitle = new JLabel("Choose carefully - your next move depends on it.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(14));
        QuestionCardPanel questionCard = new QuestionCardPanel(new Color(241, 245, 255), 17);
        questionCard.setLayout(new BorderLayout());
        questionCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        questionCard.setPreferredSize(new Dimension(582, 87));
        questionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 87));
        JTextArea prompt = new JTextArea(QUESTION);
        prompt.setEditable(false);
        prompt.setFocusable(false);
        prompt.setLineWrap(true);
        prompt.setWrapStyleWord(true);
        prompt.setOpaque(false);
        prompt.setBorder(null);
        prompt.setFont(new Font("Segoe UI", Font.BOLD, 17));
        prompt.setForeground(NAVY);
        questionCard.add(prompt, BorderLayout.CENTER);
        questionCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(questionCard);
        content.add(Box.createVerticalStrut(14));
        JLabel choose = new JLabel("SELECT ONE ANSWER");
        choose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        choose.setForeground(MUTED);
        choose.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(choose);
        content.add(Box.createVerticalStrut(9));
        ActionButton confirm = new ActionButton("Check answer", PRIMARY);
        confirm.setEnabled(false);
        ActionButton cont = new ActionButton("Continue", SUCCESS);
        cont.setVisible(false);
        for (int i = 0; i < ANSWERS.length; i++) {
            final int idx = i;
            AnswerButton button = new AnswerButton(i, ANSWERS[i]);
            answerButtons[i] = button;
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener(e -> {
                selected = idx;
                for (int j = 0; j < answerButtons.length; j++) {
                    answerButtons[j].setVisualState(j == selected ? 1 : 0);
                }
                confirm.setEnabled(true);
            });
            content.add(button);
            if (i != ANSWERS.length - 1) content.add(Box.createVerticalStrut(7));
        }
        content.add(Box.createVerticalStrut(11));
        QuestionCardPanel feedbackPanel = new QuestionCardPanel(new Color(241, 245, 255), 15);
        feedbackPanel.setLayout(new BorderLayout());
        feedbackPanel.setBorder(new EmptyBorder(11, 14, 11, 14));
        feedbackPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        feedbackPanel.setPreferredSize(new Dimension(582, 56));
        feedbackPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel feedback = new JLabel("Select an answer to check your next move.");
        feedback.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        feedback.setForeground(new Color(67, 83, 126));
        feedbackPanel.add(feedback, BorderLayout.CENTER);
        content.add(feedbackPanel);
        card.add(content, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        JLabel rule = new JLabel("Correct: +1 square    |    Wrong: -1 square");
        rule.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rule.setForeground(MUTED);
        footer.add(rule, BorderLayout.WEST);
        JPanel actions = new JPanel(new CardLayout());
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(172, 47));
        actions.add(confirm, "CHECK");
        actions.add(cont, "CONTINUE");
        footer.add(actions, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        confirm.addActionListener(e -> {
            if (selected < 0) return;
            correct = selected == CORRECT_INDEX;
            for (int i = 0; i < answerButtons.length; i++) {
                int state = i == CORRECT_INDEX ? 2 : (i == selected ? 3 : 4);
                answerButtons[i].setVisualState(state);
                answerButtons[i].setEnabled(false);
            }
            if (correct) {
                feedback.setText("Correct! Your piece moves 1 square forward.");
                feedback.setForeground(SUCCESS);
                feedbackPanel.setFill(new Color(229, 249, 239));
            } else {
                feedback.setText("Not quite. The correct answer is A (SRS). Move 1 square back.");
                feedback.setForeground(ERROR);
                feedbackPanel.setFill(new Color(255, 237, 240));
            }
            ((CardLayout) actions.getLayout()).show(actions, "CONTINUE");
            getRootPane().setDefaultButton(cont);
        });
        cont.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(confirm);
        root.add(card);
        setContentPane(root);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isCorrect() {
        return correct;
    }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static class ActionButton extends JButton {
        private final Color fill;
        ActionButton(String label, Color fill) {
            super(label);
            this.fill = fill;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            Color base = isEnabled() ? fill : new Color(183, 194, 217);
            if (getModel().isRollover() && isEnabled()) base = base.brighter();
            if (getModel().isPressed() && isEnabled()) base = base.darker();
            g.setColor(base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    static class AnswerButton extends JButton {
        private final int index;
        private final String answer;
        private int visualState; // 0 normal, 1 selected, 2 correct, 3 incorrect, 4 dimmed
        AnswerButton(int index, String answer) {
            super();
            this.index = index;
            this.answer = answer;
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(582, 57));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 57));
            getAccessibleContext().setAccessibleName("Answer " + (char)('A' + index) + ": " + answer);
        }
        void setVisualState(int state) { visualState = state; repaint(); }
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            Color bg = Color.WHITE;
            Color border = new Color(220, 227, 242);
            Color letterBg = new Color(235, 241, 252);
            Color letterFg = new Color(85, 106, 158);
            Color text = NAVY;
            if (visualState == 1) {
                bg = new Color(239, 243, 255); border = PRIMARY;
                letterBg = PRIMARY; letterFg = Color.WHITE;
            } else if (visualState == 2) {
                bg = new Color(235, 251, 242); border = SUCCESS;
                letterBg = SUCCESS; letterFg = Color.WHITE;
            } else if (visualState == 3) {
                bg = new Color(255, 239, 242); border = QuestionDialog.ERROR;
                letterBg = QuestionDialog.ERROR; letterFg = Color.WHITE;
            } else if (visualState == 4) {
                bg = new Color(249, 250, 253); border = new Color(233, 238, 247);
                text = new Color(157, 167, 187);
            } else if (getModel().isRollover()) {
                bg = new Color(247, 249, 255); border = new Color(151, 171, 248);
            }
            g.setColor(bg);
            g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            g.setColor(border);
            g.setStroke(new BasicStroke(visualState == 1 ? 2f : 1.1f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            g.setColor(letterBg);
            g.fillRoundRect(13, 11, 35, 35, 11, 11);
            g.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g.setColor(letterFg);
            FontMetrics lm = g.getFontMetrics();
            String letter = String.valueOf((char)('A' + index));
            g.drawString(letter, 13 + (35 - lm.stringWidth(letter)) / 2,
                11 + (35 + lm.getAscent() - lm.getDescent()) / 2);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            g.setColor(text);
            FontMetrics fm = g.getFontMetrics();
            String display = answer;
            int maxWidth = Math.max(0, getWidth() - 86);
            if (fm.stringWidth(display) > maxWidth) {
                while (display.length() > 1 && fm.stringWidth(display + "...") > maxWidth) {
                    display = display.substring(0, display.length() - 1);
                }
                display += "...";
            }
            g.drawString(display, 64, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    private static class QuestionBackgroundPanel extends JPanel {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                QuestionDialog.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(14, 22, 54),
                    getWidth(), getHeight(), new Color(47, 41, 112)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(115, 113, 239, 47));
                g.fillOval(-110, -110, 360, 360);
                g.setColor(new Color(92, 156, 242, 34));
                g.fillOval(360, 310, 440, 440);
                g.dispose();
            }
    }

    private static class QuestionCardPanel extends JPanel {
            private Color fill;
            private final int radius;
            QuestionCardPanel(Color fill, int radius) {
                this.fill = fill;
                this.radius = radius;
                setOpaque(false);
            }
            void setFill(Color fill) { this.fill = fill; repaint(); }
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                QuestionDialog.quality(g);
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
                g.dispose();
                super.paintComponent(graphics);
            }
    }
}
