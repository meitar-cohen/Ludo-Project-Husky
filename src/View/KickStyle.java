package View;

import java.awt.Color;

/**
 * How the kick animation of a capture looks: the word in the comic burst and its colours.
 * It is the "look" part of the Strategy pattern of the worlds: every {@link WorldTheme} returns
 * its own style from {@link WorldTheme#kickStyle()}, and the boards draw the kick with whatever
 * style they get. The 2D board uses {@link #CLASSIC}.
 */
final class KickStyle {
    static final KickStyle CLASSIC = new KickStyle("POW!", new Color(255, 214, 64), new Color(255, 120, 30),
            new Color(110, 62, 28), new Color(250, 244, 230));
    static final KickStyle FIRE = new KickStyle("BOOM!", new Color(255, 170, 40), new Color(220, 40, 20),
            new Color(60, 30, 24), new Color(255, 214, 160));
    static final KickStyle SPLASH = new KickStyle("SPLASH!", new Color(150, 225, 255), new Color(30, 130, 220),
            new Color(236, 120, 40), new Color(255, 255, 255));
    static final KickStyle GLAM = new KickStyle("SNAP!", new Color(255, 190, 225), new Color(230, 70, 150),
            new Color(190, 40, 110), new Color(255, 236, 246));
    static final KickStyle RACE = new KickStyle("VROOM!", new Color(255, 236, 90), new Color(230, 40, 40),
            new Color(30, 30, 36), new Color(240, 240, 245));

    /** Word written in the burst at the moment of the kick. */
    final String word;
    /** Inner and outer colour of the burst. */
    final Color burst, burstRim;
    /** Colour of the boot and of its sole. */
    final Color boot, sole;

    private KickStyle(String word, Color burst, Color burstRim, Color boot, Color sole) {
        this.word = word;
        this.burst = burst;
        this.burstRim = burstRim;
        this.boot = boot;
        this.sole = sole;
    }
}
