# `Block`

Una caja que apila sus hijos en vertical. Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins`,
`Rotate`, `Side` y `Clear`, y como hijos cualquier elemento. Va donde va un elemento en flujo. Con `Rotate`, gira con todo lo que contiene
y ocupa el rectángulo que la envuelve ([Capas y giro](../common/layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image)).
Con `Side`, es una [caja lateral](#cajas-laterales-side): el texto que la sigue corre a su lado.

```
Block:
    Box:
        Padding: 10
        Background: #F1EFE8
    Paragraph: Primer párrafo.
    Paragraph: Segundo párrafo.
```

## El flujo de bloque

Es como colocan a sus hijos en flujo un `Block`, cada sección (el área útil), `Frame`, `Overlay`, `Page overlay`, los
encabezados, `List`, `List item`, `Markdown`, `Table of contents`, una `Cell` con elementos y cada columna de un `Columns`. Los hijos
en flujo se apilan en vertical:

1. **Ancho.** Con `Width: auto`, la caja ocupa el ancho del contenedor de referencia menos sus márgenes horizontales. Con
   ancho conocido y los dos márgenes horizontales `auto`, el sobrante se reparte entre ambos y la caja queda **centrada**,
   y si solo uno es `auto`, ese absorbe el sobrante ([La caja](../common/box.md#márgenes-auto)).
2. **Posición.** La caja se coloca en la posición del cursor vertical, tras aplicar su margen superior fusionado con el
   anterior.
3. **Altura.** Se calcula su layout y el cursor avanza.

Con `Height: auto`, la altura del contenedor es la posición final del cursor más su relleno y su borde. **El ancho se
propaga de padres a hijos, y la altura, de hijos a padres.** Un `Overlay` o un `Page overlay` entre los hijos no ocupa sitio
([posicionados](frame-and-overlays.md)).

## Fusión de márgenes

Los márgenes **verticales** adyacentes en flujo de bloque se fusionan:

- entre hermanos consecutivos;
- entre un padre y su primer o último hijo, si el padre no tiene borde ni relleno en ese lado y no aísla los márgenes
  (el margen del hijo «sale» del padre);
- a través de cajas vacías (sin altura, borde, relleno ni contenido).

El resultado de fusionar varios márgenes es **el mayor positivo más el más negativo**: `20` y `30` dan 30, `20` y `−5`
dan 15, y `−10` y `−4` dan −10. En un corte de página, los márgenes que tocan el corte se truncan
([Cortes](../common/breaks.md#márgenes-en-los-cortes)).

El alto del contenido de una caja no baja de 0: si los márgenes negativos quitan más de lo que miden los hijos, la caja
mide su borde y su relleno, y el hijo que se sale por abajo es `BOX_OVERFLOW` en la línea del `Margin` negativo
([Desbordamiento](../common/overflow.md)).

> **Decisión:** el alto del contenido nunca es negativo. *Alternativa descartada:* dejar que la caja mida menos que su
> borde y su relleno, que pinta una caja más baja que su propio borde.

## Cajas que aíslan los márgenes

Una caja que aísla los márgenes corta su fusión: los márgenes de dentro no salen. Los aíslan: cada sección, `Overlay` y `Page overlay`, los ítems
de `Stack` y `Grid` (también las celdas de una `Table`), las columnas de un `Columns` y los `Block` y `Frame` con
`Isolate margins: true`. Los márgenes de los ítems de `Stack` y `Grid` nunca se fusionan.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Isolate margins` | booleano | `false` | Aísla los márgenes de la caja: corta la fusión de márgenes con los hijos. Solo en `Block` y `Frame`, y no en un ítem de `Stack` o `Grid` (`ISOLATE_MARGINS_IN_ITEM`). |

`Isolate margins` también puede venir de una regla de `Styles` ([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)), como `Side` y `Clear`.

> **Decisión:** `Isolate margins` solo está donde puede tener efecto: `Overlay`, `Page overlay` y los ítems ya aíslan, y `Stack`, `Grid`
> y `Text` no tienen hijos en flujo de bloque. En un `Block` o un `Frame` que es ítem, `Isolate margins: true` es un error.

## Recortar los márgenes (`Trim margins`)

Una caja con `Trim margins: true` **descarta los márgenes que tocan su borde de arriba y su borde de abajo desde
dentro**: el margen de arriba de su primer hijo en flujo y el de abajo del último, con los de sus propios primeros y
últimos hijos que se fusionarían con ellos. Esos márgenes ni se quedan dentro de la caja ni salen de ella: no se aplican.
Los márgenes entre los hijos no cambian.

```
Block:
    Trim margins: true
    Box:
        Background: #F1EFE8
        Padding: 10
    Paragraph: El primer párrafo empieza junto al relleno.
        Box:
            Margin: 12 0 12 0
    Paragraph: El último acaba junto al relleno; entre los dos quedan 12.
        Box:
            Margin: 12 0 12 0
```

Sin `Trim margins`, el relleno contiene los dos márgenes y la caja mediría 24 más. Sirve para lo que tiene fondo,
borde o relleno, para las celdas y para el principio de una sección, donde los márgenes de dentro no se fusionan con
nada y se quedarían como un hueco.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Trim margins` | booleano | `false` | Descarta los márgenes de los hijos que tocan el borde de arriba y el de abajo de la caja. En `Block`, `Frame`, `List item`, `Cell` y `Section`. |

- **Arriba, en el primer fragmento; abajo, en el último.** En una caja partida entre páginas, los márgenes que tocan
  el corte se truncan como siempre ([Cortes](../common/breaks.md#márgenes-en-los-cortes)).
- **Vale también en una caja abierta** (sin borde, relleno ni `Isolate margins`): los márgenes de sus hijos, que
  saldrían y se fusionarían con los suyos, se descartan, y la caja conserva solo su propio `Margin`.
- **Los hijos vacíos** de antes del primero con contenido, o de después del último, dejan pasar sus márgenes: se
  descartan con los demás.
- **En una `Section`** recorta en su área útil: el primer elemento empieza arriba del área aunque tenga margen. El
  de abajo del último ya se trunca al final de la página.
- **Una `Cell` con valor**, que es un texto, no tiene hijos: `Trim margins` no cambia nada en ella y no es un error,
  para que una regla `For element: Cell` valga para todas.
- Puede venir de una regla de `Styles` ([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)),
  salvo en una `Section`, a la que una regla solo da `Typography`.

> **Decisión:** los márgenes se descartan en la caja que los contiene, y no se quitan en cada hijo: quien escribe el
> contenido no sabe si su párrafo será el primero de una caja con fondo, y una regla (`For element: Paragraph`) da el
> mismo margen a todos. *Alternativa descartada:* un margen distinto para el primer y el último hijo (selectores de
> posición), que no alcanza a los hijos de los hijos.

> **Decisión:** una sola propiedad para arriba y abajo. *Alternativa descartada:* elegir el lado (`top`, `bottom`),
> que no ha hecho falta: lo que no se quiere recortar lleva un relleno.

## Cajas laterales (`Side`)

Una `Image`, un `Barcode` o un `Block` en flujo de bloque con `Side: left` o `Side: right` es una **caja lateral**: se
coloca a ese lado del contenedor, en la posición que tiene el flujo cuando se llega a ella, y no ocupa sitio en vertical.
Los hermanos que la siguen empiezan donde empezarían sin ella y, mientras solapan con ella en vertical, el texto **acorta
sus líneas** a su lado. Es la figura junto al texto de un manual, o la foto en una columna de un boletín.

```
Image: examples/newsletter/trail.jpg
    Side: left
    Alt: Un sendero entre prados con montañas al fondo
    Box:
        Width: 180
        Margin: 0 12 6 0
Paragraph: El texto de este párrafo, y el de los que siguen mientras la foto llegue a su altura, corre a su derecha.
```

- **El margen es la separación.** Lo que se quita al texto es la caja de margen de la caja lateral: `Margin: 0 12 6 0`
  deja 12 entre la foto y las líneas y 6 por debajo. Un margen `auto` no tiene efecto y es un error
  (`MARGIN_AUTO_WITHOUT_EFFECT`).
- **Quién la rodea.** Los `Text` y `Paragraph` que la siguen y, con el texto que tienen dentro, los `Block`, los
  encabezados, los `Markdown` y las listas: una línea se acorta si alguna parte de su alto cae a la altura de la caja.
  Una `List` solo rodea una caja lateral derecha: sus marcadores cuelgan a su izquierda, donde estaría una izquierda,
  así que con ella va debajo. Lo demás **se coloca debajo** de las cajas laterales: una `Table`, un `Stack`, un `Grid`,
  un `Columns`, un `Table of contents`, un `Frame`, una caja girada o recortada, un texto preformateado y un texto,
  bloque o encabezado con `Background` o `Border`, que se pintaría encima de la caja. También va debajo lo que lleva
  `Clear: true`.
- **`Clear`.** `Clear: true` en un `Text`, un `Paragraph`, un `Block`, un encabezado, un `Markdown`, una `Image` o un
  `Barcode` lo baja por debajo de las cajas laterales que hay antes que él, en lugar de rodearlas. Sin una caja lateral
  antes es un error (`CLEAR_WITHOUT_SIDE`).
- **Una palabra que no cabe** al lado de la caja deja esa línea vacía y pasa a la siguiente, hasta la primera línea con el
  ancho entero; solo es un desbordamiento si tampoco cabe ahí (`TEXT_OVERFLOW`). Con `Line breaking: balanced`, el reparto
  tiene en cuenta el ancho de cada línea.
- **Varias cajas laterales.** Una segunda caja lateral del mismo lado que solapa con la anterior va debajo de ella; una
  del otro lado va a su altura, y el texto corre entre las dos.
- **El alto del contenedor** llega hasta el final de sus cajas laterales si el texto acaba antes, y entonces el margen
  de abajo de su último hijo no sale de él.
- **Dónde va.** En flujo de bloque: una sección, un `Block`, un `Frame`, un `Overlay`, un `Page overlay`, una `Cell` con
  elementos, un `List item`, una columna de un `Columns`. No en un ítem de `Stack` o `Grid`, que coloca la caja él mismo
  (`SIDE_NOT_IN_BLOCK_FLOW`). Un `Block` con `Side` necesita `Width` o `Max width` (`SIDE_WITHOUT_WIDTH`).
- **Cortes.** Una caja lateral no se parte: si no cabe en lo que queda de la página pasa a la siguiente, y la primera
  línea que la rodea va con ella (el corte entre la caja y el hijo que la sigue es tan malo como separar dos cajas con
  `Keep with next`). Las cajas laterales acaban con la página: el texto que sigue en la página siguiente tiene el ancho
  entero y se reparte de nuevo en líneas desde donde se cortó. `Keep together` y `Edges` en un `Block` lateral son un
  error (`BREAK_IN_SIDE_BOX`), y un `Break` dentro de él, también (`BREAK_IN_UNBREAKABLE`).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Side` | `left`, `right` | sin valor | Hace de la caja una caja lateral. En `Image`, `Barcode` y `Block` (con `Width` o `Max width`). |
| `Clear` | booleano | `false` | Baja la caja por debajo de las cajas laterales anteriores. En `Text`, `Paragraph`, `Block`, los encabezados, `Markdown`, `Image` y `Barcode`. |

> **Decisión:** la caja lateral es lo único que hace que el texto rodee a otra caja; se coloca en su sitio del flujo y
> no se mueve. *Alternativa descartada:* los flotantes como mecanismo general de colocación, que `Stack`, `Grid`,
> `Frame` y `Overlay` ya cubren.

> **Decisión:** lo que no es texto va debajo de la caja lateral, y lo que tiene fondo o borde también, porque se pintaría
> encima de ella. *Alternativa descartada:* estrechar una tabla o un `Stack` al lado de la caja, o pintar la caja lateral
> encima de lo que la rodea.

> **Decisión:** la caja lateral y la primera línea que la rodea van juntas, y las cajas laterales acaban con la página:
> el texto que sigue en la otra página se reparte de nuevo con el ancho entero. *Alternativa descartada:* seguir
> acortando las líneas de la página siguiente como si la caja estuviera allí.

## Tamaños intrínsecos

El ancho más estrecho de un `Block` es el máximo de los anchos más estrechos de sus hijos, y su ancho natural, el máximo de
sus anchos naturales ([La caja](../common/box.md#tamaños-intrínsecos)).

## Cortes

Un `Block` se parte entre sus hijos, o dentro del hijo que no cabe ([Cortes](../common/breaks.md)).

## Accesibilidad

Un `Block` agrupa su contenido sin anunciarse: un lector de pantalla lee lo que contiene ([El PDF](../common/pdf.md#accesibilidad)).

## Lo que no hay

- Que una tabla, un `Stack`, un texto con fondo o borde, o una lista junto a una caja lateral izquierda, rodeen la caja:
  van debajo de ella ([Cajas laterales](#cajas-laterales-side)). Tampoco una caja lateral en un ítem de `Stack` o
  `Grid`.
- Recorte de lo que se sale (lo que se sale es un [desbordamiento](../common/overflow.md)).
