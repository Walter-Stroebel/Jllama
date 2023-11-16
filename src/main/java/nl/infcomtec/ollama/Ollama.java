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
import java.util.SortedSet;
import java.util.TreeSet;
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
