package Controller;

/**
 * Command design pattern - one action of the game (take a piece out, move, finish, capture...)
 * written as an object. The object knows how to do the action and how to take it back, so the
 * game can keep a log of what happened and undo it.
 *
 * <p>Roles: {@code GameCommand} = Command, the classes that implement it = ConcreteCommands,
 * {@link MovementController} = Receiver (does the real work), {@link CommandInvoker} = Invoker,
 * {@link GameController} (used by the game screen) = Client.
 */
public interface GameCommand {

    /** Does the action. Returns false (and changes nothing) when the action is not legal. */
    boolean execute();

    /** Takes back an executed action and restores the game as it was before it. */
    boolean undo();

    /** Short text for the game log, for example "Alice moves piece 2 from step 3 to step 9". */
    String getDescription();
}
