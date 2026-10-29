# `Multicol`

Un `Multicol` reparte su contenido en **columnas** del mismo ancho, como en una revista: el contenido llena la primera
columna, sigue en la segunda y, al acabar las columnas de una página, continúa en la primera columna de la página
siguiente. Admite `Id`, `Class`, `Link`, `Box`, `Typography` y `Break`, como un bloque, y sus propiedades; va donde va un
elemento en flujo, también dentro de otro `Multicol`.

```
Multicol:
	Count: 2
	Column gap: 20
	Rule: 0.5
	Rule color: #999999
	Paragraph:
		Content >>
			Texto largo que fluye por las dos columnas…
	Paragraph:
		Content >>
			…y sigue en la página siguiente.
```

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Count` | entero ≥ 1 | — | Número de columnas; con `Column width`, el máximo. |
| `Column width` | número > 0 | — | Ancho mínimo de las columnas: caben tantas como permita el ancho. Solo puntos. |
| `Column gap` | número ≥ 0 | `0` | Separación entre columnas. Solo puntos. Con `Count: 1` es un error (`SINGLE_COLUMN`). |
| `Rule` | número > 0 | sin línea | Grosor de la línea entre columnas, centrada en el hueco. No pasa de `Column gap` (`COLUMN_RULE_WIDTH`); con `Count: 1` es un error (`SINGLE_COLUMN`). |
| `Rule color` | color | el `Color` del texto | Solo con `Rule` (`RULE_COLOR_WITHOUT_RULE`). No afecta al layout. |
| `Fill` | `balance`, `auto` | `balance` | `balance` iguala las columnas de la última página; `auto` las llena en orden. |

`Count`, `Column width` o los dos son obligatorios (`MULTICOL_COLUMNS`). No van en las reglas de `Styles`, que solo
admiten los grupos.

## Columnas

El número y el ancho salen de `Count` y `Column width`, sobre el ancho de la caja de contenido (`W`) y con `Column gap`
(`g`) entre cada dos:

- solo `Count: n`: `n` columnas;
- solo `Column width: w`: tantas como quepan, `⌊(W + g) / (w + g)⌋`, y al menos una;
- los dos: las que quepan, sin pasar de `Count`.

Las columnas se reparten el ancho que dejan los huecos: miden `(W − (n − 1) · g) / n`.

## Contenido

Dentro de las columnas los hijos se apilan en flujo de bloque ([Block](block.md#el-flujo-de-bloque)): cada columna es un
contexto de formato de bloque, así que los márgenes de dentro no salen del `Multicol`. Admite todos los elementos en flujo;
un `Absolute` o un `Fixed` dentro de un `Multicol`, a cualquier profundidad, es un error (`POSITIONED_IN_MULTICOL`). El
alto de las columnas no es definido: un porcentaje vertical de dentro es un error (`INDEFINITE_PERCENTAGE`), aunque el
`Multicol` tenga `Height`.

> **Decisión:** el alto de las columnas no es definido, aunque el `Multicol` tenga `Height`: sale del reparto y no se
> conoce al leer el documento.

## Alto de las columnas

Cada columna es un contenedor de fragmentos ([Cortes](../comun/cortes.md#modelo)), y el contenido se parte entre columnas
igual que entre páginas, con las mismas reglas (viudas y huérfanas, `avoid`, garantía de progreso). En cada página, las
columnas miden lo que queda de página para el contenido del `Multicol` y, si tiene `Height` (o `Max height`), no pasan de lo
que le queda de él:

1. Se llenan **en orden**: la primera columna, desde donde se quedó el contenido; cada una de las siguientes, desde donde
   termina la anterior. Se para al acabar el contenido, las columnas o en un corte forzado de página.
2. Si el contenido termina en esta página y `Fill` es `balance`, las columnas se **equilibran**: se busca el alto mínimo
   con el que el contenido cabe en ellas sin cortes entre columnas que incumplan más reglas que los del reparto en orden.
   Con `Fill: auto` se quedan como en el paso 1. Las páginas en las que el contenido no termina no se equilibran.
3. El `Multicol` mide en esa página lo que su columna más alta (con `Height`, lo de cualquier caja con alto definido,
   [Cortes](../comun/cortes.md#alto-definido-y-cortes)).

Fuera de una página y sin alto definido (un `Multicol` que es un ítem de `Flex` o de `Grid`, o que está en un `Absolute`) no
hay alto que llenar: las columnas se equilibran siempre, también con `Fill: auto`.

> **Decisión:** el equilibrado no incumple reglas que el reparto en orden respeta: un bloque con `Inside: avoid` no se
> parte y un párrafo de tres líneas con viudas y huérfanas de 2 no se reparte. *Alternativa descartada:* un equilibrado que
> incumple reglas para igualar mejor, o equilibrar también las páginas intermedias.

## Cortes

Un `Multicol` se parte entre páginas: lo que no cabe en las columnas de una página sigue en la primera columna de la
siguiente. Si la primera columna no puede colocar nada, el `Multicol` entero pasa a la página siguiente, y si no puede otra
columna, el resto. Una columna solo coloca algo a la fuerza si el `Multicol` está al principio de la página. Al elegir el
corte de la página, cuenta el de la última columna. `Before: column` y `After: column` (de `Break`) fuerzan un salto a la
columna siguiente; fuera de un `Multicol` son un error (`COLUMN_BREAK_OUTSIDE_MULTICOL`). Un `Before: page` o `After: page`
de dentro termina las columnas de la página.

## Lo que no cabe

Si el alto propio del `Multicol` (`Height`, `Max height`) termina en una página y su contenido no cabe en las columnas de
esa página, es un error (`BOX_OVERFLOW`), en la línea de esa propiedad; lo mismo si, sin página ni alto que llenar, sus
cortes de columna necesitan más columnas de las que tiene, en la línea de `Count` ([Desbordamiento](../comun/desbordamiento.md)).

> **Decisión:** el contenido que no cabe en las columnas es un error. *Alternativa descartada:* columnas de desbordamiento
> a un lado, o una última columna sin límite.

## Línea entre columnas

Con `Rule`, entre cada dos columnas **con contenido** se pinta una línea de ese grosor, centrada en el hueco, desde arriba de
la caja de contenido y con el alto de la columna más alta de esa página. Su color es `Rule color` o, sin él, el `Color` del
texto. Se pinta con el fondo del `Multicol`, debajo del contenido, y no puede ser más ancha que `Column gap`.

> **Decisión:** la línea no ocupa sitio, así que más ancha que el hueco se pintaría encima de las columnas.

## Tamaños intrínsecos

Con `Count` y sin `Column width`, el min-content es `Count` columnas del min-content del contenido más los huecos; con
`Column width`, una columna, porque el número de columnas se adapta al ancho. El max-content es `Count` columnas (o una, sin
`Count`) del max-content del contenido, o de `Column width` si es mayor, más los huecos.

> **Decisión:** con `Column width`, el min-content es el de una sola columna. *Alternativa descartada:* el menor entre
> `Column width` y el min-content del contenido.

## Accesibilidad

El contenido se lee en orden, de columna en columna; la línea entre columnas no se lee.

## Lo que no hay

- Contenido que ocupe todas las columnas a la vez (un título a lo ancho en medio): se parte el `Multicol` en dos.
- Equilibrar todas las páginas, no solo la última, y líneas entre columnas con estilo.
- `Absolute` y `Fixed` dentro de las columnas.
