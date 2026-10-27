package dev.stxt.boxes.tree;

import dev.stxt.Node;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.parse;

/**
 * Interpreta el valor de una propiedad (ESPECIFICACION.md §3.1 y §4) y comprueba lo que admite:
 * longitudes, {@code auto}, palabras clave intrínsecas, pistas de grid, enteros, colores… Los
 * errores llevan la línea del nodo.
 */
final class ValueReader {

    private static final Pattern COLOR = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");

    /** Clases de valor, para comprobar lo que admite cada propiedad (§3.1 y §4). */
    enum Kind { LENGTH, AUTO, INTRINSIC, FRACTION, MINMAX }

    static final Set<Kind> SIZE = EnumSet.of(Kind.LENGTH, Kind.AUTO, Kind.INTRINSIC);
    static final Set<Kind> LENGTH_OR_AUTO = EnumSet.of(Kind.LENGTH, Kind.AUTO);
    static final Set<Kind> LENGTH = EnumSet.of(Kind.LENGTH);

    private ValueReader() {
    }

    /** Un valor de una clase admitida; sin {@code negative}, sin números negativos. */
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
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite " + p.getText().trim()
                    + ": usa " + describe(allowed));
        }
        if (!negative && isNegative(v)) {
            throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite valores negativos: " + p.getText().trim());
        }
    }

    /** Pistas de grid: longitudes, {@code auto}, {@code min-content}, {@code max-content}, fracciones y {@code minmax}. */
    static List<Value> tracks(Node p) {
        List<Value> list = parse(p, () -> ValueParser.list(p.getText()));
        for (Value v : list) {
            if (v == Value.Keyword.FIT_CONTENT) {
                throw error(p, ErrorCode.FIT_CONTENT_TRACK, "fit-content no es una pista de grid: usa auto, "
                        + "que ya se ajusta al contenido");
            }
            boolean valid = v instanceof Value.MinMax mm
                    ? track(mm.min()) && (track(mm.max()) || kind(mm.max()) == Kind.FRACTION)
                    : track(v) || kind(v) == Kind.FRACTION;
            if (!valid) {
                throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite " + v + " como pista: usa "
                        + "números, porcentajes, fracciones (1fr), auto, min-content, max-content o minmax(a, b)");
            }
            if (isNegative(v)) {
                throw error(p, ErrorCode.VALUE_NOT_ALLOWED, p.getName() + " no admite pistas negativas: " + p.getText().trim());
            }
        }
        return list;
    }

    private static boolean track(Value v) {
        Kind k = kind(v);
        return k == Kind.LENGTH || k == Kind.AUTO || (k == Kind.INTRINSIC && v != Value.Keyword.FIT_CONTENT);
    }

    /** Clase de un valor; {@code null} si es una función con argumentos que no son longitudes. */
    private static Kind kind(Value v) {
        if (v instanceof Value.Points || v instanceof Value.Percentage) {
            return Kind.LENGTH;
        }
        if (v instanceof Value.Function f) {
            return f.arguments().stream().allMatch(a -> kind(a) == Kind.LENGTH) ? Kind.LENGTH : null;
        }
        if (v instanceof Value.Fraction) {
            return Kind.FRACTION;
        }
        if (v instanceof Value.MinMax) {
            return Kind.MINMAX;
        }
        return v.isAuto() ? Kind.AUTO : Kind.INTRINSIC;
    }

    private static String describe(Set<Kind> allowed) {
        return allowed.stream().map(k -> switch (k) {
            case LENGTH -> "un número, un porcentaje o min()/max()/clamp()";
            case AUTO -> "auto";
            case INTRINSIC -> "min-content, max-content o fit-content";
            case FRACTION -> "una fracción";
            case MINMAX -> "minmax(a, b)";
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

    /** El valor depende de una base de porcentajes (directamente o dentro de una función). */
    static boolean hasPercentage(Value v) {
        return v instanceof Value.Percentage || parts(v).stream().anyMatch(ValueReader::hasPercentage);
    }

    /** Argumentos de una función o de {@code minmax}. */
    private static List<Value> parts(Value v) {
        if (v instanceof Value.MinMax m) {
            return List.of(m.min(), m.max());
        }
        return v instanceof Value.Function f ? f.arguments() : List.of();
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
            // lower-alpha es LOWER_ALPHA: el guion no cabe en el nombre de una constante.
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Valor no permitido en " + n.getName() + ": " + text);
        }
    }

    static boolean breakInside(Node n) {
        Style.Break b = enumValue(Style.Break.class, n);
        if (b == Style.Break.PAGE) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Inside solo admite auto o avoid");
        }
        return b == Style.Break.AVOID;
    }

    static boolean bool(Node n) {
        return switch (n.getText().trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes" -> true;
            case "false", "no" -> false;
            default -> throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Se esperaba true o false: " + n.getText().trim());
        };
    }

    static int integer(Node n) {
        try {
            return Integer.parseInt(n.getText().trim());
        } catch (NumberFormatException ex) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Se esperaba un entero: " + n.getText().trim());
        }
    }

    static int positiveInteger(Node n) {
        int v = integer(n);
        if (v < 1) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " debe ser al menos 1");
        }
        return v;
    }

    static double points(Node n) {
        Value v = parse(n, () -> ValueParser.value(n.getText()));
        if (!(v instanceof Value.Points p) || p.pt() <= 0) {
            throw error(n, ErrorCode.MEDIA_SIZE, n.getName() + " del medio debe ser un número de puntos positivo: "
                    + n.getText().trim());
        }
        return p.pt();
    }

    static double positive(Node n) {
        double v = nonNegative(n);
        if (v == 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " debe ser mayor que 0");
        }
        return v;
    }

    static double nonNegative(Node n) {
        Value v = parse(n, () -> ValueParser.value(n.getText()));
        if (!(v instanceof Value.Points p) || p.pt() < 0) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " debe ser un número no negativo: " + n.getText().trim());
        }
        return p.pt();
    }

    static Sides<Value> nonNegativeSides(Node n, boolean allowsPercentage) {
        Sides<Value> sides = parse(n, () -> Sides.shorthand(ValueParser.list(n.getText())));
        for (Value v : sides.asList()) {
            boolean valid = (v instanceof Value.Points p && p.pt() >= 0)
                    || (allowsPercentage && v instanceof Value.Percentage q && q.percent() >= 0);
            if (!valid) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + (allowsPercentage
                        ? " solo admite puntos o porcentajes no negativos"
                        : " solo admite puntos no negativos"));
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
            // se informa abajo
        }
        throw error(n, ErrorCode.VALUE_SYNTAX, "Aspect ratio espera 'ancho/alto' o un número positivo: " + t);
    }

    static String color(Node n) {
        String t = n.getText().trim();
        if (!COLOR.matcher(t).matches()) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Color no válido (usa #RGB o #RRGGBB): " + t);
        }
        return t;
    }

    static String fontFamily(Node n) {
        String family = n.getText().trim();
        if (family.isEmpty()) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Font necesita el nombre de una familia");
        }
        return family;
    }
}
