package dev.privalo.ui.api;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Public API of RadiantUI.
 *
 * <pre>{@code
 * RadiantUIApi ui = RadiantUIApi.get();
 * ui.setTheme(player, "snoopy_night");
 * player.openInventory(Bukkit.createInventory(null, 54, ui.containerTitle(player, 6, "<accent>Mi menú")));
 * }</pre>
 */
public final class RadiantUIApi {

    private static RadiantUIApi instance;

    private final RadiantUI plugin;

    private RadiantUIApi(RadiantUI plugin) {
        this.plugin = plugin;
    }

    /** Internal: called by RadiantUI on enable/disable. */
    public static void install(RadiantUI plugin) {
        instance = plugin == null ? null : new RadiantUIApi(plugin);
    }

    /** @throws IllegalStateException if RadiantUI is not enabled */
    public static RadiantUIApi get() {
        if (instance == null) {
            throw new IllegalStateException("RadiantUI no está habilitado");
        }
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    // ── themes ──

    /** Ids of every enabled theme. */
    public List<String> themes() {
        return plugin.themes().enabled().stream().map(Theme::id).toList();
    }

    public String globalTheme() {
        return plugin.themes().global().id();
    }

    public String themeOf(Player player) {
        return plugin.themes().of(player).id();
    }

    /** Sets a personal theme (null = global). Re-sends the pack and refreshes HUD/scoreboard. */
    public boolean setTheme(Player player, String themeId) {
        return plugin.changeTheme(player, themeId);
    }

    /** Changes the server-wide theme (saved to config.yml) and re-sends packs to everyone using it. */
    public boolean setGlobalTheme(String themeId) {
        return plugin.changeGlobalTheme(themeId);
    }

    // ── refresh ──

    public void refreshHud(Player player) {
        plugin.hud().refresh(player);
    }

    public void refreshScoreboard(Player player) {
        plugin.scoreboard().refresh(player);
    }

    public void setHud(Player player, boolean on) {
        plugin.hud().setOn(player, on);
    }

    public void setScoreboard(Player player, boolean on) {
        plugin.scoreboard().setOn(player, on);
    }

    // ── pack ──

    /** Re-sends the player's theme pack. False if the pack is not generated yet. */
    public boolean resendPack(Player player) {
        return plugin.packs().send(player, false);
    }

    /** Regenerates every theme pack and re-sends them; completes with the number of packs. */
    public CompletableFuture<Integer> rebuildPacks() {
        return plugin.packs().rebuild().thenApply(count -> {
            plugin.packs().sendAll();
            return count;
        });
    }

    // ── text ──

    /** Parses MiniMessage with RadiantUI tags and the player's theme colours. */
    public Component parse(Player player, String miniMessage) {
        return plugin.text().parse(miniMessage, plugin.themes().of(player));
    }

    /** Title with the themed chest overlay (rows 1-6) for your own GUIs. */
    public Component containerTitle(Player player, int rows, String titleMiniMessage) {
        return plugin.containers().title(player, "auto", rows, titleMiniMessage);
    }

    /** Title with a specific overlay id (chest_N or a custom overlay). */
    public Component containerTitle(Player player, String overlayId, String titleMiniMessage) {
        return plugin.containers().title(player, overlayId, 0, titleMiniMessage);
    }

    /** Single glyph component (heart, paw, puppy, coin...). */
    public Component glyph(String id) {
        return plugin.text().glyph(id);
    }
}
