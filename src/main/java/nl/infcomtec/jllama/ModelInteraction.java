package nl.infcomtec.jllama;

/**
 * One step in a model interaction.
 *
 * @author Walter Stroebel
 */
public class ModelInteraction {

    public final Request request;
    public final Response response;

    public ModelInteraction(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public String toString() {
        return "ModelInteraction{" + "request=" + request + ", response=" + response + '}';
    }
}
