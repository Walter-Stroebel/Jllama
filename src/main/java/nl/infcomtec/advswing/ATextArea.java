package nl.infcomtec.advswing;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;

/**
 *
 * @author walter
 */
public class ATextArea extends JTextArea {

    public ATextArea(String text) {
        super(text);
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    public ATextArea() {
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    public ATextArea(int rows, int columns) {
        super(rows, columns);
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    public ATextArea(String text, int rows, int columns) {
        super(text, rows, columns);
        setLineWrap(true);
        setWrapStyleWord(true);
    }

    public ATextArea(File source) throws IOException {
        this(source.toPath());
    }

    public ATextArea(Path source) throws IOException {
        this(Files.readString(source));
    }

    public JButton saveButton(String label) {
        return new JButton(choose(label, true));
    }

    public JButton loadButton(String label) {
        return new JButton(choose(label, false));
    }

    private AbstractAction choose(String label, final boolean forSave) {
        return new AbstractAction(label) {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                int ans = forSave ? jfc.showSaveDialog(null) : jfc.showOpenDialog(null);
                if (ans == JFileChooser.APPROVE_OPTION) {
                    if (forSave) {
                        save(jfc.getSelectedFile());
                    } else {
                        load(jfc.getSelectedFile());
                    }
                }
            }
        };
    }

    public void save(File f) {
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath())) {
            bw.write(getText());
            if (!getText().endsWith(System.lineSeparator())) {
                bw.write(System.lineSeparator());
            }
        } catch (IOException ex) {
            Logger.getLogger(ATextArea.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void load(File f) {
        try {
            setText(Files.readString(f.toPath()));
            if (!getText().endsWith(System.lineSeparator())) {
                append(System.lineSeparator());
            }
            setCaretPosition(getText().length());
        } catch (IOException ex) {
            Logger.getLogger(ATextArea.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
