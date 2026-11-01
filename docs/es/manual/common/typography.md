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
| `Bold` | `true`, `false` | `false` | Negrita |
| `Italic` | `true`, `false` | `false` | Cursiva |
| `Line height` | número o % | `120%` | La altura de cada línea: `15` son 15 puntos y `140%`, 1,4 veces el tamaño |
| `Align` | `left`, `right`, `center`, `justify` | `left` | La alineación de las líneas |
| `Color` | color | `#000000` | El color del texto |
| `Line breaking` | `simple`, `balanced` | `simple` | Cómo se reparte el texto en líneas |
| `Hyphenate` | `true`, `false` | `false` | Si se parten las palabras con guion al final de la línea |
| `Widows`, `Orphans` | entero ≥ 1 | `2` | Líneas de un párrafo que no se quedan solas al cambiar de página |
| `Language` | etiqueta BCP 47: `es`, `en-GB`, `ca` | el del documento | El idioma del texto, si no es el del documento |
| `Numbered` | `true`, `false` | `false` | Si los encabezados llevan número ([Encabezados](../components/heading.md)) |
| `Number format`, `Number label` | | | Cómo se escribe ese número y lo que va delante del título ([Encabezados](../components/heading.md)) |

El resto (`Markup`, `Case`, `Small caps`, `Letter spacing`, `Ligatures`, `Preformatted` y `Tab size`) está en
[Formato del texto](text-formatting.md).

## Fuente y tamaño

No hay fuente por defecto: sin `Font` y `Size`, un texto es un error. La fuente se nombra por su familia, tal como la
declara su fichero (`IBM Plex Sans`, con mayúsculas y espacios), y tiene que estar en la carpeta de fuentes
([Recursos](resources.md)). El repositorio trae **IBM Plex Sans**, **IBM Plex Serif** e **IBM Plex Mono**, cada una en
normal, negrita, cursiva y negrita cursiva. Si un texto usa un carácter que su fuente no tiene, es un error: el motor no
busca otra fuente ni pinta un cuadradito.

## Altura de las líneas

Cada línea mide exactamente `Line height`, y un texto mide sus líneas por `Line height`. Un número son **puntos**: con
`Size: 10`, `Line height: 14` deja 4 puntos de aire. Un porcentaje es relativo al tamaño de la letra (`120%`, el valor por
defecto, son 12 puntos con `Size: 10`). La letra se centra en la altura de la línea.

## Alineación

`left`, `right` y `center` alinean cada línea. `justify` estira los espacios para que cada línea llegue a los dos lados,
salvo la última de cada párrafo, las de una sola palabra y las líneas preformateadas.

## Reparto en líneas

El texto se parte en líneas del ancho de su caja, siempre entre palabras. Con `Line breaking: simple`, cada línea se llena todo lo
que puede. Con `Line breaking: balanced`, el motor mira el párrafo entero y elige los cortes que dejan las líneas más parejas, que
se nota sobre todo en texto justificado. Por defecto no se parten palabras: una palabra más ancha que la línea es un error.

Los espacios seguidos y los saltos de línea simples son un solo espacio. Una línea en blanco es un error: cada texto es
un solo párrafo, y para varios se escriben varios `Paragraph`.

## Guiones

Con `Hyphenate: true` el motor parte las palabras con guion al final de la línea donde lo permiten las reglas del idioma
de la caja (`Language`), como hace un procesador de textos, y las columnas estrechas justificadas quedan sin huecos.
Tiene reglas para el castellano, el catalán y el inglés americano y británico; en otro idioma, `Hyphenate: true` es un
error. Lo normal es declararlo una vez, en `For element: Document`:

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
            Count: 3
            Gap: 12
            Paragraph: Los patrones de partición dividen las palabras largas por sus sílabas, y así las columnas estrechas quedan sin huecos entre las palabras.
```

Un guion blando (U+00AD) escrito dentro de una palabra dice dónde puede partirse esa palabra, con o sin `Hyphenate`, y
no se ve si no se parte. El texto que se copia del PDF, y el que lee un lector de pantalla, no lleva los guiones que puso
el motor.

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
