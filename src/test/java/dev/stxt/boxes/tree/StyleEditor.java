package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Break;
import dev.stxt.boxes.model.Layout;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.TextAlign;
import dev.stxt.boxes.model.Value;

/**
 * Un {@link Style} escrito a mano, y cajas con él, para los tests de otros packages que prueban una pieza con
 * un estilo concreto sin leer un documento: fuera de {@code tree} un estilo o una caja solo se leen. El estilo se
 * puede seguir cambiando después de crear sus cajas, como hacen esos tests entre una comprobación y la siguiente.
 */
public final class StyleEditor {

    private final Style style = new Style();

    /** El estilo que se escribe. */
    public Style style() {
        return style;
    }

    /** Una caja con este estilo, sin padre ni hijos. */
    public Box box(String id, Element element) {
        return new Box(id, element, style, false, 1);
    }

    /** Cuelga {@code children} de {@code parent}, como hijas suyas y en este orden. */
    public static void addChildren(Box parent, Box... children) {
        for (Box child : children) {
            parent.addChild(child);
        }
    }

    public StyleEditor layout(Layout v) {
        style.layout = v;
        return this;
    }

    public StyleEditor breakBefore(Break v) {
        style.breaks().setBefore(v);
        return this;
    }

    public StyleEditor breakAfter(Break v) {
        style.breaks().setAfter(v);
        return this;
    }

    public StyleEditor font(String v) {
        style.font = v;
        return this;
    }

    public StyleEditor fontSize(double v) {
        style.fontSize = v;
        return this;
    }

    public StyleEditor letterSpacing(double v) {
        style.letterSpacing = v;
        return this;
    }

    public StyleEditor textAlign(TextAlign v) {
        style.textAlign = v;
        return this;
    }

    public StyleEditor width(Value v) {
        style.width = v;
        return this;
    }

    public StyleEditor height(Value v) {
        style.height = v;
        return this;
    }

    public StyleEditor minWidth(Value v) {
        style.minWidth = v;
        return this;
    }

    public StyleEditor maxWidth(Value v) {
        style.maxWidth = v;
        return this;
    }

    public StyleEditor minHeight(Value v) {
        style.minHeight = v;
        return this;
    }

    public StyleEditor maxHeight(Value v) {
        style.maxHeight = v;
        return this;
    }

    public StyleEditor aspectRatio(Double v) {
        style.aspectRatio = v;
        return this;
    }

    public StyleEditor margin(Sides<Value> v) {
        style.margin = v;
        return this;
    }

    public StyleEditor padding(Sides<Value> v) {
        style.padding = v;
        return this;
    }

    public StyleEditor border(Sides<Value> v) {
        style.border = v;
        return this;
    }
}
