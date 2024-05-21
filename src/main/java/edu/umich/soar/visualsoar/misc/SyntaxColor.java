package edu.umich.soar.visualsoar.misc;

import edu.umich.soar.visualsoar.parser.SoarParserConstants;

import java.awt.*;

public class SyntaxColor extends Color {
    private static final long serialVersionUID = 20221225L;

    /**
     * This array must be large enough to guarantee there is an entry for
     * each constant.  Making it extra big like this wastes a little RAM but
     * bypasses crashes that can occur if you try to be efficient.
     * -Nuxoll 21 May 2024
     */
    public static final int SH_ARRAY_SIZE = SoarParserConstants.tokenImage.length;

    public static SyntaxColor[] getDefaultSyntaxColors() {
        SyntaxColor[] temp = new SyntaxColor[SH_ARRAY_SIZE];

        temp[SoarParserConstants.RARROW] = new SyntaxColor(Color.red, "\"-->\"");
        temp[SoarParserConstants.SP] = new SyntaxColor(Color.red, "\"sp\"");
        temp[SoarParserConstants.GP] = new SyntaxColor(Color.decode("-65332"), "\"gp\"");
        temp[SoarParserConstants.CARET] = new SyntaxColor(Color.orange.darker(), "Literal Attributes");
        temp[SoarParserConstants.VARIABLE] = new SyntaxColor(Color.green.darker(), "Variables");
        temp[SoarParserConstants.SYMBOLIC_CONST] = new SyntaxColor(Color.blue, "Symbolic Constants");
        temp[SoarParserConstants.DEFAULT] = new SyntaxColor(Color.black); // default

        return temp;
    }

    String name = null;

    public SyntaxColor(Color c) {
        super(c.getRGB());
    }

    public SyntaxColor(int rgb, String str) {
        super(rgb);

        name = str;
    }

    public SyntaxColor(Color c, String str) {
        this(c.getRGB(), str);
    }

    public SyntaxColor(int rgb, SyntaxColor old) {
        this(rgb, old.getName());
    }

    public SyntaxColor(Color c, SyntaxColor old) {
        this(c.getRGB(), old.getName());
    }

    /**
     * Returns the string identifying the SyntaxColor
     *
     * @return the name String, null if there is none
     */
    public String getName() {
        return name;
    }

    public boolean equals(String s) {
        return (name != null && name.equals(s));
    }
}
