package dev.stxt.pdf.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Whether a conversion has to stop before it finishes (reference/implementation/api.md). The caller gives one with
 * each conversion ({@code Converter.toPdf(document, target, attachments, cancellation)}) or to the {@code Converter}
 * for all of them, and the engine asks it where it works: at each element when building the tree, at each layout
 * that is not in the cache, at each page when paginating, checking and painting, and at each image it embeds. The
 * first time it answers {@code true} the conversion ends with a {@link CancelledException}, which nothing catches on
 * the way out, and nothing has been written to the destination.
 *
 * <p>It is a functional interface: any {@code () -> boolean} works. The usual ones are here: a deadline
 * ({@link #deadline}, {@link #timeout}), the interruption of the thread ({@link #whenInterrupted}) and several at
 * once ({@link #or}). {@link #NONE} never stops.</p>
 *
 * <p>It is asked often (thousands of times per second in a large document), so an implementation has to be cheap and
 * must not block; the ones here read the monotonic clock or a flag of the thread. Once it answers {@code true} it
 * should keep answering {@code true}.</p>
 */
@FunctionalInterface
public interface Cancellation {

    /** A conversion that is never cancelled. */
    Cancellation NONE = () -> false;

    /** Whether the conversion has to stop now. */
    boolean requested();

    /**
     * Throws if {@link #requested()}; the engine calls it at each point where it can stop.
     *
     * @throws CancelledException if the conversion has to stop
     */
    default void check() {
        if (requested()) {
            throw new CancelledException();
        }
    }

    /** A cancellation that is requested when this one or {@code other} is. */
    default Cancellation or(Cancellation other) {
        Objects.requireNonNull(other, "other");
        if (this == NONE) {
            return other;
        }
        if (other == NONE) {
            return this;
        }
        return () -> requested() || other.requested();
    }

    /**
     * Requested from {@code when} on, measured with the monotonic clock of the JVM from the moment this is created:
     * a change of the wall clock afterwards does not move it. A moment already past is requested at once.
     */
    static Cancellation deadline(Instant when) {
        Objects.requireNonNull(when, "when");
        return timeout(Duration.between(Instant.now(), when));
    }

    /**
     * Requested once {@code duration} has passed since this is created, by the monotonic clock of the JVM
     * ({@link System#nanoTime()}). Zero or negative is requested at once.
     */
    static Cancellation timeout(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        long nanos;
        try {
            nanos = duration.toNanos();
        } catch (ArithmeticException e) {
            // Further than the clock can count (about 292 years): never, or at once if it is in the past.
            return duration.isNegative() ? () -> true : NONE;
        }
        long end = System.nanoTime() + nanos;
        // The difference, not the comparison: nanoTime can wrap around.
        return () -> System.nanoTime() - end >= 0;
    }

    /**
     * Requested while the thread that converts is interrupted ({@link Thread#isInterrupted()}); it does not clear the
     * flag. It is the one for a service that stops a conversion with {@link Thread#interrupt()}: it depends on the
     * thread that asks and not on a document, so it can be shared by every conversion of a {@code Converter}
     * ({@code Converter.builder().cancellation(Cancellation.whenInterrupted())}).
     */
    static Cancellation whenInterrupted() {
        return () -> Thread.currentThread().isInterrupted();
    }
}
