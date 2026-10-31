package dev.stxt.pdf.layout.grid;

/**
 * What an item contributes to track sizing on one axis (reference/components/grid.md). For columns, its narrowest
 * and natural widths; for rows, its height after the layout with the width already resolved.
 *
 * @param track first track it occupies (from 0)
 * @param span  number of tracks it occupies
 */
public record ItemContribution(int track, int span, double narrowest, double natural) {
}
