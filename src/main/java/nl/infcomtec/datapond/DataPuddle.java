/*
 * Copyright (c) 2024 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.datapond;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DataPond as an object, just encapsulates the directory and model.
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
            return DataPond.fromImage(workDir, img);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public DataDrop from(String text) {
        try {
            return DataPond.fromText(workDir, text);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public DataDrop from(File imgFile) {
        try {
            return DataPond.fromImageFile(workDir, imgFile);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public DataDrop summary(String text) {
        try {
            return DataPond.summary(workDir, model, text);
        } catch (Exception ex) {
            Logger.getLogger(DataPuddle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
