package nl.infcomtec.ollama;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.infcomtec.tools.ARROWS;
import nl.infcomtec.tools.ToolManager;

/**
 * Wrapper around Vagrant.
 *
 * @author walter
 */
public class Vagrant extends ToolManager {

    public static final File VAGRANT_DIR = new File(System.getProperty("user.home"), "vagrant/Worker");
    public static final File VAGRANT_KEY = new File(VAGRANT_DIR, ".vagrant/machines/default/virtualbox/private_key");

    public static final String MARK_START = "$#";
    public static final String MARK_END = "#$";
    private final AtomicReference<String> input = new AtomicReference<>(null);
    private com.jcraft.jsch.Session session;
    public final AtomicReference<Step> state = new AtomicReference<>(Step.stopped);
    public final StringBuilder log = new StringBuilder();
    private final String EOLN = System.lineSeparator();
    public AtomicReference<CallBack> onStateChange = new AtomicReference<>(null);
    public AtomicReference<CallBack> onInputPassed = new AtomicReference<>(null);
    public AtomicReference<CallBack> onOutputReceived = new AtomicReference<>(null);

    public Vagrant() {
    }

    public void stop() {
        Step old = state.get();
        state.set(Step.stop);
        doCallBack(onStateChange, old.name(), state.get().name());
        new Thread(this).start();
    }

    public void start() {
        Step old = state.get();
        state.set(Step.start);
        doCallBack(onStateChange, old.name(), state.get().name());
        new Thread(this).start();
    }

    /**
     * Internal use only, must be called within synchronized(log).
     */
    private void logTimeStamp() {
        log.append(String.format("%1$tF %1$tT:", System.currentTimeMillis()));
    }

    public void log(String... msg) {
        synchronized (log) {
            logTimeStamp();
            if (null == msg) {
                log.append(" Log called without any message.");
            } else {
                for (String s : msg) {
                    if (!Character.isWhitespace(log.charAt(log.length() - 1))) {
                        log.append(' ');
                    }
                    log.append(s);
                }
            }
            log.append(EOLN);
        }
    }

    @Override
    public void run() {
        switch (state.get()) {
            case stopped:
                log("Run was called in the ", state.get().name(), " state. That should not happen.");
                break;
            case start:
                log("Starting Vagrant.");
                setWorkingDir(VAGRANT_DIR);
                setCommand("vagrant", "up");
                internalRun();
                if (exitCode != 0) {
                    log("Running vagrant failed, rc=", Integer.toString(exitCode));
                }
                if (stdoutStream instanceof ByteArrayOutputStream) {
                    log("Vagrant start up:", EOLN,
                            new String(((ByteArrayOutputStream) stdoutStream).toByteArray(), StandardCharsets.UTF_8));
                    stdoutStream = null;
                } else {
                    log("Vagrant start up:", Objects.toString(stdoutStream));
                }
                if (stderrBuilder.length() > 0) {
                    log("Vagrant error output:" + stderrBuilder.toString());
                }
                stderrBuilder.setLength(0);
                try {
                    JSch jsch = new JSch();
                    jsch.addIdentity(VAGRANT_KEY.getAbsolutePath());
                    session = jsch.getSession("vagrant", "localhost", 2222);
                    java.util.Properties config = new java.util.Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    session.connect();
                    Step old = state.get();
                    state.set(Step.running);
                    doCallBack(onStateChange, old.name(), state.get().name());
                } catch (Exception e) {
                    log("Failed to connect to Vagrant:", e.getMessage());
                    Step old = state.get();
                    state.set(Step.stopped);
                    doCallBack(onStateChange, old.name(), state.get().name());
                }
                break;
            case stop:
                log("Stopping Vagrant.");
                if (null != session) {
                    try {
                        session.disconnect();
                    } catch (Exception any) {
                        // we don't care, we tried.
                    }
                    session = null;
                }
                setWorkingDir(VAGRANT_DIR);
                setCommand("vagrant", "halt");
                internalRun();
                log("Vagrant should be stopped.");
                break;
            default:
                log("Run was called in the ", state.get().name(), " state. That REALLY should not happen.");
                break;
        }
    }

    /**
     * Parse and execute.
     *
     * @param text with markers.
     * @return
     */
    public String execMarked(String text) {
        StringBuilder sb = new StringBuilder(text);
        StringBuilder output = new StringBuilder();
        int cmdStart = sb.indexOf(MARK_START);
        while (cmdStart >= 0) {
            int cmdEnd = sb.indexOf(MARK_END, cmdStart);
            if (cmdEnd > 2) {
                String cmd = sb.substring(cmdStart + 2, cmdEnd).trim(); // in case the LLM got fancy with whitespace
                execOnBox(cmd, output);
                sb.delete(cmdStart, cmdEnd + 2);
                cmdStart = sb.indexOf(MARK_START);
            }
        }
        // any non-whitespace left plus any output
        String disp = sb.toString().trim() + System.lineSeparator() + output.toString();
        return disp;
    }

    /**
     * Execute.
     *
     * @param cmd command.
     * @param input Optional input for the command.
     * @return
     */
    public String exec(String cmd, String input) {
        StringBuilder output = new StringBuilder();
        this.input.set(input);
        execOnBox(cmd, output);
        return output.toString();
    }

    private void execOnBox(final String cmd, final StringBuilder output) {
        while (Step.running != state.get()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Vagrant.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            log("Vagrant execute:", cmd);
            final Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);

            final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            ((ChannelExec) channel).setErrStream(errStream);

            channel.connect();
            final String inp = input.getAndSet(null);
            if (null != inp) {
                final OutputStream outStream = channel.getOutputStream();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            byte[] inputBytes = inp.getBytes();
                            int offset = 0;
                            int chunkSize = 1024;
                            while (offset < inputBytes.length) {
                                int length = Math.min(chunkSize, inputBytes.length - offset);
                                outStream.write(inputBytes, offset, length);
                                doCallBack(onInputPassed, inputBytes, offset, length);
                                outStream.flush();
                                offset += length;
                            }
                        } catch (IOException e) {
                            log("Error writing input to channel:", e.getMessage());
                        } finally {
                            try {
                                outStream.close();
                            } catch (IOException e) {
                                log("Error closing output stream:", e.getMessage());
                            }
                        }
                    }
                }).start();
            }
            final BufferedReader bfr;
            try {
                bfr = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                for (String line = bfr.readLine(); null != line; line = bfr.readLine()) {
                    doCallBack(onOutputReceived, line);
                    doCallBack(onOutputReceived, System.lineSeparator());
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                Logger.getLogger(Vagrant.class.getName()).log(Level.SEVERE, null, e);
            }

            // Append stderr to output
            output.append(errStream.toString());

            channel.disconnect();

            log("Output:", output.toString());
        } catch (Exception e) {
            log("Failed to execute on Vagrant:", e.getMessage());
            Step old = state.get();
            state.set(Step.stopped);
            doCallBack(onStateChange, old.name(), state.get().name());
        }
    }

    private void doCallBack(AtomicReference<CallBack> cb, String s) {
        CallBack get = cb.get();
        if (null != get) {
            get.cb(s);
        }
    }

    private void doCallBack(AtomicReference<CallBack> cb, String f, String t) {
        CallBack get = cb.get();
        if (null != get) {
            get.cb(f + ARROWS.RIGHT + t);
        }
    }

    private void doCallBack(AtomicReference<CallBack> cb, byte[] buf, int offset, int length) {
        CallBack get = cb.get();
        if (null != get) {
            get.cb(new String(buf, offset, length, StandardCharsets.UTF_8));
        }
    }

    public enum Step {
        stopped, start, running, stop
    }

    public interface CallBack {

        void cb(String s);
    }
}
