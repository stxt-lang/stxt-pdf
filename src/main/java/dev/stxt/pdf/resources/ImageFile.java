package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * An image of the {@code images/} folder (reference/components/image.md): its format, recognized by
 * the content and not by the extension, and its size in pixels. PNG, JPEG, SVG (the
 * subset of {@link SvgInspector}) and PDF ({@link PdfInspector}) are supported.
 *
 * <p>The natural size in points counts 96 pixels per inch
 * ({@link #POINTS_PER_PIXEL}), regardless of the resolution the file declares. That of an SVG is
 * that of its {@code width} and {@code height} or, if they are missing, that of its {@code viewBox}, also in
 * pixels. A PDF is already in points and each page has its own.</p>
 *
 * <p>What was read is what the layout measures (reference/implementation/api.md). An SVG keeps the bytes it was
 * checked with, and is painted with them; so does a PNG, a JPEG or a PDF if the resources keep the images in memory
 * ({@link Resources#withImagesInMemory()}). If not, it is read again when painting:
 * {@link #checkUnchanged()} compares its size and its modification time with those it had when it was read. With the
 * same comparison a {@link Resources} knows whether the image another one of the same family read is still that of
 * the file ({@link Resources#snapshot()}).</p>
 */
public final class ImageFile {

    /** Points a pixel measures: 72 / 96. */
    public static final double POINTS_PER_PIXEL = 0.75;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    public enum Format { PNG, JPEG, SVG, PDF }

    private final Path path;
    private final String name;
    private final Format format;
    /** Size and modification time of the file when it was read. */
    private final long size;
    private final FileTime modified;
    /**
     * The content of the file as it was read and checked: always that of an SVG, and that of a PNG, a JPEG or a PDF
     * if the resources keep the images in memory ({@link Resources#withImagesInMemory()}); {@code null} if not.
     */
    private final byte[] content;
    /** The fonts the text of an SVG was checked with; {@code null} without text. */
    private final FontLibrary fonts;
    private final double pixelWidth;
    private final double pixelHeight;
    /** Color components of a JPEG, as its frame header declares them; 0 for the rest and if it has none. */
    private final int components;
    private final boolean text;
    private final long svgElements;
    private final int svgDepth;
    private final List<FontFile> textFonts;
    private final List<PdfInspector.PdfPage> pages;

    private ImageFile(Path path, String name, BasicFileAttributes attributes, Format format, double pixelWidth,
            double pixelHeight, byte[] content, FontLibrary fonts, boolean text, long svgElements, int svgDepth,
            List<FontFile> textFonts, List<PdfInspector.PdfPage> pages) {
        this(path, name, attributes, format, pixelWidth, pixelHeight, 0, content, fonts, text, svgElements, svgDepth,
                textFonts, pages);
    }

    private ImageFile(Path path, String name, BasicFileAttributes attributes, Format format, double pixelWidth,
            double pixelHeight, int components, byte[] content, FontLibrary fonts, boolean text, long svgElements,
            int svgDepth, List<FontFile> textFonts, List<PdfInspector.PdfPage> pages) {
        this.components = components;
        this.path = path;
        this.fonts = fonts;
        this.name = name;
        // An image given as bytes has no file: nothing to compare it with.
        this.size = attributes == null ? content.length : attributes.size();
        this.modified = attributes == null ? null : attributes.lastModifiedTime();
        this.content = content;
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
     * Reads the format and the size of an image; {@code name} is its path in the document, for the
     * messages. An SVG is checked in full, including its fonts in {@code resources}; the pages of a PDF are read. If
     * {@code resources} keep the images in memory, the file is read whole once, and its format and size are taken
     * from the bytes it keeps.
     *
     * @throws ImageFormatException if it is not PNG, JPEG, PDF or an SVG of the subset, or cannot be read
     */
    static ImageFile read(Path path, String name, Resources resources) {
        byte[] head = new byte[12];
        int n;
        byte[] content = null;
        // Before reading it: a change while it is being read gives another modification time.
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class);
            if (resources.imagesInMemory()) {
                content = Files.readAllBytes(path);
                n = Math.min(head.length, content.length);
                System.arraycopy(content, 0, head, 0, n);
            } else {
                try (InputStream in = Files.newInputStream(path)) {
                    n = in.readNBytes(head, 0, head.length);
                }
            }
        } catch (IOException e) {
            throw new ImageFormatException("Cannot read the image " + name + ": " + e.getMessage(), e);
        }
        return parse(path, name, attributes, head, n, content, resources);
    }

    /**
     * An image given as bytes ({@link Resources.Builder#image}, {@link Resources#withImages}), which has no file: its
     * format and its size are taken from {@code content}, which it keeps and is painted with. {@code name} is its
     * path in the document, for the messages. An SVG is checked in full, including its fonts in {@code resources}.
     *
     * @throws ImageFormatException if it is not PNG, JPEG, PDF or an SVG of the subset
     */
    static ImageFile inMemory(byte[] content, String name, Resources resources) {
        int n = Math.min(12, content.length);
        return parse(null, name, null, Arrays.copyOf(content, 12), n, content, resources);
    }

    /**
     * The image of a file or of bytes already read.
     *
     * @param path       the file; {@code null} for an image given as bytes
     * @param attributes those of the file when it was read; {@code null} for an image given as bytes
     * @param head       the first bytes of the content, {@code n} of them
     * @param content    the whole content if it is kept; {@code null} to read it from {@code path} when it is needed
     */
    private static ImageFile parse(Path path, String name, BasicFileAttributes attributes, byte[] head, int n,
            byte[] content, Resources resources) {
        Format format = startsWith(head, n, PNG) ? Format.PNG : startsWith(head, n, JPEG) ? Format.JPEG : null;
        if (format == null && looksLikeXml(head, n)) {
            return readSvg(path, name, attributes, content, resources);
        }
        if (format == null && startsWith(head, n, PDF)) {
            List<PdfInspector.PdfPage> pages = PdfInspector.inspect(path, content, name);
            return new ImageFile(path, name, attributes, Format.PDF, pages.get(0).width() / POINTS_PER_PIXEL,
                    pages.get(0).height() / POINTS_PER_PIXEL, content, null, false, 0, 0, List.of(), pages);
        }
        if (format == null) {
            throw new ImageFormatException("The image " + name + " is not PNG, JPEG, SVG or PDF" + guess(head, n)
                    + ": it has to be converted to one of the four");
        }
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format.name());
        ImageReader reader = readers.next();
        try (ImageInputStream in = ImageIO.createImageInputStream(
                content != null ? new ByteArrayInputStream(content) : path.toFile())) {
            reader.setInput(in);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width <= 0 || height <= 0) {
                throw new ImageFormatException("The image " + name + " has no pixels");
            }
            int components = format == Format.JPEG ? jpegComponents(path, content) : 0;
            return new ImageFile(path, name, attributes, format, width, height, components, content, null, false, 0, 0,
                    List.of(), List.of());
        } catch (IOException | RuntimeException e) {
            if (e instanceof ImageFormatException f) {
                throw f;
            }
            throw new ImageFormatException("Cannot read the image " + name + ", which looks like " + format
                    + " but is damaged: " + e.getMessage(), e);
        } finally {
            reader.dispose();
        }
    }

    /**
     * The color components of a JPEG, from its frame header (the {@code SOFn} segment): 1 for gray, 3 for color and 4
     * for CMYK or YCCK. It reads the segments before the frame, not the pixels; 0 if no frame header is found, which
     * embedding the image reports.
     *
     * @param content the bytes of the file if they are kept; {@code null} to read them from {@code path}
     */
    private static int jpegComponents(Path path, byte[] content) throws IOException {
        try (InputStream in = content != null ? new ByteArrayInputStream(content)
                : new java.io.BufferedInputStream(Files.newInputStream(path))) {
            if (in.read() != 0xFF || in.read() != 0xD8) {
                return 0;
            }
            while (true) {
                int b = in.read();
                while (b == 0xFF) {
                    // The marker, after any fill bytes.
                    b = in.read();
                    if (b != 0xFF) {
                        break;
                    }
                }
                if (b < 0 || b == 0xD9 || b == 0xDA) {
                    // The end of the file or of the image, or the pixels, without a frame header before them.
                    return 0;
                }
                if (b == 0x01 || (b >= 0xD0 && b <= 0xD7) || b == 0x00) {
                    // Markers without a segment.
                    continue;
                }
                int length = (in.read() << 8) | in.read();
                if (length < 2) {
                    return 0;
                }
                boolean frame = b >= 0xC0 && b <= 0xCF && b != 0xC4 && b != 0xC8 && b != 0xCC;
                if (frame) {
                    // Precision (1), height (2), width (2) and the number of components.
                    byte[] header = in.readNBytes(6);
                    return header.length == 6 ? header[5] & 0xFF : 0;
                }
                in.skipNBytes(length - 2);
            }
        } catch (java.io.EOFException e) {
            return 0;
        }
    }

    /**
     * Whether the file is a PDF, looking only at its header.
     *
     * @throws IOException if it cannot be read
     */
    static boolean isPdf(Path path) throws IOException {
        byte[] head = new byte[PDF.length];
        int n;
        try (InputStream in = Files.newInputStream(path)) {
            n = in.readNBytes(head, 0, head.length);
        }
        return startsWith(head, n, PDF);
    }

    /** Whether the content is that of a PDF, looking only at its header. */
    static boolean isPdf(byte[] content) {
        return startsWith(content, content.length, PDF);
    }

    /** Whether it starts like an XML (with or without the UTF-8 byte order mark). */
    private static boolean looksLikeXml(byte[] head, int n) {
        String text = new String(head, 0, n, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (text.startsWith("\u00EF\u00BB\u00BF")) {
            text = text.substring(3);
        }
        return text.stripLeading().startsWith("<");
    }

    /** An SVG, from {@code read} bytes if it already has them ({@code null} if not). */
    private static ImageFile readSvg(Path path, String name, BasicFileAttributes attributes, byte[] read,
            Resources resources) {
        byte[] content = read;
        if (content == null) {
            try {
                content = Files.readAllBytes(path);
            } catch (IOException e) {
                throw new ImageFormatException("Cannot read the image " + name + ": " + e.getMessage(), e);
            }
        }
        SvgInspector.Info info = SvgInspector.inspect(content, name);
        for (SvgInspector.TextUse use : info.texts()) {
            checkText(use, name, resources);
        }
        // Each visible text already has its font; a <text> or <tspan> without one (without family, or one that is not
        // in fonts/) can only have spaces, which are not drawn.
        List<FontFile> fonts = new ArrayList<>();
        for (FontFace face : info.fonts()) {
            fonts.add(face == null ? null : resources.fonts().lookup(face));
        }
        return new ImageFile(path, name, attributes, Format.SVG, info.widthPx(), info.heightPx(), content,
                fonts.isEmpty() ? null : resources.fonts(), !info.texts().isEmpty(), info.elements(), info.depth(), Collections.unmodifiableList(fonts), List.of());
    }

    /** That the font of a text of the SVG exists in {@code fonts/} and has all its characters: no substitution. */
    private static void checkText(SvgInspector.TextUse use, String name, Resources resources) {
        String where = "The text \"" + abbreviate(use.text().strip()) + "\" of the SVG " + name + " (line "
                + use.line() + ")";
        if (use.family() == null) {
            throw new ImageFormatException(where + " declares no font-family: there is no default font");
        }
        String family = SvgInspector.firstFamily(use.family());
        if (SvgInspector.isGeneric(family)) {
            throw new ImageFormatException(where + " uses the generic family " + family + ", which would have to be "
                    + "replaced by another: it has to be a family of resources/fonts/");
        }
        FontFace face = new FontFace(family, use.weight(), use.italic() ? FontStyle.ITALIC : FontStyle.NORMAL);
        FontFile font;
        try {
            font = resources.fonts().find(face);
        } catch (ResourceException e) {
            throw new ImageFormatException(where + ": " + e.getMessage(), e);
        }
        use.text().codePoints().filter(cp -> !Character.isWhitespace(cp) && !Character.isISOControl(cp))
                .filter(cp -> !font.has(cp)).findFirst().ifPresent(cp -> {
                    throw new ImageFormatException(where + ": the font " + face + " has no character '"
                            + new String(Character.toChars(cp)) + "' (U+" + String.format(Locale.ROOT, "%04X", cp) + ")");
                });
    }

    private static String abbreviate(String text) {
        return text.length() <= 30 ? text : text.substring(0, 29) + "...";
    }

    private static boolean startsWith(byte[] head, int n, byte[] signature) {
        return n >= signature.length && Arrays.equals(head, 0, signature.length, signature, 0, signature.length);
    }

    /** The format a file that is not PNG, JPEG, SVG or PDF seems to have, for the message. */
    private static String guess(byte[] head, int n) {
        String text = new String(head, 0, n, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (text.startsWith("GIF8")) {
            return " (it is GIF)";
        }
        if (text.startsWith("RIFF") && text.startsWith("WEBP", 8)) {
            return " (it is WebP)";
        }
        return "";
    }

    /** {@return its file; {@code null} if it was given as bytes and has none} */
    public Path path() {
        return path;
    }

    /** Its path in the document that read it first, for the messages. */
    public String name() {
        return name;
    }

    /** {@return the name of its file, or of its path in the document if it has no file, for the messages} */
    public String fileName() {
        return path != null ? path.getFileName().toString() : name.substring(name.replace('\\', '/').lastIndexOf('/') + 1);
    }

    /**
     * The content of an SVG as it was checked when it was read, without reading the file again; a new copy in each
     * call.
     *
     * @throws IllegalStateException if it is not an SVG
     */
    public byte[] svgContent() {
        if (format != Format.SVG) {
            throw new IllegalStateException("The image " + name + " is " + format + ", not SVG");
        }
        return content.clone();
    }

    /**
     * {@return whether its bytes are kept as they were read} Always for an SVG, and for a PNG, a JPEG or a PDF if the
     * resources keep the images in memory ({@link Resources#withImagesInMemory()}).
     */
    public boolean inMemory() {
        return content != null;
    }

    /**
     * The bytes of the file, to paint it: those that were read if they are kept ({@link #inMemory()}), or the file
     * read again, after {@link #checkUnchanged()}; a new copy in each call.
     *
     * @throws ImageFormatException if it is read again and the file changed
     * @throws ResourceException    if it is read again and the file can no longer be read
     */
    public byte[] content() {
        if (content != null) {
            return content.clone();
        }
        checkUnchanged();
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw unreadable(e);
        }
    }

    /**
     * That the file of a PNG, a JPEG or a PDF, which is read again when painting, has the size and the modification
     * time it had when it was read: otherwise the document would have been laid out with one image and painted with
     * another. An image whose bytes are kept ({@link #inMemory()}, always an SVG) is painted with them, so it is not
     * checked.
     *
     * @throws ImageFormatException if the file changed
     * @throws ResourceException    if it can no longer be read, for example because it was removed
     */
    public void checkUnchanged() {
        if (content != null) {
            return;
        }
        BasicFileAttributes now;
        try {
            now = Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            throw unreadable(e);
        }
        if (now.size() != size || !now.lastModifiedTime().equals(modified)) {
            throw new ImageFormatException("The image " + name + " changed after it was read (its size or its "
                    + "modification time is not the same): the document was laid out with the previous file and "
                    + "has to be laid out again with resources that read the new one (Converter.reloaded())");
        }
    }

    /**
     * Whether it is still the image of its file for {@code resources}: the file has the size and the modification
     * time it had when it was read (false if it cannot be read, which reading it again reports) and, if it is an SVG
     * with text, the fonts of {@code resources} are those its text was checked with.
     */
    boolean isCurrent(Resources resources) {
        if (path != null) {
            try {
                BasicFileAttributes now = Files.readAttributes(path, BasicFileAttributes.class);
                if (now.size() != size || !now.lastModifiedTime().equals(modified)) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        }
        return fonts == null || fonts == resources.fonts();
    }

    /** The error of a failure to read the file again when painting, with its path. */
    public ResourceException unreadable(IOException e) {
        return new ResourceException("Cannot read the image " + name + ": " + e.getMessage(), e);
    }

    public Format format() {
        return format;
    }

    /** Width in pixels; that of an SVG or a PDF (its first page) is not necessarily an integer. */
    public double pixelWidth() {
        return pixelWidth;
    }

    /** Height in pixels; that of an SVG or a PDF (its first page) is not necessarily an integer. */
    public double pixelHeight() {
        return pixelHeight;
    }

    /** Pixels of a PNG or JPEG (width × height, read from the header); 0 for an SVG or a PDF, which have none. */
    public long pixels() {
        return isVector() ? 0 : (long) pixelWidth * (long) pixelHeight;
    }

    /**
     * {@return whether it is a JPEG with four color components, CMYK or YCCK} It is embedded as it is, with the device
     * CMYK color space, which PDF/A only admits with a CMYK output profile, and the engine writes an sRGB one
     * (reference/common/pdf.md).
     */
    public boolean cmyk() {
        return components == 4;
    }

    /** Elements an SVG draws, counting each repetition of a {@code <use>} (saturated); 0 if it is not an SVG. */
    public long svgElements() {
        return svgElements;
    }

    /** Nesting levels of an SVG, with its root {@code <svg>}; 0 if it is not an SVG. */
    public int svgDepth() {
        return svgDepth;
    }

    /** Whether it is an SVG with text, which is drawn with the fonts of {@code fonts/}. */
    public boolean hasText() {
        return text;
    }

    /**
     * The font of {@code fonts/} of each {@code <text>} and {@code <tspan>} of the SVG, in document order: the one that
     * was checked and the one that draws it. It is that of its place in the file, with the inheritance of its ancestors
     * (not that of the {@code <use>} that repeats it). {@code null} for one that has no font, which can only contain
     * spaces. Empty if it is not an SVG.
     */
    public List<FontFile> textFonts() {
        return textFonts;
    }

    /** Whether it is a vector drawing (an SVG or a PDF), which is painted without losing sharpness at any size. */
    public boolean isVector() {
        return format == Format.SVG || format == Format.PDF;
    }

    /** The pages of a PDF; 1 for the rest, which are a single image. */
    public int pages() {
        return format == Format.PDF ? pages.size() : 1;
    }

    /**
     * Why that page of a PDF would not be drawn as in its PDF according to the check {@code check}, for the
     * message: with {@link Check#PDF_IMAGE_FONTS}, a font not embedded or without {@code ToUnicode}; with
     * {@link Check#PDF_IMAGE_ANNOTATIONS}, a visible annotation. {@code null} if there is no problem, with another
     * check and for the other formats.
     *
     * @param page from 1 to {@link #pages()}
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

    /** Natural width in points; for a PDF, that of its first page. */
    public double width() {
        return width(1);
    }

    /** Natural height in points; for a PDF, that of its first page. */
    public double height() {
        return height(1);
    }

    /**
     * Natural width in points of that page: for a PDF, that of its crop box, already rotated; for the rest, which have
     * a single page, that of the image.
     *
     * @param page from 1 to {@link #pages()}
     */
    public double width(int page) {
        return format == Format.PDF ? pages.get(page - 1).width() : pixelWidth * POINTS_PER_PIXEL;
    }

    /** Natural height in points of that page, like {@link #width(int)}. */
    public double height(int page) {
        return format == Format.PDF ? pages.get(page - 1).height() : pixelHeight * POINTS_PER_PIXEL;
    }

    @Override
    public String toString() {
        return "ImageFile[" + fileName() + ", " + format + ", " + pixelWidth + "x" + pixelHeight + "]";
    }
}
