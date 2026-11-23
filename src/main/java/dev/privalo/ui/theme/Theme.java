package dev.privalo.ui.theme;

/**
 * A UI theme: colours + decoration motif. Every enabled theme gets its own resource pack.
 *
 * @param id          config id (e.g. snoopy_classic)
 * @param displayName MiniMessage display name
 * @param description plain description shown on hover
 * @param enabled     whether a pack is generated and players can pick it
 * @param motif       decoration style
 * @param palette     colours
 */
public record Theme(String id, String displayName, String description, boolean enabled, Motif motif, Palette palette) {

    public static final String DEFAULT_ID = "snoopy_classic";

    public static Theme fallback() {
        return new Theme(DEFAULT_ID,
                "<white>★ Snoopy</white> <gray>Classic</gray>",
                "Cómic clásico en blanco y negro",
                true, Motif.SNOOPY, Palette.snoopyClassic());
    }
}
