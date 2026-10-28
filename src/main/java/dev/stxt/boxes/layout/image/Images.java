package dev.stxt.boxes.layout.image;

import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.ImageFormatException;
import dev.stxt.boxes.resources.ResourceException;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;

/**
 * Las imágenes del documento en la carpeta de recursos (ESPECIFICACION.md §5.18): el fichero y
 * el tamaño natural de cada {@code Image}. Los errores del fichero son del documento y llevan la
 * línea de la {@code Image}: {@code IMAGE_UNKNOWN} si la ruta no existe o sale de la carpeta, e
 * {@code IMAGE_FORMAT} si no es PNG, JPEG ni un SVG admitido.
 */
public final class Images {

    private final Resources resources;

    public Images(Resources resources) {
        this.resources = resources;
    }

    /**
     * Comprueba, antes de maquetar, que todas las imágenes del árbol existen y se pueden leer.
     *
     * @throws InvalidModelException con la línea de la primera {@code Image} que falla
     */
    public void validate(Box box) {
        if (box.isImage()) {
            file(box);
        }
        box.children.forEach(this::validate);
    }

    /** El fichero de una {@code Image}. */
    public ImageFile file(Box box) {
        try {
            return resources.images().find(box.image);
        } catch (ImageFormatException e) {
            throw new InvalidModelException(box.line, ErrorCode.IMAGE_FORMAT, e.getMessage());
        } catch (ResourceException e) {
            throw new InvalidModelException(box.line, ErrorCode.IMAGE_UNKNOWN, e.getMessage()
                    + " (la ruta de una Image es relativa a la carpeta images/ de los recursos)");
        }
    }

    /** Tamaño natural de una {@code Image}, en puntos. */
    public NaturalSize natural(Box box) {
        ImageFile f = file(box);
        return new NaturalSize(f.width(), f.height());
    }
}
