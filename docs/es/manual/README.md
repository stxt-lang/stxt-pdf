# Manual de stxt-pdf

stxt-pdf convierte un documento [STXT](https://stxt.dev) en un PDF. El documento describe qué hay en cada página y cómo se
ve. El motor calcula dónde va cada cosa, reparte el contenido en páginas y escribe el PDF con las fuentes incrustadas,
etiquetado para la accesibilidad y con marcadores.

Para seguir este manual basta con saber escribir STXT y qué es un PDF. El detalle exacto de cada regla está en la
[referencia](../reference/README.md).

## Cómo se lee

Primero, [primeros pasos](common/getting-started.md), que escribe un documento de principio a fin, y
[conceptos](common/concepts.md), con las ideas que usa el resto del manual. Después, cada página se lee sola:

- **Comportamiento común**: lo que comparten todos los elementos (medidas, márgenes, texto, cortes de página, estilos).
- **Componentes**: cada elemento, con un documento completo que se puede copiar y convertir.

## Comportamiento común

| Página | Qué explica |
|---|---|
| [Primeros pasos](common/getting-started.md) | Un documento completo y cómo se convierte en PDF |
| [Conceptos](common/concepts.md) | Páginas, cajas, flujo, secciones y por qué el motor es estricto |
| [Valores](common/values.md) | Cómo se escriben las medidas, los porcentajes, los colores y los degradados |
| [La caja](common/box.md) | Tamaño, márgenes, relleno, borde, fondo y esquinas de cualquier elemento (`Box`) |
| [Tipografía](common/typography.md) | Fuente, tamaño, interlineado, alineación, color e idioma del texto (`Typography`) |
| [Formato del texto](common/text-formatting.md) | Negrita y cursiva dentro de una frase, mayúsculas, versalitas, interletraje, ligaduras y texto preformateado |
| [Páginas y cortes](common/pages-and-breaks.md) | Cómo se reparte el contenido en páginas, cómo controlar los cortes (`Break`) y los números de página |
| [Capas y giro](common/layers-and-rotation.md) | Qué se pinta encima de qué y cómo girar una caja |
| [Estilos](common/styles.md) | Reglas reutilizables (`Styles`) y estilos en un fichero aparte |
| [Referencias y enlaces](common/references-and-links.md) | «Véase la página 12», números de apartado y enlaces clicables |
| [Recursos](common/resources.md) | Dónde van las fuentes y las imágenes |
| [Errores](common/errors.md) | Cómo leer un error y los más habituales |
| [Uso desde Java](common/java-usage.md) | Convertir documentos desde un programa |

## Componentes

| Página | Para qué sirve |
|---|---|
| [`Document` y `Section`](components/document.md) | El documento, el tamaño de página, las secciones y la numeración de las páginas |
| [`Block`](components/block.md) | Agrupar elementos uno debajo de otro |
| [`Text` y `Paragraph`](components/text.md) | Texto y párrafos |
| [Encabezados](components/heading.md) | Títulos de capítulo y apartado, numerados o no |
| [`List`](components/list.md) | Listas con viñetas o numeradas |
| [`Table`](components/table.md) | Tablas con cabecera y pie que se repiten en cada página |
| [`Stack`](components/stack.md) | Elementos en fila o en columna que se reparten el espacio |
| [`Grid`](components/grid.md) | Una rejilla de filas y columnas |
| [`Columns`](components/columns.md) | Texto en varias columnas, como en una revista |
| [`Frame`, `Overlay` y `Page overlay`](components/frame-and-overlays.md) | Desplazar una caja, colocarla en un sitio exacto o repetirla en cada página |
| [`Image`](components/image.md) | Imágenes PNG, JPEG y SVG, y páginas de un PDF |
| [`Markdown`](components/markdown.md) | Escribir contenido largo en Markdown |
| [`Table of contents`](components/table-of-contents.md) | El índice automático |

Los [ejemplos](../../../test/examples/README.md) son once documentos reales con su PDF, de una factura a un informe anual.
