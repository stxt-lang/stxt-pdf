package dev.stxt.pdf;

import java.nio.charset.StandardCharsets;

/** Ayudas para comparar PDF en los tests. */
public final class PdfBytes {

    private PdfBytes() {
    }

    /** Las fechas de creación y modificación y el {@code /ID} del trailer son lo único que cambia entre dos conversiones iguales. */
    public static byte[] withoutVolatileFields(byte[] pdf) {
        String text = new String(pdf, StandardCharsets.ISO_8859_1)
                .replaceAll("/(CreationDate|ModDate)\\s*\\([^)]*\\)", "")
                .replaceAll("/ID\\s*\\[[^\\]]*\\]", "");
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }
}
