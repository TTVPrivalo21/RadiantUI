package dev.privalo.ui.hook;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.text.Glyph;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

/**
 * PlaceholderAPI expansion:
 * <ul>
 *   <li>%radiantui_theme% / %radiantui_theme_display% / %radiantui_theme_global%</li>
 *   <li>%radiantui_hud% / %radiantui_scoreboard% (on/off)</li>
 *   <li>%radiantui_glyph_&lt;id&gt;% → glyph character (default font)</li>
 *   <li>%radiantui_&lt;placeholder&gt;% → any internal placeholder (health, food, level, money...)</li>
 * </ul>
 */
public final class RadiantExpansion extends PlaceholderExpansion {

    private final RadiantUI plugin;

    public RadiantExpansion(RadiantUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "radiantui";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offline, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.equals("theme_global")) {
            return plugin.themes().global().id();
        }
        if (key.startsWith("glyph_")) {
            Glyph glyph = plugin.glyphs().get(key.substring("glyph_".length()));
            return glyph == null ? "" : glyph.asString();
        }
        Player player = offline == null ? null : offline.getPlayer();
        if (player == null) {
            return "";
        }
        switch (key) {
            case "theme" -> {
                return plugin.themes().of(player).id();
            }
            case "theme_display" -> {
                return plugin.themes().of(player).displayName();
            }
            case "hud" -> {
                return plugin.hud().isOn(player) ? "on" : "off";
            }
            case "scoreboard" -> {
                return plugin.scoreboard().isOn(player) ? "on" : "off";
            }
            default -> {
                Map<String, String> values = plugin.placeholders().of(player);
                return values.get(key);
            }
        }
    }
}
