
package Model;

import java.awt.Color;
import java.util.Locale;

import Enums.SquareType;

/**
 * מחלקה המייצגת נתונים של משבצת מיוחדת בלוח.
 * ציור המשבצת נשאר באחריות שכבת View.
 */
public class Square {

    private final SquareType type;
    private final Color color;
    private final int dir;

    // רמת הקושי של השאלה, אם מדובר במשבצת שאלה
    private final String questionDifficulty;

    // הבנאי הקיים - נשמר לתאימות עם הקוד הקודם
    public Square(SquareType type, Color color, int direction) {
        this(type, color, direction, null);
    }

    // בנאי המאפשר להגדיר רמת קושי למשבצת שאלה
    public Square(SquareType type, Color color,
                  int direction, String questionDifficulty) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "Square type cannot be null");
        }

        // מניעת הגדרת רמת שאלה למשבצת מסוג אחר
        if (questionDifficulty != null
                && type != SquareType.QUESTION) {
            throw new IllegalArgumentException(
                    "Only question squares may have a question difficulty");
        }

        // אימות ונרמול רמת הקושי
        if (questionDifficulty != null) {

            String normalized = questionDifficulty.trim()
                    .toLowerCase(Locale.ROOT);

            if (!normalized.equals("easy")
                    && !normalized.equals("medium")
                    && !normalized.equals("hard")) {

                throw new IllegalArgumentException(
                        "Invalid question difficulty: "
                                + questionDifficulty);
            }

            this.questionDifficulty = normalized;

        } else {
            this.questionDifficulty = null;
        }

        this.type = type;
        this.color = color;
        this.dir = direction;
    }

    public SquareType getType() {
        return type;
    }

    public Color getColor() {
        return color;
    }

    public int getDirection() {
        return dir;
    }

    // החזרת רמת השאלה ללא תלות בצבע המשבצת
    public String getQuestionDifficulty() {
        return questionDifficulty;
    }
}
