# Reglas reutilizables: `Styles`

`Styles`, hijo opcional de `Document` (como mucho uno), declara una vez propiedades que se aplican por defecto a muchos
elementos. Las mismas reglas pueden estar también en un [documento de estilos aparte](#documento-de-estilos-aparte). No
hay cascada: las reglas dan valores por defecto a los elementos de un tipo, de un estilo o con un `Id`, y el elemento los
puede sobrescribir.

## Reglas

| Regla | Valor | Se aplica a |
|---|---|---|
| `For element` | `Document` | la raíz: su `Typography` es la base que heredan todas las cajas. Solo admite `Typography` |
| `For element` | nombre de un elemento de caja (`Block`, `Text` o `Heading 1`) | todos los elementos de ese tipo |
| `For element` | un elemento generado por el motor: `Code block` o `TOC 1` a `TOC 6` | los bloques de código de un `Markdown` ([Markdown](../componentes/markdown.md#código)) o las entradas de un `Table of contents` ([Table of contents](../componentes/table-of-contents.md#estilo)). No se escriben en el documento |
| `For element` | una posición en una tabla: `Header cell`, `Body cell`, `Footer cell`, `Odd row cell` o `Even row cell` | las celdas de una `Table` en esa posición ([Table](../componentes/table.md#estilo-de-las-celdas)) |
| `For style` | un nombre, sin espacios | los elementos con ese nombre en su `Style` y las celdas de una `Row` con él en su `Style` |
| `For id` | un `Id`, sin espacios | el elemento con ese `Id` |
| `For column` | una columna (`3`) o un rango de columnas (`2 4`, ambas incluidas) | las celdas de una `Table` que **empiezan** en esas columnas |

Cada regla admite los grupos `Box`, `Typography` y `Break`, como mucho uno de cada. No admite las propiedades propias de un
elemento (`Gap`, `Column sizes`, `Offset`, `Marker`, `Link` o `Rotate`).

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

> **Decisión:** las reglas solo admiten los grupos: las propiedades propias de un elemento no se podrían validar al
> escribir el documento, porque el tipo de elemento va en el valor de `For element`.

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

Las reglas de los puntos 4 a 7 solo se aplican a las celdas de una `Table` ([Table](../componentes/table.md#estilo-de-las-celdas)).

Una regla gana a la herencia: con `For element: Text` y `Size: 10`, un `Text` mide 10 aunque su padre declare
`Size: 20`. Las reglas se mezclan **propiedad a propiedad** y no por grupo: si la regla declara `Size` y el elemento
`Bold`, se aplican las dos. Lo que una regla da a un elemento en `Typography` lo heredan sus descendientes, como si lo
hubiera declarado él. `Item` y `Area` no son cajas: las reglas se aplican al elemento que envuelven. El `Style` y el
`Id` de una `Row` aplican sus reglas a cada una de sus celdas y no a la franja de la fila. `Header rows`, `Body`,
`Footer rows` y `Row` no se pueden nombrar en `For element` (para dar estilo a las celdas de cada grupo están las reglas
de posición). `Table` y `Cell`, sí. A una `Section` solo le llega `Typography`
([Document](../componentes/document.md#section)).

> **Decisión:** prioridad fija en lugar de especificidad calculada. *Alternativa descartada:* especificidad calculada y
> selectores compuestos, más potentes pero menos predecibles.

## Validación

Lo que aporta una regla se valida igual que si lo declarara el elemento ([Errores](errores.md)), y el error señala la
línea de la propiedad dentro de la regla. Los valores de una regla se comprueban aunque no se aplique a ningún elemento.
Son errores:

- un `For element` que no nombra un elemento de caja, un elemento generado ni una posición en una tabla
  (`STYLE_UNKNOWN_ELEMENT`);
- un `For column` que no es un número (`VALUE_SYNTAX`) o un rango que no va de menor a mayor (`VALUE_NOT_ALLOWED`);
- dos reglas con el mismo selector en el mismo origen, también dos `For column` con el mismo rango (`STYLE_DUPLICATE`);
- una regla que da `Break` a un elemento que no lo admite: `Overlay`, `Page overlay`, un encabezado, un `Text` de un encabezado o
  una celda, también con una regla de posición, de columna o del estilo de su `Row` (`STYLE_NOT_ALLOWED`);
- una regla que da a una `Section` algo que no es `Typography` (`STYLE_NOT_ALLOWED`);
- un nombre de `Style` sin su regla `For style` (`STYLE_WITHOUT_RULE`).

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

Las reglas pueden escribirse en un documento aparte, de raíz `Styles` con el namespace `dev.stxt.pdf`, para quitarlas
del documento principal o compartirlas entre varios:

```
Styles (dev.stxt.pdf):
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
    For style: panel
        Box:
            Padding: 10
```

- **Se pasa al maquetar, nunca se busca.** El documento no dice dónde está su documento de estilos y el motor no busca
  ficheros ni carpetas: quien convierte lo lee y lo pasa junto al documento
  ([Uso desde Java](../implementacion/api.md#estilos-aparte)). Es opcional.
- **Vale menos que el documento.** Se admiten las mismas reglas que en el `Styles` del documento. En cada nivel de la
  prioridad (el de `For id`, el de `For style`, el de `For element`, etc.), las reglas del documento de estilos aparte van **antes** y
  las del documento **después**, así que gana el documento, propiedad a propiedad. La prioridad entre niveles no cambia:
  un `For style` del documento aparte sigue ganando a un `For element` del documento. Con esto, sacar el `Styles` de un
  documento a un fichero aparte no cambia el resultado.
- **Cada origen es estricto por separado.** Dos reglas con el mismo selector son un error dentro de un origen
  (`STYLE_DUPLICATE`), pero la misma regla puede estar en los dos. Un nombre de `Style` puede tener su `For style` en
  cualquiera de los dos.
- **Errores.** Una regla del documento de estilos se valida como una del documento, y su error lleva la línea **de ese
  fichero** y lo dice: `línea 8 de los estilos: ...`. La raíz que no es `Styles` es `DOCUMENT_ROOT`. Una propiedad que
  llega de una regla del documento de estilos conserva su línea en él, también la tipografía que se hereda, así que lo
  mismo vale para los errores que se ven al aplicarla o al maquetar (una fuente que no existe, un desbordamiento). Si un
  mensaje nombra otra línea, también dice de qué documento es: `línea 8 de los estilos: Min width es mayor que Max width
  (línea 10)`.

> **Decisión:** la prioridad entre los dos orígenes es por nivel, así que sacar el `Styles` a un fichero no cambia nada.
> *Alternativa descartada:* que todas las reglas del fichero valgan menos que cualquiera del documento.

> **Decisión:** el documento no nombra su documento de estilos: lo pasa quien convierte. *Alternativa descartada:* una
> propiedad `Styles file` en `Document`.

## Lo que no hay

- Selectores compuestos (un elemento dentro de otro, varios estilos a la vez), combinadores ni prioridades declaradas por el
  autor.
- Estilo de una fila de tabla con `Box` o de una columna entera: se da a las celdas ([Table](../componentes/table.md#lo-que-no-hay)).
