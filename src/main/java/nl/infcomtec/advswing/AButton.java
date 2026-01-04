package nl.infcomtec.advswing;

import javax.swing.JButton;

/**
 *
 * @author walter
 */
public class AButton extends JButton {

    public AButton(EzAction a) {
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
