# `Stack` e `Item`

Un `Stack` coloca sus ítems en líneas: uno tras otro en su **eje principal**, el horizontal o el vertical según
`Direction`, y alineados en el **eje cruzado**, el otro. Cada hijo en flujo es un `Item`, que envuelve al elemento que hace de
ítem. Además admite `Overlay` y `Page overlay`.
El `Stack` admite `Id`, `Style`, `Link`, `Box`, `Typography` y `Break`, y va donde va un elemento en flujo. A
diferencia de un `Block`, que apila sus hijos en vertical con su ancho, un `Stack` reparte entre sus ítems el espacio
que sobra o que falta.

> **Decisión:** el elemento se llama `Stack`, como en las herramientas de diseño, y sus nombres hablan de maquetación:
> `Base size` es el tamaño de partida de un ítem, `Line distribution` el reparto de las líneas, y `justify` y
> `distribute` los repartos que dejan los extremos en los bordes o todos los huecos iguales. *Alternativa descartada:*
> `Flex`, `Basis`, `Lines`, `between` y `evenly`, que suenan a un lenguaje de estilos y no a maquetación.

```
Stack:
    Gap: 10
    Item:
        Grow: 1
        Text: Crece
    Item:
        Block:
            Box:
                Width: 100
```

## Propiedades del `Stack`

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Direction` | `horizontal`, `vertical` | `horizontal` | El eje principal: en `horizontal` los ítems van uno al lado de otro, y en `vertical`, uno debajo de otro. |
| `Wrap items` | booleano | `false` | Permite repartir los ítems en varias líneas. |
| `Gap` | 1 o 2 valores (número, %) | `0` | Un valor para ambos ejes, o `filas columnas`, solo con `Wrap items: true` (`GAP_WITHOUT_WRAP`). |
| `Horizontal` | en el eje principal: `left`, `right`, `center`, `justify`, `distribute`; en el cruzado: `left`, `right`, `center`, `stretch` | `left` en el principal; `stretch` en el cruzado | Con `Direction: horizontal`, el reparto del sobrante. Con `vertical`, la alineación de los ítems dentro de su línea. |
| `Vertical` | como `Horizontal`, con `top` y `bottom` en lugar de `left` y `right`; en el cruzado, además, `baseline` | `top` en el principal; `stretch` en el cruzado | Con `Direction: vertical`, el reparto del sobrante. Con `horizontal`, la alineación de los ítems dentro de su línea: `baseline` los alinea por la línea base de su texto ([abajo](#alineación-por-línea-base)). |
| `Line distribution` | `top`, `bottom` (en horizontal) o `left`, `right` (en vertical), `center`, `justify`, `distribute`, `stretch` | `stretch` | Reparto de las líneas en el contenedor. Solo con `Wrap items: true` (`LINE_DISTRIBUTION_WITHOUT_WRAP`). |
| `Split items` | booleano | `true` | Una línea (en vertical, un ítem) que no cabe en lo que queda de página se parte en paralelo en esa página; con `false`, pasa entera a la siguiente y solo se parte si tampoco cabe en una página entera ([Cortes](#cortes)). En un `Stack` vertical con `Wrap items`, que no se parte nunca, es un error (`SPLIT_ITEMS_WITHOUT_EFFECT`). |

Todas pueden venir también de una regla de `Styles` ([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)).

> **Decisión:** los ejes se nombran por su dirección en la página, `Horizontal` y `Vertical`, como en `Grid`, y no por su
> papel. Qué hace cada uno lo dice `Direction`: el del eje principal reparte el sobrante y el del cruzado alinea los ítems,
> y un valor que no es de su papel es un error (`VALUE_NOT_ALLOWED`): `stretch` en el principal, y `justify` o
> `distribute` en el cruzado. *Alternativa descartada:* `Main axis` y `Cross axis`, que no cambian de sentido con `Direction`
> pero obligan a pensar en ejes abstractos.

> **Decisión:** sin `Wrap items` hay una sola línea, así que de un `Gap` de dos valores solo actuaría uno, el hueco entre
> ítems (el de las columnas en horizontal, el de las filas en vertical): es un error (`GAP_WITHOUT_WRAP`), como `Line distribution` sin
> `Wrap items`. Un solo valor sí vale, porque es el hueco entre ítems y, si hay varias líneas, también entre líneas. Por eso
> `Gap: 5%` en un `Stack` horizontal de una línea sin alto definido no es un error: el hueco entre filas, que necesitaría ese
> alto, no se usa. *Alternativa descartada:* aceptar los dos valores y no usar el que sobra, que deja en el documento una
> medida sin efecto.

## `Item`

Contiene **exactamente un elemento en flujo** (`SINGLE_ELEMENT`), que es el ítem: su `Box`, su `Typography`, su `Id` y
demás propiedades van en ese elemento. El `Item` solo añade cómo participa en el `Stack`. No es una caja y no admite `Id`
([Estructura](../common/structure.md#elementos)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Grow` | número ≥ 0 | `0` | Parte del espacio sobrante que recibe. |
| `Shrink` | número ≥ 0 | `1` | Parte del déficit que cede, ponderada por `Base size`. |
| `Base size` | número, %, `auto` | `auto` | Tamaño de partida en el eje principal. |
| `Horizontal`, `Vertical` | `left` o `top`, `right` o `bottom`, `center`, `stretch`; en `Vertical`, además, `baseline` | el del contenedor | La alineación del ítem en el eje cruzado, que sobrescribe la del contenedor: `Vertical` en un `Stack` horizontal, `Horizontal` en uno vertical. La del eje principal es un error (`ITEM_MAIN_AXIS`): ahí reparte el `Stack`, y el ítem crece o encoge con `Grow` y `Shrink`. |
| `Layer` | entero | `auto` | Orden de apilamiento del ítem ([Capas y giro](../common/layers-and-rotation.md)). |

`Grow`, `Shrink` y `Base size` también pueden venir de una regla de `Styles` del elemento que el `Item` envuelve, y lo
que declara el `Item` gana a la regla ([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)). La
alineación del ítem (`Horizontal`, `Vertical`) y `Layer`, no: en una regla, `Horizontal` y `Vertical` son las del
`Stack`.

## Algoritmo

1. Espacio disponible: la caja de contenido del contenedor en el eje principal.
2. **Base** de cada ítem: `Base size` si tiene valor. Si es `auto`, el tamaño declarado en el eje principal, y si tampoco
   lo hay, su ancho natural.
3. **Tamaño hipotético:** la base limitada por el mínimo y el máximo del ítem. Con `Min width: auto` (`Min height: auto` en
   vertical), el mínimo es el **tamaño de su contenido**: el ancho más estrecho en horizontal y el alto del contenido en vertical, sin
   pasar del máximo del ítem y nunca mayor que su `Width` (o `Height`) declarado.
4. **Líneas:** sin `Wrap items`, una sola. Con `Wrap items`, los ítems se añaden a la línea mientras sus tamaños hipotéticos y los
   huecos quepan.
5. **Tamaños flexibles** (por línea), ver abajo.
6. **Tamaño cruzado:** layout de cada ítem con su tamaño principal final (en horizontal, el texto se redistribuye con el ancho
   final). La línea mide lo que el ítem más alto, o más si tiene ítems con `baseline`
   ([abajo](#alineación-por-línea-base)). Sin `Wrap items` y con tamaño cruzado definido en el contenedor, la única línea
   mide lo que el contenedor.
7. **Estirar:** los ítems con `stretch` y sin tamaño cruzado declarado pasan a medir lo que la línea, y se repite su layout.
   En vertical, si el contenedor tiene una sola línea, estos ítems se miden desde el principio con el ancho del contenedor.
   Con `Wrap items`, se miden con su ajuste al contenido y después se estiran al ancho de su línea sin recalcular su alto.
8. **Alinear** en el eje principal, en el cruzado y las líneas en el contenedor.

Con tamaño cruzado `auto`, el contenedor mide la suma de sus líneas más los huecos.

### Reparto del espacio

Espacio libre = disponible − suma de tamaños hipotéticos − huecos.

- **Positivo:** cada ítem recibe una parte proporcional a su `Grow`.
- **Negativo:** cada ítem cede una parte proporcional a **`Shrink × Base size`**, sin sus márgenes.
- Un ítem que choca con su mínimo o máximo se **congela** en ese límite y el reparto se repite entre los demás.
- Si la suma de factores de los ítems no congelados es menor que 1, solo se reparte esa fracción del espacio libre.

> **Decisión:** los márgenes no cuentan al encoger: dos ítems con `Base size: 300`, uno con 10 de margen a cada lado, en 515,
> ceden lo mismo (52,5 cada uno). *Alternativa descartada:* ponderar con la base más los márgenes.

> **Decisión:** `Horizontal`, `Vertical` y `Line distribution` nombran los extremos de su eje por el lado de la
> página, `left` y `right`, `top` y `bottom`, como `Align`, `Side` y `Caption position`. El eje de
> `Line distribution` depende de `Direction`: en un `Stack` horizontal las líneas se apilan de arriba abajo, y
> admite `top` y `bottom`; en uno vertical van de izquierda a derecha, y admite `left` y `right`. El lado del otro
> eje es `VALUE_NOT_ALLOWED`. *Alternativa descartada:* `start` y `end`, que no dicen nada que el nombre de la
> propiedad no diga ya y obligan a recordar cuál es el principio: no hay escritura de derecha a izquierda.

### Alineación en el eje principal

Con un sobrante `L` y `n` ítems (solo actúa si nadie crece):

| Eje principal (`Horizontal` o `Vertical`) | Antes del primero | Entre ítems | Después del último |
|---|---|---|---|
| `left` o `top` | 0 | hueco | L |
| `right` o `bottom` | L | hueco | 0 |
| `center` | L/2 | hueco | L/2 |
| `justify` | 0 | hueco + L/(n−1) | 0 |
| `distribute` | L/(n+1) | hueco + L/(n+1) | L/(n+1) |

Un sobrante negativo después de encoger significa que los ítems no caben: es un error
([Desbordamiento](../common/overflow.md)). Los márgenes `auto` de un ítem son un error: la alineación se hace con
`Horizontal` y `Vertical`.

### Alineación por línea base

`Vertical: baseline`, en un `Stack` con `Direction: horizontal` o en uno de sus `Item`, pone en la misma línea las
líneas base de los ítems: un rótulo pequeño junto a una cifra grande, un precio y su unidad.

```
Stack:
    Gap: 6
    Vertical: baseline
    Item:
        Text: TOTAL
    Item:
        Text: 1.234,56
            Typography:
                Size: 20
    Item:
        Text: EUR
```

- **La línea base de un ítem** es la de su primera línea de texto: la suya o, si no tiene texto propio, la de su primer
  hijo en flujo, bajando hasta el primer texto (un `Block` con relleno y un `Paragraph` dentro, el primer `List item`
  de una `List`). El marcador de una lista no cuenta.
- **Un ítem sin texto en primer lugar** (una `Image`, un `Barcode`, un `Block` vacío, o un bloque que empieza por uno
  de ellos) apoya el borde inferior de su caja de margen en la línea base.
- **La línea base de la línea** es la del ítem que más sube por encima de ella, contando su margen superior. Los ítems
  con `baseline` de una línea la comparten, y los demás (`top`, `center`, `bottom`, `stretch`) se alinean como siempre.
- **El alto de la línea** es lo que más sube por encima de la línea base más lo que más baja por debajo, que puede ser
  más que el ítem más alto. Si el contenedor tiene alto definido y una sola línea, el grupo queda arriba.
- Un ítem con `baseline` no se estira: mide su alto.
- Con `Wrap items`, cada línea del `Stack` tiene su línea base.
- **Solo vale ahí.** En el eje principal (el `Vertical` de un `Stack` con `Direction: vertical`), en un `Grid`, en un
  `Area` y en una `Cell` es `VALUE_NOT_ALLOWED`, y en el `Item` de un `Stack` vertical, `ITEM_MAIN_AXIS`, como
  cualquier `Vertical`. `Horizontal` no lo admite.

> **Decisión:** la línea base es la de la primera línea del ítem, que es la que se lee junto a la del ítem de al lado.
> *Alternativa descartada:* la de la última, que solo sirve cuando el texto largo va delante.

> **Decisión:** un ítem sin texto se apoya en la línea base, como una imagen en una frase. *Alternativa descartada:*
> dejarlo arriba, que separa un icono del texto al que acompaña.

> **Decisión:** `baseline` solo alinea los ítems de una línea de un `Stack` horizontal. El `Vertical` de una `Cell`
> coloca su contenido en el alto de su fila y el de un `Grid`, cada ítem en su área: ninguno alinea un ítem con el de
> al lado. *Alternativa descartada:* admitirlo también en las celdas de una fila y en las filas de un `Grid`, que
> mezcla la línea base con las celdas combinadas, las filas partidas y las pistas `auto`.

## Comportamientos a tener en cuenta

- En un `Stack` vertical, `Grow` necesita que el contenedor tenga un alto definido ([Valores](../common/values.md#alto-definido)): si
  no, no hay espacio libre que repartir y es un error (`GROW_WITHOUT_EFFECT`).
- `Base size: 0` con `Grow: 1` reparte todo el espacio (columnas iguales). `Base size: auto` con `Grow: 1` solo reparte el
  sobrante sobre el tamaño del contenido.
- Un ítem no encoge por debajo del ancho más estrecho de su contenido salvo que declare `Min width: 0`. Un `Width` declarado no
  lo frena: un ítem con `Width: 400` y poco contenido encoge si no cabe.
- Un ítem estirado **no** da un alto definido a sus hijos ([Valores](../common/values.md#alto-definido)).
- Una `Image` o un `Barcode` no se estira en el eje cruzado de una fila: su alto sale de su proporción
  ([Image](image.md#dimensionado)).

## Tamaños intrínsecos

En horizontal y sin `Wrap items`, el ancho más estrecho es la suma del ancho más estrecho de los ítems más los huecos, y con `Wrap items`, el máximo de
ellos. El ancho natural es la suma del ancho natural más los huecos. En vertical, los dos son el máximo de los ítems. Se usan
los intrínsecos de cada ítem tal cual, sin `Base size`, `Grow` ni `Shrink` ([La caja](../common/box.md#tamaños-intrínsecos)).

## Cortes

En vertical, el corte se hace entre ítems, y en horizontal, entre líneas. Una línea o un ítem que no cabe en lo que queda de
página se parte en paralelo en esa página ([Cortes](../common/breaks.md#cortes-en-stack-grid-y-table)); con
`Split items: false` pasa entero a la siguiente, y solo se parte si no cabe ni en una página entera. Un `Stack` vertical con
`Wrap items` no se parte.

## Posicionadas

La posición estática de un `Overlay` o un `Page overlay` hijo de un `Stack` es la esquina superior izquierda de su caja de
contenido ([posicionados](frame-and-overlays.md#overlay)).

## Lo que no hay

- Orden visual distinto del orden del documento y direcciones invertidas.
- Alineación por la línea base de la última línea, y por línea base en un `Stack` vertical, un `Grid` o una `Table`.
- Márgenes `auto` en los ítems: son errores.
