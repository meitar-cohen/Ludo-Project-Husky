package tests;
import View.BoardPanel;

public class BoardPathCheck {

    public static void main(String[] args) {

        // הצבעים של ארבעת השחקנים האפשריים
        String[] colors = {"Red", "Blue", "Green", "Yellow"};

        // בדיקת אורך המסלול בכל רמת קושי
        checkPath("Easy", 52, colors);
        checkPath("Medium", 64, colors);
        checkPath("Hard", 72, colors);
    }

    // בדיקת אורך המסלול עבור כל אחד מהצבעים
    private static void checkPath(
            String difficulty,
            int expected,
            String[] colors) {

        // יצירת לוח זמני ברמת הקושי הנבדקת
        BoardPanel board = new BoardPanel(difficulty, colors);

        for (String color : colors) {

            // בחירת צבע השחקן הפעיל
            board.setActivePlayerColor(color);

            // קבלת אורך המסלול שלו
            int actual = board.getActiveRouteSize(0);

            // בדיקה שהאורך תואם לדרישה
            if (actual != expected) {
                throw new AssertionError(
                    difficulty + " " + color
                    + ": expected " + expected
                    + ", got " + actual
                );
            }
        }

        System.out.println(
            "PASS - " + difficulty
            + ": " + expected + " squares for all players"
        );
    }
}