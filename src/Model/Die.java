package Model;

public class Die {
    private final int faces;
    private int value;

    public Die(int faces) {
        if (faces < 1) {
            throw new IllegalArgumentException("A die must have at least one face");
        }
        this.faces = faces;
    }

    public int getFaces() {
        return faces;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        if (value < 1 || value > faces) {
            throw new IllegalArgumentException("Die value must be within its face range");
        }
        this.value = value;
    }
}
