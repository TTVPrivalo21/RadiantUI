package dev.privalo.ui.command;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.theme.Theme;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /radiantui (/rui, /ui):
 * theme [tema|reset] [jugador] · globaltheme &lt;tema&gt; · hud &lt;on|off&gt; · scoreboard &lt;on|off&gt; · pack [jugador] · reload
 */
public final class UiCommand implements TabExecutor {

    private static final String ADMIN = "radiantui.admin";

    private final RadiantUI plugin;

    public UiCommand(RadiantUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.messages().send(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "theme", "tema" -> theme(sender, args);
            case "globaltheme" -> globalTheme(sender, args);
            case "hud" -> toggle(sender, args, true);
            case "scoreboard", "sb" -> toggle(sender, args, false);
            case "pack" -> pack(sender, args);
            case "links", "enlaces" -> links(sender);
            case "menu" -> menu(sender);
            case "reload" -> reload(sender);
            default -> plugin.messages().send(sender, "usage");
        }
        return true;
    }

    private void theme(CommandSender sender, String[] args) {
        if (!sender.hasPermission("radiantui.theme")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        if (args.length == 1) {
            listThemes(sender);
            return;
        }
        Player target;
        if (args.length >= 3) {
            if (!sender.hasPermission(ADMIN)) {
                plugin.messages().send(sender, "no-permission");
                return;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                plugin.messages().send(sender, "player-not-found", "name", args[2]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            plugin.messages().send(sender, "players-only");
            return;
        }
        if (!plugin.themes().playerChoiceAvailable()) {
            plugin.messages().send(sender, "theme-locked");
            return;
        }
        String wanted = args[1].toLowerCase(Locale.ROOT);
        boolean reset = wanted.equals("reset") || wanted.equals("global");
        Theme theme = reset ? plugin.themes().global() : plugin.themes().find(wanted);
        if (theme == null) {
            plugin.messages().send(sender, "theme-unknown", "id", args[1]);
            return;
        }
        if (!plugin.changeTheme(target, reset ? null : theme.id())) {
            return;
        }
        if (target != sender) {
            plugin.messages().send(sender, "theme-set-other", "player", target.getName(), "display", theme.displayName());
        }
        plugin.messages().send(target, reset ? "theme-reset" : "theme-set", "display", theme.displayName());
    }

    private void listThemes(CommandSender sender) {
        Theme current = sender instanceof Player player ? plugin.themes().of(player) : plugin.themes().global();
        plugin.messages().send(sender, "theme-list-header", "current", current.displayName());
        for (Theme theme : plugin.themes().enabled()) {
            plugin.messages().send(sender, "theme-list-entry",
                    "id", theme.id(),
                    "display", theme.displayName(),
                    "description", plugin.text().miniMessage().escapeTags(theme.description()).replace("'", "\\'"));
        }
    }

    private void globalTheme(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            listThemes(sender);
            return;
        }
        Theme theme = plugin.themes().find(args[1]);
        if (theme == null || !plugin.changeGlobalTheme(theme.id())) {
            plugin.messages().send(sender, "theme-unknown", "id", args[1]);
            return;
        }
        plugin.messages().send(sender, "theme-global-set", "display", theme.displayName());
    }

    private void toggle(CommandSender sender, String[] args, boolean hud) {
        if (!sender.hasPermission(hud ? "radiantui.hud" : "radiantui.scoreboard")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return;
        }
        boolean current = hud ? plugin.hud().isOn(player) : plugin.scoreboard().isOn(player);
        boolean on;
        if (args.length >= 2) {
            String value = args[1].toLowerCase(Locale.ROOT);
            on = value.equals("on") || value.equals("true") || value.equals("si") || value.equals("sí");
        } else {
            on = !current;
        }
        if (hud) {
            plugin.hud().setOn(player, on);
            plugin.messages().send(player, on ? "hud-on" : "hud-off");
        } else {
            plugin.scoreboard().setOn(player, on);
            plugin.messages().send(player, on ? "scoreboard-on" : "scoreboard-off");
        }
    }

    private void pack(CommandSender sender, String[] args) {
        if (!sender.hasPermission("radiantui.pack")) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission(ADMIN)) {
                plugin.messages().send(sender, "no-permission");
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.messages().send(sender, "player-not-found", "name", args[1]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            plugin.messages().send(sender, "players-only");
            return;
        }
        if (!plugin.packs().send(target, true)) {
            plugin.messages().send(sender, "pack-not-ready");
            return;
        }
        plugin.messages().send(sender, "pack-sending", "theme", plugin.themes().of(target).displayName());
    }

    /** Native quick-links dialog (1.21.6+/26.x); falls back to the chat list on older servers. */
    private void menu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return;
        }
        if (!plugin.quickMenu().open(player) && !plugin.links().sendChatShortcuts(player)) {
            plugin.messages().send(player, "links-empty");
        }
    }

    /** Clickable server-link shortcuts in chat (the native client groups them behind one pause-menu button). */
    private void links(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return;
        }
        if (!plugin.links().sendChatShortcuts(player)) {
            plugin.messages().send(player, "links-empty");
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) {
            plugin.messages().send(sender, "no-permission");
            return;
        }
        plugin.messages().send(sender, "reload-start");
        long start = System.currentTimeMillis();
        plugin.reloadAll().whenComplete((count, error) -> {
            if (error != null) {
                plugin.messages().send(sender, "reload-failed", "error", String.valueOf(error.getMessage()));
            } else {
                plugin.messages().send(sender, "reload-done",
                        "ms", String.valueOf(System.currentTimeMillis() - start),
                        "packs", String.valueOf(count));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        boolean admin = sender.hasPermission(ADMIN);
        if (args.length == 1) {
            options.add("theme");
            options.add("hud");
            options.add("scoreboard");
            options.add("pack");
            options.add("links");
            options.add("menu");
            if (admin) {
                options.add("globaltheme");
                options.add("reload");
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "theme", "tema" -> {
                    plugin.themes().enabled().forEach(t -> options.add(t.id()));
                    options.add("reset");
                }
                case "globaltheme" -> {
                    if (admin) {
                        plugin.themes().enabled().forEach(t -> options.add(t.id()));
                    }
                }
                case "hud", "scoreboard", "sb" -> {
                    options.add("on");
                    options.add("off");
                }
                case "pack" -> {
                    if (admin) {
                        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    }
                }
                default -> {
                    return options;
                }
            }
        } else if (args.length == 3 && admin && (args[0].equalsIgnoreCase("theme") || args[0].equalsIgnoreCase("tema"))) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }
}
