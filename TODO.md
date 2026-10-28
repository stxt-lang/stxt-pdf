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
- ~~Quizá necesitamos un Preformated o Code Block para el código monoespaciado~~ (hitos 29 y 34: `White space: pre` y el elemento generado `Code block`)
- Un docker para esto
- Un manual hecho a partir de referencia :-)
- Al final de todo volver a versionar pdf's de ejemplos, para poner enlaces en readme y que se vean en github

- ~~**`Link` del PDF etiquetado con su contenido marcado**~~ (decisiones 252–257 de `docs/DECISIONES.md`: el
  `Link` envuelve lo que se pulsa, uno por enlace y fragmento, y lo que no es contenido tiene un `Link` suelto
  del `Document`). Los once ejemplos pasan PDF/UA-1 con veraPDF (`ExamplesTest`) salvo la regla 5-1. Queda:
  verificarlo con un lector de pantalla, y que el `Link` suelto de un `Fixed` (un pie con una web) se
  anuncia al final del documento, no en su sitio de lectura.
- ~~Repaso de accesibilidad del PDF etiquetado~~ (decisiones 258–260: `Text` como `P`, `Language` por caja con
  su `Lang` y un espacio al final de las líneas partidas; leído con un lector de pantalla simulado sobre los
  once ejemplos) y declaración de PDF/UA-1 (`pdfuaid:part`, decisión 261) en los documentos con `Title`: los once
  ejemplos pasan veraPDF sin ninguna regla conocida. Cabeceras de fila (`Header: true` en la `Cell`, decisión 263) y títulos reales de portada e índice (`In contents: false`, decisión 264) están hechos. Queda lo que se descartó o no se hizo:
  - **`Alt` de las imágenes:** hoy una `Image` sin `Alt` es decorativa en silencio (decisión 136, hito 18).
    Se ha decidido dejarlo; si un olvido del autor resulta ser un problema, hacer `Alt` obligatorio con `Alt:`
    vacío para las decorativas.
  - **Idioma por tramo** dentro de una línea (una palabra en inglés dentro de un párrafo español): hoy el
    idioma es de la caja.
  - **Contraste** de color entre el texto y su fondo (WCAG): el motor conoce el `Color` y el `Background`
    de las cajas, pero no lo que hay detrás de una imagen.
  - **PDF/UA-2 y PDF 2.0:** `TOCI` con `Reference` y `Ref`, destinos de estructura, `ListNumbering` distinto
    de `None` en las listas con guion, espacio de nombres de PDF 2.0. Ninguno es PDF/UA-1.
  - Probar con lectores de pantalla reales (NVDA, JAWS, VoiceOver).

## Huecos del motor detectados al escribir los ejemplos (hito 21)

- ~~**Texto preformateado**~~ (hito 29: `White space: pre` y `pre-wrap`, `Tab size`, y `Code font` en
  `Markdown`). El bloque de código de Markdown ya se estila aparte (hito 34: elemento generado `Code
  block`, decisión 285). Queda que el
  código en línea no está en un texto con `Inline: markdown` (no hay `Code font` fuera de `Markdown`, y
  podría ser una propiedad de `Typography`); que `justify` no se aplica a `pre-wrap`; y que el código
  en línea usa la altura de línea de la caja, no la de su fuente.
- ~~**Referencias a páginas e índice automático**~~ (hito 22: `{page of: id}`, `{number of: id}`
  y `Contents`).
- ~~**Excluir un encabezado del índice**~~ (`In contents: false`, decisión 264: la portada y el título del índice de
  los ejemplos 04 y 09 son ya `Heading 1`).
- ~~**Numerar los encabezados** (`1`, `1.1`…)~~ (hito 33: `Numbered` en `Typography`, decisiones 280–284). Queda: formatos por nivel
  (romanos, letras para los anexos), prefijos y sufijos propios (`Anexo A.`), número colgado a la izquierda
  (hoy va dentro del texto) y referir los encabezados de un `Markdown`.
- ~~**Primera página distinta / páginas con nombre**~~ (hito 24: `Pages` en `Fixed`; hito 32: `Section`, que
  da la portada con otro margen, la numeración que no la cuenta y el tamaño o la orientación por sección).
  Queda: empezar un `Section` en página impar con una página en blanco para imprimir a doble cara
  (`Start: odd`; la página en blanco tendría que numerarse o no), un número inicial de la numeración
  (`Start: 5`, con `Page numbering: restart`), otros formatos de número (romanos en el prefacio), un
  `Background` de página en la sección (hoy la portada a sangre pinta un bloque del tamaño de la página),
  y una página apaisada que no sea una sección entera.
- ~~**Enlaces** (a una URL o a un `Id` del documento) y anotaciones del PDF.~~ (hito 23: `Link` y
  `[texto](destino)` en Markdown). Queda: el `Link` del PDF etiquetado no envuelve el contenido
  marcado del tramo (decisión 171), y un encabezado de un `Markdown` no se puede enlazar a mano
  porque no tiene `Id`.
- ~~**Formato dentro de la línea fuera de Markdown**~~ (hito 26: `Inline: markdown` en
  `Typography`). Queda que un texto con `Inline: markdown` no admite listas ni encabezados (van en
  un `Markdown`) y que hay que escapar el primer carácter si empieza como una (`-`, `#`, `1.`).
- ~~**Estilo de las tablas**~~ (hito 25: `Head cell`, `Body cell`, `Foot cell`, `Odd row cell`,
  `Even row cell`, `For column` y `Column align`). Una fila concreta se estila con `Class` en la `Row`
  (hito 35: sus reglas `For class` se aplican a cada celda; el pie de totales de la factura lo usa).
  Queda que una fila no tiene `Box` (un borde o un fondo de fila es el de cada celda: un `Border` se repite
  entre celdas) ni `Id` (no se puede enlazar a una fila ni darle estilo con `For id`).
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
