# Desbordamiento

**Nada puede desbordar**, salvo lo que una caja decide con [`Overflow`](#política-por-caja-overflow). El motor comprueba el
resultado ya paginado y da un error con la primera de estas situaciones:

- **una palabra más ancha que su línea, o una línea de un texto con `Preformatted: true` más ancha que su caja**
  (`TEXT_OVERFLOW`), en la línea del documento donde está la palabra;
- **una caja que no cabe a lo ancho en la caja de contenido de su padre**, contando sus márgenes horizontales
  (`BOX_OVERFLOW`), en la línea de su `Width` (o `Min width`) si lo declara;
- **contenido que pasa del alto de su caja**: líneas o hijos que no caben en el alto que fija su `Height`, su `Max height`,
  su `Aspect ratio` o, en un `Grid`, sus filas de tamaño fijo (`BOX_OVERFLOW`), en la línea de esa propiedad (`Row sizes` para
  las filas, y si declara varias, la primera de esta lista). Si nada fija el alto, lo que falta lo quita un `Margin`
  negativo de dentro, y se señala ese `Margin`;
- **algo del flujo que no cabe en el área útil de una página y no se puede partir** (`PAGE_OVERFLOW`), en la línea de lo
  que impide partirlo:
  - su `Padding` o su `Border`, si el relleno y el borde del fragmento ya no caben en la página;
  - si no, y es una línea de texto, su `Line height` (o, sin él, su `Size`), propios o heredados;
  - si no, su `Height` (en una `Image` o un `Barcode` sin él, su `Width`, del que sale el alto);
  - en una caja girada, que no se parte, su `Rotate`;
- **un `Overlay` o un `Page overlay` que se sale de la página física** (`BOX_OVERFLOW`), en la línea de su `Offset`. Pueden
  ocupar los márgenes de la página, pero no salir de la página;
- **una caja en flujo que se sale de la página física** (`BOX_OVERFLOW`): cabe en su padre, pero el `Offset` de un
  `Frame` o un `Margin` negativo la sacan de la página; en la línea de su `Offset` o, si no lo tiene, de su `Margin`.
  Como un `Overlay`, puede ocupar los márgenes de la página;
- **una caja girada que se sale de la página física** (`BOX_OVERFLOW`), en la línea de su `Rotate`: lo que se comprueba es
  el rectángulo que envuelve lo que se pinta, con los giros de sus ancestros ([Capas y giro](layers-and-rotation.md#giro-rotate)).
  Un `Overlay` o un `Page overlay` girado se comprueba solo así (puede salirse sin girar y caber girado), y uno sin girar dentro
  de una caja girada, también, en la línea del elemento;
- **una caja girada en flujo más ancha que su contenedor** (`BOX_OVERFLOW`): el rectángulo que la envuelve, con sus
  márgenes, no cabe en su caja de contenido. Se señala su `Width` o, sin él, su `Rotate`: sin `Width` no parte su texto en
  líneas;
- **el marcador de una lista que no cabe en su sangría** (`LIST_MARKER_OVERFLOW`, [List](../components/list.md#posición));
- **una tabla cuya cabecera y pie no dejan sitio al cuerpo** al principio de una página (`TABLE_REPEAT_OVERFLOW`,
  [Table](../components/table.md#cortes)), en la línea de la `Table`;
- **el contenido de un `Columns` que no cabe en sus columnas** y no puede seguir en otra página (`BOX_OVERFLOW`,
  [Columns](../components/columns.md#lo-que-no-cabe)): en la línea de su `Height` (o `Max height`, o `Aspect ratio`) si
  es su alto el que se acaba, o en la de su `Count` si, sin página ni alto que llenar, tiene más cortes de columna que
  columnas.

El error señala al culpable **más interior**: si una caja se sale porque se sale algo de dentro, se nombra lo de dentro.
Los `Frame` se comprueban en su sitio, sin el desplazamiento de `Offset` ni el giro de `Rotate` (lo girado se comprueba
además contra la página), y un margen negativo cuenta como espacio ganado: no son desbordamientos, sino decisiones del
autor, mientras la caja no salga de la página física. Tampoco cuentan los márgenes verticales, que se fusionan, ni una
caja vacía (sin alto, borde, relleno ni contenido), que no pinta nada aunque sus márgenes fusionados la dejen más allá del
final del contenido de su padre. Una caja puede salirse hasta 0,01 pt sin que sea un desbordamiento, lo que acumulan las
medidas de las fuentes en milésimas.

Una caja que declara su ancho es la culpable de sus hijos que solo la llenan: no se baja a un hijo salvo que se salga
también de ella. En una caja de ancho `auto` sí se baja, porque su ancho sale de su contenido. Una caja cuyo alto impone el
padre (un ítem estirado a una fila fija) se señala a sí misma y no las `Row sizes` del `Grid`.

Lo girado se comprueba como se ve: el rectángulo que envuelve la caja girada, con el `Offset` de un `Frame` incluido.
Una caja girada en flujo se comprueba en su contenedor con ese rectángulo, que es lo que ocupa. Las cajas en flujo de
dentro de una girada se comprueban contra su padre sin girar, y no se baja a ellas para buscar al culpable de lo que se
sale a lo ancho de la girada.

> **Decisión:** el error señala la propiedad que hay que cambiar (el `Width`, el `Height`, el `Line height`) y no la caja.
> *Alternativa descartada:* la línea de la caja, o comparar siempre con el padre inmediato, que en una caja `auto` señala
> la caja y no lo que la ensancha.

## Política por caja: `Overflow`

`Overflow`, en el grupo `Box`, dice qué hace el motor cuando lo que una caja contiene no cabe en ella. No se hereda.

| Valor | Qué hace | Dónde vale |
|---|---|---|
| `error` | Lo de arriba: el primer desbordamiento para la conversión. Es el valor por defecto. | En cualquier caja |
| `shrink` | Reduce el `Size` del texto hasta que cabe, sin bajar de `Min size` ([encoger](#encoger-el-texto-shrink)). | En una caja con texto: `Text`, `Paragraph` y `Cell` con valor |
| `clip` | Pinta recortado lo que se sale de la caja de relleno y no lo comprueba ([recortar](#recortar-clip)). | En una caja sin texto dentro |

### Encoger el texto: `shrink`

Con `Overflow: shrink` el texto se reparte en líneas al `Size` de la caja y, si alguna línea es más ancha que la caja de
contenido o las líneas no caben en su alto, se reparte otra vez a un tamaño menor: de 0,5 en 0,5 pt desde el `Size`, y
`Min size` como último intento si el paso lo salta. Se queda con el mayor tamaño al que cabe. `Min size` es una propiedad
de `Typography` ([Tipografía](typography.md)): se hereda y vale 6 por defecto.

- **A lo ancho**, siempre: el ancho de una caja es definido al maquetar, y lo que tiene que caber es cada línea (en un
  texto normal, su palabra más ancha) en el ancho de la caja de contenido.
- **A lo alto**, solo si la caja tiene un alto que no sale de su contenido: su `Height`, el que le impone el padre (una
  celda de una fila de alto fijo, un ítem estirado) o el que sale de `Aspect ratio`; si no, su `Max height`. Con alto
  `auto` la caja crece con sus líneas y no hay nada que encoger.
- El `Line height` sigue al tamaño si es un porcentaje (también el 120 % por defecto); una medida en puntos no cambia.
- Nada más cambia: la fuente, el color, el `Case` y el etiquetado del PDF son los mismos. El contraste se comprueba con
  el tamaño reducido: un texto que baja de 18 pt deja de ser grande ([El PDF](pdf.md#contraste)).
- Si ni a `Min size` cabe, es el error de siempre (`TEXT_OVERFLOW` o `BOX_OVERFLOW`), y el mensaje dice que el texto ya
  está a `Min size`. Donde el texto podría encoger y no lo hace, el mensaje de esos dos errores dice que existe `shrink`.
- Los tamaños intrínsecos (el ancho más estrecho y el natural, [La caja](box.md#tamaños-intrínsecos)) se miden al `Size`,
  no a `Min size`: una caja de ancho `auto`, una columna `auto` de una tabla o un ítem con su mínimo automático crecen
  hasta su contenido y no encogen el texto. `shrink` actúa donde el tamaño está fijado.

`Overflow: shrink` solo tiene efecto en una caja con texto. Escrito en un elemento sin texto (un `Block`, una `Cell` con
elementos dentro) es un error (`SHRINK_WITHOUT_TEXT`); si llega de una regla de `Styles` a una caja sin texto, no hace
nada y no es un error, como un `Break` que llega de una regla a una caja que no se parte
([Errores](errors.md#lo-que-no-tiene-efecto)): así `For element: Cell` sirve para todas las celdas de una tabla que se
rellena desde datos. `Min size` mayor que `Size` en una caja que encoge es una contradicción (`MIN_SIZE_CONFLICT`);
igual que `Size` no lo es.

```stxt
Styles:
    For element: Cell
        Box:
            Height: 16
            Overflow: shrink
```

### Recortar: `clip`

Con `Overflow: clip` la caja es una ventana: lo que contiene se pinta recortado por su caja de relleno (con sus esquinas,
si las tiene redondeadas), también los `Overlay` que cuelgan de ella o de sus descendientes, y nada de lo que se sale de
ella se comprueba, ni contra la caja ni contra la página física. Lo que hay dentro se comprueba como siempre contra su
propio padre: un nieto que se sale de un hijo sigue siendo un desbordamiento. La caja misma también: cabe en su padre y
en la página.

- Solo en una caja sin texto dentro, a cualquier profundidad: un texto recortado sería contenido que no se lee y no
  avisaría. Con texto dentro es un error (`CLIP_WITH_TEXT`). Para recortar una decoración en una tarjeta con texto, la
  decoración se envuelve en un `Overlay` con `Offset: 0` y `Overflow: clip`, y el texto va fuera de él.
- No se parte entre páginas: como una caja girada, pasa entera a la página siguiente si no cabe en lo que queda, y si no
  cabe en una página es `PAGE_OVERFLOW` ([Cortes](breaks.md#lo-que-no-se-parte)). `Keep together` y `Edges` en ella no
  tienen efecto y son un error (`BREAK_IN_CLIPPED`), y un `Break` escrito por un elemento de dentro,
  `BREAK_IN_UNBREAKABLE`.
- En una caja sin nada que pueda salirse (una `Image`, un `Block` vacío) `clip` no hace nada y no es un error, como un
  `Corner radius` sin nada que redondear.

```stxt
Frame:
    Box:
        Width: 120
        Height: 50
        Background: #EEF2F7
        Overflow: clip
    Overlay:
        Offset: 20 auto auto 90
        Box:
            Width: 60
            Height: 60
            Corner radius: 50%
            Background: #C8102E
```

> **Decisión:** `error` por defecto: el motor sigue siendo estricto, y encoger o recortar es una decisión del autor, caja
> a caja o por tipo con una regla. *Alternativa descartada:* encoger por defecto en las celdas, que cambiaría documentos
> existentes sin avisar.

> **Decisión:** `shrink` baja de 0,5 en 0,5 pt y prueba `Min size` como último paso, en lugar de buscar el tamaño exacto
> al que cabe: da tamaños legibles (`8`, `7.5`) y el mismo resultado ante un cambio mínimo del texto. *Alternativa
> descartada:* una búsqueda binaria hasta la milésima.

> **Decisión:** los tamaños intrínsecos se miden al `Size`, así que `shrink` solo actúa donde el tamaño está fijado y una
> columna `auto` crece en lugar de encoger su texto. *Alternativa descartada:* medir el ancho más estrecho a `Min size`,
> con el que una tabla de columnas `auto` encogería el texto sin que nadie lo pidiera.

> **Decisión:** `clip` recorta también los `Overlay` de dentro y no admite texto, al revés que las esquinas redondeadas,
> que no recortan ni el texto ni los `Overlay`: su uso es la decoración que asoma por el borde de una caja, y lo que no
> se ve no puede ser contenido. *Alternativa descartada:* no recortar el texto y seguir comprobándolo, una regla mixta
> en la que el texto de una caja que se sale quedaría a la vista fuera de la ventana.

> **Decisión:** una caja con `clip` no se parte: una ventana partida entre páginas tendría que decidir qué parte de lo
> recortado va en cada fragmento. *Alternativa descartada:* partirla y recortar cada fragmento por su cuenta.
