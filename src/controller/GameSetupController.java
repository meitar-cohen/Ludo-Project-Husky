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
     * Checks the player details: no empty name, no duplicate name, no duplicate colour.
     *
     * @return the index of the first invalid player and an error message,
     *         or null when all details are valid
     */
    public static ValidationError validatePlayers(String[] names, String[] colors) {
        Set<String> usedNames = new HashSet<>();
        Set<String> usedColors = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            String name = names[i] == null ? "" : names[i].trim();
            if (name.isEmpty()) {
                return new ValidationError(i, "Please enter a name for Player " + (i + 1) + ".");
            }
            if (!usedNames.add(name.toLowerCase())) {
                return new ValidationError(i, "Each player must have a different name.");
            }
            if (colors[i] == null || !usedColors.add(colors[i])) {
                return new ValidationError(i, "Each player must choose a different color.");
            }
        }
        return null;
    }

    /** The result of a failed validation: which player's field is wrong and the message to show. */
    public static final class ValidationError {
        private final int playerIndex;
        private final String message;

        public ValidationError(int playerIndex, String message) {
            this.playerIndex = playerIndex;
            this.message = message;
        }

        public int getPlayerIndex() { return playerIndex; }

        public String getMessage() { return message; }
    }
}
