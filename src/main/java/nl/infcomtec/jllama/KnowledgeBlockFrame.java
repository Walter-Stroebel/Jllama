package nl.infcomtec.jllama;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import nl.infcomtec.advswing.ABorder;
import nl.infcomtec.advswing.ATextArea;
import nl.infcomtec.advswing.ATextField;

/**
 * Manage a KnowledgeBlock with a GUI window.
 */
public class KnowledgeBlockFrame extends JFrame {

    private final JList<String> keywordsList = new JList<>();
    private final JList<String> relatedList = new JList<>();
    private final ATextArea contentTextArea = new ATextArea();
    private final ATextField filenameTextField = new ATextField();
    private JButton saveButton;
    private JButton dismissButton;
    private final KnowledgeBaseSystem.KnowledgeBlock kb;
    private TreeMap<String, KnowledgeBaseSystem.KnowledgeBlock> ak;

    /**
     * Manage a KnowledgeBlock with a GUI window.
     *
     * @param parent For placing the new window.
     * @param kb KnowledgeBlock to manage.
     */
    public KnowledgeBlockFrame(JFrame parent, final KnowledgeBaseSystem.KnowledgeBlock kb) {
        this.kb = kb;
        setTitle(kb.title);
        setLayout(new BorderLayout());

        // Add components to the frame
        add(createTopPanel(), BorderLayout.NORTH);
        contentTextArea.setText(kb.content);
        add(new JScrollPane(contentTextArea), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        // Set frame properties
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(parent); // Center on screen
    }

    /**
     * Details panel.
     *
     * @return The panel.
     */
    private JPanel createTopPanel() {
        ak = KnowledgeBaseSystem.getAllKnown(true);
        JPanel panel = new JPanel(new GridLayout(1, 2));
        Box kwBox = Box.createVerticalBox();
        kwBox.setBorder(new ABorder("Keywords"));
        panel.add(kwBox);
        Box relBox = Box.createVerticalBox();
        relBox.setBorder(new ABorder("Related"));
        panel.add(relBox);
        DefaultListModel<String> kwModel = new DefaultListModel<>();
        keywordsList.setModel(kwModel);
        DefaultListModel<String> relModel = new DefaultListModel<>();
        relatedList.setModel(relModel);

        TreeSet<String> kws = new TreeSet<>();
        for (Map.Entry<String, KnowledgeBaseSystem.KnowledgeBlock> e : ak.entrySet()) {
            for (String kw : e.getValue().keywords) {
                if (kws.add(kw)) {
                    kwModel.addElement(kw);
                }
            }
            relModel.addElement(e.getKey() + ", " + e.getValue().title);
        }
        kwBox.add(new JScrollPane(keywordsList));
        relBox.add(new JScrollPane(relatedList));

        return panel;
    }

    /**
     * Button panel
     *
     * @return The panel.
     */
    private JPanel createBottomPanel() {
        saveButton = new JButton(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    kb.content = contentTextArea.getText();
                    List<String> kws = keywordsList.getSelectedValuesList();
                    kb.keywords = new String[kws.size()];
                    for (int i = 0; i < kws.size(); i++) {
                        kb.keywords[i] = kws.get(i);
                    }
                    List<String> rels = relatedList.getSelectedValuesList();
                    kb.related = new String[rels.size()];
                    for (int i = 0; i < rels.size(); i++) {
                        String rel = rels.get(i);
                        int comma = rel.indexOf(',');
                        kb.related[i] = rel.substring(0, comma);
                    }
                    kb.save(filenameTextField.getText());
                    dispose();
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBlockFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        dismissButton = new JButton(new AbstractAction("Dispose") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(new JLabel("Filename:"));
        panel.add(filenameTextField);
        panel.add(saveButton);
        panel.add(dismissButton);
        return panel;
    }
}
