# Páginas y cortes: `Break`

## El grupo `Break`

Los cortes de página. No se heredan. No se admite en `Absolute` ni en `Fixed`, que no se parten ni están en el flujo de
la página, ni en los encabezados ni en las celdas ([Encabezados](../componentes/heading.md#cortes),
[Table](../componentes/table.md#filas)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Before` | `auto`, `page`, `column`, `avoid` | `auto` | `page` fuerza un salto de página antes de la caja; `column`, un salto a la columna siguiente (solo dentro de un `Multicol`, [Multicol](../componentes/multicol.md#cortes)). |
| `After` | `auto`, `page`, `column`, `avoid` | `auto` | `avoid` mantiene la caja con la siguiente (títulos). |
| `Inside` | `auto`, `avoid` | `auto` | `avoid` intenta no partir la caja. |
| `Decoration` | `slice`, `clone` | `slice` | Qué pasa con el borde y el relleno en un corte. |

## Modelo

Un **contenedor de fragmentos** es donde caen los trozos de las cajas: uno por página, con la altura del área útil, y uno
por columna dentro de un `Multicol` ([Multicol](../componentes/multicol.md)). Cada caja produce un **fragmento** por
contenedor de fragmentos en el que aparece.

## Oportunidades de corte

- entre cajas hermanas en flujo de bloque;
- entre líneas de texto;
- entre líneas de un `Flex` y entre filas de un `Grid` o una `Table`.

Siempre se intenta cortar en el punto más alto del árbol posible; solo si no cabe se corta dentro de un hijo.

## Elección del corte

El motor elige el **último** corte de la página que no incumple ninguna regla. Si no hay ninguno, relaja las reglas en
este orden:

1. `Widows` y `Orphans` (de `Typography`);
2. `Before` y `After: avoid`;
3. `Inside: avoid`.

Si sigue sin haber corte posible, corta donde se acaba el espacio.

Un párrafo que no puede respetar `Widows` y `Orphans` y no tiene nada antes en la página ofrece igualmente un corte que
las incumple, para que su padre pueda preferirlo a otro que incumpla `avoid`. En ese corte se respetan las viudas si se
puede y, si no, se llena el espacio.

Solo se vuelve atrás a cortes entre hijos: si el mejor corte es anterior al hijo que no cabe, se descarta lo colocado
después, pero nunca se repite el layout de un hijo anterior con menos espacio para partirlo por dentro.

## Garantía de progreso

Al principio de una página siempre se coloca algo, aunque haya que cortarlo a la fuerza. Lo que no se puede partir y no
cabe en una página entera (una línea más alta que el área útil, una imagen, una fila con un ítem que no puede colocar
nada) es un error (`PAGE_OVERFLOW`, [Desbordamiento](desbordamiento.md)).

## Cortes forzados

`Before: page` y `After: page` fuerzan un salto de página, y `Before: column` y `After: column`, dentro de un `Multicol`,
un salto a la columna siguiente (que, en la última columna, es también de página). Solo se aplican a cajas en flujo:
`Absolute` y `Fixed` no admiten `Break`.

- El `Before` del primer hijo de un bloque y el `After` del último se propagan al bloque; de un `Multicol` solo sale el de
  página.
- Si una caja tiene los dos, gana el de página.
- Un corte forzado al principio de una página (o de una columna) se ignora: no deja páginas en blanco. Lo mismo el
  `After: page` de la última caja de una sección.
- Un corte de página dentro de un `Multicol` deja sin llenar las columnas que quedan en esa página.
- Dentro de una caja que no se parte (un `Relative` girado, un `Absolute`), los cortes forzados de sus descendientes no
  tienen efecto.

> **Decisión:** un corte forzado de página tiene una calidad propia, por encima de la de un corte perfecto, para que
> atraviese las columnas: el `Multicol` deja de llenarlas y pasa el corte a su padre; el de columna es un corte perfecto
> dentro de las columnas. *Límite:* en los ítems de un `Flex` o un `Grid` partidos en paralelo, el corte de la fila es el
> peor de sus ítems, así que un corte de página dentro de uno de ellos actúa como de columna. *Alternativa descartada:*
> marcar aparte en el resultado del layout si un corte es de página.

## Márgenes en los cortes

En un corte no forzado, los márgenes que tocan el corte se truncan a cero: tras el corte, el margen superior del primer
hijo de la página y los que salen de él por arriba; un fragmento de continuación no deja salir márgenes por arriba. En un
corte forzado, se trunca el margen anterior al corte y se conserva el posterior.

## Bordes y relleno en los cortes

Con `Decoration: slice`, el primer fragmento no tiene borde ni relleno inferiores y los siguientes no tienen los
superiores. Con `Decoration: clone`, cada fragmento se pinta como una caja completa: el contenido de cada página cabe entre
su borde y relleno superiores e inferiores.

Con `slice`, el borde y el relleno inferiores solo se reservan cuando hacen falta: si la caja terminaría en la página pero
ellos no caben, se maqueta de nuevo con ese espacio reservado, de modo que se corta antes (o pasa entera a la página
siguiente) y termina allí con su borde y su relleno. Si la caja se corta de todos modos, su fragmento llega hasta el final
de la página.

> **Decisión:** con `slice` el borde y el relleno inferiores no se reservan siempre, como con `clone`, sino solo si la caja
> terminaría en la página: así no queda un hueco al final de cada página cuando la caja se corta de todos modos.

## Alto definido y cortes

Un `Height` fijo es el total de todos los fragmentos, no el de cada uno; con `clone`, el total del contenido es ese alto
menos un borde y un relleno, y cada fragmento les suma los suyos. `Min height` y `Max height` también se aplican al total
(con `clone`, al contenido total más un borde y un relleno).

Una caja con `Height` definido se parte como cualquier otra, aunque no tenga contenido: lo que no cabe de su alto pasa a la
página siguiente. Para evitarlo se declara `Inside: avoid`, que no impide partirla si no cabe en una página entera.

Un fragmento cortado de una caja con alto `auto` mide hasta el corte; con alto definido, llega hasta el final de la página
sin pasar del alto que le queda, pero solo **consume** de ese alto lo que ocupa su contenido (con su borde y relleno
superiores): el hueco que dejan las líneas o los hijos al final de la página no se le quita al contenido que sigue. Así,
una caja con `Height` igual al alto de su contenido no desborda al partirse, y el alto que se ve puede pasar del `Height`
en lo que suman esos huecos.

> **Decisión:** un fragmento cortado de una caja con alto definido llega hasta el final de la página pero solo consume lo
> que ocupa su contenido. Antes, una caja con `Height` igual al alto de su contenido desbordaba al partirse (un `Text` de
> `Height: 240` con 20 líneas, en páginas de 100, acababa con 40 de caja para 48 de líneas), y también un ítem de grid
> estirado a una fila que se parte, sin que el autor pudiera evitarlo. A cambio, el alto que se ve puede pasar del
> `Height`. *Alternativa descartada:* que el fragmento mida solo lo que ocupa su contenido (el `Height` se respeta
> exactamente, pero la caja ya no llega al final de la página), o dejar que el contenido desborde el último fragmento.

## Cortes en `Flex`, `Grid` y `Table`

En un `Flex` en `column`, el corte se hace entre ítems. En `row` y en un `Grid`, entre líneas o filas. Una línea, fila o
ítem que no cabe en lo que queda de página pasa entero a la siguiente. Si tampoco cabe al principio de una página, sus
ítems se parten **en paralelo**: cada uno se parte por su cuenta y la fila continúa en la página siguiente con los ítems
que no han terminado, todos desde arriba, y con la altura del más largo de ellos; las filas siguientes van detrás, con el
hueco que las separaba. Las filas que ocupa un mismo ítem se parten juntas, cada ítem desde su posición. Un ítem estirado
reparte entre las páginas el alto que le impone su línea o su área, así que puede terminar antes o después que el texto de
sus hermanos. Si algún ítem no puede colocar nada, la fila no se parte, y si no cabe es un error.

Al elegir el corte, una fila partida cuenta como el peor de los cortes de sus ítems. Un `Flex` en `column` con `Wrap` no
se parte. Las tablas repiten además su cabecera y su pie ([Table](../componentes/table.md#cortes)).

## Lo que no se parte

- **Imágenes.** Una `Image` no se parte: si no cabe en lo que queda de la página (o de la columna), pasa entera a la
  siguiente ([Image](../componentes/image.md#cortes-de-página)).
- **Posicionadas.** Un `Absolute` se coloca en la página donde cae su posición y no se parte. Si su contenedor de
  referencia está partido, se resuelve contra el fragmento de esa página: su alto es el de ese fragmento, no el total. Un
  `Fixed` se repite en las páginas que dice su `Pages`. Un `Relative` se parte como cualquier bloque, salvo si está
  girado: entonces, como una imagen, pasa entero a la página siguiente si no cabe en lo que queda de la actual
  ([Capas y giro](capas-y-giro.md#giro-rotate)).

## Números de página: `{page}` y `{pages}`

`{page}` y `{pages}` en un texto se sustituyen por el número impreso de la página y el último número de su tanda, según
el `Page numbering` de cada sección ([Document](../componentes/document.md#numeración-de-las-páginas)). Se resuelven en
dos fases: se pagina reservando el ancho de un número de prueba de dos cifras (`00`) y después se sustituyen los valores
reales. Si el total tiene más cifras (100 páginas o más), antes se vuelve a paginar reservando tantas cifras como tenga el
total, y así hasta que basten. Si el texto cambia de ancho, solo se vuelve a maquetar esa caja: sus líneas conservan los
saltos y se vuelven a medir y alinear. Como los `Fixed` no ocupan espacio, el flujo principal no cambia.

`{page of: id}`, en cambio, se resuelve paginando hasta que el valor supuesto coincida con el real
([Referencias](referencias.md)): el texto se mide ya con la página escrita, así que puede cambiar el reparto en líneas y
en páginas.

> **Decisión:** con 100 páginas o más se vuelve a paginar reservando las cifras del total, en lugar de dejar que el número
> real se salga del hueco reservado.

## Lo que no hay

- **Páginas en blanco para imprimir a doble cara** (empezar en página impar).
  > **Decisión:** no hay `Before: odd-page` ni `even-page`: una página en blanco que nadie escribió sería un valor por
  > defecto silencioso, y habría que decidir si se numera. *Alternativa descartada:* `odd-page` y `even-page` en `Before` y
  > `After`.
- Volver a maquetar un hijo ya colocado con menos espacio para partirlo por dentro.
