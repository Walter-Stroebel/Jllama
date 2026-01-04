package nl.infcomtec.simpleimage;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Thread safe wrapper around BufferedImage.
 *
 * @author Walter Stroebel
 */
public class ImageObject extends Image {

    private BufferedImage image;
    private final Semaphore lock = new Semaphore(0);
    private final List<ImageObjectListener> listeners = new LinkedList<>();
    public boolean debug = false;

    public ImageObject(Image image) {
        putImage(image);
    }

    /**
     * @return The most recent image.
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Stay informed.
     *
     * @param listener called when another client changes the image.
     */
    public synchronized void addListener(ImageObjectListener listener) {
        listeners.add(listener);
    }

    /**
     * Inform all listeners about an event(message).
     *
     * @param msg The event / message.
     */
    public void sendSignal(Object msg) {
        for (ImageObjectListener listener : listeners) {
            listener.signal(msg);
        }
    }

    /**
     * Mouse Events
     */
    public enum MouseEvents {
        clicked_left, clicked_right, dragged, moved, pressed_left, released_left, pressed_right, released_right
    };

    /**
     * Inform all listeners about a mouse event.
     *
     * @param ev The mouse event.
     * @param p Point the event happened.
     */
    public synchronized void forwardMouse(MouseEvents ev, Point2D p) {
        for (ImageObjectListener listener : listeners) {
            listener.mouseEvent(this, ev, p);
        }
    }

    /**
     * Replace image.
     *
     * @param replImage If null image may still have been altered in place, else
     * replace image with replImage. All listeners will be notified.
     */
    public synchronized final void putImage(Image replImage) {
        int oldWid = (null == this.image) ? 0 : this.image.getWidth();
        if (null != replImage) {
            this.image = new BufferedImage(replImage.getWidth(null), replImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = this.image.createGraphics();
            g2.drawImage(replImage, 0, 0, null);
            g2.dispose();
        }
        for (ImageObjectListener listener : listeners) {
            if (0 == oldWid) {
                listener.imageChanged(this, 1.0);
            } else {
                listener.imageChanged(this, 1.0 * oldWid / this.image.getWidth());
            }
        }
    }

    /**
     * Get the width of the current image.
     *
     * @return The width.
     */
    public int getWidth() {
        return getWidth(null);
    }

    @Override
    public int getWidth(ImageObserver io) {
        return image.getWidth(io);
    }

    @Override
    public int getHeight(ImageObserver io) {
        return image.getHeight(io);
    }

    /**
     * Get the height of the current image.
     *
     * @return The width.
     */
    public int getHeight() {
        return getHeight(null);
    }

    @Override
    public ImageProducer getSource() {
        return image.getSource();
    }

    @Override
    public Graphics getGraphics() {
        return image.getGraphics();
    }

    @Override
    public Object getProperty(String string, ImageObserver io) {
        return image.getProperty(string, null);
    }

    /**
     * This creates a map of areas around a point of interest.
     *
     * The idea is that if something happens for a given point on the image
     * (like a mouse click), this map can associate an identifier (a simple
     * string) to that event.
     *
     * @param pois Points of interest.
     * @return The map.
     */
    public HashMap<String, BitShape> calculateClosestAreas(final Map<String, Point2D> pois) {
        long nanos = System.nanoTime();
        final HashMap<String, BitShape> ret = new HashMap<>();
        for (Map.Entry<String, Point2D> e : pois.entrySet()) {
            ret.put(e.getKey(), new BitShape(getWidth()));
        }

        int numThreads = Runtime.getRuntime().availableProcessors() - 2; // Number of threads to use, leaving some cores for routine work.
        if (numThreads < 1) {
            numThreads = 1;
        }
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        int rowsPerThread = getHeight() / numThreads;
        for (int i = 0; i < numThreads; i++) {
            final int yStart = i * rowsPerThread;
            final int yEnd = (i == numThreads - 1) ? getHeight() : yStart + rowsPerThread;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (int y = yStart; y < yEnd; y++) {
                        for (int x = 0; x < getWidth(); x++) {
                            double d = 0;
                            String poi = null;
                            for (Map.Entry<String, Point2D> e : pois.entrySet()) {
                                double d2 = e.getValue().distance(x, y);
                                if (poi == null || d2 < d) {
                                    poi = e.getKey();
                                    d = d2;
                                }
                            }
                            synchronized (ret.get(poi)) {
                                ret.get(poi).set(new Point2D.Double(x, y));
                            }
                        }
                    }
                }
            });
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("This cannot be right.");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("We are asked to stop?", ex);
        }
        if (debug) {
            System.out.format("calculateClosestAreas W=%d,H=%d,P=%d,T=%.2f ms\n",
                    getWidth(), getHeight(), pois.size(), (System.nanoTime() - nanos) / 1e6);
        }
        return ret;
    }

    /**
     * Callback listener.
     */
    public static class ImageObjectListener {

        public final String name;

        public ImageObjectListener(String name) {
            this.name = name;
        }

        /**
         * Image may have been altered.
         *
         * @param imgObj Source.
         * @param resizeHint The width of the previous image divided by the
         * width of the new image. See ImageViewer why this is useful.
         */
        public void imageChanged(ImageObject imgObj, double resizeHint) {
            // default is no action
        }

        /**
         * Used to forward mouse things by viewer implementations.
         *
         * @param imgObj Source.
         * @param ev Our event definition.
         * @param p Point of the event in image pixels.
         */
        public void mouseEvent(ImageObject imgObj, MouseEvents ev, Point2D p) {
            // default ignore
        }

        /**
         * Generic signal, generally causes viewer implementations to repaint.
         *
         * @param any
         */
        public void signal(Object any) {
            // default ignore
        }
    }
}
