package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A run of text with its format (reference/common/text-formatting.md). The text of a box is a list of
 * consecutive runs; an unformatted text is a single {@link TextFormat#PLAIN} run.
 *
 * @param text   the text of the run, which may hold {@link #LINE_SEPARATOR}; {@link #IMAGE} in a run that is an image
 * @param format the format of the run; {@link TextFormat#PLAIN} if it has none
 */
public record TextRun(String text, TextFormat format) {

    /**
     * A forced line break in the text (reference/components/markdown.md): the one a Markdown line break becomes. It
     * separates words like a space, ends the line there and is never painted.
     */
    public static final char LINE_SEPARATOR = '\u2028';

    /**
     * The text of a run that is an image ({@link TextFormat#image()}, reference/common/text-formatting.md): the object
     * replacement character, which stands for the image in the text and is never painted nor read.
     */
    public static final char OBJECT_REPLACEMENT = '\uFFFC';

    /** The text of a run that is an image: a single {@link #OBJECT_REPLACEMENT}. */
    public static final String IMAGE = String.valueOf(OBJECT_REPLACEMENT);

    public TextRun {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(format, "format");
    }

    /**
     * An unformatted text.
     *
     * @param text the text; not {@code null}
     * @return a single {@link TextFormat#PLAIN} run with that text
     */
    public static List<TextRun> plain(String text) {
        return List.of(new TextRun(text, TextFormat.PLAIN));
    }

    /**
     * {@return the text of the runs, joined}
     *
     * @param runs the runs, in order
     */
    public static String text(List<TextRun> runs) {
        return runs.size() == 1 ? runs.get(0).text() : runs.stream().map(TextRun::text).collect(Collectors.joining());
    }

    /**
     * {@return whether some run has a format other than {@link TextFormat#PLAIN}}
     *
     * @param runs the runs of a text
     */
    public static boolean formatted(List<TextRun> runs) {
        return runs.stream().anyMatch(r -> !r.format().equals(TextFormat.PLAIN));
    }

    /** Accumulates runs in order, joining consecutive ones that have the same format. */
    public static final class Joiner {
        private final List<TextRun> runs = new ArrayList<>(1);
        private final StringBuilder text = new StringBuilder();
        private TextFormat format;

        public Joiner add(String s, TextFormat f) {
            // Two images with the same format are two runs: each one is drawn where its own run is.
            if (format != null && (!format.equals(f) || f.image() != null)) {
                flush();
            }
            format = f;
            text.append(s);
            return this;
        }

        /** The accumulated runs; empty if nothing has been added. */
        public List<TextRun> build() {
            if (format != null && text.length() > 0) {
                flush();
            }
            return List.copyOf(runs);
        }

        private void flush() {
            runs.add(new TextRun(text.toString(), format));
            text.setLength(0);
        }
    }
}
