package tests;

import controller.GameSetupController;

/**
 * SRS 3.2.1.3.2 - player details validation.
 */
public class PlayerValidationCheck {

    private static final String[] TWO_COLORS = {"Red", "Blue"};

    private static void expectError(String label, String[] names, String[] colors,
                                    GameSetupController.Field field, int player) {
        GameSetupController.ValidationError e = GameSetupController.validatePlayers(names, colors);
        if (e == null || e.getField() != field || e.getPlayerIndex() != player) {
            throw new AssertionError(label + ": expected an error on player " + (player + 1)
                    + " (" + field + ")");
        }
        System.out.println("PASS - " + label + " -> " + e.getMessage());
    }

    private static void expectValid(String label, String[] names, String[] colors) {
        GameSetupController.ValidationError e = GameSetupController.validatePlayers(names, colors);
        if (e != null) {
            throw new AssertionError(label + ": expected valid input, got " + e.getMessage());
        }
        System.out.println("PASS - " + label);
    }

    public static void main(String[] args) {
        expectError("empty name", new String[] {"", "Avi"}, TWO_COLORS, GameSetupController.Field.NAME, 0);
        expectError("spaces only", new String[] {"Dana", "   "}, TWO_COLORS, GameSetupController.Field.NAME, 1);
        expectError("digits only", new String[] {"123", "Avi"}, TWO_COLORS, GameSetupController.Field.NAME, 0);
        expectError("symbols only", new String[] {"Dana", "!!"}, TWO_COLORS, GameSetupController.Field.NAME, 1);
        expectError("duplicate name (case-insensitive)", new String[] {"Dana", "dana"}, TWO_COLORS,
                GameSetupController.Field.NAME, 1);
        expectError("duplicate color", new String[] {"Dana", "Avi"}, new String[] {"Red", "Red"},
                GameSetupController.Field.COLOR, 1);
        expectValid("letters with digits", new String[] {"Dana2", "Avi"}, TWO_COLORS);
        expectValid("Hebrew name", new String[] {"\u05d3\u05e0\u05d9", "Avi"}, TWO_COLORS);
        System.out.println("All player validation checks passed.");
    }
}
