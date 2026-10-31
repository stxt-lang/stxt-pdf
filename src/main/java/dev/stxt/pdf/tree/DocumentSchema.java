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
 * The schema of the box model ({@code src/main/resources/stxt/dev.stxt.pdf.stxt},
 * reference/common/structure.md), compiled only once. It is the only definition of which children each
 * node accepts: {@code DocumentReader} uses it when reading and {@link BoxTreeBuilder} when building the
 * tree, so a document that has not gone through the reader gives the same errors.
 *
 * <p>Thread-safe: the schema is loaded in a static initializer and does not change afterwards. It relies on
 * two facts of {@code stxt-core} 1.0.3 (the version pinned in the {@code pom.xml}), checked by reading its
 * code: {@code SchemaProviderMemory.getSchema} only reads from its map, and {@code SchemaValidator} only writes
 * its fields in the constructor (each validation creates its own). If the version changes, this has to be
 * checked again; {@code ConcurrentConversionTest} is the safety net.</p>
 */
public final class DocumentSchema {

    /** Namespace of the box model, the one the schema declares. */
    public static final String NAMESPACE = "dev.stxt.pdf";

    private static final String RESOURCE = "/stxt/dev.stxt.pdf.stxt";

    private static final SchemaProviderMemory PROVIDER = load();

    private DocumentSchema() {
    }

    /** The schema provider, to validate while reading. */
    public static SchemaProvider provider() {
        return PROVIDER;
    }

    /**
     * Validates a tree already read, with all its descendants.
     *
     * @throws DocumentSyntaxException the first error, with the codes of {@code stxt-core}
     *         ({@code CHILD_NOT_DECLARED}, {@code TOO_FEW_CHILDREN}...) and its line
     */
    public static void validate(Node root) {
        validate(root, false);
    }

    /** Like {@link #validate(Node)}; {@code inStyles}, if it is a separate styles document. */
    static void validate(Node root, boolean inStyles) {
        List<ValidationException> errors = new SchemaValidator(PROVIDER, true).validate(root);
        if (!errors.isEmpty()) {
            throw new DocumentSyntaxException(errors.get(0), inStyles);
        }
    }

    private static SchemaProviderMemory load() {
        try (InputStream in = DocumentSchema.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new InternalException("Schema " + RESOURCE + " not found in the classpath");
            }
            SchemaProviderMemory provider = new SchemaProviderMemory();
            provider.addSchema(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return provider;
        } catch (IOException e) {
            throw new InternalException("Schema " + RESOURCE + " cannot be read", e);
        }
    }
}
