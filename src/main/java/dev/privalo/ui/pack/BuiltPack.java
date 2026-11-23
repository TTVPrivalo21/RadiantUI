package dev.privalo.ui.pack;

import dev.privalo.ui.theme.Theme;

import java.io.File;

/**
 * A generated, zipped resource pack for one theme.
 *
 * @param theme   theme it was painted for
 * @param zip     zip bytes (served from memory by the HTTP server)
 * @param sha1    raw SHA-1
 * @param sha1Hex lowercase hex SHA-1
 * @param file    zip written to plugins/RadiantUI/packs/
 * @param entries number of files inside the pack
 */
public record BuiltPack(Theme theme, byte[] zip, byte[] sha1, String sha1Hex, File file, int entries) {

    /** File name used in the download URL; changes whenever the content changes (cache-busting). */
    public String fileName() {
        return theme.id() + "-" + sha1Hex.substring(0, 12) + ".zip";
    }
}
