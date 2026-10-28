# Ejemplos

Documentos completos, como los que escribiría un autor, para ver lo que da el motor con las
fuentes y las imágenes reales de [`resources/`](../../resources) y **sin contornos de
depuración**. A diferencia de los casos [golden](../golden), que comprueban un algoritmo con un
resultado calculado a mano, un ejemplo es un documento con sentido, con su contenido y su estilo.

Cada carpeta tiene:

| Fichero | Qué es |
|---|---|
| `input.stxt` | El documento. Valida con el schema y se maqueta sin errores. |
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
terceros (`java tools/ExampleImages.java`). Las fuentes son IBM Plex Sans, Serif y Mono (SIL
Open Font License). Los nombres, las empresas y los datos de los documentos son inventados.

## Los once documentos

| | Documento | Qué enseña |
|---|---|---|
| [![](01-invoice/page-1.png)](01-invoice) | **[01 · Factura](01-invoice)**<br>Una página A4 | Logotipo y datos de las partes en un grid, tira de condiciones, tabla con cabecera, cuerpo y **pie de totales**, pie de página fijo con `{page}` y `{pages}`, **correos clicables** (`Link: mailto:…`). Todo el estilo en `Styles`. |
| [![](02-letter/page-1.png)](02-letter) | **[02 · Carta comercial](02-letter)**<br>Una página A4 | Membrete con logotipo, cuerpo en **Markdown** justificado con **Knuth-Plass** (`Text wrap: pretty`), negrita, cursiva y subrayado dentro de la línea, firma escaneada, web del taller en el pie y en el membrete con `Link`. |
| [![](03-resume/page-1.png)](03-resume) | **[03 · Currículum](03-resume)**<br>Una página A4, sin márgenes | Grid de dos columnas a **página completa**, foto, barras de nivel con anchos en porcentaje, línea de tiempo en un grid y cifras en un `Flex`; correo y web clicables. |
| [![](04-annual-report/page-1.png)](04-annual-report) | **[04 · Informe anual](04-annual-report)**<br>7 páginas A4 | **Portada a sangre** (un `Absolute` que sale al margen, con el pie fijo solo desde la segunda página: `Pages: not-first`), **índice automático** (`Contents`, con puntos guía, las páginas calculadas por el motor y cada entrada clicable), cifras clave, **gráficos** como imágenes, texto en **dos columnas** con filete, tabla financiera con cabecera y pie, calendario con celdas de varias columnas (`Span`) y secciones que empiezan en página nueva. |
| [![](05-newsletter/page-1.png)](05-newsletter) | **[05 · Boletín](05-newsletter)**<br>2 páginas A4 | Aire de periódico: cabecera de portada, foto recortada con `Fit: cover`, artículos a **tres columnas** con cortes equilibrados, foto al lado del texto, agenda en tabla y anuncios en una rejilla, y la web de la gaceta en el pie. |
| [![](06-menu/page-1.png)](06-menu) | **[06 · Carta de restaurante](06-menu)**<br>2 páginas, 420 × 700 | **Tamaño de página propio**, fondo y marco doble con `Fixed` de **`Layer` negativa** (detrás del contenido en todas las páginas), platos con precio en un `Flex`, adornos e imagen centrada. |
| [![](07-contract/page-1.png)](07-contract) | **[07 · Contrato](07-contract)**<br>3 páginas A4 | Cabecera y pie fijos con rúbricas, `Reunidos` y `Exponen` con numeración romana, doce **cláusulas con tres niveles de numeración** escritas en Markdown, anexo con tabla de honorarios y **bloque de firmas que no se parte**. |
| [![](08-manual/page-1.png)](08-manual) | **[08 · Manual](08-manual)**<br>4 páginas A4 | Figura con **números superpuestos** (un `Relative` con `Absolute` encima), leyenda al lado, avisos con color, pasos numerados, esquema de conexión, **bloque de terminal** en monoespaciada, tablas de especificaciones y de averías, que remite al esquema con **`{page of: id}`** (también dentro de Markdown), con un **enlace de Markdown a la tabla de especificaciones** (`[…](#specs)`) y correos y web clicables, y marcadores en el PDF. |
| [![](09-catalog/page-1.png)](09-catalog) | **[09 · Catálogo](09-catalog)**<br>5 páginas A4 | Portada a sangre, **índice de productos** (un `Contents` de dos niveles con su sangría, su página y cada entrada clicable), **fichas de producto** en una rejilla de dos columnas con foto recortada y etiqueta «Novedad» superpuesta, tabla de maderas y página de pedidos. |
| [![](10-certificate/page-1.png)](10-certificate) | **[10 · Certificado](10-certificate)**<br>Una página A4 apaisada | Fondo y **tres marcos concéntricos** con adornos en las esquinas, nombre como protagonista, dos firmas y un sello con cintas, todo centrado, con la dirección de verificación clicable en el pie. |
| [![](11-timetable/page-1.png)](11-timetable) | **[11 · Horario](11-timetable)**<br>Una página A4 apaisada | Tabla con las celdas separadas por un `Gap`, **colores por clase**, recreo y comedor que ocupan los cinco días con `Span`, leyenda en un `Flex` con salto de línea y notas en Markdown. |

## Lo que se aprende al escribirlos

Qué se resolvió bien con lo que hay y qué se echa en falta (apuntado en [`TODO.md`](../../TODO.md)):

- **Una portada sin cabecera ni pie**: el `Fixed` del pie lleva `Pages: not-first`, y la portada es
  un `Absolute` del tamaño de la página que ya no necesita `Layer` para tapar nada (ejemplos 04 y
  09). Lo que sigue siendo un truco es el bloque de 1 pt con `Break: After: page` que fuerza el
  salto detrás de ella, porque un `Absolute` no ocupa sitio, y que el margen es el de todas las
  páginas: una portada a sangre sale al margen con un `Offset` negativo. `Pages` también quita el
  logotipo de la primera hoja del contrato (07), el pie de la primera página de la carta (06) y
  pone un titulillo corrido desde la segunda página del boletín (05).
- **El texto con formato** se escribe con `Inline: markdown` (una regla `Element: Paragraph` o el
  `Typography` de un `Text`: se hereda) para una negrita o una cursiva dentro de una frase, y con
  `Markdown` cuando hay además listas, encabezados o tablas. Sin `Inline` los asteriscos son
  literales. Las cláusulas del contrato (07) y los pasos del manual (08) siguen siendo `Markdown`
  porque son listas; las partes del contrato, la leyenda de la figura (08) y la carta de la
  presidenta (04) ya son `Paragraph`.
- **Los espacios se colapsan**: no hay texto preformateado, así que un bloque de terminal con
  columnas alineadas (ejemplo 08) usa un grid.
- **Las tablas se estilan con reglas**, no con una clase en cada celda: `Element: Head cell` da la
  cabecera (también a las tablas de Markdown), `Even row cell` la cebra (ejemplo 05) y
  `Column align` o `For column` las columnas numéricas. Queda una clase para lo que es de una sola
  fila, como la de los totales de la factura.
- **Los enlaces** son una propiedad de la caja entera (`Link`) o un tramo de Markdown
  (`[texto](destino)`). Un `Text` con un teléfono y una web (ejemplos 02 y 09) es clicable entero,
  también el teléfono: con `Inline: markdown` se podría enlazar solo una parte (`[web](https://…)`),
  y los ejemplos 02 y 09 aún no lo usan.
- **El índice** lo da `Contents`, sus entradas son clicables, y las páginas, `{page of: id}`. Un `Contents` lista todos los
  encabezados de sus niveles, así que la portada y el título del propio índice son un `Text`, no
  un encabezado, para que no salgan en él (no hay forma de excluir uno).
