package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.Nodes.error;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.pdf417.encoder.PDF417;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.BarcodeText;
import dev.stxt.pdf.model.ErrorCorrection;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.Value;

/**
 * The image of a {@code Barcode} (reference/components/barcode.md): ZXing encodes its data in its {@code Barcode format} and
 * this class adds the quiet zone, the natural size of a module and, with {@code Readable text}, the text under the
 * bars. The data is checked here, so that each error says what fails with the line of the {@code Barcode}
 * ({@code BARCODE_DATA}): ZXing would accept some data it cannot encode as such (in Code 128, {@code ñ} is a function
 * character) and its messages are not those of the engine.
 *
 * <ul>
 *   <li>QR code and Data Matrix: their modules measure 0.5 mm, with a quiet zone of 4 modules on each side in a QR
 *       code and of 1 in a Data Matrix, which is always square. Data that is only ASCII goes as is; any other goes in
 *       UTF-8, with its ECI designator, which says so to the reader.</li>
 *   <li>PDF417: modules of 0.33 mm in rows of 1 mm, three times as tall as wide, with a quiet zone of 2 modules on
 *       each side and the error correction level that its standard recommends for the length of the data.</li>
 *   <li>Code 128, GS1-128 and EAN-13: one row of modules of 0.33 mm and bars of 22.85 mm, the nominal size of
 *       EAN-13, with the quiet zones of each one (10 modules on each side in Code 128 and GS1-128; 11 on the left and
 *       7 on the right in EAN-13).</li>
 *   <li>GS1-128 and GS1 Data Matrix: the data is a list of application identifiers ({@link Gs1}).</li>
 * </ul>
 *
 * <p>{@code Quiet zone} replaces the quiet zone of the format with that number of modules on each side.</p>
 *
 * <p>Each format has a narrowest module ({@link Modules#minModule()}): 0.25 mm in all of them but EAN-13, where it is
 * 80 % of the nominal one. The layout checks it once the box has its size ({@code BARCODE_TOO_SMALL}).</p>
 *
 * <p>It is deterministic: the same data in the same format gives the same modules, and so the same PDF.</p>
 */
final class Barcodes implements ImageGenerator {

    static final ImageGenerator GENERATOR = new Barcodes();

    /** Natural width of the modules of a QR code and of a Data Matrix: 0.5 mm. */
    static final double QR_MODULE = 0.5 * Value.POINTS_PER_MM;
    /** Quiet zone of a QR code, in modules, on each side. */
    static final int QR_QUIET_ZONE = 4;
    /** Quiet zone of a Data Matrix, in modules, on each side. */
    static final int DATA_MATRIX_QUIET_ZONE = 1;
    /** Natural width of the modules of a one-dimensional barcode and of a PDF417: 0.33 mm, the nominal one of EAN-13. */
    static final double LINEAR_MODULE = 0.33 * Value.POINTS_PER_MM;
    /** Natural height of the bars of a one-dimensional barcode: 22.85 mm, the nominal one of EAN-13. */
    static final double LINEAR_HEIGHT = 22.85 * Value.POINTS_PER_MM;
    /** Height of a row of a PDF417, in modules: 3, the lowest its standard allows. */
    static final int PDF417_ROW_HEIGHT = 3;
    /** Quiet zone of a PDF417, in modules, on each side. */
    static final int PDF417_QUIET_ZONE = 2;
    /**
     * Narrowest module of every format but EAN-13: 0.25 mm, about three dots of a printer at 300 dpi. Their standards
     * do not fix one.
     */
    static final double MIN_MODULE = 0.25 * Value.POINTS_PER_MM;
    /** Narrowest module of an EAN-13: 80 % of the nominal one, the smallest that its standard allows. */
    static final double EAN_13_MIN_MODULE = 0.8 * LINEAR_MODULE;
    /** Quiet zone of a Code 128 and of a GS1-128, in modules, on each side. */
    static final int CODE_128_QUIET_ZONE = 10;
    /** Quiet zone of an EAN-13, in modules, on the left. */
    static final int EAN_13_QUIET_ZONE_LEFT = 11;
    /** Quiet zone of an EAN-13, in modules, on the right. */
    static final int EAN_13_QUIET_ZONE_RIGHT = 7;
    /** Modules of each digit of an EAN-13, and those its first digit takes in the left quiet zone. */
    static final int EAN_13_DIGIT = 7;
    /**
     * Most modules of a declared {@code Quiet zone} on each side: more than the widest one of the standards (the
     * 6.35 mm of a Code 128, about 20 modules of 0.33 mm). A wider light margin is {@code Padding}.
     */
    static final int MAX_QUIET_ZONE = 40;
    /** Most characters of a Code 128, as in ZXing. */
    static final int CODE_128_MAX = 80;
    /** Most characters of a GS1-128, with its identifiers and the FNC1 between them, as its standard says. */
    static final int GS1_128_MAX = 48;
    /** The FNC1 of a Code 128 in the data that ZXing encodes. */
    private static final char CODE_128_FNC1 = '\u00f1';
    /** The FNC1 of a GS1 Data Matrix in the data that ZXing encodes: the group separator. */
    private static final char DATA_MATRIX_FNC1 = 0x1D;

    private Barcodes() {
    }

    @Override
    public Modules generate(InlineNode n, Style s) {
        String data = data(n);
        Integer quiet = s.image().quietZone();
        boolean text = s.image().readableText();
        return switch (s.image().barcodeFormat()) {
            case QR -> qr(n, data, s.image().errorCorrection(), quiet == null ? QR_QUIET_ZONE : quiet);
            case DATA_MATRIX -> dataMatrix(n, data, false, quiet == null ? DATA_MATRIX_QUIET_ZONE : quiet);
            case GS1_DATA_MATRIX -> dataMatrix(n, Gs1.encoded(Gs1.parse(n, data), DATA_MATRIX_FNC1, false), true,
                    quiet == null ? DATA_MATRIX_QUIET_ZONE : quiet);
            case PDF417 -> pdf417(n, data, quiet == null ? PDF417_QUIET_ZONE : quiet);
            case CODE_128 -> code128(n, data, quiet == null ? CODE_128_QUIET_ZONE : quiet, text);
            case GS1_128 -> gs1128(n, Gs1.parse(n, data), quiet == null ? CODE_128_QUIET_ZONE : quiet, text);
            case EAN_13 -> ean13(n, data, quiet == null ? EAN_13_QUIET_ZONE_LEFT : quiet,
                    quiet == null ? EAN_13_QUIET_ZONE_RIGHT : quiet, text);
        };
    }

    /**
     * The data of the {@code Barcode}: its value or, if it has several lines (a payment QR code, a contact card), its
     * {@code Content}, with the lines as they are, separated by a line feed. One of the two.
     */
    private static String data(InlineNode n) {
        Optional<Node> content = Nodes.child(n, "content");
        boolean value = !n.getText().isBlank();
        if (value && content.isPresent()) {
            throw error(content.get(), ErrorCode.BARCODE_DATA, "The Barcode has the data in the value and in Content "
                    + "(line " + n.getLine() + "): it goes in only one");
        }
        String data = content.map(Node::getText).orElse(n.getText().trim());
        if (data.isBlank()) {
            throw error(n, ErrorCode.BARCODE_DATA, "The Barcode needs its data: in the value, as in "
                    + "Barcode: https://example.com/verify/1234, or in a Content >> block if it has several lines");
        }
        return data;
    }

    private static Modules qr(InlineNode n, String data, ErrorCorrection correction, int quiet) {
        boolean ascii = data.chars().allMatch(c -> c < 0x80);
        Map<EncodeHintType, Object> hints = ascii ? Map.of() : Map.of(EncodeHintType.CHARACTER_SET, "UTF-8");
        ByteMatrix matrix;
        try {
            matrix = Encoder.encode(data, ErrorCorrectionLevel.valueOf(correction.name().substring(0, 1)), hints)
                    .getMatrix();
        } catch (WriterException e) {
            int bytes = data.getBytes(StandardCharsets.UTF_8).length;
            throw error(n, ErrorCode.BARCODE_DATA, "The data of the Barcode (" + bytes + " bytes) does not fit in a "
                    + "QR code with Error correction: " + correction.name().toLowerCase(Locale.ROOT) + ": it has to be "
                    + "shorter" + (correction == ErrorCorrection.LOW ? "" : ", or Error correction lower"));
        }
        ByteMatrix m = matrix;
        return matrix(m.getWidth(), m.getHeight(), (x, y) -> m.get(x, y) == 1, quiet, QR_MODULE, QR_MODULE);
    }

    /**
     * A Data Matrix (ECC 200), square: ZXing chooses the encoding that takes the fewest codewords. In a GS1 one,
     * {@code data} has a {@link #DATA_MATRIX_FNC1} where an FNC1 goes, without the first one
     * ({@link Gs1DataMatrix}).
     */
    private static Modules dataMatrix(InlineNode n, String data, boolean gs1, int quiet) {
        try {
            if (gs1) {
                boolean[][] rows = Gs1DataMatrix.encode(data, DATA_MATRIX_FNC1);
                return matrix(rows[0].length, rows.length, (x, y) -> rows[y][x], quiet, QR_MODULE, QR_MODULE);
            }
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.DATA_MATRIX_COMPACT, true);
            hints.put(EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_SQUARE);
            if (!data.chars().allMatch(c -> c < 0x80)) {
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            }
            BitMatrix matrix = new DataMatrixWriter().encode(data, com.google.zxing.BarcodeFormat.DATA_MATRIX, 0, 0,
                    hints);
            return matrix(matrix.getWidth(), matrix.getHeight(), matrix::get, quiet, QR_MODULE, QR_MODULE);
        } catch (IllegalArgumentException e) {
            int bytes = data.getBytes(StandardCharsets.UTF_8).length;
            throw error(n, ErrorCode.BARCODE_DATA, "The data of the Barcode (" + bytes + " bytes) does not fit in a "
                    + "Data Matrix: it has to be shorter");
        }
    }

    /**
     * A PDF417, with the error correction level its standard recommends for the length of the data and the columns
     * and rows that ZXing chooses.
     */
    private static Modules pdf417(InlineNode n, String data, int quiet) {
        PDF417 encoder = new PDF417();
        if (!data.chars().allMatch(c -> c < 0x80)) {
            encoder.setEncoding(StandardCharsets.UTF_8);
        }
        byte[][] rows;
        try {
            encoder.generateBarcodeLogic(data, pdf417Correction(data.length()));
            rows = encoder.getBarcodeMatrix().getMatrix();
        } catch (WriterException | IllegalArgumentException e) {
            int bytes = data.getBytes(StandardCharsets.UTF_8).length;
            throw error(n, ErrorCode.BARCODE_DATA, "The data of the Barcode (" + bytes + " bytes) does not fit in a "
                    + "PDF417: it has to be shorter");
        }
        // ZXing gives the rows from the bottom one to the top one.
        int height = rows.length;
        return matrix(rows[0].length, height, (x, y) -> rows[height - 1 - y][x] == 1, quiet, LINEAR_MODULE,
                PDF417_ROW_HEIGHT * LINEAR_MODULE);
    }

    /**
     * The error correction level of a PDF417 with {@code characters} of data: the lowest that ISO/IEC 15438
     * recommends for that many codewords, taking a codeword per character, which is never fewer than the real ones.
     */
    static int pdf417Correction(int characters) {
        if (characters <= 40) {
            return 2;
        }
        if (characters <= 160) {
            return 3;
        }
        return characters <= 320 ? 4 : 5;
    }

    /** Whether the module ({@code x}, {@code y}) of a matrix is dark. */
    @FunctionalInterface
    private interface Dark {
        boolean at(int x, int y);
    }

    /** A matrix of {@code width} × {@code height} modules with {@code quiet} light modules on each of its four sides. */
    private static Modules matrix(int width, int height, Dark matrix, int quiet, double moduleWidth,
                                  double moduleHeight) {
        int columns = width + 2 * quiet;
        BitSet dark = new BitSet();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.at(x, y)) {
                    dark.set((y + quiet) * columns + x + quiet);
                }
            }
        }
        return new Modules(columns, height + 2 * quiet, dark, moduleWidth, moduleHeight, MIN_MODULE);
    }

    private static Modules code128(InlineNode n, String data, int quiet, boolean text) {
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                throw error(n, ErrorCode.BARCODE_DATA, "Code 128 only encodes the printable ASCII characters, from "
                        + "the space to ~, and the data of the Barcode has " + describe(data.codePointAt(i)));
            }
        }
        if (data.length() > CODE_128_MAX) {
            throw error(n, ErrorCode.BARCODE_DATA, "The data of the Barcode has " + data.length() + " characters and a "
                    + "Code 128 holds at most " + CODE_128_MAX + ": a QR code holds more");
        }
        boolean[] bars = new Code128Writer().encode(data);
        return linear(bars, quiet, quiet, MIN_MODULE,
                text ? BarcodeText.centered(data, quiet + bars.length + quiet) : null);
    }

    /** A GS1-128: a Code 128 that starts with FNC1, with another one after each value of variable length. */
    private static Modules gs1128(InlineNode n, List<Gs1.Element> elements, int quiet, boolean text) {
        // Without the first FNC1, which is not data.
        int length = Gs1.encoded(elements, CODE_128_FNC1, false).length();
        if (length > GS1_128_MAX) {
            throw error(n, ErrorCode.BARCODE_DATA, "The data of the Barcode has " + length + " characters, with its "
                    + "application identifiers, and a GS1-128 holds at most " + GS1_128_MAX
                    + ": a GS1 Data Matrix holds more");
        }
        boolean[] bars = new Code128Writer().encode(Gs1.encoded(elements, CODE_128_FNC1, true));
        return linear(bars, quiet, quiet, MIN_MODULE,
                text ? BarcodeText.centered(Gs1.readable(elements), quiet + bars.length + quiet) : null);
    }

    private static Modules ean13(InlineNode n, String data, int left, int right, boolean text) {
        if (!data.matches("[0-9]{12,13}")) {
            throw error(n, ErrorCode.BARCODE_DATA, "An EAN-13 is 12 digits, to which the check digit is added, or 13 "
                    + "with the check digit, and the data of the Barcode is " + data);
        }
        int check = checkDigit(data.substring(0, 12));
        if (data.length() == 13 && data.charAt(12) - '0' != check) {
            throw error(n, ErrorCode.BARCODE_DATA, "The check digit of the EAN-13 " + data + " is " + check + ", not "
                    + data.charAt(12));
        }
        String digits = data.substring(0, 12) + check;
        return linear(new EAN13Writer().encode(digits), left, right, EAN_13_MIN_MODULE,
                text ? ean13Text(digits, left) : null);
    }

    /**
     * The text of an EAN-13 as on a product: the first digit in the left quiet zone, each of the others under its
     * seven modules, and the three guard patterns (start, middle and end) going down between them.
     *
     * @param left modules of the left quiet zone, where the bars start
     */
    private static BarcodeText ean13Text(String digits, int left) {
        List<BarcodeText.Label> labels = new ArrayList<>();
        labels.add(new BarcodeText.Label(digits.substring(0, 1), left - EAN_13_DIGIT, EAN_13_DIGIT));
        for (int i = 0; i < 12; i++) {
            // 3 modules of start guard and, before the last six digits, 5 of middle guard.
            int column = left + 3 + i * EAN_13_DIGIT + (i < 6 ? 0 : 5);
            labels.add(new BarcodeText.Label(digits.substring(i + 1, i + 2), column, EAN_13_DIGIT));
        }
        BitSet guards = new BitSet();
        guards.set(left, left + 3);
        guards.set(left + 45, left + 50);
        guards.set(left + 92, left + 95);
        return new BarcodeText(labels, guards);
    }

    /**
     * The check digit of the digits of an EAN-13, a GTIN or an SSCC: weights 3 and 1 from the right, up to the next
     * ten.
     */
    static int checkDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            sum += (digits.charAt(i) - '0') * ((digits.length() - 1 - i) % 2 == 0 ? 3 : 1);
        }
        return (10 - sum % 10) % 10;
    }

    /**
     * One row with the bars of {@code bars} between the quiet zones, with modules no narrower than {@code min} and,
     * if it is not {@code null}, {@code text} under them.
     */
    private static Modules linear(boolean[] bars, int left, int right, double min, BarcodeText text) {
        BitSet dark = new BitSet();
        for (int i = 0; i < bars.length; i++) {
            if (bars[i]) {
                dark.set(left + i);
            }
        }
        return new Modules(left + bars.length + right, 1, dark, LINEAR_MODULE, LINEAR_HEIGHT, min, text);
    }

    /** A character for a message: itself and its code point, which says what it is when it is not visible. */
    static String describe(int codePoint) {
        String hex = String.format(Locale.ROOT, "U+%04X", codePoint);
        return Character.isISOControl(codePoint) ? hex : "'" + Character.toString(codePoint) + "' (" + hex + ")";
    }
}
