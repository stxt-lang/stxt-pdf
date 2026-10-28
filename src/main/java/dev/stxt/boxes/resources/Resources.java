package dev.stxt.boxes.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Carpeta de recursos (ESPECIFICACION.md §5.11): fuentes en {@code fonts/} e imágenes en
 * {@code images/}. Por defecto es {@code resources/}, relativa al directorio de trabajo; este
 * objeto solo sirve para cambiarla.
 *
 * <p>Los recursos se buscan siempre <strong>hacia abajo</strong>: {@link #resolve(String)}
 * rechaza las rutas absolutas, los segmentos {@code ..} y los enlaces simbólicos que apuntan
 * fuera de la carpeta.</p>
 */
public final class Resources {

    /** Raíz por defecto, relativa al directorio de trabajo. */
    public static final Path DEFAULT_ROOT = Path.of("resources");

    /** {@code C:}, {@code d:}…: unidad de Windows. */
    private static final Pattern DRIVE = Pattern.compile("^[A-Za-z]:");

    private final Path root;
    /** Se crean la primera vez que se piden y se leen sin cerrojo (comprobación doble). */
    private volatile FontLibrary fonts;
    private volatile ImageLibrary images;

    /** Recursos en {@link #DEFAULT_ROOT}. */
    public Resources() {
        this(DEFAULT_ROOT);
    }

    public Resources(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    /** Carpeta de las fuentes. */
    public Path fontsFolder() {
        return root.resolve("fonts");
    }

    /** Carpeta de las imágenes. */
    public Path imagesFolder() {
        return root.resolve("images");
    }

    /**
     * Fuentes de {@code fonts/}. Se exploran la primera vez que se piden.
     *
     * @throws ResourceException si la carpeta no existe o alguna fuente no es válida
     */
    public FontLibrary fonts() {
        FontLibrary library = fonts;
        if (library == null) {
            synchronized (this) {
                library = fonts;
                if (library == null) {
                    library = FontLibrary.scan(this);
                    fonts = library;
                }
            }
        }
        return library;
    }

    /** Imágenes de {@code images/}, que se leen la primera vez que se piden. */
    public ImageLibrary images() {
        ImageLibrary library = images;
        if (library == null) {
            synchronized (this) {
                library = images;
                if (library == null) {
                    library = new ImageLibrary(this);
                    images = library;
                }
            }
        }
        return library;
    }

    /**
     * Fichero existente de la carpeta de recursos, a partir de una ruta relativa a ella
     * ({@code images/logo.png}). Admite {@code /} y {@code \} como separadores.
     *
     * @throws ResourceException si la ruta es absoluta, contiene {@code ..}, sale de la carpeta
     *                           por un enlace o no existe
     */
    public Path resolve(String relative) {
        return resolve("", relative);
    }

    /**
     * Fichero existente de una subcarpeta de los recursos ({@code images}), a partir de una ruta
     * relativa a ella ({@code logos/acme.png}), con las mismas comprobaciones que
     * {@link #resolve(String)}: la ruta no puede salir de la subcarpeta.
     *
     * @throws ResourceException si la ruta es absoluta, contiene {@code ..}, sale de la
     *                           subcarpeta (también por un enlace) o no existe
     */
    public Path resolve(String folder, String relative) {
        if (relative.isBlank()) {
            throw new ResourceException("Ruta de recurso vacía");
        }
        if (relative.startsWith("/") || relative.startsWith("\\") || relative.startsWith("~")
                || DRIVE.matcher(relative).lookingAt()) {
            throw new ResourceException("Ruta de recurso absoluta: " + relative
                    + " (las rutas son relativas a la carpeta de recursos)");
        }
        for (String segment : relative.split("[/\\\\]")) {
            if (segment.equals("..")) {
                throw new ResourceException("Ruta de recurso hacia atrás: " + relative
                        + " (no se admite '..')");
            }
        }
        Path base = root.resolve(folder).normalize();
        Path path = base.resolve(relative.replace('\\', '/')).normalize();
        if (!path.startsWith(base)) {
            throw new ResourceException("La ruta " + relative + " sale de la carpeta " + base);
        }
        if (!Files.exists(path)) {
            throw new ResourceException("No existe el recurso " + relative + " en " + base);
        }
        return checkInside(path, base);
    }

    /**
     * Comprueba que un fichero de la carpeta, una vez resueltos los enlaces simbólicos, sigue
     * dentro de ella.
     *
     * @throws ResourceException si es un enlace que sale de la carpeta
     */
    Path checkInside(Path path) {
        return checkInside(path, root);
    }

    /** Como {@link #checkInside(Path)}, dentro de una subcarpeta de los recursos. */
    private Path checkInside(Path path, Path base) {
        try {
            if (!path.toRealPath().startsWith(base.toRealPath())) {
                throw new ResourceException("El recurso " + root.relativize(path)
                        + " es un enlace que sale de la carpeta " + (base.equals(root) ? "de recursos" : base.getFileName())
                        + " (" + base + ")");
            }
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
