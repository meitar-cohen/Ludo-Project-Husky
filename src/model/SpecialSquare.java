package Model;

import enums.SquareType;

import java.awt.Color;

/**
 * נתוני משבצת מיוחדת. ציור המשבצת נשאר באחריות ה־View.
 */
public class SpecialSquare {
    private final SquareType type;
    private final Color color;
    private final int dir;

    public SpecialSquare(SquareType type, Color color, int direction) {
        if (type == null) {
            throw new IllegalArgumentException("Square type cannot be null");
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
}
