package nl.infcomtec.ollama;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.infcomtec.tools.ToolManager;

/**
 * Wrapper around Vagrant.
 *
 * @author walter
 */
public class Vagrant extends ToolManager {

    public static final File VAGRANT_DIR = new File(System.getProperty("user.home"), "vagrant/Worker");
    public static final File VAGRANT_KEY = new File(VAGRANT_DIR, ".vagrant/machines/default/virtualbox/private_key");

    public Vagrant() {
    }

    public static final String MARK_START = "$#";
    public static final String MARK_END = "#$";
    private com.jcraft.jsch.Session session;
    public final AtomicReference<Step> state = new AtomicReference<>(Step.stopped);
    public final StringBuilder log = new StringBuilder();
    private final String EOLN = System.lineSeparator();
    private final int MAX_OUTPUT = 20000;

    public void stop() {
        state.set(Step.stop);
        new Thread(this).start();
    }

    public void start() {
        state.set(Step.start);
        new Thread(this).start();
    }

    /**
     * Internal use only, must be called within synchronized(log).
     */
    private void logTimeStamp() {
        log.append(String.format("%1$tF %1$tT:", System.currentTimeMillis()));
    }

    public void log(String... msg) {
        System.out.println(Arrays.deepToString(msg));
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
                    state.set(Step.running);
                } catch (Exception e) {
                    log("Failed to connect to Vagrant:", e.getMessage());
                    state.set(Step.stopped);
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
     * @param text
     * @return
     */
    public String exec(String text) {
        while (Step.running != state.get()) {
            System.out.println("Waiting for vagrant");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Vagrant.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
        if (sb.length() > MAX_OUTPUT) {
            sb.setLength(MAX_OUTPUT / 2);
            sb.append(System.lineSeparator()).append("*** TRUNCATED ***");
        }
        if (output.length() + sb.length() > MAX_OUTPUT) {
            output.setLength(MAX_OUTPUT / 2);
            output.append(System.lineSeparator()).append("*** TRUNCATED ***");
        }
        String disp = sb.toString().trim() + System.lineSeparator() + output.toString();
        return disp;
    }

    private void execOnBox(final String cmd, final StringBuilder output) {
        try {
            log("Vagrant execute:", cmd);
            final Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);

            final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            ((ChannelExec) channel).setErrStream(errStream);

            channel.connect();

            final InputStream in = channel.getInputStream();
            final byte[] tmp = new byte[1024];
            int offset = output.length();
            while (true) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                output.append(new String(tmp, 0, i));
                if (channel.isClosed()) {
                    break;
                }
            }

            // Append stderr to output
            output.append(errStream.toString());

            channel.disconnect();
            log("Output:", output.substring(offset));
        } catch (Exception e) {
            log("Failed to execute on Vagrant:", e.getMessage());
            state.set(Step.stopped);
        }
    }

    public enum Step {
        stopped, start, running, stop
    }

}
