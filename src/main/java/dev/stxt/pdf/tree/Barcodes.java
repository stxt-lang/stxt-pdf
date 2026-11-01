package dev.stxt.pdf.tree;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.BarcodeFormat;
import dev.stxt.pdf.model.ErrorCorrection;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.Value;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static dev.stxt.pdf.tree.Nodes.error;

/**
 * The image of a {@code Barcode} (reference/components/barcode.md): ZXing encodes its data in its {@code Format} and
 * this class adds the quiet zone and the natural size of a module. The data is checked here, so that each error says
 * what fails with the line of the {@code Barcode} ({@code BARCODE_DATA}): ZXing would accept some data it cannot encode
 * as such (in Code 128, {@code ñ} is a function character) and its messages are not those of the engine.
 *
 * <ul>
 *   <li>QR code: its modules measure 0.5 mm, with a quiet zone of 4 modules on each side. Data that is only ASCII goes
 *       as is; any other goes in UTF-8, with its ECI designator, which says so to the reader.</li>
 *   <li>Code 128 and EAN-13: one row of modules of 0.33 mm and bars of 22.85 mm, the nominal size of EAN-13, with
 *       the quiet zones of each one (10 modules on each side in Code 128; 11 on the left and 7 on the right in
 *       EAN-13).</li>
 * </ul>
 *
 * <p>It is deterministic: the same data in the same format gives the same modules, and so the same PDF.</p>
 */
final class Barcodes implements ImageGenerator {

    static final ImageGenerator GENERATOR = new Barcodes();

    /** Natural width of the modules of a QR code: 0.5 mm. */
    static final double QR_MODULE = 0.5 * Value.POINTS_PER_MM;
    /** Quiet zone of a QR code, in modules, on each side. */
    static final int QR_QUIET_ZONE = 4;
    /** Natural width of the modules of a one-dimensional barcode: 0.33 mm, the nominal one of EAN-13. */
    static final double LINEAR_MODULE = 0.33 * Value.POINTS_PER_MM;
    /** Natural height of the bars of a one-dimensional barcode: 22.85 mm, the nominal one of EAN-13. */
    static final double LINEAR_HEIGHT = 22.85 * Value.POINTS_PER_MM;
    /** Most characters of a Code 128, as in ZXing. */
    static final int CODE_128_MAX = 80;

    private Barcodes() {
    }

    @Override
    public Modules generate(InlineNode n, Style s) {
        String data = data(n);
        return switch (s.image().format()) {
            case QR -> qr(n, data, s.image().errorCorrection());
            case CODE_128 -> code128(n, data);
            case EAN_13 -> ean13(n, data);
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

    private static Modules qr(InlineNode n, String data, ErrorCorrection correction) {
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
        int side = matrix.getWidth() + 2 * QR_QUIET_ZONE;
        BitSet dark = new BitSet();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y) == 1) {
                    dark.set((y + QR_QUIET_ZONE) * side + x + QR_QUIET_ZONE);
                }
            }
        }
        return new Modules(side, side, dark, QR_MODULE, QR_MODULE);
    }

    private static Modules code128(InlineNode n, String data) {
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
        return linear(new Code128Writer().encode(data), 10, 10);
    }

    private static Modules ean13(InlineNode n, String data) {
        if (!data.matches("[0-9]{12,13}")) {
            throw error(n, ErrorCode.BARCODE_DATA, "An EAN-13 is 12 digits, to which the check digit is added, or 13 "
                    + "with the check digit, and the data of the Barcode is " + data);
        }
        int check = checkDigit(data.substring(0, 12));
        if (data.length() == 13 && data.charAt(12) - '0' != check) {
            throw error(n, ErrorCode.BARCODE_DATA, "The check digit of the EAN-13 " + data + " is " + check + ", not "
                    + data.charAt(12));
        }
        return linear(new EAN13Writer().encode(data.substring(0, 12) + check), 11, 7);
    }

    /** The check digit of the 12 digits of an EAN-13: weights 1 and 3 from the left, up to the next ten. */
    static int checkDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            sum += (digits.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10;
    }

    /** One row with the bars of {@code bars} between the quiet zones. */
    private static Modules linear(boolean[] bars, int left, int right) {
        BitSet dark = new BitSet();
        for (int i = 0; i < bars.length; i++) {
            if (bars[i]) {
                dark.set(left + i);
            }
        }
        return new Modules(left + bars.length + right, 1, dark, LINEAR_MODULE, LINEAR_HEIGHT);
    }

    /** A character for a message: itself and its code point, which says what it is when it is not visible. */
    private static String describe(int codePoint) {
        String hex = String.format(Locale.ROOT, "U+%04X", codePoint);
        return Character.isISOControl(codePoint) ? hex : "'" + Character.toString(codePoint) + "' (" + hex + ")";
    }
}
