package Tests;
import Tests.Controllers.GameController;
import Enums.Stage;
import Model.Game;

public class NoLegalMoveRulesCheck {

    private static final int[] EASY = {52, 52, 52, 52};
    private static final int[] MEDIUM = {64, 64, 64, 64};
    private static final int[] HARD = {72, 72, 72, 72};

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private static Game newGame(String difficulty) {
        return new Game(TestPlayers.of(new String[]{"A", "B", "C"}, new String[]{"Red", "Blue", "Green"}), difficulty);
    }

    // בדיקת זיהוי מהלך חוקי ולא חוקי
    private static void testMoveRules(String level, int[] lengths) {

        Game game = newGame(level);
        GameController controller = new GameController(game);

        // כל הכלים בטירה - אין כלי שניתן להזיז
        game.setStage(Stage.MOVE);
        game.setPendingSteps(4);
        game.setLastStepDie(4);

        check(!controller.hasAnyLegalMove(lengths),
                level + " castle only");

        // כלי בסוף מסלול הבית שאינו יכול להתקדם
        game.selectPiece(0);
        game.setPieceOnBoard(true);
        game.setPieceInHomePath(true);
        game.setPieceStep(5);

        check(!controller.hasAnyLegalMove(
                        new int[]{6, lengths[1], lengths[2], lengths[3]}),
                level + " blocked home");

        // תוצאה 0 עם כלי שיכול לנוע במסלול הבית
        game.setPieceStep(1);
        game.setPendingSteps(0);
        game.setLastStepDie(4);

        int[] homeLengths = {
                6, lengths[1], lengths[2], lengths[3]
        };

        check(controller.hasAnyLegalMove(homeLengths),
                level + " zero boost home");

        // תנועה רגילה במסלול המשותף
        game.setPieceInHomePath(false);
        game.setPieceStep(2);
        game.setPendingSteps(4);

        check(controller.hasAnyLegalMove(lengths),
                level + " normal shared move");

        // בדיקה שלא מקבלים נתוני מסלול חסרים
        check(!controller.hasAnyLegalMove(null),
                level + " invalid route data");

        System.out.println(
                "PASS - " + level + " legal move detection");
    }

    // בדיקת כניסה חוקית למסלול הבית
    private static void testHomeEntry() {

        Game game = newGame("Medium");
        GameController controller = new GameController(game);

        game.setStage(Stage.MOVE);
        game.setPieceOnBoard(true);
        game.setPieceInInnerPath(true);
        game.setPieceStep(50);
        game.setPendingSteps(3);

        check(controller.hasAnyLegalMove(
                        new int[]{52, 64, 64, 64}),
                "Legal crossing into home path");

        // מהמסלול החיצוני אי אפשר לקפוץ ישירות לבית
        game.setPieceStep(62);

        check(!controller.hasAnyLegalMove(MEDIUM),
                "Cannot jump from outer route directly to home");

        System.out.println(
                "PASS - entry into home path recognized");
    }

    // בדיקת דילוג על משתתפים שסיימו
    private static void testSkipping() {

        Game game = newGame("Hard");
        GameController controller = new GameController(game);

        // המשתתף השני כבר סיים
        game.setPlayerFinished(1, true);
        game.setConsecutiveSixes(2);

        controller.passTurn();

        check(game.getCurrentPlayer() == 2,
                "Skip finished player");

        check(game.getStage() == Stage.ROLL,
                "Next active player can roll");

        check(game.getConsecutiveSixes() == 0,
                "Reset sixes between players");

        // חזרה לשחקן הראשון
        controller.passTurn();

        check(game.getCurrentPlayer() == 0,
                "Wrap to first active player");

        // כשהשחקן הראשון הוא האחרון, אין עוד תורות
        game.setPlayerFinished(2, true);
        controller.passTurn();

        check(game.getStage() == Stage.DONE,
                "No turns when just one player remains");

        System.out.println(
                "PASS - finished players skipped and final turn closed");
    }

    public static void main(String[] args) {

        testMoveRules("Easy", EASY);
        testMoveRules("Medium", MEDIUM);
        testMoveRules("Hard", HARD);
        testHomeEntry();
        testSkipping();

        System.out.println(
                "PASS - SRS 3.2.3.3.6 controller tests");
    }
}