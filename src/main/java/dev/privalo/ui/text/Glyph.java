package dev.privalo.ui.text;

/**
 * A bitmap glyph registered in {@code assets/minecraft/font/default.json}.
 *
 * @param id           glyph id used by {@code <glyph:id>} / {@code <overlay:id>}
 * @param character    private-use character
 * @param texture      texture resource location (namespace:path.png, relative to textures/)
 * @param imageWidth   PNG width
 * @param imageHeight  PNG height
 * @param visibleWidth last non-transparent column + 1 (what the client measures)
 * @param height       rendered height in font pixels
 * @param ascent       pixels above the baseline
 */
public record Glyph(String id, char character, String texture, int imageWidth, int imageHeight,
                    int visibleWidth, int height, int ascent) {

    /** Horizontal advance the client applies after drawing this glyph. */
    public int advance() {
        double scale = (double) height / (double) imageHeight;
        return (int) Math.floor(0.5 + visibleWidth * scale) + 1;
    }

    public String asString() {
        return String.valueOf(character);
    }
}
