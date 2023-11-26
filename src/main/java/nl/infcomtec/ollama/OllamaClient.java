package nl.infcomtec.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.TreeMap;

public class OllamaClient {

    private static final String GENERATE = "/api/generate";
    private final String API_GENERATE;
    private final TreeMap<String, LinkedList<Interaction>> context = new TreeMap<>();
    private String branch = "default";

    public OllamaClient(String endPoint) {
        if (null == Ollama.config.lastEndpoint
                || !Ollama.config.lastEndpoint.equalsIgnoreCase(endPoint)) {
            Ollama.config.lastEndpoint = endPoint;
            Ollama.config.update();
        }
        API_GENERATE = endPoint + GENERATE;
    }

    /**
     * Add a branch from the current one.
     *
     * @param branchName Name of the branch to add.
     * @return null or the old contents of the branchName if it was not new.
     * @throws NullPointerException if you try to branch from nothing.
     */
    public LinkedList<Interaction> newBranch(String branchName) {
        LinkedList<Interaction> nBr = new LinkedList<>(context.get(branch));
        branch = branchName;
        return context.put(branchName, nBr);
    }

    private Integer[] getContext() {
        LinkedList<Interaction> get = context.get(branch);
        if (null != get && !get.isEmpty()) {
            Integer[] ctx = new Integer[0];
            return get.getLast().response.context.toArray(ctx);
        }
        return null;
    }

    private void addResponse(Request rq, Response resp) {
        LinkedList<Interaction> get = context.get(branch);
        if (null == get) {
            get = new LinkedList<>();
            context.put(branch, get);
        }
        get.add(new Interaction(rq, resp));
    }

    public Response askAndAnswer(String model, String prompt) throws Exception {
        ObjectMapper mapper = Ollama.getMapper();

        Request rq = new Request();
        rq.model = model;
        rq.prompt = prompt;
        rq.context = getContext();
        String requestBody = mapper.writeValueAsString(rq);
        String response = sendRequest(requestBody);

        Response resp = mapper.readValue(response, Response.class);
        addResponse(rq, resp);
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
        rq.context = getContext();
        String requestBody = mapper.writeValueAsString(rq);
        Response resp = sendRequestWithStreaming(requestBody, listener);
        addResponse(rq, resp);
        return resp;
    }

    private String sendRequest(String requestBody) throws Exception {
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
                    if (responseLine.startsWith("{\"error")) {
                        Response err = new Response();
                        err.context = new LinkedList<>();
                        err.createdAt = LocalDateTime.now();
                        err.done = true;
                        err.evalCount = 0;
                        err.evalDuration = 1;
                        err.loadDuration = 1;
                        err.model = "?";
                        err.promptEvalCount = 0;
                        err.promptEvalDuration = 1;
                        err.sampleCount = 0;
                        err.sampleDuration = 1;
                        err.totalDuration = 3;
                        err.response = responseLine;
                        listener.onResponseReceived(err);
                        return err;
                    }
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

    /**
     * @return the branch
     */
    public String getBranch() {
        return branch;
    }
}
