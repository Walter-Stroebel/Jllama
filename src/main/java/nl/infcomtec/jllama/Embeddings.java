package nl.infcomtec.jllama;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Embeddings.
 *
 * @author walter
 */
public class Embeddings {

    public Request request;
    public Response response;

    public static class Request {

        public String model;
        public String prompt;

        @Override
        public String toString() {
            return "Request{" + "model=" + model + ", prompt=" + prompt + '}';
        }
    }

    public static class Response {

        public double[] embedding;

        @Override
        public String toString() {
            return "Response{embedding=double[" + embedding.length + "]}";
        }
    }

    @Override
    public String toString() {
        return "Embeddings{" + "request=" + request + ", response=" + response + '}';
    }

    public BufferedImage toImage(boolean log) {
        int w = (int) Math.round(Math.sqrt(response.embedding.length));
        int h = response.embedding.length / w;
        if (w * h < response.embedding.length) {
            h++;
        }
        return toImage(w, h, 1, 1, log);
    }

    /**
     * Calculates the cosine similarity between the current object's embeddings
     * and an other's embeddings. Cosine similarity measures the cosine of the
     * angle between two non-zero vectors in a multidimensional space, providing
     * a metric of orientation similarity, regardless of magnitude. It ranges
     * from -1 (exactly opposite) to 1 (exactly the same), with 0 indicating
     * orthogonality. This method is useful in comparing the similarity of two
     * embeddings, often used in text analysis and recommendation systems.
     *
     * @param other The Embeddings object to compare with the current object.
     * @return The cosine similarity between the two embeddings.
     * @throws RuntimeException If the models of the two embeddings do not
     * match, if the vectors are null or of different lengths, or if the norm of
     * one or both vectors is 0, making it impossible to calculate cosine
     * similarity.
     */
    public double cosineSimilarity(Embeddings other) {
        if (!request.model.equals(other.request.model)) {
            throw new RuntimeException("Not the same model");
        }
        double[] vectorA = response.embedding;
        double[] vectorB = other.response.embedding;
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            throw new RuntimeException("Vectors must be non-null and of equal length");
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);

        if (denominator == 0) {
            throw new RuntimeException("The norm of one or both vectors is 0.");
        }

        return dotProduct / denominator;
    }

    /**
     * Calculates the Euclidean distance between the current object's embeddings
     * and an other's embeddings. The Euclidean distance, or L2 distance, is the
     * straight-line distance between two points in a multidimensional space,
     * computed as the square root of the sum of the squared differences between
     * corresponding elements of the two vectors. It measures the actual
     * distance between points, regardless of the path taken, and is widely used
     * in clustering and classification processes.
     *
     * @param other The Embeddings object to compare with the current object.
     * @return The Euclidean distance between the two embeddings.
     * @throws RuntimeException If the models of the two embeddings do not
     * match, or if the vectors are null or of different lengths.
     */
    public double euclidianDistance(Embeddings other) {
        if (!request.model.equals(other.request.model)) {
            throw new RuntimeException("Not the same model");
        }
        double[] vectorA = response.embedding;
        double[] vectorB = other.response.embedding;
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            throw new RuntimeException("Vectors must be non-null and of equal length");
        }
        double sumSquaredDifferences = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            sumSquaredDifferences += Math.pow(vectorA[i] - vectorB[i], 2);
        }
        return Math.sqrt(sumSquaredDifferences);
    }

    public BufferedImage toImage(int w, int h, int xf, int yf, boolean log) {
        BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double d : response.embedding) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        if (log) {
            min = Math.log(-min);
            max = Math.log(max);
        }
        System.out.format("Count=%d, MiniMax=%.2f - %.2f\n", response.embedding.length, min, max);
        double rf = 255.0 / min;
        double bf = 255.0 / max;
        int x = 0;
        int y = 0;
        Graphics2D gr = ret.createGraphics();
        for (double d : response.embedding) {
            double r, b;
            if (log) {
                if (d < 0) {
                    r = Math.log(-d) * rf;
                    b = 0;
                } else {
                    r = 0;
                    b = Math.log(d) * bf;
                }
            } else {
                if (d < 0) {
                    r = -d * rf;
                    b = 0;
                } else {
                    r = 0;
                    b = d * bf;
                }
            }
            r = Math.max(0, Math.min(255, r));
            b = Math.max(0, Math.min(255, b));
            Color c = new Color((int) r, 64 + (int) ((r + b) / 4), (int) b, 255);
            gr.setColor(c);
            gr.fillRect(x, y, xf, yf);
            x += xf;
            if (x >= w) {
                x = 0;
                y += yf;
            }
        }
        gr.dispose();
        return ret;
    }

    public BufferedImage toImageRGB(int w, int h, int xf, int yf, boolean log) {
        BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double d : response.embedding) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        if (log) {
            min = Math.log(-min);
            max = Math.log(max);
        }
        System.out.format("Count=%d, MiniMax=%.2f - %.2f\n", response.embedding.length, min, max);
        double rf = 127.0 / min;
        double bf = 127.0 / max;
        int x = 0;
        int y = 0;
        Graphics2D gr = ret.createGraphics();
        double[] rgb = new double[3];
        int i = 0;
        for (double d : response.embedding) {
            if (log) {
                if (d < 0) {
                    rgb[i] = 127.5 - Math.log(-d) * rf;
                } else {
                    rgb[i] = 127.5 + Math.log(d) * bf;
                }
            } else {
                if (d < 0) {
                    rgb[i] = 127.5 + d * rf;
                } else {
                    rgb[i] = 127.5 + d * bf;
                }
            }
            i++;
            if (3 == i) {
                int r = Math.max(0, (int) Math.min(255, rgb[0]));
                int g = Math.max(0, (int) Math.min(255, rgb[1]));
                int b = Math.max(0, (int) Math.min(255, rgb[2]));
                Color c = new Color(r, g, b);
                gr.setColor(c);
                gr.fillRect(x, y, xf, yf);
                x += xf;
                if (x >= w) {
                    x = 0;
                    y += yf;
                }
                i = 0;
            }
        }
        gr.dispose();
        return ret;
    }

    /**
     * Calculates the Manhattan distance between the current object's embeddings
     * and an other's embeddings. The Manhattan distance, also known as the L1
     * distance, between two points in a multidimensional space is the sum of
     * the absolute differences of their coordinates. It represents the total
     * distance traveling only along axes (horizontal or vertical) between the
     * points, akin to navigating a grid-based city like Manhattan.
     *
     * @param other The Embeddings object to compare with the current object.
     * @return The Manhattan distance between the two embeddings.
     * @throws RuntimeException If the models of the two embeddings do not match
     * or if the vectors are null or of different lengths.
     */
    public double manhattanDistance(Embeddings other) {
        if (!request.model.equals(other.request.model)) {
            throw new RuntimeException("Not the same model");
        }
        double[] vectorA = response.embedding;
        double[] vectorB = other.response.embedding;
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            throw new RuntimeException("Vectors must be non-null and of equal length");
        }
        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            sum += Math.abs(vectorA[i] - vectorB[i]);
        }
        return sum;
    }

    /**
     * Calculates the Pearson correlation coefficient between two sets of
     * embeddings. This method assesses the linear relationship between the two
     * embeddings based on their deviations from their means.
     *
     * The coefficient is computed as the sum of the product of differences from
     * the mean for each embedding, normalized by the square roots of the sum of
     * squared differences (variance-like calculation) for each embedding.
     *
     * The result is a value between -1 and 1, where:<ul><li>1 indicates a
     * perfect positive linear relationship</li><li>-1 indicates a perfect
     * negative linear relationship</li><li>0 indicates no linear
     * relationship.</li></ul>
     *
     * @param other The second set of embeddings.
     * @return The Pearson correlation coefficient, indicating the degree of
     * linear relationship between the two embeddings.
     */
    public double pearsonCorrelation(Embeddings other) {
        if (!request.model.equals(other.request.model)) {
            throw new RuntimeException("Not the same model");
        }
        double[] vectorA = response.embedding;
        double[] vectorB = other.response.embedding;
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            throw new RuntimeException("Vectors must be non-null and of equal length");
        }
        double meanA = 0.0;
        double meanB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            meanA += vectorA[i];
            meanB += vectorB[i];
        }
        meanA /= vectorA.length;
        meanB /= vectorB.length;

        double sumProduct = 0.0;
        double sumSquareA = 0.0;
        double sumSquareB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            double diffA = vectorA[i] - meanA;
            double diffB = vectorB[i] - meanB;
            sumProduct += diffA * diffB;
            sumSquareA += diffA * diffA;
            sumSquareB += diffB * diffB;
        }
        if (sumSquareA == 0 || sumSquareB == 0) {
            throw new RuntimeException("Division by zero in Pearson calculation");
        }
        return sumProduct / (Math.sqrt(sumSquareA) * Math.sqrt(sumSquareB));
    }

}
