package dev.privalo.ui.text;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Pixel-precise horizontal offsets using a "space" font provider. Any integer offset is
 * composed from power-of-two advances, so only 24 characters are needed for ±4095 px.
 */
public final class SpaceFont {

    public static final Key FONT = Key.key("radiantui", "space");
    private static final int BITS = 12;
    private static final char NEGATIVE_BASE = '';
    private static final char POSITIVE_BASE = '';

    private SpaceFont() {
    }

    /** Raw characters for an offset (must be rendered with {@link #FONT}). */
    public static String chars(int px) {
        if (px == 0) {
            return "";
        }
        int abs = Math.min(Math.abs(px), (1 << BITS) - 1);
        char base = px < 0 ? NEGATIVE_BASE : POSITIVE_BASE;
        StringBuilder sb = new StringBuilder();
        for (int bit = BITS - 1; bit >= 0; bit--) {
            if ((abs & (1 << bit)) != 0) {
                sb.append((char) (base + bit));
            }
        }
        return sb.toString();
    }

    public static Component component(int px) {
        String chars = chars(px);
        if (chars.isEmpty()) {
            return Component.empty();
        }
        return Component.text(chars).font(FONT);
    }

    /** JSON for assets/radiantui/font/space.json. */
    public static JsonObject fontJson() {
        JsonObject advances = new JsonObject();
        for (int bit = 0; bit < BITS; bit++) {
            advances.addProperty(String.valueOf((char) (NEGATIVE_BASE + bit)), -(1 << bit));
            advances.addProperty(String.valueOf((char) (POSITIVE_BASE + bit)), 1 << bit);
        }
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "space");
        provider.add("advances", advances);
        com.google.gson.JsonArray providers = new com.google.gson.JsonArray();
        providers.add(provider);
        JsonObject root = new JsonObject();
        root.add("providers", providers);
        return root;
    }
}
