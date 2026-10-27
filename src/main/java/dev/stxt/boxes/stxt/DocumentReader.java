package dev.stxt.boxes.stxt;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.Parser;
import dev.stxt.schema.SchemaProviderMemory;
import dev.stxt.schema.SchemaValidator;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Lee un documento de cajas con la librería oficial {@code dev.stxt:stxt-core} y lo valida
 * contra el schema {@code src/main/resources/stxt/dev.stxt.boxes.stxt} ({@code @stxt.schema}). Así
 * los errores de sintaxis, estructura (qué hijos admite cada elemento, cardinalidades) y tipos
 * (ENUM, INTEGER, BOOLEAN) los detecta la librería con sus códigos estándar
 * ({@link dev.stxt.exceptions.ParseException}), y el motor solo tiene que comprobar las reglas
 * que el schema no puede expresar (ESPECIFICACION.md §6).
 *
 * <p>La librería solo valida los nodos que tienen namespace, así que el documento debe tener un
 * único nodo raíz {@code Document} que declare {@code (dev.stxt.boxes)}: sin el namespace pasaría
 * sin validar, y el schema admite cualquier elemento como raíz.</p>
 *
 * <p>Es reutilizable y seguro entre hilos: cada lectura crea su propio {@link Parser}; el
 * schema se compila una sola vez.</p>
 */
public final class DocumentReader {

    /** Namespace del modelo de cajas, el mismo que define el schema. */
    public static final String NAMESPACE = "dev.stxt.boxes";

    private static final String SCHEMA_RESOURCE = "/stxt/dev.stxt.boxes.stxt";

    private final SchemaProviderMemory schemas;

    public DocumentReader() {
        schemas = new SchemaProviderMemory();
        schemas.addSchema(loadSchema());
    }

    /** Lee y valida un documento; devuelve su nodo raíz. */
    public InlineNode read(String content) {
        return root(parser().parse(content));
    }

    /** Lee y valida un documento desde un fichero UTF-8; devuelve su nodo raíz. */
    public InlineNode read(Path file) {
        return root(parser().parseFile(file.toFile()));
    }

    private Parser parser() {
        Parser parser = new Parser();
        parser.registerValidator(new SchemaValidator(schemas));
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

    private static String loadSchema() {
        try (InputStream in = DocumentReader.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encuentra el schema " + SCHEMA_RESOURCE + " en el classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede leer el schema " + SCHEMA_RESOURCE, e);
        }
    }
}
