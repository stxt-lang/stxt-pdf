package dev.stxt.pdf.tree;

import dev.stxt.Node;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Leader;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.tree.Nodes.error;

/**
 * El índice automático (referencia/componentes/table-of-contents.md): lee {@code Levels} y {@code Leader} de un
 * {@code Table of contents} y, cuando el árbol está construido y se conocen todos los encabezados, genera
 * sus entradas.
 *
 * <p>Cada entrada es una caja {@code Flex} generada ({@link Element#CONTENTS_ENTRY_1} … según el
 * nivel del encabezado), con tres hijos: el título (el texto del encabezado), la guía, que crece
 * hasta llenar el hueco, y el número de página, que es un {@code {page of: …}} del encabezado: el
 * mismo mecanismo que escribe el autor, así que se resuelve paginando hasta que converja. Como un
 * encabezado de un {@code Markdown} no tiene {@code Id}, se le da un {@link Box#anchor()} para
 * poder referirlo. Las entradas se estilan con las reglas {@code For element: TOC N}.</p>
 */
final class ContentsBuilder {

    /** Cuánto separa la guía del título y del número, en puntos. */
    private static final double LEADER_GAP = 4;

    private ContentsBuilder() {
    }

    /** {@code Levels}: el primer y el último nivel, de 1 a 6. */
    static void levels(Node n, Style s) {
        String[] parts = n.getText().trim().split("\\s+");
        int[] levels = new int[2];
        try {
            if (parts.length != 2) {
                throw new NumberFormatException();
            }
            for (int i = 0; i < 2; i++) {
                levels[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Levels necesita el primer y el último nivel de "
                    + "encabezado, como Levels: 1 3: '" + n.getText().trim() + "'");
        }
        if (levels[0] < 1 || levels[1] > 6 || levels[0] > levels[1]) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Levels va de un nivel a otro igual o mayor, entre 1 y 6: '"
                    + n.getText().trim() + "'");
        }
        s.contents().setFrom(levels[0]);
        s.contents().setTo(levels[1]);
    }

    /** Genera las entradas de todos los {@code Table of contents} del documento. */
    static void build(BuildContext ctx, Box root) {
        if (ctx.contents.isEmpty()) {
            return;
        }
        List<Box> headings = new ArrayList<>();
        collect(root, headings);
        int n = 0;
        for (Box heading : headings) {
            n++;
            if (heading.id() == null) {
                heading.setAnchor("(heading " + n + ")");
            }
        }
        for (Box contents : ctx.contents) {
            fill(ctx, contents, headings);
        }
    }

    /** Los encabezados del árbol con entrada en los índices ({@code In table of contents}), en orden del documento. */
    private static void collect(Box box, List<Box> headings) {
        if (box.headingLevel() > 0 && box.style().inContents()) {
            headings.add(box);
        }
        box.children().forEach(child -> collect(child, headings));
    }

    private static void fill(BuildContext ctx, Box contents, List<Box> headings) {
        Style s = contents.style();
        if (s.typography().font() == null) {
            throw error(contents.line(), ErrorCode.TEXT_WITHOUT_FONT, "El índice necesita Font: declárala en el "
                    + "Typography del Table of contents, de un antecesor o de Styles (For element: Document)");
        }
        if (!s.isDeclared(PropertyKey.FONT_SIZE)) {
            throw error(contents.line(), ErrorCode.TEXT_WITHOUT_FONT_SIZE, "El índice necesita Size: decláralo en el "
                    + "Typography del Table of contents, de un antecesor o de Styles (For element: Document)");
        }
        List<Box> listed = headings.stream()
                .filter(h -> h.headingLevel() >= s.contents().from() && h.headingLevel() <= s.contents().to()).toList();
        if (listed.isEmpty()) {
            throw error(contents.line(), ErrorCode.TABLE_OF_CONTENTS_EMPTY, "El índice no tiene ningún encabezado de los niveles "
                    + s.contents().from() + " a " + s.contents().to() + " (Levels): no tendría entradas");
        }
        for (Box heading : listed) {
            contents.addChild(entry(ctx, contents, heading));
        }
    }

    private static Box entry(BuildContext ctx, Box contents, Box heading) {
        Style es = Style.inheriting(contents.style());
        es.setLayout(Layout.FLEX);
        ctx.rules.apply(Element.contentsEntry(heading.headingLevel()), es);
        ModelValidator.validateBox(ctx, es, contents);
        Box entry = new Box(null, Element.contentsEntry(heading.headingLevel()), es, false, heading.line());
        ctx.definiteHeight.put(entry, false);
        // La entrada lleva a su encabezado (referencia/comun/enlaces.md): toda ella es clicable.
        es.setLink(Links.to(heading.key()));

        boolean leader = contents.style().contents().leader() != Leader.NONE;
        int line = heading.children().isEmpty() ? heading.line() : heading.children().get(0).textLine();

        // Sin guía, el título llena el hueco y deja el número a la derecha.
        Box title = generated(ctx, entry, Element.CONTENTS_TITLE, Layout.INLINE, heading.line());
        title.setText(heading.headingText(), line, false);
        title.style().item().setGrow(leader ? 0 : 1);
        entry.addChild(title);

        if (leader) {
            Box guide = generated(ctx, entry, Element.CONTENTS_LEADER, Layout.BLOCK, heading.line());
            guide.style().contents().setLeader(contents.style().contents().leader());
            guide.style().item().setGrow(1);
            guide.style().box().setHeight(Value.pt(es.typography().usedLineHeight()));
            guide.style().box().setMargin(new Sides<>(Value.pt(0), Value.pt(LEADER_GAP), Value.pt(0), Value.pt(LEADER_GAP)));
            entry.addChild(guide);
        }

        Box page = generated(ctx, entry, Element.CONTENTS_PAGE, Layout.INLINE, heading.line());
        page.setText("{page of: " + heading.key() + "}", line, false);
        page.style().item().setShrink(0);
        entry.addChild(page);
        return entry;
    }

    /** Una caja generada de una entrada: hereda la tipografía de la entrada, y el layout lo da su tipo. */
    private static Box generated(BuildContext ctx, Box entry, Element kind, Layout layout, int line) {
        Style s = Style.inheriting(entry.style());
        s.setLayout(layout);
        Box box = new Box(null, kind, s, false, line);
        ctx.definiteHeight.put(box, false);
        return box;
    }
}
