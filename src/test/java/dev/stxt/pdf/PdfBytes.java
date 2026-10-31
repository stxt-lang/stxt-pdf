package dev.stxt.pdf;

import java.nio.charset.StandardCharsets;

/** Helpers for comparing PDFs in the tests. */
public final class PdfBytes {

    private PdfBytes() {
    }

    /**
     * The creation and modification dates and the trailer {@code /ID} are the only things that change between two
     * identical conversions.
     */
    public static byte[] withoutVolatileFields(byte[] pdf) {
        String text = new String(pdf, StandardCharsets.ISO_8859_1)
                .replaceAll("/(CreationDate|ModDate)\\s*\\([^)]*\\)", "")
                .replaceAll("/ID\\s*\\[[^\\]]*\\]", "");
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }
}
