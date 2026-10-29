// Sonda de QA (ROADMAP-QA-ARQ.md, PF-*): documentos de texto largo de 200 a 12 800 párrafos; da el tiempo de
// lectura, layout y pintado y los aciertos y fallos de la caché. Para el perfil: añade
// -XX:StartFlightRecording=filename=rec.jfr,settings=profile y mira "jfr print --events jdk.ExecutionSample".
import dev.stxt.InlineNode;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.paint.PdfCanvas;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import java.io.*;
import java.util.*;

public class ScaleProbe {
    static String head() {
        return "Document (dev.stxt.boxes):\n\tMedia:\n\t\tPaper: A4\n\t\tMargin: 40\n\tTitle: Perf\n\tLanguage: es\n"
             + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
             + "\tFixed:\n\t\tOffset: auto 0 0 0\n\t\tText: Pagina {page} de {pages}\n\tSection:\n";
    }
    static String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.";

    static String big(int paras, boolean headings) {
        StringBuilder sb = new StringBuilder(head());
        for (int i = 0; i < paras; i++) {
            if (headings && i % 20 == 0) sb.append(i == 0 ? "\t\tHeading 1: Capitulo " + i + "\n\t\t\tId: h" + i + "\n" : "\t\tHeading 1: Capitulo " + i + "\n\t\t\tId: h" + i + "\n");
            sb.append("\t\tParagraph: ").append(LOREM).append(' ').append(i).append("\n");
        }
        return sb.toString();
    }

    static void run(String label, String text, Resources r) throws Exception {
        long t0 = System.nanoTime();
        InlineNode doc = new DocumentReader().read(text);
        long t1 = System.nanoTime();
        Engine e = new Engine(new PdfMetrics(r), r);
        LaidOutDocument lo = e.layOutDocument(doc);
        long t2 = System.nanoTime();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        PdfCanvas.write(lo, r, dev.stxt.boxes.model.Limits.DEFAULT, o, false);
        long t3 = System.nanoTime();
        System.out.printf("%-28s paginas=%4d  leer=%5d ms  layout=%6d ms  pintar=%6d ms  pdf=%d KB  %s%n", label, lo.pages().size(),
            (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (t3 - t2) / 1_000_000, o.size() / 1024, e);
    }

    public static void main(String[] a) throws Exception {
        Resources r = new Resources();
        run("calentar 50", big(50, false), r);
        run("calentar 50", big(50, false), r);
        for (int n : new int[]{200, 800, 3200, 12800}) {
            long used0 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            run("parrafos " + n, big(n, false), r);
        }
        System.out.println("max heap MB: " + Runtime.getRuntime().maxMemory() / 1048576);
    }
}
