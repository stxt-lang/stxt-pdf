package dev.stxt.pdf.model;

/**
 * How much of a QR code can be damaged and still be read (reference/components/barcode.md): about 7 %, 15 %, 25 % and
 * 30 % of its modules. The higher it is, the more modules the same data needs.
 */
public enum ErrorCorrection { LOW, MEDIUM, QUARTILE, HIGH }
