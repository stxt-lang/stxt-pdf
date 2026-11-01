# `Frame`, `Overlay` y `Page overlay`

Los tres **elementos posicionados** son bloques: apilan a sus hijos en flujo de bloque ([Block](block.md)). Para posicionar
un `Stack` o un `Grid`, se pone dentro de uno de ellos. Admiten `Id`, `Style`, `Link`, `Box` y `Typography`, y
`Frame`, también `Break` y `Isolate margins`.

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Offset` | 1 a 4 valores (número, %, `auto`) | `auto` | Los tres | Arriba, derecha, abajo, izquierda ([Valores](../common/values.md#abreviados-de-cuatro-lados)). |
| `Layer` | entero | `auto` | Los tres | Orden de apilamiento ([Capas y giro](../common/layers-and-rotation.md)). También en `Item` y `Area`. |
| `Pages` | `all`, `first`, `not-first`, `odd`, `even`, `last` | `all` | `Page overlay` | Las páginas en que sale ([Pages](#pages)). |
| `Rotate`, `Rotate origin` | ver [Capas y giro](../common/layers-and-rotation.md#giro-rotate) | sin valor | Los tres | Gira la caja al pintarla. También en `Image` y `Barcode`. |

`Offset`, `Layer`, `Pages` y `Rotate` son propiedades del elemento: no van en `Styles` ni se heredan.

> **Decisión:** `Frame` es un elemento y no una propiedad de cualquier caja: así `Offset` y `Layer` solo existen en los
> elementos posicionados y en `Item`/`Area`, y el schema detecta su mal uso sin reglas propias. Un `Frame` sin `Offset`
> marca el contenedor de referencia de los `Overlay`. *Alternativa descartada:* un `Offset` en cualquier elemento en flujo,
> que necesitaría otra forma de marcar el contenedor de referencia sin desplazar.

> **Decisión:** los elementos posicionados son bloques, y un `Stack` o un `Grid` posicionado se escribe dentro
> (`Overlay` > `Stack`), con una caja más. *Alternativa descartada:* que fueran envoltorios de un elemento, como `Area`, que
> se leería peor en el caso habitual (un sello, una cabecera).

> **Decisión:** `Overlay` y `Page overlay` no admiten `Break`, porque no se parten ni están en el flujo de la página. Un
> `Break` escrito en un elemento de dentro es un error (`BREAK_IN_UNBREAKABLE`, [Cortes](../common/breaks.md#cortes-forzados)).
> El que llega de una regla no lo es, para que `For element: Paragraph` con `Keep with next: true` no dé un error en cada párrafo de un pie.
> *Alternativa descartada:* comprobar también lo que llega de las reglas.

## `Frame`

La caja se coloca como cualquier caja en flujo y después se desplaza visualmente según `Offset`. Su hueco original se
conserva y sus hermanos no se mueven. Declarar a la vez arriba y abajo, o izquierda y derecha, es un error
(`OFFSET_CONFLICT`): uno de los dos tiene que quedar en `auto`. Un `Frame` sin `Offset` no se desplaza y sirve como contenedor de
referencia de sus descendientes `Overlay`. Los porcentajes del `Offset` se resuelven contra el ancho o el alto del
contenedor de referencia. Uno vertical necesita que su alto sea definido ([Valores](../common/values.md#alto-definido)).
Un `Frame` se comprueba en su padre en su sitio, sin su desplazamiento, y ya desplazado, contra la página física: puede
ocupar los márgenes de la página, pero no salirse de ella (`BOX_OVERFLOW`, [Desbordamiento](../common/overflow.md)).

## `Overlay`

La caja sale del flujo: no ocupa espacio ni contribuye a la altura `auto` de su padre. Su contenedor de referencia es la
caja de relleno del elemento posicionado más cercano entre sus ancestros, o el área útil ([La caja](../common/box.md#el-contenedor-de-referencia)).
En cada eje se cumple:

```
inicio + margen inicio + tamaño + margen fin + fin = tamaño del contenedor de referencia
```

donde *inicio/fin* son izquierda/derecha o arriba/abajo del `Offset`. Según qué términos sean `auto`:

| `auto` | Resultado |
|---|---|
| inicio, tamaño y fin | inicio = **posición estática**, tamaño automático y se despeja fin |
| ninguno, con ambos márgenes `auto` | los márgenes se reparten el sobrante: **centrada** |
| ninguno, sin márgenes `auto` | **error** (`OVERCONSTRAINED`): la posición se fija dos veces |
| inicio y tamaño | tamaño automático y se despeja inicio (anclada al fin) |
| inicio y fin | inicio = posición estática y se despeja fin |
| tamaño y fin | tamaño automático y se despeja fin |
| solo inicio | se despeja inicio |
| solo tamaño | se despeja: la caja **se estira** entre inicio y fin |
| solo fin | se despeja fin |

Un margen `auto` solo tiene sentido cuando inicio, tamaño y fin están declarados (centra la caja o, si solo es `auto` uno
de los dos, absorbe el sobrante). En los demás casos es un error. El **tamaño automático** es el ajuste al contenido
(`fit`) en horizontal y la altura del contenido en vertical. La **posición estática** es donde habría quedado la
caja si estuviera en flujo:

- en flujo de bloque, el cursor vertical más los márgenes fusionados de las cajas anteriores, sin fusionarse con el margen
  superior de la propia caja, que se suma después según la ecuación;
- dentro de un `Stack` o un `Grid`, la esquina superior izquierda de su caja de contenido.

Un `Overlay` se coloca cuando su contenedor de referencia ya tiene su tamaño, con el contenido en flujo colocado. Se
pinta en su sitio del documento entre sus hermanos, aunque su contenedor de referencia esté más arriba ([Capas y giro](../common/layers-and-rotation.md#orden-de-pintado)). No se parte: se coloca en la página donde cae
su posición, y si su contenedor de referencia está partido, se resuelve contra el fragmento de esa página
([Cortes](../common/breaks.md#lo-que-no-se-parte)). Puede ocupar los márgenes de la página, pero no salirse de ella
(`BOX_OVERFLOW`).

## `Page overlay`

Como `Overlay`, pero su contenedor de referencia es el área útil de la página, y **se pinta en las páginas que dice
`Pages`**, todas por defecto (cabeceras, pies, marcas de agua). Un `Page overlay` hijo de `Document` sale en todas las secciones que
lo heredan, y uno hijo de una `Section`, solo en las suyas ([Document](document.md#page-overlay-del-documento-y-de-la-sección)).
Los `Page overlay` no reservan espacio: el autor debe dejar sitio con el `Margin` de `Page`. Para colocarlos en el margen de la
página se usan desplazamientos negativos (`Offset: -30 0 auto 0`). Si un `Page overlay` depende de su posición estática
(desplazamientos `auto`), se toma la de la primera página en la que aparece el flujo que lo contiene y se repite igual en
todas en las que sale. Lo que hay dentro de un `Page overlay` es decoración de la página y no contenido: un lector de pantalla no
lo lee, y no puede tener encabezados (`HEADING_IN_PAGE_OVERLAY`) ni ser el destino de un enlace o una referencia.
Si un texto suyo lleva un título corrido (`{heading N}`,
[Referencias](../common/references.md#heading-n-y-heading-n-first)), el `Page overlay` se maqueta en cada página con el
encabezado en vigor en ella; los demás se maquetan una vez y se repiten igual.

> **Decisión:** un `Page overlay` se admite en cualquier contenedor, no solo como hijo de `Document` o de una sección: su posición
> estática es la de su sitio en el documento. *Alternativa descartada:* solo como hijo de `Document` y de `Section`, que es
> donde tiene sentido una cabecera o un pie.

### `Pages`

Propiedad solo del `Page overlay` (en otro elemento es un error del schema, y un valor que no está en la lista, también). Cuenta
las páginas desde la 1 **dentro de su alcance**: las del documento entero, en orden físico, si el `Page overlay` es hijo de
`Document`, y las de su sección, si es hijo de una `Section` (`first` es la primera página de la sección). No es el número
impreso de `{page}`, que puede reiniciarse o no existir ([Document](document.md#numeración-de-las-páginas)):

| Valor | Páginas |
|---|---|
| `all` | todas (por defecto) |
| `first` | solo la primera |
| `not-first` | todas menos la primera |
| `odd` / `even` | las impares / las pares |
| `last` | solo la última |

Es una condición de pintado y no de espacio: un `Page overlay` no reserva sitio ni donde sale ni donde no, así que el flujo es el
mismo con él o sin él. Una portada a sangre, con otro margen y sin numeración, es una sección. Un `Page overlay` que no sale en
ninguna página (`not-first` en un documento de una sola) es válido y no se comprueba (`BOX_OVERFLOW` solo mira los que
salen). Si un `Page overlay` contiene otro, el de dentro sale solo donde salen los dos, cada uno con su `Pages`.

> **Decisión:** `Pages` es una propiedad del `Page overlay` y no un sistema de páginas con nombre. En un documento de una sola
> página, `first` y `last` coinciden. *Alternativa descartada:* páginas con nombre y reglas por tipo de página.

> **Decisión:** un `Page overlay` no empuja el flujo en las páginas donde sale. *Alternativa descartada:* que reservara su sitio,
> que rompe el modelo de caja fuera de flujo.

## Lo que no hay

- Cajas que se queden pegadas al borde mientras se pasa de página dentro de su contenedor, ni que floten a un lado del
  texto.
- Posicionar un `Stack` o un `Grid` sin un posicionado alrededor.
