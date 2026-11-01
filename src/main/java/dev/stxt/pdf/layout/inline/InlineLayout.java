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
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Preformatted;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * pagination ends with {@link #number} ({@link PageNumbers}).</p>
 *
 * <p>The line breaking of a text is done only once per available width and reused on
 * each page: each fragment starts at another line ({@code from}), so the cache of the
 * {@link LayoutContext} does not help, and without it a text of N pages was broken N times (quadratic cost).</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    private record BrokenKey(Box box, double available) {
    }

    private final LayoutContext context;
    /** Measures {@code {page}} and {@code {pages}} as the probe number ({@link PageNumbers}). */
    private TextMeasurer measurer;
    private final TextMeasurer real;
    /** Line breaking of each text per available width; depends on {@link #measurer}. */
    private final Map<BrokenKey, List<LineBreaker.Line>> broken = new HashMap<>();
    /**
     * Page assumed for each box referred to with {@code {page of: id}} (reference/common/references.md); see {@link
     * #assume}.
     */
    private Map<String, String> pages = Map.of();
    /** Number of each box referred to with {@code {number of: id}}. */
    private Map<String, String> numbers = Map.of();

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
        return kind == Reference.Kind.PAGE ? pages.get(id) : numbers.get(id);
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
            lines.add(LineBreaker.place(runs, real, s, available, l.wordSpacing() > 0, l.y(), l.height(),
                    l.hyphenated()));
        }
        if (!changed) {
            return f;
        }
        return new Fragment(f.box(), f.x(), f.y(), f.width(), f.height(), f.margin(), f.border(), f.padding(),
                children, lines, f.first(), f.last(), f.offsetX(), f.offsetY(), f.turned());
    }

    /** The lines of the text of the box, a single paragraph (in a preformatted one, blank lines do not split it). */
    private List<LineBreaker.Line> breakText(Box box, double available) {
        Style s = box.style();
        if (!box.hasText()) {
            return List.of();
        }
        if (s.typography().preformatted().enabled()) {
            return List.copyOf(PreformattedText.lines(runs(box), s.typography().tabSize(), s.typography().preformatted() == Preformatted.WRAP,
                    available, measurer, s.typography().fontFace(), s.typography().fontSize(), s.typography().letterSpacing()));
        }
        return List.copyOf(LineBreaker.breakLines(runs(box), available, measurer, s.typography().fontFace(),
                s.typography().fontSize(), s.typography().letterSpacing(), s.typography().lineBreaking(), hyphenator(s)));
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
        double lineHeight = s.typography().usedLineHeight();
        boolean first = from == null;
        int start = first ? 0 : ((BreakToken.Line) from).nextLine();

        List<LineBreaker.Line> texts = broken.computeIfAbsent(new BrokenKey(box, available), k -> breakText(box, available));

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
            lines.add(LineBreaker.place(texts.get(j).runs(), measurer, s, available, justify, lines.size() * lineHeight,
                    lineHeight, texts.get(j).hyphenated()));
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
