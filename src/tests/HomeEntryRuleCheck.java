package tests;
import controller.GameController;
import enums.Stage;
import model.Game;

public class HomeEntryRuleCheck {

    public static void main(String[] args) {

        // הכלי עדיין לא השלים סיבוב
        check(false, "Easy", 48, 3, 52, false);

        // הכלי השלים סיבוב ונכנס למסלול הבית
        check(true, "Easy", 50, 3, 52, false);

        // הכלי חורג ממסלול הבית
        check(false, "Easy", 50, 8, 52, false);

        // ברמה בינונית לא ניתן להיכנס מהמסלול החיצוני
        check(false, "Medium", 50, 3, 52, false);

        // ברמה בינונית ניתן להיכנס לאחר הגעה למסלול הפנימי
        check(true, "Medium", 50, 3, 52, true);

        System.out.println("PASS - Home entry validation");
    }

    private static void check(
            boolean expected,
            String level,
            int start,
            int move,
            int routeSize,
            boolean alreadyInner) {

        Game game = new Game(
            new String[] {"A", "B"},
            new String[] {"Red", "Blue"},
            level
        );

        game.setStage(Stage.MOVE);
        game.setPieceOnBoard(true);
        game.setPieceStep(start);
        game.setPendingSteps(move);
        game.setPieceInInnerPath(alreadyInner);

        boolean success = new GameController(game)
                .tryEnterHomePath(0, routeSize);

        if (success != expected) {
            throw new AssertionError(level + ": unexpected result");
        }

        // בדיקה שמיקום הכלי במסלול הבית נשמר נכון
        if (success && (!game.isPieceInHomePath()
                || game.getPieceStep() != start + move - routeSize)) {
            throw new AssertionError("Home position incorrect");
        }
    }
}