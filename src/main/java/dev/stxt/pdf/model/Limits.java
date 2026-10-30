package dev.stxt.pdf.model;

/**
 * Lo máximo que acepta una conversión (referencia/comun/limites.md). Un documento que lo supera es válido pero
 * pide más de lo que se quiere pagar en memoria y en tiempo, y falla con un error de
 * {@link StxtPdfException.Kind#LIMIT}: es lo que impide que un documento, o una imagen, pequeños en bytes
 * ocupen un servidor entero. Los valores por defecto ({@link #DEFAULT}) son conservadores y se miden en
 * {@code tools/qa/BombsProbe.java}; un servicio con más memoria o con documentos más grandes los sube.
 *
 * <p>No incluye los límites de lectura del texto STXT (10 MB, 100 niveles de anidado y 10 000 caracteres por
 * línea), que son los de {@code stxt-core} y salen como {@link StxtPdfException.Kind#LIMIT} del lector.</p>
 *
 * @param maxPages       páginas de todo el documento ({@code PAGE_LIMIT})
 * @param maxImagePixels píxeles (ancho × alto) de una imagen PNG o JPEG, que se miran en la cabecera sin
 *                       decodificarla ({@code IMAGE_TOO_LARGE}); el SVG no tiene píxeles
 * @param maxSvgElements elementos que dibuja un SVG, contando cada vez que un {@code <use>} repite un trozo
 *                       ({@code SVG_TOO_COMPLEX}); un SVG puede ser pequeño y repetirse miles de millones de veces
 * @param maxSvgDepth    niveles de anidado de un SVG, contando el {@code <svg>} de la raíz
 *                       ({@code SVG_TOO_COMPLEX}); más profundos agotan la pila del dibujo
 * @param maxPdfBytes    bytes del fichero de un PDF que dibuja una {@code Image}, que se miran antes de abrirlo
 *                       ({@code PDF_TOO_LARGE})
 */
public record Limits(int maxPages, long maxImagePixels, int maxSvgElements, int maxSvgDepth, long maxPdfBytes) {

    /** 2000 páginas, 25 millones de píxeles por imagen, 20 000 elementos y 64 niveles por SVG y 20 MB por PDF. */
    public static final Limits DEFAULT = new Limits(2_000, 25_000_000L, 20_000, 64, 20_000_000L);

    public Limits {
        require("maxPages", maxPages);
        require("maxImagePixels", maxImagePixels);
        require("maxSvgElements", maxSvgElements);
        require("maxSvgDepth", maxSvgDepth);
        require("maxPdfBytes", maxPdfBytes);
    }

    private static void require(String name, long value) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " debe ser al menos 1 y es " + value);
        }
    }

    public Limits withMaxPages(int maxPages) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    public Limits withMaxImagePixels(long maxImagePixels) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    public Limits withMaxSvgElements(int maxSvgElements) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    public Limits withMaxSvgDepth(int maxSvgDepth) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }

    public Limits withMaxPdfBytes(long maxPdfBytes) {
        return new Limits(maxPages, maxImagePixels, maxSvgElements, maxSvgDepth, maxPdfBytes);
    }
}
