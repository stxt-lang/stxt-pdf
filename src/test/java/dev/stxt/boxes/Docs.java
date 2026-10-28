package dev.stxt.boxes;

import java.util.ArrayList;
import java.util.List;

/**
 * Ayudas para escribir documentos de test. La tipografía base del documento va en
 * {@code Styles} (ESPECIFICACION.md §4.10), en la regla {@code Element: Document}; los tests la
 * escriben aparte del cuerpo, que a veces trae su propio {@code Styles}.
 */
public final class Docs {

    private Docs() {
    }

    /**
     * Junta en el primero todos los {@code Styles} de primer nivel del documento: las reglas de los
     * demás pasan al final del primero y se quita su cabecera. Un documento con un solo
     * {@code Styles}, o con ninguno, queda igual.
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
        return String.join("\n", out);
    }
}
