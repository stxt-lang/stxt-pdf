package dev.stxt.boxes.stxt;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.Parser;
import dev.stxt.schema.SchemaValidator;
import dev.stxt.boxes.tree.DocumentSchema;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;

import java.nio.file.Path;
import java.util.List;

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
 * <p>Es reutilizable y seguro entre hilos: cada lectura crea su propio {@link Parser}.</p>
 */
public final class DocumentReader {

    /** Namespace del modelo de cajas, el mismo que define el schema. */
    public static final String NAMESPACE = DocumentSchema.NAMESPACE;

    /** Lee y valida un documento; devuelve su nodo raíz. */
    public InlineNode read(String content) {
        return root(parser().parse(content));
    }

    /** Lee y valida un documento desde un fichero UTF-8; devuelve su nodo raíz. */
    public InlineNode read(Path file) {
        return root(parser().parseFile(file.toFile()));
    }

    private static Parser parser() {
        Parser parser = new Parser();
        parser.registerValidator(new SchemaValidator(DocumentSchema.provider()));
        return parser;
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
