package dev.stxt.boxes.resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Imágenes de la carpeta {@code images/} (ESPECIFICACION.md §5.18), por su ruta relativa a
 * ella ({@code logos/acme.png}). Cada fichero se lee una sola vez.
 */
public final class ImageLibrary {

    private final Resources resources;
    private final Map<String, ImageFile> files = new HashMap<>();

    ImageLibrary(Resources resources) {
        this.resources = resources;
    }

    /**
     * La imagen de esa ruta, relativa a {@code images/}.
     *
     * @throws ImageFormatException si no es PNG ni JPEG, o no se puede leer
     * @throws ResourceException    si la ruta sale de la carpeta o no existe
     */
    public synchronized ImageFile find(String path) {
        ImageFile file = files.get(path);
        if (file == null) {
            file = ImageFile.read(resources.resolve("images", path), path);
            files.put(path, file);
        }
        return file;
    }
}
