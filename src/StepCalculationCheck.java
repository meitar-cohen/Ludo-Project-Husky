
import controller.GameController;
import model.Game;

public class StepCalculationCheck {

    // בדיקה של נוסחת הצעדים לפי רמת הקושי
    private static void check(String label, String difficulty,
                              int stepDie, int boostDie, int expected) {

        // יצירת משחק זמני עבור הבדיקה
        Game game = new Game(
            new String[] {"Player1", "Player2"},
            new String[] {"Red", "Blue"},
            difficulty
        );

        GameController controller = new GameController(game);

        // חישוב מספר הצעדים בפועל
        int actual = controller.calculateSteps(stepDie, boostDie);

        if (actual != expected) {
            throw new AssertionError(
                label + ": expected " + expected + ", got " + actual
            );
        }

        System.out.println("PASS - " + label);
    }

    public static void main(String[] args) {

        // בדיקה 1: רמה קלה - קוביית צעדים בלבד
        check("Easy", "Easy", 4, 0, 4);

        // בדיקה 2: רמה בינונית - חישוב מכפיל
        check("Medium x2", "Medium", 4, 3, 8);

        // בדיקה 3: רמה בינונית - תוצאה 0
        check("Medium zero", "Medium", 4, 1, 0);

        // בדיקה 4: רמה קשה - חישוב מכפיל
        check("Hard x5", "Hard", 4, 6, 20);

        // בדיקה 5: רמה קשה - תוצאה 0
        check("Hard zero", "Hard", 4, 1, 0);


        // בדיקה 6: התוצאה המחושבת משמשת לתנועה במשחק
        Game game1 = new Game(
            new String[] {"Player1", "Player2"},
            new String[] {"Red", "Blue"},
            "Medium"
        );

        // הצבת כלי של השחקן הפעיל על הלוח
        game1.setPieceOnBoard(true);

        GameController controller1 = new GameController(game1);
        String action1 = controller1.finishRoll(4, 3).name();

        if (controller1.getPendingSteps() != 8 ||
            !"MOVE".equals(action1)) {
            throw new AssertionError(
                "Medium movement integration failed"
            );
        }

        System.out.println("PASS - Medium movement integration");


        // בדיקה 7: תוצאה 0 כשאין כלי במסלול הבית
        Game game2 = new Game(
            new String[] {"Player1", "Player2"},
            new String[] {"Red", "Blue"},
            "Medium"
        );

        game2.setPieceOnBoard(true);

        GameController controller2 = new GameController(game2);
        String action2 = controller2.finishRoll(4, 1).name();

        if (controller2.getPendingSteps() != 0 ||
            !"NEED_SIX".equals(action2)) {
            throw new AssertionError(
                "Zero movement integration failed"
            );
        }

        System.out.println("PASS - Zero movement integration");


        // בדיקה 8: הטלת 6 מאפשרת הוצאת כלי גם כשהמכפיל נותן 0
        Game game3 = new Game(
            new String[] {"Player1", "Player2"},
            new String[] {"Red", "Blue"},
            "Medium"
        );

        GameController controller3 = new GameController(game3);
        String action3 = controller3.finishRoll(6, 1).name();

        if (controller3.getPendingSteps() != 0 ||
            !"TAKE_OUT".equals(action3)) {
            throw new AssertionError(
                "Castle entry with zero movement failed"
            );
        }

        System.out.println("PASS - Castle entry with zero movement");
    }
}
