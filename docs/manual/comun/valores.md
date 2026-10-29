# Valores

## Medidas

| Se escribe | Significa | Ejemplo |
|---|---|---|
| un número | puntos; con punto decimal, nunca coma | `Width: 120`, `Size: 10.5` |
| un porcentaje | una parte de otra medida (abajo) | `Width: 50%` |
| `auto` | lo calcula el motor | `Width: auto` |
| `min-content` | lo más estrecho que puede ser sin que nada se salga: la palabra más larga | `Width: min-content` |
| `max-content` | lo que mide sin partir ninguna línea | `Width: max-content` |
| `fit-content` | lo que necesita su contenido, sin pasar del espacio disponible | `Width: fit-content` |
| `min(a, b)`, `max(a, b)` | el menor o el mayor de varios valores | `Width: min(300, 50%)` |
| `clamp(a, b, c)` | `b`, pero nunca menos que `a` ni más que `c` | `Width: clamp(200, 50%, 400)` |

Cada propiedad admite solo algunas de estas formas, y su tabla lo dice; cualquier otra es un error. Solo los márgenes, los
desplazamientos de las cajas posicionadas y el interletraje pueden ser negativos.

Las columnas de un `Grid` admiten además **fracciones** (`1fr`, `2fr`: partes del espacio que sobra), `minmax(a, b)` y
`repeat(n, …)` ([Grid](../componentes/grid.md)).

## Porcentajes

Un porcentaje es una parte de otra medida, casi siempre de su contenedor de referencia ([Conceptos](conceptos.md)):

- los anchos, los márgenes y el relleno (también arriba y abajo) son del **ancho** del contenedor;
- los altos son del **alto** del contenedor, que tiene que conocerse antes de maquetar lo de dentro;
- `Line height` es del tamaño de la fuente de la propia caja.

**Un alto que se conoce** es el del área útil de la página, el de una caja con `Height` en puntos (o en porcentaje de otro
alto que se conoce) y el de algunas cajas que se estiran (un `Absolute` anclado arriba y abajo, una celda de `Grid`). El alto
de una caja que mide lo que su contenido no se conoce antes de maquetarla, así que un `Height: 50%` dentro de ella es un
error. Si necesitas el porcentaje, da un `Height` al contenedor.

## Cuatro lados

`Margin`, `Padding`, `Border` y `Offset` admiten de 1 a 4 valores, en el orden **arriba, derecha, abajo, izquierda**:

| Se escribe | Arriba | Derecha | Abajo | Izquierda |
|---|---|---|---|---|
| `10` | 10 | 10 | 10 | 10 |
| `10 20` | 10 | 20 | 10 | 20 |
| `10 20 5` | 10 | 20 | 5 | 20 |
| `10 20 5 0` | 10 | 20 | 5 | 0 |

`Border color` admite también de 1 a 4 colores, en el mismo orden. `Radius` admite de 1 a 4 valores, uno por **esquina**:
arriba a la izquierda, arriba a la derecha, abajo a la derecha y abajo a la izquierda.

## Colores

En hexadecimal: `#RGB` o `#RRGGBB` (`#000`, `#A32D2D`). Con un dígito o un par más, la **opacidad**: `#RGBA` o
`#RRGGBBAA` (`#12315A80` es azul a medias, `00` es transparente, `FF` opaco). Valen en el color del texto, del borde, del
fondo y de la línea entre columnas.

## Degradados

Un fondo puede ser un degradado: `linear(ángulo, color, color…)`. El ángulo está en grados: `0` va de abajo arriba, `90`
de izquierda a derecha y `180` de arriba abajo. Los colores se reparten a partes iguales y pueden tener opacidad:

```
Box:
	Background: linear(180, #12315A00, #12315A)
```

pinta un azul que aparece de arriba abajo, desde transparente.

## Otros

- **Booleanos:** `true` o `false`.
- **Enteros:** sin decimales (`Widows: 3`).
- **Listas de nombres:** separadas por espacios (`Class: note wide`).
