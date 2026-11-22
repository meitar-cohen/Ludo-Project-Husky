package Controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Command design pattern - the Invoker. It runs the commands and keeps the ones that really
 * changed the game, so there is a log of the game and the last action can be taken back.
 * It does not know what a command does, only that it can execute and undo.
 */
public final class CommandInvoker {
    private final Deque<GameCommand> done = new ArrayDeque<>();
    private final List<String> log = new ArrayList<>();

    /** Runs the command; a command that is refused is not kept. */
    public boolean execute(GameCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Command is required");
        }
        if (!command.execute()) {
            return false;
        }
        done.push(command);
        log.add(command.getDescription());
        return true;
    }

    public boolean canUndo() {
        return !done.isEmpty();
    }

    /** Takes back the last executed command. */
    public boolean undoLast() {
        if (done.isEmpty()) {
            return false;
        }
        GameCommand last = done.pop();
        boolean undone = last.undo();
        if (undone) {
            log.add("Undo: " + last.getDescription());
        }
        return undone;
    }

    /** Number of commands that can still be undone. */
    public int size() {
        return done.size();
    }

    /** Text lines of everything that was done and undone, oldest first. */
    public List<String> getLog() {
        return new ArrayList<>(log);
    }

    public void clear() {
        done.clear();
        log.clear();
    }
}
