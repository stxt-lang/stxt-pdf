# `List`

Una lista con viñetas o numerada. Cada `List item` es un ítem, con un marcador (la viñeta o el número) que pone el motor.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Pasos
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: List
            Box:
                Padding: 0 0 0 18
        For element: List item
            Box:
                Margin: 0 0 4 0
    Section:
        List:
            Marker: decimal
            List item:
                Paragraph: Corta la corriente.
            List item:
                Paragraph: Retira la tapa del termostato.
                List:
                    Marker: dash
                    List item:
                        Paragraph: Guarda los tornillos.
                    List item:
                        Paragraph: No toques los cables.
            List item:
                Paragraph: Conecta el cable rojo al borne L.
```

## Propiedades

| Propiedad | Dónde | Qué hace |
|---|---|---|
| `Marker` | `List` | **Obligatorio**, en la lista o en una regla de `Styles` ([Estilos](../common/styles.md#propiedades-de-un-elemento)). `bullet` (•), `dash` (–), `none`, `image` (una imagen), `decimal` (1.), `lower-alpha` (a.), `upper-alpha` (A.), `lower-roman` (i.), `upper-roman` (I.) |
| `Marker suffix` | `List` | Lo que sigue al número: `period` (`1.`, `a.`), por defecto, o `parenthesis` (`1)`, `a)`). Solo en las numeradas |
| `Marker image` | `List`, `List item` | La imagen del marcador con `Marker: image`, de `images/`. La de la lista vale para todos sus ítems; la de un ítem la sustituye. Admite `Alt` y `Height` |
| `First number` | `List` | El número del primer ítem (1 por defecto). Solo en las numeradas |
| `Number` | `List item` | El número de este ítem. Los siguientes siguen desde él |

`List` y `List item` admiten además `Id`, `Style`, `Link`, `Box`, `Typography` y `Break`. Un ítem tiene uno o varios
elementos (un `Paragraph`, un `Text`, otra `List`), pero no encabezados.

## Enumeraciones con paréntesis

Las cláusulas de un contrato o de una póliza se numeran a menudo con un paréntesis: `a)`, `b)`, `c)`. Es
`Marker suffix: parenthesis`, en la lista o, para todas las de un estilo, en una regla con su `Marker`.

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
        For element: List
            Box:
                Padding: 0 0 0 18
        For style: clauses
            Marker: lower-alpha
            Marker suffix: parenthesis
    Section:
        Paragraph: El tomador se obliga a:
        List:
            Style: clauses
            List item:
                Paragraph: Pagar la prima en el plazo acordado.
            List item:
                Paragraph: Comunicar el siniestro en siete días.
            List item:
                Paragraph: Conservar los restos hasta la peritación.
```

## Marcadores con imagen

Con `Marker: image` el marcador es una imagen: un visto, una cruz, un icono. `Marker image` dice cuál, en la lista para todos
sus ítems o en un ítem para él solo. Mide de alto lo que el `Size` del texto, salvo que declare `Height`, y va centrada con
las mayúsculas de la primera línea. Con `Alt`, un lector de pantalla la lee; sin él, es decorativa.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Coberturas
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: List
            Box:
                Padding: 0 0 0 18
    Section:
        List:
            Marker: image
            Marker image: test/check.svg
                Alt: Incluido
            List item:
                Paragraph: Responsabilidad civil obligatoria.
            List item:
                Paragraph: Asistencia en viaje.
            List item:
                Marker image: test/cross.svg
                    Alt: Excluido
                Paragraph: Daños propios del vehículo.
```

(El ejemplo usa los iconos de prueba del repositorio, en `resources/images/test/`.)

## La sangría

El marcador cuelga **a la izquierda del ítem**, en el hueco que deja el `Padding` izquierdo de la `List`. Sin ese relleno no
hay sitio para el marcador y es un error, así que lo normal es darlo en una regla `For element: List`, como en el ejemplo. Los
marcadores quedan alineados a la derecha, pegados al texto con un espacio.

## Cómo se comporta

- El marcador usa la tipografía del ítem y se alinea con su primera línea. Por eso un ítem tiene que empezar con un texto:
  si empieza con un `Stack`, un `Grid` o una imagen, es un error.
- Cada lista numera sus ítems. Una lista dentro de otra empieza de nuevo. Una lista con imágenes no numera.
- `Marker suffix` es de su lista: una lista dentro de otra lleva el punto si no declara el suyo. En una lista que no
  numera es un error.
- `Marker image` solo tiene sentido con `Marker: image`; en otra lista es un error, y un ítem de una lista con
  `Marker: image` sin imagen, ni suya ni de la lista, también.
- Si el ítem se parte entre páginas, el marcador va en la primera.
- `{number of: id}` escribe el número de un ítem con `Id` (`3`, `c`, `IV`): «véase la cláusula {number of: pago}».

Más detalle en la [referencia](../../reference/components/list.md).
