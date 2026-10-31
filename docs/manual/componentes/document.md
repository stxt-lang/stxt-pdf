# `Document` y `Section`

`Document` es la raíz de todo documento. Tiene:

- **`Language`** y **`Title`**: obligatorios.
- **`Page`**: el papel y los márgenes.
- **`Styles`**: opcional, con las reglas de estilo ([Estilos](../comun/estilos.md)).
- **`Page overlay`**: opcionales, las cabeceras y los pies de todo el documento.
- **`Section`**: una o más, con el contenido.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe anual
    Page:
        Paper: A4
        Margin: 56
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 22
                Bold: true
    Page overlay:
        Offset: auto 0 -30 0
        Text: Página {page} de {pages}
            Typography:
                Size: 8
                Align: right
    Section:
        Page numbering: none
        Inherit page overlays: false
        Page:
            Margin: 0
        Block:
            Box:
                Height: 100%
                Padding: 300 56 0 56
                Background: #12315A
            Heading 1:
                In table of contents: false
                Text: Informe anual
                Typography:
                    Color: #FFFFFF
    Section:
        Heading 1:
            Text: Resultados
        Paragraph: El cuerpo del informe empieza en la página 1.
    Section:
        Page numbering: restart
        Page:
            Orientation: landscape
        Heading 1:
            Text: Anexo
        Paragraph: El anexo, apaisado, vuelve a la página 1.
```

Este documento tiene tres secciones:

- Una portada a sangre, sin número de página y sin el pie.
- El cuerpo, que empieza en la página 1.
- Un anexo apaisado que vuelve a contar desde 1.

## `Title` y `Language`

`Title` es el título que muestra el visor de PDF en lugar del nombre del fichero. `Language` es el idioma del documento,
una etiqueta BCP 47 (`es`, `en-US`, `ca`). El lector de pantalla lo necesita para leer el texto, y las mayúsculas automáticas siguen sus
reglas. Los dos son obligatorios, porque sin ellos el PDF no es accesible.

## `Page`

El tamaño de la página y sus márgenes. El tamaño se da de una de dos formas, nunca las dos:

- **Con un papel**: `Paper` (`A3`, `A4`, `A5`, `A6`, `B4`, `B5`, `Letter` o `Legal`) y, si es apaisado,
  `Orientation: landscape`.
- **Con medidas**: `Width` y `Height`, en puntos.

| `Paper` | Puntos | | `Paper` | Puntos |
|---|---|---|---|---|
| `A3` | 842 × 1191 | | `B4` | 709 × 1001 |
| `A4` | 595 × 842 | | `B5` | 499 × 709 |
| `A5` | 420 × 595 | | `Letter` | 612 × 792 |
| `A6` | 298 × 420 | | `Legal` | 612 × 1008 |

`Margin` son los márgenes de la página, de 1 a 4 números (arriba, derecha, abajo, izquierda). Lo que queda dentro es el
**área útil**: donde va el contenido. Las cabeceras y los pies van en los márgenes, con un `Page overlay`
([posicionados](frame-and-overlays.md#page-overlay)).

## `Section`

Una sección es una parte del documento que **empieza siempre en una página nueva**. Todo el contenido va dentro de alguna.

| Propiedad | Qué hace |
|---|---|
| `Page` | Otro papel, otra orientación u otros márgenes para esta sección. Hereda del `Page` del documento lo que no declara: con solo `Orientation: landscape`, es el mismo papel apaisado, y con solo `Margin`, el mismo papel con otros márgenes |
| `Page numbering` | `continue` (por defecto) sigue la cuenta de páginas, `restart` vuelve a empezar en 1 y con `none` sus páginas no tienen número ni cuentan |
| `Page number format` | Cómo se escriben sus números de página: `decimal` (por defecto), `lower-roman` (`i`, `ii`, `iii`) o `upper-roman` (`I`, `II`, `III`) |
| `Start` | Con `Page numbering: restart`, el número de su primera página (`Start: 5`) |
| `Inherit page overlays` | `true` (por defecto) lleva las cabeceras y los pies del documento, y `false`, solo los suyos |
| `Begin on` | `any` (por defecto) empieza en la página siguiente, y `odd`, en una impar, la de la derecha al imprimir a doble cara |
| `Id`, `Style` | Para enlazarla (`#id`, su primera página) y darle tipografía con una regla (`Typography` y nada más: un anexo en otro idioma) |

Una sección puede tener sus propios `Page overlay`, que solo salen en sus páginas. Una sección vacía es un error: no tendría
ninguna página.

## Numeración de las páginas

`{page}` escribe el número de la página y `{pages}` el último número de la cuenta en curso, que empieza de nuevo en cada
sección con `Page numbering: restart`. Una página de una sección `none` no tiene número: si un texto con `{page}` cae en ella
(un pie del documento en la portada), es un error, así que esa sección declara `Inherit page overlays: false`.

Los índices y las referencias (`{page of: id}`) dan el mismo número que el pie de la página: el impreso, también en
romanos si su sección tiene `Page number format: lower-roman`. Para un prefacio en romanos, su sección declara `Page number format` y la
del cuerpo, `Page numbering: restart`. Con `Start`, la numeración empieza en otro número.

## Doble cara

En un libro, cada capítulo empieza en una página de la derecha, que es impar. Con `Begin on: odd`, si la sección anterior
acaba en una página impar, el motor añade detrás una **página en blanco**: sin contenido, sin cabecera ni pie. Cuenta
para el número de página, como en un libro, aunque no se imprima: así las páginas impares siguen a la derecha. La primera
sección ya empieza en la página 1, así que en ella es un error.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Dos capítulos
    Page:
        Paper: A5
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Serif
                Size: 11
    Page overlay:
        Offset: auto 0 -30 0
        Text: {page}
        Typography:
            Align: center
    Section:
        Heading 1:
            Text: Capítulo 1
        Paragraph: El primer capítulo ocupa una sola página.
    Section:
        Begin on: odd
        Heading 1:
            Text: Capítulo 2
        Paragraph: El segundo empieza en la página 3: la 2 queda en blanco.
```

## Recetas

- **Portada a sangre:** una sección con `Page` de `Margin: 0`, `Page numbering: none` e `Inherit page overlays: false`, y un bloque
  de `Height: 100%` con su fondo.
- **Una página apaisada en medio:** una sección con `Orientation: landscape`.
- **Un título que no va al índice:** `In table of contents: false` en el encabezado ([Encabezados](heading.md)).

## Lo que no hay

- Un fondo de página propio: se pinta con un bloque del tamaño de la página o con un `Page overlay` detrás de todo
  ([Capas y giro](../comun/capas-y-giro.md)).

Más detalle en la [referencia](../../referencia/componentes/document.md).
