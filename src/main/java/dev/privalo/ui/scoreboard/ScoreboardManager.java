package dev.privalo.ui.scoreboard;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.theme.Theme;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player sidebar using Paper's {@link NumberFormat#blank()} (no red numbers) and
 * {@link Score#customName(Component)} lines. Only changed lines are re-sent.
 */
public final class ScoreboardManager implements Listener {

    private static final int MAX_LINES = 15;
    private static final String OBJECTIVE = "radiantui";

    private static final class Board {
        final Scoreboard scoreboard;
        final Objective objective;
        final Scoreboard previous;
        final Component[] lines = new Component[MAX_LINES];
        Component title;
        int frame;

        Board(Scoreboard scoreboard, Objective objective, Scoreboard previous) {
            this.scoreboard = scoreboard;
            this.objective = objective;
            this.previous = previous;
        }
    }

    private final RadiantUI plugin;
    private final Map<UUID, Board> boards = new HashMap<>();
    private final List<String> titleFrames = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private final Set<String> disabledWorlds = new HashSet<>();
    private boolean enabled;
    private boolean defaultOn;
    private boolean restorePrevious;
    private BukkitTask task;

    public ScoreboardManager(RadiantUI plugin) {
        this.plugin = plugin;
    }

    public void load() {
        stop();
        FileConfiguration config = plugin.configs().scoreboard();
        enabled = config.getBoolean("enabled", true);
        defaultOn = config.getBoolean("default-on", true);
        restorePrevious = config.getBoolean("restore-previous", true);
        titleFrames.clear();
        if (config.isList("title")) {
            titleFrames.addAll(config.getStringList("title"));
        } else {
            titleFrames.add(config.getString("title", "<accent>RadiantUI"));
        }
        if (titleFrames.isEmpty()) {
            titleFrames.add("<accent>RadiantUI");
        }
        lines.clear();
        List<String> configured = config.getStringList("lines");
        lines.addAll(configured.subList(0, Math.min(MAX_LINES, configured.size())));
        if (configured.size() > MAX_LINES) {
            plugin.getLogger().warning("scoreboard.yml tiene " + configured.size() + " líneas; solo se muestran " + MAX_LINES + ".");
        }
        disabledWorlds.clear();
        for (String world : config.getStringList("disabled-worlds")) {
            disabledWorlds.add(world.toLowerCase(Locale.ROOT));
        }
        if (enabled) {
            long period = Math.max(1, config.getLong("update-ticks", 20));
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public boolean isOn(Player player) {
        return enabled && plugin.prefs().scoreboard(player, defaultOn);
    }

    public void setOn(Player player, boolean on) {
        plugin.prefs().setScoreboard(player, on);
        refresh(player);
    }

    public void refresh(Player player) {
        boolean shouldShow = isOn(player) && !disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
        Board board = boards.get(player.getUniqueId());
        if (!shouldShow) {
            remove(player);
            return;
        }
        if (board == null || player.getScoreboard() != board.scoreboard) {
            board = create(player);
        } else {
            board.title = null;
            java.util.Arrays.fill(board.lines, null);
        }
        update(player, board);
    }

    private Board create(Player player) {
        remove(player);
        Scoreboard previous = player.getScoreboard();
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(OBJECTIVE, Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        Board board = new Board(scoreboard, objective, previous);
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(scoreboard);
        return board;
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Board board = boards.get(player.getUniqueId());
            if (board != null) {
                update(player, board);
            }
        }
    }

    private void update(Player player, Board board) {
        Theme theme = plugin.themes().of(player);
        Map<String, String> values = plugin.placeholders().of(player);

        String frame = titleFrames.get(board.frame % titleFrames.size());
        board.frame = (board.frame + 1) % titleFrames.size();
        Component title = plugin.text().render(player, theme, frame, values);
        if (!title.equals(board.title)) {
            board.title = title;
            board.objective.displayName(title);
        }

        int size = lines.size();
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = entry(i);
            if (i >= size) {
                if (board.lines[i] != null) {
                    board.scoreboard.resetScores(entry);
                    board.lines[i] = null;
                }
                continue;
            }
            Component line = plugin.text().render(player, theme, lines.get(i), values);
            if (!line.equals(board.lines[i])) {
                Score score = board.objective.getScore(entry);
                score.setScore(size - i);
                score.customName(line);
                score.numberFormat(NumberFormat.blank());
                board.lines[i] = line;
            }
        }
    }

    private static String entry(int index) {
        return "rui-" + (index < 10 ? "0" : "") + index;
    }

    private void remove(Player player) {
        Board board = boards.remove(player.getUniqueId());
        if (board == null) {
            return;
        }
        if (player.getScoreboard() == board.scoreboard) {
            Scoreboard target = restorePrevious && board.previous != null
                    ? board.previous
                    : Bukkit.getScoreboardManager().getMainScoreboard();
            player.setScoreboard(target);
        }
        board.objective.unregister();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            remove(player);
        }
        boards.clear();
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
        boards.remove(event.getPlayer().getUniqueId());
    }
}
