package Model;

import Enums.Stage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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
    private final Board board;
    public static final int PIECES_PER_PLAYER = 4;
    private int[] piecesInCastleByPlayer;
    private boolean[] finishedPlayers;
    private final List<Integer> finishOrder = new ArrayList<>();
    // משחק שהסתיים נכתב להיסטוריה פעם אחת בלבד
    private boolean savedToHistory;
    private int lastStepDie;
    private int consecutiveSixes;
    // Observer design pattern (Subject): notifies registered listeners when game state changes
    private final PropertyChangeSupport observers = new PropertyChangeSupport(this);

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
        this.board = new Board();
        initializePlayerState();
    }

    private void initializePlayerState() {
        this.piecesInCastleByPlayer = new int[players.length];
        this.finishedPlayers = new boolean[players.length];
        for (int i = 0; i < players.length; i++) {
            piecesInCastleByPlayer[i] = PIECES_PER_PLAYER;
        }

        this.selectedPieceIndex = 0;
    }

    // שמירת השחקנים שצריכים להפסיד את התור הבא
    private final java.util.Set<Integer> skipNextTurns =
            new java.util.HashSet<>();

    // סימון שחקן שיפסיד את התור הבא
    public void markSkipNextTurn(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= players.length) {
            throw new IllegalArgumentException(
                    "Invalid player index");
        }
        skipNextTurns.add(playerIndex);
    }

    // בדיקה וצריכה של עונש הדילוג פעם אחת בלבד
    public boolean consumeSkippedTurn(int playerIndex) {
        return skipNextTurns.remove(playerIndex);
    }

    // בדיקה אם לשחקן ממתין עונש דילוג
    public boolean isSkipNextTurnPending(int playerIndex) {
        return skipNextTurns.contains(playerIndex);
    }

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

    public void addObserver(PropertyChangeListener observer) {
        if (observer == null) {
            return;
        }
        for (PropertyChangeListener existing : observers.getPropertyChangeListeners()) {
            if (existing == observer) {
                return;
            }
        }
        observers.addPropertyChangeListener(observer);
    }

    public void removeObserver(PropertyChangeListener observer) {
        if (observer != null) {
            observers.removePropertyChangeListener(observer);
        }
    }

    public void notifyObservers(String eventName) {
        String property = (eventName == null || eventName.isBlank()) ? "gameState" : eventName;
        observers.firePropertyChange(property, null, null);
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        notifyObservers("stage");
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public String getCurrentPlayerName() {
        return players[currentPlayer].getName();
    }

    public String getCurrentPlayerColor() {
        return players[currentPlayer].getColor();
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
        notifyObservers("currentPlayer");
    }

    public int getPendingSteps() {
        return pendingSteps;
    }

    public void setPendingSteps(int pendingSteps) {
        this.pendingSteps = pendingSteps;
        notifyObservers("pendingSteps");
    }

    public String getDifficulty() {
        return difficulty;
    }

    public Board getBoard() {
        return board;
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

    public boolean isPlayerFinished(int playerIndex) {
        return finishedPlayers[playerIndex];
    }

    public void setPlayerFinished(int playerIndex, boolean finished) {
        finishedPlayers[playerIndex] = finished;
        if (finished) {
            recordFinish(playerIndex);
        } else {
            finishOrder.remove(Integer.valueOf(playerIndex));
        }
        notifyObservers("playerFinished");
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

    public boolean hasPieceInCastle() {
        return piecesInCastleByPlayer[currentPlayer] > 0;
    }

    public int getPiecesInCastle() {
        return piecesInCastleByPlayer[currentPlayer];
    }

    public void setPiecesInCastle(int piecesInCastle) {
        setPiecesInCastle(currentPlayer, piecesInCastle);
    }

    // SRS 3.2.5.3.1 - אכילה מחזירה כלי של משתתף שאינו בהכרח השחקן הנוכחי
    public void setPiecesInCastle(int playerIndex, int piecesInCastle) {
        if (playerIndex < 0 || playerIndex >= players.length) {
            throw new IllegalArgumentException("Invalid player index");
        }
        if (piecesInCastle < 0 || piecesInCastle > PIECES_PER_PLAYER) {
            throw new IllegalArgumentException("Invalid castle piece count");
        }

        piecesInCastleByPlayer[playerIndex] = piecesInCastle;
        notifyObservers("piecesInCastle");
    }

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

    public Piece getSelectedPiece() {
        return players[currentPlayer].getPiece(selectedPieceIndex);
    }
}
