package Model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stores the board geometry and special squares for one game. */
public class Board {
    private int rings;
    private int size;
    private final List<List<Point>> sharedRoutes = new ArrayList<>();
    private final Map<Point, Square> specialSquares = new HashMap<>();

    public int getRings() {
        return rings;
    }

    public void setRings(int rings) {
        this.rings = rings;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<List<Point>> getSharedRoutes() {
        List<List<Point>> result = new ArrayList<>();
        for (List<Point> route : sharedRoutes) {
            result.add(copyPoints(route));
        }
        return result;
    }

    public void setSharedRoutes(List<List<Point>> routes) {
        sharedRoutes.clear();
        for (List<Point> route : routes) {
            sharedRoutes.add(copyPoints(route));
        }
    }

    public void putSpecialSquare(Point position, Square square) {
        if (position == null || square == null) {
            throw new IllegalArgumentException("Position and square are required");
        }
        specialSquares.put(new Point(position), square);
    }

    public Square getSpecialSquare(Point position) {
        return specialSquares.get(position);
    }

    public void removeSpecialSquare(Point position) {
        specialSquares.remove(position);
    }

    public List<Point> getSpecialSquarePositions() {
        return copyPoints(new ArrayList<>(specialSquares.keySet()));
    }

    private static List<Point> copyPoints(List<Point> points) {
        List<Point> result = new ArrayList<>();
        for (Point point : points) {
            result.add(new Point(point));
        }
        return result;
    }
}
