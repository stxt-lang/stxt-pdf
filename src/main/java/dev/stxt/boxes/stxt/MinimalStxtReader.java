package dev.stxt.boxes.stxt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lector mínimo de STXT para arrancar el proyecto y para los tests.
 *
 * <p><strong>No valida contra el template</strong> ni implementa toda la especificación STXT.
 * Cubre el subconjunto que usa el modelo de cajas:</p>
 * <ul>
 *   <li>Nodos inline {@code Nombre: valor} y nodos de bloque {@code Nombre >>}.</li>
 *   <li>Namespace opcional {@code Nombre (ns): valor}, heredado por los hijos.</li>
 *   <li>Indentación con tabuladores, o con espacios en múltiplos de 4, sin mezclar en una línea.</li>
 *   <li>Comentarios {@code #} fuera de los bloques y líneas en blanco.</li>
 * </ul>
 * <p>En producción, usa {@link StxtCoreReader}.</p>
 */
public final class MinimalStxtReader implements DocumentReader {

    private static final Pattern NAME_WITH_NS = Pattern.compile("^(.*?)\\s*\\(([^)]*)\\)\\s*$");

    private static final class Builder {
        final String name;
        final String namespace;
        final String value;
        final boolean isBlock;
        final int line;
        final int level;
        final List<String> blockLines = new ArrayList<>();
        final List<Builder> children = new ArrayList<>();

        Builder(String name, String namespace, String value, boolean isBlock, int line, int level) {
            this.name = name;
            this.namespace = namespace;
            this.value = value;
            this.isBlock = isBlock;
            this.line = line;
            this.level = level;
        }

        StxtNode build() {
            String block = null;
            if (isBlock) {
                List<String> l = new ArrayList<>(blockLines);
                while (!l.isEmpty() && l.get(l.size() - 1).isBlank()) {
                    l.remove(l.size() - 1);
                }
                block = String.join("\n", l);
            }
            return new StxtNode(name, namespace, value, block,
                    children.stream().map(Builder::build).toList(), line);
        }
    }

    @Override
    public StxtNode read(String content, String source) {
        String[] lines = content.replace("\r\n", "\n").split("\n", -1);
        List<Builder> roots = new ArrayList<>();
        List<Builder> stack = new ArrayList<>(); // stack.get(n) = último nodo abierto de nivel n
        Builder openBlock = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int number = i + 1;

            if (openBlock != null) {
                if (line.isBlank()) {
                    openBlock.blockLines.add("");
                    continue;
                }
                int level = level(line, source, number, true);
                if (level > openBlock.level) {
                    openBlock.blockLines.add(stripLevels(line, openBlock.level + 1));
                    continue;
                }
                openBlock = null;
            }

            if (line.isBlank()) {
                continue;
            }
            int level = level(line, source, number, false);
            String text = line.strip();
            if (text.startsWith("#")) {
                continue;
            }
            if (level > stack.size()) {
                throw new StxtSyntaxException(source, number, "Salto de indentación: se esperaba como máximo el nivel " + stack.size());
            }

            boolean isBlock;
            String namePart;
            String value = null;
            if (text.endsWith(">>")) {
                isBlock = true;
                namePart = text.substring(0, text.length() - 2);
            } else {
                int colon = text.indexOf(':');
                if (colon < 0) {
                    throw new StxtSyntaxException(source, number, "Línea no válida: falta ':' o '>>'");
                }
                isBlock = false;
                namePart = text.substring(0, colon);
                value = text.substring(colon + 1).strip();
            }

            String name = namePart.strip();
            String namespace = null;
            Matcher m = NAME_WITH_NS.matcher(name);
            if (m.matches()) {
                name = m.group(1).strip();
                namespace = m.group(2).strip();
            }
            if (name.isEmpty()) {
                throw new StxtSyntaxException(source, number, "Nombre de nodo vacío");
            }

            while (stack.size() > level) {
                stack.remove(stack.size() - 1);
            }
            Builder parent = level == 0 ? null : stack.get(level - 1);
            if (namespace == null && parent != null) {
                namespace = parent.namespace;
            }
            Builder node = new Builder(name, namespace, value, isBlock, number, level);
            if (parent == null) {
                roots.add(node);
            } else {
                if (parent.isBlock) {
                    throw new StxtSyntaxException(source, number, "Un nodo de bloque no puede tener hijos");
                }
                parent.children.add(node);
            }
            stack.add(node);
            if (isBlock) {
                openBlock = node;
            }
        }

        if (roots.size() != 1) {
            throw new StxtSyntaxException(source, 1, "El documento debe tener exactamente un nodo raíz y tiene " + roots.size());
        }
        return roots.get(0).build();
    }

    /** Nivel de indentación: tabuladores, o espacios en múltiplos de 4. */
    private static int level(String line, String source, int number, boolean insideBlock) {
        int tabs = 0;
        int spaces = 0;
        for (char c : line.toCharArray()) {
            if (c == '\t') {
                tabs++;
            } else if (c == ' ') {
                spaces++;
            } else {
                break;
            }
        }
        if (tabs > 0 && spaces > 0 && !insideBlock) {
            throw new StxtSyntaxException(source, number, "No se pueden mezclar tabuladores y espacios en la indentación");
        }
        if (tabs > 0) {
            return tabs;
        }
        if (spaces % 4 != 0 && !insideBlock) {
            throw new StxtSyntaxException(source, number, "La indentación con espacios debe ser múltiplo de 4");
        }
        return spaces / 4;
    }

    /** Elimina {@code levels} niveles de indentación del principio de una línea de bloque. */
    private static String stripLevels(String line, int levels) {
        int i = 0;
        int removed = 0;
        while (removed < levels && i < line.length()) {
            if (line.charAt(i) == '\t') {
                i++;
                removed++;
            } else if (line.startsWith("    ", i)) {
                i += 4;
                removed++;
            } else {
                break;
            }
        }
        return line.substring(i);
    }
}
