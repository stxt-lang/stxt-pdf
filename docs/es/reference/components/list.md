# `List` y `List item`

Una `List` apila sus ítems (`List item`) en vertical, como un `Block`, y cada ítem lleva un **marcador** generado por el
motor, que no está en el documento. Las dos son cajas de bloque y admiten `Id`, `Style`, `Link`, `Box`, `Typography` y
`Break`. Un ítem contiene uno o varios elementos, cualquiera salvo los encabezados (un `Paragraph`, un `Text`, un `Block`
u otra `List`). Un ítem vacío es `LIST_ITEM_EMPTY`. Una `List` va donde va un elemento en flujo, también dentro de otro ítem.

```
Styles:
    For element: List
        Box:
            Padding: 0 0 0 18
List:
    Marker: decimal
    Start: 3
    List item:
        Paragraph: Primer punto
    List item:
        Number: 10
        Paragraph: Segundo punto
        List:
            Marker: bullet
            List item:
                Text: Subpunto
```

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Marker` | `bullet`, `dash`, `none`, `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | obligatorio | `List` | Marcador de los ítems: `•`, `–`, ninguno, o una numeración (`1.`, `a.`, `A.`, `i.`, `I.`). |
| `Start` | entero | `1` | `List` | Número del primer ítem. Solo en las numeraciones. |
| `Number` | entero | sin valor | `List item` | Número de este ítem. Los siguientes continúan desde él. Solo en las numeraciones. |

No van en las reglas de `Styles`, que solo admiten los grupos. La sangría de los ítems es el `Padding` izquierdo de la
`List`.

> **Decisión:** un único elemento `List` con `Marker` obligatorio, que no se hereda: cada lista anidada declara el suyo.
> *Alternativa descartada:* dos elementos (con viñetas y numerada), o `Marker` en `Typography`.

## Marcadores

`Marker` es obligatorio: si la lista es numerada lo decide el autor. El marcador es texto en la tipografía del
`List item` (propia, heredada o de `For element: List item`): `•` (`bullet`), `–` (`dash`), ninguno (`none`), o el número del
ítem con un punto: `3.`, `c.`, `C.`, `iii.`, `III.`. Siempre va seguido de un espacio, que lo separa del contenido.

> **Decisión:** el marcador es texto en la fuente del ítem, sin círculo hueco ni cuadrado, que IBM Plex Sans no tiene.
> *Alternativa descartada:* dibujarlos como formas.

## Numeración

El primer ítem lleva el número `Start` (1 si no se declara). Cada uno lleva el siguiente al anterior, salvo el que fija el suyo
con `Number`, desde el que continúan los demás. Las letras siguen tras la `z` con dos letras (`aa`, `ab`). `decimal`
escribe desde 0, y las letras y los romanos, desde 1. Los romanos llegan hasta 3999 (`LIST_NUMBER_RANGE`). `Start` y `Number`
solo tienen efecto en las numeraciones (`LIST_NUMBER_NOT_ALLOWED`). Cada lista numera sus ítems, y una lista anidada empieza
de nuevo. El número de un ítem se puede referir con `{number of: id}` ([Referencias](../common/references.md#number-of-id)).

## Posición

El marcador cuelga **fuera** del ítem, a su izquierda: su borde derecho (con el espacio final) toca el borde izquierdo de
la caja de borde del ítem, así que los marcadores de distinto ancho quedan alineados a la derecha. La sangría que lo aloja
es el `Padding` izquierdo de la `List`, más el margen izquierdo del ítem. Su línea base es la de la **primera línea** del
ítem: la que se encuentra bajando por el primer hijo en flujo de cada bloque hasta un texto (con una lista anidada al
principio, los dos marcadores quedan en la misma línea).

- Un ítem con marcador cuyo contenido no lleva así a un texto (empieza con un `Stack`, un `Grid`, una `Image` o un bloque
  vacío) es un error (`LIST_ITEM_WITHOUT_LINE`), en la línea de la caja que lo impide.
- Un marcador más ancho que su sangría también (`LIST_MARKER_OVERFLOW`): no se sale de la lista.

> **Decisión:** no hay una propiedad de sangría ni un ancho de marcador fijo: la sangría es el `Padding` de la `List`.
> *Alternativa descartada:* una propiedad `Indent`, o alinear el marcador con el borde superior del ítem.

## Cortes

El marcador va pegado a la primera línea: si no cabe en lo que queda de página, pasan los dos a la siguiente, y si el ítem
se parte, el marcador solo está en su primer fragmento. No cambia las reglas de corte ([Cortes](../common/breaks.md)).

## Accesibilidad

Un lector de pantalla anuncia la lista con su número de ítems, y cada ítem con su marcador.

## Lo que no hay

- Marcadores dentro del ítem, con un estilo propio distinto del ítem, con imágenes o con otro texto.
- Círculo hueco y cuadrado.
- Listas en orden inverso y otros sufijos que el punto.
