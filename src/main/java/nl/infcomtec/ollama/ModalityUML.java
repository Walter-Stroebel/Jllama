package nl.infcomtec.ollama;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import static nl.infcomtec.ollama.Ollama.WORK_DIR;

/**
 * Converts text to image if text is valid PlantUML.
 *
 * @author Walter Stroebel
 */
public class ModalityUML extends Modality {

    public ModalityUML(ExecutorService pool, String currentText) {
        super(pool, currentText);
    }

    @Override
    protected void convert() {

        // Run PlantUML with PNG output
        try {
            ProcessBuilder pb = new ProcessBuilder("plantuml", "-tpng", outputFile.getAbsolutePath());
            pb.directory(WORK_DIR);
            Process process = pb.start();
            process.waitFor();
            image = ImageIO.read(pngOutputFile);
        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
