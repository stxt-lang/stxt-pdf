# Uso desde Java

El motor es una librería Java 17 (`dev.stxt:stxt-pdf`). Lo que usa quien convierte documentos es `Converter` y
`DocumentReader`.

## Convertir

```java
InlineNode doc = new DocumentReader().read(Path.of("report.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("report.pdf"))) {
    Converter.create().toPdf(doc, out);   // maqueta con las métricas reales de las fuentes y pinta
}
```

`Converter` reúne la configuración de una conversión y es inmutable y seguro entre hilos: se crea una vez y se comparte.
Para usar las páginas antes de pintarlas, o pintarlas a PDF y a SVG a la vez:

```java
Converter converter = Converter.create();
LaidOutDocument laidOut = converter.layOut(doc);
converter.toPdf(laidOut, out);
List<String> svg = converter.toSvg(laidOut);   // una cadena por página
```

| `Converter.builder()` | Qué cambia |
|---|---|
| `resources(Resources)` | La carpeta de recursos (por defecto, `resources/` del directorio de trabajo) |
| `styles(InlineNode)`, `styles(Path)`, `styles(String)` | El documento de estilos aparte |
| `limits(Limits)` | Los límites de la conversión |
| `debug(boolean)` | Pinta los contornos de las cajas |
| `pdfUa(int)` | La parte de PDF/UA que cumple el PDF: 1 (PDF 1.7, por defecto) o 2 (PDF 2.0) ([El PDF por dentro](pdf.md#pdfua-2)). Otro valor es un `IllegalArgumentException` |
| `pdfA(PdfA)` | El PDF/A que cumple además el PDF: `A2B`, `A2A`, `A3B` o `A3A` ([abajo](#pdfa-y-adjuntos)); por defecto, ninguno. Con `pdfUa(2)`, `build()` es un `IllegalArgumentException` |
| `exclude(Check...)` | Quita comprobaciones. Por defecto se hacen todas ([Comprobaciones](#comprobaciones)) |
| `cancellation(Cancellation)` | La que para todas las conversiones del `Converter`; por defecto, ninguna ([Cancelar una conversión](#cancelar-una-conversión)) |

> **Decisión:** la configuración es un objeto inmutable, `Converter`, seguro entre hilos porque cada conversión crea su
> propio `Engine`, con `layOut`, `toPdf` (de un documento o de uno ya maquetado) y `toSvg`. Los estilos nunca son `null`: si
> no hay, no se llama a `styles`. Vive en un package propio, encima de todos los demás. `PdfDocumentWriter.write` y `Engine`
> siguen siendo públicos como capa de bajo nivel. *Alternativa descartada:* un método con una sobrecarga por cada
> combinación de opciones.

## Lo que se promete

Mientras la versión empieza por `0.`, la API puede cambiar entre versiones menores. Desde la 1.0, lo que el jar promete
es lo que lista esta tabla, y solo eso: un tipo o un miembro que no está en ella puede cambiar o desaparecer en una
versión menor. Un tipo prometido entero promete todos sus miembros públicos; de los demás, solo los que nombra la fila.
Las constantes de un enum entran siempre. De una excepción se promete lo que se lee, no sus constructores.

| Tipo | Lo que se promete |
|---|---|
| `convert.Converter`, `convert.Converter.Builder` | Todo |
| `stxt.DocumentReader` | Todo |
| `resources.Resources` | `Resources()`, `Resources(Path)`, `DEFAULT_ROOT`, `root()`, `fontsFolder()`, `imagesFolder()`, `resolve(...)`, `snapshot()` |
| `model.Limits`, `model.Check`, `model.PdfA`, `model.Cancellation`, `model.Progress`, `model.Attachment`, `model.Attachment.Relationship`, `model.Attachments` | Todo |
| `model.StxtPdfException`, `model.StxtPdfException.Kind`, `tree.ErrorCode` | Todo |
| `tree.InvalidModelException` | `errorCode()` y lo que hereda |
| `tree.DocumentSyntaxException` | Lo que hereda |
| `resources.ResourceException`, `resources.ImageFormatException`, `model.InternalException`, `model.CancelledException` | `CODE` y lo que hereda |
| `layout.LaidOutDocument` | Todo |
| `fragment.Page`, `fragment.Fragment`, `fragment.TextLine`, `fragment.TextLine.Run`, `fragment.Rule` | Todo |
| `tree.PageSetup`, `tree.PageSetup.Paper`, `tree.PageSetup.Orientation`, `tree.Metadata` | Todo |
| `tree.Box` | `id()`, `element()`, `role()`, `line()`, `anonymous()`, `parent()`, `children()`, `text()` |
| `tree.Element` | Sus constantes, `canonicalName()`, `label()`, `role()`, `headingLevel()` |
| `tree.Element.Role` | Todo |
| `model.Insets`, `model.Rect`, `model.Sides`, `model.Value`, `model.Value.Points`, `model.Value.Keyword`, `model.Value.Percentage`, `model.Value.Between`, `model.Value.Fraction`, `model.NumberFormat` | Todo |
| `model.TextRun` | `text()`, `format()`, `LINE_SEPARATOR`, `OBJECT_REPLACEMENT`, `IMAGE`, `plain(...)`, `text(...)`, `formatted(...)` |
| `model.TextFormat`, `model.FontFace`, `model.FontWeight`, `model.FontStyle`, `model.TextCase`, `model.RunStyle`, `model.InlineImage` | Todo |
| `output.StxtDump` | Todo |
| `paint.PdfPainter` | Todo |

Lo que `layOut` devuelve entra en el contrato: `LaidOutDocument`, sus páginas y sus fragmentos, con la geometría de cada
uno, sus líneas y el texto de cada tramo, y de la caja de cada fragmento lo que la identifica y la sitúa en el documento.
Un servicio puede contar las páginas o medir cuánto llena cada una sin pintar nada. Son `record`s que solo ganan
componentes: lo que un componente dice hoy lo dirá siempre.

Lo demás que el jar expone como público lo es porque otro package del motor lo usa, no para quien convierte: `Engine`,
`PdfDocumentWriter`, `BoxTreeBuilder`, `Box.style()` y los grupos de `Style`, `Resources.fonts()` e `images()` con sus
índices, los algoritmos de `layout`, los lienzos de `paint`. Se puede usar, con la versión fijada, y no se promete.

`PublicApiTest` lee esta tabla y comprueba que cada tipo y cada miembro existe y es público, que ninguna firma
prometida (parámetros, lo que devuelve, componentes, supertipos) expone un tipo del motor que no esté en la tabla ni uno
ajeno que no sea de `stxt-core` (`InlineNode`, `ParseException`) o `PDDocument` y `PDPage` de PDFBox, y que la tabla de
`docs/en/` dice lo mismo. La frontera no se mueve sin tocar esta página.

> **Decisión:** la frontera se escribe en esta tabla y la vigila un test, y no un `module-info.java` con `exports`: en
> Java sin módulos todo lo que cruza packages tiene que ser público, y con módulos `Fragment.box()` obligaría a exportar
> `tree` entero. `LaidOutDocument` sigue siendo un `record`: convertirlo en clase no ocultaría `root()`, que `paint`
> necesita. De `Box` se promete lo que identifica una caja, no su estilo ni lo que el motor le pregunta; de `Resources`,
> la carpeta y su vista, no sus índices, para que fontbox no llegue al contrato a través de `FontFile.parse`, que `paint`
> necesita para incrustar cada fuente en un `TrueTypeFont` nuevo. *Alternativas descartadas:* prometer todo lo público,
> que fija para siempre más de 160 tipos (lo que esta tabla nombra arrastra, siguiendo sus firmas, 64); dejar los
> fragmentos fuera del contrato hasta que exista el servicio, que obligaría a admitir una excepción en la regla del cierre
> desde el primer día, porque `LaidOutDocument.pages()` los expone igual.

## PDF/A y adjuntos

```java
Converter converter = Converter.builder().pdfA(PdfA.A3B).build();
Attachment xml = Attachment.of("factur-x.xml", bytes, "text/xml", Attachment.Relationship.ALTERNATIVE)
        .withDescription("Factur-X invoice");
converter.toPdf(doc, out, Attachments.of(xml).withXmp(facturXmp));   // una factura Factur-X
```

`pdfA` es del `Converter`, como `pdfUa`: una decisión de quien publica ([El PDF](../common/pdf.md#pdfa)). Los adjuntos
son de cada documento y van en la llamada: `toPdf(documento, destino, Attachments)`, también con un `LaidOutDocument`.
`Attachments.of(Attachment...)` reúne los ficheros (de nombres distintos) y `withXmp` les añade el XMP que los acompaña
(uno o más `rdf:Description`, con sus espacios de nombres declarados; `rdf` ya lo está). `Attachment.of(nombre, bytes,
tipo, relación)` es inmutable y copia los bytes; `withDescription` y `withModified` dan la descripción y la fecha de
modificación. Todo está en `model`. Los dos `toPdf` sin adjuntos equivalen a `Attachments.NONE`.

Un adjunto sin `pdfA(A3B)` o `pdfA(A3A)` es `ATTACHMENT_REQUIRES_PDFA3`, un `StxtPdfException` de tipo `RESOURCE` (es la
configuración de quien convierte, no el documento) y sin línea. Un XMP que no es XML, dos adjuntos con el mismo
nombre, un nombre en blanco o un tipo que no es `tipo/subtipo` son un `IllegalArgumentException` al construirlos, antes
de convertir. `PdfDocumentWriter.write` recibe lo mismo (`ua`, `pdfA` y `Attachments`) como capa de bajo nivel.

> **Decisión:** los adjuntos y su XMP se pasan en la llamada y no en el `builder`: un `Converter` se comparte entre
> documentos y el XML de una factura es de una factura. *Alternativa descartada:* `attach(...)` en el `builder`.

## Comprobaciones

Algunas comprobaciones no son reglas del documento sino de lo que se ve o de lo que traen los ficheros, y quien convierte
puede quitarlas con `exclude`, que se acumula entre llamadas. Por defecto se hacen todas, y `Converter.excluded()` dice
las que se han quitado. Son los valores del enum `Check` (`model`):

| `Check` | Qué comprueba | Sin ella |
|---|---|---|
| `CONTRAST` | El contraste del texto con su fondo, `CONTRAST_LOW` ([El PDF](../common/pdf.md#contraste)). `layOut` lo comprueba con `ContrastCheck`, un lienzo que apunta lo que se pinta | Un texto puede no leerse |
| `PDF_IMAGE_FONTS` | Que las fuentes de la página de un PDF que dibuja una `Image` cumplan PDF/UA: incrustadas, que digan qué carácter es cada glifo y las demás reglas de la lista, `IMAGE_FORMAT` ([Image](../components/image.md#pdf)) | El visor puede sustituir una fuente, su texto puede no copiarse y el PDF puede no cumplir PDF/UA |
| `PDF_IMAGE_ANNOTATIONS` | Que la página de un PDF no tenga anotaciones que se vean, `IMAGE_FORMAT` | La página se dibuja sin ellas |
| `BARCODE_SIZE` | Que los módulos de un `Barcode` no queden más estrechos que el mínimo de su formato, `BARCODE_TOO_SMALL` ([Barcode](../components/barcode.md#módulo-mínimo)) | El código se dibuja tan pequeño como su caja y puede no leerse |

```java
Converter converter = Converter.builder().exclude(Check.CONTRAST, Check.PDF_IMAGE_ANNOTATIONS).build();
```

`Engine` recibe las que se quitan y se las pasa a `Images`; si no se quita `BARCODE_SIZE`, comprueba los módulos con
`ModuleSizes` después de `Overflow`. El contraste lo comprueba `Converter.layOut` después de maquetar.

> **Decisión:** una sola forma de quitar comprobaciones, un enum y `exclude`, en vez de un booleano por cada una: por
> defecto se hace todo y lo que se quita se nombra. *Alternativa descartada:* `checkContrast(boolean)` y un método más por
> cada comprobación nueva.

> **Decisión:** lo que impide leer un PDF (cifrado, dañado, sin la página) no se puede quitar: no es lo que se ve, sino que
> no hay nada que dibujar. *Alternativa descartada:* una comprobación más para el PDF cifrado.

## Estilos aparte

```java
InlineNode styles = new DocumentReader().readStyles(Path.of("styles.stxt"));
Converter converter = Converter.builder().styles(styles).build();
converter.toPdf(doc, out);
```

Por debajo, `BoxTreeBuilder.build(documento, estilos)` y `Engine.layOutDocument(documento, estilos)` lo reciben igual. En
esos dos puede ser `null`. Ver [Styles](../common/styles.md#documento-de-estilos-aparte).

## Recursos

`new Resources(Path.of("otra"))` usa otra carpeta de recursos. Toda ruta pasa por `Resources.resolve`, que nunca va hacia
atrás. `Resources` se comparte entre hilos y entre conversiones. Guarda el índice de `fonts/` y las imágenes leídas,
una por fichero: `ImageLibrary` las guarda por su ruta real, así que `logo.png`, `./logo.png` y, en un sistema de
ficheros que no distingue mayúsculas, `Logo.png` son la misma imagen, y lo que escriben los documentos no hace crecer la
memoria de un servicio. Las grafías ya vistas son un atajo hacia ellas, sin ir al disco, que se vacía al llegar a 1024.

`FontLibrary` indexa los `.ttf` y los `.otf` e identifica cada uno por su interior:

- **familia:** la tipográfica de la tabla `name` (`nameID` 16) o, si no la tiene, la familia (`nameID` 1);
- **peso:** `usWeightClass` de `OS/2` tal cual, de 1 a 1000 (fuera de ahí el fichero se ignora sin error). Solo se puede
  pedir un peso que el documento sabe escribir, de 100 a 900 de 100 en 100: un fichero de 450 se indexa y sale en la
  lista de variantes del error, pero no se usa;
- **estilo:** el bit de cursiva de `fsSelection`.

`FontFile.parse` lee un fichero que empieza por `OTTO` (contornos CFF) con el lector de OpenType de fontbox y el resto
con el de TrueType, sea cual sea su extensión; `FontFile.cff()` dice cuáles son CFF, que se incrustan enteras
([El PDF por dentro](pdf.md#páginas-fuentes-y-metadatos)).

> **Decisión:** las fuentes solo se comprueban con `PdfMetrics` (`Engine.layOutDocument` llama a `validate`). Con
> `FixedMetrics` no se mira la carpeta de fuentes: basta con que `Font` esté declarada. *Alternativa descartada:* validar
> siempre las fuentes al construir el árbol, que obligaría a tener la carpeta también para maquetar sin PDF.

### Ficheros que cambian

Los ficheros de la carpeta pueden cambiar mientras vive el `Converter`: cada conversión ve la carpeta como está cuando
empieza, sin crear otro `Converter` ni otro `Resources`. `Converter` trabaja en cada conversión con
`Resources.snapshot()`, una vista de la carpeta que comparte con el `Resources` y con las demás vistas lo ya leído:

- **Fuentes.** La primera vez que la vista las pide, recorre `fonts/` mirando el tamaño y la fecha de modificación de
  cada fichero, sin leerlo. Si son los del último índice, lo usa; si se ha añadido, quitado o sustituido un fichero,
  indexa la carpeta otra vez.
- **Imágenes.** La primera vez que la vista pide una imagen, mira el tamaño y la fecha de su fichero. Si son los de la
  imagen ya leída, la usa; si no, la lee otra vez y la nueva sustituye a la vieja para las vistas que vengan después. Un
  SVG con texto se lee también otra vez si las fuentes se han indexado de nuevo, porque su texto se comprobó con ellas.
- **Durante la conversión.** La vista se queda con lo que encontró mientras vive, así que una conversión no mezcla dos
  versiones de un fichero. El `LaidOutDocument` la guarda (`LaidOutDocument.resources()`), y `toPdf` y `toSvg` lo pintan
  con las mismas fuentes e imágenes con que se maquetó.

Las fuentes y los SVG están en memoria, así que un documento ya maquetado se pinta igual aunque su fichero cambie o
desaparezca. Un PNG, un JPEG o un PDF se vuelve a leer al pintar, y antes `ImageFile.checkUnchanged` compara su tamaño y
su fecha con los que tenía al leerlo: si no coinciden, la conversión acaba en `IMAGE_FORMAT`, sin línea, en vez de pintar
una imagen con el tamaño de otra; si el fichero ya no se puede leer (porque se ha borrado, por ejemplo), en `RESOURCE`,
con su ruta. La conversión siguiente lee el fichero nuevo.

Un `Resources` creado con su constructor y usado sin `snapshot()` (con `Engine`, `PdfDocumentWriter` o `PdfPainter`
directamente) lee cada fichero la primera vez y no vuelve a mirar la carpeta. Para ver los cambios, se les pasa
`resources.snapshot()`, una por documento.

> **Decisión:** una vista por conversión, que mira cada fichero la primera vez que lo usa: los cambios se ven sin crear
> otro `Converter`, cuesta un recorrido de `fonts/` y una consulta al disco por imagen y conversión, y una conversión no
> mezcla versiones. El SVG se guarda en memoria, y el PNG, el JPEG y el PDF se comparan por su tamaño y su fecha, porque
> pueden ser grandes. No se detecta un fichero sustituido por otro del mismo tamaño con la fecha del original (como hacen
> `cp -p`, `rsync -t` o un zip al descomprimirse), ni un cambio entre la comprobación y la lectura. *Alternativas
> descartadas:* una carpeta que no puede cambiar mientras vive el `Resources`, que pedía crear otro para ver un fichero
> nuevo; un `reload()` que llama quien cambia los ficheros; mirar el disco en cada uso de un fichero, que está en cada
> medida; leer los bytes una vez al pintar y comprobar sobre ellos el formato y el tamaño medidos.

## Errores

Todo error del motor es un `StxtPdfException` (lo lanzan `DocumentReader` y `Converter`), así que una sola `catch` basta.
`kind()` dice de quién es la culpa, `code()` qué falla y `line()` dónde (`NO_LINE`, 0, si no es de una línea).
`inStyles()` dice si esa línea es del [documento de estilos aparte](../common/styles.md#documento-de-estilos-aparte) y no del documento:

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> reply(422, e.code() + " en la línea " + e.line()
                + (e.inStyles() ? " de los estilos" : "") + ": " + e.getMessage());
        case LIMIT -> reply(413, e.getMessage());
        case CANCELLED -> reply(503, e.getMessage());            // el propio servicio pidió parar
        case RESOURCE, INTERNAL -> reply(500, e.getMessage());   // de la configuración o del motor
    }
}
```

| Clase | Qué es |
|---|---|
| `InvalidModelException` (`tree`) | Las reglas del modelo, los desbordamientos y los recursos que nombra el documento. `errorCode()` da el `ErrorCode` y `ErrorCode.kind()` su tipo (`INVALID_DOCUMENT` salvo `PAGE_LIMIT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX` y `PDF_TOO_LARGE`, que son `LIMIT`) |
| `DocumentSyntaxException` (`tree`) | La sintaxis y el schema de `stxt-core`, con su código y su línea y la excepción original como causa. Un `LimitException` de la librería es `LIMIT` |
| `ResourceException` (`resources`) | La carpeta de recursos (`RESOURCE`), con `ImageFormatException`: también un fichero que no se puede leer o resolver (una fuente dañada, un enlace roto, una carpeta sin permiso de lectura), con su ruta en el mensaje |
| `InternalException` (`model`) | Un fallo del motor. La causa original va en `getCause()` |
| `CancelledException` (`model`) | La conversión parada por su `Cancellation` (`CANCELLED`), sin línea ([Cancelar una conversión](#cancelar-una-conversión)) |

Los fallos de entrada y salida al escribir en el destino siguen siendo la `IOException` de `toPdf`. `Converter` deja pasar
los errores del motor, convierte cualquier otra excepción de ejecución en `InternalException` y no toca la `IOException`
del destino. Un `IllegalStateException` o `IllegalArgumentException` solo es válido dentro del motor para un mal uso
interno. Lo que puede ocurrirle a quien convierte va con uno de estos tipos. Los códigos y los tipos están en
[Errores](../common/errors.md).

> **Decisión:** `code()` es el código en texto, común a todos, y el enum pasa a `errorCode()` en `InvalidModelException`:
> los errores de la librería tienen un conjunto abierto de códigos que no caben en `ErrorCode`. *Alternativa descartada:*
> añadir cada código de `stxt-core` a `ErrorCode`, que ata el motor a la lista de la librería.

> **Decisión:** `Converter` es la frontera de los errores: deja pasar los del motor y la `IOException` del destino, y
> convierte cualquier otra excepción de ejecución en un `InternalException` con el original como causa, así que una sola
> `catch` vale también para un defecto del motor o de PDFBox. *Alternativa descartada:* envolver también la `IOException`
> del destino, que no es un error del motor.

## Límites

`Limits` es inmutable. `Limits.DEFAULT` tiene los valores por defecto ([Límites](../common/limits.md)) y se cambia con
los métodos `withMax...`:

```java
Converter converter = Converter.builder()
        .limits(Limits.DEFAULT.withMaxPages(500).withMaxImagePixels(60_000_000))
        .build();
```

Llega al `Engine` (que lo pasa a `Images` y al `Paginator`) y a `PdfDocumentWriter.write` (los límites del dibujo de JSVG).
`Images.validate` los comprueba antes de maquetar con lo que `ImageFile` leyó sin decodificar nada: los píxeles de la
cabecera y, en un SVG, los elementos y los niveles que cuenta `SvgInspector` sin recursión. Los bytes de un PDF los mira
`ImageLibrary.pdfBytes` antes de leerlo con `find`, que lo abre con PDFBox: un PDF de más se rechaza sin abrirlo.

> **Decisión:** los valores por defecto se eligieron midiendo: un PNG con transparencia de 144 Mpx (547 KB en disco) agota
> un heap de 512 MB, uno de 900 Mpx de 106 KB tarda 23 s, 20 000 niveles de SVG desbordan la pila de la JVM y una cadena de
> `<use>` de 10⁹ elementos agota la memoria a los 51 s. Un juego más alto (10 000 páginas, 100 Mpx, 100 000 elementos y
> 256 niveles) dejaba a un servicio expuesto a cada una de ellas. *Alternativa descartada:* propiedades del sistema, o
> constantes sin configurar.

> **Decisión:** los límites son de cada conversión y no de `Resources`: dos conversiones con límites distintos comparten los
> mismos recursos. *Alternativa descartada:* atar la caché de imágenes a un único juego de límites.

> **Decisión:** `SvgInspector` cuenta por exceso lo que contará JSVG, así que lo que cumple el límite no falla al dibujar.
> JSVG trae sus propios límites (2000 elementos y 30 niveles, y 15 niveles de `<use>` anidados), que rechazaban SVG
> legítimos: el motor le da los suyos, y si aun así no carga un SVG es `IMAGE_FORMAT`. *Alternativa descartada:* dejar que
> JSVG cuente al dibujar, que falla tarde, sin línea y sin tipo.

> **Decisión:** el límite de un PDF son los bytes del fichero, que se miran sin abrirlo, y 20 MB por defecto: PDFBox lee
> su estructura entera al abrirlo, y un plano o un membrete caben de sobra. *Alternativa descartada:* contar sus páginas o
> sus objetos, que obliga a abrirlo antes de saber si se puede.

> **Decisión:** los límites del texto STXT (10 MB, 100 niveles, 10 000 caracteres por línea) son los de `stxt-core`, que ya
> da un error `LIMIT`. `Limits` solo tiene lo que el motor comprueba por su cuenta.

## Cancelar una conversión

```java
converter.toPdf(doc, out, Attachments.NONE, Cancellation.timeout(Duration.ofSeconds(60)));   // un plazo por PDF
```

`Cancellation` (`model`) dice si la conversión tiene que parar: una interfaz funcional con `requested()`, que el motor
consulta allí donde trabaja. Se pasa en la llamada (`layOut`, `toPdf` y `toSvg` tienen una sobrecarga con ella, y otra
con ella y un [`Progress`](#progreso)) o, para
todas las conversiones de un `Converter`, con `Converter.builder().cancellation(...)`; con las dos, para cuando lo pide
cualquiera. La primera vez que contesta `true`, la conversión termina con un `CancelledException`, de tipo `CANCELLED` y
sin línea ([Errores](../common/errors.md#tipos-de-error)), que atraviesa el motor sin que nada lo envuelva, y en el
destino no se ha escrito nada: el PDF se escribe entero al final.

| `Cancellation` | Cuándo para |
|---|---|
| `NONE` | Nunca; es el valor por defecto |
| `deadline(Instant)` | Desde ese momento, medido con el reloj monótono de la JVM desde que se crea: un cambio de la hora del sistema después no lo mueve |
| `timeout(Duration)` | Cuando pasa ese tiempo desde que se crea; cero o negativo, en seguida |
| `whenInterrupted()` | Mientras el hilo que convierte está interrumpido (`Thread.isInterrupted()`), sin borrar la marca |
| `a.or(b)` | Cuando lo pide cualquiera de las dos |
| Cualquier `() -> boolean` | Cuando devuelve `true`; tiene que ser barata y no bloquear, porque se consulta miles de veces |

Dónde se consulta: `BoxTreeBuilder`, en cada elemento (un `Barcode` se genera al construir el árbol); `Engine`, en cada
layout que no está en la caché (`layoutOnce` y `contentIntrinsic`); `Paginator`, en cada vuelta de la convergencia de las
referencias y en cada página, al paginar el flujo y al colocar los `Page overlay`; `Overflow`, `ModuleSizes` y `ContrastCheck`, en cada
página; `PdfDocumentWriter`, en cada página que pinta y antes de guardar; `PdfImages`, en cada imagen que incrusta o dibuja
por primera vez, y `SvgCanvas.pages` (el de `Converter.toSvg`), en cada página. Cada consulta cuesta una lectura del reloj monótono o de la marca del
hilo. Lo que no se corta es una sola llamada a PDFBox, a JSVG o a ZXing (dibujar un SVG, generar un código, guardar el
PDF), que termina sola; los [límites](#límites) acotan lo que puede tardar cada una.

> **Decisión:** la cancelación es un objeto propio que el motor consulta, y no solo la interrupción del hilo: un plazo se
> expresa sin hilos y se prueba sin ellos, no depende de quién interrumpe, y `whenInterrupted()` da la del hilo a quien
> la quiere. *Alternativa descartada:* mirar solo `Thread.interrupted()`, que obliga a un hilo por conversión y a alguien
> que lo interrumpa a tiempo.

> **Decisión:** se pasa en la llamada, como los adjuntos, porque un plazo es de cada conversión y el `Converter` se
> comparte; el `builder` la admite además como valor por defecto, para la del hilo, que es la misma para todas.
> *Alternativa descartada:* solo en el `builder`, que obligaría a un `Converter` por plazo.

> **Decisión:** `CANCELLED` es un tipo de error propio y no un `LIMIT`: no es culpa del documento ni del entorno, sino
> una decisión de quien convierte, y un servicio no contesta lo mismo (su propio plazo es un 503 o un 504; un cliente que
> se fue, nada). Un `switch` sobre `Kind` tiene que cubrirlo. *Alternativa descartada:* un `LIMIT` sin línea, que un
> servicio contestaría como un 413.

> **Decisión:** se consulta donde se trabaja y no entre fases: una sola página con miles de códigos QR puede tardar
> segundos en construirse, y comprobarlo solo entre páginas no la cortaría. *Alternativa descartada:* comprobar solo entre
> páginas en el `Paginator`.

## Progreso

```java
converter.toPdf(doc, out, Attachments.NONE, Cancellation.NONE, new Progress() {
    @Override
    public void pagePainted(int number, int pages) {
        bar.set(number, pages);   // una barra de progreso
    }
});
```

`Progress` (`model`) es a quien la conversión cuenta cómo va. Se pasa en la llamada (`layOut`, `toPdf` y `toSvg` tienen
una sobrecarga con él, detrás de la `Cancellation`) y el motor lo llama, en el hilo que convierte, a medida que avanza.
Todos sus métodos tienen una implementación vacía: se sobreescribe solo lo que se escucha, y `Progress.NONE` no
escucha nada.

| `Progress` | Cuándo se llama |
|---|---|
| `pageLaidOut(número)` | El `Paginator` ha maquetado una página del flujo: la `número`, desde 1, dentro de la pasada en curso. Un documento con referencias (`{page of: id}`) o cuyo total de páginas (`{pages}`) cambia lo maquetado se pagina otra vez ([Referencias](../common/references.md)), y la cuenta vuelve a empezar en 1 en cada pasada |
| `laidOut(páginas)` | El layout ha terminado, con ese total de páginas, antes de pintar nada. Es el final de `layOut` |
| `pagePainted(número, páginas)` | Una página está pintada en el PDF, o su SVG hecho |
| `done()` | La conversión ha terminado: el PDF está escrito en el destino (o en el `PDDocument`), o todos los SVG están hechos. `layOut` solo no lo llama |

Lo que lanza una implementación termina la conversión como un `InternalException`, con lo lanzado como causa: no es
un error del motor ni del documento, y una barra de progreso no tiene por qué parar nada.

> **Decisión:** se pasa solo en la llamada y no en el `builder`: un `Converter` se comparte entre conversiones y entre
> hilos, y un aviso sin decir de qué conversión es no sirve para una barra de progreso. *Alternativa descartada:* un
> `progress(...)` en el `builder`, como la cancelación, que allí sí tiene sentido porque la del hilo es la misma para
> todas.

> **Decisión:** `pageLaidOut` cuenta cada página de cada pasada y no solo las de la última: durante la primera pasada no
> se sabe si habrá otra, y una barra que avanza y vuelve a empezar dice la verdad. *Alternativa descartada:* avisar solo
> en la fase final del `Paginator`, que es rápida porque el flujo ya está en la caché, y no contaría lo que tarda.

## Integración con PDFBox

Dos formas de usar el motor desde un programa que ya trabaja con PDFBox: escribir el documento en un `PDDocument`
abierto, que guarda quien lo abrió después de añadirle lo que el motor no escribe, y pintar una página maquetada sobre
una página de cualquier documento.

### En un `PDDocument` abierto

```java
try (PDDocument pdf = new PDDocument()) {
    converter.toPdf(laidOut, pdf);                                      // o toPdf(doc, pdf)
    pdf.getDocumentInformation().setCustomMetadataValue("poliza", "123");   // lo que el motor no escribe
    stamp(pdf);                                                         // una marca de agua en cada página
    pdf.protect(policy);                                                // el cifrado
    pdf.save(out);
}
```

`toPdf(documento, PDDocument)` escribe en el documento lo mismo que en un fichero: las páginas, las fuentes, el árbol de
estructura, los marcadores, los enlaces, las etiquetas de página, los metadatos y la declaración de PDF/A, y con la
sobrecarga larga (`Attachments`, `Cancellation`, `Progress`), los adjuntos. Dos diferencias: **no lo guarda**, lo guarda
quien lo abrió, y por eso el identificador del fichero (`/ID`) es el que PDFBox le pone al guardar y no el del contenido
([El PDF por dentro](pdf.md#páginas-fuentes-y-metadatos)): dos guardados no dan los mismos bytes. El `PDDocument` tiene
que estar **sin páginas** (otra cosa es un `IllegalArgumentException`): un documento se escribe entero, porque su árbol de
estructura, sus etiquetas de página y sus metadatos son de todo el PDF. Lo que el `PDDocument` tuviera (claves del
diccionario `Info`) se queda, salvo lo que el documento escribe: el título, el asunto, el idioma y el paquete XMP, que
se sustituye entero; por eso las claves propias se ponen después. Si la conversión se cancela, el documento queda a
medias. Por debajo, `PdfDocumentWriter.write` tiene la misma sobrecarga con un `PDDocument`.

> **Decisión:** el `PDDocument` tiene que estar vacío, y lo que ya tiene páginas se hace con `PdfPainter`: añadir un
> documento etiquetado a otro que ya tiene árbol de estructura es fusionar los dos árboles, su `ParentTree`, sus
> `RoleMap` y sus `IDTree`, que es lo que hace `PDFMergerUtility` de PDFBox a partir de dos PDF ya guardados.
> *Alternativa descartada:* añadir las páginas al final de un documento con páginas, con o sin etiquetado.

> **Decisión:** las claves propias del `Info` y el modo de fusión de una marca de agua no entran en el motor: con el
> `PDDocument` abierto se escriben con PDFBox antes de guardar, sin reabrir el fichero, y el motor no tiene que saber de
> gestores documentales ni de marcas de agua. *Alternativa descartada:* un `Meta` en `Document` y un `Blend` en
> `Page overlay`, que eran lo que pedía el primer consumidor del motor.

### Una página sobre una página ajena

```java
try (PDDocument form = Loader.loadPDF(formFile);
     PdfPainter painter = new PdfPainter(form, converter.resources(), converter.limits())) {
    painter.paint(laidOut.pages().get(0), form.getPage(0));   // la primera página, sobre el formulario
    form.save(out);
}
```

`PdfPainter` (`paint`) pinta páginas maquetadas, de una en una, sobre páginas de un documento que no es del motor: para
estampar una página sobre un formulario, o para poner las páginas de un documento en otro PDF. Cada `paint(página,
PDPage)` añade el contenido de la página **detrás** de lo que la del PDF ya tiene, con la esquina superior izquierda de
la página maquetada en la esquina superior izquierda de la `MediaBox` del `PDPage` (su `Rotate` no se tiene en cuenta),
y añade sus anotaciones de enlace a las de la página. Las fuentes y las imágenes se incrustan una vez por pintor. La
página la maqueta el motor (`laidOut.pages()`) con las mismas `Resources` que se dan al pintor.

**Nada se etiqueta.** El documento sobre el que se pinta tiene su propio árbol de estructura, o ninguno, y el motor no
lo toca: todo el contenido es un *artifact* (el texto se copia y se busca igual) y las anotaciones de enlace no cuelgan
de ningún `Link`. Un documento que tenga que ser PDF/UA se escribe entero, con `toPdf`.

Los enlaces a una URL funcionan desde el principio. Los enlaces a una caja del documento (`#id`) reciben su destino al
**cerrar** el pintor, si la página que la tiene se ha pintado con ese mismo pintor; si no, su anotación se quita. Cerrar
el pintor cierra también los PDF de los que dibujó páginas; el documento no se guarda, lo guarda quien lo abrió. Un
pintor es de un documento y de un hilo. Admite una `Cancellation`, que se consulta antes de cada página y en cada imagen.

> **Decisión:** sobre una página ajena no se etiqueta nada: etiquetar obligaría a fusionar el árbol de estructura del
> documento ajeno con el del motor, y sin árbol el PDF sigue siendo válido (un *artifact* es contenido legítimo de un PDF
> etiquetado). Quien quiera PDF/UA escribe el documento entero. *Alternativa descartada:* crear el árbol de estructura si
> el documento ajeno no tiene, que dejaría un PDF etiquetado a medias.

> **Decisión:** un enlace interno cuyo destino no se ha pintado se quita y no se deja sin destino: una anotación de
> enlace sin acción ni destino se ve como un enlace y no lleva a nada. *Alternativa descartada:* un error, que impediría
> pintar una sola página de un documento con enlaces entre sus páginas.

## Hilos

La forma recomendada es crear **un `Converter` y compartirlo**: es inmutable y cada conversión crea su propio motor, así que
dos hilos que convierten a la vez no comparten ningún estado que cambie.

| Pieza | ¿Se comparte entre hilos? |
|---|---|
| `Converter`, `Resources`, `DocumentReader` | Sí. Se crean una vez y se usan desde todos los hilos |
| `LaidOutDocument` (lo que devuelve `layOut`) | Sí, para pintarlo: un mismo documento maquetado se puede escribir a PDF y a SVG desde varios hilos. Se entrega a los demás hilos con un `Future`, un ejecutor o un campo `volatile` |
| `Engine` | **No.** Guarda la caché del layout y el estado de la numeración. Uno por conversión, y reutilizarlo en secuencia es seguro (lo suelta todo al terminar cada documento) |
| `Box` y `Style` (el árbol de cajas) | Solo lectura una vez construido |
| `PdfPainter` | **No.** Es de un `PDDocument`, que PDFBox no comparte entre hilos |

`ConcurrentConversionTest` comprueba con ocho hilos convirtiendo los once ejemplos que el PDF es el mismo, byte a byte, que
en la conversión secuencial, y `EngineContractTest`, que un `Engine` reutilizado da lo mismo que uno nuevo. El texto de un
SVG se dibuja con las fuentes de la carpeta de recursos sin darlas de alta en la JVM, así que no depende de su idioma ni de
otras carpetas. JSVG lo dibuja con AWT, así que en un servidor sin pantalla se arranca la JVM con
`-Djava.awt.headless=true`.

> **Decisión:** el contrato de hilos está escrito y no solo observado. El schema compartido de `stxt-core` se apoya en dos
> hechos de la versión 1.0.3, comprobados en su bytecode: el proveedor del schema solo lee de un mapa y el validador solo
> escribe campos en su constructor. Un cambio de versión obliga a repasarlo (está anotado en el `pom.xml` y en
> `DocumentSchema`). *Alternativa descartada:* una copia del schema por hilo.

## Validación en el editor

La extensión de STXT para VS Code carga los schemas de la carpeta `.stxt/` del espacio de trabajo. Copiando allí el schema
([`dev.stxt.pdf.stxt`](../../../../src/main/resources/stxt/dev.stxt.pdf.stxt)) se tiene validación y ayuda contextual
mientras se escriben documentos.
