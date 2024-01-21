package nl.infcomtec.jllama;

/**
 * Embeddings.
 *
 * @author walter
 */
public class Embeddings {

    public Request request;
    public Response response;

    public static class Request {

        public String model;
        public String prompt;

        @Override
        public String toString() {
            return "Request{" + "model=" + model + ", prompt=" + prompt + '}';
        }
    }

    public static class Response {

        public double[] embedding;

        @Override
        public String toString() {
            return "Response{embedding=double[" + embedding.length + "]}";
        }
    }

    @Override
    public String toString() {
        return "Embeddings{" + "request=" + request + ", response=" + response + '}';
    }
}
