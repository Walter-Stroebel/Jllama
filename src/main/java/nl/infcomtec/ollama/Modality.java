package nl.infcomtec.ollama;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;

/**
 * Base class for modalities.
 *
 * The way this works is, when we instruct the model to output some text that
 * is, for instance, actually an image (like a SVG file, a GraphViz DOT file or
 * a PlantUML description), but also any other external task, this will
 * encapsulate that action.
 *
 * An (external) tool will do the work, like generating an image.
 *
 * The result can be fetched using getImage() or getText() once the task is
 * done.
 *
 * The task can be repeated by calling setCurrentText(), this is meant for
 * editing.
 *
 * This "should just work", in case it does not, the member "oops" might hold
 * the Exception why not.
 *
 * @author Walter Stroebel
 */
public abstract class Modality implements Runnable {

    protected File outputFile;
    protected File pngOutputFile;
    protected BufferedImage image;
    protected String currentText;
    protected String outputText;
    public final boolean isGraphical;
    public Exception oops;
    protected Semaphore done = new Semaphore(0);

    /**
     * Initialize and run the task.
     *
     * @param pool To run the conversion.
     * @param currentText
     */
    public Modality(ExecutorService pool, String currentText) {
        this(pool, currentText, false);
    }

    /**
     * Initialize and run the task.
     *
     * @param pool To run the conversion.
     * @param currentText
     * @param isGraph Whether or not the task creates an image.
     */
    public Modality(ExecutorService pool, String currentText, boolean isGraph) {
        this.currentText = currentText;
        isGraphical = isGraph;
        pool.submit(this);
    }

    @Override
    public void run() {
        try {
            outputFile = File.createTempFile("temp", ".txt");
            if (isGraphical) {
                pngOutputFile = new File(
                        outputFile.getParentFile(),
                        outputFile.getName().replace(".txt", ".png"));
            }
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(currentText);
            }
            convert();
            if (isGraphical) {
                image = ImageIO.read(pngOutputFile);
            }
        } catch (Exception any) {
            oops = any;
            outputText = any.getMessage();
        } finally {
            if (null != outputFile) {
                outputFile.delete();
            }
            outputFile = null;
            if (null != pngOutputFile) {
                pngOutputFile.delete();
            }
            pngOutputFile = null;
            done.release();
        }
    }

    /**
     * Get the image produced.
     *
     * This might block and might return null
     *
     * @return The image.
     */
    public BufferedImage getImage() {
        try {
            done.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            done.release();
        }
        return image;
    }

    /**
     * Get the text produced.
     *
     * This might block and might return null
     *
     * @return The text.
     */
    public String getText() {
        try {
            done.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            done.release();
        }
        return outputText;
    }

    /**
     * Implement the actual conversion or execution here.
     */
    protected abstract void convert();

    /**
     * Re-run with a new text.
     *
     * @param pool To run the conversion.
     * @param newText The new or altered text.
     */
    public void setCurrentText(ExecutorService pool, String newText) {
        try {
            done.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        currentText = newText;
        image = null;
        outputText = null;
        pool.submit(this);
    }
}
