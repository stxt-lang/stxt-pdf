# Manual de stxt-pdf

stxt-pdf convierte un documento escrito en [STXT](https://stxt.dev) en un PDF: tú describes qué hay en cada página y cómo
se ve, y el motor calcula dónde va cada cosa, reparte el contenido en páginas y escribe el PDF con las fuentes
incrustadas, accesible y con marcadores.

Para seguir este manual basta con saber escribir STXT y qué es un PDF. Todo lo demás se explica aquí. Si algo no
queda claro o necesitas el detalle exacto de una regla, la [referencia](../referencia/README.md) lo tiene todo; en
principio no debería hacerte falta.

## Cómo se lee

Empieza por [primeros pasos](comun/primeros-pasos.md), que escribe un documento de principio a fin, y por
[conceptos](comun/conceptos.md), que explica las ideas que usa el resto del manual. Después, cada página se puede leer
sola: las de **comportamiento común** explican lo que comparten todos los elementos (medidas, márgenes, texto, cortes de
página, estilos), y las de **componentes**, cada elemento.

Cada página de componente tiene un documento completo que puedes copiar, convertir y modificar.

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
| [`Flex`](componentes/flex.md) | Elementos en fila o en columna que se reparten el espacio |
| [`Grid`](componentes/grid.md) | Una rejilla de filas y columnas |
| [`Columns`](componentes/columns.md) | Texto en varias columnas, como en una revista |
| [`Relative`, `Absolute` y `Fixed`](componentes/relative-absolute-fixed.md) | Desplazar una caja, colocarla en un sitio exacto o repetirla en cada página |
| [`Image`](componentes/image.md) | Imágenes PNG, JPEG y SVG |
| [`Markdown`](componentes/markdown.md) | Escribir contenido largo en Markdown |
| [`Table of contents`](componentes/table-of-contents.md) | El índice automático |

Los [ejemplos](../../test/examples/README.md) son once documentos reales (una factura, un currículum, un informe anual…)
con su PDF.
