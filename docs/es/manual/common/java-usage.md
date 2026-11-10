# Uso desde Java

El motor es una librería Java 17 (Maven: `dev.stxt:stxt-pdf`).

## Convertir un documento

```java
InlineNode doc = new DocumentReader().read(Path.of("informe.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("informe.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

`DocumentReader` lee y valida el documento. `Converter` lo maqueta y escribe el PDF. Un `Converter` se crea una vez y se
puede usar desde varios hilos a la vez.

## Opciones

```java
Converter converter = Converter.builder()
        .resources(new Resources(Path.of("mis-recursos")))   // otra carpeta de recursos
        .styles(Path.of("estilos.stxt"))                      // un fichero de estilos aparte
        .limits(Limits.DEFAULT.withMaxPages(500))             // otros límites
        .build();
converter.toPdf(doc, out);
```

| Opción | Por defecto |
|---|---|
| `resources` | La carpeta `resources/` del directorio de trabajo ([Recursos](resources.md)) |
| `styles` | Ninguno. Un fichero de estilos aparte del documento; cada llamada añade uno, y gana el que se añade después ([Estilos](styles.md#estilos-en-un-fichero-aparte)) |
| `limits` | 2000 páginas, 25 millones de píxeles por imagen, 20 000 elementos y 64 niveles por SVG y 20 MB por PDF |
| `debug` | `false`. Con `true`, pinta el contorno de cada caja ([Depurar un documento](debugging.md#los-contornos)) |
| `pdfUa` | `1`: el PDF cumple PDF/UA-1. Con `2`, PDF/UA-2, en PDF 2.0, que algún visor antiguo no abre |
| `pdfA` | Ninguno. Con `PdfA.A2B`, `A2A`, `A3B` o `A3A`, el PDF cumple también ese PDF/A, la norma de archivo ([abajo](#pdfa-y-adjuntos)) |
| `exclude` | Ninguna: se comprueba todo. Quita las comprobaciones que se le dicen (abajo) |
| `cancellation` | Ninguna. La que para todas las conversiones del `Converter` ([abajo](#cancelar-una-conversión)) |

Por defecto se comprueba todo. Con `exclude` se quitan comprobaciones, que dejan de ser un error:

```java
Converter converter = Converter.builder().exclude(Check.CONTRAST).build();
```

| Comprobación | Qué es un error si no se quita |
|---|---|
| `Check.CONTRAST` | Un texto que apenas se distingue de su fondo ([Caja](box.md#contraste)) |
| `Check.PDF_IMAGE_FONTS` | Una página de un PDF con una fuente que no cumple PDF/UA, como una sin incrustar o que no dice qué carácter es cada glifo ([Image](../components/image.md#una-página-de-un-pdf)) |
| `Check.PDF_IMAGE_ANNOTATIONS` | Una página de un PDF con un campo de formulario o un comentario visibles, que no se dibujarían |
| `Check.BARCODE_SIZE` | Un código QR o de barras tan pequeño que sus módulos no se pueden leer ([Barcode](../components/barcode.md#el-tamaño)) |

## PDF/A y adjuntos

```java
Converter converter = Converter.builder().pdfA(PdfA.A3B).build();
Attachment xml = Attachment.of("factur-x.xml", bytes, "text/xml", Attachment.Relationship.ALTERNATIVE);
converter.toPdf(doc, out, Attachments.of(xml).withXmp(facturXmp));
```

Con `pdfA`, el PDF declara PDF/A y lleva lo que la norma pide: un perfil de color y sus metadatos. PDF/A-3 (`A3B`, `A3A`)
admite además ficheros adjuntos, que es cómo se hace una factura electrónica Factur-X: el XML de la factura va dentro
del PDF. Los adjuntos se pasan en cada llamada a `toPdf`, con su nombre, sus bytes, su tipo y su relación con el
documento (`ALTERNATIVE` para el XML de una factura), y `withXmp` añade el fragmento de metadatos que Factur-X pide,
tal como lo da su especificación. Sin PDF/A, un adjunto va igual; con PDF/A-2 es un error (`ATTACHMENT_REQUIRES_PDFA3`). Con PDF/A, un JPEG
en CMYK es un error (`IMAGE_FORMAT`): se convierte antes a RGB.

## Recursos de varios sitios

```java
Resources resources = Resources.builder()
        .fonts(Path.of("/datos/fuentes"))
        .images("img", Path.of("/datos/img"))              // Image: img/logos/acme.png
        .image("app/logo.svg", bytesDelLogo)               // Image: app/logo.svg
        .build();
Converter converter = Converter.builder().resources(resources).build();

Converter own = converter.withImages(Map.of("firma.png", bytesDeLaFirma));
own.toPdf(doc, out);                                       // Image: firma.png
```

Los recursos no tienen por qué ser una sola carpeta: `Resources.builder()` los compone de una carpeta de fuentes, de
carpetas de imágenes, cada una con el prefijo por el que empiezan sus rutas, y de fuentes e imágenes en memoria, para
lo que la aplicación lleva en su classpath. `withImages` da un `Converter` que añade las imágenes de un documento, las
que llegan con él, sin tocar el compartido. Para lo demás que cambia de una petición a otra (sus estilos aparte, `debug`,
la parte de PDF/UA), `converter.toBuilder().styles(estilos).debug(true).build()` da otro `Converter` con la misma
configuración y los mismos recursos ya leídos, que se usa para esa petición y se tira.

Un texto que viene de datos (un nombre, lo que escribe un usuario) se pasa por `Reference.escape` antes de ponerlo en el
documento, para que un `{page}` que traiga escrito salga tal cual
([Referencias](references-and-links.md#referencias)). Si el texto va en Markdown, se escapa como Markdown, con una barra
invertida delante de cada signo de puntuación, y no hace falta más.

## Cancelar una conversión

```java
converter.toPdf(doc, out, Attachments.NONE, Cancellation.timeout(Duration.ofSeconds(60)));   // un plazo por PDF
```

Un `Cancellation` dice cuándo parar: cuando pasa un plazo (`timeout`, `deadline`), mientras el hilo está interrumpido
(`whenInterrupted()`) o cualquier `() -> boolean`. Se pasa en la llamada, para ese documento, o en
`Converter.builder().cancellation(...)`, para todas las conversiones. Cuando pide parar, la conversión termina en seguida,
en la fase que esté, con un error de tipo `CANCELLED` (abajo), y no se escribe nada en el destino.

## Errores

Todo error es un `StxtPdfException`, con tres datos: `kind()` (de quién es la culpa), `code()` (el código) y `line()` (la
línea, 0 si no es de una línea). Si la línea es de un fichero de estilos aparte, `inStyles()` es `true` y
`stylesDocument()` dice de cuál: 1 para el primero que se pasó.

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> System.err.println(e.getMessage());   // el documento tiene un error
        case LIMIT -> System.err.println("Demasiado grande: " + e.getMessage());
        case CANCELLED -> System.err.println("Cancelada");             // lo pidió el propio programa
        case RESOURCE, INTERNAL -> throw e;                            // la configuración o el motor
    }
}
```

## Más

- Para usar las páginas antes de pintarlas, o escribir también un SVG de cada página: `converter.layOut(doc)` y
  `converter.toSvg(...)` ([Depurar un documento](debugging.md)).
- En un servidor sin pantalla, la JVM se arranca con `-Djava.awt.headless=true`.

El detalle (hilos, tipos de error, límites) está en la [referencia](../../reference/implementation/api.md).
