# Glosario

| Término | Definición |
|---|---|
| **Área útil** | Rectángulo de la página menos sus márgenes; el contenedor de referencia de los elementos de primer nivel de una sección y de los `Fixed`. Cada sección tiene la suya ([Document](../componentes/document.md#media)). |
| **Caja de borde / relleno / contenido / margen** | Los cuatro rectángulos concéntricos de una caja ([La caja](caja.md#las-cuatro-cajas)). |
| **Carpeta de recursos** | Carpeta de las fuentes y las imágenes; los recursos se buscan solo hacia abajo ([Recursos](recursos.md)). |
| **Columna** | Cada contenedor de fragmentos en que un `Multicol` reparte su contenido en una página ([Multicol](../componentes/multicol.md)). |
| **Contenedor de fragmentos** | Donde caen los fragmentos de las cajas: cada página y, dentro de un `Multicol`, cada columna ([Cortes](cortes.md#modelo)). |
| **Contenedor de referencia** | Rectángulo contra el que se resuelven los porcentajes, los `auto` y los desplazamientos de una caja ([La caja](caja.md#el-contenedor-de-referencia)). |
| **Contexto de apilamiento** | Grupo de cajas que se pinta como una unidad y ordena a sus hijos por `Layer` ([Capas y giro](capas-y-giro.md)). |
| **Contexto de formato de bloque** | Región que aísla el colapso de márgenes ([Block](../componentes/block.md#contextos-de-formato-de-bloque)). |
| **Definido** | Tamaño que se conoce sin hacer el layout del contenido ([Valores](valores.md#alto-definido)). |
| **Degradado** | Fondo que pasa de un color a otro a lo largo de una línea: `linear(ángulo, color, color…)` ([La caja](caja.md#degradado)). |
| **Elemento** | Un nodo del documento que es una caja: `Block`, `Flex`, `Grid`, `Text`, `Paragraph`, los encabezados, `List`, `Table`, `Multicol`, `Markdown`, `Contents`, `Image`, `Relative`, `Absolute` o `Fixed` ([Estructura](estructura.md#elementos)). |
| **Elemento en flujo** | Elemento que puede ser un ítem: todos menos `Absolute` y `Fixed`. |
| **Elemento generado** | Una caja que crea el motor y que el documento no escribe, pero que las reglas `Element` nombran: `Code block` y `Contents entry 1` … `6`. |
| **Elemento posicionado** | `Relative`, `Absolute` o `Fixed`. |
| **Enlace** | `Link` de una caja o `[texto](destino)` de Markdown: una zona del PDF que lleva a una URL o a otra caja ([Enlaces](enlaces.md)). |
| **Envoltorio** | `Item` o `Area`: no es una caja; añade al elemento que contiene cómo participa en su `Flex` o su `Grid`. |
| **Formato en línea** | Los tramos de un texto escritos en Markdown dentro de un `Text`, un `Paragraph` o una `Cell`, con `Inline: markdown` ([Formato del texto](formato-del-texto.md#formato-en-línea-inline)). |
| **Fragmento** | Parte de una caja que cae en un contenedor de fragmentos. |
| **Giro** (`Rotate`) | Una caja que se pinta girada: en flujo, sobre su centro y ocupando el rectángulo que la envuelve; posicionada, alrededor de su `Rotate origin` y sin que cambie el layout. Lo girado tiene que caber en la página ([Capas y giro](capas-y-giro.md#giro-rotate)). |
| **Huérfana** (`Orphans`) | Línea de un párrafo que queda sola al final de una página. |
| **Imagen** | Elemento `Image`: una caja sin hijos con un PNG, un JPEG, un SVG o una página de un PDF de la carpeta de recursos, su tamaño natural y su proporción ([Image](../componentes/image.md)). |
| **Índice** | Elemento `Contents`: una entrada por encabezado con su título, una guía y su página ([Contents](../componentes/contents.md)). |
| **Markdown** | Elemento cuyo contenido se escribe en un subconjunto de Markdown y se traduce a encabezados, párrafos, listas, tablas, bloques de código e imágenes ([Markdown](../componentes/markdown.md)). |
| **Min-content / max-content** | Tamaño mínimo sin desbordar / tamaño sin saltos de línea ([La caja](caja.md#tamaños-intrínsecos)). |
| **Papel** (`Paper`) | Tamaño de página con nombre (`A4`, `Letter`…), en vertical o, con `Orientation: landscape`, apaisado. |
| **Pista** | Columna o fila de un `Grid` o una `Table`. |
| **Posición estática** | Donde habría quedado una caja posicionada si estuviera en flujo ([posicionados](../componentes/relative-absolute-fixed.md#absolute)). |
| **Referencia** | `{page of: id}` o `{number of: id}` en un texto: la página donde empieza la caja con ese `Id`, o su número ([Referencias](referencias.md)). |
| **Sección** | Parte independiente del documento (`Section`): empieza una página y tiene su propio `Media`, sus `Fixed` y su numeración ([Document](../componentes/document.md#section)). |
| **Tanda** | Las páginas numeradas desde el último `Page numbering: restart`: `{pages}` es el último número de la suya. |
| **Texto preformateado** | Texto que conserva sus espacios y sus saltos de línea (`White space: pre` o `pre-wrap`). |
| **Tramo** | Parte del texto de una caja con su formato (negrita, cursiva, subrayado, tachado, enlace y, en el código, otra familia) ([Formato del texto](formato-del-texto.md#tramos)). |
| **Versalitas** | Minúsculas pintadas como mayúsculas más pequeñas (`Small caps`). |
| **Viuda** (`Widows`) | Línea de un párrafo que pasa sola a la página siguiente. |
