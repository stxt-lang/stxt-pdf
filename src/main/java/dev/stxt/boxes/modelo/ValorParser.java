package dev.stxt.boxes.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Convierte el texto de una propiedad en {@link Valor} (ESPECIFICACION.md §3).
 *
 * <ul>
 *   <li>Números en puntos con punto decimal: {@code 12}, {@code 12.5}, {@code -4}.</li>
 *   <li>Porcentajes ({@code 50%}) y fracciones ({@code 1fr}).</li>
 *   <li>Palabras: {@code auto}, {@code min-content}, {@code max-content}, {@code fit-content}.</li>
 *   <li>Funciones: {@code minmax(a, b)}, {@code min(...)}, {@code max(...)}, {@code clamp(a, b, c)}.</li>
 *   <li>Listas separadas por espacios, con {@code repetir(n, patrón)}.</li>
 * </ul>
 */
public final class ValorParser {

    private static final Pattern NUMERO = Pattern.compile("-?\\d+(\\.\\d+)?");

    private ValorParser() {
    }

    /** Un único valor. */
    public static Valor valor(String texto) {
        String t = texto.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("Valor vacío");
        }
        switch (t) {
            case "auto":
                return Valor.AUTO;
            case "min-content":
                return Valor.Palabra.MIN_CONTENT;
            case "max-content":
                return Valor.Palabra.MAX_CONTENT;
            case "fit-content":
                return Valor.Palabra.FIT_CONTENT;
            default:
                break;
        }
        int parentesis = t.indexOf('(');
        if (parentesis > 0 && t.endsWith(")")) {
            return funcion(t.substring(0, parentesis).trim(),
                    t.substring(parentesis + 1, t.length() - 1), t);
        }
        if (t.endsWith("%")) {
            return new Valor.Porcentaje(numero(t.substring(0, t.length() - 1), texto));
        }
        if (t.endsWith("fr")) {
            double fr = numero(t.substring(0, t.length() - 2), texto);
            if (fr < 0) {
                throw new IllegalArgumentException("Una fracción no puede ser negativa: " + texto);
            }
            return new Valor.Fraccion(fr);
        }
        return new Valor.Puntos(numero(t, texto));
    }

    /** Lista de valores separados por espacios; expande {@code repetir(n, patrón)}. */
    public static List<Valor> lista(String texto) {
        List<Valor> resultado = new ArrayList<>();
        for (String token : tokens(texto)) {
            if (token.startsWith("repetir(") && token.endsWith(")")) {
                List<String> args = argumentos(token.substring("repetir(".length(), token.length() - 1));
                if (args.size() != 2 || !args.get(0).trim().matches("\\d+")) {
                    throw new IllegalArgumentException("Se esperaba repetir(n, patrón): " + token);
                }
                int veces = Integer.parseInt(args.get(0).trim());
                List<Valor> patron = lista(args.get(1));
                for (int i = 0; i < veces; i++) {
                    resultado.addAll(patron);
                }
            } else {
                resultado.add(valor(token));
            }
        }
        if (resultado.isEmpty()) {
            throw new IllegalArgumentException("Lista de valores vacía");
        }
        return resultado;
    }

    private static Valor funcion(String nombre, String interior, String original) {
        List<Valor> args = argumentos(interior).stream().map(ValorParser::valor).toList();
        switch (nombre) {
            case "minmax":
                exigir(args.size() == 2, "minmax necesita 2 argumentos: " + original);
                exigir(!(args.get(0) instanceof Valor.Fraccion), "El mínimo de minmax no puede ser fr: " + original);
                return new Valor.MinMax(args.get(0), args.get(1));
            case "min":
                exigir(!args.isEmpty(), "min necesita argumentos: " + original);
                return new Valor.Funcion(Valor.TipoFuncion.MIN, args);
            case "max":
                exigir(!args.isEmpty(), "max necesita argumentos: " + original);
                return new Valor.Funcion(Valor.TipoFuncion.MAX, args);
            case "clamp":
                exigir(args.size() == 3, "clamp necesita 3 argumentos: " + original);
                return new Valor.Funcion(Valor.TipoFuncion.CLAMP, args);
            default:
                throw new IllegalArgumentException("Función desconocida: " + nombre);
        }
    }

    /** Divide por espacios en blanco fuera de paréntesis. */
    static List<String> tokens(String texto) {
        return dividir(texto, ' ');
    }

    /** Divide por comas fuera de paréntesis. */
    static List<String> argumentos(String texto) {
        return dividir(texto, ',');
    }

    private static List<String> dividir(String texto, char separador) {
        List<String> partes = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int profundidad = 0;
        for (char c : texto.toCharArray()) {
            boolean esSeparador = separador == ' ' ? Character.isWhitespace(c) : c == separador;
            if (c == '(') {
                profundidad++;
            } else if (c == ')') {
                profundidad--;
                if (profundidad < 0) {
                    throw new IllegalArgumentException("Paréntesis desequilibrados: " + texto);
                }
            }
            if (esSeparador && profundidad == 0) {
                if (!actual.toString().isBlank()) {
                    partes.add(actual.toString().trim());
                }
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        if (profundidad != 0) {
            throw new IllegalArgumentException("Paréntesis desequilibrados: " + texto);
        }
        if (!actual.toString().isBlank()) {
            partes.add(actual.toString().trim());
        }
        return partes;
    }

    private static double numero(String s, String original) {
        String t = s.trim();
        if (!NUMERO.matcher(t).matches()) {
            throw new IllegalArgumentException("Valor no válido: " + original.trim()
                    + " (usa punto decimal, por ejemplo 12.5)");
        }
        return Double.parseDouble(t);
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
