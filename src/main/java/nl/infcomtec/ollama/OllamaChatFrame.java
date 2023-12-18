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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import nl.infcomtec.simpleimage.ImageObject;
import nl.infcomtec.simpleimage.ImageViewer;
import nl.infcomtec.tools.PandocConverter;

/**
 * This is a FULL Ollama chat window. It allow to chat with any model available
 * at will.<p>
 * Note that this displays some extra information along with the actual chat
 * which allows one to get a good insight in how the model performs.</p>
 *
 * @author Walter Stroebel
 */
public class OllamaChatFrame {

    private final JFrame frame;
    private final JToolBar buttons;
    private final JTextArea chat;
    private final JTextArea input;
    private OllamaClient client;
    private final JComboBox<String> hosts;
    private final JComboBox<String> models;
    private JLabel curCtxSize;
    private JLabel outTokens;
    private JLabel inTokens;
    private final AtomicBoolean autoMode = new AtomicBoolean(false);
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private JLabel tokensSec;

    private JLabel modFamily;
    private JLabel modFamilies;
    private JLabel modFormat;
    private JLabel modParmSize;
    private JLabel modQuant;

    /**
     * Ties all the bits and pieces together into a GUI.
     */
    public OllamaChatFrame() {
        setupGUI(Ollama.config.fontSize);
        this.modQuant = new JLabel();
        this.modParmSize = new JLabel();
        this.modFormat = new JLabel();
        this.modFamilies = new JLabel();
        this.modFamily = new JLabel();
        this.models = new JComboBox<>();
        this.hosts = new JComboBox<>();
        this.buttons = new JToolBar();
        this.input = new JTextArea(4, 80);
        this.chat = new JTextArea();
        frame = new JFrame("Ollama chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container cont = frame.getContentPane();
        cont.setLayout(new BorderLayout());
        buttonBar();
        cont.add(buttons, BorderLayout.NORTH);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copy = new JMenuItem("Copy");
        copy.addActionListener(new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String selectedText = chat.getSelectedText();
                if (null != selectedText) {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(selectedText), null);
                }
            }
        });
        popupMenu.add(copy);
        chat.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        JScrollPane pane = new JScrollPane(chat);
        pane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 128), 5),
                "Chat"));
        cont.add(pane, BorderLayout.CENTER);
        Box bottom = Box.createHorizontalBox();
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

    private void buttonBar() {
        buttons.add(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        });
        buttons.add(new AbstractAction("Code") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                frame.dispose();
                new OllamaCoder();
            }
        });
        buttons.add(new JToolBar.Separator());
        String lsHost = Ollama.config.getLastEndpoint();
        for (String e : Ollama.getAvailableModels().keySet()) {
            addToHosts(e);
        }
        client = new OllamaClient(lsHost);
        models.removeAllItems();
        for (AvailableModels.AvailableModel am : Ollama.getAvailableModels().get(lsHost).models) {
            models.addItem(am.name);
        }
        if (null != Ollama.config.lastModel) {
            models.setSelectedItem(Ollama.config.lastModel);
        }
        models.invalidate();
        hosts.setSelectedItem(lsHost);
        hosts.addActionListener(new AddSelectHost());
        hosts.setEditable(true);
        buttons.add(new JLabel("Hosts:"));
        buttons.add(hosts);
        buttons.add(new JToolBar.Separator());
        buttons.add(new JLabel("Models:"));
        buttons.add(models);
        buttons.add(new JToolBar.Separator());
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
                        styleSheet.addRule("body { font-family: 'Arial'; font-size: " + Math.round(Ollama.config.fontSize) + "pt; }");
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
        buttons.add(new AbstractAction("Dall-E 3") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    String text = chat.getSelectedText();
                    DallEClient dale = new DallEClient();
                    ImageObject io = new ImageObject(dale.getImage(text));
                    ImageViewer iv = new ImageViewer(io);
                    iv.getScalePanFrame();
                } catch (Exception ex) {
                    Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        buttons.add(new JToolBar.Separator());
        buttons.add(new JCheckBox(new AbstractAction("Auto") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                autoMode.set(((JCheckBox) ae.getSource()).isSelected());
            }
        }));
    }

    private void addToHosts(String host) {
        for (int i = 0; i < hosts.getItemCount(); i++) {
            if (hosts.getItemAt(i).equalsIgnoreCase(host)) {
                return;
            }
        }
        hosts.addItem(host);
        if (1 == hosts.getItemCount()) {
            hosts.setSelectedItem(host);
        }
    }

    private JPanel sideBar() {
        JPanel ret = new JPanel();
        curCtxSize = new JLabel();
        outTokens = new JLabel();
        inTokens = new JLabel();
        tokensSec = new JLabel();
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
        ret.add(new JLabel("In tokens"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        ret.add(inTokens, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        ret.add(new JLabel("Out tokens"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        ret.add(outTokens, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        ret.add(new JLabel("Tokens/sec"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        ret.add(tokensSec, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        ret.add(new JLabel("Family"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        ret.add(modFamily, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        ret.add(new JLabel("Families"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 5;
        ret.add(modFamilies, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        ret.add(new JLabel("Format"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 6;
        ret.add(modFormat, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        ret.add(new JLabel("ParamSize"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 7;
        ret.add(modParmSize, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        ret.add(new JLabel("Quantization"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 8;
        ret.add(modQuant, gbc);

        ret.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 206, 80), 5),
                "Session"));
        ret.setPreferredSize(new Dimension(Ollama.config.w / 4, Ollama.config.h));
        return ret;
    }

    private void updateSideBar(Response resp) {
        OllamaClient.ModelSession session = client.getSession();
        modFamily.setText(Objects.toString(session.model.details.family));
        modFamilies.setText(Objects.toString(session.model.details.families));
        modFormat.setText(Objects.toString(session.model.details.format));
        modParmSize.setText(Objects.toString(session.model.details.parameterSize));
        modQuant.setText(Objects.toString(session.model.details.quantizationLevel));
        curCtxSize.setText(Integer.toString(resp.context.size()));
        outTokens.setText(Integer.toString(resp.evalCount));
        inTokens.setText(Integer.toString(resp.promptEvalCount));
        tokensSec.setText(String.format("%.2f", 1e9 * resp.evalCount / resp.evalDuration));
    }

    private void finishInit() {
        frame.setVisible(true);
        frame.setBounds(Ollama.config.x, Ollama.config.y, Ollama.config.w, Ollama.config.h);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                Ollama.config.update(frame.getBounds());
            }

            @Override
            public void componentResized(ComponentEvent e) {
                Ollama.config.update(frame.getBounds());
            }
        });
    }

    /**
     * Quick &amp; Dirty fix for large monitors.
     *
     * @param fontSize in font points
     */
    public final void setupGUI(float fontSize) {
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
            askModel("\n\n### Question\n\n", question);
        }

        private void askModel(final String source, final String question) {
            if (!question.isEmpty()) {
                chat.append(source + question);
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
                                            if (null != mod.getText()) {
                                                askModel("\n\n### Tooling\n\n", mod.getText());
                                            }
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
                            chat.setCaretPosition(chat.getDocument().getLength());
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

    private class AddSelectHost implements ActionListener {

        public AddSelectHost() {
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            String selHost = (String) hosts.getEditor().getItem();
            if (!selHost.isEmpty()) {
                addToHosts(selHost);
                int n = hosts.getItemCount();
                Ollama.config.ollamas = new String[n];
                for (int i = 0; i < n; i++) {
                    Ollama.config.ollamas[i] = hosts.getItemAt(i);
                }
                hosts.setSelectedItem(selHost);
                Ollama.config.update();
                String fmod = null;
                for (Map.Entry<String, AvailableModels> e : Ollama.fetchAvailableModels().entrySet()) {
                    addToHosts(e.getKey());
                    if (e.getKey().equals(selHost)) {
                        models.removeAllItems();
                        for (AvailableModels.AvailableModel am : e.getValue().models) {
                            models.addItem(am.name);
                            if (null == fmod) {
                                fmod = am.name;
                            }
                        }
                        models.setSelectedItem(fmod);
                    }
                }
                models.invalidate();
                client = new OllamaClient(selHost);
            }
        }
    }

}
