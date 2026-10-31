package dev.stxt.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Ayudas para escribir documentos de test. La tipografía base del documento va en
 * {@code Styles} (reference/common/styles.md), en la regla {@code For element: Document}; los tests la
 * escriben aparte del cuerpo, que a veces trae su propio {@code Styles}.
 */
public final class Docs {

    private Docs() {
    }

    /** Los hijos de {@code Document} que no van dentro de una {@code Section}. */
    private static final java.util.Set<String> DOCUMENT_LEVEL = java.util.Set.of("page", "title", "language", "styles");

    /**
     * Envuelve en una {@code Section} todo lo que {@code Document} no admite suelto
     * (reference/components/document.md): lo que sigue a {@code Page}, {@code Title}, {@code Language} y {@code
     * Styles}, que quedan donde estaban. No cambia un texto que ya tiene una {@code Section}, que no es un {@code
     * Document} o que no tiene contenido.
     */
    public static String sectioned(String document) {
        String[] lines = document.split("\n", -1);
        int header = -1;
        for (int i = 0; i < lines.length && header < 0; i++) {
            if (lines[i].matches("Document\\b.*")) {
                header = i;
            }
        }
        if (header < 0) {
            return document;
        }
        List<String> kept = new ArrayList<>(List.of(lines).subList(0, header + 1));
        List<String> wrapped = new ArrayList<>();
        boolean keeping = true;
        boolean content = false;
        for (int i = header + 1; i < lines.length; i++) {
            String line = lines[i];
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\t([^\t#\\s][^:\\s]*(?: [^:\\s]+)*)\\s*(?::|>>|$).*")
                    .matcher(line);
            if (m.matches() && !line.startsWith("\t\t")) {
                String name = m.group(1).trim().toLowerCase().replace(' ', '-');
                if (name.equals("section")) {
                    return document;
                }
                keeping = DOCUMENT_LEVEL.contains(name);
                content |= !keeping;
            }
            if (keeping) {
                kept.add(line);
            } else {
                wrapped.add(line.isBlank() ? line : "\t" + line);
            }
        }
        if (!content) {
            return document;
        }
        kept.add("\tSection:");
        kept.addAll(wrapped);
        return String.join("\n", kept);
    }

    /**
     * Junta en el primero todos los {@code Styles} de primer nivel del documento: las reglas de los
     * demás pasan al final del primero y se quita su cabecera. Un documento con un solo
     * {@code Styles}, o con ninguno, queda igual. Además lo pasa por {@link #sectioned}.
     */
    public static String mergeStyles(String document) {
        String[] lines = document.split("\n", -1);
        List<String> out = new ArrayList<>();
        List<String> moved = new ArrayList<>();
        int first = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].equals("\tStyles:")) {
                out.add(lines[i]);
                continue;
            }
            if (first < 0) {
                first = out.size();
                out.add(lines[i]);
                continue;
            }
            // Un Styles posterior: sus reglas pasan al primero.
            while (i + 1 < lines.length && lines[i + 1].startsWith("\t\t")) {
                moved.add(lines[++i]);
            }
        }
        if (first >= 0 && !moved.isEmpty()) {
            int end = first + 1;
            while (end < out.size() && out.get(end).startsWith("\t\t")) {
                end++;
            }
            out.addAll(end, moved);
        }
        return sectioned(String.join("\n", out));
    }
}
