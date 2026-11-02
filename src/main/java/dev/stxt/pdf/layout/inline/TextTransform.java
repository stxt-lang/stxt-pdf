package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.TextCase;
import dev.stxt.pdf.tree.Style;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The case of the text when painting it: {@code Case} and {@code Small caps}
 * (reference/common/text-formatting.md). It is applied to the runs before breaking them into lines, because it
 * changes what they measure ("ß" becomes "SS", small caps are painted smaller); the text of the box does not
 * change.
 *
 * <p>{@code Case} goes first and then the small caps: the lowercase letters that remain become
 * uppercase with {@link TextFormat#smallCaps()}. A styled run that declares its own {@code Case} or {@code Small caps}
 * (reference/common/text-formatting.md) uses them instead of those of the box. The rules of each letter are those of the language
 * ({@link Locale}) of {@code Language}. The markers {@code {page}} and {@code {pages}} are left
 * as they are: they are replaced when pagination ends ({@link PageNumbers}).</p>
 */
public final class TextTransform {

    private static final Pattern MARKER = Pattern.compile("\\{pages?}");

    private TextTransform() {
    }

    /** Whether there is something to transform in a text of a box with style {@code s}, without counting its runs. */
    public static boolean applies(Style s) {
        return s.typography().textCase() != TextCase.NONE || s.typography().smallCaps();
    }

    /**
     * Whether there is something to transform in the runs of a text of a box with style {@code s}: in the box or in a
     * styled run that declares its own {@code Case} or {@code Small caps}.
     */
    public static boolean applies(List<TextRun> runs, Style s) {
        return applies(s) || runs.stream().anyMatch(r -> r.format().style() != null
                && (r.format().textCase(TextCase.NONE) != TextCase.NONE || r.format().smallCaps(false)));
    }

    /**
     * The runs with the transformation and the small caps of {@code s} and of their styles; the same ones if none is
     * requested.
     */
    public static List<TextRun> apply(List<TextRun> runs, Style s) {
        return applies(runs, s)
                ? apply(runs, s.typography().textCase(), s.typography().smallCaps(), s.typography().locale()) : runs;
    }

    /**
     * The transformed runs, with consecutive ones of the same format joined. {@code transform} and {@code smallCaps}
     * are those of the box; a styled run that declares its own uses them instead.
     */
    public static List<TextRun> apply(List<TextRun> runs, TextCase transform, boolean smallCaps,
                                      Locale locale) {
        TextRun.Joiner out = new TextRun.Joiner();
        // For capitalize: whether the previous character (even from another run) separates words.
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
                    String changed = transform(piece, run.format().textCase(transform), wordStart, locale);
                    wordStart = Character.isWhitespace(piece.codePointBefore(piece.length()));
                    emit(out, changed, run.format(), run.format().smallCaps(smallCaps), locale);
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

    private static String transform(String text, TextCase transform, boolean wordStart, Locale locale) {
        return switch (transform) {
            case NONE -> text;
            case UPPERCASE -> text.toUpperCase(locale);
            case LOWERCASE -> text.toLowerCase(locale);
            case CAPITALIZE -> capitalize(text, wordStart, locale);
        };
    }

    /** The first letter of each word in uppercase; the rest, as it was. */
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

    /** Adds the text to the run; with small caps, its lowercase letters become marked uppercase letters. */
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
