package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.ValueParser;

import java.util.List;
import java.util.Optional;

import static dev.stxt.boxes.tree.Nodes.child;
import static dev.stxt.boxes.tree.Nodes.error;
import static dev.stxt.boxes.tree.Nodes.parse;
import static dev.stxt.boxes.tree.ValueReader.enumValue;
import static dev.stxt.boxes.tree.ValueReader.points;

/**
 * Lo que el documento dice de sí mismo y de sus páginas: el {@code Media} del documento y de cada {@code Section}
 * (referencia/componentes/document.md) y el {@code Title} y el {@code Language} (referencia/componentes/document.md).
 */
final class MediaReader {

    private MediaReader() {
    }

    /**
     * Un medio y el papel con nombre de que sale, si sale de uno: una sección puede girarlo
     * (referencia/componentes/document.md).
     */
    record Spec(Media media, Media.Paper paper) {
    }

    /** {@code Title} y {@code Language} de {@code Document}: un título no vacío y una etiqueta BCP 47. */
    static Metadata metadata(InlineNode document) {
        String title = child(document, "title").map(n -> {
            String t = n.getText().trim();
            if (t.isEmpty()) {
                throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Title necesita un texto");
            }
            return t;
        }).orElse(null);
        String language = child(document, "language").map(ValueReader::language).orElse(null);
        return new Metadata(title, language);
    }

    /**
     * El medio (referencia/componentes/document.md): su tamaño es un {@code Paper} con su {@code Orientation} o un
     * {@code Width} y un {@code Height}, nunca las dos cosas; el schema solo sabe que cada uno va una vez. El de una
     * sección hereda por grupos el del documento, {@code base} ({@code null} en el del documento).
     */
    static Spec media(InlineNode n, Spec base) {
        Optional<Node> orientation = child(n, "orientation");
        Optional<Node> widthNode = child(n, "width");
        Optional<Node> heightNode = child(n, "height");
        Media.Paper named = paper(n, base);
        double width;
        double height;
        if (named != null) {
            boolean landscape = orientation.map(o -> enumValue(Media.Orientation.class, o))
                    .orElse(Media.Orientation.PORTRAIT) == Media.Orientation.LANDSCAPE;
            width = landscape ? named.height : named.width;
            height = landscape ? named.width : named.height;
        } else if (base != null && widthNode.isEmpty() && heightNode.isEmpty() && orientation.isEmpty()) {
            // El tamaño es el del documento.
            width = base.media().width();
            height = base.media().height();
            named = base.paper();
        } else {
            if (orientation.isPresent()) {
                throw error(orientation.get(), ErrorCode.MEDIA_SIZE, "Orientation gira el papel de Paper, y el "
                        + "medio no lo declara: usa Paper o cambia el Width por el Height");
            }
            if (widthNode.isEmpty() || heightNode.isEmpty()) {
                throw error(widthNode.or(() -> heightNode).orElse(n), ErrorCode.MEDIA_SIZE, "El medio necesita "
                        + "su tamaño: un Paper (A4, Letter…) o un Width y un Height");
            }
            width = points(widthNode.get());
            height = points(heightNode.get());
        }
        Optional<Node> marginNode = child(n, "margin");
        Insets margin = marginNode.map(MediaReader::margin)
                .orElse(base != null ? base.media().margin() : Insets.ZERO);
        Media media = new Media(width, height, margin);
        if (media.pageArea().width() <= 0 || media.pageArea().height() <= 0) {
            throw error(marginNode.orElse(n), ErrorCode.MEDIA_SIZE, "Los márgenes del medio no dejan área útil");
        }
        return new Spec(media, named);
    }

    /**
     * El papel con nombre del medio: su {@code Paper} o, en una sección que solo declara {@code Orientation},
     * el del documento; {@code null} si el tamaño no sale de un papel.
     */
    private static Media.Paper paper(InlineNode n, Spec base) {
        Optional<Node> paper = child(n, "paper");
        Optional<Node> orientation = child(n, "orientation");
        Optional<Node> widthNode = child(n, "width");
        Optional<Node> heightNode = child(n, "height");
        if (paper.isPresent()) {
            Optional<Node> both = widthNode.isPresent() ? widthNode : heightNode;
            if (both.isPresent()) {
                throw error(both.get(), ErrorCode.MEDIA_SIZE, "El medio declara Paper y " + both.get().getName()
                        + ": el tamaño de la página es uno de los dos, Paper (con Orientation) o Width y Height");
            }
            return enumValue(Media.Paper.class, paper.get());
        }
        if (base != null && orientation.isPresent() && widthNode.isEmpty() && heightNode.isEmpty()) {
            // Una sección que solo gira el papel del documento.
            if (base.paper() == null) {
                throw error(orientation.get(), ErrorCode.MEDIA_SIZE, "Orientation gira el papel de Paper, y el "
                        + "documento no lo declara (da Width y Height): declara Paper en la sección o en el documento");
            }
            return base.paper();
        }
        return null;
    }

    private static Insets margin(Node n) {
        List<Double> v = parse(n, () -> ValueParser.list(n.getText())).stream().map(x -> {
            if (!(x instanceof Value.Points p)) {
                throw error(n, ErrorCode.MEDIA_SIZE, "El margen del medio solo admite puntos: " + n.getText().trim());
            }
            return p.pt();
        }).toList();
        Sides<Double> s = parse(n, () -> Sides.shorthand(v));
        return new Insets(s.top(), s.right(), s.bottom(), s.left());
    }
}
