package model;

/**
 * SRS 3.2.8.3.2 - one row of the game history file (one finished game).
 * The fields follow the columns of the course history template.
 */
public class GameRecord {

    public static final int MAX_PLAYERS = 4;

    private final int gameId;
    private final String difficulty;
    private final String timestamp;
    private final String[] placeNames;
    private final String duration;

    public GameRecord(int gameId, String difficulty, String timestamp,
                      String[] placeNames, String duration) {
        this.gameId = gameId;
        this.difficulty = difficulty == null ? "" : difficulty.trim();
        this.timestamp = timestamp == null ? "" : timestamp.trim();
        this.placeNames = new String[MAX_PLAYERS];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            String name = placeNames != null && i < placeNames.length ? placeNames[i] : null;
            this.placeNames[i] = name == null ? "" : name.trim();
        }
        this.duration = duration == null ? "" : duration.trim();
    }

    public int getGameId() {
        return gameId;
    }

    /** The difficulty as written in the file: easy, medium or hard. */
    public String getDifficulty() {
        return difficulty;
    }

    /** Date and time of the game, DD/MM/YYYY;HH:MM:SS. */
    public String getTimestamp() {
        return timestamp;
    }

    /** Player names by final place (index 0 = 1st place). Empty when the place is not used. */
    public String[] getPlaceNames() {
        return placeNames.clone();
    }

    public String getWinner() {
        return placeNames[0];
    }

    /** Game duration, HH:MM:SS. */
    public String getDuration() {
        return duration;
    }
}
