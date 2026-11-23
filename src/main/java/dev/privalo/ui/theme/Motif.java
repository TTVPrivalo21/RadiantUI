package dev.privalo.ui.theme;

import java.util.Locale;

/** Decoration style painted on frames, menus and backgrounds. */
public enum Motif {
    /** Little white puppy with black ears, paw prints, hearts and stars. */
    SNOOPY,
    /** Paw prints only. */
    PAWS,
    /** Hearts only. */
    HEARTS,
    /** Clean frames, no decoration. */
    PLAIN;

    public static Motif parse(String raw) {
        if (raw == null) {
            return SNOOPY;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SNOOPY;
        }
    }

    public boolean paws() {
        return this == SNOOPY || this == PAWS;
    }

    public boolean hearts() {
        return this == SNOOPY || this == HEARTS;
    }

    public boolean puppy() {
        return this == SNOOPY;
    }
}
