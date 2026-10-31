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
| `exclude(Check...)` | Quita comprobaciones. Por defecto se hacen todas ([Comprobaciones](#comprobaciones)) |

> **Decisión:** la configuración es un objeto inmutable, `Converter`, seguro entre hilos porque cada conversión crea su
> propio `Engine`, con `layOut`, `toPdf` (de un documento o de uno ya maquetado) y `toSvg`. Los estilos nunca son `null`: si
> no hay, no se llama a `styles`. Vive en un package propio, encima de todos los demás. `PdfDocumentWriter.write` y `Engine`
> siguen siendo públicos como capa de bajo nivel. *Alternativa descartada:* un método con una sobrecarga por cada
> combinación de opciones.

## Comprobaciones

Algunas comprobaciones no son reglas del documento sino de lo que se ve o de lo que traen los ficheros, y quien convierte
puede quitarlas con `exclude`, que se acumula entre llamadas. Por defecto se hacen todas, y `Converter.excluded()` dice
las que se han quitado. Son los valores del enum `Check` (`model`):

| `Check` | Qué comprueba | Sin ella |
|---|---|---|
| `CONTRAST` | El contraste del texto con su fondo, `CONTRAST_LOW` ([El PDF](../comun/pdf.md#contraste)). `layOut` lo comprueba con `ContrastCheck`, un lienzo que apunta lo que se pinta | Un texto puede no leerse |
| `PDF_IMAGE_FONTS` | Que las fuentes de la página de un PDF que dibuja una `Image` estén incrustadas y digan qué carácter es cada glifo, `IMAGE_FORMAT` ([Image](../componentes/image.md#pdf)) | El visor puede sustituir una fuente, su texto puede no copiarse y el PDF puede no cumplir PDF/UA |
| `PDF_IMAGE_ANNOTATIONS` | Que la página de un PDF no tenga anotaciones que se vean, `IMAGE_FORMAT` | La página se dibuja sin ellas |

```java
Converter converter = Converter.builder().exclude(Check.CONTRAST, Check.PDF_IMAGE_ANNOTATIONS).build();
```

`Engine` recibe las que se quitan y se las pasa a `Images`, y el contraste lo comprueba `Converter.layOut` después de
maquetar.

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
esos dos puede ser `null`. Ver [Styles](../comun/estilos.md#documento-de-estilos-aparte).

## Recursos

`new Resources(Path.of("otra"))` usa otra carpeta de recursos. Toda ruta pasa por `Resources.resolve`, que nunca va hacia
atrás. `Resources` se comparte entre hilos y entre conversiones. Indexa `fonts/` una vez y guarda las imágenes leídas.

`FontLibrary` identifica cada TTF por su interior:

- **familia:** la tipográfica de la tabla `name` (`nameID` 16) o, si no la tiene, la familia (`nameID` 1);
- **peso:** `usWeightClass` de `OS/2` (400 o 700, y los demás se ignoran sin error);
- **estilo:** el bit de cursiva de `fsSelection`.

> **Decisión:** las fuentes solo se comprueban con `PdfMetrics` (`Engine.layOutDocument` llama a `validate`). Con
> `FixedMetrics` no se mira la carpeta de fuentes: basta con que `Font` esté declarada. *Alternativa descartada:* validar
> siempre las fuentes al construir el árbol, que obligaría a tener la carpeta también para maquetar sin PDF.

## Errores

Todo error del motor es un `StxtPdfException` (lo lanzan `DocumentReader` y `Converter`), así que una sola `catch` basta.
`kind()` dice de quién es la culpa, `code()` qué falla y `line()` dónde (`NO_LINE`, 0, si no es de una línea).
`inStyles()` dice si esa línea es del [documento de estilos aparte](../comun/estilos.md#documento-de-estilos-aparte) y no del documento:

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> reply(422, e.code() + " en la línea " + e.line()
                + (e.inStyles() ? " de los estilos" : "") + ": " + e.getMessage());
        case LIMIT -> reply(413, e.getMessage());
        case RESOURCE, INTERNAL -> reply(500, e.getMessage());   // de la configuración o del motor
    }
}
```

| Clase | Qué es |
|---|---|
| `InvalidModelException` (`tree`) | Las reglas del modelo, los desbordamientos y los recursos que nombra el documento. `errorCode()` da el `ErrorCode` y `ErrorCode.kind()` su tipo (`INVALID_DOCUMENT` salvo `PAGE_LIMIT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX` y `PDF_TOO_LARGE`, que son `LIMIT`) |
| `DocumentSyntaxException` (`tree`) | La sintaxis y el schema de `stxt-core`, con su código y su línea y la excepción original como causa. Un `LimitException` de la librería es `LIMIT` |
| `ResourceException` (`resources`) | La carpeta de recursos (`RESOURCE`), con `ImageFormatException` |
| `InternalException` (`model`) | Un fallo del motor. La causa original va en `getCause()` |

Los fallos de entrada y salida al escribir en el destino siguen siendo la `IOException` de `toPdf`. `Converter` deja pasar
los errores del motor, convierte cualquier otra excepción de ejecución en `InternalException` y no toca la `IOException`
del destino. Un `IllegalStateException` o `IllegalArgumentException` solo es válido dentro del motor para un mal uso
interno. Lo que puede ocurrirle a quien convierte va con uno de estos tipos. Los códigos y los tipos están en
[Errores](../comun/errores.md).

> **Decisión:** `code()` es el código en texto, común a todos, y el enum pasa a `errorCode()` en `InvalidModelException`:
> los errores de la librería tienen un conjunto abierto de códigos que no caben en `ErrorCode`. *Alternativa descartada:*
> añadir cada código de `stxt-core` a `ErrorCode`, que ata el motor a la lista de la librería.

> **Decisión:** `Converter` es la frontera de los errores: deja pasar los del motor y la `IOException` del destino, y
> convierte cualquier otra excepción de ejecución en un `InternalException` con el original como causa, así que una sola
> `catch` vale también para un defecto del motor o de PDFBox. *Alternativa descartada:* envolver también la `IOException`
> del destino, que no es un error del motor.

## Límites

`Limits` es inmutable. `Limits.DEFAULT` tiene los valores por defecto ([Límites](../comun/limites.md)) y se cambia con
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

## Hilos

La forma recomendada es crear **un `Converter` y compartirlo**: es inmutable y cada conversión crea su propio motor, así que
dos hilos que convierten a la vez no comparten ningún estado que cambie.

| Pieza | ¿Se comparte entre hilos? |
|---|---|
| `Converter`, `Resources`, `DocumentReader` | Sí. Se crean una vez y se usan desde todos los hilos |
| `LaidOutDocument` (lo que devuelve `layOut`) | Sí, para pintarlo: un mismo documento maquetado se puede escribir a PDF y a SVG desde varios hilos. Se entrega a los demás hilos con un `Future`, un ejecutor o un campo `volatile` |
| `Engine` | **No.** Guarda la caché del layout y el estado de la numeración. Uno por conversión, y reutilizarlo en secuencia es seguro (lo suelta todo al terminar cada documento) |
| `Box` y `Style` (el árbol de cajas) | Solo lectura una vez construido |

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
([`dev.stxt.pdf.stxt`](../../../src/main/resources/stxt/dev.stxt.pdf.stxt)) se tiene validación y ayuda contextual
mientras se escriben documentos.
