package nl.infcomtec.advswing;

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

}
