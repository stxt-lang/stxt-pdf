package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Fit;

/**
 * The properties of an {@code Image} (reference/components/image.md): the alternative text, how it fills its box and
 * the page of a PDF. They are not inherited.
 */
public final class ImageStyle extends StyleGroup {

    /** Alternative text of an {@code Image}; {@code null} = decorative. */
    private String alt;
    private Fit fit = Fit.INSIDE;
    /** Page of a PDF, from 1. */
    private int pdfPage = 1;

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
}
