package dev.stxt.pdf.paint;

import com.github.weisj.jsvg.parser.DomElement;
import com.github.weisj.jsvg.renderer.NullPlatformSupport;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.resources.FontFile;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.ImageFormatException;

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
 * The fonts with which JSVG draws the text of an SVG (reference/components/image.md): the files of {@code fonts/}
 * that {@link ImageFile} checked, without registering anything in AWT or looking up any font by its name. One per
 * drawing.
 *
 * <p>JSVG looks up the font by family in AWT, which has two problems: with the JVM in Turkish it does not find
 * {@code IBM Plex Sans} (it lowercases {@code I} with the locale and gets {@code ıbm}) and draws with {@code Dialog}
 * without warning, and the AWT registry is JVM-wide, so two resources folders with the same family
 * share the first one. That is why, when reading the SVG, {@link #rewrite} sets on each
 * {@code <text>} and {@code <tspan>} the font that {@code ImageFile} checked: a key family
 * ({@code stxt-font-} and the digest of the file, {@link FontFile#digest()}), which belongs to no installed font, and
 * its explicit weight and style. JSVG does not find it in AWT and asks {@link #customFont} for it, which creates it
 * with {@link Font#createFont} from the content of the file.</p>
 *
 * <p>JSVG keeps the fonts it resolves in a JVM-wide cache whose key does not include this object: that is why the
 * key is the digest of the content, and what it keeps is valid for any folder with that same file.</p>
 */
final class SvgFonts implements PlatformSupport {

    private static final String PREFIX = "stxt-font-";
    /** The font properties removed from {@code style}, so that the ones set take precedence. */
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
     * The {@code DomProcessor} of JSVG: walks the SVG in document order (without recursion, since {@code Limits}
     * limits the nesting) and gives each {@code <text>} and {@code <tspan>} its font from
     * {@link ImageFile#textFonts()}.
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
        element.setAttribute("font-weight", Integer.toString(font.face().weight().value()));
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
     * The font of a key family (JSVG passes it in lowercase; the digest already is). A family that is not a key
     * is that of a text without a font, which only has spaces: JSVG uses its own and draws nothing.
     */
    @Override
    public Font customFont(String family) {
        FontFile font = byKey.get(family);
        if (font == null) {
            return null;
        }
        return created.computeIfAbsent(family, key -> {
            try {
                // From the bytes and not from the file: AWT keeps the file it is given open while the font lives,
                // and on Windows it could not be deleted or replaced. They are the ones that were measured.
                return Font.createFont(Font.TRUETYPE_FONT, font.content());
            } catch (FontFormatException | IOException e) {
                failure = "AWT does not load the font " + font.face() + " (" + e.getMessage() + ")";
                return null;
            }
        });
    }

    /**
     * After drawing: that JSVG has seen the same texts as {@code ImageFile} and has been able to load their fonts;
     * otherwise, it would have drawn some with another font.
     *
     * @throws ImageFormatException if not
     */
    void check() {
        String name = file.path().getFileName().toString();
        if (texts != file.textFonts().size()) {
            throw new ImageFormatException("The SVG " + name + " cannot be drawn: JSVG reads " + texts
                    + " <text> and <tspan> elements and the engine " + file.textFonts().size());
        }
        if (failure != null) {
            throw new ImageFormatException("The text of the SVG " + name + " cannot be drawn: " + failure);
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
