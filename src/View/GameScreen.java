package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;
import Controller.HistoryController;
import Controller.MovementController;
import Controller.GameController;
import Controller.TurnController;
import Enums.RollAction;
import Enums.SquareType;

public class GameScreen extends JPanel {

    static final Color BACKGROUND = new Color(14, 22, 54);
    static final Color PRIMARY = new Color(75, 110, 248);
    static final Color MUTED = new Color(110, 122, 150);
    static final Color INK = new Color(14, 23, 55);

    private final String difficulty;

    // 3D prototype: the whole game is shown on a 2.5D board (Board3DPanel) instead of the flat one.
    private final boolean threeD;
    private Board3DPanel board3D;
    private final WorldTheme theme;
    private JLabel boardTitle;

    private final BoardPanel board;
    private final JLabel timerLabel;
    private final GameSidebarPanel sidebar;

    private Timer gameTimer;

    // SRS 3.2.8 - the finished game is saved once; the message is shown on the end screen
    private boolean gameOverHandled;
    private String historyNote = "";
    // the last capture message stays on the status line until the next roll
    private String captureNote = "";
    private final HistoryController history;
    private final MovementController pieces;
    private final GameController turns;
    private final TurnController turnController;

    public GameScreen(GameController turns, String difficulty) {
        this(turns, difficulty, false);
    }

    /** @param threeD true = play on the 3D board (3D prototype) */
    public GameScreen(GameController turns, String difficulty, boolean threeD) {
        this(turns, difficulty, threeD, Themes.defaultTheme());
    }

    /** @param theme the world of the 3D version (Strategy pattern, see {@link WorldTheme}); ignored in 2D */
    GameScreen(GameController turns, String difficulty, boolean threeD, WorldTheme theme) {
        this.threeD = threeD;
        this.theme = theme;
        this.turns = turns;
        this.difficulty = difficulty == null ? "Easy" : difficulty;
        this.history = HistoryController.getInstance();
        this.pieces = turns.getMovementController();
        this.turnController = turns.getTurnController();
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
        if (threeD) {
            JLabel mode3d = new JLabel("  3D  ·  " + theme.title().toUpperCase() + "  ");
            mode3d.setOpaque(true);
            mode3d.setBackground(new Color(255, 190, 75));
            mode3d.setForeground(INK);
            mode3d.setFont(new Font("Segoe UI", Font.BOLD, 12));
            mode3d.setBorder(new EmptyBorder(3, 10, 3, 10));
            mode3d.setToolTipText("3D world: " + theme.title());
            headerBadges.add(mode3d);
        }
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
        boardTitle = new JLabel(threeD ? "THE 3D BOARD" : "THE BOARD");
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
        board = new BoardPanel(turns);
        board.setOpaque(false);
        board.setCastlePieceClickListener(this::onCastlePieceClicked);
        board.setBoardPieceClickListener(this::onBoardPieceClicked);
        if (threeD) {
            // the flat board is not shown: it only keeps the piece positions, the 3D board draws them
            board.setSize(700, 700);
            board.setStepDelay(190);
            board3D = new Board3DPanel(board, theme);
            boardCard.add(board3D, BorderLayout.CENTER);
        } else {
            boardCard.add(board, BorderLayout.CENTER);
        }
        arena.add(boardCard, BorderLayout.CENTER);
        sidebar = new GameSidebarPanel(
                turns.getPlayerNames(),
                turns.getPlayerColors(),
                this.difficulty,
                turnController.getBoostDieFaces(),
                this::onRoll,
                this::showEndScreen,
                this::backToMenu
        );
        GameCardPanel side = new GameCardPanel(Color.WHITE, 24);
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(330, 0));
        side.add(sidebar, BorderLayout.CENTER);
        arena.add(side, BorderLayout.EAST);
        if (board3D != null) {
            // the green "Go!" button of the 3D view rolls the dice when no piece can be moved
            board3D.setRollAction(this::onRoll, sidebar::isRollEnabled);
        }
        refreshPlayers();
        setStatus(turns.getPlayerNames()[0] + "'s turn. Click Roll Dice to start."
                + (threeD ? " (3D board)" : ""));
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
        if (!turnController.canRoll()) return;
        if ("Next Turn".equals(sidebar.getRollButtonText())) {
            passTurn();
            return;
        }
        // SRS 3.2.1.3.3 - the game clock starts with the first roll.
        captureNote = "";
        turns.startGameClock();
        board.setActivePlayerColor(turns.getCurrentPlayerColor());

        sidebar.setBoostMode(
                turnController.isUltraBoostRollPending(),
                "Hard".equals(difficulty) ? 6 : 3);

        int stepDie = turnController.rollStepDie();
        int boostDie = turnController.rollBoostDie();
        sidebar.setFormula(" ");
        sidebar.setRollButton(false, "Rolling...");
        sidebar.animateDice(stepDie, boostDie, () -> finishRoll(stepDie, boostDie));
    }

    private void finishRoll(int stepDie, int boostDie) {
        if (turnController.registerRollAndCheckThirdSix(stepDie)) {
            String player = turns.getCurrentPlayerName();
            passTurn();
            setStatus("Third 6 in a row for " + player
                + "! The roll is cancelled. "
                + turns.getCurrentPlayerName()
                + "'s turn. Click Roll Dice.");
            return;
        }
        RollAction action = turnController.finishRoll(stepDie, boostDie);
        if (action == RollAction.TAKE_OUT
                && pieces.canChooseBoardMoveInsteadOfTakeOut()
                && hasLegalMove()) {
        	int choice = GameDialogs.chooseMove(this);
            if (choice == 1
                    && pieces.chooseBoardMoveInsteadOfTakeOut()) {
                action = RollAction.MOVE;
            }
        }
        if (action == RollAction.TAKE_OUT && isTakeOutBlockedForAll()) {
            // SRS 3.2.5.3.2 - a Double Block on the start square keeps the pieces in the castle
            if (pieces.canChooseBoardMoveInsteadOfTakeOut() && hasLegalMove()
                    && pieces.chooseBoardMoveInsteadOfTakeOut()) {
                action = RollAction.MOVE;
            } else {
                captureNote += "A Double Block of another player stands on your start square. ";
                passWhenNoLegalMove();
                return;
            }
        }
        if (action == RollAction.TAKE_OUT) {
            setStatus(
                "Rolled a 6! Click a highlighted piece in your castle."
            );
            board.highlightCastlePieces(
                turnController.consumeFirstTurnPulse()
            );
            sidebar.setRollButton(false, "Select Your Piece");
        } else if (action == RollAction.MOVE) {
            if (!hasLegalMove()) {
                passWhenNoLegalMove();
                return;
            }
            if (turns.getPendingSteps() == 0) {
                setStatus(
                    "Boost result is 0. Select a home-path piece to move "
                    + stepDie + " squares."
                );
            } else {
                setStatus(
                    "Move " + turns.getPendingSteps()
                    + " squares on the shared path, or "
                    + stepDie
                    + " in the home path. Select a piece."
                );
            }
            board.highlightBoardPiece(
                turnController.consumeFirstTurnPulse()
            );
            sidebar.setRollButton(false, "Select Your Piece");
        } else if (turns.getPendingSteps() == 0) {
            if (turnController.hasExtraTurn()) {
                passWhenNoLegalMove();
                return;
            }
            String previousPlayer = turns.getCurrentPlayerName();
            passTurn();
            setStatus("Boost result is 0. " + previousPlayer
                    + " has no legal move. "
                    + turns.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice.");
        } else {
            passWhenNoLegalMove();
        }
    }

    /** A legal move exists, counting that a Double Block can stop a piece (SRS 3.2.5.3.4). */
    private boolean hasLegalMove() {
        return pieces.hasAnyLegalMove(getCurrentRouteSizes(), getCurrentArrowSteps(),
                turns.getPiecesStuckBehindBlock());
    }

    /** True when every piece in the castle is kept in by a Double Block on the start square. */
    private boolean isTakeOutBlockedForAll() {
        for (int slot = 0; slot < GameController.PIECES_PER_PLAYER; slot++) {
            if (pieces.canTakeOutActivePiece(slot) && !turns.isTakeOutBlocked(slot)) {
                return false;
            }
        }
        return true;
    }

    private int[] getCurrentRouteSizes() {
        int[] routeSizes = new int[GameController.PIECES_PER_PLAYER];
        for (int piece = 0; piece < GameController.PIECES_PER_PLAYER; piece++) {
            routeSizes[piece] = board.getActiveRouteSize(piece);
        }
        return routeSizes;
    }

    /** SRS 3.2.4.3.2 - for every piece, the step of its own arrow on its current path (-1 if none). */
    private int[] getCurrentArrowSteps() {
        int[] arrowSteps = new int[GameController.PIECES_PER_PLAYER];
        for (int piece = 0; piece < GameController.PIECES_PER_PLAYER; piece++) {
            arrowSteps[piece] = board.getOwnArrowStep(piece);
        }
        return arrowSteps;
    }

    private void passWhenNoLegalMove() {
        String previousPlayer = turns.getCurrentPlayerName();
        if (turnController.hasExtraTurn()) {
            turnController.grantExtraTurn();
            board.clearHighlight();
            sidebar.setRollButton(true, "Roll Again");
            setStatus(previousPlayer
                    + " has no legal move. Rolled a 6 - roll again.");
            return;
        }
        passTurn();
        if (turnController.canRoll()) {
            String reason =
                    turns.getPendingSteps() == 0
                    && !"Easy".equals(difficulty)
                    ? "Boost result is 0. " : "";
            setStatus(reason + previousPlayer
                    + " has no legal move. "
                    + turns.getCurrentPlayerName()
                    + "'s turn. Click Roll Dice.");
        }
    }

    private void onCastlePieceClicked(int slot) {
        if (!pieces.canTakeOutActivePiece(slot)) {
            return;
        }
        if (turns.isTakeOutBlocked(slot)) {
            setStatus("A Double Block of another player stands on your start square. "
                    + "You cannot take a piece out now.");
            return;
        }
        if (!board.placeActivePieceOnStart(slot)) {
            return;
        }
        if (!turns.takeOutPiece(slot)) {
            throw new IllegalStateException(
                    "Castle state is out of sync");
        }
        setStatus(
                "Piece on start! You earned another turn. Click Roll Dice.");
        sidebar.setRollButton(true, "Roll Dice");
    }

    private void stopBoard3D() {
        if (board3D != null) {
            board3D.stop();
        }
    }

    /**
     * SRS 3.2.4.3.1 -
     */
    private void onBoardPieceClicked(int pieceIndex) {
        int routeSize = board.getActiveRouteSize(pieceIndex);
        if (handleBlockedMove(pieceIndex, routeSize)) {
            return;
        }
        if (turns.finishPiece(pieceIndex, routeSize)) {
            if (!board.showActivePieceFinished(pieceIndex)) {
                throw new IllegalStateException(
                        "Finish state is out of sync");
            }
            String player = turns.getCurrentPlayerName();
            int finished = board.getActiveFinishedPieceCount();
            setStatus(player + ": piece reached the goal ("
                    + finished + "/4).");
            if (pieces.isCurrentPlayerFinished()) {
                passTurn();
                if (turns.isGameOver()) {
                    sidebar.setEndGameEnabled(true);
                    setStatus(
                        "Game complete! Click End Game to see the results.");
                }
            } else {
                endTurnOrExtraTurn();
            }
            return;
        }
        if (turns.enterHomePath(pieceIndex, routeSize)) {
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
        if (!turns.movePiece(pieceIndex, targetStep, routeSize)) {
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
     * SRS 3.2.5.3.3 - 3.2.5.3.4 - handles the click when an opponent's Double Block is in the way
     * of the move. The move is cut short (the piece stops on the square before the block), and it
     * is allowed only when the player has no other piece that can make the whole move.
     *
     * @return true when a block interferes and the click was handled here
     */
    private boolean handleBlockedMove(int pieceIndex, int routeSize) {
        if (!pieces.isPieceSelectionLegal(pieceIndex)) {
            return false;
        }
        int firstStep = pieces.getPieceStep(pieceIndex);
        int arrowStep = pieces.findArrowOnMove(
                pieceIndex, board.getOwnArrowStep(pieceIndex));
        int wantedStep = arrowStep >= 0
                ? arrowStep : firstStep + pieces.getMoveStepsForSelectedPiece();
        int player = turns.getCurrentPlayerIndex();
        int reach = turns.getStepsBeforeBlock(player, pieceIndex, firstStep, wantedStep);
        if (reach == wantedStep) {
            return false;
        }
        String owner = turns.getPlayerNames()[
                turns.getBlockingPlayer(player, pieceIndex, firstStep, wantedStep)];
        if (reach == firstStep) {
            setStatus("A Double Block of " + owner
                    + " is right in front of this piece. Choose another piece.");
            return true;
        }
        if (turns.hasOtherPieceWithFreeMove(
                pieceIndex, getCurrentRouteSizes(), getCurrentArrowSteps())) {
            setStatus("A Double Block of " + owner
                    + " is in the way. Choose a piece that can make the whole move.");
            return true;
        }
        captureNote += "Double Block of " + owner
                + " in the way - the piece stops before it. ";
        if (!turns.movePiece(pieceIndex, reach, routeSize)) {
            setStatus("The selected move is not legal.");
            return true;
        }
        board.animatePiece(pieceIndex, firstStep, reach,
                () -> finishPieceMovement(pieceIndex));
        return true;
    }

    /**
     * SRS 3.2.4.3.2 - moves the piece to its arrow, and from there to the next path
     * with the steps that are left.
     */
    private void moveThroughArrow(int pieceIndex, int arrowStep, int routeSize) {
        int startStep = pieces.getPieceStep(pieceIndex);
        int remaining = pieces.getMoveStepsForSelectedPiece()
                - (arrowStep - startStep);
        if (!turns.movePiece(pieceIndex, arrowStep, routeSize)) {
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
        Color pieceColor = BoardPanel.colorOf(turns.getCurrentPlayerColor());
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
            announceCapture(pieceIndex);
            setStatus("Matching arrow! Piece entered " + pathName + ".");
            endTurnOrExtraTurn();
            return;
        }
        int routeSize = board.getActiveRouteSize(pieceIndex);
        int nextArrow = pieces.findArrowOnRemainingSteps(
                pieceIndex, remaining, board.getOwnArrowStep(pieceIndex));
        int wanted = nextArrow >= 0 ? nextArrow : remaining;
        // SRS 3.2.5.3.4 - an opponent's Double Block on the new path stops the piece before it
        int target = stopBeforeBlock(pieceIndex, 0, wanted);
        boolean blocked = target != wanted;
        if (!pieces.continueMoveAfterArrow(pieceIndex, target, routeSize)) {
            throw new IllegalStateException("Arrow move is not legal");
        }
        setStatus("Matching arrow! Piece moved to " + pathName + ".");
        if (nextArrow >= 0 && !blocked) {
            board.animatePiece(pieceIndex, 0, nextArrow,
                    () -> crossArrow(pieceIndex, remaining - nextArrow));
        } else {
            board.animatePiece(pieceIndex, 0, target,
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
        announceCapture(pieceIndex);
        endTurnOrExtraTurn();
    }

    private void finishPieceMovement(int pieceIndex) {
        // SRS 3.2.3.3.1 - capture is checked before a question or surprise square is activated
        announceCapture(pieceIndex);
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
	
	    // קבלת רמת השאלה מהמשבצת שעליה הכלי נחת
	    String questionDifficulty =
	            board.getQuestionDifficultyAtStep(
	                    pieceIndex,
	                    pieces.getPieceStep(pieceIndex));
	
	    // מניעת הפעלת שאלה ללא רמת קושי מוגדרת
	    if (questionDifficulty == null) {
	        throw new IllegalStateException(
	                "Question square has no difficulty");
	    }
	
	    // בחירת שאלה אקראית מהמאגר ברמה המתאימה
	    turns.selectQuestion(questionDifficulty);
	
	    // פתיחת חלון השאלה הקיים
	    Window owner = SwingUtilities.getWindowAncestor(this);
	    QuestionDialog dialog = new QuestionDialog(owner, turns);
	    dialog.setVisible(true);
	    boolean correct = dialog.isCorrect();


        // מיקום הכלי לפני התנועה בעקבות השאלה
        int startStep = pieces.getPieceStep(pieceIndex);

        // מספר הצעדים נקבע לפי רמת השאלה שנבחרה
        int questionSteps = turns.getCurrentQuestionMoveSteps();

        // חישוב מיקום היעד לפי נכונות התשובה
        int targetStep = pieces.getAnswerMoveTarget(
                correct,
                questionSteps,
                board.getActiveRouteSize(pieceIndex));

        // SRS 3.2.5.3.4 - forward or backward, an opponent's Double Block stops the piece
        // on the square before it
        targetStep = stopBeforeBlock(pieceIndex, startStep, targetStep);

        // עדכון ההודעה בהתאם לתוצאת התנועה
        if (targetStep == startStep) {
            setStatus("Question answered. Piece stays in place at route boundary.");
        } else {
            setStatus(correct
                    ? "Correct! Move " + questionSteps + " square(s) forward."
                    : "Wrong answer. Move " + questionSteps + " square(s) back.");
        }

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
	    new SurpriseFlow(
	            turns,
	            turnController,
	            board,
	            pieceIndex,
	            SwingUtilities.getWindowAncestor(this),
	            this::endTurnOrExtraTurn,
	            this::passTurn,
	            this::setStatus,
	            message -> captureNote = message
	    ).run();
	}

    private void endTurnOrExtraTurn() {
        if (turnController.hasExtraTurn()) {
            turnController.grantExtraTurn();
            board.clearHighlight();
            sidebar.setRollButton(true, "Roll Again");

            setStatus(turnController.isUltraBoostRollPending()
                    ? "Surprise! Roll again with UltraBoost."
                    : turns.getCurrentPlayerName()
                            + " rolled a 6 - roll again!");
        } else {
            passTurn();
        }
    }

    private void passTurn() {
        turnController.passTurn();
        sidebar.setFormula(" ");
        sidebar.setRollButton(turnController.canRoll(), "Roll Dice");
        sidebar.setEndGameEnabled(turns.isGameOver());
        if (turns.isGameOver()) {
            handleGameOver();
        }
        board.clearHighlight();
        refreshPlayers();
        setStatus(turnController.canRoll()
                ? turns.getCurrentPlayerName()
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
        String error = turns.saveFinishedGame(history);
        if (error != null) {
            historyNote = error;
            JOptionPane.showMessageDialog(this, error, "Game history", JOptionPane.ERROR_MESSAGE);
        } else if (turns.isSavedToHistory()) {
            historyNote = "This game was saved to the game history.";
        }
    }

    private void showEndScreen() {
        gameTimer.stop();
        stopBoard3D();
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            // SRS 3.2.8.3.1 - the end screen shows the players by final place
            frame.setContentPane(new EndGameScreen(
                    turns.getRankingNames(), turns.getRankingColors(),
                    difficulty, turns.getElapsedTime(), historyNote));
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
	    stopBoard3D();
	    Window w =SwingUtilities.getWindowAncestor(this);
	    new MainMenuScreen().setVisible(true);
	    if (w != null) {
	        w.dispose();
	    }
	}

    // ------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------

    private void refreshPlayers() {
        sidebar.refreshPlayers(turns.getCurrentPlayerIndex());
    }

    private void setStatus(String text) {
        sidebar.setStatus(captureNote + text);
    }

    /** SRS 3.2.5.3.1 - applies a capture made by the piece that just stopped. */
    private void announceCapture(int pieceIndex) {
        String message = board.resolveCapture(turns.getCurrentPlayerIndex(), pieceIndex);
        if (message != null) {
            captureNote += message;
        }
    }

    /**
     * SRS 3.2.5.3.3 - 3.2.5.3.4 - the step a piece really reaches: an opponent's Double Block
     * can neither be passed nor stood on, so the piece stops on the square before it.
     */
    private int stopBeforeBlock(int pieceIndex, int startStep, int targetStep) {
        int player = turns.getCurrentPlayerIndex();
        int reach = turns.getStepsBeforeBlock(player, pieceIndex, startStep, targetStep);
        if (reach != targetStep) {
            int owner = turns.getBlockingPlayer(player, pieceIndex, startStep, targetStep);
            captureNote += "Double Block of " + turns.getPlayerNames()[owner]
                    + " in the way - the piece stops before it. ";
        }
        return reach;
    }

    /**
     * SRS 3.2.2.3.4 -
     * Creates a visual legend for the board square colors
     * and symbols displayed to the user.
     */
    private JPanel createLegend() {
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setOpaque(false);
        legend.setBorder(new EmptyBorder(4, 6, 4, 6));
        JLabel title = new JLabel("Board legend");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(INK);
        legend.add(leftAligned(title));
        legend.add(Box.createVerticalStrut(10));
        legend.add(legendSection("PROTECTED - a piece here cannot be captured"));
        legend.add(legendRow(SquareType.STAR, null,
                "Protection square", "Star - pieces of different players can share it"));
        legend.add(legendRow(SquareType.START, BoardPanel.BLUE,
                "Start square", "Marked #, in the colour of its player"));
        if (!"Easy".equals(difficulty)) {
            legend.add(legendRow(SquareType.SURPRISE, BoardPanel.SURPRISE,
                    "Surprise square", "Marked !, triggers a random event"));
        }
        legend.add(Box.createVerticalStrut(8));
        legend.add(legendSection("PATHS"));
        legend.add(legendRow(SquareType.ARROW, BoardPanel.GREEN,
                "Arrow", "A piece of the same colour moves to the next path"));
        legend.add(legendRow(null, BoardPanel.GREEN,
                "Home path", "Private path to the goal - cannot be captured"));
        if (!"Easy".equals(difficulty)) {
            legend.add(Box.createVerticalStrut(8));
            legend.add(legendSection("QUESTION SQUARES"));
            legend.add(legendRow(SquareType.QUESTION, BoardPanel.Q_EASY,
                    "Easy question", "Right: 1 forward - wrong: 1 back"));
            legend.add(legendRow(SquareType.QUESTION, BoardPanel.Q_MEDIUM,
                    "Medium question", "Right: 2 forward - wrong: 2 back"));
            legend.add(legendRow(SquareType.QUESTION, BoardPanel.Q_HARD,
                    "Hard question", "Right: 3 forward - wrong: 3 back"));
        }
        return legend;
    }

    private JComponent legendSection(String text) {
        JLabel label = sectionLabel(text);
        label.setBorder(new EmptyBorder(0, 0, 4, 0));
        return leftAligned(label);
    }

    private JComponent legendRow(SquareType type, Color color, String name, String description) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(3, 0, 3, 0));
        row.add(new JLabel(new ImageIcon(legendSwatch(type, color))), BorderLayout.WEST);
        JLabel label = new JLabel("<html><b>" + name + "</b><br>"
                + "<span style='color:#6e7a96'>" + description + "</span></html>");
        label.setForeground(INK);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(label, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return leftAligned(row);
    }

    /** Draws a square like the board does (null type = a home-path cell). */
    private static Image legendSwatch(SquareType type, Color color) {
        int cell = 36;
        int pad = 3;
        int tile = cell - pad * 2;
        int arc = 10;
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(cell, cell, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        quality(g);
        Color base = Color.WHITE;
        if (type == SquareType.STAR) {
            base = new Color(255, 241, 196);
        } else if (type == SquareType.SURPRISE) {
            base = new Color(234, 223, 255);
        } else if (color != null) {
            base = lighten(color);
            if (type == SquareType.START || type == SquareType.ARROW
                    || type == SquareType.QUESTION) {
                base = lighten(base);
            }
        }
        g.setColor(base);
        g.fillRect(0, 0, cell, cell);
        g.setColor(new Color(201, 209, 228));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRect(0, 0, cell - 1, cell - 1);
        if (type == SquareType.STAR) {
            g.setColor(new Color(255, 255, 255, 170));
            g.fillOval(pad, pad, tile, tile);
            java.awt.geom.Path2D star = new java.awt.geom.Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double radius = (i % 2 == 0 ? 1.0 : 0.46) * cell * 0.39;
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                double px = cell / 2.0 + Math.cos(angle) * radius;
                double py = cell / 2.0 + Math.sin(angle) * radius;
                if (i == 0) {
                    star.moveTo(px, py);
                } else {
                    star.lineTo(px, py);
                }
            }
            star.closePath();
            g.setColor(new Color(251, 181, 37));
            g.fill(star);
            g.setColor(new Color(158, 107, 19));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(star);
        } else if (type == SquareType.ARROW) {
            g.setColor(new Color(255, 255, 255, 150));
            g.fillRoundRect(pad, pad, tile, tile, arc, arc);
            double length = cell * 0.37;
            java.awt.geom.Path2D arrow = new java.awt.geom.Path2D.Double();
            arrow.moveTo(-length, -length * 0.27);
            arrow.lineTo(0, -length * 0.27);
            arrow.lineTo(0, -length * 0.69);
            arrow.lineTo(length, 0);
            arrow.lineTo(0, length * 0.69);
            arrow.lineTo(0, length * 0.27);
            arrow.lineTo(-length, length * 0.27);
            arrow.closePath();
            g.translate(cell / 2.0, cell / 2.0);
            g.setColor(color.darker());
            g.fill(arrow);
        } else if (type != null) {
            Color fill = color;
            String mark = "#";
            if (type == SquareType.QUESTION) {
                mark = "?";
                fill = BoardPanel.Q_EASY.equals(color) ? new Color(48, 167, 111)
                        : BoardPanel.Q_MEDIUM.equals(color) ? new Color(220, 157, 32)
                        : new Color(221, 69, 90);
            } else if (type == SquareType.SURPRISE) {
                mark = "!";
                fill = new Color(134, 76, 211);
            }
            g.setColor(fill);
            g.fillRoundRect(pad, pad, tile, tile, arc, arc);
            g.setFont(new Font("Segoe UI", Font.BOLD, 22));
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString(mark, (cell - metrics.stringWidth(mark)) / 2,
                    (cell + metrics.getAscent() - metrics.getDescent()) / 2);
        }
        g.dispose();
        return image;
    }

    private static Color lighten(Color color) {
        return new Color((color.getRed() + 255) / 2,
                (color.getGreen() + 255) / 2, (color.getBlue() + 255) / 2);
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
