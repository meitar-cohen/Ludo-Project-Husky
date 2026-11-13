
package controller;

import java.util.HashSet;
import java.util.Set;
import Model.Player;

/**
 * בקר שלב הגדרת משחק חדש.
 */
public final class SetupController {

    private SetupController() {
    }

    /**
     * בדיקת שמות וצבעים של המשתתפים.
     * מחזירה את השגיאה הראשונה או null אם הקלט תקין.
     */
    public static String validatePlayers(String[] names, String[] colors) {
        if (names == null || colors == null || names.length != colors.length) {
            return "יש להזין שחקנים.";
        }
        Set<String> usedNames = new HashSet<>();
        Set<String> usedColors = new HashSet<>();

        for (int i = 0; i < names.length; i++) {
            // בדיקה שהשם אינו ריק
            String name = names[i] == null ? "" : names[i].trim();

            if (name.isEmpty()) {
                return "יש להזין שם לשחקן " + (i + 1) + ".";
            }

            if (!containsLetter(name)) {
                return "השם של שחקן " + (i + 1)
                        + " חייב להכיל לפחות אות אחת.";
            }

            if (!usedNames.add(name.toLowerCase())) {
                return "לכל שחקן חייב להיות שם שונה.";
            }

            if (colors[i] == null || !usedColors.add(colors[i])) {
                return "לכל שחקן חייב להיות צבע שונה.";
            }
        }

        return null;
    }

    /**
     * מחזיר את מספר המשתתף הראשון עם שגיאת שם, או -1 אם כל השמות תקינים.
     */
    public static int findInvalidNameIndex(String[] names) {
        if (names == null) {
            return 0;
        }
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            String name = names[i] == null ? "" : names[i].trim();
            if (name.isEmpty() || !containsLetter(name)
                    || !usedNames.add(name.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    /** בודק אם הטקסט כולל לפחות אות אחת. */
    static boolean containsLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static Player[] createPlayers(String[] names, String[] colors) {
        Player[] players = new Player[names.length];
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player(names[i], colors[i]);
        }
        return players;
    }

}
