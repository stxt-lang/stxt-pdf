// Sonda de QA (ROADMAP-QA-ARQ.md, PF-05): de qué se compone el arranque en frío y cuánto cuesta cargar las fuentes
// del PDF en cada conversión. Hay que ejecutarla en una JVM recién arrancada (la primera fase es el frío).
// Se ejecuta desde la raíz del repositorio; ver tools/qa/README.md.
import dev.stxt.InlineNode;
import dev.stxt.boxes.convert.Converter;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.resources.FontFile;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ColdStartProbe {
    record In(String name, Path input, Path styles) {}

    static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    public static void main(String[] a) throws Exception {
        List<In> ins = new ArrayList<>();
        try (var ds = Files.newDirectoryStream(Path.of("test/examples"), Files::isDirectory)) {
            for (Path d : ds) ins.add(new In(d.getFileName().toString(), d.resolve("input.stxt"), d.resolve("styles.stxt")));
        }
        ins.sort(Comparator.comparing(In::name));

        // 1. Frío, por fases: cada una carga las clases y calienta lo suyo por primera vez.
        long t0 = System.nanoTime();
        DocumentReader reader = new DocumentReader();
        InlineNode first = reader.read(ins.get(0).input());
        System.out.printf("frío · leer el primer documento (schema, stxt-core): %d ms%n", ms(t0));
        t0 = System.nanoTime();
        Resources resources = new Resources();
        resources.fonts();
        System.out.printf("frío · escanear fonts/ (FontLibrary, %d familias): %d ms%n", resources.fonts().families().size(), ms(t0));
        Converter.Builder builder = Converter.builder().resources(resources);
        if (Files.exists(ins.get(0).styles())) builder.styles(reader.readStyles(ins.get(0).styles()));
        Converter converter = builder.build();
        t0 = System.nanoTime();
        LaidOutDocument laidOut = converter.layOut(first);
        System.out.printf("frío · maquetar %s: %d ms%n", ins.get(0).name(), ms(t0));
        t0 = System.nanoTime();
        converter.toPdf(laidOut, OutputStream.nullOutputStream());
        System.out.printf("frío · escribir su PDF: %d ms%n", ms(t0));
        t0 = System.nanoTime();
        for (In in : ins) convert(reader, resources, in);
        System.out.printf("frío · los %d ejemplos (1.ª ronda tras lo anterior): %d ms%n", ins.size(), ms(t0));
        for (int k = 0; k < 5; k++) {
            t0 = System.nanoTime();
            for (In in : ins) convert(reader, resources, in);
            System.out.printf("caliente · los %d ejemplos: %d ms%n", ins.size(), ms(t0));
        }

        // 2. Lo que hace PdfFonts.font por cada fuente y documento, por partes, en caliente.
        List<FontFile> files = new ArrayList<>();
        for (String family : resources.fonts().families())
            for (FontWeight w : FontWeight.values())
                for (FontStyle s : FontStyle.values()) {
                    try { files.add(resources.fonts().find(new FontFace(family, w, s))); } catch (RuntimeException e) { }
                }
        int rounds = 50;
        long read = 0, parse = 0, load = 0, save = 0, fromContent = 0;
        for (int r = 0; r < rounds + 10; r++) {
            for (FontFile f : files) {
                long c0 = System.nanoTime();
                new TTFParser().parse(new RandomAccessReadBuffer(f.content()));
                if (r >= 10) fromContent += System.nanoTime() - c0;
                long a0 = System.nanoTime();
                byte[] bytes = Files.readAllBytes(f.path());
                long a1 = System.nanoTime();
                TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(bytes));
                ttf.setEnableGsub(false);
                long a2 = System.nanoTime();
                try (PDDocument doc = new PDDocument()) {
                    PDType0Font font = PDType0Font.load(doc, ttf, true);
                    long a3 = System.nanoTime();
                    font.encode("Prueba de subconjunto");
                    doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
                    doc.save(OutputStream.nullOutputStream());
                    long a4 = System.nanoTime();
                    if (r >= 10) { read += a1 - a0; parse += a2 - a1; load += a3 - a2; save += a4 - a3; }
                }
            }
        }
        int n = rounds * files.size();
        System.out.printf("por fuente y documento (%d ficheros, %d veces): leer %.3f ms, parsear %.3f ms, PDType0Font.load %.3f ms, subconjunto y guardar %.3f ms%n",
                files.size(), n, read / 1e6 / n, parse / 1e6 / n, load / 1e6 / n, save / 1e6 / n);
        // Lo que hace PdfFonts desde PF-05: parsear los bytes que ya tiene FontFile, sin leer el fichero.
        System.out.printf("ídem, parsear desde FontFile.content(): %.3f ms%n", fromContent / 1e6 / n);
    }

    static void convert(DocumentReader reader, Resources resources, In in) throws IOException {
        Converter.Builder b = Converter.builder().resources(resources);
        if (Files.exists(in.styles())) b.styles(reader.readStyles(in.styles()));
        b.build().toPdf(reader.read(in.input()), OutputStream.nullOutputStream());
    }
}
