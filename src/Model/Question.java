package Model;

import java.util.Arrays;

/** Immutable question data used by the question dialog. */
public class Question {
    private final String text;
    private final String[] options;
    private final int correctIndex;
    private final String difficulty;

    public Question(String text, String[] options, int correctIndex,
                    String difficulty) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Question text cannot be empty");
        }
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("Question options are required");
        }
        if (correctIndex < 0 || correctIndex >= options.length) {
            throw new IllegalArgumentException("Invalid correct answer index");
        }
        if (difficulty == null || difficulty.trim().isEmpty()) {
            throw new IllegalArgumentException("Question difficulty cannot be empty");
        }
        this.text = text.trim();
        this.options = Arrays.copyOf(options, options.length);
        for (String option : this.options) {
            if (option == null || option.trim().isEmpty()) {
                throw new IllegalArgumentException("Question options cannot be empty");
            }
        }
        this.correctIndex = correctIndex;
        this.difficulty = difficulty.trim();
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
