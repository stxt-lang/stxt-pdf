package dev.stxt.boxes.stxt;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.Parser;
import dev.stxt.schema.SchemaValidator;
import dev.stxt.boxes.model.InternalException;
import dev.stxt.boxes.resources.ResourceException;
import dev.stxt.boxes.tree.DocumentSchema;
import dev.stxt.boxes.tree.DocumentSyntaxException;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.exceptions.ParseException;
import dev.stxt.exceptions.STXTException;
import dev.stxt.exceptions.STXTIOException;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lee un documento de cajas con la librería oficial {@code dev.stxt:stxt-core} y lo valida
 * contra el schema ({@link DocumentSchema}) mientras lo lee. Así los errores de sintaxis,
 * estructura (qué hijos admite cada elemento, cardinalidades) y tipos (ENUM, INTEGER, BOOLEAN)
 * los detecta la librería con sus códigos estándar ({@link dev.stxt.exceptions.ParseException}),
 * y el motor solo tiene que comprobar las reglas que el schema no puede expresar
 * (ESPECIFICACION.md §6).
 *
 * <p>La librería solo valida los nodos que tienen namespace, así que el documento debe tener un
 * único nodo raíz {@code Document} que declare {@code (dev.stxt.boxes)}: sin el namespace pasaría
 * sin validar, y el schema admite cualquier elemento como raíz.</p>
 *
 * <p>Los errores de la librería salen con el tipo del motor ({@link dev.stxt.boxes.model.StxtPdfException}):
 * {@link DocumentSyntaxException} con su código y su línea si el documento no es válido, y
 * {@link ResourceException} si el fichero no se puede leer.</p>
 *
 * <p>Es reutilizable y seguro entre hilos: cada lectura crea su propio {@link Parser}.</p>
 */
public final class DocumentReader {

    /** Namespace del modelo de cajas, el mismo que define el schema. */
    public static final String NAMESPACE = DocumentSchema.NAMESPACE;

    /** Lee y valida un documento; devuelve su nodo raíz. */
    public InlineNode read(String content) {
        return root(parse(() -> parser().parse(content)));
    }

    /** Lee y valida un documento desde un fichero UTF-8; devuelve su nodo raíz. */
    public InlineNode read(Path file) {
        return root(parse(() -> parser().parseFile(file.toFile())));
    }

    /**
     * Lee y valida un documento de estilos aparte (raíz {@code Styles}, ESPECIFICACION.md §4.10), que se
     * pasa junto al documento al maquetarlo; devuelve su nodo raíz.
     */
    public InlineNode readStyles(String content) {
        return styles(parse(() -> parser().parse(content)));
    }

    /** Lee y valida un documento de estilos aparte desde un fichero UTF-8; devuelve su nodo raíz. */
    public InlineNode readStyles(Path file) {
        return styles(parse(() -> parser().parseFile(file.toFile())));
    }

    /** Lo que lee la librería, con los errores de la librería convertidos a los del motor. */
    private static List<Node> parse(Supplier<List<Node>> parsing) {
        try {
            return parsing.get();
        } catch (ParseException e) {
            throw new DocumentSyntaxException(e);
        } catch (STXTIOException e) {
            throw new ResourceException("No se puede leer el documento: " + e.getMessage(), e);
        } catch (STXTException e) {
            throw new InternalException("Fallo de la librería STXT: " + e.getMessage(), e);
        }
    }

    private static Parser parser() {
        Parser parser = new Parser();
        parser.registerValidator(new SchemaValidator(DocumentSchema.provider()));
        return parser;
    }

    private static InlineNode styles(List<Node> roots) {
        if (roots.size() != 1) {
            int line = roots.size() > 1 ? roots.get(1).getLine() : 1;
            throw new InvalidModelException(line, ErrorCode.DOCUMENT_ROOT, "El documento de estilos debe tener "
                    + "exactamente un nodo raíz y tiene " + roots.size(), "los estilos");
        }
        Node root = roots.get(0);
        if (!NAMESPACE.equals(root.getNamespace()) || !root.getCanonicalName().equals("styles")
                || !(root instanceof InlineNode styles)) {
            throw new InvalidModelException(root.getLine(), ErrorCode.DOCUMENT_ROOT,
                    "El nodo raíz del documento de estilos debe ser Styles (" + NAMESPACE + "):", "los estilos");
        }
        return styles;
    }

    private static InlineNode root(List<Node> roots) {
        if (roots.size() != 1) {
            int line = roots.size() > 1 ? roots.get(1).getLine() : 1;
            throw new InvalidModelException(line, ErrorCode.DOCUMENT_ROOT,
                    "El documento debe tener exactamente un nodo raíz y tiene " + roots.size());
        }
        Node root = roots.get(0);
        if (!NAMESPACE.equals(root.getNamespace()) || !root.getCanonicalName().equals("document")
                || !(root instanceof InlineNode document)) {
            throw new InvalidModelException(root.getLine(), ErrorCode.DOCUMENT_ROOT,
                    "El nodo raíz debe ser Document (" + NAMESPACE + "):");
        }
        return document;
    }
}
