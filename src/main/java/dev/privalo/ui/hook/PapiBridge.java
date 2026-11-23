package dev.privalo.ui.hook;

import dev.privalo.ui.RadiantUI;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Isolates every reference to PlaceholderAPI classes so RadiantUI loads fine without it.
 */
public final class PapiBridge {

    private static volatile boolean available;

    private PapiBridge() {
    }

    public static void detect() {
        available = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static boolean available() {
        return available;
    }

    public static String apply(OfflinePlayer player, String text) {
        if (!available || text == null || text.indexOf('%') < 0) {
            return text;
        }
        return Inner.apply(player, text);
    }

    /** Registers the %radiantui_*% expansion when PlaceholderAPI is present. */
    public static void registerExpansion(RadiantUI plugin) {
        if (available) {
            Inner.register(plugin);
        }
    }

    public static void unregisterExpansion() {
        if (available) {
            Inner.unregister();
        }
    }

    /** Nested class: only loaded (and PlaceholderAPI resolved) when actually used. */
    private static final class Inner {
        private static RadiantExpansion expansion;

        static String apply(OfflinePlayer player, String text) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        static void register(RadiantUI plugin) {
            if (expansion == null) {
                expansion = new RadiantExpansion(plugin);
                expansion.register();
            }
        }

        static void unregister() {
            if (expansion != null) {
                expansion.unregister();
                expansion = null;
            }
        }
    }
}
