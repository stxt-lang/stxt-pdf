# Glosario

| Término | Definición |
|---|---|
| **Ancho más estrecho / ancho natural** | El menor ancho sin que nada se salga / el ancho sin ningún salto de línea ([La caja](box.md#tamaños-intrínsecos)). |
| **Área útil** | Rectángulo de la página menos sus márgenes. Es el contenedor de referencia de los elementos de primer nivel de una sección y de los `Page overlay`. Cada sección tiene la suya ([Document](../components/document.md#page)). |
| **Caja de borde / relleno / contenido / margen** | Los cuatro rectángulos concéntricos de una caja ([La caja](box.md#las-cuatro-cajas)). |
| **Caja lateral** (`Side`) | `Image`, `Barcode` o `Block` colocado a un lado de su contenedor, con el texto de los hermanos que la siguen corriendo a su lado ([Block](../components/block.md#cajas-laterales-side)). |
| **Caja que aísla los márgenes** | Caja de la que no salen los márgenes de sus hijos: corta la fusión de márgenes ([Block](../components/block.md#cajas-que-aíslan-los-márgenes)). |
| **Carpeta de recursos** | Carpeta de las fuentes y las imágenes. Los recursos se buscan solo hacia abajo ([Recursos](resources.md)). |
| **Columna** | Cada contenedor de fragmentos en que un `Columns` reparte su contenido en una página ([Columns](../components/columns.md)). |
| **Contenedor de fragmentos** | Donde caen los fragmentos de las cajas: cada página y, dentro de un `Columns`, cada columna ([Cortes](breaks.md#modelo)). |
| **Contenedor de referencia** | Rectángulo contra el que se resuelven los porcentajes, los `auto` y los desplazamientos de una caja ([La caja](box.md#el-contenedor-de-referencia)). |
| **Contexto de apilamiento** | Grupo de cajas que se pinta como una unidad y ordena a sus hijos por `Layer` ([Capas y giro](layers-and-rotation.md)). |
| **Definido** | Tamaño que se conoce sin hacer el layout del contenido ([Valores](values.md#alto-definido)). |
| **Degradado** | Fondo que pasa de un color a otro a lo largo de una línea: `linear(ángulo, color, color, ...)` ([La caja](box.md#degradado)). |
| **Elemento** | Un nodo del documento que es una caja: `Block`, `Stack`, `Grid`, `Text`, `Paragraph`, los encabezados, `List`, `Table`, `Columns`, `Markdown`, `Table of contents`, `Image`, `Barcode`, `Figure`, `Frame`, `Overlay` o `Page overlay` ([Estructura](structure.md#elementos)). |
| **Elemento en flujo** | Elemento que puede ser un ítem: todos menos `Overlay` y `Page overlay`. |
| **Elemento generado** | Una caja que crea el motor y que el documento no escribe, pero que las reglas `For element` nombran: `Code block` y `TOC 1` a `TOC 6`. |
| **Elemento posicionado** | `Frame`, `Overlay` o `Page overlay`. |
| **Enlace** | `Link` de una caja o `[texto](destino)` de Markdown: una zona del PDF que lleva a una URL o a otra caja ([Enlaces](links.md)). |
| **Envoltorio** | `Item` o `Area`. No es una caja: añade al elemento que contiene cómo participa en su `Stack` o su `Grid`. |
| **Figura** (`Figure`) | Elemento con exactamente un `Image`, `Barcode`, `Table` o `Block` y su pie (`Caption`), numerada con el contador de las figuras o el de las tablas ([Figure](../components/figure.md)). |
| **Formato en línea** | Los tramos de un texto escritos en Markdown dentro de un `Text`, un `Paragraph`, un `Caption` o una `Cell`, con `Markup: markdown` ([Formato del texto](text-formatting.md#formato-en-línea-markup)). |
| **Fragmento** | Parte de una caja que cae en un contenedor de fragmentos. |
| **Giro** (`Rotate`) | Una caja que se pinta girada: en flujo, sobre su centro y ocupando el rectángulo que la envuelve, y posicionada, alrededor de su `Rotate origin` y sin que cambie el layout. Lo girado tiene que caber en la página ([Capas y giro](layers-and-rotation.md#giro-rotate)). |
| **Huérfana** (`Orphans`) | Línea de un párrafo que queda sola al final de una página. |
| **Imagen** | Elemento `Image`: una caja sin hijos con un PNG, un JPEG, un SVG o una página de un PDF de la carpeta de recursos, su tamaño natural y su proporción ([Image](../components/image.md)). |
| **Índice** | Elemento `Table of contents`: una entrada por encabezado (o, con `Entries`, por figura o por tabla) con su título, una guía y su página ([Table of contents](../components/table-of-contents.md)). |
| **Letra capitular** (`Drop cap`) | Primera letra de un `Paragraph` compuesta en grande, ocupando sus primeras líneas, que empiezan a su derecha ([Text](../components/text.md#letra-capitular-drop-cap)). |
| **Markdown** | Elemento cuyo contenido se escribe en un subconjunto de Markdown y se traduce a encabezados, párrafos, listas, tablas, bloques de código e imágenes ([Markdown](../components/markdown.md)). |
| **Papel** (`Paper`) | Tamaño de página con nombre (`A4` o `Letter`), en vertical o, con `Orientation: landscape`, apaisado. |
| **Pie** (`Caption`) | El texto de una `Figure`, debajo de su contenido o encima de una tabla, con el número de la figura delante ([Figure](../components/figure.md#el-pie-caption)). |
| **Pista** | Columna o fila de un `Grid` o una `Table`. |
| **Posición estática** | Donde habría quedado una caja posicionada si estuviera en flujo ([posicionados](../components/frame-and-overlays.md#overlay)). |
| **Referencia** | `{page of: id}` o `{number of: id}` en un texto: la página donde empieza la caja con ese `Id`, o su número (el de un ítem, un encabezado o una figura) ([Referencias](references.md)). |
| **Sección** | Parte independiente del documento (`Section`): empieza una página y tiene su propio `Page`, sus `Page overlay` y su numeración ([Document](../components/document.md#section)). |
| **Tanda** | Las páginas numeradas desde el último `Page numbering: restart`: `{pages}` es el último número de la suya. |
| **Texto preformateado** | Texto que conserva sus espacios y sus saltos de línea (`Preformatted: keep` o `wrap`). |
| **Tramo** | Parte del texto de una caja con su formato (negrita, cursiva, subrayado, tachado, enlace, en el código otra familia y, en un tramo con estilo, lo que declara su regla `For style`) ([Formato del texto](text-formatting.md#tramos)). |
| **Versalitas** | Minúsculas pintadas como mayúsculas más pequeñas (`Small caps`). |
| **Viuda** (`Widows`) | Línea de un párrafo que pasa sola a la página siguiente. |
