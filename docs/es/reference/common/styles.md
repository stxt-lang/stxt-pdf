# Reglas reutilizables: `Styles`

`Styles`, hijo opcional de `Document` (como mucho uno), declara una vez propiedades que se aplican por defecto a muchos
elementos, y los [colores con nombre](#colores-con-nombre) del documento. Las mismas reglas y colores pueden estar también
en un [documento de estilos aparte](#documento-de-estilos-aparte), o en
[varios](#varios-documentos-de-estilos). No hay cascada: las reglas dan valores por defecto a
los elementos de un tipo, de un estilo o con un `Id`, y el elemento los puede sobrescribir.

## Reglas

| Regla | Valor | Se aplica a |
|---|---|---|
| `For element` | `Document` | la raíz: su `Typography` es la base que heredan todas las cajas. Solo admite `Typography` |
| `For element` | nombre de un elemento de caja (`Block`, `Text` o `Heading 1`) | todos los elementos de ese tipo |
| `For element` | un elemento generado por el motor: `Code block`, `TOC 1` a `TOC 6` o `Footnotes` | los bloques de código de un `Markdown` ([Markdown](../components/markdown.md#código)), las entradas de un `Table of contents` ([Table of contents](../components/table-of-contents.md#estilo)) o el área de notas al pie de cada página ([Footnote](../components/footnote.md#el-área-de-notas-footnotes)). No se escriben en el documento |
| `For element` | una posición en una tabla: `Header cell`, `Body cell`, `Footer cell`, `Odd row cell` o `Even row cell` | las celdas de una `Table` en esa posición ([Table](../components/table.md#estilo-de-las-celdas)) |
| `For style` | un nombre, sin espacios | los elementos con ese nombre en su `Style`, las celdas de una `Row` con él en su `Style` y los tramos con estilo `[texto]{nombre}` ([Formato del texto](text-formatting.md#tramos-con-estilo)) |
| `For id` | un `Id`, sin espacios | el elemento con ese `Id` |
| `For column` | una columna (`3`) o un rango de columnas (`2 4`, ambas incluidas) | las celdas de una `Table` que **empiezan** en esas columnas |

Cada regla admite los grupos `Box`, `Typography` y `Break`, como mucho uno de cada, y las
[propiedades de elemento](#propiedades-de-elemento) que dicen cómo se ve un elemento o cómo reparte su contenido (`Gap`,
`Column sizes`, `Fit`, `Marker`...), como mucho una de cada.

```
Styles:
    For element: Text
        Typography:
            Align: justify
    For style: note
        Box:
            Background: #F1EFE8
            Padding: 8
    For id: title
        Typography:
            Size: 20
            Bold: true
Text: Resultados del trimestre
    Id: title
    Typography:
        Align: left
Text: Una nota al margen.
    Style: note
```

## Propiedades de elemento

Una regla da también propiedades propias de un elemento, como si el elemento las declarara:

```
Styles:
    For style: dish
        Gap: 12
    For element: Image
        Fit: fill
Stack:
    Style: dish
    Item:
        Grow: 1
        Text: Croquetas de gamba roja
    Item:
        Text: 9,50 €
Image: fotos/sala.jpg
```

Son las de aspecto y de reparto. Las admite la regla que se aplica a un elemento de ese tipo:

| Elemento | Propiedades en una regla |
|---|---|
| `Stack` | `Direction`, `Wrap`, `Gap`, `Horizontal`, `Vertical`, `Line distribution`, `Split items` |
| `Grid` | `Column sizes`, `Row sizes`, `Gap`, `Areas`, `Flow`, `Horizontal`, `Vertical`, `Split rows` |
| `Table` | `Column sizes`, `Column align`, `Gap`, `Split rows` |
| `Columns` | `Count`, `Min column width`, `Gap`, `Separator`, `Separator color`, `Balance` |
| `List` | `Marker`, `Marker image` |
| `List item` | `Marker image` |
| `Image` | `Fit`, `Side`, `Clear` |
| `Barcode` | `Format`, `Error correction`, `Quiet zone`, `Side`, `Clear` |
| `Figure` | `Caption position`, `Figure counter`, `In table of contents`, `Side`, `Clear` |
| `Heading 1` a `Heading 6` | `In table of contents`, `Clear` |
| `Paragraph` | `Drop cap`, `Clear` |
| `Text` | `Clear` |
| `Block` | `Isolate margins`, `Side`, `Clear` |
| `Frame` | `Isolate margins` |
| `Markdown` | `Bullet markers`, `Number markers`, `Table columns`, `Clear` |
| `Table of contents` | `Levels`, `Leader` |
| el elemento de un `Item` | `Grow`, `Shrink`, `Base size` |

- **Se aplican como los grupos**: con la misma [prioridad](#prioridad), propiedad a propiedad, y sin herencia. Lo que
  declara el elemento gana a todas sus reglas.
- **Solo lo que el elemento puede declarar.** Una regla que da a un elemento una propiedad que no tiene (un `Gap` a un
  `Text`) es `STYLE_NOT_ALLOWED`, con la línea de la propiedad en la regla. En `For element` el error sale al leer la
  regla, aunque no se aplique a ningún elemento; en `For style` y `For id`, al aplicarla, porque el mismo nombre puede
  ir en elementos de tipos distintos. Las reglas de posición de una celda, `For column`, `For element: Document` y los
  elementos generados (`Code block`, `TOC 1` a `TOC 6`, `Footnotes`) no admiten ninguna, y una regla que llega a una
  `Section` tampoco.
- **El valor se comprueba al leer la regla.** En `For style` y `For id` tiene que valer en alguno de los elementos que
  tienen la propiedad: `Gap: 1 2 3` es `VALUE_NOT_ALLOWED` en cualquier caso, y `Horizontal: justify`, que vale en un
  `Stack`, es un error al aplicarlo a un `Grid`. Lo que depende del elemento se comprueba al aplicarla: el
  `Column align` de una regla tiene una alineación por columna de cada tabla a la que llega (`TABLE_COLUMN_ALIGN`).
- **`Horizontal` y `Vertical`** son las del `Stack` o el `Grid`, nunca las del `Item` o el `Area` que envuelve el
  elemento: la alineación de un ítem se escribe en su `Item` o su `Area`.
- **`Grow`, `Shrink` y `Base size`** son del `Item` ([Stack](../components/stack.md#item)), que no es una caja: los da la
  regla del elemento que el `Item` envuelve, y lo que declara el `Item` gana a la regla. A un elemento que no está en un
  `Item` (también al de un `Area`) son `STYLE_NOT_ALLOWED`.
- **`Marker` y `Column sizes`** son obligatorios en una `List` y en una `Table`, y pueden venir de una regla. Sin ninguno
  de los dos, el error es `LIST_MARKER_MISSING` o `TABLE_COLUMN_SIZES_MISSING`, con la línea del elemento.
- **No van en una regla** las propiedades de identidad, de contenido, de estructura y de posición: `Id`, `Style`,
  `Link`, `Alt`, `Span`, `Row header`, `Start`, `Number`, `Pdf page`, `Of`, `Offset`, `Layer`, `Pages`, `Rotate` y
  `Rotate origin`, las de `Section` y `Page`, y el valor o el `Content` de un elemento. El schema no las admite.

> **Decisión:** las reglas admiten las propiedades de elemento de aspecto y de reparto, y el motor comprueba que el
> elemento las puede declarar, porque el schema no puede: el tipo va en el valor de `For element`, y en `For style` y
> `For id` no se conoce hasta aplicar la regla. *Alternativa descartada:* que las reglas solo admitieran los grupos, que
> obligaba a repetir en cada elemento el mismo `Gap` o el mismo `Fit`.

> **Decisión:** una propiedad que el elemento no puede declarar es un error, también en `For style`. *Alternativa
> descartada:* ignorarla, que permitiría usar el mismo estilo en elementos de tipos distintos a costa de no avisar de una
> errata.

> **Decisión:** `Grow`, `Shrink` y `Base size` llegan por la regla del elemento que envuelve el `Item`, y en una regla
> `Horizontal` y `Vertical` son las del `Stack` o el `Grid`. *Alternativa descartada:* admitir `Style` en `Item` y
> `Area`, que no son cajas, y que haría ambiguas `Horizontal` y `Vertical` en un `Stack` que es a la vez ítem de otro.

> **Decisión:** quedan fuera la identidad, el contenido, la estructura y la posición, como la decisión de `Offset` y
> `Rotate` ([Capas y giro](layers-and-rotation.md)). `Row header`, también: un `For column` llega a las celdas de
> `Header rows`, donde es un error (`ROW_HEADER_IN_HEADER_ROWS`). *Alternativa descartada:* todas las del elemento salvo
> `Id` y `Style`.

> **Decisión:** `Marker` en `List` y `Column sizes` en `Table` dejan de ser obligatorios en el schema y los exige el
> motor, para que puedan venir de una regla. *Alternativa descartada:* seguir obligatorios en el elemento, con lo que la
> regla nunca tendría efecto.

## Colores con nombre

`Named color`, hijo de `Styles` y repetible, da nombre a un color: el nombre y el color van separados por un espacio. Desde
ese momento el nombre se escribe donde va un color, en las reglas y en los elementos: `Color`, `Border color`,
`Background` (también dentro de `linear(...)`) y `Separator color` ([Valores](values.md#colores)).

```
Styles:
    Named color: brand #12315A
    Named color: mist #EEF3F8
    For element: Heading 1
        Typography:
            Color: brand
    For style: panel
        Box:
            Background: mist
            Border: 0 0 0 3
            Border color: brand
Text: Una nota sobre fondo claro.
    Style: panel
    Box:
        Background: linear(180, mist, #FFFFFF)
```

- **El nombre** se escribe en minúsculas, con letras de la `a` a la `z` y cifras, empieza por una letra y une sus
  palabras con un guion: `brand`, `dark-gray`, `gray-2`. `linear` no puede ser un nombre.
- **El color** es un color hexadecimal, con su opacidad si la tiene (`#12315A80`). No puede ser otro nombre ni un
  degradado.
- **El orden no importa**: una regla puede usar un nombre declarado después en `Styles`.
- **Un nombre se sustituye por su color al leer el documento.** El resultado es el mismo que con el color escrito en su
  lugar, y los mensajes que citan un color (como el de `CONTRAST_LOW`) dan su valor hexadecimal.
- **El documento gana al documento de estilos aparte.** Los nombres de los dos orígenes forman un solo conjunto; si los
  dos declaran el mismo nombre, vale el color del documento, también en las reglas del documento de estilos aparte.
  Dentro de un mismo origen, un nombre repetido es `COLOR_DUPLICATE`. Entre varios documentos de estilos aparte gana
  el que se pasa después ([Varios documentos de estilos](#varios-documentos-de-estilos)).
- Un color con forma de nombre que ningún `Named color` declara es `COLOR_UNKNOWN`, con la línea de la propiedad. Un
  `Named color` que no se usa no es un error.

> **Decisión:** un nodo `Named color` por color, repetible. *Alternativa descartada:* un bloque `Colors >>` con una línea
> por color, que el schema no puede validar línea a línea.

> **Decisión:** un documento puede cambiar un color de su documento de estilos aparte declarando el mismo nombre, y el
> cambio llega a todas las reglas que lo usan. *Alternativa descartada:* que cada regla tome los nombres de su propio
> origen, que obligaría a copiar las reglas para cambiar un color.

> **Decisión:** solo colores hexadecimales con nombre. *Alternativa descartada:* un nombre definido con otro nombre o
> con un degradado, que añade cadenas y ciclos sin una necesidad clara.

## Prioridad

Cada propiedad toma, de más a menos fuerte:

1. lo que declara el propio elemento;
2. la regla `For id` de su `Id`;
3. las reglas `For style` de sus estilos (si varias declaran la misma propiedad, gana la que va **después en `Styles`** y
   no el orden de `Style`);
4. las reglas `For style` de los estilos de la `Row` de una celda, con el mismo criterio de orden (la regla `For id` del
   `Id` de su `Row` va entre los puntos 2 y 3);
5. las reglas `For column` de la columna de una celda, con el mismo criterio de orden;
6. las reglas de posición de una celda (`Header cell`, `Body cell`, `Footer cell`, y después `Odd row cell` o
   `Even row cell`);
7. el `Column align` de su tabla, solo para `Align`;
8. la regla `For element` de su tipo;
9. lo heredado (solo `Typography`): para una celda, la de su `Row` y, si no, la de su padre;
10. el valor por defecto.

Las reglas de los puntos 4 a 7 solo se aplican a las celdas de una `Table` ([Table](../components/table.md#estilo-de-las-celdas)).
Las [propiedades de elemento](#propiedades-de-elemento) siguen el mismo orden, sin el punto 9: no se heredan. Para
`Grow`, `Shrink` y `Base size`, lo que declara el `Item` es el punto 1.

Una regla gana a la herencia: con `For element: Text` y `Size: 10`, un `Text` mide 10 aunque su padre declare
`Size: 20`. Las reglas se mezclan **propiedad a propiedad** y no por grupo: si la regla declara `Size` y el elemento
`Bold`, se aplican las dos. Lo que una regla da a un elemento en `Typography` lo heredan sus descendientes, como si lo
hubiera declarado él. `Item` y `Area` no son cajas: las reglas se aplican al elemento que envuelven. El `Style` y el
`Id` de una `Row` aplican sus reglas a cada una de sus celdas y no a la franja de la fila. `Header rows`, `Body`,
`Footer rows` y `Row` no se pueden nombrar en `For element` (para dar estilo a las celdas de cada grupo están las reglas
de posición). `Table` y `Cell`, sí. A una `Section` solo le llega `Typography`
([Document](../components/document.md#section)).

> **Decisión:** prioridad fija en lugar de especificidad calculada. *Alternativa descartada:* especificidad calculada y
> selectores compuestos, más potentes pero menos predecibles.

## Validación

Lo que aporta una regla se valida igual que si lo declarara el elemento ([Errores](errors.md)), y el error señala la
línea de la propiedad dentro de la regla. Los valores de una regla se comprueban aunque no se aplique a ningún elemento.
Son errores:

- un `For element` que no nombra un elemento de caja, un elemento generado ni una posición en una tabla
  (`STYLE_UNKNOWN_ELEMENT`);
- un `For column` que no es un número (`VALUE_SYNTAX`) o un rango que no va de menor a mayor (`VALUE_NOT_ALLOWED`);
- dos reglas con el mismo selector en el mismo origen, también dos `For column` con el mismo rango (`STYLE_DUPLICATE`);
- una regla que da `Break` a un elemento que no lo admite: `Overlay`, `Page overlay`, un encabezado, un `Text` de un encabezado o
  una celda, también con una regla de posición, de columna o del estilo de su `Row` (`STYLE_NOT_ALLOWED`);
- una regla que da a una `Section` algo que no es `Typography` (`STYLE_NOT_ALLOWED`);
- una propiedad de elemento que el elemento no puede declarar: en `For element`, al leer la regla; en `For style` y
  `For id`, al aplicarla; también `Grow`, `Shrink` o `Base size` a un elemento que no está en un `Item`
  (`STYLE_NOT_ALLOWED`);
- una propiedad de elemento con un valor que no vale en ningún elemento que la tenga (`VALUE_SYNTAX`,
  `VALUE_NOT_ALLOWED`);
- un nombre de `Style` o de un tramo con estilo sin su regla `For style` (`STYLE_WITHOUT_RULE`);
- la regla de un tramo con estilo que declara algo que un tramo no admite, como `Size`, un `Box` o una propiedad de
  elemento (`STYLE_NOT_ALLOWED_IN_RUN`);
- dos `Named color` con el mismo nombre en el mismo origen (`COLOR_DUPLICATE`);
- un color con forma de nombre que ningún `Named color` declara (`COLOR_UNKNOWN`);
- un `Named color` sin nombre y color separados por un espacio, con un nombre mal escrito o con un color que no es
  hexadecimal (`VALUE_SYNTAX`), o con el nombre `linear` (`VALUE_NOT_ALLOWED`).

Un `For element`, un `For style`, un `For id` o un `For column` que no se aplican a ningún elemento no son errores: así las
reglas se pueden reutilizar, y un elemento con ese `Id` o ese estilo puede llegar después (de una plantilla, por ejemplo).

> **Decisión:** una regla por tipo puede dar un error en un elemento concreto (un `Margin: auto` que no centra en un ítem
> de `Stack`), con la línea de la regla. *Alternativa descartada:* ignorar lo que no tiene efecto cuando viene de una
> regla.

## La tipografía base

`Document` no admite `Typography`: lo que hereda toda la descendencia (por lo general `Font` y `Size`, que no tienen valor
por defecto) se declara en la regla `For element: Document`, que solo admite `Typography` (con `Box` o `Break`, o con
`Language`, es `STYLE_NOT_ALLOWED`):

```
Styles:
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
```

Es la base de la herencia: la regla `For element` de un tipo o lo que declara una caja la cambian para esa caja y sus
descendientes.

> **Decisión:** la base va en una regla y no en `Document`, para que esté con el resto del estilo y se pueda llevar al
> documento de estilos aparte.

## Documento de estilos aparte

Las reglas y los colores con nombre pueden escribirse en un documento aparte, de raíz `Styles` con el namespace
`dev.stxt.pdf`, para quitarlos del documento principal o compartirlos entre varios:

```
Styles (dev.stxt.pdf):
    Named color: brand #12315A
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
    For style: panel
        Box:
            Padding: 10
            Border: 0 0 0 3
            Border color: brand
```

- **Se pasa al maquetar, nunca se busca.** El documento no dice dónde está su documento de estilos y el motor no busca
  ficheros ni carpetas: quien convierte lo lee y lo pasa junto al documento
  ([Uso desde Java](../implementation/api.md#estilos-aparte)). Es opcional, y puede haber
  [varios](#varios-documentos-de-estilos).
- **Vale menos que el documento.** Se admiten las mismas reglas que en el `Styles` del documento. En cada nivel de la
  prioridad (el de `For id`, el de `For style`, el de `For element`, etc.), las reglas del documento de estilos aparte van **antes** y
  las del documento **después**, así que gana el documento, propiedad a propiedad. La prioridad entre niveles no cambia:
  un `For style` del documento aparte sigue ganando a un `For element` del documento. Con esto, sacar el `Styles` de un
  documento a un fichero aparte no cambia el resultado. Los colores con nombre siguen la misma regla: si los dos
  orígenes declaran un nombre, gana el color del documento ([Colores con nombre](#colores-con-nombre)).
- **Cada origen es estricto por separado.** Dos reglas con el mismo selector son un error dentro de un origen
  (`STYLE_DUPLICATE`), como dos `Named color` con el mismo nombre (`COLOR_DUPLICATE`), pero la misma regla o el mismo
  nombre pueden estar en los dos. Un nombre de `Style` puede tener su `For style` en
  cualquiera de los dos.
- **Errores.** Una regla del documento de estilos se valida como una del documento, y su error lleva la línea **de ese
  fichero** y lo dice: `line 8 of the styles: ...`. La raíz que no es `Styles` es `DOCUMENT_ROOT`. Una propiedad que
  llega de una regla del documento de estilos conserva su línea en él, también la tipografía que se hereda, así que lo
  mismo vale para los errores que se ven al aplicarla o al maquetar (una fuente que no existe, un desbordamiento). Si un
  mensaje nombra otra línea, también dice de qué documento es: `line 8 of the styles: Min width is greater than Max width
  (line 10)`.

> **Decisión:** la prioridad entre los dos orígenes es por nivel, así que sacar el `Styles` a un fichero no cambia nada.
> *Alternativa descartada:* que todas las reglas del fichero valgan menos que cualquiera del documento.

> **Decisión:** el documento no nombra su documento de estilos: lo pasa quien convierte. *Alternativa descartada:* una
> propiedad `Styles file` en `Document`.

### Varios documentos de estilos

Con un documento se pueden pasar varios documentos de estilos aparte, en un orden. El primero:

```
Styles (dev.stxt.pdf):
    Named color: brand #12315A
    For element: Heading 1
        Typography:
            Size: 20
            Color: brand
```

Y el segundo:

```
Styles (dev.stxt.pdf):
    Named color: brand #A32D2D
    For element: Heading 1
        Typography:
            Size: 16
```

Un `Heading 1` mide 16 y es de color `#A32D2D`: el tamaño y el color de `brand` son los del segundo, y `Color` solo lo
declara el primero.

- **Gana el que se pasa después.** En cada nivel de la prioridad van primero las reglas del primer documento de estilos,
  después las del segundo, y así hasta el último; las del documento van al final. Si varios declaran la misma propiedad
  en el mismo nivel, vale la del último, y el documento gana a todos. La prioridad entre niveles no cambia: un
  `For style` del primero sigue ganando a un `For element` del último.
- **El resultado es el de un solo documento de estilos** con las reglas de todos, unas detrás de otras en ese orden. Así,
  partir un documento de estilos en dos que se pasan en el mismo orden no cambia el resultado.
- **Los colores con nombre** siguen la misma regla: los nombres de todos forman un solo conjunto y, si varios declaran
  un nombre, vale el color del último, también en las reglas de los anteriores.
- **Cada uno es estricto por separado.** Una regla o un nombre repetidos son un error dentro de un documento de estilos
  (`STYLE_DUPLICATE`, `COLOR_DUPLICATE`), no entre dos.
- **Errores.** Con varios documentos de estilos, el mensaje dice de cuál es la línea, por el lugar en que se pasó:
  `line 8 of styles 2: ...` es la línea 8 del segundo. Con uno solo, el mensaje es `line 8 of the styles: ...`.

> **Decisión:** entre los documentos de estilos aparte gana el que se pasa después, el mismo criterio que entre las
> reglas de un `Styles` y que entre el documento de estilos aparte y el documento. *Alternativa descartada:* que la
> misma regla en dos documentos de estilos sea un error, que impide tener unos estilos comunes y cambiar parte de ellos
> en otro documento de estilos.

> **Decisión:** en los errores, un documento de estilos se nombra por el lugar en que se pasó. *Alternativa
> descartada:* darle un nombre al pasarlo, más API para lo que quien convierte ya sabe: qué pasó y en qué orden.

## Lo que no hay

- Selectores compuestos (un elemento dentro de otro, varios estilos a la vez), combinadores ni prioridades declaradas por el
  autor.
- Propiedades de identidad, de contenido, de estructura o de posición en una regla
  ([Propiedades de elemento](#propiedades-de-elemento)).
- Estilo de una fila de tabla con `Box` o de una columna entera: se da a las celdas ([Table](../components/table.md#lo-que-no-hay)).
