/*
 */
package nl.infcomtec.simpleimage;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 *
 * @author walter
 */
public class DisplayMonitor {

    public final GraphicsEnvironment grEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    public final GraphicsDevice defaultScreen = grEnv.getDefaultScreenDevice();
    public final DisplayMode displayMode = defaultScreen.getDisplayMode();

    public Rectangle getDefaultBounds() {
        return defaultScreen.getDefaultConfiguration().getBounds();
    }

    public int getDefaultWidth() {
        return displayMode.getWidth();
    }

    public int getDefaultHeight() {
        return displayMode.getHeight();
    }
}
