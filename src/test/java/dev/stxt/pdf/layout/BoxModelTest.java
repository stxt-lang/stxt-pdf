package dev.stxt.pdf.layout;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;
import dev.stxt.pdf.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Modelo de caja usado (referencia/comun/caja.md), con estilos escritos a mano. */
class BoxModelTest {

    private static final OptionalDouble NONE = OptionalDouble.empty();
    /** Los intrínsecos no deben hacer falta salvo con {@code Width} intrínseco. */
    private static final Supplier<IntrinsicSizes> UNUSED = () -> {
        throw new AssertionError("no hacen falta los tamaños intrínsecos");
    };

    private final StyleEditor style = new StyleEditor();
    private final Box box = style.box("A", Element.BLOCK);

    private static Value pt(double v) {
        return Value.pt(v);
    }

    private static Value percent(double v) {
        return new Value.Percentage(v);
    }

    private BoxModel resolve(ConstraintSpace space) {
        return BoxModel.resolve(box, space, UNUSED);
    }

    @Test
    void percentagesOfMarginBorderAndPaddingUseTheWidthBase() {
        style.margin(Sides.all(percent(10)));
        style.padding(Sides.all(percent(5)));
        style.border(Sides.all(pt(2)));
        BoxModel m = resolve(ConstraintSpace.free(400, 400, OptionalDouble.of(1000), false));
        assertEquals(Insets.all(40), m.margin());
        assertEquals(Insets.all(20), m.padding());
        assertEquals(Insets.all(2), m.border());
        // Width auto en flujo: el ancho disponible menos los márgenes.
        assertEquals(320, m.width(), 1e-9);
        assertEquals(400 - 80 - 4 - 40, m.contentWidth(), 1e-9);
    }

    @Test
    void widthPercentageWithMinAndMax() {
        style.width(percent(50));
        ConstraintSpace space = ConstraintSpace.free(400, 400, NONE, false);
        assertEquals(200, resolve(space).width(), 1e-9);
        style.minWidth(pt(250));
        assertEquals(250, resolve(space).width(), 1e-9);
        // Si chocan, gana el mínimo.
        style.minWidth(pt(150));
        style.maxWidth(pt(100));
        assertEquals(150, resolve(space).width(), 1e-9);
    }

    @Test
    void fixedWidthWinsOverTheDeclaredOne() {
        style.width(pt(200));
        style.minWidth(pt(300));
        assertEquals(123, resolve(ConstraintSpace.fixed(123, NONE, 400, NONE, false)).width(), 1e-9);
    }

    @Test
    void intrinsicWidthKeywordsUseTheMarginBoxSizes() {
        style.margin(Sides.all(pt(10)));
        Supplier<IntrinsicSizes> intrinsic = () -> new IntrinsicSizes(60, 200);
        ConstraintSpace space = ConstraintSpace.free(150, 400, NONE, false);
        style.width(Value.Keyword.MIN_CONTENT);
        assertEquals(40, BoxModel.resolve(box, space, intrinsic).width(), 1e-9);
        style.width(Value.Keyword.MAX_CONTENT);
        assertEquals(180, BoxModel.resolve(box, space, intrinsic).width(), 1e-9);
        // fit-content: el disponible (150) entre min-content y max-content.
        style.width(Value.Keyword.FIT_CONTENT);
        assertEquals(130, BoxModel.resolve(box, space, intrinsic).width(), 1e-9);
    }

    @Test
    void heightPercentageNeedsADefiniteBase() {
        style.height(percent(50));
        assertEquals(OptionalDouble.of(150), resolve(ConstraintSpace.free(100, 100, OptionalDouble.of(300), false)).height());
        assertEquals(NONE, resolve(ConstraintSpace.free(100, 100, NONE, false)).height());
    }

    @Test
    void definiteHeightWithMinAndMax() {
        style.height(pt(50));
        style.minHeight(pt(80));
        style.maxHeight(pt(60));
        BoxModel m = resolve(ConstraintSpace.free(100, 100, NONE, false));
        assertEquals(OptionalDouble.of(80), m.height());
        assertEquals(80, m.minHeight(), 1e-9);
        assertEquals(OptionalDouble.of(60), m.maxHeight());
    }

    @Test
    void aspectRatioGivesTheHeightFromTheWidth() {
        style.width(pt(200));
        style.aspectRatio(2.0);
        ConstraintSpace space = ConstraintSpace.free(400, 400, NONE, false);
        assertEquals(OptionalDouble.of(100), resolve(space).height());
        style.maxHeight(pt(80));
        assertEquals(OptionalDouble.of(80), resolve(space).height());
    }

    @Test
    void fixedHeightWinsOverTheDeclaredOne() {
        style.height(pt(50));
        style.minHeight(pt(90));
        assertEquals(OptionalDouble.of(70),
                resolve(ConstraintSpace.fixed(100, OptionalDouble.of(70), 100, NONE, false)).height());
    }

    @Test
    void measuringContentHeightIgnoresHeightAndAspectRatio() {
        style.height(pt(50));
        ConstraintSpace space = ConstraintSpace.free(200, 200, NONE, false).measuringContentHeight();
        assertEquals(NONE, resolve(space).height());
        style.height(Value.AUTO);
        style.width(pt(200));
        style.aspectRatio(2.0);
        assertEquals(NONE, resolve(space).height());
    }

    @Test
    void usedHeight() {
        style.border(Sides.all(pt(1)));
        style.padding(Sides.all(pt(4)));
        style.minHeight(pt(50));
        BoxModel auto = resolve(ConstraintSpace.free(100, 100, NONE, false));
        assertEquals(50, auto.usedHeight(30), 1e-9);
        assertEquals(110, auto.usedHeight(100), 1e-9);
        style.height(pt(70));
        assertEquals(70, resolve(ConstraintSpace.free(100, 100, NONE, false)).usedHeight(100), 1e-9);
        assertEquals(OptionalDouble.of(60), resolve(ConstraintSpace.free(100, 100, NONE, false)).contentHeight());
    }

    @Test
    void intrinsicOfTheMarginBox() {
        style.border(Sides.all(pt(2)));
        style.padding(Sides.all(pt(3)));
        style.margin(Sides.all(pt(5)));
        IntrinsicSizes content = new IntrinsicSizes(30, 80);
        // Contenido + borde y relleno (10) + márgenes (10).
        assertEquals(new IntrinsicSizes(50, 100), BoxModel.intrinsic(box, content));
        style.minWidth(pt(50));
        style.maxWidth(pt(60));
        assertEquals(new IntrinsicSizes(60, 70), BoxModel.intrinsic(box, content));
        // Un Width fijo es a la vez min-content y max-content, también limitado.
        style.width(pt(100));
        assertEquals(new IntrinsicSizes(70, 70), BoxModel.intrinsic(box, content));
        style.maxWidth(null);
        assertEquals(new IntrinsicSizes(110, 110), BoxModel.intrinsic(box, content));
        // Un porcentaje no se puede resolver sin containing block: cuenta como auto.
        style.width(percent(50));
        style.minWidth(Value.AUTO);
        assertEquals(new IntrinsicSizes(50, 100), BoxModel.intrinsic(box, content));
    }

    @Test
    void childSpaceIsTheContentBox() {
        style.border(Sides.all(pt(1)));
        style.padding(Sides.all(pt(4)));
        style.height(pt(100));
        ConstraintSpace space = ConstraintSpace.forChildren(200, NONE, OptionalDouble.of(60), true);
        BoxModel m = resolve(space);
        assertEquals(ConstraintSpace.forChildren(190, OptionalDouble.of(90), OptionalDouble.of(55), true),
                m.childSpace(space, true));
        // Los fragmentos siguientes no tienen la decoración superior.
        assertEquals(ConstraintSpace.forChildren(190, OptionalDouble.of(90), OptionalDouble.of(60), true),
                m.childSpace(space, false));
        ConstraintSpace free = ConstraintSpace.free(200, 200, NONE, false);
        assertEquals(NONE, resolve(free).childSpace(free, true).fragmentainerRemaining());
    }

    @Test
    void intrinsicSizesAreOnlyComputedWhenNeeded() {
        style.width(pt(10));
        resolve(ConstraintSpace.free(100, 100, NONE, false));
        style.width(Value.Keyword.MIN_CONTENT);
        assertThrows(AssertionError.class, () -> resolve(ConstraintSpace.free(100, 100, NONE, false)));
    }
}
