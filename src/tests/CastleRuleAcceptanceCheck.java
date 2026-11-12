package tests;

import controller.GameController;
import enums.RollAction;
import enums.Stage;
import model.Game;

public class CastleRuleAcceptanceCheck {

    private static Game newGame(String level) {
        return new Game(TestPlayers.of(new String[]{"A", "B"}, new String[]{"Red", "Blue"}), level);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // בדיקת הוצאה מהטירה
    private static void testCastleExit(String level) {

        Game game = newGame(level);
        GameController controller = new GameController(game);

        int bonus = "Easy".equals(level) ? 0 : 1;

        // ללא 6 אי אפשר לצאת מהטירה
        controller.registerRollAndCheckThirdSix(5);

        check(controller.finishRoll(5, bonus)
                != RollAction.TAKE_OUT,
                level + ": exit without six");

        check(!controller.canTakeOutActivePiece(0),
                level + ": illegal castle selection");

        check(game.getPiecesInCastle(0) == 4,
                level + ": castle unchanged");

        // עם 6 מותר לצאת, גם כאשר Boost שווה 1
        controller.registerRollAndCheckThirdSix(6);

        check(controller.finishRoll(6, bonus)
                == RollAction.TAKE_OUT,
                level + ": exit with six");

        // בדיקת אינדקסים לא חוקיים
        check(!controller.canTakeOutActivePiece(-1)
                && !controller.canTakeOutActivePiece(4),
                level + ": invalid slot");

        // הוצאת הכלי הראשון
        check(controller.takeOutActivePiece(0),
                level + ": first piece exit");

        check(game.isPieceOnBoard(0, 0)
                && game.getPieceStep(0, 0) == 0,
                level + ": start square");

        // מספר הכלים בטירה של כל שחקן
        check(game.getPiecesInCastle(0) == 3
                && game.getPiecesInCastle(1) == 4,
                level + ": castle counts");

        // אפשר להטיל שוב אחרי 6
        check(game.getStage() == Stage.ROLL,
                level + ": extra roll");

        // אי אפשר להוציא שוב את אותו הכלי
        game.setStage(Stage.TAKE_OUT);

        check(!controller.canTakeOutActivePiece(0)
                && !controller.takeOutActivePiece(0),
                level + ": duplicate exit");

        check(game.getPiecesInCastle(0) == 3,
                level + ": count unchanged");

        System.out.println("PASS - " + level + " castle exit");
    }

    // בדיקת בחירה במהלך חלופי
    private static void testMoveChoice(String level) {

        Game game = newGame(level);
        GameController controller = new GameController(game);

        int bonus = "Easy".equals(level) ? 0 : 2;

        // כלי אחד כבר נמצא על הלוח
        game.setPiecesInCastle(3);
        game.setPieceOnBoard(true);

        controller.registerRollAndCheckThirdSix(6);

        check(controller.finishRoll(6, bonus)
                == RollAction.TAKE_OUT,
                level + ": choice stage");

        check(controller.canChooseBoardMoveInsteadOfTakeOut(),
                level + ": board choice available");

        check(controller.chooseBoardMoveInsteadOfTakeOut(),
                level + ": board choice selected");

        check(game.getStage() == Stage.MOVE,
                level + ": move stage");

        // בחירת תנועה לא מוציאה כלי מהטירה
        check(game.getPiecesInCastle(0) == 3,
                level + ": castle count unchanged");

        System.out.println("PASS - " + level + " board choice");
    }

    // ב-Boost 0 אסור להזיז כלי במסלול המשותף
    private static void testZeroBoost() {

        Game game = newGame("Medium");
        GameController controller = new GameController(game);

        game.setPiecesInCastle(3);
        game.setPieceOnBoard(true);

        controller.registerRollAndCheckThirdSix(6);

        check(controller.finishRoll(6, 1)
                == RollAction.TAKE_OUT,
                "Zero Boost castle exit");

        check(!controller.canChooseBoardMoveInsteadOfTakeOut(),
                "No regular move with zero Boost");

        check(game.getStage() == Stage.TAKE_OUT,
                "Castle exit remains available");

        System.out.println("PASS - zero Boost castle option");
    }

    public static void main(String[] args) {

        String[] levels = {"Easy", "Medium", "Hard"};

        for (String level : levels) {
            testCastleExit(level);
            testMoveChoice(level);
        }

        testZeroBoost();

        System.out.println(
                "PASS - SRS 3.2.3.3.4 controller acceptance");
    }
}
