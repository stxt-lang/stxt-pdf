package dev.stxt.boxes.stxt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lector mínimo de STXT para arrancar el proyecto y para los tests.
 *
 * <p><strong>No valida contra el template</strong> ni implementa toda la especificación STXT.
 * Cubre el subconjunto que usa el modelo de cajas:</p>
 * <ul>
 *   <li>Nodos inline {@code Nombre: valor} y nodos de bloque {@code Nombre >>}.</li>
 *   <li>Namespace opcional {@code Nombre (ns): valor}, heredado por los hijos.</li>
 *   <li>Indentación con tabuladores, o con espacios en múltiplos de 4, sin mezclar en una línea.</li>
 *   <li>Comentarios {@code #} fuera de los bloques y líneas en blanco.</li>
 * </ul>
 * <p>En producción, usa {@link LectorStxtCore}.</p>
 */
public final class LectorStxtMinimo implements LectorDocumento {

    private static final Pattern NOMBRE_CON_NS = Pattern.compile("^(.*?)\\s*\\(([^)]*)\\)\\s*$");

    private static final class Constructor {
        final String nombre;
        final String namespace;
        final String valor;
        final boolean esBloque;
        final int linea;
        final int nivel;
        final List<String> lineasBloque = new ArrayList<>();
        final List<Constructor> hijos = new ArrayList<>();

        Constructor(String nombre, String namespace, String valor, boolean esBloque, int linea, int nivel) {
            this.nombre = nombre;
            this.namespace = namespace;
            this.valor = valor;
            this.esBloque = esBloque;
            this.linea = linea;
            this.nivel = nivel;
        }

        NodoStxt construir() {
            String bloque = null;
            if (esBloque) {
                List<String> l = new ArrayList<>(lineasBloque);
                while (!l.isEmpty() && l.get(l.size() - 1).isBlank()) {
                    l.remove(l.size() - 1);
                }
                bloque = String.join("\n", l);
            }
            return new NodoStxt(nombre, namespace, valor, bloque,
                    hijos.stream().map(Constructor::construir).toList(), linea);
        }
    }

    @Override
    public NodoStxt leer(String contenido, String origen) {
        String[] lineas = contenido.replace("\r\n", "\n").split("\n", -1);
        List<Constructor> raices = new ArrayList<>();
        List<Constructor> pila = new ArrayList<>(); // pila.get(n) = último nodo abierto de nivel n
        Constructor bloqueAbierto = null;

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            int numero = i + 1;

            if (bloqueAbierto != null) {
                if (linea.isBlank()) {
                    bloqueAbierto.lineasBloque.add("");
                    continue;
                }
                int nivel = nivel(linea, origen, numero, true);
                if (nivel > bloqueAbierto.nivel) {
                    bloqueAbierto.lineasBloque.add(quitarNiveles(linea, bloqueAbierto.nivel + 1));
                    continue;
                }
                bloqueAbierto = null;
            }

            if (linea.isBlank()) {
                continue;
            }
            int nivel = nivel(linea, origen, numero, false);
            String texto = linea.strip();
            if (texto.startsWith("#")) {
                continue;
            }
            if (nivel > pila.size()) {
                throw new ErrorStxt(origen, numero, "Salto de indentación: se esperaba como máximo el nivel " + pila.size());
            }

            boolean esBloque;
            String parteNombre;
            String valor = null;
            if (texto.endsWith(">>")) {
                esBloque = true;
                parteNombre = texto.substring(0, texto.length() - 2);
            } else {
                int dosPuntos = texto.indexOf(':');
                if (dosPuntos < 0) {
                    throw new ErrorStxt(origen, numero, "Línea no válida: falta ':' o '>>'");
                }
                esBloque = false;
                parteNombre = texto.substring(0, dosPuntos);
                valor = texto.substring(dosPuntos + 1).strip();
            }

            String nombre = parteNombre.strip();
            String namespace = null;
            Matcher m = NOMBRE_CON_NS.matcher(nombre);
            if (m.matches()) {
                nombre = m.group(1).strip();
                namespace = m.group(2).strip();
            }
            if (nombre.isEmpty()) {
                throw new ErrorStxt(origen, numero, "Nombre de nodo vacío");
            }

            while (pila.size() > nivel) {
                pila.remove(pila.size() - 1);
            }
            Constructor padre = nivel == 0 ? null : pila.get(nivel - 1);
            if (namespace == null && padre != null) {
                namespace = padre.namespace;
            }
            Constructor nodo = new Constructor(nombre, namespace, valor, esBloque, numero, nivel);
            if (padre == null) {
                raices.add(nodo);
            } else {
                if (padre.esBloque) {
                    throw new ErrorStxt(origen, numero, "Un nodo de bloque no puede tener hijos");
                }
                padre.hijos.add(nodo);
            }
            pila.add(nodo);
            if (esBloque) {
                bloqueAbierto = nodo;
            }
        }

        if (raices.size() != 1) {
            throw new ErrorStxt(origen, 1, "El documento debe tener exactamente un nodo raíz y tiene " + raices.size());
        }
        return raices.get(0).construir();
    }

    /** Nivel de indentación: tabuladores, o espacios en múltiplos de 4. */
    private static int nivel(String linea, String origen, int numero, boolean dentroDeBloque) {
        int tabs = 0;
        int espacios = 0;
        for (char c : linea.toCharArray()) {
            if (c == '\t') {
                tabs++;
            } else if (c == ' ') {
                espacios++;
            } else {
                break;
            }
        }
        if (tabs > 0 && espacios > 0 && !dentroDeBloque) {
            throw new ErrorStxt(origen, numero, "No se pueden mezclar tabuladores y espacios en la indentación");
        }
        if (tabs > 0) {
            return tabs;
        }
        if (espacios % 4 != 0 && !dentroDeBloque) {
            throw new ErrorStxt(origen, numero, "La indentación con espacios debe ser múltiplo de 4");
        }
        return espacios / 4;
    }

    /** Elimina {@code niveles} niveles de indentación del principio de una línea de bloque. */
    private static String quitarNiveles(String linea, int niveles) {
        int i = 0;
        int quitados = 0;
        while (quitados < niveles && i < linea.length()) {
            if (linea.charAt(i) == '\t') {
                i++;
                quitados++;
            } else if (linea.startsWith("    ", i)) {
                i += 4;
                quitados++;
            } else {
                break;
            }
        }
        return linea.substring(i);
    }
}
