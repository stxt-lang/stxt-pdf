package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.ins.Ins;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;

/**
 * Traduce un {@code Markdown} (referencia/componentes/markdown.md) a los nodos STXT de los elementos que ya existen:
 * encabezados a {@code Heading 1}…{@code Heading 6} con un {@code Text}, párrafos a {@code Paragraph}, listas a {@code
 * List} y {@code List item}, tablas GFM a {@code Table}, bloques de código a un {@code Text} con {@code Preformatted:
 * true} e imágenes solas en su párrafo a {@code Image} (referencia/componentes/image.md). Un enlace es un tramo
 * subrayado con su destino (referencia/comun/enlaces.md); el código en línea, un tramo de código, que recibe la
 * familia de {@code Code font} al construir su caja. {@link BoxTreeBuilder} construye después esos nodos como
 * cualquier otro elemento, así que el schema, las reglas de {@code Styles}, la validación de
 * referencia/comun/errores.md, el PDF etiquetado y los marcadores los tratan igual.
 *
 * <p>El formato dentro de una línea (negrita, cursiva, subrayado, tachado y enlace) no cabe en un nodo
 * STXT: el texto del nodo es el texto seguido y sus tramos van aparte, en
 * {@link Translation#runs()}.</p>
 *
 * <p>Solo se admite un subconjunto cerrado; el resto es un error
 * ({@link ErrorCode#MARKDOWN_UNSUPPORTED}). Cada nodo generado lleva la línea del documento donde
 * empieza su bloque de Markdown; el marcador de una lista, la de {@code Bullet markers} o
 * {@code Number markers}, y las columnas de una tabla, la de {@code Table columns}.</p>
 */
final class MarkdownBuilder {

    /** Seguro entre hilos (lo garantiza commonmark-java). */
    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(TablesExtension.create(),
                    StrikethroughExtension.builder().requireTwoTildes(true).build(), InsExtension.create()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();

    /**
     * El de un texto con {@code Markup: markdown}: sin bloques ni tablas, así que lo que en un {@code Markdown}
     * empezaría un bloque ({@code -}, {@code 2024.}, {@code #}, {@code >}, {@code ---}) es texto
     * (referencia/comun/formato-del-texto.md).
     */
    private static final Parser INLINE_PARSER = Parser.builder()
            .enabledBlockTypes(Set.of())
            .extensions(List.of(StrikethroughExtension.builder().requireTwoTildes(true).build(), InsExtension.create()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();

    /** El {@code {#id}} al final del texto de un encabezado. */
    private static final Pattern HEADING_ID = Pattern.compile("\\{#([^{}]*)}\\s*$");

    private static final String SUBSET = "solo admite encabezados, párrafos, listas, tablas, bloques de código, "
            + "imágenes solas en su párrafo y, dentro de la línea, **negrita**, *cursiva*, ++subrayado++, ~~tachado~~, "
            + "`código` y [enlaces](destino)";

    private static final String INLINE_SUBSET = "un texto con Markup: markdown solo admite texto con **negrita**, "
            + "*cursiva*, ++subrayado++, ~~tachado~~ y [enlaces](destino); lo demás va en un Markdown, y un carácter "
            + "especial se escribe con \\ delante (\\*)";

    /**
     * Los elementos generados, en orden, y los tramos de los textos con formato, por nodo
     * ({@code Text}, {@code Paragraph} o {@code Cell}).
     */
    record Translation(List<InlineNode> elements, Map<Node, List<TextRun>> runs, Set<Node> codeBlocks) {
    }

    private MarkdownBuilder() {
    }

    static Translation translate(InlineNode markdown) {
        Node content = child(markdown, "content").orElseThrow();
        // El bloque Content >> empieza en la línea siguiente a la suya.
        Translator t = new Translator(markdown, content.getLine() + 1, false, content.getText().split("\n", -1));
        List<InlineNode> elements = t.blocks(PARSER.parse(content.getText()), false);
        if (elements.isEmpty()) {
            throw error(content, ErrorCode.TEXT_CONTENT, "Markdown necesita contenido en Content");
        }
        // Red de seguridad: lo generado cumple el schema, como lo que escribe el autor.
        elements.forEach(DocumentSchema::validate);
        return new Translation(List.copyOf(elements), t.runs, t.codeBlocks);
    }

    /**
     * Un texto con {@code Markup: markdown} (referencia/comun/formato-del-texto.md): sus tramos. Solo párrafos, que se
     * separan con una línea en blanco, como en un {@code Text}; la sintaxis de los bloques es texto, y lo que no se
     * admite dentro de la línea, {@link ErrorCode#MARKDOWN_UNSUPPORTED}.
     *
     * @param firstLine línea del documento donde empieza el texto
     */
    static List<TextRun> inline(String text, int firstLine) {
        Translator t = new Translator(null, firstLine, true, new String[0]);
        TextRun.Joiner out = new TextRun.Joiner();
        boolean first = true;
        for (org.commonmark.node.Node b = INLINE_PARSER.parse(text).getFirstChild(); b != null; b = b.getNext()) {
            int line = t.line(b);
            if (!(b instanceof Paragraph p)) {
                throw t.unsupported(b, line);
            }
            if (!first) {
                out.add("\n\n", TextFormat.PLAIN);
            }
            first = false;
            t.inline(p, TextFormat.PLAIN, out, line);
        }
        return out.build();
    }

    private static final class Translator {
        private final InlineNode markdown;
        private final int firstLine;
        private final Optional<Node> bullet;
        private final Optional<Node> numbering;
        private final Optional<Node> tableColumns;
        private final List<String> bullets;
        private final List<String> numberings;
        private final Map<Node, List<TextRun>> runs = new IdentityHashMap<>();
        /** Los {@code Text} que son bloques de código: el builder les da el elemento {@code Code block}. */
        private final Set<Node> codeBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
        /** Listas con viñetas y numeradas que contienen el bloque que se traduce. */
        private int bulletDepth;
        private int numberingDepth;
        /** Es el texto en línea de un {@code Markup: markdown}, no un elemento {@code Markdown}. */
        private final boolean inlineOnly;
        /** Las líneas del Markdown, para contar las celdas de una fila de tabla, que el parser no conserva. */
        private final String[] source;

        Translator(InlineNode markdown, int firstLine, boolean inlineOnly, String[] source) {
            this.markdown = markdown;
            this.source = source;
            this.firstLine = firstLine;
            this.inlineOnly = inlineOnly;
            this.bullet = markdown == null ? Optional.empty() : child(markdown, "bullet-markers");
            this.numbering = markdown == null ? Optional.empty() : child(markdown, "number-markers");
            this.tableColumns = markdown == null ? Optional.empty() : child(markdown, "table-columns");
            this.bullets = bullet.map(n -> markers(n, false)).orElse(List.of());
            this.numberings = numbering.map(n -> markers(n, true)).orElse(List.of());
        }

        /** Los bloques hijos de {@code parent}; {@code inItem} si están dentro de un ítem de lista. */
        List<InlineNode> blocks(org.commonmark.node.Node parent, boolean inItem) {
            List<InlineNode> out = new ArrayList<>();
            for (org.commonmark.node.Node b = parent.getFirstChild(); b != null; b = b.getNext()) {
                out.add(block(b, inItem));
            }
            return out;
        }

        private InlineNode block(org.commonmark.node.Node b, boolean inItem) {
            int line = line(b);
            if (b instanceof Heading h) {
                if (inItem) {
                    throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Un encabezado no puede estar dentro de un "
                            + "ítem de lista, como en un List item");
                }
                InlineNode heading = node("Heading " + h.getLevel(), "", line);
                String id = headingId(h, line);
                if (id != null) {
                    heading.addChild(node("Id", id, line));
                }
                heading.addChild(text("Text", h, line, "Un encabezado de Markdown necesita texto"));
                return heading;
            }
            if (b instanceof Paragraph p) {
                if (p.getFirstChild() instanceof Image i && i.getNext() == null) {
                    return image(i, line);
                }
                return text("Paragraph", p, line, "Un párrafo de Markdown necesita texto");
            }
            if (b instanceof ListBlock l) {
                return list(l, line);
            }
            if (b instanceof TableBlock t) {
                return table(t, line);
            }
            if (b instanceof FencedCodeBlock || b instanceof IndentedCodeBlock) {
                return codeBlock(b, line);
            }
            throw unsupported(b, line);
        }

        /**
         * Un bloque de código (con vallas o sangrado): un {@code Text} con {@code Preformatted: true}, con el código en
         * un {@code Content}; es un {@code Code block} y su fuente es su {@code Code font} ({@link ElementReader}). La
         * etiqueta de lenguaje de las vallas (<code>```java</code>) no tendría efecto, porque no hay resaltado, y es un
         * error.
         */
        private InlineNode codeBlock(org.commonmark.node.Node b, int line) {
            boolean fenced = b instanceof FencedCodeBlock;
            String literal = fenced ? ((FencedCodeBlock) b).getLiteral() : ((IndentedCodeBlock) b).getLiteral();
            if (fenced && !((FencedCodeBlock) b).getInfo().isBlank()) {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown no admite la etiqueta de lenguaje de un "
                        + "bloque de código (" + ((FencedCodeBlock) b).getInfo().strip() + "): no hay resaltado, así que "
                        + "sobra");
            }
            if (literal.endsWith("\n")) {
                literal = literal.substring(0, literal.length() - 1);
            }
            if (literal.isBlank()) {
                throw error(line, ErrorCode.TEXT_CONTENT, "Un bloque de código de Markdown necesita texto");
            }
            InlineNode text = node("Text", "", line);
            InlineNode typography = node("Typography", "", line);
            typography.addChild(node("Preformatted", "true", line));
            text.addChild(typography);
            // El Content >> va en la línea de las vallas; con sangría, en la anterior a la primera línea.
            text.addChild(new TextNode("Content", DocumentSchema.NAMESPACE, List.of(literal.split("\n", -1)),
                    fenced ? line : line - 1));
            codeBlocks.add(text);
            return text;
        }

        /**
         * Una imagen sola en su párrafo: una {@code Image} con la ruta y, si lo tiene, su texto
         * alternativo (sin formato) en {@code Alt}; sin él, es decorativa. El título
         * ({@code "…"}) no tendría efecto y es un error.
         */
        private InlineNode image(Image i, int line) {
            if (i.getTitle() != null && !i.getTitle().isEmpty()) {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown no admite el título de una imagen "
                        + "(\"" + i.getTitle() + "\"): la imagen se describe en su texto alternativo, ![...]");
            }
            InlineNode image = node("Image", i.getDestination(), line);
            String alt = TextRun.text(inline(i, line)).strip();
            if (!alt.isEmpty()) {
                image.addChild(node("Alt", alt, line));
            }
            return image;
        }

        private InlineNode list(ListBlock l, int line) {
            boolean numbered = l instanceof OrderedList;
            Optional<Node> property = numbered ? numbering : bullet;
            if (property.isEmpty()) {
                throw error(line, ErrorCode.MARKDOWN_PROPERTY_MISSING, (numbered ? "La lista numerada" : "La lista")
                        + " necesita " + (numbered ? "Number markers" : "Bullet markers") + " en su Markdown (línea "
                        + markdown.getLine() + "): el marcador de cada nivel, como "
                        + (numbered ? "Number markers: decimal lower-alpha" : "Bullet markers: bullet dash"));
            }
            List<String> markers = numbered ? numberings : bullets;
            int depth = numbered ? numberingDepth++ : bulletDepth++;
            InlineNode list = node("List", "", line);
            list.addChild(node("Marker", markers.get(Math.min(depth, markers.size() - 1)), property.get().getLine()));
            if (l instanceof OrderedList o && o.getMarkerStartNumber() != 1) {
                list.addChild(node("Start", Integer.toString(o.getMarkerStartNumber()), line));
            }
            for (org.commonmark.node.Node i = l.getFirstChild(); i != null; i = i.getNext()) {
                ListItem source = (ListItem) i;
                InlineNode item = node("List item", "", line(source));
                blocks(source, true).forEach(item::addChild);
                list.addChild(item);
            }
            if (numbered) {
                numberingDepth--;
            } else {
                bulletDepth--;
            }
            return list;
        }

        private InlineNode table(TableBlock t, int line) {
            if (tableColumns.isEmpty()) {
                throw error(line, ErrorCode.MARKDOWN_PROPERTY_MISSING, "La tabla necesita Table columns en su "
                        + "Markdown (línea " + markdown.getLine() + "): equal o auto");
            }
            TableHead head = null;
            TableBody body = null;
            for (org.commonmark.node.Node g = t.getFirstChild(); g != null; g = g.getNext()) {
                if (g instanceof TableHead h) {
                    head = h;
                } else if (g instanceof TableBody b) {
                    body = b;
                }
            }
            if (head == null || body == null) {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Una tabla de Markdown necesita al menos una fila "
                        + "debajo de la cabecera");
            }
            int columns = 0;
            for (org.commonmark.node.Node c = head.getFirstChild().getFirstChild(); c != null; c = c.getNext()) {
                columns++;
            }
            Node mode = tableColumns.get();
            String track = mode.getText().trim().equals("equal") ? "*" : "auto";
            InlineNode table = node("Table", "", line);
            table.addChild(node("Column sizes", "repeat(" + columns + ", " + track + ")", mode.getLine()));
            table.addChild(rows("Header rows", head));
            table.addChild(rows("Body", body));
            for (org.commonmark.node.Node r = body.getFirstChild(); r != null; r = r.getNext()) {
                extraCells(r, columns);
            }
            return table;
        }

        /**
         * Una fila con más celdas que la cabecera es un error (referencia/componentes/markdown.md#tablas): el parser
         * descarta las que sobran, así que se cuentan en el texto de la fila, separadas por {@code |} sin escapar.
         */
        private void extraCells(org.commonmark.node.Node row, int columns) {
            if (row.getSourceSpans().isEmpty()) {
                return;
            }
            String text = source[row.getSourceSpans().get(0).getLineIndex()].strip();
            int cells = 1;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '\\') {
                    i++;
                } else if (ch == '|' && i > 0 && i < text.length() - 1) {
                    cells++;
                }
            }
            if (cells > columns) {
                throw error(line(row), ErrorCode.MARKDOWN_UNSUPPORTED, "La fila tiene " + cells + " celdas y la "
                        + "cabecera " + columns + ": sobran celdas, que Markdown descartaría sin avisar");
            }
        }

        /** {@code Header rows} o {@code Body} con sus filas; las celdas no tienen línea propia: la de su fila. */
        private InlineNode rows(String name, org.commonmark.node.Node group) {
            InlineNode out = node(name, "", line(group));
            for (org.commonmark.node.Node r = group.getFirstChild(); r != null; r = r.getNext()) {
                int line = line(r);
                InlineNode row = node("Row", "", line);
                for (org.commonmark.node.Node c = r.getFirstChild(); c != null; c = c.getNext()) {
                    row.addChild(cell((TableCell) c, line));
                }
                out.addChild(row);
            }
            return out;
        }

        /** Con texto, un {@code Cell} con valor y la alineación de su columna; vacía, un {@code Cell} vacío. */
        private InlineNode cell(TableCell c, int line) {
            List<TextRun> text = inline(c, line);
            String value = TextRun.text(text);
            if (value.isBlank()) {
                return node("Cell", "", line);
            }
            InlineNode cell = node("Cell", value, line);
            keepRuns(cell, text);
            if (c.getAlignment() != null) {
                InlineNode typography = node("Typography", "", line);
                typography.addChild(node("Align", c.getAlignment().name().toLowerCase(Locale.ROOT), line));
                cell.addChild(typography);
            }
            return cell;
        }

        /** Un {@code Text} o un {@code Paragraph} con el texto de un bloque y sus tramos. */
        /**
         * El {@code Id} de un encabezado, escrito al final de su texto como {@code {#id}}
         * (referencia/componentes/markdown.md), que se quita del texto; {@code null} si no lo lleva. Un {@code {#…}}
         * vacío o con espacios es un error.
         */
        private String headingId(Heading h, int line) {
            if (!(h.getLastChild() instanceof Text last)) {
                return null;
            }
            Matcher m = HEADING_ID.matcher(last.getLiteral());
            if (!m.find()) {
                return null;
            }
            String id = m.group(1);
            if (id.isEmpty() || !id.equals(id.strip()) || id.chars().anyMatch(Character::isWhitespace)) {
                throw error(line, ErrorCode.VALUE_NOT_ALLOWED, "El Id de un encabezado de Markdown, {#" + id + "}, "
                        + "necesita un nombre sin espacios: ## Ventas {#ventas}");
            }
            String rest = last.getLiteral().substring(0, m.start()).stripTrailing();
            if (rest.isEmpty()) {
                last.unlink();
            } else {
                last.setLiteral(rest);
            }
            return id;
        }

        private InlineNode text(String name, org.commonmark.node.Node block, int line, String empty) {
            List<TextRun> text = inline(block, line);
            String value = TextRun.text(text);
            if (value.isBlank()) {
                throw error(line, ErrorCode.TEXT_CONTENT, empty);
            }
            InlineNode node = node(name, value, line);
            keepRuns(node, text);
            return node;
        }

        private void keepRuns(InlineNode node, List<TextRun> text) {
            if (TextRun.formatted(text)) {
                runs.put(node, text);
            }
        }

        private List<TextRun> inline(org.commonmark.node.Node block, int line) {
            TextRun.Joiner out = new TextRun.Joiner();
            inline(block, TextFormat.PLAIN, out, line);
            return out.build();
        }

        /** El texto de los hijos de {@code parent}, con el formato que acumulan sus antecesores. */
        void inline(org.commonmark.node.Node parent, TextFormat f, TextRun.Joiner out, int line) {
            for (org.commonmark.node.Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
                if (n instanceof Text t) {
                    out.add(t.getLiteral(), f);
                } else if (n instanceof SoftLineBreak) {
                    // Un salto de línea simple vale un espacio, como en un Text (referencia/comun/tipografia.md).
                    out.add(" ", f);
                } else if (n instanceof StrongEmphasis) {
                    inline(n, new TextFormat(true, f.italic(), f.underline(), f.strike(), f.link()), out, line);
                } else if (n instanceof Emphasis) {
                    inline(n, new TextFormat(f.bold(), true, f.underline(), f.strike(), f.link()), out, line);
                } else if (n instanceof Ins) {
                    inline(n, new TextFormat(f.bold(), f.italic(), true, f.strike(), f.link()), out, line);
                } else if (n instanceof Strikethrough) {
                    inline(n, new TextFormat(f.bold(), f.italic(), f.underline(), true, f.link()), out, line);
                } else if (n instanceof Link l) {
                    inline(n, link(l, f, line), out, line);
                } else if (n instanceof Code c) {
                    out.add(c.getLiteral(), f.asCode());
                } else {
                    throw unsupported(n, line);
                }
            }
        }

        /**
         * El formato de los tramos de un enlace (referencia/comun/enlaces.md): el de fuera, subrayado y con su destino.
         * Un enlace de Markdown es el único que se subraya solo. El título ({@code "…"}) no
         * tendría efecto y es un error, como el de una imagen.
         */
        private static TextFormat link(Link l, TextFormat f, int line) {
            if (l.getTitle() != null && !l.getTitle().isEmpty()) {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown no admite el título de un enlace (\""
                        + l.getTitle() + "\"): sobra, [texto](destino)");
            }
            String destination = l.getDestination().strip();
            Links.problem(destination).ifPresent(problem -> {
                throw error(line, ErrorCode.LINK_SCHEME, "Enlace de Markdown: " + problem);
            });
            return new TextFormat(f.bold(), f.italic(), true, f.strike(), destination);
        }

        /**
         * Marcadores de {@code Bullet markers} (sin numerar) o de {@code Number markers} (numerados), uno por nivel.
         */
        private static List<String> markers(Node n, boolean numbered) {
            List<String> markers = new ArrayList<>();
            for (String token : n.getText().trim().split("\\s+")) {
                if (token.isEmpty()) {
                    continue;
                }
                Marker marker = null;
                try {
                    marker = Enum.valueOf(Marker.class, token.toUpperCase(Locale.ROOT).replace('-', '_'));
                } catch (IllegalArgumentException e) {
                    // Se informa abajo, con los valores admitidos.
                }
                if (marker == null || marker.numbered() != numbered) {
                    throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Valor no permitido en " + n.getName() + ": " + token
                            + (numbered ? " (decimal, lower-alpha, upper-alpha, lower-roman o upper-roman)"
                                    : " (bullet, dash o none)"));
                }
                markers.add(token.toLowerCase(Locale.ROOT));
            }
            if (markers.isEmpty()) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " necesita al menos un marcador");
            }
            return markers;
        }

        InvalidModelException unsupported(org.commonmark.node.Node n, int line) {
            String what;
            if (n instanceof Image) {
                what = inlineOnly ? "imágenes (una imagen va en un Image)"
                        : "imágenes dentro de un texto (una imagen va sola en su párrafo)";
            } else if (n instanceof Code || n instanceof FencedCodeBlock || n instanceof IndentedCodeBlock) {
                what = "código";
            } else if (n instanceof BlockQuote) {
                what = "citas (>)";
            } else if (n instanceof ThematicBreak) {
                what = "líneas horizontales";
            } else if (n instanceof HtmlBlock || n instanceof HtmlInline) {
                what = "HTML";
            } else if (n instanceof HardLineBreak) {
                what = "saltos de línea forzados (dos espacios o \\ al final de la línea)";
            } else if (n instanceof LinkReferenceDefinition) {
                what = "definiciones de enlaces";
            } else {
                what = n.getClass().getSimpleName();
            }
            return inlineOnly
                    ? error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markup: markdown no admite " + what + ": " + INLINE_SUBSET)
                    : error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown no admite " + what + ": " + SUBSET);
        }

        /** Línea del documento donde empieza el bloque; la del {@code Content} si no la tiene. */
        int line(org.commonmark.node.Node b) {
            return b.getSourceSpans().isEmpty() ? firstLine - 1 : firstLine + b.getSourceSpans().get(0).getLineIndex();
        }

        private static InlineNode node(String name, String value, int line) {
            return new InlineNode(name, DocumentSchema.NAMESPACE, value, line);
        }
    }
}
