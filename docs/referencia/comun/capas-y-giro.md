# Capas y giro

## Contextos de apilamiento

El pintado se organiza en **contextos de apilamiento**: grupos de cajas que se pintan como una unidad y ordenan a sus
hijos por `Layer`. Crean uno: cada sección (el área útil), los `Page overlay`, los `Frame` y `Overlay` con `Layer`
declarada y los ítems de `Stack` o `Grid` con `Layer` declarada en su `Item` o su `Area`.

`Layer` es un entero (por defecto `auto`) que solo existe en los elementos posicionados, en `Item` y en `Area`
([posicionados](../componentes/frame-and-overlays.md), [Stack](../componentes/stack.md#item),
[Grid](../componentes/grid.md#area)).

## Orden de pintado

Dentro de cada contexto, el orden de pintado de abajo arriba es:

1. Fondo y borde de la caja que crea el contexto.
2. Contextos hijos con `Layer` negativa, de menor a mayor.
3. Cajas en flujo en orden del árbol: primero todos los fondos y bordes, después todo el texto y las imágenes.
4. Cajas posicionadas sin `Layer` o con `Layer: 0`, en orden del árbol.
5. Contextos hijos con `Layer` positiva, de menor a mayor.

A igual `Layer`, se pinta en orden del árbol. El orden del árbol es el del documento: un `Overlay` se pinta en su sitio
entre sus hermanos aunque su contenedor de referencia esté más arriba. **`Layer` solo compara dentro del mismo
contexto:** el contenido de un contexto se pinta como un bloque al nivel de ese contexto, aunque sus hijos tengan `Layer`
mayores. Los `Page overlay` del documento se pintan antes que los de la sección
([Document](../componentes/document.md#page-overlay-del-documento-y-de-la-sección)).

## Giro (`Rotate`)

`Rotate` es una propiedad del elemento en `Block`, `Text`, `Paragraph`, `Image`, `Frame`, `Overlay` y `Page overlay`,
y `Rotate origin`, solo en los tres posicionados:

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Rotate` | número entre −360 y 360, sin `0` | sin valor | Gira la caja, en grados en el sentido de las agujas del reloj. |
| `Rotate origin` | una o dos de `top`, `bottom`, `left`, `right`, `center`, o dos medidas (número o %) | `center` | El punto de la caja de borde alrededor del que gira (`top left`, `bottom` o `20 50%`). Solo con `Rotate` (`ROTATE_ORIGIN_WITHOUT_ROTATE`) y solo en `Frame`, `Overlay` y `Page overlay`. |

Una caja con `Rotate` se pinta girada tantos grados como dice. Gira **todo lo que se pinta de la caja**: fondo, borde,
texto, imagen, sus descendientes (también los `Overlay` que cuelgan de ella y los contextos con `Layer` de dentro) y las
zonas de sus enlaces. Un giro dentro de otro se suma al de fuera. Lo que cambia según el elemento es **cómo ocupa sitio**.

### Giro en flujo: `Block`, `Text`, `Paragraph` e `Image`

Una caja en flujo girada **ocupa el rectángulo que la envuelve girada**: lo que la rodea se coloca alrededor de ese
rectángulo y nunca debajo de lo girado. Es la forma de escribir un texto en vertical en su sitio, como la cabecera de una
columna estrecha:

```
Text: Lunes
    Rotate: -90
```

- **Su tamaño sin girar no depende del contenedor.** Su ancho es el de su `Width` o, si es `auto`, lo que mide su
  contenido sin partir líneas (ancho natural), limitado por `Min width` y `Max width`. Una `Image` tiene el suyo de siempre. Su
  alto sale de ese ancho, como siempre. Un párrafo largo girado necesita un `Width` que reparta sus líneas.
- **Gira sobre su centro** y ocupa el rectángulo que envuelve su caja de borde girada, con ella centrada dentro. Por eso
  `Rotate origin` no tiene sentido en flujo y no lo admite el schema: cualquier origen daría el mismo resultado.
- **Lo que ocupa es ese rectángulo, con sus márgenes alrededor.** Con `Margin: 0 auto` se centra por él. Lo que aporta al
  ancho de su contenedor (en una columna `auto`, en un ítem que se ajusta a su contenido) es el ancho de ese rectángulo.
- **Aísla los márgenes** ([Block](../componentes/block.md#cajas-que-aíslan-los-márgenes)): los márgenes
  de sus hijos no salen de ella, y `Isolate margins`, que no tendría efecto, es `ISOLATE_MARGINS_IN_ROTATED`. Es también el contenedor de
  referencia de los `Overlay` de dentro, que se colocan en su caja sin girar y giran con ella.
- **No se parte** entre páginas, como una `Image`: entera o, si no cabe en lo que queda, en la página siguiente. `Keep together`
  y `Edges` son `BREAK_IN_ROTATED`, y un `Break` de dentro, `BREAK_IN_UNBREAKABLE` ([Cortes](cortes.md)).
- **No puede ser un ítem de `Stack` o `Grid`** (`ROTATED_ITEM`), que ocupa el sitio que le da su contenedor: se gira una
  caja dentro del ítem.

> **Decisión:** en flujo, la caja girada ocupa el rectángulo que la envuelve, para que un texto en vertical tenga su
> sitio y no pise a sus vecinos. *Alternativa descartada:* girar solo al pintar, como un `Frame`, que deja la caja
> encima de lo que la rodea.

> **Decisión:** sin `Width`, una caja girada en flujo mide su contenido sin partir líneas y no el ancho del contenedor: el
> ancho de una columna dice cuánto puede ocupar girada y no cuánto mide sin girar. *Alternativa descartada:* estirarla
> al ancho del contenedor, que con −90 grados haría una columna de texto tan alta como ancha es la página.

> **Decisión:** un ítem de `Stack` o `Grid` no gira. Su contenedor le da el tamaño, y un tamaño impuesto a la caja girada
> no diría si es el de antes o el de después de girar. *Alternativa descartada:* que el ítem ocupe su rectángulo girado,
> que contradice el `Base size`, el estirado y el `Width` del ítem.

### Giro al pintar: `Frame`, `Overlay` y `Page overlay`

Un posicionado girado gira alrededor de su `Rotate origin` (el centro de su caja de borde por defecto) y **no cambia el
layout**: como el `Offset` de un `Frame`, es solo pintado. La caja ocupa su sitio sin girar y sus hermanos no se
mueven. Para un texto vertical en el margen de la página se gira un `Overlay` o un `Page overlay` que lo contiene.

Un `Frame` girado no se parte entre páginas. `Keep together` y `Edges` no tienen efecto en él y son un error
(`BREAK_IN_ROTATED`), también desde una regla.

`Rotate origin` se escribe de una de dos formas:

- **Palabras clave:** una o dos, en cualquier orden (`top left` = `left top`). El eje que no se nombra es `center`.
- **Dos medidas:** la horizontal y la vertical, desde la esquina de arriba a la izquierda de la caja de borde, en puntos o
  en porcentaje de su ancho y de su alto (`Rotate origin: 20 50%`). Pueden ser negativas o pasar del tamaño de la caja:
  el centro del giro puede estar fuera de ella.

Dos palabras del mismo eje, una sola medida, una palabra clave con una medida, o algo que no es ni una cosa ni la otra,
es `VALUE_NOT_ALLOWED`.

> **Decisión:** las medidas van siempre de dos en dos y sin mezclarse con palabras clave: `left 20` tendría que decir si
> el 20 es la otra coordenada o una distancia desde la izquierda. *Alternativa descartada:* combinar palabras clave y
> medidas.

> **Decisión:** un `Frame` girado no se parte. *Alternativa descartada:* girar cada fragmento sobre su propio centro: un
> sello partido en dos páginas no tiene sentido.

### Lo común

- **Tiene que caber en la página.** El rectángulo que envuelve la caja de borde girada (con los giros de sus ancestros) no
  puede salirse de la página física ([Desbordamiento](desbordamiento.md)), y lo mismo un `Overlay` sin girar que cuelga
  de una caja girada. Una caja girada más alta que el área útil es `PAGE_OVERFLOW` en la línea de su `Rotate`.
- **Orden de pintado.** No cambia: la caja girada se pinta donde se pintaría sin girar.
- **Texto.** Sigue siendo texto seleccionable y nítido con cualquier ángulo, y el orden de lectura no cambia. Un enlace en
  una caja girada se pulsa en la zona girada.

`Rotate` va entre −360 y 360 grados, sin incluirlos. `0`, `360` y `−360`, que no giran nada, son `VALUE_NOT_ALLOWED`.

> **Decisión:** `Rotate` es una propiedad del elemento, como `Offset`: no va en `Styles` ni se hereda (aunque lo que
> contiene la caja gira con ella). *Alternativa descartada:* `Rotate` en `Box`, para poder darlo con un estilo.

## Lo que no hay

- Opacidad de una caja entera (solo de los colores, [Valores](valores.md#colores)), otras transformaciones que el giro
  (escala, inclinación) y cajas que floten a un lado del texto.
- Girar los demás elementos en flujo (una tabla, una lista, un `Stack`) directamente: se gira un `Block` que los contiene.
- Que un `Frame` girado se parta entre páginas.
