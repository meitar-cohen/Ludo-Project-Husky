
package controller;

import java.util.HashSet;
import java.util.Set;
import model.Player;

/**
 * Controller of the new-game setup.
 * SRS 3.2.1.3.2 - validates the player details entered in the view.
 */
public final class GameSetupController {

    private GameSetupController() {
    }

    // סוג השדה שבו נמצאה שגיאת אימות
    public enum Field {
        NAME,
        COLOR
    }

    /**
     * בדיקת שמות וצבעים של המשתתפים.
     * מחזירה את השגיאה הראשונה או null אם הקלט תקין.
     */
    public static ValidationError validatePlayers(
            String[] names, String[] colors) {
        if (names == null || colors == null || names.length != colors.length) {
            return new ValidationError(0, Field.NAME, "Players are required.");
        }
        Set<String> usedNames = new HashSet<>();
        Set<String> usedColors = new HashSet<>();

        for (int i = 0; i < names.length; i++) {
            // בדיקה שהשם אינו ריק
            String name = names[i] == null ? "" : names[i].trim();

            if (name.isEmpty()) {
                return new ValidationError(
                    i,
                    Field.NAME,
                    "Please enter a name for Player " + (i + 1) + "."
                );
            }

            // בדיקה שאין שם כפול
            if (!usedNames.add(name.toLowerCase())) {
                return new ValidationError(
                    i,
                    Field.NAME,
                    "Each player must have a different name."
                );
            }

            // בדיקה שאין צבע כפול או צבע שלא נבחר
            if (colors[i] == null || !usedColors.add(colors[i])) {
                return new ValidationError(
                    i,
                    Field.COLOR,
                    "Each player must choose a different color."
                );
            }
        }

        // כל פרטי המשתתפים תקינים
        return null;
    }

    public static Player[] createPlayers(String[] names, String[] colors) {
        Player[] players = new Player[names.length];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(names[i], colors[i]);
        }
        return players;
    }

    /**
     * מייצג שגיאה באימות פרטי משתתף.
     */
    public static final class ValidationError {

        private final int playerIndex;
        private final Field field;
        private final String message;

        public ValidationError(
                int playerIndex, Field field, String message) {

            this.playerIndex = playerIndex;
            this.field = field;
            this.message = message;
        }

        // מספר המשתתף שבו נמצאה השגיאה
        public int getPlayerIndex() {
            return playerIndex;
        }

        // סוג השדה שבו נמצאה השגיאה
        public Field getField() {
            return field;
        }

        // הודעת השגיאה להצגה למשתמש
        public String getMessage() {
            return message;
        }
    }
}
