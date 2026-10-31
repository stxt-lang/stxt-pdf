# Ejemplos

Documentos completos, como los que escribiría un autor, para ver lo que da el motor con las
fuentes y las imágenes reales de [`resources/`](../../resources) y **sin contornos de
depuración**. A diferencia de los casos [golden](../golden), que comprueban un algoritmo con un
resultado calculado a mano, un ejemplo es un documento con sentido, con su contenido y su estilo.

Cada carpeta tiene:

| Fichero | Qué es |
|---|---|
| `input.stxt` | El documento. Valida con el schema y se maqueta sin errores. |
| `styles.stxt` | Las reglas de `Styles` y la tipografía base (`For element: Document`), en un [documento de estilos aparte](../../docs/referencia/comun/estilos.md#documento-de-estilos-aparte) que se pasa junto al documento al maquetar. El documento queda con el contenido y la estructura. |
| `page-N.png` | Una imagen por página del PDF, a 110 ppp. Se versionan. |
| `expected.stxt` | El [volcado](../../docs/referencia/implementacion/pruebas.md#el-volcado) de las cajas con `Id`, con las métricas reales. |
| `output.pdf` | El PDF, para abrirlo desde el repositorio. Se reescribe con `-Dexamples.update=true` y no se compara: lleva la fecha y un identificador. |

Los ejemplos los ejecuta `ExamplesTest` con `mvn test`: comprueba que el documento se lee y se
maqueta, que el volcado no cambia (tolerancia de 0,01 pt) y que las imágenes de las páginas son las
versionadas (salvo el antialiasing). El PDF (etiquetado, con marcadores y las fuentes incrustadas) y
las imágenes nuevas se escriben en `target/reference/examples/NN-nombre/`. Si una imagen cambia, el
test falla, y si el cambio es el que se busca, se acepta con `-Dexamples.update=true` después de mirar
las imágenes. El volcado esperado no se calcula a mano: es una instantánea de lo que da el motor, que se
revisa mirando las imágenes.

```bash
mvn test -Dtest=ExamplesTest                                   # todos
mvn test -Dtest=ExamplesTest -Dexamples.only=01-invoice        # uno
mvn test -Dtest=ExamplesTest -Dexamples.update=true            # acepta los cambios: expected.stxt y page-N.png
```

Las imágenes están en [`resources/images/examples/`](../../resources/images/examples) y las
genera [`tools/ExampleImages.java`](../../tools/ExampleImages.java) con Java2D, sin ficheros de
terceros (`java tools/ExampleImages.java`). Las fotos y la pantalla del móvil son PNG o JPEG, y los
logotipos, las firmas, el sello, los gráficos, el emblema, el adorno, el termostato y el esquema de
conexión, SVG. Las fuentes son IBM Plex Sans, Serif y Mono (SIL
Open Font License). Los nombres, las empresas y los datos de los documentos son inventados.

## Los once documentos

| | Documento | Qué enseña |
|---|---|---|
| [![](01-invoice/page-1.png)](01-invoice) | **[01 · Factura](01-invoice)**<br>Una página A4 | Logotipo y datos de las partes en un grid, tira de condiciones, tabla con cabecera, cuerpo y **pie de totales**, pie de página fijo con `{page}` y `{pages}`, **correos clicables** (`Link: mailto:...`), y los datos para el pago con el IBAN y el BIC como **código en línea** (`Markup: markdown` y `Code font`). Todo el estilo en `styles.stxt`. Las tarjetas del emisor y del cliente con la franja de color a la izquierda y **esquinas redondeadas a la derecha** (`Corner radius: 0 6 6 0`). |
| [![](02-letter/page-1.png)](02-letter) | **[02 · Carta comercial](02-letter)**<br>Una página A4 | Membrete con logotipo, cuerpo en **Markdown** justificado con **Knuth-Plass** (`Line breaking: balanced`), negrita, cursiva y subrayado dentro de la línea, firma escaneada, web del taller en el pie y en el membrete con `Link`. |
| [![](03-resume/page-1.png)](03-resume) | **[03 · Currículum](03-resume)**<br>Una página A4, sin márgenes | Grid de dos columnas a **página completa**, foto **circular** (`Corner radius: 50%`), barras de nivel con anchos en porcentaje y **extremos redondeados**, línea de tiempo en un grid y cifras en un `Flex`, con el correo y la web clicables. |
| [![](04-annual-report/page-1.png)](04-annual-report) | **[04 · Informe anual](04-annual-report)**<br>7 páginas A4 | **Portada a sangre** con un **degradado** de transparente a azul (`linear(180, #12315A00, #12315A)`) que funde la foto con el panel, en una **sección propia** (`Margin: 0`, sin número de página ni el pie del documento: `Page numbering: none` e `Inherit page overlays: false`) y un cuerpo que numera desde 1 («Página 1 de 6»), **capítulos y apartados numerados** (`Numbered`: `1`, `1.1`, `1.2`, también en el índice), **índice automático** (`Table of contents`, con puntos guía, las páginas calculadas por el motor y cada entrada clicable), cifras clave, **gráficos** como imágenes, texto en **dos columnas** con filete, tabla financiera con cabecera y pie, calendario con celdas de varias columnas (`Span`) y secciones que empiezan en página nueva. |
| [![](05-newsletter/page-1.png)](05-newsletter) | **[05 · Boletín](05-newsletter)**<br>2 páginas A4 | Aire de periódico: cabecera de portada, foto recortada con `Fit: cover`, artículos a **tres columnas** con cortes equilibrados, foto al lado del texto, agenda en tabla y anuncios en una rejilla, y la web de la gaceta en el pie. |
| [![](06-menu/page-1.png)](06-menu) | **[06 · Carta de restaurante](06-menu)**<br>2 páginas, 420 × 700 | **Tamaño de página propio**, fondo y marco doble de **esquinas redondeadas** con `Page overlay` de **`Layer` negativa** (detrás del contenido en todas las páginas), platos con precio en un `Flex`, adornos e imagen centrada. |
| [![](07-contract/page-1.png)](07-contract) | **[07 · Contrato](07-contract)**<br>3 páginas A4 | Cabecera y pie fijos con rúbricas, apartados numerados (`Numbered`, con una referencia `{number of: ...}`), `Exponen` con numeración romana, doce **cláusulas con tres niveles de numeración** escritas en Markdown, **anexo numerado en romanos con su etiqueta** («Anexo I», `Number format` y `Number label`, citado desde las cláusulas) con la tabla de honorarios y **bloque de firmas que no se parte**. |
| [![](08-manual/page-1.png)](08-manual) | **[08 · Manual](08-manual)**<br>4 páginas A4 | Figura con **números superpuestos** (un `Frame` con `Overlay` encima, y cada número es una insignia **circular**), leyenda al lado, avisos con color y **esquinas redondeadas** a la derecha de la franja, pasos numerados, esquema de conexión, **bloque de terminal** preformateado (`White space: preserve`) con **código en línea** (`Code font`), tablas de especificaciones y de averías, que remite al esquema con **`{page of: id}`** (también dentro de Markdown), con un **enlace de Markdown a la tabla de especificaciones** (`[la sección {number of: ...}](#specs)`), **apartados y subapartados numerados** (`Numbered`: de `1` a `8`, y `3.1`, `3.2`) y correos y web clicables, y marcadores en el PDF. |
| [![](09-catalog/page-1.png)](09-catalog) | **[09 · Catálogo](09-catalog)**<br>5 páginas A4 | Portada a sangre, **índice de productos** (un `Table of contents` de dos niveles con su sangría, su página y cada entrada clicable), **fichas de producto** de esquinas redondeadas en una rejilla de dos columnas, con foto recortada (con las esquinas de arriba redondeadas) y etiqueta «Novedad» **en diagonal** en la esquina (un `Overlay` con `Rotate: 40`), tabla de maderas y página de pedidos. El papel es `Paper: A4`. |
| [![](10-certificate/page-1.png)](10-certificate) | **[10 · Certificado](10-certificate)**<br>Una página A4 apaisada | `Paper: A4` con `Orientation: landscape`. Fondo y **tres marcos concéntricos** con adornos **circulares** en las esquinas, nombre como protagonista, dos firmas y un sello con cintas **ligeramente girado** (`Rotate: -8`), todo centrado, el **número del certificado en vertical** en el margen (un `Page overlay` girado −90 grados alrededor de su esquina) y la dirección de verificación clicable en el pie. |
| [![](11-timetable/page-1.png)](11-timetable) | **[11 · Horario](11-timetable)**<br>Una página A4 apaisada | `Paper: A4` con `Orientation: landscape`. Tabla con las celdas separadas por un `Gap` y **redondeadas** (`Corner radius` en `For element: Cell`), **colores por estilo**, recreo y comedor que ocupan los cinco días con `Span`, leyenda en un `Flex` con salto de línea y notas en Markdown. |

## Cómo están hechos

Lo que se repite en varios ejemplos y cómo se resuelve:

- **Una portada sin cabecera ni pie** es una `Section` (ejemplos 04 y 09), con su `Page`
  (`Margin: 0`, así que es a sangre sin ningún `Offset` negativo), `Page numbering: none` (no cuenta, y la
  primera página interior es la 1) e `Inherit page overlays: false` (sin el pie, que es un `Page overlay` del
  documento). Una sección siempre empieza página. `Pages` sirve dentro de una sección o cuando no hay
  portada: quita el logotipo de la primera hoja del contrato (07), el pie de la primera página de la carta (06) y
  pone un titulillo corrido desde la segunda página del boletín (05).
- **El texto con formato** se escribe con `Markup: markdown` (una regla `For element: Paragraph` o el
  `Typography` de un `Text`: se hereda) para una negrita o una cursiva dentro de una frase, y con
  `Markdown` cuando hay además listas, encabezados o tablas. Sin `Markup` los asteriscos son
  literales. Las cláusulas del contrato (07) y los pasos del manual (08) son `Markdown` porque
  son listas. Las partes del contrato, la leyenda de la figura (08) y la carta de la
  presidenta (04) son `Paragraph`.
- **Los rótulos en mayúsculas** se escriben como una frase normal (`Emisor`, `Fecha de emisión`) y
  una regla de `Styles` los pone en mayúsculas con `Case: uppercase`, casi siempre con unos
  puntos de `Letter spacing` (ejemplos 01, 03, 04, 05, 06, 07, 08, 09 y 10). El PDF etiquetado y los
  marcadores conservan lo escrito (`Experiencia` y no `EXPERIENCIA`). Las siglas y los códigos
  (`CIF B-17482930`) se dejan como están.
- **Los espacios se colapsan**, salvo con `White space: preserve` (o `preserve-wrap`): el bloque de terminal con
  columnas alineadas del manual (08) es un solo `Text` en monoespaciada con los espacios escritos, y
  con `Code font` el código se escribe con comillas invertidas, en un texto con `Markup: markdown` (el
  IBAN y el BIC de la factura, 01) o en un `Markdown` (el borne `N` del manual, 08).
- **Las tablas se estilan con reglas** y no con un estilo en cada celda: `For element: Head cell` da la
  cabecera (también a las tablas de Markdown), `Even row cell` la cebra (ejemplo 05) y
  `Column align` o `For column` las columnas numéricas. Lo que es de una sola fila, como los
  totales de la factura (ejemplos 01, 04 y 07), va en un estilo en la `Row` (`Style: total`), que
  aplica sus reglas a todas sus celdas.
- **Los enlaces** son una propiedad de la caja entera (`Link`) o un tramo de Markdown
  (`[texto](destino)`). Un `Text` con un teléfono y una web (ejemplos 02 y 09) lleva `Link` y es
  clicable entero, también el teléfono. Con `Markup: markdown` se puede enlazar solo una parte
  (`[web](https://...)`).
- **El índice** lo da `Table of contents`: sus entradas son clicables, y sus páginas, las de `{page of: id}`. Lista
  los encabezados de sus niveles, así que el título de la portada y el del propio índice son un `Heading 1` con
  `In table of contents: false`, para que no salgan en él (ejemplos 04 y 09).
- **Esquinas, degradados y opacidad.** `Corner radius` redondea el fondo y el borde de su caja, y la caja recorta por
  su curva los fondos, los bordes y las imágenes de lo que contiene
  ([La caja](../../docs/referencia/comun/caja.md#esquinas)). Un color por lado (`Border color`) y la franja de un aviso (`Border: 0 0 0 3`)
  se combinan con un radio solo en el lado contrario (`Corner radius: 0 4 4 0`). Un degradado con un extremo
  transparente (04) funde una imagen con el color de lo que hay debajo sin tocar la imagen. Sin sombras
  ([La caja](../../docs/referencia/comun/caja.md#lo-que-no-hay)): las fichas del catálogo llevan un filete.
- **Lo girado** es una caja con `Rotate`. La etiqueta en diagonal del catálogo (09) es un `Overlay`, y el número
  vertical del certificado (10), un `Page overlay` girado −90 grados alrededor de su esquina de arriba a la izquierda
  (`Rotate origin: top left`), así que su `Offset` es el de esa esquina y la caja sube desde ella. Un posicionado
  girado no cambia el layout. El sello del certificado es una `Image` en flujo, que ocupa el rectángulo que la envuelve
  girada ([Capas y giro](../../docs/referencia/comun/capas-y-giro.md#giro-rotate)). Lo girado tiene que caber en la
  página: si no, es un error.
- **Lo vectorial es SVG**: los logotipos (01, 02, 07, 09), las firmas (02, 07, 10), el sello
  (10), los tres gráficos del informe (04), el emblema y el adorno de la carta (06) y el termostato y el
  esquema de conexión del manual (08) son `Image` con un `.svg` de `images/`: se ven nítidos a cualquier
  tamaño y el PDF los lleva como rutas y degradados y no como píxeles. Su texto usa las fuentes IBM Plex de `fonts/` (el motor
  no sustituye ninguna), pero se dibuja como contornos: no se puede seleccionar. Las fotos y la pantalla
  del móvil (08) siguen siendo PNG o JPEG.
- **El papel tiene nombre**: los A4 dicen `Paper: A4` (y los apaisados, 10 y 11, además
  `Orientation: landscape`) en vez de escribir 595 × 842. La carta del restaurante (06) es la única con
  un tamaño propio (420 × 700), que se sigue escribiendo con `Width` y `Height`.
