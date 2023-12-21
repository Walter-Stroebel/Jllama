package nl.infcomtec.jllama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedList;
import java.util.TreeMap;
import javax.imageio.ImageIO;

public class OllamaClient {

    private static final String GENERATE = "/api/generate";
    private final String API_GENERATE;
    private final String endPoint;

    public class ModelSession {

        public AvailableModels.AvailableModel model;
        public LinkedList<ModelInteraction> context;
    }
    public final TreeMap<String, ModelSession> sessions = new TreeMap<>();
    public final TreeMap<String, String> curBranch = new TreeMap<>();
    public String curModel = "";

    public OllamaClient(String endPoint) {
        this.endPoint = endPoint;
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
    public ModelSession newBranch(String branchName) {
        ModelSession nbr = new ModelSession();
        ModelSession obr = getSession();
        nbr.context = new LinkedList<>(obr.context);
        nbr.model = obr.model;
        curBranch.put(nbr.model.name, branchName);
        return sessions.put(branchName, nbr);
    }

    /**
     * Start a new tree if the model has no tree.
     *
     * @param modelName Name of the model we will query.
     */
    public void newModel(String modelName) {
        ModelSession session = getSession(modelName);
        if (null == session || null == session.model || null == session.model.name || !session.model.name.equals(modelName)) {
            Ollama.config.lastModel = modelName;
            Ollama.config.update();
            session = new ModelSession();
            AvailableModels mods = Ollama.getAvailableModels().get(endPoint);
            for (AvailableModels.AvailableModel am : mods.models) {
                if (am.name.equals(modelName)) {
                    session.model = am;
                    break;
                }
            }
            curBranch.put(modelName, modelName);
            sessions.put(modelName, session);
        }
        curModel = modelName;
    }

    public ModelSession getSession(String modelName) {
        String branch = curBranch.get(modelName);
        if (null == branch) {
            return null;
        }
        return sessions.get(branch);
    }

    public ModelSession getSession() {
        return getSession(curModel);
    }

    public LinkedList<ModelInteraction> getInter() {
        ModelSession get = getSession();
        return null != get ? get.context : null;
    }

    public Integer[] getContext() {
        LinkedList<ModelInteraction> get = getInter();
        if (null != get && !get.isEmpty()) {
            Integer[] ctx = new Integer[0];
            return get.getLast().response.context.toArray(ctx);
        }
        return null;
    }

    private void addResponse(Request rq, Response resp) {
        LinkedList<ModelInteraction> get = getInter();
        if (null == get) {
            get = new LinkedList<>();
            getSession().context = get;
        }
        get.add(new ModelInteraction(rq, resp));
    }

    public Response askAndAnswer(String model, String prompt) throws Exception {
        return askAndAnswer(model, prompt, (RenderedImage[]) null);
    }

    public Response askAndAnswer(String model, String prompt, RenderedImage... images) throws Exception {
        newModel(model);
        ObjectMapper mapper = Ollama.getMapper();
        Request rq = new Request();
        rq.model = model;
        rq.prompt = prompt;
        rq.context = getContext();
        setReqImages(images, rq);
        String requestBody = mapper.writeValueAsString(rq);
        String response = sendRequest(requestBody);
        Response resp = mapper.readValue(response, Response.class);
        addResponse(rq, resp);
        return resp;
    }

    private void setReqImages(RenderedImage[] images, Request rq) throws IOException {
        if (null != images) {
            for (RenderedImage im : images) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(im, "png", baos);
                    baos.flush();
                    String enc = Base64.getEncoder().encodeToString(baos.toByteArray());
                    if (null == rq.images) {
                        rq.images = new String[1];
                        rq.images[0] = enc;
                    } else {
                        String[] oi = rq.images;
                        rq.images = new String[oi.length + 1];
                        System.arraycopy(oi, 0, rq.images, 0, oi.length);
                        rq.images[oi.length] = enc;
                    }
                }
            }
        }
    }

    public Response askWithStream(String model, String prompt, StreamListener listener) throws Exception {
        return askWithStream(model, prompt, listener, (RenderedImage[]) null);
    }

    /**
     * This calls the listener for each word.
     *
     * @param model The model to use.
     * @param prompt The question.
     * @param listener Callback.
     * @param images For vision capable models.
     * @return Unlike the specification at
     * https://github.com/jmorganca/ollama/blob/main/docs/api.md, this will also
     * contain the full (concatenated) response in the response field.
     * @throws Exception For reasons.
     */
    public Response askWithStream(String model, String prompt, StreamListener listener, RenderedImage... images) throws Exception {
        newModel(model);
        if (null == listener) {
            throw (new RuntimeException("Listener is null"));
        }
        ObjectMapper mapper = Ollama.getMapper();

        Request rq = new Request();
        rq.model = model;
        rq.prompt = prompt;
        rq.stream = true;
        rq.context = getContext();
        setReqImages(images, rq);
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
}
