package dev.privalo.ui.update;

import java.util.Locale;

/** Lenient semantic-version helpers: "v1.2.0", "1.2", "1.2.0-beta" ... */
public final class SemVer {

    private SemVer() {
    }

    /** Removes a leading "v"/"V" (v1.2.0 → 1.2.0) and surrounding spaces. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.length() > 1 && (s.charAt(0) == 'v' || s.charAt(0) == 'V') && Character.isDigit(s.charAt(1))) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * Compares numerically by segment (1.10.0 > 1.9.9). With equal numbers, a version with a
     * pre-release suffix (-beta, -SNAPSHOT, -rc1) is older than the plain release.
     */
    public static int compare(String a, String b) {
        String[] pa = split(normalize(a));
        String[] pb = split(normalize(b));
        String[] na = pa[0].split("\\.");
        String[] nb = pb[0].split("\\.");
        int length = Math.max(na.length, nb.length);
        for (int i = 0; i < length; i++) {
            long x = i < na.length ? number(na[i]) : 0;
            long y = i < nb.length ? number(nb[i]) : 0;
            if (x != y) {
                return Long.compare(x, y);
            }
        }
        boolean preA = !pa[1].isEmpty();
        boolean preB = !pb[1].isEmpty();
        if (preA != preB) {
            return preA ? -1 : 1;
        }
        return pa[1].toLowerCase(Locale.ROOT).compareTo(pb[1].toLowerCase(Locale.ROOT));
    }

    public static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    /** [numeric part, suffix] — "1.2.0-beta+5" → ["1.2.0", "beta+5"]. */
    private static String[] split(String version) {
        int i = 0;
        while (i < version.length() && (Character.isDigit(version.charAt(i)) || version.charAt(i) == '.')) {
            i++;
        }
        String numeric = version.substring(0, i);
        String suffix = version.substring(i);
        while (!suffix.isEmpty() && (suffix.charAt(0) == '-' || suffix.charAt(0) == '+' || suffix.charAt(0) == '_')) {
            suffix = suffix.substring(1);
        }
        return new String[]{numeric.isEmpty() ? "0" : numeric, suffix};
    }

    private static long number(String part) {
        try {
            return part.isEmpty() ? 0 : Long.parseLong(part);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
