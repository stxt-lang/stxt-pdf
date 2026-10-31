package dev.stxt.pdf.layout;

import java.util.Locale;

/** Common pieces of the layout error messages ({@link Overflow}, {@code TableLayout}). */
public final class Messages {

    private Messages() {
    }

    /** Maximum number of characters of a document text quoted in a message. */
    private static final int EXCERPT = 40;

    /**
     * A document text to quote in a message: whole if it is short and, if not, its first characters with the
     * total length, so that the message does not grow with the text.
     */
    public static String excerpt(String text) {
        int length = text.codePointCount(0, text.length());
        if (length <= EXCERPT) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, EXCERPT)) + "... (" + length + " characters)";
    }

    /** A measure in points with at most two decimals and no trailing zeros: {@code 12.5}, {@code 100}. */
    public static String pt(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.replaceAll("\\.?0+$", "");
    }
}
