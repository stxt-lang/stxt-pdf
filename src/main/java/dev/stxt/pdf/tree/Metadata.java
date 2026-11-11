package dev.stxt.pdf.tree;

import java.util.List;
import java.util.Objects;

/**
 * Document data that does not affect layout (reference/implementation/pdf.md): it goes to the PDF output.
 *
 * @param title    title ({@code Title}), or {@code null} only in {@link #NONE}
 * @param language language of the content ({@code Language}), a BCP 47 tag, or {@code null}
 * @param subject  what the document is about ({@code Subject}), or {@code null}
 * @param author   who writes the document ({@code Author}), or {@code null}
 * @param keywords the keywords of the document ({@code Keywords}), in order, each one trimmed and not empty; empty if
 *                 there are none
 */
public record Metadata(String title, String language, String subject, String author, List<String> keywords) {

    /** No title, language, subject, author or keywords: a layout result built by hand, not from a document. */
    public static final Metadata NONE = new Metadata(null, null, null, null, List.of());

    /** An immutable copy of the keywords. */
    public Metadata {
        keywords = List.copyOf(Objects.requireNonNull(keywords, "keywords"));
    }
}
