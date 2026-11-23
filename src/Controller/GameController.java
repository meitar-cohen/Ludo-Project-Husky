package Controller;

import java.awt.Color;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import Enums.PieceState;
import Enums.Stage;
import Enums.SquareType;
import Model.Game;
import Model.Piece;
import Model.Player;
import Model.QuestionEvent;
import Model.QuestionEventCreator;
import Model.Square;
import java.io.IOException;
import Model.QuestionBank;
import Model.QuestionRepository;

/** Coordinates game setup, board state, questions, and game results. */
public class GameController {
    public static final int PIECES_PER_PLAYER = Game.PIECES_PER_PLAYER;
    // board corner of each colour; the same order is used by the board view
    private static final String[] CORNER_COLOR_NAMES = {"Green", "Yellow", "Blue", "Red"};
    // on the outer and middle rings the colour's arrow is 7 squares before its corner
    private static final int ARROW_DISTANCE_FROM_CORNER = 7;

    private final Game game;
    private final MovementController pieces;
    private final SurpriseController surpriseController;
    private final TurnController turnController;
    // Command pattern: every move of a piece goes through the invoker, which logs it
    private final CommandInvoker commands = new CommandInvoker();
    // Observer pattern: the views that want to hear about a capture (subject side)
    private final List<CaptureListener> captureListeners = new CopyOnWriteArrayList<>();
    // Observer pattern: forwards model changes to View observers without exposing Model to View
    private final PropertyChangeSupport stateObservers = new PropertyChangeSupport(this);
    // מאגר השאלות שנטען מקובץ ה-CSV
    private final QuestionBank questionBank;

    // אירוע השאלה הנוכחי שנוצר באמצעות Factory Method
    private QuestionEvent currentQuestionEvent;
    private long startTimeMillis = -1;
    private long endTimeMillis = -1;


	public GameController(Game game, MovementController pieces) {
	    this.game = game;
	    this.pieces = pieces;
	    this.turnController = new TurnController(game, pieces);
	    this.surpriseController =
	            new SurpriseController(game, pieces, this.turnController);
	
	    // טעינת השאלות מתוך קובץ ה-CSV הקיים
	    try {
	        this.questionBank = new QuestionBank(
	                QuestionRepository.loadProjectQuestions()
	        );
	    } catch (IOException exception) {
	        throw new IllegalStateException(
	                "Unable to load questions from CSV",
	                exception
	        );
	    }
	
	    // אתחול אירוע שאלה קלה באמצעות Factory Method
	    selectQuestion("easy");
	
	    initializeBoard();
	    this.game.addObserver(this::forwardGameObserverEvent);
	}


    public static GameController createNewGame(
            String[] names, String[] colors, String difficulty) {
        String validationError = validatePlayers(names, colors);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        Game game = new Game(createPlayers(names, colors), difficulty);
        return new GameController(game, new MovementController(game));
    }

	

    // בחירת שאלה באמצעות Factory Method לפי רמת הקושי
    public void selectQuestion(String difficulty) {

        if (difficulty == null || difficulty.isBlank()) {
            throw new IllegalArgumentException(
                    "Question difficulty is required");
        }

        // בחירת היוצר המתאים לרמת הקושי
        QuestionEventCreator creator = switch (
                difficulty.trim().toLowerCase(java.util.Locale.ROOT)) {

            case "easy" ->
                    new QuestionEventCreator.Easy(questionBank);

            case "medium" ->
                    new QuestionEventCreator.Medium(questionBank);

            case "hard" ->
                    new QuestionEventCreator.Hard(questionBank);

            default -> throw new IllegalArgumentException(
                    "Unsupported question difficulty: " + difficulty);
        };

        // יצירת האירוע באמצעות Factory Method
        currentQuestionEvent = creator.create();
    }

    // בדיקת נכונות התשובה באמצעות האירוע הנוכחי
    public boolean isCorrectQuestionAnswer(int selectedIndex) {
        return currentQuestionEvent.isCorrectAnswer(selectedIndex);
    }

    // החזרת נוסח השאלה מתוך האירוע הנוכחי
    public String getQuestionText() {
        return currentQuestionEvent.getQuestion().getText();
    }

    // החזרת אפשרויות התשובה
    public String[] getQuestionOptions() {
        return currentQuestionEvent.getQuestion().getOptions();
    }

    // החזרת רמת הקושי של השאלה
    public String getQuestionDifficulty() {
        return currentQuestionEvent.getQuestion().getDifficulty();
    }

    // מספר הצעדים נקבע לפי סוג האירוע שנוצר
    public int getCurrentQuestionMoveSteps() {
        return currentQuestionEvent.getMoveSteps();
    }

    // החזרת האינדקס של התשובה הנכונה
    public int getCorrectQuestionAnswerIndex() {
        return currentQuestionEvent.getQuestion().getCorrectIndex();
    }



    public SurpriseController getSurpriseController() {
        return surpriseController;
    }

    public MovementController getMovementController() {
        return pieces;
    }

    public TurnController getTurnController() {
        return turnController;
    }

    // ------------------------------------------------------------------ Command pattern (Client)

    /** The invoker that runs and logs the game actions. */
    public CommandInvoker getCommandInvoker() {
        return commands;
    }

    /** Text lines of all the actions done in this game, oldest first. */
    public List<String> getActionLog() {
        return commands.getLog();
    }

    /** Takes the given piece of the current player out of the castle (as a command). */
    public boolean takeOutPiece(int slot) {
        return commands.execute(new TakeOutPieceCommand(game, pieces, slot));
    }

    /** Moves a piece of the current player along its path (as a command). */
    public boolean movePiece(int pieceIndex, int targetStep, int routeSize) {
        return commands.execute(new MovePieceCommand(game, pieces, pieceIndex, targetStep, routeSize));
    }

    /** Finishes a piece that reaches the goal (as a command). */
    public boolean finishPiece(int pieceIndex, int routeSize) {
        return commands.execute(new FinishPieceCommand(game, pieces, pieceIndex, routeSize));
    }

    /** Moves a piece into the private home path (as a command). */
    public boolean enterHomePath(int pieceIndex, int sharedRouteSize) {
        return commands.execute(new EnterHomePathCommand(game, pieces, pieceIndex, sharedRouteSize));
    }

    public int getBoardRings() {
        return game.getBoard().getRings();
    }

    public int getBoardSize() {
        return game.getBoard().getSize();
    }

    public Point getInnerBoardPoint(int column, int row) {
        return new Point(column + getBoardRings(), row + getBoardRings());
    }

    public List<Point> getInnerBoardPath() {
        return game.getBoard().getSharedRoutes().get(0);
    }

    public List<Point> getBoardRingPath(int ring) {
        if (ring < 0 || ring > getBoardRings()) {
            throw new IllegalArgumentException("Invalid board ring index");
        }
        int n = getBoardSize() - 2 * ring;
        List<Point> path = new ArrayList<>();
        for (int c = 0; c < n; c++) path.add(new Point(ring + c, ring));
        for (int r = 1; r < n; r++) path.add(new Point(ring + n - 1, ring + r));
        for (int c = n - 2; c >= 0; c--) path.add(new Point(ring + c, ring + n - 1));
        for (int r = n - 2; r >= 1; r--) path.add(new Point(ring, ring + r));
        return path;
    }

    public List<List<Point>> getBoardSharedRoutes() {
        return game.getBoard().getSharedRoutes();
    }

    private static int cornerOf(String colorName) {
        for (int i = 0; i < CORNER_COLOR_NAMES.length; i++) {
            if (CORNER_COLOR_NAMES[i].equalsIgnoreCase(colorName)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * The board square a piece stands on in the shared paths, or null when the piece is
     * in the castle, on its private home path, or finished.
     */
    public Point getPieceBoardPosition(int playerIndex, int pieceIndex) {
        Piece piece = game.getPlayer(playerIndex).getPiece(pieceIndex);
        if (!piece.isOnBoard() || piece.isFinished() || piece.isInHomePath()) {
            return null;
        }
        List<Point> route = routeOf(playerIndex, piece);
        int step = Math.max(0, Math.min(piece.getSteps(), route.size() - 1));
        return route.get(Math.floorMod(firstIndexOf(playerIndex, piece) + step, route.size()));
    }

    /** The shared route the piece is walking on (outer, middle or inner path). */
    private List<Point> routeOf(int playerIndex, Piece piece) {
        if (piece.isInInnerPath()) {
            return getInnerBoardPath();
        }
        if (piece.isInMiddlePath()) {
            return getBoardRingPath(1);
        }
        return getBoardRings() == 0 ? getInnerBoardPath() : getBoardRingPath(0);
    }

    /** Index in {@link #routeOf} of the piece's step 0. */
    private int firstIndexOf(int playerIndex, Piece piece) {
        int corner = cornerOf(game.getPlayer(playerIndex).getColor());
        if (piece.isInInnerPath()) {
            return corner * 13 - 1;
        }
        List<Point> route = routeOf(playerIndex, piece);
        if (piece.isInMiddlePath()) {
            return corner * (route.size() / 4) + route.size() - ARROW_DISTANCE_FROM_CORNER + 1;
        }
        return getBoardRings() == 0 ? corner * 13 : corner * (route.size() / 4);
    }

    /**
     * The shared square the piece would stand on after {@code step} steps of its current
     * path, or null when that step is outside the shared route (home path, finish).
     */
    private Point squareAtStep(int playerIndex, Piece piece, int step) {
        List<Point> route = routeOf(playerIndex, piece);
        if (step < 0 || step >= route.size()) {
            return null;
        }
        return route.get(Math.floorMod(firstIndexOf(playerIndex, piece) + step, route.size()));
    }

    // ------------------------------------------------------------------ Double Block

    /**
     * SRS 3.2.5.3.3 - a Double Block is two (or more) pieces of one player on the same square
     * of the shared route. The block is not stored anywhere: it exists exactly as long as the
     * pieces stand there, so it is cancelled when one of them moves (SRS 3.2.5.3.4).
     *
     * @return the index of the player who owns the block on this square, or -1 when there is none
     */
    public int getDoubleBlockOwnerAt(Point position) {
        return blockOwner(allBoardPositions(), position);
    }

    /** The shared square of every piece ({@code [player][piece]}, null when not on a shared path). */
    private Point[][] allBoardPositions() {
        Point[][] positions = new Point[game.getPlayerCount()][PIECES_PER_PLAYER];
        for (int player = 0; player < positions.length; player++) {
            for (int piece = 0; piece < PIECES_PER_PLAYER; piece++) {
                positions[player][piece] = getPieceBoardPosition(player, piece);
            }
        }
        return positions;
    }

    private static int blockOwner(Point[][] positions, Point square) {
        if (square == null) {
            return -1;
        }
        for (int player = 0; player < positions.length; player++) {
            int count = 0;
            for (Point position : positions[player]) {
                if (square.equals(position)) {
                    count++;
                }
            }
            if (count >= 2) {
                return player;
            }
        }
        return -1;
    }

    public boolean isDoubleBlock(Point position) {
        return getDoubleBlockOwnerAt(position) >= 0;
    }

    /** All the squares that hold a Double Block now (used by the view to mark them). */
    public List<Point> getDoubleBlockPositions() {
        Point[][] positions = allBoardPositions();
        List<Point> blocks = new ArrayList<>();
        for (Point[] player : positions) {
            for (Point position : player) {
                if (position != null && !blocks.contains(position)
                        && blockOwner(positions, position) >= 0) {
                    blocks.add(position);
                }
            }
        }
        return blocks;
    }

    /**
     * SRS 3.2.5.3.3 - 3.2.5.3.4 - how far a piece really gets. An opponent's Double Block can
     * neither be passed nor be stood on, in both directions, so the piece stops on the square
     * before it. The pieces of the block's owner are not stopped by their own block.
     *
     * @param fromStep the current step of the piece on its path
     * @param toStep   the step it wants to reach (bigger = forward, smaller = backward)
     * @return {@code toStep} when the way is free, otherwise the last free step
     *         (equal to {@code fromStep} when the very next square is blocked)
     */
    public int getStepsBeforeBlock(int playerIndex, int pieceIndex, int fromStep, int toStep) {
        Piece piece = game.getPlayer(playerIndex).getPiece(pieceIndex);
        if (!piece.isOnBoard() || piece.isFinished() || piece.isInHomePath()
                || fromStep == toStep) {
            return toStep;
        }
        int direction = toStep > fromStep ? 1 : -1;
        Point[][] positions = allBoardPositions();
        for (int step = fromStep + direction; step != toStep + direction; step += direction) {
            Point square = squareAtStep(playerIndex, piece, step);
            if (square == null) {
                break; // the private home path cannot hold a block
            }
            int owner = blockOwner(positions, square);
            if (owner >= 0 && owner != playerIndex) {
                return step - direction;
            }
        }
        return toStep;
    }

    /** The owner of the block that stops the move, or -1 (used for the message to the player). */
    public int getBlockingPlayer(int playerIndex, int pieceIndex, int fromStep, int toStep) {
        int reach = getStepsBeforeBlock(playerIndex, pieceIndex, fromStep, toStep);
        if (reach == toStep) {
            return -1;
        }
        Piece piece = game.getPlayer(playerIndex).getPiece(pieceIndex);
        Point square = squareAtStep(playerIndex, piece, reach + (toStep > fromStep ? 1 : -1));
        return getDoubleBlockOwnerAt(square);
    }

    /**
     * SRS 3.2.5.3.4 - for every piece of the current player: true when a Double Block stands
     * on the very next square of the rolled move, so the piece cannot move at all.
     */
    public boolean[] getPiecesStuckBehindBlock() {
        int player = game.getCurrentPlayer();
        boolean[] stuck = new boolean[PIECES_PER_PLAYER];
        int steps = game.getPendingSteps();
        for (int piece = 0; piece < PIECES_PER_PLAYER; piece++) {
            Piece model = game.getPlayer(player).getPiece(piece);
            if (steps > 0 && model.isOnBoard() && !model.isFinished()
                    && !model.isInHomePath()) {
                int from = model.getSteps();
                stuck[piece] = getStepsBeforeBlock(player, piece, from, from + steps) == from;
            }
        }
        return stuck;
    }

    /**
     * SRS 3.2.5.3.4 - moving forward up to a block is allowed only when the player has no other
     * piece that can make the whole move. True when another piece has a free, complete move.
     *
     * @param routeSizes    size of the path of every piece of the current player
     * @param ownArrowSteps step of the own arrow of every piece (-1 when none), may be null
     */
    public boolean hasOtherPieceWithFreeMove(int exceptPiece, int[] routeSizes, int[] ownArrowSteps) {
        int player = game.getCurrentPlayer();
        int steps = game.getPendingSteps();
        for (int piece = 0; piece < PIECES_PER_PLAYER; piece++) {
            if (piece == exceptPiece) {
                continue;
            }
            int arrow = ownArrowSteps != null && ownArrowSteps.length == PIECES_PER_PLAYER
                    ? ownArrowSteps[piece] : -1;
            if (!pieces.isPieceMoveLegal(piece, routeSizes[piece], arrow)) {
                continue;
            }
            Piece model = game.getPlayer(player).getPiece(piece);
            int from = model.getSteps();
            if (model.isInHomePath()
                    || getStepsBeforeBlock(player, piece, from, from + steps) == from + steps) {
                return true;
            }
        }
        return false;
    }

    /**
     * SRS 3.2.5.3.2 - a start square is protected, but a Double Block still keeps the other
     * players off it: a piece cannot be taken out onto a start square that holds a block of
     * another player.
     */
    public boolean isTakeOutBlocked(int slot) {
        int player = game.getCurrentPlayer();
        Piece piece = game.getPlayer(player).getPiece(slot);
        int owner = getDoubleBlockOwnerAt(startSquareOf(player));
        return piece != null && !piece.isOnBoard() && owner >= 0 && owner != player;
    }

    private Point startSquareOf(int playerIndex) {
        Piece fresh = new Piece();
        fresh.setOnBoard(true);
        return squareAtStep(playerIndex, fresh, 0);
    }

    /**
     * SRS 3.2.5.3.2 - star, start and surprise squares are protected. The home path is
     * private to its owner, so an opponent can never stop on it.
     */
    public boolean isProtectedSquare(Point position) {
        SquareType type = getSpecialSquareType(position);
        return type == SquareType.STAR
                || type == SquareType.START
                || type == SquareType.SURPRISE;
    }

    /**
     * SRS 3.2.5.3.1 - the given piece has stopped on its square. A single piece of another
     * player on that square returns to its castle, unless the square is protected.
     *
     * @return {playerIndex, pieceIndex} of the captured piece, or null when nothing was captured
     */
    public int[] captureOpponentAt(int movingPlayer, int movingPiece) {
        Point position = getPieceBoardPosition(movingPlayer, movingPiece);
        if (position == null || isProtectedSquare(position)) {
            return null;
        }

        for (int player = 0; player < game.getPlayerCount(); player++) {
            if (player == movingPlayer) {
                continue;
            }
            int found = -1;
            int count = 0;
            for (int piece = 0; piece < PIECES_PER_PLAYER; piece++) {
                if (position.equals(getPieceBoardPosition(player, piece))) {
                    found = piece;
                    count++;
                }
            }
            // two pieces of one player on a square are a block, not a single piece
            if (count == 1 && commands.execute(new CaptureCommand(game, pieces, player, found))) {
                notifyCapture(new CaptureEvent(movingPlayer, movingPiece, player, found, position));
                return new int[]{player, found};
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Observer (subject)

    /** Registers a view that wants to be told about every capture. */
    public void addCaptureListener(CaptureListener listener) {
        if (listener != null && !captureListeners.contains(listener)) {
            captureListeners.add(listener);
        }
    }

    public void removeCaptureListener(CaptureListener listener) {
        captureListeners.remove(listener);
    }

    private void notifyCapture(CaptureEvent event) {
        for (CaptureListener listener : captureListeners) {
            listener.onCapture(event);
        }
    }

    // ------------------------------------------------------------------ Observer (Model <-> View bridge)

    /** Registers a View observer to be notified whenever the game state changes. */
    public void addObserver(PropertyChangeListener observer) {
        if (observer == null) {
            return;
        }
        for (PropertyChangeListener existing : stateObservers.getPropertyChangeListeners()) {
            if (existing == observer) {
                return;
            }
        }
        stateObservers.addPropertyChangeListener(observer);
    }

    /** Unregisters a View observer so closed screens do not leak memory. */
    public void removeObserver(PropertyChangeListener observer) {
        if (observer != null) {
            stateObservers.removePropertyChangeListener(observer);
        }
    }

    /** Notifies all registered View observers that the game state changed. */
    public void notifyObservers(String eventName) {
        String property = (eventName == null || eventName.isBlank()) ? "gameState" : eventName;
        stateObservers.firePropertyChange(property, null, null);
    }

    private void forwardGameObserverEvent(PropertyChangeEvent event) {
        notifyObservers(event.getPropertyName());
    }

    public Stage getStage() {
        return game.getStage();
    }

    public PieceState getPieceState(int playerIndex, int pieceIndex) {
        return game.getPlayer(playerIndex).getPiece(pieceIndex).getState();
    }

    public int getPieceSteps(int playerIndex, int pieceIndex) {
        return game.getPlayer(playerIndex).getPiece(pieceIndex).getSteps();
    }

    public boolean isPieceInInnerPath(int playerIndex, int pieceIndex) {
        return game.getPlayer(playerIndex).getPiece(pieceIndex).isInInnerPath();
    }

    public boolean isPieceInMiddlePath(int playerIndex, int pieceIndex) {
        return game.getPlayer(playerIndex).getPiece(pieceIndex).isInMiddlePath();
    }

    private void initializeBoard() {
        String difficulty = game.getDifficulty();
        int rings = "Hard".equals(difficulty) ? 2 : "Medium".equals(difficulty) ? 1 : 0;
        game.getBoard().setRings(rings);
        game.getBoard().setSize(15 + 2 * rings);
        List<List<Point>> routes = new ArrayList<>();
        routes.add(createInnerBoardPath());
        if (rings >= 1) {
            routes.add(getBoardRingPath(rings == 1 ? 0 : 1));
        }
        if (rings == 2) {
            routes.add(getBoardRingPath(0));
        }
        game.getBoard().setSharedRoutes(routes);
    }

    private List<Point> createInnerBoardPath() {
        List<Point> path = new ArrayList<>();
        for (int c = 1; c <= 5; c++) path.add(getInnerBoardPoint(c, 6));
        for (int r = 5; r >= 0; r--) path.add(getInnerBoardPoint(6, r));
        path.add(getInnerBoardPoint(7, 0));
        path.add(getInnerBoardPoint(8, 0));
        for (int r = 1; r <= 5; r++) path.add(getInnerBoardPoint(8, r));
        for (int c = 9; c <= 14; c++) path.add(getInnerBoardPoint(c, 6));
        path.add(getInnerBoardPoint(14, 7));
        path.add(getInnerBoardPoint(14, 8));
        for (int c = 13; c >= 9; c--) path.add(getInnerBoardPoint(c, 8));
        for (int r = 9; r <= 14; r++) path.add(getInnerBoardPoint(8, r));
        path.add(getInnerBoardPoint(7, 14));
        path.add(getInnerBoardPoint(6, 14));
        for (int r = 13; r >= 9; r--) path.add(getInnerBoardPoint(6, r));
        for (int c = 5; c >= 0; c--) path.add(getInnerBoardPoint(c, 8));
        path.add(getInnerBoardPoint(0, 7));
        path.add(getInnerBoardPoint(0, 6));
        return path;
    }

    public void addSpecialSquare(Point position, SquareType type, Color color, int direction) {
        game.getBoard().putSpecialSquare(
                new Point(position), new Square(type, color, direction));
    }
    

	/**
	 * הוספת משבצת מיוחדת עם רמת קושי לשאלה.
	 * רמת הקושי נשמרת במודל ואינה תלויה בצבע המשבצת.
	 */
	public void addSpecialSquare(Point position, SquareType type,
	        Color color, int direction, String questionDifficulty) {
	
	    game.getBoard().putSpecialSquare(
	            new Point(position),
	            new Square(type, color, direction, questionDifficulty)
	    );
	}


    public void randomizeSpecialSquares(List<List<Point>> routes, int rings, Set<Point> excludedCells, Color[] questionColors, Color surprise) {
        removeSpecialSquares(
                SquareType.STAR, SquareType.QUESTION, SquareType.SURPRISE);

        placeRandomSpecialsOnRoute(
                routes.get(0), excludedCells, 4, 0, 0, 0, 0,
                questionColors, surprise);
        if (rings == 1) {
            placeRandomSpecialsOnRoute(
                    routes.get(1), excludedCells, 4, 2, 2, 2, 2,
                    questionColors, surprise);
        } else if (rings == 2) {
            placeRandomSpecialsOnRoute(
                    routes.get(1), excludedCells, 4, 1, 1, 1, 1,
                    questionColors, surprise);
            placeRandomSpecialsOnRoute(
                    routes.get(2), excludedCells, 4, 2, 2, 2, 2,
                    questionColors, surprise);
        } else if (rings != 0) {
            throw new IllegalArgumentException("Unsupported board ring count.");
        }
    }

    private void placeRandomSpecialsOnRoute(List<Point> route, Set<Point> excludedCells, int stars, int easyQuestions, int mediumQuestions, int hardQuestions, int surprises, Color[] questionColors, Color surprise) {
        List<Point> available = new ArrayList<>();
        for (Point cell : route) {
            if (!hasSpecialSquare(cell)
                    && !excludedCells.contains(cell)) {
                available.add(cell);
            }
        }

        int needed = stars + easyQuestions + mediumQuestions
                + hardQuestions + surprises;
        if (available.size() < needed) {
            throw new IllegalStateException(
                    "Not enough free squares for special cells");
        }
        Collections.shuffle(available);
        int index = 0;
        for (int i = 0; i < stars; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.STAR, null, 0);
        }

		for (int i = 0; i < easyQuestions; i++) {
		    addSpecialSquare(
		            available.get(index++),
		            SquareType.QUESTION,
		            questionColors[0],
		            0,
		            "easy");
		}
		
		for (int i = 0; i < mediumQuestions; i++) {
		    addSpecialSquare(
		            available.get(index++),
		            SquareType.QUESTION,
		            questionColors[1],
		            0,
		            "medium");
		}
		
		for (int i = 0; i < hardQuestions; i++) {
		    addSpecialSquare(
		            available.get(index++),
		            SquareType.QUESTION,
		            questionColors[2],
		            0,
		            "hard");
		}

        for (int i = 0; i < surprises; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.SURPRISE,
                    surprise, 0);
        }
    }

    public boolean hasSpecialSquare(Point position) {
        return game.getBoard().getSpecialSquare(position) != null;
    }

    public void removeSpecialSquares(SquareType... types) {
        for (Point position : game.getBoard().getSpecialSquarePositions()) {
            for (SquareType type : types) {
                if (game.getBoard().getSpecialSquare(position).getType() == type) {
                    game.getBoard().removeSpecialSquare(position);
                    break;
                }
            }
        }
    }

    public SquareType getSpecialSquareType(Point position) {
        Square square = game.getBoard().getSpecialSquare(position);
        return square == null ? null : square.getType();
    }
    
    // שליפת רמת הקושי של משבצת שאלה מתוך המודל
    public String getSpecialSquareQuestionDifficulty(Point position) {
        Square square = game.getBoard().getSpecialSquare(position);
        return square == null ? null : square.getQuestionDifficulty();
    }

    public Color getSpecialSquareColor(Point position) {
        Square square = game.getBoard().getSpecialSquare(position);
        return square == null ? null : square.getColor();
    }

    public int getSpecialSquareDirection(Point position) {
        Square square = game.getBoard().getSpecialSquare(position);
        return square == null ? -1 : square.getDirection();
    }

    public List<Point> getSpecialSquarePositions() {
        return game.getBoard().getSpecialSquarePositions();
    }

    public String[] getPlayerNames() {
        String[] names = new String[game.getPlayerCount()];
        for (int i = 0; i < names.length; i++) {
            names[i] = game.getPlayer(i).getName();
        }
        return names;
    }

    public String[] getPlayerColors() {
        String[] colors = new String[game.getPlayerCount()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = game.getPlayer(i).getColor();
        }
        return colors;
    }

    public String[] getRankingNames() {
        Player[] ranking = getRanking();
        String[] names = new String[ranking.length];
        for (int i = 0; i < ranking.length; i++) {
            names[i] = ranking[i].getName();
        }
        return names;
    }

    public String[] getRankingColors() {
        Player[] ranking = getRanking();
        String[] colors = new String[ranking.length];
        for (int i = 0; i < ranking.length; i++) {
            colors[i] = ranking[i].getColor();
        }
        return colors;
    }

    public String getCurrentPlayerName() {
        return game.getCurrentPlayerName();
    }

    public String getCurrentPlayerColor() {
        return game.getCurrentPlayerColor();
    }

    public int getCurrentPlayerIndex() {
        return game.getCurrentPlayer();
    }

    public int getPlayerCount() {
        return game.getPlayerCount();
    }

    public int getPendingSteps() {
        return game.getPendingSteps();
    }

    public String getDifficulty() {
        return game.getDifficulty();
    }

    public boolean isGameOver() {
        return game.getStage() == Stage.DONE;
    }

    public boolean isSavedToHistory() {
        return game.isSavedToHistory();
    }

    public String saveFinishedGame(HistoryController history) {
        return history.saveFinishedGame(game, this);
    }

    /** Returns the first validation error, or null when the setup is valid. */
    public static String validatePlayers(String[] names, String[] colors) {
        if (names == null || colors == null || names.length != colors.length) {
            return "Players must be provided.";
        }
        Set<String> usedNames = new HashSet<>();
        Set<String> usedColors = new HashSet<>();

        for (int i = 0; i < names.length; i++) {
            String name = names[i] == null ? "" : names[i].trim();
            if (name.isEmpty()) {
                return "Enter a name for player " + (i + 1) + ".";
            }
            if (!containsLetter(name)) {
                return "Player " + (i + 1)
                        + "'s name must contain at least one letter.";
            }
            if (!usedNames.add(name.toLowerCase())) {
                return "Each player must have a different name.";
            }
            if (colors[i] == null || !usedColors.add(colors[i])) {
                return "Each player must have a different color.";
            }
        }
        return null;
    }

    /** Returns the first invalid player-name index, or -1 when all are valid. */
    public static int findInvalidNameIndex(String[] names) {
        if (names == null) {
            return 0;
        }
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            String name = names[i] == null ? "" : names[i].trim();
            if (name.isEmpty() || !containsLetter(name)
                    || !usedNames.add(name.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean containsLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Creates model players from the setup form values. */
    private static Player[] createPlayers(String[] names, String[] colors) {
        Player[] players = new Player[names.length];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(names[i], colors[i]);
        }
        return players;
    }

    public void startGameClock() {
        if (startTimeMillis < 0) {
            startTimeMillis = System.currentTimeMillis();
        }
    }

    public String getElapsedTime() {
        long seconds = getElapsedSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600,
                (seconds % 3600) / 60, seconds % 60);
    }

    public long getElapsedSeconds() {
        if (startTimeMillis < 0) {
            return 0;
        }
        long end = endTimeMillis >= 0 ? endTimeMillis : System.currentTimeMillis();
        return (end - startTimeMillis) / 1000;
    }

    public void stopGameClock() {
        if (startTimeMillis >= 0 && endTimeMillis < 0) {
            endTimeMillis = System.currentTimeMillis();
        }
    }

    public boolean isGameClockStarted() {
        return startTimeMillis >= 0;
    }

    public Player[] getRanking() {
        List<Player> ranking = new ArrayList<>();
        for (int index : game.getFinishOrder()) {
            ranking.add(game.getPlayer(index));
        }
        for (int i = 0; i < game.getPlayerCount(); i++) {
            if (!game.getFinishOrder().contains(i)) {
                ranking.add(game.getPlayer(i));
            }
        }
        return ranking.toArray(new Player[0]);
    }

}
