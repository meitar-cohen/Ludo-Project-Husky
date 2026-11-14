package Tests;
import View.BoardPanel;

public class BoardRouteStructureCheck {

    public static void main(String[] args) {

        String[] colors = {"Red", "Blue", "Green", "Yellow"};

        // בדיקה של כל המסלולים בכל רמת קושי
        check("Easy", new int[]{52}, colors);
        check("Medium", new int[]{52, 64}, colors);
        check("Hard", new int[]{52, 64, 72}, colors);
    }

    private static void check(
            String difficulty, int[] expected, String[] colors) {

        // יצירת לוח ברמת הקושי הנבדקת
        BoardPanel board = new BoardPanel(difficulty, colors);

        // בדיקת מספר המסלולים
        if (board.getSharedRouteCount() != expected.length) {
            throw new AssertionError(
                difficulty + ": incorrect number of routes"
            );
        }

        // בדיקת מספר המשבצות בכל מסלול
        for (int i = 0; i < expected.length; i++) {

            int actual = board.getSharedRouteSize(i);

            if (actual != expected[i]) {
                throw new AssertionError(
                    difficulty + " route " + i
                    + ": expected " + expected[i]
                    + ", got " + actual
                );
            }
        }

        System.out.println("PASS - " + difficulty);
    }
}