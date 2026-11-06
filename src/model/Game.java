package model;

import enums.Stage;

public class Game {
    private Stage stage = Stage.ROLL;
    private int currentPlayer;
    private int pendingSteps;
    private boolean firstTurnPulsePending = true;
    private int piecesInCastle = 4;

    // SRS 3.2.4.3.1 (Maor Mizrahi) - Stores the state and position of each player's pieces.
    private final boolean[][] piecesOnBoard;
    private final int[][] pieceSteps;
    private final boolean[][] piecesInHomePath;
    private int selectedPieceIndex;

    // SRS 3.2.4.3.2 (Maor Mizrahi) -Stores whether each piece is on an inner path.
    private final boolean[][] piecesInInnerPath;
    private final String[] names;
    private final String[] colors;
    private final String difficulty;

    // SRS 3.2.1.3.3 - opening state and game clock (Daniel Azulay)
    public static final int PIECES_PER_PLAYER = 4;
    private final int[] piecesInCastleByPlayer;
    private final long startTimeMillis;

    // SRS 3.2.3.3.5 - extra turn and three sixes in a row (Daniel Azulay)
    private int lastStepDie;
    private int consecutiveSixes;

    public Game(String[] names, String[] colors, String difficulty) {
        this.names = names;
        this.colors = colors;
        this.difficulty = difficulty;
        // SRS 3.2.1.3.3 - all four pieces of every player start in the castle, and the clock starts.
        this.piecesInCastleByPlayer = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            piecesInCastleByPlayer[i] = PIECES_PER_PLAYER;
        }
        this.piecesOnBoard = new boolean[names.length][PIECES_PER_PLAYER];
        this.pieceSteps = new int[names.length][PIECES_PER_PLAYER];
        this.piecesInHomePath = new boolean[names.length][PIECES_PER_PLAYER];
        this.piecesInInnerPath = new boolean[names.length][PIECES_PER_PLAYER];
        this.selectedPieceIndex = 0;
        this.startTimeMillis = System.currentTimeMillis();
    }

    /** SRS 3.2.1.3.3 - number of pieces of a player that are still in the castle. */
    public int getPiecesInCastle(int playerIndex) {
        return piecesInCastleByPlayer[playerIndex];
    }

    /** SRS 3.2.1.3.3 - game duration in seconds since the game started. */
    public long getElapsedSeconds() {
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
        return names[currentPlayer];
    }

    public String getCurrentPlayerColor() {
        return colors[currentPlayer];
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getPlayerCount() {
        return names.length;
    }

    public boolean isFirstTurnPulsePending() {
        return firstTurnPulsePending;
    }

    public void setFirstTurnPulsePending(boolean firstTurnPulsePending) {
        this.firstTurnPulsePending = firstTurnPulsePending;
    }

    public boolean hasPieceInCastle() {
        return piecesInCastle > 0;
    }

    public int getPiecesInCastle() {
        return piecesInCastle;
    }

    public boolean hasPieceOnBoard() {
        return piecesOnBoard[currentPlayer][selectedPieceIndex];
    }

    public int getPieceStep() {
        return pieceSteps[currentPlayer][selectedPieceIndex];
    }

    public void setPiecesInCastle(int piecesInCastle) {
        this.piecesInCastle = piecesInCastle;
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

    // SRS 3.2.4.3.2 (Maor Mizrahi) - Returns whether the active piece is currently on an inner path.
    public boolean isPieceInInnerPath() {
        return piecesInInnerPath[currentPlayer][selectedPieceIndex];
    }

    // SRS 3.2.4.3.2 (Maor Mizrahi) - Updates whether the active piece is currently on an inner path.
    public void setPieceInInnerPath(boolean pieceInInnerPath) {
        piecesInInnerPath[currentPlayer][selectedPieceIndex] = pieceInInnerPath;
    }

    // SRS 3.2.4.3.1 (Maor Mizrahi) - Selects one of the active player's four pieces.
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

    public boolean isPieceInHomePath(int playerIndex, int pieceIndex) {
        return piecesInHomePath[playerIndex][pieceIndex];
    }

    public boolean isPieceInInnerPath(int playerIndex, int pieceIndex) {
        return piecesInInnerPath[playerIndex][pieceIndex];
    }

    // SRS 3.2.4.3.1 (Maor Mizrahi) - Returns whether the current player has at least one piece on the board.
    public boolean hasAnyPieceOnBoard() {
        for (int pieceIndex = 0; pieceIndex < PIECES_PER_PLAYER; pieceIndex++) {
            if (piecesOnBoard[currentPlayer][pieceIndex]) {
                return true;
            }
        }
        return false;
    }
}
