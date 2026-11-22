package View;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import Controller.QuestionManagementController;
import Model.Question;
import Model.QuestionRecord;

/** ממשק ניהול שאלות - עובד מול ה-Controller ושומר שינויים לקובץ CSV. */
public final class QuestionManagementView extends JFrame {

    private static final Color NAVY = new Color(18, 27, 61);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(111, 125, 153);
    private static final Color LIGHT = new Color(243, 246, 254);
    private static final Color GREEN = new Color(25, 140, 99);
    private static final Color RED = new Color(198, 63, 82);
    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);

    private final QuestionManagementController controller;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel counter = new JLabel();
    private final JTextArea preview = new JTextArea();
    private final JButton editButton = button("Edit", false, false);
    private final JButton deleteButton = button("Delete", false, true);

    /** בכניסה רגילה משתמשים בנתיב המאגר של הפרויקט. */
    public QuestionManagementView() throws IOException {
        this(new QuestionManagementController());
    }

    /** הבנאי הזה מאפשר לבדוק את הממשק על עותק CSV זמני. */
    public QuestionManagementView(QuestionManagementController controller) {
        if (controller == null) throw new IllegalArgumentException("Controller required");
        this.controller = controller;
        setTitle("Ludo - Question Management");
        MainMenuScreen.applyAppIcon(this);
        // גם לחיצה על X מחזירה לתפריט הראשי ולא משאירה חלון ריק.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                returnToMainMenu();
            }
        });
        setMinimumSize(new Dimension(950, 660));
        setSize(1130, 790);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(
                new Object[]{"ID", "QUESTION", "DIFFICULTY", "CORRECT", "PATTERN"}, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                return column == 0 ? Integer.class : String.class;
            }
        };
        table = new JTable(model);
        configureTable();

        JPanel background = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(13, 22, 55),
                        getWidth(), getHeight(), new Color(47, 40, 110)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(117, 123, 240, 43));
                g.fillOval(-130, -140, 480, 480);
                g.fillOval(getWidth() - 380, getHeight() - 330, 520, 520);
                g.dispose();
            }
        };
        background.setBorder(new EmptyBorder(18, 18, 18, 18));

        RoundedSurface card = new RoundedSurface(Color.WHITE, 28);
        card.setPreferredSize(new Dimension(1060, 715));
        card.setLayout(new BorderLayout(0, 17));
        card.setBorder(new EmptyBorder(24, 30, 23, 30));
        card.add(createHeader(), BorderLayout.NORTH);
        card.add(createMiddle(), BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);
        background.add(card);
        setContentPane(background);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updatePreview();
        });
        refresh(-1);
    }

    private JPanel createHeader() {
        JPanel wrap = clearPanel(new BorderLayout(0, 14));
        JPanel heading = clearPanel(new BorderLayout());
        JPanel headingText = clearPanel();
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));
        headingText.add(label("ADMIN DASHBOARD  /  QUESTION BANK", 12, Font.BOLD, BLUE));
        headingText.add(Box.createVerticalStrut(3));
        headingText.add(label("Question Management", 31, Font.BOLD, NAVY));
        headingText.add(Box.createVerticalStrut(3));
        headingText.add(label("Manage questions and save changes to the CSV bank.",
                13, Font.PLAIN, MUTED));
        heading.add(headingText, BorderLayout.CENTER);
        counter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        counter.setForeground(GREEN);
        heading.add(counter, BorderLayout.EAST);
        wrap.add(heading, BorderLayout.NORTH);

        JPanel toolbar = clearPanel(new BorderLayout());
        toolbar.add(label("QUESTION LIBRARY", 12, Font.BOLD, MUTED), BorderLayout.WEST);
        JPanel actions = clearPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton add = button("+ Add Question", true, false);
        add.addActionListener(e -> showForm(null));
        editButton.addActionListener(e -> {
            QuestionRecord selected = selectedRecord();
            if (selected != null) showForm(selected);
        });
        deleteButton.addActionListener(e -> deleteSelected());
        actions.add(add);
        actions.add(editButton);
        actions.add(deleteButton);
        toolbar.add(actions, BorderLayout.EAST);
        wrap.add(toolbar, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel createMiddle() {
        JPanel middle = clearPanel(new BorderLayout(0, 14));
        JScrollPane scroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(225, 231, 245)));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        middle.add(scroll, BorderLayout.CENTER);

        RoundedSurface previewCard = new RoundedSurface(LIGHT, 18);
        previewCard.setLayout(new BorderLayout(0, 9));
        previewCard.setBorder(new EmptyBorder(15, 19, 16, 19));
        previewCard.setPreferredSize(new Dimension(980, 176));
        previewCard.add(label("QUESTION & ANSWER PREVIEW", 12, Font.BOLD, BLUE),
                BorderLayout.NORTH);
        preview.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        preview.setForeground(NAVY);
        preview.setOpaque(false);
        preview.setEditable(false);
        preview.setFocusable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setBorder(null);
        JScrollPane previewScroll = new JScrollPane(preview,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        previewScroll.setBorder(null);
        previewScroll.setOpaque(false);
        previewScroll.getViewport().setOpaque(false);
        previewCard.add(previewScroll, BorderLayout.CENTER);
        middle.add(previewCard, BorderLayout.SOUTH);
        return middle;
    }

    private JPanel createFooter() {
        JPanel footer = clearPanel(new BorderLayout());
        JLabel note = label("Changes are saved immediately. Keep at least 30 questions.",
                12, Font.PLAIN, MUTED);
        footer.add(note, BorderLayout.WEST);
        JButton back = button("Back to Main Menu", true, false);
        back.addActionListener(e -> returnToMainMenu());
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    private void returnToMainMenu() {
        new MainMenuScreen().setVisible(true);
        dispose();
    }

    private void configureTable() {
        table.setFont(BODY);
        table.setRowHeight(39);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setForeground(NAVY);
        table.setSelectionBackground(new Color(228, 234, 255));
        table.setSelectionForeground(NAVY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setBackground(new Color(241, 245, 253));
        table.getTableHeader().setForeground(MUTED);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 12, 0, 7));
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(renderer);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(460);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(235);
    }

    private void refresh(int selectedId) {
        List<QuestionRecord> records = controller.getQuestions();
        model.setRowCount(0);
        for (QuestionRecord record : records) {
            Question q = record.getQuestion();
            String text = q.getText().replace('\n', ' ').replace('\r', ' ');
            model.addRow(new Object[]{record.getId(), text,
                    q.getDifficulty().toUpperCase(),
                    Character.toString((char) ('A' + q.getCorrectIndex())),
                    record.getArchitecturalPattern()});
        }
        counter.setText(records.size() + " QUESTIONS");
        int targetRow = records.isEmpty() ? -1 : 0;
        for (int row = 0; row < model.getRowCount(); row++) {
            if (((Integer) model.getValueAt(row, 0)) == selectedId) {
                targetRow = row;
                break;
            }
        }
        if (targetRow >= 0) {
            table.setRowSelectionInterval(targetRow, targetRow);
            table.scrollRectToVisible(table.getCellRect(targetRow, 0, true));
        }
        updatePreview();
    }

    private QuestionRecord selectedRecord() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int id = (Integer) model.getValueAt(table.convertRowIndexToModel(row), 0);
        for (QuestionRecord record : controller.getQuestions()) {
            if (record.getId() == id) return record;
        }
        return null;
    }

    private void updatePreview() {
        QuestionRecord record = selectedRecord();
        editButton.setEnabled(record != null);
        deleteButton.setEnabled(record != null);
        if (record == null) {
            preview.setText("Select a question to view its answers.");
            return;
        }
        Question q = record.getQuestion();
        StringBuilder summary = new StringBuilder(q.getText()).append("\n\n");
        String[] answers = q.getOptions();
        for (int i = 0; i < answers.length; i++) {
            summary.append(i == q.getCorrectIndex() ? "[CORRECT] " : "");
            summary.append((char) ('A' + i)).append(". ")
                    .append(answers[i]).append("\n");
        }
        preview.setText(summary.toString());
        preview.setCaretPosition(0);
    }

    private void deleteSelected() {
        QuestionRecord record = selectedRecord();
        if (record == null) return;
        int response = JOptionPane.showConfirmDialog(this,
                "Delete question #" + record.getId() + "?\n"
                        + "This change will be saved to the CSV file.",
                "Confirm deletion", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (response != JOptionPane.YES_OPTION) return;
        try {
            controller.delete(record.getId());
            refresh(-1);
        } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
            showError(ex);
        }
    }

    private void showForm(QuestionRecord existing) {
        JDialog dialog = new JDialog(this,
                existing == null ? "Add Question" : "Edit Question #" + existing.getId(),
                true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(740, 745);
        dialog.setMinimumSize(new Dimension(600, 550));
        dialog.setLocationRelativeTo(this);

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

        if (existing != null) {
            Question q = existing.getQuestion();
            difficulty.setSelectedItem(q.getDifficulty().toLowerCase());
            questionText.setText(q.getText());
            String[] opts = q.getOptions();
            for (int i = 0; i < 4; i++) answerTexts[i].setText(opts[i]);
            correct.setSelectedIndex(q.getCorrectIndex());
            pattern.setText(existing.getArchitecturalPattern());
            coverage.setText(existing.getCoverage());
            source.setText(existing.getSource());
        }

        JScrollPane formScroll = new JScrollPane(fields,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(18);
        formScroll.setBorder(null);
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actionBar.setBackground(Color.WHITE);
        JButton cancel = button("Cancel", false, false);
        cancel.addActionListener(e -> dialog.dispose());
        JButton save = button(existing == null ? "Add & Save" : "Save Changes", true, false);
        save.addActionListener(e -> {
            try {
                String[] options = new String[4];
                for (int i = 0; i < 4; i++) options[i] = answerTexts[i].getText();
                Question q = new Question(questionText.getText(), options,
                        correct.getSelectedIndex(), (String) difficulty.getSelectedItem());
                int selectedId;
                if (existing == null) {
                    selectedId = controller.add(q, pattern.getText(),
                            coverage.getText(), source.getText());
                } else {
                    selectedId = existing.getId();
                    controller.update(selectedId, q, pattern.getText(),
                            coverage.getText(), source.getText());
                }
                dialog.dispose();
                refresh(selectedId);
            } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
                showError(ex);
            }
        });
        actionBar.add(cancel);
        actionBar.add(save);
        JPanel outer = new JPanel(new BorderLayout());
        outer.add(formScroll, BorderLayout.CENTER);
        outer.add(actionBar, BorderLayout.SOUTH);
        dialog.setContentPane(outer);
        dialog.getRootPane().setDefaultButton(save);
        dialog.setVisible(true);
    }

    private void addField(JPanel panel, String title, Component field) {
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

    private static JPanel clearPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static JPanel clearPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
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

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static final class RoundedSurface extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final int radius;
        RoundedSurface(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
