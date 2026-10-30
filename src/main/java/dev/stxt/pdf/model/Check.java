package dev.stxt.pdf.model;

/**
 * Las comprobaciones de una conversión que quien convierte puede quitar (referencia/implementacion/api.md). Por
 * defecto se hacen todas; las que se quitan no fallan, y el resultado puede no verse como se espera o no ser accesible.
 * Las reglas del documento no están aquí: un documento que las incumple no se puede maquetar.
 */
public enum Check {

    /** El contraste del texto con su fondo (referencia/comun/pdf.md): con poco, {@code CONTRAST_LOW}. */
    CONTRAST,

    /**
     * Las fuentes de la página de un PDF que dibuja una {@code Image} (referencia/componentes/image.md): incrustadas y
     * con lo que dice qué carácter es cada glifo; si no, {@code IMAGE_FORMAT}. Sin ella, el visor puede sustituir una
     * fuente y el documento puede no ser PDF/UA.
     */
    PDF_IMAGE_FONTS,

    /**
     * Las anotaciones que se ven en la página de un PDF que dibuja una {@code Image}, como un campo de formulario
     * relleno o un comentario (referencia/componentes/image.md): si las tiene, {@code IMAGE_FORMAT}. Sin ella, la página
     * se dibuja sin ellas.
     */
    PDF_IMAGE_ANNOTATIONS
}
