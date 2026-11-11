package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.FootnoteNumberFormat;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.model.TrimMargins;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.inline;
import static dev.stxt.pdf.tree.ValueReader.bool;
import static dev.stxt.pdf.tree.ValueReader.enumValue;
import static dev.stxt.pdf.tree.ValueReader.positiveInteger;

/**
 * What a {@code Section} says about its pages (reference/components/document.md): its {@code Id}, its medium, its
 * page numbering, whether it carries the {@code Page overlay} elements of the document, on which page it begins and
 * how it numbers its footnotes (reference/components/footnote.md) and its figures (reference/components/figure.md), with the rules of those properties. Its box and its
 * content are built by {@link BoxTreeBuilder}, as for any element.
 */
final class SectionReader {

    private SectionReader() {
    }

    /**
     * The properties of a section, with their default values where it does not declare them. They are those of
     * {@link Section} except its box and its footnote area.
     */
    record Properties(String id, PageSetup pageSetup, Section.PageNumbering numbering, NumberFormat pageNumberFormat,
                      int firstNumber, boolean inheritPageOverlays, Section.BeginOn beginOn,
                      Section.FootnoteNumbering footnoteNumbering, FootnoteNumberFormat footnoteNumberFormat,
                      Section.FigureNumbering figureNumbering, TrimMargins trimMargins) {
    }

    /**
     * Reads the properties of the section {@code n}: its {@code Page} inherits by groups the one of the document,
     * {@code documentPageSetup}; {@code first} if it is the first section of the document.
     */
    static Properties read(BuildContext ctx, InlineNode n, PageSetupReader.Spec documentPageSetup, boolean first) {
        String id = null;
        PageSetupReader.Spec pageSetup = documentPageSetup;
        Section.PageNumbering numbering = Section.PageNumbering.CONTINUE;
        NumberFormat pageNumberFormat = NumberFormat.DECIMAL;
        Node pageNumberFormatNode = null;
        Node startNode = null;
        int firstNumber = 1;
        boolean inheritPageOverlays = true;
        Section.BeginOn beginOn = Section.BeginOn.ANY;
        Section.FootnoteNumbering footnoteNumbering = Section.FootnoteNumbering.CONTINUE;
        FootnoteNumberFormat footnoteNumberFormat = FootnoteNumberFormat.DECIMAL;
        Section.FigureNumbering figureNumbering = Section.FigureNumbering.CONTINUE;
        TrimMargins trimMargins = TrimMargins.NONE;
        for (Node p : n.getChildren()) {
            switch (p.getCanonicalName()) {
                case "id" -> id = ctx.id(p);
                case "footnote-numbering" -> footnoteNumbering = enumValue(Section.FootnoteNumbering.class, p);
                case "footnote-number-format" -> footnoteNumberFormat = enumValue(FootnoteNumberFormat.class, p);
                case "figure-numbering" -> figureNumbering = enumValue(Section.FigureNumbering.class, p);
                case "page" -> pageSetup = PageSetupReader.pageSetup(inline(p), documentPageSetup);
                case "page-numbering" -> numbering = enumValue(Section.PageNumbering.class, p);
                case "page-number-format" -> {
                    pageNumberFormat = enumValue(NumberFormat.class, p);
                    pageNumberFormatNode = p;
                }
                case "first-number" -> {
                    firstNumber = positiveInteger(p);
                    startNode = p;
                }
                case "inherit-page-overlays" -> inheritPageOverlays = bool(p);
                case "trim-margins" -> trimMargins = enumValue(TrimMargins.class, p);
                case "begin-on" -> {
                    beginOn = enumValue(Section.BeginOn.class, p);
                    if (first && beginOn == Section.BeginOn.ODD) {
                        // The first page of the document is already odd (reference/components/document.md).
                        throw error(p, ErrorCode.BEGIN_ON_FIRST_SECTION, "Begin on: odd has no effect in the first "
                                + "section, which already starts on page 1: it is not needed");
                    }
                }
                default -> {
                }
            }
        }
        // What would have no effect (reference/components/document.md#page-numbering).
        if (startNode != null && numbering != Section.PageNumbering.RESTART) {
            throw error(startNode, ErrorCode.FIRST_NUMBER_WITHOUT_RESTART, "First number only has an effect with Page numbering: "
                    + "restart, which is when the numbering starts again: restart is missing or First number is not needed");
        }
        if (pageNumberFormatNode != null && numbering == Section.PageNumbering.NONE) {
            throw error(pageNumberFormatNode, ErrorCode.PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS, "Page number format has no "
                    + "effect with Page numbering: none, because the pages of the section have no number: it is not needed");
        }
        return new Properties(id, pageSetup.pageSetup(), numbering, pageNumberFormat, firstNumber, inheritPageOverlays,
                beginOn, footnoteNumbering, footnoteNumberFormat, figureNumbering, trimMargins);
    }
}
