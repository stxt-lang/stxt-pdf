# `Markdown`

Un `Markdown` es un bloque cuyo contenido se escribe en Markdown. El motor lo traduce a los elementos de siempre
(encabezados, párrafos, listas, tablas, imágenes), que se estilan con las mismas reglas de `Styles` que los escritos
en STXT. Sirve para texto largo con formato.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Condiciones
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 16
                Bold: true
            Box:
                Margin: 0 0 10 0
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
        For element: List
            Box:
                Padding: 0 0 0 18
        For element: Code block
            Box:
                Padding: 8
                Background: #F1EFE8
    Section:
        Markdown:
            Typography:
                Code font: IBM Plex Mono
            Bullet markers: bullet dash
            Number markers: decimal
            Table column sizes: auto
            Content >>
                # Condiciones de pago

                El pago se hará en **30 días** desde la fecha de la factura, por
                transferencia a la cuenta `ES12 3456 7890`.

                1. Envío de la factura.
                2. Pago:
                   - por transferencia;
                   - o en efectivo, en el taller.

                | Concepto | Importe |
                |----------|--------:|
                | Sillas   | 1740,00 |
                | Mesa     |  890,00 |

                ~~~
                Referencia: 2026-014
                ~~~

                Marta Vidal\
                Administración
```

## Propiedades

| Propiedad | Qué hace |
|---|---|
| `Content` | El Markdown, en un bloque `>>`. Obligatorio |
| `Bullet markers` | El marcador de las listas con viñetas, uno por nivel: `bullet dash` usa `•` en el primer nivel y `–` en los siguientes. Obligatorio si hay listas con viñetas |
| `Number markers` | Igual para las listas numeradas: `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman`. Obligatorio si hay listas numeradas |
| `Table column sizes` | Las columnas de las tablas: `equal` (todas iguales) o `auto` (según su contenido). Obligatorio si hay tablas |

`Bullet markers`, `Number markers` y `Table column sizes` también pueden ir una vez para todo el documento, en una regla
`For element: Markdown` ([Estilos](../common/styles.md#propiedades-de-un-elemento)).

Admite además `Id`, `Style`, `Box`, `Typography` (que hereda todo lo de dentro) y `Break`. La fuente del código es la
`Code font` de `Typography` ([Formato del texto](../common/text-formatting.md#código)): se declara en el `Typography`
del `Markdown` o, una vez para todo el documento, en `For element: Document`, y es obligatoria si hay código.

## Lo que se admite

| Markdown | Sale |
|---|---|
| `#` a `######` | `Heading 1` a `Heading 6`. Con `{#id}` al final (`## Ventas {#ventas}`), lleva ese `Id`, para enlazarlo o citarlo |
| Un párrafo | `Paragraph` |
| `-`, `*`, `+` y `1.` | `List`, con viñetas o numerada |
| Una tabla | `Table`, con la primera fila de cabecera. La alineación de cada columna sale de la fila de separación (`:--`, `--:`). A una fila con menos celdas que la cabecera se le añaden vacías, y una con más es un error |
| `**negrita**`, `*cursiva*`, `++subrayado++`, `~~tachado~~` | Formato dentro de la línea |
| `[texto](destino)` | Un enlace, subrayado ([Referencias y enlaces](../common/references-and-links.md)) |
| `` `código` `` | Código en línea, con `Code font` |
| `[texto]{nombre}` | Una parte con el color o la fuente de la regla `For style: nombre` ([Formato del texto](../common/text-formatting.md#partes-con-otro-color-u-otra-fuente)) |
| Un bloque de código (entre ```` ``` ```` o `~~~`, o sangrado cuatro espacios) | Un bloque preformateado con `Code font`, que se estila con `For element: Code block` |
| `> texto` | Una cita: un bloque, al que `For element: Block quote` da su sangría, su línea o su fondo |
| `---`, `***` o `___` | Una línea horizontal, que se estila con `For element: Divider` |
| `![texto alternativo](ruta)`, sola en su párrafo | Una `Image` |
| `![texto alternativo](ruta)`, dentro de un párrafo | Una imagen pequeña en la frase, tan alta como la letra ([Formato del texto](../common/text-formatting.md#imágenes-en-la-frase)) |
| `\` al final de la línea | Un salto de línea dentro del párrafo, como el de la firma del ejemplo |

Lo demás es un error, para que nada se pierda en silencio. Por ejemplo: HTML, imágenes o
enlaces con título, o la etiqueta de lenguaje de un bloque de código (no hay resaltado).

## Estilo

Lo que genera un `Markdown` no tiene `Style`, ni `Id` salvo los encabezados con `{#id}`: se le da estilo con las reglas
`For element` de `Styles` (`For element: Heading 1`, `For element: Paragraph`, `For element: List`, `For element: Code block`), que valen
para todo el documento, y con la `Typography` del `Markdown`. Nada tiene estilo por defecto, salvo la línea de `---`, que mide 0,5: una cita sin `For element: Block quote` se ve como un párrafo, y sin `For element: List` con un
`Padding` izquierdo, las listas no tienen sitio para el marcador.

## Cosas a tener en cuenta

- Los encabezados de un `Markdown` cuentan en los niveles del documento: después de un `Heading 1` escrito en STXT, un
  Markdown puede empezar por `##`. Se numeran y salen en el índice como los demás.
- Lo generado no tiene `Id`, salvo un encabezado que lo escribe al final, `## Ventas {#ventas}`: así se enlaza
  (`[ver ventas](#ventas)`) o se cita (`{page of: ventas}`, `{number of: ventas}`).
- Un salto de línea se escribe con `\` al final de la línea. Los dos espacios al final que admite también Markdown no
  sirven, porque STXT quita los espacios del final de cada línea. La línea que acaba en un salto no se justifica.
- Los errores dicen la línea del documento donde empieza el bloque de Markdown que falla.
- Para negrita o cursiva en un párrafo suelto, no hace falta un `Markdown`: basta `Markup: markdown`
  ([Formato del texto](../common/text-formatting.md)).

Más detalle en la [referencia](../../reference/components/markdown.md).
