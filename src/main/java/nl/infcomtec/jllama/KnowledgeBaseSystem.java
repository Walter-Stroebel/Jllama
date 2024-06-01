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
        public Long created, lastMod;
        public String[] keywords;
        public String[] related;

        // Constructor for creating a new KB from an OllamaClient session
        public KnowledgeBlock(OllamaClient client) throws Exception {
            this.created = this.lastMod = System.currentTimeMillis();
            this.title = client.execute(createTitle);
            this.content = client.execute(createKB);
            this.keywords = new String[]{};
            this.related = new String[]{};
        }

        // Constructor for loading a KB from a file
        public KnowledgeBlock(String filename) throws IOException {
            File f = new File(KBFolder, filename);
            KnowledgeBlock kb = mapper.readValue(
                    f,
                    KnowledgeBlock.class);
            this.created = this.lastMod = f.lastModified();
            if (null != kb.created) {
                this.created = kb.created;
            }
            if (null != kb.lastMod) {
                this.lastMod = kb.lastMod;
            }
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
            File f = new File(KBFolder, filename);
            lastMod = System.currentTimeMillis();
            mapper.writeValue(f, this);
            f.setLastModified(created);
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
