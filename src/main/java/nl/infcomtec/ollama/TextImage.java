/*
 */
package nl.infcomtec.ollama;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import javax.swing.AbstractAction;
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
    public final ImageObject imgObj;
    private final Modality worker;

    public TextImage(final ExecutorService pool, String title, Modality grpMod) throws Exception {
        worker = grpMod;
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        imgObj = new ImageObject(worker.getImage());
        Container cp = frame.getContentPane();
        cp.setLayout(new BorderLayout());
        final JTextArea text = new JTextArea(60, 5);
        AbstractAction updateAction = new AbstractAction("Update") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                worker.setCurrentText(pool, text.getText());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        imgObj.putImage(worker.getImage());
                    }
                }).start();
            }
        };
        cp.add(new ImageViewer(imgObj).addButton(updateAction).getScalePanPanelTools(), BorderLayout.CENTER);
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
