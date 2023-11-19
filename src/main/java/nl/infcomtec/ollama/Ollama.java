package nl.infcomtec.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import nl.infcomtec.simpleimage.ImageViewer;

/**
 * Main class.
 *
 * Holds global variables and base functions.
 *
 * @author walter
 */
public class Ollama {

    static boolean streaming = false;
    static boolean chatMode = true;
    public static final File HOME_DIR = new File(System.getProperty("user.home"));
    public static final File WORK_DIR = new File(HOME_DIR, ".ollama.data");
    private static final String API_TAGS = "http://localhost:11434/api/tags";
    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendPattern("XXX")
            .toFormatter();
    public static final String DEFAULT_QUESTION = "Hello, I am really excited to talk to you!"
            + " Please tell me about yourself?";

    public static void main(String[] args) {
        if (!WORK_DIR.exists()) {
            WORK_DIR.mkdirs();
        }
        if (!isServiceRunning()) {
            System.out.println("Linux: sudo systemctl start ollama");
            System.exit(2);
        }
        if (getAvailableModels().isEmpty()) {
            System.out.println("Commandline: ollama run mistral");
            System.exit(2);
        }
        if (chatMode) {
            new OllamaChatFrame();
        } else {
            String model = getAvailableModels().first();
            OllamaClient client = new OllamaClient();
            try {
                if (!streaming) {
                    Response answer = client.askAndAnswer(model, DEFAULT_QUESTION);
                    System.out.println(answer);
                } else {
                    Response answer = client.askWithStream(model, DEFAULT_QUESTION, new OllamaClient.StreamListener() {
                        @Override
                        public boolean onResponseReceived(StreamedResponse responsePart) {
                            System.out.println(responsePart.response);
                            return true;
                        }
                    });
                    System.out.println(answer);
                }
            } catch (Exception ex) {
                Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static int[] startsAndEndsWith(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex < 0) {
            return null;
        }

        int endIndex = text.lastIndexOf(end);
        if (endIndex == -1) {
            return null;
        }
        return new int[]{startIndex, endIndex};
    }

    /**
     * This function will check the output from the LLM for special cases.
     * <dl>
     * <dt>PlantUML</dt><dd>Anything between @startuml and @enduml</dd>
     * <dt>SVG</dt><dd>Anything between &lt;svg and &lt;/svg&gt;</dd>
     * <dt>GraphViz</dt><dd>Anything between digraph and }</dd>
     * <dt>System commands</dt><dd>Anything between $@ and @$</dd>
     * </dl>
     *
     * @param currentText Text to scan.
     */
    public static void handleOutput(String currentText) {
        System.out.println("Scanning " + currentText);
        int[] uml = startsAndEndsWith(currentText, "@startuml", "@enduml"); // Check for plantUML content
        int[] svg = startsAndEndsWith(currentText, "<", "</svg>"); // check for SVG content
        int[] dot = startsAndEndsWith(currentText, "digraph", "}");// check for GraphViz content
        if (null != uml) {
            handlePlantUML(currentText.substring(uml[0], uml[1]));
            handleRest(currentText, uml);
        } else if (null != svg) {
            handleSVG(currentText.substring(svg[0], svg[1]));
            handleRest(currentText, svg);
        } else if (null != dot) {
            handleDOT(currentText.substring(dot[0], dot[1]));
            handleRest(currentText, dot);
        } else {
            StringBuilder cat = null;
            while (currentText.contains("$@")) {
                int cmd = currentText.indexOf("$@");
                if (cmd >= 0) {
                    int eoc = currentText.indexOf("@$", cmd);
                    if (eoc > cmd + 2) {
                        if (null == cat) {
                            cat = new StringBuilder();
                        } else {
                            cat.append(System.lineSeparator());
                        }
                        cat.append(handleCommand(currentText.substring(cmd + 2, eoc)));
                        currentText = new StringBuilder(currentText).delete(cmd, eoc + 2).toString();
                    }
                }
            }
        }
    }

    private static void handleRest(String currentText, int[] se) {
        StringBuilder cat = new StringBuilder(currentText);
        cat.delete(se[0], se[1]);
        handleOutput(cat.toString());
    }

    /**
     * This should not be done on the real system but in a Vagrant instance.
     * //TODO port that code.
     *
     * @param cmdString Command to execute.
     * @return Output of the command.
     */
    private static String handleCommand(final String cmdString) {
        final StringBuilder output = new StringBuilder();
        try {
            String[] commandArray;
            if (System.getProperty("os.name").startsWith("Windows")) {
                commandArray = new String[]{"cmd.exe", "/c", cmdString};
            } else {
                commandArray = new String[]{"/bin/bash", "-c", cmdString};
            }

            ProcessBuilder pb = new ProcessBuilder(commandArray);
            pb.directory(WORK_DIR);
            pb.redirectErrorStream(true);
            final Process process = pb.start();

            Thread outputThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
                        output.append("\nYour command ").append(cmdString).append(" caused exception ").append(e.getMessage());
                    }
                }
            });
            outputThread.start();

            if (process.waitFor(10, TimeUnit.SECONDS)) {
                outputThread.join(); // Wait for the output reading thread to finish
            } else {
                process.destroy();
                output.append("\nYour command ").append(cmdString).append(" timed out");
            }
        } catch (Exception ex) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
            output.append("\nYour command ").append(cmdString).append(" caused exception ").append(ex.getMessage());
        }
        return output.toString();
    }

    /**
     * Handles DOT content detected on the clipboard.
     *
     * @param currentText The detected DOT content.
     */
    private static void handleDOT(String currentText) {
        // TODO fix
        String filename = JOptionPane.showInputDialog(null, "Filename (without extension):", "PlantUML", JOptionPane.QUESTION_MESSAGE);
        filename = filename.trim();
        String fullFilename = filename + ".dot";
        File outputFile = new File(WORK_DIR, fullFilename);

        // Handle backups
        if (outputFile.exists()) {
            File backupFile = new File(WORK_DIR, filename + ".bak");
            backupFile.delete(); // Delete existing backup
            outputFile.renameTo(backupFile); // Rename current file to backup
        }

        // Save the current text to the new file
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(currentText);
        } catch (IOException ex) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Run Graphviz
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "-o", filename + ".png", outputFile.getAbsolutePath());
            pb.directory(WORK_DIR);
            Process process = pb.start();
            process.waitFor();

            File pngOutputFile = new File(WORK_DIR, filename + ".png");
            displayImage(pngOutputFile);

        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Handles SVG content detected on the clipboard.
     *
     * @param currentText The detected DOT content.
     */
    private static void handleSVG(String currentText) {
        // TODO fix
        String filename = JOptionPane.showInputDialog(null, "Filename (without extension):", "PlantUML", JOptionPane.QUESTION_MESSAGE);
        filename = filename.trim();
        File svgFile = new File(WORK_DIR, filename + ".svg");
        File pngFile = new File(WORK_DIR, filename + ".png");

        // Handle backups
        if (svgFile.exists()) {
            File backupFile = new File(WORK_DIR, filename + ".bak");
            backupFile.delete(); // Delete existing backup
            svgFile.renameTo(backupFile); // Rename current file to backup
        }

        // Save the current text to the new file
        try (FileWriter writer = new FileWriter(svgFile)) {
            writer.write(currentText);
        } catch (IOException ex) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Run ImageMagick
        try {
            ProcessBuilder pb = new ProcessBuilder("convert", svgFile.getAbsolutePath(), pngFile.getAbsolutePath());
            pb.inheritIO();
            pb.directory(WORK_DIR);
            Process process = pb.start();
            process.waitFor();

            displayImage(pngFile);

        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void displayImage(File f) {
        new ImageViewer(f).getScalePanFrame();
    }

    /**
     * Handles PlantUML content detected on the clipboard.
     *
     * @param currentText The detected PlantUML content.
     */
    private static void handlePlantUML(String currentText) {
        // TODO fix
        String filename = JOptionPane.showInputDialog(null, "Filename (without extension):", "PlantUML", JOptionPane.QUESTION_MESSAGE);
        filename = filename.trim();
        // If "Cancel" is pressed or no filename is provided
        if (filename == null || filename.trim().isEmpty()) {
            return;
        }
        String fullFilename = filename + ".txt";
        File outputFile = new File(WORK_DIR, fullFilename);

        // Handle backups
        if (outputFile.exists()) {
            File backupFile = new File(WORK_DIR, filename + ".bak");
            backupFile.delete(); // Delete existing backup
            outputFile.renameTo(backupFile); // Rename current file to backup
        }

        // Save the current text to the new file
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(currentText);
        } catch (IOException ex) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Run PlantUML with PNG output
        try {
            ProcessBuilder pb = new ProcessBuilder("plantuml", "-tpng", outputFile.getAbsolutePath());
            pb.directory(WORK_DIR);
            Process process = pb.start();
            process.waitFor();

            File pngOutputFile = new File(WORK_DIR, filename.trim() + ".png");
            displayImage(pngOutputFile);
        } catch (Exception e) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Get an "object-aware" version of ObjectMapper.
     *
     * @return Jackson object mapper.
     */
    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Fetch the known models.
     *
     * @return See Ollama API doc.
     * @throws Exception Of course.
     */
    public static AvailableModels fetchAvailableModels() throws Exception {
        URL url = new URL(API_TAGS);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return getMapper().readValue(response.toString(), AvailableModels.class);
        } finally {
            con.disconnect();
        }
    }

    /**
     * Fetch the known models.
     *
     * @return Set of model names with tags.
     */
    public static SortedSet<String> getAvailableModels() {
        TreeSet<String> ret = new TreeSet<>();
        try {
            AvailableModels availableModels;
            availableModels = fetchAvailableModels();
            for (AvailableModels.AvailableModel am : availableModels.models) {
                ret.add(am.name);
            }
        } catch (Exception ex) {
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    /**
     * Checks if the Ollama service is currently running.
     *
     * @return true if the service is running (models can be fetched), false
     * otherwise.
     */
    private static boolean isServiceRunning() {
        try {
            AvailableModels availableModels = fetchAvailableModels();
            Logger.getLogger(Ollama.class.getName()).log(Level.INFO, "Ollama service is running: {0}", availableModels.toString());
            return true;
        } catch (Exception ex) {
            Logger.getLogger(Ollama.class.getName()).log(Level.INFO, "Ollama service is not running: {0}", ex.getMessage());
        }
        return false;
    }

}
