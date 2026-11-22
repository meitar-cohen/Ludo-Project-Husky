
package Tests;

import java.awt.Color;
import java.awt.Point;
import java.util.HashSet;

import Controller.GameController;
import Enums.SquareType;

/**
 * בדיקת שיוך רמות קושי למשבצות שאלה בלוח.
 */
public class GameQuestionSquarePlacementCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // בדיקת מיקום משבצות השאלה ברמת משחק נתונה
    private static void checkBoard(
            String gameDifficulty, int expectedPerLevel) {

        GameController controller = GameController.createNewGame(
                new String[]{"A", "B"},
                new String[]{"Red", "Blue"},
                gameDifficulty
        );

        // יצירת המשבצות המיוחדות בלוח
        controller.randomizeSpecialSquares(
                controller.getBoardSharedRoutes(),
                controller.getBoardRings(),
                new HashSet<Point>(),
                new Color[]{Color.GREEN, Color.YELLOW, Color.RED},
                Color.MAGENTA
        );

        int easy = 0;
        int medium = 0;
        int hard = 0;

        // בדיקת רמת הקושי של כל משבצת שאלה
        for (Point position : controller.getSpecialSquarePositions()) {

            SquareType type = controller.getSpecialSquareType(position);
            String difficulty =
                    controller.getSpecialSquareQuestionDifficulty(position);

            if (type == SquareType.QUESTION) {

                check(difficulty != null,
                        "Question square has no difficulty");

                switch (difficulty) {
                    case "easy" -> easy++;
                    case "medium" -> medium++;
                    case "hard" -> hard++;
                    default -> throw new AssertionError(
                            "Invalid difficulty: " + difficulty);
                }

            } else {
                check(difficulty == null,
                        "Non-question square has a question difficulty");
            }
        }

        // אימות כמות המשבצות בכל רמה
        check(easy == expectedPerLevel, "Incorrect easy count");
        check(medium == expectedPerLevel, "Incorrect medium count");
        check(hard == expectedPerLevel, "Incorrect hard count");

        System.out.println("PASS - " + gameDifficulty
                + ": " + easy + " easy, "
                + medium + " medium, "
                + hard + " hard");
    }

    public static void main(String[] args) {

        // משחק קל אינו מכיל משבצות שאלה
        checkBoard("Easy", 0);

        // משחק בינוני מכיל שתי משבצות מכל רמה
        checkBoard("Medium", 2);

        // משחק קשה מכיל שלוש משבצות מכל רמה
        checkBoard("Hard", 3);

        System.out.println(
                "All question square placement checks passed.");
    }
}
