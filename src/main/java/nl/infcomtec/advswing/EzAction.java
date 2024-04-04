package nl.infcomtec.advswing;

import java.awt.Color;
import java.awt.Font;
import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 *
 * @author walter
 */
public abstract class EzAction extends AbstractAction {

    public Color background;
    public Font font;
    public Color foreground;

    public EzAction(String name) {
        super(name);
    }

    public EzAction(String name, Icon icon) {
        super(name, icon);
    }
    
    public String getName(){
        return getValue(NAME).toString();
    }
    public String getIcon(){
        return getValue(SMALL_ICON).toString();
    }
    public void setIcon(Icon icon){
        putValue(SMALL_ICON,icon);
    }
    public void setName(String name){
        putValue(NAME, name);
    }

    public EzAction withBackColor(Color color) {
        this.background=color;
        return this;
    }

    public EzAction withIcon(Icon icon) {
        putValue(SMALL_ICON, icon);
        return this;
    }

    public EzAction withFont(Font font) {
        this.font=font;
        return this;
    }

    public EzAction withForeColor(Color color) {
        this.foreground=color;
        return this;
    }
}
