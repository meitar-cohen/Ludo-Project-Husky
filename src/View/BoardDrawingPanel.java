package View;

import javax.swing.JPanel;
import Controller.GameController;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Map;

/** Draws the board layout, castles, centre, and special squares. */
class BoardDrawingPanel extends JPanel {

    private static final Color BOARD_BG = new Color(243, 246, 255);
    private static final Color CELL_BORDER = new Color(201, 209, 228);

    BoardDrawingPanel() {
        setOpaque(false);
    }

    void drawBoard(Graphics2D g2, BoardPanel board) {
        int cs = board.cellSize();
        int ox = board.originX();
        int oy = board.originY();
        int rings = board.getRings();
        int size = board.getBoardSize();

        g2.setColor(new Color(31, 47, 95, 18));
        g2.fillRoundRect(ox - 7, oy - 5, cs * size + 14, cs * size + 14, 18, 18);
        g2.setColor(BOARD_BG);
        g2.fillRect(ox, oy, cs * size, cs * size);
        for (int ring = 0; ring < rings; ring++) {
            for (Point point : board.ringPath(ring)) {
                drawCell(g2, board, point, Color.WHITE);
            }
        }
        for (int c = 0; c < 15; c++) {
            for (int r = 0; r < 15; r++) {
                boolean vertical = c >= 6 && c <= 8 && (r < 6 || r > 8);
                boolean horizontal = r >= 6 && r <= 8 && (c < 6 || c > 8);
                if (vertical || horizontal) {
                    Point point = board.inner(c, r);
                    Color home = board.homeCellColor(point);
                    drawCell(g2, board, point,
                            home == null ? Color.WHITE : lighter(home));
                }
            }
        }
        for (int corner = 0; corner < 4; corner++) {
            drawCastle(g2, board, corner);
        }
        drawCentre(g2, board);
        for (Map.Entry<Point, GameController.SpecialSquareInfo> entry
                : board.squareEntries()) {
            drawSpecial(g2, board, entry.getKey(), entry.getValue());
        }
        g2.setColor(new Color(126, 140, 173));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRect(ox, oy, cs * size, cs * size);
    }

    private void drawCell(
            Graphics2D g2, BoardPanel board, Point point, Color fill) {
        int cs = board.cellSize();
        int x = board.originX() + point.x * cs;
        int y = board.originY() + point.y * cs;
        g2.setColor(fill);
        g2.fillRect(x, y, cs, cs);
        g2.setColor(CELL_BORDER);
        g2.setStroke(new BasicStroke(0.9f));
        g2.drawRect(x, y, cs, cs);
    }

    private void drawCastle(Graphics2D g2, BoardPanel board, int corner) {
        int cs = board.cellSize();
        Point origin = board.castleOrigin(corner);
        int x = board.originX() + origin.x * cs;
        int y = board.originY() + origin.y * cs;
        int side = cs * 6;
        Color color = BoardPanel.colorOf(
                BoardPanel.CORNER_COLOR_NAMES[corner]);
        Color base = board.isActiveCorner(corner)
                ? lighter(color) : lighter(lighter(color));
        Color glow = board.isActiveCorner(corner)
                ? new Color(246, 250, 255)
                : new Color(250, 251, 255);
        g2.setPaint(new GradientPaint(
                x, y, base, x + side, y + side,
                board.isActiveCorner(corner)
                        ? lighter(lighter(color))
                        : new Color(245, 248, 254)
        ));
        g2.fillRoundRect(
                x + 1, y + 1, side - 2, side - 2,
                Math.max(12, cs), Math.max(12, cs)
        );
        g2.setColor(new Color(
                color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2.setStroke(new BasicStroke(1.7f));
        g2.drawRoundRect(
                x + 1, y + 1, side - 2, side - 2,
                Math.max(12, cs), Math.max(12, cs)
        );
        g2.setColor(glow);
        g2.fillRoundRect(
                x + cs, y + cs, cs * 4, cs * 4,
                Math.max(12, cs), Math.max(12, cs)
        );
        g2.setColor(new Color(255, 255, 255, 210));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(
                x + cs, y + cs, cs * 4, cs * 4,
                Math.max(12, cs), Math.max(12, cs)
        );
        for (int slot = 0; slot < 4; slot++) {
            double[] centre = board.slotCenter(corner, slot);
            double radius = cs * 0.61;
            g2.setColor(new Color(
                    color.getRed(), color.getGreen(), color.getBlue(), 55));
            g2.fill(new Ellipse2D.Double(
                    centre[0] - radius, centre[1] - radius,
                    radius * 2, radius * 2
            ));
            g2.setColor(new Color(
                    color.getRed(), color.getGreen(), color.getBlue(), 85));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Ellipse2D.Double(
                    centre[0] - radius, centre[1] - radius,
                    radius * 2, radius * 2
            ));
        }
    }

    private void drawCentre(Graphics2D g2, BoardPanel board) {
        int cs = board.cellSize();
        double x0 = board.originX() + (6 + board.getRings()) * cs;
        double y0 = board.originY() + (6 + board.getRings()) * cs;
        double x1 = x0 + 3 * cs;
        double y1 = y0 + 3 * cs;
        double cx = (x0 + x1) / 2;
        double cy = (y0 + y1) / 2;
        fillTriangle(g2, BoardPanel.GREEN, x0, y0, x0, y1, cx, cy);
        fillTriangle(g2, BoardPanel.YELLOW, x0, y0, x1, y0, cx, cy);
        fillTriangle(g2, BoardPanel.BLUE, x1, y0, x1, y1, cx, cy);
        fillTriangle(g2, BoardPanel.RED, x0, y1, x1, y1, cx, cy);
        int diameter = Math.max(18, (int) (cs * 1.04));
        int x = (int) cx - diameter / 2;
        int y = (int) cy - diameter / 2;
        g2.setColor(new Color(22, 34, 77, 50));
        g2.fillOval(x + 2, y + 3, diameter, diameter);
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y, diameter, diameter);
        g2.setColor(new Color(35, 53, 105));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawOval(x, y, diameter, diameter);
        g2.setFont(new Font(
                "Segoe UI", Font.BOLD, Math.max(9, (int) (cs * 0.29))));
        FontMetrics metrics = g2.getFontMetrics();
        String goal = "GOAL";
        g2.drawString(
                goal,
                (int) cx - metrics.stringWidth(goal) / 2,
                (int) cy + (metrics.getAscent() - metrics.getDescent()) / 2
        );
    }

    private void fillTriangle(
            Graphics2D g2, Color color,
            double ax, double ay, double bx, double by,
            double cx, double cy) {
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(ax, ay);
        triangle.lineTo(bx, by);
        triangle.lineTo(cx, cy);
        triangle.closePath();
        g2.setColor(color);
        g2.fill(triangle);
        g2.setColor(Color.WHITE);
        g2.draw(triangle);
    }

    private void drawSpecial(
            Graphics2D g2, BoardPanel board, Point point,
            GameController.SpecialSquareInfo square) {
        int cs = board.cellSize();
        int x = board.originX() + point.x * cs;
        int y = board.originY() + point.y * cs;
        int pad = Math.max(2, (int) (cs * 0.09));
        int tile = cs - pad * 2;
        switch (square.getType()) {
            case START:
                drawCell(g2, board, point, lighter(lighter(square.getColor())));
                g2.setColor(square.getColor());
                g2.fillRoundRect(
                        x + pad, y + pad, tile, tile,
                        Math.max(6, cs / 4), Math.max(6, cs / 4)
                );
                drawCenteredText(g2, "#", x, y, cs, Color.WHITE, 0.68);
                break;
            case STAR:
                drawCell(g2, board, point, new Color(255, 241, 196));
                g2.setColor(new Color(255, 255, 255, 170));
                g2.fillOval(x + pad, y + pad, tile, tile);
                drawStar(g2, x + cs / 2.0, y + cs / 2.0, cs * 0.39);
                break;
            case ARROW:
                drawCell(g2, board, point, lighter(lighter(square.getColor())));
                g2.setColor(new Color(255, 255, 255, 150));
                g2.fillRoundRect(
                        x + pad, y + pad, tile, tile,
                        Math.max(6, cs / 4), Math.max(6, cs / 4)
                );
                drawArrow(
                        g2, x, y, cs, square.getDirection(),
                        square.getColor().darker()
                );
                break;
            case QUESTION:
                Color questionFill = square.getColor().equals(BoardPanel.Q_EASY)
                        ? new Color(48, 167, 111)
                        : square.getColor().equals(BoardPanel.Q_MEDIUM)
                                ? new Color(220, 157, 32)
                                : new Color(221, 69, 90);
                drawCell(g2, board, point, lighter(lighter(square.getColor())));
                g2.setColor(questionFill);
                g2.fillRoundRect(
                        x + pad, y + pad, tile, tile,
                        Math.max(6, cs / 4), Math.max(6, cs / 4)
                );
                drawCenteredText(g2, "?", x, y, cs, Color.WHITE, 0.72);
                break;
            case SURPRISE:
                drawCell(g2, board, point, new Color(234, 223, 255));
                g2.setColor(new Color(134, 76, 211));
                g2.fillRoundRect(
                        x + pad, y + pad, tile, tile,
                        Math.max(6, cs / 4), Math.max(6, cs / 4)
                );
                drawCenteredText(g2, "!", x, y, cs, Color.WHITE, 0.72);
                break;
            default:
                break;
        }
    }

    private void drawCenteredText(
            Graphics2D g2, String text, int x, int y,
            int cs, Color color, double scale) {
        int fontSize = Math.max(12, (int) (cs * scale));
        g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        FontMetrics metrics = g2.getFontMetrics();
        g2.setColor(color);
        g2.drawString(
                text,
                x + (cs - metrics.stringWidth(text)) / 2,
                y + (cs + metrics.getAscent() - metrics.getDescent()) / 2
        );
    }

    private void drawStar(Graphics2D g2, double cx, double cy, double radius) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double pointRadius = i % 2 == 0 ? radius : radius * 0.46;
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + Math.cos(angle) * pointRadius;
            double y = cy + Math.sin(angle) * pointRadius;
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();
        g2.setColor(new Color(251, 181, 37));
        g2.fill(star);
        g2.setColor(new Color(158, 107, 19));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(star);
    }

    private void drawArrow(
            Graphics2D g2, int x, int y, int cs, int direction, Color color) {
        Graphics2D arrowGraphics = (Graphics2D) g2.create();
        arrowGraphics.translate(x + cs / 2.0, y + cs / 2.0);
        arrowGraphics.rotate(direction * Math.PI / 2);
        double length = cs * 0.37;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(-length, -length * 0.27);
        arrow.lineTo(0, -length * 0.27);
        arrow.lineTo(0, -length * 0.69);
        arrow.lineTo(length, 0);
        arrow.lineTo(0, length * 0.69);
        arrow.lineTo(0, length * 0.27);
        arrow.lineTo(-length, length * 0.27);
        arrow.closePath();
        arrowGraphics.setColor(color);
        arrowGraphics.fill(arrow);
        arrowGraphics.dispose();
    }

    private static Color lighter(Color color) {
        return new Color(
                (color.getRed() + 255) / 2,
                (color.getGreen() + 255) / 2,
                (color.getBlue() + 255) / 2
        );
    }
}
