package dev.privalo.ui.theme;

import dev.privalo.ui.player.PlayerPrefs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Loads themes from config.yml and resolves which theme each player sees. */
public final class ThemeManager {

    private final PlayerPrefs prefs;
    private final Logger logger;
    private final Map<String, Theme> themes = new LinkedHashMap<>();
    private String globalId = Theme.DEFAULT_ID;
    private boolean allowPlayerThemes = true;

    public ThemeManager(PlayerPrefs prefs, Logger logger) {
        this.prefs = prefs;
        this.logger = logger;
    }

    public void load(FileConfiguration config) {
        themes.clear();
        ConfigurationSection section = config.getConfigurationSection("themes");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection t = section.getConfigurationSection(id);
                if (t == null) {
                    continue;
                }
                String key = id.toLowerCase(Locale.ROOT);
                if (!key.matches("[a-z0-9_\\-]+")) {
                    logger.warning("Tema ignorado (id inválido, usa a-z 0-9 _ -): " + id);
                    continue;
                }
                themes.put(key, new Theme(
                        key,
                        t.getString("display-name", key),
                        t.getString("description", ""),
                        t.getBoolean("enabled", true),
                        Motif.parse(t.getString("motif", "snoopy")),
                        Palette.fromConfig(t.getConfigurationSection("colors"))));
            }
        }
        if (themes.values().stream().noneMatch(Theme::enabled)) {
            Theme fallback = Theme.fallback();
            themes.put(fallback.id(), fallback);
            logger.warning("No hay temas habilitados: usando snoopy_classic por defecto.");
        }
        allowPlayerThemes = config.getBoolean("theme.allow-player-themes", true);
        String wanted = config.getString("theme.default", Theme.DEFAULT_ID).toLowerCase(Locale.ROOT);
        Theme wantedTheme = themes.get(wanted);
        if (wantedTheme == null || !wantedTheme.enabled()) {
            Theme first = enabled().get(0);
            logger.warning("El tema global '" + wanted + "' no existe o está deshabilitado, usando '" + first.id() + "'.");
            globalId = first.id();
        } else {
            globalId = wanted;
        }
    }

    public Theme global() {
        return themes.get(globalId);
    }

    public void setGlobal(String id) {
        Theme theme = find(id);
        if (theme != null) {
            globalId = theme.id();
        }
    }

    /** Theme shown to this player (their choice if allowed and valid, otherwise the global one). */
    public Theme of(Player player) {
        if (player == null || !playerChoiceAvailable()) {
            return global();
        }
        String chosen = prefs.theme(player);
        if (chosen != null) {
            Theme theme = themes.get(chosen);
            if (theme != null && theme.enabled()) {
                return theme;
            }
        }
        return global();
    }

    public boolean hasPersonalTheme(Player player) {
        String chosen = prefs.theme(player);
        return chosen != null && find(chosen) != null;
    }

    public void setPlayerTheme(Player player, String id) {
        prefs.theme(player, id == null ? null : id.toLowerCase(Locale.ROOT));
    }

    /** Enabled theme by id (case-insensitive) or null. */
    public Theme find(String id) {
        if (id == null) {
            return null;
        }
        Theme theme = themes.get(id.toLowerCase(Locale.ROOT));
        return theme != null && theme.enabled() ? theme : null;
    }

    public List<Theme> enabled() {
        List<Theme> list = new ArrayList<>();
        for (Theme theme : themes.values()) {
            if (theme.enabled()) {
                list.add(theme);
            }
        }
        return list;
    }

    public Collection<Theme> all() {
        return Collections.unmodifiableCollection(themes.values());
    }

    public boolean playerChoiceAvailable() {
        return allowPlayerThemes && enabled().size() > 1;
    }
}
