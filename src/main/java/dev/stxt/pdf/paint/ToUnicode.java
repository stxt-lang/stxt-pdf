package dev.stxt.pdf.paint;

import org.apache.fontbox.cmap.CMap;
import org.apache.fontbox.cmap.CMapParser;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * The {@code ToUnicode} of an embedded font (ISO 32000-1, 9.10.3): the text of each of its glyphs, which is what is
 * copied from the PDF, what a search finds and what is read aloud. PDFBox writes it with one character per glyph;
 * {@link #replace} writes it again with the text of some glyphs changed, which may be several characters.
 */
final class ToUnicode {

    /** The codes of a Type 0 font with Identity-H: two bytes. */
    private static final int CODES = 0x10000;
    /** The most entries a {@code beginbfchar} section may have. */
    private static final int SECTION = 100;

    private ToUnicode() {
    }

    /**
     * Writes the {@code ToUnicode} of {@code font} again, as it is, with the text of the glyphs of {@code texts}: by
     * glyph, which with Identity-H is the code of the glyph in the text.
     */
    static void replace(PDDocument doc, PDType0Font font, Map<Integer, String> texts) throws IOException {
        COSStream current = font.getCOSObject().getCOSStream(COSName.TO_UNICODE);
        if (current == null) {
            return;
        }
        CMap cmap;
        try (InputStream in = current.createInputStream()) {
            cmap = new CMapParser().parse(new RandomAccessReadBuffer(in));
        }
        Map<Integer, String> all = new TreeMap<>();
        for (int code = 0; code < CODES; code++) {
            String text = cmap.toUnicode(code, 2);
            if (text != null) {
                all.put(code, text);
            }
        }
        // Only the glyphs the font has in the PDF: a glyph that is not in the subset has no entry.
        texts.forEach((glyph, text) -> all.computeIfPresent(glyph, (g, old) -> text));
        byte[] written = write(all).getBytes(StandardCharsets.US_ASCII);
        font.getCOSObject().setItem(COSName.TO_UNICODE,
                new PDStream(doc, new ByteArrayInputStream(written), COSName.FLATE_DECODE));
    }

    /** The CMap with one {@code bfchar} entry per code, in order. */
    private static String write(Map<Integer, String> texts) {
        StringBuilder out = new StringBuilder();
        out.append("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n")
                .append("/CIDSystemInfo\n<< /Registry (Adobe)\n/Ordering (UCS)\n/Supplement 0\n>> def\n")
                .append("/CMapName /Adobe-Identity-UCS def\n/CMapType 2 def\n")
                .append("1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");
        int left = texts.size();
        int inSection = 0;
        for (Map.Entry<Integer, String> e : texts.entrySet()) {
            if (inSection == 0) {
                out.append(Math.min(SECTION, left)).append(" beginbfchar\n");
            }
            out.append(String.format("<%04X> <", e.getKey()));
            for (byte b : e.getValue().getBytes(StandardCharsets.UTF_16BE)) {
                out.append(String.format("%02X", b));
            }
            out.append(">\n");
            left--;
            if (++inSection == SECTION || left == 0) {
                out.append("endbfchar\n");
                inSection = 0;
            }
        }
        out.append("endcmap\nCMapName currentdict /CMap defineresource pop\nend\nend\n");
        return out.toString();
    }
}
