package nl.infcomtec.advswing;

import java.awt.Font;
import javax.swing.Icon;
import javax.swing.JLabel;

/**
 *
 * @author walter
 */
public class ALabel extends JLabel {

    public ALabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }

    public ALabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    public ALabel(String text) {
        super(text);
    }

    public ALabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
    }

    public ALabel(Icon image) {
        super(image);
    }

    public ALabel() {
    }

    public ALabel withFont(Font font) {
        setFont(font);
        return this;
    }

}
