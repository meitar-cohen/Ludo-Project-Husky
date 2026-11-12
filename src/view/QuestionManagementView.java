package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Ludo question management screen. */
public class QuestionManagementView extends JFrame {

    private static final Color NAVY = new Color(20, 28, 70);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(108, 121, 150);
    private static final Color LIGHT = new Color(243, 246, 253);
    private static final Color BORDER = new Color(225, 231, 245);
    private static final Color GREEN = new Color(35, 155, 105);
    private static final Color RED = new Color(204, 69, 89);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private final List<Question> questions = new ArrayList<>();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel countLabel = new JLabel();
    private final JLabel previewTitle = new JLabel();
    private final JLabel[] previewAnswers = new JLabel[4];
    private final RoundedButton editButton = new RoundedButton("Edit Question", Style.SECONDARY);
    private final RoundedButton deleteButton = new RoundedButton("Delete Question", Style.DANGER);

    public QuestionManagementView() {
        setTitle("Ludo - Question Management");
        setSize(1170, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        tableModel = new DefaultTableModel(
                new String[]{"#", "QUESTION", "DIFFICULTY", "CORRECT ANSWER"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        configureTable();

        JPanel background = new GradientBackground();
        background.setLayout(new GridBagLayout());
        background.setBorder(new EmptyBorder(22, 25, 22, 25));

        RoundedSurface card = new RoundedSurface(Color.WHITE, 28);
        card.setLayout(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(1085, 716));
        card.setBorder(new EmptyBorder(28, 32, 25, 32));
        card.add(createHeader(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 17));
        middle.setOpaque(false);
        middle.add(createQuestionTable(), BorderLayout.CENTER);
        middle.add(createPreview(), BorderLayout.SOUTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);

        background.add(card);
        setContentPane(background);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updatePreview();
        });
        refreshTable(0);
    }

    private JPanel createHeader() {
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        JLabel kicker = label("ADMIN DASHBOARD  /  QUESTION BANK", 12, Font.BOLD, BLUE);
        JLabel title = label("Question Management", 34, Font.BOLD, NAVY);
        JLabel desc = label("Review and manage multiple-choice questions for Ludo.", 14, Font.PLAIN, MUTED);
        heading.add(kicker);
        heading.add(Box.createVerticalStrut(5));
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(desc);
        top.add(heading, BorderLayout.CENTER);

        JPanel badgeContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        badgeContainer.setOpaque(false);
        RoundedSurface badge = new RoundedSurface(new Color(235, 239, 255), 14);
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 9));
        JLabel tag = label("QUESTION BANK", 12, Font.BOLD, BLUE);
        badge.add(tag);
        badgeContainer.add(badge);
        top.add(badgeContainer, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout(0, 20));
        wrap.setOpaque(false);
        wrap.add(top, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JLabel section = label("QUESTION LIBRARY", 12, Font.BOLD, MUTED);
        toolbar.add(section, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        buttons.setOpaque(false);
        RoundedButton addButton = new RoundedButton("+  Add Question", Style.PRIMARY);
        addButton.addActionListener(e -> showQuestionForm(-1));
        editButton.addActionListener(e -> {
            int index = selectedQuestionIndex();
            if (index < 0) {
                showNotice("Choose a question", "Select a row in the table to edit a question.", BLUE);
                return;
            }
            showQuestionForm(index);
        });
        deleteButton.addActionListener(e -> deleteSelectedQuestion());
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        toolbar.add(buttons, BorderLayout.EAST);
        wrap.add(toolbar, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel createQuestionTable() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(1020, 315));
        JPanel summary = new JPanel(new BorderLayout());
        summary.setOpaque(false);
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        countLabel.setForeground(NAVY);
        summary.add(countLabel, BorderLayout.WEST);
        JLabel hint = label("Select a question to preview its answers", 12, Font.PLAIN, MUTED);
        summary.add(hint, BorderLayout.EAST);
        panel.add(summary, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPreview() {
        RoundedSurface preview = new RoundedSurface(LIGHT, 20);
        preview.setLayout(new BorderLayout(0, 12));
        preview.setPreferredSize(new Dimension(1020, 181));
        preview.setBorder(new EmptyBorder(18, 22, 17, 22));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel label = label("ANSWER PREVIEW", 12, Font.BOLD, BLUE);
        heading.add(label, BorderLayout.WEST);
        JLabel marker = label("Green = correct answer", 12, Font.PLAIN, GREEN);
        heading.add(marker, BorderLayout.EAST);
        preview.add(heading, BorderLayout.NORTH);

        JPanel contents = new JPanel(new BorderLayout(0, 10));
        contents.setOpaque(false);
        previewTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        previewTitle.setForeground(NAVY);
        contents.add(previewTitle, BorderLayout.NORTH);

        JPanel answersGrid = new JPanel(new GridLayout(2, 2, 12, 8));
        answersGrid.setOpaque(false);
        for (int i = 0; i < previewAnswers.length; i++) {
            previewAnswers[i] = new JLabel(" ");
            previewAnswers[i].setFont(new Font("Segoe UI", Font.PLAIN, 13));
            previewAnswers[i].setOpaque(true);
            previewAnswers[i].setBackground(Color.WHITE);
            previewAnswers[i].setForeground(MUTED);
            previewAnswers[i].setBorder(new EmptyBorder(6, 11, 6, 11));
            answersGrid.add(previewAnswers[i]);
        }
        contents.add(answersGrid, BorderLayout.CENTER);
        preview.add(contents, BorderLayout.CENTER);
        return preview;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        RoundedButton back = new RoundedButton("Back to Main Menu", Style.PRIMARY);
        back.setPreferredSize(new Dimension(225, 45));
        back.addActionListener(e -> {
            new MainMenuView().setVisible(true);
            dispose();
        });
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    private void configureTable() {
        table.setFont(BODY_FONT);
        table.setRowHeight(43);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(Color.WHITE);
        table.setForeground(NAVY);
        table.setSelectionBackground(new Color(229, 235, 255));
        table.setSelectionForeground(NAVY);
        table.setFillsViewportHeight(true);
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(1, 42));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(236, 241, 251));
        table.getTableHeader().setForeground(new Color(92, 110, 150));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
        table.getColumnModel().getColumn(2).setPreferredWidth(135);
        table.getColumnModel().getColumn(3).setPreferredWidth(260);
        for (int c = 0; c < table.getColumnCount(); c++) {
            final int column = c;
            table.getColumnModel().getColumn(c).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean selected, boolean focus, int row, int col) {
                    super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                    setBorder(new EmptyBorder(0, 13, 0, 7));
                    setFont(new Font("Segoe UI", column == 0 ? Font.BOLD : Font.PLAIN, 14));
                    setBackground(selected ? new Color(230, 236, 255) :
                            row % 2 == 0 ? Color.WHITE : new Color(249, 250, 254));
                    Color color = NAVY;
                    if (column == 0) color = BLUE;
                    if (column == 2) {
                        String diff = String.valueOf(value);
                        color = "Easy".equals(diff) ? GREEN :
                                "Medium".equals(diff) ? new Color(187, 112, 29) : RED;
                    }
                    if (column == 3) color = GREEN;
                    setForeground(color);
                    setHorizontalAlignment(column == 0 || column == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
                    return this;
                }
            });
        }
    }

    private void refreshTable(int selectIndex) {
        tableModel.setRowCount(0);
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            tableModel.addRow(new Object[]{i + 1, q.text, q.difficulty, q.options[q.correctIndex]});
        }
        countLabel.setText(questions.size() + "  questions");
        if (!questions.isEmpty() && selectIndex >= 0) {
            table.setRowSelectionInterval(Math.min(selectIndex, questions.size() - 1),
                    Math.min(selectIndex, questions.size() - 1));
        } else {
            table.clearSelection();
        }
        updatePreview();
    }

    private int selectedQuestionIndex() {
        int selected = table.getSelectedRow();
        return selected < 0 ? -1 : table.convertRowIndexToModel(selected);
    }

    private void updatePreview() {
        int idx = selectedQuestionIndex();
        if (idx < 0) {
            previewTitle.setText("Select a question to see all four answers.");
            for (JLabel cell : previewAnswers) {
                cell.setText(" ");
                cell.setForeground(MUTED);
                cell.setBackground(Color.WHITE);
            }
            return;
        }
        Question q = questions.get(idx);
        previewTitle.setText(q.text);
        for (int i = 0; i < previewAnswers.length; i++) {
            JLabel cell = previewAnswers[i];
            boolean correct = i == q.correctIndex;
            cell.setText((char) ('A' + i) + "    " + q.options[i]);
            cell.setForeground(correct ? new Color(25, 125, 85) : NAVY);
            cell.setBackground(correct ? new Color(217, 246, 232) : Color.WHITE);
            cell.setFont(new Font("Segoe UI", correct ? Font.BOLD : Font.PLAIN, 13));
        }
    }

    private void deleteSelectedQuestion() {
        int idx = selectedQuestionIndex();
        if (idx < 0) {
            showNotice("Choose a question", "Select a row in the table to delete a question.", BLUE);
            return;
        }
        Question q = questions.get(idx);
        JDialog dialog = createModal("Confirm Deletion", 510, 275);
        RoundedSurface card = createDialogCard();
        card.setLayout(new BorderLayout(0, 16));
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(label("DELETE QUESTION", 12, Font.BOLD, RED));
        header.add(Box.createVerticalStrut(8));
        header.add(label("Are you sure?", 26, Font.BOLD, NAVY));
        header.add(Box.createVerticalStrut(8));
        JLabel question = label(shorten(q.text, 62), 14, Font.PLAIN, MUTED);
        header.add(question);
        card.add(header, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        RoundedButton cancel = new RoundedButton("Cancel", Style.SECONDARY);
        RoundedButton delete = new RoundedButton("Delete", Style.DANGER);
        cancel.addActionListener(e -> dialog.dispose());
        delete.addActionListener(e -> {
            questions.remove(idx);
            refreshTable(Math.min(idx, questions.size() - 1));
            dialog.dispose();
        });
        actions.add(cancel);
        actions.add(delete);
        card.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(wrapDialogCard(card));
        dialog.setVisible(true);
    }

    private void showQuestionForm(int index) {
        final boolean editing = index >= 0;
        final Question existing = editing ? questions.get(index) : null;
        JDialog dialog = createModal(editing ? "Edit Question" : "Add New Question", 680, 715);
        RoundedSurface card = createDialogCard();
        card.setLayout(new BorderLayout(0, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(label("QUESTION BANK  /  " + (editing ? "EDIT" : "NEW"), 12, Font.BOLD, BLUE));
        header.add(Box.createVerticalStrut(4));
        header.add(label(editing ? "Edit question" : "Add a new question", 30, Font.BOLD, NAVY));
        header.add(Box.createVerticalStrut(5));
        header.add(label("Enter the question and all four possible answers.", 14, Font.PLAIN, MUTED));
        card.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 7, 0);

        JTextField questionField = field(existing == null ? "" : existing.text);
        addFormRow(form, gbc, "QUESTION", questionField, 0);

        JTextField[] answerFields = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            answerFields[i] = field(existing == null ? "" : existing.options[i]);
            addFormRow(form, gbc, "ANSWER " + (i + 1), answerFields[i], i + 1);
        }

        JPanel selectors = new JPanel(new GridLayout(1, 2, 16, 0));
        selectors.setOpaque(false);
        JComboBox<String> correctBox = new JComboBox<>(
                new String[]{"Answer 1", "Answer 2", "Answer 3", "Answer 4"});
        JComboBox<String> difficultyBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        styleCombo(correctBox);
        styleCombo(difficultyBox);
        if (existing != null) {
            correctBox.setSelectedIndex(existing.correctIndex);
            difficultyBox.setSelectedItem(existing.difficulty);
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
        RoundedButton cancel = new RoundedButton("Cancel", Style.SECONDARY);
        RoundedButton save = new RoundedButton(editing ? "Save Changes" : "Add Question", Style.PRIMARY);
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            String text = questionField.getText().trim();
            String[] options = new String[4];
            boolean valid = !text.isEmpty();
            for (int i = 0; i < 4; i++) {
                options[i] = answerFields[i].getText().trim();
                if (options[i].isEmpty()) valid = false;
            }
            if (!valid) {
                error.setText("Please fill in the question and all four answers.");
                return;
            }
            Question updated = new Question(text, options, correctBox.getSelectedIndex(),
                    (String) difficultyBox.getSelectedItem());
            if (editing) {
                questions.set(index, updated);
                refreshTable(index);
            } else {
                questions.add(updated);
                refreshTable(questions.size() - 1);
            }
            dialog.dispose();
        });
        actions.add(cancel);
        actions.add(save);
        footer.add(actions, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(wrapDialogCard(card));
        dialog.getRootPane().setDefaultButton(save);
        dialog.setVisible(true);
    }

    private void showNotice(String title, String message, Color accent) {
        JDialog dialog = createModal(title, 510, 245);
        RoundedSurface card = createDialogCard();
        card.setLayout(new BorderLayout(0, 16));
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(label("QUESTION MANAGEMENT", 12, Font.BOLD, accent));
        body.add(Box.createVerticalStrut(9));
        body.add(label(title, 24, Font.BOLD, NAVY));
        body.add(Box.createVerticalStrut(8));
        body.add(label(message, 14, Font.PLAIN, MUTED));
        card.add(body, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.setOpaque(false);
        RoundedButton ok = new RoundedButton("OK", Style.PRIMARY);
        ok.addActionListener(e -> dialog.dispose());
        controls.add(ok);
        card.add(controls, BorderLayout.SOUTH);
        dialog.setContentPane(wrapDialogCard(card));
        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);
    }

    private JDialog createModal(String title, int w, int h) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(w, h);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        return dialog;
    }

    private RoundedSurface createDialogCard() {
        RoundedSurface card = new RoundedSurface(Color.WHITE, 26);
        card.setBorder(new EmptyBorder(25, 28, 25, 28));
        return card;
    }

    private JPanel wrapDialogCard(RoundedSurface card) {
        JPanel outer = new GradientBackground();
        outer.setLayout(new BorderLayout());
        outer.setBorder(new EmptyBorder(15, 15, 15, 15));
        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private static JPanel labeledInput(String heading, JComponent input) {
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setOpaque(false);
        box.add(label(heading, 11, Font.BOLD, MUTED), BorderLayout.NORTH);
        box.add(input, BorderLayout.CENTER);
        return box;
    }

    private static void addFormRow(JPanel panel, GridBagConstraints gbc,
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
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(510, 37));
        return field;
    }

    private static void styleCombo(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(new Color(249, 250, 255));
        combo.setForeground(NAVY);
        combo.setPreferredSize(new Dimension(190, 38));
    }

    private static JLabel label(String text, int size, int weight, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", weight, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private static final class Question {
        final String text;
        final String[] options;
        final int correctIndex;
        final String difficulty;
        Question(String text, String[] options, int correctIndex, String difficulty) {
            this.text = text;
            this.options = Arrays.copyOf(options, options.length);
            this.correctIndex = correctIndex;
            this.difficulty = difficulty;
        }
    }

    private enum Style { PRIMARY, SECONDARY, DANGER }

    private static final class RoundedButton extends JButton {
        private final Style style;
        private boolean hovering;
        RoundedButton(String text, Style style) {
            super(text);
            this.style = style;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(style == Style.SECONDARY ? BLUE : Color.WHITE);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(style == Style.PRIMARY ? 170 : 157, 43));
            setBorder(new EmptyBorder(8, 13, 8, 13));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovering = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = style == Style.PRIMARY ? BLUE : style == Style.DANGER ? RED : new Color(231, 237, 254);
            if (hovering) base = style == Style.PRIMARY ? new Color(55, 88, 229) :
                    style == Style.DANGER ? new Color(184, 50, 75) : new Color(215, 224, 251);
            if (getModel().isPressed()) base = base.darker();
            g2.setColor(base);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class RoundedSurface extends JPanel {
        private final Color fill;
        private final int radius;
        RoundedSurface(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class GradientBackground extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(29, 34, 88),
                    getWidth(), getHeight(), new Color(42, 45, 111)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(102, 122, 233, 28));
            g2.fillOval(-170, -140, 460, 460);
            g2.fillOval(getWidth() - 270, getHeight() - 230, 440, 440);
            g2.dispose();
        }
    }
}
