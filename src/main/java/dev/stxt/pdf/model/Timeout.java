package dev.stxt.pdf.model;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * The cancellation of {@link Cancellation#timeout(Duration)}, with the monotonic clock as a parameter so that the tests
 * can move it by hand instead of waiting (reference/implementation/testing.md). The engine always gives it
 * {@link System#nanoTime()}.
 */
final class Timeout {

    private Timeout() {
    }

    /**
     * {@return the cancellation requested once {@code duration} has passed on {@code clock}, counted from this call}
     *
     * @param duration the time the conversion has; zero or negative is requested at once
     * @param clock    a monotonic clock in nanoseconds, like {@link System#nanoTime()}: only differences mean
     *                 something, and it may wrap around
     */
    static Cancellation of(Duration duration, LongSupplier clock) {
        long nanos;
        try {
            nanos = duration.toNanos();
        } catch (ArithmeticException e) {
            // Further than the clock can count (about 292 years): never, or at once if it is in the past.
            return duration.isNegative() ? () -> true : Cancellation.NONE;
        }
        long end = clock.getAsLong() + nanos;
        // The difference, not the comparison: the clock can wrap around.
        return () -> clock.getAsLong() - end >= 0;
    }
}
