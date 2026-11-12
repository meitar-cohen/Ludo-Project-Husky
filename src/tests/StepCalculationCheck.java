package tests;

import controller.GameController;
import model.Game;

public class StepCalculationCheck {

    // בדיקת חישוב הצעדים לפי רמת הקושי
    private static void check(String label, String difficulty,
                              int stepDie, int boostDie, int expected) {

        Game game = new Game(TestPlayers.of(new String[] {"Player1", "Player2"}, new String[] {"Red", "Blue"}), difficulty);

        GameController controller = new GameController(game);
        int actual = controller.calculateSteps(stepDie, boostDie);

        if (actual != expected) {
            throw new AssertionError(
                label + ": expected " + expected + ", got " + actual
            );
        }

        System.out.println("PASS - " + label);
    }

    public static void main(String[] args) {

        // בדיקות חישוב הצעדים
        check("Easy", "Easy", 4, 0, 4);
        check("Medium x2", "Medium", 4, 3, 8);
        check("Medium zero", "Medium", 4, 1, 0);
        check("Hard x5", "Hard", 4, 6, 20);
        check("Hard zero", "Hard", 4, 1, 0);

        // בדיקות קצה חדשות
        check("Easy ignores bonus die", "Easy", 6, 3, 6);
        check("Hard maximum movement", "Hard", 6, 6, 30);

        // בדיקה שהמשחק משתמש בתוצאה המחושבת
        Game game1 = new Game(TestPlayers.of(new String[] {"Player1", "Player2"}, new String[] {"Red", "Blue"}), "Medium");

        game1.setPieceOnBoard(true);
        GameController controller1 = new GameController(game1);

        String action1 = controller1.finishRoll(4, 3).name();

        if (controller1.getPendingSteps() != 8 ||
            !"MOVE".equals(action1)) {
            throw new AssertionError("Medium movement integration failed");
        }

        System.out.println("PASS - Medium movement integration");

        // בדיקת תוצאה 0 ללא כלי במסלול הבית
        Game game2 = new Game(TestPlayers.of(new String[] {"Player1", "Player2"}, new String[] {"Red", "Blue"}), "Medium");

        game2.setPieceOnBoard(true);
        GameController controller2 = new GameController(game2);

        String action2 = controller2.finishRoll(4, 1).name();

        if (controller2.getPendingSteps() != 0 ||
            !"NEED_SIX".equals(action2)) {
            throw new AssertionError("Zero movement integration failed");
        }

        System.out.println("PASS - Zero movement integration");

        // בדיקת הוצאת כלי בהטלת 6 גם כשהמכפיל נותן 0
        Game game3 = new Game(TestPlayers.of(new String[] {"Player1", "Player2"}, new String[] {"Red", "Blue"}), "Medium");

        GameController controller3 = new GameController(game3);

        String action3 = controller3.finishRoll(6, 1).name();

        if (controller3.getPendingSteps() != 0 ||
            !"TAKE_OUT".equals(action3)) {
            throw new AssertionError("Castle entry with zero movement failed");
        }

        System.out.println("PASS - Castle entry with zero movement");
    }
}
