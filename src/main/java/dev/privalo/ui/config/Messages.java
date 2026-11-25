package dev.privalo.ui.config;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Chat messages from config.yml → messages, themed for the receiving player. */
public final class Messages {

    private final RadiantUI plugin;

    public Messages(RadiantUI plugin) {
        this.plugin = plugin;
    }

    /** @param replacements alternating key/value pairs for {key} placeholders */
    public Component get(CommandSender sender, String key, String... replacements) {
        // getString(path) without an explicit default falls back to the jar's config.yml,
        // so messages added in new versions work with an old config.yml too.
        String raw = plugin.configs().main().getString("messages." + key);
        if (raw == null) {
            raw = "<white>" + key;
        }
        String prefix = plugin.configs().main().getString("messages.prefix");
        if (prefix == null) {
            prefix = "";
        }
        raw = raw.replace("<prefix>", prefix);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        Theme theme = sender instanceof Player player ? plugin.themes().of(player) : plugin.themes().global();
        return plugin.text().parse(raw, theme);
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(get(sender, key, replacements));
    }
}
