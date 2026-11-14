package Tests;

import Controller.GameController;
import Controller.MovementController;
import Enums.RollAction;
import Enums.Stage;
import Model.Game;

public class CastleChoiceCheck {

    public static void main(String[] args) {

        // בדיקה 1 - אפשר לבחור כלי קיים לאחר הטלת 6
        Game game = new Game(TestPlayers.of(new String[] {"Blue", "Red"}, new String[] {"Blue", "Red"}), "Easy");

        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);

        // לשחקן יש שלושה כלים בטירה וכלי אחד על הלוח
        game.setPiecesInCastle(3);
        game.setPieceOnBoard(true);

        controller.getTurnController().registerRollAndCheckThirdSix(6);

        if (controller.getTurnController().finishRoll(6, 0) != RollAction.TAKE_OUT ||
                !controllerPieces.chooseBoardMoveInsteadOfTakeOut() ||
                game.getStage() != Stage.MOVE ||
                game.getPiecesInCastle() != 3) {

            throw new AssertionError(
                "Could not choose board move after rolling 6"
            );
        }

        System.out.println(
            "PASS - choose existing piece after rolling 6"
        );

        // בדיקה 2 - אי אפשר לבחור תנועה אם כל הכלים בטירה
        Game empty = new Game(TestPlayers.of(new String[] {"Blue", "Red"}, new String[] {"Blue", "Red"}), "Easy");

        MovementController onlyCastlePieces = new MovementController(empty);
        GameController onlyCastle = new GameController(empty, onlyCastlePieces);

        onlyCastle.getTurnController().registerRollAndCheckThirdSix(6);

        if (onlyCastle.getTurnController().finishRoll(6, 0) != RollAction.TAKE_OUT ||
                onlyCastlePieces.chooseBoardMoveInsteadOfTakeOut() ||
                empty.getStage() != Stage.TAKE_OUT) {

            throw new AssertionError(
                "Should not allow board movement"
            );
        }

        System.out.println(
            "PASS - castle-only player cannot choose board move"
        );

        // בדיקה 3 - בתוצאה 0 אי אפשר לבצע תנועה רגילה
        Game zero = new Game(TestPlayers.of(new String[] {"Blue", "Red"}, new String[] {"Blue", "Red"}), "Medium");

        zero.setPiecesInCastle(3);
        zero.setPieceOnBoard(true);

        MovementController zeroControllerPieces = new MovementController(zero);
        GameController zeroController = new GameController(zero, zeroControllerPieces);

        zeroController.getTurnController().registerRollAndCheckThirdSix(6);

        if (zeroController.getTurnController().finishRoll(6, 1) != RollAction.TAKE_OUT ||
                zeroControllerPieces.chooseBoardMoveInsteadOfTakeOut()) {

            throw new AssertionError(
                "Zero movement must not allow normal board movement"
            );
        }

        System.out.println(
            "PASS - zero movement does not allow normal board move"
        );
    }
}
