/*
 */
package nl.infcomtec.jllama;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author walter
 */
public class DebateDemo {

    public static void main(String[] args) {
        try {
            Ollama.init();
            Debate debate = Debate.mono("Should we focus all development efforts on Artificial Intelligence?", 2);
            debate.debate();
            System.out.println(debate);
        } catch (Exception ex) {
            Logger.getLogger(DebateDemo.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
