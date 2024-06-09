/*
 * Copyright (c) 2024 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.datapond;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import nl.infcomtec.jllama.Ollama;
import nl.infcomtec.jllama.OllamaClient;
import nl.infcomtec.jllama.Response;

/**
 *
 * @author walter
 */
public class DataPond {

    public static DataDrop fromImage(File workDir, BufferedImage img) throws Exception {
        OllamaClient client = new OllamaClient(Ollama.config.lastEndpoint);
        Response ans = client.askAndAnswer("llava", "Describe this image", img);
        DataDrop ret = new DataDrop();
        ret.addImage(workDir, img, ans.response);
        return ret;
    }

    public static DataDrop fromImageFile(File workDir, File imgFile) throws Exception {
        return fromImage(workDir, ImageIO.read(imgFile));
    }

    public static DataDrop summary(File workDir, String text) throws Exception {
        return summary(workDir, "phi3", text);
    }

    public static DataDrop summary(File workDir, String model, String text) throws Exception {
        OllamaClient client = new OllamaClient(Ollama.config.lastEndpoint);
        Response ans = client.askAndAnswer(model,
                "Please summarize the following:" + System.lineSeparator() + text);
        DataDrop ret = fromText(workDir, ans.response);
        return ret;
    }

    public static DataDrop fromText(File workDir, String text) throws Exception {
        DataDrop ret = new DataDrop();
        ret.text = text;
        ret.save(workDir);
        return ret;
    }
}
