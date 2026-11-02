package dev.stxt.pdf.model;

import dev.stxt.pdf.model.StxtPdfException.Kind;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ready-made {@link Cancellation}s and what {@link Cancellation#check()} throws (reference/implementation/api.md). */
class CancellationTest {

    @Test
    void noneIsNeverRequestedAndChecksQuietly() {
        assertFalse(Cancellation.NONE.requested());
        Cancellation.NONE.check();
    }

    @Test
    void checkThrowsACancelledExceptionOfItsOwnKindWithoutLine() {
        Cancellation requested = () -> true;
        CancelledException e = assertThrows(CancelledException.class, requested::check);
        assertEquals(Kind.CANCELLED, e.kind());
        assertEquals("CANCELLED", e.code());
        assertEquals(CancelledException.CODE, e.code());
        assertEquals(StxtPdfException.NO_LINE, e.line());
        assertFalse(e.inStyles());
        assertTrue(e.getMessage().startsWith("The conversion was cancelled"), e.getMessage());
    }

    @Test
    void aTimeoutOfZeroOrNegativeIsRequestedAtOnce() {
        assertTrue(Cancellation.timeout(Duration.ZERO).requested());
        assertTrue(Cancellation.timeout(Duration.ofSeconds(-5)).requested());
    }

    @Test
    void aTimeoutThatHasNotPassedIsNotRequested() {
        assertFalse(Cancellation.timeout(Duration.ofHours(1)).requested());
    }

    @Test
    void aTimeoutIsRequestedOnceItPasses() throws InterruptedException {
        Cancellation cancellation = Cancellation.timeout(Duration.ofMillis(20));
        long start = System.nanoTime();
        while (!cancellation.requested()) {
            assertTrue(System.nanoTime() - start < Duration.ofSeconds(10).toNanos(), "never requested");
            Thread.sleep(1);
        }
        assertTrue(System.nanoTime() - start >= Duration.ofMillis(20).toNanos(), "requested too early");
    }

    @Test
    void aDeadlineInThePastIsRequestedAndOneInTheFutureIsNot() {
        assertTrue(Cancellation.deadline(Instant.now().minusSeconds(1)).requested());
        assertFalse(Cancellation.deadline(Instant.now().plusSeconds(3600)).requested());
    }

    /** Further than the clock can count: it does not overflow, it just never (or always) stops. */
    @Test
    void aDeadlineBeyondTheClockDoesNotOverflow() {
        assertFalse(Cancellation.deadline(Instant.MAX).requested());
        assertTrue(Cancellation.deadline(Instant.MIN).requested());
        assertFalse(Cancellation.timeout(Duration.ofDays(365 * 1000)).requested());
    }

    @Test
    void whenInterruptedFollowsTheFlagOfTheThreadWithoutClearingIt() {
        Cancellation cancellation = Cancellation.whenInterrupted();
        assertFalse(cancellation.requested());
        Thread.currentThread().interrupt();
        try {
            assertTrue(cancellation.requested());
            assertTrue(cancellation.requested(), "the flag was cleared");
        } finally {
            assertTrue(Thread.interrupted(), "the flag was cleared");
        }
        assertFalse(cancellation.requested());
    }

    @Test
    void orIsRequestedWhenEitherIs() {
        AtomicBoolean a = new AtomicBoolean();
        AtomicBoolean b = new AtomicBoolean();
        Cancellation either = ((Cancellation) a::get).or(b::get);
        assertFalse(either.requested());
        b.set(true);
        assertTrue(either.requested());
        b.set(false);
        a.set(true);
        assertTrue(either.requested());
    }

    /** Combining with {@link Cancellation#NONE} costs nothing: it is the other one. */
    @Test
    void orWithNoneIsTheOtherOne() {
        Cancellation one = () -> false;
        assertSame(one, Cancellation.NONE.or(one));
        assertSame(one, one.or(Cancellation.NONE));
        assertSame(Cancellation.NONE, Cancellation.NONE.or(Cancellation.NONE));
    }
}
