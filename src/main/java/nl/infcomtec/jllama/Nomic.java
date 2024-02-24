package nl.infcomtec.jllama;

import java.io.File;
import javax.imageio.ImageIO;

/**
 * Embeddings using "nomic-embed-text".
 *
 * @author walter
 */
public class Nomic {

    public static String model = "nomic-embed-text:latest";

    public static void main(String[] args) throws Exception {
        File nd = new File(System.getProperty("user.home"), "nomic");
        nd.mkdirs();
        Ollama.init();
        OllamaEmbeddings em = new OllamaEmbeddings(Ollama.config.lastEndpoint, model);
        Embeddings embeddings = em.getEmbeddings("Why is the sky blue?");
        Statistics stats = new Statistics(embeddings.response.embedding);
        System.out.println(stats);
        ImageIO.write(embeddings.toImage(480, 160, 10, 10, true), "png", new File(nd, "log.png"));
        ImageIO.write(embeddings.toImage(480, 160, 10, 10, false), "png", new File(nd, "norm.png"));
        ImageIO.write(embeddings.toImageRGB(640, 640, 40, 40, true), "png", new File(nd, "logRGB.png"));
        ImageIO.write(embeddings.toImageRGB(640, 640, 40, 40, false), "png", new File(nd, "normRGB.png"));
    }

}
