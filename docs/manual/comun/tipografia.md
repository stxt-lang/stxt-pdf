# Tipografía: `Typography`

El grupo `Typography` controla el texto. Es el único que **se hereda**: lo que declara una caja vale para todo el texto que
contiene, salvo lo que cambie una caja de dentro. Por eso se declara la base una vez para todo el documento y cada elemento
cambia solo lo suyo:

```
Styles:
	Element: Document
		Typography:
			Font: IBM Plex Serif
			Size: 10.5
			Line height: 15
			Align: justify
	Element: Heading 1
		Typography:
			Font: IBM Plex Sans
			Size: 20
			Weight: bold
			Align: left
```

| Propiedad | Valores | Por defecto | Qué hace |
|---|---|---|---|
| `Font` | nombre de familia | — | La fuente. **Obligatoria** en todo texto, propia o heredada |
| `Code font` | nombre de familia | — | La fuente del código ([Formato del texto](formato-del-texto.md#código)). **Obligatoria** si hay código |
| `Size` | número > 0 | — | El tamaño de la letra, en puntos. **Obligatorio** en todo texto |
| `Weight` | `normal`, `bold` | `normal` | Negrita |
| `Style` | `normal`, `italic` | `normal` | Cursiva |
| `Line height` | número o % | `120%` | La altura de cada línea: `15` son 15 puntos; `140%`, 1,4 veces el tamaño |
| `Align` | `left`, `right`, `center`, `justify` | `left` | La alineación de las líneas |
| `Color` | color | `#000000` | El color del texto |
| `Text wrap` | `auto`, `pretty` | `auto` | Cómo se reparte el texto en líneas |
| `Widows`, `Orphans` | entero ≥ 1 | `2` | Líneas de un párrafo que no se quedan solas al cambiar de página |
| `Language` | `es`, `en-GB`, `ca`… | el del documento | El idioma del texto, si no es el del documento |
| `Numbered` | `true`, `false` | `false` | Si los encabezados llevan número ([Encabezados](../componentes/heading.md)) |
| `Number style`, `Number label` | | | Cómo se escribe ese número y lo que va delante del título ([Encabezados](../componentes/heading.md)) |

El resto (`Inline`, `Transform`, `Small caps`, `Letter spacing`, `Ligatures`, `White space` y `Tab size`) está en
[Formato del texto](formato-del-texto.md).

## Fuente y tamaño

No hay fuente por defecto: sin `Font` y `Size`, un texto es un error. La fuente se nombra por su familia, tal como la
declara su fichero (`IBM Plex Sans`, con mayúsculas y espacios), y tiene que estar en la carpeta de fuentes
([Recursos](recursos.md)). El repositorio trae **IBM Plex Sans**, **IBM Plex Serif** e **IBM Plex Mono**, cada una en
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

El texto se parte en líneas del ancho de su caja, siempre entre palabras. Con `Text wrap: auto`, cada línea se llena todo lo
que puede. Con `Text wrap: pretty`, el motor mira el párrafo entero y elige los cortes que dejan las líneas más parejas, que
se nota sobre todo en texto justificado. No se parten palabras con guion: una palabra más ancha que la línea es un error.

Los espacios seguidos y los saltos de línea simples son un solo espacio; una línea en blanco separa dos párrafos.

## Viudas y huérfanas

Cuando un párrafo cambia de página, `Orphans` es el mínimo de líneas que se quedan al final de la página y `Widows`, el
mínimo que pasan a la siguiente. Con los dos a 2 (lo normal), un párrafo no deja nunca una línea suelta. Si no hay otra
forma de cortar, el motor relaja la regla antes que dejar una página a medias ([Páginas y cortes](paginas-y-cortes.md)).

## Idioma

El idioma del documento es su `Language`. Si una parte está en otro idioma (una cita en inglés, un anexo en catalán),
declárale el suyo en `Typography`: el lector de pantalla cambia de voz ahí, y las mayúsculas automáticas siguen las reglas
de ese idioma.

```
Paragraph: «To be or not to be», dice el personaje.
	Typography:
		Language: en-GB
```

El idioma es de la caja entera, no de una palabra dentro de una línea.

Más detalle en la [referencia](../../referencia/comun/tipografia.md).
