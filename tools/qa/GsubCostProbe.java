// Sonda de QA: coste de showText de PDFBox con y sin la tabla GSUB de la fuente.
import org.apache.fontbox.ttf.*;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GsubCostProbe {
    public static void main(String[] a) throws Exception {
        Path p = Files.walk(Path.of("resources/fonts/IBM Plex Sans")).filter(x -> x.toString().endsWith("Regular.ttf") || x.toString().endsWith("-Text.ttf")).findFirst().orElseThrow();
        System.out.println("fuente: " + p);
        String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. fi fl ffi office affluent";
        for (boolean removeGsub : new boolean[]{false, true, false, true}) {
            try (PDDocument doc = new PDDocument()) {
                TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(Files.readAllBytes(p)));
                if (removeGsub) ttf.getTableMap().remove("GSUB");
                PDType0Font f = PDType0Font.load(doc, ttf, true);
                System.out.println("GSUB table: " + ttf.getTableMap().containsKey("GSUB") + " tablas=" + new TreeSet<>(ttf.getTableMap().keySet()));
                long t0 = System.nanoTime();
                for (int pg = 0; pg < 40; pg++) {
                    PDPage page = new PDPage(PDRectangle.A4); doc.addPage(page);
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        for (int l = 0; l < 60; l++) {
                            cs.beginText(); cs.setFont(f, 10); cs.newLineAtOffset(40, 800 - l * 12); cs.showText(LOREM); cs.endText();
                        }
                    }
                }
                long t1 = System.nanoTime();
                ByteArrayOutputStream o = new ByteArrayOutputStream(); doc.save(o);
                System.out.printf("removeGsub=%b  2400 showText: %d ms, pdf %d KB%n", removeGsub, (t1 - t0) / 1_000_000, o.size() / 1024);
            }
        }
        // ¿cambia lo pintado respecto a lo medido? (ligaduras)
        TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(Files.readAllBytes(p)));
        System.out.println("GSUB features: " + (ttf.getGsubData() == null ? null : ttf.getGsubData().getSupportedFeatures()));
        try (PDDocument doc = new PDDocument()) {
            PDType0Font f = PDType0Font.load(doc, p.toFile());
            for (String s : new String[]{"fi", "fl", "ffi", "office", "affluent", "1/2", "->"}) {
                byte[] enc = f.encode(s);
                System.out.println(s + " -> bytes codificados=" + enc.length + " (" + (enc.length / 2) + " glifos vs " + s.length() + " caracteres)");
            }
        }
    }
}
