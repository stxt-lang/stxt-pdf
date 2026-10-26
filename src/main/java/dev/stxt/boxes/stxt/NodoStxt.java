package dev.stxt.boxes.stxt;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Nodo STXT neutral, independiente de la librería de parseo. El resto del motor solo
 * trabaja con este tipo, de modo que cambiar de lector no afecta al layout.
 *
 * @param nombre    nombre del nodo tal como aparece en el documento
 * @param namespace namespace efectivo (heredado si no se declara), o {@code null}
 * @param valor     valor inline ({@code Nombre: valor}), o {@code null}
 * @param bloque    contenido de un bloque ({@code Nombre >>}), o {@code null}
 * @param hijos     hijos en orden de aparición (STXT preserva el orden)
 * @param linea     línea del documento, para los mensajes de error
 */
public record NodoStxt(String nombre, String namespace, String valor, String bloque,
                       List<NodoStxt> hijos, int linea) {

    public NodoStxt {
        hijos = List.copyOf(hijos);
    }

    /** Compara por nombre canónico simplificado (espacios compactados, sin mayúsculas). */
    public boolean es(String otroNombre) {
        return canonico(nombre).equals(canonico(otroNombre));
    }

    public Optional<NodoStxt> hijo(String nombreHijo) {
        return hijos.stream().filter(h -> h.es(nombreHijo)).findFirst();
    }

    public List<NodoStxt> hijos(String nombreHijo) {
        return hijos.stream().filter(h -> h.es(nombreHijo)).toList();
    }

    /** Valor inline o contenido de bloque, el que exista. */
    public String contenido() {
        return bloque != null ? bloque : (valor != null ? valor : "");
    }

    public Optional<String> contenidoDe(String nombreHijo) {
        return hijo(nombreHijo).map(NodoStxt::contenido);
    }

    /**
     * Forma canónica simplificada para comparar nombres. La canonicalización completa la define
     * STXT-SPEC §4.3 y la aplica la librería oficial; aquí basta para el subconjunto del modelo.
     */
    public static String canonico(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
