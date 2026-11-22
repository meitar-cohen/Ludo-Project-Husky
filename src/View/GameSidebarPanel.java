package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Sidebar containing the active players, dice, status, and game controls. */
class GameSidebarPanel extends JPanel {

    private static final Color LIGHT = new Color(244, 247, 254);

    private final String[] playerNames;
    private final String[] playerColors;
    private final JLabel turnLabel;
    private final JLabel formulaLabel;
    private final JTextArea statusArea;
    private final JButton rollButton;
    private final JButton endGameButton;
    private final JPanel playersPanel;
    private final DicePanel dice;

    GameSidebarPanel(
            String[] playerNames,
            String[] playerColors,
            String difficulty,
            int boostDieFaces,
            Runnable onRoll,
            Runnable onEndGame,
            Runnable onLeaveGame) {
        this.playerNames = playerNames.clone();
        this.playerColors = playerColors.clone();
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(330, 0));
        setBorder(new EmptyBorder(18, 18, 12, 18));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        JLabel eyebrow = GameScreen.sectionLabel("YOUR GAME");
        eyebrow.setForeground(GameScreen.PRIMARY);
        stack.add(GameScreen.leftAligned(eyebrow));
        stack.add(Box.createVerticalStrut(4));

        turnLabel = new JLabel(" ");
        turnLabel.setForeground(GameScreen.INK);
        turnLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        stack.add(GameScreen.leftAligned(turnLabel));
        stack.add(Box.createVerticalStrut(12));
        stack.add(GameScreen.leftAligned(GameScreen.sectionLabel("PLAYERS")));
        stack.add(Box.createVerticalStrut(6));

        playersPanel = new JPanel();
        playersPanel.setOpaque(false);
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        stack.add(GameScreen.leftAligned(playersPanel));
        stack.add(Box.createVerticalStrut(6));
        stack.add(GameScreen.leftAligned(GameScreen.sectionLabel("DICE")));
        stack.add(Box.createVerticalStrut(4));

        dice = new DicePanel(
                !"Easy".equals(difficulty),
                "Hard".equals(difficulty) ? "ULTRA BOOST" : "BOOST",
                boostDieFaces
        );
        stack.add(GameScreen.leftAligned(dice));

        formulaLabel = new JLabel(" ");
        formulaLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formulaLabel.setForeground(GameScreen.PRIMARY);
        stack.add(GameScreen.leftAligned(formulaLabel));
        stack.add(Box.createVerticalStrut(6));
        stack.add(GameScreen.leftAligned(GameScreen.sectionLabel("GAME UPDATES")));
        stack.add(Box.createVerticalStrut(7));

        GameScreen.GameCardPanel updateCard =
                new GameScreen.GameCardPanel(new Color(236, 242, 255), 16);
        updateCard.setLayout(new BorderLayout());
        updateCard.setBorder(new EmptyBorder(10, 12, 9, 12));
        updateCard.setPreferredSize(new Dimension(285, 78));
        updateCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        statusArea = new JTextArea(4, 22);
        statusArea.setEditable(false);
        statusArea.setFocusable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusArea.setForeground(new Color(45, 59, 99));
        statusArea.setOpaque(false);
        statusArea.setBorder(null);
        updateCard.add(statusArea, BorderLayout.CENTER);
        stack.add(GameScreen.leftAligned(updateCard));
        stack.add(Box.createVerticalStrut(10));

        rollButton = createButton("Roll Dice", GameScreen.PRIMARY);
        rollButton.addActionListener(e -> onRoll.run());
        stack.add(GameScreen.leftAligned(rollButton));
        stack.add(Box.createVerticalStrut(7));

        endGameButton = createButton("End Game", new Color(35, 172, 130));
        endGameButton.setEnabled(false);
        endGameButton.addActionListener(e -> onEndGame.run());
        stack.add(GameScreen.leftAligned(endGameButton));
        stack.add(Box.createVerticalStrut(7));

        JButton menuButton = createButton("Leave game", new Color(103, 117, 143));
        menuButton.addActionListener(e -> onLeaveGame.run());
        stack.add(GameScreen.leftAligned(menuButton));
        add(stack, BorderLayout.CENTER);
    }

    void animateDice(int stepDie, int boostDie, Runnable onDone) {
        dice.animate(stepDie, boostDie, onDone);
    }

    String getRollButtonText() {
        return rollButton.getText();
    }

    void setFormula(String text) {
        formulaLabel.setText(text);
    }

    void setRollButton(boolean enabled, String text) {
        rollButton.setEnabled(enabled);
        rollButton.setText(text);
    }

    void setEndGameEnabled(boolean enabled) {
        endGameButton.setEnabled(enabled);
    }

    void refreshPlayers(int currentPlayer) {
        playersPanel.removeAll();
        turnLabel.setText(playerNames[currentPlayer] + "'s turn");
        for (int i = 0; i < playerNames.length; i++) {
            boolean current = i == currentPlayer;
            JPanel chip = new GameScreen.GameCardPanel(
                    current ? new Color(235, 240, 255) : LIGHT, 15);
            chip.setLayout(new BorderLayout(9, 0));
            chip.setBorder(new EmptyBorder(3, 10, 3, 10));
            chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
            chip.setPreferredSize(new Dimension(285, 29));
            Color pieceColor = BoardPanel.colorOf(playerColors[i]);
            JLabel dot = new JLabel("\u2022");
            dot.setFont(new Font("Dialog", Font.BOLD, 19));
            dot.setForeground(pieceColor);
            chip.add(dot, BorderLayout.WEST);
            JLabel name = new JLabel(playerNames[i]);
            name.setFont(new Font(
                    "Segoe UI", current ? Font.BOLD : Font.PLAIN, 13));
            name.setForeground(GameScreen.INK);
            chip.add(name, BorderLayout.CENTER);
            if (current) {
                JLabel playing = new JLabel("PLAYING");
                playing.setForeground(GameScreen.PRIMARY);
                playing.setFont(new Font("Segoe UI", Font.BOLD, 10));
                chip.add(playing, BorderLayout.EAST);
            }
            playersPanel.add(chip);
            if (i < playerNames.length - 1) {
                playersPanel.add(Box.createVerticalStrut(3));
            }
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    void setStatus(String text) {
        statusArea.setText(text);
        statusArea.setToolTipText(text);
        statusArea.setCaretPosition(0);
    }

    private JButton createButton(String text, Color color) {
        JButton button = new GameScreen.ActionButton(text, color);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        button.setPreferredSize(new Dimension(285, 36));
        return button;
    }
}
