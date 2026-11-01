# Páginas y cortes: `Break`

## El grupo `Break`

Los cortes de página. No se heredan. No se admite en `Overlay` ni en `Page overlay`, que no se parten ni están en el flujo de
la página, ni en los encabezados ni en las celdas ([Encabezados](../components/heading.md#cortes),
[Table](../components/table.md#filas)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Before` | `auto`, `page`, `column` | `auto` | `page` fuerza un salto de página antes de la caja, y `column`, un salto a la columna siguiente (solo dentro de un `Columns`, [Columns](../components/columns.md#cortes)). |
| `After` | `auto`, `page`, `column` | `auto` | Como `Before`, después de la caja. |
| `Keep with next` | booleano | `false` | Evita el corte entre la caja y la siguiente (títulos). Con `After: page` o `column` es `KEEP_CONFLICT`. |
| `Keep with previous` | booleano | `false` | Evita el corte entre la caja y la anterior. Con `Before: page` o `column` es `KEEP_CONFLICT`. |
| `Keep together` | booleano | `false` | Intenta no partir la caja. |
| `Edges` | `open`, `closed` | `open` | Qué pasa con el borde y el relleno en un corte. |

> **Decisión:** los saltos se dicen con `Before` y `After`, y lo que se mantiene junto, con `Keep with next`,
> `Keep with previous` y `Keep together`, los nombres de los procesadores de textos. *Alternativa descartada:* un valor
> `avoid` en `Before`, `After` e `Inside`, que no dice qué se mantiene con qué.

## Modelo

Un **contenedor de fragmentos** es donde caen los trozos de las cajas: uno por página, con la altura del área útil, y uno
por columna dentro de un `Columns` ([Columns](../components/columns.md)). Cada caja produce un **fragmento** por
contenedor de fragmentos en el que aparece.

## Oportunidades de corte

- entre cajas hermanas en flujo de bloque;
- entre líneas de texto;
- entre líneas de un `Stack` y entre filas de un `Grid` o una `Table`.

Siempre se intenta cortar en el punto más alto del árbol posible. Solo si no cabe se corta dentro de un hijo.

## Elección del corte

El motor elige el **último** corte de la página que no incumple ninguna regla. Si no hay ninguno, relaja las reglas en
este orden:

1. `Widows` y `Orphans` (de `Typography`);
2. `Keep with next` y `Keep with previous`;
3. `Keep together`.

Si sigue sin haber corte posible, corta donde se acaba el espacio.

Un párrafo que no puede respetar `Widows` y `Orphans` se parte igualmente si la alternativa es incumplir un `Keep with next` o un `Keep with previous`. En
ese corte se respetan las viudas si se puede y, si no, se llena el espacio.

`Keep with next` y `Keep with previous` se propagan como los cortes forzados: el `Keep with next` del último hijo en
flujo de un bloque (o de un `Columns`) vale también para el bloque, y el `Keep with previous` del primero, igual. Así,
un encabezado que acaba un `Block` no se queda solo al final de la página.

Si el mejor corte es anterior al hijo que no cabe, lo que va después pasa a la página siguiente, pero un hijo anterior
nunca se parte por dentro para dejar sitio.

## Garantía de progreso

Al principio de una página siempre se coloca algo, aunque haya que cortarlo a la fuerza. Lo que no se puede partir y no
cabe en una página entera (una línea más alta que el área útil, una imagen, una fila con un ítem que no puede colocar
nada) es un error (`PAGE_OVERFLOW`, [Desbordamiento](overflow.md)).

## Cortes forzados

`Before: page` y `After: page` fuerzan un salto de página, y `Before: column` y `After: column`, dentro de un `Columns`,
un salto a la columna siguiente (que, en la última columna, es también de página). Solo se aplican a cajas en flujo:
`Overlay` y `Page overlay` no admiten `Break`.

- El `Before` del primer hijo de un bloque y el `After` del último se propagan al bloque. De un `Columns` solo sale el de
  página.
- Si una caja tiene los dos, gana el de página.
- Un corte forzado al principio de una página (o de una columna) se ignora: no deja páginas en blanco. Lo mismo el
  `After: page` de la última caja de una sección.
- Un corte de página dentro de un `Columns` deja sin llenar las columnas que quedan en esa página.
- Dentro de una caja que no se parte (un `Overlay`, un `Page overlay`, un `Frame` girado, una caja girada en flujo o una
  caja con `Overflow: clip`), ningún `Break` tiene efecto. Si lo escribe el propio elemento (o la `Row` de una celda) es un error
  (`BREAK_IN_UNBREAKABLE`). Si llega de una regla de `Styles`, no tiene efecto y no es un error. Las columnas de un `Columns` de dentro sí se cortan, y sus `Break` cuentan.

> **Decisión:** un corte de página atraviesa las columnas de un `Columns`. *Límite:* dentro de un ítem de un `Stack` o
> un `Grid` partido en paralelo, un corte de página actúa como uno de columna.

## Márgenes en los cortes

En un corte no forzado, los márgenes que tocan el corte se truncan a cero: tras el corte, el margen superior del primer
hijo de la página y los que salen de él por arriba. Un fragmento de continuación no deja salir márgenes por arriba. En un
corte forzado, se trunca el margen anterior al corte y se conserva el posterior.

## Bordes y relleno en los cortes

Con `Edges: open`, el primer fragmento no tiene borde ni relleno inferiores y los siguientes no tienen los
superiores. Con `Edges: closed`, cada fragmento se pinta como una caja completa: el contenido de cada página cabe entre
su borde y relleno superiores e inferiores.

Con `open`, el borde y el relleno inferiores solo se reservan si la caja terminaría en la página: si entonces no caben,
la caja se corta antes (o pasa entera a la página siguiente) y termina allí con ellos. Si la caja se corta de todos modos,
su fragmento llega hasta el final de la página, sin dejar hueco.

## Alto definido y cortes

Un `Height` fijo es el total de todos los fragmentos y no el de cada uno. Con `closed`, el total del contenido es ese alto
menos un borde y un relleno, y cada fragmento les suma los suyos. `Min height` y `Max height` también se aplican al total
(con `closed`, al contenido total más un borde y un relleno).

Una caja con `Height` definido se parte como cualquier otra, aunque no tenga contenido: lo que no cabe de su alto pasa a la
página siguiente. Para evitarlo se declara `Keep together: true`, que no impide partirla si no cabe en una página entera.

Un fragmento cortado de una caja con alto `auto` mide hasta el corte. Con alto definido, llega hasta el final de la página
sin pasar del alto que le queda, pero solo **consume** de ese alto lo que ocupa su contenido (con su borde y relleno
superiores): el hueco que dejan las líneas o los hijos al final de la página no se le quita al contenido que sigue. Así,
una caja con `Height` igual al alto de su contenido no desborda al partirse, y el alto que se ve puede pasar del `Height`
en lo que suman esos huecos.

> **Decisión:** un fragmento cortado consume solo lo que ocupa su contenido, para que una caja (o un ítem de grid
> estirado) no desborde por partirse, a cambio de que el alto que se ve pueda pasar del `Height`.
> *Alternativa descartada:* un fragmento que mide solo lo que ocupa su contenido (no llega al final de la página), o dejar que el contenido desborde el último fragmento.

## Cortes en `Stack`, `Grid` y `Table`

En un `Stack` vertical, el corte se hace entre ítems. En uno horizontal y en un `Grid`, entre líneas o filas. Una línea, fila o
ítem que no cabe en lo que queda de página pasa entero a la siguiente. Si tampoco cabe al principio de una página, sus
ítems se parten **en paralelo**: cada uno se parte por su cuenta y la fila continúa en la página siguiente con los ítems
que no han terminado, todos desde arriba, y con la altura del más largo de ellos. Las filas siguientes van detrás, con el
hueco que las separaba. Las filas que ocupa un mismo ítem se parten juntas, cada ítem desde su posición. Un ítem estirado
reparte entre las páginas el alto que le impone su línea o su área, así que puede terminar antes o después que el texto de
sus hermanos. Si algún ítem no puede colocar nada, la fila no se parte, y si no cabe es un error.

Al elegir el corte, una fila partida cuenta como el peor de los cortes de sus ítems. Un `Stack` vertical con `Wrap` no
se parte. Las tablas repiten además su cabecera y su pie ([Table](../components/table.md#cortes)).

## Lo que no se parte

- **Imágenes.** Una `Image` o un `Barcode` no se parte: si no cabe en lo que queda de la página (o de la columna), pasa
  entero a la siguiente ([Image](../components/image.md#cortes-de-página)).
- **Posicionadas.** Un `Overlay` se coloca en la página donde cae su posición y no se parte. Si su contenedor de
  referencia está partido, se resuelve contra el fragmento de esa página: su alto es el de ese fragmento y no el total. Un
  `Page overlay` se repite en las páginas que dice su `Pages`. Un `Frame` se parte como cualquier bloque, salvo si está
  girado: entonces, como una imagen, pasa entero a la página siguiente si no cabe en lo que queda de la actual
  ([Capas y giro](layers-and-rotation.md#giro-rotate)).
- **Giradas en flujo.** Un `Block`, un `Text` o un `Paragraph` girado tampoco se parte: como una imagen, entero o en la
  página siguiente, con el alto del rectángulo que lo envuelve.
- **Recortadas.** Una caja con `Overflow: clip` tampoco: entera o en la página siguiente
  ([Desbordamiento](overflow.md#recortar-clip)).

## Números de página: `{page}` y `{pages}`

`{page}` y `{pages}` en un texto se sustituyen por el número impreso de la página y el último número de su tanda, según
el `Page numbering` y el `Page number format` de cada sección
([Document](../components/document.md#numeración-de-las-páginas)). El reparto en líneas y en páginas se hace con el
ancho de un número de dos cifras (`00`), o de tantas como tenga el más largo si son más (en romanos, tantas `M` como
letras tenga el más largo). El número real se pone después en ese hueco, y la línea se vuelve a alinear sin cambiar sus
saltos.

`{page of: id}`, en cambio, se mide con la página escrita, así que puede cambiar el reparto
([Referencias](references.md)).

## Lo que no hay

- **Páginas en blanco para imprimir a doble cara** (`Before: odd-page`): una página que nadie escribió, y habría que
  decidir si se numera.
- Partir por dentro un hijo ya colocado para dejar sitio a lo que sigue.
