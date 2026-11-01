package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.pdf.tree.PropertyKey.HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MAX_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MIN_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.OVERFLOW;
import static dev.stxt.pdf.tree.PropertyKey.ROTATE;

/**
 * The footnotes of a document while its tree is built (reference/components/footnote.md): the calls
 * ({@code [^id]}, which {@link MarkdownBuilder} finds in the texts), the {@code Footnote} boxes and their numbering.
 *
 * <p>The calls are numbered in document order, as they are read, with the {@code Footnote numbering} and the
 * {@code Footnote number format} of their section: the number is the text of the call run, so the text measures what
 * is painted. The notes hang from the footnote area of their section ({@link Element#FOOTNOTES}), whatever box of the
 * document holds them. When the whole tree is read ({@link #finish}), every call has its note in the same section,
 * every note is called once, and each note gets its number as a superscript before its first text.</p>
 */
final class Footnotes {

    /** Space between the text of the page and the rule of the footnote area, by default, in points. */
    static final double DEFAULT_GAP = 8;
    /** Thickness of the rule that separates the footnotes from the text, by default, in points. */
    static final double DEFAULT_RULE = 0.5;
    /** Space between the rule and the first note, by default, in points. */
    static final double DEFAULT_PADDING = 4;

    /** A call: its line, its section and its number as it is written. */
    private record Call(int line, int section, String label) {
    }

    /** A note: its box and its section. */
    private record Note(Box box, int section) {
    }

    private final Map<String, Call> calls = new LinkedHashMap<>();
    private final Map<String, Note> notes = new LinkedHashMap<>();
    /** The footnote area of the section being built. */
    private Box area;
    private int section = -1;
    private int next = 1;
    private NumberFormat format = NumberFormat.DECIMAL;
    /**
     * Why a call is not allowed where the text being read is ({@code a Page overlay}, {@code a heading},
     * {@code a Footnote}); {@code null} if it is allowed.
     */
    String noCalls;
    /** Whether the content of a {@code Footnote} is being built. */
    boolean inside;

    /** The footnote area of the section being built; {@code null} before the first section. */
    Box area() {
        return area;
    }

    /**
     * Starts a section: its footnote area, which the caller adds to the box of the section after its content, out of
     * its flow, with the {@code For element: Footnotes} rules over the default look (a rule of {@value #DEFAULT_RULE} pt on top, {@value #DEFAULT_GAP} pt above it and
     * {@value #DEFAULT_PADDING} pt below), and its numbering.
     */
    Box beginSection(BuildContext ctx, InlineNode n, Box sectionBox, Section.FootnoteNumbering numbering,
                     NumberFormat numberFormat) {
        section++;
        if (numbering == Section.FootnoteNumbering.RESTART) {
            next = 1;
        }
        format = numberFormat;
        Style s = Style.inheriting(sectionBox.style());
        s.setLayout(Layout.BLOCK);
        s.setPosition(Position.FOOTNOTES);
        s.box().setMargin(new Sides<>(Value.pt(DEFAULT_GAP), Value.pt(0), Value.pt(0), Value.pt(0)));
        s.box().setBorder(new Sides<>(Value.pt(DEFAULT_RULE), Value.pt(0), Value.pt(0), Value.pt(0)));
        s.box().setPadding(new Sides<>(Value.pt(DEFAULT_PADDING), Value.pt(0), Value.pt(0), Value.pt(0)));
        ctx.rules.apply(Element.FOOTNOTES, s);
        validateArea(s);
        ModelValidator.validateBox(ctx, s, sectionBox);
        area = new Box(null, Element.FOOTNOTES, s, false, n.getLine());
        ctx.definiteHeight.put(area, false);
        return area;
    }

    /**
     * What a {@code For element: Footnotes} rule cannot change: the height of the area, which is that of the notes
     * of each page, its rotation and its overflow.
     */
    private static void validateArea(Style s) {
        for (String property : List.of(HEIGHT, MIN_HEIGHT, MAX_HEIGHT, ASPECT_RATIO, ROTATE, OVERFLOW)) {
            if (s.isDeclared(property)) {
                throw error(s.line(property), ErrorCode.FOOTNOTES_PROPERTY_WITHOUT_EFFECT, Nodes.label(property)
                        + " has no effect on Footnotes, the footnote area: its height is that of the notes of each "
                        + "page, and it is neither rotated nor clipped. It is not needed");
            }
        }
    }

    /**
     * A call {@code [^id]} read at {@code line}: its number, written with the format of the section. A second call to
     * the same note, or a call where none is allowed, is an error.
     */
    String call(String id, int line) {
        if (noCalls != null) {
            throw error(line, ErrorCode.FOOTNOTE_CALL_NOT_ALLOWED, "The footnote call [^" + id + "] is inside "
                    + noCalls + ": a footnote is called from the text of the page");
        }
        Call previous = calls.get(id);
        if (previous != null) {
            throw error(line, ErrorCode.FOOTNOTE_CALL_DUPLICATE, "The footnote " + id + " is already called on line "
                    + previous.line() + ": a footnote is called once, and a second note needs its own Footnote");
        }
        String label = format.format(next++);
        calls.put(id, new Call(line, section, label));
        return label;
    }

    /** A {@code Footnote} box read, with its {@code Id}. */
    void add(Box note) {
        notes.put(note.id(), new Note(note, section));
    }

    /**
     * Checks that every call has its note in its section and that every note is called, and writes the number of
     * each note as a superscript before its first text, which has to be a {@code Paragraph}, a {@code Text} or the
     * first paragraph of a {@code Markdown} without headings, images or code blocks.
     */
    void finish() {
        for (Map.Entry<String, Call> e : calls.entrySet()) {
            Note note = notes.get(e.getKey());
            if (note == null) {
                throw error(e.getValue().line(), ErrorCode.FOOTNOTE_UNKNOWN, "The footnote call [^" + e.getKey()
                        + "] has no Footnote with Id " + e.getKey() + ": the note is missing, or its Id is another");
            }
            if (note.section() != e.getValue().section()) {
                throw error(e.getValue().line(), ErrorCode.FOOTNOTE_UNKNOWN, "The footnote call [^" + e.getKey()
                        + "] is in another section than its Footnote (line " + note.box().line()
                        + "): the note goes in the section of its call");
            }
        }
        for (Note note : notes.values()) {
            Call call = calls.get(note.box().id());
            if (call == null) {
                throw error(note.box().line(), ErrorCode.FOOTNOTE_NOT_CALLED, "The Footnote " + note.box().id()
                        + " is not called from any text: the call [^" + note.box().id()
                        + "] is missing, or the note is not needed");
            }
            validateContent(note.box(), note.box());
            label(note.box(), call.label());
        }
    }

    /** What a {@code Markdown} can generate inside a note and a note does not accept. */
    private static void validateContent(Box note, Box box) {
        for (Box child : box.children()) {
            String what = child.headingLevel() > 0 ? "a heading" : child.isImage() ? "an image"
                    : child.element() == Element.CODE_BLOCK ? "a code block" : null;
            if (what != null) {
                throw error(child.line(), ErrorCode.FOOTNOTE_CONTENT, "The Footnote " + note.id() + " contains " + what
                        + ": a footnote has paragraphs, lists and tables, without headings, images or code blocks");
            }
            validateContent(note, child);
        }
    }

    /**
     * Writes the number before the first text of the note, as a superscript run followed by a space
     * (reference/components/footnote.md): the first element of the note, or the first block of a {@code Markdown}
     * that opens it, has to be a text.
     */
    private static void label(Box note, String number) {
        Box first = note.inFlowChildren().isEmpty() ? null : note.inFlowChildren().get(0);
        if (first != null && first.element() == Element.MARKDOWN && !first.inFlowChildren().isEmpty()) {
            first = first.inFlowChildren().get(0);
        }
        if (first == null || !(first.element() == Element.PARAGRAPH || first.element() == Element.TEXT)) {
            throw error(first != null ? first.line() : note.line(), ErrorCode.FOOTNOTE_CONTENT, "The Footnote "
                    + note.id() + " has to start with a Paragraph, a Text or a Markdown paragraph, which carries its "
                    + "number" + (first != null ? ", and it starts with a " + first.element().label() : ""));
        }
        List<TextRun> prefix = new ArrayList<>(2);
        prefix.add(new TextRun(number, TextFormat.PLAIN.asLabel()));
        prefix.add(new TextRun(" ", TextFormat.PLAIN));
        first.prependRuns(prefix);
    }
}
