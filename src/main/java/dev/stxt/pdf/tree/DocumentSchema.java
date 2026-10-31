package dev.stxt.pdf.tree;

import dev.stxt.Node;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.exceptions.ValidationException;
import dev.stxt.schema.SchemaProvider;
import dev.stxt.schema.SchemaProviderMemory;
import dev.stxt.schema.SchemaValidator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * El schema del modelo de cajas ({@code src/main/resources/stxt/dev.stxt.pdf.stxt},
 * reference/common/structure.md), compilado una sola vez. Es la única definición de qué hijos admite
 * cada nodo: la usan {@code DocumentReader} al leer y {@link BoxTreeBuilder} al construir el
 * árbol, de modo que un documento que no ha pasado por el lector da los mismos errores.
 *
 * <p>Seguro entre hilos: el schema se carga en un inicializador estático y no cambia después. Se apoya en
 * dos hechos de {@code stxt-core} 1.0.3 (la versión fijada en el {@code pom.xml}), comprobados leyendo su
 * código: {@code SchemaProviderMemory.getSchema} solo lee de su mapa, y {@code SchemaValidator} solo escribe
 * sus campos en el constructor (cada validación crea el suyo). Si se cambia de versión, hay que volver a
 * comprobarlo; {@code ConcurrentConversionTest} es la red de seguridad.</p>
 */
public final class DocumentSchema {

    /** Namespace del modelo de cajas, el que declara el schema. */
    public static final String NAMESPACE = "dev.stxt.pdf";

    private static final String RESOURCE = "/stxt/dev.stxt.pdf.stxt";

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
        validate(root, false);
    }

    /** Como {@link #validate(Node)}; {@code inStyles}, si es un documento de estilos aparte. */
    static void validate(Node root, boolean inStyles) {
        List<ValidationException> errors = new SchemaValidator(PROVIDER, true).validate(root);
        if (!errors.isEmpty()) {
            throw new DocumentSyntaxException(errors.get(0), inStyles);
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
