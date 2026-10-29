package dev.stxt.boxes.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Imágenes de la carpeta {@code images/} (referencia/componentes/image.md), por su ruta relativa a
 * ella ({@code logos/acme.png}). Cada fichero se lee una sola vez (salvo en una carrera entre hilos, que deja una de las lecturas). Es segura entre hilos.
 */
public final class ImageLibrary {

    private final Resources resources;
    private final Map<String, ImageFile> files = new ConcurrentHashMap<>();

    ImageLibrary(Resources resources) {
        this.resources = resources;
    }

    /**
     * La imagen de esa ruta, relativa a {@code images/}.
     *
     * @throws ImageFormatException si no es PNG, JPEG ni un SVG admitido, o no se puede leer
     * @throws ResourceException    si la ruta sale de la carpeta o no existe
     */
    public ImageFile find(String path) {
        ImageFile file = files.get(path);
        if (file == null) {
            // Dos hilos que piden a la vez la misma imagen la leen los dos y se queda una: son iguales.
            file = ImageFile.read(resources.resolve("images", path), path, resources);
            ImageFile previous = files.putIfAbsent(path, file);
            if (previous != null) {
                file = previous;
            }
        }
        return file;
    }
}
