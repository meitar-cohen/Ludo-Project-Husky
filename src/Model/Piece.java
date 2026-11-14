package Model;

import Enums.PieceState;

/**
 * מייצג כלי יחיד במשחק.
 */
public class Piece {
    private PieceState state;
    private int steps;
    private boolean inInnerPath;
    // SRS 3.2.4.3.2 - ברמה הקשה: הכלי נמצא במסלול האמצעי (64 משבצות)
    private boolean inMiddlePath;

    public Piece() {
        this.state = PieceState.CASTLE;
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
        if (inInnerPath) {
            this.inMiddlePath = false;
        }
    }

    public boolean isInMiddlePath() {
        return inMiddlePath;
    }

    public void setInMiddlePath(boolean inMiddlePath) {
        this.inMiddlePath = inMiddlePath;
        if (inMiddlePath) {
            this.inInnerPath = false;
        }
    }

    public void finish() {
        steps = 6;
        state = PieceState.FINISHED;
        inInnerPath = false;
        inMiddlePath = false;
    }
}
