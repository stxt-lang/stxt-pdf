# Capas y giro

## Contextos de apilamiento

El pintado se organiza en **contextos de apilamiento**: grupos de cajas que se pintan como una unidad y ordenan a sus
hijos por `Layer`. Crean uno: cada sección (el área útil), los `Fixed`, los `Relative` y `Absolute` con `Layer`
declarada y los ítems de `Flex` o `Grid` con `Layer` declarada en su `Item` o su `Area`.

`Layer` es un entero (por defecto `auto`) que solo existe en los elementos posicionados, en `Item` y en `Area`
([posicionados](../componentes/relative-absolute-fixed.md), [Flex](../componentes/flex.md#item),
[Grid](../componentes/grid.md#area)).

## Orden de pintado

Dentro de cada contexto, el orden de pintado de abajo arriba es:

1. Fondo y borde de la caja que crea el contexto.
2. Contextos hijos con `Layer` negativa, de menor a mayor.
3. Cajas en flujo en orden del árbol: primero todos los fondos y bordes, después todo el texto y las imágenes.
4. Cajas posicionadas sin `Layer` o con `Layer: 0`, en orden del árbol.
5. Contextos hijos con `Layer` positiva, de menor a mayor.

A igual `Layer`, se pinta en orden del árbol. El orden del árbol es el del documento: un `Absolute` se pinta en su sitio
entre sus hermanos aunque su contenedor de referencia esté más arriba. **`Layer` solo compara dentro del mismo
contexto:** el contenido de un contexto se pinta como un bloque al nivel de ese contexto, aunque sus hijos tengan `Layer`
mayores. Los `Fixed` del documento se pintan antes que los de la sección
([Document](../componentes/document.md#fixed-del-documento-y-de-la-sección)).

## Giro (`Rotate`)

`Rotate` y `Rotate origin` son propiedades del elemento en `Relative`, `Absolute`, `Fixed` e `Image`:

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Rotate` | número de −360 a 360, sin `0` | — | Gira la caja al pintarla, en grados en el sentido de las agujas del reloj. |
| `Rotate origin` | una o dos de `top`, `bottom`, `left`, `right`, `center` | `center` | El punto de la caja de borde alrededor del que gira (`top left`, `bottom`…). Solo con `Rotate` (`ROTATE_ORIGIN_WITHOUT_ROTATE`). |

Una caja con `Rotate` se pinta girada tantos grados como dice alrededor de su `Rotate origin` (el centro de su caja de
borde por defecto). Gira **todo lo que se pinta de la caja**: fondo, borde, texto, imagen, sus descendientes (también los
`Absolute` que cuelgan de ella y los contextos con `Layer` de dentro) y las zonas de sus enlaces. Un giro dentro de otro se
suma al de fuera.

- **No cambia el layout.** Como el `Offset` de un `Relative`, es solo pintado: la caja ocupa su sitio sin girar, sus
  hermanos no se mueven y el volcado da su rectángulo sin girar. Para un texto vertical en el margen se gira un
  `Absolute` o un `Fixed` que lo contiene (`Text` no admite `Rotate`).
- **Tiene que caber en la página.** El rectángulo que envuelve la caja de borde girada (con los giros de sus ancestros) no
  puede salirse de la página física ([Desbordamiento](desbordamiento.md)), y lo mismo un `Absolute` sin girar que cuelga
  de una caja girada.
- **No se parte.** Un `Relative` girado no se parte entre páginas, como una `Image` ([Cortes](cortes.md)); `Inside` y
  `Decoration` no tienen efecto en él y son un error (`BREAK_IN_ROTATED`), también desde una regla.
- **Orden de pintado.** No cambia: la caja girada se pinta en el mismo paso que sin girar.
- **PDF.** El contenido se gira con una matriz, así que el texto sigue siendo texto seleccionable y nítido con cualquier
  ángulo, y el PDF etiquetado y el orden de lectura no cambian. Un enlace en una caja girada es la zona girada dentro del
  rectángulo que la envuelve, que es lo que pulsan los visores que no entienden zonas giradas; el rectángulo de una figura
  girada y el destino de una caja girada ([Enlaces](enlaces.md)) son los del rectángulo que la envuelve.

> **Decisión:** `Rotate` es una propiedad del elemento, como `Offset`: no va en `Styles` ni en un grupo y no se hereda
> (aunque lo que contiene la caja gira con ella), y solo la admiten `Relative`, `Absolute`, `Fixed` e `Image`. Un `Text`
> no gira: se gira el bloque que lo contiene. *Alternativa descartada:* `Rotate` en `Box`, para poder darlo con una clase.

> **Decisión:** el ángulo va de −360 a 360 grados; `0`, que no gira nada, y lo de fuera son `VALUE_NOT_ALLOWED`. 360 y
> −360 se admiten aunque dejen la caja como estaba. *Alternativa descartada:* normalizar a (−180, 180] o rechazar las
> vueltas enteras.

> **Decisión:** `Rotate origin` son una o dos palabras clave en cualquier orden (`top left` = `left top`); el eje que no se
> nombra es `center`, y `center center` vale. Dos del mismo eje, o algo que no es una palabra clave (puntos, porcentajes),
> es `VALUE_NOT_ALLOWED`.

> **Decisión:** un `Relative` girado no se parte: se maqueta entero y, si no cabe en lo que queda de página, pasa a la
> siguiente, como una imagen; si es más alto que el área útil, `PAGE_OVERFLOW` en la línea de su `Rotate`. Los cortes
> forzados de sus descendientes no tienen efecto. *Alternativa descartada:* partirlo y girar cada fragmento sobre su
> propio centro (un sello partido en dos páginas no tiene sentido), o un error para los cortes forzados de dentro.

> **Decisión:** el giro envuelve todo el contexto de apilamiento de la caja girada, también sus posicionados y sus
> contextos con `Layer`; una `Image` en flujo se gira en sus dos pasos (fondo y borde, y contenido). El orden de pintado
> no cambia.

## Lo que no hay

- Opacidad de una caja entera (solo de los colores, [Valores](valores.md#colores)), otras transformaciones que el giro
  (escala, inclinación) y cajas que floten a un lado del texto.
- `Rotate origin` con medidas (puntos o porcentajes), girar un `Text` o un `Block` directamente, que una caja girada ocupe
  en el layout el rectángulo que la envuelve y que un `Relative` girado se parta.
