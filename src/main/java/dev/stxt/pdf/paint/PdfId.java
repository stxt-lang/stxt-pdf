package dev.stxt.pdf.paint;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * The file identifier of the PDF ({@code /ID} of the trailer), made from its content so that the same document gives
 * the same bytes (reference/implementation/pdf.md). PDFBox makes it from the time if it is missing. Here the document is
 * saved with a placeholder of the same length, the first 16 bytes of the SHA-256 of what was saved replace it, and
 * nothing else moves: the two halves of the identifier are equal, as in any file written for the first time.
 */
final class PdfId {

    /** Bytes of the identifier, written by PDFBox as 32 hexadecimal digits. */
    private static final int LENGTH = 16;
    private static final String ZEROS = "0".repeat(2 * LENGTH);
    private static final byte[] PLACEHOLDER = ("/ID [<" + ZEROS + "> <" + ZEROS + ">]")
            .getBytes(StandardCharsets.ISO_8859_1);

    private PdfId() {
    }

    /** Gives the document the placeholder identifier, before saving it. */
    static void prepare(PDDocument doc) {
        COSArray id = new COSArray();
        for (int i = 0; i < 2; i++) {
            COSString half = new COSString(new byte[LENGTH]);
            half.setForceHexForm(true);
            id.add(half);
        }
        doc.getDocument().setDocumentID(id);
    }

    /**
     * Replaces in {@code pdf}, saved after {@link #prepare}, the placeholder with the identifier made from its content.
     *
     * @throws IllegalStateException if the placeholder is not there exactly once
     */
    static void fill(byte[] pdf) {
        int at = find(pdf, 0);
        if (at < 0 || find(pdf, at + 1) >= 0) {
            throw new IllegalStateException("The PDF does not have its placeholder /ID exactly once");
        }
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(pdf);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] hex = HexFormat.of().withUpperCase().formatHex(digest, 0, LENGTH).getBytes(StandardCharsets.ISO_8859_1);
        int first = at + "/ID [<".length();
        System.arraycopy(hex, 0, pdf, first, hex.length);
        System.arraycopy(hex, 0, pdf, first + hex.length + "> <".length(), hex.length);
    }

    /** Position of the placeholder in {@code pdf} from {@code from}, or -1. */
    private static int find(byte[] pdf, int from) {
        for (int i = from; i <= pdf.length - PLACEHOLDER.length; i++) {
            if (pdf[i] == PLACEHOLDER[0] && Arrays.equals(pdf, i, i + PLACEHOLDER.length, PLACEHOLDER, 0,
                    PLACEHOLDER.length)) {
                return i;
            }
        }
        return -1;
    }
}
