// QA probe: what the cold start is made of and how much it costs to load the fonts
// of the PDF in each conversion. It has to run in a freshly started JVM (the first phase is the cold one).
// It runs from the root of the repository; see tools/qa/README.md.
import dev.stxt.InlineNode;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.resources.FontFile;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
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
            for (Path d : ds)
                for (String lang : List.of("es", "en"))
                    ins.add(new In(d.getFileName() + "/" + lang, d.resolve(lang).resolve("input.stxt"), d.resolve("styles.stxt")));
        }
        ins.sort(Comparator.comparing(In::name));

        // 1. Cold, by phases: each one loads the classes and warms up its own part for the first time.
        long t0 = System.nanoTime();
        DocumentReader reader = new DocumentReader();
        InlineNode first = reader.read(ins.get(0).input());
        System.out.printf("cold · read the first document (schema, stxt-core): %d ms%n", ms(t0));
        t0 = System.nanoTime();
        Resources resources = new Resources();
        resources.fonts();
        System.out.printf("cold · scan fonts/ (FontLibrary, %d families): %d ms%n", resources.fonts().families().size(), ms(t0));
        Converter.Builder builder = Converter.builder().resources(resources);
        if (Files.exists(ins.get(0).styles())) builder.styles(reader.readStyles(ins.get(0).styles()));
        Converter converter = builder.build();
        t0 = System.nanoTime();
        LaidOutDocument laidOut = converter.layOut(first);
        System.out.printf("cold · lay out %s: %d ms%n", ins.get(0).name(), ms(t0));
        t0 = System.nanoTime();
        converter.toPdf(laidOut, OutputStream.nullOutputStream());
        System.out.printf("cold · write its PDF: %d ms%n", ms(t0));
        t0 = System.nanoTime();
        for (In in : ins) convert(reader, resources, in);
        System.out.printf("cold · the %d examples (1st round after the above): %d ms%n", ins.size(), ms(t0));
        for (int k = 0; k < 5; k++) {
            t0 = System.nanoTime();
            for (In in : ins) convert(reader, resources, in);
            System.out.printf("warm · the %d examples: %d ms%n", ins.size(), ms(t0));
        }

        // 2. What PdfFonts.font does for each font and document, by parts, warm.
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
        System.out.printf("per font and document (%d files, %d times): read %.3f ms, parse %.3f ms, PDType0Font.load %.3f ms, subset and save %.3f ms%n",
                files.size(), n, read / 1e6 / n, parse / 1e6 / n, load / 1e6 / n, save / 1e6 / n);
        // What PdfFonts does: parse the bytes FontFile already has, without reading the file.
        System.out.printf("same, parsing from FontFile.content(): %.3f ms%n", fromContent / 1e6 / n);
    }

    static void convert(DocumentReader reader, Resources resources, In in) throws IOException {
        Converter.Builder b = Converter.builder().resources(resources);
        if (Files.exists(in.styles())) b.styles(reader.readStyles(in.styles()));
        b.build().toPdf(reader.read(in.input()), OutputStream.nullOutputStream());
    }
}
