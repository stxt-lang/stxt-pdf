package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads an SVG of {@code images/} and checks that it is in the subset the engine supports
 * (reference/components/image.md): basic shapes, paths, groups, transformations, gradients,
 * clipping, strokes and text. Everything else (scripts, animation, filters, masks, patterns,
 * markers, embedded images, CSS styles) is an error, and so is any reference
 * outside the file itself ({@code href} and {@code url(...)} that do not start with {@code #}) and
 * DTDs and entities. Nothing is silently ignored.
 *
 * <p>From the SVG come its natural size in CSS pixels ({@code width} and {@code height}, or the
 * {@code viewBox}), the texts with their font, which {@link ImageFile} checks against
 * {@code fonts/}, and the font of each {@code <text>} and {@code <tspan>}, which is the one that draws it. The errors
 * carry the line of the SVG.</p>
 */
final class SvgInspector {

    static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";
    private static final String XML_NS = XMLConstants.XML_NS_URI;

    /** Supported elements. {@code title} and {@code desc} are not painted. */
    private static final Set<String> ELEMENTS = Set.of("svg", "g", "defs", "title", "desc", "rect", "circle",
            "ellipse", "line", "polyline", "polygon", "path", "linearGradient", "radialGradient", "stop",
            "clipPath", "use", "text", "tspan");

    /** Properties that change what is seen with something outside the subset. */
    private static final Set<String> FORBIDDEN_PROPERTIES = Set.of("filter", "mask", "marker", "marker-start",
            "marker-mid", "marker-end");

    private static final Set<String> GENERIC_FAMILIES = Set.of("serif", "sans-serif", "monospace", "cursive",
            "fantasy", "system-ui", "ui-serif", "ui-sans-serif", "ui-monospace", "ui-rounded", "emoji", "math",
            "fangsong");

    private static final Pattern URL = Pattern.compile("url\\(\\s*(['\"]?)\\s*([^)'\"]*)");
    private static final Pattern LENGTH = Pattern.compile("([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)([a-z%]*)");

    /** A text of the SVG with the font that draws it. */
    record TextUse(int line, String family, FontWeight weight, boolean italic, String text) {
    }

    /**
     * What needs to be known about an SVG apart from its drawing.
     *
     * @param elements those it draws: all those of the file and, in addition, each time a {@code <use>} repeats a
     *                 piece, those of that piece; it is an upper bound of what the drawing counts, saturated at
     *                 {@link #SATURATED}
     * @param depth    the nesting levels, counting the root {@code <svg>}
     * @param fonts    the font of each {@code <text>} and {@code <tspan>}, in document order, with the inheritance of
     *                 its ancestors in the file (not that of the {@code <use>} that repeats it); {@code null} if it
     *                 neither declares {@code font-family} nor inherits it
     */
    record Info(double widthPx, double heightPx, List<TextUse> texts, long elements, int depth, List<FontFace> fonts) {
    }

    /** Cap of the count of drawn elements, so that the repetitions of {@code <use>} do not overflow. */
    static final long SATURATED = 1L << 50;

    private SvgInspector() {
    }

    /**
     * @param name path of the image in the document, for the messages
     * @throws ImageFormatException if it is not a valid SVG of the subset
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
                why = "DTDs and entities are not supported";
            }
            throw new ImageFormatException("The image " + name + " is not a well-formed SVG (line "
                    + e.getLineNumber() + "): " + why, e);
        } catch (ImageFormatException e) {
            throw e;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ImageFormatException("Cannot read the SVG " + name + ": " + e.getMessage(), e);
        }
        return handler.result();
    }

    /** The properties of a font that are inherited. */
    private record FontState(String family, String weight, String style) {
        FontState with(String family, String weight, String style) {
            return new FontState(family != null ? family : this.family, weight != null ? weight : this.weight,
                    style != null ? style : this.style);
        }

        /**
         * The weight it requests: {@code normal}, {@code bold} or a number from 100 to 900 in steps of 100, like
         * {@code Weight}; {@code null} for any other value ({@code bolder}, {@code 350}...).
         */
        FontWeight fontWeight() {
            return switch (weight) {
                case "normal" -> FontWeight.NORMAL;
                case "bold" -> FontWeight.BOLD;
                default -> weight.matches("[1-9]00") ? new FontWeight(Integer.parseInt(weight)) : null;
            };
        }

        boolean italic() {
            return style.equals("italic");
        }

        /** The font of {@code fonts/} it requests, or {@code null} if it has no family. */
        FontFace face() {
            FontWeight w = fontWeight();
            return family == null || w == null ? null
                    : new FontFace(firstFamily(family), w, italic() ? FontStyle.ITALIC : FontStyle.NORMAL);
        }
    }

    private static final class Handler extends DefaultHandler {

        private final String name;
        private final Deque<FontState> fonts = new ArrayDeque<>();
        private final List<TextUse> texts = new ArrayList<>();
        private final List<FontFace> elementFonts = new ArrayList<>();
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
            return new ImageFormatException("The SVG " + name + " cannot be used (line " + line() + "): "
                    + message);
        }

        @Override
        public void startElement(String uri, String local, String qName, Attributes attrs) {
            if (!sawRoot) {
                sawRoot = true;
                if (!SVG_NS.equals(uri) || !local.equals("svg")) {
                    throw new ImageFormatException("The image " + name + " is not an SVG: its root is <" + qName
                            + ">" + (SVG_NS.equals(uri) ? "" : " and is not in the namespace " + SVG_NS));
                }
            }
            if (!SVG_NS.equals(uri)) {
                throw error("the element <" + qName + "> is not SVG (namespace " + (uri.isEmpty()
                        ? "empty" : uri) + ")");
            }
            if (!ELEMENTS.contains(local)) {
                throw error("the element <" + local + "> is not in the subset of SVG that is supported ("
                        + String.join(", ", ELEMENTS.stream().sorted().toList()) + ")");
            }
            if (local.equals("svg") && depth > 0) {
                throw error("an <svg> inside another one is not in the subset that is supported");
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
                    throw error("the attribute " + attrs.getQName(i) + " is from another namespace (" + attrUri
                            + ")");
                }
                if (attrName.toLowerCase(Locale.ROOT).startsWith("on")) {
                    throw error("the attribute " + attrName + " is an event handler (scripts)");
                }
                if (attrName.equals("href")) {
                    checkReference(value, "href");
                    if (!local.equals("use") && !local.endsWith("Gradient")) {
                        throw error("href has no meaning in <" + local + ">");
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
            if (local.equals("text") || local.equals("tspan")) {
                elementFonts.add(font.face());
            }

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
                    throw error("a <tspan> only goes inside a <text>");
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
                    // The text that follows the <tspan> returns to the font of the one that contains it.
                    textFont = fonts.peek();
                    textLine = line();
                }
            }
        }

        private void flushText() {
            if (textFont != null && !text.toString().isBlank()) {
                FontWeight weight = textFont.fontWeight();
                if (weight == null) {
                    throw new ImageFormatException("The text \"" + text.toString().strip() + "\" of the SVG " + name
                            + " (line " + textLine + ") has font-weight " + textFont.weight()
                            + ": only normal, bold and 100 to 900 in steps of 100 are supported");
                }
                String style = textFont.style();
                if (!Set.of("normal", "italic").contains(style)) {
                    throw new ImageFormatException("The text \"" + text.toString().strip() + "\" of the SVG " + name
                            + " (line " + textLine + ") has font-style " + style
                            + ": only normal and italic are supported");
                }
                texts.add(new TextUse(textLine, textFont.family(), weight, textFont.italic(),
                        text.toString()));
            }
            text.setLength(0);
        }

        /** The properties with an effect outside the subset, and the references of a value. */
        private void checkProperty(String property, String value) {
            if (FORBIDDEN_PROPERTIES.contains(property)) {
                throw error(property + " (filters, masks and markers) is not in the subset that is supported");
            }
            Matcher m = URL.matcher(value);
            while (m.find()) {
                checkReference(m.group(2).strip(), property);
            }
        }

        private void checkReference(String value, String where) {
            if (!value.startsWith("#")) {
                throw error("the reference '" + value + "' of " + where + " leaves the file: only internal "
                        + "references (#id) are supported");
            }
        }

        /** The declarations of a {@code style} attribute, with the property in lower case. */
        private Map<String, String> declarations(String style) {
            Map<String, String> out = new java.util.LinkedHashMap<>();
            for (String declaration : style.split(";")) {
                int colon = declaration.indexOf(':');
                if (colon < 0) {
                    if (declaration.isBlank()) {
                        continue;
                    }
                    throw error("the declaration '" + declaration.strip() + "' of style is not property: value");
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
                    throw error("viewBox '" + box + "' is not four numbers");
                }
                if (viewBox[2] <= 0 || viewBox[3] <= 0) {
                    throw error("viewBox '" + box + "' does not have a positive width and height");
                }
            }
        }

        /** An absolute length in CSS pixels, or NaN if it is missing. */
        private double length(String value, String attribute) {
            if (value == null) {
                return Double.NaN;
            }
            Matcher m = LENGTH.matcher(value.strip());
            if (!m.matches()) {
                throw error(attribute + " '" + value + "' is not a length");
            }
            double number = Double.parseDouble(m.group(1));
            double px = switch (m.group(2)) {
                case "", "px" -> number;
                case "pt" -> number * 96 / 72;
                case "pc" -> number * 16;
                case "in" -> number * 96;
                case "cm" -> number * 96 / 2.54;
                case "mm" -> number * 96 / 25.4;
                default -> throw error(attribute + " '" + value + "' is not an absolute length (px, pt, pc, in, cm "
                        + "or mm): with % or em there is no natural size");
            };
            if (px <= 0) {
                throw error(attribute + " '" + value + "' must be positive");
            }
            return px;
        }

        /**
         * The elements the SVG draws: each one counts 1, and a {@code <use>} also counts everything it repeats.
         * It is computed without recursion, because a hostile SVG can nest or chain tens of thousands of levels.
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
                throw new ImageFormatException("The SVG " + name + " repeats itself: a <use> points to an "
                        + "element that contains it");
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
                    throw new ImageFormatException("The SVG " + name + " has no size: its <svg> lacks width and "
                            + "height, or a viewBox");
                }
                w = viewBox[2];
                h = viewBox[3];
            } else if (Double.isNaN(w) || Double.isNaN(h)) {
                if (viewBox == null) {
                    throw new ImageFormatException("The SVG " + name + " only has " + (Double.isNaN(w) ? "height"
                            : "width") + " and has no viewBox: the other side cannot be deduced");
                }
                if (Double.isNaN(w)) {
                    w = h * viewBox[2] / viewBox[3];
                } else {
                    h = w * viewBox[3] / viewBox[2];
                }
            }
            return new Info(w, h, List.copyOf(texts), drawn(), maxDepth,
                    Collections.unmodifiableList(new ArrayList<>(elementFonts)));
        }
    }

    /**
     * An element of the SVG, to count how many elements it draws: what it contains and, for a {@code <use>}, what it
     * repeats.
     */
    private static final class Element {
        enum State { NEW, ACTIVE, DONE }

        final String id;
        /** The {@code id} the {@code href} of a {@code <use>} points to, without the {@code #}; or {@code null}. */
        String ref;
        final List<Element> children = new ArrayList<>();
        State state = State.NEW;
        long drawn;

        Element(String id, String href) {
            this.id = id;
            this.ref = href != null && href.startsWith("#") ? href.substring(1) : null;
        }
    }

    /** Whether the name is a generic CSS family (sans-serif...), which would need to be substituted. */
    static boolean isGeneric(String family) {
        return GENERIC_FAMILIES.contains(family.toLowerCase(Locale.ROOT));
    }

    /** The first family of a {@code font-family}, without quotes. */
    static String firstFamily(String list) {
        String first = list.split(",")[0].strip();
        if (first.length() >= 2 && (first.startsWith("'") && first.endsWith("'")
                || first.startsWith("\"") && first.endsWith("\""))) {
            first = first.substring(1, first.length() - 1).strip();
        }
        return first;
    }
}
