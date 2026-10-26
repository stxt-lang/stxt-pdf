package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Una caja del árbol de cajas (módulo 0). No coincide 1:1 con los nodos {@code Box} del
 * documento: puede ser anónima y algunas cajas contienen texto directamente.
 *
 * <p>La geometría que guarda es la del <em>último layout</em> y es relativa a la caja de
 * contenido del padre (módulo 2). El resultado definitivo, por páginas, son los
 * {@link dev.stxt.boxes.fragment.Fragment}.</p>
 *
 * <p>La igualdad es por identidad: la caché de layout usa la caja como clave.</p>
 */
public final class Box {

    public final String id;
    public final Style style;
    public final boolean anonymous;
    /** Línea del documento de la que procede, para mensajes de error. */
    public final int line;
    public final List<Box> children = new ArrayList<>();
    public Box parent;
    /** Contenido en línea, si la caja contiene texto directamente. */
    public String text;

    // Geometría usada (caja de borde), relativa a la caja de contenido del padre.
    public double x;
    public double y;
    public double width;
    public double height;
    public Insets margin = Insets.ZERO;
    public Insets border = Insets.ZERO;
    public Insets padding = Insets.ZERO;

    public Box(String id, Style style, boolean anonymous, int line) {
        this.id = id;
        this.style = style;
        this.anonymous = anonymous;
        this.line = line;
    }

    public void addChild(Box child) {
        child.parent = this;
        children.add(child);
    }

    /** Hijos que participan en el flujo del padre ({@code static} y {@code relative}). */
    public List<Box> inFlowChildren() {
        return children.stream().filter(h -> !h.style.isOutOfFlow()).toList();
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    public Rect borderBox() {
        return new Rect(x, y, width, height);
    }

    public Rect marginBox() {
        return borderBox().expand(margin);
    }

    public Rect paddingBox() {
        return borderBox().shrink(border);
    }

    public Rect contentBox() {
        return paddingBox().shrink(padding);
    }

    /** Nombre legible para mensajes y volcados. */
    public String describe() {
        if (id != null) {
            return id;
        }
        return anonymous ? "(anónima)" : "(caja línea " + line + ")";
    }

    @Override
    public String toString() {
        return "Box[" + describe() + ", " + style.layout + "]";
    }
}
