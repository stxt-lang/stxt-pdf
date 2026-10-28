# Ideas varias

## Accesibilidad (prioritario: es básica para lo que hacemos)

- ~~Styles en un documento aparte. Además, Typograpy de documento no puede ir, ahora debe ir en styles si es necesario~~ (hecho: raíz `Styles`, `Element: Document`; decisiones 226–231 de `docs/DECISIONES.md`). Queda: los errores posteriores a la construcción (fuente que no existe, desbordamientos) señalan la línea de una regla del documento aparte sin decir el fichero.
- `Sections` o `Media Sections`, y retomar que una section no vaya al índex o algo así, cosas que habían quedado apartadas de páginas
- ~~Rotación de los fixed y absolutos~~ (hito 30: `Rotate` y `Rotate origin` en `Relative`, `Absolute`, `Fixed` e `Image`)
- ~~Índice!!~~ (hito 22)
- Float? Algo más que haga falta?
- Referencia de cada elemento en páginas separadas + una referencia de elementos comunes de box, etc
- ~~Más pdf friendly: Tamaños de páginas estilo A4, Letter, etc. + rotación~~ (hito 30: `Paper` y `Orientation` en `Media`). Queda: que cada página o sección tenga su propio tamaño u orientación (una página apaisada dentro de un documento vertical).
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
- Al final de todo volver a versionar pdf's de ejemplos, para poner enlaces en readme y que se vean en github

- ~~**`Link` del PDF etiquetado con su contenido marcado**~~ (decisiones 252–257 de `docs/DECISIONES.md`: el
  `Link` envuelve lo que se pulsa, uno por enlace y fragmento, y lo que no es contenido tiene un `Link` suelto
  del `Document`). Los once ejemplos pasan PDF/UA-1 con veraPDF (`ExamplesTest`) salvo la regla 5-1. Queda:
  verificarlo con un lector de pantalla, y que el `Link` suelto de un `Fixed` (un pie con una web) se
  anuncia al final del documento, no en su sitio de lectura.
- **Declarar la conformidad PDF/UA** (regla 5-1 de veraPDF, la única que incumplen los ejemplos): el XMP
  llevaría `pdfuaid:part` = 1. Es afirmar algo, así que hay que decidir cuándo se hace: con el repaso de
  abajo hecho y los once ejemplos sin ningún fallo en `ExamplesTest.KNOWN_UA_FAILURES`.
- Hacer el repaso de accesibilidad del PDF etiquetado entero: texto alternativo de las figuras, orden de
  lectura con `Fixed` y `Absolute`, tablas con `Scope`, idioma por tramo, y probar con lectores de pantalla
  (veraPDF ya no encuentra nada en los ejemplos más allá de la 5-1, pero no comprueba que el orden o los
  textos tengan sentido).

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
- ~~**Imágenes vectoriales (SVG)**~~ (hito 31: `Image` admite SVG, vectorial en el PDF). Queda: el texto de
  un SVG se dibuja como contornos y no se puede seleccionar (JSVG no lo entrega como texto; haría falta
  una `Output` propia que implemente `TextOutput` y escriba el texto con `drawString` en el
  `PdfBoxGraphics2D`); que el SVG de depuración incrusta el SVG como `data:` y un navegador no tiene las
  fuentes de `fonts/`; que no hay `pattern`, `mask`, `filter`, `marker`, `image` ni CSS (`style`) en el
  subconjunto, y que un SVG no puede enlazar a otro del repositorio; y que las fuentes de AWT que da de
  alta `PdfImages` son globales a la JVM (dos carpetas de recursos con la misma familia y distinto TTF no
  se distinguen). Los PNG de los ejemplos que no son dibujos (fotos, la pantalla del móvil del manual, el
  icono del horario) se quedan como están.
- ~~**Rotación**~~ (hito 30). Queda: girar un `Text` o un `Block` directamente (hoy se gira el posicionado
  que lo contiene), `Rotate origin` con medidas (puntos o porcentajes), que una caja girada ocupe en el
  layout su rectángulo envolvente (hoy es solo pintado), y que un `Relative` girado se pueda partir.
