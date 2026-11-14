package Tests;

import Controller.GameController;

/**
 * בדיקת אימות פרטי שחקנים.
 */
public class PlayerValidationCheck {

    private static final String[] TWO_COLORS = {"Red", "Blue"};

    private static void expectError(String label, String[] names, String[] colors,
                                    int player) {
        String error = GameController.validatePlayers(names, colors);
        if (error == null) {
            throw new AssertionError(label + ": expected an error on player "
                    + (player + 1));
        }
        System.out.println("PASS - " + label + " -> " + error);
    }

    private static void expectValid(String label, String[] names, String[] colors) {
        String error = GameController.validatePlayers(names, colors);
        if (error != null) {
            throw new AssertionError(label + ": expected valid input, got " + error);
        }
        System.out.println("PASS - " + label);
    }

    public static void main(String[] args) {
        expectError("empty name", new String[] {"", "Avi"}, TWO_COLORS, 0);
        expectError("spaces only", new String[] {"Dana", "   "}, TWO_COLORS, 1);
        expectError("digits only", new String[] {"123", "Avi"}, TWO_COLORS, 0);
        expectError("symbols only", new String[] {"Dana", "!!"}, TWO_COLORS, 1);
        expectError("duplicate name (case-insensitive)", new String[] {"Dana", "dana"}, TWO_COLORS,
                1);
        expectError("duplicate color", new String[] {"Dana", "Avi"}, new String[] {"Red", "Red"},
                1);
        expectValid("letters with digits", new String[] {"Dana2", "Avi"}, TWO_COLORS);
        expectValid("Hebrew name", new String[] {"\u05d3\u05e0\u05d9", "Avi"}, TWO_COLORS);
        System.out.println("All player validation checks passed.");
    }
}
