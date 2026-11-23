package dev.privalo.ui.text;

import dev.privalo.ui.hook.VaultHook;
import dev.privalo.ui.theme.ThemeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Builds the internal {placeholder} values shared by HUD, scoreboard and PlaceholderAPI. */
public final class Placeholders {

    private final VaultHook vault;
    private final ThemeManager themes;
    private int heartsCount = 10;
    private int segments = 10;
    private int heartSpacing = 0;
    private Attribute armorAttribute;

    public Placeholders(VaultHook vault, ThemeManager themes) {
        this.vault = vault;
        this.themes = themes;
    }

    public void configure(FileConfiguration hud) {
        heartsCount = Math.max(1, Math.min(40, hud.getInt("bars-style.hearts-count", 10)));
        segments = Math.max(1, Math.min(40, hud.getInt("bars-style.segments", 10)));
        heartSpacing = hud.getInt("bars-style.heart-spacing", 0);
        armorAttribute = resolveArmorAttribute();
    }

    /** Works on every 1.21.x/26.x: the attribute key was renamed from generic.armor to armor in 1.21.2. */
    private static Attribute resolveArmorAttribute() {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("armor"));
        if (attribute == null) {
            attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.armor"));
        }
        return attribute;
    }

    @SuppressWarnings("deprecation")
    public Map<String, String> of(Player player) {
        Map<String, String> map = new HashMap<>(32);
        double health = player.getHealth();
        double maxHealth = Math.max(1, player.getMaxHealth());
        int food = player.getFoodLevel();
        double armor = armor(player);
        Location loc = player.getLocation();

        map.put("player", player.getName());
        map.put("health", String.valueOf((int) Math.ceil(health)));
        map.put("max_health", String.valueOf((int) Math.ceil(maxHealth)));
        map.put("hearts", hearts(health / maxHealth));
        map.put("food", String.valueOf(food));
        map.put("food_bar", bar(food / 20.0));
        map.put("armor", String.valueOf((int) armor));
        map.put("armor_bar", bar(armor / 20.0));
        int maxAir = Math.max(1, player.getMaximumAir());
        map.put("air", String.valueOf((int) Math.ceil(Math.max(0, player.getRemainingAir()) * 10.0 / maxAir)));
        map.put("level", String.valueOf(player.getLevel()));
        map.put("xp_percent", String.valueOf((int) (player.getExp() * 100)));
        map.put("xp_bar", bar(player.getExp()));
        map.put("x", String.valueOf(loc.getBlockX()));
        map.put("y", String.valueOf(loc.getBlockY()));
        map.put("z", String.valueOf(loc.getBlockZ()));
        map.put("world", player.getWorld().getName());
        map.put("money", vault.formatted(player));
        map.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));
        map.put("max_online", String.valueOf(Bukkit.getMaxPlayers()));
        map.put("ping", String.valueOf(player.getPing()));
        map.put("theme", themes.of(player).displayName());
        map.put("time", worldTime(player.getWorld().getTime()));
        return map;
    }

    private double armor(Player player) {
        if (armorAttribute == null) {
            return 0;
        }
        AttributeInstance instance = player.getAttribute(armorAttribute);
        return instance == null ? 0 : instance.getValue();
    }

    /** Row of heart glyphs: full / half / empty. */
    public String hearts(double ratio) {
        int halves = (int) Math.ceil(Math.max(0, Math.min(1, ratio)) * heartsCount * 2);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < heartsCount; i++) {
            int remaining = halves - i * 2;
            String id = remaining >= 2 ? "heart" : remaining == 1 ? "heart_half" : "heart_empty";
            sb.append("<glyph:").append(id).append('>');
            if (heartSpacing != 0 && i < heartsCount - 1) {
                sb.append("<space:").append(heartSpacing).append('>');
            }
        }
        return sb.toString();
    }

    /** Segmented bar of bar_full/bar_empty glyphs. */
    public String bar(double ratio) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, ratio)) * segments);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            sb.append(i < filled ? "<glyph:bar_full>" : "<glyph:bar_empty>");
        }
        return sb.toString();
    }

    private static String worldTime(long ticks) {
        long hours = (ticks / 1000 + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
    }
}
