package nl.infcomtec.ollama;

/**
 * One step in a model interaction.
 *
 * @author Walter Stroebel
 */
public class Interaction {

    public final Request request;
    public final Response response;

    public Interaction(Request request, Response response) {
        this.request = request;
        this.response = response;
    }
}
