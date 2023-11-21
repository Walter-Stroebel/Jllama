package nl.infcomtec.ollama;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import nl.infcomtec.tools.PandocConverter;

/**
 * This is a FULL Ollama chat window. It allow to chat with any model available
 * at will.<p>
 * Note that this displays a LOT of extra information along with the actual chat
 * which allows one to get a good insight in how the model performs.</p>
 *
 * @author Walter Stroebel
 */
public class OllamaChatFrame {

    private final JFrame frame;
    private final JToolBar buttons;
    private final JTextArea chat;
    private final JTextArea input;
    private final OllamaClient client;
    private final JComboBox<String> models;
    private ChatConfig config;
    private static final File configFile = new File(Ollama.WORK_DIR, "chatcfg.json");
    private JLabel curCtxSize;
    private JLabel createdAt;
    private JLabel outTokens;
    private JLabel inTokens;
    private final AtomicBoolean autoMode = new AtomicBoolean(false);
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public OllamaChatFrame() {
        client = new OllamaClient();
        try {
            if (configFile.exists()) {
                config = Ollama.getMapper().readValue(configFile, ChatConfig.class);
            }
        } catch (Exception any) {
            config = null;
        }
        if (null == config) {
            config = new ChatConfig();
            config.x = config.y = 0;
            config.w = 1000;
            config.h = 700;
            config.fontSize = 18;
            try {
                Ollama.getMapper().writeValue(configFile, config);
            } catch (IOException ex) {
                Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        setupGUI(config.fontSize);
        frame = new JFrame("Ollama chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container cont = frame.getContentPane();
        cont.setLayout(new BorderLayout());
        buttons = new JToolBar();
        buttons.add(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });
        models = new JComboBox<>();
        String fm = null;
        for (String mn : Ollama.getAvailableModels()) {
            models.addItem(mn);
            if (null == fm) {
                fm = mn;
            }
        }
        models.setSelectedItem(fm);
        buttons.add(models);
        buttons.add(new AbstractAction("HTML") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String markDown = chat.getText();
                JFrame html = new JFrame("HTML");
                html.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                html.getContentPane().setLayout(new BorderLayout());
                JEditorPane pane = new JEditorPane();
                pane.setContentType("text/html");
                HTMLEditorKit kit = new HTMLEditorKit() {
                    @Override
                    public StyleSheet getStyleSheet() {
                        StyleSheet styleSheet = super.getStyleSheet();
                        styleSheet.addRule("body { font-family: 'Arial'; font-size: " + Math.round(config.fontSize) + "pt; }");
                        return styleSheet;
                    }
                };
                pane.setEditorKit(kit);
                pane.setText(new PandocConverter().convertMarkdownToHTML(markDown));
                html.getContentPane().add(new JScrollPane(pane), BorderLayout.CENTER);
                html.pack();
                html.setVisible(true);
            }
        });
        buttons.add(new JCheckBox(new AbstractAction("Auto") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                autoMode.set(((JCheckBox) ae.getSource()).isSelected());
            }
        }));
        cont.add(buttons, BorderLayout.NORTH);
        chat = new JTextArea();
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        JScrollPane pane = new JScrollPane(chat);
        pane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 128), 5),
                "Chat"));
        cont.add(pane, BorderLayout.CENTER);
        Box bottom = Box.createHorizontalBox();

        input = new JTextArea(4, 80);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        bottom.add(new JScrollPane(input));
        bottom.add(Box.createHorizontalStrut(10));
        bottom.add(new JButton(new Interact()));

        bottom.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(135, 206, 250), 5),
                "Input"));
        cont.add(bottom, BorderLayout.SOUTH);
        cont.add(sideBar(), BorderLayout.EAST);
        frame.pack();
        if (EventQueue.isDispatchThread()) {
            finishInit();
        } else {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        finishInit();
                    }
                });
            } catch (Exception ex) {
                Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        }
    }

    private JPanel sideBar() {
        JPanel ret = new JPanel();
        curCtxSize = new JLabel();
        createdAt = new JLabel();
        outTokens = new JLabel();
        inTokens = new JLabel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        ret.setLayout(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        ret.add(new JLabel("Context"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        ret.add(curCtxSize, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        ret.add(new JLabel("Created at"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        ret.add(createdAt, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        ret.add(new JLabel("In tokens"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        ret.add(inTokens, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        ret.add(new JLabel("Out tokens"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        ret.add(outTokens, gbc);
        ret.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 206, 80), 5),
                "Session"));
        ret.setPreferredSize(new Dimension(config.w / 4, config.h));
        return ret;
    }

    private void updateSideBar(Response resp) {
        curCtxSize.setText(Integer.toString(resp.context.size()));
        createdAt.setText(resp.createdAt.toString());
        outTokens.setText(Integer.toString(resp.evalCount));
        inTokens.setText(Integer.toString(resp.promptEvalCount));
    }

    private void finishInit() {
        frame.setVisible(true);
        frame.setBounds(config.x, config.y, config.w, config.h);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                config.update(frame.getBounds());
            }

            @Override
            public void componentResized(ComponentEvent e) {
                config.update(frame.getBounds());
            }
        });
    }

    /**
     * Quick &amp; Dirty fix for large monitors.
     */
    public static void setupGUI(float fontSize) {
        Font defaultFont = UIManager.getFont("Label.font");
        Font useFont = defaultFont.deriveFont(fontSize);
        Set<Map.Entry<Object, Object>> entries = new HashSet<>(UIManager.getLookAndFeelDefaults().entrySet());
        for (Map.Entry<Object, Object> entry : entries) {
            if (entry.getKey().toString().endsWith(".font")) {
                UIManager.put(entry.getKey(), useFont);
            }
        }
    }

    class Interact extends AbstractAction {

        public Interact() {
            super("Send");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            final String question = input.getText().trim();
            input.setText("");
            if (!question.isEmpty()) {
                chat.append("\n\n### Question\n\n" + question);
                SwingWorker<Response, StreamedResponse> sw = new SwingWorker<Response, StreamedResponse>() {

                    OllamaClient.StreamListener listener = new OllamaClient.StreamListener() {
                        List<StreamedResponse> chunks = new LinkedList<>();

                        @Override
                        public boolean onResponseReceived(StreamedResponse responsePart) {
                            chunks.add(responsePart);
                            process(chunks);
                            return true;
                        }
                    };

                    @Override
                    protected void done() {
                        try {
                            Response resp = get();
                            updateSideBar(resp);
                            if (autoMode.get()) {
                                List<Modality> mods = Ollama.handleOutput(pool, resp.response);
                                if (!mods.isEmpty()) {
                                    for (Modality mod : mods) {
                                        if (mod.isGraphical) {
                                            new TextImage(pool, mod.getClass().getSimpleName(), mod);
                                        } else {
                                            System.out.println(mod.getText());
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    @Override
                    protected void process(List<StreamedResponse> chunks) {
                        for (StreamedResponse sr : chunks) {
                            chat.append(sr.response);
                        }
                        chunks.clear();
                    }

                    @Override
                    protected Response doInBackground() throws Exception {
                        Response resp = client.askWithStream((String) models.getSelectedItem(), question, listener);
                        return resp;
                    }
                };
                chat.append("\n\n### Answer\n\n");
                sw.execute();
            }
        }
    }

    public static class ChatConfig {

        public int x, y, w, h; // of the chat window
        public float fontSize; // to keep things readable

        public void update(Rectangle bounds) {
            x = bounds.x;
            y = bounds.y;
            w = bounds.width;
            h = bounds.height;
            try {
                Ollama.getMapper().writeValue(configFile, this);
            } catch (IOException ex) {
                Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
