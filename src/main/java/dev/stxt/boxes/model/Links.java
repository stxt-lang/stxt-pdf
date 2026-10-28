package dev.stxt.boxes.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * El destino de un enlace (ESPECIFICACION.md §5.20): una URL de fuera ({@code https://…},
 * {@code http://…} o {@code mailto:…}) o el {@code Id} de una caja del documento ({@code #id}).
 * Cualquier otra cosa se rechaza: el motor no abre nada, solo escribe la anotación del PDF, y un
 * visor no debe encontrarse con {@code javascript:}, {@code file:} ni una ruta relativa.
 */
public final class Links {

    private static final Set<String> SCHEMES = Set.of("http", "https", "mailto");

    private Links() {
    }

    /** Si el destino señala una caja del documento ({@code #id}) en lugar de una URL. */
    public static boolean internal(String destination) {
        return destination.startsWith("#");
    }

    /** El {@code Id} al que lleva un destino interno ({@code #intro} → {@code intro}). */
    public static String target(String destination) {
        return destination.substring(1);
    }

    /** El destino interno que lleva a la caja de ese {@code Id} o nombre de índice. */
    public static String to(String id) {
        return "#" + id;
    }

    /**
     * Por qué un destino no es válido, o vacío si lo es. Un destino interno solo necesita un
     * nombre sin espacios (que exista se comprueba al construir el árbol); uno externo, un
     * esquema admitido y lo que ese esquema necesita: un servidor en {@code http} y
     * {@code https}, un destinatario en {@code mailto}.
     */
    public static Optional<String> problem(String destination) {
        if (destination.isEmpty()) {
            return Optional.of("el destino está vacío; usa una URL (https://…, mailto:…) o #id");
        }
        if (internal(destination)) {
            String id = target(destination);
            if (id.isEmpty() || id.chars().anyMatch(Character::isWhitespace)) {
                return Optional.of("'" + destination + "' no es el Id de una caja: usa #id, sin espacios");
            }
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(destination);
        } catch (URISyntaxException e) {
            return Optional.of("'" + destination + "' no es una URL válida (" + e.getReason() + ")");
        }
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme == null || !SCHEMES.contains(scheme)) {
            return Optional.of("'" + destination + "' no tiene un esquema admitido: solo https://, http://, "
                    + "mailto: y #id");
        }
        if (scheme.equals("mailto")) {
            String to = uri.getSchemeSpecificPart();
            if (to == null || to.isBlank() || to.startsWith("?")) {
                return Optional.of("'" + destination + "' no tiene destinatario: mailto:nombre@dominio");
            }
        } else if (uri.getHost() == null || uri.getHost().isEmpty()) {
            return Optional.of("'" + destination + "' no tiene servidor: " + scheme + "://servidor/ruta");
        }
        return Optional.empty();
    }
}
