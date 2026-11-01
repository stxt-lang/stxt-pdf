# La caja: `Box`

## Las cuatro cajas

Cada caja tiene cuatro rectángulos concéntricos: **contenido**, **relleno** (contenido + `Padding`), **borde**
(relleno + `Border`) y **margen** (borde + `Margin`).

`Width` y `Height` (y sus mínimos y máximos, y `Base size`) miden siempre la **caja de borde**: una caja con `Width: 200`
ocupa 200 pt antes de los márgenes, con el relleno y el borde dentro. No hay forma de que midan la caja de contenido.

## El contenedor de referencia

El **contenedor de referencia** de una caja es el rectángulo contra el que se resuelven sus porcentajes, sus `auto` y
su `Offset`:

| Caja | Contenedor de referencia |
|---|---|
| En flujo (también `Frame`) | la caja de **contenido** del padre |
| `Overlay` | la caja de **relleno** del elemento posicionado (`Frame`, `Overlay` o `Page overlay`) más cercano entre sus ancestros, o el área útil si no hay ninguno |
| `Page overlay` | el área útil de cada página |
| Ítem de `Grid` (y celda de `Table`) | su área: las pistas que ocupa y los huecos de dentro |

## El grupo `Box`

El modelo de caja. No se hereda.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Width` | número, %, `auto`, `narrowest`, `natural`, `fit` | `auto` | |
| `Height` | número, %, `auto` | `auto` | `auto` es el alto del contenido. |
| `Min width`, `Min height` | número, %, `auto` | `auto` | `auto` = ancho más estrecho en ítems de `Stack` y `Grid`, y 0 en el resto. |
| `Max width`, `Max height` | número, % | sin máximo | |
| `Aspect ratio` | `ancho/alto` o número | sin valor | Si un eje es `auto` y el otro es conocido, se deduce. Ejemplo: `16/9`. Con `Width` y `Height` declarados es un error (`ASPECT_RATIO_CONFLICT`). |
| `Margin` | 1 a 4 valores (número, %, `auto`) | `0` | Puede ser negativo, sin sacar la caja de la página. `auto` solo centra (ver [márgenes `auto`](#márgenes-auto)). |
| `Padding` | 1 a 4 valores (número ≥ 0, % ≥ 0) | `0` | |
| `Border` | 1 a 4 números ≥ 0 | `0` | Grosor. |
| `Border color` | de 1 a 4 colores | el `Color` del texto | Arriba, derecha, abajo, izquierda ([Valores](values.md#abreviados-de-cuatro-lados)). No afecta al layout. |
| `Corner radius` | de 1 a 4 valores (número ≥ 0, % ≥ 0) | `0` | Esquinas redondeadas de la caja de borde: arriba izquierda, arriba derecha, abajo derecha, abajo izquierda ([esquinas](#esquinas)). No afecta al layout. |
| `Background` | color o `linear(...)` | transparente | Se pinta en la caja de relleno ([fondo](#fondo-borde-y-esquinas)). No afecta al layout. |
| `Overflow` | `error`, `shrink`, `clip` | `error` | Qué se hace con lo que no cabe en la caja ([Desbordamiento](overflow.md#política-por-caja-overflow)). |

> **Decisión:** `narrowest`, `natural` y `fit` solo valen en `Width` (y en las pistas de `Grid` y `Table`): un alto
> `auto` ya es el de su contenido, y en `Height` serían otra forma de escribir `auto`. *Alternativa descartada:*
> admitirlas también en `Height`, como sinónimos de `auto`.

> **Decisión:** lo que se hereda va en `Typography` y lo que no, en `Box` o `Break`: por eso `Background` y
> `Border color` van en `Box`, y `Widows` y `Orphans`, en `Typography`. *Alternativa descartada:* un grupo de pintado.

### Márgenes `auto`

`Margin: auto` solo tiene efecto donde centra, y en cualquier otro sitio es un error (`MARGIN_AUTO_WITHOUT_EFFECT`):

- **en flujo de bloque**, en horizontal y en una caja con `Width` o `Max width` (o una `Image`, que mide su ancho
  natural): con los dos márgenes `auto`, el sobrante se reparte entre ambos y la caja queda **centrada**, y si solo uno es
  `auto`, ese absorbe el sobrante;
- **en un `Overlay` o un `Page overlay`**, con inicio, tamaño y fin declarados ([posicionados](../components/frame-and-overlays.md#overlay)).

Los márgenes verticales `auto` en flujo de bloque y los márgenes `auto` de los ítems de `Stack` y `Grid` (que se alinean
con `Horizontal` y `Vertical`) son errores.

> **Decisión:** un margen `auto` que no centraría nada es un error y no un 0 silencioso: con ancho `auto` la caja ocupa
> toda la línea y no hay sobrante que repartir.

## Dimensionado

### Tamaños intrínsecos

Los anchos de la caja de margen que una caja necesita sin que nada desborde (**ancho más estrecho**) y sin ningún salto de
línea (**ancho natural**):

| Elemento | Ancho más estrecho | Ancho natural |
|---|---|---|
| `Text` | palabra más ancha | párrafo más largo en una línea |
| `Image`, `Barcode` | ancho natural o, con un `Height` que es una medida, el que sale de él con la proporción | igual |
| `Block`, `Frame`, `Overlay`, `Page overlay` | máximo del ancho más estrecho de los hijos | máximo del ancho natural de los hijos |
| `Stack` horizontal, sin `Wrap` | suma del ancho más estrecho de los ítems + huecos | suma del ancho natural + huecos |
| `Stack` horizontal, con `Wrap` | máximo del ancho más estrecho de los ítems | suma del ancho natural + huecos |
| `Stack` vertical | máximo de los ítems | máximo de los ítems |
| `Grid` | suma de las columnas dimensionadas sin espacio libre, más los huecos | suma de las columnas dimensionadas con espacio infinito y cada ítem aportando su ancho natural también como mínimo, más los huecos |
| `Columns` | ver [Columns](../components/columns.md#tamaños-intrínsecos) | |

Una caja con `Width` fijo tiene ese valor como ancho más estrecho y ancho natural (el mínimo automático de un ítem de `Stack` mira
en cambio su contenido, [Stack](../components/stack.md#algoritmo)). `Overlay` y `Page overlay` no aportan nada a sus ancestros.
En `Stack` se usan los tamaños intrínsecos de cada ítem tal cual, sin tener en cuenta `Base size`, `Grow` ni `Shrink`. En
`Grid`, el ancho natural hace que las fracciones lleguen al ancho natural de su contenido.

### Definido e indefinido

Un tamaño es **definido** si se conoce sin hacer el layout del contenido. Los anchos lo son siempre al maquetar. Qué
altos lo son está en [Valores](values.md#alto-definido), y un porcentaje de un alto indefinido es un error. El área útil
es definida: una caja de primer nivel de una sección con `Height: 100%` ocupa la página.

### Mínimos y máximos

Se calcula el tamaño provisional, se limita por el máximo y se sube al mínimo. Si se pueden comparar al leer el
documento (los dos medidas o los dos porcentajes), un mínimo mayor que el máximo, o un `Width`/`Height` fuera de
ellos, es un error (`MIN_MAX_CONFLICT`). Si no se pueden comparar hasta maquetar (`Min width: 150` y `Max width: 20%`) y
chocan, **gana el mínimo**.

> **Decisión:** el choque entre unidades distintas no es un error de layout: gana el mínimo.

### Proporción

Con `Aspect ratio` y un eje `auto`, ese eje se deduce del otro, sobre la caja de borde. Una `Image` tiene siempre
proporción: la de su fichero, o la de `Aspect ratio`, y en ella se aplica a la caja de contenido
([Image](../components/image.md#dimensionado)).

> **Decisión:** en una `Image`, la proporción es la de la caja de contenido, para que el relleno y el borde no deformen
> el dibujo. En las demás cajas, la de la caja de borde.

## Fondo, borde y esquinas

`Background`, `Border color` y `Corner radius` solo cambian el aspecto: ninguno mueve ni cambia el tamaño de una caja. El fondo
y el borde van debajo del contenido ([Capas y giro](layers-and-rotation.md#orden-de-pintado)). Para quien lee el PDF con un
lector de pantalla son decoración ([El PDF](pdf.md#accesibilidad)).

### Esquinas

`Corner radius` redondea la caja de borde con un cuarto de elipse por esquina. Un porcentaje horizontal es del ancho de la caja
de borde y uno vertical, de su alto: `50%` hace un círculo en una caja cuadrada y una elipse en las demás. Si dos radios
seguidos de un lado suman más que el lado, todos se reducen por el mismo factor hasta que caben.

- **Dentro de la caja.** El fondo, que va en la caja de relleno, tiene los radios de la caja de borde menos el grosor del
  borde de cada lado, y una esquina que se queda sin radio es cuadrada. Una `Image` o un `Barcode` se recorta con los
  radios de su caja de contenido (la de borde menos el borde y el relleno).
- **Lo que contiene.** Una caja con esquinas redondeadas recorta por la curva de su caja de relleno los **fondos, los
  bordes y las imágenes** de lo que tiene en su flujo, a cualquier profundidad (también un `Frame` y un ítem con
  `Layer`): un hijo con fondo que llega a la esquina sigue la curva sin redondearlo. El **texto no se recorta**, ni un
  `Overlay` o un `Page overlay`, que se colocan a propósito y pueden sobresalir (una insignia que asoma por la esquina). Varias
  cajas redondeadas una dentro de otra suman sus recortes. Como nada del flujo se sale de su caja (sería un
  [desbordamiento](overflow.md)), el recorte solo cambia lo que toca las esquinas.
- **Cajas partidas.** Con `Edges: open`, solo el primer fragmento de una caja tiene las esquinas de arriba y solo
  el último, las de abajo. Con `closed`, todos las tienen todas. Los radios en % se resuelven sobre el tamaño del
  fragmento y no sobre el de la caja entera.

> **Decisión:** la caja redondeada recorta siempre lo que contiene, sin propiedad que lo pida, porque lo que se sale de
> la curva nunca es lo que se quiere. *Alternativa descartada:* que no recorte y haya que redondear también cada hijo que
> toca una esquina, o una propiedad `Clip`.

> **Decisión:** el recorte no alcanza al texto: un texto cortado a medias se leería mal y no avisaría. Tampoco a un
> `Overlay`, que sale del flujo para colocarse donde quiera. *Alternativa descartada:* recortarlo todo, como una ventana.

> **Decisión:** un radio por esquina, y la elipse se pide con %. Los % de una caja partida son de cada fragmento: uno muy
> bajo reduce sus radios. *Alternativa descartada:* dos radios por esquina (`10 / 20`).

### Borde

Es el anillo entre la caja de borde y la de relleno. Con un solo color se pinta de una vez. Con colores distintos, cada
lado va con su color, y dos lados seguidos se encuentran en la **diagonal** que une la esquina de la caja de borde con la de
la de relleno (con esquinas redondeadas, la diagonal sigue hacia dentro para cubrir la curva). Un lado sin grosor no
tiene color: `Border: 0 0 0 3` con `Border color: #B5651D` es una franja a la izquierda. El borde es siempre una línea
continua.

### Fondo

Un color, liso o con opacidad, o un degradado lineal. Se pinta en la caja de relleno, con sus esquinas.

#### Degradado

`linear(ángulo, ...)` pasa por el centro de la caja de relleno y mide lo que hace falta para que las esquinas tengan el
primer y el último color (`|ancho · sen(ángulo)| + |alto · cos(ángulo)|`). Más allá de los extremos se repite el color del
extremo. Cada color se pinta con su opacidad: un degradado hacia un color transparente deja ver lo que hay debajo.

### Opacidad

El color de un fondo, de un borde o de un texto con opacidad deja ver lo que hay debajo. En un borde de un solo color,
las esquinas no se pintan dos veces.

## Lo que no hay

- Sombras: el PDF no desenfoca y habría que aproximarlas con capas semitransparentes.
- Estilos de borde (punteado o doble), varias capas de fondo ni imágenes de fondo.
