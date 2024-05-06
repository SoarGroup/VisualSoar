package edu.umich.soar.visualsoar.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;


/**
 * These are some utilities that I wrote to ease the reading in of files
 *
 * @author Brad Jones
 * @version 0.5a 5 Aug 1999
 */
public class ReaderUtils {
    // Deny Default Construction
    private ReaderUtils() {
    }

///////////////////////////////////////////////////////////////////
// Methods
///////////////////////////////////////////////////////////////////

    /**
     * a word is defined as non-whitespace characters terminated by a whitespace
     * character
     *
     * @param r the reader from which the user wants the word extracted
     * @param delim     a character that overrides whitespace.  This was added
     *                  to support datamap enumeration values that are
     *                  surrounded by pipes.
     *                  Example:  |this is treated as one word even though it has spaces|
     * @return a string that is a word as defined above
     * @throws IOException represents something went wrong reading the stream
     */
    public static String getWord(Reader r, char delim) throws IOException {
        StringBuilder s = new StringBuilder();
        boolean wordbegin = false;
        boolean wordend = false;
        boolean inDelim = false;

        // Get rid of leading whitespace
        while (!wordend && r.ready()) {
            char c = (char) r.read();
            if (!inDelim && Character.isWhitespace(c)) {
                if (wordbegin) {
                    wordend = true;
                }
            }
            else {
                if (!wordbegin) {
                    wordbegin = true;

                    //Notice if a word begins with a delimeter char
                    if(c == delim) {
                        inDelim = true;
                    }
                }

                //If the word began with a delim char then it must end with one
                else if ( inDelim && (c == delim)) {
                    wordend = true;
                    inDelim = false;
                }


                s.append(c);
            }
        }
        return s.toString();
    }

    /** helper version of {@link #getWord(Reader, char)} that has no delim char */
    public static String getWord(Reader r) throws IOException {
        return getWord(r, '\0');
    }

    /**
     * This gets an Integer out of the reader
     *
     * @param r the reader from which the user wants the integer extracted
     * @return an int that is the integer
     * @throws IOException           if there was an error reading from the reader
     * @throws NumberFormatException if the integer in the reader isn't properly formatted
     */
    public static int getInteger(Reader r) throws IOException, NumberFormatException {
        return Integer.parseInt(getWord(r));
    }

    /**
     * This gets a float out of the reader
     *
     * @param r the reader from which the user wants the float extracted
     * @return a float that is the number from the reader
     * @throws IOException           if there was an error reading from the reader
     * @throws NumberFormatException if the float in the reader isn't properly formatted
     */
    public static float getFloat(Reader r) throws IOException, NumberFormatException {
        String strFloat = getWord(r);
        if (strFloat.equals("-Infinity")) {
            return Float.NEGATIVE_INFINITY;
        } else if (strFloat.equals("Infinity")) {
            return Float.POSITIVE_INFINITY;
        } else {
            return Float.parseFloat(strFloat);
        }
    }

    /**
     * This gets a line out of the reader
     *
     * @param r the reader from which the user
     * @throws IOException if there was an error reading from the reader
     */

    public static String getLine(Reader r) throws IOException {
        StringWriter w = new StringWriter();
        int ch = r.read();
        while (ch != '\n' && ch != '\r' && ch != -1) {
            w.write(ch);
            ch = r.read();
        }
        if (ch == '\r') {
            r.read();
        }
        w.write("\n");
        return w.toString();
    }

    /**
     * This function takes all data from a reader, and writes it to a writer a byte at a time
     *
     * @param w the destination
     * @param r the source
     * @throws IOException if there was an error reading from the source or writing to the destination
     */

    public static void copy(Writer w, Reader r) throws IOException {
        int ch = r.read();
        while (ch != -1) {
            w.write(ch);
            ch = r.read();
        }
    }
}
