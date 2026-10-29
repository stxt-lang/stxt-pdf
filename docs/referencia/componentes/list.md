# `List` y `List item`

Una `List` apila sus ítems (`List item`) en vertical, como un `Block`, y cada ítem lleva un **marcador** generado por el
motor, que no está en el documento. Las dos son cajas de bloque y admiten `Id`, `Class`, `Link`, `Box`, `Typography` y
`Break`; un ítem contiene uno o varios elementos, cualquiera salvo los encabezados (`Paragraph`, `Text`, `Block`, otra
`List`…). Un ítem vacío es `LIST_ITEM_EMPTY`. Una `List` va donde va un
elemento en flujo, también dentro de otro ítem.

```
Styles:
	Element: List
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
			Marker: disc
			List item:
				Text: Subpunto
```

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Marker` | `disc`, `dash`, `none`, `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | — (obligatorio) | `List` | Marcador de los ítems: `•`, `–`, ninguno, o una numeración (`1.`, `a.`, `A.`, `i.`, `I.`). |
| `Start` | entero | `1` | `List` | Número del primer ítem. Solo en las numeraciones. |
| `Number` | entero | — | `List item` | Número de este ítem; los siguientes continúan desde él. Solo en las numeraciones. |

No van en las reglas de `Styles`, que solo admiten los grupos. La sangría de los ítems es el `Padding` izquierdo de la
`List`.

> **Decisión:** un único elemento `List` con `Marker` obligatorio, en lugar de dos elementos (con viñetas y numerada) o de
> un valor por defecto. `Marker`, `Start` y `Number` son propiedades del elemento: no se heredan ni van en `Styles`, así
> que cada lista anidada declara su marcador. *Alternativa descartada:* `Marker` en `Typography`, heredable y en las
> reglas.

## Marcadores

`Marker` es obligatorio: si la lista es numerada lo decide el autor. El marcador es texto en la tipografía del
`List item` (propia, heredada o de `Element: List item`): `•` (`disc`), `–` (`dash`), ninguno (`none`), o el número del
ítem con un punto: `3.`, `c.`, `C.`, `iii.`, `III.`. Siempre va seguido de un espacio, que lo separa del contenido.

> **Decisión:** el marcador es texto en la fuente del ítem, sin círculo hueco ni cuadrado, porque IBM Plex Sans no tiene
> `◦` ni `▪` y no hay fuente de sustitución. *Alternativa descartada:* dibujar los marcadores como formas, que dejarían de
> depender de la fuente.

## Numeración

El primer ítem lleva el número `Start` (1 si no se declara); cada uno, el siguiente al anterior, salvo el que fija el suyo
con `Number`, desde el que continúan los demás. Las letras siguen tras la `z` con dos letras (`aa`, `ab`…). `decimal`
escribe desde 0; las letras y los romanos, desde 1, y los romanos, hasta 3999 (`LIST_NUMBER_RANGE`). `Start` y `Number`
solo tienen efecto en las numeraciones (`LIST_NUMBER_NOT_ALLOWED`). Cada lista numera sus ítems; una lista anidada empieza
de nuevo. El número de un ítem se puede referir con `{number of: id}` ([Referencias](../comun/referencias.md#number-of-id)).

## Posición

El marcador cuelga **fuera** del ítem, a su izquierda: su borde derecho (con el espacio final) toca el borde izquierdo de
la caja de borde del ítem, así que los marcadores de distinto ancho quedan alineados a la derecha. La sangría que lo aloja
es el `Padding` izquierdo de la `List`, más el margen izquierdo del ítem. Su línea base es la de la **primera línea** del
ítem: la que se encuentra bajando por el primer hijo en flujo de cada bloque hasta un texto (con una lista anidada al
principio, los dos marcadores quedan en la misma línea).

- Un ítem con marcador cuyo contenido no lleva así a un texto (empieza con un `Flex`, un `Grid`, una `Image` o un bloque
  vacío) es un error (`LIST_ITEM_WITHOUT_LINE`), en la línea de la caja que lo impide.
- Un marcador más ancho que su sangría también (`LIST_MARKER_OVERFLOW`, al maquetar, porque depende de las métricas): no
  se sale de la lista.

> **Decisión:** la sangría es el `Padding` izquierdo de la `List` y el espacio que separa el marcador del contenido es el
> espacio final de su texto (`"1. "`); no hay una propiedad de sangría ni un ancho de marcador fijo. Un marcador más ancho
> que la sangría es un error, no un marcador que se sale. *Alternativa descartada:* una propiedad `Indent` con un ancho de
> marcador fijo.

> **Decisión:** la primera línea se busca bajando por el primer hijo en flujo de cada bloque hasta un texto, y si no se
> llega es un error, comprobado ya al construir el árbol. *Alternativa descartada:* tomar también la línea base de un
> `Flex` o un `Grid` y saltar los bloques vacíos, o alinear el marcador con el borde superior del ítem.

## Cortes

El marcador va pegado a la primera línea: si no cabe en lo que queda de página, pasan los dos a la siguiente, y si el ítem
se parte, el marcador solo está en su primer fragmento. No cambia las reglas de corte ([Cortes](../comun/cortes.md)).

> **Decisión:** el marcador va en el primer fragmento del ítem y nunca se repite.

## Salida PDF

Una `List` es `L`, con el atributo `ListNumbering` (`Disc`, `Decimal`, `LowerAlpha`, `UpperAlpha`, `LowerRoman`,
`UpperRoman`; `None` para `dash` y `none`); cada ítem es `LI`, con el marcador en `Lbl` y su contenido en `LBody`. En el
volcado, el marcador de un ítem con `Id` aparece dentro de él como `Marker`, con su texto y su rectángulo (el espacio final
incluido en el ancho; [Pruebas](../implementacion/pruebas.md#el-volcado)).

> **Decisión:** `dash` se etiqueta con `ListNumbering: None`, porque PDF/UA-1 no tiene un valor para el guion; la lista
> sigue siendo `L` con `Lbl` y `LBody`. *Alternativa descartada:* `Unordered`, de PDF 2.0.

## Lo que no hay

- Marcadores dentro del ítem, con un estilo propio distinto del ítem, con imágenes o con otro texto; círculo hueco y
  cuadrado.
- Listas en orden inverso y otros sufijos que el punto.
