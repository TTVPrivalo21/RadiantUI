package dev.privalo.ui.text;

import dev.privalo.ui.hook.PapiBridge;
import dev.privalo.ui.theme.Palette;
import dev.privalo.ui.theme.Theme;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MiniMessage with RadiantUI tags: {@code <space:px>}, {@code <glyph:id>},
 * {@code <overlay:id[:dx]>} and theme colour tags ({@code <accent>}, {@code <dark>}, ...).
 */
public final class TextService {

    public static final Key DEFAULT_FONT = Key.key("minecraft", "default");

    private final GlyphRegistry glyphs;
    private final MiniMessage miniMessage;
    private final TagResolver radiantTags;

    public TextService(GlyphRegistry glyphs) {
        this.glyphs = glyphs;
        this.miniMessage = MiniMessage.miniMessage();
        this.radiantTags = TagResolver.resolver(
                TagResolver.resolver("space", (args, ctx) ->
                        Tag.selfClosingInserting(SpaceFont.component(args.hasNext() ? args.pop().asInt().orElse(0) : 0))),
                TagResolver.resolver("glyph", (args, ctx) ->
                        Tag.selfClosingInserting(glyph(args.hasNext() ? args.pop().value() : ""))),
                TagResolver.resolver("overlay", (args, ctx) -> {
                    String id = args.hasNext() ? args.pop().value() : "";
                    int dx = args.hasNext() ? args.pop().asInt().orElse(0) : 0;
                    return Tag.selfClosingInserting(overlay(id, dx));
                }));
    }

    /** Icon glyph, untinted and without shadow. */
    public Component glyph(String id) {
        Glyph glyph = glyphs.get(id);
        if (glyph == null) {
            return Component.empty();
        }
        return Component.text(glyph.asString())
                .font(DEFAULT_FONT)
                .color(NamedTextColor.WHITE)
                .shadowColor(ShadowColor.none());
    }

    /**
     * Draws a glyph without moving the cursor: {@code space(dx) + glyph + space(-(dx + advance))}.
     */
    public Component overlay(String id, int dx) {
        Glyph glyph = glyphs.get(id);
        if (glyph == null) {
            return Component.empty();
        }
        return Component.text()
                .append(SpaceFont.component(dx))
                .append(glyph(id))
                .append(SpaceFont.component(-(dx + glyph.advance())))
                .build();
    }

    public TagResolver themeTags(Theme theme) {
        List<TagResolver> resolvers = new ArrayList<>();
        Palette palette = theme.palette();
        for (String key : palette.asMap().keySet()) {
            resolvers.add(Placeholder.styling(key, palette.text(key)));
            if (key.contains("-")) {
                resolvers.add(Placeholder.styling(key.replace('-', '_'), palette.text(key)));
            }
        }
        return TagResolver.resolver(resolvers);
    }

    /** Parses MiniMessage with RadiantUI + theme tags. */
    public Component parse(String input, Theme theme, TagResolver... extra) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        TagResolver resolver = TagResolver.builder()
                .resolver(TagResolver.standard())
                .resolver(radiantTags)
                .resolver(themeTags(theme))
                .resolvers(extra)
                .build();
        return miniMessage.deserialize(input, resolver);
    }

    /**
     * Full pipeline for configurable lines: {key} placeholders → PlaceholderAPI → legacy codes → MiniMessage.
     */
    public Component render(Player player, Theme theme, String template, Map<String, String> placeholders) {
        return parse(prepare(player, template, placeholders), theme);
    }

    public String prepare(Player player, String template, Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }
        String text = template;
        if (placeholders != null && text.indexOf('{') >= 0) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                text = text.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        if (player != null) {
            text = PapiBridge.apply(player, text);
        }
        return Legacy.toMiniMessage(text);
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }
}
