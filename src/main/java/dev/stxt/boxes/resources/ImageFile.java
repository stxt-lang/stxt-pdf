package dev.stxt.boxes.resources;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Una imagen de la carpeta {@code images/} (ESPECIFICACION.md §5.18): su formato, reconocido por
 * el contenido y no por la extensión, y su tamaño en píxeles. Solo se admiten PNG y JPEG.
 *
 * <p>El tamaño natural en puntos cuenta cada píxel como en CSS, a 96 por pulgada
 * ({@link #POINTS_PER_PIXEL}), sin mirar la resolución que declare el fichero.</p>
 */
public final class ImageFile {

    /** Puntos que mide un píxel: 72 / 96, como en CSS. */
    public static final double POINTS_PER_PIXEL = 0.75;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    public enum Format { PNG, JPEG }

    private final Path path;
    private final Format format;
    private final int pixelWidth;
    private final int pixelHeight;

    private ImageFile(Path path, Format format, int pixelWidth, int pixelHeight) {
        this.path = path;
        this.format = format;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
    }

    /**
     * Lee el formato y el tamaño de una imagen; {@code name} es su ruta en el documento, para los
     * mensajes.
     *
     * @throws ImageFormatException si no es PNG ni JPEG o no se puede leer su cabecera
     */
    static ImageFile read(Path path, String name) {
        byte[] head = new byte[12];
        int n;
        try (InputStream in = Files.newInputStream(path)) {
            n = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new ImageFormatException("No se puede leer la imagen " + name + ": " + e.getMessage(), e);
        }
        Format format = startsWith(head, n, PNG) ? Format.PNG : startsWith(head, n, JPEG) ? Format.JPEG : null;
        if (format == null) {
            throw new ImageFormatException("La imagen " + name + " no es PNG ni JPEG" + guess(head, n)
                    + ": conviértela a uno de los dos");
        }
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format.name());
        ImageReader reader = readers.next();
        try (ImageInputStream in = ImageIO.createImageInputStream(path.toFile())) {
            reader.setInput(in);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width <= 0 || height <= 0) {
                throw new ImageFormatException("La imagen " + name + " no tiene píxeles");
            }
            return new ImageFile(path, format, width, height);
        } catch (IOException | RuntimeException e) {
            if (e instanceof ImageFormatException f) {
                throw f;
            }
            throw new ImageFormatException("No se puede leer la imagen " + name + ", que parece " + format
                    + " pero está dañada: " + e.getMessage(), e);
        } finally {
            reader.dispose();
        }
    }

    private static boolean startsWith(byte[] head, int n, byte[] signature) {
        return n >= signature.length && Arrays.equals(head, 0, signature.length, signature, 0, signature.length);
    }

    /** El formato que parece tener un fichero que no es PNG ni JPEG, para el mensaje. */
    private static String guess(byte[] head, int n) {
        String text = new String(head, 0, n, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (text.startsWith("GIF8")) {
            return " (es GIF)";
        }
        if (text.startsWith("RIFF") && text.startsWith("WEBP", 8)) {
            return " (es WebP)";
        }
        if (text.stripLeading().startsWith("<")) {
            return " (parece SVG o XML)";
        }
        return "";
    }

    public Path path() {
        return path;
    }

    public Format format() {
        return format;
    }

    public int pixelWidth() {
        return pixelWidth;
    }

    public int pixelHeight() {
        return pixelHeight;
    }

    /** Ancho natural en puntos. */
    public double width() {
        return pixelWidth * POINTS_PER_PIXEL;
    }

    /** Alto natural en puntos. */
    public double height() {
        return pixelHeight * POINTS_PER_PIXEL;
    }

    @Override
    public String toString() {
        return "ImageFile[" + path.getFileName() + ", " + format + ", " + pixelWidth + "x" + pixelHeight + "]";
    }
}
