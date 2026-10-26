package dev.stxt.boxes.stxt;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.Parser;
import dev.stxt.TextNode;
import dev.stxt.exceptions.ParseException;
import dev.stxt.exceptions.STXTException;
import dev.stxt.schema.SchemaValidator;
import dev.stxt.template.TemplateSchemaProviderMemory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Adaptador a la librería oficial {@code dev.stxt:stxt-core}.
 *
 * <p>Parsea el documento, lo valida contra el template
 * {@code src/main/resources/stxt/dev.stxt.boxes.stxt} y convierte el resultado a
 * {@link StxtNode}. Así los errores de estructura y de tipos (ENUM, INTEGER, BOOLEAN,
 * cardinalidades) los detecta la librería con sus códigos estándar, y el motor solo tiene que
 * comprobar las reglas que el template no puede expresar (ESPECIFICACION.md §6).</p>
 *
 * <p>La librería solo valida los nodos que tienen namespace, así que el nodo raíz debe declarar
 * {@code (dev.stxt.boxes)}: sin él, el documento pasaría sin validar.</p>
 *
 * <p>Es reutilizable y seguro entre hilos: cada lectura crea su propio {@link Parser}; el
 * template se compila una sola vez.</p>
 */
public final class StxtCoreReader implements DocumentReader {

    /** Namespace del modelo de cajas, el mismo que define el template. */
    public static final String NAMESPACE = "dev.stxt.boxes";

    private static final String TEMPLATE_RESOURCE = "/stxt/dev.stxt.boxes.stxt";

    private final TemplateSchemaProviderMemory schemas;

    public StxtCoreReader() {
        schemas = new TemplateSchemaProviderMemory();
        schemas.addTemplate(loadTemplate());
    }

    @Override
    public StxtNode read(String content, String source) {
        Parser parser = new Parser();
        parser.registerValidator(new SchemaValidator(schemas));
        List<Node> roots;
        try {
            roots = parser.parse(content);
        } catch (ParseException e) {
            throw new StxtSyntaxException(source, e.getLine(), e.getCode(), e.getMessage());
        } catch (STXTException e) {
            throw new StxtSyntaxException(source, ParseException.NO_LINE, e.getCode(), e.getMessage());
        }

        if (roots.size() != 1) {
            throw new StxtSyntaxException(source, 1, "El documento debe tener exactamente un nodo raíz y tiene " + roots.size());
        }
        Node root = roots.get(0);
        if (!NAMESPACE.equals(root.getNamespace())) {
            throw new StxtSyntaxException(source, root.getLine(),
                    "El nodo raíz debe declarar el namespace " + NAMESPACE + ": " + root.getName() + " (" + NAMESPACE + "):");
        }
        return convert(root);
    }

    /** Convierte un nodo de la librería, con sus hijos, al nodo neutral del motor. */
    private static StxtNode convert(Node node) {
        String namespace = node.getNamespace().isEmpty() ? null : node.getNamespace();
        if (node instanceof TextNode text) {
            return new StxtNode(node.getName(), namespace, null, text.getText(), List.of(), node.getLine());
        }
        InlineNode inline = (InlineNode) node;
        List<StxtNode> children = inline.getChildren().stream().map(StxtCoreReader::convert).toList();
        return new StxtNode(node.getName(), namespace, inline.getValue(), null, children, node.getLine());
    }

    private static String loadTemplate() {
        try (InputStream in = StxtCoreReader.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encuentra el template " + TEMPLATE_RESOURCE + " en el classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede leer el template " + TEMPLATE_RESOURCE, e);
        }
    }
}
