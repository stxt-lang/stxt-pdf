// Sonda de QA (ROADMAP-QA-ARQ.md, QA-06): imágenes y SVG pequeños en bytes pero enormes al dibujarlos
// («bombas de descompresión»). Dos modos, para repetir la medición con la memoria limitada de una JVM aparte:
//
//   java BombsProbe gen <carpeta>            crea <carpeta>/{fonts -> resources/fonts, images/*} con cada caso
//   java -Xmx512m BombsProbe run <carpeta> <imagen>   convierte un documento con esa Image y cuenta tiempo y resultado
//
// Se ejecuta desde la raíz del repositorio; ver tools/qa/README.md.
import dev.stxt.InlineNode;
import dev.stxt.boxes.convert.Converter;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class BombsProbe {

    static void png(Path out, int w, int h, int type) throws Exception {
        BufferedImage image = new BufferedImage(w, h, type);
        ImageIO.write(image, "png", out.toFile());
    }

    static void jpeg(Path out, int w, int h) throws Exception {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        ImageIO.write(image, "jpg", out.toFile());
    }

    /** n rectángulos. */
    static String manyRects(int n) {
        StringBuilder sb = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">\n");
        for (int i = 0; i < n; i++) sb.append("<rect x=\"").append(i % 100).append("\" y=\"").append(i % 97)
                .append("\" width=\"1\" height=\"1\" fill=\"#123456\"/>\n");
        return sb.append("</svg>\n").toString();
    }

    /** n grupos anidados. */
    static String deepGroups(int n) {
        StringBuilder sb = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">\n");
        for (int i = 0; i < n; i++) sb.append("<g>");
        sb.append("<rect width=\"10\" height=\"10\"/>");
        for (int i = 0; i < n; i++) sb.append("</g>");
        return sb.append("\n</svg>\n").toString();
    }

    /** `use` en cadena: nivel k usa diez veces el nivel k-1, así que dibuja 10^levels rectángulos. */
    static String useChain(int levels, int fan) {
        StringBuilder sb = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                + " width=\"100\" height=\"100\">\n<defs>\n<g id=\"l0\"><rect width=\"1\" height=\"1\"/></g>\n");
        for (int k = 1; k <= levels; k++) {
            sb.append("<g id=\"l").append(k).append("\">");
            for (int j = 0; j < fan; j++) sb.append("<use href=\"#l").append(k - 1).append("\"/>");
            sb.append("</g>\n");
        }
        return sb.append("</defs>\n<use href=\"#l").append(levels).append("\"/>\n</svg>\n").toString();
    }

    static void gen(Path dir) throws Exception {
        Path images = dir.resolve("images");
        Files.createDirectories(images);
        Path fonts = dir.resolve("fonts");
        if (!Files.exists(fonts)) Files.createSymbolicLink(fonts, Path.of("resources", "fonts").toAbsolutePath());
        png(images.resolve("gray-4000.png"), 4000, 4000, BufferedImage.TYPE_BYTE_GRAY);            // 16 Mpx
        png(images.resolve("gray-12000.png"), 12000, 12000, BufferedImage.TYPE_BYTE_GRAY);         // 144 Mpx
        png(images.resolve("rgb-8000.png"), 8000, 8000, BufferedImage.TYPE_INT_RGB);               // 64 Mpx
        png(images.resolve("argb-4000.png"), 4000, 4000, BufferedImage.TYPE_INT_ARGB);             // 16 Mpx con transparencia
        png(images.resolve("argb-8000.png"), 8000, 8000, BufferedImage.TYPE_INT_ARGB);             // 64 Mpx con transparencia
        png(images.resolve("argb-12000.png"), 12000, 12000, BufferedImage.TYPE_INT_ARGB);          // 144 Mpx con transparencia
        png(images.resolve("binary-30000.png"), 30000, 30000, BufferedImage.TYPE_BYTE_BINARY);     // 900 Mpx
        jpeg(images.resolve("jpeg-20000.jpg"), 20000, 20000);                                      // 400 Mpx
        Files.writeString(images.resolve("rects-300k.svg"), manyRects(300_000));
        Files.writeString(images.resolve("rects-3k.svg"), manyRects(3_000));
        Files.writeString(images.resolve("deep-2000.svg"), deepGroups(2_000));
        Files.writeString(images.resolve("deep-20000.svg"), deepGroups(20_000));
        Files.writeString(images.resolve("use-5x10.svg"), useChain(5, 10));      // 10^5 rectángulos
        Files.writeString(images.resolve("use-9x10.svg"), useChain(9, 10));      // 10^9 rectángulos
        try (var list = Files.list(images)) {
            list.sorted().forEach(f -> System.out.printf("%-20s %8d KB%n", f.getFileName(), f.toFile().length() / 1024));
        }
    }

    static void run(Path dir, String image) throws Exception {
        String document = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tPaper: A4\n\t\tMargin: 40\n\tTitle: T\n\tLanguage: es\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + "\tSection:\n\t\tImage: " + image + "\n\t\t\tBox:\n\t\t\t\tWidth: 100\n";
        long t0 = System.nanoTime();
        String result;
        try {
            InlineNode doc = new DocumentReader().read(document);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Converter.builder().resources(new Resources(dir)).build().toPdf(doc, out);
            result = "OK pdf=" + out.size() / 1024 + "KB";
        } catch (Throwable e) {
            result = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()).lines().findFirst().orElse("");
            for (Throwable c = e.getCause(); c != null; c = c.getCause())
                result += "  <- " + c.getClass().getSimpleName() + ": " + String.valueOf(c.getMessage()).lines().findFirst().orElse("");
        }
        System.out.printf("%-20s %7d ms  %s%n", image, (System.nanoTime() - t0) / 1_000_000, result);
    }

    public static void main(String[] a) throws Exception {
        System.setProperty("java.awt.headless", "true");
        if (a[0].equals("gen")) gen(Path.of(a[1]).toAbsolutePath());
        else run(Path.of(a[1]).toAbsolutePath(), a[2]);
    }
}
