package dev.stxt.pdf;

import dev.stxt.InlineNode;
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
     * The PDF of {@code document} (its content wrapped in a {@code Section} if it is not: {@link Docs#sectioned}) with
     * the default converter ({@code Converter.create()}): PDF/UA-1, the default resources and no styles apart.
     */
    public static byte[] of(String document) throws IOException {
        return of(READER.read(Docs.sectioned(document)));
    }

    /** The PDF of a document already read, with the default converter. */
    public static byte[] of(InlineNode document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Converter.create().toPdf(document, out);
        return out.toByteArray();
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
