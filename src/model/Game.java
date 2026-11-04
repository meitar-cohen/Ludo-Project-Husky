package model;

import enums.Stage;

public class Game {
    private Stage stage = Stage.ROLL;
    private int currentPlayer;
    private int pendingSteps;
    private boolean firstTurnPulsePending = true;
    private int piecesInCastle = 4;
    private boolean pieceOnBoard;
    private int pieceStep;
    private boolean pieceInHomePath;
    private final String[] names;
    private final String[] colors;
    private final String difficulty;

    public Game(String[] names, String[] colors, String difficulty) {
        this.names = names;
        this.colors = colors;
        this.difficulty = difficulty;
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
        return pieceOnBoard;
    }

    public int getPieceStep() {
        return pieceStep;
    }

    public void setPiecesInCastle(int piecesInCastle) {
        this.piecesInCastle = piecesInCastle;
    }

    public void setPieceOnBoard(boolean pieceOnBoard) {
        this.pieceOnBoard = pieceOnBoard;
    }

    public void setPieceStep(int pieceStep) {
        this.pieceStep = pieceStep;
    }

    public void setPieceInHomePath(boolean pieceInHomePath) {
        this.pieceInHomePath = pieceInHomePath;
    }

    public boolean isPieceInHomePath() {
        return pieceInHomePath;
    }
}
