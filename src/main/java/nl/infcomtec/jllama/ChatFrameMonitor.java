package nl.infcomtec.jllama;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * This class implements a GUI monitor for observing chat interaction metrics in
 * real-time. It tracks statistics like input characters, output characters,
 * context size, and response duration for each AI model used during the chat
 * interactions. These statistics are visualized in a JFrame with custom drawing
 * to represent the data graphically. The class demonstrates the use of AWT and
 * Swing in a multi-threaded environment, synchronization of shared resources,
 * and atomic references for thread-safe operations.
 */
public class ChatFrameMonitor implements Monitor {

    public static final String NAME = "ChatFrame Monitor";
    private final TreeMap<String, Stats> stats = new TreeMap<>();
    private final JFrame frame;

    /**
     * Constructs a new ChatFrameMonitor instance, initializing the GUI
     * components and making the frame visible.
     */
    public ChatFrameMonitor() {
        frame = new JFrame(NAME);
        frame.setBounds(100, 100, 1000, 1000);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(new Grapher(), BorderLayout.CENTER);
        frame.setBounds(100, 100, 1000, 1000);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void requested(String request) {
        try {
            Request val = Ollama.getMapper().readValue(request, Request.class);
            synchronized (stats) {
                Stats st = stats.get(val.model);
                if (null == st) {
                    st = new Stats();
                }
                Stats.Data dt = new Stats.Data();
                dt.inChars += val.prompt.length();
                st.data.add(dt);
                stats.put(val.model, st);
                active.set(st);
            }
        } catch (Exception any) {
            Ollama.oops(any);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.repaint();
            }
        });
    }
    private final AtomicReference<Stats> active = new AtomicReference<>();

    @Override
    public void responded(String response) {
        try {
            Response val = Ollama.getMapper().readValue(response, Response.class);
            synchronized (stats) {
                Stats st = stats.get(val.model);
                if (null == st) {
                    st = new Stats();
                }
                Stats.Data dt = new Stats.Data();
                dt.outChars += val.response.length();
                if (null != val.context) {
                    dt.ctxSize = val.context.size();
                }
                dt.duration = val.totalDuration;
                st.data.add(dt);
                stats.put(val.model, st);
                active.set(st);
            }
        } catch (Exception any) {
            Ollama.oops(any);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.repaint();
            }
        });
    }

    @Override
    public void oops(Exception excptn) {
        System.err.println("Exception: " + excptn.getMessage());
    }

    @Override
    public void close() throws Exception {
        frame.dispose();
    }

    private static class Stats {

        private final long first = System.currentTimeMillis();
        private final LinkedList<Data> data = new LinkedList<>();

        private static class Data {

            private final long at = System.currentTimeMillis();
            private long duration;
            private int ctxSize;
            private int inChars;
            private int outChars;
        }
    }

    private class Grapher extends JPanel {

        public Grapher() {
        }

        @Override
        public void paint(Graphics g) {
            int w = getWidth() - 10;
            int h = getHeight() - 10;
            int h4 = h / 4;
            g.setColor(Color.DARK_GRAY);
            g.fillRect(5, 5, w, h);
            synchronized (stats) {
                Stats st = active.get();
                if (null != st) {
                    int maxCtx = 0;
                    int maxDur = 0;
                    int maxIn = 0;
                    int maxOut = 0;
                    for (Stats.Data dt : st.data) {
                        maxCtx = Math.max(maxCtx, dt.ctxSize);
                        maxDur = Math.max(maxDur, (int) (dt.duration / 1000000));
                        maxIn = Math.max(maxIn, dt.inChars);
                        maxOut = Math.max(maxOut, dt.outChars);
                    }
                    if (st.data.size() < 2 || maxCtx <= 0 || maxDur <= 0 || maxIn <= 0 || maxOut <= 0) {
                        return;
                    }
                    double xF = 1.0 * w / st.data.size();
                    double ctxF = 1.0 * h4 / maxCtx;
                    double durF = 1.0 * h4 / maxDur;
                    double inF = 1.0 * h4 / maxIn;
                    double outF = 1.0 * h4 / maxOut;
                    int lx = 0;
                    int lCtx = 0;
                    int lDur = 0;
                    int lIn = 0;
                    int lOut = 0;
                    int p = 0;
                    for (Stats.Data dt : st.data) {
                        int x = (int) (p * xF);
                        int y = (int) (dt.ctxSize * ctxF);
                        g.setColor(Color.CYAN);
                        g.drawString("Context " + maxCtx, 0, h - h4 + 16);
                        if (y > 0) {
                            g.drawLine(lx, h - lCtx, x, h - y);
                            lCtx = y;
                        } else {
                            g.drawLine(lx, h - lCtx, x, h - lCtx);
                        }
                        y = (int) (dt.duration / 1000000 * durF);
                        g.setColor(Color.MAGENTA);
                        g.drawString("Duration " + maxDur, 0, h - h4 - h4 + 16);
                        if (y > 0) {
                            g.drawLine(lx, h - h4 - lDur, x, h - h4 - y);
                            lDur = y;
                        } else {
                            g.drawLine(lx, h - h4 - lDur, x, h - h4 - lDur);
                        }
                        y = (int) (dt.inChars * inF);
                        g.setColor(Color.GREEN);
                        g.drawString("CharsIn " + maxIn, 0, h - h4 - h4 - h4 + 16);
                        if (y > 0) {
                            g.drawLine(lx, h - h4 - h4 - lIn, x, h - h4 - h4 - y);
                            lIn = y;
                        } else {
                            g.drawLine(lx, h - h4 - h4 - lIn, x, h - h4 - h4 - lIn);
                        }
                        y = (int) (dt.outChars * outF);
                        g.setColor(Color.RED);
                        g.drawString("CharsOut " + maxOut, 0, 16);
                        if (y > 0) {
                            g.drawLine(lx, h - h4 - h4 - h4 - lOut, x, h - h4 - h4 - h4 - y);
                            lOut = y;
                        } else {
                            g.drawLine(lx, h - h4 - h4 - h4 - lOut, x, h - h4 - h4 - h4 - lOut);
                        }
                        lx = x;
                        p++;
                    }
                }
            }
        }
    }
}
