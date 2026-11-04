// QA probe: long text documents of 200 to 12 800 paragraphs, with {page} and {pages}
// in a Page overlay; it gives the time of reading, layout and painting and the hits and misses of the cache, also per
// page. Those of 2000 and 2100 paragraphs fall on both sides of page 100, where {pages} needs one more digit;
// the "toc" ones also carry a Table of contents and a {page of: ...} per chapter, which repeat until they converge; the
// "chapters" ones group the paragraphs in blocks headed by a Heading 1 whose top margin comes out of the block and moves
// it down once laid out (five paragraphs per block, a quarter of a page; "long chapters", twenty, more than a page). The
// "table", "grid" and "stack" ones are containers of 500 and 2000 units (rows, or items) over many pages: what they
// arrange before breaking (placement, track sizing, bands, merged borders) is memoized per container, and the misses per
// page tell whether it is. The "contrast" ones are dense pages (a table in small type with cell backgrounds, Merge borders
// and underlined and struck words): thousands of runs and layers per page, and the time of the contrast check apart.
// The "heads" ones are books with a {heading 1} in the Page overlay and a chapter every 40 paragraphs (about three
// pages): at each page where the running head changes the engine forgets what it stored of that text and its ancestors.
// For the profile: -XX:StartFlightRecording=filename=rec.jfr,settings=profile, then "jfr print --events
// jdk.ExecutionSample".
import dev.stxt.InlineNode;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.paint.ContrastCheck;
import dev.stxt.pdf.paint.PdfDocumentWriter;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import java.io.*;
import java.util.*;

public class ScaleProbe {
    static String head() {
        return "Document (dev.stxt.pdf):\n\tPage:\n\t\tPaper: A4\n\t\tMargin: 40\n\tTitle: Perf\n\tLanguage: es\n"
             + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
             + "\tPage overlay:\n\t\tOffset: auto 0 0 0\n\t\tText: Pagina {page} de {pages}\n\tSection:\n";
    }
    static String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.";

    static String big(int paras, boolean headings) {
        StringBuilder sb = new StringBuilder(head());
        if (headings) sb.append("\t\tTable of contents:\n");
        for (int i = 0; i < paras; i++) {
            if (headings && i % 20 == 0) {
                sb.append("\t\tHeading 1:\n\t\t\tId: h" + i + "\n\t\t\tText: Capitulo " + i + "\n");
                sb.append("\t\tParagraph: Este capitulo empieza en la pagina {page of: h" + i + "}.\n");
            }
            sb.append("\t\tParagraph: ").append(LOREM).append(' ').append(i).append("\n");
        }
        return sb.toString();
    }

    /**
     * Chapters of perChapter paragraphs: a Block with a Heading 1 whose top margin comes out of it and moves it down once
     * laid out, and its paragraphs. A short chapter fits whole on the page where it starts; a long one is broken there.
     */
    static String chapters(int paras, int perChapter) {
        StringBuilder sb = new StringBuilder(head());
        for (int i = 0; i < paras; i++) {
            if (i % perChapter == 0) {
                sb.append("\t\tBlock:\n\t\t\tHeading 1:\n\t\t\t\tText: Capitulo " + i + "\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 24 0 12 0\n");
            }
            sb.append("\t\t\tParagraph: ").append(LOREM).append(' ').append(i).append("\n");
        }
        return sb.toString();
    }

    /**
     * A book of paras paragraphs with a Heading 1 every perChapter of them and the running head {heading 1} in the Page
     * overlay: each page where the chapter changes lays out the Page overlay again, after forgetting what the engine
     * stored of its text.
     */
    static String heads(int paras, int perChapter) {
        StringBuilder sb = new StringBuilder(head().replace("Text: Pagina {page}", "Text: {heading 1}, pagina {page}"));
        for (int i = 0; i < paras; i++) {
            if (i % perChapter == 0) {
                sb.append("\t\tHeading 1:\n\t\t\tText: Capitulo ").append(i / perChapter + 1).append('\n');
            }
            sb.append("\t\tParagraph: ").append(LOREM).append(' ').append(i).append("\n");
        }
        return sb.toString();
    }

    /** The head with the rules of the tables: cells with padding and border, and Merge borders on every table. */
    static String tableHead() {
        return head().replace("\tPage overlay:", "\t\tFor element: Cell\n\t\t\tBox:\n\t\t\t\tPadding: 2\n\t\t\t\tBorder: 1\n"
             + "\t\tFor element: Table\n\t\t\tBox:\n\t\t\t\tMerge borders: true\n\tPage overlay:");
    }

    /**
     * A table of rows body rows and four columns, with a header row repeated on each page and Merge borders from a rule:
     * each page repeats the header and draws its lines. The whole table is arranged (placement, track sizing, bands,
     * lines) before breaking it between rows.
     */
    static String table(int rows) {
        StringBuilder sb = new StringBuilder(tableHead());
        sb.append("\t\tTable:\n\t\t\tColumn sizes: auto auto auto *\n\t\t\tHeader rows:\n\t\t\t\tRow:\n");
        for (int c = 1; c <= 4; c++) {
            sb.append("\t\t\t\t\tCell: Columna ").append(c).append('\n');
        }
        sb.append("\t\t\tBody:\n");
        for (int i = 0; i < rows; i++) {
            sb.append("\t\t\t\tRow:\n");
            for (int c = 1; c <= 4; c++) {
                sb.append("\t\t\t\t\tCell: Fila ").append(i).append(", columna ").append(c).append('\n');
            }
        }
        return sb.toString();
    }

    /** A grid of rows rows and three columns of short texts; the rows are the fragmentation units. */
    static String grid(int rows) {
        StringBuilder sb = new StringBuilder(head());
        sb.append("\t\tGrid:\n\t\t\tColumn sizes: * * *\n");
        for (int i = 0; i < rows; i++) {
            for (int c = 1; c <= 3; c++) {
                sb.append("\t\t\tArea:\n\t\t\t\tText: Fila ").append(i).append(", columna ").append(c).append('\n');
            }
        }
        return sb.toString();
    }

    /** A vertical stack of items short paragraphs; the items are the fragmentation units. */
    static String stack(int items) {
        StringBuilder sb = new StringBuilder(head());
        sb.append("\t\tStack:\n\t\t\tDirection: vertical\n");
        for (int i = 0; i < items; i++) {
            sb.append("\t\t\tItem:\n\t\t\t\tText: Elemento ").append(i).append(" de la pila.\n");
        }
        return sb.toString();
    }

    /**
     * A table of rows body rows and six columns in size 6, with a background in every cell, Merge borders and, in each
     * cell, two underlined and one struck word: each line of a cell is several runs, and each underline, strike and line
     * of the table is one more layer for the contrast check, which looks for what lies under every run. Without the Page
     * overlay of the head: it would lie over the last row, on a line of the table, and its contrast would be too low.
     */
    static String contrast(int rows) {
        StringBuilder sb = new StringBuilder(tableHead().replace("\t\t\t\tBorder: 1\n",
                "\t\t\t\tBorder: 1\n\t\t\t\tBackground: #F4F2EC\n\t\t\tTypography:\n\t\t\t\tSize: 6\n"
                + "\t\t\t\tMarkup: markdown\n").replaceAll("\tPage overlay:\n(\t\t.*\n)*", ""));
        sb.append("\t\tTable:\n\t\t\tColumn sizes: * * * * * *\n\t\t\tBody:\n");
        for (int i = 0; i < rows; i++) {
            sb.append("\t\t\t\tRow:\n");
            for (int c = 1; c <= 6; c++) {
                sb.append("\t\t\t\t\tCell: ++Fila++ ").append(i).append(" ~~col~~ ").append(c).append(" ++fin++\n");
            }
        }
        return sb.toString();
    }

    /** Like {@link #run}, with the time of the contrast check of the laid-out document apart. */
    static void runContrast(String label, String text, Resources r) throws Exception {
        InlineNode doc = new DocumentReader().read(text);
        long t0 = System.nanoTime();
        LaidOutDocument lo = new Engine(new PdfMetrics(r), r).layOutDocument(doc);
        long t1 = System.nanoTime();
        ContrastCheck.check(lo);
        long t2 = System.nanoTime();
        System.out.printf("%-28s pages=%4d  layout=%6d ms  contrast=%6d ms  contrast/page=%.2f ms%n", label,
            lo.pages().size(), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000,
            (t2 - t1) / 1e6 / lo.pages().size());
    }

    static void run(String label, String text, Resources r) throws Exception {
        long t0 = System.nanoTime();
        InlineNode doc = new DocumentReader().read(text);
        long t1 = System.nanoTime();
        Engine e = new Engine(new PdfMetrics(r), r);
        LaidOutDocument lo = e.layOutDocument(doc);
        long t2 = System.nanoTime();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        PdfDocumentWriter.write(lo, r, dev.stxt.pdf.model.Limits.DEFAULT, o, false);
        long t3 = System.nanoTime();
        String stats = e.toString();
        long misses = Long.parseLong(stats.replaceAll(".*misses=(\\d+).*", "$1"));
        System.out.printf("%-28s pages=%4d  read=%5d ms  layout=%6d ms  paint=%6d ms  pdf=%d KB  %s  misses/page=%.1f%n",
            label, lo.pages().size(), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (t3 - t2) / 1_000_000, o.size() / 1024,
            stats, (double) misses / lo.pages().size());
    }

    /**
     * Without arguments, every probe; with "text", "chapters", "units" or "contrast", only those of the long texts, of
     * the chapters, of the containers or of the contrast check.
     */
    public static void main(String[] a) throws Exception {
        List<String> only = Arrays.asList(a);
        boolean text = a.length == 0 || only.contains("text");
        boolean chapters = a.length == 0 || only.contains("text") || only.contains("chapters");
        boolean units = a.length == 0 || only.contains("units");
        boolean contrast = a.length == 0 || only.contains("contrast");
        boolean heads = a.length == 0 || only.contains("heads");
        Resources r = new Resources();
        run("warm-up 50", big(50, false), r);
        run("warm-up 50", big(50, false), r);
        if (text) {
            for (int n : new int[]{200, 800, 2000, 2100, 3200, 12800}) {
                run("paragraphs " + n, big(n, false), r);
            }
            for (int n : new int[]{800, 3200}) {
                run("toc " + n, big(n, true), r);
            }
        }
        if (chapters) {
            for (int n : new int[]{800, 3200}) {
                run("chapters " + n, chapters(n, 5), r);
                run("long chapters " + n, chapters(n, 20), r);
            }
        }
        if (units) {
            run("warm-up table 100", table(100), r);
            for (int n : new int[]{500, 2000}) {
                run("table " + n, table(n), r);
                run("grid " + n, grid(n), r);
                run("stack " + n, stack(n), r);
            }
        }
        if (heads) {
            for (int n : new int[]{4200, 12800}) {
                run("heads " + n, heads(n, 40), r);
                // The same book without the running head: what the heads cost on top.
                run("heads control " + n, heads(n, 40).replace("{heading 1}, ", ""), r);
            }
        }
        if (contrast) {
            runContrast("warm-up contrast 100", contrast(100), r);
            runContrast("warm-up contrast 100", contrast(100), r);
            for (int n : new int[]{500, 2000}) {
                runContrast("contrast " + n, contrast(n), r);
            }
        }
        System.out.println("max heap MB: " + Runtime.getRuntime().maxMemory() / 1048576);
    }
}
