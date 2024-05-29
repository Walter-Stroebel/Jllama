package nl.infcomtec.jllama;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts text to image if text is valid SVG.
 *
 * @author Walter Stroebel
 */
public class ModalitySVG extends Modality {

    public ModalitySVG(ExecutorService pool, String currentText) {
        super(pool, currentText);
    }

    @Override
    protected void convert() {

        // Run ImageMagick
        try {
            ProcessBuilder pb = new ProcessBuilder("convert", "svg:" + outputFile.getAbsolutePath(),
                    pngOutputFile.getAbsolutePath());
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
