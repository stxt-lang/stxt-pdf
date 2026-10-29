package dev.stxt.boxes.resources;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee un SVG de {@code images/} y comprueba que está en el subconjunto que admite el motor
 * (ESPECIFICACION.md §5.18): formas básicas, rutas, grupos, transformaciones, degradados,
 * recortes, trazos y texto. Todo lo demás (scripts, animación, filtros, máscaras, patrones,
 * marcadores, imágenes incrustadas, estilos CSS) es un error, y también cualquier referencia
 * fuera del propio fichero ({@code href} y {@code url(…)} que no empiecen por {@code #}) y los
 * DTD y entidades. Nada se ignora en silencio.
 *
 * <p>Del SVG sale su tamaño natural en píxeles CSS ({@code width} y {@code height}, o el
 * {@code viewBox}) y los textos con su fuente, que {@link ImageFile} comprueba contra
 * {@code fonts/}. Los errores llevan la línea del SVG.</p>
 */
final class SvgInspector {

    static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";
    private static final String XML_NS = XMLConstants.XML_NS_URI;

    /** Elementos admitidos. {@code title} y {@code desc} no se pintan. */
    private static final Set<String> ELEMENTS = Set.of("svg", "g", "defs", "title", "desc", "rect", "circle",
            "ellipse", "line", "polyline", "polygon", "path", "linearGradient", "radialGradient", "stop",
            "clipPath", "use", "text", "tspan");

    /** Propiedades que cambian lo que se ve con algo fuera del subconjunto. */
    private static final Set<String> FORBIDDEN_PROPERTIES = Set.of("filter", "mask", "marker", "marker-start",
            "marker-mid", "marker-end");

    private static final Set<String> GENERIC_FAMILIES = Set.of("serif", "sans-serif", "monospace", "cursive",
            "fantasy", "system-ui", "ui-serif", "ui-sans-serif", "ui-monospace", "ui-rounded", "emoji", "math",
            "fangsong");

    private static final Pattern URL = Pattern.compile("url\\(\\s*(['\"]?)\\s*([^)'\"]*)");
    private static final Pattern LENGTH = Pattern.compile("([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)([a-z%]*)");

    /** Un texto del SVG con la fuente que lo dibuja. */
    record TextUse(int line, String family, boolean bold, boolean italic, String text) {
    }

    /**
     * Lo que se necesita saber de un SVG fuera de su dibujo.
     *
     * @param elements los que dibuja: todos los del fichero y, además, cada vez que un {@code <use>} repite un
     *                 trozo, los de ese trozo; es un tope de lo que cuenta el dibujo, saturado en {@link #SATURATED}
     * @param depth    los niveles de anidado, contando el {@code <svg>} de la raíz
     */
    record Info(double widthPx, double heightPx, List<TextUse> texts, long elements, int depth) {
    }

    /** Tope del recuento de elementos dibujados, para que las repeticiones de los {@code <use>} no desborden. */
    static final long SATURATED = 1L << 50;

    private SvgInspector() {
    }

    /**
     * @param name ruta de la imagen en el documento, para los mensajes
     * @throws ImageFormatException si no es un SVG válido del subconjunto
     */
    static Info inspect(byte[] content, String name) {
        Handler handler = new Handler(name);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.newSAXParser().parse(new InputSource(new ByteArrayInputStream(content)), handler);
        } catch (SAXParseException e) {
            String why = e.getMessage();
            if (why != null && why.contains("DOCTYPE")) {
                why = "no se admiten DTD ni entidades";
            }
            throw new ImageFormatException("La imagen " + name + " no es un SVG bien formado (línea "
                    + e.getLineNumber() + "): " + why, e);
        } catch (ImageFormatException e) {
            throw e;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ImageFormatException("No se puede leer el SVG " + name + ": " + e.getMessage(), e);
        }
        return handler.result();
    }

    /** Las propiedades de una fuente que se heredan. */
    private record FontState(String family, String weight, String style) {
        FontState with(String family, String weight, String style) {
            return new FontState(family != null ? family : this.family, weight != null ? weight : this.weight,
                    style != null ? style : this.style);
        }
    }

    private static final class Handler extends DefaultHandler {

        private final String name;
        private final Deque<FontState> fonts = new ArrayDeque<>();
        private final List<TextUse> texts = new ArrayList<>();
        private Locator locator;
        private int depth;
        private int maxDepth;
        private final Deque<Element> open = new ArrayDeque<>();
        private final Map<String, Element> byId = new HashMap<>();
        private Element root;
        private boolean sawRoot;
        private double width = Double.NaN;
        private double height = Double.NaN;
        private double[] viewBox;
        private int textDepth;
        private int textLine;
        private FontState textFont;
        private final StringBuilder text = new StringBuilder();

        Handler(String name) {
            this.name = name;
            fonts.push(new FontState(null, "normal", "normal"));
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        private int line() {
            return locator == null ? 0 : locator.getLineNumber();
        }

        private ImageFormatException error(String message) {
            return new ImageFormatException("El SVG " + name + " no se puede usar (línea " + line() + "): "
                    + message);
        }

        @Override
        public void startElement(String uri, String local, String qName, Attributes attrs) {
            if (!sawRoot) {
                sawRoot = true;
                if (!SVG_NS.equals(uri) || !local.equals("svg")) {
                    throw new ImageFormatException("La imagen " + name + " no es un SVG: su raíz es <" + qName
                            + ">" + (SVG_NS.equals(uri) ? "" : " y no está en el espacio de nombres " + SVG_NS));
                }
            }
            if (!SVG_NS.equals(uri)) {
                throw error("el elemento <" + qName + "> no es de SVG (espacio de nombres " + (uri.isEmpty()
                        ? "vacío" : uri) + ")");
            }
            if (!ELEMENTS.contains(local)) {
                throw error("el elemento <" + local + "> no está en el subconjunto de SVG que se admite ("
                        + String.join(", ", ELEMENTS.stream().sorted().toList()) + ")");
            }
            if (local.equals("svg") && depth > 0) {
                throw error("un <svg> dentro de otro no está en el subconjunto que se admite");
            }
            depth++;
            maxDepth = Math.max(maxDepth, depth);
            String href = null;
            if (local.equals("use")) {
                href = attrs.getValue(XLINK_NS, "href");
                href = href != null ? href : attrs.getValue("href");
            }
            Element element = new Element(attrs.getValue("id"), href);
            if (open.isEmpty()) {
                root = element;
            } else {
                open.peek().children.add(element);
            }
            if (element.id != null) {
                byId.putIfAbsent(element.id, element);
            }
            open.push(element);

            String family = null;
            String weight = null;
            String style = null;
            for (int i = 0; i < attrs.getLength(); i++) {
                String attrUri = attrs.getURI(i);
                String attrName = attrs.getLocalName(i);
                String value = attrs.getValue(i);
                if (!attrUri.isEmpty() && !attrUri.equals(XLINK_NS) && !attrUri.equals(XML_NS)) {
                    throw error("el atributo " + attrs.getQName(i) + " es de otro espacio de nombres (" + attrUri
                            + ")");
                }
                if (attrName.toLowerCase(Locale.ROOT).startsWith("on")) {
                    throw error("el atributo " + attrName + " es un manejador de eventos (scripts)");
                }
                if (attrName.equals("href")) {
                    checkReference(value, "href");
                    if (!local.equals("use") && !local.endsWith("Gradient")) {
                        throw error("href no tiene sentido en <" + local + ">");
                    }
                    continue;
                }
                if (attrName.equals("style")) {
                    for (Map.Entry<String, String> d : declarations(value).entrySet()) {
                        String property = d.getKey();
                        checkProperty(property, d.getValue());
                        switch (property) {
                            case "font-family" -> family = d.getValue();
                            case "font-weight" -> weight = d.getValue();
                            case "font-style" -> style = d.getValue();
                            default -> {
                            }
                        }
                    }
                    continue;
                }
                checkProperty(attrName, value);
                switch (attrName) {
                    case "font-family" -> family = value;
                    case "font-weight" -> weight = value;
                    case "font-style" -> style = value;
                    default -> {
                    }
                }
            }
            FontState font = fonts.peek().with(family, weight, style);
            fonts.push(font);

            if (local.equals("svg")) {
                size(attrs);
            } else if (local.equals("text")) {
                if (textDepth == 0) {
                    textLine = line();
                    textFont = font;
                    text.setLength(0);
                }
                textDepth++;
            } else if (local.equals("tspan")) {
                if (textDepth == 0) {
                    throw error("un <tspan> solo va dentro de un <text>");
                }
                flushText();
                textDepth++;
                textLine = line();
                textFont = font;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (textDepth > 0) {
                text.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String local, String qName) {
            depth--;
            open.pop();
            fonts.pop();
            if (local.equals("text") || local.equals("tspan")) {
                flushText();
                textDepth--;
                if (textDepth > 0) {
                    // El texto que sigue al <tspan> vuelve a la fuente del que lo contiene.
                    textFont = fonts.peek();
                    textLine = line();
                }
            }
        }

        private void flushText() {
            if (textFont != null && !text.toString().isBlank()) {
                String weight = textFont.weight();
                if (!Set.of("normal", "400", "bold", "700").contains(weight)) {
                    throw new ImageFormatException("El texto \"" + text.toString().strip() + "\" del SVG " + name
                            + " (línea " + textLine + ") tiene font-weight " + weight
                            + ": solo se admiten normal (400) y bold (700)");
                }
                String style = textFont.style();
                if (!Set.of("normal", "italic").contains(style)) {
                    throw new ImageFormatException("El texto \"" + text.toString().strip() + "\" del SVG " + name
                            + " (línea " + textLine + ") tiene font-style " + style
                            + ": solo se admiten normal e italic");
                }
                texts.add(new TextUse(textLine, textFont.family(), weight.equals("bold") || weight.equals("700"),
                        style.equals("italic"), text.toString()));
            }
            text.setLength(0);
        }

        /** Las propiedades con efecto fuera del subconjunto, y las referencias de un valor. */
        private void checkProperty(String property, String value) {
            if (FORBIDDEN_PROPERTIES.contains(property)) {
                throw error(property + " (filtros, máscaras y marcadores) no está en el subconjunto que se admite");
            }
            Matcher m = URL.matcher(value);
            while (m.find()) {
                checkReference(m.group(2).strip(), property);
            }
        }

        private void checkReference(String value, String where) {
            if (!value.startsWith("#")) {
                throw error("la referencia '" + value + "' de " + where + " sale del fichero: solo se admiten "
                        + "referencias internas (#id)");
            }
        }

        /** Las declaraciones de un atributo {@code style}, con la propiedad en minúsculas. */
        private Map<String, String> declarations(String style) {
            Map<String, String> out = new java.util.LinkedHashMap<>();
            for (String declaration : style.split(";")) {
                int colon = declaration.indexOf(':');
                if (colon < 0) {
                    if (declaration.isBlank()) {
                        continue;
                    }
                    throw error("la declaración '" + declaration.strip() + "' de style no es propiedad: valor");
                }
                out.put(declaration.substring(0, colon).strip().toLowerCase(Locale.ROOT),
                        declaration.substring(colon + 1).strip());
            }
            return out;
        }

        private void size(Attributes attrs) {
            width = length(attrs.getValue("width"), "width");
            height = length(attrs.getValue("height"), "height");
            String box = attrs.getValue("viewBox");
            if (box != null) {
                String[] parts = box.strip().split("[\\s,]+");
                try {
                    if (parts.length != 4) {
                        throw new NumberFormatException();
                    }
                    viewBox = new double[4];
                    for (int i = 0; i < 4; i++) {
                        viewBox[i] = Double.parseDouble(parts[i]);
                    }
                } catch (NumberFormatException e) {
                    throw error("viewBox '" + box + "' no son cuatro números");
                }
                if (viewBox[2] <= 0 || viewBox[3] <= 0) {
                    throw error("viewBox '" + box + "' no tiene ancho y alto positivos");
                }
            }
        }

        /** Una longitud absoluta en píxeles CSS, o NaN si falta. */
        private double length(String value, String attribute) {
            if (value == null) {
                return Double.NaN;
            }
            Matcher m = LENGTH.matcher(value.strip());
            if (!m.matches()) {
                throw error(attribute + " '" + value + "' no es una longitud");
            }
            double number = Double.parseDouble(m.group(1));
            double px = switch (m.group(2)) {
                case "", "px" -> number;
                case "pt" -> number * 96 / 72;
                case "pc" -> number * 16;
                case "in" -> number * 96;
                case "cm" -> number * 96 / 2.54;
                case "mm" -> number * 96 / 25.4;
                default -> throw error(attribute + " '" + value + "' no es una medida absoluta (px, pt, pc, in, cm "
                        + "o mm): con % o em no hay tamaño natural");
            };
            if (px <= 0) {
                throw error(attribute + " '" + value + "' debe ser positivo");
            }
            return px;
        }

        /**
         * Los elementos que dibuja el SVG: cada uno cuenta 1, y un {@code <use>} cuenta además todo lo que repite.
         * Se calcula sin recursión, porque un SVG hostil puede anidar o encadenar decenas de miles de niveles.
         */
        private long drawn() {
            Deque<Element> stack = new ArrayDeque<>();
            stack.push(root);
            while (!stack.isEmpty()) {
                Element e = stack.peek();
                if (e.state == Element.State.DONE) {
                    stack.pop();
                } else if (e.state == Element.State.NEW) {
                    e.state = Element.State.ACTIVE;
                    for (Element child : e.children) {
                        visit(child, stack);
                    }
                    Element target = e.ref == null ? null : byId.get(e.ref);
                    if (target != null) {
                        visit(target, stack);
                    }
                } else {
                    long total = 1;
                    for (Element child : e.children) {
                        total = Math.min(SATURATED, total + child.drawn);
                    }
                    Element target = e.ref == null ? null : byId.get(e.ref);
                    if (target != null) {
                        total = Math.min(SATURATED, total + target.drawn);
                    }
                    e.drawn = total;
                    e.state = Element.State.DONE;
                    stack.pop();
                }
            }
            return root.drawn;
        }

        private void visit(Element e, Deque<Element> stack) {
            if (e.state == Element.State.ACTIVE) {
                throw new ImageFormatException("El SVG " + name + " se repite a sí mismo: un <use> apunta a un "
                        + "elemento que lo contiene");
            }
            if (e.state == Element.State.NEW) {
                stack.push(e);
            }
        }

        Info result() {
            double w = width;
            double h = height;
            if (Double.isNaN(w) && Double.isNaN(h)) {
                if (viewBox == null) {
                    throw new ImageFormatException("El SVG " + name + " no tiene tamaño: declara width y height, o "
                            + "un viewBox, en su <svg>");
                }
                w = viewBox[2];
                h = viewBox[3];
            } else if (Double.isNaN(w) || Double.isNaN(h)) {
                if (viewBox == null) {
                    throw new ImageFormatException("El SVG " + name + " solo tiene " + (Double.isNaN(w) ? "height"
                            : "width") + " y no tiene viewBox: no se puede deducir el otro lado");
                }
                if (Double.isNaN(w)) {
                    w = h * viewBox[2] / viewBox[3];
                } else {
                    h = w * viewBox[3] / viewBox[2];
                }
            }
            return new Info(w, h, List.copyOf(texts), drawn(), maxDepth);
        }
    }

    /** Un elemento del SVG, para contar cuántos elementos dibuja: lo que contiene y, si es un {@code <use>}, lo que repite. */
    private static final class Element {
        enum State { NEW, ACTIVE, DONE }

        final String id;
        /** El {@code id} al que apunta el {@code href} de un {@code <use>}, sin la almohadilla; si no, {@code null}. */
        String ref;
        final List<Element> children = new ArrayList<>();
        State state = State.NEW;
        long drawn;

        Element(String id, String href) {
            this.id = id;
            this.ref = href != null && href.startsWith("#") ? href.substring(1) : null;
        }
    }

    /** Si el nombre es una familia genérica de CSS (sans-serif…), que necesitaría sustituirla. */
    static boolean isGeneric(String family) {
        return GENERIC_FAMILIES.contains(family.toLowerCase(Locale.ROOT));
    }

    /** La primera familia de un {@code font-family}, sin comillas. */
    static String firstFamily(String list) {
        String first = list.split(",")[0].strip();
        if (first.length() >= 2 && (first.startsWith("'") && first.endsWith("'")
                || first.startsWith("\"") && first.endsWith("\""))) {
            first = first.substring(1, first.length() - 1).strip();
        }
        return first;
    }
}
