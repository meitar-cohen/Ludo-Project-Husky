
package Tests;

import java.awt.Color;

import Enums.SquareType;
import Model.Square;

/**
 * בדיקות לשמירת רמת הקושי במשבצת שאלה.
 */
public class SquareQuestionDifficultyCheck {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // בדיקה שקלט לא תקין נדחה
    private static void expectInvalid(Runnable action) {
        try {
            action.run();
            throw new AssertionError(
                    "Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // הקלט נדחה כמצופה
        }
    }

    public static void main(String[] args) {

        // בדיקת שלוש רמות הקושי
        for (String difficulty :
                new String[]{"easy", "medium", "hard"}) {

            Square square = new Square(
                    SquareType.QUESTION,
                    Color.GREEN,
                    0,
                    difficulty
            );

            check(square.getType() == SquareType.QUESTION,
                    "Question square type changed");

            check(difficulty.equals(
                            square.getQuestionDifficulty()),
                    "Incorrect question difficulty");
        }

        System.out.println("PASS - Three difficulty levels");

        // בדיקה שרמת השאלה אינה תלויה בצבע
        Square mediumQuestion = new Square(
                SquareType.QUESTION,
                Color.RED,
                0,
                " MEDIUM "
        );

        check("medium".equals(
                        mediumQuestion.getQuestionDifficulty()),
                "Difficulty normalization failed");

        System.out.println(
                "PASS - Difficulty independent of color");

        // בדיקת תאימות לבנאי הישן
        Square regularSquare = new Square(
                SquareType.STAR, null, 0);

        check(regularSquare.getQuestionDifficulty() == null,
                "Regular square must not have question difficulty");

        Square legacyQuestion = new Square(
                SquareType.QUESTION, Color.GREEN, 0);

        check(legacyQuestion.getQuestionDifficulty() == null,
                "Legacy constructor is incompatible");

        System.out.println("PASS - Existing constructor compatibility");

        // בדיקת דחיית רמת קושי לא חוקית
        expectInvalid(() -> new Square(
                SquareType.QUESTION, Color.RED, 0, "expert"));

        // בדיקה שמשבצת אחרת אינה מקבלת רמת שאלה
        expectInvalid(() -> new Square(
                SquareType.STAR, null, 0, "easy"));

        System.out.println("PASS - Invalid data handling");

        System.out.println(
                "All square question difficulty checks passed.");
    }
}
