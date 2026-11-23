package dev.privalo.ui.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.privalo.ui.config.ConfigFiles;
import dev.privalo.ui.text.GlyphRegistry;
import dev.privalo.ui.text.SpaceFont;
import dev.privalo.ui.text.TextService;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Builds a complete resource pack for a theme: painted textures, nine-slice metadata,
 * fonts (glyphs + spaces), pack.mcmeta, admin overrides and optional Oraxen merge.
 */
public final class PackGenerator {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2024, 1, 1, 0, 0);

    private final File dataFolder;
    private final ConfigFiles configs;
    private final GlyphRegistry glyphs;
    private final TextService text;
    private final Logger logger;

    public PackGenerator(File dataFolder, ConfigFiles configs, GlyphRegistry glyphs, TextService text, Logger logger) {
        this.dataFolder = dataFolder;
        this.configs = configs;
        this.glyphs = glyphs;
        this.text = text;
        this.logger = logger;
    }

    public BuiltPack build(Theme theme) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        TexturePainter painter = new TexturePainter(theme);
        Map<String, PixelCanvas> textures = new LinkedHashMap<>();

        FileConfiguration hud = configs.hud();
        if (hud.getBoolean("textures.hearts", true)) painter.hearts(textures);
        if (hud.getBoolean("textures.food", true)) painter.food(textures);
        if (hud.getBoolean("textures.armor", true)) painter.armor(textures);
        if (hud.getBoolean("textures.air", true)) painter.air(textures);
        if (hud.getBoolean("textures.experience", true)) painter.experience(textures);
        if (hud.getBoolean("textures.hotbar", true)) painter.hotbar(textures);
        String hidden = hud.getString("bossbar.hide-bar-color", "");
        if (hud.getBoolean("bossbar.enabled", true) && hidden != null && !hidden.isBlank() && !hidden.equalsIgnoreCase("none")) {
            painter.hiddenBossBar(textures, hidden.trim().toLowerCase(Locale.ROOT));
        }

        FileConfiguration containers = configs.containers();
        if (containers.getBoolean("textures.player-inventory", true)) painter.playerInventory(textures);
        if (containers.getBoolean("textures.chests", true)) painter.chests(textures);
        if (containers.getBoolean("textures.creative-inventory", true)) painter.creative(textures);

        FileConfiguration pause = configs.pauseMenu();
        if (pause.getBoolean("textures.buttons", true)) {
            painter.buttons(textures);
            for (String name : List.of("button", "button_highlighted", "button_disabled")) {
                files.put(TexturePainter.SPRITES + "widget/" + name + ".png.mcmeta", nineSlice(200, 20, TexturePainter.WIDGET_SIDE_BORDER, 3));
            }
        }
        if (pause.getBoolean("textures.sliders", true)) {
            painter.sliders(textures);
            for (String name : List.of("slider", "slider_highlighted")) {
                files.put(TexturePainter.SPRITES + "widget/" + name + ".png.mcmeta", nineSlice(200, 20, TexturePainter.WIDGET_SIDE_BORDER, 3));
            }
        }
        if (pause.getBoolean("textures.text-fields", true)) {
            painter.textFields(textures);
            for (String name : List.of("text_field", "text_field_highlighted")) {
                files.put(TexturePainter.SPRITES + "widget/" + name + ".png.mcmeta", nineSlice(200, 20, 2, 2));
            }
        }
        if (pause.getBoolean("textures.separators", true)) painter.separators(textures);
        if (pause.getBoolean("textures.backgrounds", true)) painter.backgrounds(textures);

        textures.putAll(glyphs.paint(painter));

        for (Map.Entry<String, PixelCanvas> e : textures.entrySet()) {
            files.put(e.getKey(), e.getValue().png());
        }
        files.put("pack.png", packIcon(painter));
        files.put("assets/minecraft/font/default.json", json(glyphs.fontJson()));
        files.put("assets/radiantui/font/space.json", json(SpaceFont.fontJson()));
        files.put("pack.mcmeta", json(mcmeta(theme)));
        files.putAll(langOverrides(pause));

        // Admin overrides always win: global assets/ first, then themes/<id>/assets/
        int overrides = copyOverrides(new File(dataFolder, "assets"), files);
        overrides += copyOverrides(new File(dataFolder, "themes/" + theme.id() + "/assets"), files);
        if (overrides > 0) {
            logger.info("[" + theme.id() + "] " + overrides + " archivo(s) personalizado(s) aplicados desde assets/.");
        }

        FileConfiguration main = configs.main();
        if (main.getBoolean("pack.oraxen-merge.enabled", true)) {
            File oraxen = new File(main.getString("pack.oraxen-merge.zip", "plugins/Oraxen/pack/pack.zip"));
            if (oraxen.isFile()) {
                int merged = mergeZip(oraxen, files);
                logger.info("[" + theme.id() + "] Pack de Oraxen combinado (" + merged + " archivos).");
            }
        }

        if (main.getBoolean("pack.export-unzipped", true)) {
            export(new File(dataFolder, "generated/" + theme.id()), files);
        }

        byte[] zip = zip(files);
        byte[] sha1 = sha1(zip);
        File packsDir = new File(dataFolder, "packs");
        Files.createDirectories(packsDir.toPath());
        File out = new File(packsDir, theme.id() + ".zip");
        Files.write(out.toPath(), zip);
        return new BuiltPack(theme, zip, sha1, HexFormat.of().formatHex(sha1), out, files.size());
    }

    /**
     * pack.png priority: plugins/RadiantUI/pack.png → plugin logo bundled in the jar (radiantui96.png)
     * → painted puppy card.
     */
    private byte[] packIcon(TexturePainter painter) throws IOException {
        File custom = new File(dataFolder, "pack.png");
        if (custom.isFile()) {
            return Files.readAllBytes(custom.toPath());
        }
        try (InputStream in = PackGenerator.class.getResourceAsStream("/pack/pack.png")) {
            if (in != null) {
                return in.readAllBytes();
            }
        }
        return painter.packIcon().png();
    }

    /**
     * Language overrides (pausemenu.yml → lang-overrides) for every locale in lang-locales.
     * Tokens: {glyph:id} → glyph character, {space:px} → pixel offset (both live in the default font,
     * because lang strings cannot change font). Used to turn the "Game Menu" title into a ribbon banner.
     */
    private Map<String, byte[]> langOverrides(FileConfiguration pause) {
        Map<String, byte[]> out = new TreeMap<>();
        org.bukkit.configuration.ConfigurationSection section = pause.getConfigurationSection("lang-overrides");
        if (section == null) {
            return out;
        }
        JsonObject entries = new JsonObject();
        // Translation keys contain dots ("menu.game"), which YAML/Bukkit turns into nested sections:
        // walk every leaf and use its full path as the translation key.
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) {
                continue;
            }
            String value = section.getString(key);
            if (value != null) {
                entries.addProperty(key, resolveTokens(value));
            }
        }
        if (entries.size() == 0) {
            return out;
        }
        byte[] data = json(entries);
        List<String> locales = pause.getStringList("lang-locales");
        if (locales.isEmpty() || locales.stream().anyMatch(l -> l.equalsIgnoreCase("all"))) {
            locales = ALL_LOCALES;
        }
        for (String locale : locales) {
            String id = locale.trim().toLowerCase(Locale.ROOT);
            if (id.matches("[a-z0-9_]{2,12}")) {
                out.put("assets/minecraft/lang/" + id + ".json", data);
            }
        }
        return out;
    }

    /** Every language the vanilla client ships, so the override works whatever language the player uses. */
    private static final List<String> ALL_LOCALES = List.of(
            "af_za", "ar_sa", "ast_es", "az_az", "ba_ru", "bar", "be_by", "bg_bg", "br_fr", "brb", "bs_ba", "ca_es",
            "cs_cz", "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca", "en_gb", "en_nz",
            "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy", "es_ar", "es_cl", "es_ec", "es_es", "es_mx",
            "es_uy", "es_ve", "esan", "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr",
            "fra_de", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il", "hi_in", "hn_no", "hr_hr",
            "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is", "isv", "it_it", "ja_jp", "jbo_en", "ka_ge",
            "kk_kz", "kn_in", "ko_kr", "ksh", "kw_gb", "ky_kg", "la_la", "lb_lu", "li_li", "lmo", "lo_la", "lol_us",
            "lt_lt", "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de", "nl_be", "nl_nl", "nn_no",
            "no_no", "oc_fr", "ovd", "pl_pl", "pt_br", "pt_pt", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua",
            "sah_sah", "se_no", "sk_sk", "sl_si", "so_so", "sq_al", "sr_cs", "sr_sp", "sv_se", "sxu", "szl_pl",
            "ta_in", "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr", "tt_ru", "tzo_mx", "uk_ua", "val_es", "vec_it",
            "vi_vn", "vp_vl", "yi_de", "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab");

    private String resolveTokens(String value) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{(glyph|space):(-?[a-z0-9_]+)}").matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String replacement;
            if (m.group(1).equals("glyph")) {
                var glyph = glyphs.get(m.group(2));
                replacement = glyph == null ? "" : glyph.asString();
            } else {
                int px;
                try {
                    px = Integer.parseInt(m.group(2));
                } catch (NumberFormatException ex) {
                    px = 0;
                }
                replacement = SpaceFont.chars(px);
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private JsonObject mcmeta(Theme theme) {
        FileConfiguration main = configs.main();
        int format = main.getInt("pack.pack-format", 46);
        int min = main.getInt("pack.min-format", 34);
        int max = Math.max(min, main.getInt("pack.max-format", 999));
        String description = main.getString("pack.description", "RadiantUI · {theme}").replace("{theme}", theme.displayName());

        JsonObject supported = new JsonObject();
        supported.addProperty("min_inclusive", min);
        supported.addProperty("max_inclusive", max);
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", Math.max(min, Math.min(max, format)));
        pack.add("supported_formats", supported);
        // 1.21.9+ (format 65+) reads min_format/max_format
        pack.addProperty("min_format", min);
        pack.addProperty("max_format", max);
        pack.add("description", GsonComponentSerializer.gson().serializeToTree(text.parse(description, theme)));
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }

    /** Nine-slice metadata: side borders keep the heart rivets / ink corners unstretched. */
    private static byte[] nineSlice(int width, int height, int sideBorder, int verticalBorder) {
        JsonObject border = new JsonObject();
        border.addProperty("left", sideBorder);
        border.addProperty("right", sideBorder);
        border.addProperty("top", verticalBorder);
        border.addProperty("bottom", verticalBorder);
        JsonObject scaling = new JsonObject();
        scaling.addProperty("type", "nine_slice");
        scaling.addProperty("width", width);
        scaling.addProperty("height", height);
        scaling.add("border", border);
        JsonObject gui = new JsonObject();
        gui.add("scaling", scaling);
        JsonObject root = new JsonObject();
        root.add("gui", gui);
        return json(root);
    }

    private static byte[] json(JsonElement element) {
        return GSON.toJson(element).getBytes(StandardCharsets.UTF_8);
    }

    private static int copyOverrides(File root, Map<String, byte[]> files) throws IOException {
        if (!root.isDirectory()) {
            return 0;
        }
        Path base = root.toPath();
        int count = 0;
        try (Stream<Path> walk = Files.walk(base)) {
            for (Path path : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String rel = base.relativize(path).toString().replace('\\', '/');
                files.put("assets/" + rel, Files.readAllBytes(path));
                count++;
            }
        }
        return count;
    }

    /** Adds entries from another pack; font/atlas JSON files are merged instead of replaced. */
    private int mergeZip(File source, Map<String, byte[]> files) {
        int merged = 0;
        try (ZipFile zip = new ZipFile(source)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || name.equals("pack.mcmeta") || name.equals("pack.png")) {
                    continue;
                }
                byte[] data;
                try (InputStream in = zip.getInputStream(entry)) {
                    data = in.readAllBytes();
                }
                byte[] existing = files.get(name);
                if (existing == null) {
                    files.put(name, data);
                    merged++;
                } else if (name.endsWith(".json") && (name.contains("/font/") || name.contains("/atlases/"))) {
                    String arrayKey = name.contains("/font/") ? "providers" : "sources";
                    byte[] combined = mergeJsonArrays(existing, data, arrayKey);
                    if (combined != null) {
                        files.put(name, combined);
                        merged++;
                    }
                }
            }
        } catch (IOException ex) {
            logger.warning("No se pudo combinar el pack de Oraxen: " + ex.getMessage());
        }
        return merged;
    }

    private static byte[] mergeJsonArrays(byte[] ours, byte[] theirs, String key) {
        try {
            JsonObject a = JsonParser.parseString(new String(ours, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject b = JsonParser.parseString(new String(theirs, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray result = new JsonArray();
            if (a.has(key)) result.addAll(a.getAsJsonArray(key));
            if (b.has(key)) result.addAll(b.getAsJsonArray(key));
            a.add(key, result);
            return json(a);
        } catch (JsonParseException | IllegalStateException ex) {
            return null;
        }
    }

    private void export(File dir, Map<String, byte[]> files) throws IOException {
        if (dir.isDirectory()) {
            try (Stream<Path> walk = Files.walk(dir.toPath())) {
                List<Path> paths = walk.sorted(java.util.Comparator.reverseOrder()).toList();
                for (Path p : paths) {
                    Files.deleteIfExists(p);
                }
            }
        }
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            Path target = dir.toPath().resolve(e.getKey());
            Files.createDirectories(target.getParent());
            Files.write(target, e.getValue());
        }
    }

    /** Deterministic zip (sorted entries, fixed timestamps) → same content = same SHA-1 = client cache hit. */
    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 16);
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.setLevel(9);
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                entry.setTimeLocal(FIXED_TIME);
                zip.putNextEntry(entry);
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] sha1(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(data);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 no disponible", ex);
        }
    }
}
