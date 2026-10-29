package dev.stxt.boxes.paint;

import com.github.weisj.jsvg.parser.DomElement;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.resources.FontFile;
import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.ImageFormatException;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Las fuentes con que JSVG dibuja el texto de un SVG (ESPECIFICACION.md §5.18): los ficheros de {@code fonts/} que
 * {@link ImageFile} comprobó, sin dar nada de alta en AWT ni buscar ninguna fuente por su nombre. Uno por dibujo.
 *
 * <p>JSVG busca la fuente por familia en AWT, que tiene dos problemas: con la JVM en turco no encuentra
 * {@code IBM Plex Sans} (pasa {@code I} a minúscula con el idioma y da {@code ıbm}) y dibuja con {@code Dialog}
 * sin avisar, y el registro de AWT es de toda la JVM, así que dos carpetas de recursos con la misma familia
 * comparten la primera (ROADMAP-QA-ARQ.md, QA-02 y TS-03). Por eso, al leer el SVG, {@link #rewrite} pone en cada
 * {@code <text>} y {@code <tspan>} la fuente que comprobó {@code ImageFile}: una familia clave
 * ({@code stxt-font-} y la huella del fichero, {@link FontFile#digest()}), que no es de ninguna fuente instalada, y
 * su peso y su estilo explícitos. JSVG no la encuentra en AWT y la pide a {@link #customFont}, que la crea con
 * {@link Font#createFont} desde el fichero.</p>
 *
 * <p>JSVG guarda las fuentes que resuelve en una caché de toda la JVM cuya clave no incluye este objeto: por eso la
 * clave es la huella del contenido, y lo que guarda vale para cualquier carpeta con ese mismo fichero.</p>
 */
final class SvgFonts implements PlatformSupport {

    private static final String PREFIX = "stxt-font-";
    /** Las propiedades de la fuente que se quitan de {@code style}, para que manden las que se ponen. */
    private static final Set<String> FONT_PROPERTIES = Set.of("font-family", "font-weight", "font-style");

    private final ImageFile file;
    private final Map<String, FontFile> byKey = new HashMap<>();
    private final Map<String, Font> created = new HashMap<>();
    private int texts;
    private String failure;

    SvgFonts(ImageFile file) {
        this.file = file;
    }

    /**
     * El {@code DomProcessor} de JSVG: recorre el SVG en orden del documento (sin recursión, que el anidado lo limita
     * {@code Limits}) y da a cada {@code <text>} y {@code <tspan>} su fuente de {@link ImageFile#textFonts()}.
     */
    void rewrite(DomElement root) {
        List<FontFile> fonts = file.textFonts();
        Deque<DomElement> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            DomElement element = stack.pop();
            String tag = element.tagName();
            if (tag.equals("text") || tag.equals("tspan")) {
                FontFile font = texts < fonts.size() ? fonts.get(texts) : null;
                if (font != null) {
                    apply(element, font);
                }
                texts++;
            }
            List<? extends DomElement> children = element.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
    }

    private void apply(DomElement element, FontFile font) {
        String key = PREFIX + font.digest();
        byKey.put(key, font);
        element.setAttribute("font-family", key);
        element.setAttribute("font-weight", font.face().weight() == FontWeight.BOLD ? "bold" : "normal");
        element.setAttribute("font-style", font.face().style() == FontStyle.ITALIC ? "italic" : "normal");
        String style = element.attribute("style");
        if (style != null) {
            List<String> kept = new ArrayList<>();
            for (String declaration : style.split(";")) {
                int colon = declaration.indexOf(':');
                String property = colon < 0 ? "" : declaration.substring(0, colon).strip().toLowerCase(Locale.ROOT);
                if (!FONT_PROPERTIES.contains(property)) {
                    kept.add(declaration);
                }
            }
            element.setAttribute("style", String.join(";", kept));
        }
    }

    /**
     * La fuente de una familia clave (JSVG la pasa en minúsculas; la huella ya lo está). Una familia que no es clave
     * es la de un texto sin fuente, que solo tiene espacios: JSVG usa la suya y no dibuja nada.
     */
    @Override
    public Font customFont(String family) {
        FontFile font = byKey.get(family);
        if (font == null) {
            return null;
        }
        return created.computeIfAbsent(family, key -> {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, font.path().toFile());
            } catch (FontFormatException | IOException e) {
                failure = "AWT no carga la fuente " + font.face() + " (" + e.getMessage() + ")";
                return null;
            }
        });
    }

    /**
     * Después de dibujar: que JSVG ha visto los mismos textos que {@code ImageFile} y ha podido cargar sus fuentes;
     * si no, habría dibujado alguno con otra fuente.
     *
     * @throws ImageFormatException si no
     */
    void check() {
        String name = file.path().getFileName().toString();
        if (texts != file.textFonts().size()) {
            throw new ImageFormatException("No se puede dibujar el SVG " + name + ": JSVG lee " + texts
                    + " elementos <text> y <tspan> y el motor " + file.textFonts().size());
        }
        if (failure != null) {
            throw new ImageFormatException("No se puede dibujar el texto del SVG " + name + ": " + failure);
        }
    }

    @Override
    public ImageObserver imageObserver() {
        return NullPlatformSupport.INSTANCE.imageObserver();
    }

    @Override
    @SuppressWarnings("deprecation")
    public TargetSurface targetSurface() {
        return NullPlatformSupport.INSTANCE.targetSurface();
    }
}
