package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.pdf.model.Markup;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.pdf.tree.ListBuilder.validateList;
import static dev.stxt.pdf.tree.ModelValidator.font;
import static dev.stxt.pdf.tree.ModelValidator.validateStack;
import static dev.stxt.pdf.tree.ModelValidator.validateImage;
import static dev.stxt.pdf.tree.ModelValidator.validateColumns;
import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.PropertyReader.stack;
import static dev.stxt.pdf.tree.PropertyReader.fragmentation;
import static dev.stxt.pdf.tree.PropertyReader.grid;
import static dev.stxt.pdf.tree.PropertyReader.item;
import static dev.stxt.pdf.tree.PropertyReader.columns;
import static dev.stxt.pdf.tree.PropertyReader.typography;
import static dev.stxt.pdf.tree.TableBuilder.cellText;
import static dev.stxt.pdf.tree.TableBuilder.table;

/**
 * Lo propio de cada familia de elementos al construir su caja: las propiedades de su tipo, lo que
 * comprueba de las propiedades ya leídas y su contenido (un texto, una imagen o sus hijos).
 * {@link BoxTreeBuilder#element} hace lo común a todos, en orden, y llama a la familia del elemento
 * ({@link #of}) en cada paso. Un elemento nuevo sin nada propio es un {@link #CONTAINER}; si tiene algo propio,
 * es una familia nueva aquí, sin tocar el orden de {@code element}.
 */
enum ElementReader {

    /** {@code Text}, {@code Paragraph} y el bloque de código de un {@code Markdown}: el texto, con sus tramos. */
    TEXT {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            text(n, box);
            box.setRuns(ctx.runs.get(n));
            inlineFormat(ctx, box);
            codeFont(box);
        }
    },

    /**
     * El bloque de código de un {@code Markdown} (reference/components/markdown.md): un texto cuya fuente es su
     * {@code Code font}, propia o heredada, con la línea de su declaración.
     */
    CODE_BLOCK {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            if (s.typography().codeFont() == null) {
                throw error(n, ErrorCode.CODE_WITHOUT_FONT, "El bloque de código necesita Code font: se declara en el "
                        + "Typography del Markdown, de un antecesor o de Styles (For element: Document o For element: Code "
                        + "block), como Code font: IBM Plex Mono");
            }
            s.typography().setFont(s.typography().codeFont());
            s.declared(PropertyKey.FONT, s.line(PropertyKey.CODE_FONT));
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            TEXT.content(builder, ctx, n, box);
        }
    },

    IMAGE {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateImage(s);
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            image(n, box);
        }
    },

    /**
     * La {@code Cell} de una tabla (reference/components/table.md): con valor es una caja de texto y, sin él, un
     * bloque. Su fila ({@code wrapper}) le da su {@code Typography}, como herencia, y su {@code Break}.
     */
    CELL {
        @Override
        Layout layout(InlineNode n, Element kind) {
            return n.getText().isBlank() ? kind.layout() : Layout.INLINE;
        }

        @Override
        void wrapper(InlineNode wrapper, Style s, Box parent) {
            // El Typography de la fila lo heredan sus celdas: va antes que las reglas.
            child(wrapper, "typography").map(Nodes::inline).ifPresent(t -> typography(t, s));
        }

        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            // El Break de la fila es el de sus celdas: con él se corta entre filas, como en un grid.
            child(wrapper, "break").map(Nodes::inline).ifPresent(b -> fragmentation(b, s));
            if (s.rowHeader() && position != null && position.group() == CellPosition.HEADER) {
                throw error(s.line("row-header"), ErrorCode.ROW_HEADER_IN_HEADER_ROWS, "Row header no tiene efecto en "
                        + "una celda de Header rows: las celdas de la cabecera ya son cabeceras de columna");
            }
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            if (box.style().layout() == Layout.INLINE) {
                cellText(n, box);
                box.setRuns(ctx.runs.get(n));
                inlineFormat(ctx, box);
                codeFont(box);
            } else {
                builder.buildContent(ctx, n, box);
            }
        }
    },

    /** Una {@code Table} (reference/components/table.md): un grid cuyas celdas coloca {@link TableBuilder}. */
    TABLE {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.GRID.contains(p.getCanonicalName())) {
                return false;
            }
            grid(p, s);
            return true;
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            table(ctx, n, box, (cell, row, at) -> builder.element(ctx, cell, box, row, at));
        }
    },

    GRID {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.GRID.contains(p.getCanonicalName())) {
                return false;
            }
            grid(p, s);
            return true;
        }
    },

    STACK {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.STACK.contains(p.getCanonicalName())) {
                return false;
            }
            stack(p, s);
            return true;
        }

        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateStack(s);
        }
    },

    LIST {
        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateList(s);
        }
    },

    /**
     * Un {@code Columns} (reference/components/columns.md): su contenido va en su flujo de columnas, una caja de
     * bloque generada que es su único hijo y de la que cada columna es un fragmento. Aísla los márgenes, para que
     * no salgan de las columnas, y su alto no es definido: lo decide el reparto en
     * columnas.
     */
    COLUMNS {
        @Override
        boolean property(Node p, Style s) {
            if (!PropertyReader.COLUMNS.contains(p.getCanonicalName())) {
                return false;
            }
            columns(p, s);
            return true;
        }

        @Override
        void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
            validateColumns(s, n.getLine());
        }

        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box columns) {
            Style s = Style.inheriting(columns.style());
            s.setIsolateMargins(true);
            Box flow = new Box(null, Element.COLUMN_FLOW, s, false, n.getLine());
            columns.addChild(flow);
            ctx.definiteHeight.put(flow, false);
            boolean outer = ctx.insideColumns;
            String outerUnbreakable = ctx.unbreakable;
            ctx.insideColumns = true;
            ctx.unbreakable = null;
            builder.buildContent(ctx, n, flow);
            ctx.insideColumns = outer;
            ctx.unbreakable = outerUnbreakable;
        }
    },

    /**
     * Un {@code Markdown} (reference/components/markdown.md): los elementos que genera {@link MarkdownBuilder},
     * construidos como si los hubiera escrito el autor; sus textos con formato llevan sus tramos.
     */
    MARKDOWN {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            MarkdownBuilder.Translation t = MarkdownBuilder.translate(n);
            ctx.runs.putAll(t.runs());
            ctx.codeBlocks.addAll(t.codeBlocks());
            ctx.insideMarkdown = true;
            for (InlineNode e : t.elements()) {
                box.addChild(builder.element(ctx, e, box, null, null));
            }
            ctx.insideMarkdown = false;
        }
    },

    /**
     * Un {@code Table of contents} (reference/components/table-of-contents.md): sus entradas las genera {@link
     * TableOfContentsBuilder} al final, con todos los encabezados.
     */
    TABLE_OF_CONTENTS {
        @Override
        void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
            builder.buildContent(ctx, n, box);
            ctx.contents.add(box);
        }
    },

    /** El resto: {@code Block}, los encabezados, {@code List item}, las cajas posicionadas…, con sus hijos. */
    CONTAINER;

    /** Una línea en blanco dentro del texto: separa párrafos (reference/common/typography.md). */
    private static final Pattern BLANK_LINE = Pattern.compile("\\n[ \\t]*\\n");

    /** La familia de un elemento. */
    static ElementReader of(Element kind) {
        if (kind == Element.CODE_BLOCK) {
            return CODE_BLOCK;
        }
        if (kind.hasText()) {
            return TEXT;
        }
        return switch (kind) {
            case IMAGE -> IMAGE;
            case CELL -> CELL;
            case TABLE -> TABLE;
            case GRID -> GRID;
            case STACK -> STACK;
            case LIST -> LIST;
            case COLUMNS -> COLUMNS;
            case MARKDOWN -> MARKDOWN;
            case TABLE_OF_CONTENTS -> TABLE_OF_CONTENTS;
            default -> CONTAINER;
        };
    }

    /** El layout de la caja: el del elemento, salvo en una {@code Cell}. */
    Layout layout(InlineNode n, Element kind) {
        return kind.layout();
    }

    /**
     * Lo que le da su envoltorio antes de aplicar las reglas: a un ítem, las propiedades de su {@code Item} o su
     * {@code Area}; {@code wrapper} es {@code null} si no tiene.
     */
    void wrapper(InlineNode wrapper, Style s, Box parent) {
        if (wrapper != null) {
            item(wrapper, s, parent.style().grid().areas());
        }
    }

    /** Lee {@code p} si es una propiedad del tipo del elemento ({@code Stack}, {@code Grid}…); {@code false} si no. */
    boolean property(Node p, Style s) {
        return false;
    }

    /** Lo que comprueba la familia de las propiedades ya leídas, antes de crear la caja. */
    void check(InlineNode n, InlineNode wrapper, Style s, CellPosition position) {
    }

    /** El contenido de la caja: por defecto, sus elementos hijos. */
    void content(BoxTreeBuilder builder, BuildContext ctx, InlineNode n, Box box) {
        builder.buildContent(ctx, n, box);
    }

    /**
     * Con {@code Markup: markdown} (reference/common/text-formatting.md), el texto de la caja es Markdown en línea:
     * pasa a ser el texto seguido, sin las marcas, y los tramos con formato van en {@link Box#formattedRuns()}. Dentro
     * de un {@code Markdown} no hace nada, porque sus textos ya vienen interpretados. El texto deja de ser un bloque:
     * sus errores señalan la línea donde empieza.
     */
    private static void inlineFormat(BuildContext ctx, Box box) {
        if (box.style().typography().markup() != Markup.MARKDOWN || ctx.insideMarkdown) {
            return;
        }
        int first = box.textBlock() ? box.textLine() + 1 : box.textLine();
        List<TextRun> runs = MarkdownBuilder.inline(box.text(), first);
        box.setText(TextRun.text(runs), first, false);
        box.setRuns(TextRun.formatted(runs) ? runs : null);
    }

    /**
     * El código en línea (reference/common/text-formatting.md) se escribe con la familia de {@code Code font}, propia
     * o heredada, que solo conoce la caja: sus tramos la reciben aquí. Sin ella es un error, con la línea del código.
     */
    private static void codeFont(Box box) {
        List<TextRun> runs = box.formattedRuns();
        if (runs == null || runs.stream().noneMatch(r -> r.format().code())) {
            return;
        }
        String family = box.style().typography().codeFont();
        List<TextRun> resolved = new ArrayList<>(runs.size());
        int index = 0;
        for (TextRun r : runs) {
            if (r.format().code()) {
                if (family == null) {
                    throw error(box.textLineAt(index), ErrorCode.CODE_WITHOUT_FONT, "El código necesita Code font: "
                            + "se declara en el Typography del texto, de un antecesor o de Styles (For element: Document), "
                            + "como Code font: IBM Plex Mono");
                }
                r = new TextRun(r.text(), r.format().withFamily(family));
            }
            resolved.add(r);
            index += r.text().length();
        }
        box.setRuns(List.copyOf(resolved));
    }

    /**
     * La ruta de una {@code Image} (reference/components/image.md), relativa a {@code images/}. Que el fichero exista
     * y sea una imagen se comprueba al maquetar, porque hace falta la carpeta de recursos.
     */
    private static void image(InlineNode n, Box box) {
        String path = n.getText().trim();
        if (path.isEmpty()) {
            throw error(n, ErrorCode.IMAGE_UNKNOWN, "Image necesita la ruta de la imagen, relativa a la carpeta "
                    + "images/ de los recursos: Image: logos/acme.png");
        }
        box.setImage(path);
    }

    /**
     * El texto de un {@code Text} o un {@code Paragraph}: en su valor o en {@code Content}, uno de los dos, y sin líneas en
     * blanco, que lo partirían en párrafos (reference/components/text.md). En un texto preformateado, una línea en blanco
     * es contenido y sí vale, salvo en un {@code Paragraph}, que es siempre un solo párrafo.
     */
    private static void text(InlineNode n, Box box) {
        Optional<Node> content = child(n, "content");
        boolean value = !n.getText().isBlank();
        if (value && content.isPresent()) {
            throw error(content.get(), ErrorCode.TEXT_CONTENT, n.getName() + " tiene el texto en el valor y en Content "
                    + "(línea " + n.getLine() + "): va en uno solo");
        }
        if (!value && content.isEmpty()) {
            throw error(n, ErrorCode.TEXT_CONTENT, n.getName() + " necesita su texto: en el valor ("
                    + n.getName() + ": …) o en un bloque Content >>");
        }
        Node source = content.orElse(n);
        font(n, source, box);
        boolean paragraph = box.element() == Element.PARAGRAPH;
        if (paragraph || !box.style().typography().preformatted().enabled()) {
            // Las líneas en blanco al principio o al final no separan nada.
            String text = source.getText();
            Matcher blank = BLANK_LINE.matcher(text)
                    .region(text.length() - text.stripLeading().length(), text.stripTrailing().length());
            if (blank.find()) {
                // El bloque empieza en la línea siguiente a Content; la línea en blanco, tras el salto encontrado.
                int line = source.getLine() + 2 + (int) text.substring(0, blank.start()).chars().filter(c -> c == '\n').count();
                throw error(line, ErrorCode.TEXT_BLANK_LINE, paragraph
                        ? "Un Paragraph es un solo párrafo: sobra la línea en blanco, o va un Paragraph para cada uno"
                        : "Un Text no tiene párrafos: sobra la línea en blanco, o va un Paragraph para cada párrafo "
                        + "(en un Block si van juntos). En un texto preformateado (Preformatted: true) sí vale");
            }
        }
        box.setText(source.getText(), source.getLine(), source instanceof TextNode);
    }
}
