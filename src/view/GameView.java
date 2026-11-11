package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import java.util.Random;
import controller.GameController;
import enums.RollAction;
import model.Game;

public class GameView extends JPanel {

    private static final Color BACKGROUND = new Color(14, 22, 54);
    private static final Color PRIMARY = new Color(75, 110, 248);
    private static final Color MUTED = new Color(110, 122, 150);
    private static final Color INK = new Color(14, 23, 55);
    private static final Color LIGHT = new Color(244, 247, 254);

    private final String[] names;
    private final String[] colors;
    private final String difficulty;

    private final BoardPanel board;
    private final DicePanel dice;
    private final JLabel timerLabel;
    private final JLabel turnLabel;
    private final JLabel formulaLabel;
    private final JTextArea statusArea;
    private final JButton rollButton;
    private final JButton endGameButton;
    private final JPanel playersPanel;

    private Timer gameTimer;
    private int secondsElapsed = 0;
    private final GameController controller;

    /** Default constructor kept for two-player compatibility. */
    public GameView() {
        this(new String[]{"Player 1", "Player 2"}, new String[]{"Red", "Blue"}, "Easy");
    }

    public GameView(String[] names, String[] colors, String difficulty) {
        this.names = names;
        this.colors = colors;
        this.difficulty = difficulty == null ? "Easy" : difficulty;
        Game game = new Game(names, colors, this.difficulty);
        this.controller = new GameController(game);

        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(15, 20, 18, 20));

        // Header / title bar – same navy-and-purple style as the main menu.
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

        RoundedPanel boardCard = new RoundedPanel(Color.WHITE, 24);
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

        board = new BoardPanel(this.difficulty, colors);
        board.setOpaque(false);
        board.setPieceClickListener(new BoardPanel.PieceClickListener() {
            @Override public void castlePieceClicked(int slot) { onCastlePieceClicked(slot); }
            @Override public void boardPieceClicked(int pieceIndex) { onBoardPieceClicked(pieceIndex); }
        });
        boardCard.add(board, BorderLayout.CENTER);
        arena.add(boardCard, BorderLayout.CENTER);

        // Compact control dashboard – all primary controls remain visible without scrolling.
        RoundedPanel side = new RoundedPanel(Color.WHITE, 24);
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(330, 0));
        side.setBorder(new EmptyBorder(18, 18, 12, 18));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JLabel eyebrow = sectionLabel("YOUR GAME");
        eyebrow.setForeground(PRIMARY);
        stack.add(leftAligned(eyebrow));
        stack.add(Box.createVerticalStrut(4));
        turnLabel = new JLabel(" ");
        turnLabel.setForeground(INK);
        turnLabel.setFont(new Font("Segoe UI", Font.BOLD, 19));
        stack.add(leftAligned(turnLabel));
        stack.add(Box.createVerticalStrut(12));

        stack.add(leftAligned(sectionLabel("PLAYERS")));
        stack.add(Box.createVerticalStrut(6));
        playersPanel = new JPanel();
        playersPanel.setOpaque(false);
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        stack.add(leftAligned(playersPanel));
        stack.add(Box.createVerticalStrut(6));

        stack.add(leftAligned(sectionLabel("DICE")));
        stack.add(Box.createVerticalStrut(4));
        dice = new DicePanel(!controller.isEasyGame(), "Hard".equals(this.difficulty) ? "ULTRA BOOST" : "BOOST",
        	    controller.getBoostDieFaces());
        stack.add(leftAligned(dice));
        formulaLabel = new JLabel(" ");
        formulaLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formulaLabel.setForeground(PRIMARY);
        stack.add(leftAligned(formulaLabel));
        stack.add(Box.createVerticalStrut(6));

        stack.add(leftAligned(sectionLabel("GAME UPDATES")));
        stack.add(Box.createVerticalStrut(7));

        // Compact instruction card: no Gemini-like sparkle and no internal scrollbar.
        RoundedPanel updateCard = new RoundedPanel(new Color(236, 242, 255), 16);
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
        stack.add(leftAligned(updateCard));
        stack.add(Box.createVerticalStrut(10));

        rollButton = createButton("Roll Dice", PRIMARY);
        rollButton.addActionListener(e -> onRoll());
        stack.add(leftAligned(rollButton));
        stack.add(Box.createVerticalStrut(7));
        endGameButton = createButton("End Game", new Color(35, 172, 130));
        endGameButton.setEnabled(false);
        endGameButton.addActionListener(e -> showEndScreen());
        stack.add(leftAligned(endGameButton));
        stack.add(Box.createVerticalStrut(7));
        JButton menuButton = createButton("Leave game", new Color(103, 117, 143));
        menuButton.addActionListener(e -> backToMenu());
        stack.add(leftAligned(menuButton));
        // The board legend is accessible from the board header.
        // No outer JScrollPane: the right-hand scrollbar is completely removed.
        side.add(stack, BorderLayout.CENTER);
        arena.add(side, BorderLayout.EAST);

        refreshPlayers();
        setStatus(names[0] + "'s turn. Click Roll Dice to start.");
        startTimer();
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
        if (!controller.canRoll()) return;
        if ("Next Turn".equals(rollButton.getText())) {
            passTurn();
            return;
        }

        // SRS 3.2.1.3.3 - the game clock starts with the first roll.
        controller.startGameClock();
        board.setActivePlayerColor(controller.getCurrentPlayerColor());
        int stepDie = controller.rollStepDie();
        int boostDie = controller.rollBoostDie();
        formulaLabel.setText(" ");
        rollButton.setEnabled(false);
        rollButton.setText("Rolling...");
        dice.animate(stepDie, boostDie, () -> finishRoll(stepDie, boostDie));
    }


    private void finishRoll(int stepDie, int boostDie) {

        // בדיקה אם התקבלו שלוש שישיות ברצף
        if (controller.registerRollAndCheckThirdSix(stepDie)) {
            String player = controller.getCurrentPlayerName();
            passTurn();

            setStatus("Third 6 in a row for " + player
                + "! The roll is cancelled. "
                + controller.getCurrentPlayerName()
                + "'s turn. Click Roll Dice.");
            return;
        }

        // קביעת הפעולה האפשרית בעקבות ההטלה
        RollAction action = controller.finishRoll(stepDie, boostDie);

        // אם הוטל 6 ויש כלי על הלוח, מציגים אפשרות בחירה
        if (action == RollAction.TAKE_OUT
                && controller.canChooseBoardMoveInsteadOfTakeOut()
                && controller.hasAnyLegalMove(getCurrentRouteSizes())) {

        	int choice = GameDialogs.chooseMove(this);


            // בחירה בהזזת כלי קיים במקום הוצאת כלי
            if (choice == 1
                    && controller.chooseBoardMoveInsteadOfTakeOut()) {
                action = RollAction.MOVE;
            }
        }

        // אפשרות 1: הוצאת כלי מהטירה
        if (action == RollAction.TAKE_OUT) {
            setStatus(
                "Rolled a 6! Click a highlighted piece in your castle."
            );

            board.highlightCastlePieces(
                controller.consumeFirstTurnPulse()
            );

            rollButton.setEnabled(false);
            rollButton.setText("Select Your Piece");

        // אפשרות 2: הזזת כלי שכבר נמצא על הלוח
        } else if (action == RollAction.MOVE) {

            // בודקים אם לפחות אחד מארבעת הכלים יכול לזוז
            if (!controller.hasAnyLegalMove(getCurrentRouteSizes())) {
                passWhenNoLegalMove();
                return;
            }

            if (controller.getPendingSteps() == 0) {

                setStatus(
                    "Boost result is 0. Select a home-path piece to move "
                    + stepDie + " squares."
                );

            } else {

                setStatus(
                    "Move " + controller.getPendingSteps()
                    + " squares on the shared path, or "
                    + stepDie
                    + " in the home path. Select a piece."
                );
            }
            board.highlightBoardPiece(
                controller.consumeFirstTurnPulse()
            );

            rollButton.setEnabled(false);
            rollButton.setText("Select Your Piece");


         // SRS 3.2.3.3.3 - טיפול בתוצאה 0 ללא מהלך חוקי
        } else if (controller.getPendingSteps() == 0) {

        	// בהטלת 6 אין להעביר את התור לשחקן הבא
            if (controller.hasExtraTurn()) {
                passWhenNoLegalMove();
                return;
            }
        	
            // שמירת שם השחקן שלא יכול לבצע מהלך
            String previousPlayer = controller.getCurrentPlayerName();

            // העברת התור לשחקן הבא באופן אוטומטי
            passTurn();

            // הצגת הסיבה למעבר התור
            setStatus("Boost result is 0. " + previousPlayer
                    + " has no legal move. "
                    + controller.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice.");

        } else {

            // SRS 3.2.3.3.6 - אין מהלך חוקי גם כשהתוצאה חיובית
            passWhenNoLegalMove();
        }
    }

    // SRS 3.2.3.3.6 - קבלת אורך המסלול של כל כלי
    private int[] getCurrentRouteSizes() {

        int[] routeSizes = new int[Game.PIECES_PER_PLAYER];

        // בדיקת המסלול הנוכחי של כל אחד מארבעת הכלים
        for (int piece = 0; piece < Game.PIECES_PER_PLAYER; piece++) {
            routeSizes[piece] = board.getActiveRouteSize(piece);
        }

        return routeSizes;
    }

    // SRS 3.2.3.3.6 - טיפול במצב ללא מהלך חוקי
    private void passWhenNoLegalMove() {

        String previousPlayer = controller.getCurrentPlayerName();

        // הטלת 6 מעניקה תור נוסף גם כשאין מהלך חוקי
        if (controller.hasExtraTurn()) {

            controller.grantExtraTurn();
            board.clearHighlight();

            rollButton.setEnabled(true);
            rollButton.setText("Roll Again");

            setStatus(previousPlayer
                    + " has no legal move. Rolled a 6 - roll again.");

            return;
        }

        // בהטלה רגילה מעבירים את התור לשחקן הבא
        passTurn();

        if (controller.canRoll()) {

            String reason =
                    controller.getPendingSteps() == 0
                    && !controller.isEasyGame()
                    ? "Boost result is 0. " : "";

            setStatus(reason + previousPlayer
                    + " has no legal move. "
                    + controller.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice.");
        }
    }

    private void onCastlePieceClicked(int slot) {

        // בדיקת חוקיות לפני שינוי התצוגה
        if (!controller.canTakeOutActivePiece(slot)) {
            return;
        }

        // הצגת הכלי במשבצת ההתחלה
        if (!board.placeActivePieceOnStart(slot)) {
            return;
        }

        // עדכון מצב הכלי והטירה במודל
        if (!controller.takeOutActivePiece(slot)) {
            throw new IllegalStateException(
                    "Castle state is out of sync");
        }

        // הוצאת כלי עם 6 מאפשרת הטלה נוספת
        setStatus(
                "Piece on start! You earned another turn. Click Roll Dice.");

        rollButton.setEnabled(true);
        rollButton.setText("Roll Dice");
    }


    /**
     * SRS 3.2.4.3.1 -
     * Handles the user's selection of the active board piece.
     * Game-rule validation and movement decisions are delegated
     * to GameController.
     */
    private void onBoardPieceClicked(int pieceIndex) {

        // קבלת אורך המסלול הנוכחי של הכלי
        int routeSize = board.getActiveRouteSize(pieceIndex);


        // SRS 3.2.4.3.4 - בדיקת כניסה מדויקת לבית הסיום
        if (controller.tryFinishPiece(pieceIndex, routeSize)) {

            // סנכרון התצוגה עם המודל
            if (!board.showActivePieceFinished(pieceIndex)) {
                throw new IllegalStateException(
                        "Finish state is out of sync");
            }

            String player = controller.getCurrentPlayerName();
            int finished = board.getActiveFinishedPieceCount();

            setStatus(player + ": piece reached the goal ("
                    + finished + "/4).");

            if (controller.isCurrentPlayerFinished()) {

                // משתתף שסיים את כל כליו אינו מקבל תור נוסף
                passTurn();

                // אם לא נותרה תחרות פעילה, מאפשרים מסך סיום
                if (controller.getStage() == enums.Stage.DONE) {
                    endGameButton.setEnabled(true);

                    setStatus(
                        "Game complete! Click End Game to see the results.");
                }

            } else {

                // ממשיכים לפי חוק התור הנוסף או מעבר תור רגיל
                endTurnOrExtraTurn();
            }

            return;
        }

        // בדיקה האם המהלך מכניס את הכלי למסלול הבית
        if (controller.tryEnterHomePath(pieceIndex, routeSize)) {

            // קבלת המיקום שחושב ונשמר במודל
            int homeStep = controller.getPieceStep(pieceIndex);

            // עדכון מיקום הכלי בתצוגת הלוח
            if (!board.showActivePieceInHomePath(pieceIndex, homeStep)) {
                throw new IllegalStateException(
                    "Home path view is out of sync"
                );
            }

            // סיום המהלך ועדכון מצב התור
            board.clearHighlight();
            setStatus("Piece entered the home path!");
            endTurnOrExtraTurn();
            return;
        }

        // אם אין כניסה למסלול הבית, בודקים תנועה רגילה
        int targetStep = controller.validatePieceMove(pieceIndex, routeSize);
        if (targetStep < 0) {
            setStatus("The selected piece cannot make this move.");
            return;
        }

        int startStep = controller.getPieceStep(pieceIndex);

        if (!controller.movePiece(pieceIndex, targetStep, routeSize)) {
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

    // המשך התור לאחר שהכלי סיים את התנועה
    private void finishPieceMovement(int pieceIndex) {

        // בדיקות האכילה והחסימה ישולבו כאן
        // במסגרת דרישות 3.2.5

        // בדיקת המשבצת שבה הכלי נעצר
        String event = board.getSquareEventAtStep(
                pieceIndex, controller.getPieceStep(pieceIndex));

        // הפעלת שאלה רק בנחיתה על משבצת שאלה
        if ("QUESTION".equals(event)) {
            askQuestion(pieceIndex);
            return;
        }

        // הפעלת הפתעה רק בנחיתה על משבצת הפתעה
        if ("SURPRISE".equals(event)) {
            showSurprise(pieceIndex);
            return;
        }

        // אם אין אירוע, מסיימים את המהלך
        setStatus("Moved "
                + controller.getMoveStepsForSelectedPiece()
                + " squares.");

        endTurnOrExtraTurn();
    }
    
    private void askQuestion(int pieceIndex) {
        Window owner = SwingUtilities.getWindowAncestor(this);

        QuestionDialog dialog = new QuestionDialog(owner);
        dialog.setVisible(true);

        boolean correct = dialog.isCorrect();

        int startStep = controller.getPieceStep(pieceIndex);

        int targetStep = controller.getAnswerMoveTarget(
            correct,
            board.getActiveRouteSize(pieceIndex)
        );

        setStatus(correct
            ? "Correct! Move 1 square forward."
            : "Wrong answer. Move 1 square back."
        );

        // שמירת התנועה מהשאלה גם במודל
        if (!controller.applyEventMovement(
                pieceIndex,
                targetStep,
                board.getActiveRouteSize(pieceIndex))) {

            throw new IllegalStateException(
                    "Question movement is not legal");
        }

        // מבצעים את התנועה ומסיימים את התור
        // בלי להפעיל שוב משבצת מיוחדת
        board.animatePiece(
                pieceIndex,
                startStep,
                targetStep,
                this::endTurnOrExtraTurn
        );

    }

    private void showSurprise(int pieceIndex) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Ludo - Surprise",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setUndecorated(true);
        dialog.setSize(510, 390);
        dialog.setResizable(false);

        JPanel background = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(29, 31, 78),
                        getWidth(), getHeight(), new Color(72, 57, 130)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
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

        JLabel tag = new JLabel("SURPRISE SQUARE");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tag.setForeground(new Color(108, 78, 198));
        tag.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("A LITTLE DETOUR!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(30, 37, 79));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("You've landed on a surprise square.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(103, 111, 136));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel eventCard = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
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
        JLabel amount = new JLabel("-3 SQUARES");
        amount.setFont(new Font("Segoe UI", Font.BOLD, 27));
        amount.setForeground(new Color(105, 70, 199));
        amount.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel explanation = new JLabel("Move your piece three squares back");
        explanation.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        explanation.setForeground(new Color(91, 95, 119));
        explanation.setAlignmentX(Component.CENTER_ALIGNMENT);
        eventText.add(amount);
        eventText.add(Box.createVerticalStrut(4));
        eventText.add(explanation);
        eventCard.add(eventText);

        JButton continueButton = new JButton("CONTINUE GAME") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                        ? new Color(86, 77, 218) : new Color(77, 104, 241));
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
        continueButton.addActionListener(e -> dialog.dispose());

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
        dialog.setContentPane(background);
        dialog.getRootPane().setDefaultButton(continueButton);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

     // קבלת מיקום הכלי לפני הפעלת ההפתעה
        int startStep = controller.getPieceStep(pieceIndex);

        // חישוב התנועה אחורה
        int targetStep = controller.getSurpriseMoveTarget(
                board.getActiveRouteSize(pieceIndex));

        // עדכון המודל
        if (!controller.applyEventMovement(
                pieceIndex,
                targetStep,
                board.getActiveRouteSize(pieceIndex))) {

            throw new IllegalStateException(
                    "Surprise movement is not legal");
        }

        setStatus("Surprise! Move back 3 squares.");

        // לאחר ההפתעה מסיימים רק את התור,
        // ולא את המשחק כולו
        board.animatePiece(
                pieceIndex,
                startStep,
                targetStep,
                this::endTurnOrExtraTurn
        );
    }

    private void endTurnOrExtraTurn() {
        if (controller.hasExtraTurn()) {
            controller.grantExtraTurn();
            board.clearHighlight();
            rollButton.setEnabled(true);
            rollButton.setText("Roll Again");
            setStatus(controller.getCurrentPlayerName() + " rolled a 6 - roll again!");
        } else {
            passTurn();
        }
    }

    private void passTurn() {

        // מעבר למשתתף הבא שלא סיים
        controller.passTurn();

        // ניקוי מצב ההטלה והלוח
        dice.clear();
        formulaLabel.setText(" ");

        // מאפשרים הטלה רק אם נותר משחק פעיל
        rollButton.setEnabled(controller.canRoll());
        rollButton.setText("Roll Dice");

        // אם המשחק הסתיים, מאפשרים מעבר למסך הסיום
        endGameButton.setEnabled(controller.getStage() == enums.Stage.DONE);

        board.clearHighlight();
        refreshPlayers();

        // הודעה לפי מצב המשחק
        setStatus(controller.canRoll()
                ? controller.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice."
                : "Game finished: no active turns remain.");
    }

    private void finishScenario() {
        controller.finishGame();
        rollButton.setEnabled(false);
        endGameButton.setEnabled(true);
        setStatus("Game complete! Click End Game to see the results.");
    }


    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    private void showEndScreen() {
        gameTimer.stop();
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            frame.setContentPane(new EndGameScreen(names, colors, difficulty, formatTime(secondsElapsed)));
            frame.setSize(700, 600);
            frame.setLocationRelativeTo(null);
            frame.revalidate();
            frame.repaint();
        }
    }


	private void backToMenu() {
	
	    // הצגת חלון אישור יציאה מעוצב
	    if (!GameDialogs.confirmLeave(this)) {
	        return;
	    }
	
	    // עצירת שעון המשחק לאחר אישור היציאה
	    gameTimer.stop();
	
	    // מעבר לתפריט הראשי וסגירת חלון המשחק
	    Window w = SwingUtilities.getWindowAncestor(this);
	    new MainMenuView().setVisible(true);
	
	    if (w != null) {
	        w.dispose();
	    }
	}


    // ------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------

    private void refreshPlayers() {
        playersPanel.removeAll();
        int currentPlayer = controller.getCurrentPlayer();
        turnLabel.setText(names[currentPlayer] + "'s turn");
        for (int i = 0; i < names.length; i++) {
            final boolean current = i == currentPlayer;
            JPanel chip = new RoundedPanel(current ? new Color(235, 240, 255) : LIGHT, 15);
            chip.setLayout(new BorderLayout(9, 0));
            chip.setBorder(new EmptyBorder(3, 10, 3, 10));
            chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
            chip.setPreferredSize(new Dimension(285, 29));
            Color pieceColor = BoardPanel.colorOf(colors[i]);
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Dialog", Font.BOLD, 19));
            dot.setForeground(pieceColor);
            chip.add(dot, BorderLayout.WEST);
            JLabel name = new JLabel(names[i]);
            name.setFont(new Font("Segoe UI", current ? Font.BOLD : Font.PLAIN, 13));
            name.setForeground(INK);
            chip.add(name, BorderLayout.CENTER);
            if (current) {
                JLabel playing = new JLabel("PLAYING");
                playing.setForeground(PRIMARY);
                playing.setFont(new Font("Segoe UI", Font.BOLD, 10));
                chip.add(playing, BorderLayout.EAST);
            }
            playersPanel.add(chip);
            if (i < names.length - 1) playersPanel.add(Box.createVerticalStrut(3));
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    private void setStatus(String text) {
        statusArea.setText(text);
        statusArea.setToolTipText(text);
        statusArea.setCaretPosition(0);
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
        legend.add(legendRow("★", new Color(255, 230, 150), "Protection square"));
        legend.add(legendRow("→", new Color(225, 230, 239), "Arrow to inner path"));
        legend.add(legendRow("", new Color(215, 230, 255), "Home path"));
        if (!controller.isEasyGame()) {
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

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(MUTED);
        return label;
    }

    private static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    private JButton createButton(String text, Color bg) {
        JButton b = new ActionButton(text, bg);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setPreferredSize(new Dimension(285, 36));
        return b;
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // Rounded cards are painted locally; no external Swing library required.
    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final int radius;
        RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ActionButton extends JButton {
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
            secondsElapsed = (int) controller.getElapsedSeconds();
            timerLabel.setText(controller.getElapsedTime());
        });
        gameTimer.start();
    }

    static String formatTime(int seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

   
    private static class DicePanel extends JPanel {
        private final boolean showBoost;
        private final String boostName;
        private int stepValue = 0;
        private int boostValue = 0;
        private Timer rollTimer;
        private int rollFrames;

        private final int boostFaces;

        DicePanel(boolean showBoost, String boostName, int boostFaces) {
            this.showBoost = showBoost;
            this.boostName = boostName;
            this.boostFaces = boostFaces;
            setOpaque(false);
            setPreferredSize(new Dimension(285, 77));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 77));
        }

        void show(int step, int boost) {
            stepValue = step;
            boostValue = boost;
            repaint();
        }

        void animate(int step, int boost, Runnable onFinished) {
            if (rollTimer != null && rollTimer.isRunning()) {
                rollTimer.stop();
            }
            rollFrames = 0;
            Random animationRandom = new Random();
            rollTimer = new Timer(65, null);
            rollTimer.addActionListener(e -> {
                rollFrames++;
                stepValue = animationRandom.nextInt(6) + 1;
                boostValue = showBoost ? animationRandom.nextInt(boostFaces) + 1 : 0;
                if (rollFrames >= 9) {
                    rollTimer.stop();
                    stepValue = step;
                    boostValue = boost;
                    repaint();
                    if (onFinished != null) onFinished.run();
                } else {
                    repaint();
                }
            });
            rollTimer.start();
        }

        void clear() {
            show(0, 0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawDie(g2, 8, 0, 58, stepValue, true);
            caption(g2, "STEP DIE", 6, 73, 70);
            if (showBoost) {
                drawDie(g2, 120, 0, 58, boostValue, false);
                caption(g2, boostName, 112, 73, 85);
            }
            g2.dispose();
        }

        private void caption(Graphics2D g2, String text, int x, int y, int w) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(MUTED);
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, x + (w - tw) / 2, y);
        }

        private void drawDie(Graphics2D g2, int x, int y, int s, int value, boolean pips) {
            g2.setColor(pips ? new Color(238, 242, 255) : new Color(255, 241, 218));
            g2.fillRoundRect(x, y, s, s, 16, 16);
            g2.setColor(pips ? new Color(85, 117, 235) : new Color(238, 164, 71));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRoundRect(x, y, s, s, 16, 16);
            if (value == 0) {
                g2.setFont(new Font("Arial", Font.BOLD, 30));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("?", x + (s - fm.stringWidth("?")) / 2, y + (s + fm.getAscent()) / 2 - 4);
                return;
            }
            if (!pips) {
                g2.setColor(new Color(200, 110, 0));
                g2.setFont(new Font("Arial", Font.BOLD, 36));
                FontMetrics fm = g2.getFontMetrics();
                String t = String.valueOf(value);
                g2.drawString(t, x + (s - fm.stringWidth(t)) / 2, y + (s + fm.getAscent()) / 2 - 4);
                return;
            }
            g2.setColor(new Color(56, 78, 163));
            int d = s / 6;
            int l = x + s / 4, m = x + s / 2, r = x + 3 * s / 4;
            int t = y + s / 4, c = y + s / 2, b = y + 3 * s / 4;
            int[][] spots;
            switch (value) {
                case 1: spots = new int[][]{{m, c}}; break;
                case 2: spots = new int[][]{{l, t}, {r, b}}; break;
                case 3: spots = new int[][]{{l, t}, {m, c}, {r, b}}; break;
                case 4: spots = new int[][]{{l, t}, {r, t}, {l, b}, {r, b}}; break;
                case 5: spots = new int[][]{{l, t}, {r, t}, {m, c}, {l, b}, {r, b}}; break;
                default: spots = new int[][]{{l, t}, {r, t}, {l, c}, {r, c}, {l, b}, {r, b}}; break;
            }
            for (int[] p : spots) {
                g2.fillOval(p[0] - d / 2, p[1] - d / 2, d, d);
            }
        }
    }
}
