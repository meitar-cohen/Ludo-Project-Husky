package Tests;

import Controller.BoardController;
import java.awt.Color;
import java.awt.Point;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import View.BoardPanel;

public class RandomSquaresCheck {

    private static final String[] COLORS = {
            "Green", "Yellow", "Blue", "Red"
    };

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    // קריאת שדה פרטי לצורך בדיקת קבלה בלבד
    private static Object value(Object object, String name)
            throws Exception {

        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static BoardController squares(BoardPanel board)
            throws Exception {
        return (BoardController) value(board, "boardController");
    }

    @SuppressWarnings("unchecked")
    private static List<List<Point>> routes(BoardPanel board)
            throws Exception {

        return (List<List<Point>>) value(board, "sharedRoutes");
    }

    @SuppressWarnings("unchecked")
    private static Map<Point, Color> home(BoardPanel board)
            throws Exception {

        return (Map<Point, Color>) value(board, "homeCells");
    }

    private static String type(BoardController squares, Point position) {
        return squares.getSpecialSquareType(position).toString();
    }

    // שמירת המיקומים הקבועים להשוואה בין משחקים
    private static Map<Point, String> fixedSquares(
            BoardController squares) {

        Map<Point, String> fixed = new HashMap<>();

        for (Point position : squares.getSpecialSquarePositions()) {
            String squareType = type(squares, position);

            if ("START".equals(squareType)
                    || "ARROW".equals(squareType)) {

                fixed.put(position,
                        squareType + ":"
                        + squares.getSpecialSquareColor(position)
                        + ":"
                        + squares.getSpecialSquareDirection(position));
            }
        }

        return fixed;
    }

    // בדיקת לוח אחד והחזרת המיקומים שהוגרלו
    private static String inspect(BoardPanel board, String level,
            Map<Point, String> originalFixed) throws Exception {

        BoardController special = squares(board);
        List<List<Point>> routes = routes(board);
        Map<Point, Color> home = home(board);
        List<Point> specialPositions = special.getSpecialSquarePositions();

        // איסוף כל המשבצות של המסלולים המשותפים
        Set<Point> shared = new HashSet<>();

        for (List<Point> route : routes) {
            shared.addAll(route);
        }

        int expectedCount =
                "Easy".equals(level) ? 12
                : "Medium".equals(level) ? 28 : 40;

        check(specialPositions.size() == expectedCount,
                level + ": total special squares");

        // בדיקה שהתחלות וחצים נשארו ללא שינוי
        check(fixedSquares(special).equals(originalFixed),
                level + ": fixed squares changed");

        int stars = 0;
        int questions = 0;
        int surprises = 0;
        int starts = 0;
        int arrows = 0;

        int easyQ = 0;
        int mediumQ = 0;
        int hardQ = 0;

        List<String> randomPositions = new ArrayList<>();

        for (Point cell : specialPositions) {
            String kind = type(special, cell);

            // משבצת מיוחדת חייבת להיות במסלול משותף
            check(shared.contains(cell),
                    level + ": square outside shared path");

            // אסור שמשבצת מיוחדת תהיה במסלול הבית
            check(!home.containsKey(cell),
                    level + ": special square in home path");

            if ("START".equals(kind)) starts++;
            if ("ARROW".equals(kind)) arrows++;
            if ("STAR".equals(kind)) stars++;
            if ("SURPRISE".equals(kind)) surprises++;

            if ("QUESTION".equals(kind)) {

                questions++;

                Color color = special.getSpecialSquareColor(cell);

                if (BoardPanel.Q_EASY.equals(color)) {
                    easyQ++;
                } else if (BoardPanel.Q_MEDIUM.equals(color)) {
                    mediumQ++;
                } else if (BoardPanel.Q_HARD.equals(color)) {
                    hardQ++;
                } else {
                    throw new AssertionError(
                            "Unknown question color");
                }
            }

            // מיקומים שאמורים להשתנות ממשחק למשחק
            if (!"START".equals(kind)
                    && !"ARROW".equals(kind)) {

                randomPositions.add(
                        cell.x + "," + cell.y + ":"
                        + kind + ":"
                        + special.getSpecialSquareColor(cell));
            }
        }

        int ringCount =
                "Easy".equals(level) ? 1
                : "Medium".equals(level) ? 2 : 3;

        check(routes.size() == ringCount,
                level + ": shared route count");

        check(starts == 4 && arrows == ringCount * 4,
                level + ": fixed squares count");

        check(stars == ringCount * 4,
                level + ": protection count");

        check(questions ==
                (ringCount == 1 ? 0 : ringCount == 2 ? 6 : 9),
                level + ": question count");

        check(surprises ==
                (ringCount == 1 ? 0 : ringCount == 2 ? 2 : 3),
                level + ": surprise count");

        // שאלות מתחלקות שווה בשווה בין שלוש הרמות
        check(easyQ == questions / 3
                && mediumQ == questions / 3
                && hardQ == questions / 3,
                level + ": question level distribution");

        // בדיקת חלוקת המשבצות בין המסלולים
        for (int routeIndex = 0;
                routeIndex < routes.size(); routeIndex++) {

            int routeStars = 0;
            int routeQuestions = 0;
            int routeSurprises = 0;

            for (Point point : routes.get(routeIndex)) {
                if (!special.hasSpecialSquare(point)) {
                    continue;
                }

                String kind = type(special, point);

                if ("STAR".equals(kind)) routeStars++;
                if ("QUESTION".equals(kind)) routeQuestions++;
                if ("SURPRISE".equals(kind)) routeSurprises++;
            }

            int expectedQuestions =
                    routeIndex == 0 ? 0
                    : routeIndex == routes.size() - 1 ? 6 : 3;

            int expectedSurprises =
                    routeIndex == 0 ? 0
                    : routeIndex == routes.size() - 1 ? 2 : 1;

            check(routeStars == 4,
                    level + ": route protection count");

            check(routeQuestions == expectedQuestions
                    && routeSurprises == expectedSurprises,
                    level + ": route special distribution");
        }

        Collections.sort(randomPositions);

        return randomPositions.toString();
    }

    public static void main(String[] args) throws Exception {

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {

            BoardPanel initial = new BoardPanel(level, COLORS);

            Map<Point, String> fixed =
                    fixedSquares(squares(initial));

            Set<String> layouts = new HashSet<>();

            layouts.add(inspect(initial, level, fixed));

            // יצירת 30 משחקים נוספים ובדיקת כל לוח
            for (int i = 0; i < 30; i++) {

                BoardPanel board = new BoardPanel(level, COLORS);

                layouts.add(inspect(board, level, fixed));
            }

            // המיקומים האקראיים צריכים להשתנות בין משחקים
            check(layouts.size() > 1,
                    level + ": board did not change");

            System.out.println(
                    "PASS - " + level
                    + " counts, fixed cells, no overlaps and random layouts");
        }

        System.out.println(
                "PASS - SRS 3.2.2.3.3 random special squares");
    }
}
