package dev.stxt.pdf.tree;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentSchemaTest {

    /** The text is that of the schema of the repository, the one the engine validates with. */
    @Test
    void textIsTheSchemaOfTheEngine() throws Exception {
        assertEquals(Files.readString(Path.of("src/main/resources/stxt/dev.stxt.pdf.stxt"), StandardCharsets.UTF_8),
                DocumentSchema.text());
    }
}
