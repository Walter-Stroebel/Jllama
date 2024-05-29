package nl.infcomtec.advswing;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

/**
 * Just a JFrame with some utility methods.
 *
 * @author walter
 */
public class AFrame extends JFrame {

    public AFrame(String title) throws HeadlessException {
        super(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    public void showFrame() {
        if (getWidth() < 200 || getHeight() < 100) {
            // avoid zero-sized window
            setSize(200, 100);
        }
        if (EventQueue.isDispatchThread()) {
            super.setVisible(true);
        } else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    AFrame.super.setVisible(true);
                }
            });
        }
    }

    public void withToolBar(JComponent... cmp) {
        JToolBar tb = new JToolBar();
        if (null != cmp) {
            for (JComponent jc : cmp) {
                tb.add(jc);
            }
        }
        getContentPane().add(tb, BorderLayout.NORTH);
    }

    public static void main(String[] args) {
        notePad();
    }

    /**
     * Very basic NotePad for instance to hack a configuration file or so.
     *
     * <b>Be aware</b>: Kids, do not try this at home.
     *
     * @return The JTextArea inside a small floating JFrame.
     */
    public static JTextArea notePad() {
        final ATextArea text = new ATextArea(64, 16);
        final AFrame notePad = new AFrame("Notepad");
        notePad.withToolBar(new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                notePad.dispose();
            }
        }), new JButton(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                int ans = jfc.showSaveDialog(notePad);
                if (ans == JFileChooser.APPROVE_OPTION) {
                    try (FileWriter fw = new FileWriter(jfc.getSelectedFile())) {
                        fw.write(text.getText());
                        if (!text.getText().endsWith(System.lineSeparator())) {
                            fw.write(System.lineSeparator());
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(AFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }), new JButton(new AbstractAction("Load") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                int ans = jfc.showOpenDialog(notePad);
                if (ans == JFileChooser.APPROVE_OPTION) {
                    try {
                        text.setText(Files.readString(jfc.getSelectedFile().toPath()));
                        if (!text.getText().endsWith(System.lineSeparator())) {
                            text.append(System.lineSeparator());
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(AFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }));
        notePad.getContentPane().add(new JScrollPane(text), BorderLayout.CENTER);
        notePad.showFrame();
        return text;
    }
}
