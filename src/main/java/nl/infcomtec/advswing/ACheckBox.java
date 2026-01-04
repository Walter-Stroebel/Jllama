package nl.infcomtec.advswing;

import javax.swing.JCheckBox;

/**
 *
 * @author walter
 */
public class ACheckBox extends JCheckBox {

    public ACheckBox(EzAction a) {
        super(a);
        if (null != a.font) {
            setFont(a.font);
        }
        if (null != a.background) {
            setBackground(a.background);
        }
        if (null != a.foreground) {
            setForeground(a.foreground);
        }
    }

}
