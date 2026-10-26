package dev.stxt.boxes.stxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lee un documento STXT y devuelve su nodo raíz. */
public interface DocumentReader {

    StxtNode read(String content, String source);

    default StxtNode read(Path file) throws IOException {
        return read(Files.readString(file, StandardCharsets.UTF_8), file.toString());
    }
}
