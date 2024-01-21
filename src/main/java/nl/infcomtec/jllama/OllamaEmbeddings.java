package nl.infcomtec.jllama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaEmbeddings {

    private static final String EMBED = "/api/embeddings";
    private final String model;
    private final String endPoint;
    private final String API_EMBED;

    public OllamaEmbeddings(String endPoint, String model) {
        this.endPoint = endPoint;
        this.model = model;
        API_EMBED = endPoint + EMBED;
    }

    public Embeddings getEmbeddings(String prompt) throws Exception {
        ObjectMapper mapper = Ollama.getMapper();
        Embeddings ret = new Embeddings();
        ret.request = new Embeddings.Request();
        ret.request.model = model;
        ret.request.prompt = prompt;
        String requestBody = mapper.writeValueAsString(ret.request);
        String response = sendRequest(requestBody);
        ret.response = mapper.readValue(response, Embeddings.Response.class);
        return ret;
    }

    private String sendRequest(String requestBody) throws Exception {
        URL url = new URL(API_EMBED);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } finally {
            con.disconnect();
        }
    }
}
