# Tipografía: `Typography`

El grupo `Typography` controla el texto. Es el único que **se hereda**: lo que declara una caja vale para todo el texto que
contiene, salvo lo que cambie una caja de dentro. Por eso se declara la base una vez para todo el documento y cada elemento
cambia solo lo suyo:

```
Styles:
    For element: Document
        Typography:
            Font: IBM Plex Serif
            Size: 10.5
            Line height: 15
            Align: justify
    For element: Heading 1
        Typography:
            Font: IBM Plex Sans
            Size: 20
            Bold: true
            Align: left
```

| Propiedad | Valores | Por defecto | Qué hace |
|---|---|---|---|
| `Font` | nombre de familia | sin valor | La fuente. **Obligatoria** en todo texto, propia o heredada |
| `Code font` | nombre de familia | sin valor | La fuente del código ([Formato del texto](text-formatting.md#código)). **Obligatoria** si hay código |
| `Size` | número > 0 | sin valor | El tamaño de la letra. **Obligatorio** en todo texto |
| `Min size` | número > 0 | `6` | Hasta dónde puede encoger la letra con `Overflow: shrink` ([Caja](box.md#lo-que-no-cabe)) |
| `Bold` | `true`, `false` | `false` | Negrita: lo mismo que `Weight: 700` |
| `Weight` | de 100 a 900, de 100 en 100 | `400` | El peso de la letra ([abajo](#pesos)) |
| `Italic` | `true`, `false` | `false` | Cursiva |
| `Line height` | número o % | `120%` | La altura de cada línea: `15` son 15 puntos y `140%`, 1,4 veces el tamaño |
| `Align` | `left`, `right`, `center`, `justify` | `left` | La alineación de las líneas |
| `Color` | color | `#000000` | El color del texto |
| `Line breaking` | `simple`, `optimal` | `simple` | Cómo se reparte el texto en líneas |
| `Hyphenate` | `true`, `false` | `false` | Si se parten las palabras con guion al final de la línea |
| `Break long words` | `never`, `new-line`, `same-line` | `never` | Si se corta, sin guion, una palabra que no cabe en una línea entera, y dónde empieza ([abajo](#palabras-largas)) |
| `First line indent` | medida | `0` | Lo que se sangra la primera línea de cada párrafo ([abajo](#sangría-de-primera-línea)) |
| `Kerning` | `true`, `false` | `true` | Si se ajusta el espacio de las parejas de letras que lo piden (`AV`, `To`) |
| `Widows`, `Orphans` | entero ≥ 1 | `2` | Líneas de un párrafo que no se quedan solas al cambiar de página |
| `Language` | etiqueta BCP 47: `es`, `en-GB`, `ca` | el del documento | El idioma del texto, si no es el del documento |
| `Numbered` | `true`, `false` | `false` | Si los encabezados llevan número ([Encabezados](../components/heading.md)); las figuras lo llevan salvo que digan `false` ([Figure](../components/figure.md)) |
| `Number format`, `Number label` | | | Cómo se escribe ese número y lo que va delante del título, o del pie de una figura ([Encabezados](../components/heading.md)) |

El resto (`Markup`, `Case`, `Small caps`, `Position`, `Letter spacing`, `Ligatures`, `Preformatted` y `Tab size`) está en
[Formato del texto](text-formatting.md).

## Fuente y tamaño

No hay fuente por defecto: sin `Font` y `Size`, un texto es un error. La fuente se nombra por su familia, tal como la
declara su fichero (`IBM Plex Sans`, con mayúsculas y espacios), y tiene que estar en la carpeta de fuentes
([Recursos](resources.md)). El repositorio trae **IBM Plex Sans**, **IBM Plex Serif** e **IBM Plex Mono**, cada una en
normal, negrita, cursiva y negrita cursiva, y la Sans además en los pesos 300, 500 y 600. Si un texto usa un carácter que
su fuente no tiene, es un error: el motor no busca otra fuente ni pinta un cuadradito.

## Pesos

`Weight` elige lo gruesa que es la letra, de 100 a 900: 400 es la normal, 700 la negrita, y los demás, los pesos
intermedios que trae la familia (300 ligera, 500 media, 600 seminegrita...). Cada peso es un fichero de la fuente: uno que
la familia no tiene es un error. `Bold: true` es otra forma de escribir `Weight: 700`, y los dos no van juntos en el mismo
`Typography`.

```stxt
Document (dev.stxt.pdf):
    Language: es
    Title: Pesos de la letra
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
    Section:
        Text: Marta Ferrer Soler
            Typography:
                Size: 28
                Weight: 300
        Text: Diseñadora de producto
            Typography:
                Weight: 600
        Paragraph: Ocho años entre estudio, cooperativa y **producto propio**.
            Typography:
                Markup: markdown
```

Una negrita dentro de la frase (`**producto propio**`) es el 700, o el peso del texto si ya es 700 o más.

## Altura de las líneas

Cada línea mide exactamente `Line height`, y un texto mide sus líneas por `Line height`. Un número son **puntos**: con
`Size: 10`, `Line height: 14` deja 4 puntos de aire. Un porcentaje es relativo al tamaño de la letra (`120%`, el valor por
defecto, son 12 puntos con `Size: 10`). La letra se centra en la altura de la línea.

## Alineación

`left`, `right` y `center` alinean cada línea. `justify` estira los espacios para que cada línea llegue a los dos lados,
salvo la última de cada párrafo, las de una sola palabra y las líneas preformateadas.

## Reparto en líneas

El texto se parte en líneas del ancho de su caja, entre palabras o tras un guion escrito dentro de una palabra
(`REF-2026-` y `009812`). Con `Line breaking: simple`, cada línea se llena todo lo
que puede. Con `Line breaking: optimal`, el motor mira el párrafo entero y elige los cortes que dejan las líneas más parejas, que
se nota sobre todo en texto justificado. Por defecto las palabras no se parten por ningún otro sitio: una palabra más ancha que la línea es un error.

Los espacios seguidos y los saltos de línea simples son un solo espacio. Una línea en blanco es un error: cada texto es
un solo párrafo, y para varios se escriben varios `Paragraph`.

## Guiones

Con `Hyphenate: true` el motor parte las palabras con guion al final de la línea donde lo permiten las reglas del idioma
de la caja (`Language`), como hace un procesador de textos, y las columnas estrechas justificadas quedan sin huecos.
Tiene reglas para el castellano, el catalán, el inglés americano y británico, el alemán (con la ortografía de 1996), el
francés, el italiano, el neerlandés y el portugués; en otro idioma, `Hyphenate: true` es un error. Lo normal es
declararlo una vez, en `For element: Document`:

```
Document (dev.stxt.pdf):
    Language: es
    Title: Guiones
    Page:
        Paper: A5
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 10
                Align: justify
                Hyphenate: true
    Section:
        Columns:
            Column count: 3
            Gap: 12
            Paragraph: Los patrones de partición dividen las palabras largas por sus sílabas, y así las columnas estrechas quedan sin huecos entre las palabras.
```

Un guion blando (U+00AD) escrito dentro de una palabra dice dónde puede partirse esa palabra, con o sin `Hyphenate`, y
no se ve si no se parte. El texto que se copia del PDF, y el que lee un lector de pantalla, no lleva los guiones que puso
el motor.

Una palabra con un guion escrito entre letras o cifras (`físico-químico`, `REF-2026-009812`) se puede partir detrás
de ese guion, también sin `Hyphenate`, y no se le añade otro. Para que no se parta ahí, el guion se escribe con el
guion de no separación (U+2011).

## Palabras largas

Un IBAN, una dirección de correo o una URL no tienen por dónde partirse, y en una celda estrecha no caben: son un
error. Con `Break long words: new-line`, una palabra más ancha que una línea entera de su caja empieza en una línea
propia, se corta por la última letra que cabe, sin guion, y sigue en la línea siguiente:

```
Styles:
    For element: Cell
        Typography:
            Break long words: new-line
```

Con `same-line`, la palabra no empieza en una línea propia: sigue a las palabras anteriores y se corta donde acaba
esa línea, sin dejar una línea corta delante. `never`, el valor por defecto, no la corta.

Solo se corta lo que no cabe de ninguna otra forma: una palabra que cabe en una línea propia pasa entera a la línea
siguiente, y una que tiene guiones se parte antes por ellos. El texto que se copia del PDF es la palabra entera. No
tiene efecto en un texto con `Preformatted`.

## Sangría de primera línea

En un contrato o en un informe largo los párrafos se distinguen por la sangría de su primera línea. `First line indent`
la da, en puntos o en milímetros. Se hereda, y lo normal es declararla en una regla para los párrafos, que no toca los
encabezados ni las celdas:

```
Styles:
    For element: Paragraph
        Typography:
            First line indent: 14
```

Solo se sangra la primera línea del párrafo: no la que sigue a un salto de línea forzado, ni la primera de la página
siguiente cuando el párrafo continúa en ella.

Con un valor negativo la primera línea empieza antes que las demás: es la **sangría francesa** de las cláusulas de un
contrato, las bibliografías y los glosarios. Esa línea va en el `Padding` izquierdo de la caja, que tiene que medir al
menos lo mismo:

```
Styles:
    For style: clause
        Box:
            Padding: 0 0 0 30
        Typography:
            First line indent: -30
```

```
Text: PRIMERO.— El arrendador cede al arrendatario el uso de la vivienda, que este destina a domicilio habitual.
    Style: clause
```

Un párrafo con letra capitular (`Drop cap`) no se sangra: la sangría que recibe de una regla no se le aplica.

## Kerning

Algunas parejas de letras dejan un hueco visible entre ellas por su forma: `AV`, `To`, `P.`. Las fuentes traen una lista
de esas parejas con lo que hay que acercarlas, o separarlas: el **kerning**. Se aplica por defecto, y se nota sobre todo
en los títulos grandes. `Kerning: false` lo quita, en un texto o en todo el documento:

```
Document (dev.stxt.pdf):
    Language: es
    Title: Kerning
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
    Section:
        Text: AVANCE TÉCNICO
            Typography:
                Size: 36
                Bold: true
        Text: AVANCE TÉCNICO
            Typography:
                Size: 36
                Bold: true
                Kerning: false
        Paragraph: Torre Vall, octubre de 2025.
```

Una pareja no se forma por encima de un espacio ni entre una letra en negrita y otra que no lo está. IBM Plex Sans y Serif
traen kerning; IBM Plex Mono, en la que todas las letras miden lo mismo, no. El texto que se copia del PDF no cambia.

## Viudas y huérfanas

Cuando un párrafo cambia de página, `Orphans` es el mínimo de líneas que se quedan al final de la página y `Widows`, el
mínimo que pasan a la siguiente. Con los dos a 2 (lo normal), un párrafo no deja nunca una línea suelta. Si no hay otra
forma de cortar, el motor relaja la regla antes que dejar una página a medias ([Páginas y cortes](pages-and-breaks.md)).

## Idioma

El idioma del documento es su `Language`. Una parte en otro idioma (una cita en inglés, un anexo en catalán) declara el
suyo en `Typography`. El lector de pantalla cambia de voz ahí, y las mayúsculas automáticas siguen las reglas de ese
idioma.

```
Paragraph: «To be or not to be», dice el personaje.
    Typography:
        Language: en-GB
```

El idioma es de la caja entera, no de una palabra dentro de una línea.

Más detalle en la [referencia](../../reference/common/typography.md).
