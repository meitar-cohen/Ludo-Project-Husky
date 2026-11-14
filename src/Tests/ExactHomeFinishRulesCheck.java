package Tests;

import Model.Game;
import Controller.GameController;
import Controller.MovementController;
import Enums.Stage;

public class ExactHomeFinishRulesCheck {

    private static Game newGame(String level) {
        return new Game(TestPlayers.of(new String[]{"A", "B"}, new String[]{"Red", "Blue"}), level);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void prepareHomePiece(
            Game game, int position, int die) {

        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setInHomePath(true);
        game.getSelectedPiece().setSteps(position);
        game.setLastStepDie(die);
        game.setPendingSteps(0);
        game.setStage(Stage.MOVE);
    }

    // בדיקת תוצאה מדויקת, קטנה מדי וגדולה מדי
    private static void testHomeFinish(String level) {

        Game game = newGame(level);
        MovementController controllerPieces = new MovementController(game);
        // משבצת 4, תוצאה 3 - חריגה מבית הסיום
        prepareHomePiece(game, 4, 3);

        check(!controllerPieces.tryFinishPiece(0, 6),
                level + ": overshoot rejected");

        check(game.getSelectedPiece().getSteps() == 4
                && !game.getPlayer(0).getPiece(0).isFinished(),
                level + ": unchanged after overshoot");

        check(!controllerPieces.hasAnyLegalMove(
                new int[]{6, 6, 6, 6}),
                level + ": overshoot not a legal move");

        // משבצת 4, תוצאה 1 - תנועה רגילה ולא סיום
        game.setLastStepDie(1);

        check(!controllerPieces.tryFinishPiece(0, 6),
                level + ": short result not finish");

        check(controllerPieces.validatePieceMove(0, 6) == 5,
                level + ": normal movement is legal");

        // משבצת 4, תוצאה 2 - כניסה מדויקת לבית הסיום
        game.setLastStepDie(2);

        check(controllerPieces.hasAnyLegalMove(
                new int[]{6, 6, 6, 6}),
                level + ": exact finish is legal");

        check(controllerPieces.tryFinishPiece(0, 6),
                level + ": exact finish succeeded");

        check(game.getPlayer(0).getPiece(0).isFinished()
                && game.getPlayer(0).getPiece(0).getSteps() == 6,
                level + ": finish position");

        game.getSelectedPiece().setInHomePath(false);
        check(game.getSelectedPiece().isFinished(),
                level + ": leaving home path does not reset a finished piece");

        check(!game.getPlayer(0).getPiece(0).isOnBoard()
                && game.getPlayer(0).getFinishedPieceCount() == 1,
                level + ": finished piece removed from path");

        // כלי שכבר סיים לא ניתן לבחירה נוספת
        game.setStage(Stage.MOVE);

        check(!controllerPieces.isPieceSelectionLegal(0)
                && !controllerPieces.tryFinishPiece(0, 6),
                level + ": finished piece cannot move");

        check(!game.isPlayerFinished(0),
                level + ": player not finished after one piece");

        System.out.println(
                "PASS - " + level + " exact/over/under finish");
    }

    // בדיקת סיום ישיר מהמסלול המשותף הפנימי
    private static void testDirectFinish(String level) {

        Game game = newGame(level);
        MovementController controllerPieces = new MovementController(game);
        game.getSelectedPiece().setOnBoard(true);
        game.getSelectedPiece().setInInnerPath(true);
        game.getSelectedPiece().setSteps(50);
        game.setPendingSteps(8);
        game.setLastStepDie(4);
        game.setStage(Stage.MOVE);

        check(controllerPieces.hasAnyLegalMove(
                new int[]{52, 52, 52, 52}),
                level + ": direct finish is legal");

        check(controllerPieces.tryFinishPiece(0, 52),
                level + ": direct finish succeeded");

        check(game.getPlayer(0).getPiece(0).isFinished(),
                level + ": direct finish saved");

        System.out.println(
                "PASS - " + level + " direct shared finish");
    }

    // בדיקה שארבעת הכלים מסיימים את השתתפות השחקן
    private static void testAllFourPieces() {

        Game game = newGame("Easy");
        MovementController controllerPieces = new MovementController(game);
        GameController controller = new GameController(game, controllerPieces);

        for (int piece = 0; piece < 4; piece++) {

            game.selectPiece(piece);
            game.getSelectedPiece().setOnBoard(true);
            game.getSelectedPiece().setInHomePath(true);
            game.getSelectedPiece().setSteps(5);
            game.setLastStepDie(1);
            game.setStage(Stage.MOVE);

            check(controllerPieces.tryFinishPiece(piece, 6),
                    "Piece " + piece + " finished");
        }

        check(game.isPlayerFinished(0)
                && !game.isPlayerFinished(1),
                "Player finishes after four pieces");

        check(game.getPlayer(0).getFinishedPieceCount() == 4,
                "All four pieces counted");

        // במשחק זוגי לא נותרת תחרות פעילה
        controller.getTurnController().passTurn();

        check(game.getStage() == Stage.DONE,
                "Game ends after four finished pieces");

        System.out.println(
                "PASS - all four pieces and game completion");
    }

    public static void main(String[] args) {

        for (String level :
                new String[]{"Easy", "Medium", "Hard"}) {
            testHomeFinish(level);
        }

        testDirectFinish("Medium");
        testDirectFinish("Hard");
        testAllFourPieces();

        System.out.println(
                "PASS - SRS 3.2.4.3.4 controller acceptance");
    }
}
