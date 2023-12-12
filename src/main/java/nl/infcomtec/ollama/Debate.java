package nl.infcomtec.ollama;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

/**
 * Debate a question.
 *
 * @author walter
 */
public class Debate {

    public String protagonistModel;
    public String opponentModel;
    public String protagonistEndpoint;
    public String opponentEndpoint;
    public String judgeModel;
    public String judgeEndpoint;
    public String question;
    public String protagonistPoints[];
    public String opponentPoints[];
    public String conclusion;

    public static Debate fromFile(File json) throws IOException {
        return Ollama.getMapper().readValue(json, Debate.class);
    }

    /**
     * Set up a debate using the same model for all participants.
     *
     * @param question The question/subject to debate.
     * @param _rounds number of rounds, between 1 and 3, depends on num_ctx how
     * far you can take this.
     * @return instantiated Debate, ready to run debate() on.
     */
    public static Debate mono(String question, int _rounds) {
        int rounds = Math.min(_rounds, 3);
        rounds = Math.max(rounds, 1);
        TreeMap<String, AvailableModels> models = Ollama.fetchAvailableModels();
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
        return ret;
    }

    public void toFile(File json) throws IOException {
        Ollama.getMapper().writeValue(json, this);
    }

    /**
     * Create and fill in an instance of this object and call this to run the
     * full debate.
     *
     * @throws Exception
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
