package nl.infcomtec.ollama.llm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.infcomtec.ollama.Ollama;

/**
 * This holds generic data for the LLM (Large Language Model).
 *
 * The concept is to give the LLM a navigable data storage it can update by
 * itself.
 *
 * This will require instructing the LLM how to fetch and update said data
 * store.
 *
 * The code is written quite defensive but not all weird cases can be handled.
 *
 * Hint: if things break ... you could write a good prompt and pass the data to
 * an LLM and let it figure out what the intent might have been.
 *
 * @author Walter Stroebel
 */
public class LLMTreeData {

    public static final File DATA_DIR = new File(Ollama.WORK_DIR, "llmdata");
    public static final String MARKER = "##";
    public static final int RADIX = 31; // First prime smaller than 36
    private static final int MIN_UID = Integer.parseInt("000001", RADIX); // > "000000" (31)
    private static final int MAX_UID = Integer.parseInt("1000000", RADIX); // < "1000000" (31)
    private static final Random random = new Random();
    private static final String UID_MARK = "UID";
    private static final String NEWUID_VALUE = "NEWUID";
    private static final String LINK_MARK = "LINK";

    /**
     * Generate a new UId. Note this does do some storage I/O and is not thread
     * safe.
     *
     * @return New unique identifier which is a RADIX based integer between
     * MIN_UID and MAX_UID.
     */
    public static String generateUID() {
        int uid;
        do {
            uid = random.nextInt(MAX_UID - MIN_UID) + MIN_UID;
        } while (new File(LLMTreeData.DATA_DIR, Integer.toString(uid, 31)).exists());
        return Integer.toString(uid, RADIX);
    }

    /**
     * Check if the passed value is a valid UId.
     *
     * @param uidStr either a RADIX valid integer between MIN_UID and MAX_UID or
     * NEWUID_VALUE.
     * @return true if all is well
     */
    public static boolean isValidUID(String uidStr) {
        if (uidStr.equals(NEWUID_VALUE)) {
            return true;
        }
        try {
            int uid = Integer.parseInt(uidStr, 31);
            return uid > MIN_UID && uid < MAX_UID;
        } catch (NumberFormatException e) {
            Logger.getLogger(LLMTreeData.class.getName()).log(Level.SEVERE, "Invalid UID");
            return false;
        }
    }

    public static LLMTreeData parse(String raw) {
        LLMTreeData ret = new LLMTreeData();
        ret.data = raw;
        int mark = raw.indexOf(MARKER);
        while (mark >= 0) {
            int markEnd = raw.indexOf(MARKER, mark + MARKER.length());
            if (markEnd > mark) {
                int eoln = raw.indexOf("\n", markEnd);
                if (eoln < markEnd + MARKER.length()) {
                    eoln = markEnd + MARKER.length();
                }
                String field = raw.substring(mark + MARKER.length(), markEnd).trim();
                String fieldValue = raw.substring(markEnd + MARKER.length(), eoln).trim();
                switch (field) {
                    case UID_MARK:
                        ret.uid = fieldValue;
                        break;
                    case LINK_MARK:
                        ret.addLink(fieldValue);
                        break;
                    default: // leave alone
                        break;
                }
                mark = raw.indexOf(MARKER, eoln);
            }
        }
        ret.generateUIds();
        return ret;
    }

    /**
     * Get a record from storage by UId.
     *
     * @param uid UId to fetch.
     * @return Record.
     */
    public static LLMTreeData get(String uid) {
        int testUID = 0;
        if (isValidUID(uid)) {
            File f = new File(DATA_DIR, uid);
            if (!f.exists()) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            LLMTreeData ret = new LLMTreeData();
            try (BufferedReader bfr = new BufferedReader(new FileReader(f))) {
                for (String line = bfr.readLine(); null != line; line = bfr.readLine()) {
                    text.append(line).append(System.lineSeparator());
                    String[] nvp = getMarked(line);
                    if (0 == testUID && null != nvp && 2 == nvp.length
                            && nvp[0].equalsIgnoreCase(UID_MARK)) {
                        testUID = Integer.parseInt(nvp[1], RADIX);
                    }
                    if (null != nvp && 2 == nvp.length
                            && nvp[0].equalsIgnoreCase(LINK_MARK)) {
                        ret.addLink(nvp[1]);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(LLMTreeData.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (testUID != Integer.parseInt(uid, RADIX)) {
                Logger.getLogger(LLMTreeData.class.getName()).log(Level.SEVERE,
                        "Data error, expected {0} but got {1}", new Object[]{uid, testUID});
                return null;
            }
            ret.data = text.toString();
            return ret;
        }
        return null;
    }

    /**
     * Checks and extracts marked fields from a string.
     *
     * @param line The string to be checked.
     * @return null if the line does not contain valid markers, NVP otherwise.
     */
    public static String[] getMarked(String line) {
        if (null != line) {
            int start = line.indexOf(MARKER);
            // allow some but not too much preamble (white space, quotes).
            if (start >= 0 && start < 4) {
                int end = line.indexOf(MARKER, start + MARKER.length());
                if (end > start) {
                    StringBuilder sb = new StringBuilder(line);
                    String fieldName = sb.substring(start + MARKER.length(), end);
                    String fieldValue = sb.delete(start, end + MARKER.length()).toString();
                    return new String[]{fieldName.trim(), fieldValue.trim()};
                }
            }
        }
        return null;
    }

    /**
     * Finds the next index of the marker in a string.
     *
     * @param line The string to be searched.
     * @param fromIndex The index to start the search from.
     * @return The index of the next occurrence of the marker, or -1 if it's not
     * found.
     */
    public static int nextIndexOfMarker(String line, int fromIndex) {
        if (line == null) {
            return -1;
        }
        return line.indexOf(MARKER, fromIndex);
    }
    public String uid;
    public String data;
    public String[] links = new String[0];

    /**
     * Add to the list of links.
     *
     * @param linkUid UId or NEWUID_VALUE to add.
     */
    public void addLink(String linkUid) {
        if (isValidUID(linkUid)) {
            String[] links2 = new String[links.length + 1];
            if (links.length > 0) {
                System.arraycopy(links, 0, links2, 0, links.length);
            }
            links2[links.length] = linkUid;
            links = links2;
        }
    }

    /**
     * Save the data.
     *
     * Do not get silly idea's of creating backups if the file already exists or
     * implement any fancy checking. The source is likely an LLM ... weird
     * things WILL happen.
     *
     * When those things become needed: use git first!
     */
    public void save() {
        File f = new File(DATA_DIR, uid);
        try (FileWriter wr = new FileWriter(f)) {
            wr.write(data);
        } catch (IOException ex) {
            Logger.getLogger(LLMTreeData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Find any occurrence of NEWUID_VALUE and replace by a generated UId.
     */
    public void generateUIds() {
        if (uid.toUpperCase().contains(NEWUID_VALUE)) {
            uid = generateUID();
        }
        for (int i = 0; i < links.length; i++) {
            if (links[i].toUpperCase().contains(NEWUID_VALUE)) {
                links[i] = generateUID();
            }
        }
        // This is not idiot proof: it only handles same-case occurences and
        // will not fix errors like MARKER+NEWUID+MARKER.
        // If you're reading this comment in a debugger, guess who is going to fix it.
        if (data.contains(NEWUID_VALUE)) {
            StringBuilder sb = new StringBuilder(data);
            int rep = sb.indexOf(NEWUID_VALUE);
            while (rep >= 0) {
                sb.delete(rep, rep + NEWUID_VALUE.length());
                sb.insert(rep, generateUID());
                rep = sb.indexOf(NEWUID_VALUE);
            }
            data = sb.toString();
        }
    }
}
