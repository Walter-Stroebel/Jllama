package nl.infcomtec.ollama;

import java.util.logging.Level;
import java.util.logging.Logger;
import static nl.infcomtec.ollama.Ollama.WORK_DIR;

/**
 * Converts text to image if text is valid PlantUML.
 *
 * @author Walter Stroebel
 */
public class ModalityDOT extends GraphModality {

    public ModalityDOT(String currentText) {
        super(currentText);
    }

    @Override
    void convert() {

        // Run Graphviz
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "-o",
                    pngOutputFile.getAbsolutePath(),
                    outputFile.getAbsolutePath());
            pb.directory(WORK_DIR);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
