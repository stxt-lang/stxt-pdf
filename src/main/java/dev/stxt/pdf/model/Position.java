package dev.stxt.pdf.model;

/**
 * Cómo participa la caja en el flujo de su padre: {@code Frame}, {@code Overlay} y {@code Page overlay}; el resto,
 * {@code STATIC}.
 */
public enum Position { STATIC, FRAME, OVERLAY, PAGE_OVERLAY }
