package nl.infcomtec.jllama;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Manage a KnowledgeBlock with a GUI window.
 */
public class KnowledgeBlockFrame extends JFrame {

    private final JComboBox<String> keywordsComboBox = new JComboBox<>(new String[]{""});
    private final JList<String> relatedList = new JList<>();
    private final JTextArea contentTextArea = new JTextArea();
    private final JTextField filenameTextField = new JTextField();
    private JButton saveButton;
    private JButton dismissButton;
    private final KnowledgeBaseSystem.KnowledgeBlock kb;

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

        // Event Handlers
        setupEventHandlers();
    }

    /**
     * Details panel.
     *
     * @return The panel.
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        keywordsComboBox.setEditable(true);
        panel.add(keywordsComboBox);

        DefaultListModel<String> model = new DefaultListModel<>();
        File[] kbs = KnowledgeBaseSystem.KBFolder.listFiles();
        if (null != kbs) {
            for (File f : kbs) {
                try {
                    KnowledgeBaseSystem.KnowledgeBlock nkb = new KnowledgeBaseSystem.KnowledgeBlock(f.getName());
                    model.addElement(f.getName() + ", " + nkb.title);
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBlockFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        relatedList.setModel(model);
        panel.add(new JScrollPane(relatedList));

        return panel;
    }

    /**
     * Button panel
     *
     * @return The panel.
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(new JLabel("Filename:"));
        panel.add(filenameTextField);
        panel.add(saveButton);
        panel.add(dismissButton);
        return panel;
    }

    /**
     * Button stuff.
     */
    private void setupEventHandlers() {
        saveButton = new JButton(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int kwc = keywordsComboBox.getItemCount();
                    kb.keywords = new String[kwc];
                    for (int i = 0; i < kwc; i++) {
                        kb.keywords[i] = keywordsComboBox.getItemAt(i);
                    }
                    List<String> rels = relatedList.getSelectedValuesList();
                    kb.related = new String[rels.size()];
                    for (int i = 0; i < rels.size(); i++) {
                        String rel = rels.get(i);
                        int comma = rel.indexOf(',');
                        kb.related[i] = rel.substring(0, comma);
                    }
                    kb.save(filenameTextField.getText());
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
    }
}
