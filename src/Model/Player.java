package Model;

import java.util.Arrays;

/**
 * מייצג משתתף במשחק.
 */
public class Player {
    public static final int PIECES_COUNT = 4;

    private String name;
    private String color;
    private final Piece[] pieces;

    public Player(String name, String color) {
        this.name = requireText(name, "Player name");
        this.color = requireText(color, "Player color");
        this.pieces = new Piece[PIECES_COUNT];
        for (int i = 0; i < PIECES_COUNT; i++) {
            pieces[i] = new Piece(i);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "Player name");
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

    public Piece[] getPieces() {
        return Arrays.copyOf(pieces, pieces.length);
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
