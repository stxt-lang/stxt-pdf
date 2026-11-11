package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.Nodes.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.stxt.InlineNode;

/**
 * The data of a GS1 {@code Barcode} (reference/components/barcode.md, {@code gs1-128} and {@code gs1-data-matrix}):
 * a list of element strings, each one an application identifier in parentheses and its value, as they are read on a
 * label: {@code (01)09501101530003(17)261231(10)AB-123}. The parentheses are not encoded. The symbol starts with the
 * function character FNC1 and has another one after each value of variable length that is not the last.
 *
 * <p>What is checked: the form, the characters of the values, the length of the values of the identifiers of
 * predefined length (GS1 General Specifications, figure 7.8.5-1) and the check digit of an SSCC and a GTIN
 * ({@code 00}, {@code 01} and {@code 02}). The format of the other identifiers is not.</p>
 */
final class Gs1 {

    /** Longest value of an identifier. */
    static final int MAX_VALUE = 90;

    /**
     * The identifiers of predefined length, by their first two digits: the digits of the identifier and those of its
     * value. No FNC1 follows them.
     */
    private static final Map<String, int[]> PREDEFINED = Map.ofEntries(
            fixed("00", 2, 18), fixed("01", 2, 14), fixed("02", 2, 14), fixed("03", 2, 14), fixed("04", 2, 16),
            fixed("11", 2, 6), fixed("12", 2, 6), fixed("13", 2, 6), fixed("14", 2, 6), fixed("15", 2, 6),
            fixed("16", 2, 6), fixed("17", 2, 6), fixed("18", 2, 6), fixed("19", 2, 6), fixed("20", 2, 2),
            fixed("31", 4, 6), fixed("32", 4, 6), fixed("33", 4, 6), fixed("34", 4, 6), fixed("35", 4, 6),
            fixed("36", 4, 6), fixed("41", 3, 13));

    /** The characters of a value besides digits and letters (GS1 character set 82), without the parentheses. */
    private static final String PUNCTUATION = "!\"%&'*+,-./:;<=>?_";

    /**
     * An element string.
     *
     * @param id         the application identifier, 2 to 4 digits
     * @param value      its value
     * @param predefined whether its length is predefined, so that no FNC1 follows it
     */
    record Element(String id, String value, boolean predefined) {
    }

    private Gs1() {
    }

    private static Map.Entry<String, int[]> fixed(String prefix, int idDigits, int valueDigits) {
        return Map.entry(prefix, new int[] {idDigits, valueDigits});
    }

    /**
     * The element strings of {@code data}.
     *
     * @throws InvalidModelException {@code BARCODE_DATA}, with the line of {@code n}, if it is not a list of them
     */
    static List<Element> parse(InlineNode n, String data) {
        List<Element> elements = new ArrayList<>();
        int i = 0;
        while (i < data.length()) {
            int close = data.indexOf(')', i);
            if (data.charAt(i) != '(' || close < 0) {
                throw error(n, ErrorCode.BARCODE_DATA, "The data of a GS1 Barcode is a list of application "
                        + "identifiers in parentheses, each one before its value, as in (01)09501101530003(10)AB-123, "
                        + "and this one is " + data);
            }
            String id = data.substring(i + 1, close);
            if (!id.matches("[0-9]{2,4}")) {
                throw error(n, ErrorCode.BARCODE_DATA, "A GS1 application identifier is 2 to 4 digits, and the data "
                        + "of the Barcode has (" + id + ")");
            }
            int next = data.indexOf('(', close);
            String value = data.substring(close + 1, next < 0 ? data.length() : next);
            elements.add(element(n, id, value));
            i = close + 1 + value.length();
        }
        return elements;
    }

    private static Element element(InlineNode n, String id, String value) {
        if (value.isEmpty()) {
            throw error(n, ErrorCode.BARCODE_DATA, "The GS1 application identifier (" + id + ") of the Barcode has "
                    + "no value after it");
        }
        int[] lengths = PREDEFINED.get(id.substring(0, 2));
        if (lengths == null) {
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                boolean allowed = c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                        || PUNCTUATION.indexOf(c) >= 0;
                if (!allowed) {
                    throw error(n, ErrorCode.BARCODE_DATA, "The value of the GS1 application identifier (" + id
                            + ") of the Barcode has " + Barcodes.describe(value.codePointAt(i)) + ": it allows "
                            + "digits, unaccented letters and " + PUNCTUATION);
                }
            }
            if (value.length() > MAX_VALUE) {
                throw error(n, ErrorCode.BARCODE_DATA, "The value of the GS1 application identifier (" + id
                        + ") of the Barcode has " + value.length() + " characters, and at most " + MAX_VALUE
                        + " are allowed");
            }
            return new Element(id, value, false);
        }
        if (id.length() != lengths[0]) {
            throw error(n, ErrorCode.BARCODE_DATA, "The GS1 application identifiers that start with "
                    + id.substring(0, 2) + " have " + lengths[0] + " digits, and the data of the Barcode has (" + id
                    + ")");
        }
        if (!value.matches("[0-9]{" + lengths[1] + "}")) {
            throw error(n, ErrorCode.BARCODE_DATA, "The value of the GS1 application identifier (" + id
                    + ") is " + lengths[1] + " digits, and the data of the Barcode has " + value);
        }
        if (id.equals("00") || id.equals("01") || id.equals("02")) {
            int check = Barcodes.checkDigit(value.substring(0, value.length() - 1));
            char last = value.charAt(value.length() - 1);
            if (last - '0' != check) {
                throw error(n, ErrorCode.BARCODE_DATA, "The check digit of (" + id + ")" + value + " is " + check
                        + ", not " + last);
            }
        }
        return new Element(id, value, true);
    }

    /**
     * What the symbol encodes: {@code fnc1}, and each identifier with its value, with another {@code fnc1} after a
     * value of variable length that is not the last.
     *
     * @param first whether the encoded data starts with {@code fnc1}; if not, the encoder adds it
     */
    static String encoded(List<Element> elements, char fnc1, boolean first) {
        StringBuilder sb = new StringBuilder();
        if (first) {
            sb.append(fnc1);
        }
        for (int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);
            sb.append(e.id()).append(e.value());
            if (!e.predefined() && i < elements.size() - 1) {
                sb.append(fnc1);
            }
        }
        return sb.toString();
    }

    /** What is read under the bars: each identifier in parentheses before its value. */
    static String readable(List<Element> elements) {
        StringBuilder sb = new StringBuilder();
        for (Element e : elements) {
            sb.append('(').append(e.id()).append(')').append(e.value());
        }
        return sb.toString();
    }
}
