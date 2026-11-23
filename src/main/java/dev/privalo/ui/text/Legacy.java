package dev.privalo.ui.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts legacy §-codes (as returned by many PlaceholderAPI expansions) into MiniMessage tags. */
public final class Legacy {

    private static final Pattern BUNGEE_HEX = Pattern.compile("§x((?:§[0-9a-fA-F]){6})");
    private static final Pattern AMP_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern CODE = Pattern.compile("§([0-9a-fk-orA-FK-OR])");

    private Legacy() {
    }

    public static String toMiniMessage(String input) {
        if (input == null || (input.indexOf('§') < 0 && !input.contains("&#"))) {
            return input;
        }
        String out = replace(BUNGEE_HEX, input, m -> "<#" + m.group(1).replace("§", "") + ">");
        out = replace(AMP_HEX, out, m -> "<#" + m.group(1) + ">");
        out = replace(CODE, out, m -> tag(Character.toLowerCase(m.group(1).charAt(0))));
        return out;
    }

    private static String replace(Pattern pattern, String input, java.util.function.Function<Matcher, String> fn) {
        Matcher m = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(fn.apply(m)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String tag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            default -> "<reset>";
        };
    }
}
