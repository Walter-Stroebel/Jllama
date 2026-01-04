package nl.infcomtec.simpleimage;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;

/**
 * Simple image viewer with drag and scale.
 *
 * @author Walter Stroebel
 */
public class ImageViewer {

    public static final int MESSAGE_HEIGHT = 200;
    public static final int MESSAGE_WIDTH = 800;
    public List<Component> tools;
    public String message = null;
    public long messageMillis = 0;
    public long shownLast = 0;
    public Font messageFont = UIManager.getFont("Label.font");

    public final ImageObject imgObj;
    public LUT lut;
    public List<Marker> marks;

    public ImageViewer(ImageObject imgObj) {
        this.imgObj = imgObj;
    }

    public ImageViewer(Image image) {
        imgObj = new ImageObject(image);
    }

    public synchronized void flashMessage(Font font, String msg, long millis) {
        messageFont = font;
        messageMillis = millis;
        message = msg;
        shownLast = 0;
        imgObj.sendSignal(null);
    }

    public synchronized void flashMessage(String msg, long millis) {
        flashMessage(messageFont, msg, millis);
    }

    public synchronized void flashMessage(String msg) {
        flashMessage(messageFont, msg, 3000);
    }

    public synchronized void addMarker(Marker marker) {
        if (null == marks) {
            marks = new LinkedList<>();
        }
        marks.add(marker);
        imgObj.putImage(null);
    }

    public synchronized void clearMarkers() {
        marks = null;
        imgObj.putImage(null);
    }

    public ImageViewer(File f) {
        ImageObject tmp;
        try {
            tmp = new ImageObject(ImageIO.read(f));
        } catch (Exception ex) {
            tmp = showError(f);
        }
        imgObj = tmp;
    }

    /**
     * Although there are many reasons an image may not load, all we can do
     * about it is tell the user by creating an image with the failure.
     *
     * @param f File we tried to load.
     */
    private ImageObject showError(File f) {
        BufferedImage img = new BufferedImage(MESSAGE_WIDTH, MESSAGE_HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D gr = img.createGraphics();
        String msg1 = "Cannot open image:";
        String msg2 = f.getAbsolutePath();
        Font font = gr.getFont();
        float points = 24;
        int w, h;
        do {
            font = font.deriveFont(points);
            gr.setFont(font);
            FontMetrics metrics = gr.getFontMetrics(font);
            w = Math.max(metrics.stringWidth(msg1), metrics.stringWidth(msg2));
            h = metrics.getHeight() * 2; // For two lines of text
            if (w > MESSAGE_WIDTH - 50 || h > MESSAGE_HEIGHT / 3) {
                points--;
            }
        } while (w > MESSAGE_WIDTH || h > MESSAGE_HEIGHT / 3);
        gr.drawString(msg1, 50, MESSAGE_HEIGHT / 3);
        gr.drawString(msg2, 50, MESSAGE_HEIGHT / 3 * 2);
        gr.dispose();
        return new ImageObject(img);
    }

    /**
     * Often images have overly bright or dark areas. This permits to have a
     * lighter or darker view.
     *
     * @return For chaining.
     */
    public synchronized ImageViewer addShadowView() {
        ButtonGroup bg = new ButtonGroup();
        addChoice(bg, new AbstractAction("Dark") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.darker();
                imgObj.putImage(null);
            }
        });
        addChoice(bg, new AbstractAction("Normal") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.unity();
                imgObj.putImage(null);
            }
        }, true);
        addChoice(bg, new AbstractAction("Bright") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.brighter();
                imgObj.putImage(null);
            }
        });
        addChoice(bg, new AbstractAction("Brighter") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.sqrt(0);
                imgObj.putImage(null);
            }
        });
        addChoice(bg, new AbstractAction("Extreme") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.sqrt2();
                imgObj.putImage(null);
            }
        });
        return this;
    }

    /**
     * Add a choice,
     *
     * @param group If not null, only one choice in the group can be active.
     * @param action Choice.
     * @return For chaining.
     */
    public synchronized ImageViewer addChoice(ButtonGroup group, Action action) {
        return addChoice(group, action, false);
    }

    /**
     * Add a choice,
     *
     * @param group If not null, only one choice in the group can be active.
     * @param action Choice.
     * @param selected Only useful if true.
     * @return For chaining.
     */
    public synchronized ImageViewer addChoice(ButtonGroup group, Action action, boolean selected) {
        if (null == tools) {
            tools = new LinkedList<>();
        }
        JCheckBox button = new JCheckBox(action);
        button.setSelected(selected);
        if (null != group) {
            group.add(button);
            if (selected) {
                group.setSelected(button.getModel(), selected);
            }
        }
        tools.add(button);
        return this;
    }

    /**
     * Add any button.
     *
     * @param action The action for the button to perform when clicked.
     * @return For chaining.
     */
    public synchronized ImageViewer addButton(Action action) {
        if (null == tools) {
            tools = new LinkedList<>();
        }
        tools.add(new JButton(action));
        return this;
    }

    /**
     * Scale the image to the maximum size.
     *
     * @param dsp The text to display on the button.
     * @return For chaining.
     */
    public synchronized ImageViewer addMaxButton(String dsp) {
        if (null == tools) {
            tools = new LinkedList<>();
        }
        tools.add(new JButton(new AbstractAction(dsp) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                imgObj.sendSignal(ScaleCommand.SCALE_MAX);
            }
        }));
        return this;
    }

    /**
     * Scale the image to the original size.
     *
     * @param dsp The text to display on the button.
     * @return For chaining.
     */
    public synchronized ImageViewer addOrgButton(String dsp) {
        if (null == tools) {
            tools = new LinkedList<>();
        }
        tools.add(new JButton(new AbstractAction(dsp) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                imgObj.sendSignal(ScaleCommand.SCALE_ORG);
            }
        }));
        return this;
    }

    /**
     * Add any component to the bar.
     *
     * @param comp The component to add, for instance a progress bar.
     * @return For chaining.
     */
    public synchronized ImageViewer addAnything(Component comp) {
        if (null == tools) {
            tools = new LinkedList<>();
        }
        tools.add(comp);
        return this;
    }

    /**
     * Show the image in a JPanel component with simple pan(drag) and zoom(mouse
     * wheel).
     *
     * @return the JPanel.
     */
    public JPanel getScalePanPanel() {
        final JPanel ret = new ImagePanel(this);
        return ret;
    }

    /**
     * Show the image in a JPanel component with simple pan(drag) and zoom(mouse
     * wheel). Includes a JToolbar if tools are provided, see add functions.
     *
     * @return the JPanel.
     */
    public JPanel getScalePanPanelTools() {
        final JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(new ImagePanel(this), BorderLayout.CENTER);
        if (null != tools) {
            JToolBar tb = new JToolBar();
            for (Component c : tools) {
                tb.add(c);
            }
            outer.add(tb, BorderLayout.NORTH);
        }
        return outer;
    }

    /**
     * Show the image in a JFrame component with simple pan(drag) and zoom(mouse
     * wheel). Includes a JToolbar if tools are provided, see add functions.
     *
     * @return the JFrame.
     */
    public JFrame getScalePanFrame() {
        final JFrame ret = new JFrame();
        ret.setAlwaysOnTop(true);
        ret.getContentPane().setLayout(new BorderLayout());
        ret.getContentPane().add(new ImagePanel(this), BorderLayout.CENTER);
        if (null != tools) {
            JToolBar tb = new JToolBar();
            for (Component c : tools) {
                tb.add(c);
            }
            ret.getContentPane().add(tb, BorderLayout.NORTH);
        }
        ret.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ret.pack();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ret.setVisible(true);
            }
        });
        return ret;
    }

    /**
     * Get the image object.
     *
     * @return The current ImageObject.
     */
    public ImageObject getImageObject() {
        return imgObj;
    }

    /**
     * Scaling options.
     */
    public enum ScaleCommand {
        SCALE_ORG, SCALE_MAX
    }

}
