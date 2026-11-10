package controller;

import java.util.HashSet;
import java.util.Set;

/**
 * Controller of the new-game setup.
 * SRS 3.2.1.3.2 - validates the player details entered in the view (Daniel Azulay).
 */
public final class GameSetupController {

    private GameSetupController() {
    }

    /**
     * Checks the player details (SRS 3.2.1.3.2): every name must be filled in, contain at least
     * one letter and be different from the other names, and every colour must be different.
     *
     * @return the invalid field (player and field type) with an error message,
     *         or null when all details are valid
     */
    public static ValidationError validatePlayers(String[] names, String[] colors) {
        Set<String> usedNames = new HashSet<>();
        Set<String> usedColors = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            String player = "Player " + (i + 1);
            String name = names[i] == null ? "" : names[i].trim();
            if (name.isEmpty()) {
                return new ValidationError(i, Field.NAME, "Please enter a name for " + player + ".");
            }
            if (!containsLetter(name)) {
                return new ValidationError(i, Field.NAME, player + "'s name must contain at least one letter.");
            }
            if (!usedNames.add(name.toLowerCase())) {
                return new ValidationError(i, Field.NAME, player + "'s name is already used. Each player needs a different name.");
            }
            if (colors[i] == null || !usedColors.add(colors[i])) {
                return new ValidationError(i, Field.COLOR, player + "'s color is already taken. Each player needs a different color.");
            }
        }
        return null;
    }

    /** True when the text has at least one letter (in any language, e.g. English or Hebrew). */
    static boolean containsLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Which field of a player is invalid. */
    public enum Field { NAME, COLOR }

    /** The result of a failed validation: which player and which field are wrong, and the message to show. */
    public static final class ValidationError {
        private final int playerIndex;
        private final Field field;
        private final String message;

        public ValidationError(int playerIndex, Field field, String message) {
            this.playerIndex = playerIndex;
            this.field = field;
            this.message = message;
        }

        public int getPlayerIndex() { return playerIndex; }

        public Field getField() { return field; }

        public String getMessage() { return message; }
    }
}
