package nl.infcomtec.ollama;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    public static final File HOME_DIR = new File(System.getProperty("user.home"));
    public static final File WORK_DIR = new File(HOME_DIR, ".ollama.data");
    private static final String LOCAL_ENDPOINT = "http://localhost:11434";
    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendPattern("XXX")
            .toFormatter();
    public static final String DEFAULT_QUESTION = "Hello, I am really excited to talk to you!"
            + " Please tell me about yourself?";
    public static final File configFile = new File(Ollama.WORK_DIR, "chatcfg.json");
    public static OllamaConfig config;

    public static void main(String[] args) {
        if (!WORK_DIR.exists()) {
            WORK_DIR.mkdirs();
        }
        try {
            if (configFile.exists()) {
                config = getMapper().readValue(configFile, OllamaConfig.class);
            }
            if (null == config.ollamas) {
                config.ollamas = new String[]{LOCAL_ENDPOINT};
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
            config.ollamas = new String[]{LOCAL_ENDPOINT};
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
        if (config.chatMode) {
            new OllamaChatFrame();
        } else {
            try {
                Map.Entry<String, AvailableModels> first = fetchAvailableModels().firstEntry();
                String model = first.getValue().models[0].name;
                System.out.println("Using model " + model);
                OllamaClient client = new OllamaClient(first.getKey());
                if (!config.streaming) {
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
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
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
    public static TreeMap<String, AvailableModels> fetchAvailableModels() {
        TreeMap<String, AvailableModels> ret = new TreeMap<>();
        for (String endPoint : config.ollamas) {
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
                    ret.put(endPoint, getMapper().readValue(response.toString(), AvailableModels.class));
                } catch (Exception any) {
                    System.out.println("Endpoint " + url + " is not responding.");
                } finally {
                    con.disconnect();
                }
            } catch (Exception ex) {
                Logger.getLogger(Ollama.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }
    public static final String TAGS = "/api/tags";

}
