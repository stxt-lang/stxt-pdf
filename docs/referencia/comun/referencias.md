# Referencias

Un texto puede escribir datos que el motor calcula:

| Marcador | Qué escribe |
|---|---|
| `{page}` | El número impreso de la página donde está el texto ([Cortes](cortes.md#números-de-página-page-y-pages)) |
| `{pages}` | El último número impreso de su tanda ([Document](../componentes/document.md#numeración-de-las-páginas)) |
| `{page of: id}` | El número impreso de la página donde empieza la caja con ese `Id` |
| `{number of: id}` | El número de esa caja en el documento: el de un `List item` numerado o un encabezado numerado |

Se escriben en el texto de un `Text`, un `Paragraph` o una `Cell` con valor, y se pueden mezclar:

```
Text: El esquema de conexión está en la página {page of: wiring}; véase la cláusula {number of: pago}.
Block:
    Id: wiring
List:
    Marker: decimal
    List item:
        Id: pago
        Paragraph: …
```

## `{page of: id}`

- **Qué página es.** La de la **primera** caja que sale de la caja referida: si está partida entre varias páginas, la
  página donde empieza. Es el **número impreso** de esa página, no el físico: el mismo que dice `{page}` en su pie. Si la
  página no tiene número (`Page numbering: none`), es `REFERENCE_NOT_PLACEABLE`. Una `Section` con `Id` se puede referir:
  es su primera página. Si la caja es una fila repetida de una tabla (cabecera o pie), la primera de sus páginas.
- **Dónde se puede referir.** Cualquier caja con `Id` que no sea un `Page overlay` ni esté dentro de uno, que se repiten en todas
  las páginas (`REFERENCE_NOT_PLACEABLE`). La referencia puede ir antes o después de la caja, y también en un `Page overlay` (que
  escribe lo mismo en todas las páginas).
- **El `Id` es el de la caja**, igual que en `For id`: el de un `Item` o un `Area` va en su elemento. Un `Id` que no
  existe, o una referencia sin `Id` (`{page of: }`), es un error (`REFERENCE_UNKNOWN`) con la línea del texto, o la del
  carácter si el texto es un bloque `Content >>`.
- **El texto mide lo que se escribe.** `página 9` y `página 10` miden distinto, y el texto puede pasar a una línea más.
  Eso puede mover la caja referida a otra página; el motor vuelve a paginar hasta que la página escrita y la real
  coinciden ([cómo](../implementacion/layout.md#paginación)). Si no lo consigue, es `REFERENCE_UNSTABLE`, con la línea de
  la referencia que sigue cambiando; con documentos normales no pasa, porque un número solo cambia de ancho al cambiar de
  cifras (9 → 10).
- **La fuente necesita las cifras**, no las letras del marcador.

> **Decisión:** la página de una caja es la de su primer fragmento, también en las filas repetidas de una tabla.
> *Alternativa descartada:* la del último fragmento.

## `{number of: id}`

Lo tienen los `List item` de una lista numerada, y es lo que escribe su marcador sin el sufijo: `3`, `c`, `IV` (`decimal`,
`lower-alpha`, `upper-roman`…), con el `Start` y el `Number` que haya. Cada ítem da solo su propio número: para escribir
«4.2» de una lista anidada, se escriben los dos, `{number of: clausula}.{number of: apartado}`. También lo tienen los
encabezados con `Numbered: true`, y su número es el completo (`4.2`: sus niveles unidos por puntos,
[Encabezados](../componentes/heading.md#numeración-numbered)). Un `List item` de una lista con viñetas o sin marcador, un
encabezado sin numerar o cualquier otra caja no tiene número (`REFERENCE_NOT_NUMBERED`). Un encabezado escrito en un
`Markdown` se puede referir si lleva `{#id}` ([Markdown](../componentes/markdown.md#encabezados)); un ítem de una lista
de Markdown, no, porque no lleva `Id`.

## Lo que no hay

- Contadores propios del autor, ni otros formatos de número que los de los marcadores y los de `Number format`.
- Referir los elementos generados por un `Markdown`, salvo sus encabezados con `{#id}`.
