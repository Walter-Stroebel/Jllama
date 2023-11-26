package nl.infcomtec.ollama;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Overall configuration.
 *
 * @author Walter Stroebel
 */
public class Config {

    public int x;
    public int y;
    public int w;
    public int h; // of the chat window
    public float fontSize; // to keep things readable
    public String[] ollamas;
    public Boolean streaming = false;
    public Boolean chatMode = true;
    public String lastEndpoint;

    public void update(Rectangle bounds) {
        x = bounds.x;
        y = bounds.y;
        w = bounds.width;
        h = bounds.height;
        update();
    }

    public void update() {
        try {
            Ollama.getMapper().writeValue(Ollama.configFile, this);
        } catch (IOException ex) {
            Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
