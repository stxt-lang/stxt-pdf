import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the TeX hyphenation patterns of hyph-utf8 ({@code hyph-xx.tex}) into the plain-text resources of the engine
 * ({@code src/main/resources/dev/stxt/pdf/hyphenation/xx.txt}, reference/common/resources.md). The patterns and the
 * exceptions are copied as they are, one per line; what changes is the wrapping: the TeX commands and the comments
 * inside the blocks go away, and the header of the original (its copyright and its license, which have to be
 * preserved) is kept as comments.
 *
 * <pre>
 * java tools/HyphenationPatterns.java /path/to/hyph-utf8/tex/generic/hyph-utf8/patterns/tex   # from the repository root
 * </pre>
 *
 * <p>The originals are in https://github.com/hyphenation/tex-hyphen (folder
 * {@code hyph-utf8/tex/generic/hyph-utf8/patterns/tex}). Which languages are converted, and from which file, is the
 * table {@link #LANGUAGES}: the engine maps each {@code Language} to one of them.</p>
 */
public final class HyphenationPatterns {

    /** Resource name → file of hyph-utf8 it is converted from. */
    private static final Map<String, String> LANGUAGES = Map.of(
            "es", "hyph-es.tex",
            "ca", "hyph-ca.tex",
            "en-us", "hyph-en-us.tex",
            "en-gb", "hyph-en-gb.tex");

    private static final Path OUTPUT = Path.of("src", "main", "resources", "dev", "stxt", "pdf", "hyphenation");

    private static final Pattern BLOCK = Pattern.compile("\\\\(patterns|hyphenation)\\s*\\{");
    private static final Pattern MIN = Pattern.compile("^%\\s+(left|right):\\s*(\\d+)\\s*$");

    private HyphenationPatterns() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java tools/HyphenationPatterns.java <folder with the hyph-*.tex files>");
            System.exit(1);
        }
        Path source = Path.of(args[0]);
        Files.createDirectories(OUTPUT);
        for (Map.Entry<String, String> language : LANGUAGES.entrySet()) {
            Path in = source.resolve(language.getValue());
            Path out = OUTPUT.resolve(language.getKey() + ".txt");
            Files.writeString(out, convert(Files.readString(in, StandardCharsets.UTF_8), language.getValue()),
                    StandardCharsets.UTF_8);
            System.out.println(in + " -> " + out);
        }
    }

    /** The resource: the header of the original as comments, the minima, the patterns and the exceptions. */
    static String convert(String tex, String fileName) {
        StringBuilder out = new StringBuilder();
        out.append("# Hyphenation patterns converted from ").append(fileName).append(" of the hyph-utf8 package\n");
        out.append("# (https://github.com/hyphenation/tex-hyphen) by tools/HyphenationPatterns.java of stxt-pdf: the same\n");
        out.append("# patterns and exceptions in another format, one per line. This file is not the original and is not\n");
        out.append("# edited by hand: it is generated again from the original. The header of the original follows; its\n");
        out.append("# copyright notice and its license apply to this file.\n#\n");
        int left = -1;
        int right = -1;
        boolean typesetting = false;
        for (String line : tex.split("\n")) {
            if (!line.startsWith("%")) {
                break;
            }
            out.append('#').append(line.substring(1).stripTrailing()).append('\n');
            // The minima are those of "hyphenmins: typesetting:" (the ones for "generation" are not used).
            if (line.matches("^%\\s+typesetting:\\s*$")) {
                typesetting = true;
            } else if (line.matches("^%\\s+\\w+:\\s*$")) {
                typesetting = false;
            }
            Matcher m = MIN.matcher(line);
            if (m.matches() && typesetting) {
                if (m.group(1).equals("left")) {
                    left = Integer.parseInt(m.group(2));
                } else {
                    right = Integer.parseInt(m.group(2));
                }
            }
        }
        if (left < 0 || right < 0) {
            throw new IllegalArgumentException(fileName + ": the header has no hyphenmins/typesetting left and right");
        }
        out.append("left ").append(left).append('\n');
        out.append("right ").append(right).append('\n');
        out.append("patterns\n");
        block(tex, "patterns").forEach(p -> out.append(p).append('\n'));
        out.append("exceptions\n");
        block(tex, "hyphenation").forEach(p -> out.append(p).append('\n'));
        return out.toString();
    }

    /** The entries of the {@code \patterns{...}} or {@code \hyphenation{...}} block, without the TeX comments. */
    static List<String> block(String tex, String command) {
        List<String> entries = new ArrayList<>();
        Matcher m = BLOCK.matcher(tex);
        while (m.find()) {
            if (!m.group(1).equals(command)) {
                continue;
            }
            int end = tex.indexOf('}', m.end());
            for (String line : tex.substring(m.end(), end).split("\n")) {
                int comment = line.indexOf('%');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                for (String token : line.trim().split("\\s+")) {
                    if (!token.isEmpty()) {
                        if (token.indexOf('\\') >= 0) {
                            throw new IllegalArgumentException("TeX command inside the block: " + token);
                        }
                        entries.add(token);
                    }
                }
            }
        }
        return entries;
    }
}
