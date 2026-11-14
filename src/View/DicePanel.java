package View;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

    class DicePanel extends JPanel {
        private final boolean showBoost;
        private final String boostName;
        private int stepValue = 0;
        private int boostValue = 0;
        private Timer rollTimer;
        private int rollFrames;
        private final int boostFaces;

        DicePanel(boolean showBoost, String boostName, int boostFaces) {
            this.showBoost = showBoost;
            this.boostName = boostName;
            this.boostFaces = boostFaces;
            setOpaque(false);
            setPreferredSize(new Dimension(285, 77));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 77));
        }

        void show(int step, int boost) {
            stepValue = step;
            boostValue = boost;
            repaint();
        }

        void animate(int step, int boost, Runnable onFinished) {
            if (rollTimer != null && rollTimer.isRunning()) {
                rollTimer.stop();
            }
            rollFrames = 0;
            Random animationRandom = new Random();
            rollTimer = new Timer(65, null);
            rollTimer.addActionListener(e -> {
                rollFrames++;
                stepValue = animationRandom.nextInt(6) + 1;
                boostValue = showBoost ? animationRandom.nextInt(boostFaces) + 1 : 0;
                if (rollFrames >= 9) {
                    rollTimer.stop();
                    stepValue = step;
                    boostValue = boost;
                    repaint();
                    if (onFinished != null) onFinished.run();
                } else {
                    repaint();
                }
            });
            rollTimer.start();
        }

        void clear() {
            show(0, 0);
        }

        @Override

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawDie(g2, 8, 0, 58, stepValue, true);
            caption(g2, "STEP DIE", 6, 73, 70);
            if (showBoost) {
                drawDie(g2, 120, 0, 58, boostValue, false);
                caption(g2, boostName, 112, 73, 85);
            }
            g2.dispose();
        }

        private void caption(Graphics2D g2, String text, int x, int y, int w) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(GameScreen.MUTED);
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, x + (w - tw) / 2, y);
        }

        private void drawDie(Graphics2D g2, int x, int y, int s, int value, boolean pips) {
            g2.setColor(pips ? new Color(238, 242, 255) : new Color(255, 241, 218));
            g2.fillRoundRect(x, y, s, s, 16, 16);
            g2.setColor(pips ? new Color(85, 117, 235) : new Color(238, 164, 71));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRoundRect(x, y, s, s, 16, 16);
            if (value == 0) {
                g2.setFont(new Font("Arial", Font.BOLD, 30));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("?", x + (s - fm.stringWidth("?")) / 2, y + (s + fm.getAscent()) / 2 - 4);
                return;
            }
            if (!pips) {
                g2.setColor(new Color(200, 110, 0));
                g2.setFont(new Font("Arial", Font.BOLD, 36));
                FontMetrics fm = g2.getFontMetrics();
                String t = String.valueOf(value);
                g2.drawString(t, x + (s - fm.stringWidth(t)) / 2, y + (s + fm.getAscent()) / 2 - 4);
                return;
            }
            g2.setColor(new Color(56, 78, 163));
            int d = s / 6;
            int l = x + s / 4, m = x + s / 2, r = x + 3 * s / 4;
            int t = y + s / 4, c = y + s / 2, b = y + 3 * s / 4;
            int[][] spots;
            switch (value) {
                case 1: spots = new int[][]{{m, c}}; break;
                case 2: spots = new int[][]{{l, t}, {r, b}}; break;
                case 3: spots = new int[][]{{l, t}, {m, c}, {r, b}}; break;
                case 4: spots = new int[][]{{l, t}, {r, t}, {l, b}, {r, b}}; break;
                case 5: spots = new int[][]{{l, t}, {r, t}, {m, c}, {l, b}, {r, b}}; break;
                default: spots = new int[][]{{l, t}, {r, t}, {l, c}, {r, c}, {l, b}, {r, b}}; break;
            }
            for (int[] p : spots) {
                g2.fillOval(p[0] - d / 2, p[1] - d / 2, d, d);
            }
        }

}
