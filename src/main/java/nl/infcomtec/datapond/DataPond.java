/*
 * Copyright (c) 2024 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.datapond;

import java.io.File;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds a collection of DataDrop objects.
 *
 * @author walter
 */
public class DataPond extends TreeMap<DataDrop, DataDrop> {

    /**
     * The pond.
     */
    public final File workDir;

    /**
     * Load all from the pond.
     *
     * @param workDir The pond.
     */
    public DataPond(File workDir) {
        this.workDir = workDir;
        load();
    }

    /**
     * Save all.
     * <ul>
     * <li>Does not delete removed elements.</li>
     * <li>Overwrites all files.</li>
     * </ul>
     */
    public final synchronized void save() {
        for (DataDrop dd : keySet()) {
            try {
                dd.save(workDir);
            } catch (Exception ex) {
                Logger.getLogger(DataPond.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * (Re)read all from the pond.
     */
    public final synchronized void load() {
        clear();
        File[] all = workDir.listFiles();
        if (null != all) {
            for (File f : all) {
                if (f.getName().toLowerCase().endsWith(".json")) {
                    try {
                        DataDrop dd = DataDrop.load(f);
                        put(dd, dd);
                    } catch (Exception ex) {
                        Logger.getLogger(DataPond.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Add (and save) an element.
     *
     * @param dd Element to add.
     */
    public final synchronized void add(DataDrop dd) {
        put(dd, dd);
        try {
            dd.save(workDir);
        } catch (Exception ex) {
            Logger.getLogger(DataPond.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Remove (and delete) an element.
     *
     * @param dd Element to delete.
     */
    public final synchronized void delete(DataDrop dd) {
        remove(dd);
        dd.delete(workDir);
    }
}
