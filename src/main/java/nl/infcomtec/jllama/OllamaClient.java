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

/**
 * The OllamaClient class is used to interact with the Ollama API. It provides
 * methods to generate text responses, handle model sessions, and manage
 * branches within a session.
 */
public class OllamaClient {

    private static final String GENERATE = "/api/generate";
    private final String API_GENERATE;
    private final String endPoint;

    /**
     * A TreeMap to store the model sessions, mapped by their names.
     */
    public final TreeMap<String, ModelSession> sessions = new TreeMap<>();
    /**
     * A TreeMap to store the current branch for each model, mapped by model
     * name.
     */
    public final TreeMap<String, String> curBranch = new TreeMap<>();
    /**
     * The name of the currently active model.
     */
    public String curModel = "";

    /**
     * Constructs an OllamaClient instance with the given endpoint.
     *
     * @param endPoint The endpoint for the Ollama API.
     */
    public OllamaClient(String endPoint) {
        this.endPoint = endPoint;
        if (null != Ollama.config) {
            if (null == Ollama.config.lastEndpoint
                    || !Ollama.config.lastEndpoint.equalsIgnoreCase(endPoint)) {
                Ollama.config.lastEndpoint = endPoint;
                Ollama.config.update();
            }
        }
        API_GENERATE = endPoint + GENERATE;
    }

    /**
     * Clears all the sessions and creates a new session for the current model.
     */
    public void clear() {
        sessions.clear();
        newModel(curModel);
    }

    /**
     * Sends a direct request to the Ollama API without streaming.
     *
     * @param rq The Request object containing the request details.
     * @return The Response object containing the API response.
     * @throws Exception If an error occurs during the request.
     */
    public Response direct(Request rq) throws Exception {
        ObjectMapper mapper = Ollama.getMapper();
        String requestBody = mapper.writeValueAsString(rq);
        String response = sendRequest(requestBody);
        return mapper.readValue(response, Response.class);
    }

    /**
     * Sends a direct request to the Ollama API without streaming.
     *
     * @param modelName The name of the model to use.
     * @param system The system prompt.
     * @param prompt The user prompt.
     * @return The Response object containing the API response.
     * @throws Exception If an error occurs during the request.
     */
    public Response direct(String modelName, String system, String prompt) throws Exception {
        Request rq = new Request();
        rq.model = modelName;
        rq.prompt = prompt;
        rq.stream = false;
        rq.system = system;
        return direct(rq);
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
        if (null != Ollama.config) {
            if (null == session || null == session.model || null == session.model.name || !session.model.name.equals(modelName)) {
                Ollama.config.lastModel = modelName;
                Ollama.config.update();
            }
        }
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
        curModel = modelName;
    }

    /**
     * Get the ModelSession for the specified model name.
     *
     * @param modelName The name of the model.
     * @return The ModelSession object, or null if not found.
     */
    public ModelSession getSession(String modelName) {
        String branch = curBranch.get(modelName);
        if (null == branch) {
            return null;
        }
        return sessions.get(branch);
    }

    /**
     * Get the ModelSession for the current model.
     *
     * @return The ModelSession object for the current model.
     */
    public ModelSession getSession() {
        return getSession(curModel);
    }

    /**
     * Get the list of ModelInteraction objects for the current model session.
     *
     * @return The LinkedList of ModelInteraction objects, or null if the
     * session is not found.
     */
    public LinkedList<ModelInteraction> getInter() {
        ModelSession get = getSession();
        return null != get ? get.context : null;
    }

    /**
     * Get the context (array of Integer) for the current model session.
     *
     * @return The array of Integer objects representing the context, or null if
     * the session is not found or empty.
     */
    public Integer[] getContext() {
        LinkedList<ModelInteraction> get = getInter();
        if (null != get && !get.isEmpty()) {
            Integer[] ctx = new Integer[0];
            return get.getLast().response.context.toArray(ctx);
        }
        return null;
    }

    /**
     * Add a response to the current model session.
     *
     * @param rq The Request object containing the request details.
     * @param resp The Response object containing the response details.
     */
    private void addResponse(Request rq, Response resp) {
        LinkedList<ModelInteraction> get = getInter();
        if (null == get) {
            get = new LinkedList<>();
            getSession().context = get;
        }
        get.add(new ModelInteraction(rq, resp));
    }

    /**
     * Send a prompt to the specified model and get the response.
     *
     * @param model The name of the model to use.
     * @param prompt The user prompt.
     * @return The Response object containing the API response.
     * @throws Exception If an error occurs during the request.
     */
    public Response askAndAnswer(String model, String prompt) throws Exception {
        return askAndAnswer(model, prompt, (RenderedImage[]) null);
    }

    /**
     * Send a prompt and images to the specified model and get the response.
     *
     * @param model The name of the model to use.
     * @param prompt The user prompt.
     * @param images The array of RenderedImage objects to include with the
     * request.
     * @return The Response object containing the API response.
     * @throws Exception If an error occurs during the request.
     */
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

    /**
     * Set the image data in the Request object.
     *
     * @param images The array of RenderedImage objects to include with the
     * request.
     * @param rq The Request object to modify.
     * @throws IOException If an error occurs while encoding the image data.
     */
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

    /**
     * Send a prompt to the specified model and get the response, streaming the
     * output.
     *
     * @param model The name of the model to use.
     * @param prompt The user prompt.
     * @param listener The StreamListener object to receive the streamed
     * response.
     * @return The Response object containing the API response.
     * @throws Exception If an error occurs during the request.
     */
    public Response askWithStream(String model, String prompt, StreamListener listener) throws Exception {
        return askWithStream(model, prompt, listener, (RenderedImage[]) null);
    }

    /**
     * Send a prompt and images to the specified model and get the response,
     * streaming the output.
     *
     * @param model The name of the model to use.
     * @param prompt The user prompt.
     * @param listener The StreamListener object to receive the streamed
     * response.
     * @param images The array of RenderedImage objects to include with the
     * request.
     * @return The Response object containing the API response.
     * @throws Exception If an error occurs during the request.
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

    /**
     * Send a request to the Ollama API and return the response as a String.
     *
     * @param requestBody The request body as a String.
     * @return The response from the API as a String.
     * @throws Exception If an error occurs during the request.
     */
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

    /**
     * Send a request to the Ollama API and stream the response to a
     * StreamListener.
     *
     * @param requestBody The request body as a String.
     * @param listener The StreamListener object to receive the streamed
     * response.
     * @return The Response object containing the API response.
     * @throws Exception If an error occurs during the request.
     */
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

    /**
     * The ModelSession class represents a session for a particular model. It
     * contains the model information and the context (history) of interactions.
     */
    public class ModelSession {

        public AvailableModels.AvailableModel model;
        public LinkedList<ModelInteraction> context;
    }

    /**
     * The StreamListener interface defines a callback method for receiving
     * streamed responses.
     */
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
