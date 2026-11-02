package dev.stxt.pdf.model;

/**
 * The counter a {@code Figure} is numbered with (reference/components/figure.md): the figures or the tables of the
 * document, each with its own count. Without {@code Figure counter}, a {@code Figure} with a {@code Table} counts
 * among the tables and the rest, among the figures.
 */
public enum FigureCounter { FIGURES, TABLES }
