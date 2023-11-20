/*
 */
package nl.infcomtec.ollama;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import nl.infcomtec.simpleimage.ImageObject;
import nl.infcomtec.simpleimage.ImageViewer;

/**
 *
 * @author walter
 */
public class TextImage {

    public final JFrame frame;
    public final ImageObject image;
    private final Modality worker;

    public TextImage(String title, Modality grpMod) throws Exception {
        worker = grpMod;
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        image = new ImageObject(worker.getImage());
        Container cp = frame.getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(new ImageViewer(image).getScalePanPanel(), BorderLayout.CENTER);
        JTextArea text = new JTextArea(60, 5);
        text.setText(worker.currentText);
        cp.add(new JScrollPane(text), BorderLayout.SOUTH);
        frame.pack();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }
}
