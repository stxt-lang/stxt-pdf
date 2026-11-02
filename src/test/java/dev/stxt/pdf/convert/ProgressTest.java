package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Attachments;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.Progress;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The progress of a conversion (reference/implementation/api.md): a {@link Progress} given with the call is told
 * each page of the flow laid out, the number of pages, each page painted and the end, in that order and on the
 * thread that converts.
 */
class ProgressTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 200\n\t\tHeight: 200\n";
    private static final String BASE = "Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tTypography:\n\t\t\tFont: IBM Plex Sans\n"
            + "\t\t\tSize: 10\n";

    /** Every event, in order, as text. */
    private static final class Events implements Progress {
        final List<String> events = new ArrayList<>();
        final Thread thread = Thread.currentThread();

        @Override
        public void pageLaidOut(int number) {
            add("laid out " + number);
        }

        @Override
        public void laidOut(int pages) {
            add("laid out: " + pages + " pages");
        }

        @Override
        public void pagePainted(int number, int pages) {
            add("painted " + number + " of " + pages);
        }

        @Override
        public void done() {
            add("done");
        }

        private void add(String event) {
            assertSame(thread, Thread.currentThread(), "on the thread that converts");
            events.add(event);
        }
    }

    private static Converter converter() {
        return Converter.builder().styles(BASE).build();
    }

    private static InlineNode document(String... lines) {
        return READER.read(Docs.sectioned(HEADER + String.join("\n", lines) + "\n"));
    }

    /** Two pages: a text, a block taller than the page, which breaks, and a last text. */
    private static InlineNode twoPages() {
        return document("\tParagraph: Primera página.", "\tBlock:", "\t\tBox:", "\t\t\tHeight: 300",
                "\tParagraph: Tercera página.");
    }

    @Test
    void toPdfTellsEachPageLaidOutAndPaintedAndTheEnd() throws IOException {
        Events events = new Events();
        converter().toPdf(twoPages(), new ByteArrayOutputStream(), Attachments.NONE, Cancellation.NONE, events);
        assertEquals(List.of("laid out 1", "laid out 2", "laid out: 2 pages", "painted 1 of 2", "painted 2 of 2",
                "done"), events.events);
    }

    @Test
    void layOutTellsThePagesAndTheTotalButNotTheEnd() {
        Events events = new Events();
        LaidOutDocument laidOut = converter().layOut(twoPages(), Cancellation.NONE, events);
        assertEquals(2, laidOut.pages().size());
        assertEquals(List.of("laid out 1", "laid out 2", "laid out: 2 pages"), events.events);
    }

    @Test
    void toSvgTellsEachPageMadeAndTheEnd() {
        Converter converter = converter();
        LaidOutDocument laidOut = converter.layOut(twoPages());
        Events events = new Events();
        assertEquals(2, converter.toSvg(laidOut, Cancellation.NONE, events).size());
        assertEquals(List.of("painted 1 of 2", "painted 2 of 2", "done"), events.events);
    }

    @Test
    void writingIntoAnOpenDocumentTellsTheSameAsAFile() throws IOException {
        Events events = new Events();
        try (PDDocument target = new PDDocument()) {
            converter().toPdf(twoPages(), target, Attachments.NONE, Cancellation.NONE, events);
            assertEquals(2, target.getNumberOfPages());
        }
        assertEquals(List.of("laid out 1", "laid out 2", "laid out: 2 pages", "painted 1 of 2", "painted 2 of 2",
                "done"), events.events);
    }

    /** A reference to a page is paginated twice (reference/common/references.md): the count starts over. */
    @Test
    void aSecondPaginationPassCountsFromOneAgain() {
        Events events = new Events();
        converter().layOut(document("\tText: La meta está en la página {page of: meta}.", "\tBlock:", "\t\tBox:",
                "\t\t\tHeight: 300", "\tText: La meta.", "\t\tId: meta"), Cancellation.NONE, events);
        assertEquals(List.of("laid out 1", "laid out 2", "laid out 1", "laid out 2", "laid out: 2 pages"),
                events.events);
    }

    @Test
    void whatTheListenerThrowsEndsTheConversionAsAnInternalError() {
        Progress failing = new Progress() {
            @Override
            public void pageLaidOut(int number) {
                throw new IllegalStateException("the listener failed");
            }
        };
        InternalException e = assertThrows(InternalException.class,
                () -> converter().toPdf(twoPages(), new ByteArrayOutputStream(), Attachments.NONE,
                        Cancellation.NONE, failing));
        assertEquals("the listener failed", e.getCause().getMessage());
    }

    @Test
    void noneTellsNothing() throws IOException {
        converter().toPdf(twoPages(), new ByteArrayOutputStream(), Attachments.NONE, Cancellation.NONE,
                Progress.NONE);
    }
}
