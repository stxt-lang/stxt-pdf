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
- entre líneas de un `Stack` y entre filas de un `Grid` o una `Table`, y dentro de una línea o una fila, partiendo sus
  ítems en paralelo ([abajo](#cortes-en-stack-grid-y-table)).

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

Una caja que contiene los márgenes de sus hijos (una caja con borde o relleno, con `Isolate margins`, la columna de un
`Columns`) contiene también en su alto el margen inferior de su último hijo, así que ese margen tiene que caber en la
página como cabe el hijo: si el hijo cabe y su margen no, el corte va antes del hijo (o antes, si ese corte es mejor), y
antes de la caja si dentro de ella no puede cortar nada. El área de página es la excepción: su final es el final de la
página, y el margen inferior de su último hijo se trunca ahí. Una caja con `Height` tiene el alto que tiene, haga lo que
haga su contenido.

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

Ese hueco tampoco se lo quita a las cajas que la contienen: para ellas, lo que ocupa el fragmento es lo que ocupa su
contenido, aunque se vea hasta el final de la página. Así, una columna estirada a su fila que contiene un texto
estirado a la suya sigue teniendo sitio en la página siguiente para las líneas que le quedan.

> **Decisión:** un fragmento cortado consume solo lo que ocupa su contenido, para que una caja (o un ítem de grid
> estirado) no desborde por partirse, a cambio de que el alto que se ve pueda pasar del `Height`.
> *Alternativa descartada:* un fragmento que mide solo lo que ocupa su contenido (no llega al final de la página), o dejar que el contenido desborde el último fragmento.

## Cortes en `Stack`, `Grid` y `Table`

En un `Stack` vertical, el corte se hace entre ítems. En uno horizontal y en un `Grid`, entre líneas o filas. Una línea, fila o
ítem que no cabe en lo que queda de página se parte **en paralelo** en esa página (`Split rows` en `Grid` y `Table`,
`Split items` en `Stack`; los dos, `true` por defecto): cada ítem se parte por su cuenta y la fila continúa en la página
siguiente con los ítems que no han terminado, todos desde arriba, y con la altura del más largo de ellos. Las filas
siguientes van detrás, con el hueco que las separaba. Las filas que ocupa un mismo ítem se parten juntas, cada ítem desde
su posición. Un ítem estirado (`stretch`, sin alto propio) mide en cada página lo que su fila o su línea: llega al final
de la página mientras la fila sigue y, en la última, acaba donde acaba la fila, que es donde acaba el más largo de los
ítems con su margen inferior, aunque el alto que le impone la fila sin partir le dé para menos. El margen inferior del
ítem estirado se descuenta. Así, dos columnas con fondo acaban a la misma altura en cada página, también en la última.

Tres ítems estirados no se alargan y conservan el alto que tenían sin partir: el de una fila que se
parte junto con las siguientes, que acaba con su propia fila y no con la última; el que `Max height` deja más corto que
su fila; y la caja que no se parte (`Rotate`, `Overflow: clip`).

> **Decisión:** un ítem estirado acaba con su fila, como las celdas de una tabla. Las líneas de un texto dejan un hueco al
> final de cada página que no se le quita a su alto ([Alto definido y cortes](#alto-definido-y-cortes)), así que la fila
> partida mide más que sin partir; un hermano estirado al alto de la fila sin partir quedaba más corto en la última
> página, o se acababa una página antes. *Alternativa descartada:* repartir entre las páginas el alto de la fila sin
> partir, que fue la regla hasta la 0.10.0.

Mientras la fila sigue, cada ítem avanza a su ritmo: un texto de líneas más altas coloca menos alto en cada página, y un
ítem estirado vacío llega al final. Para las cajas que contienen la fila, la fila ha consumido de su alto hasta donde ha
llegado **el más lento de los ítems que siguen**, no hasta donde llega el fragmento del que más avanza
([Alto definido y cortes](#alto-definido-y-cortes)). El sitio que la fila sin partir deja debajo de un ítem cuenta como
avanzado por él. Así, una columna con alto definido que contiene la fila tiene sitio en su última página para lo que le
queda al ítem más lento.

> **Decisión:** la fila partida consume lo que el más lento de sus ítems, para que la caja que la contiene no desborde.
> *Alternativa descartada:* lo que ocupa el ítem que más avanza, que fue la regla hasta la 0.10.0: junto a un ítem
> estirado vacío, o a un texto de líneas más bajas, la columna se quedaba sin el alto que el otro ítem aún necesitaba
> (`BOX_OVERFLOW`).

Partir la fila es un corte más, que compite con los cortes entre filas por las reglas de la [elección del
corte](#elección-del-corte): su calidad es la peor de la de sus ítems, así que una fila cuyos ítems no pueden respetar
sus viudas y huérfanas o un `Keep together` (en una `Table`, el de la `Row`, que es el de sus celdas) pasa entera a la
página siguiente si el corte antes de ella es mejor. Si algún ítem no puede colocar nada, la fila tampoco se parte. Con
`Split rows: false` (`Split items: false`), toda fila que no cabe pasa entera a la página siguiente, y solo se parte en
paralelo si no cabe ni al principio de una página; si entonces algún ítem no puede colocar nada, es un error.

> **Decisión:** las filas se parten en la página en la que están, por defecto, como en los procesadores de textos y como
> los hijos de un `Block`: dos columnas paralelas de texto siguen cada una por su lado y no dejan media página en blanco,
> y una fila corta no se parte igualmente, porque sus viudas y huérfanas lo impiden. Quien quiere filas enteras (una
> rejilla de tarjetas) lo dice con `Split rows: false`. *Alternativa descartada:* partir solo la fila que no cabe en una
> página entera, que fue la regla hasta la 0.8.0: una fila que cabía en una página dejaba la anterior casi vacía.

Un `Stack` vertical con `Wrap` no se parte. Las tablas repiten además su cabecera y su pie
([Table](../components/table.md#cortes)).

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
- **Cajas laterales.** Una `Image`, un `Barcode` o un `Block` con `Side` tampoco: pasa a la página siguiente con la
  primera línea que la rodea, y las líneas acortadas a su lado acaban con la página: en la siguiente, el texto tiene el
  ancho entero ([Block](../components/block.md#cajas-laterales-side)).

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
