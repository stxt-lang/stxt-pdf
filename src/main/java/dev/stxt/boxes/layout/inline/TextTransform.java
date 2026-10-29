package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.model.TextFormat;
import dev.stxt.boxes.model.TextRun;
import dev.stxt.boxes.model.Transform;
import dev.stxt.boxes.tree.Style;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las mayúsculas del texto al pintarlo: {@code Transform} y {@code Small caps}
 * (referencia/comun/formato-del-texto.md). Se aplica a los tramos antes de repartirlos en líneas, porque cambia lo
 * que miden (la «ß» pasa a «SS», las versalitas se pintan más pequeñas); el texto de la caja no
 * cambia.
 *
 * <p>Primero va {@code Transform} y después las versalitas: las minúsculas que quedan pasan a
 * mayúsculas con {@link TextFormat#smallCaps()}. Las reglas de cada letra son las del idioma
 * ({@link Locale}) de {@code Language}. Los marcadores {@code {page}} y {@code {pages}} se dejan
 * como están: se sustituyen al terminar de paginar ({@link PageNumbers}).</p>
 */
public final class TextTransform {

    private static final Pattern MARKER = Pattern.compile("\\{pages?}");

    private TextTransform() {
    }

    /** Si hay algo que transformar. */
    public static boolean applies(Style s) {
        return s.typography().transform() != Transform.NONE || s.typography().smallCaps();
    }

    /** Los tramos con la transformación y las versalitas de {@code s}; los mismos si no se piden. */
    public static List<TextRun> apply(List<TextRun> runs, Style s) {
        return applies(s) ? apply(runs, s.typography().transform(), s.typography().smallCaps(), s.typography().locale()) : runs;
    }

    /** Los tramos transformados, con los seguidos del mismo formato unidos. */
    public static List<TextRun> apply(List<TextRun> runs, Transform transform, boolean smallCaps,
                                      Locale locale) {
        TextRun.Joiner out = new TextRun.Joiner();
        // Para capitalize: si el carácter anterior (aunque sea de otro tramo) separa palabras.
        boolean wordStart = true;
        for (TextRun run : runs) {
            String text = run.text();
            Matcher m = MARKER.matcher(text);
            int at = 0;
            while (at < text.length()) {
                boolean marker = m.find(at);
                int end = marker ? m.start() : text.length();
                if (end > at) {
                    String piece = text.substring(at, end);
                    String changed = transform(piece, transform, wordStart, locale);
                    wordStart = Character.isWhitespace(piece.codePointBefore(piece.length()));
                    emit(out, changed, run.format(), smallCaps, locale);
                }
                if (!marker) {
                    break;
                }
                out.add(m.group(), run.format());
                wordStart = false;
                at = m.end();
            }
        }
        return out.build();
    }

    private static String transform(String text, Transform transform, boolean wordStart, Locale locale) {
        return switch (transform) {
            case NONE -> text;
            case UPPERCASE -> text.toUpperCase(locale);
            case LOWERCASE -> text.toLowerCase(locale);
            case CAPITALIZE -> capitalize(text, wordStart, locale);
        };
    }

    /** La primera letra de cada palabra en mayúscula; el resto, como estaba. */
    private static String capitalize(String text, boolean wordStart, Locale locale) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean start = wordStart;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            sb.append(start && Character.isLetter(cp) ? ch.toUpperCase(locale) : ch);
            start = Character.isWhitespace(cp);
            i += ch.length();
        }
        return sb.toString();
    }

    /** Añade el texto al tramo; con versalitas, sus minúsculas pasan a mayúsculas marcadas. */
    private static void emit(TextRun.Joiner out, String text, TextFormat format, boolean smallCaps, Locale locale) {
        if (!smallCaps) {
            out.add(text, format);
            return;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            i += ch.length();
            String upper = ch.toUpperCase(locale);
            boolean small = Character.isLowerCase(cp) && !upper.equals(ch);
            out.add(small ? upper : ch, format.withSmallCaps(small));
        }
    }
}
