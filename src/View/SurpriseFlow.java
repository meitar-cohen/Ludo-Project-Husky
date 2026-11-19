package View;

import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import Controller.GameController;
import Controller.SurpriseController;
import Controller.SurpriseResolution;
import Controller.TurnController;
import Model.SurprisePlan;
import Model.SurpriseType;

/** מתאם תצוגה להפעלת הפתעות; אינו קובע חוקי משחק בעצמו. */
final class SurpriseFlow {
    private final GameController game;
    private final TurnController turns;
    private final BoardPanel board;
    private final int pieceIndex;
    private final Window owner;
    private final Runnable endTurn;
    private final Runnable passTurn;
    private final Consumer<String> status;

    SurpriseFlow(GameController game, TurnController turns, BoardPanel board,
            int pieceIndex, Window owner, Runnable endTurn,
            Runnable passTurn, Consumer<String> status) {
        this.game = game;
        this.turns = turns;
        this.board = board;
        this.pieceIndex = pieceIndex;
        this.owner = owner;
        this.endTurn = endTurn;
        this.passTurn = passTurn;
        this.status = status;
    }

    void run() {
        SurpriseController controller = game.getSurpriseController();
        SurprisePlan plan = controller.drawForPiece(pieceIndex,
                board.getActiveRouteSize(pieceIndex));
        int opponent = -1;
        int die = 0;
        if (plan.getType() == SurpriseType.MOVE_OPPONENT) {
            List<Integer> players = controller.eligibleOpponents();
            if (!players.isEmpty()) {
                String[] names = game.getPlayerNames();
                String[] choices = players.stream().map(i -> names[i]).toArray(String[]::new);
                int choice = JOptionPane.showOptionDialog(owner,
                        "Choose an opponent to move backward:", "Surprise - Opponent",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, choices, choices[0]);
                // גם בסגירת תיבת הבחירה ההפתעה חייבת להתבצע.
                opponent = players.get(choice < 0 ? 0 : choice);
                die = turns.rollStepDie();
            }
        }
        SurpriseResolution outcome = controller.apply(plan, pieceIndex,
                board.getActiveRouteSize(pieceIndex), opponent, die);

        SurpriseDialog dialog = new SurpriseDialog(owner,
                outcome.getTitle(), heading(plan.getType()), outcome.getDescription());
        dialog.setVisible(true);
        status.accept(outcome.getDescription());
        switch (plan.getType()) {
            case FINISH_PIECE:
                if (!board.showActivePieceFinished(pieceIndex))
                    throw new IllegalStateException("Surprise finish view is out of sync");
                if (game.getMovementController().isCurrentPlayerFinished()) passTurn.run();
                else endTurn.run();
                break;
            case RETURN_TO_CASTLE:
                if (!board.showActivePieceReturnedToCastle(pieceIndex))
                    throw new IllegalStateException("Surprise castle view is out of sync");
                endTurn.run();
                break;
            case MOVE_BACK_THREE:
                board.animatePiece(pieceIndex, outcome.getBeforeStep(),
                        outcome.getAfterStep(), endTurn);
                break;
            case MOVE_OPPONENT:
                if (outcome.hasAnimatedMovement()) {
                    board.animateOtherPlayerPiece(outcome.getAffectedPlayer(),
                            outcome.getAffectedPiece(), outcome.getBeforeStep(),
                            outcome.getAfterStep(), endTurn);
                } else {
                    endTurn.run();
                }
                break;
            case SKIP_NEXT_TURN:
            case EXTRA_TURN_ULTRABOOST:
                endTurn.run();
                break;
            default:
                throw new IllegalStateException("Unknown surprise effect");
        }
    }

    private static String heading(SurpriseType type) {
        return switch (type) {
            case FINISH_PIECE -> "INSTANT FINISH";
            case MOVE_OPPONENT -> "OPPONENT MOVEMENT";
            case RETURN_TO_CASTLE -> "RETURN TO CASTLE";
            case MOVE_BACK_THREE -> "BACK 3 SQUARES";
            case SKIP_NEXT_TURN -> "SKIP NEXT TURN";
            case EXTRA_TURN_ULTRABOOST -> "ULTRA BOOST ROLL";
        };
    }
}