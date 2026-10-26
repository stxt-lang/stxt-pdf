package dev.stxt.boxes.stxt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lee un documento STXT y devuelve su nodo raíz. */
public interface LectorDocumento {

    NodoStxt leer(String contenido, String origen);

    default NodoStxt leer(Path fichero) throws IOException {
        return leer(Files.readString(fichero, StandardCharsets.UTF_8), fichero.toString());
    }
}
