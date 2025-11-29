package nl.infcomtec.jllama;

import com.formdev.flatlaf.FlatDarculaLaf;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import nl.infcomtec.simpleimage.ImageObject;
import nl.infcomtec.simpleimage.ImageViewer;
import nl.infcomtec.tools.PandocConverter;

/**
 * This is a FULL Ollama chat window. It allows you to chat with any available
 * model.
 * <p>
 * Note that this displays some extra information along with the actual chat,
 * which allows you to gain insight into how the model performs.
 * </p>
 *
 * @author Walter Stroebel
 */
public class OllamaChatFrame {

    /**
     * The main frame of the application.
     */
    private final JFrame frame;

    /**
     * The toolbar containing buttons and other UI elements.
     */
    private final JToolBar buttons;

    /**
     * The text area displaying the chat conversation.
     */
    private final JTextArea chat;

    /**
     * The text area for user input.
     */
    private final JTextArea input;

    /**
     * The client used for communicating with the Ollama service.
     */
    private OllamaClient client;

    /**
     * The combo box for selecting the host.
     */
    private final JComboBox<String> hosts;

    /**
     * The combo box for selecting the model.
     */
    private final JComboBox<String> models;

    /**
     * A label displaying the current interactions size.
     */
    private JLabel curCtxSize;

    /**
     * A label displaying the number of output tokens.
     */
    private JLabel outTokens;

    /**
     * A label displaying the number of input tokens.
     */
    private JLabel inTokens;

    /**
     * A flag indicating whether the application is in auto mode.
     */
    private final AtomicBoolean autoMode = new AtomicBoolean(true);

    /**
     * A flag indicating whether Enter sends the current input content.
     */
    private final AtomicBoolean autoSend = new AtomicBoolean(false);

    /**
     * An executor service for running background tasks.
     */
    private final ExecutorService pool = Executors.newCachedThreadPool();

    /**
     * A label displaying the number of tokens per second.
     */
    private JLabel tokensSec;

    /**
     * A label displaying the reason the model is done.
     */
    private JLabel doneReason;

    /**
     * A label displaying the model family.
     */
    private JLabel modFamily;

    /**
     * A label displaying the model families.
     */
    private JLabel modFamilies;

    /**
     * A label displaying the model format.
     */
    private JLabel modFormat;

    /**
     * A label displaying the model parameter size.
     */
    private JLabel modParmSize;

    /**
     * A label displaying the model quantization level.
     */
    private JLabel modQuant;

    /**
     * A label displaying the parent model.
     */
    private JLabel modParMod;

    /**
     * A label displaying the previously uploaded image.
     */
    private JLabel prevImage;

    /**
     * The previously uploaded image.
     */
    private final AtomicReference<BufferedImage> uplImage = new AtomicReference<>();
    /**
     * Last directory used to upload image from.
     */
    private File lastImageDir = null;

    /**
     * Ties all the bits and pieces together into a GUI.
     */
    public OllamaChatFrame() {
        FlatDarculaLaf.setup();
        this.modQuant = new JLabel();
        this.modParMod = new JLabel();
        this.modParmSize = new JLabel();
        this.modFormat = new JLabel();
        this.modFamilies = new JLabel();
        this.modFamily = new JLabel();
        this.prevImage = new JLabel();
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
        createMenuBar();
        cont.add(buttons, BorderLayout.NORTH);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copy = new JMenuItem(new AbstractAction("Copy") {
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
        final JButton send = new JButton(new Interact());
        bottom.add(send);
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (autoSend.get() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    send.doClick();
                }
            }
        });
        bottom.add(new JButton(new AbstractAction("\u2191 image") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fileChooser = new JFileChooser();
                if (null != lastImageDir) {
                    fileChooser.setCurrentDirectory(lastImageDir);
                }
                int returnValue = fileChooser.showOpenDialog(frame);
                uplImage.set(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    lastImageDir = selectedFile.getParentFile();
                    try {
                        BufferedImage originalImage = ImageIO.read(selectedFile);

                        if (originalImage.getHeight() != 256 || originalImage.getWidth() != 256) {
                            Image scaledInstance = originalImage.getScaledInstance(256, 256, BufferedImage.SCALE_SMOOTH);
                            BufferedImage resizedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g2d = resizedImage.createGraphics();
                            g2d.drawImage(scaledInstance, 0, 0, null);
                            g2d.dispose();
                            uplImage.set(resizedImage);
                        } else {
                            uplImage.set(originalImage);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
                        uplImage.set(null);
                    }
                }
                updateSideBar(null);
            }
        }));
        bottom.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(135, 206, 250), 5),
                "Input"));
        cont.add(bottom, BorderLayout.SOUTH);
        cont.add(createSidePanel(), BorderLayout.EAST);
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

    /**
     * Creates the toolbar buttons and initializes the hosts and models combo
     * boxes.
     */
    private void buttonBar() {
        String lsHost = Ollama.config.getLastEndpoint();
        for (String e : Ollama.getAvailableModels().keySet()) {
            addToHosts(e);
            if (null == lsHost) {
                lsHost = e;
            }
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
        hosts.addActionListener(new AddSelectHostListener());
        hosts.setEditable(true);
        buttons.add(new JLabel("Hosts:"));
        buttons.add(hosts);
        buttons.add(new JToolBar.Separator());
        buttons.add(new JLabel("Models:"));
        buttons.add(models);
        buttons.add(new JToolBar.Separator());
    }

    /**
     * Creates the application menu.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(new AbstractAction("Save chat (selection)...") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fileChooser = new JFileChooser();

                int returnValue = fileChooser.showSaveDialog(frame);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try (PrintWriter out = new PrintWriter(selectedFile)) {
                        String txt = chat.getSelectedText();
                        if (null == txt) {
                            txt = chat.getText();
                        }
                        out.print(txt);
                        if (!txt.endsWith(System.lineSeparator())) {
                            out.print(System.lineSeparator());
                        }
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(frame,
                                "Error while writing to file: " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }));
        fileMenu.add(new JMenuItem(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                System.exit(0);
            }
        }));
        menuBar.add(fileMenu);
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem(new AbstractAction("Clear chat") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                chat.setText("");
                client.clear();
                uplImage.set(null);
                updateSideBar(null);
            }
        }));
        editMenu.add(new JMenuItem(new AbstractAction("Copy chat (selection)") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String selectedText = chat.getSelectedText();
                if (null == selectedText) {
                    selectedText = chat.getText();
                }
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(selectedText), null);
            }
        }));
        menuBar.add(editMenu);
        JMenu actionMenu = new JMenu("Actions");
        actionMenu.add(new JMenuItem(new AbstractAction("Show chat as HTML") {
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
        }));
        {
            final String key = Ollama.config.openAIKey;
            if (null != key) {
                actionMenu.add(new JMenuItem(new AbstractAction("Send selected chat text to Dall-E 3") {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            String text = chat.getSelectedText();
                            if (!text.isBlank()) {
                                DallEClient dale = new DallEClient(key);
                                ImageObject io = new ImageObject(dale.getImage(text));
                                ImageViewer iv = new ImageViewer(io);
                                iv.getScalePanFrame();
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(OllamaChatFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }));
            }
        }
        {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new AbstractAction("Process chat output (function calling)") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    autoMode.set(((JCheckBoxMenuItem) ae.getSource()).isSelected());
                }
            });
            item.setSelected(true);
            actionMenu.add(item);
        }

        actionMenu.add(new JCheckBoxMenuItem(new AbstractAction("Auto Send on Enter") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                autoSend.set(((JCheckBoxMenuItem) ae.getSource()).isSelected());
            }
        }));
        actionMenu.add(new JCheckBoxMenuItem(new AbstractAction("Monitor the interactions") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Ollama.deregisterMonitor(ChatFrameMonitor.NAME);
                if (((JCheckBoxMenuItem) ae.getSource()).isSelected()) {
                    Ollama.registerMonitor(new ChatFrameMonitor());
                }
            }
        }));
        actionMenu.add(new JMenuItem(new AbstractAction("Knowledge Base System") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                KnowledgeBaseSystem.createAndShowKBFrame(frame, client);
            }
        }));
        actionMenu.add(new JMenuItem(new AbstractAction("Model testing (grade school reading test)") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                new ModelTester();
            }
        }));
        menuBar.add(actionMenu);

        frame.setJMenuBar(menuBar);
    }

    /**
     * Adds a host to the hosts combo box if it doesn't already exist.
     *
     * @param host The host to add.
     */
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

    /**
     * Creates a panel containing various labels for displaying model
     * information.
     *
     * @return The panel containing the model information labels.
     */
    private JPanel createSidePanel() {
        JPanel ret = new JPanel();
        curCtxSize = new JLabel();
        outTokens = new JLabel();
        inTokens = new JLabel();
        tokensSec = new JLabel();
        doneReason = new JLabel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        ret.setLayout(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        ret.add(new JLabel("Context"), gbc);
        gbc.gridx = 1;
        ret.add(curCtxSize, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        ret.add(new JLabel("In tokens"), gbc);
        gbc.gridx = 1;
        ret.add(inTokens, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        ret.add(new JLabel("Out tokens"), gbc);
        gbc.gridx = 1;
        ret.add(outTokens, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        ret.add(new JLabel("Tokens/sec"), gbc);
        gbc.gridx = 1;
        ret.add(tokensSec, gbc);
        gbc.gridx = 0;
        gbc.gridy = 4;
        ret.add(new JLabel("Done reason"), gbc);
        gbc.gridx = 1;
        ret.add(doneReason, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        ret.add(new JLabel("Family"), gbc);
        gbc.gridx = 1;
        ret.add(modFamily, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        ret.add(new JLabel("Families"), gbc);
        gbc.gridx = 1;
        ret.add(modFamilies, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        ret.add(new JLabel("Format"), gbc);
        gbc.gridx = 1;
        ret.add(modFormat, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        ret.add(new JLabel("ParamSize"), gbc);
        gbc.gridx = 1;
        ret.add(modParmSize, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        ret.add(new JLabel("Quantization"), gbc);
        gbc.gridx = 1;
        ret.add(modQuant, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10;
        ret.add(new JLabel("ParentModel"), gbc);
        gbc.gridx = 1;
        ret.add(modParMod, gbc);

        gbc.gridx = 0;
        gbc.gridy = 11;
        ret.add(new JLabel("Image"), gbc);
        gbc.gridx = 1;
        ret.add(prevImage, gbc);

        ret.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 206, 80), 5),
                "Session"));
        ret.setPreferredSize(new Dimension(Ollama.config.w / 4, Ollama.config.h));
        return ret;
    }

    /**
     * Updates the sidebar with information from the given response.
     *
     * @param resp The response object containing the information to display.
     */
    private void updateSideBar(Response resp) {
        modFamily.setText("");
        modFamilies.setText("");
        modFormat.setText("");
        modParmSize.setText("");
        modQuant.setText("");
        modParMod.setText("");
        curCtxSize.setText("");
        outTokens.setText("");
        inTokens.setText("");
        tokensSec.setText("");
        doneReason.setText("");
        prevImage.setIcon(null);
        if (null != resp) {
            OllamaClient.ModelSession session = client.getSession();
            modFamily.setText(Objects.toString(session.model.details.family));
            if (null == session.model.details.families) {
                modFamilies.setText("");
            } else {
                modFamilies.setText(Arrays.toString(session.model.details.families));
            }
            modFormat.setText(Objects.toString(session.model.details.format));
            modParmSize.setText(Objects.toString(session.model.details.parameterSize));
            modQuant.setText(Objects.toString(session.model.details.quantizationLevel));
            modParMod.setText(Objects.toString(session.model.model) + "/"
                    + Objects.toString(session.model.details.parentModel));
            curCtxSize.setText(Integer.toString(resp.context.size()));
            outTokens.setText(Integer.toString(resp.evalCount));
            inTokens.setText(Integer.toString(resp.promptEvalCount));
            tokensSec.setText(String.format("%.2f", 1e9 * resp.evalCount / resp.evalDuration));
            if (null == resp.doneReason) {
                doneReason.setText("");
            } else {
                doneReason.setText(resp.doneReason);
            }
        }
        if (null != uplImage.get()) {
            System.out.println("Setting sidebar image " + (null == resp));
            prevImage.setIcon(new ImageIcon(uplImage.get()));
        }
    }

    /**
     * Called on the dispatch thread to show the frame.
     */
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
     * Action class for sending user input to the model and displaying the
     * response.
     */
    class Interact extends AbstractAction {

        /**
         * Constructor.
         */
        public Interact() {
            super("Send");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            final String question = input.getText().trim();
            input.setText("");
            askModel("\n\n### Question\n\n", question);
        }

        /**
         * Asks the model the given question and displays the response in the
         * chat area.
         *
         * @param source The source text to prepend to the question.
         * @param question The question to ask the model.
         */
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
                                            new ModalityImage(pool, mod.getClass().getSimpleName(), mod);
                                        } else {
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
                        if (null == uplImage.get()) {
                            Response resp = client.askWithStream(
                                    (String) models.getSelectedItem(),
                                    question,
                                    listener);
                            return resp;
                        } else {
                            Response resp = client.askWithStream(
                                    (String) models.getSelectedItem(),
                                    question,
                                    listener,
                                    uplImage.get());
                            return resp;
                        }
                    }
                };
                chat.append("\n\n### Answer\n\n");
                sw.execute();
            }
        }
    }

    /**
     * Listener for selecting a new host from the combo box.
     */
    private class AddSelectHostListener implements ActionListener {

        public AddSelectHostListener() {
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
