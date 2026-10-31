package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.tree.Box;

/**
 * Lo que un lienzo hace con el significado de lo que pinta, aparte de dibujarlo: las secuencias de texto del PDF
 * etiquetado (reference/implementation/pdf.md) y las zonas de los enlaces (reference/common/links.md). Cada {@link
 * Canvas} da la suya ({@link Canvas#tagging()}): la de {@link PdfCanvas} es {@link PdfTagging}; la de un lienzo que
 * solo dibuja, {@link #NONE}, que no hace nada. No cambia lo que se ve.
 */
public interface Tagging {

    /** La de un lienzo que solo dibuja: no marca el texto ni crea enlaces. */
    Tagging NONE = new Tagging() {
        @Override
        public void beginText(Box box) {
        }

        @Override
        public void beginLinkText(Box box) {
        }

        @Override
        public void endText() {
        }

        @Override
        public void beginActualText(String text) {
        }

        @Override
        public void endActualText() {
        }

        @Override
        public void link(Box box, Rect area, String destination, String description) {
        }
    };

    /**
     * Empieza el texto de un fragmento de la caja: las líneas que siguen, hasta {@link #endText()},
     * son su contenido (el PDF etiquetado lo marca, reference/implementation/pdf.md).
     */
    void beginText(Box box);

    /**
     * Empieza el texto de un enlace dentro del texto de la caja (reference/implementation/pdf.md): como
     * {@link #beginText(Box)}, pero lo que sigue, hasta {@link #endText()}, es el contenido de un
     * {@code Link} del PDF etiquetado. Las zonas clicables de ese texto ({@link #link}) se piden
     * después de empezarlo.
     */
    void beginLinkText(Box box);

    /** Termina el texto empezado con {@link #beginText(Box)} o {@link #beginLinkText(Box)}. */
    void endText();

    /**
     * Lo que se pinta hasta {@link #endActualText()}, dentro de un texto empezado, se extrae como {@code text}
     * (reference/implementation/pdf.md): el texto con interletraje, cuyas letras separadas harían adivinar espacios.
     */
    void beginActualText(String text);

    /** Termina lo empezado con {@link #beginActualText(String)}. */
    void endActualText();

    /**
     * Una zona clicable (reference/common/links.md): el área {@code area} de la página lleva a
     * {@code destination} ({@link dev.stxt.pdf.model.Links}). {@code box} es la caja a la que
     * pertenece (el PDF etiquetado cuelga el enlace de ella) y {@code description} el texto que
     * se lee en su lugar, que puede ser {@code null}.
     */
    void link(Box box, Rect area, String destination, String description);
}
