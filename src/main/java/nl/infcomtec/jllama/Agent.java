package nl.infcomtec.jllama;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author Walter Stroebel.
 */
public class Agent {

    public final String role;
    public final String task;
    public Response response;

    public Agent(String role, String task) {
        this.role = role;
        this.task = task;
    }

    public Future<Agent> interact(final ExecutorService pool, final OllamaClient client, final String model) {
        final StringBuilder question = new StringBuilder("You are a cooperative agent.\n");
        question.append("Your role is: ").append(role).append("\n");
        question.append("Your task is: ").append(task).append("\n");
        return pool.submit(new Callable<Agent>() {
            @Override
            public Agent call() throws Exception {
                response = client.askAndAnswer(model, question.toString());
                return Agent.this;
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Ollama.init();
        String endpoint = Ollama.config.getLastEndpoint();
        String model = Ollama.config.lastModel;
        OllamaClient client = new OllamaClient(endpoint);
        Agent agent = new Agent("Evaluator", "What is bigger, a bus or a train?");
        ExecutorService pool = Executors.newCachedThreadPool();
        Future<Agent> interact = agent.interact(pool, client, model);
        System.out.println(interact.get().response.response);
        pool.shutdown();
    }
}
