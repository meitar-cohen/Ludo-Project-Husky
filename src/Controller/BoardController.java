package Controller;

import Enums.SquareType;
import Model.Square;
import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Manages the special squares placed on a game board. */
public class BoardController {
    private final Map<Point, Square> squares = new HashMap<>();

    public void addSpecialSquare(Point position, SquareType type, Color color, int direction) {
        squares.put(new Point(position), new Square(type, color, direction));
    }

    public void randomizeSpecialSquares(List<List<Point>> routes, int rings, Set<Point> excludedCells, Color easyQuestion, Color mediumQuestion, Color hardQuestion, Color surprise) {
        squares.entrySet().removeIf(entry -> {
            SquareType type = entry.getValue().getType();
            return type == SquareType.STAR
                    || type == SquareType.QUESTION
                    || type == SquareType.SURPRISE;
        });

        placeRandomSpecialsOnRoute(
                routes.get(0), excludedCells, 4, 0, 0, 0, 0,
                easyQuestion, mediumQuestion, hardQuestion, surprise);
        if (rings == 1) {
            placeRandomSpecialsOnRoute(
                    routes.get(1), excludedCells, 4, 2, 2, 2, 2,
                    easyQuestion, mediumQuestion, hardQuestion, surprise);
        } else if (rings == 2) {
            placeRandomSpecialsOnRoute(
                    routes.get(1), excludedCells, 4, 1, 1, 1, 1,
                    easyQuestion, mediumQuestion, hardQuestion, surprise);
            placeRandomSpecialsOnRoute(
                    routes.get(2), excludedCells, 4, 2, 2, 2, 2,
                    easyQuestion, mediumQuestion, hardQuestion, surprise);
        } else if (rings != 0) {
            throw new IllegalArgumentException("Unsupported board ring count.");
        }
    }

    private void placeRandomSpecialsOnRoute(List<Point> route, Set<Point> excludedCells, int stars, int easyQuestions, int mediumQuestions, int hardQuestions, int surprises, Color easyQuestion, Color mediumQuestion, Color hardQuestion, Color surprise) {
        List<Point> available = new ArrayList<>();
        for (Point cell : route) {
            if (!squares.containsKey(cell) && !excludedCells.contains(cell)) {
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
                    easyQuestion, 0);
        }
        for (int i = 0; i < mediumQuestions; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.QUESTION,
                    mediumQuestion, 0);
        }
        for (int i = 0; i < hardQuestions; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.QUESTION,
                    hardQuestion, 0);
        }
        for (int i = 0; i < surprises; i++) {
            addSpecialSquare(
                    available.get(index++), SquareType.SURPRISE,
                    surprise, 0);
        }
    }

    public boolean hasSpecialSquare(Point position) {
        return squares.containsKey(position);
    }

    public void removeSpecialSquares(SquareType... types) {
        squares.entrySet().removeIf(entry -> {
            for (SquareType type : types) {
                if (entry.getValue().getType() == type) {
                    return true;
                }
            }
            return false;
        });
    }

    public SquareType getSpecialSquareType(Point position) {
        Square square = squares.get(position);
        return square == null ? null : square.getType();
    }

    public Color getSpecialSquareColor(Point position) {
        Square square = squares.get(position);
        return square == null ? null : square.getColor();
    }

    public int getSpecialSquareDirection(Point position) {
        Square square = squares.get(position);
        return square == null ? -1 : square.getDirection();
    }

    public List<Point> getSpecialSquarePositions() {
        List<Point> positions = new ArrayList<>();
        for (Point position : squares.keySet()) {
            positions.add(new Point(position));
        }
        return positions;
    }
}
