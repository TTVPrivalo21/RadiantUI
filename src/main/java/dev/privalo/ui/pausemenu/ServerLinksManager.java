package dev.privalo.ui.pausemenu;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ServerLinks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLinksSendEvent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Native Paper 1.21+ server links (pause menu → "Server Links" screen), themed per player,
 * plus optional clickable shortcuts in chat ({@code /ui links} and on join).
 */
public final class ServerLinksManager implements Listener {

    private record Link(String id, String name, String icon, String description, ServerLinks.Type type, URI uri, boolean chat, String shortName) {
    }

    private final RadiantUI plugin;
    private final List<Link> links = new ArrayList<>();
    private boolean enabled;
    private boolean replaceExisting;
    private boolean numbered;
    private String linkFormat;
    private boolean chatEnabled;
    private boolean chatOnJoin;
    private long chatOnJoinDelay;
    private String chatHeader;
    private String chatEntry;
    private String chatFooter;
    private boolean compactOnJoin;
    private String compactFormat;
    private String compactEntry;
    private String compactSeparator;

    public ServerLinksManager(RadiantUI plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration config = plugin.configs().pauseMenu();
        enabled = config.getBoolean("server-links.enabled", true);
        replaceExisting = config.getBoolean("server-links.replace-existing", false);
        numbered = config.getBoolean("server-links.numbered", false);
        linkFormat = config.getString("server-links.link-format", "{icon}{name}");
        chatEnabled = config.getBoolean("chat-shortcuts.enabled", true);
        chatOnJoin = config.getBoolean("chat-shortcuts.on-join", false);
        chatOnJoinDelay = Math.max(1, config.getLong("chat-shortcuts.on-join-delay-ticks", 60));
        chatHeader = config.getString("chat-shortcuts.header", "");
        chatEntry = config.getString("chat-shortcuts.entry", "{label}");
        chatFooter = config.getString("chat-shortcuts.footer", "");
        compactOnJoin = config.getString("chat-shortcuts.on-join-style", "compact").equalsIgnoreCase("compact");
        compactFormat = config.getString("chat-shortcuts.compact.format", "{entries}");
        compactEntry = config.getString("chat-shortcuts.compact.entry", "[{name}]");
        compactSeparator = config.getString("chat-shortcuts.compact.separator", " ");
        links.clear();
        ConfigurationSection section = config.getConfigurationSection("server-links.links");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            String url = s.getString("url", "");
            URI uri;
            try {
                uri = new URI(url);
                if (uri.getScheme() == null || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
                    throw new URISyntaxException(url, "la URL debe empezar por http:// o https://");
                }
            } catch (URISyntaxException ex) {
                plugin.getLogger().warning("pausemenu.yml → enlace '" + id + "' con URL inválida: " + url + " (" + ex.getReason() + ")");
                continue;
            }
            ServerLinks.Type type = null;
            String rawType = s.getString("type");
            if (rawType != null && !rawType.isBlank()) {
                try {
                    type = ServerLinks.Type.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("pausemenu.yml → enlace '" + id + "': tipo desconocido " + rawType + " (se usará el nombre)");
                }
            }
            links.add(new Link(id, s.getString("name", id), s.getString("icon", ""),
                    s.getString("description", ""), type, uri, s.getBoolean("chat", true),
                    s.getString("short-name", Character.toUpperCase(id.charAt(0)) + id.substring(1))));
        }
    }

    // ───────────────────────── native links ─────────────────────────

    /**
     * Only {@code getLinks()} is called directly: it exists on every version. The player is resolved by
     * reflection because the event changed shape:
     * <ul>
     *   <li>1.21.x: {@code extends PlayerEvent} → {@code getPlayer()}</li>
     *   <li>26.x: plain {@code Event} fired in the configuration phase → {@code getConnection().getProfile()}
     *       (no {@code Player} exists yet, so the global theme is used and links are re-sent on join)</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLinks(PlayerLinksSendEvent event) {
        if (!enabled) {
            return;
        }
        try {
            apply(EventPlayers.resolve(event), event.getLinks());
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("No se pudieron añadir los Server Links: " + ex);
        }
    }

    /** On 26.x the links were sent before a Player existed: re-send them with the player's own theme. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinResend(PlayerJoinEvent event) {
        if (enabled && EventPlayers.configurationPhaseLinks()) {
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.themes().hasPersonalTheme(player)) {
                    resend(player);
                }
            }, 5L);
        }
    }

    private void apply(Player player, ServerLinks target) {
        if (replaceExisting) {
            for (ServerLinks.ServerLink existing : new ArrayList<>(target.getLinks())) {
                target.removeLink(existing);
            }
        }
        Theme theme = plugin.themes().of(player);
        int index = 1;
        for (Link link : links) {
            if (link.type() != null) {
                // Typed links are translated by the client (e.g. "Report Bugs") and some get special buttons.
                target.addLink(link.type(), link.uri());
            } else {
                target.addLink(label(player, theme, link, index), link.uri());
            }
            index++;
        }
    }

    /** "1 · <glyph:chat> Discord" style label built from link-format. */
    private Component label(Player player, Theme theme, Link link, int index) {
        Map<String, String> values = new HashMap<>();
        values.put("icon", link.icon() == null || link.icon().isBlank() ? "" : "<glyph:" + link.icon() + "> ");
        values.put("name", link.name());
        values.put("index", String.valueOf(index));
        values.put("url", plugin.text().miniMessage().escapeTags(link.uri().toString()));
        String format = numbered ? "<gray>{index} ·</gray> " + linkFormat : linkFormat;
        return plugin.text().render(player, theme, format, values);
    }

    /** Re-sends links to online players (after reload or theme change). */
    public void resendAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            resend(player);
        }
    }

    public void resend(Player player) {
        ServerLinks copy = Bukkit.getServerLinks().copy();
        if (enabled) {
            apply(player, copy);
        }
        player.sendLinks(copy);
    }

    // ───────────────────────── chat shortcuts ─────────────────────────

    /** Sends clickable shortcuts to chat. Returns false if there is nothing to show. */
    public boolean sendChatShortcuts(Player player) {
        if (!chatEnabled) {
            return false;
        }
        Theme theme = plugin.themes().of(player);
        List<Component> lines = new ArrayList<>();
        for (Map<String, String> values : chatValues()) {
            lines.add(plugin.text().render(player, theme, chatEntry, values));
        }
        if (lines.isEmpty()) {
            return false;
        }
        if (chatHeader != null && !chatHeader.isBlank()) {
            player.sendMessage(plugin.text().render(player, theme, chatHeader, null));
        }
        lines.forEach(player::sendMessage);
        if (chatFooter != null && !chatFooter.isBlank()) {
            player.sendMessage(plugin.text().render(player, theme, chatFooter, null));
        }
        return true;
    }

    /** One clickable line with every link: [Discord] [Tienda] [Web]... (used on join by default). */
    public boolean sendCompactShortcuts(Player player) {
        if (!chatEnabled) {
            return false;
        }
        List<String> entries = new ArrayList<>();
        for (Map<String, String> values : chatValues()) {
            String entry = compactEntry;
            for (Map.Entry<String, String> v : values.entrySet()) {
                entry = entry.replace("{" + v.getKey() + "}", v.getValue());
            }
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            return false;
        }
        Map<String, String> values = new HashMap<>();
        values.put("entries", String.join(compactSeparator, entries));
        player.sendMessage(plugin.text().render(player, plugin.themes().of(player), compactFormat, values));
        return true;
    }

    /** Placeholder values for every link shown in chat. */
    private List<Map<String, String>> chatValues() {
        List<Map<String, String>> out = new ArrayList<>();
        int index = 1;
        for (Link link : links) {
            if (link.chat()) {
                Map<String, String> values = new LinkedHashMap<>();
                String safeUrl = link.uri().toString().replace("'", "%27");
                values.put("url", safeUrl);
                values.put("index", String.valueOf(index));
                values.put("icon", link.icon() == null || link.icon().isBlank() ? "" : "<glyph:" + link.icon() + "> ");
                values.put("name", link.type() != null && link.name().equals(link.id()) ? typeName(link.type()) : link.name());
                values.put("description", link.description() == null || link.description().isBlank()
                        ? safeUrl : link.description().replace("'", "\\'"));
                values.put("label", linkFormat.replace("{icon}", values.get("icon")).replace("{name}", values.get("name")));
                values.put("short", link.shortName());
                out.add(values);
            }
            index++;
        }
        return out;
    }

    private static String typeName(ServerLinks.Type type) {
        String raw = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!chatEnabled || !chatOnJoin) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                if (compactOnJoin) {
                    sendCompactShortcuts(player);
                } else {
                    sendChatShortcuts(player);
                }
            }
        }, chatOnJoinDelay);
    }

    public int count() {
        return links.size();
    }
}
