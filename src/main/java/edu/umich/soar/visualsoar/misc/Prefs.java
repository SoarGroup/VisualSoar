package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.ruleeditor.SoarDocument;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public enum Prefs {
    openFolder(System.getProperty("user.dir")),
    autoTileEnabled(true),
    horizTile(true),
    highlightingEnabled(true),
    autoIndentingEnabled(true),
    autoSoarCompleteEnabled(true),
    sharedProjectFile(null),
    sharedProjectEnabled(false),
    userName("User"),
    editorFontSize("" + SoarDocument.DEFAULT_FONT_SIZE),
    dividerPosition("" + MainFrame.DEFAULT_DIV_POS),
    customTemplateFolder("");

    private static final Preferences preferences = Preferences.userRoot().node("edu/umich/soar/visualsoar");
    private static final SyntaxColor[] colors = SyntaxColor.getDefaultSyntaxColors();
    private static Vector<String> customTemplates = new Vector<String>();
    //default custom template content
    public static final String defaultCustTempText = "# Any text you write in a template file is automatically inserted when\n# the template is selected from the Insert Template menu.  This\n# comment has been inserted to help you.  You should probably\n# remove it from this file after your template is written.\n#\n# Certain macros can be placed in your text and they will be\n# automatically replaced with the relevant text.\n#\n# The macros are:\n# - $super-operator$ = the super operator\n# - $operator$ = the current operator\n# - $production$ = the name of the production nearest to the cursor\n# - $date$ = today's date\n# - $time$ = the time right now\n# - $project$ or $agent$ = the name of the project\n# - $user$ = the current user name\n# - $caret$ or $cursor$ = indicates where where the cursor should be\n#   after the template is inserted (to be implemented...)\n#\n\nsp {$super-operator$*custom-template\n   (state <s> ^foo bar) \n-->\n   (<s> ^baz qux)\n}\n";


    //Load custom colors
    static {
        // start at 1 to skip DEFAULT
        for (int i = 1; i < colors.length; ++i) {
            if (colors[i] == null) {
                continue;
            }
            int rgb = preferences.getInt("syntaxColor" + i, colors[i].getRGB());
            colors[i] = new SyntaxColor(rgb, colors[i]);
        }
    }

    //Load custom templates
    static {
        //Get a list of all template files (those with .vsoart extension)
        File prefDir = getCustomTemplatesFolder();
        String[] templates = prefDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".vsoart");
            }
        });

        if (templates != null) {

            int len = templates.length;
            customTemplates.clear();
            for (int i = 0; i < len; ++i) {
                //trim away the path and extension
                String name = templates[i];
                int lastSlash = name.lastIndexOf(System.getProperty("file.separator"));
                name = name.substring(lastSlash + 1);
                int ext = name.lastIndexOf(".vsoart");
                name = name.substring(0, ext);

                //add to list
                customTemplates.add(name);
            }//for
        }//if
    }//load custom templates

    public static SyntaxColor[] getSyntaxColors() {
        SyntaxColor[] temp = new SyntaxColor[colors.length];
        System.arraycopy(colors, 0, temp, 0, colors.length);
        return temp;
    }

    public static void setSyntaxColors(SyntaxColor[] colors) {
        SyntaxColor[] defaults = SyntaxColor.getDefaultSyntaxColors();

        // start at 1 to skip DEFAULT
        for (int i = 1; i < colors.length; ++i) {
            if (colors[i] == null) {
                if (defaults[i] != null) {
                    colors[i] = defaults[i];
                } else {
                    continue;
                }
            } else {
                continue;
            }
            Prefs.colors[i] = colors[i];

            preferences.putInt("syntaxColor" + i, colors[i].getRGB());
        }
    }

    public static File getCustomTemplatesFolder() {
        //Verify the folder exists and can be accessed
        String prefDirName = Prefs.customTemplateFolder.get();
        if (prefDirName.length() != 0) {
            File prefDir = new File(prefDirName);
            if (! prefDir.exists()) {
                if (!prefDir.mkdirs()) {
                    prefDirName = ""; //reset so default will be used (below)
                }
            }
            //Also reset if not a directory or not accessible
            if (! prefDir.isDirectory()) {
                prefDirName = "";
            }
            if (! prefDir.canWrite()) {
                prefDirName = "";
            }
        }

        //If folder is not set, use default location
        //(If this isn't accessible then we're screwed so just trust...)
        if (prefDirName.length() == 0) {
            String sep = System.getProperty("file.separator");
            prefDirName = System.getProperty("user.dir") + sep + ".java" + sep + "edu" + sep + "umich" + sep + "soar" + sep + "visualsoar" + sep;
            Prefs.customTemplateFolder.set(prefDirName);
        }

        return new File(prefDirName);
    }//getCustomTemplatesFolder

    /** given the name of a custom template return the associated file.
     * This method doesn't guarantee the file exists or is accessible! */
    public static File getCustomTemplateFile(String name) {
        File prefsDir = getCustomTemplatesFolder();
        String filename = prefsDir + System.getProperty("file.separator") + name + ".vsoart";
        return new File(filename);
    }//getCustomTemplateFile

    public static Vector<String> getCustomTemplates() {
        Vector<String> result = new Vector<>(Prefs.customTemplates);
        return result;
    }

    /**
     * adds a new custom template.  If the associated file does not exist, it is created
     * and default content is added to it.
     *
     * @return a File object referencing the new template; or null on failure
     */
    public static File addCustomTemplate(String newbie) {
        //Add the new template
        if (newbie == null) return null;
        if (newbie.length() == 0) return null;
        if (Prefs.customTemplates.contains(newbie)) return null;

        //If the file doesn't exist (it shouldn't), create it
        File newbieFile = getCustomTemplateFile(newbie);
        if (!newbieFile.exists()) {
            //create it
            try {
                newbieFile.createNewFile();

                //Add some default content to help the user
                PrintWriter pw = new PrintWriter(newbieFile);
                pw.print(defaultCustTempText);
                pw.close();
            }
            catch(IOException ioe) {
                return null;
            }
        }//file can't be created

        //Success
        Prefs.customTemplates.add(newbie);
        return newbieFile;
    }//addCustomTemplate

    public static void removeCustomTemplate(String removeMe) {
        //remove the template
        if (! customTemplates.contains(removeMe)) return;
        customTemplates.remove(removeMe);

        //delete the file
        File removeFile = getCustomTemplateFile(removeMe);
        removeFile.delete();
    }//removeCustomTemplate

    private final String def;
    private final boolean defBoolean;

    Prefs(String def) {
        this.def = def;
        this.defBoolean = false;
    }

    Prefs(boolean def) {
        this.def = null;
        this.defBoolean = def;
    }

    public String get() {
        return preferences.get(this.toString(), def);
    }

    public void set(String value) {
        preferences.put(this.toString(), value);
    }

    public boolean getBoolean() {
        return preferences.getBoolean(this.toString(), defBoolean);
    }

    public void setBoolean(boolean value) {
        preferences.putBoolean(this.toString(), value);
    }

    public static void flush() {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

}
