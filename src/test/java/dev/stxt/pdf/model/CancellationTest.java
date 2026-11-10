package dev.stxt.pdf.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.StxtPdfException.Kind;

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

    /** With a clock moved by hand ({@link Timeout}), not waiting: requested exactly when the duration has passed. */
    @Test
    void aTimeoutIsRequestedOnceItPasses() {
        AtomicLong clock = new AtomicLong(1_000);
        Cancellation cancellation = Timeout.of(Duration.ofMillis(20), clock::get);
        clock.addAndGet(Duration.ofMillis(20).toNanos() - 1);
        assertFalse(cancellation.requested());
        clock.incrementAndGet();
        assertTrue(cancellation.requested());
        clock.addAndGet(Duration.ofDays(1).toNanos());
        assertTrue(cancellation.requested(), "it keeps answering true");
    }

    /** The monotonic clock can wrap around: the deadline is compared by difference, not by value. */
    @Test
    void aTimeoutSurvivesTheClockWrappingAround() {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 10);
        Cancellation cancellation = Timeout.of(Duration.ofNanos(100), clock::get);
        clock.addAndGet(50);
        assertTrue(clock.get() < 0, "the clock has wrapped around");
        assertFalse(cancellation.requested());
        clock.addAndGet(50);
        assertTrue(cancellation.requested());
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
