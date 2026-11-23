package dev.privalo.ui.container;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.text.Glyph;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Re-skins container GUIs by replacing their title with
 * {@code <overlay:chest_N:-8>} + formatted title (Paper's {@link InventoryOpenEvent#titleOverride(Component)}).
 */
public final class ContainerSkinManager implements Listener {

    private record Skin(String id, Pattern match, String overlay, String titleFormat) {
    }

    private final RadiantUI plugin;
    private final List<Skin> skins = new ArrayList<>();
    private Skin vanillaSkin;
    private int xOffset = -8;

    public ContainerSkinManager(RadiantUI plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.configs().containers();
        xOffset = config.getInt("overlay.x-offset", -8);
        skins.clear();
        ConfigurationSection section = config.getConfigurationSection("skins");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    skins.add(new Skin(id,
                            Pattern.compile(s.getString("match", "^$")),
                            s.getString("overlay", "auto"),
                            s.getString("title-format", "<text>{title}")));
                } catch (PatternSyntaxException ex) {
                    plugin.getLogger().warning("containers.yml → skin '" + id + "': regex inválida (" + ex.getDescription() + ")");
                }
            }
        }
        vanillaSkin = config.getBoolean("vanilla-chests.enabled", false)
                ? new Skin("vanilla", null, config.getString("vanilla-chests.overlay", "auto"),
                config.getString("vanilla-chests.title-format", "<text>{title}"))
                : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        Component original = event.titleOverride() != null ? event.titleOverride() : event.getView().title();
        String plain = PlainTextComponentSerializer.plainText().serialize(original);

        Skin skin = null;
        for (Skin candidate : skins) {
            if (candidate.match().matcher(plain).matches()) {
                skin = candidate;
                break;
            }
        }
        int rows = rows(top);
        if (skin == null && vanillaSkin != null && rows > 0 && isWorldContainer(top)) {
            skin = vanillaSkin;
        }
        if (skin == null) {
            return;
        }
        event.titleOverride(build(player, skin, plain, rows));
    }

    /** Builds an overlay title for any GUI (also used by the public API). */
    public Component title(Player player, String overlayId, int rows, String titleMiniMessage) {
        Theme theme = plugin.themes().of(player);
        Component overlay = overlayComponent(overlayId, rows);
        return Component.text().append(overlay).append(plugin.text().parse(titleMiniMessage, theme)).build();
    }

    private Component build(Player player, Skin skin, String plainTitle, int rows) {
        Theme theme = plugin.themes().of(player);
        String escaped = plugin.text().miniMessage().escapeTags(plainTitle);
        Map<String, String> values = plugin.placeholders().of(player);
        values.put("title", escaped);
        Component title = plugin.text().render(player, theme, skin.titleFormat(), values);
        return Component.text().append(overlayComponent(skin.overlay(), rows)).append(title).build();
    }

    private Component overlayComponent(String overlayId, int rows) {
        String id = overlayId == null ? "auto" : overlayId.toLowerCase(Locale.ROOT);
        if (id.equals("auto")) {
            if (rows <= 0) {
                return Component.empty();
            }
            id = "chest_" + Math.min(6, rows);
        }
        if (id.equals("none")) {
            return Component.empty();
        }
        Glyph glyph = plugin.glyphs().get(id);
        return glyph == null ? Component.empty() : plugin.text().overlay(id, xOffset);
    }

    private static int rows(Inventory inventory) {
        InventoryType type = inventory.getType();
        return switch (type) {
            case CHEST -> inventory.getSize() / 9;
            case ENDER_CHEST, BARREL -> 3;
            default -> 0;
        };
    }

    private static boolean isWorldContainer(Inventory inventory) {
        if (inventory.getType() == InventoryType.ENDER_CHEST) {
            return true;
        }
        InventoryHolder holder = inventory.getHolder(false);
        return holder instanceof Container || holder instanceof DoubleChest;
    }
}
