package dev.privalo.ui.theme;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Colour set of a theme. Colours are stored as ARGB ints so they can be used both
 * for painting textures and for MiniMessage colour tags.
 */
public final class Palette {

    public static final String BACKGROUND = "background";
    public static final String BACKGROUND_ALT = "background-alt";
    public static final String DARK = "dark";
    public static final String OUTLINE = "outline";
    public static final String ACCENT = "accent";
    public static final String ACCENT_LIGHT = "accent-light";
    public static final String ACCENT_PALE = "accent-pale";
    public static final String TEXT = "text";
    public static final String HIGHLIGHT = "highlight";
    public static final String SHADE = "shade";

    /** Defaults = snoopy_classic (white paper, black ink, greys). */
    private static final Map<String, Integer> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(BACKGROUND, 0xFFFFFFFF);
        DEFAULTS.put(BACKGROUND_ALT, 0xFFE5E7EB);
        DEFAULTS.put(DARK, 0xFF18181B);
        DEFAULTS.put(OUTLINE, 0xFF09090B);
        DEFAULTS.put(ACCENT, 0xFF18181B);
        DEFAULTS.put(ACCENT_LIGHT, 0xFF52525B);
        DEFAULTS.put(ACCENT_PALE, 0xFFE5E7EB);
        DEFAULTS.put(TEXT, 0xFF18181B);
        DEFAULTS.put(HIGHLIGHT, 0xFFFFFFFF);
        DEFAULTS.put(SHADE, 0xFF52525B);
    }

    private final Map<String, Integer> colors;

    private Palette(Map<String, Integer> colors) {
        this.colors = Collections.unmodifiableMap(colors);
    }

    public static Palette snoopyClassic() {
        return new Palette(new LinkedHashMap<>(DEFAULTS));
    }

    public static Palette fromConfig(ConfigurationSection section) {
        Map<String, Integer> map = new LinkedHashMap<>(DEFAULTS);
        if (section != null) {
            for (String key : DEFAULTS.keySet()) {
                String raw = section.getString(key);
                if (raw != null) {
                    map.put(key, parse(raw, DEFAULTS.get(key)));
                }
            }
        }
        return new Palette(map);
    }

    /** Parses #RRGGBB or #AARRGGBB. */
    public static int parse(String raw, int fallback) {
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            if (s.length() == 6) {
                return 0xFF000000 | Integer.parseInt(s, 16);
            }
            if (s.length() == 8) {
                return (int) Long.parseLong(s, 16);
            }
        } catch (NumberFormatException ignored) {
            return fallback;
        }
        return fallback;
    }

    public int get(String key) {
        Integer value = colors.get(key);
        return value != null ? value : DEFAULTS.getOrDefault(key, 0xFFFF00FF);
    }

    public TextColor text(String key) {
        return TextColor.color(get(key) & 0xFFFFFF);
    }

    public String hex(String key) {
        return String.format(Locale.ROOT, "#%06X", get(key) & 0xFFFFFF);
    }

    public Map<String, Integer> asMap() {
        return colors;
    }

    public int background() { return get(BACKGROUND); }
    public int backgroundAlt() { return get(BACKGROUND_ALT); }
    public int dark() { return get(DARK); }
    public int outline() { return get(OUTLINE); }
    public int accent() { return get(ACCENT); }
    public int accentLight() { return get(ACCENT_LIGHT); }
    public int accentPale() { return get(ACCENT_PALE); }
    public int textColor() { return get(TEXT); }
    public int highlight() { return get(HIGHLIGHT); }
    public int shade() { return get(SHADE); }
}
