package dev.stxt.pdf.model;

/** The symbology of a {@code Barcode} (reference/components/barcode.md): its {@code Barcode format}. */
public enum BarcodeFormat {
    QR, DATA_MATRIX, GS1_DATA_MATRIX, PDF417, CODE_128, GS1_128, EAN_13;

    /** Whether it is a single row of bars, which is stretched to its box and can have a readable text under it. */
    public boolean isLinear() {
        return this == CODE_128 || this == GS1_128 || this == EAN_13;
    }

    /** How the documents write it: {@code gs1-data-matrix}. */
    public String value() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
