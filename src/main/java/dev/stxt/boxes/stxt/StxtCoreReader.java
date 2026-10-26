package dev.stxt.boxes.stxt;

/**
 * Adaptador a la librería oficial {@code dev.stxt:stxt-core} (hito 1).
 *
 * <p>Debe parsear el documento, validarlo contra el template
 * {@code src/main/resources/stxt/dev.stxt.boxes.stxt} y convertir el resultado a
 * {@link StxtNode}. Así los errores de estructura y de tipos (ENUM, INTEGER, BOOLEAN,
 * cardinalidades) los detecta la librería con sus códigos estándar, y el motor solo tiene que
 * comprobar las reglas que el template no puede expresar (ESPECIFICACION.md §6).</p>
 *
 * <p>La API de la librería no se ha incorporado todavía en este esqueleto: consúltala en
 * https://stxt.dev (sección Tools, Java library). Mientras tanto, {@link MinimalStxtReader}
 * permite trabajar y ejecutar los tests.</p>
 */
public final class StxtCoreReader implements DocumentReader {

    @Override
    public StxtNode read(String content, String source) {
        // TODO(hito 1): parsear y validar con dev.stxt:stxt-core y convertir a StxtNode
        //   conservando: nombre, namespace efectivo, valor inline, bloque, hijos y línea.
        throw new UnsupportedOperationException(
                "Pendiente: conectar con dev.stxt:stxt-core (usa MinimalStxtReader mientras tanto)");
    }
}
