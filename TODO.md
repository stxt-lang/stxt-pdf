# Pendiente para que pdf_gen pueda usar stxt-pdf

pdf_gen (`../pdf_gen`) genera hoy sus PDF con XHTML y OpenHTMLtoPDF. En noviembre de 2026 se hizo una prueba de concepto
para cambiarlo por stxt-pdf: los renders de pdf_gen traducirían su STXT `es.fiatc.pdf` a `dev.stxt.pdf` y stxt-pdf
maquetaría y pintaría. Sale: cuatro documentos (carta, IPID, preimpreso y rotación) se escribieron a mano en
`dev.stxt.pdf`, cumplen PDF/A-3a y PDF/UA-1 y se parecen mucho a lo de hoy, y stxt-pdf es unas 2,5 veces más rápido a
24.000 páginas. Faltan las cosas de esta lista.

Todo está en `../pdf_gen/poc-stxt-pdf/`: el informe (`INFORME.md`), los documentos traducidos (`documentos/`), lo que
genera cada motor (`referencia-pdf_gen/` y `salida/`) y el arnés (`./poc.sh documento documentos/ipid.stxt salida/ipid`,
`./poc.sh escala 24000`, con las clases de `target/` de aquí: antes, `mvn test-compile`).

Cada punto, cuando se haga, va al `CHANGELOG.md` y a la documentación (`docs/es` y `docs/en`) como cualquier otro cambio,
y se quita de aquí.

## Bloqueante

### 1. Cancelar una conversión

**Qué pide pdf_gen.** Un plazo por PDF (`GENERACION_MAX_S`, 60 s): 100 KB de códigos QR son 20–30 s, y el tamaño del
cuerpo no acota el tiempo. Hoy el plazo funciona porque OpenHTMLtoPDF llama a código de pdf_gen durante el layout (cada
imagen), y ahí se comprueba un `ThreadLocal`. `Converter.toPdf` es una sola llamada y no hay ningún punto en el que se
pueda cortar (no se mira `Thread.interrupted()` en ningún sitio).

**Dónde.** `Engine` (cada `context.layout`), `Paginator` (cada página y cada vuelta de la convergencia de las
referencias), `StackingContext`/`PdfCanvas` (cada página al pintar), la lectura de imágenes y SVG.

**Opciones.** Mirar `Thread.currentThread().isInterrupted()`, o recibir algo propio en el `Converter` o en la llamada
(un `BooleanSupplier`, un `Instant` límite). Lo segundo no depende de quién interrumpe el hilo y se prueba sin hilos. El
error, un tipo nuevo (`LIMIT`, `TIMEOUT`...) sin línea, que atraviesa todo sin que nada lo envuelva.

**Hecho cuando** un documento caro (miles de `Barcode`, miles de páginas) se corta poco después del límite, en cualquier
fase, y lo prueba un test.

## Importante: sin esto, el IPID no se puede hacer bien

### 2. Columnas paralelas que se parten cada una por su lado

**Qué pide pdf_gen.** Su `Columnas`: 2 o 3 columnas, cada una con su contenido, con fondo, de la misma altura, y que se
parten entre páginas cada una por su lado, **empezando en la página en la que están**.

**Qué pasa.** Con un `Grid` (`* *`), una fila que cabe entera en una página salta entera a la siguiente: el IPID dejaba
la página 1 casi vacía y salía en 3 páginas en vez de 2. stxt-pdf solo parte una fila si no cabe ni al principio de una
página. Con `Columns` (`Balance: false`, `Before: column` en la segunda) cabe en 2, pero lo que no cabe en la columna
derecha sigue en la columna **izquierda** de la página siguiente.

**Dónde.** `UnitBreaker` (los cortes de `Stack`, `Grid` y tablas) y `GridLayout`.

**Opciones.** Una propiedad del `Grid` (o de la fila) para partir las filas en la página actual, cada celda por su lado
y con el fondo hasta el final de la página. Lo de las tablas (`Keep together`) debería quedar igual.

**Hecho cuando** `../pdf_gen/poc-stxt-pdf/documentos/ipid.stxt` (o una versión con `Grid`) sale en 2 páginas con la
columna derecha que sigue en la derecha.

### 3. Marcador de lista con imagen

**Qué pide pdf_gen.** Sus `Lista` llevan un símbolo por elemento (`visto`, `visto azul`, `cruz`, `aviso`, `guion`): un
icono PNG de 48 x 48 (`../pdf_gen/src/main/resources/simbolos/`), y en la misma lista puede haber iconos distintos. En el
PDF etiquetado tiene que seguir siendo una lista (`L`, `LI`, `Lbl`, `LBody`).

**Qué pasa.** `Marker` solo tiene marcadores de texto. En la POC, `Marker: none` y un `Grid` por elemento con el icono
y el texto: se ve igual, pero cada elemento salta entero de página y en el PDF ya no es una lista de verdad.

**Dónde.** `model/Marker`, `tree/ListBuilder` y el layout de los marcadores (el bloque usa `inline`).

**Opciones.** Un `Marker` con una imagen de `images/`, en la lista y en cada `List item`, con su tamaño (o el del
texto). La imagen, decorativa (`Lbl` sin texto alternativo, o el texto del símbolo).

**Hecho cuando** el IPID se escribe con `List` y cada elemento se parte entre páginas como un párrafo.

## Menores

### 4. `Subject` del PDF

pdf_gen exige `Descripción` en todo documento y la escribe como `Subject` del diccionario Info y como `dc:description`
del XMP. stxt-pdf solo escribe el título (`paint/PdfMetadata`). Con PDF/A, retocarlo después con PDFBox obliga a
rehacer el XMP: mejor en el motor. Una propiedad de `Document` (`Description`?), opcional.

### 5. Salto de línea forzado en el Markdown

`tree/MarkdownBuilder` rechaza `HardLineBreak` («firmante\cargo» da `MARKDOWN_UNSUPPORTED`), y el Markdown de pdf_gen
lo admite. El traductor puede partir el párrafo en varios `Text`, pero dentro de un párrafo justificado no es lo mismo.
En `docs/es/reference/components/markdown.md` está en «Lo que no hay», sin ninguna decisión que diga por qué: falta ver
si hay algún motivo (un `Text` sí admite saltos de línea con `Preformatted`).

### 6. Reparto de las columnas de una tabla según su contenido

pdf_gen deja que OpenHTMLtoPDF reparta el ancho de las columnas en proporción al contenido. En la tabla de la carta, la
2.ª columna empieza en 72,8 mm; aquí, con `auto *`, en 51,7 mm, y con `auto auto`, en 90,5 mm. O una regla de reparto
como la de CSS (`TrackSizing`), o que el traductor de pdf_gen fije los anchos: decidir cuál.

### 7. Modo de fusión

pdf_gen pone fuera de PROD una marca de agua enorme («Entorno: XXX») encima de todo, con fusión `Multiply`: oscurece lo
de debajo sin taparlo y los códigos se siguen leyendo. Aquí hay opacidad, pero no modo de fusión. pdf_gen puede seguir
dibujándola después con PDFBox, así que solo vale la pena si sirve para algo más (`Blend: multiply` en un `Page
overlay`?).

### 8. Metadatos propios del Info

pdf_gen admite hasta 50 claves propias en el diccionario Info (`Meta: poliza = ...`, para gestores documentales) y las
escribe antes de cifrar. Se pueden seguir escribiendo después con PDFBox (veraPDF no pide su copia en el XMP), así que
solo hace falta si se quiere todo en el motor.

## Decisiones (de pdf_gen y de aquí)

- **Lo que se repite en cada página, etiquetado.** `Page overlay` siempre es decoración. pdf_gen etiqueta como `Figure`
  o `P` lo que va con `Posición` y `En páginas: todas/siguientes` (en un preimpreso, los campos de cada hoja). Decidir si
  basta con que sea artefacto (lo repetido no se suele leer) o si hace falta un `Page overlay` etiquetado.
- **Zona de silencio del Code 128.** Aquí son 10 módulos por lado, los de la norma, y pdf_gen usa 5: en la misma caja,
  las barras salen 3,5–5 mm más estrechas. Aceptarlo o hacerla configurable (`tree/Barcodes`).
- **Texto de un código más ancho que su caja.** pdf_gen lo deja salirse; aquí es un error. Probablemente está bien así.
- **Ayudas de maquetación.** pdf_gen dibuja, fuera de PROD, la rejilla, el límite de los márgenes y cada caja con
  posición con su etiqueta (`línea N: x, y | ancho x alto`) y los solapes en rojo. Aquí solo está `debug` (contornos),
  que es del `Converter` y no del documento. pdf_gen puede seguir dibujándolas con PDFBox: decidir si interesa algo
  así en el motor.

## Lo que no hay que hacer aquí

Lo resuelve el traductor de pdf_gen (detalle en el informe):
- Los errores en castellano y con la línea de su STXT.
- `Máximo de páginas` y el `Relleno`, que salen de `converter.layOut()` y de los `Fragment`.
- Los márgenes de dentro de los contenedores, que hoy da el CSS de pdf_gen.
- El texto bajo los códigos, el interlineado del 133 % y el tamaño de lo girado en diagonal.
- El cifrado, que pdf_gen ya hace después con PDFBox.
