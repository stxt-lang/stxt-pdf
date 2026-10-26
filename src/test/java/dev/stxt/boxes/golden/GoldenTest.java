package dev.stxt.boxes.golden;

import dev.stxt.boxes.arbol.BoxTreeBuilder;
import dev.stxt.boxes.fragmentos.Pagina;
import dev.stxt.boxes.layout.Motor;
import dev.stxt.boxes.salida.VolcadoStxt;
import dev.stxt.boxes.stxt.LectorStxtMinimo;
import dev.stxt.boxes.stxt.NodoStxt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de referencia (ESPECIFICACION.md §7): cada carpeta de {@code golden/} contiene
 * {@code entrada.stxt} y {@code esperado.stxt}. Se comparan nombres y valores; los números de
 * {@code Rect} con una tolerancia de 0.01 pt.
 */
@Disabled("Activar al completar el hito 2 (BlockLayout) y el hito 4 (FlexLayout)")
class GoldenTest {

    private static final double TOLERANCIA = 0.01;
    private final LectorStxtMinimo lector = new LectorStxtMinimo();

    @TestFactory
    Stream<DynamicTest> casos() throws IOException, URISyntaxException {
        Path raiz = Path.of(getClass().getResource("/golden").toURI());
        try (Stream<Path> dirs = Files.list(raiz)) {
            List<Path> casos = dirs.filter(Files::isDirectory).sorted().toList();
            return casos.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> {
                NodoStxt entrada = lector.leer(dir.resolve("entrada.stxt"));
                NodoStxt esperado = lector.leer(dir.resolve("esperado.stxt"));
                List<Pagina> paginas = new Motor().maquetar(entrada);
                var medio = new BoxTreeBuilder().construir(entrada).medio();
                String volcado = VolcadoStxt.volcar(paginas, medio.areaUtil(), false);
                comparar(esperado, lector.leer(volcado, "volcado"), dir.getFileName() + "/");
            }));
        }
    }

    private static void comparar(NodoStxt esperado, NodoStxt obtenido, String ruta) {
        String aqui = ruta + esperado.nombre();
        assertEquals(NodoStxt.canonico(esperado.nombre()), NodoStxt.canonico(obtenido.nombre()), aqui);
        if (esperado.es("Rect")) {
            double[] e = numeros(esperado.contenido());
            double[] o = numeros(obtenido.contenido());
            assertEquals(4, o.length, aqui);
            for (int i = 0; i < 4; i++) {
                assertEquals(e[i], o[i], TOLERANCIA, aqui + " (componente " + i + ")");
            }
        } else {
            assertEquals(esperado.contenido().trim(), obtenido.contenido().trim(), aqui);
        }
        assertEquals(esperado.hijos().size(), obtenido.hijos().size(), aqui + ": número de hijos");
        for (int i = 0; i < esperado.hijos().size(); i++) {
            comparar(esperado.hijos().get(i), obtenido.hijos().get(i), aqui + "/");
        }
    }

    private static double[] numeros(String s) {
        return Stream.of(s.trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
    }
}
