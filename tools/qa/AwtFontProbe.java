import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * QA-02: qué fuente da AWT por el nombre de una familia de resources/fonts/ después de darla de alta con
 * registerFont, que es como la buscaba JSVG antes de la sesión 4. Con la JVM arrancada en turco
 * (-Duser.language=tr -Duser.country=TR) devuelve Dialog aunque la familia esté en la lista: cambiar el Locale en
 * caliente no lo reproduce.
 */
public class AwtFontProbe {
    public static void main(String[] args) throws Exception {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        File fonts = new File(args.length > 0 ? args[0] : "resources/fonts");
        for (File dir : fonts.listFiles(File::isDirectory)) {
            for (File f : dir.listFiles((d, n) -> n.endsWith(".ttf"))) {
                env.registerFont(Font.createFont(Font.TRUETYPE_FONT, f));
            }
        }
        String[] families = env.getAvailableFontFamilyNames();
        System.out.println("idioma " + Locale.getDefault());
        for (File dir : fonts.listFiles(File::isDirectory)) {
            String family = dir.getName();
            boolean listed = Arrays.stream(families).anyMatch(family::equalsIgnoreCase);
            Map<TextAttribute, Object> attributes = new HashMap<>();
            attributes.put(TextAttribute.FAMILY, family);
            attributes.put(TextAttribute.SIZE, 12f);
            Font font = new Font(attributes);
            System.out.printf("%-16s en la lista: %-5s -> %s%n", family, listed, font.getFontName(Locale.ROOT));
        }
    }
}
