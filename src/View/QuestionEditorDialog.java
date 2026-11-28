package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import Model.Question;
import Model.QuestionRecord;

/**
 * חלון עריכת שאלה עצמאי בהתאם לתרשים המחלקות.
 * השמירה בפועל מתבצעת באמצעות callback מהמסך, ולא דרך גישה ישירה ל-CSV.
 */
public final class QuestionEditorDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color NAVY = new Color(18, 27, 61);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 125, 153);
    private static final Color RED = new Color(198, 63, 82);
    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);

    /** ממשק התואם לבנאי המתוכנן בתרשים המחלקות. */
    public QuestionEditorDialog(Window owner, Question existing, Consumer<Question> onSave) {
        this(owner, existing, null, saveOnly(onSave));
    }

    private static SaveHandler saveOnly(Consumer<Question> callback) {
        Objects.requireNonNull(callback, "Save callback required");
        return (question, pattern, coverage, source) -> callback.accept(question);
    }

    /** הרחבה נדרשת לשמירת נתוני המטא-דאטה של ה-CSV ללא איבוד עמודות. */
    public QuestionEditorDialog(Window owner, QuestionRecord existing, SaveHandler onSave) {
        this(owner, existing == null ? null : existing.getQuestion(), existing, onSave);
    }

    @FunctionalInterface
    public interface SaveHandler {
        void save(Question question, String pattern, String coverage, String source)
                throws IOException;
    }

    private QuestionEditorDialog(Window owner, Question question,
                                 QuestionRecord existing, SaveHandler onSave) {
        super(owner, existing != null ? "Edit Question #" + existing.getId()
                : question != null ? "Edit Question" : "Add Question",
                ModalityType.APPLICATION_MODAL);
        if (onSave == null) throw new IllegalArgumentException("Save handler required");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(740, 745);
        setMinimumSize(new Dimension(600, 550));
        setLocationRelativeTo(owner);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setBackground(Color.WHITE);
        fields.setBorder(new EmptyBorder(15, 22, 15, 22));

        JComboBox<String> difficulty = new JComboBox<>(
                new String[]{"easy", "medium", "hard"});
        difficulty.setFont(BODY);
        addField(fields, "DIFFICULTY", difficulty);

        JTextArea questionText = textArea(4);
        addField(fields, "QUESTION", areaScroll(questionText, 96));

        JTextArea[] answerTexts = new JTextArea[4];
        for (int i = 0; i < 4; i++) {
            answerTexts[i] = textArea(3);
            addField(fields, "ANSWER " + (i + 1), areaScroll(answerTexts[i], 68));
        }

        JComboBox<String> correct = new JComboBox<>(new String[]{
                "Answer 1 (A)", "Answer 2 (B)",
                "Answer 3 (C)", "Answer 4 (D)"});
        correct.setFont(BODY);
        addField(fields, "CORRECT ANSWER", correct);

        JTextField pattern = new JTextField();
        pattern.setFont(BODY);
        addField(fields, "ARCHITECTURAL PATTERN", pattern);
        JTextArea coverage = textArea(3);
        addField(fields, "ADVANTAGES / DISADVANTAGES COVERED",
                areaScroll(coverage, 65));
        JTextArea source = textArea(3);
        addField(fields, "SOURCE", areaScroll(source, 65));

        if (question != null) {
            Question q = question;
            difficulty.setSelectedItem(q.getDifficulty().toLowerCase());
            questionText.setText(q.getText());
            String[] opts = q.getOptions();
            for (int i = 0; i < 4; i++) answerTexts[i].setText(opts[i]);
            correct.setSelectedIndex(q.getCorrectIndex());
            if (existing != null) {
                pattern.setText(existing.getArchitecturalPattern());
                coverage.setText(existing.getCoverage());
                source.setText(existing.getSource());
            }
        }

        JScrollPane formScroll = new JScrollPane(fields,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(18);
        formScroll.setBorder(null);
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actionBar.setBackground(Color.WHITE);
        JButton cancel = button("Cancel", false, false);
        cancel.addActionListener(e -> dispose());
        JButton save = button(question == null ? "Add & Save" : "Save Changes", true, false);
        save.addActionListener(e -> {
            try {
                String[] options = new String[4];
                for (int i = 0; i < 4; i++) options[i] = answerTexts[i].getText();
                Question q = new Question(questionText.getText(), options,
                        correct.getSelectedIndex(), (String) difficulty.getSelectedItem());
                onSave.save(q, pattern.getText(), coverage.getText(), source.getText());
                dispose();
            } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
                showError(ex);
            }
        });
        actionBar.add(cancel);
        actionBar.add(save);
        JPanel outer = new JPanel(new BorderLayout());
        outer.add(formScroll, BorderLayout.CENTER);
        outer.add(actionBar, BorderLayout.SOUTH);
        setContentPane(outer);
        getRootPane().setDefaultButton(save);
    }

    private static void addField(JPanel panel, String title, Component field) {
        JLabel heading = label(title, 11, Font.BOLD, MUTED);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        ((javax.swing.JComponent) field).setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                field.getPreferredSize().height));
        panel.add(heading);
        panel.add(Box.createVerticalStrut(5));
        panel.add(field);
        panel.add(Box.createVerticalStrut(13));
    }

    private static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 30);
        area.setFont(BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(6, 8, 6, 8));
        return area;
    }

    private static JScrollPane areaScroll(JTextArea area, int height) {
        JScrollPane scroll = new JScrollPane(area,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(570, height));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(215, 224, 240)));
        return scroll;
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Question Management", JOptionPane.ERROR_MESSAGE);
    }

    private static JLabel label(String text, int size, int weight, Color color) {
        JLabel result = new JLabel(text);
        result.setFont(new Font("Segoe UI", weight, size));
        result.setForeground(color);
        return result;
    }

    private static JButton button(String text, boolean primary, boolean danger) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(primary ? Color.WHITE : danger ? RED : NAVY);
        button.setBackground(primary ? BLUE : danger
                ? new Color(255, 238, 241) : new Color(239, 243, 253));
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? BLUE :
                        new Color(224, 230, 247)),
                new EmptyBorder(10, 17, 10, 17)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

}
