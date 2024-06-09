/*
 * Copyright (c) 2024 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.datapond;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DataDrop as an object, just encapsulates the directory and model.
 *
 * @author walter
 */
public class DataPuddle {

    public final File workDir;
    public final String model;

    public DataPuddle(File workDir, String model) {
        this.workDir = workDir;
        this.model = model;
    }

    public DataDrop from(BufferedImage img) {
        try {
            return DataDrop.fromImage(workDir, img);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public DataDrop from(String text) {
        try {
            return DataDrop.fromText(workDir, text);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public DataDrop from(File imgFile) {
        try {
            return DataDrop.fromImageFile(workDir, imgFile);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public DataDrop summary(String text) {
        try {
            return DataDrop.summary(workDir, model, text);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
