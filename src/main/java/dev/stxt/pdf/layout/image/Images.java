package dev.stxt.pdf.layout.image;

import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.ImageFormatException;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.PropertyKey;

import java.util.EnumSet;
import java.util.Set;

/**
 * Las imágenes del documento en la carpeta de recursos (referencia/componentes/image.md): el fichero y
 * el tamaño natural de cada {@code Image}. Los errores del fichero son del documento y llevan la
 * línea de la {@code Image}: {@code IMAGE_UNKNOWN} si la ruta no existe o sale de la carpeta, e
 * {@code IMAGE_FORMAT} si no es PNG, JPEG, PDF ni un SVG admitido. Una imagen que supera los {@link Limits} es
 * {@code IMAGE_TOO_LARGE} (los píxeles de un PNG o JPEG) o {@code SVG_TOO_COMPLEX} (los elementos y los
 * niveles de un SVG), también con la línea de la {@code Image}; un PDF de más bytes que los {@link Limits},
 * {@code PDF_TOO_LARGE}, antes de abrirlo. La página de un PDF tiene que existir, y {@code Pdf page} en otro formato es
 * un error ({@code IMAGE_PAGE}, con la línea de {@code Pdf page}); una página que no se dibujaría igual que en su PDF
 * es {@code IMAGE_FORMAT}, salvo que se quite su comprobación ({@link Check#PDF_IMAGE_FONTS},
 * {@link Check#PDF_IMAGE_ANNOTATIONS}).
 */
public final class Images {

    private final Resources resources;
    private final Limits limits;
    private final Set<Check> excluded;

    /** Sin las comprobaciones de {@code excluded}. */
    public Images(Resources resources, Limits limits, Set<Check> excluded) {
        this.resources = resources;
        this.limits = limits;
        this.excluded = excluded.isEmpty() ? Set.of() : EnumSet.copyOf(excluded);
    }

    /**
     * Comprueba, antes de maquetar, que todas las imágenes del árbol existen y se pueden leer.
     *
     * @throws InvalidModelException con la línea de la primera {@code Image} que falla
     */
    public void validate(Box box) {
        if (box.isImage()) {
            checkPdfBytes(box);
            ImageFile file = file(box);
            checkLimits(file, box);
            checkPage(file, box);
        }
        box.children().forEach(this::validate);
    }

    /** Que la imagen no pida más de lo que admiten los {@link Limits}; se mira en la cabecera o en el recuento del SVG, sin dibujarla. */
    private void checkLimits(ImageFile file, Box box) {
        if (file.pixels() > limits.maxImagePixels()) {
            throw new InvalidModelException(box.line(), ErrorCode.IMAGE_TOO_LARGE, "La imagen " + box.image() + " tiene "
                    + (long) file.pixelWidth() + " × " + (long) file.pixelHeight() + " píxeles ("
                    + megapixels(file.pixels()) + ") y el máximo es " + megapixels(limits.maxImagePixels())
                    + ": hay que reducir su resolución (Limits.maxImagePixels lo cambia)");
        }
        if (file.svgDepth() > limits.maxSvgDepth()) {
            throw new InvalidModelException(box.line(), ErrorCode.SVG_TOO_COMPLEX, "El SVG " + box.image() + " anida "
                    + file.svgDepth() + " niveles y el máximo es " + limits.maxSvgDepth()
                    + ": hay que aplanarlo (Limits.maxSvgDepth lo cambia)");
        }
        if (file.svgElements() > limits.maxSvgElements()) {
            throw new InvalidModelException(box.line(), ErrorCode.SVG_TOO_COMPLEX, "El SVG " + box.image() + " dibuja "
                    + (file.svgElements() >= (1L << 50) ? "una cantidad desorbitada de" : String.valueOf(file.svgElements()))
                    + " elementos, contando cada vez que un <use> repite un trozo, y el máximo es "
                    + limits.maxSvgElements() + ": hay que simplificarlo (Limits.maxSvgElements lo cambia)");
        }
    }

    /** Que un PDF no tenga más bytes que el límite: se mira antes de abrirlo. */
    private void checkPdfBytes(Box box) {
        long bytes;
        try {
            bytes = resources.images().pdfBytes(box.image());
        } catch (ResourceException e) {
            // La ruta no existe o sale de la carpeta: lo dice file(box), con su código.
            return;
        }
        if (bytes > limits.maxPdfBytes()) {
            throw new InvalidModelException(box.line(), ErrorCode.PDF_TOO_LARGE, "El PDF " + box.image() + " ocupa "
                    + megabytes(bytes) + " y el máximo es " + megabytes(limits.maxPdfBytes())
                    + ": hace falta un PDF más pequeño, con solo las páginas que se usan (Limits.maxPdfBytes lo cambia)");
        }
    }

    private static String megabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1_000_000.0);
    }

    /**
     * Que {@code Pdf page} sea de un PDF y la página exista, y que se pueda dibujar con las comprobaciones que se
     * hacen.
     */
    private void checkPage(ImageFile file, Box box) {
        int page = box.style().image().pdfPage();
        if (box.style().isDeclared(PropertyKey.PDF_PAGE)) {
            if (file.format() != ImageFile.Format.PDF) {
                throw new InvalidModelException(box.style().line(PropertyKey.PDF_PAGE), ErrorCode.IMAGE_PAGE, "Pdf page solo "
                        + "tiene efecto en un PDF, y la imagen " + box.image() + " es " + file.format() + ": sobra");
            }
            if (page > file.pages()) {
                throw new InvalidModelException(box.style().line(PropertyKey.PDF_PAGE), ErrorCode.IMAGE_PAGE, "El PDF "
                        + box.image() + " tiene " + file.pages() + (file.pages() == 1 ? " página" : " páginas")
                        + " y no tiene la " + page);
            }
        }
        for (Check check : new Check[] {Check.PDF_IMAGE_ANNOTATIONS, Check.PDF_IMAGE_FONTS}) {
            String problem = excluded.contains(check) ? null : file.problem(page, check);
            if (problem != null) {
                throw new InvalidModelException(box.line(), ErrorCode.IMAGE_FORMAT, "La página " + page + " del PDF "
                        + box.image() + " " + problem + " (o se quita la comprobación " + check + " al convertir)");
            }
        }
    }

    private static String megapixels(long pixels) {
        return String.format(java.util.Locale.ROOT, "%.1f Mpx", pixels / 1_000_000.0);
    }

    /** El fichero de una {@code Image}. */
    public ImageFile file(Box box) {
        try {
            return resources.images().find(box.image());
        } catch (ImageFormatException e) {
            throw new InvalidModelException(box.line(), ErrorCode.IMAGE_FORMAT, e.getMessage());
        } catch (ResourceException e) {
            throw new InvalidModelException(box.line(), ErrorCode.IMAGE_UNKNOWN, e.getMessage()
                    + " (la ruta de una Image es relativa a la carpeta images/ de los recursos)");
        }
    }

    /** Tamaño natural de una {@code Image}, en puntos: el de un PDF, el de su página. */
    public NaturalSize natural(Box box) {
        ImageFile f = file(box);
        int page = box.style().image().pdfPage();
        return new NaturalSize(f.width(page), f.height(page));
    }
}
