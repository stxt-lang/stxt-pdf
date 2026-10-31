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
 * Layout {@code inline}: texto repartido en line boxes (reference/common/text-formatting.md).
 *
 * <p>Pasos: resolver el ancho de contenido, partir con {@link LineBreaker}, alinear cada línea,
 * altura = número de líneas × {@code Line height}. Con {@code justify}, el sobrante de cada línea
 * se reparte entre sus espacios ({@link TextLine#wordSpacing()}), salvo en la última del texto,
 * que es un solo párrafo; una línea cortada por un salto de página no es la última.</p>
 *
 * <p>En cada página (reference/common/breaks.md) se colocan las líneas que caben desde la del {@link BreakToken.Line}
 * respetando viudas y huérfanas ({@link BreakRules#cutLines}); si no cabe ninguna, se devuelve
 * {@link BreakToken#BEFORE} para que el padre corte antes de la caja.</p>
 *
 * <p>{@code {page}} y {@code {pages}} se miden como un número de prueba y se sustituyen al
 * terminar la paginación con {@link #number} ({@link PageNumbers}).</p>
 *
 * <p>El reparto en líneas de un texto se hace una sola vez por ancho disponible y se reutiliza en
 * cada página: cada fragmento empieza en otra línea ({@code from}), así que la caché del
 * {@link LayoutContext} no ayuda, y sin él un texto de N páginas se partía N veces (coste cuadrático).</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    private record BrokenKey(Box box, double available) {
    }

    private final LayoutContext context;
    /** Mide {@code {page}} y {@code {pages}} como el número de prueba ({@link PageNumbers}). */
    private TextMeasurer measurer;
    private final TextMeasurer real;
    /** Reparto en líneas de cada texto por ancho disponible; depende de {@link #measurer}. */
    private final Map<BrokenKey, List<LineBreaker.Line>> broken = new HashMap<>();
    /**
     * Página que se supone a cada caja referida con {@code {page of: id}} (reference/common/references.md); ver {@link
     * #assume}.
     */
    private Map<String, String> pages = Map.of();
    /** Número de cada caja referida con {@code {number of: id}}. */
    private Map<String, String> numbers = Map.of();

    public InlineLayout(LayoutContext context, TextMeasurer measurer) {
        this.context = context;
        this.real = measurer;
        reset();
    }

    /** Vuelve al estado de antes de maquetar: las cifras de prueba por defecto, sin referencias ni reparto memorizado. */
    public void reset() {
        this.measurer = PageNumbers.probing(real, PageNumbers.firstProbe());
        this.pages = Map.of();
        this.numbers = Map.of();
        broken.clear();
    }

    /**
     * El número de prueba de {@code {page}} y {@code {pages}} ({@link PageNumbers}). Cambia lo que miden los textos con
     * esos marcadores: quien llama olvida su reparto memorizado ({@link #forget}) y lo que guarda de ellos la caché del
     * {@link LayoutContext}.
     */
    public void reservePageNumbers(String probe) {
        this.measurer = PageNumbers.probing(real, probe);
    }

    /**
     * Lo que valen las referencias a otras cajas del texto ({@code {page of: id}} y {@code {number of: id}},
     * reference/common/references.md): se sustituyen antes de repartir el texto en líneas, así que el texto mide lo
     * que va a pintarse. Las páginas son una suposición que el {@link dev.stxt.pdf.layout.Paginator} repite hasta que
     * se cumple. Como con {@link #reservePageNumbers}, quien llama olvida los textos cuyo valor cambia.
     */
    public void assume(Map<String, String> pages, Map<String, String> numbers) {
        this.pages = Map.copyOf(pages);
        this.numbers = Map.copyOf(numbers);
    }

    /** Olvida el reparto en líneas memorizado de esas cajas. */
    public void forget(Set<Box> boxes) {
        broken.keySet().removeIf(k -> boxes.contains(k.box()));
    }

    /**
     * Los tramos del texto de la caja, con sus referencias a otras cajas ya sustituidas y sus
     * mayúsculas transformadas ({@link TextTransform}).
     */
    private List<TextRun> runs(Box box) {
        List<TextRun> runs = box.runs();
        if (Reference.contains(box.text())) {
            runs = runs.stream().map(r -> new TextRun(Reference.replace(r.text(), this::referenced), r.format()))
                    .toList();
        }
        return TextTransform.apply(runs, box.style());
    }

    private String referenced(Reference.Kind kind, String id) {
        return kind == Reference.Kind.PAGE ? pages.get(id) : numbers.get(id);
    }

    /**
     * Segunda fase de la numeración ({@link PageNumbers}): el fragmento y sus descendientes con
     * los valores reales de {@code {page}} y {@code {pages}} en sus líneas. Cada línea con un
     * marcador vuelve a medirse y a alinearse; con {@code justify}, solo se justifica si ya lo
     * estaba. Devuelve el mismo fragmento si no cambia nada.
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
            lines.add(LineBreaker.place(runs, real, s, available, l.wordSpacing() > 0, l.y(), l.height()));
        }
        if (!changed) {
            return f;
        }
        return new Fragment(f.box(), f.x(), f.y(), f.width(), f.height(), f.margin(), f.border(), f.padding(),
                children, lines, f.first(), f.last(), f.offsetX(), f.offsetY(), f.turned());
    }

    /** Las líneas del texto de la caja, que es un solo párrafo (en uno preformateado, las líneas en blanco no lo parten). */
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
                s.typography().fontSize(), s.typography().letterSpacing(), s.typography().lineBreaking()));
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
        return LineBreaker.intrinsics(runs(box), measurer, s.typography().fontFace(), s.typography().fontSize(), s.typography().letterSpacing());
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
            // Las líneas preformateadas no se justifican.
            boolean justify = !s.typography().preformatted().enabled() && j != texts.size() - 1;
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
        // Una caja sin líneas, borde, relleno ni alto deja fusionar sus márgenes (reference/components/block.md).
        boolean empty = first && lines.isEmpty() && slice.height() == 0 && model.openAtTop() && model.openAtBottom()
                && !box.isolatesMargins();
        return LayoutResult.finished(fragment, List.of(), empty ? AdjoiningMargins.EMPTY_BOX : AdjoiningMargins.NONE);
    }
}
