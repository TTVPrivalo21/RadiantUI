package dev.privalo.ui.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/** Loads config.yml + hud.yml + containers.yml + scoreboard.yml + pausemenu.yml (with jar defaults). */
public final class ConfigFiles {

    private final JavaPlugin plugin;
    private FileConfiguration main;
    private FileConfiguration hud;
    private FileConfiguration containers;
    private FileConfiguration scoreboard;
    private FileConfiguration pauseMenu;

    public ConfigFiles(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Bump when the bundled defaults change in a way old files cannot express (v2 = black & white cartoon). */
    public static final int CONFIG_VERSION = 2;

    public void load() {
        for (String name : new String[]{"config.yml", "hud.yml", "containers.yml", "scoreboard.yml", "pausemenu.yml"}) {
            migrate(name);
        }
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        main = plugin.getConfig();
        hud = load("hud.yml");
        containers = load("containers.yml");
        scoreboard = load("scoreboard.yml");
        pauseMenu = load("pausemenu.yml");
        for (String dir : new String[]{"assets", "overlays", "themes"}) {
            File folder = new File(plugin.getDataFolder(), dir);
            if (!folder.isDirectory() && !folder.mkdirs()) {
                plugin.getLogger().warning("No se pudo crear la carpeta " + folder.getPath());
            }
        }
    }

    /** Moves an outdated file to "<name>.old-v<version>.yml" so the new defaults are written. */
    private void migrate(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.isFile()) {
            return;
        }
        int version = YamlConfiguration.loadConfiguration(file).getInt("config-version", 1);
        if (version >= CONFIG_VERSION) {
            return;
        }
        String base = name.substring(0, name.length() - ".yml".length());
        File backup = new File(plugin.getDataFolder(), base + ".old-v" + version + ".yml");
        for (int i = 2; backup.exists(); i++) {
            backup = new File(plugin.getDataFolder(), base + ".old-v" + version + "-" + i + ".yml");
        }
        if (file.renameTo(backup)) {
            plugin.getLogger().warning(name + " era de una versión anterior (v" + version + "): copia guardada en "
                    + backup.getName() + " y se generó el nuevo con el estilo blanco y negro.");
        } else {
            plugin.getLogger().warning("No se pudo mover " + name + " antiguo; bórralo para regenerarlo.");
        }
    }

    private FileConfiguration load(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(name)) {
            if (in != null) {
                config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "No se pudieron leer los valores por defecto de " + name, ex);
        }
        return config;
    }

    /** Persists a value in config.yml (used for /ui globaltheme). */
    public void saveMain(String path, Object value) {
        main.set(path, value);
        plugin.saveConfig();
    }

    public FileConfiguration main() { return main; }
    public FileConfiguration hud() { return hud; }
    public FileConfiguration containers() { return containers; }
    public FileConfiguration scoreboard() { return scoreboard; }
    public FileConfiguration pauseMenu() { return pauseMenu; }
}
