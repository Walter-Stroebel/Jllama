package nl.infcomtec.simpleimage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Simple image marker: add labeled points on an image.
 *
 * @author Walter Stroebel
 */
public class ImageMarker extends ImageViewer {

    private final List<LabelledPoint> markers = new LinkedList<>();

    public ImageMarker(Image image) {
        super(image);
    }

    public ImageMarker(File f) {
        super(f);
    }

    /**
     * Show the image in a JPanel component with simple pan(drag) and zoom(mouse
     * wheel).
     *
     * @return
     */
    public JPanel getMarkerPanel() {
        final JPanel ret = new JPanelImpl();
        return ret;
    }

    /**
     * Show the image in a JFrame component with simple pan(drag) and zoom(mouse
     * wheel).
     *
     * @return
     */
    public JFrame getMarkerFrame() {
        final JFrame ret = new JFrame();
        ret.setAlwaysOnTop(true);
        ret.getContentPane().setLayout(new BorderLayout());
        ret.getContentPane().add(new JPanelImpl(), BorderLayout.CENTER);
        ret.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ret.setSize(Math.min(800, imgObj.getWidth()), Math.min(600, imgObj.getHeight()));
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ret.setVisible(true);
            }
        });
        return ret;
    }

    private class JPanelImpl extends JPanel {

        int ofsX = 0;
        int ofsY = 0;
        double scale = 1;
        LabelledPoint lp = null;
        AffineTransform transform;
        AffineTransform inverseTransform;

        public JPanelImpl() {
            updateTrans();
            MouseAdapter ma = new MouseAdapter() {
                private int lastX, lastY;

                @Override
                public void mousePressed(MouseEvent e) {
                    Point2D mouse = inverseTransform.transform(e.getPoint(), null);
                    lp = null;
                    for (LabelledPoint mark : markers) {
                        if (mark.selects(mouse)) {
                            lp = mark;
                            break;
                        }
                    }
                    lastX = e.getX();
                    lastY = e.getY();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        imgObj.forwardMouse(ImageObject.MouseEvents.clicked_right, inverseTransform.transform(e.getPoint(), null));
                    } else {
                        imgObj.forwardMouse(ImageObject.MouseEvents.clicked_left, inverseTransform.transform(e.getPoint(), null));
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (null == lp) {
                        ofsX += e.getX() - lastX;
                        ofsY += e.getY() - lastY;
                    } else {
                        lp.x += e.getX() - lastX;
                        lp.y += e.getY() - lastY;
                    }
                    lastX = e.getX();
                    lastY = e.getY();
                    updateTrans();
                    repaint(); // Repaint the panel to reflect the new position
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    int notches = e.getWheelRotation();
                    scale += notches * 0.1; // Adjust the scaling factor
                    if (scale < 0.1) {
                        scale = 0.1; // Prevent the scale from becoming too small
                    }
                    updateTrans();
                    repaint(); // Repaint the panel to reflect the new scale
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
            imgObj.addListener(new ImageObject.ImageObjectListener("Repaint") {
                @Override
                public void imageChanged(ImageObject imgObj, double resizeHint) {
                    scale *= resizeHint;
                    repaint(); // Repaint the panel to reflect any changes
                }
            });
        }

        private void updateTrans() throws RuntimeException {
            // Create an AffineTransform to apply scaling and translation
            transform = AffineTransform.getTranslateInstance(ofsX, ofsY);
            transform.scale(scale, scale);
            // Get the inverse transform
            try {
                inverseTransform = transform.createInverse();
            } catch (NoninvertibleTransformException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), getHeight());
            int scaledWidth = (int) (imgObj.getWidth() * scale);
            int scaledHeight = (int) (imgObj.getHeight() * scale);
            g2.drawImage(imgObj.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_FAST), ofsX, ofsY, null);
            g2.setColor(Color.BLACK);
            g2.setXORMode(Color.WHITE);
            for (LabelledPoint mark : markers) {
                g2.draw(transform.createTransformedShape(mark.getShape()));
            }
        }
    }
}
