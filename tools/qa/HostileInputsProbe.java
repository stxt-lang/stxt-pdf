// Sonda de QA (ROADMAP-QA-ARQ.md, QA-06): entradas patológicas (anidado, rejillas enormes, palabras largas,
// muchas páginas, índice con muchas referencias) con tiempo y resultado o error de cada una.
import dev.stxt.InlineNode;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.paint.PdfDocumentWriter;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class HostileInputsProbe {
    static String head(String extraStyles) {
        return "Document (dev.stxt.boxes):\n\tMedia:\n\t\tPaper: A4\n\t\tMargin: 40\n\tTitle: T\n\tLanguage: es\n"
             + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + extraStyles
             + "\tSection:\n";
    }
    static String tabs(int n) { return "\t".repeat(n); }

    static String nested(int depth) {
        StringBuilder sb = new StringBuilder(head(""));
        for (int i = 0; i < depth; i++) sb.append(tabs(2 + i)).append("Block:\n");
        sb.append(tabs(2 + depth)).append("Text: hola\n");
        return sb.toString();
    }
    static String gridRepeat(int n) {
        return head("") + "\t\tGrid:\n\t\t\tColumns: repeat(" + n + ", 1fr)\n\t\t\tArea:\n\t\t\t\tText: hola\n";
    }
    static String longWord(int n) { return head("") + "\t\tText: " + "x".repeat(n) + "\n"; }
    static String manyPages(int n) { // n bloques con salto de pagina forzado
        StringBuilder sb = new StringBuilder(head(""));
        for (int i = 0; i < n; i++) sb.append("\t\tText: pagina\n\t\t\tBreak:\n\t\t\t\tAfter: page\n");
        return sb.toString();
    }
    static String refs(int n) { // n encabezados + indice
        StringBuilder sb = new StringBuilder(head(""));
        sb.append("\t\tContents:\n");
        for (int i = 0; i < n; i++) sb.append("\t\tBlock:\n\t\t\tBreak:\n\t\t\t\tBefore: page\n\t\t\tHeading 1:\n\t\t\t\tId: h").append(i).append("\n\t\t\t\tText: Capitulo ").append(i).append("\n\t\t\tParagraph: Ver pagina {page of: h").append(i).append("} y texto de relleno para el capitulo.\n");
        return sb.toString();
    }

    static void run(String label, String text, long timeoutSec) {
        ExecutorService ex = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(null, r, "w", 0); t.setDaemon(true); return t; });
        long t0 = System.nanoTime();
        Future<String> f = ex.submit(() -> {
            Resources r = new Resources();
            InlineNode doc = new DocumentReader().read(text);
            Engine e = new Engine(new PdfMetrics(r), r);
            var lo = e.layOutDocument(doc);
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            PdfDocumentWriter.write(lo, r, dev.stxt.boxes.model.Limits.DEFAULT, o, false);
            return "OK paginas=" + lo.pages().size() + " pdf=" + o.size() / 1024 + "KB";
        });
        String res;
        try { res = f.get(timeoutSec, TimeUnit.SECONDS); }
        catch (TimeoutException e) { res = "TIMEOUT > " + timeoutSec + "s"; }
        catch (ExecutionException e) { Throwable c = e.getCause(); res = c.getClass().getSimpleName() + ": " + String.valueOf(c.getMessage()).lines().findFirst().orElse(""); }
        catch (Exception e) { res = e.toString(); }
        System.out.printf("%-26s %6d ms  %s%n", label, (System.nanoTime() - t0) / 1_000_000, res);
    }

    public static void main(String[] a) {
        run("calentar", nested(3), 60);
        for (int d : new int[]{50, 200, 1000, 3000}) run("anidado " + d, nested(d), 60);
        for (int n : new int[]{100, 5000, 100000}) run("grid repeat(" + n + ")", gridRepeat(n), 60);
        for (int n : new int[]{1000, 100000, 1000000}) run("palabra de " + n, longWord(n), 60);
        for (int n : new int[]{500, 2000}) run("saltos de pagina " + n, manyPages(n), 120);
        for (int n : new int[]{50, 200, 800, 1600}) run("indice con " + n + " cap.", refs(n), 280);
    }
}
