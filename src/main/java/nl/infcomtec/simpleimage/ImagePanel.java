/*
 */
package nl.infcomtec.simpleimage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author walter
 */
class ImagePanel extends JPanel {

    int ofsX = 0;
    int ofsY = 0;
    double scale = 1;
    private BufferedImage dispImage = null;
    private final ImageViewer imgView;

    public ImagePanel(final ImageViewer imgView) {
        this.imgView = imgView;
        MouseAdapter ma = new MouseAdapter() {
            private int lastX;
            private int lastY;

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    imgView.imgObj.forwardMouse(ImageObject.MouseEvents.pressed_right, pixelMouse(e));
                } else {
                    lastX = e.getX();
                    lastY = e.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    imgView.imgObj.forwardMouse(ImageObject.MouseEvents.released_right, pixelMouse(e));
                } else {
                    imgView.imgObj.forwardMouse(ImageObject.MouseEvents.released_left, pixelMouse(e));
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    imgView.imgObj.forwardMouse(ImageObject.MouseEvents.clicked_right, pixelMouse(e));
                } else {
                    imgView.imgObj.forwardMouse(ImageObject.MouseEvents.clicked_left, pixelMouse(e));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    imgView.imgObj.forwardMouse(ImageObject.MouseEvents.dragged, pixelMouse(e));
                } else {
                    ofsX += e.getX() - lastX;
                    ofsY += e.getY() - lastY;
                    lastX = e.getX();
                    lastY = e.getY();
                    repaint(); // Repaint the panel to reflect the new position
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                scale += notches * 0.1; // Adjust the scaling factor
                if (scale < 0.1) {
                    scale = 0.1; // Prevent the scale from becoming too small
                }
                dispImage = null;
                repaint(); // Repaint the panel to reflect the new scale
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
        imgView.imgObj.addListener(new ImageObject.ImageObjectListener("Repaint") {
            @Override
            public void imageChanged(ImageObject imgObj, double resizeHint) {
                // If the image we are displaying is for instance 512 pixels and
                // the new image is 1536, we need to adjust scaling to match.
                scale *= resizeHint;
                dispImage = null;
                repaint(); // Repaint the panel to reflect any changes
            }

            @Override
            public void signal(Object any) {
                if (any instanceof ImageViewer.ScaleCommand) {
                    ImageViewer.ScaleCommand sc = (ImageViewer.ScaleCommand) any;
                    switch (sc) {
                        case SCALE_MAX:
                            {
                                double w = (1.0 * getWidth()) / imgView.imgObj.getWidth();
                                double h = (1.0 * getHeight()) / imgView.imgObj.getHeight();
                                System.out.format("%f %f %d %d\n", w, h, imgView.imgObj.getWidth(), getWidth());
                                if (w > h) {
                                    scale = h;
                                } else {
                                    scale = w;
                                }
                            }
                            break;
                        case SCALE_ORG:
                            {
                                scale = 1;
                            }
                            break;
                    }
                    dispImage = null;
                }
                repaint();
            }
        });
        Dimension dim = new Dimension(imgView.imgObj.getWidth(), imgView.imgObj.getHeight());
        setPreferredSize(dim);
    }

    private Point pixelMouse(MouseEvent e) {
        int ax = e.getX() - ofsX;
        int ay = e.getY() - ofsY;
        ax = (int) Math.round(ax / scale);
        ay = (int) Math.round(ay / scale);
        return new Point(ax, ay);
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (null == dispImage) {
            int scaledWidth = (int) (imgView.imgObj.getWidth() * scale);
            int scaledHeight = (int) (imgView.imgObj.getHeight() * scale);
            dispImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = dispImage.createGraphics();
            g2.drawImage(imgView.imgObj.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_SMOOTH), 0, 0, null);
            g2.dispose();
            if (null != imgView.lut) {
                dispImage = imgView.lut.apply(dispImage);
            }
            if (null != imgView.marks) {
                for (Marker marker : imgView.marks) {
                    marker.mark(dispImage);
                }
            }
        }
        g.drawImage(dispImage, ofsX, ofsY, null);
        if (null != imgView.message) {
            if (0 == imgView.shownLast) {
                imgView.shownLast = System.currentTimeMillis();
            }
            if (imgView.shownLast + imgView.messageMillis >= System.currentTimeMillis()) {
                g.setFont(imgView.messageFont);
                g.setColor(Color.WHITE);
                g.setXORMode(Color.BLACK);
                int ofs = g.getFontMetrics().getHeight();
                System.out.println(imgView.message + " " + ofs);
                g.drawString(imgView.message, ofs, ofs * 2);
            } else {
                imgView.message = null;
                imgView.shownLast = 0;
            }
        }
    }

}
