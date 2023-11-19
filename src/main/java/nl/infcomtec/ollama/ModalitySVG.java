package nl.infcomtec.ollama;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts text to image if text is valid SVG.
 *
 * @author Walter Stroebel
 */
public class ModalitySVG extends GraphModality {

    public ModalitySVG(String currentText) {
        super(currentText);
    }

    @Override
    void convert() {

        // Run ImageMagick
        try {
            ProcessBuilder pb = new ProcessBuilder("convert", "svg:" + outputFile.getAbsolutePath(), pngOutputFile.getAbsolutePath());
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
