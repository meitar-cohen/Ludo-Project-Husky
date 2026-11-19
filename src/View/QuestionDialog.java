package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import Controller.GameController;


public class QuestionDialog extends JDialog {

    static final Color NAVY = new Color(18, 27, 61);
    static final Color PRIMARY = new Color(75, 110, 248);
    static final Color MUTED = new Color(111, 125, 153);
    static final Color SUCCESS = new Color(27, 148, 107);
    static final Color ERROR = new Color(207, 70, 87);

    private int selected = -1;
    private boolean correct = false;
    private final AnswerButton[] answerButtons;

    public QuestionDialog(Window owner, GameController gameController) {
        super(owner, "Ludo - Question Challenge", ModalityType.APPLICATION_MODAL);
        MainMenuScreen.applyAppIcon(this);
		String[] options = gameController.getQuestionOptions();
		// קבלת מספר צעדי התנועה לפי רמת השאלה מה-Controller
		int questionSteps = gameController.getCurrentQuestionMoveSteps();
		// התאמת הטקסט ליחיד או לרבים
		String squareLabel = questionSteps == 1 ? "square" : "squares";
		answerButtons = new AnswerButton[options.length];
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        JPanel root = new QuestionBackgroundPanel();
        root.setLayout(new GridBagLayout());
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        
        QuestionCardPanel card = new QuestionCardPanel(Color.WHITE, 27);
        card.setLayout(new BorderLayout(0, 0));
        
        card.setBorder(new EmptyBorder(20, 31, 16, 31));
        ScrollableColumn content = new ScrollableColumn();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel topLine = new JPanel(new BorderLayout());
        topLine.setOpaque(false);
        topLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tag = new JLabel("LUDO   /   QUESTION SQUARE");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tag.setForeground(PRIMARY);
        topLine.add(tag, BorderLayout.WEST);
        JLabel level = new JLabel(
                "  " + gameController.getQuestionDifficulty().toUpperCase(Locale.ROOT) + "  ");
        level.setOpaque(true);
        level.setBackground(new Color(225, 247, 235));
        level.setForeground(SUCCESS);
        level.setFont(new Font("Segoe UI", Font.BOLD, 12));
        level.setBorder(new EmptyBorder(4, 8, 4, 8));
        topLine.add(level, BorderLayout.EAST);
        content.add(topLine);
        content.add(Box.createVerticalStrut(9));
        JLabel title = new JLabel("Question time!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 31));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(5));
        JLabel subtitle = new JLabel("Choose carefully - your next move depends on it.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(14));
        QuestionCardPanel questionCard = new QuestionCardPanel(new Color(241, 245, 255), 17);
        questionCard.setLayout(new BorderLayout());
        questionCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        JTextArea prompt = new JTextArea(gameController.getQuestionText());
        prompt.setEditable(false);
        prompt.setFocusable(false);
        prompt.setLineWrap(true);
        prompt.setWrapStyleWord(true);
        prompt.setOpaque(false);
        prompt.setBorder(null);
        prompt.setFont(new Font("Segoe UI", Font.BOLD, 17));

		prompt.setForeground(NAVY);
		
		// התאמת גובה תיבת השאלה למספר שורות הטקסט
		prompt.setSize(new Dimension(530, 10000));
		int questionHeight = Math.max(
		        87, prompt.getPreferredSize().height + 28);
		
		questionCard.setPreferredSize(
		        new Dimension(582, questionHeight));
		questionCard.setMaximumSize(
		        new Dimension(Integer.MAX_VALUE, questionHeight));
		
		questionCard.add(prompt, BorderLayout.CENTER);

        questionCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(questionCard);
        content.add(Box.createVerticalStrut(14));
        JLabel choose = new JLabel("SELECT ONE ANSWER");
        choose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        choose.setForeground(MUTED);
        choose.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(choose);
        content.add(Box.createVerticalStrut(9));
        ActionButton confirm = new ActionButton("Check answer", PRIMARY);
        confirm.setEnabled(false);
        ActionButton cont = new ActionButton("Continue", SUCCESS);
        cont.setVisible(false);
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            AnswerButton button = new AnswerButton(i, options[i]);
            answerButtons[i] = button;
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener(e -> {
                selected = idx;
                for (int j = 0; j < answerButtons.length; j++) {
                    answerButtons[j].setVisualState(j == selected ? 1 : 0);
                }
                confirm.setEnabled(true);
            });
            content.add(button);
            if (i != options.length - 1) content.add(Box.createVerticalStrut(7));
        }
        content.add(Box.createVerticalStrut(11));
        QuestionCardPanel feedbackPanel = new QuestionCardPanel(new Color(241, 245, 255), 15);
        feedbackPanel.setLayout(new BorderLayout());
        feedbackPanel.setBorder(new EmptyBorder(11, 14, 11, 14));
        feedbackPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        feedbackPanel.setPreferredSize(new Dimension(582, 56));
        feedbackPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel feedback = new JLabel("Select an answer to check your next move.");
        feedback.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        feedback.setForeground(new Color(67, 83, 126));
        feedbackPanel.add(feedback, BorderLayout.CENTER);


		
		content.add(feedbackPanel);
		
		// התאמת התוכן לרוחב החלון ללא פסי גלילה גלויים
		JScrollPane questionScroll = new JScrollPane(
		        content,
		        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
		        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		questionScroll.setBorder(null);
		questionScroll.setOpaque(false);
		questionScroll.getViewport().setOpaque(false);
		
		// הסתרת פס הגלילה תוך שמירת גלילה בגלגלת העכבר
		questionScroll.getVerticalScrollBar().setPreferredSize(
		        new Dimension(0, 0));
		questionScroll.getVerticalScrollBar().setUnitIncrement(18);
		
		card.add(questionScroll, BorderLayout.CENTER);
		
		JPanel footer = new JPanel(new BorderLayout(8, 0));



        footer.setOpaque(false);
		JLabel rule = new JLabel(
		        "Correct: +" + questionSteps + " " + squareLabel
		        + "    |    Wrong: -" + questionSteps + " " + squareLabel);
        rule.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rule.setForeground(MUTED);
        footer.add(rule, BorderLayout.WEST);
        JPanel actions = new JPanel(new CardLayout());
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(172, 47));
        actions.add(confirm, "CHECK");
        actions.add(cont, "CONTINUE");
        footer.add(actions, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        confirm.addActionListener(e -> {
            if (selected < 0) return;
            correct = gameController.isCorrectQuestionAnswer(selected);
            for (int i = 0; i < answerButtons.length; i++) {
                int state = i == gameController.getCorrectQuestionAnswerIndex()
                        ? 2 : (i == selected ? 3 : 4);
                answerButtons[i].setVisualState(state);
                answerButtons[i].setEnabled(false);
            }
            
		if (correct) {
		    // הודעה על תשובה נכונה בהתאם לרמת השאלה
		    feedback.setText("Correct! Your piece moves "
		            + questionSteps + " " + squareLabel + " forward.");
		    feedback.setForeground(SUCCESS);
		    feedbackPanel.setFill(new Color(229, 249, 239));
		} else {
		    // הודעה על תשובה שגויה והצגת התשובה הנכונה
		    char correctAnswer = (char) (
		            'A' + gameController.getCorrectQuestionAnswerIndex());
		
		    feedback.setText("Not quite. The correct answer is "
		            + correctAnswer + ". Move "
		            + questionSteps + " " + squareLabel + " back.");
		    feedback.setForeground(ERROR);
		    feedbackPanel.setFill(new Color(255, 237, 240));
		}

            ((CardLayout) actions.getLayout()).show(actions, "CONTINUE");
            getRootPane().setDefaultButton(cont);
        });
        cont.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(confirm);


	     // חישוב הגובה הדרוש לתוכן החלון
	     int contentHeight = content.getPreferredSize().height;
	     int footerHeight = footer.getPreferredSize().height;
	
	     int desiredCardHeight =
	             contentHeight + footerHeight + 20 + 16 + 12;
	
	     // בדיקת גובה המסך הזמין
	     Rectangle usableScreen = GraphicsEnvironment
	             .getLocalGraphicsEnvironment()
	             .getMaximumWindowBounds();
	
	     // הגבלת גובה החלון כדי שלא יחרוג מהמסך
	     int maxCardHeight = Math.max(
	             380, usableScreen.height - 80);
	
	     // התאמת הגובה לפי אורך התוכן וגודל המסך
	     int cardHeight = Math.min(
	             desiredCardHeight, maxCardHeight);
	
	     card.setPreferredSize(
	             new Dimension(650, cardHeight));
	
	     root.setPreferredSize(
	             new Dimension(710, cardHeight + 32));
	
	     root.add(card);
	     setContentPane(root);
	     pack();
	     setLocationRelativeTo(owner);


    }

    public boolean isCorrect() {
        return correct;
    }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static class ActionButton extends JButton {
        private final Color fill;
        ActionButton(String label, Color fill) {
            super(label);
            this.fill = fill;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            Color base = isEnabled() ? fill : new Color(183, 194, 217);
            if (getModel().isRollover() && isEnabled()) base = base.brighter();
            if (getModel().isPressed() && isEnabled()) base = base.darker();
            g.setColor(base);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

	
	static class AnswerButton extends JButton {
	
	    private final int index;
	    private final String answer;
	
	    // מצבי הכפתור: רגיל, נבחר, נכון, שגוי ומנוטרל
	    private int visualState;
	
	    private static final int TEXT_WIDTH = 440;
	    private static final Font ANSWER_FONT =
	            new Font("Segoe UI", Font.PLAIN, 14);
	
	    AnswerButton(int index, String answer) {
	        super();
	
	        this.index = index;
	        this.answer = answer;
	
	        setContentAreaFilled(false);
	        setOpaque(false);
	        setBorderPainted(false);
	        setFocusPainted(false);
	        setCursor(new Cursor(Cursor.HAND_CURSOR));
	
	        // חישוב מספר השורות הנדרש להצגת התשובה
	        FontMetrics metrics = getFontMetrics(ANSWER_FONT);
	        int lines = wrapAnswer(metrics, TEXT_WIDTH).size();
	
	        // התאמת גובה הכפתור למספר שורות הטקסט
	        int buttonHeight = Math.max(
	                57, 22 + lines * metrics.getHeight());
	
	        setPreferredSize(new Dimension(582, buttonHeight));
	        setMaximumSize(
	                new Dimension(Integer.MAX_VALUE, buttonHeight));
	
	        getAccessibleContext().setAccessibleName(
	                "Answer " + (char) ('A' + index) + ": " + answer);
	    }
	
	    void setVisualState(int state) {
	        visualState = state;
	        repaint();
	    }
	
	    // חלוקת תשובה ארוכה למספר שורות ללא קיצור הטקסט
	    private java.util.List<String> wrapAnswer(
	            FontMetrics metrics, int width) {
	
	        java.util.List<String> lines =
	                new java.util.ArrayList<>();
	
	        StringBuilder current = new StringBuilder();
	
	        for (String word : answer.trim().split("\\s+")) {
	
	            // טיפול גם במילה ארוכה במיוחד
	            if (metrics.stringWidth(word) > width) {
	
	                if (current.length() > 0) {
	                    lines.add(current.toString());
	                    current.setLength(0);
	                }
	
	                while (metrics.stringWidth(word) > width) {
	                    int cut = 1;
	
	                    while (cut < word.length()
	                            && metrics.stringWidth(
	                                    word.substring(0, cut + 1))
	                                    <= width) {
	                        cut++;
	                    }
	
	                    lines.add(word.substring(0, cut));
	                    word = word.substring(cut);
	                }
	
	                if (!word.isEmpty()) {
	                    current.append(word);
	                }
	
	            } else if (current.length() == 0) {
	                current.append(word);
	
	            } else if (metrics.stringWidth(
	                    current + " " + word) <= width) {
	
	                current.append(' ').append(word);
	
	            } else {
	                lines.add(current.toString());
	                current.setLength(0);
	                current.append(word);
	            }
	        }
	
	        if (current.length() > 0) {
	            lines.add(current.toString());
	        }
	
	        if (lines.isEmpty()) {
	            lines.add("");
	        }
	
	        return lines;
	    }
	
	    @Override
	    protected void paintComponent(Graphics graphics) {
	
	        Graphics2D g = (Graphics2D) graphics.create();
	        quality(g);
	
	        Color bg = Color.WHITE;
	        Color border = new Color(220, 227, 242);
	        Color letterBg = new Color(235, 241, 252);
	        Color letterFg = new Color(85, 106, 158);
	        Color text = NAVY;
	
	        // שמירת הצבעים המקוריים של מצבי התשובה
	        if (visualState == 1) {
	            bg = new Color(239, 243, 255);
	            border = PRIMARY;
	            letterBg = PRIMARY;
	            letterFg = Color.WHITE;
	
	        } else if (visualState == 2) {
	            bg = new Color(235, 251, 242);
	            border = SUCCESS;
	            letterBg = SUCCESS;
	            letterFg = Color.WHITE;
	
	        } else if (visualState == 3) {
	            bg = new Color(255, 239, 242);
	            border = QuestionDialog.ERROR;
	            letterBg = QuestionDialog.ERROR;
	            letterFg = Color.WHITE;
	
	        } else if (visualState == 4) {
	            bg = new Color(249, 250, 253);
	            border = new Color(233, 238, 247);
	            text = new Color(157, 167, 187);
	
	        } else if (getModel().isRollover()) {
	            bg = new Color(247, 249, 255);
	            border = new Color(151, 171, 248);
	        }
	
	        // ציור הרקע והמסגרת המקוריים
	        g.setColor(bg);
	        g.fillRoundRect(
	                1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
	
	        g.setColor(border);
	        g.setStroke(new BasicStroke(
	                visualState == 1 ? 2f : 1.1f));
	
	        g.drawRoundRect(
	                1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
	
	        // ציור ריבוע האות A/B/C/D
	        int letterY = (getHeight() - 35) / 2;
	
	        g.setColor(letterBg);
	        g.fillRoundRect(13, letterY, 35, 35, 11, 11);
	
	        g.setFont(new Font("Segoe UI", Font.BOLD, 16));
	        g.setColor(letterFg);
	
	        FontMetrics letterMetrics = g.getFontMetrics();
	        String letter = String.valueOf((char) ('A' + index));
	
	        g.drawString(
	                letter,
	                13 + (35 - letterMetrics.stringWidth(letter)) / 2,
	                letterY + (35 + letterMetrics.getAscent()
	                        - letterMetrics.getDescent()) / 2);
	
	        // הצגת התשובה במלואה על פני מספר שורות
	        g.setFont(ANSWER_FONT);
	        g.setColor(text);
	
	        FontMetrics metrics = g.getFontMetrics();
	
	        java.util.List<String> lines = wrapAnswer(
	                metrics,
	                Math.max(1,
	                        Math.min(TEXT_WIDTH, getWidth() - 86)));
	
	        int lineHeight = metrics.getHeight();
	        int baseline = (getHeight()
	                - lines.size() * lineHeight) / 2
	                + metrics.getAscent();
	
	        for (String line : lines) {
	            g.drawString(line, 64, baseline);
	            baseline += lineHeight;
	        }
	
	        g.dispose();
	    }
	}

    

 // התאמת רוחב התוכן לחלון ומניעת גלילה אופקית
 private static class ScrollableColumn extends JPanel
         implements Scrollable {

     @Override
     public Dimension getPreferredScrollableViewportSize() {
         return getPreferredSize();
     }

     @Override
     public int getScrollableUnitIncrement(
             Rectangle visible, int orientation, int direction) {
         return 18;
     }

     @Override
     public int getScrollableBlockIncrement(
             Rectangle visible, int orientation, int direction) {
         return Math.max(18, visible.height - 36);
     }

     @Override
     public boolean getScrollableTracksViewportWidth() {
         return true;
     }

     @Override
     public boolean getScrollableTracksViewportHeight() {
         return false;
     }
 }


    private static class QuestionBackgroundPanel extends JPanel {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                QuestionDialog.quality(g);
                g.setPaint(new GradientPaint(0, 0, new Color(14, 22, 54),
                    getWidth(), getHeight(), new Color(47, 41, 112)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(115, 113, 239, 47));
                g.fillOval(-110, -110, 360, 360);
                g.setColor(new Color(92, 156, 242, 34));
                g.fillOval(360, 310, 440, 440);
                g.dispose();
            }
    }

    private static class QuestionCardPanel extends JPanel {
            private Color fill;
            private final int radius;
            QuestionCardPanel(Color fill, int radius) {
                this.fill = fill;
                this.radius = radius;
                setOpaque(false);
            }
            void setFill(Color fill) { this.fill = fill; repaint(); }
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                QuestionDialog.quality(g);
                g.setColor(fill);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
                g.dispose();
                super.paintComponent(graphics);
            }
    }
}
