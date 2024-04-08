package nl.infcomtec.jllama;

/**
 * For monitoring callbacks.
 *
 * @author walter
 */
public interface Monitor extends AutoCloseable {

    /**
     * To identify this monitor.
     * <b>Note:</b> Uniqueness is not enforced.
     *
     * @return The name of this monitor.
     */
    String getName();

    /**
     * Will be called on any request.
     *
     * @param request Should be a Ollama API request as JSON.
     */
    void requested(String request);

    /**
     * Will be called when the API responds.
     *
     * @param response As received from the API.
     */
    void responded(String response);

    /**
     * Will be called when an exception occurs.
     * <b>Note:</b> Is not assured, not all exception handlers will call this.
     *
     * @param exception
     */
    void oops(Exception exception);

}
