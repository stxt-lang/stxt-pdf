package dev.stxt.pdf.layout;

/**
 * Adjoining vertical margins pending to be merged (reference/components/block.md). It keeps the largest positive
 * and the most negative separately, because merging several margins is their sum and cannot be computed from
 * partial results: {@code 20} and {@code −5} give 15, but adding {@code −10} gives 10, not 5.
 *
 * <p>It is immutable: it is part of {@link AdjoiningMargins}, which travels in {@link LayoutResult}.</p>
 *
 * @param positive the largest positive margin, or 0
 * @param negative the most negative margin, or 0
 */
public record MarginStrut(double positive, double negative) {

    public static final MarginStrut EMPTY = new MarginStrut(0, 0);

    public MarginStrut add(double margin) {
        return new MarginStrut(Math.max(positive, margin), Math.min(negative, margin));
    }

    public MarginStrut add(MarginStrut other) {
        return new MarginStrut(Math.max(positive, other.positive), Math.min(negative, other.negative));
    }

    /** Largest positive plus the most negative. */
    public double resolve() {
        return positive + negative;
    }

    /** Merges a series of adjoining margins. */
    public static double collapse(double... margins) {
        MarginStrut s = EMPTY;
        for (double m : margins) {
            s = s.add(m);
        }
        return s.resolve();
    }
}
