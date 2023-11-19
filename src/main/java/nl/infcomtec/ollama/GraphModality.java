package nl.infcomtec.ollama;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

/**
 * Base class for graphics modalities.
 *
 * The way this works is we instruct the model to output some text that is
 * actually an image. Like a SVG file, a GraphViz DOT file or a PlantUML
 * description.
 *
 * We then display said text in a small JTextArea so the user can tweak it and
 * press a button to view the result.
 *
 * The button will call an external tool to generate an image, load the image
 * and show it in a preview pane.
 *
 * Implementations of this base class provide the SwingWorker instances for the
 * conversions above.
 *
 * @author Walter Stroebel
 */
public abstract class GraphModality extends SwingWorker<BufferedImage, String> {

    protected File outputFile;
    protected File pngOutputFile;
    protected BufferedImage ret;
    protected final String currentText;

    public GraphModality(String currentText) {
        this.currentText = currentText;
    }

    @Override
    protected BufferedImage doInBackground() throws Exception {
        try {
            outputFile = File.createTempFile("temp", ".txt");
            pngOutputFile = new File(
                    outputFile.getParentFile(),
                    outputFile.getName().replace(".txt", ".png"));
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(currentText);
            }
            convert();
            ret = ImageIO.read(pngOutputFile);
        } finally {
            outputFile.delete();
            pngOutputFile.delete();
        }
        return ret;
    }

    /**
     * Implement the actual conversion here.
     */
    abstract void convert();

}
