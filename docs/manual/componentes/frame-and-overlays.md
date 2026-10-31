# `Frame`, `Overlay` y `Page overlay`

Los tres elementos **posicionados** son bloques (apilan a sus hijos como un `Block`) que se colocan con `Offset`, un
desplazamiento de 1 a 4 valores: arriba, derecha, abajo, izquierda.

| Elemento | Qué hace |
|---|---|
| `Frame` | Va en su sitio, como cualquier caja, y después se **desplaza** sin mover a las demás |
| `Overlay` | **Sale del flujo**: no ocupa espacio y se coloca en un sitio exacto respecto al `Frame`, `Overlay` o `Page overlay` que lo contiene (o, si no hay, el área útil) |
| `Page overlay` | Como `Overlay`, pero respecto al área útil de **cada página**, y se repite en todas: cabeceras, pies, marcas de agua |

```
Document (dev.stxt.pdf):
    Language: es
    Title: Presupuesto
    Page:
        Paper: A4
        Margin: 60 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Page overlay
            Typography:
                Size: 8
                Color: #5F5E5A
    Page overlay:
        Offset: -36 0 auto 0
        Text: Taller Nogal · Presupuesto 2026-014
    Page overlay:
        Pages: not-first
        Offset: auto 0 -36 0
        Text: Página {page} de {pages}
            Typography:
                Align: right
    Section:
        Frame:
            Box:
                Height: 120
                Background: #F1EFE8
            Paragraph: Presupuesto para la cocina de la calle Mayor.
            Overlay:
                Offset: 10 10 auto auto
                Rotate: 8
                Box:
                    Padding: 4 8
                    Border: 1
                    Border color: #A32D2D
                Text: BORRADOR
                    Typography:
                        Color: #A32D2D
                        Bold: true
```

La cabecera sale en todas las páginas y el pie, en todas menos la primera; los dos van en el margen de la página (por eso el
desplazamiento negativo). El sello se coloca en la esquina de arriba a la derecha del `Frame`, girado.

## `Offset`

Cada lado del `Offset` es la distancia desde ese borde de su contenedor; `auto` es «no fijado». Para un `Overlay` o un
`Page overlay`:

| Escribes | Resultado |
|---|---|
| `Offset: 10 10 auto auto` | A 10 del borde de arriba y 10 del de la derecha, con su tamaño |
| `Offset: auto 0 0 0` | Abajo, de lado a lado |
| `Offset: 0` | Estirado a todo el contenedor |
| `Offset: auto` (o nada) | Donde habría quedado en el flujo |

Si un lado y su contrario están fijados, la caja se estira entre los dos (salvo que también tenga su tamaño: entonces es un
error, porque está fijada dos veces; con `Margin: auto` en ese eje, se centra). Los porcentajes son del ancho o del alto del
contenedor.

Un `Frame` no puede fijar a la vez arriba y abajo, ni izquierda y derecha: se desplaza desde uno de los dos. Un `Frame`
sin `Offset` no se mueve: sirve para que los `Overlay` de dentro se coloquen respecto a él.

## Propiedades

| Propiedad | Elementos | Qué hace |
|---|---|---|
| `Offset` | Los tres | El desplazamiento |
| `Layer` | Los tres | Qué se pinta encima ([Capas y giro](../comun/capas-y-giro.md)) |
| `Rotate`, `Rotate origin` | Los tres | Girar la caja ([Capas y giro](../comun/capas-y-giro.md#girar-una-caja-rotate)) |
| `Pages` | `Page overlay` | En qué páginas sale |

Admiten además `Id`, `Style`, `Link`, `Box` y `Typography`. `Frame` admite también `Break` y `Contain margins`; `Overlay` y
`Page overlay` no admiten `Break`, porque no se parten.

## `Page overlay`

- No ocupa espacio: deja sitio en el `Margin` de la página para las cabeceras y los pies, y colócalos en el margen con un
  `Offset` negativo.
- Un `Page overlay` del `Document` sale en todas las secciones (salvo las que dicen `Inherit page overlays: false`); uno de una `Section`,
  solo en las suyas.
- Lo que hay dentro de un `Page overlay` es decoración para el PDF etiquetado: no se lee como contenido, no puede tener
  encabezados y no se puede enlazar ni referir.

### `Pages`

| Valor | Páginas |
|---|---|
| `all` | todas (por defecto) |
| `first` | la primera |
| `not-first` | todas menos la primera |
| `odd`, `even` | las impares, las pares |
| `last` | la última |

Las páginas se cuentan desde la primera de su documento o de su sección, en orden, sin mirar el número que llevan impreso.

## Lo que hay que saber

- Un `Overlay` o un `Page overlay` puede ocupar los márgenes de la página, pero no salirse del papel: es un error.
- Un `Overlay` no se parte entre páginas: va entero en la página donde cae.
- Para colocar un `Flex` o un `Grid` en un sitio exacto, ponlo dentro de un `Overlay`.

Más detalle en la [referencia](../../referencia/componentes/frame-and-overlays.md).
