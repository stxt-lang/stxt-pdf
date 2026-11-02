package dev.stxt.pdf.tree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static dev.stxt.pdf.tree.Nodes.error;

/**
 * The styled runs of a Markdown text, {@code [text]{name}} (reference/common/text-formatting.md), marked before
 * commonmark-java parses it. Its brackets are those of a link without a target, and commonmark-java neither lets a link
 * hold one nor puts one inside a link; so the {@code [} becomes {@link #OPEN} with the number of the name and the
 * {@code ]{name}}, {@link #CLOSE}, which the parser takes as letters, and the {@link MarkdownBuilder} finds them in the
 * text. The marks are noncharacters of Unicode, which a text never has.
 *
 * <p>What is not Markdown text keeps its brackets: an escaped bracket ({@code \[}), the inside of inline code, the
 * target of a link ({@code (...)}), the brackets of an image ({@code ![...]}) and those that a blank line separates.
 * Anything other than a single word in the braces ({@code [x]{page of: id}}, {@code [x]{a b}}) is not a styled run
 * either, nor are the page numbers ({@code [x]{page}}). A code block, which the marking does not tell apart, gets its text back with {@link #unmark}.</p>
 */
final class RunMarks {

    /** The start of a styled run, followed by the number of its name ({@link #DIGIT}) and {@link #END}. */
    static final char OPEN = '\uFDD0';
    /** The end of the number of the name after {@link #OPEN}. */
    static final char END = '\uFDD1';
    /** The end of a styled run. */
    static final char CLOSE = '\uFDD2';
    /** The digit 0 of the number of a name, in base 16: U+FDE0 to U+FDEF. */
    private static final char DIGIT = '\uFDE0';

    /** A text with its styled runs marked and their names, in the order of their numbers. */
    record Marked(String text, List<String> names) {
    }

    private record Span(int open, int close, int end, String name) {
    }

    private RunMarks() {
    }

    /**
     * Marks the styled runs of a Markdown text.
     *
     * @param line line of the document where the text begins, for the error of a text that already has a mark
     */
    static Marked mark(String text, int line) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) >= OPEN && text.charAt(i) <= '\uFDEF') {
                throw error(line, ErrorCode.MARKDOWN_UNSUPPORTED, String.format("The text has the noncharacter "
                        + "U+%04X, which is not text", (int) text.charAt(i)));
            }
        }
        List<Span> spans = spans(text);
        if (spans.isEmpty()) {
            return new Marked(text, List.of());
        }
        spans.sort((a, b) -> Integer.compare(a.open(), b.open()));
        List<String> names = new ArrayList<>();
        // The positions where something is replaced: each [ with its OPEN and each ]{name} with CLOSE.
        StringBuilder out = new StringBuilder(text.length() + spans.size() * 4);
        List<int[]> closes = new ArrayList<>();
        for (Span s : spans) {
            closes.add(new int[]{s.close(), s.end()});
        }
        closes.sort((a, b) -> Integer.compare(a[0], b[0]));
        int nextOpen = 0;
        int nextClose = 0;
        for (int i = 0; i < text.length(); ) {
            if (nextOpen < spans.size() && spans.get(nextOpen).open() == i) {
                out.append(OPEN).append(number(names.size())).append(END);
                names.add(spans.get(nextOpen).name());
                nextOpen++;
                i++;
            } else if (nextClose < closes.size() && closes.get(nextClose)[0] == i) {
                out.append(CLOSE);
                i = closes.get(nextClose)[1];
                nextClose++;
            } else {
                out.append(text.charAt(i++));
            }
        }
        return new Marked(out.toString(), List.copyOf(names));
    }

    /** The text with its marks written again as {@code [text]{name}}: what a code block shows. */
    static String unmark(String text, List<String> names) {
        if (names.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        Deque<String> open = new ArrayDeque<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == OPEN) {
                int end = text.indexOf(END, i);
                open.push(names.get(number(text, i + 1, end)));
                out.append('[');
                i = end;
            } else if (c == CLOSE) {
                out.append("]{").append(open.isEmpty() ? "" : open.pop()).append('}');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** The number written from {@code from} to {@code to} (exclusive). */
    static int number(String text, int from, int to) {
        int n = 0;
        for (int i = from; i < to; i++) {
            n = n * 16 + (text.charAt(i) - DIGIT);
        }
        return n;
    }

    private static String number(int n) {
        return Integer.toHexString(n).chars().mapToObj(c -> String.valueOf((char) (DIGIT + Character.digit(c, 16))))
                .reduce("", String::concat);
    }

    /**
     * The styled runs of the text: each {@code ]} followed by {@code {name}} closes the {@code [} that opens it,
     * walking the text as the inline Markdown does.
     */
    private static List<Span> spans(String text) {
        List<Span> spans = new ArrayList<>();
        // The open brackets: their position, or -1 for that of an image, which does not open a styled run.
        Deque<Integer> brackets = new ArrayDeque<>();
        int n = text.length();
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < n && isPunctuation(text.charAt(i + 1))) {
                i += 2;
            } else if (c == '`') {
                i = afterCode(text, i);
            } else if (c == '\n') {
                if (blankLineAfter(text, i)) {
                    // A blank line ends the paragraph: no bracket goes past it.
                    brackets.clear();
                }
                i++;
            } else if (c == '[') {
                boolean image = i > 0 && text.charAt(i - 1) == '!' && !(i > 1 && text.charAt(i - 2) == '\\');
                brackets.push(image ? -1 : i);
                i++;
            } else if (c == ']' && !brackets.isEmpty()) {
                int open = brackets.pop();
                if (i + 1 < n && text.charAt(i + 1) == '(') {
                    i = afterTarget(text, i + 1);
                    continue;
                }
                int end = i + 1 < n && text.charAt(i + 1) == '{' ? text.indexOf('}', i + 2) : -1;
                String name = end > i + 2 ? text.substring(i + 2, end) : null;
                if (open >= 0 && isName(name)) {
                    spans.add(new Span(open, i, end + 1, name));
                    i = end + 1;
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
        return spans;
    }

    /**
     * Whether what is in the braces is the name of a style: a single word, which is not {@code page} or {@code pages},
     * the page numbers ({@code [x]{page}} is a bracket and the number of the page).
     */
    private static boolean isName(String name) {
        return name != null && !name.equals("page") && !name.equals("pages")
                && name.chars().noneMatch(ch -> Character.isWhitespace(ch) || ch == '{');
    }

    /** After the inline code that starts at {@code i}, or after its backticks if nothing closes them. */
    private static int afterCode(String text, int i) {
        int run = i;
        while (run < text.length() && text.charAt(run) == '`') {
            run++;
        }
        int length = run - i;
        for (int j = run; j < text.length(); ) {
            if (text.charAt(j) != '`') {
                j++;
                continue;
            }
            int close = j;
            while (close < text.length() && text.charAt(close) == '`') {
                close++;
            }
            if (close - j == length) {
                return close;
            }
            j = close;
        }
        return run;
    }

    /** After the target of a link, {@code (...)} from {@code i}, with its parentheses balanced. */
    private static int afterTarget(String text, int i) {
        int depth = 0;
        for (int j = i; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '\\') {
                j++;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return j + 1;
            }
        }
        return i + 1;
    }

    /** Whether the line after the line break at {@code i} is blank. */
    private static boolean blankLineAfter(String text, int i) {
        for (int j = i + 1; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '\n') {
                return true;
            }
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    /** The ASCII punctuation that a backslash escapes in Markdown. */
    private static boolean isPunctuation(char c) {
        return c < 128 && "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".indexOf(c) >= 0;
    }
}
