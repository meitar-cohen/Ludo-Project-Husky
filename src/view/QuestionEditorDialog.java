package View;

import Model.Question;
import Enums.ButtonStyle;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Dialog for adding or editing a multiple-choice question. */
class QuestionEditorDialog extends JDialog {

    private static final Color NAVY = new Color(20, 28, 70);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(108, 121, 150);
    private static final Color RED = new Color(204, 69, 89);
    private static final Color BORDER = new Color(225, 231, 245);

    QuestionEditorDialog(
            Window owner, Question existing, Consumer<Question> onSave) {
        super(
                owner,
                existing == null ? "Add New Question" : "Edit Question",
                ModalityType.APPLICATION_MODAL
        );
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(680, 715);
        setResizable(false);
        setLocationRelativeTo(owner);

        JPanel outer = new QuestionEditorBackgroundPanel();
        outer.setLayout(new BorderLayout());
        outer.setBorder(new EmptyBorder(15, 15, 15, 15));
        QuestionManagementScreen.QuestionManagementSurfacePanel card =
                new QuestionManagementScreen.QuestionManagementSurfacePanel(
                        Color.WHITE, 26);
        card.setBorder(new EmptyBorder(25, 28, 25, 28));
        card.setLayout(new BorderLayout(0, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(label(
                "QUESTION BANK  /  "
                        + (existing == null ? "NEW" : "EDIT"),
                12, Font.BOLD, BLUE
        ));
        header.add(Box.createVerticalStrut(4));
        header.add(label(
                existing == null ? "Add a new question" : "Edit question",
                30, Font.BOLD, NAVY
        ));
        header.add(Box.createVerticalStrut(5));
        header.add(label(
                "Enter the question and all four possible answers.",
                14, Font.PLAIN, MUTED
        ));
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 7, 0);

        JTextField questionField =
                field(existing == null ? "" : existing.getText());
        addFormRow(form, gbc, "QUESTION", questionField, 0);
        JTextField[] answerFields = new JTextField[4];
        for (int i = 0; i < answerFields.length; i++) {
            answerFields[i] = field(
                    existing == null ? "" : existing.getOptions()[i]);
            addFormRow(form, gbc, "ANSWER " + (i + 1), answerFields[i], i + 1);
        }

        JPanel selectors = new JPanel(new GridLayout(1, 2, 16, 0));
        selectors.setOpaque(false);
        JComboBox<String> correctBox = new JComboBox<>(
                new String[]{"Answer 1", "Answer 2", "Answer 3", "Answer 4"});
        JComboBox<String> difficultyBox = new JComboBox<>(
                new String[]{"Easy", "Medium", "Hard"});
        styleCombo(correctBox);
        styleCombo(difficultyBox);
        if (existing != null) {
            correctBox.setSelectedIndex(existing.getCorrectIndex());
            difficultyBox.setSelectedItem(existing.getDifficulty());
        }
        selectors.add(labeledInput("CORRECT ANSWER", correctBox));
        selectors.add(labeledInput("DIFFICULTY", difficultyBox));
        gbc.gridy = 5;
        gbc.insets = new Insets(7, 0, 0, 0);
        form.add(selectors, gbc);
        card.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        JLabel error = label(" ", 13, Font.PLAIN, RED);
        footer.add(error, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 11, 0));
        actions.setOpaque(false);
        QuestionManagementScreen.RoundedButton cancel =
                new QuestionManagementScreen.RoundedButton(
                "Cancel", ButtonStyle.SECONDARY);
        QuestionManagementScreen.RoundedButton save =
                new QuestionManagementScreen.RoundedButton(
                existing == null ? "Add Question" : "Save Changes",
                ButtonStyle.PRIMARY
        );
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            String text = questionField.getText().trim();
            String[] options = new String[4];
            boolean valid = !text.isEmpty();
            for (int i = 0; i < answerFields.length; i++) {
                options[i] = answerFields[i].getText().trim();
                if (options[i].isEmpty()) {
                    valid = false;
                }
            }
            if (!valid) {
                error.setText(
                        "Please fill in the question and all four answers.");
                return;
            }
            onSave.accept(new Question(
                    text,
                    options,
                    correctBox.getSelectedIndex(),
                    (String) difficultyBox.getSelectedItem()
            ));
            dispose();
        });
        actions.add(cancel);
        actions.add(save);
        footer.add(actions, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);
        outer.add(card, BorderLayout.CENTER);
        setContentPane(outer);
        getRootPane().setDefaultButton(save);
    }

    private static JPanel labeledInput(String heading, JComponent input) {
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);
        box.add(label(heading, 11, Font.BOLD, MUTED), BorderLayout.NORTH);
        box.add(input, BorderLayout.CENTER);
        return box;
    }

    private static void addFormRow(
            JPanel panel, GridBagConstraints gbc,
            String title, JTextField input, int row) {
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 8, 0);
        panel.add(labeledInput(title, input), gbc);
    }

    private static JTextField field(String value) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(NAVY);
        field.setBackground(new Color(249, 250, 255));
        field.setCaretColor(BLUE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setPreferredSize(new Dimension(510, 37));
        return field;
    }

    private static void styleCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(new Color(249, 250, 255));
        combo.setForeground(NAVY);
        combo.setPreferredSize(new Dimension(190, 38));
    }

    private static JLabel label(
            String text, int size, int weight, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", weight, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static class QuestionEditorBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2.setPaint(new GradientPaint(
                    0, 0, new Color(29, 34, 88),
                    getWidth(), getHeight(), new Color(42, 45, 111)
            ));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
