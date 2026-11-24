package Controller;

import java.util.ArrayList;
import java.util.List;
import Model.Game;
import Model.Piece;
import Model.SurpriseContext;
import Model.SurpriseEngine;
import Model.SurprisePlan;
import Model.SurpriseType;
import Enums.Stage;

/**
 * בקר הפתעות: מפעיל את Strategy שנבחרה ואת ההשפעה על מודל המשחק.
 * לא מכיל רכיבי Swing או החלטות על עיצוב הממשק.
 */
public final class SurpriseController {
    private final Game game;
    private final MovementController movement;
    private final TurnController turns;
    private final SurpriseEngine engine;
    // the board rules (Double Block); null when the controller is used without a board
    private final GameController board;

    public SurpriseController(Game game, MovementController movement, TurnController turns) {
        this(game, movement, turns, new SurpriseEngine());
    }

    public SurpriseController(Game game, MovementController movement,
            TurnController turns, SurpriseEngine engine) {
        this(game, movement, turns, engine, null);
    }

    SurpriseController(Game game, MovementController movement,
            TurnController turns, SurpriseEngine engine, GameController board) {
        if (game == null || movement == null || turns == null || engine == null)
            throw new IllegalArgumentException("Surprise dependencies are required");
        this.game = game;
        this.movement = movement;
        this.turns = turns;
        this.engine = engine;
        this.board = board;
    }

    /**
     * SRS 3.2.5.3.4 and 3.2.3.3.7 - a move caused by a surprise also stops on the square before an
     * opponent's Double Block. Returns the step the piece really reaches ({@code to} when free).
     */
    private int stopBeforeBlock(int playerIndex, int pieceIndex, int from, int to) {
        return board == null ? to : board.getStepsBeforeBlock(playerIndex, pieceIndex, from, to);
    }

    public SurprisePlan drawForPiece(int pieceIndex, int routeSize) {
        return engine.draw(context(pieceIndex, routeSize));
    }

    // נקודת כניסה דטרמיניסטית לצורכי בדיקות.
    public SurprisePlan resolveForPiece(SurpriseType type, int pieceIndex, int routeSize) {
        return engine.resolve(type, context(pieceIndex, routeSize));
    }

    private SurpriseContext context(int pieceIndex, int routeSize) {
        if (game.getStage() != Stage.BUSY || !game.selectPiece(pieceIndex))
            throw new IllegalStateException("Surprise requires an active moving piece");
        Piece piece = game.getSelectedPiece();
        if (!piece.isOnBoard() || piece.isFinished())
            throw new IllegalStateException("Surprise requires an on-board piece");
        return new SurpriseContext(piece.getSteps(), routeSize);
    }

    public List<Integer> eligibleOpponents() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < game.getPlayerCount(); i++) {
            if (i == game.getCurrentPlayer() || game.isPlayerFinished(i)) continue;
            for (int piece = 0; piece < Game.PIECES_PER_PLAYER; piece++) {
                if (game.getPlayer(i).getPiece(piece).isOnBoard()) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * מפעילה את ההשפעה. עבור MOVE_OPPONENT יש לספק יריב חוקי ותוצאת קובייה 1..6.
     * עבור הסוגים האחרים הפרמטרים אינם בשימוש (העבירי ‎-1,0).
     */
    public SurpriseResolution apply(SurprisePlan plan, int pieceIndex,
            int routeSize, int opponentIndex, int normalDie) {
        if (plan == null) throw new IllegalArgumentException("Missing surprise plan");
        // הקונטקסט מוודא שהכלי הנוכחי והמשבצת תואמים למצב המשחק.
        SurpriseContext now = context(pieceIndex, routeSize);
        int actor = game.getCurrentPlayer();
        int start = now.getCurrentStep();

        switch (plan.getType()) {
            case FINISH_PIECE: {
                if (!movement.finishPieceFromSurprise(pieceIndex))
                    throw new IllegalStateException("Cannot finish surprise piece");
                return result(plan, actor, pieceIndex, -1, -1,
                        "STRAIGHT TO HOME!", "Your piece reached the finish directly.");
            }
            case RETURN_TO_CASTLE: {
                if (!movement.returnPieceToCastleFromSurprise(pieceIndex))
                    throw new IllegalStateException("Cannot return piece to castle");
                return result(plan, actor, pieceIndex, -1, -1,
                        "BACK TO THE CASTLE!", "Your piece returns to its castle.");
            }
            case MOVE_BACK_THREE: {
                int target = plan.getTargetStep();
                if (target != Math.max(0, start - 3))
                    throw new IllegalArgumentException("Invalid movement plan");
                int reached = stopBeforeBlock(actor, pieceIndex, start, target);
                if (!movement.applyEventMovement(pieceIndex, reached, routeSize))
                    throw new IllegalStateException("Cannot move backward");
                String text = reached == target
                        ? "Your piece moves back " + (start - reached) + " squares."
                        : reached == start
                        ? "A Double Block stops your piece: it cannot move back."
                        : "A Double Block stops your piece after " + (start - reached) + " squares.";
                return result(plan, actor, pieceIndex, start, reached, "A LITTLE DETOUR!", text);
            }
            case SKIP_NEXT_TURN: {
                turns.markSkipNextTurn(actor);
                return result(plan, actor, pieceIndex, -1, -1,
                        "MISS YOUR NEXT TURN!", "Your next turn will be skipped.");
            }
            case EXTRA_TURN_ULTRABOOST: {
                turns.requestSurpriseExtraTurn();
                return result(plan, actor, pieceIndex, -1, -1,
                        "BONUS ULTRABOOST!", "Roll again immediately with UltraBoost (1-6).");
            }
            case MOVE_OPPONENT: {
                if (eligibleOpponents().isEmpty()) {
                    return result(plan, actor, pieceIndex, -1, -1,
                            "NO TARGET AVAILABLE", "No opponent has a piece on the board.");
                }
                if (!eligibleOpponents().contains(opponentIndex))
                    throw new IllegalArgumentException("Invalid surprise opponent");
                if (normalDie < 1 || normalDie > 6)
                    throw new IllegalArgumentException("Normal die must be 1-6");
                int chosen = -1;
                // בוחרים את הכלי הראשון של היריב שעל המסלול.
                for (int i = 0; i < Game.PIECES_PER_PLAYER; i++) {
                    if (game.getPlayer(opponentIndex).getPiece(i).isOnBoard()) {
                        chosen = i;
                        break;
                    }
                }
                if (chosen < 0) throw new IllegalStateException("Opponent target disappeared");
                Piece enemy = game.getPlayer(opponentIndex).getPiece(chosen);
                int before = enemy.getSteps();
                int distance = (normalDie + 1) / 2; // עיגול כלפי מעלה לפי ההוראות
                int wanted = Math.max(0, before - distance);
                int reached = stopBeforeBlock(opponentIndex, chosen, before, wanted);
                int after = movement.moveOpponentPieceBack(opponentIndex, chosen, before - reached);
                if (after < 0) throw new IllegalStateException("Cannot move opponent backward");
                return result(plan, opponentIndex, chosen, before, after,
                        "OPPONENT DETOUR!", "Rolled " + normalDie + "; opponent moves back "
                                + (before - after) + " squares."
                                + (reached == wanted ? "" : " A Double Block stops it."));
            }
            default:
                throw new IllegalStateException("Unhandled surprise: " + plan.getType());
        }
    }

    private static SurpriseResolution result(SurprisePlan plan, int player, int piece,
            int before, int after, String title, String description) {
        return new SurpriseResolution(plan.getType(), player, piece,
                before, after, title, description);
    }
}