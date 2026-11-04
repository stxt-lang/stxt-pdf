package dev.stxt.pdf.convert;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Attachments;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.CancelledException;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.model.StxtPdfException.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cancelling a conversion (reference/implementation/api.md): the engine asks the {@link Cancellation} in every phase,
 * stops as soon as it says so, with a {@link CancelledException} that nothing wraps, and writes nothing.
 */
class ConversionCancellationTest {

    private static final String HEADER = Docs.header(200, 200);
    private static final String BASE = Docs.STYLES;

    /** Every piece of the engine that has a point where a cancelled conversion stops. */
    private static final List<String> PHASES = List.of(
            "dev.stxt.pdf.tree.BoxTreeBuilder",
            "dev.stxt.pdf.layout.Engine",
            "dev.stxt.pdf.layout.Paginator",
            "dev.stxt.pdf.layout.Overflow",
            "dev.stxt.pdf.paint.ContrastCheck",
            "dev.stxt.pdf.paint.PdfDocumentWriter",
            "dev.stxt.pdf.paint.PdfImages");

    private static Converter converter() {
        return Converter.builder().styles(BASE).build();
    }

    /** A document with every kind of work: text, a generated image, an image file and several pages. */
    private static InlineNode everything() {
        return Docs.read(HEADER, "\tParagraph: Un párrafo que se maqueta con las métricas reales de la fuente.",
                "\tBarcode: https://stxt.dev", "\t\tAlt: Enlace", "\t\tBox:", "\t\t\tWidth: 40",
                "\tImage: test/square.png", "\t\tBox:", "\t\t\tWidth: 40",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 500",
                "\tParagraph: El último párrafo, en la tercera página.");
    }

    /** The engine classes on the stack of the caller. */
    private static Set<String> engineClassesOnTheStack() {
        return StackWalker.getInstance().walk(frames -> frames.map(StackWalker.StackFrame::getClassName)
                .filter(name -> name.startsWith("dev.stxt.pdf.") && !name.startsWith("dev.stxt.pdf.model."))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    /** A cancellation requested the first time {@code phase} is on the stack. */
    private static Cancellation inPhase(String phase) {
        return () -> engineClassesOnTheStack().contains(phase);
    }

    @Test
    void aDeadlineAlreadyPassedStopsBeforeAnythingIsBuiltAndWritesNothing() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CancelledException e = assertThrows(CancelledException.class,
                () -> converter().toPdf(everything(), out, Attachments.NONE, Cancellation.timeout(Duration.ZERO)));
        assertEquals(Kind.CANCELLED, e.kind());
        assertEquals("CANCELLED", e.code());
        assertEquals(StxtPdfException.NO_LINE, e.line());
        assertEquals(0, out.size());
    }

    /** Without a cancellation, or with one that never asks, the conversion is the usual one. */
    @Test
    void aCancellationThatNeverAsksChangesNothing() throws IOException {
        ByteArrayOutputStream plain = new ByteArrayOutputStream();
        ByteArrayOutputStream watched = new ByteArrayOutputStream();
        converter().toPdf(everything(), plain);
        converter().toPdf(everything(), watched, Attachments.NONE, Cancellation.timeout(Duration.ofHours(1)));
        assertTrue(plain.size() > 0);
        assertEquals(plain.size(), watched.size());
    }

    /** The engine asks in every phase: building the tree, layout, pagination, checks, painting and images. */
    @Test
    void theEngineAsksInEveryPhase() throws IOException {
        Set<String> asked = new LinkedHashSet<>();
        Cancellation recording = () -> {
            asked.addAll(engineClassesOnTheStack());
            return false;
        };
        converter().toPdf(everything(), new ByteArrayOutputStream(), Attachments.NONE, recording);
        for (String phase : PHASES) {
            assertTrue(asked.contains(phase), "never asked from " + phase + "; asked from " + asked);
        }
    }

    /**
     * Requested in any phase, the conversion stops there: the exception comes out of that phase, as a
     * {@link CancelledException} and not wrapped in anything else, and nothing has been written.
     */
    @ParameterizedTest
    @ValueSource(strings = {"dev.stxt.pdf.tree.BoxTreeBuilder", "dev.stxt.pdf.layout.Engine",
            "dev.stxt.pdf.layout.Paginator", "dev.stxt.pdf.layout.Overflow", "dev.stxt.pdf.paint.ContrastCheck",
            "dev.stxt.pdf.paint.PdfDocumentWriter", "dev.stxt.pdf.paint.PdfImages"})
    void aCancellationRequestedInAnyPhaseStopsThereAndWritesNothing(String phase) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CancelledException e = assertThrows(CancelledException.class,
                () -> converter().toPdf(everything(), out, Attachments.NONE, inPhase(phase)));
        assertTrue(Arrays.stream(e.getStackTrace()).anyMatch(f -> f.getClassName().equals(phase)),
                "not thrown from " + phase);
        assertEquals(0, out.size());
    }

    /** Once it says so, the engine does no more work: nothing catches the exception and goes on. */
    @Test
    void onceRequestedTheEngineAsksNoMore() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger afterRequested = new AtomicInteger();
        Cancellation late = () -> {
            int n = calls.incrementAndGet();
            if (n > 40) {
                afterRequested.incrementAndGet();
            }
            return n >= 40;
        };
        assertThrows(CancelledException.class,
                () -> converter().toPdf(everything(), new ByteArrayOutputStream(), Attachments.NONE, late));
        assertEquals(40, calls.get());
        assertEquals(0, afterRequested.get());
    }

    @Test
    void theCancellationOfTheConverterAndThatOfTheCallBothStop() throws IOException {
        AtomicBoolean flag = new AtomicBoolean();
        Converter shared = Converter.builder().styles(BASE).cancellation(flag::get).build();
        shared.toPdf(everything(), new ByteArrayOutputStream());
        flag.set(true);
        assertThrows(CancelledException.class, () -> shared.toPdf(everything(), new ByteArrayOutputStream()));
        assertThrows(CancelledException.class, () -> shared.layOut(everything()));
        assertThrows(CancelledException.class,
                () -> converter().toPdf(everything(), new ByteArrayOutputStream(), Attachments.NONE, flag::get));
        assertThrows(CancelledException.class, () -> converter().layOut(everything(), flag::get));
        assertEquals(Cancellation.NONE, converter().cancellation());
    }

    @Test
    void anInterruptedThreadStopsTheConversionAndKeepsItsFlag() {
        Converter interruptible = Converter.builder().styles(BASE).cancellation(Cancellation.whenInterrupted()).build();
        Thread.currentThread().interrupt();
        boolean kept;
        try {
            assertThrows(CancelledException.class,
                    () -> interruptible.toPdf(everything(), new ByteArrayOutputStream()));
        } finally {
            kept = Thread.interrupted();
        }
        assertTrue(kept, "the interrupt flag was cleared");
    }

    /** Painting and the SVG of a document already laid out can be cancelled too, before anything is written. */
    @Test
    void paintingALaidOutDocumentCanBeCancelledAndWritesNothing() {
        LaidOutDocument laidOut = converter().layOut(everything());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(CancelledException.class, () -> converter().toPdf(laidOut, out, Attachments.NONE, () -> true));
        assertEquals(0, out.size());
        assertThrows(CancelledException.class, () -> converter().toSvg(laidOut, () -> true));
        assertFalse(converter().toSvg(laidOut, Cancellation.NONE).isEmpty());
    }

    /** A costly document with a short deadline stops soon after it, instead of running to the end. */
    @Test
    void aCostlyDocumentStopsSoonAfterItsDeadline() {
        String[] lines = new String[1500];
        Arrays.fill(lines, "\tParagraph: Un párrafo más, de los muchos que hacen que la conversión tarde.");
        InlineNode costly = Docs.read(HEADER, lines);
        long start = System.nanoTime();
        assertThrows(CancelledException.class, () -> converter().toPdf(costly, new ByteArrayOutputStream(),
                Attachments.NONE, Cancellation.timeout(Duration.ofMillis(20))));
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertTrue(elapsed < 3000, "took " + elapsed + " ms to stop");
    }
}
