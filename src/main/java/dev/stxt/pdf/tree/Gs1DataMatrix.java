package dev.stxt.pdf.tree;

import com.google.zxing.datamatrix.encoder.DefaultPlacement;
import com.google.zxing.datamatrix.encoder.ErrorCorrection;
import com.google.zxing.datamatrix.encoder.SymbolInfo;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;

/**
 * The matrix of a GS1 Data Matrix (reference/components/barcode.md, {@code gs1-data-matrix}). The data is encoded
 * here, in the ASCII encodation of ISO/IEC 16022 (a codeword per character, one per two digits and 232 for FNC1), and
 * ZXing adds the error correction and places the modules. The encoder of ZXing that knows FNC1, the one of its compact
 * mode, writes it wrong inside a run of capitals and digits (it shifts to the wrong set), so it is not used. GS1 data
 * is mostly digits, which this encodation packs in pairs.
 */
final class Gs1DataMatrix {

    /** The codeword of FNC1. */
    private static final char FNC1 = 232;
    /** The codeword that ends the data when it does not fill the symbol. */
    private static final char PAD = 129;

    private Gs1DataMatrix() {
    }

    /**
     * The modules of the smallest square symbol that holds {@code data}, row by row from the top, without quiet
     * zone: {@code [row][column]}.
     *
     * @param data ASCII characters, with {@code fnc1} where an FNC1 goes; the symbol starts with one
     * @throws IllegalArgumentException if no symbol holds it
     */
    static boolean[][] encode(String data, char fnc1) {
        StringBuilder codewords = new StringBuilder().append(FNC1);
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c == fnc1) {
                codewords.append(FNC1);
            } else if (digit(c) && i + 1 < data.length() && digit(data.charAt(i + 1))) {
                codewords.append((char) (130 + (c - '0') * 10 + data.charAt(i + 1) - '0'));
                i++;
            } else {
                codewords.append((char) (c + 1));
            }
        }
        SymbolInfo symbol = SymbolInfo.lookup(codewords.length(), SymbolShapeHint.FORCE_SQUARE);
        if (codewords.length() < symbol.getDataCapacity()) {
            codewords.append(PAD);
        }
        while (codewords.length() < symbol.getDataCapacity()) {
            // The rest is padding, randomized by its position (ISO/IEC 16022, annex B.1.1).
            int pad = PAD + 149 * (codewords.length() + 1) % 253 + 1;
            codewords.append((char) (pad <= 254 ? pad : pad - 254));
        }
        DefaultPlacement placement = new DefaultPlacement(ErrorCorrection.encodeECC200(codewords.toString(), symbol),
                symbol.getSymbolDataWidth(), symbol.getSymbolDataHeight());
        placement.place();
        return modules(placement, symbol);
    }

    private static boolean digit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * The symbol with the finder pattern of each of its regions: a solid side on the left and at the bottom, and a
     * side of alternate modules at the top and on the right.
     */
    private static boolean[][] modules(DefaultPlacement placement, SymbolInfo symbol) {
        boolean[][] modules = new boolean[symbol.getSymbolHeight()][symbol.getSymbolWidth()];
        int row = 0;
        for (int y = 0; y < symbol.getSymbolDataHeight(); y++) {
            if (y % symbol.matrixHeight == 0) {
                for (int x = 0; x < symbol.getSymbolWidth(); x++) {
                    modules[row][x] = x % 2 == 0;
                }
                row++;
            }
            int column = 0;
            for (int x = 0; x < symbol.getSymbolDataWidth(); x++) {
                if (x % symbol.matrixWidth == 0) {
                    modules[row][column++] = true;
                }
                modules[row][column++] = placement.getBit(x, y);
                if (x % symbol.matrixWidth == symbol.matrixWidth - 1) {
                    modules[row][column++] = y % 2 == 0;
                }
            }
            row++;
            if (y % symbol.matrixHeight == symbol.matrixHeight - 1) {
                for (int x = 0; x < symbol.getSymbolWidth(); x++) {
                    modules[row][x] = true;
                }
                row++;
            }
        }
        return modules;
    }
}
