package dev.stxt.pdf.resources;

import org.apache.fontbox.ttf.KerningSubtable;
import org.apache.fontbox.ttf.KerningTable;
import org.apache.fontbox.ttf.TTFTable;
import org.apache.fontbox.ttf.TrueTypeFont;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * The kerning pairs of a font (reference/common/typography.md#kerning-kerning): how much the advance of a glyph
 * changes when another one follows it, in units of the font. They come from the {@code kern} feature of the
 * {@code GPOS} table, which fontbox does not read, or, in a font without it, from the {@code kern} table, which
 * fontbox reads.
 *
 * <p>Of {@code GPOS} only what kerning needs is read: the pair adjustment lookups (type 2, in its two formats, also
 * inside extension lookups, type 9) of the {@code kern} features of the default script ({@code DFLT}) or, if it has
 * none, of the Latin one ({@code latn}), or, if neither has them, of every {@code kern} feature. A lookup gives the
 * value of the first of its subtables that has the pair, and the values of several lookups add up, as when a shaper
 * applies them one after the other. Only the advance of the first glyph ({@code XAdvance} of its value) is read: it is
 * what a horizontal kerning pair changes. The flags of the lookups (ignoring marks) are not applied: a mark between two
 * letters leaves them without a pair.</p>
 *
 * <p>Everything is read into arrays when the font is first asked for its pairs, and nothing changes afterwards: an
 * instance can be read from several threads without locks.</p>
 */
abstract class KerningPairs {

    /** The adjustment of the advance of {@code left} when {@code right} follows it, in units of the font. */
    abstract int adjustment(int left, int right);

    /**
     * The pairs of a font, or {@code null} if it has none. It reads the font: the caller does not share {@code ttf}
     * with another thread while it does.
     *
     * @throws IOException if a table cannot be read; a {@code GPOS} table with offsets outside it, too
     */
    static KerningPairs read(TrueTypeFont ttf) throws IOException {
        TTFTable table = ttf.getTableMap().get("GPOS");
        if (table != null) {
            KerningPairs gpos;
            try {
                gpos = Gpos.read(ttf.getTableBytes(table));
            } catch (IndexOutOfBoundsException e) {
                throw new IOException("its GPOS table has offsets outside it", e);
            }
            if (gpos != null) {
                return gpos;
            }
        }
        KerningTable kern = ttf.getKerning();
        KerningSubtable horizontal = kern != null ? kern.getHorizontalKerningSubtable() : null;
        return horizontal != null ? new Kern(horizontal) : null;
    }

    /** The pairs of the {@code kern} table, through fontbox, which keeps them in sorted arrays. */
    private static final class Kern extends KerningPairs {
        private final KerningSubtable subtable;

        Kern(KerningSubtable subtable) {
            this.subtable = subtable;
        }

        @Override
        int adjustment(int left, int right) {
            return subtable.getKerning(left, right);
        }
    }

    /** The pairs of the {@code kern} feature of {@code GPOS}: its lookups, in the order of the table. */
    private static final class Gpos extends KerningPairs {
        private static final int PAIR = 2;
        private static final int EXTENSION = 9;

        private final List<List<Subtable>> lookups;

        private Gpos(List<List<Subtable>> lookups) {
            this.lookups = lookups;
        }

        @Override
        int adjustment(int left, int right) {
            int total = 0;
            for (List<Subtable> lookup : lookups) {
                for (Subtable subtable : lookup) {
                    int value = subtable.adjustment(left, right);
                    if (value != Subtable.NOT_COVERED) {
                        total += value;
                        break;
                    }
                }
            }
            return total;
        }

        /** The pairs of a {@code GPOS} table (ISO/IEC 14496-22, 6.3), or {@code null} if it has no kerning. */
        static Gpos read(byte[] d) {
            Bytes b = new Bytes(d);
            int scripts = b.u16(4);
            int features = b.u16(6);
            int lookupList = b.u16(8);
            TreeSet<Integer> indices = kernLookups(b, scripts, features, "DFLT");
            if (indices.isEmpty()) {
                indices = kernLookups(b, scripts, features, "latn");
            }
            if (indices.isEmpty()) {
                int count = b.u16(features);
                for (int i = 0; i < count; i++) {
                    indices.addAll(lookupsOfKern(b, features, i));
                }
            }
            List<List<Subtable>> lookups = new ArrayList<>();
            int lookupCount = b.u16(lookupList);
            for (int index : indices) {
                if (index >= lookupCount) {
                    continue;
                }
                List<Subtable> lookup = lookup(b, lookupList + b.u16(lookupList + 2 + 2 * index));
                if (!lookup.isEmpty()) {
                    lookups.add(lookup);
                }
            }
            return lookups.isEmpty() ? null : new Gpos(lookups);
        }

        /** The lookups of the {@code kern} features of the default language of a script; empty if it has none. */
        private static TreeSet<Integer> kernLookups(Bytes b, int scripts, int features, String tag) {
            TreeSet<Integer> indices = new TreeSet<>();
            int count = b.u16(scripts);
            for (int i = 0; i < count; i++) {
                int record = scripts + 2 + 6 * i;
                if (!b.tag(record).equals(tag)) {
                    continue;
                }
                int script = scripts + b.u16(record + 4);
                int defaultLanguage = b.u16(script);
                if (defaultLanguage == 0) {
                    return indices;
                }
                int language = script + defaultLanguage;
                int featureCount = b.u16(language + 4);
                for (int f = 0; f < featureCount; f++) {
                    indices.addAll(lookupsOfKern(b, features, b.u16(language + 6 + 2 * f)));
                }
                return indices;
            }
            return indices;
        }

        /** The lookups of feature {@code index} of the list, if it is a {@code kern} feature. */
        private static List<Integer> lookupsOfKern(Bytes b, int features, int index) {
            List<Integer> lookups = new ArrayList<>();
            if (index >= b.u16(features)) {
                return lookups;
            }
            int record = features + 2 + 6 * index;
            if (!b.tag(record).equals("kern")) {
                return lookups;
            }
            int feature = features + b.u16(record + 4);
            int count = b.u16(feature + 2);
            for (int i = 0; i < count; i++) {
                lookups.add(b.u16(feature + 4 + 2 * i));
            }
            return lookups;
        }

        /** The pair adjustment subtables of a lookup, also those inside extensions; empty if it is of another type. */
        private static List<Subtable> lookup(Bytes b, int lookup) {
            int type = b.u16(lookup);
            int count = b.u16(lookup + 4);
            List<Subtable> subtables = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int at = lookup + b.u16(lookup + 6 + 2 * i);
                int subtableType = type;
                if (type == EXTENSION) {
                    subtableType = b.u16(at + 2);
                    at += b.u32(at + 4);
                }
                if (subtableType == PAIR) {
                    Subtable subtable = Subtable.read(b, at);
                    if (subtable != null) {
                        subtables.add(subtable);
                    }
                }
            }
            return subtables;
        }
    }

    /** A pair adjustment subtable ({@code PairPos}), in its format 1 (pairs of glyphs) or 2 (pairs of classes). */
    private abstract static class Subtable {
        /** What {@link #adjustment} gives when the subtable does not have the pair: the next one is looked at. */
        static final int NOT_COVERED = Integer.MIN_VALUE;
        private static final int X_ADVANCE = 0x0004;

        final Coverage coverage;

        Subtable(Coverage coverage) {
            this.coverage = coverage;
        }

        /** The adjustment of the pair, or {@link #NOT_COVERED}. */
        abstract int adjustment(int left, int right);

        static Subtable read(Bytes b, int at) {
            int format = b.u16(at);
            Coverage coverage = Coverage.read(b, at + b.u16(at + 2));
            int format1 = b.u16(at + 4);
            int format2 = b.u16(at + 6);
            if (format == 1) {
                return PairSets.read(b, at, coverage, format1, format2);
            }
            if (format == 2) {
                return ClassPairs.read(b, at, coverage, format1, format2);
            }
            return null;
        }

        /** The size of a value record of that format, in bytes: two for each field it has. */
        static int size(int valueFormat) {
            return 2 * Integer.bitCount(valueFormat & 0xFF);
        }

        /** The {@code XAdvance} of a value record of that format, or 0 if it does not have it. */
        static int xAdvance(Bytes b, int record, int valueFormat) {
            if ((valueFormat & X_ADVANCE) == 0) {
                return 0;
            }
            return b.s16(record + size(valueFormat & (X_ADVANCE - 1)));
        }
    }

    /** Format 1: for each first glyph, its second glyphs in order and the adjustment of each pair. */
    private static final class PairSets extends Subtable {
        private final int[][] seconds;
        private final int[][] values;

        private PairSets(Coverage coverage, int[][] seconds, int[][] values) {
            super(coverage);
            this.seconds = seconds;
            this.values = values;
        }

        static PairSets read(Bytes b, int at, Coverage coverage, int format1, int format2) {
            int count = b.u16(at + 8);
            int recordSize = 2 + size(format1) + size(format2);
            int[][] seconds = new int[count][];
            int[][] values = new int[count][];
            for (int i = 0; i < count; i++) {
                int set = at + b.u16(at + 10 + 2 * i);
                int pairs = b.u16(set);
                seconds[i] = new int[pairs];
                values[i] = new int[pairs];
                for (int p = 0; p < pairs; p++) {
                    int record = set + 2 + recordSize * p;
                    seconds[i][p] = b.u16(record);
                    values[i][p] = xAdvance(b, record + 2, format1);
                }
            }
            return new PairSets(coverage, seconds, values);
        }

        @Override
        int adjustment(int left, int right) {
            int index = coverage.index(left);
            if (index < 0 || index >= seconds.length) {
                return NOT_COVERED;
            }
            int found = Arrays.binarySearch(seconds[index], right);
            return found >= 0 ? values[index][found] : NOT_COVERED;
        }
    }

    /** Format 2: the class of each glyph and the adjustment of each pair of classes. */
    private static final class ClassPairs extends Subtable {
        private final ClassDef first;
        private final ClassDef second;
        private final int class1Count;
        private final int class2Count;
        /** {@code values[c1 * class2Count + c2]}. */
        private final int[] values;

        private ClassPairs(Coverage coverage, ClassDef first, ClassDef second, int class1Count, int class2Count,
                           int[] values) {
            super(coverage);
            this.first = first;
            this.second = second;
            this.class1Count = class1Count;
            this.class2Count = class2Count;
            this.values = values;
        }

        static ClassPairs read(Bytes b, int at, Coverage coverage, int format1, int format2) {
            ClassDef first = ClassDef.read(b, at + b.u16(at + 8));
            ClassDef second = ClassDef.read(b, at + b.u16(at + 10));
            int class1Count = b.u16(at + 12);
            int class2Count = b.u16(at + 14);
            int recordSize = size(format1) + size(format2);
            if ((long) class1Count * class2Count * Math.max(recordSize, 2) > b.d().length) {
                // A table that says it has more pairs than fit in it: nothing is allocated for them.
                throw new IndexOutOfBoundsException(class1Count + " × " + class2Count + " classes");
            }
            int[] values = new int[class1Count * class2Count];
            for (int c1 = 0; c1 < class1Count; c1++) {
                for (int c2 = 0; c2 < class2Count; c2++) {
                    int record = at + 16 + recordSize * (c1 * class2Count + c2);
                    values[c1 * class2Count + c2] = xAdvance(b, record, format1);
                }
            }
            return new ClassPairs(coverage, first, second, class1Count, class2Count, values);
        }

        @Override
        int adjustment(int left, int right) {
            if (coverage.index(left) < 0) {
                return NOT_COVERED;
            }
            int c1 = first.classOf(left);
            int c2 = second.classOf(right);
            if (c1 >= class1Count || c2 >= class2Count) {
                return NOT_COVERED;
            }
            return values[c1 * class2Count + c2];
        }
    }

    /** The glyphs a subtable applies to, each with its index: a sorted list of glyphs or of ranges. */
    private static final class Coverage {
        /** Format 1: the glyphs; format 2: the first glyph of each range. */
        private final int[] starts;
        /** Format 2: the last glyph of each range; {@code null} in format 1. */
        private final int[] ends;
        /** Format 2: the index of the first glyph of each range. */
        private final int[] indices;

        private Coverage(int[] starts, int[] ends, int[] indices) {
            this.starts = starts;
            this.ends = ends;
            this.indices = indices;
        }

        static Coverage read(Bytes b, int at) {
            int format = b.u16(at);
            int count = b.u16(at + 2);
            int[] starts = new int[count];
            if (format == 1) {
                for (int i = 0; i < count; i++) {
                    starts[i] = b.u16(at + 4 + 2 * i);
                }
                return new Coverage(starts, null, null);
            }
            int[] ends = new int[count];
            int[] indices = new int[count];
            for (int i = 0; i < count; i++) {
                int record = at + 4 + 6 * i;
                starts[i] = b.u16(record);
                ends[i] = b.u16(record + 2);
                indices[i] = b.u16(record + 4);
            }
            return new Coverage(starts, ends, indices);
        }

        /** The index of the glyph, or -1 if it is not covered. */
        int index(int glyph) {
            if (ends == null) {
                int found = Arrays.binarySearch(starts, glyph);
                return found >= 0 ? found : -1;
            }
            int range = Ranges.find(starts, ends, glyph);
            return range >= 0 ? indices[range] + glyph - starts[range] : -1;
        }
    }

    /** The class of each glyph; 0 for those it does not list. */
    private static final class ClassDef {
        /** Format 1: the first glyph and the class of each one from it; format 2: ranges with their class. */
        private final int[] starts;
        private final int[] ends;
        private final int[] classes;

        private ClassDef(int[] starts, int[] ends, int[] classes) {
            this.starts = starts;
            this.ends = ends;
            this.classes = classes;
        }

        static ClassDef read(Bytes b, int at) {
            int format = b.u16(at);
            if (format == 1) {
                int start = b.u16(at + 2);
                int count = b.u16(at + 4);
                int[] classes = new int[count];
                for (int i = 0; i < count; i++) {
                    classes[i] = b.u16(at + 6 + 2 * i);
                }
                return new ClassDef(new int[]{start}, null, classes);
            }
            int count = b.u16(at + 2);
            int[] starts = new int[count];
            int[] ends = new int[count];
            int[] classes = new int[count];
            for (int i = 0; i < count; i++) {
                int record = at + 4 + 6 * i;
                starts[i] = b.u16(record);
                ends[i] = b.u16(record + 2);
                classes[i] = b.u16(record + 4);
            }
            return new ClassDef(starts, ends, classes);
        }

        int classOf(int glyph) {
            if (ends == null) {
                int i = glyph - starts[0];
                return i >= 0 && i < classes.length ? classes[i] : 0;
            }
            int range = Ranges.find(starts, ends, glyph);
            return range >= 0 ? classes[range] : 0;
        }
    }

    private static final class Ranges {
        private Ranges() {
        }

        /** The range of sorted, disjoint ranges that holds {@code glyph}, or -1. */
        static int find(int[] starts, int[] ends, int glyph) {
            int low = 0;
            int high = starts.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (glyph < starts[mid]) {
                    high = mid - 1;
                } else if (glyph > ends[mid]) {
                    low = mid + 1;
                } else {
                    return mid;
                }
            }
            return -1;
        }
    }

    /** Big-endian reading of a table; an offset outside it throws {@link IndexOutOfBoundsException}. */
    private record Bytes(byte[] d) {
        int u16(int at) {
            return (d[at] & 0xFF) << 8 | d[at + 1] & 0xFF;
        }

        int s16(int at) {
            return (short) u16(at);
        }

        int u32(int at) {
            return u16(at) << 16 | u16(at + 2);
        }

        String tag(int at) {
            return new String(d, at, 4, StandardCharsets.US_ASCII);
        }
    }
}
