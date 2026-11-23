package dev.privalo.ui.pack;

import java.util.Map;

/**
 * Hand-made pixel-art patterns. Legend (resolved by {@link TexturePainter}):
 * <pre>
 *  O outline (black)      S spot (dark patch)     W paper (white)     G shade (grey)
 *  b background-alt       B background            P light fill        L light shade 
 *  p accent-pale          D accent shadow         H highlight         . transparent
 * </pre>
 */
public final class Sprites {

    private Sprites() {
    }

    // ───────────── 9x9 HUD sprites ─────────────

    public static final String[] HEART = {
            ".OO...OO.",
            "OPPO.OPPO",
            "OHLPOPPPO",
            "OLPPPPPDO",
            "OPPPPPPDO",
            ".OPPPPDO.",
            "..OPPDO..",
            "...ODO...",
            "....O....",
    };

    public static final String[] BONE = {
            ".........",
            ".........",
            ".OO...OO.",
            "OWWOOOWWO",
            ".OWWWWWO.",
            "OWGOOOGWO",
            ".OO...OO.",
            ".........",
            ".........",
    };

    public static final String[] SHIELD = {
            ".OOOOOOO.",
            "OWWPWPWWO",
            "OWWPPPWWO",
            "OWWWPWWWO",
            "OWWWWWWGO",
            ".OWWWWGO.",
            ".OWWWWGO.",
            "..OWWGO..",
            "...OOO...",
    };

    public static final String[] BUBBLE = {
            "...OOO...",
            ".OOWWWOO.",
            ".OHLWWWO.",
            "OWLWWWWWO",
            "OWWWWWWpO",
            "OWWWWWpLO",
            ".OWWWpLO.",
            ".OOpLLOO.",
            "...OOO...",
    };

    public static final String[] BUBBLE_POP = {
            "...P.P...",
            ".P.....P.",
            ".........",
            "P.......P",
            ".........",
            "P.......P",
            ".........",
            ".P.....P.",
            "...P.P...",
    };

    // ───────────── 8px-tall font icons ─────────────

    public static final String[] ICON_HEART = {
            ".OO.OO.",
            "OHPOPPO",
            "OLPPPDO",
            "OPPPPDO",
            ".OPPDO.",
            "..ODO..",
            "...O...",
            ".......",
    };

    public static final String[] ICON_BONE = {
            ".........",
            ".OO...OO.",
            "OWWOOOWWO",
            ".OWWWWWO.",
            "OWGOOOGWO",
            ".OO...OO.",
            ".........",
            ".........",
    };

    public static final String[] ICON_STAR = {
            "...O...",
            "..OPO..",
            "OOOPOOO",
            "OPPPPPO",
            ".OPPPO.",
            ".OPOPO.",
            "OPO.OPO",
            "OO...OO",
    };

    public static final String[] ICON_COIN = {
            "..OOO..",
            ".OPPPO.",
            "OPLPPDO",
            "OPPWPDO",
            "OPPPPDO",
            ".ODDDO.",
            "..OOO..",
            ".......",
    };

    public static final String[] ICON_PIN = {
            ".OOOOO.",
            "OPPPPPO",
            "OPPWPPO",
            "OPPPPPO",
            ".OPPPO.",
            "..OPO..",
            "...O...",
            ".......",
    };

    public static final String[] ICON_CLOCK = {
            "..OOO..",
            ".OWWWO.",
            "OWWOWWO",
            "OWWOOWO",
            "OWWWWWO",
            ".OWWWO.",
            "..OOO..",
            ".......",
    };

    public static final String[] ICON_PAW = {
            "..P.P..",
            ".......",
            "P.....P",
            "..PPP..",
            ".PPPPP.",
            ".PPPPP.",
            "..P.P..",
            ".......",
    };

    public static final String[] ICON_SHIELD = {
            "OOOOOOO",
            "OWWPWWO",
            "OWPPPWO",
            "OWWPWWO",
            "OWWWWGO",
            ".OWWGO.",
            "..OGO..",
            "...O...",
    };

    public static final String[] ICON_BUBBLE = {
            "..OOO..",
            ".OHWWO.",
            "OHWWWWO",
            "OWWWWpO",
            "OWWWpLO",
            ".OWpLO.",
            "..OOO..",
            ".......",
    };

    /** Friendly white puppy face with black floppy ears and grey cheeks. */
    public static final String[] PUPPY = {
                ".......OOOOOO....",
                "......OWWWWWWO...",
                ".....OWWWWWWWOO..",
                "....OWWWWWSWWWO..",
                "...OWWWWWSWWWSO..",
                "..OWWWWWWWWWWSO..",
                "..OWWWWWWWWWWSO..",
                ".OOWWWWWWWWWWSO..",
                "OOWWWWWWWWWWWWSO.",
                "OWWWOOOWWWWWWWSO.",
                "OWWO...OWWWWWWSO.",
                "OWWO...OWWWWWWSO.",
                "OWWOOOOWWWWWWWSO.",
                ".OWWWWWWWWWWWWSO.",
                "..OOOOOOOOOOOWWO.",
                "............OOO.."
    };

    /**
     * "Pip", RadiantUI's original mascot (16x14, facing left): round white head, short muzzle,
     * black nose, pointed black ear, black eye patch with a white glint and a grey collar with a tag.
     */
    public static final String[] PUP_BIG = {
            ".......OOOOOO....",
            "......OWWWWWWO...",
            ".....OWWWWWWWOO..",
            "....OWWWWWSWWWO..",
            "...OWWWWWSWWWSO..",
            "..OWWWWWWWWWWSO..",
            "..OWWWWWWWWWWSO..",
            ".OOWWWWWWWWWWSO..",
            "OOWWWWWWWWWWWWSO.",
            "OWWWOOOWWWWWWWSO.",
            "OWWO...OWWWWWWSO.",
            "OWWO...OWWWWWWSO.",
            "OWWOOOOWWWWWWWSO.",
            ".OWWWWWWWWWWWWSO.",
            "..OOOOOOOOOOOWWO.",
            "............OOO.."
    };

    /** Horizontally mirrored copy of a pattern (e.g. a pup facing right). */
    public static String[] mirror(String[] rows) {
        String[] out = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            out[i] = new StringBuilder(rows[i]).reverse().toString();
        }
        return out;
    }

    public static final String[] BAR_FULL = {
            "...",
            "OOO",
            "OLO",
            "OPO",
            "OPO",
            "ODO",
            "OOO",
            "...",
    };

    public static final String[] BAR_EMPTY = {
            "...",
            "OOO",
            "ObO",
            "ObO",
            "ObO",
            "ObO",
            "OOO",
            "...",
    };

    public static final String[] DOT = {
            "..",
            "..",
            "..",
            "PP",
            "PP",
            "..",
            "..",
            "..",
    };

    public static final String[] ICON_LINK = {
            "OOO.OOO",
            "O....PO",
            "O...P.O",
            "O..P...",
            "O.P...O",
            "O.....O",
            "OOOOOOO",
            ".......",
    };

    public static final String[] ICON_CHAT = {
            ".OOOOO.",
            "OWWWWWO",
            "OWPWPWO",
            "OWWWWWO",
            ".OWOOO.",
            ".OO....",
            "O......",
            ".......",
    };

    public static final String[] ICON_CART = {
            "O......",
            ".OOOOOO",
            ".OPLPPO",
            ".OPPPO.",
            "..OOOO.",
            "..O..O.",
            ".OO.OO.",
            ".......",
    };

    public static final String[] ICON_BOOK = {
            "OOO.OOO",
            "OWWOWWO",
            "OWPOWWO",
            "OWWOWPO",
            "OWPOWWO",
            "OWWOWWO",
            "OOOOOOO",
            ".......",
    };

    public static final String[] ICON_GLOBE = {
            "..OOO..",
            ".OPWPO.",
            "OPWPWPO",
            "OWWPWWO",
            "OPWPWPO",
            ".OPWPO.",
            "..OOO..",
            ".......",
    };

    public static final String[] ICON_BUG = {
            ".O...O.",
            "..OOO..",
            ".OPPPO.",
            "OOPOPOO",
            ".OPPPO.",
            "OOPPPOO",
            "..OOO..",
            ".......",
    };

    // ───────────── decoration ─────────────

    public static final String[] STAR_SMALL = {
            "..P..",
            ".PPP.",
            "PPPPP",
            ".PPP.",
            ".P.P.",
    };

    /** Small heart with black ink outline and a white shine (comic sticker). */
    public static final String[] STICKER_HEART = {
            ".OO.OO.",
            "OHPOPPO",
            "OPPPPDO",
            ".OPPDO.",
            "..ODO..",
            "...O...",
    };

    public static final String[] PAW_SMALL = {
            "P.P.P",
            ".....",
            ".PPP.",
            ".PPP.",
            "..P..",
    };

    public static final String[] HEART_SMALL = {
            "PP.PP",
            "PPPPP",
            ".PPP.",
            "..P..",
    };

    /** Returns a copy of the pattern with characters substituted. */
    public static String[] recolor(String[] rows, Map<Character, Character> mapping) {
        String[] out = new String[rows.length];
        for (int r = 0; r < rows.length; r++) {
            StringBuilder sb = new StringBuilder(rows[r].length());
            for (char c : rows[r].toCharArray()) {
                Character mapped = mapping.get(c);
                sb.append(mapped != null ? mapped : c);
            }
            out[r] = sb.toString();
        }
        return out;
    }

    /** Keeps only columns [0, keepColumns), rest transparent (half hearts/bones/shields). */
    public static String[] leftHalf(String[] rows, int keepColumns) {
        String[] out = new String[rows.length];
        for (int r = 0; r < rows.length; r++) {
            StringBuilder sb = new StringBuilder(rows[r]);
            for (int c = keepColumns; c < sb.length(); c++) {
                sb.setCharAt(c, '.');
            }
            out[r] = sb.toString();
        }
        return out;
    }

    /** Puts single characters at the given (x,y) pairs. */
    public static String[] stamp(String[] rows, char ch, int... xy) {
        String[] out = rows.clone();
        for (int i = 0; i + 1 < xy.length; i += 2) {
            int x = xy[i];
            int y = xy[i + 1];
            if (y >= 0 && y < out.length && x >= 0 && x < out[y].length()) {
                StringBuilder sb = new StringBuilder(out[y]);
                sb.setCharAt(x, ch);
                out[y] = sb.toString();
            }
        }
        return out;
    }
}
