package nl.infcomtec.ollama;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import nl.infcomtec.tools.ToolManager;

/**
 * Calls nvidia-smi and returns the most vital information.
 *
 * @author Walter Stroebel.
 */
public class NVidiaSMI extends ToolManager {

    private String output;

    /**
     * Simply list the installed cards.
     *
     * @return Any installed and supported cards.
     */
    public String getGPUs() {
        setCommand("nvidia-smi", "-L");
        run();
        return output;
    }

    @Override
    public void run() {
        internalRun();
        if (exitCode != 0) {
            output = "Running tool failed, rc=" + exitCode;
        } else {
            if (stdoutStream instanceof ByteArrayOutputStream) {
                output = new String(((ByteArrayOutputStream) stdoutStream).toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }
}
