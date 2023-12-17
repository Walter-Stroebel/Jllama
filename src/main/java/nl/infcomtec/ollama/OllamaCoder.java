package nl.infcomtec.ollama;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Rectangle;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

/**
 * This is a the Ollama coder window. It is meant to help with or learn coding,
 * with any model available.
 *
 * @author Walter Stroebel
 */
public class OllamaCoder {

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
        chat.setFont(useFont);
        input.setFont(useFont);
        code.setFont(useFont);
    }

    private final JFrame frame;
    private final JToolBar buttons = new JToolBar();
    private final JTextArea chat = new JTextArea();
    private final JTextArea code = new JTextArea();
    private final JTextArea input = new JTextArea(4, 80);
    private OllamaClient client;
    private final JComboBox<String> hosts = new JComboBox<>();
    private final JComboBox<String> models = new JComboBox<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private JScrollPane codePanel;

    /**
     * Ties all the bits and pieces together into a GUI.
     */
    public OllamaCoder() {
        setupGUI(Ollama.config.fontSize);
        frame = new JFrame("Ollama Coder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container cont = frame.getContentPane();
        cont.setLayout(new BorderLayout());
        buttonBar();
        cont.add(buttons, BorderLayout.NORTH);
        chat.setLineWrap(true);
        code.setLineWrap(true);
        chat.setWrapStyleWord(true);
        code.setWrapStyleWord(true);
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
                "Assistant"));
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
                Logger.getLogger(OllamaCoder.class.getName()).log(Level.SEVERE, null, ex);
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
        buttons.add(new AbstractAction("Chat") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                frame.dispose();
                new OllamaChatFrame();
            }
        });
        buttons.add(new JToolBar.Separator());
        String lsModel = Ollama.config.getLastModel();
        String lsHost = Ollama.config.getLastEndpoint();
        for (Map.Entry<String, AvailableModels> e : Ollama.getAvailableModels().entrySet()) {
            addToHosts(e.getKey());
            if (0 == models.getItemCount()) {
                for (AvailableModels.AvailableModel am : e.getValue().models) {
                    models.addItem(am.name);
                    if (null == lsHost) {
                        lsHost = e.getKey();
                    }
                    if (null == lsModel) {
                        lsModel = am.name;
                    }
                }
            }
        }
        client = new OllamaClient(lsHost);
        models.setSelectedItem(lsModel);
        hosts.setSelectedItem(lsHost);
        hosts.addActionListener(new AddSelectHost());
        hosts.setEditable(true);
        buttons.add(new JLabel("Hosts:"));
        buttons.add(hosts);
        buttons.add(new JToolBar.Separator());
        buttons.add(new JLabel("Models:"));
        buttons.add(models);
        buttons.add(new JToolBar.Separator());
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

    private JScrollPane sideBar() {
        codePanel = new JScrollPane();
        codePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 206, 80), 5),
                "Code window"));
        codePanel.setPreferredSize(new Dimension(Ollama.config.w / 2 - 20, Ollama.config.h));
        codePanel.setViewportView(code);
        return codePanel;
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
                Rectangle bounds = frame.getBounds();
                Ollama.config.update(bounds);
                codePanel.setPreferredSize(new Dimension(Ollama.config.w / 2 - 20, Ollama.config.h));
            }
        });
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
                        } catch (Exception ex) {
                            Logger.getLogger(OllamaCoder.class.getName()).log(Level.SEVERE, null, ex);
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
                        Ollama.config.lastModel = (String) models.getSelectedItem();
                        Ollama.config.update();
                        Response resp = client.askWithStream(Ollama.config.lastModel, question, listener);
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
                for (Map.Entry<String, AvailableModels> e : Ollama.getAvailableModels().entrySet()) {
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
                client = new OllamaClient(selHost);
            }
        }
    }

}
