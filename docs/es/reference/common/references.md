# Referencias

Un texto puede escribir datos que el motor calcula:

| Marcador | Qué escribe |
|---|---|
| `{page}` | El número impreso de la página donde está el texto ([Cortes](breaks.md#números-de-página-page-y-pages)) |
| `{pages}` | El último número impreso de su tanda ([Document](../components/document.md#numeración-de-las-páginas)) |
| `{page of: id}` | El número impreso de la página donde empieza la caja con ese `Id` |
| `{number of: id}` | El número de esa caja en el documento: el de un `List item` numerado o un encabezado numerado |
| `{heading N}` | El texto del último `Heading N` que empieza en esa página o antes, en un `Page overlay`: el título corrido |
| `{heading N: first}` | El del primer `Heading N` que empieza en esa página |

Se escriben en el texto de un `Text`, un `Paragraph` o una `Cell` con valor (los títulos corridos, solo en un
`Page overlay`), y se pueden mezclar:

```
Text: El esquema de conexión está en la página {page of: wiring}; véase la cláusula {number of: pago}.
Block:
    Id: wiring
List:
    Marker: decimal
    List item:
        Id: pago
        Paragraph: ...
```

## `{page of: id}`

- **Qué página es.** La de la **primera** caja que sale de la caja referida: si está partida entre varias páginas, la
  página donde empieza. Es el **número impreso** de esa página y no el físico: el mismo que dice `{page}` en su pie. Si la
  página no tiene número (`Page numbering: none`), es `REFERENCE_NOT_PLACEABLE`. Una `Section` con `Id` se puede referir:
  es su primera página. Si la caja es una fila repetida de una tabla (cabecera o pie), la primera de sus páginas.
- **Dónde se puede referir.** Cualquier caja con `Id` que no sea un `Page overlay` ni esté dentro de uno, que se repiten en todas
  las páginas (`REFERENCE_NOT_PLACEABLE`). La referencia puede ir antes o después de la caja, y también en un `Page overlay` (que
  escribe lo mismo en todas las páginas).
- **El `Id` es el de la caja**, igual que en `For id`: el de un `Item` o un `Area` va en su elemento. Un `Id` que no
  existe, o una referencia sin `Id` (`{page of: }`), es un error (`REFERENCE_UNKNOWN`) con la línea del texto, o la del
  carácter si el texto es un bloque `Content >>`.
- **El texto mide lo que se escribe.** `página 9` y `página 10` miden distinto, y el texto puede pasar a una línea más.
  Eso puede mover la caja referida a otra página. El motor vuelve a paginar hasta que la página escrita y la real
  coinciden ([cómo](../implementation/layout.md#paginación)). Si no lo consigue, es `REFERENCE_UNSTABLE`, con la línea de
  la referencia que sigue cambiando. Con documentos normales no pasa, porque un número solo cambia de ancho al cambiar de
  cifras (9 → 10).
- **La fuente necesita las cifras** y no las letras del marcador.

> **Decisión:** la página de una caja es la de su primer fragmento, también en las filas repetidas de una tabla.
> *Alternativa descartada:* la del último fragmento.

## `{number of: id}`

Lo tienen los `List item` de una lista numerada, y es lo que escribe su marcador sin el sufijo: `3`, `c`, `IV` (`decimal`,
`lower-alpha`, `upper-roman`), con el `Start` y el `Number` que haya. Cada ítem da solo su propio número: para escribir
«4.2» de una lista anidada, se escriben los dos, `{number of: clausula}.{number of: apartado}`. También lo tienen los
encabezados con `Numbered: true`, y su número es el completo (`4.2`: sus niveles unidos por puntos,
[Encabezados](../components/heading.md#numeración-numbered)). Un `List item` de una lista con viñetas o sin marcador, un
encabezado sin numerar o cualquier otra caja no tiene número (`REFERENCE_NOT_NUMBERED`). Un encabezado escrito en un
`Markdown` se puede referir si lleva `{#id}` ([Markdown](../components/markdown.md#encabezados)). Un ítem de una lista
de Markdown no se puede referir, porque no lleva `Id`.

## `{heading N}` y `{heading N: first}`

Un **título corrido**: el texto de un encabezado en la cabecera o el pie de cada página, como el capítulo o el apartado
en curso de un libro o un manual.

- **Qué encabezado es.** `{heading N}`, con `N` de 1 a 6, escribe el texto del último `Heading N` que **empieza en esa
  página o antes**: el que está en vigor al acabar la página. `{heading N: first}` escribe el **primero que empieza en la
  página** o, si en ella no empieza ninguno, lo mismo que `{heading N}` (en un diccionario, la primera y la última
  entrada de la página). Un encabezado de nivel superior (un número menor) deja sin encabezado a los niveles de debajo:
  en la primera página de un capítulo sin apartado todavía, `{heading 2}` está vacío, y no escribe el último apartado
  del capítulo anterior. Antes del primer `Heading N` del documento también está vacío.
- **Qué texto.** El del encabezado, como en el índice y en los marcadores del PDF
  ([Encabezados](../components/heading.md)): sus `Text` unidos por espacios y, si está numerado, con su número delante
  (`3 Instalación`). `Case` y `Small caps` del texto que lo escribe se le aplican como al resto. Un texto que queda vacío
  no ocupa ninguna línea.
- **Dónde.** Solo en un texto de un `Page overlay`, o dentro de uno: el encabezado en vigor depende de la página, y un
  `Page overlay` que lleva un título corrido **se maqueta en cada página** con el texto de esa página (los demás, una vez,
  y se repiten). En cualquier otro texto es `HEADING_REFERENCE_OUTSIDE_PAGE_OVERLAY`. Como un `Page overlay` no ocupa
  espacio, el flujo y las referencias a páginas no cambian por él.
- **La forma.** `{heading 2}` o `{heading 2: first}`; otro nivel u otra variante es `REFERENCE_UNKNOWN`. Un nivel que el
  documento no usa es válido y queda vacío en todas las páginas.
- **La fuente necesita los caracteres** de todos los encabezados de ese nivel, en la fuente del texto que los escribe
  (`GLYPH_MISSING`).

> **Decisión:** un encabezado de nivel superior vacía los títulos corridos de los niveles inferiores, en lugar de dejar el
> último que empezó. Así el apartado del capítulo anterior no sigue en la cabecera de un capítulo nuevo. *Alternativa
> descartada:* el último `Heading N` que empezó, sin mirar los niveles superiores, que es lo que hace el `StyleRef` de un
> procesador de textos.

> **Decisión:** `{heading N: first}` sin ningún encabezado de ese nivel en la página escribe lo mismo que `{heading N}`,
> en lugar de quedar vacío: una página sin apartado nuevo sigue dentro del apartado en curso. *Alternativa descartada:*
> vacío.

## Lo que no hay

- Contadores propios del autor, ni otros formatos de número que los de los marcadores y los de `Number format`.
- Referir los elementos generados por un `Markdown`, salvo sus encabezados con `{#id}`.
