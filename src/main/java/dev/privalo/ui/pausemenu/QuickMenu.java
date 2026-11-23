package dev.privalo.ui.pausemenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Native quick-links menu for 1.21.6+ / 26.x clients (no mods): writes a small data pack with a
 * {@code minecraft:multi_action} dialog and adds it to the {@code pause_screen_additions} and
 * {@code quick_actions} dialog tags. Result: a real button in the ESC menu (and on the Quick Actions key)
 * that opens a menu of URL/command buttons. On older servers it does nothing and /ui menu falls back to chat.
 */
public final class QuickMenu {

    public static final String DIALOG_ID = "radiantui:quick_menu";
    private static final String PACK_FOLDER = "radiantui";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private record Button(String label, String tooltip, String url, String command, int width) {
    }

    private final RadiantUI plugin;
    private boolean enabled;
    private boolean dialogsSupported;
    private boolean active;

    public QuickMenu(RadiantUI plugin) {
        this.plugin = plugin;
    }

    /** Writes/updates (or removes) the data pack. Safe to call on reload. */
    public void load() {
        FileConfiguration config = plugin.configs().pauseMenu();
        enabled = config.getBoolean("quick-menu.enabled", true);
        dialogsSupported = classExists("io.papermc.paper.dialog.Dialog");
        File folder = packFolder();
        if (folder == null) {
            active = false;
            return;
        }
        if (!enabled || !dialogsSupported || !config.getBoolean("quick-menu.install-datapack", true)) {
            if (!dialogsSupported && enabled) {
                plugin.getLogger().info("Menú rápido nativo (diálogos) no disponible: requiere servidor 1.21.6+ / 26.x. Se usará el chat.");
            }
            removePack(folder);
            active = false;
            return;
        }
        boolean existedBefore = new File(folder, "pack.mcmeta").isFile();
        boolean changed;
        try {
            changed = writePack(folder, config);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo escribir el datapack del menú rápido: " + ex.getMessage());
            active = false;
            return;
        }
        active = existedBefore;
        if (!changed) {
            return;
        }
        if (config.getBoolean("quick-menu.auto-reload", true)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("Datapack del menú rápido actualizado: recargando datos (/minecraft:reload)...");
                Bukkit.reloadData();
                active = true;
            });
        } else {
            plugin.getLogger().info("Datapack del menú rápido actualizado en " + folder.getPath()
                    + ": usa /minecraft:reload o reinicia para aplicarlo.");
        }
        if (!existedBefore) {
            plugin.getLogger().info("Primera instalación del menú rápido: si el botón no aparece en ESC, reinicia el servidor una vez "
                    + "y vuelve a entrar (los diálogos se envían al conectar).");
        }
    }

    /** Opens the dialog for the player. False when not available (caller falls back to chat). */
    public boolean open(Player player) {
        if (!enabled || !dialogsSupported || !active) {
            return false;
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:dialog show " + player.getUniqueId() + " " + DIALOG_ID);
    }

    public boolean available() {
        return enabled && dialogsSupported && active;
    }

    // ───────────────────────── data pack ─────────────────────────

    private File packFolder() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            return null;
        }
        return new File(new File(worlds.get(0).getWorldFolder(), "datapacks"), PACK_FOLDER);
    }

    private boolean writePack(File folder, FileConfiguration config) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", GSON.toJson(packMeta()));
        files.put("data/radiantui/dialog/quick_menu.json", GSON.toJson(dialog(config)));
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        JsonArray values = new JsonArray();
        values.add(DIALOG_ID);
        tag.add("values", values);
        String tagJson = GSON.toJson(tag);
        String pauseTag = "data/minecraft/tags/dialog/pause_screen_additions.json";
        String quickTag = "data/minecraft/tags/dialog/quick_actions.json";
        if (config.getBoolean("quick-menu.pause-button", true)) {
            files.put(pauseTag, tagJson);
        }
        if (config.getBoolean("quick-menu.quick-actions-key", true)) {
            files.put(quickTag, tagJson);
        }
        boolean changed = false;
        for (String stale : new String[]{pauseTag, quickTag}) {
            if (!files.containsKey(stale)) {
                changed |= Files.deleteIfExists(folder.toPath().resolve(stale));
            }
        }
        for (Map.Entry<String, String> e : files.entrySet()) {
            Path target = folder.toPath().resolve(e.getKey());
            byte[] data = e.getValue().getBytes(StandardCharsets.UTF_8);
            if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), data)) {
                continue;
            }
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            changed = true;
        }
        return changed;
    }

    private static JsonObject packMeta() {
        JsonObject supported = new JsonObject();
        supported.addProperty("min_inclusive", 80);
        supported.addProperty("max_inclusive", 999);
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "RadiantUI - menú rápido de enlaces (generado automáticamente)");
        pack.addProperty("pack_format", 80);
        pack.add("supported_formats", supported);
        pack.addProperty("min_format", 80);
        pack.addProperty("max_format", 999);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }

    private JsonObject dialog(FileConfiguration config) {
        Theme theme = plugin.themes().global();
        JsonObject dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");
        dialog.add("title", component(config.getString("quick-menu.title", "<white><bold>Enlaces rápidos"), theme));
        dialog.add("external_title", component(config.getString("quick-menu.pause-button-label", "<white>★ Enlaces ★"), theme));
        dialog.addProperty("can_close_with_escape", true);
        dialog.addProperty("after_action", "close");

        JsonArray body = new JsonArray();
        String bodyText = config.getString("quick-menu.body", "");
        String banner = config.getBoolean("quick-menu.banner", true) ? "<glyph:links_banner>" : "";
        if (!banner.isEmpty() || (bodyText != null && !bodyText.isBlank())) {
            JsonObject message = new JsonObject();
            message.addProperty("type", "minecraft:plain_message");
            String text = banner + (banner.isEmpty() || bodyText == null || bodyText.isBlank() ? "" : "<newline>") + (bodyText == null ? "" : bodyText);
            message.add("contents", component(text, theme));
            message.addProperty("width", 300);
            body.add(message);
        }
        dialog.add("body", body);

        JsonArray actions = new JsonArray();
        for (Button button : buttons(config)) {
            JsonObject action = new JsonObject();
            action.add("label", component(button.label(), theme));
            if (button.tooltip() != null && !button.tooltip().isBlank()) {
                action.add("tooltip", component(button.tooltip(), theme));
            }
            action.addProperty("width", button.width());
            JsonObject click = new JsonObject();
            if (button.url() != null && !button.url().isBlank()) {
                click.addProperty("type", "open_url");
                click.addProperty("url", button.url());
            } else {
                String command = button.command().startsWith("/") ? button.command().substring(1) : button.command();
                click.addProperty("type", "run_command");
                click.addProperty("command", command);
            }
            action.add("action", click);
            actions.add(action);
        }
        if (actions.isEmpty()) {
            JsonObject fallback = new JsonObject();
            fallback.add("label", component("<white>/ui links", theme));
            JsonObject click = new JsonObject();
            click.addProperty("type", "run_command");
            click.addProperty("command", "ui links");
            fallback.add("action", click);
            actions.add(fallback);
        }
        dialog.add("actions", actions);
        dialog.addProperty("columns", Math.max(1, config.getInt("quick-menu.columns", 2)));
        String exit = config.getString("quick-menu.close-label", "<gray>Volver");
        if (exit != null && !exit.isBlank()) {
            JsonObject exitAction = new JsonObject();
            exitAction.add("label", component(exit, theme));
            exitAction.addProperty("width", 200);
            dialog.add("exit_action", exitAction);
        }
        return dialog;
    }

    /** Server links (optional) followed by the extra buttons of quick-menu.buttons. */
    private List<Button> buttons(FileConfiguration config) {
        List<Button> out = new ArrayList<>();
        int width = Math.max(40, Math.min(1024, config.getInt("quick-menu.button-width", 150)));
        if (config.getBoolean("quick-menu.include-server-links", true)) {
            ConfigurationSection links = config.getConfigurationSection("server-links.links");
            if (links != null) {
                for (String id : links.getKeys(false)) {
                    ConfigurationSection s = links.getConfigurationSection(id);
                    if (s == null || !s.getBoolean("quick-menu", true)) {
                        continue;
                    }
                    String url = s.getString("url", "");
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        continue;
                    }
                    String icon = s.getString("icon", "");
                    String label = (icon.isBlank() ? "" : "<glyph:" + icon + "> ") + s.getString("name", id);
                    out.add(new Button(label, s.getString("description", url), url, null, width));
                }
            }
        }
        ConfigurationSection extra = config.getConfigurationSection("quick-menu.buttons");
        if (extra != null) {
            for (String id : extra.getKeys(false)) {
                ConfigurationSection s = extra.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                String url = s.getString("url", "");
                String command = s.getString("command", "");
                if (url.isBlank() && command.isBlank()) {
                    plugin.getLogger().warning("pausemenu.yml → quick-menu.buttons." + id + " necesita url o command.");
                    continue;
                }
                out.add(new Button(s.getString("label", id), s.getString("tooltip", ""), url, command,
                        Math.max(40, Math.min(1024, s.getInt("width", width)))));
            }
        }
        return out;
    }

    private JsonElement component(String miniMessage, Theme theme) {
        Component component = plugin.text().parse(miniMessage, theme);
        return GsonComponentSerializer.gson().serializeToTree(component);
    }

    private void removePack(File folder) {
        if (!folder.isDirectory()) {
            return;
        }
        try (var walk = Files.walk(folder.toPath())) {
            for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
            plugin.getLogger().info("Datapack del menú rápido eliminado (quick-menu desactivado). Aplica con /minecraft:reload.");
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo borrar " + folder.getPath() + ": " + ex.getMessage());
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, QuickMenu.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }
}
