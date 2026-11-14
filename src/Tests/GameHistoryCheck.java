package Tests;

import Controller.GameController;
import Controller.HistoryController;
import Controller.MovementController;
import Enums.Stage;
import Model.Game;
import Model.GameRecord;
import Model.Player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * SRS 3.2.8.3.1 - 3.2.8.3.4 - saving and reading the game history file.
 * Uses a temporary copy of the file, so the project's history file is not changed.
 */
public class GameHistoryCheck {

    private static void check(String label, boolean ok) {
        if (!ok) {
            throw new AssertionError(label);
        }
        System.out.println("PASS - " + label);
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("ludo-history").toFile();

        // 1. הקובץ נוצר עם שורת הכותרת של התבנית אם אינו קיים
        File missing = new File(new File(dir, "data"), HistoryController.HISTORY_FILE_NAME);
        HistoryController historyController = new HistoryController(missing);
        check("missing file means no games", historyController.readGames().isEmpty());
        historyController.appendRecord(new GameRecord(1, "easy", "01/11/2026;10:00:00",
                new String[] {"Dana", "Avi", "", ""}, "00:12:30"));
        String content = read(missing);
        check("file created with the template header",
                content.startsWith(HistoryController.HISTORY_HEADER + "\r\n"));
        check("row written in template column order",
                content.contains("1,easy,01/11/2026;10:00:00,Dana,Avi,,,00:12:30\r\n"));

        // 2. הוספה לקובץ התבנית של הקורס (שאינו מסתיים בירידת שורה)
        File template = new File(dir, "template.csv");
        Files.write(template.toPath(), (HistoryController.HISTORY_HEADER
                + "\r\n1,hard,19/10/2026;12:01:00 ,Mor,Noy,Safa,Muhammed,01:05:04")
                .getBytes(StandardCharsets.UTF_8));
        HistoryController templateController = new HistoryController(template);
        GameRecord added = new GameRecord(2, "medium", "02/11/2026;18:30:05",
                new String[] {"Noa", "Tal", "Maor", ""}, "00:40:00");
        templateController.appendRecord(added);
        check("next game ID is a running number", added.getGameId() == 2);
        List<GameRecord> games = templateController.readGames();
        check("template row and new row are both read", games.size() == 2);
        check("template sample row is kept", "Mor".equals(games.get(0).getWinner())
                && "hard".equals(games.get(0).getDifficulty()));
        check("new row is on its own line", "Noa".equals(games.get(1).getWinner())
                && games.get(1).getPlaceNames()[3].isEmpty());

        // 3. שורות פגומות מדולגות
        Files.write(template.toPath(), "\r\nnot,a,valid,row\r\nx,easy,a,b,c,d,e,f\r\n"
                .getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
        List<GameRecord> read = templateController.readGames();
        check("damaged rows are skipped and counted",
                read.size() == 2 && templateController.getSkippedRows() == 2);

        // 4. שם עם פסיק נשמר ונקרא נכון
        GameRecord comma = new GameRecord(3, "easy", "03/11/2026;09:00:00",
                new String[] {"Cohen, Dana", "Avi", "", ""}, "00:05:00");
        templateController.appendRecord(comma);
        List<GameRecord> afterComma = templateController.readGames();
        check("name with a comma is kept",
                "Cohen, Dana".equals(afterComma.get(afterComma.size() - 1).getWinner())
                && comma.getGameId() == 3);

        // 5. דירוג לפי סדר הסיום, והמשתתף האחרון במקום האחרון
        Game game = new Game(TestPlayers.of(new String[] {"A", "B", "C"},
                new String[] {"Red", "Blue", "Green"}), "Medium");
        game.setPlayerFinished(2, true);
        game.setPlayerFinished(0, true);
        GameController turns = new GameController(
                game, new MovementController(game));
        Player[] ranking = turns.getRanking();
        check("ranking follows the finish order",
                "C".equals(ranking[0].getName()) && "A".equals(ranking[1].getName())
                && "B".equals(ranking[2].getName()));

        // 6. שמירת משחק שהסתיים דרך הבקר, פעם אחת בלבד
        File gameFile = new File(dir, "games.csv");
        HistoryController gameHistory = new HistoryController(gameFile);
        turns.startGameClock();
        turns.getTurnController().passTurn();
        check("game is over", game.getStage() == Stage.DONE);
        turns.stopGameClock();
        check("save succeeds", gameHistory.saveFinishedGame(game, turns) == null
                && game.isSavedToHistory());
        gameHistory.saveFinishedGame(game, turns);
        List<GameRecord> saved = new HistoryController(gameFile).readGames();
        check("game saved exactly once", saved.size() == 1);
        GameRecord row = saved.get(0);
        check("saved row: medium, places by ranking, empty 4th place",
                "medium".equals(row.getDifficulty()) && "C".equals(row.getPlaceNames()[0])
                && "B".equals(row.getPlaceNames()[2]) && row.getPlaceNames()[3].isEmpty());
        check("saved timestamp and duration formats",
                row.getTimestamp().matches("\\d{2}/\\d{2}/\\d{4};\\d{2}:\\d{2}:\\d{2}")
                && row.getDuration().matches("\\d{2}:\\d{2}:\\d{2}"));

        // 7. משחק בלי אף הטלה לא נשמר
        Game unplayed = new Game(TestPlayers.of(new String[] {"A", "B"},
                new String[] {"Red", "Blue"}), "Easy");
        File unplayedFile = new File(dir, "unplayed.csv");
        GameController unplayedTurns = new GameController(
                unplayed, new MovementController(unplayed));
        new HistoryController(unplayedFile).saveFinishedGame(
                unplayed, unplayedTurns);
        check("a game without any roll is not saved", !unplayedFile.exists());

        // 8. כשל בכתיבה מחזיר הודעת שגיאה במקום קריסה
        File blocked = new File(dir, "blocked");
        check("create blocking folder", blocked.mkdir());
        Game failing = new Game(TestPlayers.of(new String[] {"A", "B"},
                new String[] {"Red", "Blue"}), "Easy");
        GameController failingTurns = new GameController(
                failing, new MovementController(failing));
        failingTurns.startGameClock();
        String error = new HistoryController(blocked).saveFinishedGame(
                failing, failingTurns);
        check("write failure returns an error message", error != null && !failing.isSavedToHistory());

        // 9. טעינת ההיסטוריה למסך: המשחק החדש ראשון
        HistoryController loadedHistory = new HistoryController(template);
        List<GameRecord> result = loadedHistory.loadHistory();
        check("history is loaded newest first",
                result.get(0).getGameId() == 3
                && loadedHistory.getSkippedRows() == 2);

        System.out.println("All game history checks passed.");
    }
}
