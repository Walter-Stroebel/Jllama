package nl.infcomtec.jllama;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import nl.infcomtec.tools.ImageConverter;

public class DallEClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/images/generations";

    public DallEClient() {
        if (null == Ollama.config.openAIKey || Ollama.config.openAIKey.isEmpty()) {
            throw new RuntimeException("No OpenAI key");
        }
    }

    public BufferedImage getImage(String prompt) throws Exception {
        ObjectMapper mapper = Ollama.getMapper();

        DallERequest rq = new DallERequest();
        rq.prompt = prompt;
        String requestBody = mapper.writeValueAsString(rq);
        String response = sendRequest(requestBody);

        DallEResponse resp = mapper.readValue(response, DallEResponse.class);
        byte[] decode = Base64.getDecoder().decode(resp.data[0].b64Json);
        return new ImageConverter().convertWebpToImage(decode);
    }

    public static class DallERequest {

        public String model = "dall-e-3";
        public String prompt;
        public int n = 1;
        public String size = "1024x1024";
        public String style = "natural";
        public String quality = "hd";
        @JsonProperty(value = "response_format")
        public String responseFormat = "b64_json";
    }

    public static class DallERespData {

        @JsonProperty(value = "revised_prompt")
        public String revisedPrompt;
        @JsonProperty(value = "b64_json")
        public String b64Json;
    }

    public static class DallEResponse {

        public int created;
        public DallERespData[] data;
    }

    private String sendRequest(String requestBody) throws Exception {
        URL url = new URL(ENDPOINT);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + Ollama.config.openAIKey);
        con.setDoOutput(true);
        System.out.println(con);
        System.out.println(requestBody);
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
