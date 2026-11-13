package Model;

import enums.PieceState;

/**
 * מייצג כלי יחיד במשחק.
 */
public class Piece {
    private final int index;
    private PieceState state;
    private int steps;
    private boolean inInnerPath;

    public Piece(int index) {
        this.index = index;
        this.state = PieceState.CASTLE;
    }

    public int getIndex() {
        return index;
    }

    public PieceState getState() {
        return state;
    }

    public void setState(PieceState state) {
        if (state == null) {
            throw new IllegalArgumentException("Piece state cannot be null");
        }
        this.state = state;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("Piece steps cannot be negative");
        }
        this.steps = steps;
    }

    public boolean isOnBoard() {
        return state == PieceState.BOARD || state == PieceState.HOME_PATH;
    }

    public boolean isInHomePath() {
        return state == PieceState.HOME_PATH
                || state == PieceState.FINISHED;
    }

    public boolean isFinished() {
        return state == PieceState.FINISHED;
    }

    public boolean isInInnerPath() {
        return inInnerPath;
    }

    public void setInInnerPath(boolean inInnerPath) {
        this.inInnerPath = inInnerPath;
    }

    public void moveTo(int steps) {
        setSteps(steps);
        state = PieceState.BOARD;
    }

    public void enterHomePath(int steps) {
        setSteps(steps);
        state = PieceState.HOME_PATH;
    }

    public void finish() {
        steps = 6;
        state = PieceState.FINISHED;
        inInnerPath = false;
    }
}
