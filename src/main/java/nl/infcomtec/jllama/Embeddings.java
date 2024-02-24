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

}
