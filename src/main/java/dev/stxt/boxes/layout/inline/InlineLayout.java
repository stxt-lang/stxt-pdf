package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout {@code inline}: texto repartido en line boxes (ESPECIFICACION.md §5.4).
 *
 * <p>Pasos: resolver el ancho de contenido, partir con {@link LineBreaker}, alinear cada línea,
 * altura = número de líneas × {@code Line height}.</p>
 *
 * <p>Pendiente: {@code justify} se coloca como {@code left}; repartir el sobrante entre los
 * espacios necesita que {@link TextLine} y el {@code Canvas} sepan del espaciado entre
 * palabras.</p>
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
        // TODO(hito 6): en modo paginado, colocar solo las líneas que indique
        //   BreakRules.linesThatFit y devolver un BreakToken.Line con la siguiente.
        BoxModel model = BoxModel.resolve(box, space, () -> intrinsic(box));
        model.applyTo(box);
        Style s = box.style;
        double available = model.contentWidth();
        double lineHeight = s.usedLineHeight();

        List<String> texts = box.hasText()
                ? LineBreaker.breakLines(box.text, available, measurer, s.font, s.fontSize)
                : List.of();
        List<TextLine> lines = new ArrayList<>();
        for (String text : texts) {
            double width = measurer.width(text, s.font, s.fontSize);
            double x = LineBreaker.offset(width, available, s.textAlign);
            lines.add(new TextLine(x, lines.size() * lineHeight, width, lineHeight, text));
        }

        double height = model.usedHeight(lines.size() * lineHeight);
        box.height = height;
        Fragment fragment = Fragment.complete(box, 0, 0, model.width(), height, List.of(), lines);
        // Una caja sin líneas, borde, relleno ni alto deja colapsar sus márgenes (§5.3).
        boolean empty = lines.isEmpty() && height == 0 && model.openAtTop() && model.openAtBottom()
                && !box.establishesBlockFormattingContext();
        return LayoutResult.finished(fragment, List.of(), empty ? AdjoiningMargins.EMPTY_BOX : AdjoiningMargins.NONE);
    }
}
