package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converts the text of a property into {@link Value} (reference/common/values.md).
 *
 * <ul>
 *   <li>Lengths with a decimal point: in points, the number alone ({@code 12}, {@code 12.5}, {@code -4}), or in
 *   millimeters, with the unit attached ({@code 12mm}, {@code -4.5mm}).</li>
 *   <li>Percentages ({@code 50%}) and fractions ({@code *}, {@code 2*}; {@code *} alone is {@code 1*}).</li>
 *   <li>Words: {@code auto}, {@code narrowest}, {@code natural}, {@code fit}.</li>
 *   <li>Tracks between two sizes: {@code between(a, b)}.</li>
 *   <li>Lists separated by spaces, with {@code repeat(n, pattern)}.</li>
 * </ul>
 */
public final class ValueParser {

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    /** A number followed by letters: a length with a unit. */
    private static final Pattern WITH_UNIT = Pattern.compile("-?\\d+(\\.\\d+)?\\p{IsAlphabetic}+");

    private static final String MILLIMETERS = "mm";

    private ValueParser() {
    }

    /** A single value. */
    public static Value value(String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Empty value");
        }
        switch (t) {
            case "auto":
                return Value.AUTO;
            case "narrowest":
                return Value.Keyword.NARROWEST;
            case "natural":
                return Value.Keyword.NATURAL;
            case "fit":
                return Value.Keyword.FIT;
            default:
                break;
        }
        int paren = t.indexOf('(');
        if (paren > 0 && t.endsWith(")")) {
            return function(t.substring(0, paren).trim(),
                    t.substring(paren + 1, t.length() - 1), t);
        }
        if (t.endsWith("%")) {
            return new Value.Percentage(number(t.substring(0, t.length() - 1), text));
        }
        if (t.endsWith("*")) {
            String weight = t.substring(0, t.length() - 1);
            double fraction = weight.isEmpty() ? 1 : number(weight, text);
            if (fraction < 0) {
                throw new IllegalArgumentException("A fraction cannot be negative: " + text);
            }
            return new Value.Fraction(fraction);
        }
        if (t.endsWith(MILLIMETERS)) {
            String amount = t.substring(0, t.length() - MILLIMETERS.length());
            if (amount.isEmpty() || Character.isWhitespace(amount.charAt(amount.length() - 1))) {
                throw new IllegalArgumentException("Invalid value: " + t
                        + " (the unit goes attached to the number, as in 12mm)");
            }
            return Value.mm(number(amount, text));
        }
        if (WITH_UNIT.matcher(t).matches()) {
            throw new IllegalArgumentException("Invalid value: " + t
                    + " (a length is a number of points, as in 12, or of millimeters, as in 12mm)");
        }
        return new Value.Points(number(t, text));
    }

    /** A number without unit, with a decimal point: a factor such as {@code Grow}, not a length. */
    public static double factor(String text) {
        return number(text, text);
    }

    /** List of values separated by spaces; expands {@code repeat(n, pattern)}. */
    public static List<Value> list(String text) {
        List<Value> result = new ArrayList<>();
        for (String token : tokens(text)) {
            if (token.startsWith("repeat(") && token.endsWith(")")) {
                List<String> args = arguments(token.substring("repeat(".length(), token.length() - 1));
                if (args.size() != 2 || !args.get(0).trim().matches("\\d+")) {
                    throw new IllegalArgumentException("Expected repeat(n, pattern): " + token);
                }
                int times = Integer.parseInt(args.get(0).trim());
                List<Value> pattern = list(args.get(1));
                for (int i = 0; i < times; i++) {
                    result.addAll(pattern);
                }
            } else {
                result.add(value(token));
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Empty list of values");
        }
        return result;
    }

    private static Value function(String name, String inner, String original) {
        List<Value> args = arguments(inner).stream().map(ValueParser::value).toList();
        switch (name) {
            case "between":
                require(args.size() == 2, "between needs 2 arguments: " + original);
                require(!(args.get(0) instanceof Value.Fraction), "The minimum of between cannot be a fraction: " + original);
                return new Value.Between(args.get(0), args.get(1));
            default:
                throw new IllegalArgumentException("Unknown function: " + name);
        }
    }

    /** Splits on white space outside parentheses. */
    static List<String> tokens(String text) {
        return split(text, ' ');
    }

    /** Splits on commas outside parentheses. */
    static List<String> arguments(String text) {
        return split(text, ',');
    }

    private static List<String> split(String text, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (char c : text.toCharArray()) {
            boolean isSeparator = separator == ' ' ? Character.isWhitespace(c) : c == separator;
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("Unbalanced parentheses: " + text);
                }
            }
            if (isSeparator && depth == 0) {
                if (!current.toString().isBlank()) {
                    parts.add(current.toString().trim());
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (depth != 0) {
            throw new IllegalArgumentException("Unbalanced parentheses: " + text);
        }
        if (!current.toString().isBlank()) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    private static double number(String s, String original) {
        String t = s.trim();
        if (!NUMBER.matcher(t).matches()) {
            throw new IllegalArgumentException("Invalid value: " + original.trim()
                    + " (the decimal separator is a point, for example 12.5)");
        }
        return Double.parseDouble(t);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
