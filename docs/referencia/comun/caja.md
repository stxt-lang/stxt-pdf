# La caja: `Box`

## Las cuatro cajas

Cada caja tiene cuatro rectángulos concéntricos: **contenido**, **relleno** (contenido + `Padding`), **borde**
(relleno + `Border`) y **margen** (borde + `Margin`).

`Width` y `Height` (y sus mínimos y máximos, y `Basis`) miden siempre la **caja de borde**: una caja con `Width: 200`
ocupa 200 pt antes de los márgenes, con el relleno y el borde dentro. No hay forma de que midan la caja de contenido.

## El contenedor de referencia

El **contenedor de referencia** de una caja es el rectángulo contra el que se resuelven sus porcentajes, sus `auto` y
su `Offset`:

| Caja | Contenedor de referencia |
|---|---|
| En flujo (también `Relative`) | la caja de **contenido** del padre |
| `Absolute` | la caja de **relleno** del elemento posicionado (`Relative`, `Absolute` o `Fixed`) más cercano entre sus ancestros; si no hay ninguno, el área útil |
| `Fixed` | el área útil de cada página |
| Ítem de `Grid` (y celda de `Table`) | su área: las pistas que ocupa y los huecos de dentro |

## El grupo `Box`

El modelo de caja. No se hereda.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Width` | número, %, `auto`, intrínsecos, funciones | `auto` | |
| `Height` | número, %, `auto`, intrínsecos, funciones | `auto` | |
| `Min width`, `Min height` | número, %, `auto` | `auto` | `auto` = min-content en ítems de `Flex` y `Grid`; 0 en el resto. |
| `Max width`, `Max height` | número, % | sin máximo | |
| `Aspect ratio` | `ancho/alto` o número | — | Si un eje es `auto` y el otro es conocido, se deduce. Ejemplo: `16/9`. Con `Width` y `Height` declarados es un error (`ASPECT_RATIO_CONFLICT`). |
| `Margin` | 1 a 4 valores (número, %, `auto`) | `0` | Puede ser negativo. `auto` solo centra (ver [márgenes `auto`](#márgenes-auto)). |
| `Padding` | 1 a 4 valores (número ≥ 0, % ≥ 0) | `0` | |
| `Border` | 1 a 4 números ≥ 0 | `0` | Grosor. |
| `Border color` | de 1 a 4 colores | el `Color` del texto | Arriba, derecha, abajo, izquierda ([Valores](valores.md#abreviados-de-cuatro-lados)). No afecta al layout. |
| `Radius` | de 1 a 4 valores (número ≥ 0, % ≥ 0) | `0` | Esquinas redondeadas de la caja de borde: arriba izquierda, arriba derecha, abajo derecha, abajo izquierda ([esquinas](#esquinas)). No afecta al layout. |
| `Background` | color o `linear(…)` | transparente | Se pinta en la caja de relleno ([fondo](#fondo-borde-y-esquinas)). No afecta al layout. |

> **Decisión:** lo que se hereda va en `Typography` y lo que no, en `Box` o `Break`. Por eso `Background` y
> `Border color` van en `Box` (no son geometría, pero no se heredan) y `Widows` y `Orphans` van en `Typography` (son de
> los párrafos y se heredan), no en `Break`. *Alternativa descartada:* un cuarto grupo de pintado, o `Widows` y
> `Orphans` en `Break`; en los dos casos un grupo mezclaría lo que se hereda con lo que no.

### Márgenes `auto`

`Margin: auto` solo tiene efecto donde centra, y en cualquier otro sitio es un error (`MARGIN_AUTO_WITHOUT_EFFECT`):

- **en flujo de bloque**, en horizontal y en una caja con `Width` o `Max width` (o una `Image`, que mide su ancho
  natural): con los dos márgenes `auto`, el sobrante se reparte entre ambos y la caja queda **centrada**; si solo uno es
  `auto`, absorbe el sobrante;
- **en un `Absolute` o un `Fixed`**, con inicio, tamaño y fin declarados ([posicionados](../componentes/relative-absolute-fixed.md#absolute)).

Los márgenes verticales `auto` en flujo de bloque y los márgenes `auto` de los ítems de `Flex` y `Grid` (que se alinean
con `Main axis`, `Cross axis`, `Horizontal` y `Vertical`) son errores.

> **Decisión:** un `Margin: auto` horizontal en flujo de bloque necesita `Width` o `Max width`: con ancho `auto` la caja
> ocupa toda la línea y los márgenes valdrían 0. Con el mismo criterio son errores los verticales en flujo de bloque, los
> de un `Absolute` sin inicio, tamaño y fin y los de los ítems de `Flex` y `Grid`: un margen `auto` que no centra no
> tendría efecto. *Alternativa descartada:* que valgan 0 en silencio.

## Dimensionado

### Tamaños intrínsecos

Los anchos de la caja de margen que una caja necesita sin que nada desborde (**min-content**) y sin ningún salto de
línea (**max-content**):

| Elemento | min-content | max-content |
|---|---|---|
| `Text` | palabra más ancha | párrafo más largo en una línea |
| `Image` | ancho natural o, con `Height` en puntos, el que sale de él con la proporción | igual |
| `Block`, `Relative`, `Absolute`, `Fixed` | máximo del min-content de los hijos | máximo del max-content de los hijos |
| `Flex` en `row`, sin `Wrap` | suma del min-content de los ítems + huecos | suma del max-content + huecos |
| `Flex` en `row`, con `Wrap` | máximo del min-content de los ítems | suma del max-content + huecos |
| `Flex` en `column` | máximo de los ítems | máximo de los ítems |
| `Grid` | suma de las columnas dimensionadas sin espacio libre, más los huecos | suma de las columnas dimensionadas con espacio infinito y cada ítem aportando su max-content también como mínimo, más los huecos |
| `Multicol` | ver [Multicol](../componentes/multicol.md#tamaños-intrínsecos) | |

Una caja con `Width` fijo tiene ese valor como min-content y max-content (el mínimo automático de un ítem de `Flex` mira
en cambio su contenido, [Flex](../componentes/flex.md#algoritmo)). `Absolute` y `Fixed` no aportan nada a sus ancestros.
En `Flex` se usan los tamaños intrínsecos de cada ítem tal cual, sin tener en cuenta `Basis`, `Grow` ni `Shrink`. En
`Grid`, el max-content hace que las fracciones lleguen al max-content de su contenido.

### Definido e indefinido

Un tamaño es **definido** si se conoce sin hacer el layout del contenido. Los anchos lo son siempre al maquetar; qué
altos lo son está en [Valores](valores.md#alto-definido), y un porcentaje de un alto que no lo es, un error. El área útil
es definida: una caja de primer nivel de una sección con `Height: 100%` ocupa la página.

### Mínimos y máximos

Se calcula el tamaño provisional, se limita por el máximo y se sube al mínimo. Si se pueden comparar al leer el
documento (los dos en puntos o los dos en porcentaje), un mínimo mayor que el máximo, o un `Width`/`Height` fuera de
ellos, es un error (`MIN_MAX_CONFLICT`). Si no se pueden comparar hasta maquetar (`Min width: 150` y `Max width: 20%`) y
chocan, **gana el mínimo**.

> **Decisión:** los mínimos y los máximos solo se comparan al leer si están en las mismas unidades (puntos con puntos,
> porcentaje con porcentaje); si no, al maquetar gana el mínimo, sin error. *Alternativa descartada:* compararlos
> también al maquetar y dar un error de layout.

### Proporción

Con `Aspect ratio` y un eje `auto`, ese eje se deduce del otro, sobre la caja de borde. Una `Image` tiene siempre
proporción: la de su fichero, o la de `Aspect ratio`, y en ella se aplica a la caja de contenido
([Image](../componentes/image.md#dimensionado)).

> **Decisión:** `Aspect ratio` se aplica a la caja de borde en todas las cajas salvo en una `Image`, donde se aplica a la
> caja de contenido para que el relleno y el borde no deformen el dibujo. *Alternativa descartada:* la caja de contenido
> también en las demás.

## Fondo, borde y esquinas

`Background`, `Border color` y `Radius` solo cambian el pintado: ninguno mueve ni cambia el tamaño de una caja. El fondo
y el borde se pintan en el paso de fondos de su contexto ([Capas y giro](capas-y-giro.md#orden-de-pintado)).

### Esquinas

`Radius` redondea la caja de borde con un cuarto de elipse por esquina. Un porcentaje horizontal es del ancho de la caja
de borde y uno vertical, de su alto: `50%` hace un círculo en una caja cuadrada y una elipse en las demás. Si dos radios
seguidos de un lado suman más que el lado, todos se reducen por el mismo factor hasta que caben.

- **Dentro de la caja.** El fondo, que va en la caja de relleno, tiene los radios de la caja de borde menos el grosor del
  borde de cada lado, y una esquina que se queda sin radio es cuadrada. Una `Image` se recorta con los radios de su caja
  de contenido (la de borde menos el borde y el relleno). Los hijos de una caja redondeada no se recortan: un hijo con
  fondo en la esquina se sale de la curva, y se redondea también.
- **Cajas partidas.** Con `Decoration: slice`, solo el primer fragmento de una caja tiene las esquinas de arriba y solo
  el último, las de abajo; con `clone`, todos las tienen todas. Los radios en % se resuelven sobre el tamaño del
  fragmento, no sobre el de la caja entera.

> **Decisión:** `Radius` es un radio por esquina, de 1 a 4 valores; la elipse se pide con %. *Alternativa descartada:*
> un solo valor para las cuatro esquinas, o dos radios por esquina (`10 / 20`).

> **Decisión:** los radios en % de una caja partida se resuelven sobre cada fragmento, no sobre la caja entera, que el
> pintado no conoce: un fragmento muy bajo reduce sus radios.

### Borde

Es el anillo entre la caja de borde y la de relleno. Con un solo color se pinta de una vez; con colores distintos, cada
lado con su color, y dos lados seguidos se encuentran en la **diagonal** que une la esquina de la caja de borde con la de
la de relleno (con esquinas redondeadas, la diagonal sigue hacia dentro para cubrir la curva). Un lado sin grosor no
tiene color: `Border: 0 0 0 3` con `Border color: #B5651D` es una franja a la izquierda. El borde es siempre una línea
continua.

> **Decisión:** `Border color` admite de 1 a 4 colores (arriba, derecha, abajo, izquierda), sin propiedades por lado.
> *Alternativa descartada:* `Border top color` y las demás, cuatro nombres más en el schema.

### Fondo

Un color, liso o con opacidad, o un degradado lineal. Se pinta en la caja de relleno, con sus esquinas.

#### Degradado

`linear(ángulo, …)` pasa por el centro de la caja de relleno y mide lo que hace falta para que las esquinas tengan el
primer y el último color (`|ancho · sen(ángulo)| + |alto · cos(ángulo)|`); más allá de los extremos se repite el color del
extremo. Cada color se pinta con su opacidad: un degradado hacia un color transparente deja ver lo que hay debajo.

### Opacidad

El color de un fondo, de un borde o de un texto con opacidad deja ver lo que hay debajo; en un borde de un solo color,
las esquinas no se pintan dos veces.

### PDF etiquetado

Fondos, bordes y degradados son *artifacts*: no forman parte del contenido ([El PDF](pdf.md#pdf-etiquetado)).

## Lo que no hay

- Sombras.
- Estilos de borde (punteado, doble…), varias capas de fondo ni imágenes de fondo.
- Recorte del contenido por el contorno de la caja: lo que se sale de la caja es un
  [desbordamiento](desbordamiento.md), y las esquinas redondeadas solo recortan una `Image`.

> **Decisión:** no hay sombras (`Shadow`): PDFBox no desenfoca y habría que aproximarlas con capas semitransparentes, lo
> más caro de pintar y lo que menos aporta. *Alternativa descartada:* `Shadow: x y desenfoque color`, exterior y única,
> con capas.
