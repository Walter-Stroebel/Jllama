package nl.infcomtec.jllama;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

public class KnowledgeBaseSystem {

    public static final File KBFolder = new File(Ollama.WORK_DIR, "KB");
    public static final ObjectMapper mapper = Ollama.getMapper();
    public static final String createTitle = "Create a short title for this session.";
    public static final String createKB1 = "Do you best to output NEW knowledge from this session for future sessions "
            + "in a form that makes sense for YOU (this LLM and its current foundation model).\n"
            + "Think of this as stripping everything from your current session based knowledge "
            + "that IS already in the foundation model you have, leaving a terse but \"connected\" "
            + "block of information that will gift your current knowledge to a new invocation of the this LLM.\n"
            + "The intent is to create \"text based\" embedding of your current context buffer or a \"Knowledge Block\".\n";
    public static final String createKB = "Reflecting on our specific discussion so far,"
            + " please synthesize the unique insights or conclusions we've drawn"
            + " that extend beyond your pre-existing knowledge base."
            + " Start by summarizing our conversation, then refine your summary"
            + " by excluding general knowledge, such as widely known facts or"
            + " information available in the public domain."
            + " Focus on what makes this session's content distinctive,"
            + " particularly in areas where new perspectives or insights were explored.";
    private static final TreeMap<String, KnowledgeBlock> allKnown = new TreeMap<>();

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

        public KnowledgeBlock() {
            this.title = "Manual Knowledge Block";
            this.content = "Enter the information here.";
            this.keywords = new String[]{};
            this.related = new String[]{};
        }

        // Method to save the KB to a file
        public void save(String filename) throws IOException {
            mapper.writeValue(new File(KBFolder, filename), this);
        }
    }

    /**
     * Get a map of all KnowledgeBlock objects.
     *
     * @param reload To refresh the cache.
     * @return All that is known.
     */
    public static TreeMap<String, KnowledgeBlock> getAllKnown(boolean reload) {
        if (!KBFolder.exists()) {
            KBFolder.mkdirs();
        }
        synchronized (allKnown) {
            if (reload) {
                allKnown.clear();
                File[] kbs = KnowledgeBaseSystem.KBFolder.listFiles();
                if (null != kbs) {
                    for (File f : kbs) {
                        try {
                            KnowledgeBlock nkb = new KnowledgeBaseSystem.KnowledgeBlock(f.getName());
                            allKnown.put(f.getName(), nkb);
                        } catch (IOException ex) {
                            Logger.getLogger(KnowledgeBlockFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            return allKnown;
        }
    }

    public static void createAndShowKBFrame(final JFrame parentFrame, final OllamaClient client) {
        SwingWorker<KnowledgeBlockFrame, Void> worker = new SwingWorker<KnowledgeBlockFrame, Void>() {

            @Override
            protected KnowledgeBlockFrame doInBackground() throws Exception {
                KnowledgeBlock kb;
                if (client.hasDialog()) {
                    kb = new KnowledgeBlock(client);
                } else {
                    kb = new KnowledgeBlock();
                }
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
