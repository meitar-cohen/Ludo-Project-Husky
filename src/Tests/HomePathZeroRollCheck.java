package Tests;
import Controller.GameController;
import Controller.MovementController;
import Enums.RollAction;
import Enums.Stage;
import Model.Game;

public class HomePathZeroRollCheck {

    public static void main(String[] args) {

        checkZeroWithHome("Medium");
        checkZeroWithHome("Hard");
        checkZeroWithoutHome();
        checkMixedPieces();
        checkNormalMovement();
        checkHomeBoundary();

        System.out.println("PASS - all home path roll checks");
    }

    private static Game newGame(String difficulty) {
        return new Game(TestPlayers.of(new String[]{"Alice", "Bob"}, new String[]{"Red", "Blue"}), difficulty);
    }

    // בדיקה שתוצאה 0 עדיין מאפשרת תנועה במסלול הבית
    private static void checkZeroWithHome(String difficulty) {

        Game game = newGame(difficulty);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setInHomePath(true);
        game.getSelectedPiece().setSteps(1);

        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);
        controller.getTurnController().registerRollAndCheckThirdSix(4);

        same(RollAction.MOVE, controller.getTurnController().finishRoll(4, 1),
                difficulty + " chooses MOVE");

        same(Stage.MOVE, game.getStage(), difficulty + " stage");
        same(0, game.getPendingSteps(), difficulty + " multiplier");

        same(5, controllerPieces.validatePieceMove(0, 6),
                difficulty + " home target");

        yes(controllerPieces.movePiece(0, 5, 6),
                difficulty + " moves home piece");

        same(5, game.getSelectedPiece().getSteps(),
                difficulty + " saved position");

        System.out.println("PASS - " + difficulty
                + " zero boost moves home piece");
    }

    // בדיקה שתוצאה 0 לא מאפשרת תנועה במסלול משותף
    private static void checkZeroWithoutHome() {

        Game game = newGame("Medium");
        game.getSelectedPiece().setOnBoard(true);

        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);
        controller.getTurnController().registerRollAndCheckThirdSix(4);

        same(RollAction.NEED_SIX, controller.getTurnController().finishRoll(4, 1),
                "Zero boost with shared path only");

        System.out.println("PASS - shared path cannot move");
    }

    // בדיקה שאפשר לבחור רק את הכלי שבמסלול הבית
    private static void checkMixedPieces() {

        Game game = newGame("Medium");

        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setSteps(3);

        game.selectPiece(1);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setInHomePath(true);
        game.getSelectedPiece().setSteps(0);

        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);
        controller.getTurnController().registerRollAndCheckThirdSix(3);

        same(RollAction.MOVE, controller.getTurnController().finishRoll(3, 1),
                "Mixed pieces roll");

        same(-1, controllerPieces.validatePieceMove(0, 64),
                "Shared piece must not move");

        same(3, controllerPieces.validatePieceMove(1, 6),
                "Home piece must move");

        System.out.println("PASS - mixed pieces");
    }

    // בדיקה שתנועה רגילה עדיין משתמשת במכפיל
    private static void checkNormalMovement() {

        Game game = newGame("Medium");
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setSteps(2);

        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);
        controller.getTurnController().registerRollAndCheckThirdSix(4);

        same(RollAction.MOVE, controller.getTurnController().finishRoll(4, 3),
                "Normal roll");

        same(10, controllerPieces.validatePieceMove(0, 64),
                "Boosted target");

        System.out.println("PASS - normal Boost movement");
    }

    // בדיקה שלא חורגים מהמשבצות של מסלול הבית
    private static void checkHomeBoundary() {

        Game game = newGame("Medium");
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setInHomePath(true);
        game.getSelectedPiece().setSteps(4);

        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);
        controller.getTurnController().registerRollAndCheckThirdSix(3);

        same(RollAction.NEED_SIX, controller.getTurnController().finishRoll(3, 1),
                "Home path boundary");

        System.out.println("PASS - home path boundary");
    }

    private static void same(
            Object expected, Object actual, String description) {

        if (!expected.equals(actual)) {
            throw new AssertionError(
                description + ": expected " + expected
                + ", actual " + actual
            );
        }
    }

    private static void yes(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}