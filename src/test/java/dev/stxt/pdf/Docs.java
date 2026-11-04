package dev.stxt.pdf;

import dev.stxt.InlineNode;
import dev.stxt.pdf.stxt.DocumentReader;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for writing test documents: the smallest header ({@link #header}), the typography the text needs
 * ({@link #STYLES} apart from the document or {@link #TYPOGRAPHY} inside it), the document from its lines
 * ({@link #document}, {@link #read}) and the rewriting that wraps the content in a {@code Section}
 * ({@link #sectioned}) and merges the {@code Styles} ({@link #mergeStyles}). The base typography of the document
 * goes in {@code Styles} (reference/common/styles.md), in the rule {@code For element: Document}; the tests write
 * it apart from the body, which sometimes brings its own {@code Styles}.
 */
public final class Docs {

    private static final DocumentReader READER = new DocumentReader();

    /**
     * The typography the text needs (reference/common/errors.md), as the {@code Styles} document a test gives apart
     * from the document ({@code Converter.builder().styles(STYLES)}): IBM Plex Sans, 10 pt, for {@code Document}. A
     * {@code Typography} line more goes after it, with three tabs.
     */
    public static final String STYLES = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    /**
     * The same typography inside the document, as its {@code Styles}, to go right after {@link #header}. A
     * {@code Typography} line more goes after it, with four tabs.
     */
    public static final String TYPOGRAPHY = "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\t\tSize: 10\n";

    private Docs() {
    }

    /**
     * The smallest header of a test document: a {@code Document} in Spanish titled {@code Prueba} with pages of
     * {@code width} × {@code height} pt and the default margins, and no typography (it goes in {@link #TYPOGRAPHY}
     * after it, or in {@link #STYLES} apart from the document).
     */
    public static String header(int width, int height) {
        return "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: " + width + "\n\t\tHeight: "
                + height + "\n";
    }

    /** {@link #header(int, int)} with margins of {@code margin} pt on the four sides. */
    public static String header(int width, int height, int margin) {
        return header(width, height) + "\t\tMargin: " + margin + "\n";
    }

    /**
     * The text of a test document: {@code header} and then {@code lines}, one per line with their tabs, ending
     * with a line break; with its {@code Styles} merged and its content in a {@code Section} ({@link #mergeStyles}).
     */
    public static String document(String header, String... lines) {
        return mergeStyles(header + String.join("\n", lines) + "\n");
    }

    /** {@link #document} read with {@link DocumentReader}: what the engine takes. */
    public static InlineNode read(String header, String... lines) {
        return READER.read(document(header, lines));
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
