package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.ruleeditor.SoarDocument;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
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
    operDividerPosition("" + MainFrame.DEFAULT_OPER_DIV_POS),
    fbDividerPosition("" + MainFrame.DEFAULT_FB_DIV_POS),
    customTemplateFolder(""),
    //Filenames of last 5 .vsa files that have been loaded
    recentProj0(""),
    recentProj1(""),
    recentProj2(""),
    recentProj3(""),
    recentProj4(""),
    lastXPos(null),
    lastYPos(null),
    lastWidth(null),
    lastHeight(null);

    private static final Preferences preferences = Preferences.userRoot().node("edu/umich/soar/visualsoar");
    private static final SyntaxColor[] colors = SyntaxColor.getDefaultSyntaxColors();
    private static final Vector<String> customTemplates = new Vector<>();
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
        loadCustomTemplates();
    }

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
        if (!prefDirName.isEmpty()) {
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
        if (prefDirName.isEmpty()) {
            String sep = FileSystems.getDefault().getSeparator();
            prefDirName = System.getProperty("user.dir") + sep + ".java" + sep + "edu" + sep + "umich" + sep + "soar" + sep + "visualsoar" + sep;
            Prefs.customTemplateFolder.set(prefDirName);
        }

        return new File(prefDirName);
    }//getCustomTemplatesFolder

    /** loads all the custom templates */
    public static void loadCustomTemplates() {
        //Get a list of all template files (those with .vsoart extension)
        File prefDir = getCustomTemplatesFolder();
        String[] templates = prefDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".vsoart");
            }
        });

        //put the template names in Prefs.customTemplates
        if (templates != null) {

            customTemplates.clear();
            for (String template : templates) {
                //trim away the path and extension
                String name = template;
                int lastSlash = name.lastIndexOf(FileSystems.getDefault().getSeparator());
                name = name.substring(lastSlash + 1);
                int ext = name.lastIndexOf(".vsoart");
                name = name.substring(0, ext);

                //add to list
                customTemplates.add(name);
            }//for
        }//if
    }//loadCustomTemplates

    /** given the name of a custom template return the associated file.
     * This method doesn't guarantee the file exists or is accessible! */
    public static File getCustomTemplateFile(String name) {
        File prefsDir = getCustomTemplatesFolder();
        String filename = prefsDir + FileSystems.getDefault().getSeparator() + name + ".vsoart";
        return new File(filename);
    }//getCustomTemplateFile

    public static Vector<String> getCustomTemplates() {
        return new Vector<>(Prefs.customTemplates);
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
        if (newbie.isEmpty()) return null;
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



    /* helper method for getRecentProjs() and addRecentProject() below.  It verifies
       a given filename is the valid name of an existing .vsa file and adds it to a list. */
    private static void addIfValid(String filename, Vector<File> vec) {
        if ( (filename != null)
              && (filename.endsWith(".vsa"))
              && (!vec.contains(filename)) ) {

            //Make sure the new file still exists
            File newbieFile = new File(filename);
            if (!newbieFile.exists()) return;

            //Extract the canonical (unique) name of this file for comparison
            String newbieFN;
            try {
                newbieFN = newbieFile.getCanonicalPath();
            }
            catch(IOException ioe) {
                return; //invalid file is ignored (should not happen)
            }

            //compare to canonical name of existing files to see if this is a duplicate
            File dupOf = null;
            for(File prevFile : vec) {
                String prevFN;
                try {
                    prevFN = prevFile.getCanonicalPath();
                }
                catch(IOException ioe) {
                    continue; //skip this one
                }

                //If we've seen this file before exit
                if (newbieFN.equals(prevFN)) {
                    dupOf = prevFile;
                    break;
                }
            }

            //If it's a duplicate, then remove the old one so it's replaced by the new
            //(as a side effect this helpfully adds it to the top of the Open Recent menu list)
            vec.remove(dupOf);

            //all checks passed
            vec.add(newbieFile);
        }
    }//addIfValid


    /** retrieves a list of the most recently opened .vsa files */
    public static Vector<File> getRecentProjs() {
        Vector<File> recentProjs = new Vector<>();
        addIfValid(recentProj0.get(), recentProjs);
        addIfValid(recentProj1.get(), recentProjs);
        addIfValid(recentProj2.get(), recentProjs);
        addIfValid(recentProj3.get(), recentProjs);
        addIfValid(recentProj4.get(), recentProjs);
        return recentProjs;
    }//getRecentProjs

    /**
     * adds a new filename to the list of recent projects
     */
    public static void addRecentProject(String newbie) {
        //Make sure this file is valid and unique.
        Vector<File> recentProjs = getRecentProjs();
        addIfValid(newbie, recentProjs);

        //If there are too many projects, remove some
        while (recentProjs.size() > 5) {
            recentProjs.remove(0);
        }

        //Place the recent projects back into the prefs
        if (recentProjs.size() > 0) recentProj0.set(recentProjs.get(0).getPath());
        if (recentProjs.size() > 1) recentProj1.set(recentProjs.get(1).getPath());
        if (recentProjs.size() > 2) recentProj2.set(recentProjs.get(2).getPath());
        if (recentProjs.size() > 3) recentProj3.set(recentProjs.get(3).getPath());
        if (recentProjs.size() > 4) recentProj4.set(recentProjs.get(4).getPath());

        //Update the VisualSoar menu to reflect the change
        MainFrame.getMainFrame().updateRecentProjectsSubMenu();
    }//addRecentProject


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
