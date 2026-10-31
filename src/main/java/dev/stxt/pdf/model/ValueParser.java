package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Convierte el texto de una propiedad en {@link Value} (reference/common/values.md).
 *
 * <ul>
 *   <li>Números en puntos con punto decimal: {@code 12}, {@code 12.5}, {@code -4}.</li>
 *   <li>Porcentajes ({@code 50%}) y fracciones ({@code *}, {@code 2*}; {@code *} solo es {@code 1*}).</li>
 *   <li>Palabras: {@code auto}, {@code narrowest}, {@code natural}, {@code fit}.</li>
 *   <li>Pistas entre dos tamaños: {@code between(a, b)}.</li>
 *   <li>Listas separadas por espacios, con {@code repeat(n, patrón)}.</li>
 * </ul>
 */
public final class ValueParser {

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private ValueParser() {
    }

    /** Un único valor. */
    public static Value value(String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Valor vacío");
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
                throw new IllegalArgumentException("Una fracción no puede ser negativa: " + text);
            }
            return new Value.Fraction(fraction);
        }
        return new Value.Points(number(t, text));
    }

    /** Lista de valores separados por espacios; expande {@code repeat(n, patrón)}. */
    public static List<Value> list(String text) {
        List<Value> result = new ArrayList<>();
        for (String token : tokens(text)) {
            if (token.startsWith("repeat(") && token.endsWith(")")) {
                List<String> args = arguments(token.substring("repeat(".length(), token.length() - 1));
                if (args.size() != 2 || !args.get(0).trim().matches("\\d+")) {
                    throw new IllegalArgumentException("Se esperaba repeat(n, patrón): " + token);
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
            throw new IllegalArgumentException("Lista de valores vacía");
        }
        return result;
    }

    private static Value function(String name, String inner, String original) {
        List<Value> args = arguments(inner).stream().map(ValueParser::value).toList();
        switch (name) {
            case "between":
                require(args.size() == 2, "between necesita 2 argumentos: " + original);
                require(!(args.get(0) instanceof Value.Fraction), "El mínimo de between no puede ser una fracción: " + original);
                return new Value.Between(args.get(0), args.get(1));
            default:
                throw new IllegalArgumentException("Función desconocida: " + name);
        }
    }

    /** Divide por espacios en blanco fuera de paréntesis. */
    static List<String> tokens(String text) {
        return split(text, ' ');
    }

    /** Divide por comas fuera de paréntesis. */
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
                    throw new IllegalArgumentException("Paréntesis desequilibrados: " + text);
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
            throw new IllegalArgumentException("Paréntesis desequilibrados: " + text);
        }
        if (!current.toString().isBlank()) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    private static double number(String s, String original) {
        String t = s.trim();
        if (!NUMBER.matcher(t).matches()) {
            throw new IllegalArgumentException("Valor no válido: " + original.trim()
                    + " (el decimal se escribe con punto, por ejemplo 12.5)");
        }
        return Double.parseDouble(t);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
