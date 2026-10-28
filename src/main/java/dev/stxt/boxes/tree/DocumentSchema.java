package dev.stxt.boxes.tree;

import dev.stxt.Node;
import dev.stxt.boxes.model.InternalException;
import dev.stxt.exceptions.ValidationException;
import dev.stxt.schema.SchemaProvider;
import dev.stxt.schema.SchemaProviderMemory;
import dev.stxt.schema.SchemaValidator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * El schema del modelo de cajas ({@code src/main/resources/stxt/dev.stxt.boxes.stxt},
 * ESPECIFICACION.md §9), compilado una sola vez. Es la única definición de qué hijos admite
 * cada nodo: la usan {@code DocumentReader} al leer y {@link BoxTreeBuilder} al construir el
 * árbol, de modo que un documento que no ha pasado por el lector da los mismos errores.
 *
 * <p>Seguro entre hilos: el schema no cambia después de cargarlo.</p>
 */
public final class DocumentSchema {

    /** Namespace del modelo de cajas, el que declara el schema. */
    public static final String NAMESPACE = "dev.stxt.boxes";

    private static final String RESOURCE = "/stxt/dev.stxt.boxes.stxt";

    private static final SchemaProviderMemory PROVIDER = load();

    private DocumentSchema() {
    }

    /** El proveedor del schema, para validar mientras se lee. */
    public static SchemaProvider provider() {
        return PROVIDER;
    }

    /**
     * Valida un árbol ya leído, con todos sus descendientes.
     *
     * @throws DocumentSyntaxException el primer error, con los códigos de {@code stxt-core}
     *         ({@code CHILD_NOT_DECLARED}, {@code TOO_FEW_CHILDREN}…) y su línea
     */
    public static void validate(Node root) {
        List<ValidationException> errors = new SchemaValidator(PROVIDER, true).validate(root);
        if (!errors.isEmpty()) {
            throw new DocumentSyntaxException(errors.get(0));
        }
    }

    private static SchemaProviderMemory load() {
        try (InputStream in = DocumentSchema.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new InternalException("No se encuentra el schema " + RESOURCE + " en el classpath");
            }
            SchemaProviderMemory provider = new SchemaProviderMemory();
            provider.addSchema(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return provider;
        } catch (IOException e) {
            throw new InternalException("No se puede leer el schema " + RESOURCE, e);
        }
    }
}
