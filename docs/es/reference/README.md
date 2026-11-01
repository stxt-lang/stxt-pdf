# Referencia de stxt-pdf

**Versión del modelo:** 0.1 · **Namespace:** `dev.stxt.pdf` · **Unidades:** puntos tipográficos (pt) y milímetros (mm)

La referencia es la **fuente de verdad** del comportamiento del motor: dice qué admite cada elemento, qué significa
cada propiedad, cómo calcula el motor la posición y el tamaño de cada caja, qué es un error y qué se escribe en el PDF.
Si el [manual](../manual/README.md) y la referencia dicen cosas distintas, manda la referencia.

Cada vez que el motor ha tenido que elegir entre dos interpretaciones razonables, la elección está escrita en el sitio
al que afecta, en un bloque así:

> **Decisión:** lo que se decidió y lo que implica. *Alternativa descartada:* lo que se podría haber hecho en su lugar.

El schema STXT del modelo es [`src/main/resources/stxt/dev.stxt.pdf.stxt`](../../../src/main/resources/stxt/dev.stxt.pdf.stxt).
Si difiere del resumen de [la estructura](common/structure.md#schema), manda el fichero.

## Comportamiento común

| Página | Contenido |
|---|---|
| [Estructura de un documento](common/structure.md) | Convenciones, elementos y envoltorios, el árbol de cajas, quién coloca a quién y el resumen del schema |
| [Valores](common/values.md) | Números, porcentajes, fracciones, abreviados de cuatro lados, colores y las bases de los porcentajes (alto definido) |
| [La caja: `Box`](common/box.md) | Las cuatro cajas, el contenedor de referencia, el grupo `Box`, el dimensionado (intrínsecos, mínimos y máximos, proporción) y el aspecto (fondo, borde, esquinas) |
| [Tipografía: `Typography`](common/typography.md) | La herencia, la fuente, el interlineado y la línea base, el reparto en líneas, la alineación y el idioma |
| [Formato del texto](common/text-formatting.md) | Tramos con formato, `Markup: markdown`, mayúsculas, versalitas, interletraje, ligaduras, texto preformateado, subrayado y tachado |
| [Páginas y cortes: `Break`](common/breaks.md) | La fragmentación: oportunidades y elección del corte, viudas y huérfanas, cortes forzados, bordes en los cortes, `{page}` y `{pages}` |
| [Capas y giro](common/layers-and-rotation.md) | Los contextos de apilamiento, `Layer`, el orden de pintado y `Rotate` |
| [Reglas reutilizables: `Styles`](common/styles.md) | `For element`, `For style`, `For id` y `For column`, la prioridad y el documento de estilos aparte |
| [Referencias](common/references.md) | `{page of: id}` y `{number of: id}`, y cómo converge la paginación |
| [Enlaces](common/links.md) | `Link` y los enlaces de Markdown: destinos, zonas y PDF etiquetado |
| [Recursos y fuentes](common/resources.md) | La carpeta de recursos, las rutas seguras y cómo se identifican y se comprueban las fuentes |
| [El PDF](common/pdf.md) | Lo que lleva todo PDF: fuentes incrustadas, accesibilidad, contraste, marcadores, título y PDF/UA-1 o PDF/UA-2; PDF/A y ficheros adjuntos (Factur-X) si se piden |
| [Desbordamiento](common/overflow.md) | Nada puede desbordar: qué se comprueba y a quién se señala; `Overflow: shrink` y `clip`, caja a caja |
| [Errores](common/errors.md) | La política de errores, los niveles de validación, los códigos y sus tipos |
| [Límites](common/limits.md) | Páginas, píxeles y complejidad de los SVG que admite una conversión |
| [Ejemplo completo](common/complete-example.md) | Un documento con el cálculo de cada caja hecho a mano |
| [Glosario](common/glossary.md) | Los términos de la referencia |

## Componentes

| Página | Elementos |
|---|---|
| [`Document`](components/document.md) | `Document`, `Page`, `Title`, `Language` y `Section` |
| [`Block`](components/block.md) | `Block` y el flujo de bloque, que comparten todos los bloques |
| [`Text`](components/text.md) | `Text` y `Paragraph` |
| [Encabezados](components/heading.md) | `Heading 1` a `Heading 6` y su numeración |
| [`List`](components/list.md) | `List` y `List item` |
| [`Table`](components/table.md) | `Table`, `Header rows`, `Body`, `Footer rows`, `Row` y `Cell` |
| [`Stack`](components/stack.md) | `Stack` e `Item` |
| [`Grid`](components/grid.md) | `Grid` y `Area` |
| [`Columns`](components/columns.md) | `Columns` |
| [`Frame`, `Overlay` y `Page overlay`](components/frame-and-overlays.md) | Los elementos posicionados |
| [`Image`](components/image.md) | `Image`: PNG, JPEG y SVG |
| [`Barcode`](components/barcode.md) | `Barcode`: QR, Code 128 y EAN-13 |
| [`Markdown`](components/markdown.md) | `Markdown` y el elemento generado `Code block` |
| [`Footnote`](components/footnote.md) | `Footnote`, la llamada `[^id]` y el área de notas generada `Footnotes` |
| [`Table of contents`](components/table-of-contents.md) | `Table of contents` y las entradas que genera |

## Implementación

| Página | Contenido |
|---|---|
| [Arquitectura](architecture.md) | Los packages, el flujo de datos y las clases principales |
| [El árbol de cajas](implementation/tree.md) | Cómo se construye y se cierra el árbol: `BoxTreeBuilder`, `Style`, el schema |
| [El layout](implementation/layout.md) | `Engine`, `LayoutContext`, la caché, los tokens de corte y la paginación |
| [El pintado](implementation/painting.md) | `StackingContext`, los lienzos, el texto, los bordes y las imágenes |
| [El PDF por dentro](implementation/pdf.md) | El árbol de estructura del PDF etiquetado, los *artifacts*, las anotaciones de enlace, los marcadores y los metadatos |
| [Uso desde Java](implementation/api.md) | `Converter`, `DocumentReader`, los tipos de error, los límites y los hilos |
| [Pruebas](implementation/testing.md) | Tests de referencia, ejemplos, catálogo de errores, el volcado de resultados y las pruebas de mutación |
