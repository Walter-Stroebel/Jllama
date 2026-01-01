package nl.infcomtec.jllama;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
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

    /**
     * The user's home directory.
     */
    public static final File HOME_DIR = new File(System.getProperty("user.home"));

    /**
     * The working directory for storing Ollama data.
     */
    public static final File WORK_DIR = new File(HOME_DIR, ".ollama.data");

    /**
     * The local endpoint for the Ollama server.
     */
    private static final String LOCAL_ENDPOINT = "http://localhost:11434";

    /**
     * A DateTimeFormatter for formatting and parsing LocalDateTime objects.
     */
    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendPattern("XXX")
            .toFormatter();

    /**
     * The configuration file for Ollama.
     */
    public static final File configFile = new File(Ollama.WORK_DIR, "chatcfg.json");

    /**
     * The configuration object for Ollama.
     */
    public static OllamaConfig config;

    /**
     * A map of available models and their metadata.
     */
    private static TreeMap<String, AvailableModels> models;

    /**
     * The Vagrant instance for executing system commands.
     */
    public static Vagrant vagrant;

    /**
     * The API endpoint for fetching tags (available models).
     */
    public static final String TAGS = "/api/tags";
    /**
     * Monitoring hooks.
     */
    private static final LinkedList<Monitor> monitors = new LinkedList<>();

    /**
     * Register a monitor.
     *
     * @param monitor Callback to register.
     */
    public static void registerMonitor(Monitor monitor) {
        synchronized (monitors) {
            monitors.add(monitor);
        }
    }

    /**
     * Register a monitor.
     *
     * @param name The name of a previously registered monitor. Any registered
     *             monitor with that exact name will be de-registered.
     */
    public static void deregisterMonitor(String name) {
        synchronized (monitors) {
            for (Iterator<Monitor> it = monitors.iterator(); it.hasNext();) {
                Monitor mon = it.next();
                if (mon.getName().equals(name)) {
                    try {
                        mon.close();
                    } catch (Exception any) {
                        // just ignore
                    }
                    it.remove();
                }
            }
        }
    }

    /**
     * Will call any registered monitors from a synchronized context.
     *
     * @param req  If true the data should be an API request, else an API
     *             response.
     * @param data Either a request or a response.
     */
    public static void doMonitoring(boolean req, String data) {
        synchronized (monitors) {
            for (Monitor mon : monitors) {
                if (req) {
                    mon.requested(data);
                } else {
                    mon.responded(data);
                }
            }
        }
    }

    /**
     * Will call any registered monitors from a synchronized context.
     *
     * @param exception The exception that occurred.
     */
    public static void oops(Exception exception) {
        synchronized (monitors) {
            for (Monitor mon : monitors) {
                mon.oops(exception);
            }
        }
    }

    /**
     * The main entry point of the application.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        init();
        new OllamaChatFrame();
    }

    /**
     * Initialize the Ollama application.
     */
    public static void init() {
        if (!WORK_DIR.exists()) {
            WORK_DIR.mkdirs();
        }
        try {
            if (configFile.exists()) {
                config = getMapper().readValue(configFile, OllamaConfig.class);
            }
            if (null == config.ollamas) {
                config.ollamas = new String[] { LOCAL_ENDPOINT };
                config.update();
            }
        } catch (Exception any) {
            System.out.println(any.getMessage());
            config = null;
        }
        if (null == config) {
            config = new OllamaConfig();
            config.x = config.y = 0;
            config.w = 1000;
            config.h = 700;
            config.fontSize = 18;
            config.ollamas = new String[] { LOCAL_ENDPOINT };
            config.update();
        }
        if (null == config.openAIKey || config.openAIKey.isEmpty()) {
            System.out.println("No OpenAI key: Dall-E is disabled.");
            System.out.println("Edit " + configFile.getAbsolutePath() + " to fix this.");
        }
        if (fetchAvailableModels().isEmpty()) {
            System.out.println("Startup failed. Suggestions:"
                    + "\n\tollama run mistral"
                    + "\n\tollama serve"
                    + "\n\tsudo systemctl start ollama");
            System.out.println("Or edit " + configFile.getAbsolutePath() + " to fix this.");
            System.exit(2);
        }
    }

    /**
     * Find the start and end indices of a substring within a given string.
     *
     * @param text  The input string.
     * @param start The start marker.
     * @param end   The end marker.
     * @return An array containing the start and end indices, or null if not
     *         found.
     */
    private static int[] startsAndEndsWith(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex < 0) {
            return null;
        }

        int endIndex = text.lastIndexOf(end);
        if (endIndex == -1) {
            return null;
        }
        return new int[] { startIndex, endIndex + end.length() };
    }

    /**
     * This function will check the output from the LLM for special cases.
     * <dl>
     * <dt>PlantUML</dt>
     * <dd>Anything between @startuml and @enduml</dd>
     * <dt>SVG</dt>
     * <dd>Anything between &lt;svg and &lt;/svg&gt;</dd>
     * <dt>GraphViz</dt>
     * <dd>Anything between digraph and }</dd>
     * <dt>System commands</dt>
     * <dd>Anything between $@ and @$</dd>
     * </dl>
     *
     * @param pool        The service that will run the threads.
     * @param currentText Text to scan.
     * @return A list of Modality instances, usually zero or one but can be
     *         more.
     */
    public static List<Modality> handleOutput(ExecutorService pool, String currentText) {
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

    /**
     * Handle the remaining text after extracting a special case.
     *
     * @param pool        The service that will run the threads.
     * @param ret         The list of Modality instances to add to.
     * @param currentText The original text.
     * @param se          The start and end indices of the extracted special case.
     */
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
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        mapper.registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Get the (cached) known models.
     *
     * @return A map of available models and their metadata.
     */
    public static TreeMap<String, AvailableModels> getAvailableModels() {
        if (null == models) {
            models = fetchAvailableModels();
        }
        return models;
    }

    /**
     * Fetch the known models.
     *
     * @return A map of available models and their metadata.
     */
    public static TreeMap<String, AvailableModels> fetchAvailableModels() {
        TreeMap<String, AvailableModels> ret = new TreeMap<>();
        for (String endPoint : config.ollamas) {
            AvailableModels avm = fetchAvailableModels(endPoint);
            if (null != avm) {
                ret.put(endPoint, avm);
            }
        }
        return models = ret;
    }

    /**
     * Fetch the available models from a given endpoint.
     *
     * @param endPoint The endpoint to fetch from.
     * @return The available models metadata, or null if the endpoint is not
     *         responding.
     */
    public static AvailableModels fetchAvailableModels(String endPoint) {
        try {
            URL url = new URL(endPoint + TAGS);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return getMapper().readValue(response.toString(), AvailableModels.class);
            } catch (Exception any) {
                oops(any);
            } finally {
                con.disconnect();
            }
        } catch (Exception ex) {
            oops(ex);
            Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
