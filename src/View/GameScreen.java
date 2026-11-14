package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import Controller.HistoryController;
import Controller.MovementController;
import Controller.GameController;
import Enums.RollAction;
import Model.Game;
import Model.Player;

public class GameScreen extends JPanel {

    static final Color BACKGROUND = new Color(14, 22, 54);
    static final Color PRIMARY = new Color(75, 110, 248);
    static final Color MUTED = new Color(110, 122, 150);
    static final Color INK = new Color(14, 23, 55);

    private final Player[] players;
    private final String difficulty;

    private final BoardPanel board;
    private final DicePanel dice;
    private final JLabel timerLabel;
    private final GameSidebarPanel sidebar;

    private Timer gameTimer;

    // SRS 3.2.8 - the finished game is saved once; the message is shown on the end screen
    private boolean gameOverHandled;
    private String historyNote = "";
    private final Game game;
    private final HistoryController history;
    private final MovementController pieces;
    private final GameController turns;

    public GameScreen(Player[] players, String difficulty) {
        this.players = players.clone();
        this.difficulty = difficulty == null ? "Easy" : difficulty;
        this.game = new Game(this.players, this.difficulty);
        this.history = new HistoryController();
        this.pieces = new MovementController(game);
        this.turns = new GameController(game, pieces);
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(15, 20, 18, 20));
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 56));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("LUDO  /  GAME ARENA");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 24));
        brand.setForeground(Color.WHITE);
        JLabel tagline = new JLabel("Let the game begin!");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagline.setForeground(new Color(191, 203, 242));
        heading.add(brand);
        heading.add(Box.createVerticalStrut(2));
        heading.add(tagline);
        header.add(heading, BorderLayout.WEST);
        JPanel headerBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 7));
        headerBadges.setOpaque(false);
        JLabel level = new JLabel("  " + this.difficulty.toUpperCase() + " MODE  ");
        level.setOpaque(true);
        level.setBackground(new Color(84, 83, 168));
        level.setForeground(Color.WHITE);
        level.setFont(new Font("Segoe UI", Font.BOLD, 12));
        level.setBorder(new EmptyBorder(3, 10, 3, 10));
        headerBadges.add(level);
        timerLabel = new JLabel("00:00:00");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        timerLabel.setForeground(Color.WHITE);
        headerBadges.add(timerLabel);
        header.add(headerBadges, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        // Main two-column layout.
        JPanel arena = new JPanel(new BorderLayout(16, 0));
        arena.setOpaque(false);
        add(arena, BorderLayout.CENTER);
        GameCardPanel boardCard = new GameCardPanel(Color.WHITE, 24);
        boardCard.setLayout(new BorderLayout(0, 7));
        boardCard.setBorder(new EmptyBorder(12, 13, 13, 13));
        JPanel boardCaption = new JPanel(new BorderLayout());
        boardCaption.setOpaque(false);
        JLabel boardTitle = new JLabel("THE BOARD");
        boardTitle.setForeground(INK);
        boardTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        boardCaption.add(boardTitle, BorderLayout.WEST);
        // Keep the legend available without using space in the sidebar.
        JButton legendButton = new JButton("Board legend");
        legendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        legendButton.setForeground(PRIMARY);
        legendButton.setContentAreaFilled(false);
        legendButton.setBorderPainted(false);
        legendButton.setFocusPainted(false);
        legendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        legendButton.addActionListener(e -> JOptionPane.showMessageDialog(
            this, createLegend(), "Board legend", JOptionPane.PLAIN_MESSAGE));
        boardCaption.add(legendButton, BorderLayout.EAST);
        boardCard.add(boardCaption, BorderLayout.NORTH);
        board = new BoardPanel(this.difficulty, playerColors());
        board.setOpaque(false);
        board.setCastlePieceClickListener(this::onCastlePieceClicked);
        board.setBoardPieceClickListener(this::onBoardPieceClicked);
        boardCard.add(board, BorderLayout.CENTER);
        arena.add(boardCard, BorderLayout.CENTER);
        sidebar = new GameSidebarPanel(
                this.players,
                this.difficulty,
                turns.getBoostDieFaces(),
                this::onRoll,
                this::showEndScreen,
                this::backToMenu
        );
        dice = sidebar.getDice();
        GameCardPanel side = new GameCardPanel(Color.WHITE, 24);
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(330, 0));
        side.add(sidebar, BorderLayout.CENTER);
        arena.add(side, BorderLayout.EAST);
        refreshPlayers();
        setStatus(players[0].getName() + "'s turn. Click Roll Dice to start.");
        startTimer();
    }

    private String[] playerColors() {
        String[] colors = new String[players.length];
        for (int i = 0; i < players.length; i++) {
            colors[i] = players[i].getColor();
        }
        return colors;
    }

    @Override
    protected void paintComponent(Graphics graphic) {
        super.paintComponent(graphic);
        Graphics2D g = (Graphics2D) graphic.create();
        quality(g);
        g.setPaint(new GradientPaint(0, 0, BACKGROUND,
            getWidth(), getHeight(), new Color(44, 39, 100)));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(108, 98, 231, 45));
        g.fillOval(-240, -260, 650, 650);
        g.setColor(new Color(62, 169, 245, 25));
        g.fillOval(600, 300, 700, 700);
        g.dispose();
    }

    private void onRoll() {
        if (!turns.canRoll()) return;
        if ("Next Turn".equals(sidebar.getRollButtonText())) {
            passTurn();
            return;
        }
        // SRS 3.2.1.3.3 - the game clock starts with the first roll.
        turns.startGameClock();
        board.setActivePlayerColor(game.getCurrentPlayerColor());
        int stepDie = turns.rollStepDie();
        int boostDie = turns.rollBoostDie();
        sidebar.setFormula(" ");
        sidebar.setRollButton(false, "Rolling...");
        sidebar.animateDice(stepDie, boostDie, () -> finishRoll(stepDie, boostDie));
    }

    private void finishRoll(int stepDie, int boostDie) {
        if (turns.registerRollAndCheckThirdSix(stepDie)) {
            String player = game.getCurrentPlayerName();
            passTurn();
            setStatus("Third 6 in a row for " + player
                + "! The roll is cancelled. "
                + game.getCurrentPlayerName()
                + "'s turn. Click Roll Dice.");
            return;
        }
        RollAction action = turns.finishRoll(stepDie, boostDie);
        if (action == RollAction.TAKE_OUT
                && pieces.canChooseBoardMoveInsteadOfTakeOut()
                && pieces.hasAnyLegalMove(getCurrentRouteSizes())) {
        	int choice = GameDialogs.chooseMove(this);
            if (choice == 1
                    && pieces.chooseBoardMoveInsteadOfTakeOut()) {
                action = RollAction.MOVE;
            }
        }
        if (action == RollAction.TAKE_OUT) {
            setStatus(
                "Rolled a 6! Click a highlighted piece in your castle."
            );
            board.highlightCastlePieces(
                turns.consumeFirstTurnPulse()
            );
            sidebar.setRollButton(false, "Select Your Piece");
        } else if (action == RollAction.MOVE) {
            if (!pieces.hasAnyLegalMove(getCurrentRouteSizes())) {
                passWhenNoLegalMove();
                return;
            }
            if (game.getPendingSteps() == 0) {
                setStatus(
                    "Boost result is 0. Select a home-path piece to move "
                    + stepDie + " squares."
                );
            } else {
                setStatus(
                    "Move " + game.getPendingSteps()
                    + " squares on the shared path, or "
                    + stepDie
                    + " in the home path. Select a piece."
                );
            }
            board.highlightBoardPiece(
                turns.consumeFirstTurnPulse()
            );
            sidebar.setRollButton(false, "Select Your Piece");
        } else if (game.getPendingSteps() == 0) {
            if (turns.hasExtraTurn()) {
                passWhenNoLegalMove();
                return;
            }
            String previousPlayer = game.getCurrentPlayerName();
            passTurn();
            setStatus("Boost result is 0. " + previousPlayer
                    + " has no legal move. "
                    + game.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice.");
        } else {
            passWhenNoLegalMove();
        }
    }

    private int[] getCurrentRouteSizes() {
        int[] routeSizes = new int[Game.PIECES_PER_PLAYER];
        for (int piece = 0; piece < Game.PIECES_PER_PLAYER; piece++) {
            routeSizes[piece] = board.getActiveRouteSize(piece);
        }
        return routeSizes;
    }

    private void passWhenNoLegalMove() {
        String previousPlayer = game.getCurrentPlayerName();
        if (turns.hasExtraTurn()) {
            turns.grantExtraTurn();
            board.clearHighlight();
            sidebar.setRollButton(true, "Roll Again");
            setStatus(previousPlayer
                    + " has no legal move. Rolled a 6 - roll again.");
            return;
        }
        passTurn();
        if (turns.canRoll()) {
            String reason =
                    game.getPendingSteps() == 0
                    && !"Easy".equals(difficulty)
                    ? "Boost result is 0. " : "";
            setStatus(reason + previousPlayer
                    + " has no legal move. "
                    + game.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice.");
        }
    }

    private void onCastlePieceClicked(int slot) {
        if (!pieces.canTakeOutActivePiece(slot)) {
            return;
        }
        if (!board.placeActivePieceOnStart(slot)) {
            return;
        }
        if (!pieces.takeOutActivePiece(slot)) {
            throw new IllegalStateException(
                    "Castle state is out of sync");
        }
        setStatus(
                "Piece on start! You earned another turn. Click Roll Dice.");
        sidebar.setRollButton(true, "Roll Dice");
    }

    /**
     * SRS 3.2.4.3.1 -
     */
    private void onBoardPieceClicked(int pieceIndex) {
        int routeSize = board.getActiveRouteSize(pieceIndex);
        if (pieces.tryFinishPiece(pieceIndex, routeSize)) {
            if (!board.showActivePieceFinished(pieceIndex)) {
                throw new IllegalStateException(
                        "Finish state is out of sync");
            }
            String player = game.getCurrentPlayerName();
            int finished = board.getActiveFinishedPieceCount();
            setStatus(player + ": piece reached the goal ("
                    + finished + "/4).");
            if (pieces.isCurrentPlayerFinished()) {
                passTurn();
                if (game.getStage() == Enums.Stage.DONE) {
                    sidebar.setEndGameEnabled(true);
                    setStatus(
                        "Game complete! Click End Game to see the results.");
                }
            } else {
                endTurnOrExtraTurn();
            }
            return;
        }
        if (pieces.tryEnterHomePath(pieceIndex, routeSize)) {
            int homeStep = pieces.getPieceStep(pieceIndex);
            if (!board.showActivePieceInHomePath(pieceIndex, homeStep)) {
                throw new IllegalStateException(
                    "Home path view is out of sync"
                );
            }
            board.clearHighlight();
            setStatus("Piece entered the home path!");
            endTurnOrExtraTurn();
            return;
        }
        // SRS 3.2.4.3.2 - passing over or stopping on an arrow of the piece's colour
        int arrowStep = pieces.findArrowOnMove(
                pieceIndex, board.getOwnArrowStep(pieceIndex));
        if (arrowStep >= 0) {
            moveThroughArrow(pieceIndex, arrowStep, routeSize);
            return;
        }
        int targetStep = pieces.validatePieceMove(pieceIndex, routeSize);
        if (targetStep < 0) {
            setStatus("The selected piece cannot make this move.");
            return;
        }
        int startStep = pieces.getPieceStep(pieceIndex);
        if (!pieces.movePiece(pieceIndex, targetStep, routeSize)) {
            setStatus("The selected move is not legal.");
            return;
        }
        board.animatePiece(
                pieceIndex,
                startStep,
                targetStep,
                () -> finishPieceMovement(pieceIndex)
        );
    }

    /**
     * SRS 3.2.4.3.2 - moves the piece to its arrow, and from there to the next path
     * with the steps that are left.
     */
    private void moveThroughArrow(int pieceIndex, int arrowStep, int routeSize) {
        int startStep = pieces.getPieceStep(pieceIndex);
        int remaining = pieces.getMoveStepsForSelectedPiece()
                - (arrowStep - startStep);
        if (!pieces.movePiece(pieceIndex, arrowStep, routeSize)) {
            setStatus("The selected move is not legal.");
            return;
        }
        board.animatePiece(pieceIndex, startStep, arrowStep,
                () -> crossArrow(pieceIndex, remaining));
    }

    /** SRS 3.2.4.3.2 - the piece is on its own arrow: move it to the next path. */
    private void crossArrow(int pieceIndex, int remaining) {
        Color arrowColor = board.getArrowColorAtStep(
                pieceIndex, pieces.getPieceStep(pieceIndex));
        Color pieceColor = BoardPanel.colorOf(game.getCurrentPlayerColor());
        if (!pieces.enterNextPathFromArrow(pieceIndex, arrowColor, pieceColor)) {
            throw new IllegalStateException("Arrow move is out of sync");
        }
        boolean middle = pieces.isPieceInMiddlePath(pieceIndex);
        boolean shown = middle
                ? board.showActivePieceInMiddlePath(pieceIndex, 0)
                : board.showActivePieceInInnerPath(pieceIndex, 0);
        if (!shown) {
            throw new IllegalStateException("Path view is out of sync");
        }
        String pathName = middle ? "the middle path (64)" : "the inner path (52)";
        if (remaining <= 0) {
            setStatus("Matching arrow! Piece entered " + pathName + ".");
            endTurnOrExtraTurn();
            return;
        }
        int routeSize = board.getActiveRouteSize(pieceIndex);
        int nextArrow = pieces.findArrowOnRemainingSteps(
                pieceIndex, remaining, board.getOwnArrowStep(pieceIndex));
        int target = nextArrow >= 0 ? nextArrow : remaining;
        if (!pieces.continueMoveAfterArrow(pieceIndex, target, routeSize)) {
            throw new IllegalStateException("Arrow move is not legal");
        }
        setStatus("Matching arrow! Piece moved to " + pathName + ".");
        if (nextArrow >= 0) {
            board.animatePiece(pieceIndex, 0, nextArrow,
                    () -> crossArrow(pieceIndex, remaining - nextArrow));
        } else {
            board.animatePiece(pieceIndex, 0, remaining,
                    () -> finishPieceMovement(pieceIndex));
        }
    }

    /** SRS 3.2.3.3.7 - a forward move from a question can also stop on an arrow. */
    private void finishEventMovement(int pieceIndex) {
        int ownArrow = board.getOwnArrowStep(pieceIndex);
        if (ownArrow >= 0 && ownArrow == pieces.getPieceStep(pieceIndex)
                && pieces.canUseArrows(pieceIndex)) {
            crossArrow(pieceIndex, 0);
            return;
        }
        endTurnOrExtraTurn();
    }

    private void finishPieceMovement(int pieceIndex) {
        String event = board.getSquareEventAtStep(
                pieceIndex, pieces.getPieceStep(pieceIndex));
        if ("QUESTION".equals(event)) {
            askQuestion(pieceIndex);
            return;
        }
        if ("SURPRISE".equals(event)) {
            showSurprise(pieceIndex);
            return;
        }
        setStatus("Moved "
                + pieces.getMoveStepsForSelectedPiece()
                + " squares.");
        endTurnOrExtraTurn();
    }

    private void askQuestion(int pieceIndex) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        QuestionDialog dialog = new QuestionDialog(owner);
        dialog.setVisible(true);
        boolean correct = dialog.isCorrect();
        int startStep = pieces.getPieceStep(pieceIndex);
        int targetStep = pieces.getAnswerMoveTarget(
            correct,
            board.getActiveRouteSize(pieceIndex)
        );
        setStatus(correct
            ? "Correct! Move 1 square forward."
            : "Wrong answer. Move 1 square back."
        );
        if (!pieces.applyEventMovement(
                pieceIndex,
                targetStep,
                board.getActiveRouteSize(pieceIndex))) {
            throw new IllegalStateException(
                    "Question movement is not legal");
        }
        board.animatePiece(
                pieceIndex,
                startStep,
                targetStep,
                () -> finishEventMovement(pieceIndex)
        );
    }

    private void showSurprise(int pieceIndex) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        SurpriseDialog dialog = new SurpriseDialog(owner);
        dialog.setVisible(true);
        int startStep = pieces.getPieceStep(pieceIndex);
        int targetStep = pieces.getSurpriseMoveTarget(
                board.getActiveRouteSize(pieceIndex));
        if (!pieces.applyEventMovement(
                pieceIndex,
                targetStep,
                board.getActiveRouteSize(pieceIndex))) {
            throw new IllegalStateException(
                    "Surprise movement is not legal");
        }
        setStatus("Surprise! Move back 3 squares.");
        board.animatePiece(
                pieceIndex,
                startStep,
                targetStep,
                this::endTurnOrExtraTurn
        );
    }

    private void endTurnOrExtraTurn() {
        if (turns.hasExtraTurn()) {
            turns.grantExtraTurn();
            board.clearHighlight();
            sidebar.setRollButton(true, "Roll Again");
            setStatus(game.getCurrentPlayerName() + " rolled a 6 - roll again!");
        } else {
            passTurn();
        }
    }

    private void passTurn() {
        turns.passTurn();
        sidebar.setFormula(" ");
        sidebar.setRollButton(turns.canRoll(), "Roll Dice");
        sidebar.setEndGameEnabled(game.getStage() == Enums.Stage.DONE);
        if (game.getStage() == Enums.Stage.DONE) {
            handleGameOver();
        }
        board.clearHighlight();
        refreshPlayers();
        setStatus(turns.canRoll()
                ? game.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice."
                : "Game finished: no active turns remain.");
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    /**
     * SRS 3.2.8.3.2 - 3.2.8.3.3 - the game is over: the controller saves it to the history file.
     * If saving fails, an error message is shown and the game continues normally.
     */
    private void handleGameOver() {
        if (gameOverHandled) {
            return;
        }
        gameOverHandled = true;
        turns.stopGameClock();
        String error = history.saveFinishedGame(game, turns);
        if (error != null) {
            historyNote = error;
            JOptionPane.showMessageDialog(this, error, "Game history", JOptionPane.ERROR_MESSAGE);
        } else if (game.isSavedToHistory()) {
            historyNote = "This game was saved to the game history.";
        }
    }

    private void showEndScreen() {
        gameTimer.stop();
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            // SRS 3.2.8.3.1 - the end screen shows the players by final place
            frame.setContentPane(new EndGameScreen(turns.getRanking(), difficulty,
                    turns.getElapsedTime(), historyNote));
            frame.setSize(700, 600);
            frame.setLocationRelativeTo(null);
            frame.revalidate();
            frame.repaint();
        }
    }

	private void backToMenu() {
	    if (!GameDialogs.confirmLeave(this)) {
	        return;
	    }
	    gameTimer.stop();
	    Window w = SwingUtilities.getWindowAncestor(this);
	    new MainMenuScreen().setVisible(true);
	    if (w != null) {
	        w.dispose();
	    }
	}

    // ------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------

    private void refreshPlayers() {
        sidebar.refreshPlayers(game.getCurrentPlayer());
    }

    private void setStatus(String text) {
        sidebar.setStatus(text);
    }

    /**
     * SRS 3.2.2.3.4 -
     * Creates a visual legend for the board square colors
     * and symbols displayed to the user.
     */
    private JPanel createLegend() {
        JPanel legend = new JPanel(new GridLayout(0, 1, 0, 2));
        legend.setOpaque(false);
        legend.add(sectionLabel("BOARD LEGEND"));
        legend.add(legendRow("#", new Color(225, 230, 239), "Start square"));
        legend.add(legendRow("\u2605", new Color(255, 230, 150), "Protection square"));
        legend.add(legendRow("\u2192", new Color(225, 230, 239), "Arrow to inner path"));
        legend.add(legendRow("", new Color(215, 230, 255), "Home path"));
        if (!"Easy".equals(difficulty)) {
            legend.add(legendRow("?", BoardPanel.Q_EASY, "Question square"));
            legend.add(legendRow("!", BoardPanel.SURPRISE, "Surprise square"));
        }
        return legend;
    }

    private JPanel legendRow(String symbol, Color bg, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        row.setOpaque(false);
        JLabel s = new JLabel(symbol, SwingConstants.CENTER);
        s.setOpaque(true);
        s.setBackground(bg);
        s.setForeground(INK);
        s.setFont(new Font("Segoe UI", Font.BOLD, 12));
        s.setPreferredSize(new Dimension(19, 19));
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        row.add(s);
        row.add(label);
        return row;
    }

    static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(MUTED);
        return label;
    }

    static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // Rounded cards are painted locally; no external Swing library required.

    static class ActionButton extends JButton {
        private final Color color;
        private boolean hovered;
        ActionButton(String text, Color color) {
            super(text);
            this.color = color;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            Color bg = !isEnabled() ? new Color(185, 195, 212)
                : hovered ? color.brighter() : color;
            g.setColor(bg);
            g.fillRoundRect(0, 0, getWidth(), getHeight() - 1, 14, 14);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private void startTimer() {
        // SRS 3.2.1.3.3 - the game clock is kept by the model; the view only shows it.
        // It shows 00:00:00 until the first roll and then counts the game time.
        gameTimer = new Timer(1000, e -> {
            timerLabel.setText(turns.getElapsedTime());
        });
        gameTimer.start();
    }

    static class GameCardPanel extends JPanel {
            private final Color fill;
            private final int radius;
            GameCardPanel(Color fill, int radius) {
                this.fill = fill;
                this.radius = radius;
                setOpaque(false);
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GameScreen.quality(g2);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.dispose();
                super.paintComponent(g);
            }
    }
}
