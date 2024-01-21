package nl.infcomtec.jllama;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import nl.infcomtec.simpleimage.ImageViewer;

/**
 * Embeddings tests
 *
 * @author walter
 */
public class Embed {

    public static void main(String[] args) throws Exception {
        Ollama.init();
        OllamaEmbeddings em = new OllamaEmbeddings(Ollama.config.lastEndpoint, Ollama.config.lastModel);
        {
            Embeddings embeddings = em.getEmbeddings("Why is the sky blue?");
            System.out.println(embeddings);
            BufferedImage toImage = toImage(embeddings);
            ImageIO.write(toImage, "png", new File("/tmp/blue_sky.png"));
            Image im = toImage.getScaledInstance(320, 320, BufferedImage.SCALE_DEFAULT);
            ImageViewer iv = new ImageViewer(im);
            iv.getScalePanFrame().setTitle("Blue sky");
        }
        {
            Embeddings embeddings = em.getEmbeddings("What are the planets in our solar system?");
            System.out.println(embeddings);
            BufferedImage toImage = toImage(embeddings);
            ImageIO.write(toImage, "png", new File("/tmp/planets.png"));
            Image im = toImage.getScaledInstance(320, 320, BufferedImage.SCALE_DEFAULT);
            ImageViewer iv = new ImageViewer(im);
            JFrame f2 = iv.getScalePanFrame();
            f2.setTitle("Planets");
            f2.setLocation(f2.getLocation().x + 400, f2.getLocation().y);
        }
    }

    public static BufferedImage toImage(Embeddings em) {
        int w = (int) Math.round(Math.sqrt(em.response.embedding.length));
        int h = em.response.embedding.length / w;
        BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double d : em.response.embedding) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        min = Math.log(-min);
        max = Math.log(max);
        System.out.format("WxH=%dx%d, MiniMax=%.2f - %.2f\n", w, h, min, max);
        double rf = 255.0 / min;
        double bf = 255 / max;
        int x = 0;
        int y = 0;
        for (double d : em.response.embedding) {
            double r, b;
            if (d < 0) {
                r = Math.log(-d) * rf;
                b = 0;
            } else {
                r = 0;
                b = Math.log(d) * bf;
            }
            r = Math.max(0, Math.min(255, r));
            b = Math.max(0, Math.min(255, b));
            Color c = new Color((int) r, 128, (int) b, 255);
            ret.setRGB(x, y, c.getRGB());
            x++;
            if (x >= w) {
                x = 0;
                y++;
            }
        }
        return ret;
    }

}
