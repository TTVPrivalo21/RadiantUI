package dev.privalo.ui.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.privalo.ui.pack.BuiltPack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Minimal embedded HTTP server (JDK built-in) that serves the generated packs from memory:
 * {@code GET /pack/<theme>-<sha1>.zip}.
 */
public final class PackHttpServer {

    private final Logger logger;
    private HttpServer server;
    private ExecutorService executor;
    private String bind;
    private int port;

    public PackHttpServer(Logger logger) {
        this.logger = logger;
    }

    public synchronized boolean start(String bind, int port, int threads, Function<String, BuiltPack> lookup) {
        if (server != null && bind.equals(this.bind) && port == this.port) {
            return true;
        }
        stop();
        try {
            InetSocketAddress address = bind == null || bind.isBlank() || bind.equals("0.0.0.0")
                    ? new InetSocketAddress(port)
                    : new InetSocketAddress(bind, port);
            HttpServer created = HttpServer.create(address, 16);
            AtomicInteger counter = new AtomicInteger();
            executor = Executors.newFixedThreadPool(Math.max(1, threads), r -> {
                Thread t = new Thread(r, "RadiantUI-HTTP-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            });
            created.setExecutor(executor);
            created.createContext("/", exchange -> handle(exchange, lookup));
            created.start();
            server = created;
            this.bind = bind;
            this.port = port;
            logger.info("Servidor HTTP del resource pack escuchando en " + (bind == null || bind.isBlank() ? "0.0.0.0" : bind) + ":" + port);
            return true;
        } catch (IOException | IllegalArgumentException ex) {
            logger.severe("No se pudo iniciar el servidor HTTP en el puerto " + port + ": " + ex.getMessage());
            server = null;
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            return false;
        }
    }

    private void handle(HttpExchange exchange, Function<String, BuiltPack> lookup) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            boolean head = "HEAD".equalsIgnoreCase(method);
            if (!head && !"GET".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().add("Allow", "GET, HEAD");
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/pack/") && path.endsWith(".zip")) {
                BuiltPack pack = lookup.apply(path.substring("/pack/".length()));
                if (pack != null) {
                    byte[] body = pack.zip();
                    exchange.getResponseHeaders().add("Content-Type", "application/zip");
                    exchange.getResponseHeaders().add("Cache-Control", "public, max-age=31536000, immutable");
                    exchange.getResponseHeaders().add("ETag", "\"" + pack.sha1Hex() + "\"");
                    exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + pack.fileName() + "\"");
                    if (head) {
                        exchange.getResponseHeaders().add("Content-Length", String.valueOf(body.length));
                        exchange.sendResponseHeaders(200, -1);
                    } else {
                        exchange.sendResponseHeaders(200, body.length);
                        try (OutputStream out = exchange.getResponseBody()) {
                            out.write(body);
                        }
                    }
                    return;
                }
            }
            byte[] body = (path.equals("/") ? "RadiantUI resource pack server" : "Not found").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            int status = path.equals("/") ? 200 : 404;
            if (head) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        }
    }

    public synchronized boolean running() {
        return server != null;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        bind = null;
        port = -1;
    }
}
