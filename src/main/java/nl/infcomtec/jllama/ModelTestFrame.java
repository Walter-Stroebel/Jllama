package nl.infcomtec.jllama;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Model test manager.
 *
 * @author walter
 */
public class ModelTestFrame extends JFrame {

    public final Semaphore start = new Semaphore(0);
    public JTextField endPoint = new JTextField(20);
    public JTextField model = new JTextField(20);
    public JTextField evalModel = new JTextField(20);
    private final JTextField testRuns = new JTextField(20);
    public JTextField outputFile = new JTextField(20);
    public final AtomicBoolean running = new AtomicBoolean(true);
    private final JTextArea updateArea = new JTextArea(10, 60);
    public File output = null;
    public int numRuns = 10;

    public ModelTestFrame() {
        setTitle("Model Tester");
        setLayout(new BorderLayout());
        getContentPane().add(new TestPanel(), BorderLayout.CENTER);
        updateArea.setEditable(false);
        updateArea.setLineWrap(true);
        updateArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(updateArea);
        getContentPane().add(scrollPane, BorderLayout.SOUTH);
        setSize(1080, 500);
    }

    public void postUpdate(final String update) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateArea.append(update);
                if (!update.endsWith(System.lineSeparator())) {
                    updateArea.append(System.lineSeparator());
                }
                updateArea.setCaretPosition(updateArea.getDocument().getLength());
                repaint();
            }
        });
    }

    private class TestPanel extends JPanel {

        public TestPanel() {
            endPoint.setText(Ollama.config.lastEndpoint);
            model.setText(Ollama.config.lastModel);
            evalModel.setText(Ollama.config.lastModel);
            testRuns.setText("10");
            model.setEditable(false);
            evalModel.setEditable(false);
            outputFile.setEditable(false);
            try {
                output = File.createTempFile("modeltest", ".md");
            } catch (IOException ex) {
                Logger.getLogger(ModelTestFrame.class.getName()).log(Level.SEVERE, null, ex);
                // Can't create a temp file? Fatal error!
                System.exit(2);
            }
            outputFile.setText(output.getAbsolutePath());
            setLayout(new GridLayout(0, 3));
            add(new JLabel("Endpoint:"));
            add(endPoint);
            add(new JButton(new AbstractAction("Test") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new SwingWorker<Response, Void>() {
                        @Override
                        protected Response doInBackground() throws Exception {
                            OllamaClient ollamaClient = new OllamaClient(endPoint.getText());
                            return ollamaClient.askAndAnswer(model.getText(), "hello");
                        }

                        @Override
                        protected void done() {
                            // This method is invoked on the EDT, allowing safe interaction with UI components.
                            try {
                                Response askAndAnswer = get(); // Retrieve the result of doInBackground.
                                JOptionPane.showMessageDialog(ModelTestFrame.this,
                                        askAndAnswer.response, "Test Succeeded", JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception any) {
                                JOptionPane.showMessageDialog(ModelTestFrame.this,
                                        any.getMessage(), "Test failed", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }.execute();
                }
            }));
            add(new JLabel("Model to test:"));
            add(model);
            final JComboBox<String> othModel = new JComboBox<>();
            final JComboBox<String> othEval = new JComboBox<>();
            for (AvailableModels.AvailableModel am : Ollama.getAvailableModels().get(endPoint.getText()).models) {
                othModel.addItem(am.name);
                othEval.addItem(am.name);
            }
            othModel.setSelectedItem(model.getText());
            othEval.setSelectedItem(model.getText());
            othModel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedModel = (String) othModel.getSelectedItem();
                    model.setText(selectedModel);
                }
            });
            othEval.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedModel = (String) othEval.getSelectedItem();
                    evalModel.setText(selectedModel);
                }
            });
            add(othModel);
            add(new JLabel("Evaluator model:"));
            add(evalModel);
            add(othEval);
            add(new JLabel("Number of test runs:"));
            add(testRuns);
            add(new JLabel(" 2 <= tests <= 100"));
            add(new JLabel("Write results to:"));
            add(outputFile);
            add(new JButton(new AbstractAction("Change file") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser jfc = new JFileChooser();
                    int res = jfc.showSaveDialog(ModelTestFrame.this);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        output = jfc.getSelectedFile();
                        outputFile.setText(output.getAbsolutePath());
                    }
                }
            }));
            add(new JButton(new AbstractAction("Help?") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(ModelTestFrame.this,
                            "Testing a model can be a VERY lenghty operation.\n"
                            + "While the test is running your machine will be under heavy load.\n"
                            + "You will most likely not be able to do any other tasks.\n"
                            + "The Cancel button will abort starting the test.\n"
                            + "The Run button will start the test with the configuration entered here.");
                }
            }));
            add(new JButton(new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    running.set(false);
                    start.release();
                    dispose();
                }
            }));
            add(new JButton(new AbstractAction("Run") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        numRuns = Integer.parseInt(testRuns.getText());
                    } catch (Exception any) {
                        numRuns = 10;
                    }
                    numRuns = Math.min(Math.max(numRuns, 2), 100);
                    testRuns.setText(Integer.toString(numRuns));
                    start.release();
                }
            }));
        }
    }

}
