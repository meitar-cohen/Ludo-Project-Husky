package View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The catalogue of worlds (strategies) the player can choose from. A new world is added by
 * writing one class that implements {@link WorldTheme} and listing it here.
 */
final class Themes {

    private static final List<WorldTheme> ALL = new ArrayList<>();

    static {
        ALL.add(new SkyTheme());
        ALL.add(new VolcanoTheme());
        ALL.add(new PoolTheme());
        ALL.add(new SalonTheme());
        ALL.add(new RacingTheme());
    }

    private Themes() {
    }

    /** All worlds, in the order of the choice screen. */
    static List<WorldTheme> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** "Ludo in the Sky", the original world. */
    static WorldTheme defaultTheme() {
        return ALL.get(0);
    }

    /** The world with the given {@link WorldTheme#id()}, or the default one. */
    static WorldTheme byId(String id) {
        for (WorldTheme theme : ALL) {
            if (theme.id().equals(id)) {
                return theme;
            }
        }
        return defaultTheme();
    }
}
