package nl.infcomtec.jllama;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

/**
 * Debate a question.
 *
 * This class facilitates a debate on a given question, involving a protagonist,
 * an opponent, and a judge. The debate is conducted over multiple rounds, where
 * the protagonist and opponent take turns presenting their points. After the
 * debate, the judge provides a conclusion or judgment based on the points
 * presented by both sides.
 *
 * @author walter
 */
public class Debate {

    /**
     * The model used for the protagonist.
     */
    public String protagonistModel;

    /**
     * The model used for the opponent.
     */
    public String opponentModel;

    /**
     * The endpoint where the protagonist model is hosted.
     */
    public String protagonistEndpoint;

    /**
     * The endpoint where the opponent model is hosted.
     */
    public String opponentEndpoint;

    /**
     * The model used for the judge.
     */
    public String judgeModel;

    /**
     * The endpoint where the judge model is hosted.
     */
    public String judgeEndpoint;

    /**
     * The question or topic to be debated.
     */
    public String question;

    /**
     * An array to store the points made by the protagonist in each round.
     */
    public String protagonistPoints[];

    /**
     * An array to store the points made by the opponent in each round.
     */
    public String opponentPoints[];

    /**
     * The conclusion or judgment provided by the judge.
     */
    public String conclusion;

    /**
     * Creates a Debate instance from a JSON file.
     *
     * @param json The JSON file containing the Debate data.
     * @return A Debate instance populated with data from the JSON file.
     * @throws IOException If there is an error reading the JSON file.
     */
    public static Debate fromFile(File json) throws IOException {
        return Ollama.getMapper().readValue(json, Debate.class);
    }

    /**
     * Sets up a debate using the same model for all participants (protagonist,
     * opponent, and judge).
     *
     * @param question The question or subject to debate.
     * @param _rounds The number of rounds for the debate, between 1 and 3,
     * depending on the model's context length.
     * @return An instantiated Debate object, ready to run the debate() method.
     */
    public static Debate mono(String question, int _rounds) {
        int rounds = Math.min(_rounds, 3);
        rounds = Math.max(rounds, 1);
        TreeMap<String, AvailableModels> models = Ollama.getAvailableModels();
        String ep = models.firstEntry().getKey();
        String md = models.firstEntry().getValue().models[0].name;
        Debate ret = new Debate();
        ret.conclusion = "";
        ret.judgeEndpoint = ep;
        ret.judgeModel = md;
        ret.opponentEndpoint = ep;
        ret.opponentModel = md;
        ret.opponentPoints = new String[rounds];
        ret.protagonistEndpoint = ep;
        ret.protagonistModel = md;
        ret.protagonistPoints = new String[rounds];
        ret.question = question;
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Debate:\n");
        sb.append("protagonistModel=").append(protagonistModel);
        sb.append("\nopponentModel=").append(opponentModel);
        sb.append("\njudgeModel=").append(judgeModel);
        sb.append("\nquestion=").append(question);
        for (int i = 0; i < protagonistPoints.length; i++) {
            sb.append("\nPro round ").append(i + 1).append(": ").append(protagonistPoints[i]);
            sb.append("\nCon round ").append(i + 1).append(": ").append(opponentPoints[i]);
        }
        sb.append("\nconclusion=").append(conclusion);
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Writes the Debate instance to a JSON file.
     *
     * @param json The JSON file to write the Debate data to.
     * @throws IOException If there is an error writing to the JSON file.
     */
    public void toFile(File json) throws IOException {
        Ollama.getMapper().writeValue(json, this);
    }

    /**
     * Conducts the full debate process by: 1. Initializing the protagonist,
     * opponent, and judge clients. 2. Conducting the debate rounds, where the
     * protagonist and opponent take turns presenting their points. 3. Obtaining
     * the judge's conclusion or judgment based on the points presented by both
     * sides.
     *
     * @throws Exception If there is an error during the debate process.
     */
    public void debate() throws Exception {
        int rounds = protagonistPoints.length;
        OllamaClient pro = new OllamaClient(protagonistEndpoint);
        OllamaClient con = new OllamaClient(opponentEndpoint);
        OllamaClient jud = new OllamaClient(judgeEndpoint);
        StringBuilder proQ = new StringBuilder("You are participating in a debate.\n");
        proQ.append("The debate will be over ").append(rounds).append(" round");
        if (1 != rounds) {
            proQ.append("s");
        }
        proQ.append(".\n");
        proQ.append("The subject is: ").append(question).append("\n");
        // this part is the same
        StringBuilder conQ = new StringBuilder(proQ.toString());
        proQ.append("You are the protagonist.");
        proQ.append("Make your first point(s).\n");
        protagonistPoints[0] = pro.askAndAnswer(protagonistModel, proQ.toString()).response;
        conQ.append("You are the opponent.");
        conQ.append("The first point(s) of the protagonist are:\n");
        conQ.append(protagonistPoints[0]);
        conQ.append("Give your first opposing point(s).\n");
        opponentPoints[0] = con.askAndAnswer(opponentModel, conQ.toString()).response;

        // Conducting remaining rounds
        for (int i = 1; i < rounds; i++) {
            StringBuilder proRoundQ = new StringBuilder("Round " + (i + 1) + " of the debate.\n");
            proRoundQ.append("The opponent's previous point was:\n");
            proRoundQ.append(opponentPoints[i - 1]);
            proRoundQ.append("\nMake your next point.\n");

            protagonistPoints[i] = pro.askAndAnswer(protagonistModel, proRoundQ.toString()).response;

            StringBuilder conRoundQ = new StringBuilder("Round " + (i + 1) + " of the debate.\n");
            conRoundQ.append("The protagonist's point was:\n");
            conRoundQ.append(protagonistPoints[i]);
            conRoundQ.append("\nGive your counterpoint.\n");

            opponentPoints[i] = con.askAndAnswer(opponentModel, conRoundQ.toString()).response;
        }

        // Conclusion by the judge
        StringBuilder judgeQ = new StringBuilder("You are the arbitor in a debate.\n");
        judgeQ.append("The debate is over. Here are the points:\n");
        for (int i = 0; i < rounds; i++) {
            judgeQ.append("Protagonist points in round ").append(i + 1).append(":\n");
            judgeQ.append(protagonistPoints[i]).append("\n");
            judgeQ.append("\nOpponent points in round ").append(i + 1).append(":\n");
            judgeQ.append(opponentPoints[i]).append("\n");
        }
        judgeQ.append("\nPlease provide your judgment.\n");

        conclusion = jud.askAndAnswer(judgeModel, judgeQ.toString()).response;
    }
}
