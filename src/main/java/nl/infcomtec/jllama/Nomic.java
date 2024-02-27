package nl.infcomtec.jllama;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import javax.imageio.ImageIO;

/**
 * Embeddings using "nomic-embed-text".
 *
 * @author walter
 */
public class Nomic {

    public static String model = "nomic-embed-text:latest";

    public static void main1(String[] args) throws Exception {
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

    public static void main(String[] args) throws Exception {
        File nd = new File(System.getProperty("user.home"), "nomic");
        Ollama.init();
        OllamaEmbeddings em = new OllamaEmbeddings(Ollama.config.lastEndpoint, model);
        cleanup(nd);
        File[] java = nd.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".java");
            }
        });
        StringBuilder sb = new StringBuilder();
        int n = 0;
        Embeddings last = null;
        for (File f : java) {
            Statistics stats = new Statistics();
            try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
                for (String s = bfr.readLine(); null != s; s = bfr.readLine()) {
                    s = s.trim();
                    while (sb.length() + s.length() > 5000) {
                        int nl = sb.indexOf("\n");
                        if (nl < 0) {
                            sb.setLength(0);
                            break;
                        }
                        sb.delete(0, nl + 1);
                    }
                    sb.append(s);
                    sb.append(System.lineSeparator());
                    Embeddings embeddings = em.getEmbeddings(sb.toString());
                    if (null != last) {
                        stats.rolling(last.cosineSimilarity(embeddings));
                    }
                    last = embeddings;
                }
                System.out.println(stats);
            }
        }
    }

    private static void cleanup(File nd) {
        File[] rmPrn = nd.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".png");
            }
        });
        for (File f : rmPrn) {
            f.delete();
        }
        new File(nd, "java.mp4").delete();
    }
}
