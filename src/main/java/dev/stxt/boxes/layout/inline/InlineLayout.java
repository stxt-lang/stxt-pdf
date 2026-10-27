package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.BreakAppeal;
import dev.stxt.boxes.fragment.BreakRules;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.Slice;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;

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
 */
public final class InlineLayout implements LayoutAlgorithm {

    /** Mide {@code {page}} y {@code {pages}} como el número de prueba ({@link PageNumbers}). */
    private TextMeasurer measurer;
    private final TextMeasurer real;

    public InlineLayout(TextMeasurer measurer) {
        this.real = measurer;
        reservePageDigits(PageNumbers.DIGITS);
    }

    /**
     * Cifras del número de prueba de {@code {page}} y {@code {pages}}. Cambia las medidas: hay
     * que vaciar la caché antes de volver a maquetar ({@link dev.stxt.boxes.layout.Engine}).
     */
    public void reservePageDigits(int digits) {
        this.measurer = PageNumbers.probing(real, digits);
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
        double available = f.width() - f.border().horizontal() - f.padding().horizontal();
        for (TextLine l : f.lines()) {
            if (!PageNumbers.contains(l.text())) {
                lines.add(l);
                continue;
            }
            changed = true;
            String text = PageNumbers.apply(l.text(), page, pages);
            double width = real.width(text, s.fontFace(), s.fontSize);
            double spacing = l.wordSpacing() > 0
                    ? LineBreaker.wordSpacing(text, width, available, s.textAlign, false) : 0;
            double x = LineBreaker.offset(width, available, s.textAlign);
            width += spacing * text.chars().filter(c -> c == ' ').count();
            lines.add(new TextLine(x, l.y(), width, l.height(), text, spacing));
        }
        if (!changed) {
            return f;
        }
        return new Fragment(f.box(), f.x(), f.y(), f.width(), f.height(), f.margin(), f.border(), f.padding(),
                children, lines, f.first(), f.last(), f.offsetX(), f.offsetY());
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        IntrinsicSizes content = box.hasText()
                ? LineBreaker.intrinsics(box.text, measurer, box.style.fontFace(), box.style.fontSize)
                : IntrinsicSizes.ZERO;
        return content;
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, () -> BoxModel.intrinsic(box, contentIntrinsic(box)));
        Style s = box.style;
        double available = model.contentWidth();
        double lineHeight = s.usedLineHeight();
        boolean first = from == null;
        int start = first ? 0 : ((BreakToken.Line) from).nextLine();

        List<List<String>> paragraphs = box.hasText()
                ? LineBreaker.breakParagraphs(box.text, available, measurer, s.fontFace(), s.fontSize, s.textWrap)
                : List.of();
        List<String> texts = new ArrayList<>();
        int[] paragraphEnds = new int[paragraphs.size()];
        for (int p = 0; p < paragraphs.size(); p++) {
            texts.addAll(paragraphs.get(p));
            paragraphEnds[p] = texts.size();
        }

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
            String text = texts.get(j);
            while (paragraphEnds[paragraph] <= j) {
                paragraph++;
            }
            double width = measurer.width(text, s.fontFace(), s.fontSize);
            double x = LineBreaker.offset(width, available, s.textAlign);
            double spacing = LineBreaker.wordSpacing(text, width, available, s.textAlign,
                    j == paragraphEnds[paragraph] - 1);
            width += spacing * text.chars().filter(c -> c == ' ').count();
            lines.add(new TextLine(x, lines.size() * lineHeight, width, lineHeight, text, spacing));
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
