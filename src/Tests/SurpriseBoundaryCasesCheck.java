package Tests;

import Controller.MovementController;
import Controller.SurpriseController;
import Controller.SurpriseResolution;
import Controller.TurnController;
import Enums.Stage;
import Model.Game;
import Model.Piece;
import Model.Player;
import Model.SurprisePlan;
import Model.SurpriseType;

/** בדיקות מקרי קצה למנגנון ההפתעות, ללא פתיחת משחק. */
public final class SurpriseBoundaryCasesCheck {

    private SurpriseBoundaryCasesCheck() { }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void mustReject(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static final class Setup {
        final Game game = new Game(new Player[]{
                new Player("A", "Green"), new Player("B", "Red")
        }, "Medium");
        final MovementController movement = new MovementController(game);
        final TurnController turns = new TurnController(game, movement);
        final SurpriseController controller = new SurpriseController(game, movement, turns);

        Setup(int startStep) {
            Piece actor = game.getPlayer(0).getPiece(0);
            actor.setOnBoard(true);
            actor.setSteps(startStep);
            game.setPiecesInCastle(3);
            game.setStage(Stage.BUSY);
        }

        SurprisePlan plan(SurpriseType type) {
            return controller.resolveForPiece(type, 0, 52);
        }

        SurpriseResolution apply(SurpriseType type, int opponent, int die) {
            return controller.apply(plan(type), 0, 52, opponent, die);
        }

        void putOpponentAt(int step) {
            Piece enemy = game.getPlayer(1).getPiece(0);
            enemy.setOnBoard(true);
            enemy.setSteps(step);
        }
    }

    public static void main(String[] args) {
        // גם ממש בתחילת המסלול אין תנועה לאינדקס שלילי.
        for (int start = 0; start <= 5; start++) {
            Setup setup = new Setup(start);
            SurpriseResolution result = setup.apply(SurpriseType.MOVE_BACK_THREE, -1, 0);
            int expected = Math.max(0, start - 3);
            check(result.getAfterStep() == expected
                    && setup.game.getPlayer(0).getPiece(0).getSteps() == expected,
                    "Backward movement failed at step " + start);
        }
        System.out.println("PASS - backward movement remains within route boundaries");

        // לכל ערך קובייה מאמתים עיגול כלפי מעלה ותנועה של כלי היריב בלבד.
        for (int die = 1; die <= 6; die++) {
            Setup setup = new Setup(8);
            setup.putOpponentAt(10);
            SurpriseResolution result = setup.apply(SurpriseType.MOVE_OPPONENT, 1, die);
            int expected = 10 - ((die + 1) / 2);
            check(result.getAffectedPlayer() == 1
                    && result.getAffectedPiece() == 0
                    && result.getAfterStep() == expected
                    && setup.game.getPlayer(1).getPiece(0).getSteps() == expected
                    && setup.game.getPlayer(0).getPiece(0).getSteps() == 8
                    && setup.game.getCurrentPlayer() == 0,
                    "Opponent movement failed for die=" + die);
        }
        Setup nearStart = new Setup(8);
        nearStart.putOpponentAt(1);
        check(nearStart.apply(SurpriseType.MOVE_OPPONENT, 1, 6).getAfterStep() == 0,
                "Opponent movement passed the start of the route");
        System.out.println("PASS - all six die values and opponent route boundary");

        // ללא כלי יריב על הלוח אין בחירה שגויה ואין שינוי תור.
        Setup noOpponent = new Setup(8);
        SurpriseResolution noTarget = noOpponent.apply(SurpriseType.MOVE_OPPONENT, -1, 0);
        check(noTarget.getTitle().contains("NO TARGET")
                && noOpponent.game.getCurrentPlayer() == 0,
                "Missing opponent was not handled safely");
        System.out.println("PASS - no available opponent is handled safely");

        // קלטים שגויים נדחים בלי לשנות את מיקום הכלים.
        Setup invalid = new Setup(8);
        invalid.putOpponentAt(10);
        mustReject(() -> invalid.apply(SurpriseType.MOVE_OPPONENT, -1, 5),
                "Invalid opponent was accepted");
        mustReject(() -> invalid.apply(SurpriseType.MOVE_OPPONENT, 1, 0),
                "Die zero was accepted");
        mustReject(() -> invalid.apply(SurpriseType.MOVE_OPPONENT, 1, 7),
                "Die seven was accepted");
        mustReject(() -> invalid.controller.apply(
                        new SurprisePlan(SurpriseType.MOVE_BACK_THREE, 7),
                        0, 52, -1, 0),
                "Incorrect backward movement target was accepted");
        check(invalid.game.getPlayer(0).getPiece(0).getSteps() == 8
                && invalid.game.getPlayer(1).getPiece(0).getSteps() == 10,
                "Invalid input unexpectedly changed game state");
        System.out.println("PASS - invalid inputs rejected without moving pieces");

        // ההפתעה מופעלת רק על כלי חוקי במהלך שלב התנועה.
        Setup wrongStage = new Setup(8);
        wrongStage.game.setStage(Stage.ROLL);
        mustReject(() -> wrongStage.controller.resolveForPiece(
                        SurpriseType.MOVE_BACK_THREE, 0, 52),
                "Surprise was accepted outside BUSY stage");
        Setup invalidPiece = new Setup(8);
        mustReject(() -> invalidPiece.controller.resolveForPiece(
                        SurpriseType.RETURN_TO_CASTLE, 4, 52),
                "Invalid piece index was accepted");
        System.out.println("PASS - surprise requires a legal active piece and stage");
        System.out.println("All surprise boundary case checks passed.");
    }
}