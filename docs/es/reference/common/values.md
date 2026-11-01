# Valores

## Tipos de valor

| Sintaxis | Significado | Ejemplo |
|---|---|---|
| Número | Puntos. Punto decimal, nunca coma. Puede ser negativo donde se indique. | `12`, `12.5`, `-30` |
| Número con `mm` | Milímetros, con la unidad pegada al número. Vale donde vale un número, salvo en `Grow`, `Shrink`, `Rotate` y `Aspect ratio`. | `12mm`, `-4.5mm` |
| Porcentaje | Porcentaje de una base (ver [bases de los porcentajes](#bases-de-los-porcentajes)). | `50%` |
| Fracción | Una parte del espacio libre, con su peso delante (`*` solo es `1*`). Solo en pistas de grid. | `*`, `2*`, `2.5*` |
| `auto` | El motor calcula el valor según el contexto. | `auto` |
| `narrowest` | El menor tamaño posible sin desbordar (la palabra más larga). | `narrowest` |
| `natural` | El tamaño sin ningún salto de línea. | `natural` |
| `fit` | El ancho natural, sin pasar del disponible ni bajar del más estrecho. | `fit` |
| `between(a, b)` | Pista de grid entre `a` y `b`. `a` no puede ser fracción. | `between(100, *)` |

Cada propiedad admite solo las formas de la columna *Valores* de su tabla. Cualquier otra (`Width: *`,
`Max width: natural`, `Gap: auto`, un `between()` fuera de las pistas) es `VALUE_NOT_ALLOWED` y no un `auto`
silencioso. Solo `Margin`, `Offset` y `Letter spacing` admiten valores negativos. Un porcentaje de un alto que no es
[definido](#alto-definido) es un error.

Un milímetro son 72 / 25,4 puntos, unos 2,83: la medida se pasa a puntos al leer el documento, y los mensajes de error
dan las medidas en puntos, con su unidad (`it measures 600 pt`). Puntos y milímetros se mezclan, también en un mismo
valor (`Margin: 20mm 40`). Cualquier otra unidad (`5cm`, `12pt`, `1in`) y una unidad separada del número (`12 mm`) son
`VALUE_SYNTAX`. `Grow` y `Shrink` (factores), `Rotate` (grados) y `Aspect ratio` (una proporción) no son medidas: una
unidad en ellos también es un error.

> **Decisión:** un número solo son puntos y las medidas en milímetros llevan `mm`: los documentos escritos en puntos
> siguen valiendo igual. Hay una sola unidad más, porque los milímetros y los puntos cubren el papel y la tipografía, y
> cada medida se escribe de una sola forma. *Alternativa descartada:* `cm`, `in` y un `pt` explícito, que darían dos
> formas a la misma medida (`1cm` y `10mm`, `12` y `12pt`), y una unidad por documento, que cambiaría el sentido de un
> número según dónde se lea (también en un documento de estilos aparte).

> **Decisión:** una fracción se escribe con un asterisco y su peso: `120 * 2*` da a la tercera pista el doble de lo que
> sobra que a la segunda. *Alternativa descartada:* `1fr`, una unidad que solo conoce quien ya la ha usado.

> **Decisión:** las palabras que miden el contenido dicen qué medida dan: `narrowest` (lo más estrecho), `natural` (sin
> partir líneas) y `fit` (ajustado al espacio). *Alternativa descartada:* `min-content`, `max-content` y `fit-content`,
> nombres del mecanismo de la maquetación web.

> **Decisión:** una pista entre dos tamaños se escribe `between(a, b)`, que se lee tal cual: `between(100, *)`.
> *Alternativa descartada:* `minmax(a, b)`, que nombra las dos funciones de la pista y no lo que mide.

> **Decisión:** no hay funciones que elijan entre medidas (el menor, el mayor o uno entre dos límites): un tamaño entre
> dos límites se escribe con `Min width` y `Max width` (`Width: 50%`, `Min width: 200`, `Max width: 400`), y cada cosa
> se dice de una sola forma. *Alternativa descartada:* `min()`, `max()` y `clamp()`, que repetían lo que ya hacen los
> mínimos y los máximos.

## Listas y `repeat`

Algunas propiedades admiten varios valores separados por espacios (`Column sizes`, `Row sizes` o `Margin`). En las listas de
pistas, `repeat(n, patrón)` repite un patrón:

```
Column sizes: 150 repeat(3, *) between(100, 2*)
```

equivale a `150 * * * between(100, 2*)`.

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
**opacidad** (`FF` o `F`, opaco, y `00`, transparente) y, si falta, el color es opaco. Vale en todas las propiedades de
color (`Color`, `Border color`, `Background`, `Separator color`).

> **Decisión:** la opacidad va en cada color. No hay `Opacity` de la caja, que afectaría a todo lo que contiene.

El fondo también admite un **degradado lineal**: `linear(ángulo, color, color, ...)`, con el ángulo en grados (`0` va hacia
arriba, `90` hacia la derecha, `180` hacia abajo) y al menos dos colores, repartidos a partes iguales:
`linear(180, #12315A00, #12315A)`. Cómo se pinta está en [La caja](box.md#degradado).

No hay colores con nombre (`red`), funciones de color (`rgb()`), paradas con posición ni degradados radiales, cónicos o
repetidos.

## Booleanos y enteros

Los booleanos se escriben `true` o `false`. Los enteros, sin decimales: `Layer: -1`, `Widows: 3`.

## Bases de los porcentajes

| Propiedad | Porcentaje de |
|---|---|
| `Width`, `Min width`, `Max width` | el ancho del contenedor de referencia |
| `Height`, `Min height`, `Max height` | el alto del contenedor de referencia, que **debe ser definido** |
| `Margin`, `Padding` (los cuatro lados) | el **ancho** del contenedor de referencia, también arriba y abajo |
| `Offset` arriba/abajo | el alto del contenedor de referencia, que **debe ser definido** |
| `Offset` izquierda/derecha | el ancho del contenedor de referencia |
| `Base size` (ítem de `Stack`) | el tamaño del `Stack` en el eje principal (en un `Stack` vertical, su alto, que debe ser definido) |
| `Gap` | el tamaño de la caja de contenido del contenedor en ese eje (entre filas, su alto, que debe ser definido) |
| Pistas de grid | el tamaño de la caja de contenido del contenedor en ese eje, sin descontar los huecos (`50% 50%` con `Gap` desborda, y es un error); las filas, su alto, que debe ser definido |
| `Line height` | el `Size` de la propia caja |
| `Corner radius` | el ancho de la caja de borde en horizontal y su alto en vertical ([La caja](box.md#esquinas)) |

`Border` no admite porcentajes. El contenedor de referencia de cada caja está en [La caja](box.md#el-contenedor-de-referencia).

## Alto definido

Los anchos se conocen siempre. Los altos, no. Un porcentaje vertical es un error (`INDEFINITE_PERCENTAGE`) si su base no
es un **alto definido**, que se decide al leer el documento con esta regla fija:

- el área útil;
- el de una caja con `Height` en puntos o en milímetros, o en porcentaje de un alto definido;
- el de una caja con `Height: auto` y `Aspect ratio`;
- el de un `Overlay` o un `Page overlay` con `Height: auto` estirado entre arriba y abajo;
- el de un ítem de grid con `Height: auto` estirado en su área (`Vertical: stretch`).

La base de `Height`, `Min height` y `Max height` de un `Overlay` o un `Page overlay` es su contenedor de referencia, y la de un
ítem de grid, su área: las dos son siempre definidas. Un ítem de `Stack` estirado **no** da un alto definido a sus hijos:
si lo necesitan, el ítem debe declarar su `Height`. El alto de las columnas de un `Columns` tampoco es definido
([Columns](../components/columns.md#contenido)).

> **Decisión:** el alto definido se decide al leer el documento y no al maquetar. Es más estricto (un ítem de `Stack`
> estirado no da alto definido aunque su alto se acabe conociendo), pero el error sale siempre, en la línea de la
> propiedad. *Alternativa descartada:* detectar los porcentajes sin base durante el layout.
