package Model;

import java.util.Arrays;

/**
 * שאלה ותשובות למסך ניהול השאלות.
 */
public class Question {
    private final String text;
    private final String[] options;
    private final int correctIndex;
    private final String difficulty;

    public Question(String text, String[] options, int correctIndex,
                    String difficulty) {
        this.text = text;
        this.options = Arrays.copyOf(options, options.length);
        this.correctIndex = correctIndex;
        this.difficulty = difficulty;
    }

    public String getText() {
        return text;
    }

    public String[] getOptions() {
        return Arrays.copyOf(options, options.length);
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getDifficulty() {
        return difficulty;
    }
}
