package dev.stxt.pdf;

import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** The PDF of a test document, with the default styles and resources. */
public final class Pdfs {

    private static final DocumentReader READER = new DocumentReader();

    private Pdfs() {
    }

    /**
     * The PDF of {@code document} (its content wrapped in a {@code Section} if it is not: {@link Docs#sectioned})
     * conforming to PDF/UA-{@code ua}, 1 or 2 ({@code Converter.Builder#pdfUa}).
     */
    public static byte[] of(String document, int ua) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.builder().pdfUa(ua).build().toPdf(READER.read(Docs.sectioned(document)), out);
        return out.toByteArray();
    }
}
