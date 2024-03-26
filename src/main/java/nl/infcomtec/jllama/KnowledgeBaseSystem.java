package nl.infcomtec.jllama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

public class KnowledgeBaseSystem {

    public static File KBFolder = new File("path/to/your/git/folder");
    public static ObjectMapper mapper = Ollama.getMapper();
    public static String createTitle = "Create a short title for this session.";
    public static String createKB = "Do you best to output NEW knowledge from this session for future sessions "
            + "in a form that makes sense for YOU (this LLM and its current foundation model).\n"
            + "Think of this as stripping everything from your current session based knowledge "
            + "that IS already in the foundation model you have, leaving a terse but \"connected\" "
            + "block of information that will gift your current knowledge to a new invocation of the this LLM.\n"
            + "The intent is to create \"text based\" embedding of your current context buffer or a \"Knowledge Block\".\n";

    public static class KnowledgeBlock {

        public String title;
        public String content;
        public String[] keywords;
        public String[] related;

        // Constructor for creating a new KB from an OllamaClient session
        public KnowledgeBlock(OllamaClient client) throws Exception {
            this.title = client.execute(createTitle);
            this.content = client.execute(createKB);
            this.keywords = new String[]{};
            this.related = new String[]{};
        }

        // Constructor for loading a KB from a file
        public KnowledgeBlock(String filename) throws IOException {
            KnowledgeBlock kb = mapper.readValue(new File(KBFolder, filename), KnowledgeBlock.class);
            this.title = kb.title;
            this.content = kb.content;
            this.keywords = kb.keywords;
            this.related = kb.related;
        }

        // Method to save the KB to a file
        public void save(String filename) throws IOException {
            mapper.writeValue(new File(KBFolder, filename), this);
        }
    }

    public static void createAndShowKBFrame(final JFrame parentFrame, final OllamaClient client) {
        SwingWorker<KnowledgeBlockFrame, Void> worker = new SwingWorker<KnowledgeBlockFrame, Void>() {

            @Override
            protected KnowledgeBlockFrame doInBackground() throws Exception {
                KnowledgeBlock kb = new KnowledgeBlock(client);
                return new KnowledgeBlockFrame(parentFrame, kb);
            }

            @Override
            protected void done() {
                try {
                    get().setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(KnowledgeBaseSystem.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        worker.execute();
    }

}
