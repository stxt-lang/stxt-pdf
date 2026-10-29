package dev.stxt.boxes.layout.image;

import dev.stxt.boxes.model.Limits;
import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.ImageFormatException;
import dev.stxt.boxes.resources.ResourceException;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;

/**
 * Las imágenes del documento en la carpeta de recursos (referencia/componentes/image.md): el fichero y
 * el tamaño natural de cada {@code Image}. Los errores del fichero son del documento y llevan la
 * línea de la {@code Image}: {@code IMAGE_UNKNOWN} si la ruta no existe o sale de la carpeta, e
 * {@code IMAGE_FORMAT} si no es PNG, JPEG ni un SVG admitido. Una imagen que supera los {@link Limits} es
 * {@code IMAGE_TOO_LARGE} (los píxeles de un PNG o JPEG) o {@code SVG_TOO_COMPLEX} (los elementos y los
 * niveles de un SVG), también con la línea de la {@code Image}.
 */
public final class Images {

    private final Resources resources;
    private final Limits limits;

    public Images(Resources resources, Limits limits) {
        this.resources = resources;
        this.limits = limits;
    }

    /**
     * Comprueba, antes de maquetar, que todas las imágenes del árbol existen y se pueden leer.
     *
     * @throws InvalidModelException con la línea de la primera {@code Image} que falla
     */
    public void validate(Box box) {
        if (box.isImage()) {
            checkLimits(file(box), box);
        }
        box.children().forEach(this::validate);
    }

    /** Que la imagen no pida más de lo que admiten los {@link Limits}; se mira en la cabecera o en el recuento del SVG, sin dibujarla. */
    private void checkLimits(ImageFile file, Box box) {
        if (file.pixels() > limits.maxImagePixels()) {
            throw new InvalidModelException(box.line(), ErrorCode.IMAGE_TOO_LARGE, "La imagen " + box.image() + " tiene "
                    + (long) file.pixelWidth() + " × " + (long) file.pixelHeight() + " píxeles ("
                    + megapixels(file.pixels()) + ") y el máximo es " + megapixels(limits.maxImagePixels())
                    + ": reduce su resolución (Limits.maxImagePixels lo cambia)");
        }
        if (file.svgDepth() > limits.maxSvgDepth()) {
            throw new InvalidModelException(box.line(), ErrorCode.SVG_TOO_COMPLEX, "El SVG " + box.image() + " anida "
                    + file.svgDepth() + " niveles y el máximo es " + limits.maxSvgDepth()
                    + ": aplánalo (Limits.maxSvgDepth lo cambia)");
        }
        if (file.svgElements() > limits.maxSvgElements()) {
            throw new InvalidModelException(box.line(), ErrorCode.SVG_TOO_COMPLEX, "El SVG " + box.image() + " dibuja "
                    + (file.svgElements() >= (1L << 50) ? "una cantidad desorbitada de" : String.valueOf(file.svgElements()))
                    + " elementos, contando cada vez que un <use> repite un trozo, y el máximo es "
                    + limits.maxSvgElements() + ": simplifícalo (Limits.maxSvgElements lo cambia)");
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

    /** Tamaño natural de una {@code Image}, en puntos. */
    public NaturalSize natural(Box box) {
        ImageFile f = file(box);
        return new NaturalSize(f.width(), f.height());
    }
}
