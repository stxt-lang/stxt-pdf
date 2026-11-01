# `Footnote`

Una `Footnote` es una **nota al pie**. En el texto se escribe la llamada, `[^id]`, pegada a la palabra, y en cualquier
sitio de la sección, la nota con ese `Id`. El motor numera las llamadas en orden, las pinta en superíndice y coloca
cada nota al pie de la página donde cae su llamada, bajo un filete.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe
    Page:
        Paper: A4
        Margin: 60
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 11
                Line height: 16
                Markup: markdown
        For element: Paragraph
            Box:
                Margin: 0 0 10 0
        For element: Footnotes
            Typography:
                Size: 8
                Line height: 11
            Box:
                Border color: #999999
    Section:
        Paragraph:
            Content >>
                Las ventas crecieron un 12 % en el trimestre[^fuente], por encima de lo previsto,
                y el margen se mantuvo en el 31 %.
        Paragraph: El plan para el año que viene se presenta en el capítulo siguiente[^plan].
        Footnote:
            Id: fuente
            Paragraph: Datos de la memoria anual, cerrados a 31 de diciembre.
        Footnote:
            Id: plan
            Paragraph:
                Content >>
                    El plan incluye la apertura de dos tiendas y se aprobó en la junta de octubre,
                    con los votos a favor de todos los socios presentes.
```

## Propiedades

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `Id` | obligatorio | El nombre de la nota, el que se escribe en la llamada `[^id]` |

Admite además `Style`, `Box` y `Typography`. La llamada necesita `Markup: markdown` en el texto (o en una regla, como
arriba), o estar en un `Markdown`. En la `Section`, `Footnote numbering: restart` vuelve a empezar la numeración en 1, y
`Footnote number format` la escribe en letras o en romanos (`lower-alpha`, `upper-roman`...).

## Cómo se comporta

- La nota empieza con un `Paragraph`, un `Text` o un `Markdown`, que recibe el número en superíndice; después puede
  llevar más párrafos, listas o tablas. Sin encabezados ni imágenes.
- Cada nota se llama una vez, y toda llamada tiene su nota en la misma sección; si no, es un error.
- Las notas de una página van juntas al pie, bajo un filete, con el aspecto de `For element: Footnotes` (el filete es su
  borde superior; su tipografía es la de todas las notas) y el de `For element: Footnote` (cada nota).
- Una línea solo entra en la página si su nota cabe debajo; si no, pasa a la página siguiente con la nota. Una nota
  larga puede seguir al pie de la página siguiente.
- En un `Columns`, las notas van al pie de la página, no de la columna.
- La llamada no puede ir en un `Page overlay`, en un encabezado ni dentro de otra nota.

Más detalle en la [referencia](../../reference/components/footnote.md).
