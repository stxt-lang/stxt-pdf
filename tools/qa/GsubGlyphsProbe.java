// Sonda de QA: qué cadenas pinta distinto PDFBox con GSUB (ligaduras) que con las
// métricas del motor, que no las aplica, para cada fuente de resources/fonts.
import org.apache.fontbox.ttf.*;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GsubGlyphsProbe {
    static String content(Path p, boolean removeGsub, String text) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(Files.readAllBytes(p)));
            if (removeGsub) ttf.getTableMap().remove("GSUB");
            PDType0Font f = PDType0Font.load(doc, ttf, true);
            PDPage page = new PDPage(PDRectangle.A4); doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText(); cs.setFont(f, 10); cs.newLineAtOffset(40, 800); cs.showText(text); cs.endText();
            }
            return new String(page.getContents().readAllBytes());
        }
    }
    public static void main(String[] a) throws Exception {
        try (var w = Files.walk(Path.of("resources/fonts"))) {
            for (Path p : w.filter(x -> x.toString().endsWith(".ttf")).sorted().toList()) {
                int diff = 0; List<String> bad = new ArrayList<>();
                for (String s : new String[]{"fi", "fl", "ffi", "ffl", "office", "affluent", "1/2", "->", "=>", "!=", "<=", ">=", "www", "0123456789", "Æ", "áéíóúñç", "AV To", "ǅ", "ﬁ"}) {
                    try { if (!content(p, false, s).equals(content(p, true, s))) { diff++; bad.add(s); } } catch (Exception e) { bad.add(s + "(EXC " + e.getClass().getSimpleName() + ")"); }
                }
                System.out.println(p.getFileName() + ": cadenas que cambian por GSUB = " + bad);
            }
        }
    }
}
