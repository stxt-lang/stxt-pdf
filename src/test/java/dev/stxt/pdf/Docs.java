package dev.stxt.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for writing test documents. The base typography of the document goes in
 * {@code Styles} (reference/common/styles.md), in the rule {@code For element: Document}; the tests write it
 * apart from the body, which sometimes brings its own {@code Styles}.
 */
public final class Docs {

    private Docs() {
    }

    /** The children of {@code Document} that do not go inside a {@code Section}. */
    private static final java.util.Set<String> DOCUMENT_LEVEL = java.util.Set.of("page", "title", "language", "styles");

    /**
     * Wraps in a {@code Section} everything that {@code Document} does not accept directly
     * (reference/components/document.md): what follows {@code Page}, {@code Title}, {@code Language} and {@code
     * Styles}, which stay where they were. It does not change a text that already has a {@code Section}, that is not
     * a {@code Document} or that has no content.
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
     * Merges into the first one all the top-level {@code Styles} of the document: the rules of the
     * others move to the end of the first one and their header is removed. A document with a single
     * {@code Styles}, or with none, stays the same. It also passes it through {@link #sectioned}.
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
            // A later Styles: its rules move to the first one.
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
