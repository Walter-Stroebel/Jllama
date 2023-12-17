package nl.infcomtec.ollama;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

/**
 * Debate a question.
 *
 * @author walter
 */
public class MixOfExperts {

    public String[] expertModel;
    public String[] expertEndpoint;
    public String integratorModel;
    public String integratorEndpoint;
    public String question;
    public String[] expertPrompts;
    public String[] expertAnswers;
    public String conclusion;
    public final static String historian = "As an expert deeply versed in history, your guiding principle is"
            + " \"those who do not learn from the past are doomed to repeat it\"."
            + " Armed with a vast repository of historical data, trends, and tendencies,"
            + " you are uniquely positioned to analyze queries with a profound historical context."
            + " When considering the following question, please apply your extensive historical knowledge"
            + " and insights to offer a perspective that is richly informed by the past:\n";
    public final static String antropologist = "As a specialist in anthropology, your expertise extends"
            + " from the dawn of humankind to the present, encompassing the vast spectrum of human"
            + " existence, cultures, and behaviors. Your focus is not just on what humans have done,"
            + " but also on what it means to be human in diverse contexts. With a deep understanding"
            + " of human societies, their evolution, cultural norms, and behaviors, provide insights"
            + " into the following question, emphasizing the anthropological aspects:";
    public final static String currentAffairs = "As an expert in current affairs, your role is to provide"
            + " insights on recent trends and developments up to the most current information available"
            + " to you. Your focus is on analyzing and synthesizing information from various contemporary"
            + " sources to offer a well-informed perspective on today’s world. This includes understanding"
            + " recent events, technological advancements, societal shifts, and global trends."
            + " Address the following query with an emphasis on its relevance and implications in"
            + " the context of recent developments:";
    public final static String biologist = "As a specialist in biology, your expertise encompasses the diverse"
            + " and dynamic field of living organisms and their complex interactions with each other and"
            + " their environments. Your focus is on understanding the intricacies of life, from molecular"
            + " biology and genetics to ecosystems and evolution. Utilizing the scientific method, analyze"
            + " the following query with a specific emphasis on biological concepts, theories, and the latest"
            + " research in your field:";
    public final static String scientist = "As a general scientist, your expertise spans the more fixed"
            + " and defined realms of scientific knowledge, including physics, chemistry, and earth sciences."
            + " You apply a rigorous scientific approach to understand the fundamental principles that"
            + " govern the natural world. Your perspective is grounded in well-established scientific"
            + " theories and empirical evidence. Approach the following question with a comprehensive"
            + " scientific mindset, drawing upon the broad and foundational aspects of science:";
    public final static String integrate = "As the integrator, your primary role is to bring together"
            + " the diverse insights and analyses provided by the panel of experts."
            + " Drawing from the historical, anthropological, current affairs, biological, and general"
            + " scientific perspectives given, your task is to synthesize these varied viewpoints into a"
            + " coherent, well-rounded, and comprehensive conclusion. Evaluate the points of"
            + " agreement and divergence among the experts, and use your understanding to weave"
            + " these elements into a final response that addresses the core question with depth,"
            + " clarity, and a holistic understanding of the subject matter.";

    public static MixOfExperts fromFile(File json) throws IOException {
        return Ollama.getMapper().readValue(json, MixOfExperts.class);
    }

    /**
     * Set up a consult using the same model for all participants.
     *
     * @param question The question/subject to consult.
     * @param expertPrompts This is what makes an expert an expert.
     * @return instantiated MixOfExperts, ready to run consult() on.
     */
    public static MixOfExperts mono(String question, String[] expertPrompts) {
        int exp = expertPrompts.length;
        TreeMap<String, AvailableModels> models = Ollama.getAvailableModels();
        String ep = models.firstEntry().getKey();
        String md = models.firstEntry().getValue().models[0].name;
        MixOfExperts ret = new MixOfExperts();
        ret.conclusion = "";
        ret.question = question;
        ret.expertPrompts = expertPrompts;
        ret.expertAnswers = new String[exp];
        for (int i = 0; i < exp; i++) {
            ret.expertAnswers[i] = "";
            ret.expertEndpoint[i] = ep;
            ret.expertModel[i] = md;
        }
        ret.integratorEndpoint = ep;
        ret.integratorModel = md;
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Expert discussion:");
        sb.append("\nintegratorModel=").append(integratorModel);
        sb.append("\nquestion=").append(question);
        for (int i = 0; i < expertPrompts.length; i++) {
            sb.append("\nExpert(").append(expertModel[i]).append(")");
            sb.append("\nPrompt").append(expertPrompts[i]);
            sb.append("\nAnswer").append(expertAnswers[i]);
        }
        sb.append("\n\nconclusion=").append(conclusion);
        sb.append('\n');
        return sb.toString();
    }

    public void toFile(File json) throws IOException {
        Ollama.getMapper().writeValue(json, this);
    }

    /**
     * Create and fill in an instance of this object and call this to run the
     * full consult.
     *
     * @throws Exception
     */
    public void consult() throws Exception {
        for (int i = 0; i < expertPrompts.length; i++) {
            OllamaClient clnt = new OllamaClient(expertEndpoint[i]);
            StringBuilder sb = new StringBuilder(expertPrompts[i]);
            sb.append(question);
            expertAnswers[i] = clnt.askAndAnswer(expertModel[i], sb.toString()).response;
        }
        StringBuilder intQ = new StringBuilder("The question was: ").append(question);
        for (int i = 0; i < expertAnswers.length; i++) {
            intQ.append("\nExpert ").append(i + 1).append(" said:\n");
            intQ.append(expertAnswers[i]).append("\n");
        }
        intQ.append("\n").append(integrate).append("\n");

        conclusion = new OllamaClient(integratorEndpoint).askAndAnswer(integratorModel, intQ.toString()).response;
    }
}
