package nl.infcomtec.jllama;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nl.infcomtec.jllama.Ollama.WORK_DIR;

/**
 * Converts text to image if text is valid PlantUML.
 *
 * @author Walter Stroebel
 */
public class ModalityDOT extends Modality {

    public ModalityDOT(ExecutorService pool, String currentText) {
        super(pool, currentText);
    }

    @Override
    protected void convert() {

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
