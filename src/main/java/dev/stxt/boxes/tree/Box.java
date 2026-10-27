package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Una caja del árbol de cajas (ESPECIFICACION.md §5.1): cada elemento de caja del documento
 * ({@code Block}, {@code Flex}, {@code Text}…) es una; la raíz, anónima, es el área útil. Un
 * {@code Text} guarda su texto directamente.
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
    /** Línea del nodo {@code Text}. */
    public int textLine;
    /** El texto es un bloque ({@code Text >>}): su contenido empieza en la línea siguiente. */
    public boolean textBlock;

    // Geometría usada (caja de borde), relativa a la caja de contenido del padre; en las cajas
    // fuera de flujo, a la de su containing block. Incluye el desplazamiento de las relative.
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

    /**
     * Crea un contexto de formato de bloque, que aísla el colapso de márgenes
     * (ESPECIFICACION.md §5.3): el área útil (la raíz), las cajas {@code absolute} y
     * {@code fixed}, los ítems de flex y grid y las cajas con {@code Isolate: true}.
     */
    public boolean establishesBlockFormattingContext() {
        if (parent == null || style.isolate || style.isOutOfFlow()) {
            return true;
        }
        Style.Layout l = parent.style.layout;
        return l == Style.Layout.FLEX || l == Style.Layout.GRID;
    }

    /**
     * Containing block de una caja fuera de flujo (ESPECIFICACION.md §5.2): para {@code absolute},
     * el ancestro más cercano con {@code Position} distinta de {@code static}; si no hay ninguno,
     * o si la caja es {@code fixed}, la raíz, que representa el área útil.
     */
    public Box outOfFlowContainingBlock() {
        Box b = parent;
        boolean fixed = style.position == Style.Position.FIXED;
        while (b.parent != null && (fixed || !b.style.isPositioned())) {
            b = b.parent;
        }
        return b;
    }

    /** Línea del documento donde está el carácter {@code index} del texto. */
    public int textLineAt(int index) {
        if (!textBlock) {
            return textLine;
        }
        int line = textLine + 1;
        for (int i = 0; i < index; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
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
        return anonymous ? "(área útil)" : "(caja línea " + line + ")";
    }

    @Override
    public String toString() {
        return "Box[" + describe() + ", " + style.layout + "]";
    }
}
