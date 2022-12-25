package edu.umich.soar.visualsoar.misc;

import java.io.File;
import java.io.FilenameFilter;

/**
 * This class describes the filter of a JFileChooser
 *
 * @author Brad Jones & Jon Bauman
 * @version 0.5a 6 15 1999
 */
public class SoarFileFilter extends javax.swing.filechooser.FileFilter implements FilenameFilter {
    /**
     * Will only accept files with a .vsa extension
     *
     * @return true if the file ends with vsa or is a folder false otherwise
     */
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String suffix = getSuffix(f);
        if (suffix != null) {
            return suffix.equals("vsa");
        }
        return false;
    }

    /**
     * Same as the other accept method, but included for compatibility with the FilenameFilter
     * interface used for native dialogs
     *
     * @return true if the file ends with vsa or is a folder false otherwise
     */
    public boolean accept(File dir, String name) {
        return name.endsWith(".vsa");
    }

    /**
     * @return a string describing the filefilter
     */
    public String getDescription() {
        return "Visual Soar Agent (*.vsa)";
    }

    /**
     * Gets a suffix off a file
     *
     * @param f the file to examine
     * @return the suffix of the file
     */
    private String getSuffix(File f) {
        String s = f.getPath(), suffix = null;

        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            suffix = s.substring(i + 1).toLowerCase();
        }

        return suffix;
    }
}
