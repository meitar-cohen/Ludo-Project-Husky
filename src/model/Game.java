package model;

import enums.Stage;

public class Game {
    private Stage stage = Stage.ROLL;
    private int currentPlayer;
    private int pendingSteps;
    private boolean firstTurnPulsePending = true;
    

    // SRS 3.2.4.3.1 - Stores the state and position of each player's pieces.
    private boolean[][] piecesOnBoard;
    private int[][] pieceSteps;
    private boolean[][] piecesInHomePath;
    // SRS 3.2.4.3.4 - שמירת הכלים שהגיעו לבית הסיום
    private boolean[][] finishedPieces;
    private int selectedPieceIndex;

    // SRS 3.2.4.3.2 -Stores whether each piece is on an inner path.
    private boolean[][] piecesInInnerPath;
    private final Player[] players;
    private final String difficulty;

    // SRS 3.2.1.3.3 - opening state and game clock
    public static final int PIECES_PER_PLAYER = 4;
    private int[] piecesInCastleByPlayer;
    
 // SRS 3.2.3.3.6 - שמירת המשתתפים שכבר סיימו את המשחק
    private boolean[] finishedPlayers;
    private long startTimeMillis = -1;   // -1 = the clock has not started yet

    // SRS 3.2.3.3.5 - extra turn and three sixes in a row
    private int lastStepDie;
    private int consecutiveSixes;

    public Game(Player[] players, String difficulty) {
        if (players == null || players.length == 0) {
            throw new IllegalArgumentException("At least one player is required");
        }
        this.players = players.clone();
        for (Player player : this.players) {
            if (player == null) {
                throw new IllegalArgumentException("Players cannot contain null values");
            }
        }
        this.difficulty = difficulty;
        initializePlayerState();
    }

    private void initializePlayerState() {
        // SRS 3.2.1.3.3 - all four pieces of every player start in the castle.
        // The game clock starts with the first roll (see startClock).
        this.piecesInCastleByPlayer = new int[players.length];
        this.finishedPlayers = new boolean[players.length];
        for (int i = 0; i < players.length; i++) {
            piecesInCastleByPlayer[i] = PIECES_PER_PLAYER;
        }
        this.piecesOnBoard = new boolean[players.length][PIECES_PER_PLAYER];
        this.pieceSteps = new int[players.length][PIECES_PER_PLAYER];
        this.piecesInHomePath = new boolean[players.length][PIECES_PER_PLAYER];
        this.finishedPieces = new boolean[players.length][PIECES_PER_PLAYER];
        this.piecesInInnerPath = new boolean[players.length][PIECES_PER_PLAYER];
        this.selectedPieceIndex = 0;
    }

    /** SRS 3.2.1.3.3 - number of pieces of a player that are still in the castle. */
    public int getPiecesInCastle(int playerIndex) {
        return piecesInCastleByPlayer[playerIndex];
    }

    /** SRS 3.2.1.3.3 - starts the game clock. Called once, at the first roll; later calls do nothing. */
    public void startClock() {
        if (startTimeMillis < 0) {
            startTimeMillis = System.currentTimeMillis();
        }
    }

    public boolean isClockStarted() {
        return startTimeMillis >= 0;
    }

    /** SRS 3.2.1.3.3 - game duration in seconds since the first roll (0 before the first roll). */
    public long getElapsedSeconds() {
        if (startTimeMillis < 0) {
            return 0;
        }
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }

    public int getLastStepDie() {
        return lastStepDie;
    }

    public void setLastStepDie(int lastStepDie) {
        this.lastStepDie = lastStepDie;
    }

    public int getConsecutiveSixes() {
        return consecutiveSixes;
    }

    public void setConsecutiveSixes(int consecutiveSixes) {
        this.consecutiveSixes = consecutiveSixes;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getPendingSteps() {
        return pendingSteps;
    }

    public void setPendingSteps(int pendingSteps) {
        this.pendingSteps = pendingSteps;
    }

    public String getCurrentPlayerName() {
        return players[currentPlayer].getName();
    }

    public String getCurrentPlayerColor() {
        return players[currentPlayer].getColor();
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getPlayerCount() {
        return players.length;
    }

    public Player getPlayer(int playerIndex) {
        return players[playerIndex];
    }

    public Player[] getPlayers() {
        return players.clone();
    }

    public boolean isFirstTurnPulsePending() {
        return firstTurnPulsePending;
    }

    public void setFirstTurnPulsePending(boolean firstTurnPulsePending) {
        this.firstTurnPulsePending = firstTurnPulsePending;
    }

    // בדיקה אם משתתף מסוים כבר סיים את המשחק
    public boolean isPlayerFinished(int playerIndex) {
        return finishedPlayers[playerIndex];
    }

    // עדכון מצב סיום של משתתף
    public void setPlayerFinished(int playerIndex, boolean finished) {
        finishedPlayers[playerIndex] = finished;
    }
    // בדיקה אם לשחקן הפעיל נותרו כלים בטירה
    public boolean hasPieceInCastle() {
        return piecesInCastleByPlayer[currentPlayer] > 0;
    }

    // מספר הכלים בטירה של השחקן הפעיל
    public int getPiecesInCastle() {
        return piecesInCastleByPlayer[currentPlayer];
    }


    public boolean hasPieceOnBoard() {
        return piecesOnBoard[currentPlayer][selectedPieceIndex];
    }

    public int getPieceStep() {
        return pieceSteps[currentPlayer][selectedPieceIndex];
    }


    // עדכון מספר הכלים בטירה של השחקן הפעיל בלבד
    public void setPiecesInCastle(int piecesInCastle) {
        if (piecesInCastle < 0 || piecesInCastle > PIECES_PER_PLAYER) {
            throw new IllegalArgumentException(
                "Invalid castle piece count"
            );
        }

        piecesInCastleByPlayer[currentPlayer] = piecesInCastle;
    }


    public void setPieceOnBoard(boolean pieceOnBoard) {
        piecesOnBoard[currentPlayer][selectedPieceIndex] = pieceOnBoard;
    }

    public void setPieceStep(int pieceStep) {
        pieceSteps[currentPlayer][selectedPieceIndex] = pieceStep;
    }

    public void setPieceInHomePath(boolean pieceInHomePath) {
        piecesInHomePath[currentPlayer][selectedPieceIndex] = pieceInHomePath;
    }

    public boolean isPieceInHomePath() {
        return piecesInHomePath[currentPlayer][selectedPieceIndex];
    }

    // SRS 3.2.4.3.2 - Returns whether the active piece is currently on an inner path.
    public boolean isPieceInInnerPath() {
        return piecesInInnerPath[currentPlayer][selectedPieceIndex];
    }

    // SRS 3.2.4.3.2 - Updates whether the active piece is currently on an inner path.
    public void setPieceInInnerPath(boolean pieceInInnerPath) {
        piecesInInnerPath[currentPlayer][selectedPieceIndex] = pieceInInnerPath;
    }

    // SRS 3.2.4.3.1 - Selects one of the active player's four pieces.
    public boolean selectPiece(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= PIECES_PER_PLAYER) {
            return false;
        }

        selectedPieceIndex = pieceIndex;
        return true;
    }

    public int getSelectedPieceIndex() {
        return selectedPieceIndex;
    }

    public boolean isPieceOnBoard(int playerIndex, int pieceIndex) {
        return piecesOnBoard[playerIndex][pieceIndex];
    }

    public int getPieceStep(int playerIndex, int pieceIndex) {
        return pieceSteps[playerIndex][pieceIndex];
    }


    // בדיקה אם כלי של משתתף מסוים כבר הגיע לבית הסיום
    public boolean isPieceFinished(int playerIndex, int pieceIndex) {
        return finishedPieces[playerIndex][pieceIndex];
    }

    // מספר הכלים שהגיעו לבית הסיום אצל משתתף מסוים
    public int getFinishedPieceCount(int playerIndex) {

        int count = 0;

        for (int piece = 0; piece < PIECES_PER_PLAYER; piece++) {
            if (finishedPieces[playerIndex][piece]) {
                count++;
            }
        }

        return count;
    }

    // סימון הכלי שנבחר כסיים
    public boolean finishSelectedPiece() {

        if (!hasPieceOnBoard()
                || finishedPieces[currentPlayer][selectedPieceIndex]) {
            return false;
        }

        // סימון הכלי כסיים והוצאתו ממסלולי התנועה
        finishedPieces[currentPlayer][selectedPieceIndex] = true;
        piecesOnBoard[currentPlayer][selectedPieceIndex] = false;
        piecesInHomePath[currentPlayer][selectedPieceIndex] = true;
        pieceSteps[currentPlayer][selectedPieceIndex] = 6;

        // משתתף מסיים לאחר שכל ארבעת הכלים הגיעו לבית הסיום
        if (getFinishedPieceCount(currentPlayer) == PIECES_PER_PLAYER) {
            finishedPlayers[currentPlayer] = true;
        }

        return true;
    }

    
    public boolean isPieceInHomePath(int playerIndex, int pieceIndex) {
        return piecesInHomePath[playerIndex][pieceIndex];
    }

    public boolean isPieceInInnerPath(int playerIndex, int pieceIndex) {
        return piecesInInnerPath[playerIndex][pieceIndex];
    }

    // SRS 3.2.4.3.1 - Returns whether the current player has at least one piece on the board.
    public boolean hasAnyPieceOnBoard() {
        for (int pieceIndex = 0; pieceIndex < PIECES_PER_PLAYER; pieceIndex++) {
            if (piecesOnBoard[currentPlayer][pieceIndex]) {
                return true;
            }
        }
        return false;
    }
}
