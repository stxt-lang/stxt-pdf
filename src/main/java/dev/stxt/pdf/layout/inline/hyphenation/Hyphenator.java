package dev.stxt.pdf.layout.inline.hyphenation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Liang's hyphenation algorithm, the one of TeX, with the patterns of one language (reference/common/typography.md).
 *
 * <p>Each pattern is a sequence of letters with digits between them ({@code 1b}, {@code 4b.}, {@code .ach4},
 * {@code 2c3h}): the digit is the level of the position between the two letters it separates, and a dot marks the start
 * or the end of the word. A word is hyphenated by surrounding it with dots, taking every pattern that matches a
 * substring and keeping, at each position, the highest level any pattern gives it: the word may be hyphenated where the
 * level is odd. An exception ({@code as-so-ciate}) replaces the patterns for that word: it is hyphenated exactly where
 * its hyphens are. The minima ({@code left}, {@code right}) are the letters that have to stay on each side of a
 * hyphen, those the patterns were made for.</p>
 *
 * <p>The patterns are kept in a trie of their letters; the word is walked once from each of its positions.
 * Matching is on the word in lower case ({@link Locale#ROOT}, like the {@code \lccode} of TeX). The result of each word
 * is memorized: the same words repeat along a document.</p>
 *
 * <p>A hyphenator is immutable once built and can be shared between threads.</p>
 */
public final class Hyphenator {

    /** Entries memorized before the cache is emptied, so that a hostile document does not grow it without bound. */
    private static final int CACHE_LIMIT = 50_000;

    /** A node of the pattern trie: its children by letter and, if a pattern ends here, its levels. */
    private static final class Node {
        private final Map<Character, Node> children = new HashMap<>(2);
        /** Levels of the pattern that ends at this node, one per position (letters + 1); {@code null} if none. */
        private byte[] levels;
    }

    private final Node root = new Node();
    /** Exceptions: lower-case word → positions where it may be hyphenated. */
    private final Map<String, int[]> exceptions = new HashMap<>();
    private final int left;
    private final int right;
    private final ConcurrentHashMap<String, int[]> cache = new ConcurrentHashMap<>();

    /**
     * A hyphenator with these patterns and exceptions, in the TeX notation.
     *
     * @param left  minimum number of letters before a hyphen
     * @param right minimum number of letters after a hyphen
     */
    public Hyphenator(List<String> patterns, List<String> exceptions, int left, int right) {
        if (left < 1 || right < 1) {
            throw new IllegalArgumentException("The minima have to be at least 1: " + left + ", " + right);
        }
        this.left = left;
        this.right = right;
        patterns.forEach(this::addPattern);
        exceptions.forEach(this::addException);
    }

    /**
     * Reads a hyphenator from the resource format of the engine (what {@code tools/HyphenationPatterns.java} writes):
     * comment lines ({@code #}), {@code left N}, {@code right N}, a {@code patterns} line followed by one pattern per
     * line and an {@code exceptions} line followed by one exception per line.
     */
    public static Hyphenator read(InputStream in) {
        List<String> patterns = new ArrayList<>();
        List<String> exceptions = new ArrayList<>();
        int left = -1;
        int right = -1;
        List<String> current = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.equals("patterns")) {
                    current = patterns;
                } else if (line.equals("exceptions")) {
                    current = exceptions;
                } else if (current != null) {
                    current.add(line);
                } else if (line.startsWith("left ")) {
                    left = Integer.parseInt(line.substring(5).strip());
                } else if (line.startsWith("right ")) {
                    right = Integer.parseInt(line.substring(6).strip());
                } else {
                    throw new IllegalArgumentException("Unexpected line in the hyphenation patterns: " + line);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (left < 0 || right < 0) {
            throw new IllegalArgumentException("The hyphenation patterns have no left and right minima");
        }
        return new Hyphenator(patterns, exceptions, left, right);
    }

    /** Minimum number of letters before a hyphen. */
    public int left() {
        return left;
    }

    /** Minimum number of letters after a hyphen. */
    public int right() {
        return right;
    }

    /**
     * The positions of {@code word} where a hyphen may be inserted (the number of characters before it), in order;
     * empty if it cannot be hyphenated. {@code word} is a run of letters without spaces, digits or punctuation: the
     * caller gives it the letters of the word alone. The minima apply to it: a position before {@code left} letters or
     * after which fewer than {@code right} remain is never given.
     */
    public int[] breaks(String word) {
        int[] cached = cache.get(word);
        if (cached != null) {
            return cached.clone();
        }
        int[] result = compute(word);
        if (cache.size() >= CACHE_LIMIT) {
            cache.clear();
        }
        cache.put(word, result);
        return result.clone();
    }

    private int[] compute(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (lower.length() != word.length()) {
            // A letter whose lower case has another length (an upper-case İ): the positions would not match.
            return new int[0];
        }
        int n = lower.length();
        if (n < left + right) {
            return new int[0];
        }
        int[] exception = exceptions.get(lower);
        if (exception != null) {
            return within(exception, n);
        }
        // levels[i]: level of the position before letter i of the word (0 = start, n = end).
        int[] levels = new int[n + 1];
        String dotted = "." + lower + ".";
        for (int start = 0; start < dotted.length(); start++) {
            Node node = root;
            for (int i = start; i < dotted.length(); i++) {
                node = node.children.get(dotted.charAt(i));
                if (node == null) {
                    break;
                }
                if (node.levels != null) {
                    // The pattern covers dotted[start..i]: its level k is the position before dotted[start + k], which
                    // is position start + k - 1 of the word (the leading dot shifts it by one).
                    for (int k = 0; k < node.levels.length; k++) {
                        int position = start + k - 1;
                        if (position >= 0 && position <= n && node.levels[k] > levels[position]) {
                            levels[position] = node.levels[k];
                        }
                    }
                }
            }
        }
        List<Integer> odd = new ArrayList<>();
        for (int position = left; position <= n - right; position++) {
            if (levels[position] % 2 == 1) {
                odd.add(position);
            }
        }
        return odd.stream().mapToInt(Integer::intValue).toArray();
    }

    /** The positions of an exception that respect the minima. */
    private int[] within(int[] positions, int n) {
        List<Integer> kept = new ArrayList<>();
        for (int p : positions) {
            if (p >= left && p <= n - right) {
                kept.add(p);
            }
        }
        return kept.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Adds a pattern in TeX notation: letters and dots with digits between them. */
    private void addPattern(String pattern) {
        StringBuilder letters = new StringBuilder();
        List<Byte> levels = new ArrayList<>();
        // The level before the next letter; levels has one entry per letter plus one at the end.
        byte pending = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c >= '0' && c <= '9') {
                pending = (byte) (c - '0');
            } else {
                letters.append(c);
                levels.add(pending);
                pending = 0;
            }
        }
        levels.add(pending);
        if (letters.length() == 0) {
            throw new IllegalArgumentException("A hyphenation pattern needs letters: " + pattern);
        }
        Node node = root;
        for (int i = 0; i < letters.length(); i++) {
            node = node.children.computeIfAbsent(letters.charAt(i), k -> new Node());
        }
        byte[] array = new byte[levels.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = levels.get(i);
        }
        node.levels = array;
    }

    /** Adds an exception in TeX notation: the word with a hyphen at each allowed position ({@code as-so-ciate}). */
    private void addException(String exception) {
        StringBuilder word = new StringBuilder();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < exception.length(); i++) {
            char c = exception.charAt(i);
            if (c == '-') {
                positions.add(word.length());
            } else {
                word.append(c);
            }
        }
        exceptions.put(word.toString().toLowerCase(Locale.ROOT), positions.stream().mapToInt(Integer::intValue).toArray());
    }
}
