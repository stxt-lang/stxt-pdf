# `Table of contents`

Un `Table of contents` es un índice automático: un bloque en flujo, sin hijos, que genera una **entrada por encabezado** de los
niveles de `Levels` (por defecto 1 a 3), en orden del documento, también los de un `Markdown` y los que están después del
propio índice, salvo los que declaran `In table of contents: false` ([Encabezados](heading.md#fuera-del-índice)). Con
`Entries: figures` o `Entries: tables` es un **índice de figuras o de tablas**: una entrada por `Figure` de ese contador
([abajo](#qué-lista-of)). Admite `Id`, `Style`, `Box`, `Typography` y `Break`, como un `Block`, y va donde va un
`Markdown`. No admite `Link`: sus entradas ya llevan el suyo.

```
Table of contents:
    Levels: 1 2
    Leader: dots
```

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Entries` | `headings`, `figures`, `tables` | `headings` | Qué lista: los encabezados de `Levels`, las figuras o las tablas ([abajo](#qué-lista-of)). |
| `Levels` | dos enteros de 1 a 6: el primer y el último nivel | `1 3` | Los encabezados que lista: `Levels: 1 2` lista los `Heading 1` y `Heading 2`. Sin ningún encabezado en esos niveles es un error. Solo con `Entries: headings`. |
| `Leader` | `dots`, `line`, `none` | `dots` | Lo que llena el hueco entre el título y la página de cada entrada: puntos, una línea o nada. |

`Levels` y `Leader` también pueden venir de una regla de `Styles` ([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)); `Entries`, no: dice qué es el índice.

## Qué lista (`Entries`)

Con `Entries: figures`, el índice lista las `Figure` que se numeran con el contador de las figuras, y con `Entries: tables`, las
que se numeran con el de las tablas ([Figure](figure.md#numeración)): por defecto, una `Figure` con una `Table` cuenta
entre las tablas y las demás entre las figuras, y `Figure counter` lo cambia. Salen en orden del documento, también las
que están después del índice, numeradas o no, salvo las que declaran `In table of contents: false`. Cada entrada es como
la de un encabezado: el **texto del pie**, con su número o su `Number label` delante, la guía y la página de la figura,
clicable entera. Todas las entradas son `TOC 1`, y `For element: TOC 1` las estila.

```
Heading 1:
    In table of contents: false
    Text: Índice de figuras
Table of contents:
    Entries: figures
Heading 1:
    In table of contents: false
    Text: Índice de tablas
Table of contents:
    Entries: tables
```

`Levels` no tiene efecto con `Entries: figures` o `tables`, y es un error (`LEVELS_WITHOUT_HEADINGS`). Un índice de figuras o de
tablas sin ninguna que listar es `TABLE_OF_CONTENTS_EMPTY`.

> **Decisión:** un índice lista una sola cosa (`Entries`), y un índice de figuras y otro de tablas son dos `Table of contents`.
> Las entradas de los dos son `TOC 1`, con la misma regla, porque no tienen niveles. *Alternativa descartada:* un índice
> mixto de figuras y tablas, o elementos generados propios (`TOC figure`), que piden más reglas.

## Entradas

Cada entrada es una fila con tres partes:

- el **título**, que es el texto del encabezado, con su número si lo lleva
  ([Encabezados](heading.md#numeración-numbered)), sus `Text` unidos por un espacio y sin formato en línea;
- la **guía**, que crece hasta llenar el hueco, con 4 pt a cada lado;
- el **número de página**, que funciona como un `{page of: ...}` del encabezado ([Referencias](../common/references.md)),
  también si el encabezado no tiene `Id`.

Sin guía (`Leader: none`), el
título llena el hueco. Si el título no cabe en una línea, se parte, y el número queda en la primera línea.

Cada entrada es clicable entera y lleva a su encabezado, como un `Link: #id` ([Enlaces](../common/links.md)).

> **Decisión:** con un título largo, el número queda en la primera línea. *Alternativa descartada:* alinearlo con la
> última, que pediría alinear por la línea base.

## Guía

Con `dots`, puntos cuadrados de 0,09 veces el cuerpo, cada medio cuerpo y pegados al número. Con `line`, es una línea de 0,05
veces el cuerpo, de un lado al otro del hueco. Se pintan sobre la línea base de la entrada y con el color del texto. Son
dibujo y no texto: no se seleccionan ni se leen.

> **Decisión:** la guía es un dibujo que llena exactamente el hueco. *Alternativa descartada:* el carácter «.» repetido,
> que se copiaría con el texto.

## Estilo

La entrada hereda la tipografía del `Table of contents`, y sus cajas, la de la entrada. Las reglas `For element: TOC 1` a
`TOC 6` dan `Box`, `Typography` y `Break` a las entradas de un nivel: la sangría de cada nivel es su `Padding`
izquierdo, y el espacio entre entradas, su `Margin` o su `Padding` vertical. Las propias entradas no llevan `Id` ni
`Style`.

> **Decisión:** una regla por nivel y no por parte (`TOC title`), que sería más fina y más larga de escribir.

## Errores

Son errores:

- un `Table of contents` sin ningún encabezado en sus niveles, o sin ninguna figura o tabla con `Entries: figures` o
  `tables` (`TABLE_OF_CONTENTS_EMPTY`, con la línea del `Table of contents`);
- un `Levels` que no son dos niveles de 1 a 6 en orden (`VALUE_NOT_ALLOWED`), o un `Levels` con `Entries: figures` o
  `tables` (`LEVELS_WITHOUT_HEADINGS`);
- un `Table of contents` sin `Font` o sin `Size` propios o heredados (`TEXT_WITHOUT_FONT` y `TEXT_WITHOUT_FONT_SIZE`, con
  la línea del `Table of contents`).

Un encabezado de una sección sin números de
página no puede estar en un índice: se declara con `In table of contents: false`.

> **Decisión:** `Levels` son siempre dos niveles, para no dudar entre «hasta el 2» y «solo el 2».

## Accesibilidad

Un lector de pantalla lo anuncia como un índice, con cada entrada como un enlace a su encabezado o a su figura. Los
marcadores del PDF no cambian ([El PDF](../common/pdf.md#marcadores)).

## Lo que no hay

- Índices de otras cosas que los encabezados, las figuras y las tablas (listados, ecuaciones), índices alfabéticos y
  entradas escritas a mano.
- Un índice mixto de figuras y tablas.
