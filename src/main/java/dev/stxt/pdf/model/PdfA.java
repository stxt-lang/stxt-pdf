package dev.stxt.pdf.model;

/**
 * The part and the conformance level of PDF/A the PDF complies with (reference/common/pdf.md), chosen by whoever
 * converts ({@code Converter.builder().pdfA(...)}): a decision of who publishes, not of the document, like PDF/UA.
 * Parts 2 and 3 are PDF 1.7 files, compatible with PDF/UA-1; part 3 is part 2 plus embedded files of any kind
 * ({@link Attachment}). Level {@code B} promises the visual appearance and level {@code A} adds the tagging, which
 * every PDF of the engine has: the two levels give the same PDF with a different declaration.
 */
public enum PdfA {

    /** PDF/A-2b: ISO 19005-2, level B. */
    A2B(2, 'B'),
    /** PDF/A-2a: ISO 19005-2, level A (tagged, which the PDF always is). */
    A2A(2, 'A'),
    /** PDF/A-3b: ISO 19005-3, level B, with embedded files of any kind. */
    A3B(3, 'B'),
    /** PDF/A-3a: ISO 19005-3, level A, with embedded files of any kind. */
    A3A(3, 'A');

    private final int part;
    private final char level;

    PdfA(int part, char level) {
        this.part = part;
        this.level = level;
    }

    /** The part of ISO 19005: 2 or 3 ({@code pdfaid:part}). */
    public int part() {
        return part;
    }

    /** The conformance level: {@code 'A'} or {@code 'B'} ({@code pdfaid:conformance}). */
    public char level() {
        return level;
    }

    /** Whether files of any kind can be embedded ({@link Attachment}): only in part 3. */
    public boolean allowsAttachments() {
        return part == 3;
    }

    /** As it is written: {@code PDF/A-3b}. */
    @Override
    public String toString() {
        return "PDF/A-" + part + Character.toLowerCase(level);
    }
}
