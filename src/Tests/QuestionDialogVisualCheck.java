
package Tests;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import Controller.GameController;
import View.QuestionDialog;

/**
 * בדיקה חזותית של חלון השאלות.
 * בודקת תשובות נכונות ושגויות בכל רמות הקושי.
 */
public final class QuestionDialogVisualCheck {

    private QuestionDialogVisualCheck() {
    }

    // בדיקת תנאי
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // חיפוש טקסט בתוויות הממשק
    private static boolean hasLabelText(
            Container container, String expected) {

        for (Component component : container.getComponents()) {

            if (component instanceof JLabel label
                    && label.getText() != null
                    && label.getText().contains(expected)) {
                return true;
            }

            if (component instanceof Container child
                    && hasLabelText(child, expected)) {
                return true;
            }
        }

        return false;
    }

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            // יצירת בקר משחק עם מאגר השאלות הקיים
            GameController controller =
                    GameController.createNewGame(
                            new String[]{"A", "B"},
                            new String[]{"Red", "Blue"},
                            "Medium");

            String[] levels = {"easy", "medium", "hard"};
            int testNumber = 0;

            // בדיקת תשובה נכונה ושגויה בכל רמה
            for (String level : levels) {

                for (boolean expectedCorrect :
                        new boolean[]{true, false}) {

                    testNumber++;

                    // בחירת שאלה מהמאגר לפי רמה
                    controller.selectQuestion(level);

                    int steps =
                            controller.getCurrentQuestionMoveSteps();

                    int correctIndex =
                            controller.getCorrectQuestionAnswerIndex();

                    // בחירת אפשרות נכונה או שגויה לבדיקה
                    int selectedIndex = expectedCorrect
                            ? correctIndex
                            : (correctIndex + 1) % 4;

                    char choice = (char) ('A' + selectedIndex);

                    String squareWord =
                            steps == 1 ? "square" : "squares";

                    // הצגת הוראות למבצעת הבדיקה
                    JOptionPane.showMessageDialog(
                            null,
                            "Test " + testNumber + "/6 - "
                                    + level.toUpperCase()
                                    + (expectedCorrect
                                            ? " (correct answer)"
                                            : " (wrong answer)")
                                    + "\nChoose option " + choice
                                    + " and click 'Check answer'."
                                    + "\nVerify the feedback and click 'Continue'.",
                            "Question dialog acceptance check",
                            JOptionPane.INFORMATION_MESSAGE);

                    // פתיחת חלון השאלה האמיתי
                    QuestionDialog dialog =
                            new QuestionDialog(null, controller);

                    // אימות הודעת חוק התנועה
                    String expectedRule =
                            "Correct: +" + steps + " " + squareWord
                            + "    |    Wrong: -" + steps
                            + " " + squareWord;

                    check(hasLabelText(
                            dialog.getContentPane(), expectedRule),
                            "Incorrect rule text for " + level);

                    // אימות הצגת רמת הקושי
                    check(hasLabelText(
                            dialog.getContentPane(),
                            level.toUpperCase()),
                            "Incorrect difficulty label for " + level);

                    dialog.setVisible(true);

                    // אימות תוצאת התשובה שנבחרה
                    check(dialog.isCorrect() == expectedCorrect,
                            "Unexpected answer result in test "
                                    + testNumber);

                    // אימות הודעת המשוב אחרי התשובה
                    String expectedFeedback = expectedCorrect
                            ? "Your piece moves "
                                    + steps + " " + squareWord
                                    + " forward."
                            : "Move " + steps + " "
                                    + squareWord + " back.";

                    check(hasLabelText(
                            dialog.getContentPane(), expectedFeedback),
                            "Incorrect feedback text in test "
                                    + testNumber);

                    System.out.println(
                            "PASS - " + level + ": "
                                    + (expectedCorrect
                                            ? "correct" : "wrong")
                                    + " answer, "
                                    + steps + " " + squareWord);
                }
            }

            System.out.println(
                    "All question dialog visual checks passed.");

            JOptionPane.showMessageDialog(
                    null,
                    "All 6 question dialog checks passed!",
                    "Test completed",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
