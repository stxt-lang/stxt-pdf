# Valores

## Tipos de valor

| Sintaxis | Significado | Ejemplo |
|---|---|---|
| Número | Puntos. Punto decimal, nunca coma. Puede ser negativo donde se indique. | `12`, `12.5`, `-30` |
| Porcentaje | Porcentaje de una base (ver [bases de los porcentajes](#bases-de-los-porcentajes)). | `50%` |
| Fracción | Una parte del espacio libre, con su peso delante (`*` solo es `1*`). Solo en pistas de grid. | `*`, `2*`, `2.5*` |
| `auto` | El motor calcula el valor según el contexto. | `auto` |
| `min-content` | El menor tamaño posible sin desbordar (la palabra más larga). | `min-content` |
| `max-content` | El tamaño sin ningún salto de línea. | `max-content` |
| `fit-content` | `min(max-content, max(min-content, disponible))`. | `fit-content` |
| `minmax(a, b)` | Pista de grid entre `a` y `b`. `a` no puede ser fracción. | `minmax(100, *)` |

Cada propiedad admite solo las formas de la columna *Valores* de su tabla; cualquier otra (`Width: *`,
`Max width: max-content`, `Gap: auto`, un `minmax()` fuera de las pistas) es `VALUE_NOT_ALLOWED`, nunca un `auto`
silencioso. Solo `Margin`, `Offset` y `Letter spacing` admiten valores negativos. Un porcentaje de un alto que no es
[definido](#alto-definido) es un error.

> **Decisión:** una fracción se escribe con un asterisco y su peso: `120 * 2*` da a la tercera pista el doble de lo que
> sobra que a la segunda. *Alternativa descartada:* `1fr`, una unidad que solo conoce quien ya la ha usado.

> **Decisión:** no hay funciones que elijan entre medidas (el menor, el mayor o uno entre dos límites): un tamaño entre
> dos límites se escribe con `Min width` y `Max width` (`Width: 50%`, `Min width: 200`, `Max width: 400`), y cada cosa
> se dice de una sola forma. *Alternativa descartada:* `min()`, `max()` y `clamp()`, que repetían lo que ya hacen los
> mínimos y los máximos.

## Listas y `repeat`

Algunas propiedades admiten varios valores separados por espacios (`Column sizes`, `Row sizes`, `Margin`…). En las listas de
pistas, `repeat(n, patrón)` repite un patrón:

```
Column sizes: 150 repeat(3, *) minmax(100, 2*)
```

equivale a `150 * * * minmax(100, 2*)`.

## Abreviados de cuatro lados

`Margin`, `Padding`, `Border` y `Offset` aceptan de 1 a 4 valores en el orden **arriba, derecha, abajo, izquierda**:

| Valor | Arriba | Derecha | Abajo | Izquierda |
|---|---|---|---|---|
| `10` | 10 | 10 | 10 | 10 |
| `10 20` | 10 | 20 | 10 | 20 |
| `10 20 5` | 10 | 20 | 5 | 20 |
| `10 20 5 0` | 10 | 20 | 5 | 0 |

`Border color` admite también de 1 a 4 colores con este orden. `Corner radius` admite de 1 a 4 valores con el mismo convenio,
pero uno por **esquina**: arriba a la izquierda, arriba a la derecha, abajo a la derecha y abajo a la izquierda
(`Corner radius: 0 20` redondea solo la esquina de arriba a la derecha y la de abajo a la izquierda).

## Colores

`#RGB`, `#RGBA`, `#RRGGBB` o `#RRGGBBAA` en hexadecimal: `#000`, `#A32D2D`, `#12315A80`. El último dígito o par es la
**opacidad** (`FF` o `F`, opaco; `00`, transparente) y, si falta, el color es opaco. Vale en todas las propiedades de
color (`Color`, `Border color`, `Background`, `Separator color`).

> **Decisión:** la opacidad va en cada color; no hay `Opacity` de la caja, que afectaría a todo lo que contiene.

El fondo también admite un **degradado lineal**: `linear(ángulo, color, color…)`, con el ángulo en grados (`0` va hacia
arriba, `90` hacia la derecha, `180` hacia abajo) y al menos dos colores, repartidos a partes iguales:
`linear(180, #12315A00, #12315A)`. Cómo se pinta está en [La caja](caja.md#degradado).

No hay colores con nombre (`red`), funciones de color (`rgb()`), paradas con posición ni degradados radiales, cónicos o
repetidos.

## Booleanos y enteros

Los booleanos se escriben `true` o `false`. Los enteros, sin decimales: `Layer: -1`, `Widows: 3`.

## Bases de los porcentajes

| Propiedad | Porcentaje de… |
|---|---|
| `Width`, `Min width`, `Max width` | el ancho del contenedor de referencia |
| `Height`, `Min height`, `Max height` | el alto del contenedor de referencia, que **debe ser definido** |
| `Margin`, `Padding` (los cuatro lados) | el **ancho** del contenedor de referencia, también arriba y abajo |
| `Offset` arriba/abajo | el alto del contenedor de referencia, que **debe ser definido** |
| `Offset` izquierda/derecha | el ancho del contenedor de referencia |
| `Basis` (ítem de `Flex`) | el tamaño del `Flex` en el eje principal (en un `Flex` vertical, su alto, que debe ser definido) |
| `Gap` | el tamaño de la caja de contenido del contenedor en ese eje (entre filas, su alto, que debe ser definido) |
| Pistas de grid | el tamaño de la caja de contenido del contenedor en ese eje, sin descontar los huecos (`50% 50%` con `Gap` desborda, y es un error); las filas, su alto, que debe ser definido |
| `Line height` | el `Size` de la propia caja |
| `Corner radius` | el ancho de la caja de borde en horizontal y su alto en vertical ([La caja](caja.md#esquinas)) |

`Border` no admite porcentajes. El contenedor de referencia de cada caja está en [La caja](caja.md#el-contenedor-de-referencia).

## Alto definido

Los anchos se conocen siempre; los altos, no. Un porcentaje vertical es un error (`INDEFINITE_PERCENTAGE`) si su base no
es un **alto definido**, que se decide al leer el documento con esta regla fija:

- el área útil;
- el de una caja con `Height` en puntos, o en porcentaje de un alto definido;
- el de una caja con `Height: auto` y `Aspect ratio`;
- el de un `Overlay` o un `Page overlay` con `Height: auto` estirado entre arriba y abajo;
- el de un ítem de grid con `Height: auto` estirado en su área (`Vertical: stretch`).

La base de `Height`, `Min height` y `Max height` de un `Overlay` o un `Page overlay` es su contenedor de referencia, y la de un
ítem de grid, su área: las dos son siempre definidas. Un ítem de `Flex` estirado **no** da un alto definido a sus hijos:
si lo necesitan, declara su `Height`. El alto de las columnas de un `Columns` tampoco es definido
([Columns](../componentes/columns.md#contenido)).

> **Decisión:** el alto definido se decide al leer el documento, no al maquetar. Es más estricto (un ítem de `Flex`
> estirado no da alto definido aunque su alto se acabe conociendo), pero el error sale siempre, en la línea de la
> propiedad. *Alternativa descartada:* detectar los porcentajes sin base durante el layout.
