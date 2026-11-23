package dev.privalo.ui.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.privalo.ui.pack.PixelCanvas;
import dev.privalo.ui.pack.TexturePainter;
import dev.privalo.ui.theme.Theme;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * All bitmap glyphs of RadiantUI (icons, menu overlays, custom overlays) with stable
 * private-use characters. The same characters are used in every theme pack, so text
 * components never depend on the viewer's theme.
 */
public final class GlyphRegistry {

    public static final String NAMESPACE = "radiantui";
    private static final char ICON_BASE = '';
    private static final char OVERLAY_BASE = '';
    private static final char CUSTOM_BASE = '';
    private static final int ICON_HEIGHT = 8;
    private static final int ICON_ASCENT = 7;
    private static final int HUD_PANEL_ASCENT = 10;
    private static final char BANNER_BASE = '';

    private final Logger logger;
    private final Map<String, Glyph> glyphs = new LinkedHashMap<>();
    private final Map<String, File> customFiles = new LinkedHashMap<>();
    private int overlayAscent = 13;
    private String pauseTitleText = "MENU";
    private String linksBannerText = "ENLACES";
    private java.awt.image.BufferedImage logo;
    private int logoHeight = 56;
    private boolean logoGray = true;
    private boolean logoPups = false;

    public GlyphRegistry(Logger logger) {
        this.logger = logger;
    }

    /** Recomputes glyph metrics using the reference (global) theme and the custom overlay folder. */
    public synchronized void rebuild(Theme reference, FileConfiguration containers, FileConfiguration pauseMenu, File overlaysDir) {
        glyphs.clear();
        customFiles.clear();
        overlayAscent = containers.getInt("overlay.ascent", 13);
        pauseTitleText = pauseMenu.getString("pause-title.text", "MENU");
        linksBannerText = pauseMenu.getString("quick-menu.banner-text", "ENLACES");
        TexturePainter painter = new TexturePainter(reference);

        // Pause-menu banners (ribbons with pixel text): sized by their text, so registered first in their own range
        PixelCanvas pauseTitle = painter.banner(pauseTitleText);
        register("pause_title", BANNER_BASE, pauseTitle, pauseTitle.height(), pauseMenu.getInt("pause-title.ascent", 26));
        PixelCanvas linksBanner = painter.banner(linksBannerText);
        register("links_banner", (char) (BANNER_BASE + 1), linksBanner, linksBanner.height(), 24);

        // Pause-menu logo, drawn through the "menu.game" title (fixed at GUI y=40 by the client)
        logo = loadLogo(overlaysDir.getParentFile());
        logoHeight = Math.max(16, Math.min(250, pauseMenu.getInt("pause-title.logo-height", 56)));
        logoGray = pauseMenu.getBoolean("pause-title.grayscale", true);
        logoPups = pauseMenu.getBoolean("pause-title.side-pups", false);
        PixelCanvas pauseLogo = logo != null ? painter.pauseLogo(logo, logoHeight, logoGray, logoPups) : pauseTitle;
        // bottom edge on the bottom of the vanilla title line (GUI y≈48); offset-y moves it up (+) or down (-)
        int logoAscent = pauseLogo.height() - 1 + pauseMenu.getInt("pause-title.offset-y", -24);
        register("pause_logo", (char) (BANNER_BASE + 2), pauseLogo, pauseLogo.height(), logoAscent);

        char next = ICON_BASE;
        for (Map.Entry<String, PixelCanvas> icon : painter.icons().entrySet()) {
            register(icon.getKey(), next++, icon.getValue(), ICON_HEIGHT, ICON_ASCENT);
        }
        next = OVERLAY_BASE;
        for (int rows = 1; rows <= 6; rows++) {
            PixelCanvas overlay = painter.chestOverlay(rows);
            register("chest_" + rows, next++, overlay, overlay.height(), overlayAscent);
        }
        PixelCanvas panel = painter.hudPanel();
        register("hud_panel", next, panel, panel.height(), HUD_PANEL_ASCENT);

        ConfigurationSection custom = containers.getConfigurationSection("custom-overlays");
        if (custom != null) {
            List<String> ids = new ArrayList<>(custom.getKeys(false));
            Collections.sort(ids);
            char customChar = CUSTOM_BASE;
            for (String id : ids) {
                ConfigurationSection section = custom.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                String key = id.toLowerCase(java.util.Locale.ROOT);
                if (glyphs.containsKey(key) || !key.matches("[a-z0-9_\\-]+")) {
                    logger.warning("Overlay personalizado ignorado (id inválido o repetido): " + id);
                    continue;
                }
                File file = new File(overlaysDir, section.getString("file", key + ".png"));
                BufferedImage image = readImage(file);
                if (image == null) {
                    logger.warning("Overlay personalizado '" + id + "': no se pudo leer " + file.getPath());
                    continue;
                }
                PixelCanvas canvas = PixelCanvas.of(image);
                int height = Math.min(256, section.getInt("height", image.getHeight()));
                int ascent = section.getInt("ascent", overlayAscent);
                register(key, customChar++, canvas, height, ascent);
                customFiles.put(key, file);
            }
        }
    }

    private void register(String id, char character, PixelCanvas canvas, int height, int ascent) {
        int safeHeight = Math.max(1, Math.min(256, height));
        int safeAscent = Math.min(ascent, safeHeight);
        glyphs.put(id, new Glyph(id, character, NAMESPACE + ":font/" + id + ".png",
                canvas.width(), canvas.height(), canvas.visibleWidth(), safeHeight, safeAscent));
    }

    /** pause-logo.png → pack.png (data folder) → logo bundled in the jar (/pack/pack.png); null if none. */
    private static BufferedImage loadLogo(File dataFolder) {
        if (dataFolder != null) {
            for (String name : new String[]{"pause-logo.png", "pack.png"}) {
                BufferedImage image = readImage(new File(dataFolder, name));
                if (image != null) {
                    return image;
                }
            }
        }
        try (java.io.InputStream in = GlyphRegistry.class.getResourceAsStream("/pack/pack.png")) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException ex) {
            return null;
        }
    }

    private static BufferedImage readImage(File file) {
        if (!file.isFile()) {
            return null;
        }
        try {
            return ImageIO.read(file);
        } catch (IOException ex) {
            return null;
        }
    }

    public synchronized Glyph get(String id) {
        return id == null ? null : glyphs.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public synchronized Collection<Glyph> all() {
        return List.copyOf(glyphs.values());
    }

    public int overlayAscent() {
        return overlayAscent;
    }

    /** Paints every glyph image for a theme: pack path → canvas. */
    public synchronized Map<String, PixelCanvas> paint(TexturePainter painter) {
        Map<String, PixelCanvas> images = new LinkedHashMap<>(painter.icons());
        for (int rows = 1; rows <= 6; rows++) {
            images.put("chest_" + rows, painter.chestOverlay(rows));
        }
        images.put("hud_panel", painter.hudPanel());
        images.put("pause_title", painter.banner(pauseTitleText));
        images.put("links_banner", painter.banner(linksBannerText));
        images.put("pause_logo", logo != null ? painter.pauseLogo(logo, logoHeight, logoGray, logoPups) : painter.banner(pauseTitleText));
        for (Map.Entry<String, File> custom : customFiles.entrySet()) {
            BufferedImage image = readImage(custom.getValue());
            if (image != null) {
                images.put(custom.getKey(), PixelCanvas.of(image));
            }
        }
        Map<String, PixelCanvas> out = new LinkedHashMap<>();
        for (Map.Entry<String, PixelCanvas> e : images.entrySet()) {
            if (glyphs.containsKey(e.getKey())) {
                out.put("assets/" + NAMESPACE + "/textures/font/" + e.getKey() + ".png", e.getValue());
            }
        }
        return out;
    }

    /** JSON for assets/minecraft/font/default.json. */
    public synchronized JsonObject fontJson() {
        JsonArray providers = new JsonArray();
        for (Glyph glyph : glyphs.values()) {
            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", glyph.texture());
            provider.addProperty("ascent", glyph.ascent());
            provider.addProperty("height", glyph.height());
            JsonArray chars = new JsonArray();
            chars.add(glyph.asString());
            provider.add("chars", chars);
            providers.add(provider);
        }
        JsonObject root = new JsonObject();
        // spaces also in the default font: lang strings cannot switch fonts
        for (com.google.gson.JsonElement space : SpaceFont.fontJson().getAsJsonArray("providers")) {
            providers.add(space);
        }
        root.add("providers", providers);
        return root;
    }
}
