package dev.stxt.pdf.layout;

import dev.stxt.InlineNode;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.block.BlockLayout;
import dev.stxt.pdf.layout.stack.StackLayout;
import dev.stxt.pdf.layout.grid.GridLayout;
import dev.stxt.pdf.layout.image.ImageLayout;
import dev.stxt.pdf.layout.image.Images;
import dev.stxt.pdf.layout.image.NaturalSize;
import dev.stxt.pdf.layout.inline.FixedMetrics;
import dev.stxt.pdf.layout.inline.InlineLayout;
import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.layout.inline.hyphenation.Hyphenations;
import dev.stxt.pdf.layout.columns.ColumnsLayout;
import dev.stxt.pdf.layout.position.OverlayLayout;
import dev.stxt.pdf.layout.table.TableLayout;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.BoxTreeBuilder;
import dev.stxt.pdf.tree.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Layout engine and dispatcher of algorithms by {@code Layout}. All recursive layout calls go through here to use
 * the cache: the algorithms see it as their {@link LayoutContext}, without depending on the engine. A caller that
 * only wants to convert a document uses {@link dev.stxt.pdf.convert.Converter}, which creates and configures it.
 *
 * <p><strong>Threads: an {@code Engine} is not thread-safe and is not shared.</strong> It keeps the layout cache
 * (unsynchronized maps), the memoized line breaking and the assumed pages of the references
 * ({@link dev.stxt.pdf.layout.inline.InlineLayout}). Two conversions at once need one engine each, which is what
 * the {@code Converter} does; what can be shared is what does not change:
 * {@link Resources}, {@link dev.stxt.pdf.stxt.DocumentReader} and the already laid out {@link LaidOutDocument}.
 * It can be reused in sequence: {@link #layOutDocument(BoxTreeBuilder.Result)} empties the cache and the
 * state when it finishes, so that a stored engine does not retain the box tree of the last document.</p>
 *
 * <p>The protocol with the {@link Paginator} (reserved digits, assumed pages, numbering and out-of-flow boxes)
 * is not public: it is only used from this package.</p>
 */
public final class Engine implements LayoutContext {

    private final LayoutCache cache = new LayoutCache();
    private final TextMeasurer measurer;
    private final Images images;
    private final Limits limits;
    private final InlineLayout inline;
    private final TableLayout table;
    private final OverlayLayout overlays;
    /** The algorithm of each {@code Layout}; a table, which is a grid, has its own ({@link #algorithm}). */
    private final Map<Layout, LayoutAlgorithm> algorithms = new EnumMap<>(Layout.class);

    /**
     * Engine with {@link FixedMetrics}, so that the tests and the golden cases are deterministic and
     * computable by hand. It does not need the fonts folder; the images, if any, are those
     * of the default resources folder. The PDF is laid out with
     * {@code new Engine(new PdfMetrics(resources), resources)}.
     */
    public Engine() {
        this(new FixedMetrics());
    }

    /** Engine with those text metrics and the images of the default resources folder. */
    public Engine(TextMeasurer measurer) {
        this(measurer, new Resources());
    }

    /**
     * Engine with those text metrics and the images of {@code resources} (reference/components/image.md), with the
     * {@link Limits#DEFAULT}.
     */
    public Engine(TextMeasurer measurer, Resources resources) {
        this(measurer, resources, Limits.DEFAULT);
    }

    /** Engine with those metrics, those images and those {@link Limits} (reference/common/limits.md). */
    public Engine(TextMeasurer measurer, Resources resources, Limits limits) {
        this(measurer, resources, limits, Set.of());
    }

    /**
     * Engine with those metrics, those images and those {@link Limits}, without the checks in {@code excluded} that
     * are made during layout (those of the PDF images, {@link Check}).
     */
    public Engine(TextMeasurer measurer, Resources resources, Limits limits, Set<Check> excluded) {
        this.measurer = measurer;
        this.limits = limits;
        this.images = new Images(resources, limits, excluded);
        this.inline = new InlineLayout(this, measurer);
        GridLayout grid = new GridLayout(this);
        this.table = new TableLayout(this, grid);
        this.overlays = new OverlayLayout(this);
        algorithms.put(Layout.BLOCK, new BlockLayout(this));
        algorithms.put(Layout.INLINE, inline);
        algorithms.put(Layout.STACK, new StackLayout(this));
        algorithms.put(Layout.GRID, grid);
        algorithms.put(Layout.COLUMNS, new ColumnsLayout(this));
        algorithms.put(Layout.IMAGE, new ImageLayout(this));
    }

    /**
     * STXT document → pages, with the medium and the metadata. The usual way is to read it with
     * {@link dev.stxt.pdf.stxt.DocumentReader}; if it is not validated, {@link BoxTreeBuilder}
     * validates it against the schema. Before laying out it checks with the {@link TextMeasurer} that
     * all the text can be measured (the fonts, in the PDF output), that all the images exist and that every
     * hyphenated text is in a language with patterns ({@link Hyphenations#validate}),
     * and afterwards that nothing overflows ({@link Overflow}).
     */
    public LaidOutDocument layOutDocument(InlineNode document) {
        return layOutDocument(new BoxTreeBuilder().build(document));
    }

    /**
     * Like {@link #layOutDocument(InlineNode)}, with a separate styles document
     * ({@link dev.stxt.pdf.stxt.DocumentReader#readStyles}), whose rules weigh less than those of the
     * document itself (reference/common/styles.md). {@code styles} can be {@code null}.
     */
    public LaidOutDocument layOutDocument(InlineNode document, InlineNode styles) {
        return layOutDocument(new BoxTreeBuilder().build(document, styles));
    }

    /**
     * Like {@link #layOutDocument(InlineNode)}, with the tree already built. It serves to adjust the tree before
     * laying it out (this is how the tests put formatted runs in a text, reference/common/text-formatting.md); the
     * tree cannot be changed afterwards.
     */
    public LaidOutDocument layOutDocument(BoxTreeBuilder.Result tree) {
        measurer.validate(tree.root());
        images.validate(tree.root());
        Hyphenations.validate(tree.root());
        release();
        try {
            List<Page> pages = new Paginator(this).paginate(tree);
            Overflow.check(pages);
            return new LaidOutDocument(tree.pageSetup(), tree.metadata(), tree.root(), pages);
        } finally {
            release();
        }
    }

    /**
     * Releases what the engine memoizes of a document: the cache, the line breaking, the reserved digits and the
     * assumed pages.
     */
    private void release() {
        cache.clear();
        inline.reset();
    }

    /** The limits of this engine, which the {@link Paginator} applies to the number of pages. */
    Limits limits() {
        return limits;
    }

    /** Layouts this engine has computed since it was created, to measure what the cache saves. */
    int cacheMisses() {
        return cache.misses();
    }

    /** Entries of the cache right now, to check that {@link #release} empties it. */
    int cachedEntries() {
        return cache.entries();
    }

    /**
     * Digits that {@code {page}} and {@code {pages}} reserve during layout ({@link PageNumbers}). The measurements
     * of {@code changed}, the texts with those markers, change: what the cache stores of them and of their
     * ancestors is forgotten ({@link #forget}).
     */
    void reservePageNumbers(String probe, Collection<Box> changed) {
        inline.reservePageNumbers(probe);
        forget(changed);
    }

    /**
     * What the references to other boxes in the texts are worth (reference/common/references.md): the page of each
     * box referred to with {@code {page of: id}}, which the {@link Paginator} assumes and checks, and its number with
     * {@code {number of: id}}. The measurements of {@code changed}, the texts whose references are worth something
     * different from the previous pass, change: what the cache stores of them and of their ancestors is forgotten.
     */
    void assume(Map<String, String> pages, Map<String, String> numbers, Collection<Box> changed) {
        inline.assume(pages, numbers);
        forget(changed);
    }

    /**
     * The running heads of the page whose {@code Page overlay} boxes are going to be laid out
     * ({@code {heading N}}, reference/common/references.md): the heading of each level in effect on it, by the id of
     * the reference. The {@link Paginator} sets them before each page whose values differ from the previous one; the
     * measurements of {@code changed}, the texts with those references, change, and what the cache stores of them and
     * of their ancestors is forgotten.
     */
    void headings(Map<String, String> headings, Collection<Box> changed) {
        inline.headings(headings);
        forget(changed);
    }

    /**
     * Forgets what the cache and the line breaking store of those texts and of their ancestors, whose layout depends
     * on what they measure. The rest is still valid: the result of a box only depends on its subtree, so
     * the body of a document is not laid out again because the number in its footer changes.
     */
    private void forget(Collection<Box> changed) {
        Set<Box> stale = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Box box : changed) {
            for (Box b = box; b != null && stale.add(b); b = b.parent()) {
                // Goes up to the root, or to an ancestor already noted.
            }
        }
        if (!stale.isEmpty()) {
            cache.forget(stale);
            inline.forget(stale);
        }
    }

    @Override
    public TextMeasurer measurer() {
        return measurer;
    }

    @Override
    public NaturalSize natural(Box box) {
        return images.natural(box);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A rotated box in flow contributes the width of the rectangle that encloses it, laid out with the width it
     * would have without a container: its {@code Width} or, if it has none, its natural width
     * (reference/common/layers-and-rotation.md). Percentages count as in any box ({@link BoxModel#intrinsic}).</p>
     */
    @Override
    public IntrinsicSizes intrinsic(Box box) {
        return cache.intrinsic(box, () -> {
            IntrinsicSizes own = BoxModel.intrinsic(box, contentIntrinsic(box));
            if (!box.style().rotatesInFlow()) {
                return own;
            }
            double margins = Insets.resolve(box.style().box().margin(), 0).horizontal();
            ConstraintSpace space = ConstraintSpace.fixed(own.natural() - margins, OptionalDouble.empty(), 0,
                    OptionalDouble.empty(), false);
            double width = turned(box, layoutOnce(box, space, null).fragment()).width() + margins;
            return new IntrinsicSizes(width, width);
        });
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        return cache.contentIntrinsic(box, () -> algorithm(box).contentIntrinsic(box));
    }

    /**
     * Layout of a box with its algorithm. If the box is the reference container of {@code Overlay} boxes
     * inside it, it resolves them when it finishes (reference/components/frame-and-overlays.md).
     *
     * <p>If the box ends on this page and only its bottom border and padding stick out of it, the layout is
     * repeated with that space reserved (reference/common/breaks.md): the box ends inside the page or is broken
     * before, and the end goes to the next one with the border and the padding.</p>
     *
     * <p>A box with {@code Rotate} is not broken, like an image: it is laid out without fragmentation and,
     * if it does not fit in what is left of the page, it returns {@code BEFORE}; at the start of a page it is
     * placed even if it does not fit, and {@link Overflow} reports the overflow.</p>
     */
    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        if (box.style().rotatesInFlow()) {
            return rotatedInFlow(box, space);
        }
        if (space.fragmenting() && box.style().rotation() != null) {
            // A rotated box is not broken (reference/common/breaks.md): whole or, if it does not fit, on the next page.
            LayoutResult whole = layoutOnce(box, space.unfragmented(), from);
            if (!space.atFragmentainerStart()
                    && !fits(whole.fragment().height(), space.fragmentainerRemaining().getAsDouble())) {
                return LayoutResult.before(box);
            }
            return whole;
        }
        LayoutResult r = layoutOnce(box, space, from);
        if (!space.fragmenting() || r.breakToken() != null) {
            return r;
        }
        Fragment f = r.fragment();
        double bottom = f.border().bottom() + f.padding().bottom();
        double remaining = space.fragmentainerRemaining().getAsDouble();
        if (bottom > 0 && !fits(f.height(), remaining) && fits(f.height() - bottom, remaining)) {
            return layoutOnce(box, space.withRemaining(remaining - bottom, space.atFragmentainerStart()), from);
        }
        return r;
    }

    /**
     * A rotated box in flow (reference/common/layers-and-rotation.md): it is laid out unrotated with its width, which
     * does not depend on the container ({@link #turnedWidth}), and takes up the rectangle that encloses its border
     * box rotated about its center. It is not broken: whole or, if it does not fit in what is left of the page, on
     * the next one. It isolates margins: those of its children do not come out of it.
     */
    private LayoutResult rotatedInFlow(Box box, ConstraintSpace space) {
        ConstraintSpace own = ConstraintSpace.fixed(turnedWidth(box, space), OptionalDouble.empty(),
                space.percentBaseWidth(), space.percentBaseHeight(), space.atFragmentainerStart());
        LayoutResult r = layoutOnce(box, own, null);
        Fragment f = turned(box, r.fragment());
        if (space.fragmenting() && !space.atFragmentainerStart()
                && !fits(f.height(), space.fragmentainerRemaining().getAsDouble())) {
            return LayoutResult.before(box);
        }
        // What comes up from inside (the Page overlay) has its static position in the unrotated box.
        List<PendingOverlay> pending = r.pending().stream()
                .map(p -> p.translated(f.turned().x(), f.turned().y())).toList();
        return LayoutResult.finished(f, pending, AdjoiningMargins.NONE);
    }

    /**
     * The border box width of a rotated box in flow: the one given by its {@code Width} or, if it is {@code auto},
     * its natural width, without stretching or breaking its lines at the width of the container; an {@code Image},
     * its usual one.
     */
    private double turnedWidth(Box box, ConstraintSpace space) {
        if (box.isImage() || !box.style().box().width().isAuto()) {
            return BoxModel.resolve(box, ConstraintSpace.free(space.availableWidth(), space.percentBaseWidth(),
                    space.percentBaseHeight(), space.atFragmentainerStart()), this).width();
        }
        Insets border = Insets.resolve(box.style().box().border(), space.percentBaseWidth());
        Insets padding = Insets.resolve(box.style().box().padding(), space.percentBaseWidth());
        return BoxModel.constrainWidth(box.style(),
                contentIntrinsic(box).natural() + border.horizontal() + padding.horizontal(),
                space.percentBaseWidth());
    }

    /** The unrotated fragment of a rotated box in flow, with the rectangle that encloses it rotated about its center.
     */
    private static Fragment turned(Box box, Fragment f) {
        Rect border = new Rect(0, 0, f.width(), f.height());
        Rotation rotation = box.style().rotation();
        Rect envelope = Rotation.envelope(rotation.transform(border), border);
        return f.turned(new Rect(-envelope.x(), -envelope.y(), f.width(), f.height()), envelope.width(),
                envelope.height());
    }

    private LayoutResult layoutOnce(Box box, ConstraintSpace space, BreakToken from) {
        return cache.layout(box, space, from,
                () -> overlays.resolvePending(box, algorithm(box).layout(box, space, from)));
    }

    /**
     * Replaces {@code {page}} and {@code {pages}} in the lines of an already laid out fragment and in
     * those of its descendants; the {@link Paginator} uses it when it finishes paginating.
     */
    Fragment number(Fragment fragment, String page, String pages) {
        return inline.number(fragment, page, pages);
    }

    /** Layout of the out-of-flow boxes; the {@link Paginator} uses it for the {@code Page overlay} ones. */
    OverlayLayout overlays() {
        return overlays;
    }

    /** The algorithm of the box: that of its {@code Layout} or, if it is a table, that of tables. */
    private LayoutAlgorithm algorithm(Box box) {
        // A table is a grid that repeats its header and footer on each page (reference/components/table.md).
        return box.element() == Element.TABLE ? table : algorithms.get(box.style().layout());
    }

    @Override
    public String toString() {
        return "Engine[" + cache.stats() + "]";
    }
}
