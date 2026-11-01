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
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Preformatted;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

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
 * <p>{@code {page}} and {@code {pages}} are measured as a probe number and replaced when
 * pagination ends with {@link #number} ({@link PageNumbers}). The references to other boxes and the running heads
 * ({@link Reference}) are replaced before breaking the text into lines, with the values the
 * {@link dev.stxt.pdf.layout.Paginator} gives ({@link #assume}, {@link #headings}).</p>
 *
 * <p>With {@code Overflow: shrink} (reference/common/overflow.md) the text is broken at the {@code Size} of the box
 * and, if some line is wider than the box or the lines are taller than its definite height, again at smaller sizes,
 * from {@code Size} down in steps of {@link #SHRINK_STEP} and {@code Min size} as the last one, until it fits
 * ({@link #shrink}). The lines keep the size they were broken at ({@link TextLine#fontSize()}) and the line height
 * follows it if it is a percentage.</p>
 *
 * <p>The line breaking of a text is done only once per available width (and height limit, if it shrinks) and reused
 * on each page: each fragment starts at another line ({@code from}), so the cache of the
 * {@link LayoutContext} does not help, and without it a text of N pages was broken N times (quadratic cost).</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    /** How much {@code Overflow: shrink} reduces the {@code Size} at each step, in points. */
    public static final double SHRINK_STEP = 0.5;

    /**
     * @param limit height of the content box the lines have to fit in, if the text shrinks and the height is definite
     *              or has a maximum; empty otherwise (it is not part of the key of a text that does not shrink)
     */
    private record BrokenKey(Box box, double available, OptionalDouble limit) {
    }

    /** The lines of a text and the size they are broken at: the {@code Size} of the box, or the one it shrank to. */
    private record Broken(List<LineBreaker.Line> lines, double fontSize) {
    }

    private final LayoutContext context;
    /** Measures {@code {page}} and {@code {pages}} as the probe number ({@link PageNumbers}). */
    private TextMeasurer measurer;
    private final TextMeasurer real;
    /** Line breaking of each text per available width (and height limit); depends on {@link #measurer}. */
    private final Map<BrokenKey, Broken> broken = new HashMap<>();
    /**
     * Page assumed for each box referred to with {@code {page of: id}} (reference/common/references.md); see {@link
     * #assume}.
     */
    private Map<String, String> pages = Map.of();
    /** Number of each box referred to with {@code {number of: id}}. */
    private Map<String, String> numbers = Map.of();
    /**
     * The running heads of the page being laid out, by the id of their reference ({@code "2"}, {@code "2: first"}):
     * the text of the heading in effect on the page, or empty; see {@link #headings}.
     */
    private Map<String, String> headings = Map.of();

    public InlineLayout(LayoutContext context, TextMeasurer measurer) {
        this.context = context;
        this.real = measurer;
        reset();
    }

    /** Returns to the state before the layout: the default probe digits, no references, no memorized line breaking. */
    public void reset() {
        this.measurer = PageNumbers.probing(real, PageNumbers.firstProbe());
        this.pages = Map.of();
        this.numbers = Map.of();
        this.headings = Map.of();
        broken.clear();
    }

    /**
     * The probe number of {@code {page}} and {@code {pages}} ({@link PageNumbers}). It changes what the texts with
     * those markers measure: the caller forgets their memorized line breaking ({@link #forget}) and what the cache of
     * the {@link LayoutContext} keeps of them.
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

    /** Forgets the memorized line breaking of those boxes. */
    public void forget(Set<Box> boxes) {
        broken.keySet().removeIf(k -> boxes.contains(k.box()));
    }

    /**
     * The runs of the text of the box, with their references to other boxes already replaced and their
     * case transformed ({@link TextTransform}). In a preformatted text the soft hyphens are removed: its lines break
     * only where the text does (reference/common/text-formatting.md).
     */
    private List<TextRun> runs(Box box) {
        List<TextRun> runs = box.runs();
        if (Reference.contains(box.text())) {
            runs = runs.stream().map(r -> new TextRun(Reference.replace(r.text(), this::referenced), r.format()))
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

    private String referenced(Reference.Kind kind, String id) {
        return switch (kind) {
            case PAGE -> pages.get(id);
            case NUMBER -> numbers.get(id);
            case HEADING -> headings.get(id);
        };
    }

    /**
     * Second phase of the numbering ({@link PageNumbers}): the fragment and its descendants with
     * the real values of {@code {page}} and {@code {pages}} in their lines. Each line with a
     * marker is measured and aligned again; with {@code justify}, it is only justified if it already
     * was. Returns the same fragment if nothing changes.
     */
    public Fragment number(Fragment f, String page, String pages) {
        List<Fragment> children = new ArrayList<>();
        boolean changed = false;
        for (Fragment child : f.children()) {
            Fragment numbered = number(child, page, pages);
            changed |= numbered != child;
            children.add(numbered);
        }
        List<TextLine> lines = new ArrayList<>();
        Style s = f.box().style();
        double available = f.contentWidth();
        for (TextLine l : f.lines()) {
            if (!PageNumbers.contains(l.text())) {
                lines.add(l);
                continue;
            }
            changed = true;
            List<TextRun> runs = l.runs().stream()
                    .map(r -> new TextRun(PageNumbers.apply(r.text(), page, pages), r.format())).toList();
            lines.add(LineBreaker.place(runs, real, s, l.fontSize(), available, l.wordSpacing() > 0, l.y(),
                    l.height(), l.hyphenated()));
        }
        if (!changed) {
            return f;
        }
        return new Fragment(f.box(), f.x(), f.y(), f.width(), f.height(), f.margin(), f.border(), f.padding(),
                children, lines, f.first(), f.last(), f.offsetX(), f.offsetY(), f.turned());
    }

    /**
     * The lines of the text of the box at {@code fontSize}, a single paragraph (in a preformatted one, blank lines do
     * not split it).
     */
    private List<LineBreaker.Line> breakText(Box box, double available, double fontSize) {
        Style s = box.style();
        if (!box.hasText()) {
            return List.of();
        }
        if (s.typography().preformatted().enabled()) {
            return List.copyOf(PreformattedText.lines(runs(box), s.typography().tabSize(), s.typography().preformatted() == Preformatted.WRAP,
                    available, measurer, s.typography().fontFace(), fontSize, s.typography().letterSpacing()));
        }
        return List.copyOf(LineBreaker.breakLines(runs(box), available, measurer, s.typography().fontFace(),
                fontSize, s.typography().letterSpacing(), s.typography().lineBreaking(), hyphenator(s)));
    }

    /**
     * The lines of the text at the {@code Size} of the box or, with {@code Overflow: shrink}
     * (reference/common/overflow.md), at the largest size, from {@code Size} down in steps of {@link #SHRINK_STEP} and
     * {@code Min size} as the last one, at which no line is wider than {@code available} and, with a {@code limit},
     * the lines are not taller than it. If none fits, those of {@code Min size}: {@link dev.stxt.pdf.layout.Overflow}
     * reports what still sticks out.
     */
    private Broken shrink(Box box, double available, OptionalDouble limit) {
        Style s = box.style();
        double size = s.typography().fontSize();
        List<LineBreaker.Line> lines = breakText(box, available, size);
        if (!s.box().shrinks()) {
            return new Broken(lines, size);
        }
        double min = s.typography().minFontSize();
        while (!fits(lines, s, size, available, limit) && size > min + Tolerance.EPSILON) {
            size = Math.max(min, size - SHRINK_STEP);
            lines = breakText(box, available, size);
        }
        return new Broken(lines, size);
    }

    /**
     * Whether the lines at {@code size} fit: none is wider than {@code available}, with the tolerance of
     * {@link dev.stxt.pdf.layout.Overflow} ({@link Tolerance#MODEL}), and together they are not taller than
     * {@code limit}.
     */
    private boolean fits(List<LineBreaker.Line> lines, Style s, double size, double available, OptionalDouble limit) {
        FontFace font = s.typography().fontFace();
        for (LineBreaker.Line l : lines) {
            if (LineBreaker.width(l.runs(), measurer, font, size, s.typography().letterSpacing()) > available + Tolerance.MODEL) {
                return false;
            }
        }
        return limit.isEmpty() || lines.size() * s.typography().usedLineHeight(size) <= limit.getAsDouble() + Tolerance.MODEL;
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

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        Style s = box.style();
        if (!box.hasText()) {
            return IntrinsicSizes.ZERO;
        }
        if (s.typography().preformatted().enabled()) {
            return PreformattedText.intrinsics(runs(box), s.typography().tabSize(), s.typography().preformatted() == Preformatted.WRAP,
                    measurer, s.typography().fontFace(), s.typography().fontSize(), s.typography().letterSpacing());
        }
        return LineBreaker.intrinsics(runs(box), measurer, s.typography().fontFace(), s.typography().fontSize(),
                s.typography().letterSpacing(), hyphenator(s));
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, context);
        Style s = box.style();
        double available = model.contentWidth();
        boolean first = from == null;
        int start = first ? 0 : ((BreakToken.Line) from).nextLine();

        OptionalDouble limit = limit(box, model);
        Broken b = broken.computeIfAbsent(new BrokenKey(box, available, limit), k -> shrink(box, available, limit));
        List<LineBreaker.Line> texts = b.lines();
        double fontSize = b.fontSize();
        double lineHeight = s.typography().usedLineHeight(fontSize);

        int end = texts.size();
        int appeal = BreakAppeal.PERFECT_BREAK;
        if (space.fragmenting()) {
            double remaining = Slice.contentRemaining(model, first, space);
            BreakRules.Cut cut = BreakRules.cutLines(texts.size(), start, BreakRules.capacity(remaining, lineHeight),
                    s.typography().widows(), s.typography().orphans(), space.atFragmentainerStart());
            if (cut.end() == start && start < texts.size()) {
                return LayoutResult.before(box);
            }
            end = cut.end();
            appeal = cut.appeal();
        }

        List<TextLine> lines = new ArrayList<>();
        for (int j = start; j < end; j++) {
            // Preformatted lines are not justified.
            boolean justify = !s.typography().preformatted().enabled() && j != texts.size() - 1;
            lines.add(LineBreaker.place(texts.get(j).runs(), measurer, s, fontSize, available, justify,
                    lines.size() * lineHeight, lineHeight, texts.get(j).hyphenated()));
        }

        Slice slice = Slice.of(model, first, BreakToken.consumed(from), lines.size() * lineHeight,
                end == texts.size(), space);
        Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(), slice.border(),
                slice.padding(), List.of(), lines, first, slice.finished());
        if (!slice.finished()) {
            // Lines are missing or, if they have ended, the box's own height.
            return new LayoutResult(fragment, new BreakToken.Line(end, slice.consumed()),
                    end == texts.size() ? BreakAppeal.PERFECT_BREAK : appeal, List.of(), AdjoiningMargins.NONE);
        }
        // A box without lines, border, padding or height lets its margins merge (reference/components/block.md).
        boolean empty = first && lines.isEmpty() && slice.height() == 0 && model.openAtTop() && model.openAtBottom()
                && !box.isolatesMargins();
        return LayoutResult.finished(fragment, List.of(), empty ? AdjoiningMargins.EMPTY_BOX : AdjoiningMargins.NONE);
    }
}
