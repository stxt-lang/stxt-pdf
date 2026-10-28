# Ideas varias

## Accesibilidad (prioritario: es básica para lo que hacemos)

- Styles en un documento aparte. Además, Typograpy de documento no puede ir, ahora debe ir en styles si es necesario
- `Sections` o `Media Sections`, y retomar que una section no vaya al índex o algo así, cosas que habían quedado apartadas de páginas
- Rotación de los fixed y absolutos
- ~~Índice!!~~ (hito 22)
- Float? Algo más que haga falta?
- Referencia de cada elemento en páginas separadas + una referencia de elementos comunes de box, etc
- Más pdf friendly: Tamaños de páginas estilo A4, Letter, etc. + rotación
- Renombre de elementos para que no sea tan "css copy" y más "css inspired". Es como un repensado de css
- Revisar ThreadSafe
- Revisar rendimiento
- Como extender fácil? Valorar alternativas
- Httpservice para probar, que se pueda ejecutar en maven, etc. 
- Mirar como api permite a partir de stxt crear otro, y con cualquier página de esto usarlo dircto en pdfbox, para poder poner listeners, e integrar en otros lados
- Revisión código desde punto de vista de QA y Arquitectura: SOLID, DRY, KISS
- Quizá necesitamos un Preformated o Code Block para el código monoespaciado
- Un docker para esto
- Un manual hecho a partir de referencia :-)

- **`Link` del PDF etiquetado con su contenido marcado** (hito 23, decisión 171 de
  `docs/DECISIONES.md`). Hoy cada enlace es un elemento `Link` hijo del elemento de la caja que
  solo contiene la anotación (`OBJR`) y un `Alt` con su texto; no envuelve el texto marcado del
  tramo. Lo completo, y lo que pide PDF/UA, es que el `Link` contenga el contenido marcado de
  su texto además de la `OBJR`. Para ello hay que partir el contenido marcado de un `P` (o
  `Span`, `H1`…, `TD`) en varios `MCID`, uno por cada tramo con enlace, y colgar cada trozo del
  `Link` correspondiente. *Dónde:* `paint/StackingContext.paintText` (hoy un único
  `beginText`/`endText` por fragmento), `paint/PdfStructure.mark` y `link`. Verificar el
  resultado con un validador de PDF/UA (por ejemplo veraPDF) y, a ser posible, con un lector de
  pantalla.
- Hacer ese repaso de accesibilidad del PDF etiquetado entero cuando se termine esto: texto
  alternativo de las figuras, orden de lectura con `Fixed` y `Absolute`, tablas con `Scope`,
  idioma por tramo, y la validación PDF/UA de los once ejemplos.

## Huecos del motor detectados al escribir los ejemplos (hito 21)

- ~~**Texto preformateado**~~ (hito 29: `White space: pre` y `pre-wrap`, `Tab size`, y `Code font` en
  `Markdown`). Queda que un bloque de código de Markdown no se puede estilar aparte (es un `Text`, que
  alcanza `Element: Text` pero también el `Text` de cada encabezado): haría falta un elemento
  generado que las reglas `Element` nombren (`Code block`), como las entradas de un `Contents`; que el
  código en línea no está en un texto con `Inline: markdown` (no hay `Code font` fuera de `Markdown`, y
  podría ser una propiedad de `Typography`); que `justify` no se aplica a `pre-wrap`; y que el código
  en línea usa la altura de línea de la caja, no la de su fuente.
- ~~**Referencias a páginas e índice automático**~~ (hito 22: `{page of: id}`, `{number of: id}`
  y `Contents`).
- **Excluir un encabezado del índice**: un `Contents` lista todos los encabezados de sus niveles,
  así que la portada y el título del propio índice (ejemplos 04 y 09) son un `Text` en vez de un
  `Heading 1`, y pierden su marcador en el PDF. Algo como una propiedad `Contents: false` en el
  encabezado, o una clase de exclusión.
- **Numerar los encabezados** (`1`, `1.1`…): hoy `{number of: id}` solo da el número de un ítem de
  una lista numerada, así que «la sección 4.2» no se puede escribir de un encabezado.
- ~~**Primera página distinta / páginas con nombre**~~ (hito 24: `Pages` en `Fixed`). Queda: un
  margen distinto en la primera página (`First margin`: la portada a sangre sigue siendo un
  `Absolute` con `Offset` negativo y la primera hoja del contrato deja el mismo hueco de la
  cabecera), una numeración que no cuente la portada (`{page}` es la página física), páginas con
  nombre al estilo de `@page`, y forzar que una sección empiece en página impar con una página en
  blanco para imprimir a doble cara (`Break: Before: odd-page`).
- ~~**Enlaces** (a una URL o a un `Id` del documento) y anotaciones del PDF.~~ (hito 23: `Link` y
  `[texto](destino)` en Markdown). Queda: el `Link` del PDF etiquetado no envuelve el contenido
  marcado del tramo (decisión 171), y un encabezado de un `Markdown` no se puede enlazar a mano
  porque no tiene `Id`.
- ~~**Formato dentro de la línea fuera de Markdown**~~ (hito 26: `Inline: markdown` en
  `Typography`). Queda que un texto con `Inline: markdown` no admite listas ni encabezados (van en
  un `Markdown`) y que hay que escapar el primer carácter si empieza como una (`-`, `#`, `1.`).
- ~~**Estilo de las tablas**~~ (hito 25: `Head cell`, `Body cell`, `Foot cell`, `Odd row cell`,
  `Even row cell`, `For column` y `Column align`). Queda que una fila no tiene `Box` y que no se
  puede dar estilo a una fila concreta sin tocar cada una de sus celdas (el pie de totales de la
  factura sigue necesitando `Class: total` en las dos celdas de la fila).
- ~~**Aspecto de las cajas**~~ (hito 28: `Radius`, `Border color` por lado, `Background` con `linear(…)` y
  colores con opacidad). Queda: **sombras** (`Shadow`, descartadas en el hito: PDFBox no desenfoca y habría que
  aproximar con capas), degradados con paradas con posición, radiales o repetidos, y dos radios por esquina
  (`10 / 20`: la elipse se pide con %). Un recorte con `Radius` no se aplica a los hijos que no son una
  `Image` (no hay `overflow`: un hijo con fondo en la esquina de una caja redondeada se sale de la curva, y
  hay que redondear también al hijo, como las fotos de las fichas del catálogo). Los radios en % de una caja
  partida se calculan sobre cada fragmento.
- ~~**Texto**: interletraje, versalitas y `text-transform`~~ (hito 27: `Transform`, `Letter spacing` y
  `Small caps`). Queda que las versalitas son sintetizadas (mayúsculas al 80 %, no la característica
  `smcp` de la fuente) y que el texto con interletraje grande puede extraerse del PDF con espacios
  entre las letras.
- **Imágenes vectoriales (SVG)**: los gráficos de los ejemplos son PNG generados; con SVG
  quedarían nítidos a cualquier tamaño.
- **Rotación**: nada se puede girar, ni un sello ni un texto vertical.
