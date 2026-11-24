package Tests;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import Controller.GameController;
import Controller.MovementController;
import Model.Game;
import Model.Player;

/** Singleton design pattern: there is one central controller for the running game. */
public final class GameControllerSingletonCheck {
    private GameControllerSingletonCheck() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // nobody can create a second controller with new
        for (Constructor<?> constructor : GameController.class.getDeclaredConstructors()) {
            check(Modifier.isPrivate(constructor.getModifiers()), "the constructor is private");
        }

        // before a game starts there is no instance
        check(!GameController.hasInstance(), "no instance before the first game");
        boolean refused = false;
        try {
            GameController.getInstance();
        } catch (IllegalStateException expected) {
            refused = true;
        }
        check(refused, "getInstance refuses when no game was started");
        System.out.println("PASS - private constructor, and no instance before a game starts");

        // starting a game makes its controller the instance
        GameController first = GameController.createNewGame(
                new String[]{"Ann", "Bob"}, new String[]{"Green", "Red"}, "Easy");
        check(GameController.hasInstance(), "there is an instance after a game started");
        check(GameController.getInstance() == first, "getInstance returns the controller of the game");
        check(GameController.getInstance() == GameController.getInstance(), "always the same object");
        System.out.println("PASS - one instance for the running game");

        // a new game replaces it, so a part of the program never works with a stale game
        GameController second = GameController.createNewGame(
                new String[]{"Cy", "Dan", "Eve"}, new String[]{"Green", "Blue", "Red"}, "Medium");
        check(second != first, "a new game has a new controller");
        check(GameController.getInstance() == second, "the new game replaced the instance");
        check(second.getPlayerCount() == 3 && "Medium".equals(second.getDifficulty()),
                "the instance is the controller of the new game");
        System.out.println("PASS - a new game replaces the instance");

        // invalid setup does not touch the instance
        boolean rejected = false;
        try {
            GameController.createNewGame(new String[]{"Ann", "Ann"}, new String[]{"Green", "Red"}, "Easy");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected && GameController.getInstance() == second, "a bad setup leaves the running game alone");
        System.out.println("PASS - a bad setup leaves the running game alone");

        // the test seam makes separate controllers that never become the instance
        Game gameA = new Game(new Player[]{new Player("A", "Green"), new Player("B", "Red")}, "Easy");
        Game gameB = new Game(new Player[]{new Player("C", "Blue"), new Player("D", "Yellow")}, "Hard");
        GameController a = GameController.forTesting(gameA, new MovementController(gameA));
        GameController b = GameController.forTesting(gameB, new MovementController(gameB));
        check(a != b && a != second && b != second, "the test seam gives separate controllers");
        check(GameController.getInstance() == second, "the test seam does not touch the instance");
        check("Easy".equals(a.getDifficulty()) && "Hard".equals(b.getDifficulty()), "the test controllers are independent");
        System.out.println("PASS - the test seam does not touch the instance");

        // leaving the game forgets it
        GameController.clearInstance();
        check(!GameController.hasInstance(), "the instance is forgotten");
        System.out.println("PASS - clearInstance");
        System.out.println("All game controller singleton checks passed.");
    }
}
