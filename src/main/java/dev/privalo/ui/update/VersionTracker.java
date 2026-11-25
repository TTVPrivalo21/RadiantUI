package dev.privalo.ui.update;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

/**
 * Remembers the last plugin version that ran on this server ({@code plugins/RadiantUI/.last-version})
 * and prints a console notice when the jar was replaced by a different version.
 */
public final class VersionTracker {

    private static final String FILE_NAME = ".last-version";

    private VersionTracker() {
    }

    /** Compares the running version with the stored one, logs the change and stores the new version. */
    public static void check(File dataFolder, String currentVersion, Logger logger) {
        File file = new File(dataFolder, FILE_NAME);
        String previous = null;
        if (file.isFile()) {
            try {
                previous = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
            } catch (IOException ex) {
                logger.warning("No se pudo leer " + FILE_NAME + ": " + ex.getMessage());
            }
        }
        if (previous == null || previous.isEmpty()) {
            logger.info("================================================");
            logger.info(" RadiantUI " + currentVersion + " · instalación inicial");
            logger.info("================================================");
        } else if (!previous.equals(currentVersion)) {
            int cmp = SemVer.compare(currentVersion, previous);
            String kind = cmp > 0 ? "actualizado" : cmp < 0 ? "versión anterior instalada (downgrade)" : "cambio de build";
            logger.warning("================================================");
            logger.warning(" RadiantUI " + kind + ": " + previous + " -> " + currentVersion);
            logger.warning(" Revisa el README por si hay nuevas opciones en los .yml");
            logger.warning("================================================");
        } else {
            return;
        }
        try {
            Files.createDirectories(dataFolder.toPath());
            Files.writeString(file.toPath(), currentVersion, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.warning("No se pudo guardar " + FILE_NAME + ": " + ex.getMessage());
        }
    }
}
