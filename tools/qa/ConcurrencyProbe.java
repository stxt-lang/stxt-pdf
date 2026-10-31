// QA probe: converts the examples sequentially and with 8 threads and compares
// the PDFs (without dates or ID). It also measures the contention of a shared Resources against one per thread
// (four alternating phases, so that the warm-up of the JVM favors neither).
// It runs from the root of the repository; see tools/qa/README.md.
import dev.stxt.InlineNode;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ConcurrencyProbe {
    record In(String name, InlineNode doc, InlineNode styles) {}

    static byte[] convert(In in, Resources r) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        Converter.Builder b = Converter.builder().resources(r);
        if (in.styles != null) b.styles(in.styles);
        b.build().toPdf(in.doc, o);
        return o.toByteArray();
    }

    static byte[] norm(byte[] b) { // removes dates and IDs
        String s = new String(b, java.nio.charset.StandardCharsets.ISO_8859_1);
        s = s.replaceAll("/CreationDate\\(.*?\\)", "").replaceAll("/ModDate\\(.*?\\)", "").replaceAll("/ID ?\\[.*?\\]", "");
        return s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    public static void main(String[] a) throws Exception {
        DocumentReader rd = new DocumentReader();
        List<In> ins = new ArrayList<>();
        try (var ds = Files.newDirectoryStream(Path.of("test/examples"), p -> Files.isDirectory(p))) {
            for (Path d : ds) {
                Path st = d.resolve("styles.stxt");
                ins.add(new In(d.getFileName().toString(), rd.read(d.resolve("input.stxt")),
                        Files.exists(st) ? rd.readStyles(st) : null));
            }
        }
        ins.sort(Comparator.comparing(In::name));
        Resources shared = new Resources();
        Map<String, byte[]> base = new HashMap<>();
        long t0 = System.nanoTime();
        for (In in : ins) base.put(in.name, norm(convert(in, shared)));
        System.out.printf("sequential 1st round (cold): %d ms%n", (System.nanoTime() - t0) / 1_000_000);
        for (int k = 0; k < 3; k++) {
            t0 = System.nanoTime();
            for (In in : ins) convert(in, shared);
            System.out.printf("sequential warm: %d ms (%d docs)%n", (System.nanoTime() - t0) / 1_000_000, ins.size());
        }
        // determinism
        for (In in : ins) if (!Arrays.equals(base.get(in.name), norm(convert(in, shared)))) System.out.println("NOT DETERMINISTIC " + in.name);

        for (boolean sharedRes : new boolean[]{false, true, false, true}) {
            int threads = 8, rounds = 10;
            ExecutorService ex = Executors.newFixedThreadPool(threads);
            List<Future<String>> fs = new ArrayList<>();
            long t1 = System.nanoTime();
            for (int t = 0; t < threads; t++) {
                final int tt = t;
                fs.add(ex.submit(() -> {
                    Resources r = sharedRes ? shared : new Resources();
                    int bad = 0;
                    for (int i = 0; i < rounds; i++)
                        for (In in : ins) {
                            try {
                                if (!Arrays.equals(base.get(in.name), norm(convert(in, r)))) { bad++; }
                            } catch (Throwable e) { bad++; if (bad < 3) System.out.println("EXC " + in.name + " " + e); }
                        }
                    return "thread " + tt + " differences=" + bad;
                }));
            }
            int total = 0;
            for (var f : fs) { String s = f.get(); if (!s.endsWith("=0")) System.out.println(s); total++; }
            ex.shutdown();
            System.out.printf("parallel resources %s: %d threads x %d rounds: %d ms%n", sharedRes ? "shared" : "own", threads, rounds, (System.nanoTime() - t1) / 1_000_000);
        }
    }
}
