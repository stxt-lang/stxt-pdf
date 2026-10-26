package dev.stxt.boxes.layout.inline;

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
 * <p>En modo paginado (§5.10) se colocan las líneas que caben desde la del {@link BreakToken.Line}
 * respetando viudas y huérfanas en cada párrafo ({@link BreakRules#cutLines}); si no cabe
 * ninguna, se devuelve {@link BreakToken#BEFORE} para que el padre corte antes de la caja.</p>
 */
public final class InlineLayout implements LayoutAlgorithm {

    private final TextMeasurer measurer;

    public InlineLayout(TextMeasurer measurer) {
        this.measurer = measurer;
    }

    @Override
    public IntrinsicSizes intrinsic(Box box) {
        IntrinsicSizes content = box.hasText()
                ? LineBreaker.intrinsics(box.text, measurer, box.style.font, box.style.fontSize)
                : IntrinsicSizes.ZERO;
        return BoxModel.intrinsic(box, content);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, () -> intrinsic(box));
        model.applyTo(box);
        Style s = box.style;
        double available = model.contentWidth();
        double lineHeight = s.usedLineHeight();
        boolean first = from == null;
        int start = first ? 0 : ((BreakToken.Line) from).nextLine();

        List<List<String>> paragraphs = box.hasText()
                ? LineBreaker.breakParagraphs(box.text, available, measurer, s.font, s.fontSize)
                : List.of();
        List<String> texts = new ArrayList<>();
        int[] paragraphEnds = new int[paragraphs.size()];
        for (int p = 0; p < paragraphs.size(); p++) {
            texts.addAll(paragraphs.get(p));
            paragraphEnds[p] = texts.size();
        }

        int end = texts.size();
        int appeal = LayoutResult.PERFECT_BREAK;
        if (space.fragmenting()) {
            double remaining = space.fragmentainerRemaining().getAsDouble() - Slice.top(model, first);
            BreakRules.Cut cut = BreakRules.cutLines(paragraphEnds, start, BreakRules.capacity(remaining, lineHeight),
                    s.widows, s.orphans, space.atFragmentainerStart());
            if (cut.end() == start && start < texts.size()) {
                return new LayoutResult(Fragment.empty(box), BreakToken.BEFORE, LayoutResult.PERFECT_BREAK,
                        List.of(), AdjoiningMargins.NONE);
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
            double width = measurer.width(text, s.font, s.fontSize);
            double x = LineBreaker.offset(width, available, s.textAlign);
            double spacing = LineBreaker.wordSpacing(text, width, available, s.textAlign,
                    j == paragraphEnds[paragraph] - 1);
            width += spacing * text.chars().filter(c -> c == ' ').count();
            lines.add(new TextLine(x, lines.size() * lineHeight, width, lineHeight, text, spacing));
        }

        Slice slice = Slice.of(model, first, BreakToken.consumed(from), lines.size() * lineHeight,
                end == texts.size(), space);
        box.height = slice.height();
        Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), slice.border(), slice.padding(),
                List.of(), lines, first, slice.finished());
        if (!slice.finished()) {
            // Faltan líneas o, si han terminado, el alto propio de la caja.
            return new LayoutResult(fragment, new BreakToken.Line(end, slice.consumed()),
                    end == texts.size() ? LayoutResult.PERFECT_BREAK : appeal, List.of(), AdjoiningMargins.NONE);
        }
        // Una caja sin líneas, borde, relleno ni alto deja colapsar sus márgenes (§5.3).
        boolean empty = first && lines.isEmpty() && slice.height() == 0 && model.openAtTop() && model.openAtBottom()
                && !box.establishesBlockFormattingContext();
        return LayoutResult.finished(fragment, List.of(), empty ? AdjoiningMargins.EMPTY_BOX : AdjoiningMargins.NONE);
    }
}
