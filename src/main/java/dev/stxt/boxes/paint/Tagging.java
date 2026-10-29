package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;

/**
 * Lo que un lienzo hace con el significado de lo que pinta, aparte de dibujarlo (AR-05): las secuencias de texto
 * del PDF etiquetado (ESPECIFICACION.md §5.13) y las zonas de los enlaces (§5.20). Cada {@link Canvas} da la
 * suya ({@link Canvas#tagging()}): la de {@link PdfCanvas} es {@link PdfTagging}; la de un lienzo que solo dibuja,
 * {@link #NONE}, que no hace nada. No cambia lo que se ve.
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
        public void link(Box box, Rect area, String destination, String description) {
        }
    };

    /**
     * Empieza el texto de un fragmento de la caja: las líneas que siguen, hasta {@link #endText()},
     * son su contenido (el PDF etiquetado lo marca, §5.13).
     */
    void beginText(Box box);

    /**
     * Empieza el texto de un enlace dentro del texto de la caja (ESPECIFICACION.md §5.20): como
     * {@link #beginText(Box)}, pero lo que sigue, hasta {@link #endText()}, es el contenido de un
     * {@code Link} del PDF etiquetado. Las zonas clicables de ese texto ({@link #link}) se piden
     * después de empezarlo.
     */
    void beginLinkText(Box box);

    /** Termina el texto empezado con {@link #beginText(Box)} o {@link #beginLinkText(Box)}. */
    void endText();

    /**
     * Una zona clicable (ESPECIFICACION.md §5.20): el área {@code area} de la página lleva a
     * {@code destination} ({@link dev.stxt.boxes.model.Links}). {@code box} es la caja a la que
     * pertenece (el PDF etiquetado cuelga el enlace de ella) y {@code description} el texto que
     * se lee en su lugar, que puede ser {@code null}.
     */
    void link(Box box, Rect area, String destination, String description);
}
