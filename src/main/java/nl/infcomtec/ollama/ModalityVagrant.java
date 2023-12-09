package nl.infcomtec.ollama;

import java.util.concurrent.ExecutorService;

/**
 * Vagrant Modality.
 *
 * @author Walter Stroebel
 */
public class ModalityVagrant extends Modality {

    private static final Vagrant vagrant = new Vagrant();

    public ModalityVagrant(ExecutorService pool, String currentText) {
        super(pool, currentText, false);
        vagrant.start();
    }

    @Override
    protected void convert() {
        System.out.println("Vagrant exec: " + currentText);
        outputText = vagrant.exec(currentText);
    }

}
