package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;

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

import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.ins.Ins;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.node.BlockQuote;
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

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.TextNode;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

/**
 * Translates a {@code Markdown} (reference/components/markdown.md) into the STXT nodes of the elements that already
 * exist: headings to {@code Heading 1}...{@code Heading 6} with a {@code Text}, paragraphs to {@code Paragraph}, lists
 * to {@code List} and {@code List item}, GFM tables to {@code Table}, code blocks to a {@code Text} with
 * {@code Preformatted: true} and images alone in their paragraph to {@code Image} (reference/components/image.md). A
 * link is an underlined run with its target (reference/common/links.md); inline code, a code run, which gets the
 * family of {@code Code font} when its box is built. {@link BoxTreeBuilder} then builds those nodes like any other
 * element, so the schema, the rules of {@code Styles}, the validation of reference/common/errors.md, the tagged PDF and
 * the bookmarks treat them the same way.
 *
 * <p>Inline formatting (bold, italic, underline, strikethrough and link) does not fit in an STXT node: the text of the
 * node is the plain text and its runs go separately, in {@link Translation#runs()}.</p>
 *
 * <p>Only a closed subset is allowed; the rest is an error ({@link ErrorCode#MARKDOWN_UNSUPPORTED}). Each generated
 * node carries the line of the document where its Markdown block begins; the marker of a list, that of
 * {@code Bullet markers} or {@code Number markers}, and the columns of a table, that of {@code Table columns}.</p>
 */
final class MarkdownBuilder {

    /** Thread-safe (guaranteed by commonmark-java). */
    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(TablesExtension.create(),
                    StrikethroughExtension.builder().requireTwoTildes(true).build(), InsExtension.create()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();

    /**
     * The one of a text with {@code Markup: markdown}: no blocks or tables, so what would begin a block in a
     * {@code Markdown} ({@code -}, {@code 2024.}, {@code #}, {@code >}, {@code ---}) is text
     * (reference/common/text-formatting.md).
     */
    private static final Parser INLINE_PARSER = Parser.builder()
            .enabledBlockTypes(Set.of())
            .extensions(List.of(StrikethroughExtension.builder().requireTwoTildes(true).build(), InsExtension.create()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();

    /** The {@code {#id}} at the end of the text of a heading. */
    private static final Pattern HEADING_ID = Pattern.compile("\\{#([^{}]*)}\\s*$");

    private static final String SUBSET = "only headings, paragraphs, lists, tables, code blocks, "
            + "images alone in their paragraph and, inline, **bold**, *italic*, ++underline++, ~~strikethrough~~, "
            + "`code` and [links](target) are allowed";

    private static final String INLINE_SUBSET = "a text with Markup: markdown only allows text with **bold**, "
            + "*italic*, ++underline++, ~~strikethrough~~ and [links](target); the rest goes in a Markdown, and a special "
            + "character is written with \\ before it (\\*)";

    /**
     * The generated elements, in order, and the runs of the formatted texts, by node ({@code Text}, {@code Paragraph}
     * or {@code Cell}).
     */
    record Translation(List<InlineNode> elements, Map<Node, List<TextRun>> runs, Set<Node> codeBlocks) {
    }

    private MarkdownBuilder() {
    }

    static Translation translate(InlineNode markdown) {
        Node content = child(markdown, "content").orElseThrow();
        // The Content >> block begins on the line after its own.
        Translator t = new Translator(markdown, content.getLine() + 1, false, content.getText().split("\n", -1));
        List<InlineNode> elements = t.blocks(PARSER.parse(content.getText()), false);
        if (elements.isEmpty()) {
            throw error(content, ErrorCode.TEXT_CONTENT, "Markdown needs content in Content");
        }
        // Safety net: what is generated conforms to the schema, like what the author writes.
        elements.forEach(DocumentSchema::validate);
        return new Translation(List.copyOf(elements), t.runs, t.codeBlocks);
    }

    /**
     * A text with {@code Markup: markdown} (reference/common/text-formatting.md): its runs. Only paragraphs, which are
     * separated by a blank line, as in a {@code Text}; block syntax is text, and what is not allowed inline,
     * {@link ErrorCode#MARKDOWN_UNSUPPORTED}.
     *
     * @param firstLine line of the document where the text begins
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
        /** The {@code Text} nodes that are code blocks: the builder gives them the {@code Code block} element. */
        private final Set<Node> codeBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
        /** Bulleted and numbered lists that contain the block being translated. */
        private int bulletDepth;
        private int numberingDepth;
        /** It is the inline text of a {@code Markup: markdown}, not a {@code Markdown} element. */
        private final boolean inlineOnly;
        /** The lines of the Markdown, to count the cells of a table row, which the parser does not keep. */
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

        /** The child blocks of {@code parent}; {@code inItem} if they are inside a list item. */
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
                    throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "A heading cannot be inside a "
                            + "list item, as in a List item");
                }
                InlineNode heading = node("Heading " + h.getLevel(), "", line);
                String id = headingId(h, line);
                if (id != null) {
                    heading.addChild(node("Id", id, line));
                }
                heading.addChild(text("Text", h, line, "A Markdown heading needs text"));
                return heading;
            }
            if (b instanceof Paragraph p) {
                if (p.getFirstChild() instanceof Image i && i.getNext() == null) {
                    return image(i, line);
                }
                return text("Paragraph", p, line, "A Markdown paragraph needs text");
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
         * A code block (fenced or indented): a {@code Text} with {@code Preformatted: true}, with the code in a
         * {@code Content}; it is a {@code Code block} and its font is its {@code Code font} ({@link ElementReader}).
         * The language tag of the fences (<code>```java</code>) would have no effect, because there is no highlighting,
         * and it is an error.
         */
        private InlineNode codeBlock(org.commonmark.node.Node b, int line) {
            boolean fenced = b instanceof FencedCodeBlock;
            String literal = fenced ? ((FencedCodeBlock) b).getLiteral() : ((IndentedCodeBlock) b).getLiteral();
            if (fenced && !((FencedCodeBlock) b).getInfo().isBlank()) {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown does not allow the language tag of a "
                        + "code block (" + ((FencedCodeBlock) b).getInfo().strip() + "): there is no highlighting, so it is "
                        + "not needed");
            }
            if (literal.endsWith("\n")) {
                literal = literal.substring(0, literal.length() - 1);
            }
            if (literal.isBlank()) {
                throw error(line, ErrorCode.TEXT_CONTENT, "A Markdown code block needs text");
            }
            InlineNode text = node("Text", "", line);
            InlineNode typography = node("Typography", "", line);
            typography.addChild(node("Preformatted", "true", line));
            text.addChild(typography);
            // The Content >> goes on the line of the fences; when indented, on the line before the first line.
            text.addChild(new TextNode("Content", DocumentSchema.NAMESPACE, List.of(literal.split("\n", -1)),
                    fenced ? line : line - 1));
            codeBlocks.add(text);
            return text;
        }

        /**
         * An image alone in its paragraph: an {@code Image} with the path and, if it has one, its alternative text
         * (without formatting) in {@code Alt}; without it, it is decorative. The title ({@code "..."}) would have no
         * effect and is an error.
         */
        private InlineNode image(Image i, int line) {
            if (i.getTitle() != null && !i.getTitle().isEmpty()) {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown does not allow the title of an image "
                        + "(\"" + i.getTitle() + "\"): the image is described in its alternative text, ![...]");
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
                throw error(line, ErrorCode.MARKDOWN_PROPERTY_MISSING, (numbered ? "The numbered list" : "The list")
                        + " needs " + (numbered ? "Number markers" : "Bullet markers") + " in its Markdown (line "
                        + markdown.getLine() + "): the marker of each level, as in "
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
                throw error(line, ErrorCode.MARKDOWN_PROPERTY_MISSING, "The table needs Table columns in its "
                        + "Markdown (line " + markdown.getLine() + "): equal or auto");
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
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "A Markdown table needs at least one row "
                        + "below the header");
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
         * A row with more cells than the header is an error (reference/components/markdown.md#tables): the parser
         * discards the extra ones, so they are counted in the text of the row, separated by unescaped {@code |}.
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
                throw error(line(row), ErrorCode.MARKDOWN_UNSUPPORTED, "The row has " + cells + " cells and the "
                        + "header " + columns + ": the extra cells would be discarded by Markdown without warning");
            }
        }

        /** {@code Header rows} or {@code Body} with its rows; cells have no line of their own: that of their row. */
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

        /** With text, a {@code Cell} with a value and the alignment of its column; empty, an empty {@code Cell}. */
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

        /** A {@code Text} or a {@code Paragraph} with the text of a block and its runs. */
        /**
         * The {@code Id} of a heading, written at the end of its text as {@code {#id}}
         * (reference/components/markdown.md), which is removed from the text; {@code null} if it has none. An empty
         * {@code {#...}} or one with spaces is an error.
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
                throw error(line, ErrorCode.VALUE_NOT_ALLOWED, "The Id of a Markdown heading, {#" + id + "}, "
                        + "needs a name without spaces: ## Sales {#sales}");
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

        /** The text of the children of {@code parent}, with the formatting its ancestors accumulate. */
        void inline(org.commonmark.node.Node parent, TextFormat f, TextRun.Joiner out, int line) {
            for (org.commonmark.node.Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
                if (n instanceof Text t) {
                    out.add(t.getLiteral(), f);
                } else if (n instanceof SoftLineBreak) {
                    // A single line break counts as a space, as in a Text (reference/common/typography.md).
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
         * The formatting of the runs of a link (reference/common/links.md): the outer one, underlined and with its
         * target. A Markdown link is the only one that is underlined by itself. The title ({@code "..."}) would have no
         * effect and is an error, like that of an image.
         */
        private static TextFormat link(Link l, TextFormat f, int line) {
            if (l.getTitle() != null && !l.getTitle().isEmpty()) {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown does not allow the title of a link (\""
                        + l.getTitle() + "\"): it is not needed, [text](target)");
            }
            String destination = l.getDestination().strip();
            Links.problem(destination).ifPresent(problem -> {
                throw error(line, ErrorCode.LINK_SCHEME, "Markdown link: " + problem);
            });
            return new TextFormat(f.bold(), f.italic(), true, f.strike(), destination);
        }

        /**
         * Markers of {@code Bullet markers} (unnumbered) or of {@code Number markers} (numbered), one per level.
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
                    // Reported below, with the allowed values.
                }
                if (marker == null || marker.numbered() != numbered) {
                    throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Value not allowed in " + n.getName() + ": " + token
                            + (numbered ? " (decimal, lower-alpha, upper-alpha, lower-roman or upper-roman)"
                                    : " (bullet, dash or none)"));
                }
                markers.add(token.toLowerCase(Locale.ROOT));
            }
            if (markers.isEmpty()) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, n.getName() + " needs at least one marker");
            }
            return markers;
        }

        InvalidModelException unsupported(org.commonmark.node.Node n, int line) {
            String what;
            if (n instanceof Image) {
                what = inlineOnly ? "images (an image goes in an Image)"
                        : "images inside a text (an image goes alone in its paragraph)";
            } else if (n instanceof Code || n instanceof FencedCodeBlock || n instanceof IndentedCodeBlock) {
                what = "code";
            } else if (n instanceof BlockQuote) {
                what = "quotes (>)";
            } else if (n instanceof ThematicBreak) {
                what = "horizontal rules";
            } else if (n instanceof HtmlBlock || n instanceof HtmlInline) {
                what = "HTML";
            } else if (n instanceof HardLineBreak) {
                what = "forced line breaks (two spaces or \\ at the end of the line)";
            } else if (n instanceof LinkReferenceDefinition) {
                what = "link definitions";
            } else {
                what = n.getClass().getSimpleName();
            }
            return inlineOnly
                    ? error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markup: markdown does not allow " + what + ": " + INLINE_SUBSET)
                    : error(line, ErrorCode.MARKDOWN_UNSUPPORTED, "Markdown does not allow " + what + ": " + SUBSET);
        }

        /** Line of the document where the block begins; that of the {@code Content} if it has none. */
        int line(org.commonmark.node.Node b) {
            return b.getSourceSpans().isEmpty() ? firstLine - 1 : firstLine + b.getSourceSpans().get(0).getLineIndex();
        }

        private static InlineNode node(String name, String value, int line) {
            return new InlineNode(name, DocumentSchema.NAMESPACE, value, line);
        }
    }
}
