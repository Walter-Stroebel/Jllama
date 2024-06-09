/*
 * Copyright (c) 2024 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.advswing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Signs of life.
 *
 * @author walter
 */
public class ShowLife {

    /**
     * Base class.
     */
    public static abstract class TickableComponent extends JPanel {

        private final AtomicBoolean working = new AtomicBoolean();
        private long startTime;

        /**
         * Create in stopped state.
         */
        public TickableComponent() {
            this(false);
        }

        /**
         * Create
         *
         * @param start working state.
         */
        public TickableComponent(boolean start) {
            this.working.set(start);
            this.startTime = System.currentTimeMillis();
            Timer timer = new Timer(200, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tick();
                }
            });
            timer.start();
        }

        /**
         * Start/stop.
         *
         * @param state
         */
        public void setWorking(boolean state) {
            working.set(state);
        }

        private void tick() {
            long currentTime = System.currentTimeMillis();
            if (working.get()) {
                long elapsedTime = currentTime - startTime;
                advanceState(elapsedTime);
            }
            startTime = currentTime;
        }

        /**
         * Override with display update.
         *
         * @param elapsedTime Time spend running since last update.
         */
        protected abstract void advanceState(long elapsedTime);
    }

    /**
     * Simple label.
     */
    public static class SecondsElapsedLabel extends TickableComponent {

        private final JLabel label;
        private long totalTime = 0;

        public SecondsElapsedLabel(boolean start) {
            super(start);
            this.label = new JLabel("   0 seconds elapsed   ");
            add(label);
        }

        public SecondsElapsedLabel() {
            this(false);
        }

        @Override
        protected void advanceState(long elapsedTime) {
            totalTime += elapsedTime;
            label.setText(String.format("  %.1f seconds elapsed  ", 0.001 * totalTime));
        }
    }

    public static void main(String[] args) {
        final SecondsElapsedLabel label = new SecondsElapsedLabel();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(label, BorderLayout.CENTER);
                frame.pack();
                frame.setVisible(true);
            }
        });
        try {
            label.setWorking(true);
            Thread.sleep(3000);
            label.setWorking(false);
            Thread.sleep(3000);
            label.setWorking(true);
        } catch (InterruptedException ex) {
            Logger.getLogger(ShowLife.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
