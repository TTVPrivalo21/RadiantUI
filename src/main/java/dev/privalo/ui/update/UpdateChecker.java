package dev.privalo.ui.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.privalo.ui.RadiantUI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous update checker (GitHub Releases or SpigotMC via Spiget). Every network or parsing
 * problem (no internet, private repo, no releases yet / 404, timeout...) is ignored silently.
 */
public final class UpdateChecker implements Listener {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String ADMIN_PERMISSION = "radiantui.admin";

    /** Result of a successful check that found a newer version. */
    private record Update(String version, String url, String profile) {
    }

    private final RadiantUI plugin;
    private volatile Update available;
    private volatile boolean notifyAdmins;

    public UpdateChecker(RadiantUI plugin) {
        this.plugin = plugin;
    }

    /** Starts a new check (never blocks the main thread). */
    public void check() {
        FileConfiguration config = plugin.configs().main();
        available = null;
        notifyAdmins = config.getBoolean("update-checker.notify-admins-on-join", true);
        if (!config.getBoolean("update-checker.enabled", true)) {
            return;
        }
        String current = plugin.getPluginMeta().getVersion();
        String source = config.getString("update-checker.source", "github").trim().toLowerCase(Locale.ROOT);
        CompletableFuture<Update> request = switch (source) {
            case "spigot", "spigotmc", "spiget" -> spigot(config);
            default -> github(config);
        };
        request.thenAccept(update -> {
            if (update != null && SemVer.isNewer(update.version(), current)) {
                available = update;
                logUpdate(current, update);
            }
        }).exceptionally(error -> null);
    }

    private CompletableFuture<Update> github(FileConfiguration config) {
        String repo = config.getString("update-checker.github-repo", "TTVPrivalo21/RadiantUI").trim();
        if (!repo.matches("[A-Za-z0-9_.\\-]+/[A-Za-z0-9_.\\-]+")) {
            return CompletableFuture.completedFuture(null);
        }
        return fetch("https://api.github.com/repos/" + repo + "/releases/latest", "application/vnd.github+json")
                .thenApply(body -> {
                    JsonObject json = object(body);
                    if (json == null || !json.has("tag_name")) {
                        return null;
                    }
                    String version = SemVer.normalize(json.get("tag_name").getAsString());
                    String url = json.has("html_url") && !json.get("html_url").isJsonNull()
                            ? json.get("html_url").getAsString()
                            : "https://github.com/" + repo + "/releases";
                    return version.isEmpty() ? null : new Update(version, url, null);
                });
    }

    private CompletableFuture<Update> spigot(FileConfiguration config) {
        int id = config.getInt("update-checker.spigot-resource-id", 0);
        String authorUrl = config.getString("update-checker.spigot-author-url",
                "https://www.spigotmc.org/resources/authors/thekillerv2.1619961/");
        if (id <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        return fetch("https://api.spiget.org/v2/resources/" + id + "/versions/latest", "application/json")
                .thenApply(body -> {
                    JsonObject json = object(body);
                    if (json == null || !json.has("name")) {
                        return null;
                    }
                    String version = SemVer.normalize(json.get("name").getAsString());
                    String url = "https://www.spigotmc.org/resources/" + id + "/";
                    return version.isEmpty() ? null : new Update(version, url, authorUrl);
                });
    }

    /** GET with 5s timeout and User-Agent; completes with null on any non-200 or error. */
    private CompletableFuture<String> fetch(String url, String accept) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "RadiantUI/" + plugin.getPluginMeta().getVersion() + " (update-checker)")
                    .header("Accept", accept)
                    .GET()
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> response.statusCode() == 200 ? response.body() : null)
                    .exceptionally(error -> null);
        } catch (IllegalArgumentException ex) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static JsonObject object(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(body);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void logUpdate(String current, Update update) {
        var log = plugin.getLogger();
        log.warning("================================================");
        log.warning(" ¡Hay una nueva versión de RadiantUI disponible!");
        log.warning("   Versión actual:     " + current);
        log.warning("   Nueva versión:      " + update.version());
        log.warning("   Descarga:           " + update.url());
        if (update.profile() != null && !update.profile().isBlank()) {
            log.warning("   Perfil del autor:   " + update.profile());
        }
        log.warning("================================================");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Update update = available;
        Player player = event.getPlayer();
        if (update == null || !notifyAdmins || !player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                String url = update.url().replace("'", "%27");
                plugin.messages().send(player, "update-available",
                        "current", plugin.getPluginMeta().getVersion(),
                        "latest", update.version(),
                        "url", url);
            }
        }, 60L);
    }

    /** Latest newer version found, or null. */
    public String availableVersion() {
        Update update = available;
        return update == null ? null : update.version();
    }
}
