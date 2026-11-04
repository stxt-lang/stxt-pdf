package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.InlineImage;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.tree.Box;

import java.util.Map;

/**
 * What the text of a document is worth while it is laid out, which the {@link dev.stxt.pdf.layout.Paginator} sets
 * pass by pass: the probe number that {@code {page}} and {@code {pages}} are measured as ({@link PageNumbers}), the
 * page assumed for each box referred to with {@code {page of: id}} and the number of each one referred to with
 * {@code {number of: id}} (reference/common/references.md), and the running heads of the page whose
 * {@code Page overlay} boxes are being laid out ({@code {heading N}}). It belongs to the
 * {@link dev.stxt.pdf.layout.Engine}, which sets it through its protocol with the paginator and, each time something
 * changes, forgets what the cache and the line breaking ({@link LineBreakingMemo}) store of the texts that measure
 * differently; {@link InlineLayout} only reads it.
 */
public final class TextValues {

    /** The measurer of the engine, with the images measured with their file. */
    private final TextMeasurer real;
    /** {@link #real} measuring {@code {page}} and {@code {pages}} as the probe number ({@link PageNumbers}). */
    private TextMeasurer measurer;
    /** Page assumed for each box referred to with {@code {page of: id}}; see {@link #assume}. */
    private Map<String, String> pages = Map.of();
    /** Number of each box referred to with {@code {number of: id}}. */
    private Map<String, String> numbers = Map.of();
    /**
     * The running heads of the page being laid out, by the id of their reference ({@code "2"}, {@code "2: first"}):
     * the text of the heading in effect on the page, or empty; see {@link #headings}.
     */
    private Map<String, String> headings = Map.of();

    /**
     * The values of a layout that measures the text with {@code measurer}, with an image inside a text
     * ({@link TextMeasurer#imageWidth}) measured with the proportion {@code context} reads from its file
     * ({@link LayoutContext#imageRatio}), whatever the measurer of the text (also {@link FixedMetrics}, so that the
     * tests draw the real image in its place). It starts as {@link #reset} leaves it.
     */
    public TextValues(TextMeasurer measurer, LayoutContext context) {
        this.real = new WithImages(measurer, context);
        reset();
    }

    /** The measurer of the text with the images measured through the context. */
    private record WithImages(TextMeasurer text, LayoutContext context) implements TextMeasurer {

        @Override
        public double width(String s, FontFace font, double fontSize) {
            return text.width(s, font, fontSize);
        }

        @Override
        public double imageWidth(InlineImage image, double height) {
            return height * context.imageRatio(image.path());
        }

        @Override
        public VerticalMetrics vertical(FontFace font) {
            return text.vertical(font);
        }

        @Override
        public void validate(Box root) {
            text.validate(root);
        }
    }

    /** Returns to the state before the layout: the default probe digits and no references. */
    public void reset() {
        this.measurer = PageNumbers.probing(real, PageNumbers.firstProbe());
        this.pages = Map.of();
        this.numbers = Map.of();
        this.headings = Map.of();
    }

    /**
     * The probe number of {@code {page}} and {@code {pages}} ({@link PageNumbers}). It changes what the texts with
     * those markers measure: the caller forgets their memorized line breaking ({@link LineBreakingMemo#forget}) and
     * what the cache of the {@link LayoutContext} keeps of them.
     */
    public void reservePageNumbers(String probe) {
        this.measurer = PageNumbers.probing(real, probe);
    }

    /**
     * The values of the references to other boxes in the text ({@code {page of: id}} and {@code {number of: id}},
     * reference/common/references.md): they are replaced before breaking the text into lines, so the text measures what
     * is going to be painted. The pages are an assumption that the {@link dev.stxt.pdf.layout.Paginator} repeats until
     * it holds. As with {@link #reservePageNumbers}, the caller forgets the texts whose value changes.
     */
    public void assume(Map<String, String> pages, Map<String, String> numbers) {
        this.pages = Map.copyOf(pages);
        this.numbers = Map.copyOf(numbers);
    }

    /**
     * The running heads of the page whose {@code Page overlay} boxes are going to be laid out
     * ({@code {heading N}} and {@code {heading N: first}}, reference/common/references.md): the text of the heading of
     * each level in effect on it, by the id of the reference, and empty where there is none yet. They change from one
     * page to the next, so the {@link dev.stxt.pdf.layout.Paginator} sets them before each page and, as with
     * {@link #assume}, the caller forgets the texts that contain them.
     */
    public void headings(Map<String, String> headings) {
        this.headings = Map.copyOf(headings);
    }

    /**
     * Measures the text as it is broken into lines: {@code {page}} and {@code {pages}} as the probe number, and an
     * image inside the text with its file.
     */
    public TextMeasurer measurer() {
        return measurer;
    }

    /**
     * Measures what is painted: the measurer of the engine, with an image inside the text measured with its file.
     * {@link PageNumbers#number} places with it the lines whose markers already have their value.
     */
    public TextMeasurer realMeasurer() {
        return real;
    }

    /** What the reference of that kind to {@code id} is worth now, or {@code null} if nothing was assumed for it. */
    String referenced(Reference.Kind kind, String id) {
        return switch (kind) {
            case PAGE -> pages.get(id);
            case NUMBER -> numbers.get(id);
            case HEADING -> headings.get(id);
        };
    }
}
