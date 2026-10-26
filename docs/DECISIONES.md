# Decisiones pendientes de repaso

Decisiones de interpretación tomadas durante la implementación: huecos de la especificación,
diferencias con CSS y simplificaciones. Se apuntan aquí al tomarlas y se repasan todas juntas
cuando se terminen los hitos. Al repasar cada una: confirmarla (y llevarla a la especificación
o al §8 si es una diferencia con CSS) o cambiar el código.

Cada entrada indica el hito, dónde está en el código, qué se hace y la alternativa.

## Posicionamiento (hito 3)

1. **Alto del área útil en modo continuo.** Cuando el containing block de una caja
   `absolute` o `fixed` es el área útil, su alto es el del flujo del documento, porque el área
   útil no tiene alto. Ya está en la especificación (§5.5, "Orden de cálculo").
   *Dónde:* `fragment/Paginator`, `layout/position/AbsoluteLayout`.
   *Alternativa:* exigir `Height` en `Media` también en continuo, o tratar el `Offset` inferior
   como `auto`.

2. **Las `absolute` cuelgan del fragmento de su containing block**, detrás de sus hijos en
   flujo, y no del de su padre en el árbol de cajas. Afecta al anidamiento del volcado (§7) y al
   "orden del árbol" del pintado (§5.6): una `absolute` se pinta después de una hermana
   `relative` posterior en el documento si su containing block está más arriba.
   *Dónde:* `layout/position/AbsoluteLayout.resolvePending`, `paint/StackingContext`.
   *Alternativa:* insertarlas en orden del árbol o volcarlas bajo su padre.

3. **Posición estática vertical en flujo de bloque:** el cursor más los márgenes colapsados
   anteriores, sin colapsar con el margen superior de la propia caja posicionada.
   *Dónde:* `layout/block/BlockLayout`.
   *Alternativa:* simular el colapso como si la caja estuviera en flujo.

4. **`fixed` con desplazamientos `auto`:** se usa la posición estática de la página en la que
   aparece la caja por primera vez, y se repite igual en todas las páginas.
   *Dónde:* `fragment/Paginator`.

5. **`Offset` vertical en porcentaje en una `relative` con containing block de alto
   indefinido:** se comporta como `auto` (desplazamiento 0), como en CSS.
   *Dónde:* `layout/position/RelativeOffset`.

## Flex (hito 4)

6. **Un ítem con `Width` fijo no encoge por debajo de ese ancho.** Se sigue la especificación
   al pie de la letra: con `Min width: auto` el mínimo es el min-content del ítem (§5.7), y una
   caja con `Width` fijo tiene ese valor como min-content (§5.9). En CSS, un ítem con
   `width: 400px` sí encoge hasta el min-content de su contenido, así que `Shrink` solo afecta
   aquí a ítems sin ancho declarado.
   *Dónde:* `layout/flex/FlexLayout.rowItem`.
   *Alternativa (CSS):* mínimo automático = min(ancho declarado, min-content del contenido);
   necesita los intrínsecos del contenido sin el `Width` de la caja.

7. **En `column`, el mínimo automático de un ítem con `Height` declarado es ese alto**, no el
   alto de su contenido como en CSS.
   *Dónde:* `layout/flex/FlexLayout.columnItem`.

8. **Una sola línea (sin `Wrap`) con tamaño cruzado definido mide lo que el contenedor**, como
   en CSS. Ya está en la especificación (§5.7, paso 6).
   *Dónde:* `layout/flex/FlexLayout`.

9. **Sobrante negativo en `Main axis` y `Lines`:** `between` se alinea al principio y
   `around` y `evenly` se centran, como en CSS. La especificación no lo dice.
   *Dónde:* `layout/flex/FlexLayout.distribute`.

10. **Posición estática de un hijo `absolute` de un contenedor flex:** la esquina de su caja de
    contenido. En CSS se coloca como si fuera el único ítem, con la alineación del contenedor.
    *Dónde:* `layout/flex/FlexLayout`.

11. **Intrínsecos de flex:** la tabla del §5.9 tal cual, con los intrínsecos de cada ítem; no
    tiene en cuenta `Basis`, `Grow` ni `Shrink`.
    *Dónde:* `layout/flex/FlexLayout.intrinsic`.

12. **`column` con `stretch`:** en un contenedor de una sola línea, los ítems se miden ya con el
    ancho del contenedor; con `Wrap`, con su ajuste al contenido y después se estiran a la línea
    sin recalcular su tamaño principal.
    *Dónde:* `layout/flex/FlexLayout.columnItem`.

## Grid (hito 5)

13. **Márgenes `auto` de los ítems de grid:** valen 0, como en flex. En CSS alinean el ítem en
    su área (dos `auto` lo centran).
    *Dónde:* `layout/grid/GridLayout`.

14. **Pista en porcentaje sobre un tamaño indefinido** (filas con el contenedor en
    `Height: auto`, o al calcular intrínsecos): vale 0. En CSS se comporta como `auto`.
    *Dónde:* `layout/grid/TrackSizing`.

15. **Un ítem que ocupa varias pistas y alguna es flexible no aporta nada a su tamaño**
    (simplificación que ya traía `TrackSizing`). Un ítem largo que cruza una columna `1fr`
    puede desbordar su área.
    *Dónde:* `layout/grid/TrackSizing.distributeExcess`.

16. **Porcentajes de alto de los ítems:** se resuelven contra el alto del área, pero solo en el
    layout final; al medir para dimensionar las filas cuentan como `auto`.
    *Dónde:* `layout/grid/GridLayout.finalLayout`.

17. **Intrínsecos de grid:** min-content = columnas dimensionadas sin espacio libre; max-content =
    columnas con espacio infinito y cada ítem aportando su max-content también como mínimo, de
    modo que las fracciones llegan al max-content de su contenido.
    *Dónde:* `layout/grid/GridLayout.intrinsic`.

18. **Posición estática de un hijo `absolute` de un contenedor grid:** la esquina de su caja
    de contenido (como en flex, decisión 10).
    *Dónde:* `layout/grid/GridLayout`.

## Fragmentación y PDF (hito 6)

19. **Una caja con `Height` definido se parte como cualquier otra**, aunque no tenga contenido:
    lo que no cabe de su alto pasa a la página siguiente (como en CSS). Para que no se parta
    hay que declarar `Break inside: avoid`, y aun así se parte si no cabe en una página entera.
    *Dónde:* `layout/Slice`.
    *Alternativa:* tratar como monolíticas las cajas sin contenido.

20. **Alto de un fragmento cortado:** con alto `auto` mide hasta el corte (el fondo no llega al
    final de la página); con alto definido llega hasta el final de la página, sin pasar del alto
    que le queda. `Min height` y `Max height` se aplican al total de los fragmentos.
    *Dónde:* `layout/Slice`.
    *Alternativa:* estirar siempre hasta el final de la página, como hace Chromium.

21. **El borde y el relleno inferiores no se reservan:** si el contenido cabe y solo el borde o
    el relleno inferiores se salen de la página, la caja termina igualmente y desborda.
    *Dónde:* `layout/Slice`.

22. **Solo se vuelve atrás a cortes entre hijos.** Si el mejor corte es anterior al hijo que no
    cabe, se descarta lo colocado después; nunca se repite el layout de un hijo anterior con
    menos espacio para partirlo por dentro.
    *Dónde:* `layout/block/BlockLayout`.

23. **Relajar viudas y huérfanas:** un párrafo que no puede respetarlas y no tiene nada antes
    en la página ofrece igualmente un corte (calidad `VIOLATES_WIDOWS`), para que el padre
    pueda preferirlo a un corte que incumpla `avoid`. Se respetan las viudas si se puede y, si
    no, se llena el espacio.
    *Dónde:* `fragment/BreakRules.cutLines`, `relaxedLines`.

24. **Cortes forzados:** el `Break before: page` del primer hijo de un bloque (y el
    `Break after: page` del último) se propaga a su padre, como en CSS. Un corte forzado al
    principio de una página se ignora (no deja páginas vacías), y también el
    `Break after: page` de la última caja del documento.
    *Dónde:* `fragment/BreakRules.forcedBefore`, `forcedAfter`; `layout/block/BlockLayout`.

25. **Márgenes en los cortes:** tras un corte no forzado se truncan el margen superior del
    primer hijo de la página y los que salen de él por arriba. Un fragmento de continuación no
    deja salir márgenes por arriba.
    *Dónde:* `layout/block/BlockLayout`.

26. **Flex y grid cortan entre unidades:** entre líneas (`row`), entre ítems (`column`) o entre
    filas. Un contenedor `column` con `Wrap` no se parte. No se corta entre dos filas que ocupa
    un mismo ítem. Desde el hito 7, una unidad más alta que la página parte sus ítems en
    paralelo (decisiones 35 a 37).
    *Dónde:* `layout/UnitBreaker`, `layout/flex/FlexLayout`, `layout/grid/GridLayout`.

27. **Cajas `absolute` con el containing block partido:** se resuelven con el fragmento de su
    containing block de la página donde cae su posición estática; su alto es el de ese
    fragmento, no el total.
    *Dónde:* `layout/position/AbsoluteLayout.resolvePending`.

28. ~~**`Decoration: clone` se trata como `slice`** hasta el hito 7.~~ Resuelta en el hito 7:
    ver decisiones 33 y 34.

29. **Fuentes en PDF:** solo las 14 estándar y los alias que reconoce PDFBox (`Arial`,
    `TimesNewRoman`…); una fuente desconocida es un error al maquetar con `PdfMetrics`, no una
    validación del árbol (el SVG admite cualquiera). La especificación (§4.6) habla también de
    fuentes registradas, que quedan pendientes. Los caracteres fuera de WinAnsi se pintan y se
    miden como `?`.
    *Dónde:* `layout/inline/PdfMetrics`, `paint/PdfCanvas`.

30. **Línea base aproximada:** la de `StackingContext` (línea centrada con el 80 % del cuerpo
    por encima), también en PDF; no se usan el ascendente ni el descendente reales de la fuente.
    *Dónde:* `paint/StackingContext`.
    *Alternativa:* calcularla con las métricas de la fuente.

31. **`new Engine()` sigue usando `FixedMetrics`**, para que los tests y los casos golden sean
    deterministas y calculables a mano; el PDF se maqueta con `new Engine(new PdfMetrics())`
    (`PdfCanvas.write(document, out)` lo hace).
    *Dónde:* `layout/Engine`.

## Texto justificado (hito 2, terminado después del 6)

32. **`justify`:** el sobrante de la línea se reparte a partes iguales entre sus espacios (en
    PDF, con el espaciado de palabras `Tw`; en SVG, con `word-spacing`). No se justifican la
    última línea de cada párrafo, las líneas de una sola palabra ni las que desbordan (no se
    comprimen). Una línea cortada por un salto de página no es la última de su párrafo, así que
    se justifica.
    *Dónde:* `layout/inline/LineBreaker.wordSpacing`, `layout/inline/InlineLayout`.
    *Alternativa:* repartir también entre letras, o dejar sin justificar la última línea de
    cada fragmento.

## Pulido (hito 7)

33. **`Decoration: clone` con `Height` definido:** el alto declarado es el de una caja
    completa, así que el total del contenido es ese alto menos un borde y un relleno, y cada
    fragmento les suma los suyos (`Height: 250` con borde 5 en páginas de 100 da 100 + 100 + 70).
    `Min height` y `Max height` se aplican igual: al contenido total más un borde y un relleno.
    El alto consumido del `BreakToken` cuenta solo la caja de contenido.
    *Dónde:* `layout/Slice.cloned`.
    *Alternativa:* que el alto declarado sea la suma de los fragmentos con su decoración (como
    con `slice`).

34. **Con `clone` se reservan el borde y el relleno inferiores** en cada página: el contenido
    corta antes para que quepan. Con `slice` no se reservan (decisión 21).
    *Dónde:* `layout/Slice.contentRemaining`.

35. **"Una fila que no cabe en una página" es una que no cabe ni al principio de una página.**
    Una fila que no cabe en lo que queda de página pasa entera a la siguiente, como hasta el
    hito 6; solo si tampoco cabe ahí se parten sus ítems en paralelo. Mantiene el caso golden 06.
    *Dónde:* `layout/UnitBreaker`.
    *Alternativa (CSS):* partir la fila en cuanto no cabe en lo que queda de página, comparando
    la calidad de ese corte con la de cortar antes de la fila.

36. **El corte en paralelo también parte los ítems de flex `column`** que no caben en una
    página (la especificación solo habla de filas). Un contenedor `column` con `Wrap` sigue sin
    partirse.
    *Dónde:* `layout/flex/FlexLayout` (`Units.splittable`).
    *Alternativa:* dejar que esos ítems desborden.

37. **Continuación de una fila partida en paralelo:** los ítems que no han terminado siguen
    desde arriba de la página siguiente, sin su margen superior; los que han terminado no
    tienen más fragmentos. Un ítem estirado (alto impuesto por la línea o el área) reparte ese
    alto entre las páginas, así que puede acabar antes o después que el texto de sus hermanos.
    La fila continúa con el alto del ítem más largo y las filas siguientes van detrás, con el
    hueco que las separaba. Las filas entre las que no se puede cortar se parten juntas, cada
    ítem desde su posición. Si algún ítem no puede colocar nada, la fila no se parte y desborda
    como antes. La calidad del corte es la peor de los ítems que se parten
    (`Break inside: avoid` la deja en `FORCED`).
    *Dónde:* `layout/UnitBreaker.split`.
    *Alternativa:* alinear la continuación de cada ítem con su posición en la fila.

38. **Numeración en dos fases:** en la primera, `{page}` y `{pages}` se miden como `00`
    (reservan dos cifras); en la segunda, solo se rehacen las líneas que los contienen: texto,
    ancho, alineación y espaciado de `justify` (solo si la línea ya estaba justificada). Los
    saltos de línea y los altos de la primera fase no cambian, así que con 100 páginas o más el
    número puede desbordar su línea. En modo continuo valen 1 y 1.
    *Dónde:* `layout/inline/PageNumbers`, `InlineLayout.number`, `fragment/Paginator`.
    *Alternativa:* volver a maquetar la caja entera (puede cambiar su alto) o reservar tantas
    cifras como tenga el total de la primera fase y paginar otra vez si no bastan.

39. **Knuth-Plass simplificado (`Text wrap: pretty`):** sin guiones, sin encogimiento de los
    espacios (las líneas justificadas no se comprimen, decisión 32) y sin clases de ajuste
    (*fitness classes*). Cada espacio se estira hasta la mitad de su ancho; `badness = 100 × r³`
    hasta 10 000; deméritos = (10 + badness)²; la última línea del párrafo no tiene badness. El
    nombre sigue a CSS (`text-wrap: pretty`), aunque en los navegadores `pretty` solo retoca
    las últimas líneas.
    *Dónde:* `layout/inline/LineBreaker.optimal`.
    *Alternativa:* guiones con un diccionario, encogimiento y clases de ajuste, como en TeX.
