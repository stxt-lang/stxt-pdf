package dev.stxt.boxes.output;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.model.Rect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Vuelca el resultado del layout en STXT (referencia/implementacion/pruebas.md). Es el formato de los tests de
 * referencia: rectángulos de caja de borde en coordenadas absolutas de página, en pt.
 *
 * <pre>
 * Result:
 * 	Page: 1
 * 		Fragment: header
 * 			Rect: 40 40 515 60
 * </pre>
 *
 * <p>Por defecto solo se vuelcan los fragmentos con {@code Id}; sus descendientes con
 * {@code Id} se anidan dentro.</p>
 */
public final class StxtDump {

    private StxtDump() {
    }

    public static String dump(List<Page> pages, boolean includeWithoutId) {
        StringBuilder sb = new StringBuilder("Result:\n");
        for (Page p : pages) {
            // Las coordenadas son las de la página, con el área útil del medio de su sección
            // (referencia/componentes/document.md).
            Rect pageArea = p.media().pageArea();
            sb.append("\tPage: ").append(p.number()).append('\n');
            dumpChildren(sb, p.content(), pageArea.x(), pageArea.y(), 2, includeWithoutId);
            for (Fragment fixed : p.fixed()) {
                dumpFragment(sb, fixed, pageArea.x(), pageArea.y(), 2, includeWithoutId);
            }
        }
        return sb.toString();
    }

    private static void dumpChildren(StringBuilder sb, Fragment parent, double absX, double absY,
                                     int level, boolean includeWithoutId) {
        Rect content = new Rect(absX, absY, parent.width(), parent.height())
                .shrink(parent.border()).shrink(parent.padding());
        for (Fragment h : parent.children()) {
            dumpFragment(sb, h, content.x(), content.y(), level, includeWithoutId);
        }
    }

    private static void dumpFragment(StringBuilder sb, Fragment f, double originX, double originY,
                                     int level, boolean includeWithoutId) {
        double x = originX + f.x();
        double y = originY + f.y();
        if (f.box().isMarker()) {
            // Va dentro del ítem: solo si se vuelca el ítem.
            if (f.box().parent().id() != null || includeWithoutId) {
                String tab = "\t".repeat(level);
                sb.append(tab).append("Marker: ").append(f.box().text().strip()).append('\n');
                appendRect(sb, tab, x, y, f);
            }
            return;
        }
        boolean visible = f.box().id() != null || includeWithoutId;
        int childLevel = level;
        if (visible) {
            String tab = "\t".repeat(level);
            String name = f.box().id() != null ? f.box().id() : f.box().describe();
            sb.append(tab).append("Fragment: ").append(name).append('\n');
            appendRect(sb, tab, x, y, f);
            childLevel = level + 1;
        }
        dumpChildren(sb, f, x, y, childLevel, includeWithoutId);
    }

    private static void appendRect(StringBuilder sb, String tab, double x, double y, Fragment f) {
        sb.append(tab).append("\tRect: ").append(format(x)).append(' ').append(format(y)).append(' ')
                .append(format(f.width())).append(' ').append(format(f.height())).append('\n');
    }

    /** Hasta 2 decimales, sin ceros finales. */
    static String format(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
