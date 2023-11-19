package nl.infcomtec.simpleimage;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Look-Up table.
 *
 * @author Walter Stroebel
 */
public class LUT {

    public final int[] r = new int[256];
    public final int[] g = new int[256];
    public final int[] b = new int[256];

    /**
     * Create a LUT that does nothing.
     *
     * @return The identity LUT.
     */
    public static LUT unity() {
        LUT ret = new LUT();
        for (int c = 0; c < 256; c++) {
            ret.r[c] = ret.g[c] = ret.b[c] = c;
        }
        return ret;
    }

    /**
     * Create a LUT that inverts (makes negative) the source.
     *
     * @return a LUT that inverts (makes negative) the source.
     */
    public static LUT inverse() {
        return unity().invert();
    }
    private static final double LOG_POINTFIVE = -0.6931471805599453;

    /**
     * The function used has the effect of making bigger values bigger and
     * smaller values smaller when amt > 0.5. It has the effect of making bigger
     * values smaller and smaller values bigger when amt < 0.5.
     *
     * @param amt Use this for contrast with a value for amt of something
     * greater than 0.5 if you want the image to have more contrast, or less
     * than 0.5 if you want the image to have less contrast.
     * @return The requested gain LUT.
     */
    public static LUT gain(double amt) {
        LUT ret = new LUT();
        for (int c = 0; c < 256; c++) {
            int g = (int) Math.round(gain(c / 255.0, amt) * 255.0);
            if (g > 255) {
                g = 255;
            }
            if (g < 0) {
                g = 0;
            }
            ret.r[c] = ret.g[c] = ret.b[c] = g;
        }
        return ret;
    }

    private static double gain(double a, double b) {

        if (a < .001) {
            return 0.;
        }
        if (a > .999) {
            return 1.0;
        }
        double p = Math.log(1.0 - b) / LOG_POINTFIVE;
        if (a < 0.5) {
            return Math.pow(2 * a, p) / 2;
        }
        return 1.0 - Math.pow(2 * (1.0 - a), p) / 2;
    }

    /**
     * Invert (make negative) the LUT.
     *
     * @return The inverted LUT.
     */
    public LUT invert() {
        LUT ret = new LUT();
        for (int c = 0; c < 256; c++) {
            ret.r[c] = 255 - r[c];
            ret.g[c] = 255 - g[c];
            ret.b[c] = 255 - b[c];
        }
        return ret;
    }

    /**
     * Create a LUT based on the square root of the source value. This amplifies
     * dark areas.
     *
     * @param ofs Add to the multiplication factor.
     * @return the LUT.
     */
    public static LUT sqrt(double ofs) {
        LUT ret = new LUT();
        double f = 16.0 + ofs;
        for (int c = 0; c < 256; c++) {
            int c2 = (int) Math.round(Math.sqrt(c) * f);
            if (c2 > 255) {
                c2 = 255;
            }
            if (c2 < 0) {
                c2 = 0;
            }
            ret.r[c] = ret.g[c] = ret.b[c] = c2;
        }
        return ret;
    }

    /**
     * Create a lookup table based on Color.brighter().
     *
     * @return a lookup table based on Color.brighter().
     */
    public static LUT brighter() {
        LUT ret = new LUT();
        for (int i = 0; i < 256; i++) {
            Color c = new Color(i, i, i).brighter();
            ret.r[i] = c.getRed();
            ret.g[i] = c.getGreen();
            ret.b[i] = c.getBlue();
        }
        return ret;
    }

    /**
     * Create a lookup table based on Color.darker().
     *
     * @return a lookup table based on Color.darker().
     */
    public static LUT darker() {
        LUT ret = new LUT();
        for (int i = 0; i < 256; i++) {
            Color c = new Color(i, i, i).darker();
            ret.r[i] = c.getRed();
            ret.g[i] = c.getGreen();
            ret.b[i] = c.getBlue();
        }
        return ret;
    }

    /**
     * Create a LUT based on the square root of the source value. This amplifies
     * dark areas.
     *
     * @param r add to red
     * @param g add to green
     * @param b add to blue
     * @return the LUT.
     */
    public static LUT sqrt(double r, double g, double b) {
        LUT ret = new LUT();
        double fr = 16.0 + r;
        double fg = 16.0 + g;
        double fb = 16.0 + b;
        for (int c = 0; c < 256; c++) {
            int c2 = (int) Math.round(Math.sqrt(c) * fr);
            if (c2 > 255) {
                c2 = 255;
            }
            if (c2 < 0) {
                c2 = 0;
            }
            ret.r[c] = c2;
        }
        for (int c = 0; c < 256; c++) {
            int c2 = (int) Math.round(Math.sqrt(c) * fg);
            if (c2 > 255) {
                c2 = 255;
            }
            if (c2 < 0) {
                c2 = 0;
            }
            ret.g[c] = c2;
        }
        for (int c = 0; c < 256; c++) {
            int c2 = (int) Math.round(Math.sqrt(c) * fb);
            if (c2 > 255) {
                c2 = 255;
            }
            if (c2 < 0) {
                c2 = 0;
            }
            ret.b[c] = c2;
        }
        return ret;
    }

    /**
     * Create a LUT based on the square root of the square root of the source
     * value. This drastically amplifies dark areas.
     *
     * @return the LUT.
     */
    public static LUT sqrt2() {
        LUT ret = new LUT();
        double f = 64.0;
        for (int c = 0; c < 256; c++) {
            int c2 = (int) Math.round(Math.sqrt(Math.sqrt(c)) * f);
            if (c2 > 255) {
                c2 = 255;
            }
            ret.r[c] = ret.g[c] = ret.b[c] = c2;
        }
        return ret;
    }

    public Color lut(int rgb) {
        return lut(new Color(rgb));
    }

    public Color lut(Color c) {
        return new Color(r[c.getRed()], g[c.getGreen()], b[c.getBlue()]);
    }

    public int lut(int rv, int gv, int bv) {
        return (r[rv & 0xFF] << 16) + (g[gv & 0xFF] << 8) + b[bv & 0xFF];
    }

    public BufferedImage apply(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = source.getRGB(x, y);
                int rv = (p & 0xFF0000) >> 16;
                int gv = (p & 0xFF00) >> 8;
                int bv = p & 0xFF;
                image.setRGB(x, y, (p & 0xFF000000) + (r[rv] << 16) + (g[gv] << 8) + b[bv]);
            }
        }
        return image;
    }
}
