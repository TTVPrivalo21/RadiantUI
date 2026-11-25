package dev.privalo.ui;

import dev.privalo.ui.api.RadiantUIApi;
import dev.privalo.ui.api.event.ThemeChangeEvent;
import dev.privalo.ui.command.UiCommand;
import dev.privalo.ui.config.ConfigFiles;
import dev.privalo.ui.config.Messages;
import dev.privalo.ui.container.ContainerSkinManager;
import dev.privalo.ui.hook.PapiBridge;
import dev.privalo.ui.hook.VaultHook;
import dev.privalo.ui.hud.HudManager;
import dev.privalo.ui.pack.PackGenerator;
import dev.privalo.ui.pack.PackService;
import dev.privalo.ui.pausemenu.QuickMenu;
import dev.privalo.ui.pausemenu.ServerLinksManager;
import dev.privalo.ui.player.PlayerPrefs;
import dev.privalo.ui.scoreboard.ScoreboardManager;
import dev.privalo.ui.text.GlyphRegistry;
import dev.privalo.ui.text.Placeholders;
import dev.privalo.ui.text.TextService;
import dev.privalo.ui.update.UpdateChecker;
import dev.privalo.ui.update.VersionTracker;
import dev.privalo.ui.theme.Theme;
import dev.privalo.ui.theme.ThemeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * RadiantUI — complete UI redesign (HUD, containers, scoreboard, pause menu, server links)
 * with per-theme generated resource packs served by an embedded HTTP server.
 * Default theme: snoopy_classic (black & white cartoon).
 */
public final class RadiantUI extends JavaPlugin {

    private ConfigFiles configs;
    private PlayerPrefs prefs;
    private ThemeManager themes;
    private GlyphRegistry glyphs;
    private TextService text;
    private VaultHook vault;
    private Placeholders placeholders;
    private Messages messages;
    private PackService packs;
    private HudManager hud;
    private ScoreboardManager scoreboard;
    private ContainerSkinManager containers;
    private ServerLinksManager links;
    private QuickMenu quickMenu;
    private UpdateChecker updates;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        VersionTracker.check(getDataFolder(), getPluginMeta().getVersion(), getLogger());
        configs = new ConfigFiles(this);
        configs.load();

        prefs = new PlayerPrefs(this);
        themes = new ThemeManager(prefs, getLogger());
        themes.load(configs.main());

        glyphs = new GlyphRegistry(getLogger());
        glyphs.rebuild(themes.global(), configs.containers(), configs.pauseMenu(), overlaysFolder());
        text = new TextService(glyphs);

        vault = new VaultHook();
        vault.hook();
        PapiBridge.detect();
        placeholders = new Placeholders(vault, themes);
        placeholders.configure(configs.hud());
        messages = new Messages(this);

        packs = new PackService(this, new PackGenerator(getDataFolder(), configs, glyphs, text, getLogger()));
        hud = new HudManager(this);
        scoreboard = new ScoreboardManager(this);
        containers = new ContainerSkinManager(this);
        links = new ServerLinksManager(this);
        quickMenu = new QuickMenu(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(packs, this);
        pm.registerEvents(hud, this);
        pm.registerEvents(scoreboard, this);
        pm.registerEvents(containers, this);
        pm.registerEvents(links, this);
        updates = new UpdateChecker(this);
        pm.registerEvents(updates, this);

        PluginCommand command = getCommand("radiantui");
        if (command != null) {
            UiCommand executor = new UiCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        containers.load();
        links.load();
        quickMenu.load();
        hud.load();
        scoreboard.load();

        packs.startHttp();
        packs.rebuild().thenAccept(count -> packs.sendAll());

        PapiBridge.registerExpansion(this);
        RadiantUIApi.install(this);

        // Economy plugins sometimes register their Vault provider after us: hook again once the server is up.
        Bukkit.getScheduler().runTask(this, () -> {
            if (!vault.available() && vault.hook()) {
                getLogger().info("Economía de Vault detectada.");
            }
        });
        scheduleOraxenRebuild();
        updates.check();

        getLogger().info("RadiantUI habilitado en " + (System.currentTimeMillis() - start) + "ms · tema global: "
                + themes.global().id() + " · temas: " + themes.enabled().size()
                + " · PlaceholderAPI: " + (PapiBridge.available() ? "sí" : "no")
                + " · Vault: " + (vault.available() ? "sí" : "no"));
    }

    @Override
    public void onDisable() {
        RadiantUIApi.install(null);
        PapiBridge.unregisterExpansion();
        if (hud != null) {
            hud.stop();
        }
        if (scoreboard != null) {
            scoreboard.stop();
        }
        if (packs != null) {
            packs.stop();
        }
    }

    private File overlaysFolder() {
        return new File(getDataFolder(), "overlays");
    }

    /** Oraxen writes its pack a few seconds after startup; rebuild ours afterwards so the merge includes it. */
    private void scheduleOraxenRebuild() {
        if (!configs.main().getBoolean("pack.oraxen-merge.enabled", true)
                || !getServer().getPluginManager().isPluginEnabled("Oraxen")) {
            return;
        }
        long delay = Math.max(1, configs.main().getLong("pack.oraxen-merge.rebuild-delay-seconds", 15)) * 20L;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("Oraxen detectado: regenerando packs para combinar su contenido...");
            packs.rebuild().thenAccept(count -> packs.sendAll());
        }, delay);
    }

    /** Reloads every file, regenerates every pack and re-sends everything. Completes with the pack count. */
    public CompletableFuture<Integer> reloadAll() {
        try {
            configs.load();
            themes.load(configs.main());
            glyphs.rebuild(themes.global(), configs.containers(), configs.pauseMenu(), overlaysFolder());
            vault.hook();
            PapiBridge.detect();
            PapiBridge.registerExpansion(this);
            placeholders.configure(configs.hud());
            containers.load();
            links.load();
            quickMenu.load();
            hud.load();
            scoreboard.load();
            packs.startHttp();
            updates.check();
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
        return packs.rebuild().thenApply(count -> {
            packs.sendAll();
            links.resendAll();
            return count;
        });
    }

    /** Changes a player's personal theme (null = global). Fires {@link ThemeChangeEvent}. */
    public boolean changeTheme(Player player, String themeId) {
        String old = themes.hasPersonalTheme(player) ? themes.of(player).id() : null;
        ThemeChangeEvent event = new ThemeChangeEvent(player, old, themeId);
        getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        themes.setPlayerTheme(player, themeId);
        applyTheme(player);
        return true;
    }

    /** Changes the global theme, saves it to config.yml and updates everyone following it. */
    public boolean changeGlobalTheme(String themeId) {
        Theme theme = themes.find(themeId);
        if (theme == null) {
            return false;
        }
        themes.setGlobal(theme.id());
        configs.saveMain("theme.default", theme.id());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!themes.playerChoiceAvailable() || !themes.hasPersonalTheme(player)) {
                applyTheme(player);
            }
        }
        return true;
    }

    private void applyTheme(Player player) {
        packs.send(player, false);
        hud.refresh(player);
        scoreboard.refresh(player);
        links.resend(player);
    }

    // ── accessors ──

    public ConfigFiles configs() { return configs; }
    public PlayerPrefs prefs() { return prefs; }
    public ThemeManager themes() { return themes; }
    public GlyphRegistry glyphs() { return glyphs; }
    public TextService text() { return text; }
    public VaultHook vault() { return vault; }
    public Placeholders placeholders() { return placeholders; }
    public Messages messages() { return messages; }
    public PackService packs() { return packs; }
    public HudManager hud() { return hud; }
    public ScoreboardManager scoreboard() { return scoreboard; }
    public ContainerSkinManager containers() { return containers; }
    public ServerLinksManager links() { return links; }
    public QuickMenu quickMenu() { return quickMenu; }
    public UpdateChecker updates() { return updates; }
}
