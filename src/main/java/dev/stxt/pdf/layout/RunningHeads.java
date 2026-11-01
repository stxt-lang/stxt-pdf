package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.tree.Box;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The running heads of each page ({@code {heading N}} and {@code {heading N: first}} in a {@code Page overlay},
 * reference/common/references.md): the text of the heading of each level in effect on a page, with its number if it
 * has one ({@link Box#headingText()}). The {@link Paginator} walks the pages in order and asks for each one; the
 * object keeps which heading of each level is in effect: the last one that has started so far, until a heading of a
 * higher level (a lower number) starts and leaves the levels below it without a heading, as a new chapter does with
 * its sections. Before the first heading of a level, its running head is empty.
 *
 * <p>{@code {heading N}} is the heading in effect at the end of the page; {@code {heading N: first}}, the first one of
 * that level that starts on the page or, if none starts on it, the same as {@code {heading N}}.</p>
 */
final class RunningHeads {

    private static final int LEVELS = 6;

    /** The heading of each level in effect so far, by level (1 to 6); empty where there is none. */
    private final String[] current = new String[LEVELS + 1];
    /** The ids of the references of the document ({@code "2"}, {@code "2: first"}), the keys of each page's values. */
    private final Set<String> keys = new LinkedHashSet<>();

    /** For the running heads of {@code references}, those of the document; only its heading references count. */
    RunningHeads(Collection<Reference> references) {
        Arrays.fill(current, "");
        references.stream().filter(r -> r.kind() == Reference.Kind.HEADING).forEach(r -> keys.add(r.id()));
    }

    /** Whether the document has any running head. */
    boolean any() {
        return !keys.isEmpty();
    }

    /**
     * The value of each reference on the page whose flow is {@code content}, the next one in order, and advances to
     * its end: the headings that start on it are in effect from then on.
     */
    Map<String, String> page(Fragment content) {
        String[] first = new String[LEVELS + 1];
        collect(content, first);
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : keys) {
            int level = key.charAt(0) - '0';
            boolean wantsFirst = key.endsWith(Reference.FIRST);
            values.put(key, wantsFirst && first[level] != null ? first[level] : current[level]);
        }
        return values;
    }

    /** Records the headings that start in the fragment, in order, and the first one of each level in {@code first}. */
    private void collect(Fragment f, String[] first) {
        Box box = f.box();
        int level = box.headingLevel();
        if (level > 0 && f.first()) {
            String text = box.headingText();
            if (first[level] == null) {
                first[level] = text;
            }
            current[level] = text;
            for (int below = level + 1; below <= LEVELS; below++) {
                current[below] = "";
            }
        }
        f.children().forEach(child -> collect(child, first));
    }
}
