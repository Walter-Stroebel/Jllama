package nl.infcomtec.jllama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * See the testing method in the documentation.
 *
 * @author walter
 */
public class ModelTester {

    private static final String NL = System.lineSeparator();
    private static final String EVAL_SYSTEM = "Compare a generated answer to an expected answer."
            + " If the generated answer captures the question's core idea,"
            + " even with extra details or a different perspective, answer 'YES'."
            + " Answer 'NO' only if it misses the core idea.";
    public StringBuilder fullTest = new StringBuilder();
    public String eval;

    public void run() {
        ObjectMapper mapper = Ollama.getMapper();
        LinkedList<String> evaluations = new LinkedList<>();
        try (BufferedReader bfr = openResource("files")) {
            for (String resNam = bfr.readLine(); null != resNam; resNam = bfr.readLine()) {
                try (BufferedReader res = openResource(resNam)) {
                    if (resNam.endsWith(".json")) {
                        Test test = mapper.readValue(res, Test.class);
                        fullTest.append(NL).append("# Title: ").append(test.passage.title).append(NL);
                        fullTest.append("# Story:").append(NL);
                        fullTest.append(test.passage.story).append(NL);
                        for (int i = 0; i < 10; i++) {
                            fullTest.append(NL).append("## Pass ").append(i + 1).append(NL);
                            OllamaClient client = new OllamaClient(Ollama.config.lastEndpoint);
                            String question = test.passage.story + NL;
                            for (Test.Question q : test.questions) {
                                fullTest.append(NL).append("## Question: ").append(q.text).append(NL);
                                Response ans = client.askAndAnswer(Ollama.config.lastModel, question + q.text);
                                question = "";
                                fullTest.append("- ").append(q.answer).append(NL).append("  - ").append(ans.response);
                                StringBuilder forEval = new StringBuilder("Is the generated answer \"");
                                forEval.append(ans.response.replace('"', '\''));
                                forEval.append("\" semantically equivalent to the expected answer \"");
                                forEval.append(q.answer.replace('"', '\'')).append("\"?");
                                evaluations.add(forEval.toString());
                            }
                        }
                    }
                } catch (Exception any) {
                    System.out.println(any.getMessage());
                    System.out.println("In: " + resNam);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ModelTester.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            OllamaClient evaluator = new OllamaClient(Ollama.config.lastEndpoint);

            int score = 0;
            for (String ev : evaluations) {
                System.out.println(ev);
                // parameters: model name, system prompt, prompt
                // this call does not maintain a context
                Response direct = evaluator.direct("mistral", EVAL_SYSTEM, ev);
                fullTest.append(NL).append("  - Eval: ").append(direct.response);
                if (direct.response.toUpperCase().contains("YES")) {
                    score++;
                } else {
                    score -= 2;
                }
            }
            StringBuilder endEval = new StringBuilder("Model under test: ").append(Ollama.config.lastModel).append(NL);
            endEval.append("Evaluation score = ").append(score).append(NL);
            eval = endEval.toString();
        } catch (Exception any) {
            eval = "Evaluation failed: " + any.getMessage();
        }
    }

    private BufferedReader openResource(String name) {
        return new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(name)));
    }

    /**
     * Corresponds to the JSON test cases
     */
    public static class Test {

        public Passage passage;
        public Question[] questions;

        public static class Passage {

            public String title;
            public String story;
        }

        public static class Question {

            public String text;
            public String answer;
        }
    }
}
