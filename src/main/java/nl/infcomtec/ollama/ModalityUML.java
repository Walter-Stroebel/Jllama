package nl.infcomtec.ollama;

import java.util.logging.Level;
import java.util.logging.Logger;
import static nl.infcomtec.ollama.Ollama.WORK_DIR;
import static nl.infcomtec.ollama.Ollama.displayImage;

/**
 * Converts text to image if text is valid PlantUML.
 *
 * @author Walter Stroebel
 */
public class ModalityUML extends GraphModality {

    public ModalityUML(String currentText) {
        super(currentText);
    }

    @Override
    void convert() {

        // Run PlantUML with PNG output
        try {
            ProcessBuilder pb = new ProcessBuilder("plantuml", "-tpng", outputFile.getAbsolutePath());
            pb.directory(WORK_DIR);
            Process process = pb.start();
            process.waitFor();
            displayImage(pngOutputFile);
        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
