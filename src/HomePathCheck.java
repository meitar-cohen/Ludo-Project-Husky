import view.BoardPanel;

public class HomePathCheck {

    public static void main(String[] args) {

        // צבעי השחקנים ורמות הקושי
        String[] colors = {"Red", "Blue", "Green", "Yellow"};
        String[] levels = {"Easy", "Medium", "Hard"};

        // בדיקת מסלול הבית בכל רמה ולכל צבע
        for (String difficulty : levels) {

            BoardPanel board = new BoardPanel(difficulty, colors);

            for (String color : colors) {

                // בחירת השחקן שאת מסלול הבית שלו בודקים
                board.setActivePlayerColor(color);

                // בדיקת מספר המשבצות במסלול הבית
                int actual = board.getActiveHomePathSize();

                if (actual != 6) {
                    throw new AssertionError(
                        difficulty + " / " + color
                        + ": expected 6 home squares, got " + actual
                    );
                }
            }

            System.out.println(
                "PASS - " + difficulty
                + ": six home squares for all players"
            );
        }
    }
}