package nl.infcomtec.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class.
 *
 * Holds global variables and base functions.
 *
 * @author walter
 */
public class Ollama {

    static boolean streaming = true;
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
        if (!isServiceRunning(API_TAGS)) {
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
            String model = getAvailableModels(API_TAGS).first();
            System.out.println("Using model " + model);
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
        return new int[]{startIndex, endIndex + end.length()};
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
     * @param pool The service that will run the threads.
     * @param currentText Text to scan.
     * @return A list of Modality instances, usually zero or one but can be
     * more.
     */
    public static List<Modality> handleOutput(ExecutorService pool, String currentText) {
        System.out.println("Parsing " + currentText);
        LinkedList<Modality> ret = new LinkedList<>();
        int[] uml = startsAndEndsWith(currentText, "@startuml", "@enduml"); // Check for plantUML content
        int[] svg = startsAndEndsWith(currentText, "<", "</svg>"); // check for SVG content
        int[] dot = startsAndEndsWith(currentText, "digraph", "}");// check for GraphViz content
        if (null != uml) {
            ret.add(new ModalityUML(pool, currentText.substring(uml[0], uml[1])));
            handleRest(pool, ret, currentText, uml);
        } else if (null != svg) {
            ret.add(new ModalitySVG(pool, currentText.substring(svg[0], svg[1])));
            handleRest(pool, ret, currentText, svg);
        } else if (null != dot) {
            ret.add(new ModalityDOT(pool, currentText.substring(dot[0], dot[1])));
            handleRest(pool, ret, currentText, dot);
        } else {
            if (currentText.contains(Vagrant.MARK_START)) {
                ret.add(new ModalityVagrant(pool, currentText));
            }
        }
        return ret;
    }
    public static Vagrant vagrant;

    private static void handleRest(ExecutorService pool, LinkedList<Modality> ret, String currentText, int[] se) {
        StringBuilder cat = new StringBuilder(currentText);
        cat.delete(se[0], se[1]);
        String rest = cat.toString();
        if (!rest.trim().isEmpty()) {
            ret.addAll(handleOutput(pool, rest));
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
     * @param endPoint End point for the Ollama service.
     * @return See Ollama API doc.
     * @throws Exception Of course.
     */
    public static AvailableModels fetchAvailableModels(String endPoint) throws Exception {
        URL url = new URL(endPoint);
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
        return getAvailableModels(API_TAGS);
    }

    /**
     * Fetch the known models.
     *
     * @param endPoint End point for the Ollama service.
     * @return Set of model names with tags.
     */
    public static SortedSet<String> getAvailableModels(String endPoint) {
        TreeSet<String> ret = new TreeSet<>();
        try {
            AvailableModels availableModels;
            availableModels = fetchAvailableModels(endPoint);
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
    private static boolean isServiceRunning(String endPoint) {
        try {
            AvailableModels availableModels = fetchAvailableModels(endPoint);
            Logger.getLogger(Ollama.class.getName()).log(Level.INFO, "Ollama service is running: {0}", availableModels.toString());
            return true;
        } catch (Exception ex) {
            Logger.getLogger(Ollama.class.getName()).log(Level.INFO, "Ollama service is not running: {0}", ex.getMessage());
        }
        return false;
    }

}
