package nl.infcomtec.tools;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*!
 *  Usage example.
 *
 *  \code
 *  public class Convert extends ToolManager {
 *      public Image convert (Image in, String fromFormat, String toFormat){
 *          // build command line, set working dir and provide input if any
 *      }
 *      public void run(){
 *          internalRun();
 *          // handle the result(s)
 *      }
 *  }
 *  \endcode
 *
 */
/**
 * Management class for using external system utilities.
 *
 * Examples of tools:
 * <ul>
 * <li>GraphViz</li>
 * <li>PanDoc</li>
 * <li>ImageMagick</li>
 * </ul>
 *
 * The design principle is to leverage tools like those mentions as OOP objects.
 *
 * @author Walter Stroebel
 */
public abstract class ToolManager implements Runnable {

    private static final String NO_COMMAND_SPECIFIED = "No command specified";

    protected static void deleteAll(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] files = fileOrDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteAll(file);
                }
            }
        } else {
            fileOrDir.delete();
        }
    }

    private byte[] inBytes;
    private ProcessBuilder pb;
    protected File workingDir;
    protected OutputStream stdoutStream;
    protected StringBuilder stderrBuilder = new StringBuilder();
    public int exitCode;

    protected void setWorkingDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to create the directory");
            }
        }
        if (dir.exists() && dir.isDirectory()) {
            this.workingDir = dir;
        } else {
            throw new RuntimeException("Invalid directory");
        }
    }

    public void setInput(Object in) {
        if (null != in) {
            if (in instanceof byte[]) {
                this.inBytes = (byte[]) in;
            } else {
                inBytes = in.toString().getBytes(StandardCharsets.UTF_8);
            }
        }
    }

    public void setOutput(OutputStream out) {
        stdoutStream = out;
    }

    protected void setCommand(List<String> cmd) {
        this.pb = new ProcessBuilder(cmd);
    }

    public void setCommand(String... cmd) {
        this.pb = new ProcessBuilder(cmd);
    }

    public String getCommand() {
        if (null == pb) {
            return NO_COMMAND_SPECIFIED;
        }
        return pb.command().toString();
    }

    protected void internalRun() {
        if (null == pb) {
            throw new RuntimeException(NO_COMMAND_SPECIFIED);
        }
        if (null == workingDir) {
            try {
                Path tempDir = Files.createTempDirectory("ToolManagerTempDir");
                workingDir = tempDir.toFile();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        deleteAll(workingDir);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        pb.directory(workingDir);
        if (null == stdoutStream) {
            stdoutStream = new ByteArrayOutputStream();
        }
        try {
            final Process p = pb.start();

            // Thread to handle stdout
            Thread stdoutThread = new Thread() {
                @Override
                public void run() {
                    try (InputStream is = p.getInputStream()) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            stdoutStream.write(buffer, 0, read);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            // Thread to handle stderr
            Thread stderrThread = new Thread() {
                @Override
                public void run() {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.err.println(line);
                            stderrBuilder.append(line).append(System.lineSeparator());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            stdoutThread.start();
            stderrThread.start();

            // Thread to handle input, if any
            if (inBytes != null) {
                // Spawn a thread to manage input
                new Thread() {
                    @Override
                    public void run() {
                        try (OutputStream os = p.getOutputStream()) {
                            os.write(inBytes);
                            p.getOutputStream().close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }.start();
            } else {
                p.getOutputStream().close();
            }

            // Wait for the process to complete
            exitCode = p.waitFor();

            // Wait for stdout and stderr threads to complete
            stdoutThread.join();
            stderrThread.join();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
