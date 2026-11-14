package Controller;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Enums.Stage;
import Enums.SquareType;
import Model.Game;
import Model.Player;
import Model.Question;
import Model.Square;

/** Coordinates game setup, board state, questions, and game results. */
public class GameController {
    public static final int PIECES_PER_PLAYER = Game.PIECES_PER_PLAYER;

    private final Game game;
    private final MovementController pieces;
    private final TurnController turnController;
    private final Map<Point, Square> specialSquares = new HashMap<>();
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
        this.turnController = new TurnController(game, pieces);
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

    public TurnController getTurnController() {
        return turnController;
    }

    public void addSpecialSquare(Point position, SquareType type, Color color, int direction) {
        specialSquares.put(
                new Point(position), new Square(type, color, direction));
    }

    public void randomizeSpecialSquares(List<List<Point>> routes, int rings, Set<Point> excludedCells, Color[] questionColors, Color surprise) {
        specialSquares.entrySet().removeIf(entry -> {
            SquareType type = entry.getValue().getType();
            return type == SquareType.STAR
                    || type == SquareType.QUESTION
                    || type == SquareType.SURPRISE;
        });

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
            if (!specialSquares.containsKey(cell)
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
                    available.get(index++), SquareType.QUESTION,
                    questionColors[0], 0);
        }
        for (int i = 0; i < mediumQuestions; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.QUESTION,
                    questionColors[1], 0);
        }
        for (int i = 0; i < hardQuestions; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.QUESTION,
                    questionColors[2], 0);
        }
        for (int i = 0; i < surprises; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.SURPRISE,
                    surprise, 0);
        }
    }

    public boolean hasSpecialSquare(Point position) {
        return specialSquares.containsKey(position);
    }

    public void removeSpecialSquares(SquareType... types) {
        specialSquares.entrySet().removeIf(entry -> {
            for (SquareType type : types) {
                if (entry.getValue().getType() == type) {
                    return true;
                }
            }
            return false;
        });
    }

    public SquareType getSpecialSquareType(Point position) {
        Square square = specialSquares.get(position);
        return square == null ? null : square.getType();
    }

    public Color getSpecialSquareColor(Point position) {
        Square square = specialSquares.get(position);
        return square == null ? null : square.getColor();
    }

    public int getSpecialSquareDirection(Point position) {
        Square square = specialSquares.get(position);
        return square == null ? -1 : square.getDirection();
    }

    public List<Point> getSpecialSquarePositions() {
        List<Point> positions = new ArrayList<>();
        for (Point position : specialSquares.keySet()) {
            positions.add(new Point(position));
        }
        return positions;
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
