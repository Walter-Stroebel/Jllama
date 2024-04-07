package nl.infcomtec.jllama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

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
    public final ModelTestFrame frame;

    public ModelTester() {
        frame = new ModelTestFrame();
        frame.setVisible(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                runTest();
                return null;
            }
        }.execute();
    }

    private void runTest() {
        try {
            frame.start.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(ModelTester.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        frame.postUpdate("Starting test of model: " + frame.model.getText());
        ObjectMapper mapper = Ollama.getMapper();
        LinkedList<String> evaluations = new LinkedList<>();
        try (BufferedReader bfr = openResource("files")) {
            for (String resNam = bfr.readLine(); frame.running.get() && null != resNam; resNam = bfr.readLine()) {
                try (BufferedReader res = openResource(resNam)) {
                    if (resNam.endsWith(".json")) {
                        Test test = mapper.readValue(res, Test.class);
                        frame.postUpdate("Running test: " + test.passage.title);
                        fullTest.append(NL).append("# Title: ").append(test.passage.title).append(NL);
                        fullTest.append("# Story:").append(NL);
                        fullTest.append(test.passage.story).append(NL);
                        OllamaClient client = new OllamaClient(frame.endPoint.getText());
                        for (Test.Question q : test.questions) {
                            fullTest.append(NL).append("## Question: ").append(q.text).append(NL);
                            fullTest.append("## Expected Answer: ").append(q.answer).append(NL);
                            for (int i = 0; frame.running.get() && i < frame.numRuns; i++) {
                                fullTest.append(NL).append("- Pass ").append(i + 1);
                                // parameters: model name, system prompt, prompt
                                // this call does not maintain a context
                                Response ans = client.direct(frame.model.getText(), null, test.passage.story + NL + q.text + NL);
                                fullTest.append(": ").append(ans.response).append(NL);
                                StringBuilder forEval = new StringBuilder("Is the generated answer \"");
                                forEval.append(ans.response.replace('"', '\''));
                                forEval.append("\" to the question \"").append(q.text.replace('"', '\''));
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
        frame.postUpdate("Running test evaluation.");
        fullTest.append(NL).append("# Running test evaluation.").append(NL);
        for (int attempts = 0; attempts < 5; attempts++) {
            if (!frame.running.get()) {
                break;
            }
            try {
                runEvalLLM(evaluations);
                break;
            } catch (Exception any) {
                eval = "## Evaluation failed: " + any.getMessage() + NL;
                frame.postUpdate("Restarting test evaluation: " + any.getMessage());
            }
        }
        try (FileWriter wrt = new FileWriter(frame.output)) {
            wrt.write(eval);
            wrt.write(fullTest.toString());
            wrt.write(NL);
        } catch (IOException ex) {
            Logger.getLogger(ModelTester.class.getName()).log(Level.SEVERE, null, ex);
        }
        frame.dispose();
        JOptionPane.showMessageDialog(null, eval);
    }

    private void runEvalLLM(LinkedList<String> evaluations) throws Exception {
        OllamaClient evaluator = new OllamaClient(frame.endPoint.getText());
        int score = 0;
        double prog10 = evaluations.size() * 0.1;
        int prog = 0;
        for (String ev : evaluations) {
            if (!frame.running.get()) {
                break;
            }
            // parameters: model name, system prompt, prompt
            // this call does not maintain a context
            Response direct = evaluator.direct(frame.evalModel.getText(), EVAL_SYSTEM, ev);
            fullTest.append(NL).append("- ").append(ev).append(NL);
            fullTest.append("  - ").append(direct.response).append(NL);
            if (direct.response.toUpperCase().contains("YES")) {
                score++;
            } else {
                score -= 2;
            }
            prog++;
            if (prog > prog10) {
                frame.postUpdate(prog + " evaluations done, " + (evaluations.size() - prog) + " to go.");
                prog10 += evaluations.size() * 0.1;
            }
        }
        StringBuilder endEval = new StringBuilder("# Model tested: ").append(frame.model.getText()).append(NL);
        endEval.append("## Number of test runs: ").append(frame.numRuns).append(NL);
        endEval.append("## Evaluated by model: ").append(frame.evalModel.getText()).append(NL);
        endEval.append("## Evaluation score = ").append(String.format("%.2f", 1.0 * score / frame.numRuns)).append(NL);
        eval = endEval.toString();
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
