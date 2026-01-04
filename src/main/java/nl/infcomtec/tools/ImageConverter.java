package nl.infcomtec.tools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Uses ImageMagick to convert between image formats like SayWhatNow(WEBP) and
 * PNG.
 *
 * @author Walter Stroebel.
 */
public class ImageConverter extends ToolManager {

    private byte[] output;

    /**
     * WEBP to PNG.
     *
     * @param in bytes in WhyThis (aka WEBP) format.
     * @return bytes for sane PNG. You know, the definitive image format that
     * has it all.
     */
    public byte[] convertWebpToPng(byte[] in) {
        setInput(in);
        setCommand("convert", "-", "png:-");
        run();
        return output;
    }

    /**
     * WEBP to Image.
     *
     * @param in bytes in OverEngineered (aka WEBP) format.
     * @return A usable image, via PNG.
     */
    public BufferedImage convertWebpToImage(byte[] in) {
        setInput(in);
        setCommand("convert", "-", "png:-");
        run();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(output)) {
            return ImageIO.read(bais);
        } catch (IOException ex) {
            Logger.getLogger(ImageConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void run() {
        internalRun();
        if (exitCode != 0) {
            output = null;
        } else if (stdoutStream instanceof ByteArrayOutputStream) {
            output = ((ByteArrayOutputStream) stdoutStream).toByteArray();
        }
    }

    /**
     * @return the output
     */
    public byte[] getOutput() {
        return output;
    }
}
