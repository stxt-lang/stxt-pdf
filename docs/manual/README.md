# Manual de stxt-pdf

stxt-pdf convierte un documento [STXT](https://stxt.dev) en un PDF. El documento describe qué hay en cada página y cómo se
ve. El motor calcula dónde va cada cosa, reparte el contenido en páginas y escribe el PDF con las fuentes incrustadas,
etiquetado para la accesibilidad y con marcadores.

Para seguir este manual basta con saber escribir STXT y qué es un PDF. El detalle exacto de cada regla está en la
[referencia](../referencia/README.md).

## Cómo se lee

Primero, [primeros pasos](comun/primeros-pasos.md), que escribe un documento de principio a fin, y
[conceptos](comun/conceptos.md), con las ideas que usa el resto del manual. Después, cada página se lee sola:

- **Comportamiento común**: lo que comparten todos los elementos (medidas, márgenes, texto, cortes de página, estilos).
- **Componentes**: cada elemento, con un documento completo que se puede copiar y convertir.

## Comportamiento común

| Página | Qué explica |
|---|---|
| [Primeros pasos](comun/primeros-pasos.md) | Un documento completo y cómo se convierte en PDF |
| [Conceptos](comun/conceptos.md) | Páginas, cajas, flujo, secciones y por qué el motor es estricto |
| [Valores](comun/valores.md) | Cómo se escriben las medidas, los porcentajes, los colores y los degradados |
| [La caja](comun/caja.md) | Tamaño, márgenes, relleno, borde, fondo y esquinas de cualquier elemento (`Box`) |
| [Tipografía](comun/tipografia.md) | Fuente, tamaño, interlineado, alineación, color e idioma del texto (`Typography`) |
| [Formato del texto](comun/formato-del-texto.md) | Negrita y cursiva dentro de una frase, mayúsculas, versalitas, interletraje, ligaduras y texto preformateado |
| [Páginas y cortes](comun/paginas-y-cortes.md) | Cómo se reparte el contenido en páginas, cómo controlar los cortes (`Break`) y los números de página |
| [Capas y giro](comun/capas-y-giro.md) | Qué se pinta encima de qué y cómo girar una caja |
| [Estilos](comun/estilos.md) | Reglas reutilizables (`Styles`) y estilos en un fichero aparte |
| [Referencias y enlaces](comun/referencias-y-enlaces.md) | «Véase la página 12», números de apartado y enlaces clicables |
| [Recursos](comun/recursos.md) | Dónde van las fuentes y las imágenes |
| [Errores](comun/errores.md) | Cómo leer un error y los más habituales |
| [Uso desde Java](comun/uso-desde-java.md) | Convertir documentos desde un programa |

## Componentes

| Página | Para qué sirve |
|---|---|
| [`Document` y `Section`](componentes/document.md) | El documento, el tamaño de página, las secciones y la numeración de las páginas |
| [`Block`](componentes/block.md) | Agrupar elementos uno debajo de otro |
| [`Text` y `Paragraph`](componentes/text.md) | Texto y párrafos |
| [Encabezados](componentes/heading.md) | Títulos de capítulo y apartado, numerados o no |
| [`List`](componentes/list.md) | Listas con viñetas o numeradas |
| [`Table`](componentes/table.md) | Tablas con cabecera y pie que se repiten en cada página |
| [`Stack`](componentes/stack.md) | Elementos en fila o en columna que se reparten el espacio |
| [`Grid`](componentes/grid.md) | Una rejilla de filas y columnas |
| [`Columns`](componentes/columns.md) | Texto en varias columnas, como en una revista |
| [`Frame`, `Overlay` y `Page overlay`](componentes/frame-and-overlays.md) | Desplazar una caja, colocarla en un sitio exacto o repetirla en cada página |
| [`Image`](componentes/image.md) | Imágenes PNG, JPEG y SVG, y páginas de un PDF |
| [`Markdown`](componentes/markdown.md) | Escribir contenido largo en Markdown |
| [`Table of contents`](componentes/table-of-contents.md) | El índice automático |

Los [ejemplos](../../test/examples/README.md) son once documentos reales con su PDF, de una factura a un informe anual.
