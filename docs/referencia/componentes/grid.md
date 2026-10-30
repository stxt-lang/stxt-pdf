# `Grid` y `Area`

En un `Grid`, cuyos ítems son el elemento de cada `Area`, el motor resuelve primero **dónde** va cada ítem y después
**cuánto** mide cada pista (cada columna y cada fila). Cada hijo en flujo es un `Area`; además admite `Overlay` y `Page overlay`.
El `Grid` admite `Id`, `Style`, `Link`, `Box`, `Typography` y `Break`, y va donde va un elemento en flujo.

```
Grid:
	Column sizes: 1fr 1fr
	Area: 1 2
		Text: Fila 1, columna 2
	Area:
		Text: Se coloca automáticamente
```

## Propiedades del `Grid`

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Column sizes` | lista de pistas | una columna `auto` | Ver [dimensionado de pistas](#dimensionado-de-pistas). |
| `Row sizes` | lista de pistas | filas implícitas `auto` | |
| `Gap` | 1 o 2 valores (número, %) | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Areas` | bloque de texto | — | Una fila por línea; nombres separados por espacios; `.` = celda vacía. |
| `Flow` | `horizontal`, `vertical` | `horizontal` | Dirección de la colocación automática: `horizontal` llena las filas una a una; `vertical`, las columnas. |
| `Horizontal` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación horizontal de los ítems en su área. |
| `Vertical` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación vertical de los ítems en su área. |

Pistas válidas: número, %, `auto`, `min-content`, `max-content`, fracción, `minmax(a, b)` y `repeat(n, patrón)`.
`fit-content` no es una pista (`FIT_CONTENT_TRACK`), porque `auto` ya se ajusta al contenido.

## `Area`

Contiene **exactamente un elemento en flujo** (`SINGLE_ELEMENT`), que es el ítem; el `Area` solo dice dónde va y cómo se
alinea; no es una caja y no admite `Id`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| *(valor)* | área o `fila columna [filas columnas]` | `auto` | Dónde va, contando desde 1. Sin valor, se coloca automáticamente. |
| `Horizontal` | como en `Grid` | el del contenedor | |
| `Vertical` | como en `Grid` | el del contenedor | |
| `Layer` | entero | `auto` | Orden de apilamiento del ítem ([Capas y giro](../comun/capas-y-giro.md)). |

**Valor de `Area`:** un nombre de área (`Area: side`) o `fila columna [filas columnas]` contando desde 1, donde fila y
columna pueden ser `auto`: `Area: 1 1 1 2` ocupa la fila 1 y las columnas 1 y 2; `Area: auto auto 1 2` se coloca
automáticamente ocupando dos columnas. `Area:` sin valor equivale a `Area: auto auto`.

## Colocación

1. Se construye la rejilla explícita con `Column sizes`, `Row sizes` y `Areas`. Cada nombre de `Areas` debe formar un rectángulo y
   todas las filas deben tener el mismo número de celdas.
2. Se colocan los ítems cuyo `Area` tiene fila y columna, o nombre de área.
3. Se colocan los demás en orden del árbol con un **cursor** que recorre la rejilla por filas (o por columnas, con
   `Flow: vertical`). Cada ítem va al primer hueco donde quepa. El cursor solo avanza, de modo que pueden quedar huecos atrás.
4. Si un ítem cae fuera de la rejilla declarada, se crean pistas implícitas `auto`.

Un ítem con **solo la fila fija** (`Area: 2 auto`) va a la primera columna de esa fila donde quepa, sin mover el cursor; si
no queda ninguna, es un error (`GRID_PLACEMENT`): la fila no crea columnas implícitas. Uno con **solo la columna fija**
(`Area: auto 2`) va a la primera fila libre desde el cursor, en la siguiente si la columna queda detrás de él, y mueve el
cursor detrás de sí. Con `Flow: vertical` se intercambian filas y columnas. Estos ítems se colocan en el paso 3, en orden del
árbol con los automáticos. Un nombre de área que no existe o unas `Areas` que no forman rectángulos también son
`GRID_PLACEMENT`.

> **Decisión:** un ítem con la fila fija y sin hueco en ella es un error, con la línea del `Area`; la rejilla no crea
> columnas implícitas a mitad de la colocación, con los automáticos anteriores ya colocados con menos columnas. Los ítems
> con solo la fila o solo la columna fija se colocan en orden del árbol con los automáticos, no antes, y el de fila fija
> toma la primera columna libre de su fila. *Alternativa descartada:* crear la columna implícita.

## Dimensionado de pistas

Cada pista tiene una función mínima y una máxima: `150` es fija en ambas; `auto` equivale a
`minmax(min-content, max-content)`; `1fr` equivale a `minmax(auto, 1fr)`, de modo que **una fracción no baja del
min-content de su contenido**.

1. **Inicializar:** cada pista recibe una base (su mínimo) y un límite (su máximo; infinito en las fracciones).
2. **Aportaciones del contenido:** las pistas intrínsecas toman el min-content o el max-content de sus ítems; el exceso de
   los ítems que ocupan varias pistas se reparte entre las pistas intrínsecas que ocupan: primero lo que le falta hasta su
   min-content, a las bases, y después, contando las bases ya subidas, lo que le falta hasta su max-content, a los límites
   (el área del ítem no pasa de su max-content por repartirlo). Si alguna de ellas es una fracción, lo que le falta al ítem
   hasta su min-content se reparte solo entre las fracciones, en proporción a su `fr`: un ítem que cruza columnas `1fr` no
   desborda su área.
3. **Maximizar:** el espacio libre se reparte a partes iguales entre las pistas no flexibles hasta su límite. Una pista
   `auto` alcanza su max-content antes de que las fracciones reciban nada.
4. **Fracciones:** el resto se divide entre las fracciones de forma proporcional. Si una fracción necesita más que su parte
   por su min-content, se fija en su base y se repite el reparto.
5. **Estirar:** si no hay fracciones y sobra espacio, se reparte entre las pistas `auto`.

Las **columnas** se dimensionan primero, con los anchos min-content y max-content de los ítems. Las **filas** después, con
la altura de cada ítem maquetado al ancho de sus columnas. Con `Height: auto` en el contenedor, las filas en fracción no
tienen espacio que repartir y miden en proporción a su contenido: todas las filas `1fr` miden lo que la más alta.

Los porcentajes de las pistas son de la caja de contenido del contenedor, sin descontar los huecos
([Valores](../comun/valores.md#bases-de-los-porcentajes)). Las filas en porcentaje necesitan un alto definido en el
contenedor; las columnas en porcentaje se comportan como `auto` solo al calcular los tamaños intrínsecos del contenedor,
cuando todavía no hay ancho. Los márgenes `auto` de un ítem son un error: la alineación se hace con `Horizontal` y
`Vertical`.

> **Decisión:** lo que le falta a un ítem que cruza alguna fracción solo lo reciben las fracciones, en proporción a su
> `fr`: así un ítem que cruza columnas `1fr` no desborda su área.

> **Decisión:** el max-content se reparte contando las bases ya subidas: con columnas `50 auto 1fr`, hueco 10 y un ítem de
> max-content 400 en las dos primeras, la `auto` mide 340 y el área del ítem, 400 (y no 480 y 540).

## Alineación en el área

Cada ítem ocupa un área formada por sus pistas y los huecos internos. Con `stretch` (por defecto) y sin `Width`/`Height`
declarado, ocupa toda el área y se repite su layout. Con `start`, `end` o `center`, conserva su tamaño de contenido y se
desplaza. Los porcentajes de alto de un ítem se resuelven contra el alto de su área en el layout final; al dimensionar las
filas, todavía sin área, cuentan como `auto`. Un ítem estirado en vertical tiene un alto definido
([Valores](../comun/valores.md#alto-definido)).

## Tamaños intrínsecos

El min-content de un `Grid` es la suma de las columnas dimensionadas sin espacio libre, más los huecos; el max-content, la
suma de las columnas dimensionadas con espacio infinito y cada ítem aportando su max-content también como mínimo, más los
huecos ([La caja](../comun/caja.md#tamaños-intrínsecos)).

## Cortes

Un `Grid` se corta entre filas; una fila que no cabe en una página entera se parte con sus ítems en paralelo, y las filas
que ocupa un mismo ítem se parten juntas ([Cortes](../comun/cortes.md#cortes-en-flex-grid-y-table)).

## Posicionadas

La posición estática de un `Overlay` o un `Page overlay` hijo de un `Grid` es la esquina superior izquierda de su caja de
contenido ([posicionados](frame-and-overlays.md#overlay)).

## Lo que no hay

- Colocación densa (que vuelva atrás a llenar huecos), rejillas anidadas que compartan pistas con su padre y `fit-content`
  como pista.
- Márgenes `auto` en los ítems: son errores.
