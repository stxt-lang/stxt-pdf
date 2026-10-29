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
- **Dónde se puede referir.** Cualquier caja con `Id` que no sea un `Fixed` ni esté dentro de uno, que se repiten en todas
  las páginas (`REFERENCE_NOT_PLACEABLE`). La referencia puede ir antes o después de la caja, y también en un `Fixed` (que
  escribe lo mismo en todas las páginas).
- **El `Id` es el de la caja**, igual que en `For id`: el de un `Item` o un `Area` va en su elemento. Un `Id` que no
  existe, o una referencia sin `Id` (`{page of: }`), es un error (`REFERENCE_UNKNOWN`) con la línea del texto, o la del
  carácter si el texto es un bloque `Content >>`.
- **El texto mide lo que se escribe.** La referencia se sustituye antes de repartir el texto en líneas, así que `página 9`
  y `página 10` miden distinto y el texto puede pasar a una línea más. Eso mueve el contenido y puede cambiar la página de
  la caja referida; por eso la página se resuelve **paginando hasta que converja**: la primera pasada supone un número de
  prueba de dos cifras (como `{page}`), cada pasada comprueba en qué página ha quedado cada caja referida, y si no coincide
  con la supuesta, se vuelve a paginar con la que ha salido. Termina cuando coinciden. Si tras 10 pasadas que cambian
  alguna página no han coincidido, es un error (`REFERENCE_UNSTABLE`, con la línea de la referencia que sigue cambiando).
  No pasa con documentos normales, porque un número solo cambia de ancho al cambiar de cifras (9 → 10, 99 → 100).

> **Decisión:** la referencia se sustituye antes de repartir el texto en líneas, a diferencia de `{page}` y `{pages}`,
> que reservan un hueco y se sustituyen al final: el marcador tiene espacios y el reparto lo partiría en tres palabras, y
> con la convergencia no hace falta un segundo paso. *Alternativa descartada:* un marcador sin espacios, resuelto en dos
> fases como `{page}`.

> **Decisión:** la página de una caja es la de su primer fragmento, también en las filas repetidas de una tabla.
> *Alternativa descartada:* la del último fragmento.

> **Decisión:** la primera pasada supone el número de prueba de `{page}` (`00`), así que con referencias a páginas
> siempre hay al menos una segunda pasada; el máximo es de 10 pasadas que cambian alguna página, y las pasadas por crecer
> las cifras de `{pages}` no cuentan, porque solo crecen. `REFERENCE_UNSTABLE` es una red de seguridad: con un medidor
> de texto aditivo, la paginación converge siempre. *Alternativa descartada:* quedarse con la última pasada sin avisar.

> **Decisión:** las referencias que no pueden resolverse se detectan al construir el árbol, no al maquetar: un `Id` que no
> existe (`REFERENCE_UNKNOWN`) y la página de una caja de un `Fixed` (`REFERENCE_NOT_PLACEABLE`) dependen solo de la
> estructura del documento. Se comprueban todas las referencias, también las de un `Fixed`, un `Markdown` o una `Cell`.
> La de una caja que cae en una página sin número solo se sabe al paginar.

> **Decisión:** en las métricas reales, una referencia se comprueba como cifras: un texto con `{page of: id}` necesita en
> su fuente las cifras, no las letras del marcador.

## `{number of: id}`

Lo tienen los `List item` de una lista numerada, y es lo que escribe su marcador sin el sufijo: `3`, `c`, `IV` (`decimal`,
`lower-alpha`, `upper-roman`…), con el `Start` y el `Number` que haya. Cada ítem da solo su propio número: para escribir
«4.2» de una lista anidada, se escriben los dos, `{number of: clausula}.{number of: apartado}`. También lo tienen los
encabezados con `Numbered: true`, y su número es el completo (`4.2`: sus niveles unidos por puntos,
[Encabezados](../componentes/heading.md#numeración-numbered)). Un `List item` de una lista con viñetas o sin marcador, un
encabezado sin numerar o cualquier otra caja no tiene número (`REFERENCE_NOT_NUMBERED`). No se puede referir a un ítem ni
a un encabezado escrito en un `Markdown`, porque lo generado no lleva `Id`.

> **Decisión:** `{number of: id}` no depende de la paginación: el número sale del árbol, así que se sustituye igual desde
> la primera pasada. *Alternativa descartada:* dar `Id` a lo generado por un `Markdown` para poder referirlo, que lo
> sacaría en los volcados.

## Accesibilidad

El texto que resulta es contenido de su elemento (`P`, `Span`…), como cualquier número.

## Lo que no hay

- Contadores propios del autor, ni otros formatos de número que los de los marcadores y el decimal de los encabezados.
- Referir los elementos generados por un `Markdown`.
