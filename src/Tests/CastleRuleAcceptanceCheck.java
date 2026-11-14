package Tests;

import Controller.GameController;
import Controller.MovementController;
import Enums.RollAction;
import Enums.Stage;
import Model.Game;

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
        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);

        int bonus = "Easy".equals(level) ? 0 : 1;

        // ללא 6 אי אפשר לצאת מהטירה
        controller.getTurnController().registerRollAndCheckThirdSix(5);

        check(controller.getTurnController().finishRoll(5, bonus)
                != RollAction.TAKE_OUT,
                level + ": exit without six");

        check(!controllerPieces.canTakeOutActivePiece(0),
                level + ": illegal castle selection");

        check(game.getPiecesInCastle(0) == 4,
                level + ": castle unchanged");

        // עם 6 מותר לצאת, גם כאשר Boost שווה 1
        controller.getTurnController().registerRollAndCheckThirdSix(6);

        check(controller.getTurnController().finishRoll(6, bonus)
                == RollAction.TAKE_OUT,
                level + ": exit with six");

        // בדיקת אינדקסים לא חוקיים
        check(!controllerPieces.canTakeOutActivePiece(-1)
                && !controllerPieces.canTakeOutActivePiece(4),
                level + ": invalid slot");

        // הוצאת הכלי הראשון
        check(controllerPieces.takeOutActivePiece(0),
                level + ": first piece exit");

        check(game.getPlayer(0).getPiece(0).isOnBoard()
                && game.getPlayer(0).getPiece(0).getSteps() == 0,
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

        check(!controllerPieces.canTakeOutActivePiece(0)
                && !controllerPieces.takeOutActivePiece(0),
                level + ": duplicate exit");

        check(game.getPiecesInCastle(0) == 3,
                level + ": count unchanged");

        System.out.println("PASS - " + level + " castle exit");
    }

    // בדיקת בחירה במהלך חלופי
    private static void testMoveChoice(String level) {

        Game game = newGame(level);
        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);

        int bonus = "Easy".equals(level) ? 0 : 2;

        // כלי אחד כבר נמצא על הלוח
        game.setPiecesInCastle(3);
        game.getSelectedPiece().setOnBoard(true);

        controller.getTurnController().registerRollAndCheckThirdSix(6);

        check(controller.getTurnController().finishRoll(6, bonus)
                == RollAction.TAKE_OUT,
                level + ": choice stage");

        check(controllerPieces.canChooseBoardMoveInsteadOfTakeOut(),
                level + ": board choice available");

        check(controllerPieces.chooseBoardMoveInsteadOfTakeOut(),
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
        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);

        game.setPiecesInCastle(3);
        game.getSelectedPiece().setOnBoard(true);

        controller.getTurnController().registerRollAndCheckThirdSix(6);

        check(controller.getTurnController().finishRoll(6, 1)
                == RollAction.TAKE_OUT,
                "Zero Boost castle exit");

        check(!controllerPieces.canChooseBoardMoveInsteadOfTakeOut(),
                "No regular move with zero Boost");

        check(game.getStage() == Stage.TAKE_OUT,
                "Castle exit remains available");

        System.out.println("PASS - zero Boost castle option");
    }

    // SRS 3.2.3.3.4 - כלי שכבר הגיע לסיום אינו בטירה ולא ניתן להוציא אותו שוב
    private static void testFinishedPieceCannotLeaveCastle() {
        Game game = new Game(TestPlayers.of(new String[]{"A", "B"},
                new String[]{"Red", "Blue"}), "Easy");
        MovementController pieces = new MovementController(game);
        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);
        game.getPlayer(0).getPiece(0).finish();
        game.setPiecesInCastle(3);
        game.setStage(Stage.TAKE_OUT);
        check(!pieces.canTakeOutActivePiece(0) && !pieces.takeOutActivePiece(0),
                "finished piece must not leave the castle again");
        check(game.getPlayer(0).getPiece(0).isFinished(), "finished piece stays finished");
        check(pieces.canTakeOutActivePiece(1), "a piece in the castle can still leave");
        System.out.println("PASS - finished piece is not taken out of the castle");
    }

    public static void main(String[] args) {
        testFinishedPieceCannotLeaveCastle();

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
