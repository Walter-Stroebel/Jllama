package nl.infcomtec.simpleimage;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Mark pixels in an image.
 *
 * @author Walter Stroebel
 */
public class Marker {

    private final BitShape mark;
    private final int markMask;
    private final int markColor;

    /**
     * Create a marker.
     *
     * @param mark Should hold the bits (area) to mark.
     * @param markMask Bits to mask from the existing color.
     * @param markColor Bits to add to the existing color.
     */
    public Marker(BitShape mark, int markMask, int markColor) {
        this.mark = mark;
        this.markMask = markMask;
        this.markColor = markColor;
    }

    /**
     * Apply the marker to an image.
     *
     * @param image image to mark (destructive).
     */
    public void mark(BufferedImage image) {
        for (Point p : mark) {
            int c = image.getRGB(p.x, p.y) & markMask;
            image.setRGB(p.x, p.y, c | markColor);
        }
    }

    /**
     * Apply the marker to an image.
     * <p>
     * <b>Note:</b> This is rather memory intensive.</p>
     *
     * @param image image to mark (non-destructive).
     * @return Marked image.
     */
    public BufferedImage markCopy(BufferedImage image) {
        BufferedImage ret = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = ret.createGraphics();
        gr.drawImage(image, 0, 0, null);
        gr.dispose();
        mark(ret);
        return ret;
    }

}
