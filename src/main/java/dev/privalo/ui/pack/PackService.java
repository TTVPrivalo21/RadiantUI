package dev.privalo.ui.pack;

import dev.privalo.ui.RadiantUI;
import dev.privalo.ui.http.PackHttpServer;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;

/** Owns the generated packs, the HTTP server and sends the right pack to every player. */
public final class PackService implements Listener {

    /** Same id for every theme: switching theme replaces the previous RadiantUI pack on the client. */
    public static final UUID PACK_ID = UUID.nameUUIDFromBytes("radiantui:resource-pack".getBytes(StandardCharsets.UTF_8));

    private final RadiantUI plugin;
    private final PackGenerator generator;
    private final PackHttpServer http;
    private final Set<UUID> notifyOnLoad = ConcurrentHashMap.newKeySet();
    private volatile Map<String, BuiltPack> byTheme = Collections.emptyMap();
    private volatile Map<String, BuiltPack> byFile = Collections.emptyMap();
    private volatile boolean building;
    private String baseUrl = "";

    public PackService(RadiantUI plugin, PackGenerator generator) {
        this.plugin = plugin;
        this.generator = generator;
        this.http = new PackHttpServer(plugin.getLogger());
    }

    // ───────────────────────── HTTP ─────────────────────────

    public void startHttp() {
        FileConfiguration config = plugin.configs().main();
        if (!config.getBoolean("pack.enabled", true) || !config.getBoolean("http.enabled", true)) {
            http.stop();
            baseUrl = "";
            return;
        }
        String bind = config.getString("http.bind", "0.0.0.0");
        int port = config.getInt("http.port", 8163);
        http.start(bind, port, config.getInt("http.threads", 2), name -> byFile.get(name));
        String configured = config.getString("http.public-url", "");
        if (configured != null && !configured.isBlank()) {
            baseUrl = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        } else {
            String host = detectHost();
            baseUrl = "http://" + host + ":" + port;
            plugin.getLogger().info("URL automática del pack: " + baseUrl + "  (configura http.public-url si los jugadores no pueden descargarlo)");
        }
    }

    private static String detectHost() {
        String ip = Bukkit.getIp();
        if (ip != null && !ip.isBlank() && !ip.equals("0.0.0.0")) {
            return ip;
        }
        // UDP "connect" picks the outgoing interface without sending any packet
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("192.0.2.1"), 9);
            InetAddress local = socket.getLocalAddress();
            if (local != null && !local.isAnyLocalAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignored) {
            // fall through to the loopback-safe default below
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }

    public void stop() {
        http.stop();
    }

    // ───────────────────────── build ─────────────────────────

    /** Regenerates every enabled theme pack asynchronously; completes on the main thread with the pack count. */
    public CompletableFuture<Integer> rebuild() {
        if (!plugin.configs().main().getBoolean("pack.enabled", true)) {
            byTheme = Collections.emptyMap();
            byFile = Collections.emptyMap();
            return CompletableFuture.completedFuture(0);
        }
        building = true;
        Executor async = r -> Bukkit.getScheduler().runTaskAsynchronously(plugin, r);
        Executor main = r -> {
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, r);
            }
        };
        var themes = plugin.themes().enabled();
        return CompletableFuture.supplyAsync(() -> {
            Map<String, BuiltPack> built = new HashMap<>();
            for (Theme theme : themes) {
                try {
                    long start = System.currentTimeMillis();
                    BuiltPack pack = generator.build(theme);
                    built.put(theme.id(), pack);
                    plugin.getLogger().info("Pack '" + theme.id() + "' generado: " + pack.entries() + " archivos, "
                            + (pack.zip().length / 1024) + " KB, SHA-1 " + pack.sha1Hex() + " (" + (System.currentTimeMillis() - start) + "ms)");
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Error generando el pack del tema '" + theme.id() + "'", ex);
                }
            }
            return built;
        }, async).thenApplyAsync(built -> {
            Map<String, BuiltPack> files = new HashMap<>();
            for (BuiltPack pack : built.values()) {
                files.put(pack.fileName(), pack);
            }
            byTheme = Map.copyOf(built);
            byFile = Map.copyOf(files);
            building = false;
            return built.size();
        }, main).whenComplete((count, error) -> {
            if (error != null) {
                building = false;
            }
        });
    }

    public boolean building() {
        return building;
    }

    public BuiltPack pack(Theme theme) {
        return byTheme.get(theme.id());
    }

    public Map<String, BuiltPack> packs() {
        return byTheme;
    }

    public String url(BuiltPack pack) {
        FileConfiguration config = plugin.configs().main();
        if (!config.getBoolean("http.enabled", true)) {
            String external = config.getString("pack.external-url", "");
            if (external == null || external.isBlank()) {
                return null;
            }
            return external.replace("{theme}", pack.theme().id()).replace("{sha1}", pack.sha1Hex());
        }
        return baseUrl + "/pack/" + pack.fileName();
    }

    // ───────────────────────── sending ─────────────────────────

    /** Sends the pack of the player's theme. Returns false when no pack/URL is available yet. */
    public boolean send(Player player, boolean notifyWhenLoaded) {
        Theme theme = plugin.themes().of(player);
        BuiltPack pack = pack(theme);
        if (pack == null) {
            return false;
        }
        String url = url(pack);
        if (url == null) {
            plugin.getLogger().warning("http.enabled=false y pack.external-url vacío: no se puede enviar el pack.");
            return false;
        }
        FileConfiguration config = plugin.configs().main();
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), pack.sha1Hex());
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(config.getBoolean("pack.required", false))
                .prompt(plugin.text().parse(config.getString("pack.prompt", ""), theme))
                .replace(false)
                .build();
        if (notifyWhenLoaded) {
            notifyOnLoad.add(player.getUniqueId());
        }
        player.sendResourcePacks(request);
        return true;
    }

    public void sendAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.configs().main().getBoolean("pack.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        long delay = Math.max(1, plugin.configs().main().getLong("pack.send-delay-ticks", 20));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                send(player, false);
            }
        }, delay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        notifyOnLoad.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (!PACK_ID.equals(event.getID())) {
            return;
        }
        Player player = event.getPlayer();
        FileConfiguration config = plugin.configs().main();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> {
                if (notifyOnLoad.remove(player.getUniqueId())) {
                    plugin.messages().send(player, "pack-loaded");
                }
                plugin.hud().refresh(player);
                plugin.scoreboard().refresh(player);
            }
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> {
                notifyOnLoad.remove(player.getUniqueId());
                if (event.getStatus() != PlayerResourcePackStatusEvent.Status.DECLINED) {
                    plugin.getLogger().warning("El pack falló para " + player.getName() + ": " + event.getStatus()
                            + " (revisa http.public-url y que el puerto " + config.getInt("http.port", 8163) + " esté abierto)");
                }
                if (config.getBoolean("pack.kick-on-decline", false)) {
                    player.kick(plugin.text().parse(config.getString("pack.kick-message", ""), plugin.themes().of(player)));
                }
            }
            default -> {
                // ACCEPTED / DOWNLOADED are intermediate states: nothing to do until it loads
            }
        }
    }
}
