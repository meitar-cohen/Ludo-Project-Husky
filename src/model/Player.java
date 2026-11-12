package model;

/**
 * Represents a participant in a game.
 */
public class Player {
    private String name;
    private String color;

    public Player(String name, String color) {
        this.name = requireText(name, "Player name");
        this.color = requireText(color, "Player color");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "Player name");
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = requireText(color, "Player color");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return value.trim();
    }
}
