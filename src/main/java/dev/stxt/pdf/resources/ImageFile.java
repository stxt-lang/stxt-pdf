package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Una imagen de la carpeta {@code images/} (referencia/componentes/image.md): su formato, reconocido por
 * el contenido y no por la extensión, y su tamaño en píxeles. Se admiten PNG, JPEG, SVG (el
 * subconjunto de {@link SvgInspector}) y PDF ({@link PdfInspector}).
 *
 * <p>El tamaño natural en puntos cuenta cada píxel como en CSS, a 96 por pulgada
 * ({@link #POINTS_PER_PIXEL}), sin mirar la resolución que declare el fichero. El de un SVG es
 * el de su {@code width} y {@code height} o, si faltan, el de su {@code viewBox}, también en
 * píxeles CSS. Un PDF ya está en puntos y cada página tiene el suyo.</p>
 */
public final class ImageFile {

    /** Puntos que mide un píxel: 72 / 96, como en CSS. */
    public static final double POINTS_PER_PIXEL = 0.75;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    public enum Format { PNG, JPEG, SVG, PDF }

    private final Path path;
    private final Format format;
    private final double pixelWidth;
    private final double pixelHeight;
    private final boolean text;
    private final long svgElements;
    private final int svgDepth;
    private final List<FontFile> textFonts;
    private final List<PdfInspector.PdfPage> pages;

    private ImageFile(Path path, Format format, double pixelWidth, double pixelHeight, boolean text,
            long svgElements, int svgDepth, List<FontFile> textFonts, List<PdfInspector.PdfPage> pages) {
        this.path = path;
        this.format = format;
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        this.text = text;
        this.svgElements = svgElements;
        this.svgDepth = svgDepth;
        this.textFonts = textFonts;
        this.pages = pages;
    }

    /**
     * Lee el formato y el tamaño de una imagen; {@code name} es su ruta en el documento, para los
     * mensajes. Un SVG se comprueba entero, también sus fuentes en {@code resources}; de un PDF se leen sus páginas.
     *
     * @throws ImageFormatException si no es PNG, JPEG, PDF ni un SVG del subconjunto, o no se puede leer
     */
    static ImageFile read(Path path, String name, Resources resources) {
        byte[] head = new byte[12];
        int n;
        try (InputStream in = Files.newInputStream(path)) {
            n = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new ImageFormatException("No se puede leer la imagen " + name + ": " + e.getMessage(), e);
        }
        Format format = startsWith(head, n, PNG) ? Format.PNG : startsWith(head, n, JPEG) ? Format.JPEG : null;
        if (format == null && looksLikeXml(head, n)) {
            return readSvg(path, name, resources);
        }
        if (format == null && startsWith(head, n, PDF)) {
            List<PdfInspector.PdfPage> pages = PdfInspector.inspect(path, name);
            return new ImageFile(path, Format.PDF, pages.get(0).width() / POINTS_PER_PIXEL,
                    pages.get(0).height() / POINTS_PER_PIXEL, false, 0, 0, List.of(), pages);
        }
        if (format == null) {
            throw new ImageFormatException("La imagen " + name + " no es PNG, JPEG, SVG ni PDF" + guess(head, n)
                    + ": hay que convertirla a uno de los cuatro");
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
            return new ImageFile(path, format, width, height, false, 0, 0, List.of(), List.of());
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

    /**
     * Si el fichero es un PDF, mirando solo su cabecera.
     *
     * @throws IOException si no se puede leer
     */
    static boolean isPdf(Path path) throws IOException {
        byte[] head = new byte[PDF.length];
        int n;
        try (InputStream in = Files.newInputStream(path)) {
            n = in.readNBytes(head, 0, head.length);
        }
        return startsWith(head, n, PDF);
    }

    /** Si empieza como un XML (con o sin la marca de orden de bytes de UTF-8). */
    private static boolean looksLikeXml(byte[] head, int n) {
        String text = new String(head, 0, n, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (text.startsWith("\u00EF\u00BB\u00BF")) {
            text = text.substring(3);
        }
        return text.stripLeading().startsWith("<");
    }

    private static ImageFile readSvg(Path path, String name, Resources resources) {
        byte[] content;
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ImageFormatException("No se puede leer la imagen " + name + ": " + e.getMessage(), e);
        }
        SvgInspector.Info info = SvgInspector.inspect(content, name);
        for (SvgInspector.TextUse use : info.texts()) {
            checkText(use, name, resources);
        }
        // Cada texto que se ve ya tiene su fuente; un <text> o <tspan> sin ella (sin familia, o una que no está en
        // fonts/) solo puede tener espacios, que no se dibujan.
        List<FontFile> fonts = new ArrayList<>();
        for (FontFace face : info.fonts()) {
            fonts.add(face == null ? null : resources.fonts().lookup(face));
        }
        return new ImageFile(path, Format.SVG, info.widthPx(), info.heightPx(), !info.texts().isEmpty(),
                info.elements(), info.depth(), Collections.unmodifiableList(fonts), List.of());
    }

    /** Que la fuente de un texto del SVG exista en {@code fonts/} y tenga todos sus caracteres: no hay sustitución. */
    private static void checkText(SvgInspector.TextUse use, String name, Resources resources) {
        String where = "El texto \"" + abbreviate(use.text().strip()) + "\" del SVG " + name + " (línea "
                + use.line() + ")";
        if (use.family() == null) {
            throw new ImageFormatException(where + " no declara font-family: no hay fuente por defecto");
        }
        String family = SvgInspector.firstFamily(use.family());
        if (SvgInspector.isGeneric(family)) {
            throw new ImageFormatException(where + " usa la familia genérica " + family + ", que habría que sustituir "
                    + "por otra: tiene que ser una familia de resources/fonts/");
        }
        FontFace face = new FontFace(family, use.bold() ? FontWeight.BOLD : FontWeight.NORMAL,
                use.italic() ? FontStyle.ITALIC : FontStyle.NORMAL);
        FontFile font;
        try {
            font = resources.fonts().find(face);
        } catch (ResourceException e) {
            throw new ImageFormatException(where + ": " + e.getMessage(), e);
        }
        use.text().codePoints().filter(cp -> !Character.isWhitespace(cp) && !Character.isISOControl(cp))
                .filter(cp -> !font.has(cp)).findFirst().ifPresent(cp -> {
                    throw new ImageFormatException(where + " tiene el carácter '" + new String(Character.toChars(cp))
                            + "' (U+" + String.format(Locale.ROOT, "%04X", cp) + "), que la fuente " + face + " no tiene");
                });
    }

    private static String abbreviate(String text) {
        return text.length() <= 30 ? text : text.substring(0, 29) + "…";
    }

    private static boolean startsWith(byte[] head, int n, byte[] signature) {
        return n >= signature.length && Arrays.equals(head, 0, signature.length, signature, 0, signature.length);
    }

    /** El formato que parece tener un fichero que no es PNG, JPEG, SVG ni PDF, para el mensaje. */
    private static String guess(byte[] head, int n) {
        String text = new String(head, 0, n, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (text.startsWith("GIF8")) {
            return " (es GIF)";
        }
        if (text.startsWith("RIFF") && text.startsWith("WEBP", 8)) {
            return " (es WebP)";
        }
        return "";
    }

    public Path path() {
        return path;
    }

    public Format format() {
        return format;
    }

    /** Ancho en píxeles; el de un SVG o un PDF (su primera página), en píxeles CSS (no tiene por qué ser entero). */
    public double pixelWidth() {
        return pixelWidth;
    }

    /** Alto en píxeles; el de un SVG o un PDF (su primera página), en píxeles CSS (no tiene por qué ser entero). */
    public double pixelHeight() {
        return pixelHeight;
    }

    /** Píxeles de un PNG o JPEG (ancho × alto, leídos de la cabecera); 0 en un SVG o un PDF, que no tienen. */
    public long pixels() {
        return isVector() ? 0 : (long) pixelWidth * (long) pixelHeight;
    }

    /** Elementos que dibuja un SVG, contando cada repetición de un {@code <use>} (saturado); 0 si no es un SVG. */
    public long svgElements() {
        return svgElements;
    }

    /** Niveles de anidado de un SVG, con su {@code <svg>} raíz; 0 si no es un SVG. */
    public int svgDepth() {
        return svgDepth;
    }

    /** Si es un SVG con texto, que se dibuja con las fuentes de {@code fonts/}. */
    public boolean hasText() {
        return text;
    }

    /**
     * La fuente de {@code fonts/} de cada {@code <text>} y {@code <tspan>} del SVG, en orden del documento: la que se
     * comprobó y la que lo dibuja. Es la de su sitio en el fichero, con la herencia de sus antepasados (no la del
     * {@code <use>} que lo repita). {@code null} en uno que no tiene fuente, que solo puede contener espacios.
     * Vacía si no es un SVG.
     */
    public List<FontFile> textFonts() {
        return textFonts;
    }

    /** Si es un dibujo vectorial (un SVG o un PDF), que se pinta sin perder nitidez a ningún tamaño. */
    public boolean isVector() {
        return format == Format.SVG || format == Format.PDF;
    }

    /** Las páginas de un PDF; 1 en el resto, que son una sola imagen. */
    public int pages() {
        return format == Format.PDF ? pages.size() : 1;
    }

    /**
     * Por qué esa página de un PDF no se dibujaría igual que en su PDF según la comprobación {@code check}, para el
     * mensaje: con {@link Check#PDF_IMAGE_FONTS}, una fuente sin incrustar o sin {@code ToUnicode}; con
     * {@link Check#PDF_IMAGE_ANNOTATIONS}, una anotación que se ve. {@code null} si no hay problema, con otra
     * comprobación y en el resto de formatos.
     *
     * @param page desde 1, hasta {@link #pages()}
     */
    public String problem(int page, Check check) {
        if (format != Format.PDF) {
            return null;
        }
        PdfInspector.PdfPage p = pages.get(page - 1);
        return switch (check) {
            case PDF_IMAGE_FONTS -> p.fonts();
            case PDF_IMAGE_ANNOTATIONS -> p.annotations();
            default -> null;
        };
    }

    /** Ancho natural en puntos; el de un PDF, el de su primera página. */
    public double width() {
        return width(1);
    }

    /** Alto natural en puntos; el de un PDF, el de su primera página. */
    public double height() {
        return height(1);
    }

    /**
     * Ancho natural en puntos de esa página: el de un PDF, el de su recorte ya girado; el del resto, que tienen una
     * sola página, el de la imagen.
     *
     * @param page desde 1, hasta {@link #pages()}
     */
    public double width(int page) {
        return format == Format.PDF ? pages.get(page - 1).width() : pixelWidth * POINTS_PER_PIXEL;
    }

    /** Alto natural en puntos de esa página, como {@link #width(int)}. */
    public double height(int page) {
        return format == Format.PDF ? pages.get(page - 1).height() : pixelHeight * POINTS_PER_PIXEL;
    }

    @Override
    public String toString() {
        return "ImageFile[" + path.getFileName() + ", " + format + ", " + pixelWidth + "x" + pixelHeight + "]";
    }
}
