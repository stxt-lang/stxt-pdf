package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A run of text with its format (reference/common/text-formatting.md). The text of a box is a list of
 * consecutive runs; an unformatted text is a single {@link TextFormat#PLAIN} run.
 */
public record TextRun(String text, TextFormat format) {

    public TextRun {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(format, "format");
    }

    /** An unformatted text. */
    public static List<TextRun> plain(String text) {
        return List.of(new TextRun(text, TextFormat.PLAIN));
    }

    /** The text of the runs, joined. */
    public static String text(List<TextRun> runs) {
        return runs.size() == 1 ? runs.get(0).text() : runs.stream().map(TextRun::text).collect(Collectors.joining());
    }

    /** Whether some run has a format. */
    public static boolean formatted(List<TextRun> runs) {
        return runs.stream().anyMatch(r -> !r.format().equals(TextFormat.PLAIN));
    }

    /** Accumulates runs in order, joining consecutive ones that have the same format. */
    public static final class Joiner {
        private final List<TextRun> runs = new ArrayList<>(1);
        private final StringBuilder text = new StringBuilder();
        private TextFormat format;

        public Joiner add(String s, TextFormat f) {
            if (format != null && !format.equals(f)) {
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
