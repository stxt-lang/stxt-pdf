# Ejemplos

Documentos completos, como los que escribiría un autor, para ver lo que da el motor con las
fuentes y las imágenes reales de [`resources/`](../../resources) y **sin contornos de
depuración**. A diferencia de los casos [golden](../golden), que comprueban un algoritmo con un
resultado calculado a mano, un ejemplo es un documento con sentido, con su contenido y su estilo.

Cada carpeta tiene:

| Fichero | Qué es |
|---|---|
| `input.stxt` | El documento. Valida con el schema y se maqueta sin errores. |
| `styles.stxt` | Las reglas de `Styles` y la tipografía base (`Element: Document`), en un [documento de estilos aparte](../../docs/ESPECIFICACION.md#documento-de-estilos-aparte) que se pasa junto al documento al maquetar. El documento queda con el contenido y la estructura. |
| `output.pdf` | El PDF (etiquetado, con marcadores y las fuentes incrustadas). **No se versiona**: se regenera con `mvn test` (está en `.gitignore`). |
| `page-N.png` | Una imagen por página del PDF, a 110 ppp. Se versionan. |
| `expected.stxt` | El volcado ([§7](../../docs/ESPECIFICACION.md#7-formato-de-resultados)) de las cajas con `Id`, con las métricas reales. |

Los ejemplos los ejecuta `ExamplesTest` con `mvn test`: comprueba que el documento se lee y se
maqueta y que el volcado no cambia (tolerancia de 0,01 pt). Las imágenes solo se
reescriben si cambian sus píxeles, así que si aparecen modificadas en git, el resultado ha
cambiado: míralas antes de dar el cambio por bueno. El volcado esperado no se
calcula a mano, es una instantánea de lo que da el motor, que se revisa mirando las imágenes.

```bash
mvn test -Dtest=ExamplesTest                                   # todos
mvn test -Dtest=ExamplesTest -Dexamples.only=01-invoice        # uno
mvn test -Dtest=ExamplesTest -Dexamples.update=true            # regenera los expected.stxt
```

Las imágenes están en [`resources/images/examples/`](../../resources/images/examples) y las
genera [`tools/ExampleImages.java`](../../tools/ExampleImages.java) con Java2D, sin ficheros de
terceros (`java tools/ExampleImages.java`): las fotos y la pantalla del móvil son PNG o JPEG; los
logotipos, las firmas, el sello, los gráficos, el emblema, el adorno, el termostato y el esquema de
conexión son SVG. Las fuentes son IBM Plex Sans, Serif y Mono (SIL
Open Font License). Los nombres, las empresas y los datos de los documentos son inventados.

## Los once documentos

| | Documento | Qué enseña |
|---|---|---|
| [![](01-invoice/page-1.png)](01-invoice) | **[01 · Factura](01-invoice)**<br>Una página A4 | Logotipo y datos de las partes en un grid, tira de condiciones, tabla con cabecera, cuerpo y **pie de totales**, pie de página fijo con `{page}` y `{pages}`, **correos clicables** (`Link: mailto:…`), y los datos para el pago en un `Markdown` con el IBAN y el BIC como **código en línea** (`Code font`). Todo el estilo en `styles.stxt`; las tarjetas del emisor y del cliente con la franja de color a la izquierda y **esquinas redondeadas a la derecha** (`Radius: 0 6 6 0`). |
| [![](02-letter/page-1.png)](02-letter) | **[02 · Carta comercial](02-letter)**<br>Una página A4 | Membrete con logotipo, cuerpo en **Markdown** justificado con **Knuth-Plass** (`Text wrap: pretty`), negrita, cursiva y subrayado dentro de la línea, firma escaneada, web del taller en el pie y en el membrete con `Link`. |
| [![](03-resume/page-1.png)](03-resume) | **[03 · Currículum](03-resume)**<br>Una página A4, sin márgenes | Grid de dos columnas a **página completa**, foto **circular** (`Radius: 50%`), barras de nivel con anchos en porcentaje y **extremos redondeados**, línea de tiempo en un grid y cifras en un `Flex`; correo y web clicables. |
| [![](04-annual-report/page-1.png)](04-annual-report) | **[04 · Informe anual](04-annual-report)**<br>7 páginas A4 | **Portada a sangre** con un **degradado** de transparente a azul (`linear(180, #12315A00, #12315A)`) que funde la foto con el panel, en una **sección propia** (`Margin: 0`, sin número de página ni el pie del documento: `Page numbering: none` e `Inherit fixed: false`) y un cuerpo que numera desde 1 («Página 1 de 6»), **capítulos y apartados numerados** (`Numbered`: `1`, `1.1`…, también en el índice), **índice automático** (`Contents`, con puntos guía, las páginas calculadas por el motor y cada entrada clicable), cifras clave, **gráficos** como imágenes, texto en **dos columnas** con filete, tabla financiera con cabecera y pie, calendario con celdas de varias columnas (`Span`) y secciones que empiezan en página nueva. |
| [![](05-newsletter/page-1.png)](05-newsletter) | **[05 · Boletín](05-newsletter)**<br>2 páginas A4 | Aire de periódico: cabecera de portada, foto recortada con `Fit: cover`, artículos a **tres columnas** con cortes equilibrados, foto al lado del texto, agenda en tabla y anuncios en una rejilla, y la web de la gaceta en el pie. |
| [![](06-menu/page-1.png)](06-menu) | **[06 · Carta de restaurante](06-menu)**<br>2 páginas, 420 × 700 | **Tamaño de página propio**, fondo y marco doble de **esquinas redondeadas** con `Fixed` de **`Layer` negativa** (detrás del contenido en todas las páginas), platos con precio en un `Flex`, adornos e imagen centrada. |
| [![](07-contract/page-1.png)](07-contract) | **[07 · Contrato](07-contract)**<br>3 páginas A4 | Cabecera y pie fijos con rúbricas, apartados numerados (`Numbered`, con una referencia `{number of: …}`), `Exponen` con numeración romana, doce **cláusulas con tres niveles de numeración** escritas en Markdown, anexo con tabla de honorarios y **bloque de firmas que no se parte**. |
| [![](08-manual/page-1.png)](08-manual) | **[08 · Manual](08-manual)**<br>4 páginas A4 | Figura con **números superpuestos** (un `Relative` con `Absolute` encima; cada número es una insignia **circular**), leyenda al lado, avisos con color y **esquinas redondeadas** a la derecha de la franja, pasos numerados, esquema de conexión, **bloque de terminal** preformateado (`White space: pre`) con **código en línea** (`Code font`), tablas de especificaciones y de averías, que remite al esquema con **`{page of: id}`** (también dentro de Markdown), con un **enlace de Markdown a la tabla de especificaciones** (`[la sección {number of: …}](#specs)`), **apartados y subapartados numerados** (`Numbered`: `1`…`8`, `3.1`…) y correos y web clicables, y marcadores en el PDF. |
| [![](09-catalog/page-1.png)](09-catalog) | **[09 · Catálogo](09-catalog)**<br>5 páginas A4 | Portada a sangre, **índice de productos** (un `Contents` de dos niveles con su sangría, su página y cada entrada clicable), **fichas de producto** de esquinas redondeadas en una rejilla de dos columnas, con foto recortada (con las esquinas de arriba redondeadas) y etiqueta «Novedad» **en diagonal** en la esquina (un `Absolute` con `Rotate: 40`), tabla de maderas y página de pedidos; el papel es `Paper: A4`. |
| [![](10-certificate/page-1.png)](10-certificate) | **[10 · Certificado](10-certificate)**<br>Una página A4 apaisada | `Paper: A4` con `Orientation: landscape`. Fondo y **tres marcos concéntricos** con adornos **circulares** en las esquinas, nombre como protagonista, dos firmas y un sello con cintas **ligeramente girado** (`Rotate: -8`), todo centrado; el **número del certificado en vertical** en el margen (un `Fixed` girado −90 grados alrededor de su esquina) y la dirección de verificación clicable en el pie. |
| [![](11-timetable/page-1.png)](11-timetable) | **[11 · Horario](11-timetable)**<br>Una página A4 apaisada | `Paper: A4` con `Orientation: landscape`. Tabla con las celdas separadas por un `Gap` y **redondeadas** (`Radius` en `Element: Cell`), **colores por clase**, recreo y comedor que ocupan los cinco días con `Span`, leyenda en un `Flex` con salto de línea y notas en Markdown. |

## Lo que se aprende al escribirlos

Qué se resolvió bien con lo que hay y qué se echa en falta (apuntado en [`TODO.md`](../../TODO.md)):

- **Una portada sin cabecera ni pie** es una `Section` (hito 32, ejemplos 04 y 09): con su `Media`
  (`Margin: 0`, así que es a sangre sin ningún `Offset` negativo), `Page numbering: none` (no cuenta, y la
  primera página interior es la 1) e `Inherit fixed: false` (sin el pie, que es un `Fixed` del documento).
  Ya no hace falta el bloque de 1 pt con `Break: After: page` que forzaba el salto: una sección siempre
  empieza página. `Pages` sigue sirviendo dentro de una sección o cuando no hay portada: quita el
  logotipo de la primera hoja del contrato (07), el pie de la primera página de la carta (06) y
  pone un titulillo corrido desde la segunda página del boletín (05).
- **El texto con formato** se escribe con `Inline: markdown` (una regla `Element: Paragraph` o el
  `Typography` de un `Text`: se hereda) para una negrita o una cursiva dentro de una frase, y con
  `Markdown` cuando hay además listas, encabezados o tablas. Sin `Inline` los asteriscos son
  literales. Las cláusulas del contrato (07) y los pasos del manual (08) siguen siendo `Markdown`
  porque son listas; las partes del contrato, la leyenda de la figura (08) y la carta de la
  presidenta (04) ya son `Paragraph`.
- **Los rótulos en mayúsculas** se escriben como una frase normal (`Emisor`, `Fecha de emisión`) y
  una regla de `Styles` los pone en mayúsculas con `Transform: uppercase`, casi siempre con unos
  puntos de `Letter spacing` (ejemplos 01, 03, 04, 05, 06, 07, 08, 09 y 10). El PDF etiquetado y los
  marcadores conservan lo escrito (`Experiencia`, no `EXPERIENCIA`). Las siglas y los códigos
  (`CIF B-…`) se dejan como están.
- **Los espacios se colapsan**, salvo con `White space: pre` (o `pre-wrap`): el bloque de terminal con
  columnas alineadas del manual (08) es un solo `Text` en monoespaciada con los espacios escritos, y
  en un `Markdown` con `Code font` el código se escribe con comillas invertidas (el IBAN y el BIC de la
  factura, 01; el borne `N` del manual, 08).
- **Las tablas se estilan con reglas**, no con una clase en cada celda: `Element: Head cell` da la
  cabecera (también a las tablas de Markdown), `Even row cell` la cebra (ejemplo 05) y
  `Column align` o `For column` las columnas numéricas. Lo que es de una sola fila, como los
  totales de la factura (ejemplos 01, 04 y 07), va en una clase en la `Row` (`Class: total`), que
  aplica sus reglas a todas sus celdas.
- **Los enlaces** son una propiedad de la caja entera (`Link`) o un tramo de Markdown
  (`[texto](destino)`). Un `Text` con un teléfono y una web (ejemplos 02 y 09) es clicable entero,
  también el teléfono: con `Inline: markdown` se podría enlazar solo una parte (`[web](https://…)`),
  y los ejemplos 02 y 09 aún no lo usan.
- **El índice** lo da `Contents`, sus entradas son clicables, y las páginas, `{page of: id}`. Un `Contents` lista todos los
  encabezados de sus niveles, así que la portada y el título del propio índice son un `Text`, no
  un encabezado, para que no salgan en él (no hay forma de excluir uno).
- **Esquinas, degradados y opacidad** (hito 28). `Radius` redondea el fondo y el borde de su caja pero no
  recorta a los hijos (no hay `overflow`), así que la foto de una ficha del catálogo (09) lleva su propio
  `Radius` en las esquinas de arriba. Un color por lado (`Border color`) y la franja de un aviso (`Border: 0 0 0 3`)
  se combinan con un radio solo en el lado contrario (`Radius: 0 4 4 0`). Un degradado con un extremo
  transparente (04) funde una imagen con el color de lo que hay debajo sin tocar la imagen. Sin sombras
  (ver `TODO.md`): las fichas del catálogo siguen con su filete.
- **Lo girado** (hito 30) es un posicionado o una imagen con `Rotate`: la etiqueta en diagonal del
  catálogo (09) es un `Absolute`, y el número vertical del certificado (10), un `Fixed` girado −90 grados
  alrededor de su esquina de arriba a la izquierda (`Rotate origin: top left`), así que su `Offset` es
  el de esa esquina y la caja sube desde ella. Como el giro no cambia el layout, el sello girado del
  certificado no mueve las firmas; lo que hay que vigilar es que lo girado quepa en la página, porque
  si no es un error.
- **Lo vectorial es SVG** (hito 31): los logotipos (01, 02, 07, 09), las firmas (02, 07, 10), el sello
  (10), los tres gráficos del informe (04), el emblema y el adorno de la carta (06) y el termostato y el
  esquema de conexión del manual (08) son `Image` con un `.svg` de `images/`: se ven nítidos a cualquier
  tamaño y el PDF los lleva como rutas y degradados, no como píxeles. Tienen el mismo tamaño natural que
  tenían los PNG, así que ningún volcado cambia. Su texto usa las fuentes IBM Plex de `fonts/` (el motor
  no sustituye ninguna), pero se dibuja como contornos: no se puede seleccionar. Las fotos y la pantalla
  del móvil (08) siguen siendo PNG o JPEG.
- **El papel tiene nombre**: los A4 dicen `Paper: A4` (y los apaisados, 10 y 11, además
  `Orientation: landscape`) en vez de escribir 595 × 842. La carta del restaurante (06) es la única con
  un tamaño propio (420 × 700), que se sigue escribiendo con `Width` y `Height`.
