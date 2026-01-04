package nl.infcomtec.simpleimage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.UIManager;

/**
 * Collection of actions one can do to an ImageObject.
 *
 * @author walter
 */
public class ImageActions {

    private final ImageObject io;
    private boolean changed = false;

    public ImageActions(ImageObject io) {
        this.io = io;
    }

    public int getPixel(int x, int y) {
        return io.getImage().getRGB(x, y);
    }

    public synchronized void setPixel(int x, int y, int rgb) {
        io.getImage().setRGB(x, y, rgb);
        changed = true;
    }

    public Color getColor(int x, int y) {
        return new Color(getPixel(x, y));
    }

    /**
     * Draw a string at the indicated position.
     *
     * @param font Font to use.
     * @param x X.
     * @param y Y.
     * @param str String to draw.
     */
    public synchronized void drawString(Font font, int x, int y, String str) {
        Graphics gr = io.getGraphics();
        Font oldFont = gr.getFont();
        gr.setFont(font);
        gr.drawString(str, x, y);
        gr.setFont(oldFont);
        changed = true;
    }

    /**
     * Draw a string at the indicated position.
     *
     * @param x X.
     * @param y Y.
     * @param str String to draw.
     */
    public void drawString(int x, int y, String str) {
        drawString(UIManager.getFont("Label.font"), x, y, str);
    }

    public void setColor(int x, int y, Color color) {
        io.getImage().setRGB(x, y, color.getRGB());
        changed = true;
    }

    /**
     * If any actions changed the image, signal any observers.
     *
     * This will reset the changed state.
     */
    public synchronized void callObserversIfChanged() {
        if (changed) {
            io.putImage(null);
        }
        changed = false;
    }
}
