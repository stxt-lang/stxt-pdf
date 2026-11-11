package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.inline.LineBreaker;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.InlineImage;
import dev.stxt.pdf.model.Preformatted;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The text lines of a fragment, each run with its font (reference/common/text-formatting.md), as {@link StackingContext}
 * paints them in the content step of a box. The underline is painted before the text and the strikethrough after it;
 * both outside the marked content, because they are not text (in the tagged PDF, <em>artifacts</em>).
 *
 * <p>The runs of each line are split first into {@linkplain Sequence sequences}: consecutive runs that are one marked
 * content of the tagged PDF (same link, the call or the label of a footnote, which are marked apart, or an image, which
 * is a sequence of its own). Text sequences of the same kind that follow one another, also from one line to the next,
 * share their marked content ({@link Tagging#beginText} if they have no link, {@link Tagging#beginLinkText} if they
 * have one), and that of a link hangs from the {@code Link} of the tagged PDF (reference/common/links.md). Each link
 * sequence has one clickable area per line ({@link #paintLinkZone}).</p>
 *
 * <p>A line that is not the last one ends with an invisible space, so that whoever joins the lines does not run the
 * words together; a line split inside a word ({@link TextLine#splitsWord()}) does not, and if the line breaking added
 * a hyphen to it ({@link TextLine#hyphenated()}) the sequence that ends it carries an {@code ActualText} in which the
 * hyphen is the soft hyphen U+00AD, which is how a tagged PDF says that the hyphen was added by the line breaking
 * (reference/implementation/pdf.md). A line that ends with a hyphen written in the word, or with none
 * ({@code Break long words}), is read as it is painted.</p>
 */
final class TextPainter {

    /** What makes runs one marked content of the tagged PDF (reference/implementation/pdf.md). */
    private enum Kind {
        /** Text without a link. */
        TEXT,
        /** Text with a link: the same destination. */
        LINK,
        /** The call of a footnote: the same note. */
        FOOTNOTE_CALL,
        /** The number of a footnote. */
        FOOTNOTE_LABEL,
        /** An image inside the text, always a sequence of its own. */
        IMAGE
    }

    /**
     * The readable text of a barcode ({@code Readable text}, reference/components/barcode.md), whose line is relative
     * to the content box of its image: each run with the baseline that the font of the box gives it in the line, in
     * the color of the box. The canvas marks it with the image it belongs to ({@link Canvas#image}).
     */
    static void paintReadable(Canvas canvas, Box box, Rect content, List<TextLine> text) {
        for (TextLine line : text) {
            // Inside the loop: a barcode without a readable text has no line, and may have no font.
            FontFace font = box.style().typography().fontFace();
            double baseline = canvas.vertical(font).baseline(content.y() + line.y(), line.height(), line.fontSize());
            for (TextLine.Run run : line.runs()) {
                canvas.text(content.x() + line.x() + run.x(), baseline, run.text(), font, line.fontSize(),
                        box.style().typography().color(), 0, 0);
            }
        }
    }

    /**
     * The runs {@code from} to {@code to} (exclusive) of a line, which are one marked content: their {@code kind} and
     * {@code target}, the destination of a link or the {@code Id} of the footnote of a call ({@code null} otherwise).
     */
    private record Sequence(int from, int to, Kind kind, String target) {

        /** Whether the marked content of {@code previous}, open, goes on with this one. */
        boolean continues(Sequence previous) {
            return previous != null && kind != Kind.IMAGE && kind == previous.kind
                    && Objects.equals(target, previous.target);
        }
    }

    private final Canvas canvas;
    private final Tagging tags;
    private final Box box;
    private final Style style;
    private final FontFace font;
    private final VerticalMetrics metrics;
    private final Rect content;
    private final List<TextLine> lines;
    /** The text sequence whose marked content is open, {@code null} if there is none. */
    private Sequence open;

    private TextPainter(Canvas canvas, Box box, Rect content, List<TextLine> lines) {
        this.canvas = canvas;
        this.tags = canvas.tagging();
        this.box = box;
        this.style = box.style();
        this.font = style.typography().fontFace();
        this.metrics = canvas.vertical(font);
        this.content = content;
        this.lines = lines;
    }

    /** Paints the {@code lines} of a fragment of {@code box}, relative to its content box {@code content}. */
    static void paint(Canvas canvas, Box box, Rect content, List<TextLine> lines) {
        if (!lines.isEmpty()) {
            new TextPainter(canvas, box, content, lines).paint();
        }
    }

    private void paint() {
        paintDecorations(true);
        for (int n = 0; n < lines.size(); n++) {
            paintLine(n);
        }
        if (open != null) {
            tags.endText();
        }
        paintDecorations(false);
    }

    private void paintLine(int n) {
        TextLine l = lines.get(n);
        // The line break swallows the space: it is written invisible after the last word, so that whoever joins the
        // text of the lines (screen reader, extraction, search) does not run the words together. A line split inside
        // a word has no space: its two parts are one word, and neither does the drop cap (the first line of a
        // Paragraph with Drop cap, reference/components/text.md), which is the first letter of the word that starts
        // the line after it.
        boolean spaceAfter = n < lines.size() - 1 && style.typography().preformatted() == Preformatted.NONE
                && !l.splitsWord() && !(n == 0 && style.dropCap() > 0);
        double baseline = metrics.baseline(content.y() + l.y(), l.height(), l.fontSize());
        // With letter spacing, whoever extracts the text guesses spaces between the letters: each sequence of runs of
        // a line carries its text separately (reference/implementation/pdf.md). A styled run can have its own.
        double letterSpacing = style.typography().letterSpacing();
        boolean spaced = l.runs().stream().anyMatch(r -> r.format().letterSpacing(letterSpacing) != 0);
        for (Sequence seq : sequences(l.runs())) {
            boolean end = seq.to() == l.runs().size();
            if (seq.kind() == Kind.IMAGE) {
                paintImage(l, seq, baseline, spaceAfter && end);
                continue;
            }
            if (!seq.continues(open)) {
                if (open != null) {
                    tags.endText();
                }
                begin(seq);
                open = seq;
            }
            // The call of a footnote inside a link is also part of the link.
            if (l.runs().get(seq.from()).format().link() != null) {
                paintLinkZone(l, seq);
            }
            // The sequence carries its text when letter spacing would make extractors guess spaces, and when it ends
            // the line with the hyphen of a split word, which is read as a soft hyphen.
            boolean actual = spaced || l.hyphenated() && end;
            if (actual) {
                tags.beginActualText(actualText(l, seq, spaceAfter));
            }
            for (int i = seq.from(); i < seq.to(); i++) {
                TextLine.Run r = l.runs().get(i);
                String shown = spaceAfter && i == l.runs().size() - 1 ? r.text() + " " : r.text();
                // A superscript (the call or the label of a footnote, a styled run) is raised above the baseline, and
                // a subscript is lowered.
                canvas.text(content.x() + l.x() + r.x(), baseline - r.format().rise() * l.fontSize(), shown,
                        r.format().face(font), l.fontSize() * r.format().scale(),
                        r.format().color(style.typography().color()), l.wordSpacing(),
                        r.format().letterSpacing(letterSpacing));
            }
            if (actual) {
                tags.endActualText();
            }
        }
    }

    /** The runs of a line split into sequences, in order. */
    private static List<Sequence> sequences(List<TextLine.Run> runs) {
        List<Sequence> sequences = new ArrayList<>();
        int from = 0;
        while (from < runs.size()) {
            TextFormat f = runs.get(from).format();
            Kind kind = kind(f);
            String target = target(f, kind);
            int to = from + 1;
            while (kind != Kind.IMAGE && to < runs.size() && kind(runs.get(to).format()) == kind
                    && Objects.equals(target(runs.get(to).format(), kind), target)) {
                to++;
            }
            sequences.add(new Sequence(from, to, kind, target));
            from = to;
        }
        return sequences;
    }

    private static Kind kind(TextFormat f) {
        if (f.image() != null) {
            return Kind.IMAGE;
        }
        if (f.footnote() != null) {
            return Kind.FOOTNOTE_CALL;
        }
        if (f.label()) {
            return Kind.FOOTNOTE_LABEL;
        }
        return f.link() == null ? Kind.TEXT : Kind.LINK;
    }

    private static String target(TextFormat f, Kind kind) {
        return kind == Kind.LINK ? f.link() : kind == Kind.FOOTNOTE_CALL ? f.footnote() : null;
    }

    /** Opens the marked content of a text sequence. */
    private void begin(Sequence seq) {
        switch (seq.kind()) {
            case FOOTNOTE_CALL -> tags.beginFootnoteCall(box, seq.target());
            case FOOTNOTE_LABEL -> tags.beginFootnoteLabel(box);
            case TEXT -> tags.beginText(box);
            case LINK -> tags.beginLinkText(box);
            default -> throw new IllegalStateException("An image is not text: " + seq);
        }
    }

    /**
     * An image inside the text (reference/common/text-formatting.md): as tall as the size of the line, centered on the
     * capitals on the baseline, like the image marker of a list; outside any text sequence, with its own marked
     * content. If it ends a line that has the invisible space ({@code spaceAfter}), it writes the space apart.
     */
    private void paintImage(TextLine l, Sequence seq, double baseline, boolean spaceAfter) {
        if (open != null) {
            tags.endText();
            open = null;
        }
        TextLine.Run r = l.runs().get(seq.from());
        String link = r.format().link();
        double height = l.fontSize();
        double capitals = metrics.capHeight() * l.fontSize();
        double x = content.x() + l.x() + r.x();
        canvas.inlineImage(box, r.format().image(), link,
                new Rect(x, baseline - capitals / 2 - height / 2, r.width(), height));
        if (link != null) {
            paintLinkZone(l, seq);
        }
        if (spaceAfter) {
            // The invisible space of the line break, which the image cannot carry.
            tags.beginText(box);
            canvas.text(x + r.width(), baseline, " ", font, l.fontSize(),
                    r.format().color(style.typography().color()), 0, 0);
            tags.endText();
        }
    }

    /**
     * The text of a sequence, with the invisible trailing space if it reaches the end of the line and the line has one
     * ({@code spaceAfter}), and with the hyphen of a split word turned into the soft hyphen U+00AD if it reaches it.
     */
    private static String actualText(TextLine l, Sequence seq, boolean spaceAfter) {
        StringBuilder text = new StringBuilder();
        for (int i = seq.from(); i < seq.to(); i++) {
            text.append(l.runs().get(i).text());
        }
        if (seq.to() == l.runs().size()) {
            if (l.hyphenated() && text.length() > 0 && text.charAt(text.length() - 1) == LineBreaker.HYPHEN.charAt(0)) {
                text.setCharAt(text.length() - 1, LineBreaker.SOFT_HYPHEN);
            }
            if (spaceAfter) {
                text.append(' ');
            }
        }
        return text.toString();
    }

    /**
     * The clickable area (reference/common/links.md) of the runs of a sequence of the line {@code l}, with the
     * destination of its first run (a bold part inside a link is a single area): from the start of its first run to
     * the end of its last one, relative to the line.
     */
    private void paintLinkZone(TextLine l, Sequence seq) {
        List<TextLine.Run> runs = l.runs();
        StringBuilder text = new StringBuilder();
        for (int i = seq.from(); i < seq.to(); i++) {
            // An image says its alternative text, if it has one; what is read of a link is its text.
            InlineImage image = runs.get(i).format().image();
            text.append(image != null ? (image.alt() != null ? image.alt() : "") : runs.get(i).text());
        }
        TextLine.Run first = runs.get(seq.from());
        TextLine.Run last = runs.get(seq.to() - 1);
        Rect area = new Rect(content.x() + l.x() + first.x(), content.y() + l.y(),
                last.x() + last.width() - first.x(), l.height());
        tags.link(box, area, first.format().link(), text.toString());
    }

    /** Underline ({@code underline}) or strikethrough lines of the runs, with the color of the text of each run. */
    private void paintDecorations(boolean underline) {
        for (TextLine l : lines) {
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), l.fontSize());
            for (TextLine.Run r : l.runs()) {
                // An image is neither underlined nor struck out, not even inside a link.
                if (r.format().image() != null || (underline ? !r.format().underline() : !r.format().strike())) {
                    continue;
                }
                DecorationMetrics d = canvas.decoration(r.format().face(font));
                double x = content.x() + l.x() + r.x();
                // The underline stays on the line; the strikethrough of a superscript or a subscript goes with its
                // letters, at their baseline and their size.
                double rise = r.format().rise();
                canvas.background(underline ? d.underline(x, baseline, r.width(), l.fontSize())
                        : rise == 0 ? d.strikeout(x, baseline, r.width(), l.fontSize())
                        : d.strikeout(x, baseline - rise * l.fontSize(), r.width(),
                                l.fontSize() * r.format().scale()),
                        r.format().color(style.typography().color()));
            }
        }
    }

    /**
     * What a box with a link says, for whoever cannot see it: the alternative text of an image or the text of the box
     * and of what it contains (without the page number of the entries of a table of contents); {@code null} if it has
     * no text.
     */
    static String describe(Box box) {
        if (box.style().image().alt() != null) {
            return box.style().image().alt();
        }
        StringBuilder out = new StringBuilder();
        collectText(box, out);
        // A forced line break (U+2028) is a space too.
        String text = out.toString().replaceAll("[\\s\\u2028]+", " ").strip();
        return text.isEmpty() ? null : text;
    }

    private static void collectText(Box box, StringBuilder out) {
        if (box.role() == Role.PAGE_NUMBER) {
            return;
        }
        if (box.hasText()) {
            out.append(Reference.shown(box.text())).append(' ');
        }
        box.children().forEach(child -> collectText(child, out));
    }
}
