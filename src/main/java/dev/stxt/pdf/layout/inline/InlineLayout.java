package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakRules;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.AdjoiningMargins;
import dev.stxt.pdf.layout.BoxModel;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutAlgorithm;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.Slice;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenations;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenator;
import dev.stxt.pdf.model.Exclusion;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Preformatted;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Layout {@code inline}: text broken into line boxes (reference/common/text-formatting.md).
 *
 * <p>Steps: resolve the content width, break with {@link LineBreaker}, align each line,
 * height = number of lines × {@code Line height}. With {@code justify}, the leftover space of each line
 * is distributed among its spaces ({@link TextLine#wordSpacing()}), except on the last one of the text,
 * which is a single paragraph; a line cut by a page break is not the last one.</p>
 *
 * <p>On each page (reference/common/breaks.md) the lines that fit are placed from the one of the
 * {@link BreakToken.Line}, respecting widows and orphans ({@link BreakRules#cutLines}); if none fits,
 * {@link BreakToken#BEFORE} is returned so that the parent breaks before the box.</p>
 *
 * <p><strong>Beside a side box</strong> (reference/components/block.md) the lines are shorter: the
 * {@link ConstraintSpace#exclusions()} say what the side boxes of the parent take from each side of the box, and
 * {@link LineWidths} turns them into the line box of each line. The line breaking depends on them, so it is made for
 * the fragment that has them and the token records it ({@link BreakToken.Line#exclusions()}); the next page, with
 * nothing beside the text, breaks the rest of it again from the start of its first line
 * ({@link LineBreaker.Line#start()}), at the whole width.</p>
 *
 * <p><strong>Drop cap</strong> (reference/components/text.md). In a {@code Paragraph} with {@code Drop cap}, the
 * first letter of the text is taken out of it and set as a line of its own ({@link #dropCap}), at the size whose
 * capitals are as tall as its {@code Drop cap} lines, with its baseline on that of the last of them; those lines
 * start after it, like beside a side box, and the paragraph is at least that tall. The lines and their drop cap go
 * together: a page break does not fall among them.</p>
 *
 * <p><strong>First line indent</strong> (reference/common/typography.md). The first line of the paragraph starts
 * after the {@code First line indent} of the box, or before it if it is negative, and is that much shorter or longer
 * ({@link LineWidths#withFirstLineIndent}); only that line: not the first one of a fragment that goes on from another
 * page, whose text starts at a later position or at a later line.</p>
 *
 * <p>{@code {page}} and {@code {pages}} are measured as a probe number and replaced when
 * pagination ends ({@link PageNumbers#number}). The references to other boxes and the running heads
 * ({@link Reference}) are replaced before breaking the text into lines, with the values the
 * {@link dev.stxt.pdf.layout.Paginator} gives ({@link TextValues}).</p>
 *
 * <p>With {@code Overflow: shrink} (reference/common/overflow.md) the text is broken at the {@code Size} of the box
 * and, if some line is wider than its line box or the lines are taller than its definite height, again at smaller
 * sizes, from {@code Size} down in steps of {@link #SHRINK_STEP} and {@code Min size} as the last one, until it fits
 * ({@link #shrink}). The lines keep the size they were broken at ({@link TextLine#fontSize()}) and the line height
 * follows it if it is a percentage.</p>
 *
 * <p>The line breaking of a text is done only once per available width (and height limit, if it shrinks, exclusions
 * and start position) and reused on each page ({@link LineBreakingMemo}): each fragment starts at another line
 * ({@code from}), so the cache of the {@link LayoutContext} does not help, and without it a text of N pages was broken
 * N times (quadratic cost). The algorithm keeps nothing of the document: the values of the text and that memory
 * belong to the {@link dev.stxt.pdf.layout.Engine}, which gives them to it.</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    /** How much {@code Overflow: shrink} reduces the {@code Size} at each step, in points. */
    public static final double SHRINK_STEP = 0.5;

    /**
     * The key of a memorized line breaking inside its box ({@link LineBreakingMemo}).
     *
     * @param limit  height of the content box the lines have to fit in, if the text shrinks and the height is definite
     *               or has a maximum; empty otherwise (it is not part of the key of a text that does not shrink)
     * @param beside what the side boxes take from the content box, by line ({@link #beside})
     * @param from   position of the text the line breaking starts at (reference/components/block.md); 0 is its start
     */
    record BrokenKey(double available, OptionalDouble limit, List<Exclusion> beside, int from) {
    }

    /**
     * The first letter of a {@code Paragraph} with {@code Drop cap} (reference/components/text.md), set as a line of
     * its own.
     *
     * @param letter the letter, as it is painted
     * @param format the format of the run it comes from
     * @param size   the size it is set at: its capitals are as tall as the {@code Drop cap} lines
     * @param width  the room it takes from the left of those lines: its advance plus a space of the paragraph
     * @param rest   the runs of the text without it
     */
    record DropCap(String letter, TextFormat format, double size, double width, List<TextRun> rest) {
    }

    /**
     * The lines of a text and the size they are broken at: the {@code Size} of the box, or the one it shrank to; the
     * line box of each one and, in a paragraph with {@code Drop cap} broken from its start, its drop cap.
     */
    record Broken(List<LineBreaker.Line> lines, double fontSize, LineWidths widths, DropCap dropCap) {
    }

    private final LayoutContext context;
    /** What the text measures and is worth now: the probe number, the references and the running heads. */
    private final TextValues values;
    /** Line breaking of each text per available width (and height limit); depends on {@link #values}. */
    private final LineBreakingMemo memo;

    /**
     * The layout of the text with the values and the memorized line breaking of the engine, which sets the former
     * through its protocol with the {@link dev.stxt.pdf.layout.Paginator} and empties both when it finishes.
     */
    public InlineLayout(LayoutContext context, TextValues values, LineBreakingMemo memo) {
        this.context = context;
        this.values = values;
        this.memo = memo;
    }

    /** Measures the text as it is broken: {@code {page}} and {@code {pages}} as the probe number. */
    private TextMeasurer measurer() {
        return values.measurer();
    }

    /**
     * The runs of the text of the box, with their references to other boxes already replaced and their
     * case transformed ({@link TextTransform}). In a preformatted text the soft hyphens are removed: its lines break
     * only where the text does (reference/common/text-formatting.md).
     */
    private List<TextRun> runs(Box box) {
        List<TextRun> runs = box.runs();
        if (Reference.contains(box.text())) {
            runs = runs.stream().map(r -> new TextRun(Reference.replace(r.text(), values::referenced), r.format()))
                    .toList();
        }
        if (box.style().typography().preformatted().enabled() && box.text().indexOf(LineBreaker.SOFT_HYPHEN) >= 0) {
            runs = runs.stream().map(r -> new TextRun(r.text().replace(String.valueOf(LineBreaker.SOFT_HYPHEN), ""),
                    r.format())).filter(r -> !r.text().isEmpty()).toList();
        }
        return TextTransform.apply(runs, box.style());
    }

    /**
     * The hyphenator of the text of the box with {@code Hyphenate: true}, the one of its language
     * (reference/common/typography.md); {@code null} without it. {@link Hyphenations#validate} has already checked
     * that the language has patterns.
     */
    private static Hyphenator hyphenator(Style s) {
        return s.typography().hyphenate() ? Hyphenations.forLocale(s.typography().locale()) : null;
    }

    /**
     * The runs of the text of a {@code Paragraph} with {@code Drop cap} without its first letter, whatever the
     * fragment: the positions of the text, which the tokens carry, are always those of this text. Without
     * {@code Drop cap}, the runs as they are.
     */
    private List<TextRun> textRuns(Box box) {
        List<TextRun> runs = runs(box);
        return box.style().dropCap() > 0 ? dropCapRest(runs) : runs;
    }

    /** The first letter of the text and the runs without it: the first code point after the leading white space. */
    private static List<TextRun> dropCapRest(List<TextRun> runs) {
        List<TextRun> rest = new ArrayList<>(runs);
        int i = 0;
        while (i < rest.size() && rest.get(i).text().isBlank()) {
            i++;
        }
        if (i == rest.size()) {
            return runs;
        }
        TextRun first = rest.get(i);
        String text = first.text().stripLeading();
        String remaining = text.substring(text.offsetByCodePoints(0, 1));
        if (remaining.isEmpty()) {
            rest.remove(i);
        } else {
            rest.set(i, new TextRun(remaining, first.format()));
        }
        return List.copyOf(rest.subList(i, rest.size()));
    }

    /** The first letter of the text and its format, as {@link #dropCapRest} takes them out. */
    private static TextRun dropCapLetter(List<TextRun> runs) {
        for (TextRun r : runs) {
            if (!r.text().isBlank()) {
                String text = r.text().stripLeading();
                return new TextRun(text.substring(0, text.offsetByCodePoints(0, 1)), r.format());
            }
        }
        return null;
    }

    /**
     * The drop cap of the paragraph at {@code fontSize} (reference/components/text.md): the first letter at the size
     * whose cap height spans its {@code Drop cap} lines, from the cap height of the first one to the baseline of the
     * last one, and the room it takes beside them: its advance plus a space of the paragraph.
     */
    private DropCap dropCap(Box box, List<TextRun> runs, double fontSize, double lineHeight) {
        Style s = box.style();
        TextRun letter = dropCapLetter(runs);
        if (letter == null) {
            return null;
        }
        FontFace font = s.typography().fontFace();
        VerticalMetrics metrics = measurer().vertical(font);
        double span = (s.dropCap() - 1) * lineHeight + metrics.capHeight() * fontSize;
        double size = span / metrics.capHeight();
        double advance = measurer().width(letter.text(), letter.format().face(font), size)
                + letter.format().letterSpacing(s.typography().letterSpacing())
                * letter.text().codePointCount(0, letter.text().length());
        double gap = measurer().width(" ", font, fontSize);
        return new DropCap(letter.text(), letter.format(), size, advance + gap, dropCapRest(runs));
    }

    /**
     * The lines of the text of the box at {@code fontSize} from position {@code from}, a single paragraph (in a
     * preformatted one, blank lines do not split it), beside the side boxes of the parent ({@code beside}) and, from
     * the start of a {@code Paragraph} with {@code Drop cap}, beside its drop cap too. Broken from its start, its
     * first line has the {@code First line indent} of the box.
     */
    private Broken breakText(Box box, double available, double fontSize, List<Exclusion> beside, int from) {
        Style s = box.style();
        double lineHeight = s.typography().usedLineHeight(fontSize);
        if (!box.hasText()) {
            return new Broken(List.of(), fontSize, LineWidths.uniform(available), null);
        }
        // Only the first line of the paragraph is indented: a text broken from a later position has none.
        double indent = from == 0 ? s.typography().firstLineIndent() : 0;
        if (s.typography().preformatted().enabled()) {
            // Preformatted text keeps its lines: it goes below the side boxes, never beside them.
            List<LineBreaker.Line> lines = List.copyOf(PreformattedText.lines(runs(box), s.typography().tabSize(),
                    s.typography().preformatted() == Preformatted.WRAP, available, indent, measurer(),
                    s.typography().fontFace(), fontSize, s.typography().letterSpacing()));
            return new Broken(lines, fontSize, LineWidths.uniform(available).withFirstLineIndent(indent), null);
        }
        LineWidths widths = LineWidths.of(available, lineHeight, beside).withFirstLineIndent(indent);
        List<TextRun> runs = runs(box);
        DropCap dropCap = null;
        if (s.dropCap() > 0) {
            dropCap = dropCap(box, runs, fontSize, lineHeight);
            if (dropCap != null) {
                runs = dropCap.rest();
                if (from > 0) {
                    // The drop cap goes with the first lines: a text broken from a later position has none, and
                    // nothing takes room from its lines.
                    dropCap = null;
                } else {
                    widths = widths.withDropCap(s.dropCap(), dropCap.width());
                }
            }
        }
        // With Break long words, a part of a word wider than a whole line of the box is cut at any character.
        double longWord = s.typography().breakLongWords() ? available : Double.POSITIVE_INFINITY;
        Paragraph paragraph = Paragraph.of(runs, measurer(), s.typography().fontFace(), fontSize,
                s.typography().letterSpacing(), hyphenator(s), longWord);
        if (paragraph == null) {
            return new Broken(List.of(), fontSize, widths, dropCap);
        }
        return new Broken(List.copyOf(LineBreaker.breakLines(paragraph, from, widths, s.typography().lineBreaking())),
                fontSize, widths, dropCap);
    }

    /**
     * The lines of the text at the {@code Size} of the box or, with {@code Overflow: shrink}
     * (reference/common/overflow.md), at the largest size, from {@code Size} down in steps of {@link #SHRINK_STEP} and
     * {@code Min size} as the last one, at which no line is wider than its line box and, with a {@code limit},
     * the lines are not taller than it. If none fits, those of {@code Min size}: {@link dev.stxt.pdf.layout.Overflow}
     * reports what still sticks out.
     */
    private Broken shrink(Box box, double available, OptionalDouble limit, List<Exclusion> beside, int from) {
        Style s = box.style();
        double size = s.typography().fontSize();
        Broken b = breakText(box, available, size, beside, from);
        if (!s.box().shrinks()) {
            return b;
        }
        double min = s.typography().minFontSize();
        while (!fits(b, s, limit) && size > min + Tolerance.EPSILON) {
            size = Math.max(min, size - SHRINK_STEP);
            b = breakText(box, available, size, beside, from);
        }
        return b;
    }

    /** The memorized line breaking of the text with that key ({@link LineBreakingMemo}), made the first time. */
    private Broken broken(Box box, double available, OptionalDouble limit, List<Exclusion> beside, int from) {
        return memo.broken(box, new BrokenKey(available, limit, beside, from),
                () -> shrink(box, available, limit, beside, from));
    }

    /**
     * Whether the lines fit: none is wider than its line box, with the tolerance of
     * {@link dev.stxt.pdf.layout.Overflow} ({@link Tolerance#MODEL}), and together they are not taller than
     * {@code limit}.
     */
    private boolean fits(Broken b, Style s, OptionalDouble limit) {
        FontFace font = s.typography().fontFace();
        List<LineBreaker.Line> lines = b.lines();
        for (int k = 0; k < lines.size(); k++) {
            if (LinePlacer.width(lines.get(k).runs(), measurer(), font, b.fontSize(), s.typography().letterSpacing())
                    > b.widths().width(k) + Tolerance.MODEL) {
                return false;
            }
        }
        return limit.isEmpty()
                || height(b, s, lines.size()) <= limit.getAsDouble() + Tolerance.MODEL;
    }

    /** Height of {@code count} lines of the text: at least the {@code Drop cap} lines, if it has a drop cap. */
    private static double height(Broken b, Style s, int count) {
        int lines = b.dropCap() != null ? Math.max(count, s.dropCap()) : count;
        return lines * s.typography().usedLineHeight(b.fontSize());
    }

    /**
     * The height of the content box that the lines of a text with {@code Overflow: shrink} have to fit in: that of its
     * definite height (declared, imposed by the parent or deduced from {@code Aspect ratio}) or, without one, of its
     * {@code Max height}; empty if nothing limits it, or if the text does not shrink.
     */
    private static OptionalDouble limit(Box box, BoxModel model) {
        if (!box.style().box().shrinks()) {
            return OptionalDouble.empty();
        }
        double decoration = model.border().vertical() + model.padding().vertical();
        OptionalDouble height = model.height().isPresent() ? model.height() : model.maxHeight();
        return height.isPresent() ? OptionalDouble.of(Math.max(0, height.getAsDouble() - decoration)) : OptionalDouble.empty();
    }

    /**
     * What the side boxes of the parent take from the content box of this fragment ({@link
     * BoxModel#exclusionsInContent}). A preformatted text never gets them: it goes below the side boxes.
     */
    private static List<Exclusion> beside(Box box, ConstraintSpace space, BoxModel model, boolean first) {
        if (box.style().typography().preformatted().enabled()) {
            return List.of();
        }
        return model.exclusionsInContent(space, first);
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        Style s = box.style();
        if (!box.hasText()) {
            return IntrinsicSizes.ZERO;
        }
        if (s.typography().preformatted().enabled()) {
            return PreformattedText.intrinsics(runs(box), s.typography().tabSize(), s.typography().preformatted() == Preformatted.WRAP,
                    s.typography().firstLineIndent(), measurer(), s.typography().fontFace(), s.typography().fontSize(),
                    s.typography().letterSpacing());
        }
        return LineBreaker.intrinsics(textRuns(box), measurer(), s.typography().fontFace(), s.typography().fontSize(),
                s.typography().letterSpacing(), hyphenator(s), s.typography().firstLineIndent());
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, context);
        Style s = box.style();
        double available = model.contentWidth();
        boolean first = from == null;
        BreakToken.Line token = first ? null : (BreakToken.Line) from;
        List<Exclusion> beside = beside(box, space, model, first);
        OptionalDouble limit = limit(box, model);

        // The line breaking of this fragment: the one of the token if nothing is beside the text in either, or else
        // the rest of the text from the first line that was not placed, broken again with what is beside it now.
        int position;
        int start;
        if (first) {
            position = 0;
            start = 0;
        } else if (token.exclusions().isEmpty() && beside.isEmpty()) {
            position = token.from();
            start = token.nextLine();
        } else {
            Broken previous = broken(box, available, limit, token.exclusions(), token.from());
            position = previous.lines().get(token.nextLine()).start();
            start = 0;
        }
        Broken b = broken(box, available, limit, beside, position);
        List<LineBreaker.Line> texts = b.lines();
        double fontSize = b.fontSize();
        double lineHeight = s.typography().usedLineHeight(fontSize);
        boolean dropCap = b.dropCap() != null && start == 0;

        int end = texts.size();
        int appeal = BreakAppeal.PERFECT_BREAK;
        if (space.fragmenting()) {
            double remaining = Slice.contentRemaining(model, first, space);
            // The drop cap and its lines go together: at least those lines stay with it.
            int orphans = dropCap ? Math.max(s.typography().orphans(), s.dropCap()) : s.typography().orphans();
            BreakRules.Cut cut = BreakRules.cutLines(texts.size(), start, BreakRules.capacity(remaining, lineHeight),
                    s.typography().widows(), orphans, space.atFragmentainerStart());
            if (cut.end() == start && start < texts.size()) {
                return LayoutResult.before(box);
            }
            end = widows(box, available, limit, b, cut.end(), start, s);
            appeal = cut.appeal();
            if (dropCap && end < texts.size() && end - start < s.dropCap() && !space.atFragmentainerStart()) {
                // Fewer lines than the drop cap spans would leave it hanging: the paragraph moves whole.
                return LayoutResult.before(box);
            }
        }

        int placed = end - start;
        double contentHeight = (dropCap ? Math.max(placed, s.dropCap()) : placed) * lineHeight;
        Slice slice = Slice.of(model, first, BreakToken.consumed(from), contentHeight, end == texts.size(), space);
        // The Vertical of a Cell with a value (reference/components/table.md): in the fragment the cell ends in, its
        // lines go down in what its row leaves over. A Paragraph with Drop cap is never a cell.
        double down = slice.contentOffset(s.cellVertical(), contentHeight);

        List<TextLine> lines = new ArrayList<>();
        if (dropCap) {
            lines.add(dropCapLine(s, b, lineHeight));
        }
        for (int j = start; j < end; j++) {
            LineBreaker.Line text = texts.get(j);
            double y = down + (j - start) * lineHeight;
            double left = b.widths().left(j);
            double right = b.widths().right(j);
            if (text.isEmpty()) {
                // No word fit beside the side box: a blank line.
                lines.add(new TextLine(left, y, 0, lineHeight, fontSize, "", 0, List.of(), false, left, right));
                continue;
            }
            // Preformatted lines are not justified, nor is the one a forced line break ends.
            boolean justify = !s.typography().preformatted().enabled() && j != texts.size() - 1 && !text.forced();
            lines.add(LinePlacer.place(text.runs(), measurer(), s, fontSize, left, right, b.widths().width(j), justify,
                    y, lineHeight, text.hyphenated(), text.splitsWord()));
        }

        Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(), slice.border(),
                slice.padding(), List.of(), lines, first, slice.finished());
        if (!slice.finished()) {
            // Lines are missing or, if they have ended, the box's own height.
            return new LayoutResult(fragment, new BreakToken.Line(end, slice.consumed(), position, beside),
                    end == texts.size() ? BreakAppeal.PERFECT_BREAK : appeal, List.of(), AdjoiningMargins.NONE);
        }
        // A box without lines, border, padding or height lets its margins merge (reference/components/block.md).
        boolean empty = first && lines.isEmpty() && slice.height() == 0 && model.openAtTop() && model.openAtBottom()
                && !box.isolatesMargins();
        return LayoutResult.finished(fragment, List.of(), empty ? AdjoiningMargins.EMPTY_BOX : AdjoiningMargins.NONE);
    }

    /**
     * The widows of a text cut beside a side box: the lines that move to the next page are broken again there at
     * the whole width, so they are fewer than the ones left here. If fewer than {@code Widows}, the cut moves up
     * while the lines that stay respect {@code Orphans}; if it cannot, it stays where {@link BreakRules#cutLines} put
     * it. A text whose lines differ only in its first line indent is not broken again: its cut stays.
     */
    private int widows(Box box, double available, OptionalDouble limit, Broken b, int end, int start, Style s) {
        if (b.widths() == null || !b.widths().shortened() || end >= b.lines().size()) {
            return end;
        }
        int cut = end;
        while (cut - start > s.typography().orphans() && remainingLines(box, available, limit, b, cut)
                < s.typography().widows()) {
            cut--;
        }
        return remainingLines(box, available, limit, b, cut) >= s.typography().widows() ? cut : end;
    }

    /** The lines the text from line {@code line} of {@code b} takes when broken again at the whole width. */
    private int remainingLines(Box box, double available, OptionalDouble limit, Broken b, int line) {
        return broken(box, available, limit, List.of(), b.lines().get(line).start()).lines().size();
    }

    /**
     * The line of the drop cap (reference/components/text.md): at the left of the first line box, with its baseline on
     * that of the last line it spans; its {@link TextLine#y()} is the top of a line of the usual height whose baseline,
     * at the size of the drop cap, falls there.
     */
    private TextLine dropCapLine(Style s, Broken b, double lineHeight) {
        DropCap d = b.dropCap();
        VerticalMetrics metrics = measurer().vertical(s.typography().fontFace());
        double baseline = metrics.baseline((s.dropCap() - 1) * lineHeight, lineHeight, b.fontSize());
        double y = baseline - metrics.baseline(0, lineHeight, d.size());
        double left = b.widths().left(0) - d.width();
        double width = d.width() - measurer().width(" ", s.typography().fontFace(), b.fontSize());
        return new TextLine(left, y, width, lineHeight, d.size(), d.letter(), 0,
                List.of(new TextLine.Run(0, width, d.letter(), d.format())), false, left, 0);
    }
}
