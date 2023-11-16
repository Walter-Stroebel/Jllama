package nl.infcomtec.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeMap;

public class OllamaClient {

    private static final String API_GENERATE = "http://localhost:11434/api/generate";
    private static final TreeMap<String, ArrayList<Integer>> context = new TreeMap<>();

    public OllamaClient() {
    }

    public Response askAndAnswer(String model, String prompt) throws Exception {
        ObjectMapper mapper = Ollama.getMapper();

        Request rq = new Request();
        rq.model = model;
        rq.prompt = prompt;
        Integer[] ctx = new Integer[0];
        ArrayList<Integer> ctxL = context.get(model);
        if (null != ctxL) {
            ctx = ctxL.toArray(ctx);
        }
        rq.context = ctx;
        String requestBody = mapper.writeValueAsString(rq);
        String response = sendRequest(requestBody);

        Response resp = mapper.readValue(response, Response.class);
        context.put(model, new ArrayList<>(resp.context));
        return resp;
    }

    /**
     * This calls the listener for each word.
     *
     * @param model The model to use.
     * @param prompt The question.
     * @param listener Callback.
     * @return Unlike the specification at
     * https://github.com/jmorganca/ollama/blob/main/docs/api.md, this will also
     * contain the full (concatenated) response in the response field.
     * @throws Exception For reasons.
     */
    public Response askWithStream(String model, String prompt, StreamListener listener) throws Exception {
        if (null == listener) {
            throw (new RuntimeException("Listener is null"));
        }
        ObjectMapper mapper = Ollama.getMapper();

        Request rq = new Request();
        rq.model = model;
        rq.prompt = prompt;
        rq.stream = true;
        Integer[] ctx = new Integer[0];
        ArrayList<Integer> ctxL = context.get(model);
        if (null != ctxL) {
            ctx = ctxL.toArray(ctx);
        }
        rq.context = ctx;
        String requestBody = mapper.writeValueAsString(rq);
        Response resp = sendRequestWithStreaming(requestBody, listener);
        context.put(model, new ArrayList<>(resp.context));
        return resp;
    }

    /**
     * Clear a model's context.
     *
     * @param model to clear the context of.
     */
    public void clearContext(String model) {
        context.put(model, new ArrayList<Integer>());
    }

    private String sendRequest(String requestBody) throws Exception {
        // System.out.println(requestBody);
        URL url = new URL(API_GENERATE);
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

    private Response sendRequestWithStreaming(String requestBody, StreamListener listener) throws Exception {
        URL url = new URL(API_GENERATE);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
            ObjectMapper mapper = Ollama.getMapper();
            String responseLine;
            StringBuilder fullResponse = new StringBuilder();
            while ((responseLine = br.readLine()) != null) {
                if (!responseLine.trim().isEmpty()) {
                    Response val = mapper.readValue(responseLine, Response.class);
                    if (val.done) {
                        val.response = fullResponse.toString();
                        return val;
                    } else {
                        fullResponse.append(val.response);
                        if (!listener.onResponseReceived(val)) {
                            return null;
                        }
                    }
                }
            }
            return mapper.readValue(responseLine, Response.class);
        } finally {
            con.disconnect();
        }
    }

    public interface StreamListener {

        /**
         * Called for each piece of the response.
         *
         * @param responsePart next part.
         * @return true to continue, false to stop.
         */
        boolean onResponseReceived(StreamedResponse responsePart);
    }
}
