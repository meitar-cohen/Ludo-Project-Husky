package Tests;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import Controller.HistoryController;

/** Singleton design pattern (SCRUM 3.5): the application has one history manager. */
public final class HistorySingletonCheck {
    private static void check(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }

    public static void main(String[] args) {
        check(HistoryController.getInstance() != null, "the instance exists");
        check(HistoryController.getInstance() == HistoryController.getInstance(),
                "getInstance always returns the same object");
        for (Constructor<?> constructor : HistoryController.class.getDeclaredConstructors()) {
            check(Modifier.isPrivate(constructor.getModifiers()),
                    "nobody can create a second manager with new");
        }
        System.out.println("PASS - one instance, private constructor");

        HistoryController forTest = HistoryController.forFile(new File("not-the-real-history.csv"));
        check(forTest != HistoryController.getInstance(),
                "the test seam never replaces the application instance");
        check(HistoryController.getInstance() == HistoryController.getInstance(),
                "the instance is still the same after a test manager was made");
        System.out.println("PASS - the test seam does not touch the singleton");
        System.out.println("All singleton checks passed.");
    }
}
