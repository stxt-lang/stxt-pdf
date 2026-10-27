package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.BreakAppeal;
import dev.stxt.boxes.fragment.BreakRules;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.Slice;
import dev.stxt.boxes.model.TextRun;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout {@code inline}: texto repartido en line boxes (ESPECIFICACION.md §5.4).
 *
 * <p>Pasos: resolver el ancho de contenido, partir con {@link LineBreaker}, alinear cada línea,
 * altura = número de líneas × {@code Line height}. Con {@code justify}, el sobrante de cada línea
 * se reparte entre sus espacios ({@link TextLine#wordSpacing()}), salvo en la última de cada
 * párrafo; una línea cortada por un salto de página no es la última de su párrafo.</p>
 *
 * <p>En cada página (§5.10) se colocan las líneas que caben desde la del {@link BreakToken.Line}
 * respetando viudas y huérfanas en cada párrafo ({@link BreakRules#cutLines}); si no cabe
 * ninguna, se devuelve {@link BreakToken#BEFORE} para que el padre corte antes de la caja.</p>
 *
 * <p>{@code {page}} y {@code {pages}} se miden como un número de prueba y se sustituyen al
 * terminar la paginación con {@link #number} ({@link PageNumbers}).</p>
 *
 * <p>El reparto en líneas de un texto se hace una sola vez por ancho disponible y se reutiliza en
 * cada página: cada fragmento empieza en otra línea ({@code from}), así que la caché del
 * {@link Engine} no ayuda, y sin él un texto de N páginas se partía N veces (coste cuadrático).</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    /** Un texto repartido en líneas: todas, y dónde acaba cada párrafo (índice tras su última línea). */
    private record Broken(List<LineBreaker.Line> lines, int[] paragraphEnds) {
    }

    private record BrokenKey(Box box, double available) {
    }

    private final Engine engine;
    /** Mide {@code {page}} y {@code {pages}} como el número de prueba ({@link PageNumbers}). */
    private TextMeasurer measurer;
    private final TextMeasurer real;
    /** Reparto en líneas de cada texto por ancho disponible; depende de {@link #measurer}. */
    private final Map<BrokenKey, Broken> broken = new HashMap<>();

    public InlineLayout(Engine engine, TextMeasurer measurer) {
        this.engine = engine;
        this.real = measurer;
        reservePageDigits(PageNumbers.DIGITS);
    }

    /**
     * Cifras del número de prueba de {@code {page}} y {@code {pages}}. Cambia las medidas: hay
     * que vaciar la caché antes de volver a maquetar ({@link Engine}); el reparto en líneas
     * memorizado se vacía aquí.
     */
    public void reservePageDigits(int digits) {
        this.measurer = PageNumbers.probing(real, digits);
        broken.clear();
    }

    /**
     * Segunda fase de la numeración ({@link PageNumbers}): el fragmento y sus descendientes con
     * los valores reales de {@code {page}} y {@code {pages}} en sus líneas. Cada línea con un
     * marcador vuelve a medirse y a alinearse; con {@code justify}, solo se justifica si ya lo
     * estaba. Devuelve el mismo fragmento si no cambia nada.
     */
    public Fragment number(Fragment f, int page, int pages) {
        List<Fragment> children = new ArrayList<>();
        boolean changed = false;
        for (Fragment child : f.children()) {
            Fragment numbered = number(child, page, pages);
            changed |= numbered != child;
            children.add(numbered);
        }
        List<TextLine> lines = new ArrayList<>();
        Style s = f.box().style;
        double available = f.contentWidth();
        for (TextLine l : f.lines()) {
            if (!PageNumbers.contains(l.text())) {
                lines.add(l);
                continue;
            }
            changed = true;
            List<TextRun> runs = l.runs().stream()
                    .map(r -> new TextRun(PageNumbers.apply(r.text(), page, pages), r.format())).toList();
            lines.add(LineBreaker.place(runs, real, s, available, l.wordSpacing() > 0, l.y(), l.height()));
        }
        if (!changed) {
            return f;
        }
        return new Fragment(f.box(), f.x(), f.y(), f.width(), f.height(), f.margin(), f.border(), f.padding(),
                children, lines, f.first(), f.last(), f.offsetX(), f.offsetY());
    }

    private Broken breakText(Box box, double available) {
        Style s = box.style;
        List<List<LineBreaker.Line>> paragraphs = box.hasText()
                ? LineBreaker.breakParagraphs(box.runs(), available, measurer, s.fontFace(), s.fontSize, s.textWrap)
                : List.of();
        List<LineBreaker.Line> texts = new ArrayList<>();
        int[] paragraphEnds = new int[paragraphs.size()];
        for (int p = 0; p < paragraphs.size(); p++) {
            texts.addAll(paragraphs.get(p));
            paragraphEnds[p] = texts.size();
        }
        return new Broken(List.copyOf(texts), paragraphEnds);
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        IntrinsicSizes content = box.hasText()
                ? LineBreaker.intrinsics(box.runs(), measurer, box.style.fontFace(), box.style.fontSize)
                : IntrinsicSizes.ZERO;
        return content;
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, () -> engine.intrinsic(box));
        Style s = box.style;
        double available = model.contentWidth();
        double lineHeight = s.usedLineHeight();
        boolean first = from == null;
        int start = first ? 0 : ((BreakToken.Line) from).nextLine();

        Broken b = broken.computeIfAbsent(new BrokenKey(box, available), k -> breakText(box, available));
        List<LineBreaker.Line> texts = b.lines();
        int[] paragraphEnds = b.paragraphEnds();

        int end = texts.size();
        int appeal = BreakAppeal.PERFECT_BREAK;
        if (space.fragmenting()) {
            double remaining = Slice.contentRemaining(model, first, space);
            BreakRules.Cut cut = BreakRules.cutLines(paragraphEnds, start, BreakRules.capacity(remaining, lineHeight),
                    s.widows, s.orphans, space.atFragmentainerStart());
            if (cut.end() == start && start < texts.size()) {
                return LayoutResult.before(box);
            }
            end = cut.end();
            appeal = cut.appeal();
        }

        List<TextLine> lines = new ArrayList<>();
        int paragraph = 0;
        for (int j = start; j < end; j++) {
            while (paragraphEnds[paragraph] <= j) {
                paragraph++;
            }
            boolean justify = j != paragraphEnds[paragraph] - 1;
            lines.add(LineBreaker.place(texts.get(j).runs(), measurer, s, available, justify, lines.size() * lineHeight,
                    lineHeight));
        }

        Slice slice = Slice.of(model, first, BreakToken.consumed(from), lines.size() * lineHeight,
                end == texts.size(), space);
        Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(), slice.border(),
                slice.padding(), List.of(), lines, first, slice.finished());
        if (!slice.finished()) {
            // Faltan líneas o, si han terminado, el alto propio de la caja.
            return new LayoutResult(fragment, new BreakToken.Line(end, slice.consumed()),
                    end == texts.size() ? BreakAppeal.PERFECT_BREAK : appeal, List.of(), AdjoiningMargins.NONE);
        }
        // Una caja sin líneas, borde, relleno ni alto deja colapsar sus márgenes (§5.3).
        boolean empty = first && lines.isEmpty() && slice.height() == 0 && model.openAtTop() && model.openAtBottom()
                && !box.establishesBlockFormattingContext();
        return LayoutResult.finished(fragment, List.of(), empty ? AdjoiningMargins.EMPTY_BOX : AdjoiningMargins.NONE);
    }
}
