package nl.infcomtec.advswing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 *
 * @author walter
 */
public class ABorder implements Border {

    private final Border inner;

    public ABorder(int tickness, Color lineColor, String title, boolean rounded) {
        Border b1 = BorderFactory.createLineBorder(lineColor, tickness, rounded);
        if (null != title) {
            inner = BorderFactory.createTitledBorder(b1, title);
        } else {
            inner = b1;
        }
    }

    public ABorder(String title) {
        this(2, Color.WHITE, title, true);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        inner.paintBorder(c, g, x, y, width, height);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return inner.getBorderInsets(c);
    }

    @Override
    public boolean isBorderOpaque() {
        return inner.isBorderOpaque();
    }

}
