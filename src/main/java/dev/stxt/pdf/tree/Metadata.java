package dev.stxt.pdf.tree;

/**
 * Document data that does not affect layout (reference/implementation/pdf.md): it goes to the PDF output.
 *
 * @param title    title ({@code Title}), or {@code null} only in {@link #NONE}
 * @param language language of the content ({@code Language}), a BCP 47 tag, or {@code null}
 * @param subject  what the document is about ({@code Subject}), or {@code null}
 * @param author   who writes the document ({@code Author}), or {@code null}
 */
public record Metadata(String title, String language, String subject, String author) {

    /** No title, language, subject or author: a layout result built by hand, not from a document. */
    public static final Metadata NONE = new Metadata(null, null, null, null);
}
