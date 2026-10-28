// Sonda de QA (ROADMAP-QA-ARQ.md, TS-02): convierte los ejemplos en secuencial y con 8 hilos y compara
// los PDF (sin fechas ni ID). Mide también la contención de un Resources compartido frente a uno por hilo
// (cuatro fases alternadas, para que el calentamiento de la JVM no favorezca a ninguno).
// Se ejecuta desde la raíz del repositorio; ver tools/qa/README.md.
import dev.stxt.InlineNode;
import dev.stxt.boxes.convert.Converter;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
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

    static byte[] norm(byte[] b) { // quita fechas e IDs
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
        System.out.printf("secuencial 1ª ronda (frío): %d ms%n", (System.nanoTime() - t0) / 1_000_000);
        for (int k = 0; k < 3; k++) {
            t0 = System.nanoTime();
            for (In in : ins) convert(in, shared);
            System.out.printf("secuencial caliente: %d ms (%d docs)%n", (System.nanoTime() - t0) / 1_000_000, ins.size());
        }
        // determinismo
        for (In in : ins) if (!Arrays.equals(base.get(in.name), norm(convert(in, shared)))) System.out.println("NO DETERMINISTA " + in.name);

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
                    return "hilo " + tt + " diferencias=" + bad;
                }));
            }
            int total = 0;
            for (var f : fs) { String s = f.get(); if (!s.endsWith("=0")) System.out.println(s); total++; }
            ex.shutdown();
            System.out.printf("paralelo resources %s: %d hilos x %d rondas: %d ms%n", sharedRes ? "compartido" : "propio", threads, rounds, (System.nanoTime() - t1) / 1_000_000);
        }
    }
}
