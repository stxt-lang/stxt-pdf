package dev.stxt.pdf.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The target of a link (reference/common/links.md): an external URL ({@code https://...},
 * {@code http://...} or {@code mailto:...}) or the {@code Id} of a box of the document ({@code #id}).
 * Anything else is rejected: the engine opens nothing, it only writes the PDF annotation, and a
 * viewer must not come across {@code javascript:}, {@code file:} or a relative path.
 */
public final class Links {

    private static final Set<String> SCHEMES = Set.of("http", "https", "mailto");

    private Links() {
    }

    /** Whether the target points to a box of the document ({@code #id}) instead of a URL. */
    public static boolean internal(String destination) {
        return destination.startsWith("#");
    }

    /** The {@code Id} an internal target leads to ({@code #intro} → {@code intro}). */
    public static String target(String destination) {
        return destination.substring(1);
    }

    /** The internal target that leads to the box with that {@code Id} or table of contents name. */
    public static String to(String id) {
        return "#" + id;
    }

    /**
     * Why a target is not valid, or empty if it is. An internal target only needs a
     * name without spaces (whether it exists is checked when the tree is built); an external one, a
     * supported scheme and what that scheme needs: a host in {@code http} and
     * {@code https}, a recipient in {@code mailto}.
     */
    public static Optional<String> problem(String destination) {
        if (destination.isEmpty()) {
            return Optional.of("the target is empty: it has to be a URL (https://..., mailto:...) or #id");
        }
        if (internal(destination)) {
            String id = target(destination);
            if (id.isEmpty() || id.chars().anyMatch(Character::isWhitespace)) {
                return Optional.of("'" + destination + "' is not the Id of a box: it is written #id, without spaces");
            }
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(destination);
        } catch (URISyntaxException e) {
            return Optional.of("'" + destination + "' is not a valid URL (" + e.getReason() + ")");
        }
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme == null || !SCHEMES.contains(scheme)) {
            return Optional.of("'" + destination + "' does not have a supported scheme: only https://, http://, "
                    + "mailto: and #id");
        }
        if (scheme.equals("mailto")) {
            String to = uri.getSchemeSpecificPart();
            if (to == null || to.isBlank() || to.startsWith("?")) {
                return Optional.of("'" + destination + "' has no recipient: mailto:name@domain");
            }
        } else if (uri.getHost() == null || uri.getHost().isEmpty()) {
            return Optional.of("'" + destination + "' has no host: " + scheme + "://host/path");
        }
        return Optional.empty();
    }
}
