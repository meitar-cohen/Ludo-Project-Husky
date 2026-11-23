
package Tests;

import Controller.GameController;
import Controller.MovementController;
import Model.Game;
import Model.Player;

/**
 * בדיקות לחישוב תנועה בעקבות מענה על שאלה.
 */
public class QuestionMoveTargetCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // בדיקת דחיית מספר צעדים לא חוקי
    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    public static void main(String[] args) {

        // יצירת משחק ושני שחקנים לצורך הבדיקה
        Player[] players = {
                new Player("A", "Red"),
                new Player("B", "Blue")
        };

        Game game = new Game(players, "Medium");
        MovementController movement = new MovementController(game);
        GameController controller = new GameController(game, movement);

        game.selectPiece(0);
        game.getSelectedPiece().setOnBoard(true);

        // בדיקת תנועה בשלוש רמות השאלה
        String[] levels = {"easy", "medium", "hard"};

        for (int i = 0; i < levels.length; i++) {

            String level = levels[i];
            controller.selectQuestion(level);

            int steps = controller.getCurrentQuestionMoveSteps();

            check(steps == i + 1,
                    "Wrong step count for " + level);

            // מיקום התחלתי לבדיקה
            game.getSelectedPiece().setSteps(10);

            // תשובה נכונה - תנועה קדימה
            check(movement.getAnswerMoveTarget(
                            true, steps, 52) == 10 + steps,
                    "Incorrect forward movement for " + level);

            // תשובה שגויה - תנועה אחורה
            check(movement.getAnswerMoveTarget(
                            false, steps, 52) == 10 - steps,
                    "Incorrect backward movement for " + level);
        }

        System.out.println(
                "PASS - Easy/Medium/Hard movement: 1/2/3 steps");

        // תנועה שגויה נעצרת בתחילת המסלול ולא מחזירה את הכלי לטירה
        game.getSelectedPiece().setSteps(0);
        check(movement.getAnswerMoveTarget(false, 3, 52) == 0,
                "Backward movement from the start must stay at the start");

        game.getSelectedPiece().setSteps(1);
        check(movement.getAnswerMoveTarget(false, 3, 52) == 0,
                "Backward movement must stop at the route start");

        game.getSelectedPiece().setSteps(2);
        check(movement.getAnswerMoveTarget(false, 3, 52) == 0,
                "Backward movement must stop at the route start");

        // בדיקת חריגה מסוף המסלול
        game.getSelectedPiece().setSteps(50);

        check(movement.getAnswerMoveTarget(true, 3, 52) == 50,
                "Movement beyond route end must keep current step");

        System.out.println("PASS - Route boundaries");

        // בדיקת תאימות למתודה הישנה
        game.getSelectedPiece().setSteps(10);

        check(movement.getAnswerMoveTarget(true, 52) == 11,
                "Legacy forward movement changed");

        check(movement.getAnswerMoveTarget(false, 52) == 9,
                "Legacy backward movement changed");

        System.out.println("PASS - Existing method compatibility");

        // בדיקת דחיית מרחקי תנועה לא חוקיים
        expectInvalid(() ->
                movement.getAnswerMoveTarget(true, 0, 52));

        expectInvalid(() ->
                movement.getAnswerMoveTarget(false, 4, 52));

        System.out.println(
                "PASS - Invalid movement distances rejected");

        System.out.println(
                "All question movement target checks passed.");
    }
}
