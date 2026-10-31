package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.Value;

/**
 * A {@link Style} written by hand, and boxes with it, for the tests of other packages that test a piece with
 * a specific style without reading a document: outside {@code tree} a style or a box is only read. The style can
 * still be changed after creating its boxes, as those tests do between one check and the next.
 */
public final class StyleEditor {

    private final Style style = new Style();

    /** The style being written. */
    public Style style() {
        return style;
    }

    /** A box with this style, without parent or children. */
    public Box box(String id, Element element) {
        return new Box(id, element, style, false, 1);
    }

    /** Hangs {@code children} from {@code parent}, as its children and in this order. */
    public static void addChildren(Box parent, Box... children) {
        for (Box child : children) {
            parent.addChild(child);
        }
    }

    public StyleEditor layout(Layout v) {
        style.setLayout(v);
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
        style.typography().setFont(v);
        return this;
    }

    public StyleEditor fontSize(double v) {
        style.typography().setFontSize(v);
        return this;
    }

    public StyleEditor letterSpacing(double v) {
        style.typography().setLetterSpacing(v);
        return this;
    }

    public StyleEditor textAlign(TextAlign v) {
        style.typography().setTextAlign(v);
        return this;
    }

    public StyleEditor width(Value v) {
        style.box().setWidth(v);
        return this;
    }

    public StyleEditor height(Value v) {
        style.box().setHeight(v);
        return this;
    }

    public StyleEditor minWidth(Value v) {
        style.box().setMinWidth(v);
        return this;
    }

    public StyleEditor maxWidth(Value v) {
        style.box().setMaxWidth(v);
        return this;
    }

    public StyleEditor minHeight(Value v) {
        style.box().setMinHeight(v);
        return this;
    }

    public StyleEditor maxHeight(Value v) {
        style.box().setMaxHeight(v);
        return this;
    }

    public StyleEditor aspectRatio(Double v) {
        style.box().setAspectRatio(v);
        return this;
    }

    public StyleEditor margin(Sides<Value> v) {
        style.box().setMargin(v);
        return this;
    }

    public StyleEditor padding(Sides<Value> v) {
        style.box().setPadding(v);
        return this;
    }

    public StyleEditor border(Sides<Value> v) {
        style.box().setBorder(v);
        return this;
    }
}
