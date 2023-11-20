package nl.infcomtec.ollama;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

/**
 * Base class for modalities.
 *
 * The way this works is we instruct the model to output some text that is for
 * instance actually an image. Like a SVG file, a GraphViz DOT file or a
 * PlantUML description.
 *
 * We then display said text in a small JTextArea so the user can tweak it and
 * press a button to view the result.
 *
 * The button will call an external tool to do the work like generating an
 * image, load the result and show it in a preview pane.
 *
 * Implementations of this base class provide the SwingWorker instances for the
 * conversions above.
 *
 * @author Walter Stroebel
 */
public abstract class Modality {

    protected File outputFile;
    protected File pngOutputFile;
    protected BufferedImage image;
    protected String currentText;
    public final boolean isGraphical;

    public Modality(String currentText) {
        this.currentText = currentText;
        isGraphical = true;
    }

    protected BufferedImage work() throws Exception {
        try {
            outputFile = File.createTempFile("temp", ".txt");
            pngOutputFile = new File(
                    outputFile.getParentFile(),
                    outputFile.getName().replace(".txt", ".png"));
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(currentText);
            }
            convert();
            image = ImageIO.read(pngOutputFile);
        } finally {
            outputFile.delete();
            pngOutputFile.delete();
        }
        return image;
    }

    public BufferedImage getImage() {
        while (null == image) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return image;
    }

    /**
     * Implement the actual conversion here.
     */
    protected abstract void convert();

    public abstract SwingWorker<BufferedImage, String> getWorker();

}
