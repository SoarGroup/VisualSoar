package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.MainFrame;
import edu.umich.soar.visualsoar.ruleeditor.SoarDocument;

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
    numCustomTemplates("" + 0);

    private static final Preferences preferences = Preferences.userRoot().node("edu/umich/soar/visualsoar");
    private static final SyntaxColor[] colors = SyntaxColor.getDefaultSyntaxColors();
    private static Vector<String> customTemplates = new Vector<String>();

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
        int len = Integer.parseInt(numCustomTemplates.get());
        for(int i = 0; i < len; ++i) {
            String customFN = preferences.get("customTemplate" + i, null);
            if (customFN == null) continue;
            customTemplates.add(customFN);
        }
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

    public static Vector<String> getCustomTemplates() {
        Vector<String> result = new Vector<>(Prefs.customTemplates);
        return result;
    }

    public static void addCustomTemplate(String newbie) {
        //Add the new template
        if (newbie == null) return;
        if (newbie.length() == 0) return;
        if (Prefs.customTemplates.contains(newbie)) return;
        Prefs.customTemplates.add(newbie);

        //Increment the count
        int len = Integer.parseInt(numCustomTemplates.get());
        len++;
        numCustomTemplates.set("" + len);
    }

    public static void removeCustomTemplate(String removeMe) {
        //remove the template
        if (! customTemplates.contains(removeMe)) return;
        customTemplates.remove(removeMe);

        //decrement the count
        int len = Integer.parseInt(numCustomTemplates.get());
        len--;
        if (len < 0) len = 0;  //should never happen
        numCustomTemplates.set("" + len);
    }//removeCustomTemplate

    public static void saveCustomTemplates() {
        //If templates have been removed then corresponding entries need to be removed
        int len = customTemplates.size();
        int index = len;
        while(true) {
            String key = "customTemplate" + index;
            String val = Prefs.preferences.get(key, null);
            if (val == null) break;
            Prefs.preferences.remove(key);
            index++;
        }

        //make sure the count is right
        numCustomTemplates.set("" + len); //just in case we're out of sync

        //save the custom templates
        for(int i = 0; i < len; ++i) {
            String key = "customTemplate" + i;
            Prefs.preferences.put(key, customTemplates.get(i));
        }
    }//saveCustomTemplates

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
            saveCustomTemplates();
            preferences.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

}
