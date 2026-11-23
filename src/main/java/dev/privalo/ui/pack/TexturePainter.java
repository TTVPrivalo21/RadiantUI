package dev.privalo.ui.pack;

import dev.privalo.ui.theme.Motif;
import dev.privalo.ui.theme.Palette;
import dev.privalo.ui.theme.Theme;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static dev.privalo.ui.pack.PixelCanvas.mix;
import static dev.privalo.ui.pack.PixelCanvas.withAlpha;

/**
 * Paints every texture of a theme in code, in a classic cartoon / comic-strip style:
 * thick black ink outlines, hard inner shadows, white highlights, Ben-Day halftone dots
 * in grey, round "sticker" badges and dashed ink separators with hearts.
 * Output keys are full pack paths (e.g. {@code assets/minecraft/textures/gui/sprites/hud/heart/full.png}).
 */
public final class TexturePainter {

    public static final String TEXTURES = "assets/minecraft/textures/";
    public static final String SPRITES = TEXTURES + "gui/sprites/";
    public static final String CONTAINER = TEXTURES + "gui/container/";
    public static final String CREATIVE = CONTAINER + "creative_inventory/";
    public static final String CREATIVE_SPRITES = SPRITES + "container/creative_inventory/";

    private final Motif motif;
    private final Map<Character, Integer> ink = new HashMap<>();

    private final int outline;
    private final int spot;
    private final int paper;
    private final int shade;
    private final int bg;
    private final int bgAlt;
    private final int accent;
    private final int accentLight;
    private final int accentPale;
    private final int accentShadow;
    /** Inner bottom/right "ink" shadow of panels. */
    private final int panelShadow;
    /** Inner top/left highlight of panels. */
    private final int panelLight;

    public TexturePainter(Theme theme) {
        Palette p = theme.palette();
        this.motif = theme.motif();
        this.outline = p.outline();
        this.spot = PixelCanvas.luminance(p.dark()) < 0.45 ? p.dark() : 0xFF18181B;
        this.paper = PixelCanvas.luminance(p.background()) >= PixelCanvas.luminance(p.highlight()) ? p.background() : p.highlight();
        this.shade = p.shade();
        this.bg = p.background();
        this.bgAlt = p.backgroundAlt();
        this.accent = p.accent();
        this.accentLight = p.accentLight();
        this.accentPale = p.accentPale();
        this.accentShadow = mix(accent, outline, 0.25);
        this.panelShadow = mix(bg, shade, 0.5);
        this.panelLight = mix(bg, 0xFFFFFFFF, 0.6);
        // Sprite "fill" ink: always the light colour so hearts/bones/stars read as white cartoon shapes
        // with black ink, on the world, on dark sidebars and on white panels alike.
        int fill = PixelCanvas.luminance(accent) < 0.5 ? paper : accent;
        ink.put('O', outline);
        ink.put('S', spot);
        ink.put('W', paper);
        ink.put('H', 0xFFFFFFFF);
        ink.put('G', shade);
        ink.put('b', bgAlt);
        ink.put('B', bg);
        ink.put('P', fill);
        ink.put('L', mix(fill, bgAlt, 0.7));
        ink.put('p', accentPale);
        ink.put('D', mix(fill, shade, 0.55));
    }

    /** Halftone colour: soft grey dots that never flood the screen. */
    private int dots(int alpha) {
        return withAlpha(accentLight, alpha * 6 / 10);
    }

    // ══════════════════════════════ HUD ══════════════════════════════

    public void hearts(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "hud/heart/";
        String[] full = Sprites.HEART;
        String[] container = Sprites.recolor(full, map('P', 'G', 'L', 'G', 'D', 'S', 'H', 'G'));
        out.put(dir + "container.png", sprite(container));
        out.put(dir + "container_blinking.png", sprite(blink(container)));
        String[] hardcoreContainer = Sprites.stamp(container, 'S', 5, 3, 6, 3, 6, 4);
        out.put(dir + "container_hardcore.png", sprite(hardcoreContainer));
        out.put(dir + "container_hardcore_blinking.png", sprite(blink(hardcoreContainer)));

        Map<String, String[]> types = new LinkedHashMap<>();
        types.put("", full);
        types.put("poisoned_", Sprites.stamp(Sprites.recolor(full, map('P', 'p', 'D', 'L')), 'S', 3, 4, 5, 3, 6, 5));
        types.put("withered_", Sprites.recolor(full, map('P', 'S', 'D', 'O', 'L', 'G', 'H', 'G')));
        types.put("absorbing_", Sprites.recolor(full, map('P', 'W', 'L', 'W', 'D', 'b', 'O', 'G')));
        types.put("frozen_", Sprites.recolor(full, map('P', 'b', 'L', 'W', 'D', 'G', 'O', 'S')));

        for (Map.Entry<String, String[]> e : types.entrySet()) {
            String prefix = e.getKey();
            String[] base = e.getValue();
            char spotChar = prefix.equals("withered_") ? 'W' : 'S';
            String[] hardcore = Sprites.stamp(base, spotChar, 5, 3, 6, 3, 6, 4);
            putHeart(out, dir, prefix, "", base);
            putHeart(out, dir, prefix, "hardcore_", hardcore);
        }

        String[] vehicle = Sprites.recolor(full, map('P', 'L', 'D', 'P'));
        out.put(dir + "vehicle_full.png", sprite(vehicle));
        out.put(dir + "vehicle_half.png", sprite(Sprites.leftHalf(vehicle, 5)));
        out.put(dir + "vehicle_container.png", sprite(container));
    }

    private void putHeart(Map<String, PixelCanvas> out, String dir, String prefix, String hardcore, String[] sprite) {
        String[] half = Sprites.leftHalf(sprite, 5);
        out.put(dir + prefix + hardcore + "full.png", sprite(sprite));
        out.put(dir + prefix + hardcore + "full_blinking.png", sprite(blink(sprite)));
        out.put(dir + prefix + hardcore + "half.png", sprite(half));
        out.put(dir + prefix + hardcore + "half_blinking.png", sprite(blink(half)));
    }

    public void food(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "hud/";
        String[] full = Sprites.BONE;
        String[] hunger = Sprites.stamp(Sprites.recolor(full, map('W', 'p', 'G', 'L')), 'S', 2, 4, 6, 4);
        String[] empty = Sprites.recolor(full, map('W', 'G', 'G', 'S'));
        String[] emptyHunger = Sprites.recolor(empty, map('O', 'D'));
        out.put(dir + "food_full.png", sprite(full));
        out.put(dir + "food_half.png", sprite(Sprites.leftHalf(full, 5)));
        out.put(dir + "food_empty.png", sprite(empty));
        out.put(dir + "food_full_hunger.png", sprite(hunger));
        out.put(dir + "food_half_hunger.png", sprite(Sprites.leftHalf(hunger, 5)));
        out.put(dir + "food_empty_hunger.png", sprite(emptyHunger));
    }

    public void armor(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "hud/";
        String[] full = Sprites.SHIELD;
        out.put(dir + "armor_full.png", sprite(full));
        out.put(dir + "armor_half.png", sprite(Sprites.leftHalf(full, 5)));
        out.put(dir + "armor_empty.png", sprite(Sprites.recolor(full, map('W', 'G', 'P', 'S', 'G', 'S'))));
    }

    public void air(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "hud/";
        out.put(dir + "air.png", sprite(Sprites.BUBBLE));
        out.put(dir + "air_bursting.png", sprite(Sprites.BUBBLE_POP));
        out.put(dir + "air_empty.png", sprite(Sprites.recolor(Sprites.BUBBLE, map('W', 'G', 'H', 'G', 'L', 'G', 'p', 'S'))));
    }

    public void experience(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "hud/";
        PixelCanvas background = new PixelCanvas(182, 5);
        background.fill(1, 1, 180, 3, bgAlt);
        background.hline(1, 3, 180, mix(bgAlt, shade, 0.6));
        background.roundStroke(0, 0, 182, 5, outline);
        for (int x = 20; x < 180; x += 20) {
            background.vline(x, 1, 3, outline);
        }
        out.put(dir + "experience_bar_background.png", background);

        PixelCanvas progress = new PixelCanvas(182, 5);
        progress.fill(1, 1, 180, 3, accent);
        progress.hline(1, 1, 180, accentLight);
        progress.hline(1, 3, 180, accentShadow);
        for (int x = 3; x < 180; x += 4) {
            progress.set(x, 1, 0xFFFFFFFF);
        }
        progress.roundStroke(0, 0, 182, 5, outline);
        for (int x = 20; x < 180; x += 20) {
            progress.vline(x, 1, 3, outline);
        }
        out.put(dir + "experience_bar_progress.png", progress);
    }

    public void hotbar(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "hud/";
        PixelCanvas bar = new PixelCanvas(182, 22);
        bar.fill(1, 1, 180, 20, withAlpha(bg, 0xE8));
        for (int i = 0; i < 9; i++) {
            int x = 1 + i * 20;
            bar.fill(x + 1, 2, 18, 18, withAlpha(bgAlt, 0xE8));
            bar.hline(x + 1, 2, 18, withAlpha(panelShadow, 0xE8));
            bar.vline(x + 1, 2, 18, withAlpha(panelShadow, 0xE8));
            bar.hline(x + 2, 19, 17, withAlpha(panelLight, 0xE8));
            if (i > 0) {
                bar.vline(x, 1, 20, outline);
            }
        }
        bar.roundStroke(0, 0, 182, 22, outline);
        out.put(dir + "hotbar.png", bar);

        PixelCanvas selection = new PixelCanvas(24, 23);
        selection.roundStroke(0, 0, 24, 23, outline);
        selection.stroke(1, 1, 22, 21, accent);
        selection.stroke(2, 2, 20, 19, accentLight);
        selection.roundStroke(3, 3, 18, 17, outline);
        selection.hline(4, 1, 2, 0xFFFFFFFF);
        selection.vline(1, 4, 2, 0xFFFFFFFF);
        stickerHeart(selection, 9, 0);
        out.put(dir + "hotbar_selection.png", selection);

        PixelCanvas left = new PixelCanvas(29, 24);
        offhand(left, 0);
        out.put(dir + "hotbar_offhand_left.png", left);
        PixelCanvas right = new PixelCanvas(29, 24);
        offhand(right, 7);
        out.put(dir + "hotbar_offhand_right.png", right);
    }

    private void offhand(PixelCanvas c, int x) {
        c.fill(x + 1, 2, 20, 20, withAlpha(bg, 0xE8));
        c.fill(x + 3, 4, 16, 16, withAlpha(bgAlt, 0xE8));
        c.hline(x + 3, 4, 16, panelShadow);
        c.vline(x + 3, 4, 16, panelShadow);
        c.roundStroke(x, 1, 22, 22, outline);
        c.stroke(x + 2, 3, 18, 18, outline);
    }

    /** Makes a boss bar colour invisible so only its title (the HUD) is drawn. */
    public void hiddenBossBar(Map<String, PixelCanvas> out, String color) {
        String dir = SPRITES + "boss_bar/";
        out.put(dir + color + "_background.png", new PixelCanvas(182, 5));
        out.put(dir + color + "_progress.png", new PixelCanvas(182, 5));
    }

    /** 182px translucent comic panel for the boss bar HUD (glyph hud_panel). */
    public PixelCanvas hudPanel() {
        PixelCanvas c = new PixelCanvas(182, 14);
        c.fill(2, 2, 178, 10, withAlpha(bg, 0xE6));
        halftone(c, 100, 2, 80, 10, dots(0xB0), true);
        c.hline(3, 2, 176, panelLight);
        c.hline(3, 11, 176, panelShadow);
        inkFrame(c, 0, 0, 182, 14);
        if (motif.hearts()) {
            stickerHeart(c, 3, 4);
            stickerHeart(c, 172, 4);
        }
        return c;
    }

    // ═══════════════════════════ CONTAINERS ═══════════════════════════

    public void playerInventory(Map<String, PixelCanvas> out) {
        PixelCanvas c = new PixelCanvas(256, 256);
        comicPanel(c, 0, 0, 176, 166);
        // header band behind "Crafting"
        halftone(c, 90, 3, 82, 11, dots(0x90), true);
        for (int i = 0; i < 4; i++) {
            slot(c, 7, 7 + i * 18);
        }
        slot(c, 76, 61);
        // player preview "comic panel"
        c.fill(26, 8, 50, 70, mix(bg, accentPale, 0.55));
        halftone(c, 26, 8, 50, 70, dots(0xC0), false);
        c.blendFill(27, 60, 48, 17, withAlpha(mix(bg, accentPale, 0.55), 0xD0));
        c.hline(26, 8, 50, panelShadow);
        c.vline(26, 8, 70, panelShadow);
        c.stroke(25, 7, 52, 72, outline);
        if (motif.paws()) {
            c.pattern(Sprites.PAW_SMALL, 31, 70, ink(withAlpha(accent, 0xB0)));
            c.pattern(Sprites.PAW_SMALL, 66, 12, ink(withAlpha(accent, 0xB0)));
        }
        // crafting 2x2 + result
        for (int r = 0; r < 2; r++) {
            for (int col = 0; col < 2; col++) {
                slot(c, 97 + col * 18, 17 + r * 18);
            }
        }
        bigSlot(c, 149, 23, 26);
        slot(c, 153, 27);
        arrow(c, 134, 30);
        // main inventory + hotbar
        dashedSeparator(c, 7, 79, 162, true);
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 9; col++) {
                slot(c, 7 + col * 18, 83 + r * 18);
            }
        }
        dottedLine(c, 8, 138, 160, accentLight);
        for (int col = 0; col < 9; col++) {
            slot(c, 7 + col * 18, 141);
        }
        if (motif.puppy()) {
            badge(c, 161, 66, 8);
        }
        if (motif.hearts()) {
            stickerHeart(c, 131, 62);
            stickerHeart(c, 141, 70);
        }
        out.put(CONTAINER + "inventory.png", c);
    }

    public void chests(Map<String, PixelCanvas> out) {
        PixelCanvas c = new PixelCanvas(256, 256);
        chestPanel(c, 6, false);
        out.put(CONTAINER + "generic_54.png", c);
    }

    /** Overlay drawn through the inventory title font (slot interiors are transparent). */
    public PixelCanvas chestOverlay(int rows) {
        int height = 113 + rows * 18;
        PixelCanvas c = new PixelCanvas(176, height);
        chestPanel(c, rows, true);
        return c;
    }

    private void chestPanel(PixelCanvas c, int rows, boolean overlay) {
        int height = overlay ? 113 + rows * 18 : 222;
        comicPanel(c, 0, 0, 176, height);
        header(c, 176);
        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < 9; col++) {
                slot(c, 7 + col * 18, 17 + r * 18);
            }
        }
        // bottom half starts where the top half ends (vanilla blits it from texture y=126)
        int bottom = overlay ? 17 + rows * 18 : 126;
        int invY = bottom + 13;
        int hotbarY = bottom + 71;
        // "Inventory ─ ─ ─ ♥" separator next to the vanilla label
        dashedSeparator(c, 62, bottom + 6, 96, false);
        if (motif.hearts()) {
            stickerHeart(c, 162, bottom + 4);
        } else if (motif.paws()) {
            c.pattern(Sprites.PAW_SMALL, 162, bottom + 3, ink);
        }
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 9; col++) {
                slot(c, 7 + col * 18, invY + r * 18);
            }
        }
        dottedLine(c, 8, invY + 55, 160, accentLight);
        for (int col = 0; col < 9; col++) {
            slot(c, 7 + col * 18, hotbarY);
        }
        if (overlay) {
            for (int r = 0; r < rows; r++) {
                for (int col = 0; col < 9; col++) {
                    c.clear(8 + col * 18, 18 + r * 18, 16, 16);
                }
            }
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 9; col++) {
                    c.clear(8 + col * 18, invY + 1 + r * 18, 16, 16);
                }
            }
            for (int col = 0; col < 9; col++) {
                c.clear(8 + col * 18, hotbarY + 1, 16, 16);
            }
        }
    }

    /** Title band: halftone dots that fade towards the title, ink underline and puppy badge. */
    private void header(PixelCanvas c, int width) {
        halftone(c, 70, 3, width - 90, 11, dots(0xA0), true);
        c.hline(5, 15, width - 34, outline);
        for (int i = 0; i < 3; i++) {
            c.set(width - 29 + i * 2, 15, outline);
        }
        c.hline(5, 14, width - 34, accentLight);
        if (motif.puppy()) {
            badge(c, width - 12, 9, 7);
        } else if (motif.hearts()) {
            stickerHeart(c, width - 15, 6);
        } else if (motif.paws()) {
            c.pattern(Sprites.PAW_SMALL, width - 14, 6, ink);
        }
    }

    // ═══════════════════════════ CREATIVE ═══════════════════════════

    /** Creative inventory backgrounds (items, search, survival inventory tab), tabs and scroller. */
    public void creative(Map<String, PixelCanvas> out) {
        out.put(CREATIVE + "tab_items.png", creativeItems(false));
        out.put(CREATIVE + "tab_item_search.png", creativeItems(true));
        out.put(CREATIVE + "tab_inventory.png", creativeInventory());
        for (int i = 1; i <= 7; i++) {
            out.put(CREATIVE_SPRITES + "tab_top_selected_" + i + ".png", tab(true, true, i));
            out.put(CREATIVE_SPRITES + "tab_top_unselected_" + i + ".png", tab(true, false, i));
            out.put(CREATIVE_SPRITES + "tab_bottom_selected_" + i + ".png", tab(false, true, i));
            out.put(CREATIVE_SPRITES + "tab_bottom_unselected_" + i + ".png", tab(false, false, i));
        }
        out.put(CREATIVE_SPRITES + "scroller.png", scroller(false));
        out.put(CREATIVE_SPRITES + "scroller_disabled.png", scroller(true));
    }

    private PixelCanvas creativeItems(boolean search) {
        PixelCanvas c = new PixelCanvas(256, 256);
        comicPanel(c, 0, 0, 195, 136);
        halftone(c, 60, 3, 112, 11, dots(0x90), true);
        c.hline(5, 15, 165, outline);
        c.hline(5, 14, 165, accentLight);
        for (int r = 0; r < 5; r++) {
            for (int col = 0; col < 9; col++) {
                slot(c, 8 + col * 18, 17 + r * 18);
            }
        }
        dashedSeparator(c, 8, 108, 162, true);
        for (int col = 0; col < 9; col++) {
            slot(c, 8 + col * 18, 111);
        }
        scrollTrack(c, 174, 17, 14, 112);
        if (search) {
            // vanilla draws a borderless, white-text search box at (82,6): give it a dark comic field
            c.fill(81, 4, 89, 12, spot);
            c.hline(82, 5, 87, mix(spot, 0xFFFFFFFF, 0.15));
            c.roundStroke(80, 3, 91, 14, outline);
            c.stroke(81, 4, 89, 12, accent);
        }
        if (motif.puppy() && !search) {
            badge(c, 181, 9, 6);
        }
        return c;
    }

    private PixelCanvas creativeInventory() {
        PixelCanvas c = new PixelCanvas(256, 256);
        comicPanel(c, 0, 0, 195, 136);
        // armour 2x2 (x 54/108, y 6/33) + offhand (35,20)
        slot(c, 53, 5);
        slot(c, 53, 32);
        slot(c, 107, 5);
        slot(c, 107, 32);
        slot(c, 34, 19);
        // player preview between the armour columns
        c.fill(73, 6, 32, 43, mix(bg, accentPale, 0.55));
        halftone(c, 73, 6, 32, 43, dots(0xC0), false);
        c.stroke(72, 5, 34, 45, outline);
        // decoration column on the right
        if (motif.puppy()) {
            badge(c, 152, 24, 12);
        } else if (motif.hearts()) {
            stickerHeart(c, 148, 20);
        }
        if (motif.paws()) {
            c.pattern(Sprites.PAW_SMALL, 10, 8, ink(withAlpha(accent, 0xB0)));
            c.pattern(Sprites.PAW_SMALL, 18, 40, ink(withAlpha(accent, 0xB0)));
        }
        dashedSeparator(c, 8, 50, 179, true);
        for (int r = 0; r < 3; r++) {
            for (int col = 0; col < 9; col++) {
                slot(c, 8 + col * 18, 53 + r * 18);
            }
        }
        dottedLine(c, 9, 108, 160, accentLight);
        for (int col = 0; col < 9; col++) {
            slot(c, 8 + col * 18, 111);
        }
        // destroy-item slot (173,112): grey halftone + black ink cross
        slot(c, 172, 111);
        halftone(c, 173, 112, 16, 16, dots(0xD0), false);
        for (int i = 0; i < 8; i++) {
            c.set(177 + i, 116 + i, outline);
            c.set(178 + i, 116 + i, outline);
            c.set(184 - i, 116 + i, outline);
            c.set(185 - i, 116 + i, outline);
        }
        return c;
    }

    /** 26x32 tab. Selected tabs stay open on the panel side so they merge with the background. */
    private PixelCanvas tab(boolean top, boolean selected, int index) {
        PixelCanvas c = new PixelCanvas(26, 32);
        int body = selected ? bg : mix(bg, shade, 0.45);
        int length = selected ? 32 : 30;
        c.fill(2, 2, 22, length - 2, body);
        if (selected) {
            c.fill(2, 2, 22, 3, accent);
            c.hline(2, 5, 22, accentShadow);
            halftone(c, 3, 6, 20, 8, dots(0x90), false);
            c.hline(3, 2, 6, 0xFFFFFFFF);
        } else {
            halftone(c, 3, 3, 20, 10, withAlpha(shade, 0xC0), false);
        }
        // 2px ink: top + sides (bottom side is under / merged with the panel)
        c.hline(2, 0, 22, outline);
        c.hline(1, 1, 24, outline);
        c.vline(0, 2, length - 2, outline);
        c.vline(1, 1, length - 1, outline);
        c.vline(25, 2, length - 2, outline);
        c.vline(24, 1, length - 1, outline);
        if (!selected) {
            c.hline(0, length - 1, 26, outline);
        }
        c.vline(2, 6, length - 8, selected ? panelLight : mix(body, 0xFFFFFFFF, 0.3));
        c.vline(23, 6, length - 8, selected ? panelShadow : mix(body, outline, 0.25));
        // little numbered notch dots so the 7 tab positions read as distinct "file tabs"
        for (int i = 0; i < index && i < 7; i++) {
            c.set(5 + i * 2, selected ? 13 : 9, selected ? accent : mix(body, outline, 0.4));
        }
        if (!top) {
            flipVertical(c);
        }
        return c;
    }

    private PixelCanvas scroller(boolean disabled) {
        PixelCanvas c = new PixelCanvas(12, 15);
        int body = disabled ? shade : accent;
        c.fill(1, 1, 10, 13, body);
        c.vline(1, 1, 13, disabled ? mix(shade, 0xFFFFFFFF, 0.4) : accentLight);
        c.hline(1, 1, 10, disabled ? mix(shade, 0xFFFFFFFF, 0.4) : 0xFFFFFFFF);
        c.vline(10, 2, 12, disabled ? mix(shade, outline, 0.3) : accentShadow);
        c.hline(2, 13, 9, disabled ? mix(shade, outline, 0.3) : accentShadow);
        c.roundStroke(0, 0, 12, 15, outline);
        for (int y = 5; y <= 9; y += 2) {
            c.hline(3, y, 6, outline);
        }
        return c;
    }

    private void scrollTrack(PixelCanvas c, int x, int y, int w, int h) {
        c.fill(x + 1, y + 1, w - 2, h - 2, mix(spot, bgAlt, 0.25));
        halftone(c, x + 1, y + 1, w - 2, h - 2, dots(0x60), false);
        c.vline(x + 1, y + 1, h - 2, outline);
        c.roundStroke(x, y, w, h, outline);
    }

    private static void flipVertical(PixelCanvas c) {
        for (int y = 0; y < c.height() / 2; y++) {
            int other = c.height() - 1 - y;
            for (int x = 0; x < c.width(); x++) {
                int a = c.get(x, y);
                c.set(x, y, c.get(x, other));
                c.set(x, other, a);
            }
        }
    }

    // ═════════════════════════ PAUSE MENU / WIDGETS ═════════════════════════
    // Composition inspired by "sign-board" pause menus: framed planks with diagonal grain, bevel
    // light/shadow and corner rivets, a ribbon banner replacing the "Game Menu" title and a featured
    // quick-links button. Everything in black, white and greys.

    /** Nine-slice side border of buttons/sliders: rivets and corners live inside it and never stretch. */
    public static final int WIDGET_SIDE_BORDER = 6;

    public void buttons(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "widget/";
        // grey "wood" planks: mid-grey body, lighter diagonal grain, light frame (white text stays readable)
        int body = 0xFF52525B;
        out.put(dir + "button.png", plank(body, mix(body, 0xFFFFFFFF, 0.12), 0xFFE5E7EB, true));
        int hiBody = 0xFF71717A;
        out.put(dir + "button_highlighted.png", plank(hiBody, mix(hiBody, 0xFFFFFFFF, 0.16), 0xFFFFFFFF, true));
        int off = 0xFF3F3F46;
        out.put(dir + "button_disabled.png", plank(off, off, 0xFF52525B, false));
    }

    /**
     * 200x20 "plank": 1px black ink, 1px light inner frame, bevel (light top, two dark bottom rows),
     * diagonal grain stripes and star rivets inside the side borders. Text area stays clean so long
     * labels ("Opciones del mundo...") never collide with decorations.
     */
    private PixelCanvas plank(int body, int grain, int frame, boolean rivets) {
        int w = 200;
        int h = 20;
        PixelCanvas c = new PixelCanvas(w, h);
        // frame: light ring (inset 1) + bevel ring (inset 2), darker on the bottom/right
        int frameDark = mix(frame, outline, 0.35);
        c.stroke(1, 1, w - 2, h - 2, frame);
        c.hline(1, h - 2, w - 2, frameDark);
        c.vline(w - 2, 1, h - 2, frameDark);
        c.stroke(2, 2, w - 4, h - 4, mix(frame, body, 0.45));
        c.hline(2, h - 3, w - 4, mix(frameDark, body, 0.45));
        c.vline(w - 3, 2, h - 4, mix(frameDark, body, 0.45));
        // body + diagonal grain inside the inner ink line (inset 3)
        c.fill(4, 4, w - 8, h - 8, body);
        if (grain != body) {
            for (int y = 4; y < h - 4; y++) {
                for (int x = WIDGET_SIDE_BORDER; x < w - WIDGET_SIDE_BORDER; x++) {
                    if ((x + y) % 9 < 3) {
                        c.set(x, y, grain);
                    }
                }
            }
        }
        c.hline(4, 4, w - 8, mix(body, 0xFFFFFFFF, 0.25));
        c.hline(4, h - 5, w - 8, mix(body, outline, 0.4));
        c.stroke(3, 3, w - 6, h - 6, outline);
        c.roundStroke(0, 0, w, h, outline);
        if (rivets) {
            rivet(c, 1, h / 2 - 2, 0xFFFFFFFF);
            rivet(c, w - 4, h / 2 - 2, 0xFFFFFFFF);
        }
        return c;
    }

    /** 3x3 star rivet with a black centre. */
    private void rivet(PixelCanvas c, int x, int y, int color) {
        c.set(x + 1, y, color);
        c.set(x, y + 1, color);
        c.set(x + 1, y + 1, outline);
        c.set(x + 2, y + 1, color);
        c.set(x + 1, y + 2, color);
    }

    public void sliders(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "widget/";
        out.put(dir + "slider.png", sliderTrack(mix(spot, 0xFFFFFFFF, 0.45)));
        out.put(dir + "slider_highlighted.png", sliderTrack(0xFFFFFFFF));
        out.put(dir + "slider_handle.png", sliderHandle(paper, shade));
        out.put(dir + "slider_handle_highlighted.png", sliderHandle(0xFFFFFFFF, mix(shade, 0xFFFFFFFF, 0.4)));
    }

    private PixelCanvas sliderTrack(int frame) {
        PixelCanvas c = new PixelCanvas(200, 20);
        int body = mix(spot, outline, 0.35);
        c.fill(2, 2, 196, 16, body);
        c.hline(2, 2, 196, mix(body, outline, 0.6));
        c.vline(2, 2, 16, mix(body, outline, 0.6));
        for (int x = WIDGET_SIDE_BORDER; x < 200 - WIDGET_SIDE_BORDER; x += 4) {
            c.hline(x, 9, 2, mix(body, 0xFFFFFFFF, 0.35));
            c.hline(x, 10, 2, mix(body, 0xFFFFFFFF, 0.15));
        }
        c.roundStroke(0, 0, 200, 20, outline);
        c.roundStroke(1, 1, 198, 18, frame);
        return c;
    }

    private PixelCanvas sliderHandle(int body, int bevel) {
        PixelCanvas c = new PixelCanvas(8, 20);
        c.fill(1, 1, 6, 18, body);
        c.vline(6, 2, 17, bevel);
        c.hline(2, 17, 5, bevel);
        c.hline(1, 1, 6, 0xFFFFFFFF);
        c.roundStroke(0, 0, 8, 20, outline);
        c.hline(2, 7, 4, outline);
        c.hline(2, 9, 4, outline);
        c.hline(2, 11, 4, outline);
        return c;
    }

    public void textFields(Map<String, PixelCanvas> out) {
        String dir = SPRITES + "widget/";
        out.put(dir + "text_field.png", textField(shade));
        out.put(dir + "text_field_highlighted.png", textField(0xFFFFFFFF));
    }

    private PixelCanvas textField(int frame) {
        PixelCanvas c = new PixelCanvas(200, 20);
        c.fill(2, 2, 196, 16, spot);
        c.hline(2, 2, 196, mix(spot, outline, 0.6));
        c.vline(2, 2, 16, mix(spot, outline, 0.6));
        c.stroke(0, 0, 200, 20, outline);
        c.stroke(1, 1, 198, 18, frame);
        return c;
    }

    public void separators(Map<String, PixelCanvas> out) {
        PixelCanvas header = new PixelCanvas(32, 2);
        header.hline(0, 0, 32, outline);
        stitches(header, 1);
        PixelCanvas footer = new PixelCanvas(32, 2);
        stitches(footer, 0);
        footer.hline(0, 1, 32, outline);
        out.put(TEXTURES + "gui/header_separator.png", header);
        out.put(TEXTURES + "gui/footer_separator.png", footer);
        out.put(TEXTURES + "gui/inworld_header_separator.png", header);
        out.put(TEXTURES + "gui/inworld_footer_separator.png", footer);
    }

    private void stitches(PixelCanvas c, int y) {
        for (int x = 0; x < 32; x++) {
            c.set(x, y, (x / 4) % 2 == 0 ? 0xFFFFFFFF : shade);
        }
    }

    public void backgrounds(Map<String, PixelCanvas> out) {
        out.put(TEXTURES + "gui/menu_background.png", tile(0xC8, 0x40));
        out.put(TEXTURES + "gui/menu_list_background.png", tile(0xA0, 0x2C));
        // over the world (pause menu, in-game lists): plain soft dim, no repeating decoration
        out.put(TEXTURES + "gui/inworld_menu_background.png", new PixelCanvas(32, 32).fill(withAlpha(spot, 0x70)));
        out.put(TEXTURES + "gui/inworld_menu_list_background.png", new PixelCanvas(32, 32).fill(withAlpha(spot, 0x50)));
    }

    /** Dark translucent tile with a sparse white star and paw: decoration without flooding the screen. */
    private PixelCanvas tile(int baseAlpha, int decoAlpha) {
        PixelCanvas c = new PixelCanvas(32, 32);
        c.fill(withAlpha(spot, baseAlpha));
        Map<Character, Integer> deco = ink(withAlpha(0xFFFFFFFF, decoAlpha));
        c.pattern(Sprites.STAR_SMALL, 5, 5, deco);
        if (motif.paws()) {
            c.pattern(Sprites.PAW_SMALL, 21, 21, deco);
        } else if (motif.hearts()) {
            c.pattern(Sprites.HEART_SMALL, 21, 22, deco);
        }
        return c;
    }

    /**
     * Ribbon banner with pixel-font text (used as the pause-menu title and the quick-links header):
     * white ribbon with 2px black ink, grey folded tails, puppy badges and a star on top.
     */
    public PixelCanvas banner(String text) {
        String label = PixelFont.normalize(text);
        int scale = 2;
        int textWidth = PixelFont.width(label, scale);
        int w = Math.max(96, textWidth + 64);
        int h = 32;
        PixelCanvas c = new PixelCanvas(w, h);
        int tail = mix(spot, 0xFFFFFFFF, 0.3);
        // tails (behind the body)
        ribbonTail(c, 0, 12, tail, true);
        ribbonTail(c, w - 16, 12, tail, false);
        c.fill(8, 26, 6, 4, outline);
        c.fill(w - 14, 26, 6, 4, outline);
        // body
        int bodyX = 8;
        int bodyW = w - 16;
        c.fill(bodyX + 2, 6, bodyW - 4, 20, paper);
        c.hline(bodyX + 3, 6, bodyW - 6, 0xFFFFFFFF);
        c.hline(bodyX + 2, 23, bodyW - 4, panelShadow);
        c.hline(bodyX + 2, 22, bodyW - 4, mix(paper, panelShadow, 0.5));
        halftone(c, bodyX + 3, 7, 14, 14, dots(0x90), false);
        halftone(c, bodyX + bodyW - 17, 7, 14, 14, dots(0x90), false);
        inkFrame(c, bodyX, 4, bodyW, 22);
        if (motif.puppy()) {
            c.pattern(Sprites.PUPPY, bodyX + 5, 11, ink);
            c.pattern(Sprites.PUPPY, bodyX + bodyW - 14, 11, ink);
        }
        // star badge on top
        c.pattern(Sprites.ICON_STAR, w / 2 - 3, 0, ink);
        // text: grey drop shadow + black ink
        int tx = (w - textWidth) / 2;
        PixelFont.draw(c, label, tx + 1, 10, scale, shade);
        PixelFont.draw(c, label, tx, 9, scale, outline);
        return c;
    }

    private void ribbonTail(PixelCanvas c, int x, int y, int color, boolean left) {
        c.fill(x + 1, y + 1, 14, 16, color);
        c.stroke(x, y, 16, 18, outline);
        for (int i = 0; i < 5; i++) {
            int nx = left ? x + i : x + 15 - i;
            c.vline(nx, y + 9 - (4 - i), (4 - i) * 2 + 1, PixelCanvas.CLEAR);
        }
        for (int i = 0; i < 5; i++) {
            int nx = left ? x + i : x + 15 - i;
            c.set(nx + (left ? 1 : -1), y + 9 - (4 - i) - 1, outline);
            c.set(nx + (left ? 1 : -1), y + 9 + (4 - i) + 1, outline);
        }
    }

    /** Fallback pack.png when no logo is bundled: puppy sticker on a halftone card. */
    public PixelCanvas packIcon() {
        PixelCanvas c = new PixelCanvas(64, 64);
        c.fill(2, 2, 60, 60, bgAlt);
        halftone(c, 2, 2, 60, 60, dots(0xA0), true);
        disc(c, 32, 33, 26, 0xFFFFFFFF, outline);
        disc(c, 32, 33, 24, 0xFFFFFFFF, 0xFFFFFFFF);
        c.pattern(Sprites.PUP_BIG, 8, 12, 3, ink);
        c.pattern(Sprites.ICON_STAR, 52, 50, ink);
        inkFrame(c, 0, 0, 64, 64);
        return c;
    }

    // ═══════════════════════════ FONT ICONS ═══════════════════════════

    /** glyph id → 8px tall icon image. */
    public Map<String, PixelCanvas> icons() {
        Map<String, PixelCanvas> icons = new LinkedHashMap<>();
        icons.put("heart", sprite(Sprites.ICON_HEART));
        String[] empty = Sprites.recolor(Sprites.ICON_HEART, map('P', 'G', 'L', 'G', 'D', 'S', 'H', 'G'));
        PixelCanvas half = sprite(empty);
        half.pattern(Sprites.leftHalf(Sprites.ICON_HEART, 4), 0, 0, ink);
        icons.put("heart_half", half);
        icons.put("heart_empty", sprite(empty));
        icons.put("bone", sprite(Sprites.ICON_BONE));
        icons.put("star", sprite(Sprites.ICON_STAR));
        icons.put("coin", sprite(Sprites.ICON_COIN));
        icons.put("pin", sprite(Sprites.ICON_PIN));
        icons.put("clock", sprite(Sprites.ICON_CLOCK));
        icons.put("paw", sprite(Sprites.ICON_PAW));
        icons.put("shield", sprite(Sprites.ICON_SHIELD));
        icons.put("bubble", sprite(Sprites.ICON_BUBBLE));
        icons.put("puppy", sprite(Sprites.PUPPY));
        icons.put("bar_full", sprite(Sprites.BAR_FULL));
        icons.put("bar_empty", sprite(Sprites.BAR_EMPTY));
        icons.put("dot", sprite(Sprites.DOT));
        icons.put("link", sprite(Sprites.ICON_LINK));
        icons.put("chat", sprite(Sprites.ICON_CHAT));
        icons.put("cart", sprite(Sprites.ICON_CART));
        icons.put("book", sprite(Sprites.ICON_BOOK));
        icons.put("globe", sprite(Sprites.ICON_GLOBE));
        icons.put("bug", sprite(Sprites.ICON_BUG));
        return icons;
    }

    // ═══════════════════════════ comic primitives ═══════════════════════════

    /** Panel: 2px rounded black ink outline, white top/left highlight, hard grey bottom/right shadow. */
    private void comicPanel(PixelCanvas c, int x, int y, int w, int h) {
        c.fill(x + 2, y + 2, w - 4, h - 4, bg);
        c.hline(x + 3, y + 2, w - 6, panelLight);
        c.vline(x + 2, y + 3, h - 6, panelLight);
        c.hline(x + 3, y + h - 3, w - 5, panelShadow);
        c.hline(x + 4, y + h - 4, w - 7, mix(bg, panelShadow, 0.5));
        c.vline(x + w - 3, y + 3, h - 5, panelShadow);
        c.vline(x + w - 4, y + 4, h - 7, mix(bg, panelShadow, 0.5));
        inkFrame(c, x, y, w, h);
        // grey "stitching" just inside the ink
        for (int i = x + 6; i < x + w - 6; i += 4) {
            c.hline(i, y + 3, 2, accentLight);
        }
    }

    /** 2px black outline with rounded (cut) corners. */
    private void inkFrame(PixelCanvas c, int x, int y, int w, int h) {
        c.roundStroke(x, y, w, h, outline);
        c.stroke(x + 1, y + 1, w - 2, h - 2, outline);
        c.set(x, y, PixelCanvas.CLEAR);
        c.set(x + w - 1, y, PixelCanvas.CLEAR);
        c.set(x, y + h - 1, PixelCanvas.CLEAR);
        c.set(x + w - 1, y + h - 1, PixelCanvas.CLEAR);
    }

    /** Recessed comic slot: black ink frame, dark inset top/left, light bottom/right. */
    private void slot(PixelCanvas c, int x, int y) {
        c.fill(x + 1, y + 1, 16, 16, bgAlt);
        int inset = mix(bgAlt, outline, 0.18);
        c.hline(x + 1, y + 1, 16, inset);
        c.vline(x + 1, y + 1, 16, inset);
        int light = mix(bgAlt, 0xFFFFFFFF, 0.7);
        c.hline(x + 2, y + 16, 15, light);
        c.vline(x + 16, y + 2, 15, light);
        c.stroke(x, y, 18, 18, outline);
    }

    /** Larger frame with a grey halftone ring (crafting result). */
    private void bigSlot(PixelCanvas c, int x, int y, int size) {
        c.fill(x + 1, y + 1, size - 2, size - 2, accentPale);
        halftone(c, x + 1, y + 1, size - 2, size - 2, dots(0xD0), false);
        c.roundStroke(x, y, size, size, outline);
        c.roundStroke(x + 1, y + 1, size - 2, size - 2, outline);
    }

    /** Ben-Day dots. fade=true: big dots on the right, shrinking to nothing on the left. */
    private void halftone(PixelCanvas c, int x, int y, int w, int h, int color, boolean fade) {
        if ((color >>> 24) == 0) {
            return;
        }
        for (int yy = 0; yy < h; yy += 3) {
            int offset = (yy / 3) % 2 == 0 ? 0 : 2;
            for (int xx = offset; xx < w; xx += 4) {
                double t = fade ? 1.0 - (double) xx / Math.max(1, w) : 0.0;
                if (t < 0.4) {
                    c.blendFill(x + xx, y + yy, Math.min(2, w - xx), Math.min(2, h - yy), color);
                } else if (t < 0.75) {
                    c.blend(x + xx, y + yy, color);
                }
            }
        }
    }

    /** Dashed ink line (3 on, 2 off). With heart=true a sticker heart is placed in the middle. */
    private void dashedSeparator(PixelCanvas c, int x, int y, int length, boolean heart) {
        for (int i = 0; i < length; i++) {
            if (i % 5 < 3) {
                c.set(x + i, y, outline);
            }
        }
        if (heart && motif.hearts()) {
            int mid = x + length / 2 - 4;
            c.fill(mid - 1, y - 4, 10, 8, bg);
            stickerHeart(c, mid, y - 3);
        }
    }

    private void dottedLine(PixelCanvas c, int x, int y, int length, int color) {
        for (int i = 0; i < length; i += 2) {
            c.set(x + i, y, color);
        }
    }

    /** 7x6 white heart with black ink outline. */
    private void stickerHeart(PixelCanvas c, int x, int y) {
        c.pattern(Sprites.STICKER_HEART, x, y, ink);
    }

    /** Round white "sticker" with black ink outline and Pip (the mascot) inside. */
    private void badge(PixelCanvas c, int cx, int cy, int radius) {
        disc(c, cx + 1, cy + 1, radius, withAlpha(outline, 0xFF), withAlpha(outline, 0xFF));
        disc(c, cx, cy, radius, 0xFFFFFFFF, outline);
        if (radius >= 9) {
            c.pattern(Sprites.PUP_BIG, cx - 8, cy - 7, ink);
        } else {
            c.pattern(Sprites.PUPPY, cx - 4, cy - 4, ink);
        }
    }

    /**
     * Pause-menu header (glyph pause_logo): the plugin logo scaled to {@code height} (optionally greyscale)
     * with Pip on each side, a small star and an ink "ground" line under them.
     */
    public PixelCanvas pauseLogo(java.awt.image.BufferedImage logo, int height, boolean grayscale, boolean pups) {
        int h = Math.max(16, Math.min(250, height));
        int lw = Math.max(1, (int) Math.round(logo.getWidth() * (h / (double) logo.getHeight())));
        java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(lw, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                h * 2 <= logo.getHeight() ? java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
                        : java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(logo, 0, 0, lw, h, null);
        g.dispose();

        int side = pups ? 26 : 0;
        PixelCanvas c = new PixelCanvas(lw + side * 2, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < lw; x++) {
                int argb = scaled.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                if (grayscale) {
                    int lum = (int) Math.round(PixelCanvas.luminance(argb) * 255);
                    // a touch of contrast so the logo reads as black ink + white paper
                    lum = Math.max(0, Math.min(255, (int) Math.round((lum - 128) * 1.25 + 128)));
                    argb = (argb & 0xFF000000) | lum << 16 | lum << 8 | lum;
                }
                c.set(side + x, y, argb);
            }
        }
        if (pups) {
            int py = h - 16;
            c.pattern(Sprites.mirror(Sprites.PUP_BIG), 2, py, ink);
            c.pattern(Sprites.PUP_BIG, c.width() - 18, py, ink);
            c.hline(0, h - 1, 22, outline);
            c.hline(c.width() - 22, h - 1, 22, outline);
            c.pattern(Sprites.ICON_STAR, 8, Math.max(0, py - 10), ink);
            c.pattern(Sprites.ICON_STAR, c.width() - 15, Math.max(0, py - 10), ink);
        }
        return c;
    }

    private static void disc(PixelCanvas c, int cx, int cy, int r, int fill, int edge) {
        double outer = (r + 0.5) * (r + 0.5);
        double inner = (r - 0.5) * (r - 0.5);
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int d = dx * dx + dy * dy;
                if (d <= outer) {
                    c.set(cx + dx, cy + dy, d > inner ? edge : fill);
                }
            }
        }
    }

    private void arrow(PixelCanvas c, int x, int y) {
        c.fill(x, y + 3, 11, 5, outline);
        c.fill(x + 1, y + 4, 10, 3, accent);
        c.hline(x + 1, y + 4, 10, accentLight);
        for (int i = 0; i < 6; i++) {
            c.vline(x + 10 + i, y - 1 + i, 13 - i * 2, outline);
            if (i < 5) {
                c.vline(x + 10 + i, y + i, 11 - i * 2, accent);
            }
        }
        c.vline(x + 10, y + 1, 2, accentLight);
    }

    // ═══════════════════════════ helpers ═══════════════════════════

    private PixelCanvas sprite(String[] rows) {
        PixelCanvas c = new PixelCanvas(rows[0].length(), rows.length);
        c.pattern(rows, 0, 0, ink);
        return c;
    }

    private String[] blink(String[] rows) {
        return Sprites.recolor(rows, map('O', 'W'));
    }

    /** Palette where every "ink" char maps to a single colour (silhouettes / watermarks). */
    private Map<Character, Integer> ink(int color) {
        Map<Character, Integer> m = new HashMap<>();
        for (Character key : ink.keySet()) {
            m.put(key, color);
        }
        return m;
    }

    private static Map<Character, Character> map(char... pairs) {
        Map<Character, Character> m = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }
}
