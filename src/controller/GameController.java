package controller;

import java.awt.Color;
import java.util.Random;
import enums.RollAction;
import enums.Stage;
import model.Game;
import model.Player;

public class GameController {
    private final Game game;
    private final Random random = new Random();
    private final HistoryController history;

    public GameController(Game game) {
        this(game, new HistoryController());
    }

    public GameController(Game game, HistoryController history) {
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null");
        }
        if (history == null) {
            throw new IllegalArgumentException("History controller cannot be null");
        }
        this.game = game;
        this.history = history;
    }

    /**
     * SRS 3.2.8.3.2 - 3.2.8.3.3 - the game is over: stop the clock and save the game
     * to the history file.
     *
     * @return null on success, otherwise an error message for the user
     */
    public String saveGameToHistory() {
        game.stopClock();
        return history.saveFinishedGame(game);
    }

    public boolean isGameSavedToHistory() {
        return game.isSavedToHistory();
    }

    /** SRS 3.2.8.3.1 - players by final place. */
    public Player[] getRankedPlayers() {
        return game.getRanking();
    }

    public Stage getStage() {
        return game.getStage();
    }

    public void setStage(Stage stage) {
        game.setStage(stage);
    }

    public int getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public int getPendingSteps() {
        return game.getPendingSteps();
    }

    public boolean consumeFirstTurnPulse() {
        if (!game.isFirstTurnPulsePending()) return false;
        game.setFirstTurnPulsePending(false);
        return true;
    }

    public boolean canRoll() {
        return game.getStage() == Stage.ROLL;
    }

    public boolean isInTakeOutStage() {
        return game.getStage() == Stage.TAKE_OUT;
    }

    public boolean isInMoveStage() {
        return game.getStage() == Stage.MOVE;
    }

    public boolean isEasyGame() {
        return "Easy".equals(game.getDifficulty());
    }


    // SRS 3.2.3.3.4 - בדיקה שהכלי עדיין נמצא בטירה
    public boolean canTakeOutActivePiece(int slot) {

        return game.getStage() == Stage.TAKE_OUT
                && slot >= 0
                && slot < Game.PIECES_PER_PLAYER
                && game.hasPieceInCastle()
                && !game.isPieceOnBoard(
                        game.getCurrentPlayer(), slot);
    }

    // הוצאת כלי מהטירה ועדכון מצב המשחק
    public boolean takeOutActivePiece(int slot) {

        // אין להוציא כלי שכבר נמצא על הלוח
        if (!canTakeOutActivePiece(slot)
                || !game.selectPiece(slot)) {
            return false;
        }

        // הפחתת כלי אחד מהטירה של השחקן הפעיל
        game.setPiecesInCastle(
                game.getPiecesInCastle() - 1);

        // מיקום הכלי במשבצת ההתחלה
        game.setPieceOnBoard(true);
        game.setPieceStep(0);
        game.setPieceInInnerPath(false);
        game.setPieceInHomePath(false);

        // לאחר הוצאת כלי עם 6 השחקן יכול להטיל שוב
        game.setStage(Stage.ROLL);

        return true;
    }

    // בדיקה האם אפשר להזיז כלי קיים במקום להוציא כלי
    public boolean canChooseBoardMoveInsteadOfTakeOut() {

        return game.getStage() == Stage.TAKE_OUT
                && game.getLastStepDie() == 6
                && game.hasAnyPieceOnBoard()
                && (game.getPendingSteps() > 0
                    || hasAvailableHomePathMove(
                            game.getLastStepDie()));
    }

    // בחירת הזזת כלי קיים במקום הוצאה מהטירה
    public boolean chooseBoardMoveInsteadOfTakeOut() {

        if (!canChooseBoardMoveInsteadOfTakeOut()) {
            return false;
        }

        // מעבר למצב בחירת כלי על הלוח
        game.setStage(Stage.MOVE);

        return true;
    }

    
    public boolean hasPieceInCastle() {
        return game.hasPieceInCastle();
    }

    public boolean hasPieceOnBoard() {
        return game.hasAnyPieceOnBoard();
    }

    public int getActivePieceStep() {
        return game.getPieceStep();
    }

    public int getPieceStep(int pieceIndex) {
        if (!game.selectPiece(pieceIndex)) {
            return -1;
        }

        return game.getPieceStep();
    }

    public int getAnswerMoveTarget(boolean correct, int routeSize) {
        int change = correct ? 1 : -1;
        return getClockwiseTarget(game.getPieceStep(), change) >= 0
            && getClockwiseTarget(game.getPieceStep(), change) < routeSize
                ? getClockwiseTarget(game.getPieceStep(), change) : game.getPieceStep();
    }

    public int getSurpriseMoveTarget(int routeSize) {
        int target = game.getPieceStep() - 3;
        return target >= 0 ? target : 0;
    }

    public void finishGame() {
        game.setStage(Stage.DONE);
    }

    public String getCurrentPlayerName() {
        return game.getCurrentPlayerName();
    }

    public String getCurrentPlayerColor() {
        return game.getCurrentPlayerColor();
    }

    // SRS 3.2.4.3.3 - חישוב צעדים בהתאם למסלול שבו נמצא הכלי
    public int getMoveStepsForSelectedPiece() {

        // במסלול הבית משתמשים בקובייה הרגילה ללא מכפיל
        if (game.isPieceInHomePath()) {
            return game.getLastStepDie();
        }

        // במסלול המשותף משתמשים בתוצאה המחושבת
        return game.getPendingSteps();
    }

    // בדיקה אם קיים כלי שניתן להזיז במסלול הבית
    private boolean hasAvailableHomePathMove(int stepDie) {

        int player = game.getCurrentPlayer();

        for (int pieceIndex = 0;
                pieceIndex < Game.PIECES_PER_PLAYER;
                pieceIndex++) {

            // הכלי חייב להיות על הלוח ובמסלול הבית
            if (game.isPieceOnBoard(player, pieceIndex)
                    && game.isPieceInHomePath(player, pieceIndex)
                    && !game.isPieceFinished(player, pieceIndex)
                    && game.getPieceStep(player, pieceIndex) + stepDie <= 6) {

                return true;
            }
        }

        return false;
    }
    
    /**
     * SRS 3.2.4.3.1 -
     * Checks whether the selected piece may be moved during the current turn.
     */
    public boolean isPieceSelectionLegal(int pieceIndex) {
        if (game.getStage() != Stage.MOVE) {
            return false;
        }

        if (!game.selectPiece(pieceIndex)) {
            return false;
        }

        if (!game.hasPieceOnBoard()
                || game.isPieceFinished(
                        game.getCurrentPlayer(), pieceIndex)) {
            return false;
        }

        return getMoveStepsForSelectedPiece() > 0;    }

    /**
     * SRS 3.2.4.3.1 -
     * Validates the selected piece and calculates its target position
     * without changing its position.
     */
    public int validatePieceMove(int pieceIndex, int routeSize) {
        if (!isPieceSelectionLegal(pieceIndex)) {
            return -1;
        }

        int currentStep = game.getPieceStep();
        int steps = getMoveStepsForSelectedPiece();

        if (!isClockwiseMoveAllowed(currentStep, steps, routeSize)) {
            return -1;
        }

        return getClockwiseTarget(currentStep, steps);
    }

    /**
     * SRS 3.2.4.3.1 -
     * Applies an approved movement to the selected piece.
     */
    public boolean movePiece(int pieceIndex, int targetStep, int routeSize) {
        if (!isPieceSelectionLegal(pieceIndex)) {
            return false;
        }

        if (targetStep < 0 || targetStep >= routeSize) {
            return false;
        }

        game.setPieceStep(targetStep);
        game.setStage(Stage.BUSY);
        return true;
    }

    // עדכון מיקום הכלי לאחר אירוע שאלה או הפתעה
    public boolean applyEventMovement(
            int pieceIndex, int targetStep, int routeSize) {

        // האירוע יכול לשנות מיקום רק במהלך תנועה פעיל
        if (game.getStage() != Stage.BUSY
                || !game.selectPiece(pieceIndex)
                || !game.hasPieceOnBoard()
                || targetStep < 0
                || targetStep >= routeSize) {
            return false;
        }

        // שמירת המיקום החדש במודל
        game.setPieceStep(targetStep);

        return true;
    }
    

    // SRS 3.2.4.3.4 - כניסה לבית הסיום בתוצאה מדויקת בלבד
    public boolean tryFinishPiece(int pieceIndex, int routeSize) {

        // בדיקת מצב המשחק והכלי שנבחר
        if (game.getStage() != Stage.MOVE
                || !game.selectPiece(pieceIndex)
                || !game.hasPieceOnBoard()
                || game.isPieceFinished(
                        game.getCurrentPlayer(), pieceIndex)) {
            return false;
        }

        boolean inHome = game.isPieceInHomePath();

        // במסלול הבית משתמשים בקובייה הרגילה בלבד
        int steps = inHome
                ? game.getLastStepDie()
                : game.getPendingSteps();

        int current = game.getPieceStep();

        if (steps <= 0 || current < 0) {
            return false;
        }

        if (inHome) {

            // משבצות מסלול הבית הן 0 עד 5
            // בית הסיום נמצא בדיוק בצעד 6
            if (routeSize != 6
                    || current >= 6
                    || current + steps != 6) {
                return false;
            }

        } else {

            // אפשר לסיים גם במהלך שחוצה את סוף המסלול המשותף
            // רק ממסלול 52 המשבצות ובתוצאה מדויקת
            if (routeSize != 52
                    || current >= routeSize
                    || (!isEasyGame() && !game.isPieceInInnerPath())
                    || current + steps != routeSize + 6) {
                return false;
            }
        }

        // עדכון מצב הכלי במודל
        if (!game.finishSelectedPiece()) {
            return false;
        }

        game.setStage(Stage.BUSY);
        return true;
    }

    // בדיקה אם השחקן הפעיל סיים את כל ארבעת הכלים
    public boolean isCurrentPlayerFinished() {
        return game.isPlayerFinished(game.getCurrentPlayer());
    }

    
    public boolean isClockwiseMoveAllowed(int currentStep, int steps, int routeSize) {
        return currentStep >= 0 && steps >= 0 && currentStep + steps < routeSize;
    }

    public int getClockwiseTarget(int currentStep, int steps) {
        return currentStep + steps;
    }

    /**
     * דרישה 3.2.4.3.2 -
     * בודקת אם צבע משבצת החץ תואם לצבע הכלי הפעיל.
     *
     * חץ תואם מאפשר לכלי להיכנס למסלול הפנימי הבא.
     * חץ בצבע אחר נחשב למשבצת רגילה.
     */
    public boolean isMatchingArrow(Color arrowColor, Color pieceColor) {
        return arrowColor != null
                && pieceColor != null
                && arrowColor.equals(pieceColor);
    }

    /**
     * דרישה 3.2.4.3.2 -
     * מעבירה את הכלי שנבחר למסלול הפנימי כאשר צבע החץ תואם.
     */
    public boolean enterInnerPathFromArrow(Color arrowColor, Color pieceColor, int innerPathStartStep) {
        if (isEasyGame()
                || !isMatchingArrow(arrowColor, pieceColor)) {
            return false;
        }

        if (!game.hasPieceOnBoard()) {
            return false;
        }

        if (innerPathStartStep < 0) {
            return false;
        }

        game.setPieceInInnerPath(true);
        game.setPieceInHomePath(false);
        game.setPieceStep(innerPathStartStep);

        return true;
    }

    public void setPendingSteps(int pendingSteps) {
        game.setPendingSteps(pendingSteps);
    }

    public int rollStepDie() {
        return random.nextInt(6) + 1;
    }

    /**
     * SRS 3.2.3.3.2 - the bonus die of the level:
     * Easy - none (0), Medium - Boost 1-3, Hard - UltraBoost 1-6.
     */
    public int rollBoostDie() {
        int faces = getBoostDieFaces();
        return faces == 0 ? 0 : random.nextInt(faces) + 1;
    }

    /** SRS 3.2.3.3.2 - number of faces of the bonus die: 0 (Easy), 3 (Boost), 6 (UltraBoost). */
    public int getBoostDieFaces() {
        if ("Hard".equals(game.getDifficulty())) return 6;
        if ("Medium".equals(game.getDifficulty())) return 3;
        return 0;
    }

    /**
     * SRS 3.2.3.3.5 - records the step die of a roll and counts sixes in a row.
     *
     * @return true when this roll is the third 6 in a row - the roll is cancelled
     *         and the turn should pass
     */
    public boolean registerRollAndCheckThirdSix(int stepDie) {
        game.setLastStepDie(stepDie);
        if (stepDie != 6) {
            game.setConsecutiveSixes(0);
            return false;
        }
        game.setConsecutiveSixes(game.getConsecutiveSixes() + 1);
        if (game.getConsecutiveSixes() >= 3) {
            game.setConsecutiveSixes(0);
            return true;
        }
        return false;
    }

    /** SRS 3.2.3.3.5 - a 6 on the step die gives the same player another turn. */
    public boolean hasExtraTurn() {
        return game.getLastStepDie() == 6;
    }

    /** SRS 3.2.3.3.5  - the same player rolls again: the game waits for a new roll. */
    public void grantExtraTurn() {
        game.setStage(Stage.ROLL);
    }

    /** SRS 3.2.1.3.3  - starts the game clock at the first roll. */
    public void startGameClock() {
        game.startClock();
    }

    /** SRS 3.2.1.3.3  - game clock as HH:MM:SS. */
    public String getElapsedTime() {
        long seconds = game.getElapsedSeconds();
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    public long getElapsedSeconds() {
        return game.getElapsedSeconds();
    }


    // SRS 3.2.3.3.3 - חישוב מספר הצעדים לפי רמת הקושי
    public RollAction finishRoll(int stepDie, int boostDie) {

        // חישוב התנועה ושמירתה במצב המשחק
        game.setPendingSteps(calculateSteps(stepDie, boostDie));

        // הוצאת כלי מהטירה תלויה בקובייה הרגילה בלבד
        if (stepDie == 6 && game.hasPieceInCastle()) {
            game.setStage(Stage.TAKE_OUT);
            return RollAction.TAKE_OUT;
        }

        // תנועה במסלול משותף או במסלול הבית
        // במסלול הבית מותר לזוז גם כשהמכפיל נותן 0
        if ((game.getPendingSteps() > 0 && game.hasAnyPieceOnBoard())
                || hasAvailableHomePathMove(stepDie)) {

            game.setStage(Stage.MOVE);
            return RollAction.MOVE;
        }

        // כרגע אין תנועה חוקית במסלול המשותף
        game.setStage(Stage.ROLL);
        return RollAction.NEED_SIX;
    }

    // חישוב הצעדים בהתאם לקוביות ולרמת הקושי
    public int calculateSteps(int stepDie, int boostDie) {

        // ברמה קלה משתמשים רק בקובייה הרגילה
        if ("Easy".equals(game.getDifficulty())) {
            return stepDie;
        }

        // ברמות Medium ו-Hard משתמשים במכפיל
        return stepDie * (boostDie - 1);
    }


    public void takePieceOutOfCastle() {
        return;
    }

    // SRS 3.2.3.3.6 - בדיקה אם קיים כלי שניתן להזיז
    public boolean hasAnyLegalMove(int[] routeSizes) {

        // הבדיקה רלוונטית רק במצב בחירת מהלך
        if ((game.getStage() != Stage.MOVE
                && game.getStage() != Stage.TAKE_OUT)
                || routeSizes == null
                || routeSizes.length != Game.PIECES_PER_PLAYER) {
            return false;
        }

        int player = game.getCurrentPlayer();

        // בדיקת כל ארבעת הכלים של המשתתף הפעיל
        for (int piece = 0; piece < Game.PIECES_PER_PLAYER; piece++) {

            // כלי שנמצא בטירה אינו יכול לזוז על הלוח
        	if (!game.isPieceOnBoard(player, piece)
        	        || game.isPieceFinished(player, piece)) {
        	    continue;
        	}

            boolean inHome = game.isPieceInHomePath(player, piece);

            // במסלול הבית משתמשים בקובייה הרגילה
            int steps = inHome
                    ? game.getLastStepDie()
                    : game.getPendingSteps();

            int current = game.getPieceStep(player, piece);
            int routeSize = routeSizes[piece];

            // בדיקת נתונים תקינים ותנועה חיובית
            if (steps <= 0
                    || current < 0
                    || routeSize <= 0
                    || current >= routeSize) {
                continue;
            }

            int destination = current + steps;

            // תנועה חוקית בתוך המסלול הנוכחי
            // במסלול הבית מותר להגיע בדיוק לבית הסיום
            if (inHome && destination <= 6) {
                return true;
            }

            // במסלול המשותף מותר לנוע בתוך גבולות המסלול
            if (!inHome && destination < routeSize) {
                return true;
            }

            // מעבר חוקי ממסלול 52 המשבצות למסלול הבית
            if (!inHome
                    && routeSize == 52
                    && (isEasyGame()
                        || game.isPieceInInnerPath(player, piece))
                    && destination <= 52 + 6) {
                return true;
            }
        }

        // כל הכלים נבדקו ולא נמצא מהלך חוקי
        return false;
    }

 // SRS 3.2.4.3.3 - כניסה למסלול הבית לאחר השלמת המסלול המשותף
    public boolean tryEnterHomePath(int pieceIndex, int sharedRouteSize) {

        // בדיקת מצב המשחק והכלי שנבחר
        if (game.getStage() != Stage.MOVE
                || sharedRouteSize <= 0
                || !game.selectPiece(pieceIndex)
                || !game.hasPieceOnBoard()
                || game.isPieceInHomePath()) {
            return false;
        }

        // ברמות בינונית וקשה נדרש להגיע תחילה למסלול הפנימי
        if (!"Easy".equals(game.getDifficulty())
                && !game.isPieceInInnerPath()) {
            return false;
        }

        int steps = game.getPendingSteps();
        int destination = game.getPieceStep() + steps;

        // בדיקה שהכלי השלים את המסלול בלי לחרוג ממסלול הבית
        if (steps <= 0
                || destination < sharedRouteSize
                || destination >= sharedRouteSize + 6) {
            return false;
        }

        // מעבר הכלי למסלול הבית ושמירת מיקומו
        game.setPieceInHomePath(true);
        game.setPieceStep(destination - sharedRouteSize);
        game.setStage(Stage.BUSY);

        return true;
    }

    public void takeOutPiece() {
        takePieceOutOfCastle();
        game.setStage(Stage.ROLL);
    }

    // SRS 3.2.3.3.6 - מעבר תור ודילוג על משתתפים שסיימו
    public void passTurn() {

        // רצף השישיות שייך למשתתף ולא עובר לשחקן הבא
        game.setConsecutiveSixes(0);

        int total = game.getPlayerCount();
        int active = 0;

        // ספירת המשתתפים שעדיין במשחק
        for (int player = 0; player < total; player++) {
            if (!game.isPlayerFinished(player)) {
                active++;
            }
        }

        // כאשר נותר משתתף אחד או פחות, אין תור נוסף
        if (active <= 1) {
            game.setStage(Stage.DONE);
            return;
        }

        // חיפוש המשתתף הבא שעדיין לא סיים
        for (int offset = 1; offset <= total; offset++) {

            int candidate =
                    (game.getCurrentPlayer() + offset) % total;

            if (!game.isPlayerFinished(candidate)) {

                game.setCurrentPlayer(candidate);
                game.setStage(Stage.ROLL);
                return;
            }
        }
    }
}
