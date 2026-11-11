package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.CaptionPosition;
import dev.stxt.pdf.model.FigureCounter;
import dev.stxt.pdf.model.NumberFormat;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.PropertyKey.ALT;
import static dev.stxt.pdf.tree.PropertyKey.NUMBERED;

/**
 * The figures of a document while its tree is built (reference/components/figure.md): what a {@code Figure} needs
 * from its content, where its caption goes, which counter numbers it and the number itself.
 *
 * <p>A {@code Figure} has exactly one element ({@code Image}, {@code Barcode}, {@code Table} or {@code Block}) and a
 * {@code Caption}. Without {@code Caption position}, the caption goes below the content and above a table; without
 * {@code Figure counter}, a table counts among the tables and the rest among the figures. With
 * {@code Figure numbering: chapter} in its section, the counter starts again in each chapter. A figure is numbered unless
 * it, or a rule that reaches it, declares {@code Numbered: false}: what it inherits does not count, because a figure
 * with a caption is numbered by default. The number is written before the text of the caption, like that of a heading
 * ({@link HeadingNumbers#prefix}). A figure is content for whoever cannot see it: the image inside carries its
 * {@code Alt}, and a figure with a {@code Block}, its own.</p>
 */
final class Figures {

    /** One counter per {@link FigureCounter}, in document order, for the sections that number in a row. */
    private final int[] counters = new int[FigureCounter.values().length];
    /** One counter per {@link FigureCounter} for the sections that number by chapter: it starts again in each one. */
    private final int[] inChapter = new int[FigureCounter.values().length];
    /** The stretch of the document ({@link HeadingNumbers#stretch}) each counter by chapter belongs to. */
    private final int[] stretch = new int[FigureCounter.values().length];
    /** The {@code Figure numbering} of the section being built. */
    private Section.FigureNumbering numbering = Section.FigureNumbering.CONTINUE;

    /** Starts a section, with its {@code Figure numbering}. */
    void beginSection(Section.FigureNumbering figureNumbering) {
        numbering = figureNumbering;
    }

    /**
     * Records a figure in document order and returns its number ({@code "3"}, {@code "C"}, or {@code "2.3"} by
     * chapter), or {@code null} if it is not numbered. By chapter, the counter starts again with each chapter of
     * {@code headings} and with each heading that ends one, and the number of the chapter goes first, with a period;
     * outside any chapter, the number goes alone.
     */
    String next(FigureCounter counter, boolean numbered, NumberFormat format, HeadingNumbers headings) {
        if (!numbered) {
            return null;
        }
        int i = counter.ordinal();
        if (numbering == Section.FigureNumbering.CONTINUE) {
            return format.format(++counters[i]);
        }
        if (stretch[i] != headings.stretch()) {
            stretch[i] = headings.stretch();
            inChapter[i] = 0;
        }
        String number = format.format(++inChapter[i]);
        return headings.chapter() == null ? number : headings.chapter() + "." + number;
    }

    /** Whether the figure is numbered: unless it declares {@code Numbered: false}, itself or through a rule. */
    static boolean numbered(Style s) {
        return !s.isDeclared(NUMBERED) || s.typography().numbered();
    }

    /** Where the caption goes: as declared or, by default, above a table and below anything else. */
    static CaptionPosition captionPosition(Style s, Box content) {
        if (s.figure().captionPosition() != null) {
            return s.figure().captionPosition();
        }
        return content.element() == Element.TABLE ? CaptionPosition.TOP : CaptionPosition.BOTTOM;
    }

    /** The counter of the figure: as declared or, by default, the tables for a table and the figures for the rest. */
    static FigureCounter counter(Style s, Box content) {
        if (s.figure().counter() != null) {
            return s.figure().counter();
        }
        return content.element() == Element.TABLE ? FigureCounter.TABLES : FigureCounter.FIGURES;
    }

    /**
     * The alternative text of the figure: an image inside a figure carries its {@code Alt} (a figure with a caption is
     * content, never decoration), a figure with a {@code Block} carries its own, and a figure with an image or a table
     * does not declare one, which would have no effect.
     */
    static void validateAlt(InlineNode n, Style s, Box content) {
        if (content.element() == Element.IMAGE && content.style().image().alt() == null) {
            // A Barcode without Alt is BARCODE_WITHOUT_ALT when it is built, before getting here.
            throw error(content.line(), ErrorCode.FIGURE_WITHOUT_ALT, "The Image of a Figure needs Alt, the text that "
                    + "says what it shows to whoever cannot see it: a figure with a caption is content, not decoration");
        }
        if (content.element() == Element.BLOCK && s.image().alt() == null) {
            throw error(n, ErrorCode.FIGURE_WITHOUT_ALT, "A Figure with a Block needs Alt, the text that says what "
                    + "the whole figure shows to whoever cannot see it, as in Alt: The two photos of the device");
        }
        if (content.element() != Element.BLOCK && s.isDeclared(ALT)) {
            throw error(s.line(ALT), ErrorCode.FIGURE_PROPERTY_WITHOUT_EFFECT, "Alt has no effect on a Figure with "
                    + (content.element() == Element.TABLE ? "a Table, which is read by itself"
                    : "an " + content.element().label() + ", which carries the Alt itself") + ": it is not needed");
        }
    }
}
