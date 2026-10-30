# Referencia de stxt-pdf

**Versión del modelo:** 0.1 · **Namespace:** `dev.stxt.pdf` · **Unidades:** puntos tipográficos (pt)

La referencia es la **fuente de verdad** del comportamiento del motor: dice qué admite cada elemento, qué significa
cada propiedad, cómo calcula el motor la posición y el tamaño de cada caja, qué es un error y qué se escribe en el PDF.
Si el [manual](../manual/README.md) y la referencia dicen cosas distintas, manda la referencia.

Cada vez que el motor ha tenido que elegir entre dos interpretaciones razonables, la elección está escrita en el sitio
al que afecta, en un bloque así:

> **Decisión:** lo que se decidió y lo que implica. *Alternativa descartada:* lo que se podría haber hecho en su lugar.

El schema STXT del modelo es [`src/main/resources/stxt/dev.stxt.pdf.stxt`](../../src/main/resources/stxt/dev.stxt.pdf.stxt);
si difiere del resumen de [la estructura](comun/estructura.md#schema), manda el fichero.

## Comportamiento común

| Página | Contenido |
|---|---|
| [Estructura de un documento](comun/estructura.md) | Convenciones, elementos y envoltorios, el árbol de cajas, quién coloca a quién y el resumen del schema |
| [Valores](comun/valores.md) | Números, porcentajes, fracciones, funciones, abreviados de cuatro lados, colores y las bases de los porcentajes (alto definido) |
| [La caja: `Box`](comun/caja.md) | Las cuatro cajas, el contenedor de referencia, el grupo `Box`, el dimensionado (intrínsecos, mínimos y máximos, proporción) y el aspecto (fondo, borde, esquinas) |
| [Tipografía: `Typography`](comun/tipografia.md) | La herencia, la fuente, el interlineado y la línea base, el reparto en líneas, la alineación y el idioma |
| [Formato del texto](comun/formato-del-texto.md) | Tramos con formato, `Inline: markdown`, mayúsculas, versalitas, interletraje, ligaduras, texto preformateado, subrayado y tachado |
| [Páginas y cortes: `Break`](comun/cortes.md) | La fragmentación: oportunidades y elección del corte, viudas y huérfanas, cortes forzados, bordes en los cortes, `{page}` y `{pages}` |
| [Capas y giro](comun/capas-y-giro.md) | Los contextos de apilamiento, `Layer`, el orden de pintado y `Rotate` |
| [Reglas reutilizables: `Styles`](comun/estilos.md) | `For element`, `For style`, `For id` y `For column`, la prioridad y el documento de estilos aparte |
| [Referencias](comun/referencias.md) | `{page of: id}` y `{number of: id}`, y cómo converge la paginación |
| [Enlaces](comun/enlaces.md) | `Link` y los enlaces de Markdown: destinos, zonas y PDF etiquetado |
| [Recursos y fuentes](comun/recursos.md) | La carpeta de recursos, las rutas seguras y cómo se identifican y se comprueban las fuentes |
| [El PDF](comun/pdf.md) | Lo que lleva todo PDF: fuentes incrustadas, accesibilidad, contraste, marcadores, título y PDF/UA-1 o PDF/UA-2 |
| [Desbordamiento](comun/desbordamiento.md) | Nada puede desbordar: qué se comprueba y a quién se señala |
| [Errores](comun/errores.md) | La política de errores, los niveles de validación, los códigos y sus tipos |
| [Límites](comun/limites.md) | Páginas, píxeles y complejidad de los SVG que admite una conversión |
| [Ejemplo completo](comun/ejemplo-completo.md) | Un documento con el cálculo de cada caja hecho a mano |
| [Glosario](comun/glosario.md) | Los términos de la referencia |

## Componentes

| Página | Elementos |
|---|---|
| [`Document`](componentes/document.md) | `Document`, `Page`, `Title`, `Language` y `Section` |
| [`Block`](componentes/block.md) | `Block` y el flujo de bloque, que comparten todos los bloques |
| [`Text`](componentes/text.md) | `Text` y `Paragraph` |
| [Encabezados](componentes/heading.md) | `Heading 1` … `Heading 6` y su numeración |
| [`List`](componentes/list.md) | `List` y `List item` |
| [`Table`](componentes/table.md) | `Table`, `Head`, `Body`, `Foot`, `Row` y `Cell` |
| [`Flex`](componentes/flex.md) | `Flex` e `Item` |
| [`Grid`](componentes/grid.md) | `Grid` y `Area` |
| [`Columns`](componentes/columns.md) | `Columns` |
| [`Frame`, `Overlay` y `Page overlay`](componentes/frame-and-overlays.md) | Los elementos posicionados |
| [`Image`](componentes/image.md) | `Image`: PNG, JPEG y SVG |
| [`Markdown`](componentes/markdown.md) | `Markdown` y el elemento generado `Code block` |
| [`Table of contents`](componentes/table-of-contents.md) | `Table of contents` y las entradas que genera |

## Implementación

| Página | Contenido |
|---|---|
| [Arquitectura](arquitectura.md) | Los packages, el flujo de datos y las clases principales |
| [El árbol de cajas](implementacion/arbol.md) | Cómo se construye y se cierra el árbol: `BoxTreeBuilder`, `Style`, el schema |
| [El layout](implementacion/layout.md) | `Engine`, `LayoutContext`, la caché, los tokens de corte y la paginación |
| [El pintado](implementacion/pintado.md) | `StackingContext`, los lienzos, el texto, los bordes y las imágenes |
| [El PDF por dentro](implementacion/pdf.md) | El árbol de estructura del PDF etiquetado, los *artifacts*, las anotaciones de enlace, los marcadores y los metadatos |
| [Uso desde Java](implementacion/api.md) | `Converter`, `DocumentReader`, los tipos de error, los límites y los hilos |
| [Pruebas](implementacion/pruebas.md) | Tests de referencia, ejemplos, catálogo de errores, el volcado de resultados y las pruebas de mutación |
