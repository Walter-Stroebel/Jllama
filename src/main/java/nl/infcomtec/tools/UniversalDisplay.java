/*
 * Copyright (c) 2025 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;

public class UniversalDisplay extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JLabel imageLabel = new JLabel();
    private final JEditorPane htmlPane = new JEditorPane();
    private final JTextArea hexArea = new JTextArea();
    private final JScrollPane scrollPane;
    private final ButtonGroup modeGroup = new ButtonGroup();
    private final BufferedImage asImage;
    private final String asString;
    private final String htmlContent;

    public UniversalDisplay(byte[] data) {
        super("Universal Display");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width / 2;
        int height = screenSize.height / 2;
        setSize(width, height);
        setLocationRelativeTo(null); // Center on screen
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        asImage = tryImage(data);
        boolean isHMTL;
        boolean isText;
        if (null == asImage) {
            asString = new String(data, StandardCharsets.UTF_8);
            isHMTL = isHTML();
            if (!isHMTL) {
                isText = isMostlyText(data);
                htmlContent = new PandocConverter().convertMarkdownToHTML(asString);
            } else {
                isText = false;
                htmlContent = asString;
            }
        } else {
            asString = "This seems to be an image.";
            isHMTL = false;
            isText = false;
            htmlContent = "<h1>This seems to be an image.</h1>";
        }
        if (null != asImage) {
            Image img = asImage;
            if (asImage.getWidth() > width || asImage.getHeight() > height) {
                double wf = 1.0 * asImage.getWidth() / width;
                double hf = 1.0 * asImage.getHeight() / height;
                if (wf > hf) {
                    int w = (int) Math.round(asImage.getWidth() / wf);
                    int h = (int) Math.round(asImage.getHeight() / wf);
                    img = asImage.getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT);
                } else {
                    int w = (int) Math.round(asImage.getWidth() / hf);
                    int h = (int) Math.round(asImage.getHeight() / hf);
                    img = asImage.getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT);
                }
            }
            scrollPane = new JScrollPane(new JLabel(new ImageIcon(img)));
        } else if (isHMTL) {
            htmlPane.setEditable(false);
            htmlPane.setContentType("text/html");
            htmlPane.setEditorKit(new HTMLEditorKit());
            htmlPane.setText(htmlContent);
            scrollPane = new JScrollPane(htmlPane);
        } else if (isText) {
            textArea.setText(asString);
            textArea.setCaretPosition(0);
            scrollPane = new JScrollPane(textArea);
        } else {
            textArea.setText(asString);
            textArea.setCaretPosition(0);
            scrollPane = new JScrollPane(textArea);
        }
        // Toolbar for mode switching
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        addModeButton(toolBar, "Text", new Runnable() {
            @Override
            public void run() {
                textArea.setText(asString);
                textArea.setCaretPosition(0);
                scrollPane.setViewportView(textArea);
            }
        }, isText);
        addModeButton(toolBar, "HTML", new Runnable() {
            @Override
            public void run() {
                htmlPane.setEditable(false);
                htmlPane.setContentType("text/html");
                htmlPane.setEditorKit(new HTMLEditorKit());
                htmlPane.setText(htmlContent);
                scrollPane.setViewportView(htmlPane);
            }
        }, isHMTL);
        addModeButton(toolBar, "Image", new Runnable() {
            @Override
            public void run() {
                Image img = asImage;
                if (null == asImage) {
                    BufferedImage notThis = new BufferedImage(200, 16, BufferedImage.TYPE_BYTE_BINARY);
                    Graphics2D g2 = notThis.createGraphics();
                    g2.setColor(Color.WHITE);
                    g2.drawString("Not an image", 0, 15);
                    g2.dispose();
                    img = notThis;
                } else if (asImage.getWidth() > width || asImage.getHeight() > height) {
                    double wf = 1.0 * asImage.getWidth() / width;
                    double hf = 1.0 * asImage.getHeight() / height;
                    if (wf > hf) {
                        int w = (int) Math.round(asImage.getWidth() / wf);
                        int h = (int) Math.round(asImage.getHeight() / wf);
                        img = asImage.getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT);
                    } else {
                        int w = (int) Math.round(asImage.getWidth() / hf);
                        int h = (int) Math.round(asImage.getHeight() / hf);
                        img = asImage.getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT);
                    }
                }
                scrollPane.setViewportView(new JLabel(new ImageIcon(img)));
            }
        }, null != asImage);

        setVisible(true);
    }

    private void addModeButton(JToolBar toolbar, String label, Runnable action, boolean isSel) {
        JCheckBox button = new JCheckBox(label);
        modeGroup.add(button);
        toolbar.add(button);
        button.addActionListener((ActionEvent e) -> {
            action.run();
        });
        button.setSelected(isSel);
    }

    private BufferedImage tryImage(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            return ImageIO.read(bais);
        } catch (Exception ignored) {
            try {
                return new ImageConverter().convertWebpToImage(data);
            } catch (Exception alsoIgnored) {
                return null;
            }
        }
    }

    private boolean isHTML() {
        long count = asString.chars().limit(100).filter(c -> c == '<' || c == '>').count();
        return count > 5;
    }

    private boolean isMostlyText(byte[] data) {
        int ascii = 0;
        int weird = 0;
        for (byte sb : data) {
            int b = sb & 0xFF;
            if (b > 31 && b < 127) {
                ascii++;
            } else {
                weird++;
            }
        }
        return ascii > weird * 2;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UniversalDisplay("# Hello, World!\n## Here we go.\n".getBytes(StandardCharsets.UTF_8)));
    }
}
