package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.parse;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import dev.stxt.Node;
import dev.stxt.pdf.model.Corners;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.model.ValueParser;

/**
 * Parses the value of a property (reference/common/values.md and reference/common/structure.md) and checks what it
 * allows: lengths, {@code auto}, intrinsic keywords, grid tracks, integers, colors... Errors carry the line of the
 * node.
 */
final class ValueReader {

    /**
     * Value classes, to check what each property allows (reference/common/values.md and
     * reference/common/structure.md).
     */
    enum Kind { LENGTH, AUTO, INTRINSIC, FRACTION, BETWEEN }

    static final Set<Kind> SIZE = EnumSet.of(Kind.LENGTH, Kind.AUTO, Kind.INTRINSIC);
    static final Set<Kind> LENGTH_OR_AUTO = EnumSet.of(Kind.LENGTH, Kind.AUTO);
    static final Set<Kind> LENGTH = EnumSet.of(Kind.LENGTH);

    private ValueReader() {
    }

    /** A value of an allowed class; without {@code negative}, no negative numbers. */
    static Value value(Node p, Set<Kind> allowed, boolean negative) {
        Value v = parse(p, () -> ValueParser.value(p.getText()));
        check(p, v, allowed, negative);
        return v;
    }

    static Sides<Value> sides(Node p, Set<Kind> allowed, boolean negative) {
        List<Value> list = parse(p, () -> ValueParser.list(p.getText()));
        list.forEach(v -> check(p, v, allowed, negative));
        return parse(p, () -> Sides.shorthand(list));
    }

    static void check(Node p, Value v, Set<Kind> allowed, boolean negative) {
        Kind kind = kind(v);
        if (kind == null || !allowed.contains(kind)) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " does not allow " + p.getText().trim()
                    + ": it allows " + describe(allowed));
        }
        if (!negative && isNegative(v)) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " does not allow negative values: " + p.getText().trim());
        }
    }

    /** Grid tracks: lengths, {@code auto}, {@code narrowest}, {@code natural}, fractions and {@code between}. */
    static List<Value> tracks(Node p) {
        List<Value> list = parse(p, () -> ValueParser.list(p.getText()));
        for (Value v : list) {
            if (v == Value.Keyword.FIT) {
                throw error(p, ErrorCode.FIT_TRACK, "fit is not a grid track: the one that fits the content "
                        + "is auto");
            }
            boolean valid = v instanceof Value.Between b
                    ? track(b.min()) && (track(b.max()) || kind(b.max()) == Kind.FRACTION)
                    : track(v) || kind(v) == Kind.FRACTION;
            if (!valid) {
                throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " does not allow " + v + " as a track: it allows "
                        + "lengths, percentages, fractions (*), auto, narrowest, natural or between(a, b)");
            }
            if (isNegative(v)) {
                throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " does not allow negative tracks: " + p.getText().trim());
            }
        }
        return list;
    }

    private static boolean track(Value v) {
        Kind k = kind(v);
        return k == Kind.LENGTH || k == Kind.AUTO || (k == Kind.INTRINSIC && v != Value.Keyword.FIT);
    }

    /** Class of a value. */
    private static Kind kind(Value v) {
        if (v instanceof Value.Points || v instanceof Value.Percentage) {
            return Kind.LENGTH;
        }
        if (v instanceof Value.Fraction) {
            return Kind.FRACTION;
        }
        if (v instanceof Value.Between) {
            return Kind.BETWEEN;
        }
        return v.isAuto() ? Kind.AUTO : Kind.INTRINSIC;
    }

    private static String describe(Set<Kind> allowed) {
        return allowed.stream().map(k -> switch (k) {
            case LENGTH -> "a length or a percentage";
            case AUTO -> "auto";
            case INTRINSIC -> "narrowest, natural or fit";
            case FRACTION -> "a fraction";
            case BETWEEN -> "between(a, b)";
        }).collect(Collectors.joining("; "));
    }

    private static boolean isNegative(Value v) {
        if (v instanceof Value.Points p) {
            return p.pt() < 0;
        }
        if (v instanceof Value.Percentage p) {
            return p.percent() < 0;
        }
        return parts(v).stream().anyMatch(ValueReader::isNegative);
    }

    /** The value depends on a percentage base (directly or inside {@code between}). */
    static boolean hasPercentage(Value v) {
        return v instanceof Value.Percentage || parts(v).stream().anyMatch(ValueReader::hasPercentage);
    }

    /** The two ends of {@code between}. */
    private static List<Value> parts(Value v) {
        return v instanceof Value.Between m ? List.of(m.min(), m.max()) : List.of();
    }

    static boolean comparable(Value a, Value b) {
        return (a instanceof Value.Points && b instanceof Value.Points)
                || (a instanceof Value.Percentage && b instanceof Value.Percentage);
    }

    static double amount(Value v) {
        return v instanceof Value.Points p ? p.pt() : ((Value.Percentage) v).percent();
    }

    static <E extends Enum<E>> E enumValue(Class<E> type, Node n) {
        String text = n.getText().trim();
        try {
            // lower-alpha is LOWER_ALPHA: a hyphen cannot go in the name of a constant.
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Value not allowed in " + n.getName() + ": " + text);
        }
    }

    static boolean bool(Node n) {
        return switch (n.getText().trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes" -> true;
            case "false", "no" -> false;
            default -> throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Expected true or false: " + n.getText().trim());
        };
    }

    static int integer(Node n) {
        try {
            return Integer.parseInt(n.getText().trim());
        } catch (NumberFormatException ex) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Expected an integer: " + n.getText().trim());
        }
    }

    /**
     * {@code Rotate} (reference/common/layers-and-rotation.md): degrees between −360 and 360, without 0 or the whole
     * turns, which would not rotate anything.
     */
    static double angle(Node n) {
        double v;
        try {
            v = Double.parseDouble(n.getText().trim());
        } catch (NumberFormatException ex) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Expected a number of degrees: " + n.getText().trim());
        }
        if (v == 0 || Math.abs(v) >= 360) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Rotate allows degrees between -360 and 360, except 0, 360 and -360, "
                    + "which do not rotate anything: " + n.getText().trim());
        }
        return v;
    }

    /**
     * {@code Rotate origin} (reference/common/layers-and-rotation.md): the horizontal and the vertical origin, from the
     * top left corner of the border box.
     */
    static Value[] rotateOrigin(Node n) {
        try {
            return Rotation.origin(n.getText());
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, ex.getMessage());
        }
    }

    /** BCP 47 language tag (simple form): {@code es}, {@code en-US}, {@code ca}... */
    private static final java.util.regex.Pattern LANGUAGE = java.util.regex.Pattern
            .compile("[a-zA-Z]{2,3}(-[a-zA-Z0-9]{1,8})*");

    /** The value of {@code Language}: a BCP 47 language tag. */
    static String language(Node n) {
        String tag = n.getText().trim();
        if (!LANGUAGE.matcher(tag).matches()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Language expects a BCP 47 language tag "
                    + "(es, en-US, ca...): '" + tag + "'");
        }
        return tag;
    }

    /** {@code Weight} (reference/common/typography.md): from 100 to 900 in steps of 100. */
    static FontWeight weight(Node n) {
        int v = integer(n);
        if (v < 100 || v > 900 || v % 100 != 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Weight allows 100 to 900 in steps of 100: " + v);
        }
        return new FontWeight(v);
    }

    static int positiveInteger(Node n) {
        int v = integer(n);
        if (v < 1) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " must be at least 1");
        }
        return v;
    }

    static double points(Node n) {
        Value v = parse(n, () -> ValueParser.value(n.getText()));
        if (!(v instanceof Value.Points p) || p.pt() <= 0) {
            throw error(n, ErrorCode.PAGE_SIZE, n.getName() + " of the medium must be a positive length in points or "
                    + "millimeters: " + n.getText().trim());
        }
        return p.pt();
    }

    /** A length, positive, zero or negative, in points once read. */
    static double signedPoints(Node n) {
        Value v = parse(n, () -> ValueParser.value(n.getText()));
        if (!(v instanceof Value.Points p)) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " must be a length in points or millimeters: "
                    + n.getText().trim());
        }
        return p.pt();
    }

    static double positive(Node n) {
        double v = nonNegative(n);
        if (v == 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " must be greater than 0");
        }
        return v;
    }

    static double nonNegative(Node n) {
        Value v = parse(n, () -> ValueParser.value(n.getText()));
        if (!(v instanceof Value.Points p) || p.pt() < 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " must be a non-negative length: " + n.getText().trim());
        }
        return p.pt();
    }

    /** A factor without unit ({@code Grow}, {@code Shrink}): a non-negative number, never a length. */
    static double factor(Node n) {
        double v = parse(n, () -> ValueParser.factor(n.getText()));
        if (v < 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " must be a non-negative number: " + n.getText().trim());
        }
        return v;
    }

    static Sides<Value> nonNegativeSides(Node n, boolean allowsPercentage) {
        Sides<Value> sides = parse(n, () -> Sides.shorthand(ValueParser.list(n.getText())));
        for (Value v : sides.asList()) {
            boolean valid = (v instanceof Value.Points p && p.pt() >= 0)
                    || (allowsPercentage && v instanceof Value.Percentage q && q.percent() >= 0);
            if (!valid) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + (allowsPercentage
                        ? " only allows non-negative lengths or percentages"
                        : " only allows non-negative lengths"));
            }
        }
        return sides;
    }

    static Double aspectRatio(Node n) {
        String t = n.getText().trim();
        try {
            if (t.contains("/")) {
                String[] p = t.split("/");
                double r = Double.parseDouble(p[0].trim()) / Double.parseDouble(p[1].trim());
                if (r > 0 && Double.isFinite(r)) {
                    return r;
                }
            } else {
                double r = Double.parseDouble(t);
                if (r > 0) {
                    return r;
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            // reported below
        }
        throw error(n, ErrorCode.VALUE_SYNTAX, "Aspect ratio expects 'width/height' or a positive number: " + t);
    }

    /** A color: {@code #...} or the name of a {@code Named color} ({@link NamedColors}). */
    static String color(Node n, NamedColors names) {
        return names.resolve(n, n.getText().trim(), "");
    }

    /** {@code Border color}: from 1 to 4 colors, top, right, bottom, left. */
    static Sides<String> colorSides(Node n, NamedColors names) {
        String[] parts = n.getText().trim().split("\\s+");
        if (parts.length > 4) {
            throw error(n, ErrorCode.VALUE_SYNTAX, n.getName() + " allows from 1 to 4 colors and there are " + parts.length);
        }
        List<String> colors = new java.util.ArrayList<>();
        for (String part : parts) {
            colors.add(names.resolve(n, part, ""));
        }
        return Sides.shorthand(colors);
    }

    /**
     * {@code Background}: a color or {@code linear(angle, color, color...)}, with the angle in degrees (0
     * upward, 90 to the right, 180 downward) and at least two colors. Each color can be a name.
     */
    static Paint paint(Node n, NamedColors names) {
        String t = n.getText().trim();
        if (!t.startsWith("linear(")) {
            return new Paint.Solid(color(n, names));
        }
        if (!t.endsWith(")")) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "linear( is not closed: " + t);
        }
        String[] args = t.substring("linear(".length(), t.length() - 1).split(",", -1);
        if (args.length < 3) {
            throw error(n, ErrorCode.VALUE_SYNTAX,
                    "linear expects an angle and at least two colors: linear(180, #FFFFFF00, #FFFFFF)");
        }
        double angle;
        try {
            angle = Double.parseDouble(args[0].trim());
        } catch (NumberFormatException e) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "The angle of linear is a number of degrees: " + args[0].trim());
        }
        if (!Double.isFinite(angle)) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "The angle of linear is a number of degrees: " + args[0].trim());
        }
        List<String> stops = new java.util.ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            stops.add(names.resolve(n, args[i].trim(), " in linear"));
        }
        return new Paint.Linear(angle, stops);
    }

    /**
     * {@code Corner radius}: from 1 to 4 radii (non-negative points or percentages), top left, top right, bottom right,
     * bottom left.
     */
    static Corners<Value> radius(Node n) {
        List<Value> list = parse(n, () -> ValueParser.list(n.getText()));
        if (list.isEmpty() || list.size() > 4) {
            throw error(n, ErrorCode.VALUE_SYNTAX, n.getName() + " allows from 1 to 4 values and there are " + list.size());
        }
        for (Value v : list) {
            boolean valid = (v instanceof Value.Points p && p.pt() >= 0)
                    || (v instanceof Value.Percentage q && q.percent() >= 0);
            if (!valid) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " only allows non-negative lengths or percentages");
            }
        }
        return parse(n, () -> Corners.shorthand(list));
    }

    static String fontFamily(Node n) {
        String family = n.getText().trim();
        if (family.isEmpty()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " needs the name of a family");
        }
        return family;
    }
}
