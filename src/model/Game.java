package Model;

import Enums.Stage;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private Stage stage = Stage.ROLL;
    private int currentPlayer;
    private int pendingSteps;
    private boolean firstTurnPulsePending = true;
    

    private int selectedPieceIndex;
    private final Player[] players;
    private final String difficulty;

    // מספר הכלים בתחילת המשחק ושעון המשחק
    public static final int PIECES_PER_PLAYER = 4;
    private int[] piecesInCastleByPlayer;
    
    // שמירת המשתתפים שכבר סיימו את המשחק
    private boolean[] finishedPlayers;
    // הסדר שבו המשתתפים סיימו את המשחק
    private final List<Integer> finishOrder = new ArrayList<>();
    // משחק שהסתיים נכתב להיסטוריה פעם אחת בלבד
    private boolean savedToHistory;

    // תור נוסף ושלוש שישיות ברצף
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
        // כל ארבעת הכלים של כל משתתף מתחילים בטירה.
        // שעון המשחק מתחיל בהטלה הראשונה.
        this.piecesInCastleByPlayer = new int[players.length];
        this.finishedPlayers = new boolean[players.length];
        for (int i = 0; i < players.length; i++) {
            piecesInCastleByPlayer[i] = PIECES_PER_PLAYER;
        }
        this.selectedPieceIndex = 0;
    }

    /** מספר הכלים של המשתתף שעדיין נמצאים בטירה. */
    public int getPiecesInCastle(int playerIndex) {
        return piecesInCastleByPlayer[playerIndex];
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

    public List<Integer> getFinishOrder() {
        return new ArrayList<>(finishOrder);
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
        if (finished) {
            recordFinish(playerIndex);
        } else {
            finishOrder.remove(Integer.valueOf(playerIndex));
        }
    }

    // SRS 3.2.8.3.1 - שמירת הסדר שבו המשתתפים סיימו
    private void recordFinish(int playerIndex) {
        if (!finishOrder.contains(playerIndex)) {
            finishOrder.add(playerIndex);
        }
    }

    public boolean isSavedToHistory() {
        return savedToHistory;
    }

    public void markSavedToHistory() {
        savedToHistory = true;
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
        return getSelectedPiece().isOnBoard();
    }

    public int getPieceStep() {
        return getSelectedPiece().getSteps();
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
        Piece piece = getSelectedPiece();
        if (pieceOnBoard) {
            piece.setState(Enums.PieceState.BOARD);
        } else {
            piece.setState(Enums.PieceState.CASTLE);
        }
    }

    public void setPieceStep(int pieceStep) {
        getSelectedPiece().setSteps(pieceStep);
    }

    public void setPieceInHomePath(boolean pieceInHomePath) {
        Piece piece = getSelectedPiece();
        if (pieceInHomePath) {
            piece.setState(Enums.PieceState.HOME_PATH);
        } else if (!piece.isFinished()) {
            piece.setState(Enums.PieceState.BOARD);
        }
    }

    public boolean isPieceInHomePath() {
        return getSelectedPiece().isInHomePath();
    }

    // מחזיר האם הכלי הפעיל נמצא במסלול הפנימי.
    public boolean isPieceInInnerPath() {
        return getSelectedPiece().isInInnerPath();
    }

    // מעדכן האם הכלי הפעיל נמצא במסלול הפנימי.
    public void setPieceInInnerPath(boolean pieceInInnerPath) {
        getSelectedPiece().setInInnerPath(pieceInInnerPath);
    }

    // בוחר אחד מארבעת הכלים של המשתתף הפעיל.
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
        return players[playerIndex].getPiece(pieceIndex).isOnBoard();
    }

    public int getPieceStep(int playerIndex, int pieceIndex) {
        return players[playerIndex].getPiece(pieceIndex).getSteps();
    }


    // בדיקה אם כלי של משתתף מסוים כבר הגיע לבית הסיום
    public boolean isPieceFinished(int playerIndex, int pieceIndex) {
        return players[playerIndex].getPiece(pieceIndex).isFinished();
    }

    // מספר הכלים שהגיעו לבית הסיום אצל משתתף מסוים
    public int getFinishedPieceCount(int playerIndex) {
        return players[playerIndex].getFinishedPieceCount();
    }

    public boolean isPieceInHomePath(int playerIndex, int pieceIndex) {
        return players[playerIndex].getPiece(pieceIndex).isInHomePath();
    }

    public boolean isPieceInInnerPath(int playerIndex, int pieceIndex) {
        return players[playerIndex].getPiece(pieceIndex).isInInnerPath();
    }

    // בודק האם למשתתף הפעיל יש לפחות כלי אחד על הלוח.
    public boolean hasAnyPieceOnBoard() {
        return players[currentPlayer].hasPieceOnBoard();
    }

    private Piece getSelectedPiece() {
        return players[currentPlayer].getPiece(selectedPieceIndex);
    }
}
