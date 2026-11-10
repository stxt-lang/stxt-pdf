package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.BarcodeFormat;
import dev.stxt.pdf.model.ErrorCorrection;
import dev.stxt.pdf.model.Fit;

/**
 * The properties of the images: of an {@code Image} (reference/components/image.md), the alternative text, how it
 * fills its box and the page of a PDF; of a {@code Barcode} (reference/components/barcode.md), the alternative text,
 * its format, its error correction and its quiet zone. They are not inherited.
 */
public final class ImageStyle extends StyleGroup {

    /** Alternative text of an {@code Image}; {@code null} = decorative. */
    private String alt;
    private Fit fit = Fit.INSIDE;
    /** Page of a PDF, from 1. */
    private int pdfPage = 1;
    private BarcodeFormat barcodeFormat = BarcodeFormat.QR;
    private ErrorCorrection errorCorrection = ErrorCorrection.MEDIUM;
    /** Quiet zone of a {@code Barcode}, in modules on each side; {@code null} = that of its format. */
    private Integer quietZone;

    ImageStyle() {
    }

    public String alt() {
        return alt;
    }

    void setAlt(String v) {
        writable();
        alt = v;
    }

    public Fit fit() {
        return fit;
    }

    void setFit(Fit v) {
        writable();
        fit = v;
    }

    /** The page of a PDF that is drawn, from 1; 1 if it is not declared. */
    public int pdfPage() {
        return pdfPage;
    }

    void setPdfPage(int v) {
        writable();
        pdfPage = v;
    }

    /** The format of a {@code Barcode}; {@code qr} if it is not declared. */
    public BarcodeFormat barcodeFormat() {
        return barcodeFormat;
    }

    void setBarcodeFormat(BarcodeFormat v) {
        writable();
        barcodeFormat = v;
    }

    /** The error correction of a QR code; {@code medium} if it is not declared. */
    public ErrorCorrection errorCorrection() {
        return errorCorrection;
    }

    void setErrorCorrection(ErrorCorrection v) {
        writable();
        errorCorrection = v;
    }

    /** The quiet zone of a {@code Barcode}, in modules on each side; {@code null} if it is not declared. */
    public Integer quietZone() {
        return quietZone;
    }

    void setQuietZone(int v) {
        writable();
        quietZone = v;
    }
}
