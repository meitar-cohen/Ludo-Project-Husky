package View;

import Controllers.HistoryController;
import Model.GameRecord;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Game history screen.
 * SRS 3.2.8.3.4 - shows the games saved in the history file (through HistoryController).
 */
public class GameHistoryScreen extends JFrame {

    private static final Color NAVY = new Color(20, 27, 65);
    private static final Color BLUE = new Color(75, 110, 248);
    private static final Color MUTED = new Color(112, 124, 151);
    private static final Color LIGHT = new Color(243, 246, 253);
    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private final JPanel rankingCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
    private final JLabel rankingGameLabel = new JLabel();
    private final List<GameRecord> history;
    private final int skippedRows;
    private final String historyError;
    private final String historyFileName;
    private JTable table;

    public GameHistoryScreen() {
        this(new HistoryController());
    }

    public GameHistoryScreen(HistoryController controller) {
        // SRS 3.2.8.3.4 - the saved games are read from the history file
        this.history = controller.loadHistory();
        this.skippedRows = controller.getSkippedRows();
        this.historyError = controller.getLastError();
        this.historyFileName = controller.getHistoryFileName();
        setTitle("Ludo - Game History");
        setSize(1090, 730);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        JPanel background = new HistoryBackgroundPanel();
        background.setLayout(new GridBagLayout());
        HistorySurfacePanel main = new HistorySurfacePanel(Color.WHITE, 30);
        main.setPreferredSize(new Dimension(970, 640));
        main.setLayout(new BorderLayout(0, 22));
        main.setBorder(new EmptyBorder(31, 37, 28, 37));
        background.add(main);
        main.add(createHeader(), BorderLayout.NORTH);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(createGamesTable());
        content.add(Box.createVerticalStrut(20));
        content.add(createRankingPanel());
        main.add(content, BorderLayout.CENTER);
        main.add(createFooter(), BorderLayout.SOUTH);
        setContentPane(background);
        if (!history.isEmpty()) {
            table.setRowSelectionInterval(0, 0);   // show the standings of the newest game
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setOpaque(false);
        JPanel headings = new JPanel();
        headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        headings.setOpaque(false);
        JLabel kicker = new JLabel("YOUR LUDO JOURNEY");
        kicker.setForeground(BLUE);
        kicker.setFont(new Font("Segoe UI", Font.BOLD, 12));
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        headings.add(kicker);
        headings.add(Box.createVerticalStrut(6));
        JLabel title = new JLabel("Game History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 35));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        headings.add(title);
        headings.add(Box.createVerticalStrut(3));
        JLabel subtitle = new JLabel("Explore past games and their final standings.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headings.add(subtitle);
        header.add(headings, BorderLayout.CENTER);
        int saved = history.size();
        JLabel count = new JLabel(saved == 0 ? "  NO SAVED GAMES  "
                : "  " + saved + (saved == 1 ? " SAVED GAME  " : " SAVED GAMES  "));
        count.setFont(new Font("Segoe UI", Font.BOLD, 12));
        count.setForeground(BLUE);
        count.setOpaque(true);
        count.setBackground(new Color(232, 237, 255));
        count.setBorder(new EmptyBorder(10, 11, 10, 11));
        JPanel badgeArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 3));
        badgeArea.setOpaque(false);
        badgeArea.add(count);
        header.add(badgeArea, BorderLayout.EAST);
        return header;
    }

    private JComponent createGamesTable() {
        HistorySurfacePanel container = new HistorySurfacePanel(LIGHT, 21);
        container.setLayout(new BorderLayout());
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, 219));
        container.setPreferredSize(new Dimension(880, 219));
        container.setBorder(new EmptyBorder(10, 13, 10, 13));
        String[] columns = {"GAME", "DATE & TIME", "DIFFICULTY", "WINNER", "DURATION"};
        DefaultTableModel model = new DefaultTableModel(new Object[0][columns.length], columns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (GameRecord game : history) {
            model.addRow(new Object[] {
                "#" + game.getGameId(),
                game.getTimestamp().replace(";", "  "),
                HistoryController.toDisplayDifficulty(game.getDifficulty()),
                game.getWinner(),
                game.getDuration()
            });
        }
        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(49);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFocusable(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(227, 234, 255));
        table.setSelectionForeground(NAVY);
        table.setBackground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(238, 242, 251));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(100, 39));
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {90, 200, 145, 125, 122};
        for (int i = 0; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setCellRenderer(new HistoryCellRenderer(i));
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        container.add(scroll, BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int index = table.convertRowIndexToModel(table.getSelectedRow());
                showRanking(index);
            }
        });
        rankingGameLabel.setText("No completed games yet");
        return container;
    }

    private JComponent createRankingPanel() {
        HistorySurfacePanel panel = new HistorySurfacePanel(new Color(247, 249, 254), 21);
        panel.setLayout(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(880, 177));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 177));
        panel.setBorder(new EmptyBorder(17, 19, 15, 19));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("FINAL STANDINGS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(NAVY);
        head.add(title, BorderLayout.WEST);
        rankingGameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rankingGameLabel.setForeground(MUTED);
        head.add(rankingGameLabel, BorderLayout.EAST);
        panel.add(head, BorderLayout.NORTH);
        rankingCards.setOpaque(false);
        panel.add(rankingCards, BorderLayout.CENTER);
        JLabel empty = new JLabel("Complete a game to see its final standings.",
                SwingConstants.CENTER);
        empty.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        empty.setForeground(MUTED);
        rankingCards.add(empty);
        return panel;
    }

    /** Shows the final standings of the selected game. */
    private void showRanking(int index) {
        if (index < 0 || index >= history.size()) {
            return;
        }
        GameRecord game = history.get(index);
        rankingGameLabel.setText("Game #" + game.getGameId() + "  |  "
                + HistoryController.toDisplayDifficulty(game.getDifficulty()) + "  |  " + game.getDuration());
        rankingCards.removeAll();
        String[] places = {"1ST", "2ND", "3RD", "4TH"};
        String[] names = game.getPlaceNames();
        for (int i = 0; i < names.length; i++) {
            if (names[i].isEmpty()) {
                continue;   // fewer than 4 players
            }
            HistorySurfacePanel card = new HistorySurfacePanel(i == 0 ? new Color(232, 237, 255) : Color.WHITE, 16);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new EmptyBorder(10, 16, 10, 16));
            card.setPreferredSize(new Dimension(195, 66));
            JLabel place = new JLabel(places[i]);
            place.setFont(new Font("Segoe UI", Font.BOLD, 11));
            place.setForeground(i == 0 ? BLUE : MUTED);
            JLabel name = new JLabel(names[i]);
            name.setFont(new Font("Segoe UI", Font.BOLD, 16));
            name.setForeground(NAVY);
            card.add(place);
            card.add(Box.createVerticalStrut(4));
            card.add(name);
            rankingCards.add(card);
        }
        rankingCards.revalidate();
        rankingCards.repaint();
    }

    private JComponent createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        // SRS 3.2.8.3.4 - message when there are no games, when rows were skipped or the file failed
        String text;
        if (historyError != null) {
            text = historyError;
        } else if (history.isEmpty()) {
            text = "No completed games to display.";
        } else {
            text = "Games are read from " + historyFileName + ".";
        }
        if (skippedRows > 0) {
            text += "  " + skippedRows + " damaged row(s) were skipped.";
        }
        JLabel note = new JLabel(text);
        note.setForeground(MUTED);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(note, BorderLayout.WEST);
        JButton back = new ModernButton("Back to Main Menu");
        back.addActionListener(e -> {
            new MainMenuScreen().setVisible(true);
            dispose();
        });
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static class HistoryBackgroundPanel extends JPanel {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setPaint(new GradientPaint(0, 0, new Color(17, 24, 60),
                    getWidth(), getHeight(), new Color(47, 43, 105)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(112, 110, 233, 38));
            g.fillOval(-160, -140, 510, 510);
            g.setColor(new Color(83, 154, 255, 27));
            g.fillOval(650, 370, 500, 500);
            g.dispose();
        }
    }

    private static class HistorySurfacePanel extends JPanel {
        private final Color fill;
        private final int radius;
        HistorySurfacePanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Double(0, 0,
                getWidth() - 1, getHeight() - 1, radius, radius));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class ModernButton extends JButton {
        private boolean hovered;
        ModernButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(205, 46));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            g.setColor(hovered ? new Color(58, 91, 226) : BLUE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HistoryCellRenderer extends DefaultTableCellRenderer {
        private final int column;
        HistoryCellRenderer(int column) { this.column = column; }
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            setBorder(new EmptyBorder(0, 18, 0, 5));
            setFont(new Font("Segoe UI", column == 0 || column == 3 ? Font.BOLD : Font.PLAIN, 13));
            Color fg = column == 0 ? BLUE : NAVY;
            if (column == 2) {
                fg = "Easy".equals(value) ? new Color(32, 145, 96) :
                    "Medium".equals(value) ? new Color(199, 134, 12) : new Color(209, 77, 95);
            }
            setForeground(fg);
            setBackground(selected ? new Color(226, 234, 255) :
                    row % 2 == 0 ? Color.WHITE : new Color(249, 250, 254));
            return this;
        }
    }
}
