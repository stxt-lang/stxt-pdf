package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
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
 * checked with, and is painted with them. A PNG, a JPEG or a PDF is read again when painting:
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
    /** The content of an SVG, as it was checked; {@code null} for the other formats. */
    private final byte[] svg;
    /** The fonts the text of an SVG was checked with; {@code null} without text. */
    private final FontLibrary fonts;
    private final double pixelWidth;
    private final double pixelHeight;
    private final boolean text;
    private final long svgElements;
    private final int svgDepth;
    private final List<FontFile> textFonts;
    private final List<PdfInspector.PdfPage> pages;

    private ImageFile(Path path, String name, BasicFileAttributes attributes, Format format, double pixelWidth,
            double pixelHeight, byte[] svg, FontLibrary fonts, boolean text, long svgElements, int svgDepth,
            List<FontFile> textFonts, List<PdfInspector.PdfPage> pages) {
        this.path = path;
        this.fonts = fonts;
        this.name = name;
        this.size = attributes.size();
        this.modified = attributes.lastModifiedTime();
        this.svg = svg;
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
     * messages. An SVG is checked in full, including its fonts in {@code resources}; the pages of a PDF are read.
     *
     * @throws ImageFormatException if it is not PNG, JPEG, PDF or an SVG of the subset, or cannot be read
     */
    static ImageFile read(Path path, String name, Resources resources) {
        byte[] head = new byte[12];
        int n;
        // Before reading it: a change while it is being read gives another modification time.
        BasicFileAttributes attributes;
        try (InputStream in = Files.newInputStream(path)) {
            attributes = Files.readAttributes(path, BasicFileAttributes.class);
            n = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new ImageFormatException("Cannot read the image " + name + ": " + e.getMessage(), e);
        }
        Format format = startsWith(head, n, PNG) ? Format.PNG : startsWith(head, n, JPEG) ? Format.JPEG : null;
        if (format == null && looksLikeXml(head, n)) {
            return readSvg(path, name, attributes, resources);
        }
        if (format == null && startsWith(head, n, PDF)) {
            List<PdfInspector.PdfPage> pages = PdfInspector.inspect(path, name);
            return new ImageFile(path, name, attributes, Format.PDF, pages.get(0).width() / POINTS_PER_PIXEL,
                    pages.get(0).height() / POINTS_PER_PIXEL, null, null, false, 0, 0, List.of(), pages);
        }
        if (format == null) {
            throw new ImageFormatException("The image " + name + " is not PNG, JPEG, SVG or PDF" + guess(head, n)
                    + ": it has to be converted to one of the four");
        }
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format.name());
        ImageReader reader = readers.next();
        try (ImageInputStream in = ImageIO.createImageInputStream(path.toFile())) {
            reader.setInput(in);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width <= 0 || height <= 0) {
                throw new ImageFormatException("The image " + name + " has no pixels");
            }
            return new ImageFile(path, name, attributes, format, width, height, null, null, false, 0, 0, List.of(),
                    List.of());
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

    /** Whether it starts like an XML (with or without the UTF-8 byte order mark). */
    private static boolean looksLikeXml(byte[] head, int n) {
        String text = new String(head, 0, n, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (text.startsWith("\u00EF\u00BB\u00BF")) {
            text = text.substring(3);
        }
        return text.stripLeading().startsWith("<");
    }

    private static ImageFile readSvg(Path path, String name, BasicFileAttributes attributes, Resources resources) {
        byte[] content;
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ImageFormatException("Cannot read the image " + name + ": " + e.getMessage(), e);
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

    public Path path() {
        return path;
    }

    /** Its path in the document that read it first, for the messages. */
    public String name() {
        return name;
    }

    /**
     * The content of an SVG as it was checked when it was read, without reading the file again; a new copy in each
     * call.
     *
     * @throws IllegalStateException if it is not an SVG
     */
    public byte[] svgContent() {
        if (svg == null) {
            throw new IllegalStateException("The image " + name + " is " + format + ", not SVG");
        }
        return svg.clone();
    }

    /**
     * That the file of a PNG, a JPEG or a PDF, which is read again when painting, has the size and the modification
     * time it had when it was read: otherwise the document would have been laid out with one image and painted with
     * another. An SVG is painted with the bytes that were read ({@link #svgContent()}), so it is not checked.
     *
     * @throws ImageFormatException if the file changed
     * @throws ResourceException    if it can no longer be read, for example because it was removed
     */
    public void checkUnchanged() {
        if (format == Format.SVG) {
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
        try {
            BasicFileAttributes now = Files.readAttributes(path, BasicFileAttributes.class);
            if (now.size() != size || !now.lastModifiedTime().equals(modified)) {
                return false;
            }
        } catch (IOException e) {
            return false;
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
        return "ImageFile[" + path.getFileName() + ", " + format + ", " + pixelWidth + "x" + pixelHeight + "]";
    }
}
