package Controller;

import java.awt.Color;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import Enums.RollAction;
import Enums.Stage;
import Enums.SquareType;
import Model.Game;
import Model.Player;
import Model.Question;
import Model.Square;
import java.util.ArrayList;
import java.util.List;

/** Coordinates game setup, dice rolls, turns, and game phases. */
public class GameController {
    public static final int PIECES_PER_PLAYER = Game.PIECES_PER_PLAYER;

    private final Game game;
    private final MovementController pieces;
    private final Random random = new Random();
    private final Question question = new Question(
            "Which document defines the software requirements according to IEEE Std 830?",
            new String[]{
                "SRS - Software Requirements Specification",
                "SDP - Software Development Plan",
                "STD - Software Test Document",
                "SDD - Software Design Description"
            },
            0,
            "Easy"
    );
    private long startTimeMillis = -1;
    private long endTimeMillis = -1;

    public GameController(Game game, MovementController pieces) {
        this.game = game;
        this.pieces = pieces;
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

    public static SpecialSquareInfo createSpecialSquare(
            SquareType type, Color color, int direction) {
        return new SpecialSquareInfo(new Square(type, color, direction));
    }

    public static final class SpecialSquareInfo {
        private final Square square;

        private SpecialSquareInfo(Square square) {
            this.square = square;
        }

        public SquareType getType() {
            return square.getType();
        }

        public Color getColor() {
            return square.getColor();
        }

        public int getDirection() {
            return square.getDirection();
        }
    }

    public boolean isCorrectQuestionAnswer(int selectedIndex) {
        return selectedIndex == question.getCorrectIndex();
    }

    public String getQuestionText() {
        return question.getText();
    }

    public String[] getQuestionOptions() {
        return question.getOptions();
    }

    public String getQuestionDifficulty() {
        return question.getDifficulty();
    }

    public int getCorrectQuestionAnswerIndex() {
        return question.getCorrectIndex();
    }

    public MovementController getMovementController() {
        return pieces;
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
    public static Player[] createPlayers(String[] names, String[] colors) {
        Player[] players = new Player[names.length];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(names[i], colors[i]);
        }
        return players;
    }

    public boolean canRoll() {
        return game.getStage() == Stage.ROLL;
    }

    public boolean consumeFirstTurnPulse() {
        if (!game.isFirstTurnPulsePending()) {
            return false;
        }
        game.setFirstTurnPulsePending(false);
        return true;
    }

    public int rollStepDie() {
        return random.nextInt(6) + 1;
    }

    public int rollBoostDie() {
        int faces = getBoostDieFaces();
        return faces == 0 ? 0 : random.nextInt(faces) + 1;
    }

    public int getBoostDieFaces() {
        if ("Hard".equals(game.getDifficulty())) {
            return 6;
        }
        if ("Medium".equals(game.getDifficulty())) {
            return 3;
        }
        return 0;
    }

    public boolean registerRollAndCheckThirdSix(int stepDie) {
        game.setLastStepDie(stepDie);
        if (stepDie != 6) {
            game.setConsecutiveSixes(0);
            return false;
        }
        game.setConsecutiveSixes(game.getConsecutiveSixes() + 1);
        if (game.getConsecutiveSixes() >= 3) {
            game.setConsecutiveSixes(0);
            return true;
        }
        return false;
    }

    public boolean hasExtraTurn() {
        return game.getLastStepDie() == 6;
    }

    public void grantExtraTurn() {
        game.setStage(Stage.ROLL);
    }

    public RollAction finishRoll(int stepDie, int boostDie) {
        game.setPendingSteps(calculateSteps(stepDie, boostDie));
        if (stepDie == 6 && game.hasPieceInCastle()) {
            game.setStage(Stage.TAKE_OUT);
            return RollAction.TAKE_OUT;
        }
        if ((game.getPendingSteps() > 0 && game.hasAnyPieceOnBoard())
                || pieces.hasAvailableHomePathMove(stepDie)) {
            game.setStage(Stage.MOVE);
            return RollAction.MOVE;
        }
        game.setStage(Stage.ROLL);
        return RollAction.NEED_SIX;
    }

    public int calculateSteps(int stepDie, int boostDie) {
        if ("Easy".equals(game.getDifficulty())) {
            return stepDie;
        }
        return stepDie * (boostDie - 1);
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

    public void passTurn() {
        game.setConsecutiveSixes(0);
        int total = game.getPlayerCount();
        int active = 0;
        for (int player = 0; player < total; player++) {
            if (!game.isPlayerFinished(player)) {
                active++;
            }
        }
        if (active <= 1) {
            game.setStage(Stage.DONE);
            return;
        }
        for (int offset = 1; offset <= total; offset++) {
            int candidate = (game.getCurrentPlayer() + offset) % total;
            if (!game.isPlayerFinished(candidate)) {
                game.setCurrentPlayer(candidate);
                game.setStage(Stage.ROLL);
                return;
            }
        }
    }
}
