package dev.stxt.pdf.model;

/**
 * Distribution along an axis: the main axis of a {@code Stack} ({@code Horizontal} or {@code Vertical}) and
 * {@code Line distribution}. {@code BASELINE} is only read as the alignment of the cross axis of a horizontal
 * {@code Stack} ({@link Alignment#BASELINE}); it distributes nothing.
 */
public enum Distribution { START, END, CENTER, JUSTIFY, DISTRIBUTE, STRETCH, BASELINE }
