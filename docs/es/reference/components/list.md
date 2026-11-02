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
| `Marker` | `bullet`, `dash`, `none`, `image`, `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | obligatorio | `List` | Marcador de los ítems: `•`, `–`, ninguno, una imagen, o una numeración (`1.`, `a.`, `A.`, `i.`, `I.`). |
| `Marker image` | ruta relativa a `images/` | sin valor | `List`, `List item` | La imagen del marcador con `Marker: image` ([Marcadores con imagen](#marcadores-con-imagen)): la de la lista vale para todos sus ítems, y la de un ítem la sustituye. Admite `Alt` y `Height`. |
| `Start` | entero | `1` | `List` | Número del primer ítem. Solo en las numeraciones. |
| `Number` | entero | sin valor | `List item` | Número de este ítem. Los siguientes continúan desde él. Solo en las numeraciones. |

No van en las reglas de `Styles`, que solo admiten los grupos. La sangría de los ítems es el `Padding` izquierdo de la
`List`.

> **Decisión:** un único elemento `List` con `Marker` obligatorio, que no se hereda: cada lista anidada declara el suyo.
> *Alternativa descartada:* dos elementos (con viñetas y numerada), o `Marker` en `Typography`.

## Marcadores

`Marker` es obligatorio: si la lista es numerada lo decide el autor. El marcador es texto en la tipografía del
`List item` (propia, heredada o de `For element: List item`): `•` (`bullet`), `–` (`dash`), ninguno (`none`), o el número del
ítem con un punto: `3.`, `c.`, `C.`, `iii.`, `III.`. Siempre va seguido de un espacio, que lo separa del contenido. Con
`image`, el marcador es una imagen ([abajo](#marcadores-con-imagen)).

> **Decisión:** el marcador es texto en la fuente del ítem, sin círculo hueco ni cuadrado, que IBM Plex Sans no tiene.
> *Alternativa descartada:* dibujarlos como formas.

## Marcadores con imagen

Con `Marker: image`, el marcador de cada ítem es la imagen de un `Marker image`: el de la lista, que vale para todos sus
ítems, o el del ítem, que sustituye entero al de la lista (ruta, `Alt` y `Height`). En una misma lista puede haber
imágenes distintas: las coberturas incluidas con un visto y las excluidas con una cruz.

```
List:
    Marker: image
    Marker image: symbols/check.svg
        Alt: Incluido
    List item:
        Paragraph: Responsabilidad civil
    List item:
        Marker image: symbols/cross.svg
            Alt: Excluido
            Height: 8
        Paragraph: Daños propios
```

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| *(valor)* | ruta relativa a `images/` | obligatorio | El fichero, como en una [`Image`](image.md#fichero): PNG, JPEG, SVG o PDF (su primera página). |
| `Alt` | texto no vacío | sin valor | Texto alternativo: con él, la imagen se lee como figura dentro del marcador ([accesibilidad](#accesibilidad)). Sin él, es decorativa. |
| `Height` | longitud positiva | el `Size` del ítem | Alto de la imagen. El ancho sale de su proporción. |

- La imagen mide de alto `Height` o, sin él, el `Size` de la tipografía del ítem, y de ancho lo que da su proporción.
  Le sigue el mismo espacio que a un marcador de texto, en la fuente del ítem, y su borde derecho toca el borde izquierdo
  de la caja de borde del ítem, como los demás marcadores. Su centro vertical es el centro de las mayúsculas de la
  tipografía del ítem sobre la línea base de la primera línea; con `Height` mayor que el `Size`, sobresale por arriba y por
  abajo de la línea.
- El ítem necesita `Font` y `Size`, como con un marcador de texto (`TEXT_WITHOUT_FONT`, `TEXT_WITHOUT_FONT_SIZE`): el espacio y
  el alto por defecto salen de ellos. Y necesita una primera línea con la que alinearse (`LIST_ITEM_WITHOUT_LINE`).
- Un `Marker image` en una lista, o en un ítem de una lista, cuyo `Marker` no es `image` es un error
  (`LIST_IMAGE_NOT_ALLOWED`), en la línea del `Marker image`. Un ítem de una lista con `Marker: image` que no tiene imagen,
  ni propia ni de la lista, también (`LIST_IMAGE_MISSING`), en la línea del ítem.
- `Start` y `Number` no tienen efecto (`LIST_NUMBER_NOT_ALLOWED`), y `{number of: id}` no vale para sus ítems: una lista con
  imágenes no numera.
- Los errores del fichero son los de una `Image` (`IMAGE_UNKNOWN`, `IMAGE_FORMAT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX`), con
  la línea del `Marker image`. Un `Alt` vacío es `VALUE_NOT_ALLOWED`, y un `Height` que no es una longitud positiva, también.
- Una lista de un `Markdown` no tiene marcadores con imagen: `image` en `Bullet markers` es `VALUE_NOT_ALLOWED`.

> **Decisión:** `Marker: image` dice que los marcadores son imágenes y `Marker image` cuál es cada una, con el `Alt` y el
> `Height` como hijos suyos, igual que una `Image` lleva los suyos. *Alternativa descartada:* que la ruta fuera el valor de
> `Marker`, que dejaría de ser una enumeración; o `Marker image` solo, sin `Marker: image`, que haría opcional el
> `Marker`.

> **Decisión:** el `Marker image` de un ítem sustituye entero al de la lista. *Alternativa descartada:* heredar del de la
> lista el `Alt` o el `Height` que el ítem no declara: el `Alt` describe una imagen distinta.

> **Decisión:** la imagen se centra en las mayúsculas del ítem, y mide por defecto lo que su `Size`. *Alternativa descartada:*
> apoyarla en la línea base como un glifo, que deja un icono visiblemente alto respecto a las minúsculas; o centrarla en
> la caja de línea, que se mueve con `Line height`.

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

- Un ítem con marcador cuyo contenido no lleva así a un texto (empieza con un `Stack`, un `Grid`, una `Image`, un
  `Barcode` o un bloque vacío) es un error (`LIST_ITEM_WITHOUT_LINE`), en la línea de la caja que lo impide.
- Un marcador más ancho que su sangría también (`LIST_MARKER_OVERFLOW`): no se sale de la lista.

> **Decisión:** no hay una propiedad de sangría ni un ancho de marcador fijo: la sangría es el `Padding` de la `List`.
> *Alternativa descartada:* una propiedad `Indent`, o alinear el marcador con el borde superior del ítem.

## Cortes

El marcador va pegado a la primera línea: si no cabe en lo que queda de página, pasan los dos a la siguiente, y si el ítem
se parte, el marcador solo está en su primer fragmento. No cambia las reglas de corte ([Cortes](../common/breaks.md)).

## Accesibilidad

Un lector de pantalla anuncia la lista con su número de ítems, y cada ítem con su marcador. Un marcador con imagen y `Alt`
se lee con su texto alternativo (es una figura dentro del marcador, `Lbl` → `Figure`); sin `Alt` es decorativo y el ítem no
tiene marcador para el lector ([El PDF por dentro](../implementation/pdf.md#listas)).

## Lo que no hay

- Marcadores dentro del ítem, con un estilo propio distinto del ítem o con otro texto.
- Círculo hueco y cuadrado.
- Listas en orden inverso y otros sufijos que el punto.
- Marcadores con imagen en las listas de un `Markdown`.
