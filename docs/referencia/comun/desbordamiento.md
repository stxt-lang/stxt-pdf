# Desbordamiento

**Nada puede desbordar.** Después de maquetar y paginar, el motor comprueba el resultado y da un error con la primera de
estas situaciones:

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
  [Table](../componentes/table.md#cortes)), en la línea de la `Table`. Se detecta al maquetar, porque sin sitio la tabla no
  avanza;
- **el contenido de un `Multicol` que no cabe en sus columnas** y no puede seguir en otra página (`BOX_OVERFLOW`,
  [Multicol](../componentes/multicol.md#lo-que-no-cabe)): en la línea de su `Height` (o `Max height`, o `Aspect ratio`) si
  es su alto el que se acaba, o en la de su `Count` si, sin página ni alto que llenar, tiene más cortes de columna que
  columnas. También se detecta al maquetar.

El error señala al culpable **más interior**: si una caja se sale porque se sale algo de dentro, se nombra lo de dentro.
Los `Relative` se comprueban en su sitio, sin el desplazamiento de `Offset` ni el giro de `Rotate` (lo girado se comprueba
además contra la página), y un margen negativo cuenta como espacio ganado: no son desbordamientos, sino decisiones del
autor. Tampoco cuentan los márgenes verticales, que colapsan. Una caja puede salirse hasta 0,01 pt sin que sea un
desbordamiento, lo que acumulan las medidas de las fuentes en milésimas.

> **Decisión:** los desbordamientos se comprueban sobre el resultado final, una vez paginado, y no durante el layout: así no
> cuentan las pasadas de medida, en las que una caja se maqueta con un espacio provisional. *Alternativa descartada:*
> detectarlo en cada algoritmo al colocar, con el riesgo de confundir medidas provisionales con el resultado.

> **Decisión:** el culpable es lo más interior que se sale. A lo ancho se señala su `Width` (o `Min width`, o la caja); si es
> una palabra, la línea del documento de esa palabra. Una caja que declara su ancho es la culpable de sus hijos que solo la
> llenan: no se baja a un hijo de una caja así salvo que ese hijo se salga también de ella, porque lo que causa el
> desbordamiento es el `Width`. En una caja de ancho `auto` sí se baja, porque su ancho sale de su contenido.
> *Alternativa descartada:* comparar siempre con la caja del padre inmediato, que en una caja `auto` señala la caja y no lo
> que la ensancha.

> **Decisión:** los desbordamientos verticales señalan la propiedad que fija el alto. En `BOX_OVERFLOW`, si la caja no
> declara `Height` ni `Max height`, su `Aspect ratio` o, en un `Grid`, sus `Rows`. En `PAGE_OVERFLOW`, por este orden: el
> `Padding` o el `Border` si el relleno y el borde ya no caben en la página, el `Line height` o, sin él, el `Size` de una
> línea de texto, y el `Height` (en una `Image` sin él, el `Width`). *Límite:* una caja cuyo alto impone el padre (un ítem
> estirado a una fila fija) se señala a sí misma, no las `Rows` del `Grid`. *Alternativa descartada:* la línea de la caja.

> **Decisión:** lo girado se comprueba como se pinta: el rectángulo que envuelve la caja de borde girada, con el `Offset` de
> un `Relative` incluido, aunque el `Relative` se compruebe en su sitio sin él, porque lo que importa es que lo girado se
> vea. Las cajas en flujo de dentro de una girada se siguen comprobando contra su padre sin girar. *Alternativa
> descartada:* comprobar solo la caja girada y no lo que cuelga de ella.

> **Decisión:** una caja puede salirse hasta 0,01 pt sin desbordar. *Alternativa descartada:* la tolerancia de la
> aritmética (10⁻⁹), con la que una caja que se sale 0,005 pt (un `Width` con decimales, lo que acumulan las medidas de las
> fuentes) sería un error.
