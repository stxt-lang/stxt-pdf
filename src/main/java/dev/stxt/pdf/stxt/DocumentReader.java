package dev.stxt.pdf.stxt;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.Parser;
import dev.stxt.schema.SchemaValidator;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.StxtPdfException;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.tree.DocumentSchema;
import dev.stxt.pdf.tree.DocumentSyntaxException;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.SourceLine;
import dev.stxt.exceptions.ParseException;
import dev.stxt.exceptions.STXTException;
import dev.stxt.exceptions.STXTIOException;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * Reads a document of boxes with the official {@code dev.stxt:stxt-core} library and validates it
 * against the schema ({@link DocumentSchema}) while reading it. This way the errors of syntax,
 * structure (which children each element allows, cardinalities) and types (ENUM, INTEGER, BOOLEAN)
 * are detected by the library with its standard codes ({@link dev.stxt.exceptions.ParseException}),
 * and the engine only has to check the rules the schema cannot express
 * (reference/common/errors.md).
 *
 * <p>The library only validates the nodes that have a namespace, so the document must have a
 * single root node {@code Document} that declares {@code (dev.stxt.pdf)}: without the namespace it would pass
 * unvalidated, and the schema accepts any element as root.</p>
 *
 * <p>The errors of the library come out with the type of the engine ({@link dev.stxt.pdf.model.StxtPdfException}):
 * {@link DocumentSyntaxException} with its code and its line if the document is not valid, and
 * {@link ResourceException} if the file cannot be read.</p>
 *
 * <p>It is reusable and thread-safe: each read creates its own {@link Parser}.</p>
 */
public final class DocumentReader {

    /** Namespace of the box model, the same one the schema defines. */
    public static final String NAMESPACE = DocumentSchema.NAMESPACE;

    /** The main document, for the errors of reading it. */
    private static final SourceLine DOCUMENT = SourceLine.of(StxtPdfException.NO_LINE);

    /**
     * Reads and validates a document; returns its root node.
     *
     * @param content the STXT text of the document
     * @return the root node {@code Document}, ready for {@code Converter.layOut} or {@code Converter.toPdf}
     */
    public InlineNode read(String content) {
        return root(parse(() -> parser().parse(content), DOCUMENT));
    }

    /**
     * Reads and validates a document from a UTF-8 file; returns its root node.
     *
     * @param file the STXT file of the document, in UTF-8
     * @return the root node {@code Document}, ready for {@code Converter.layOut} or {@code Converter.toPdf}
     */
    public InlineNode read(Path file) {
        return root(parse(() -> parser().parseFile(file.toFile()), DOCUMENT));
    }

    /**
     * Reads and validates a separate styles document (root {@code Styles}, reference/common/styles.md), which is
     * passed along with the document when laying it out; returns its root node.
     *
     * @param content the STXT text of the styles document
     * @return the root node {@code Styles}, for {@code Converter.Builder.styles}
     */
    public InlineNode readStyles(String content) {
        return readStyles(content, 1);
    }

    /**
     * Reads and validates a separate styles document from a UTF-8 file; returns its root node.
     *
     * @param file the STXT file of the styles document, in UTF-8
     * @return the root node {@code Styles}, for {@code Converter.Builder.styles}
     */
    public InlineNode readStyles(Path file) {
        return readStyles(file, 1);
    }

    /**
     * Like {@link #readStyles(String)}, for the styles document that goes in the place {@code position} among those of
     * a conversion: an error of reading it says so ({@code StxtPdfException.stylesDocument()}), and from the second
     * one on, so does its message ({@code line 8 of styles 2}).
     *
     * @param content the STXT text of the styles document
     * @param position the place of the styles document among those of the conversion, from 1
     * @return the root node {@code Styles}, for {@code Converter.Builder.styles}
     */
    public InlineNode readStyles(String content, int position) {
        SourceLine source = source(position);
        return styles(parse(() -> parser().parse(content), source), source);
    }

    /**
     * Like {@link #readStyles(Path)}, for the styles document that goes in the place {@code position} among those of
     * a conversion ({@link #readStyles(String, int)}).
     *
     * @param file the STXT file of the styles document, in UTF-8
     * @param position the place of the styles document among those of the conversion, from 1
     * @return the root node {@code Styles}, for {@code Converter.Builder.styles}
     */
    public InlineNode readStyles(Path file, int position) {
        SourceLine source = source(position);
        return styles(parse(() -> parser().parseFile(file.toFile()), source), source);
    }

    /** The styles document in the place {@code position}, for its errors; the first one is not numbered. */
    private static SourceLine source(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("The place of a styles document starts at 1: " + position);
        }
        return SourceLine.inStyles(StxtPdfException.NO_LINE, position, position > 1);
    }

    /**
     * What the library reads, with the errors of the library turned into those of the engine; {@code source} is the
     * document that is read: the main one or a separate styles document.
     */
    private static List<Node> parse(Supplier<List<Node>> parsing, SourceLine source) {
        try {
            return parsing.get();
        } catch (ParseException e) {
            throw new DocumentSyntaxException(e, source);
        } catch (STXTIOException e) {
            throw new ResourceException("The document cannot be read: " + e.getMessage(), e);
        } catch (STXTException e) {
            throw new InternalException("Failure of the STXT library: " + e.getMessage(), e);
        }
    }

    private static Parser parser() {
        Parser parser = new Parser();
        parser.registerValidator(new SchemaValidator(DocumentSchema.provider()));
        return parser;
    }

    private static InlineNode styles(List<Node> roots, SourceLine source) {
        if (roots.size() != 1) {
            int line = roots.size() > 1 ? roots.get(1).getLine() : 1;
            throw new InvalidModelException(source.at(line), ErrorCode.DOCUMENT_ROOT, "The styles "
                    + "document must have exactly one root node and has " + roots.size());
        }
        Node root = roots.get(0);
        if (!NAMESPACE.equals(root.getNamespace()) || !root.getCanonicalName().equals("styles")
                || !(root instanceof InlineNode styles)) {
            throw new InvalidModelException(source.at(root.getLine()), ErrorCode.DOCUMENT_ROOT,
                    "The root node of the styles document must be Styles (" + NAMESPACE + "):");
        }
        return styles;
    }

    private static InlineNode root(List<Node> roots) {
        if (roots.size() != 1) {
            int line = roots.size() > 1 ? roots.get(1).getLine() : 1;
            throw new InvalidModelException(line, ErrorCode.DOCUMENT_ROOT,
                    "The document must have exactly one root node and has " + roots.size());
        }
        Node root = roots.get(0);
        if (!NAMESPACE.equals(root.getNamespace()) || !root.getCanonicalName().equals("document")
                || !(root instanceof InlineNode document)) {
            throw new InvalidModelException(root.getLine(), ErrorCode.DOCUMENT_ROOT,
                    "The root node must be Document (" + NAMESPACE + "):");
        }
        return document;
    }
}
