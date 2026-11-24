package Tests;

import java.awt.Point;

import Controller.GameController;
import Controller.MovementController;
import Controller.SurpriseController;
import Controller.SurpriseResolution;
import Enums.SquareType;
import Enums.Stage;
import Model.Game;
import Model.Piece;
import Model.Player;
import Model.SurprisePlan;
import Model.SurpriseType;

/**
 * SRS 3.2.5.3.4 and 3.2.3.3.7 - the two surprise moves backward ("move back 3" and "move an
 * opponent back") stop on the square before an opponent's Double Block, like every other move.
 * On the Easy board Ann (Green) step s is the same square as Bob (Red) step s + 13.
 */
public final class SurpriseBlockCheck {
    private SurpriseBlockCheck() {}

    private static final int ROUTE = 52;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Setup {
        final Game game = new Game(new Player[]{new Player("Ann", "Green"), new Player("Bob", "Red")}, "Easy");
        final GameController controller = GameController.forTesting(game, new MovementController(game));
        final SurpriseController surprises = controller.getSurpriseController();

        void place(int player, int piece, int steps) {
            Piece model = game.getPlayer(player).getPiece(piece);
            model.setOnBoard(true);
            model.setSteps(steps);
            game.setPiecesInCastle(player, game.getPiecesInCastle(player) - 1);
        }

        /** A Double Block of Bob on the square that is step {@code annStep} for Ann. */
        void bobBlockAt(int annStep) {
            place(1, 1, annStep + 13);
            place(1, 2, annStep + 13);
        }

        /** A Double Block of Ann on the square that is step {@code annStep} for Ann. */
        void annBlockAt(int annStep) {
            place(0, 1, annStep);
            place(0, 2, annStep);
        }

        /** Ann's piece 0 is on the board at {@code steps} and the surprise "move back 3" happens to it. */
        SurpriseResolution backThree(int steps) {
            place(0, 0, steps);
            game.setCurrentPlayer(0);
            game.setStage(Stage.BUSY);
            SurprisePlan plan = surprises.resolveForPiece(SurpriseType.MOVE_BACK_THREE, 0, ROUTE);
            return surprises.apply(plan, 0, ROUTE, -1, 0);
        }
    }

    public static void main(String[] args) {
        // --- move back 3
        Setup free = new Setup();
        SurpriseResolution r = free.backThree(20);
        check(r.getAfterStep() == 17 && free.game.getPlayer(0).getPiece(0).getSteps() == 17, "no block: 3 back");
        check(!r.getDescription().contains("Double Block"), "no block: no word about a block");

        Setup two = new Setup();
        two.bobBlockAt(18);
        r = two.backThree(20);
        check(r.getAfterStep() == 19 && two.game.getPlayer(0).getPiece(0).getSteps() == 19,
                "a block 2 squares behind: the piece stops before it");
        check(r.getDescription().contains("Double Block") && r.hasAnimatedMovement(), "the player is told");

        Setup next = new Setup();
        next.bobBlockAt(19);
        r = next.backThree(20);
        check(r.getAfterStep() == 20 && next.game.getPlayer(0).getPiece(0).getSteps() == 20,
                "a block on the very next square: the piece cannot move");
        check(!r.hasAnimatedMovement() && r.getDescription().contains("cannot move back"),
                "nothing to animate, and the player is told");

        Setup landing = new Setup();
        landing.bobBlockAt(17);
        check(landing.backThree(20).getAfterStep() == 18, "a block on the landing square: stop before it");

        Setup far = new Setup();
        far.bobBlockAt(16);
        check(far.backThree(20).getAfterStep() == 17, "a block beyond the landing square does not matter");
        System.out.println("PASS - move back 3 stops before an opponent's Double Block");

        Setup own = new Setup();
        own.annBlockAt(18);
        check(own.backThree(20).getAfterStep() == 17, "the player's own block does not stop it");

        Setup star = new Setup();
        star.bobBlockAt(18);
        Point square = star.controller.getPieceBoardPosition(1, 1);
        star.controller.addSpecialSquare(square, SquareType.STAR, null, 0);
        check(star.backThree(20).getAfterStep() == 19, "a block on a protected square still blocks");

        Setup start = new Setup();
        start.bobBlockAt(1);
        check(start.backThree(2).getAfterStep() == 2, "near the start square: the block keeps the piece in place");
        Setup atStart = new Setup();
        check(atStart.backThree(1).getAfterStep() == 0, "the rule of the start square (step 0) still works");
        System.out.println("PASS - own blocks, protected squares and the start square");

        // --- move an opponent back
        Setup opponent = new Setup();
        opponent.place(0, 0, 5);
        opponent.place(1, 0, 33);                       // Bob's piece, square idx 20
        opponent.annBlockAt(18);                        // Ann's Double Block 2 squares behind it
        opponent.game.setCurrentPlayer(0);
        opponent.game.setStage(Stage.BUSY);
        SurprisePlan plan = opponent.surprises.resolveForPiece(SurpriseType.MOVE_OPPONENT, 0, ROUTE);
        r = opponent.surprises.apply(plan, 0, ROUTE, 1, 6);          // die 6: half is 3
        check(r.getAfterStep() == 32 && opponent.game.getPlayer(1).getPiece(0).getSteps() == 32,
                "the opponent stops before Ann's block (32 instead of 30)");
        check(r.getDescription().contains("Double Block"), "the player is told");

        Setup openRoad = new Setup();
        openRoad.place(0, 0, 5);
        openRoad.place(1, 0, 33);
        openRoad.game.setCurrentPlayer(0);
        openRoad.game.setStage(Stage.BUSY);
        plan = openRoad.surprises.resolveForPiece(SurpriseType.MOVE_OPPONENT, 0, ROUTE);
        check(openRoad.surprises.apply(plan, 0, ROUTE, 1, 6).getAfterStep() == 30, "no block: half of the die back");

        Setup ownBlock = new Setup();
        ownBlock.place(0, 0, 5);
        ownBlock.place(1, 0, 33);
        ownBlock.bobBlockAt(18);                        // the opponent's own block is behind its piece
        ownBlock.game.setCurrentPlayer(0);
        ownBlock.game.setStage(Stage.BUSY);
        plan = ownBlock.surprises.resolveForPiece(SurpriseType.MOVE_OPPONENT, 0, ROUTE);
        check(ownBlock.surprises.apply(plan, 0, ROUTE, 1, 6).getAfterStep() == 30,
                "the opponent is not stopped by its own block");
        System.out.println("PASS - an opponent moved back by a surprise stops before a Double Block");
        System.out.println("All surprise block checks passed.");
    }
}
