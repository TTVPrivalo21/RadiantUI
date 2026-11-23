package dev.privalo.ui.pack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/** Tiny ARGB pixel-art canvas used to paint every generated texture. */
public final class PixelCanvas {

    public static final int CLEAR = 0x00000000;

    private final BufferedImage image;
    private final int width;
    private final int height;

    public PixelCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public static PixelCanvas of(BufferedImage source) {
        PixelCanvas canvas = new PixelCanvas(source.getWidth(), source.getHeight());
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                canvas.image.setRGB(x, y, source.getRGB(x, y));
            }
        }
        return canvas;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void set(int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            image.setRGB(x, y, argb);
        }
    }

    /** Alpha-composites {@code argb} over the existing pixel ("source over"). */
    public void blend(int x, int y, int argb) {
        int sa = argb >>> 24;
        if (sa == 0xFF) {
            set(x, y, argb);
            return;
        }
        if (sa == 0 || x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int dst = image.getRGB(x, y);
        int da = dst >>> 24;
        if (da == 0) {
            set(x, y, argb);
            return;
        }
        double a = sa / 255.0;
        double outA = a + (da / 255.0) * (1 - a);
        int r = channel(argb >> 16, dst >> 16, a, da, outA);
        int g = channel(argb >> 8, dst >> 8, a, da, outA);
        int b = channel(argb, dst, a, da, outA);
        set(x, y, (int) Math.round(outA * 255) << 24 | r << 16 | g << 8 | b);
    }

    private static int channel(int src, int dst, double a, int da, double outA) {
        double s = (src & 0xFF) * a;
        double d = (dst & 0xFF) * (da / 255.0) * (1 - a);
        return (int) Math.max(0, Math.min(255, Math.round((s + d) / outA)));
    }

    /** Like {@link #fill(int, int, int, int, int)} but alpha-composited. */
    public PixelCanvas blendFill(int x, int y, int w, int h, int argb) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                blend(xx, yy, argb);
            }
        }
        return this;
    }

    public int get(int x, int y) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            return image.getRGB(x, y);
        }
        return CLEAR;
    }

    public PixelCanvas fill(int x, int y, int w, int h, int argb) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                set(xx, yy, argb);
            }
        }
        return this;
    }

    public PixelCanvas fill(int argb) {
        return fill(0, 0, width, height, argb);
    }

    public PixelCanvas clear(int x, int y, int w, int h) {
        return fill(x, y, w, h, CLEAR);
    }

    public PixelCanvas hline(int x, int y, int length, int argb) {
        return fill(x, y, length, 1, argb);
    }

    public PixelCanvas vline(int x, int y, int length, int argb) {
        return fill(x, y, 1, length, argb);
    }

    public PixelCanvas stroke(int x, int y, int w, int h, int argb) {
        hline(x, y, w, argb);
        hline(x, y + h - 1, w, argb);
        vline(x, y, h, argb);
        vline(x + w - 1, y, h, argb);
        return this;
    }

    /** Rectangle outline with the 4 corner pixels left out (soft rounded look). */
    public PixelCanvas roundStroke(int x, int y, int w, int h, int argb) {
        hline(x + 1, y, w - 2, argb);
        hline(x + 1, y + h - 1, w - 2, argb);
        vline(x, y + 1, h - 2, argb);
        vline(x + w - 1, y + 1, h - 2, argb);
        return this;
    }

    /** Draws an ASCII pattern. '.' and ' ' are transparent/skipped; unknown chars are skipped. */
    public PixelCanvas pattern(String[] rows, int ox, int oy, Map<Character, Integer> colors) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length(); c++) {
                Integer color = colors.get(row.charAt(c));
                if (color != null) {
                    blend(ox + c, oy + r, color);
                }
            }
        }
        return this;
    }

    /** Draws a pattern scaled by an integer factor (nearest neighbour). */
    public PixelCanvas pattern(String[] rows, int ox, int oy, int scale, Map<Character, Integer> colors) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length(); c++) {
                Integer color = colors.get(row.charAt(c));
                if (color != null) {
                    blendFill(ox + c * scale, oy + r * scale, scale, scale, color);
                }
            }
        }
        return this;
    }

    /** Index of the last column containing a visible pixel + 1 (Minecraft's bitmap glyph width). */
    public int visibleWidth() {
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < height; y++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    return x + 1;
                }
            }
        }
        return 0;
    }

    public BufferedImage image() {
        return image;
    }

    public byte[] png() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // ── colour helpers ──

    public static int withAlpha(int argb, int alpha) {
        return (alpha & 0xFF) << 24 | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, double t) {
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF, aa = a >>> 24;
        int br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF, ba = b >>> 24;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        int al = (int) Math.round(aa + (ba - aa) * t);
        return al << 24 | r << 16 | g << 8 | bl;
    }

    public static double luminance(int argb) {
        double r = (argb >> 16 & 0xFF) / 255.0;
        double g = (argb >> 8 & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
