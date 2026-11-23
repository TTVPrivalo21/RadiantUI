package dev.privalo.ui.hud;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Top HUD made of boss bars whose titles are glyph/placeholder lines (hud.yml → bossbar).
 * Titles are re-rendered every N ticks and only re-sent when they actually change.
 */
public final class HudManager implements Listener {

    private enum ProgressSource { FIXED, HEALTH, FOOD, XP, AIR }

    private record BarSpec(String id, String format, BossBar.Color color, BossBar.Overlay overlay,
                           ProgressSource source, float fixed) {
    }

    private static final class PlayerHud {
        final List<BossBar> bars = new ArrayList<>();
        final List<Component> lastNames = new ArrayList<>();
    }

    private final RadiantUI plugin;
    private final Map<UUID, PlayerHud> huds = new HashMap<>();
    private final List<BarSpec> specs = new ArrayList<>();
    private final Set<String> disabledWorlds = new HashSet<>();
    private boolean enabled;
    private boolean defaultOn;
    private BukkitTask task;

    public HudManager(RadiantUI plugin) {
        this.plugin = plugin;
    }

    public void load() {
        stop();
        FileConfiguration config = plugin.configs().hud();
        specs.clear();
        disabledWorlds.clear();
        enabled = config.getBoolean("bossbar.enabled", true);
        defaultOn = config.getBoolean("bossbar.default-on", true);
        for (String world : config.getStringList("bossbar.disabled-worlds")) {
            disabledWorlds.add(world.toLowerCase(Locale.ROOT));
        }
        ConfigurationSection bars = config.getConfigurationSection("bossbar.bars");
        if (bars != null) {
            for (String id : bars.getKeys(false)) {
                ConfigurationSection s = bars.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                specs.add(new BarSpec(id,
                        s.getString("format", ""),
                        parseEnum(BossBar.Color.class, s.getString("color", "WHITE"), BossBar.Color.WHITE),
                        parseEnum(BossBar.Overlay.class, s.getString("overlay", "PROGRESS"), BossBar.Overlay.PROGRESS),
                        parseEnum(ProgressSource.class, s.getString("progress", "FIXED"), ProgressSource.FIXED),
                        (float) Math.max(0, Math.min(1, s.getDouble("fixed-progress", 1.0)))));
            }
        }
        if (enabled && !specs.isEmpty()) {
            long period = Math.max(1, config.getLong("bossbar.update-ticks", 10));
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerHud hud = huds.get(player.getUniqueId());
            if (hud != null) {
                update(player, hud);
            }
        }
    }

    public boolean isOn(Player player) {
        return enabled && plugin.prefs().hud(player, defaultOn);
    }

    public void setOn(Player player, boolean on) {
        plugin.prefs().setHud(player, on);
        refresh(player);
    }

    /** Shows/hides/rebuilds the HUD of a player according to config + preference + world. */
    public void refresh(Player player) {
        hide(player);
        if (!isOn(player) || specs.isEmpty() || disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
            return;
        }
        PlayerHud hud = new PlayerHud();
        for (BarSpec spec : specs) {
            BossBar bar = BossBar.bossBar(Component.empty(), spec.fixed(), spec.color(), spec.overlay());
            hud.bars.add(bar);
            hud.lastNames.add(Component.empty());
            player.showBossBar(bar);
        }
        huds.put(player.getUniqueId(), hud);
        update(player, hud);
    }

    private void update(Player player, PlayerHud hud) {
        Theme theme = plugin.themes().of(player);
        Map<String, String> values = plugin.placeholders().of(player);
        for (int i = 0; i < specs.size() && i < hud.bars.size(); i++) {
            BarSpec spec = specs.get(i);
            BossBar bar = hud.bars.get(i);
            Component name = plugin.text().render(player, theme, spec.format(), values);
            if (!name.equals(hud.lastNames.get(i))) {
                hud.lastNames.set(i, name);
                bar.name(name);
            }
            float progress = progress(player, spec);
            if (Math.abs(bar.progress() - progress) > 0.001f) {
                bar.progress(progress);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static float progress(Player player, BarSpec spec) {
        double value = switch (spec.source()) {
            case FIXED -> spec.fixed();
            case HEALTH -> player.getHealth() / Math.max(1, player.getMaxHealth());
            case FOOD -> player.getFoodLevel() / 20.0;
            case XP -> player.getExp();
            case AIR -> Math.max(0, player.getRemainingAir()) / (double) Math.max(1, player.getMaximumAir());
        };
        return (float) Math.max(0, Math.min(1, value));
    }

    public void hide(Player player) {
        PlayerHud hud = huds.remove(player.getUniqueId());
        if (hud != null) {
            for (BossBar bar : hud.bars) {
                player.hideBossBar(bar);
            }
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            hide(player);
        }
        huds.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        hide(event.getPlayer());
    }
}
