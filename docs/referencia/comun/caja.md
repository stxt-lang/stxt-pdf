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

> **Decisión:** lo que se hereda va en `Typography` y lo que no, en `Box` o `Break`: por eso `Background` y
> `Border color` van en `Box`, y `Widows` y `Orphans`, en `Typography`. *Alternativa descartada:* un grupo de pintado.

### Márgenes `auto`

`Margin: auto` solo tiene efecto donde centra, y en cualquier otro sitio es un error (`MARGIN_AUTO_WITHOUT_EFFECT`):

- **en flujo de bloque**, en horizontal y en una caja con `Width` o `Max width` (o una `Image`, que mide su ancho
  natural): con los dos márgenes `auto`, el sobrante se reparte entre ambos y la caja queda **centrada**; si solo uno es
  `auto`, absorbe el sobrante;
- **en un `Absolute` o un `Fixed`**, con inicio, tamaño y fin declarados ([posicionados](../componentes/relative-absolute-fixed.md#absolute)).

Los márgenes verticales `auto` en flujo de bloque y los márgenes `auto` de los ítems de `Flex` y `Grid` (que se alinean
con `Main axis`, `Cross axis`, `Horizontal` y `Vertical`) son errores.

> **Decisión:** un margen `auto` que no centraría nada es un error, no un 0 silencioso: con ancho `auto` la caja ocupa
> toda la línea y no hay sobrante que repartir.

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

> **Decisión:** el choque entre unidades distintas no es un error de layout: gana el mínimo.

### Proporción

Con `Aspect ratio` y un eje `auto`, ese eje se deduce del otro, sobre la caja de borde. Una `Image` tiene siempre
proporción: la de su fichero, o la de `Aspect ratio`, y en ella se aplica a la caja de contenido
([Image](../componentes/image.md#dimensionado)).

> **Decisión:** en una `Image`, la proporción es la de la caja de contenido, para que el relleno y el borde no deformen
> el dibujo; en las demás cajas, la de la caja de borde.

## Fondo, borde y esquinas

`Background`, `Border color` y `Radius` solo cambian el aspecto: ninguno mueve ni cambia el tamaño de una caja. El fondo
y el borde van debajo del contenido ([Capas y giro](capas-y-giro.md#orden-de-pintado)). Para quien lee el PDF con un
lector de pantalla son decoración ([El PDF](pdf.md#accesibilidad)).

### Esquinas

`Radius` redondea la caja de borde con un cuarto de elipse por esquina. Un porcentaje horizontal es del ancho de la caja
de borde y uno vertical, de su alto: `50%` hace un círculo en una caja cuadrada y una elipse en las demás. Si dos radios
seguidos de un lado suman más que el lado, todos se reducen por el mismo factor hasta que caben.

- **Dentro de la caja.** El fondo, que va en la caja de relleno, tiene los radios de la caja de borde menos el grosor del
  borde de cada lado, y una esquina que se queda sin radio es cuadrada. Una `Image` se recorta con los radios de su caja
  de contenido (la de borde menos el borde y el relleno).
- **Lo que contiene.** Una caja con esquinas redondeadas recorta por la curva de su caja de relleno los **fondos, los
  bordes y las imágenes** de lo que tiene en su flujo, a cualquier profundidad (también un `Relative` y un ítem con
  `Layer`): un hijo con fondo que llega a la esquina sigue la curva sin redondearlo. El **texto no se recorta**, ni un
  `Absolute` o un `Fixed`, que se colocan a propósito y pueden sobresalir (una insignia que asoma por la esquina). Varias
  cajas redondeadas una dentro de otra suman sus recortes. Como nada del flujo se sale de su caja (sería un
  [desbordamiento](desbordamiento.md)), el recorte solo cambia lo que toca las esquinas.
- **Cajas partidas.** Con `Decoration: slice`, solo el primer fragmento de una caja tiene las esquinas de arriba y solo
  el último, las de abajo; con `clone`, todos las tienen todas. Los radios en % se resuelven sobre el tamaño del
  fragmento, no sobre el de la caja entera.

> **Decisión:** la caja redondeada recorta siempre lo que contiene, sin propiedad que lo pida, porque lo que se sale de
> la curva nunca es lo que se quiere. *Alternativa descartada:* que no recorte y haya que redondear también cada hijo que
> toca una esquina; o una propiedad `Clip`.

> **Decisión:** el recorte no alcanza al texto: un texto cortado a medias se leería mal y no avisaría. Tampoco a un
> `Absolute`, que sale del flujo para colocarse donde quiera. *Alternativa descartada:* recortarlo todo, como una ventana.

> **Decisión:** un radio por esquina; la elipse se pide con %. Los % de una caja partida son de cada fragmento: uno muy
> bajo reduce sus radios. *Alternativa descartada:* dos radios por esquina (`10 / 20`).

### Borde

Es el anillo entre la caja de borde y la de relleno. Con un solo color se pinta de una vez; con colores distintos, cada
lado con su color, y dos lados seguidos se encuentran en la **diagonal** que une la esquina de la caja de borde con la de
la de relleno (con esquinas redondeadas, la diagonal sigue hacia dentro para cubrir la curva). Un lado sin grosor no
tiene color: `Border: 0 0 0 3` con `Border color: #B5651D` es una franja a la izquierda. El borde es siempre una línea
continua.

### Fondo

Un color, liso o con opacidad, o un degradado lineal. Se pinta en la caja de relleno, con sus esquinas.

#### Degradado

`linear(ángulo, …)` pasa por el centro de la caja de relleno y mide lo que hace falta para que las esquinas tengan el
primer y el último color (`|ancho · sen(ángulo)| + |alto · cos(ángulo)|`); más allá de los extremos se repite el color del
extremo. Cada color se pinta con su opacidad: un degradado hacia un color transparente deja ver lo que hay debajo.

### Opacidad

El color de un fondo, de un borde o de un texto con opacidad deja ver lo que hay debajo; en un borde de un solo color,
las esquinas no se pintan dos veces.

## Lo que no hay

- Sombras: el PDF no desenfoca y habría que aproximarlas con capas semitransparentes.
- Estilos de borde (punteado, doble…), varias capas de fondo ni imágenes de fondo.
- Recorte del contenido por el contorno de una caja cuadrada: lo que se sale de la caja es un
  [desbordamiento](desbordamiento.md); las esquinas redondeadas solo recortan los fondos, los bordes y las imágenes
  ([arriba](#esquinas)).
