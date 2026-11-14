package Tests;

import java.awt.image.BufferedImage;
import View.BoardPanel;

public class BoardVisibilityCheck {

    public static void main(String[] args) {

        // בדיקת נראות הכלים בכל רמות הקושי
        check("Easy");
        check("Medium");
        check("Hard");
    }

    private static void check(String level) {

        String[] colors = {"Blue", "Red"};

        // לוח ללא כלי כחול במסלול - להשוואה
        BoardPanel baselineBoard = new BoardPanel(level, colors);
        baselineBoard.setActivePlayerColor("Red");

        // לוח עם כלי כחול, לאחר שהתור עבר לאדום
        BoardPanel testBoard = new BoardPanel(level, colors);

        if (!testBoard.placeActivePieceOnStart(0)) {
            throw new AssertionError(
                "Could not place Blue piece: " + level
            );
        }

        testBoard.setActivePlayerColor("Red");

        BufferedImage baseline = render(baselineBoard);
        BufferedImage actual = render(testBoard);

        int boardSize = "Easy".equals(level) ? 15 :
                        "Medium".equals(level) ? 17 : 19;

        int cellSize = 700 / boardSize;
        int origin = (700 - boardSize * cellSize) / 2;

        // משבצת ההתחלה של השחקן הכחול
        int startX = "Easy".equals(level) ? 13 : boardSize - 1;
        int startY = "Easy".equals(level) ? 8 : boardSize - 1;

        int cx = origin + (int) ((startX + 0.5) * cellSize);
        int cy = origin + (int) ((startY + 0.5) * cellSize);

        // בדיקת הבדל חזותי באזור שבו הכלי הכחול אמור להופיע
        int changedPixels = 0;

        for (int y = cy - 8; y <= cy + 8; y++) {
            for (int x = cx - 8; x <= cx + 8; x++) {

                if (baseline.getRGB(x, y) != actual.getRGB(x, y)) {
                    changedPixels++;
                }
            }
        }

        if (changedPixels < 15) {
            throw new AssertionError(
                level + ": Blue piece disappeared after turn change"
            );
        }

        System.out.println(
            "PASS - " + level +
            ": Blue piece remains visible during Red's turn"
        );
    }

    // ציור הלוח לתמונה לצורך בדיקה אוטומטית
    private static BufferedImage render(BoardPanel board) {

        board.setSize(700, 700);

        BufferedImage image = new BufferedImage(
            700, 700, BufferedImage.TYPE_INT_ARGB
        );

        java.awt.Graphics2D graphics = image.createGraphics();
        board.paint(graphics);
        graphics.dispose();

        return image;
    }
}
