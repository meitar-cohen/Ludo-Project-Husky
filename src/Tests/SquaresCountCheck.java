package Tests;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Map;

import View.BoardPanel;

public class SquaresCountCheck {

    // קריאת שדה פרטי לצורך בדיקת קבלה בלבד
    private static Object readField(Object object, String fieldName)
            throws Exception {

        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        return field.get(object);
    }

    // השוואת הכמות בפועל לכמות הנדרשת
    private static void checkCount(
            String level, String type, int actual, int expected) {

        if (actual != expected) {
            throw new AssertionError(
                    level + " - " + type
                    + ": expected " + expected
                    + ", got " + actual);
        }
    }

    // בדיקת הכמויות בלוח בהתאם לרמת הקושי
    private static void testDifficulty(
            String level,
            int expectedStars,
            int expectedArrows,
            int expectedQuestionsPerLevel,
            int expectedSurprises) throws Exception {

        String[] colors = {
                "Green", "Yellow", "Blue", "Red"
        };

        // בודקים כמה לוחות שונים בכל רמת קושי
        for (int gameNumber = 0; gameNumber < 20; gameNumber++) {

            BoardPanel board = new BoardPanel(level, colors);

            // אוסף המשבצות המיוחדות של הלוח
            Map<?, ?> squares =
                    (Map<?, ?>) readField(board, "squares");

            int stars = 0;
            int arrows = 0;
            int starts = 0;
            int surprises = 0;

            int easyQuestions = 0;
            int mediumQuestions = 0;
            int hardQuestions = 0;

            // ספירת כל סוגי המשבצות
            for (Object special : squares.values()) {

                String type = TestPlayers.specialSquareType(special);

                switch (type) {

                    case "STAR":
                        stars++;
                        break;

                    case "ARROW":
                        arrows++;
                        break;

                    case "START":
                        starts++;
                        break;

                    case "SURPRISE":
                        surprises++;
                        break;

                    case "QUESTION":

                        Color color = TestPlayers.specialSquareColor(special);

                        if (BoardPanel.Q_EASY.equals(color)) {
                            easyQuestions++;

                        } else if (BoardPanel.Q_MEDIUM.equals(color)) {
                            mediumQuestions++;

                        } else if (BoardPanel.Q_HARD.equals(color)) {
                            hardQuestions++;

                        } else {
                            throw new AssertionError(
                                    level + ": unknown question level");
                        }
                        break;

                    default:
                        throw new AssertionError(
                                level + ": unknown square type " + type);
                }
            }

            // אימות הכמויות לפי דרישה 3.2.2.3.2
            checkCount(level, "Protection", stars, expectedStars);
            checkCount(level, "Arrows", arrows, expectedArrows);
            checkCount(level, "Start", starts, 4);

            checkCount(level, "Easy questions",
                    easyQuestions, expectedQuestionsPerLevel);

            checkCount(level, "Medium questions",
                    mediumQuestions, expectedQuestionsPerLevel);

            checkCount(level, "Hard questions",
                    hardQuestions, expectedQuestionsPerLevel);

            checkCount(level, "Surprises",
                    surprises, expectedSurprises);

            // בדיקת הכמות הכוללת
            int expectedTotal = expectedStars
                    + expectedArrows
                    + 4
                    + 3 * expectedQuestionsPerLevel
                    + expectedSurprises;

            checkCount(level, "Total special squares",
                    squares.size(), expectedTotal);
        }

        System.out.println(
                "PASS - " + level + " special squares counts");
    }

    public static void main(String[] args) throws Exception {

        // Easy: ארבע הגנות, ארבעה חצים וארבע התחלות
        testDifficulty("Easy", 4, 4, 0, 0);

        // Medium: שמונה הגנות, שש שאלות ושתי הפתעות
        testDifficulty("Medium", 8, 8, 2, 2);

        // Hard: שתים-עשרה הגנות, תשע שאלות ושלוש הפתעות
        testDifficulty("Hard", 12, 12, 3, 3);

        System.out.println(
                "PASS - SRS 3.2.2.3.2 special squares validation");
    }
}