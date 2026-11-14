package Model;

/**
 * מייצג משתתף במשחק.
 */
public class Player {
    public static final int PIECES_COUNT = 4;

    private final String name;
    private String color;
    private final Piece[] pieces;

    public Player(String name, String color) {
        this.name = requireText(name, "Player name");
        this.color = requireText(color, "Player color");
        this.pieces = new Piece[PIECES_COUNT];
        for (int i = 0; i < PIECES_COUNT; i++) {
            pieces[i] = new Piece();
        }
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = requireText(color, "Player color");
    }

    public Piece getPiece(int index) {
        if (index < 0 || index >= pieces.length) {
            throw new IllegalArgumentException("Invalid piece index");
        }
        return pieces[index];
    }

    public boolean hasPieceOnBoard() {
        for (Piece piece : pieces) {
            if (piece.isOnBoard()) {
                return true;
            }
        }
        return false;
    }

    public int getFinishedPieceCount() {
        int count = 0;
        for (Piece piece : pieces) {
            if (piece.isFinished()) {
                count++;
            }
        }
        return count;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return value.trim();
    }
}
