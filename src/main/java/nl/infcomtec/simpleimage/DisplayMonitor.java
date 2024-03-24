package nl.infcomtec.simpleimage;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * Simple utility class to get information about the system's monitor(s).
 *
 * The getter functions are optional as the members are public and final.
 *
 * @author walter
 */
public class DisplayMonitor {

    public final GraphicsEnvironment grEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    public final GraphicsDevice defaultScreen = grEnv.getDefaultScreenDevice();
    public final DisplayMode displayMode = defaultScreen.getDisplayMode();

    public GraphicsEnvironment getGraphicsEnvironment() {
        return grEnv;
    }

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
