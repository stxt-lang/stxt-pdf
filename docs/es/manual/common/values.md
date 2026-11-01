# Valores

## Medidas

| Se escribe | Significa | Ejemplo |
|---|---|---|
| un número | puntos, con punto decimal y nunca coma | `Width: 120`, `Size: 10.5` |
| un número con `mm` | milímetros, con la unidad pegada al número | `Width: 42mm`, `Margin: 12.5mm` |
| un porcentaje | una parte de otra medida (abajo) | `Width: 50%` |
| `auto` | lo calcula el motor | `Width: auto` |
| `narrowest` | lo más estrecho que puede ser sin que nada se salga: la palabra más larga | `Width: narrowest` |
| `natural` | lo que mide sin partir ninguna línea | `Width: natural` |
| `fit` | lo que necesita su contenido, sin pasar del espacio disponible | `Width: fit` |

Puntos y milímetros se pueden mezclar, también en un mismo valor (`Margin: 20mm 40`). No hay otras unidades: `5cm`,
`12pt` o `12 mm`, con espacio, son un error. `Grow`, `Shrink`, los grados de `Rotate` y `Aspect ratio` no son medidas y
no llevan unidad.

`narrowest`, `natural` y `fit` son anchos: valen en `Width` y en las columnas y filas de un `Grid`. En `Height` no, porque
`auto` ya es el alto del contenido. Cada propiedad admite solo algunas de estas formas, y su tabla dice cuáles. Cualquier
otra es un error. Solo los márgenes, los desplazamientos de las cajas posicionadas y el interletraje pueden ser negativos. Un tamaño entre dos límites se escribe
con su mínimo y su máximo: `Width: 50%`, `Min width: 200` y `Max width: 400`.

Las columnas de un `Grid` admiten además **fracciones** (`*`, `2*`: partes del espacio que sobra), `between(a, b)` y
`repeat(n, ...)` ([Grid](../components/grid.md)).

## Porcentajes

Un porcentaje es una parte de otra medida, casi siempre de su contenedor de referencia ([Conceptos](concepts.md)):

- **Anchos, márgenes y relleno** (también arriba y abajo): del **ancho** del contenedor.
- **Altos**: del **alto** del contenedor, que tiene que conocerse antes de maquetar lo de dentro.
- **`Line height`**: del tamaño de la fuente de la propia caja.

**Un alto que se conoce** es el del área útil de la página, el de una caja con `Height` en puntos o en milímetros (o en
porcentaje de otro alto que se conoce) y el de algunas cajas que se estiran (un `Overlay` anclado arriba y abajo, una
celda de `Grid`). El alto de una caja que mide lo que su contenido no se conoce antes de maquetarla, así que un
`Height: 50%` dentro de ella es un error. Para usar el porcentaje, el contenedor necesita un `Height`.

## Cuatro lados

`Margin`, `Padding`, `Border` y `Offset` admiten de 1 a 4 valores, en el orden **arriba, derecha, abajo, izquierda**:

| Se escribe | Arriba | Derecha | Abajo | Izquierda |
|---|---|---|---|---|
| `10` | 10 | 10 | 10 | 10 |
| `10 20` | 10 | 20 | 10 | 20 |
| `10 20 5` | 10 | 20 | 5 | 20 |
| `10 20 5 0` | 10 | 20 | 5 | 0 |

`Border color` admite también de 1 a 4 colores, en el mismo orden. `Corner radius` admite de 1 a 4 valores, uno por **esquina**:
arriba a la izquierda, arriba a la derecha, abajo a la derecha y abajo a la izquierda.

## Colores

En hexadecimal: `#RGB` o `#RRGGBB` (`#000`, `#A32D2D`). Con un dígito o un par más, la **opacidad**: `#RGBA` o
`#RRGGBBAA` (`#12315A80` es azul a medias, `00` es transparente, `FF` opaco). Valen en el color del texto, del borde, del
fondo y de la línea entre columnas.

Un color que se repite se puede declarar una vez con nombre en `Styles` (`Named color: brand #12315A`) y escribir
después `Color: brand` ([Colores con nombre](styles.md#colores-con-nombre)).

## Degradados

Un fondo puede ser un degradado: `linear(ángulo, color, color, ...)`. El ángulo está en grados: `0` va de abajo arriba,
`90` de izquierda a derecha y `180` de arriba abajo. Los colores se reparten a partes iguales y pueden tener opacidad. Este
pinta un azul que aparece de arriba abajo, desde transparente:

```
Box:
    Background: linear(180, #12315A00, #12315A)
```

## Otros

- **Booleanos:** `true` o `false`.
- **Enteros:** sin decimales (`Widows: 3`).
- **Listas de nombres:** separadas por espacios (`Style: note wide`).
