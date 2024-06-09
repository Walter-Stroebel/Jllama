/*
 * Copyright (c) 2024 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.datapond;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;
import javax.imageio.ImageIO;
import nl.infcomtec.jllama.Ollama;
import nl.infcomtec.jllama.OllamaClient;
import nl.infcomtec.jllama.Response;

/**
 * A DataDrop is some related information in any possible digital formats.
 *
 * Parts can be images, a videos, audio clips, PDFs, anything.
 *
 * A DataDrop is stored as unique files where the filename is actually an UUID.
 * Thus we can "join" several files, like an image file and a description or
 * other meta data about that image, by having multiple files that evaluate to
 * the same DataDrop.
 *
 * @author walter
 */
public class DataDrop implements Comparable<DataDrop> {

    /**
     * Unsigned fixed radix 36.
     *
     * @param l value
     * @return radix 32 representation as 13 characters.
     */
    public static String r32(long l) {
        StringBuilder ret = new StringBuilder(Long.toUnsignedString(l, 32));
        while (ret.length() < 13) {
            ret.insert(0, '0');
        }
        return ret.toString();
    }

    /**
     * Return as key string (radix 32).
     *
     * @param uuid
     * @return 26 characters.
     */
    public static String fixedString(UUID uuid) {
        return r32(uuid.getMostSignificantBits()) + r32(uuid.getLeastSignificantBits());
    }

    /**
     * Decode string in radix 32 to UUID.
     *
     * @param s String
     * @return UUID
     */
    public static UUID from(String s) {
        return new UUID(
                Long.parseUnsignedLong(s.substring(0, 13), 32),
                Long.parseUnsignedLong(s.substring(13, 26), 32));
    }

    /**
     * Interpret file as UUID in radix 32.
     *
     * @param f File, name is string, extension is ignored.
     * @return UUID
     */
    public static UUID from(File f) {
        return from(f.getName());
    }

    /**
     * Creates a File object that should work on most known file systems and is
     * as short as possible.
     *
     * @param parent Directory.
     * @param uuid DataDrop id.
     * @param ext optional, defaults to ".json". Should normally start with a
     * dot.
     * @return As File object.
     */
    public static File asFilePath(File parent, UUID uuid, String ext) {
        if (null == ext) {
            return new File(parent, fixedString(uuid) + ".json");
        }
        return new File(parent, fixedString(uuid) + ext.toLowerCase());
    }

    /**
     * Load a DataDrop from storage.
     *
     * @param workDir Where the pond is.
     * @param uuid UUID to load.
     * @return A DataDrop.
     * @throws Exception When things do not work.
     */
    public static DataDrop load(File workDir, UUID uuid) throws Exception {
        return load(asFilePath(workDir, uuid, ".json"));
    }

    /**
     * Load a DataDrop from storage.
     *
     * @param file Stored DataDrop (JSON file).
     * @return A DataDrop.
     * @throws Exception When things do not work.
     */
    public static DataDrop load(File file) throws Exception {
        return Ollama.getMapper().readValue(file, DataDrop.class);
    }

    /**
     * Create a DataDrop from a loaded image.
     *
     * @param workDir Where the pond is.
     * @param img Image as loaded.
     * @return A DataDrop.
     * @throws Exception When things do not work.
     */
    public static DataDrop fromImage(File workDir, BufferedImage img) throws Exception {
        OllamaClient client = new OllamaClient(Ollama.config.lastEndpoint);
        Response ans = client.askAndAnswer("llava", "Describe this image", img);
        DataDrop ret = new DataDrop();
        ret.addImage(workDir, img, ans.response);
        return ret;
    }

    /**
     * Create a DataDrop from an image file.
     *
     * @param workDir Where the pond is.
     * @param imgFile To load the image from.
     * @return A DataDrop.
     * @throws Exception When things do not work.
     */
    public static DataDrop fromImageFile(File workDir, File imgFile) throws Exception {
        return DataDrop.fromImage(workDir, ImageIO.read(imgFile));
    }

    /**
     * Create a DataDrop straight from text.
     *
     * @param workDir Where the pond is.
     * @param text To store in the drop.
     * @return A DataDrop.
     * @throws Exception When things do not work.
     */
    public static DataDrop fromText(File workDir, String text) throws Exception {
        DataDrop ret = new DataDrop();
        ret.text = text;
        ret.save(workDir);
        return ret;
    }

    /**
     * Create a DataDrop that is a summary of the text.
     *
     * @param workDir Where the pond is.
     * @param text To summarize in the drop.
     * @return A DataDrop.
     * @throws Exception When things do not work.
     */
    public static DataDrop summary(File workDir, String text) throws Exception {
        return summary(workDir, Ollama.config.lastModel, text);
    }

    /**
     * Create a DataDrop that is a summary of the text.
     *
     * @param workDir Where the pond is.
     * @param model Use a specific model.
     * @param text To summarize in the drop.
     * @return A DataDrop.
     * @throws Exception When things do not work.
     */
    public static DataDrop summary(File workDir, String model, String text) throws Exception {
        OllamaClient client = new OllamaClient(Ollama.config.lastEndpoint);
        Response ans = client.askAndAnswer(model, "Please summarize the following:" + System.lineSeparator() + text);
        DataDrop ret = fromText(workDir, ans.response);
        return ret;
    }
    /**
     * The unique key
     */
    public final UUID uuid;
    /**
     * Any DataDrop has at least some text.
     */
    public String text;

    /**
     * Create new DataDrop.
     */
    public DataDrop() {
        this.uuid = UUID.randomUUID();
        this.text = "";
    }

    /**
     * Create from UUID.
     *
     * @param uuid The UUID
     */
    public DataDrop(UUID uuid) {
        this.uuid = uuid;
        this.text = "";
    }

    @Override
    public int compareTo(DataDrop o) {
        return uuid.compareTo(o.uuid);
    }

    /**
     * Creates a DataDrop image object.
     *
     * @param workDir Where the pond is.
     * @param img Image to store.
     * @param desc Description of the image.
     * @throws Exception When things do not work.
     */
    public void addImage(File workDir, BufferedImage img, String desc) throws Exception {
        ImageIO.write(img, "png", asFilePath(workDir, uuid, ".png"));
        text = desc;
        save(workDir);
    }

    /**
     * Remove all files belonging to this DataDrop.
     *
     * @param workDir Where the pond is.
     */
    public void delete(File workDir) {
        File[] files = workDir.listFiles();
        if (null == files) {
            return;
        }
        String id = fixedString(uuid);
        for (File f : files) {
            if (f.getName().toLowerCase().startsWith(id)) {
                f.delete();
            }
        }
    }

    /**
     * Save the drop as JSON.
     *
     * @param workDir Where the pond is.
     * @throws Exception When things do not work.
     */
    public void save(File workDir) throws Exception {
        Ollama.getMapper().writeValue(asFilePath(workDir, uuid, ".json"), this);
    }

    @Override
    public String toString() {
        return "DataDrop{" + "uuid=" + uuid + ", text=" + text + '}';
    }

    /**
     * Fetch an image if there is one.
     *
     * @param workDir Where the pond is.
     * @return an image.
     * @throws Exception When things do not work.
     */
    public BufferedImage getImage(File workDir) throws Exception {
        return ImageIO.read(asFilePath(workDir, uuid, ".png"));
    }

    /**
     * Check if there is an image.
     *
     * @param workDir Where the pond is.
     * @return True if there is an image.
     */
    public boolean hasImage(File workDir) {
        return asFilePath(workDir, uuid, ".png").exists();
    }

}
