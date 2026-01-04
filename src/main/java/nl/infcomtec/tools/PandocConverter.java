package nl.infcomtec.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Uses Pandoc to convert between text formats like Markdown and HTML.
 *
 * @author Walter Stroebel.
 */
public class PandocConverter extends ToolManager {

    private String output;

    /**
     * Markdown to HTML.
     *
     * @param markdownInput Markdown.
     * @return HTML.
     */
    public String convertMarkdownToHTML(String markdownInput) {
        setInput(markdownInput);
        setCommand("pandoc", "-f", "markdown", "-t", "html");
        run();
        return output;
    }

    /**
     * Markdown to RTF. Note that RTF support in Java is pretty broken.
     *
     * @param markdownInput Markdown.
     * @return HTML.
     */
    public String convertMarkdownToRTF(String markdownInput) {
        setInput(markdownInput);
        setCommand("pandoc", "-f", "markdown", "-t", "rtf");
        run();
        return output;
    }

    /**
     * HTML to Markdown.
     *
     * @param htmlInput HTML.
     * @return Markdown.
     */
    public String convertHTMLToMarkdown(String htmlInput) {
        setInput(htmlInput);
        setCommand("pandoc", "-f", "html", "-t", "markdown");
        run();
        return output;
    }

    /**
     * RTF to Markdown. Note that RTF support in Java is pretty broken.
     *
     * @param rtfInput HTML.
     * @return Markdown.
     */
    public String convertRTFToMarkdown(String rtfInput) {
        setInput(rtfInput);
        setCommand("pandoc", "-f", "rtf", "-t", "markdown");
        run();
        return output;
    }

    /**
     * MarkDown to plain text.
     *
     * @param markdownInput Markdown.
     * @return Plain text.
     */
    public String convertMarkdownToText(String markdownInput) {
        setInput(markdownInput);
        setCommand("pandoc", "-f", "markdown", "--wrap=auto", "--columns=80", "-t", "plain");
        run();
        return output;
    }

    /**
     * MarkDown to file.
     *
     * @param markdownInput Markdown.
     * @param file The result will be determined by the file's extension.
     */
    public void convertMarkdownToFile(String markdownInput, File file) {
        setInput(markdownInput);
        setCommand("pandoc", "-f", "markdown", "-o", file.getAbsolutePath());
        run();
    }

    @Override
    public void run() {
        internalRun();
        if (exitCode != 0) {
            output = "Running pandoc failed, rc=" + exitCode;
        } else {

            if (stdoutStream instanceof ByteArrayOutputStream) {
                output = new String(((ByteArrayOutputStream) stdoutStream).toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }

    /**
     * @return the output
     */
    public String getOutput() {
        return output;
    }
}
