# Desbordamiento

**Nada puede desbordar.** El motor comprueba el resultado ya paginado y da un error con la primera de estas situaciones:

- **una palabra más ancha que su línea, o una línea de un texto con `White space: pre` más ancha que su caja**
  (`TEXT_OVERFLOW`), en la línea del documento donde está la palabra;
- **una caja que no cabe a lo ancho en la caja de contenido de su padre**, contando sus márgenes horizontales
  (`BOX_OVERFLOW`), en la línea de su `Width` (o `Min width`) si lo declara;
- **contenido que pasa del alto de su caja**: líneas o hijos que no caben en el alto que fija su `Height`, su `Max height`,
  su `Aspect ratio` o, en un `Grid`, sus filas de tamaño fijo (`BOX_OVERFLOW`), en la línea de esa propiedad (`Rows` para
  las filas; si declara varias, la primera de esta lista);
- **algo del flujo que no cabe en el área útil de una página y no se puede partir** (`PAGE_OVERFLOW`), en la línea de lo
  que impide partirlo: su `Padding` o su `Border` si el relleno y el borde del fragmento ya no caben en la página; si no,
  su `Line height` (o, sin él, su `Size`), propios o heredados, si es una línea de texto; si no, su `Height` (en una
  `Image` sin él, su `Width`, del que sale el alto); en un `Relative` girado, que no se parte, su `Rotate`;
- **un `Absolute` o un `Fixed` que se sale de la página física** (`BOX_OVERFLOW`), en la línea de su `Offset`. Pueden
  ocupar los márgenes del medio, pero no salir de la página;
- **una caja girada que se sale de la página física** (`BOX_OVERFLOW`), en la línea de su `Rotate`: lo que se comprueba es
  el rectángulo que envuelve lo que se pinta, con los giros de sus ancestros ([Capas y giro](capas-y-giro.md#giro-rotate)).
  Un `Absolute` o un `Fixed` girado se comprueba solo así (puede salirse sin girar y caber girado), y uno sin girar dentro
  de una caja girada, también, en la línea del elemento;
- **el marcador de una lista que no cabe en su sangría** (`LIST_MARKER_OVERFLOW`, [List](../componentes/list.md#posición));
- **una tabla cuya cabecera y pie no dejan sitio al cuerpo** al principio de una página (`TABLE_REPEAT_OVERFLOW`,
  [Table](../componentes/table.md#cortes)), en la línea de la `Table`;
- **el contenido de un `Multicol` que no cabe en sus columnas** y no puede seguir en otra página (`BOX_OVERFLOW`,
  [Multicol](../componentes/multicol.md#lo-que-no-cabe)): en la línea de su `Height` (o `Max height`, o `Aspect ratio`) si
  es su alto el que se acaba, o en la de su `Count` si, sin página ni alto que llenar, tiene más cortes de columna que
  columnas.

El error señala al culpable **más interior**: si una caja se sale porque se sale algo de dentro, se nombra lo de dentro.
Los `Relative` se comprueban en su sitio, sin el desplazamiento de `Offset` ni el giro de `Rotate` (lo girado se comprueba
además contra la página), y un margen negativo cuenta como espacio ganado: no son desbordamientos, sino decisiones del
autor. Tampoco cuentan los márgenes verticales, que colapsan. Una caja puede salirse hasta 0,01 pt sin que sea un
desbordamiento, lo que acumulan las medidas de las fuentes en milésimas.

Una caja que declara su ancho es la culpable de sus hijos que solo la llenan: no se baja a un hijo salvo que se salga
también de ella. En una caja de ancho `auto` sí se baja, porque su ancho sale de su contenido. Una caja cuyo alto impone el
padre (un ítem estirado a una fila fija) se señala a sí misma, no las `Rows` del `Grid`.

Lo girado se comprueba como se ve: el rectángulo que envuelve la caja girada, con el `Offset` de un `Relative` incluido.
Las cajas en flujo de dentro de una girada se comprueban contra su padre sin girar.

> **Decisión:** el error señala la propiedad que hay que cambiar (el `Width`, el `Height`, el `Line height`), no la caja.
> *Alternativa descartada:* la línea de la caja, o comparar siempre con el padre inmediato, que en una caja `auto` señala
> la caja y no lo que la ensancha.
