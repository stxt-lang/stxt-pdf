# Decisiones pendientes de repaso

Decisiones de interpretación tomadas durante la implementación: huecos de la especificación,
diferencias con CSS y simplificaciones. Se apuntan aquí al tomarlas y se repasan todas juntas
cuando se terminen los hitos. Al repasar cada una: confirmarla (y llevarla a la especificación
o al §8 si es una diferencia con CSS) o cambiar el código.

Cada entrada indica el hito, dónde está en el código, qué se hace y la alternativa.

## Repaso tras el hito 7 (2026-10-26)

Se repasaron las decisiones 1 a 40. Las confirmadas están ya en la especificación (§5 y, si
son diferencias con CSS, §8) y se han quitado de aquí; la 31 (`new Engine()` usa
`FixedMetrics`) es del API, no del modelo, y está en el javadoc de `Engine`. Quedan las
abiertas, con su número original. Las decisiones nuevas siguen desde la 41.

## Cambiar el código

Pendientes de implementar, una a una. Al resolver cada una, actualiza la especificación y
quítala de aquí.

2. **Las `absolute` cuelgan del fragmento de su containing block**, detrás de sus hijos en
   flujo, y no del de su padre en el árbol de cajas. Afecta al anidamiento del volcado (§7) y al
   "orden del árbol" del pintado (§5.6): una `absolute` se pinta después de una hermana
   `relative` posterior en el documento si su containing block está más arriba. Incumple el
   §5.6.
   *Dónde:* `layout/position/AbsoluteLayout.resolvePending`, `paint/StackingContext`.
   *Propuesta:* pintar las posicionadas en el orden del documento; el volcado sigue anidándolas
   bajo su containing block.

6. **Un ítem con `Width` fijo no encoge por debajo de ese ancho.** Con `Min width: auto` el
   mínimo es el min-content del ítem (§5.7), y una caja con `Width` fijo tiene ese valor como
   min-content (§5.9). En CSS, un ítem con `width: 400px` sí encoge hasta el min-content de su
   contenido, así que `Shrink` solo afecta aquí a ítems sin ancho declarado.
   *Dónde:* `layout/flex/FlexLayout.rowItem`.
   *Propuesta (CSS):* mínimo automático = min(ancho declarado, min-content del contenido);
   necesita los intrínsecos del contenido sin el `Width` de la caja. Se resuelve junto con la 7.

7. **En `column`, el mínimo automático de un ítem con `Height` declarado es ese alto**, no el
   alto de su contenido como en CSS.
   *Dónde:* `layout/flex/FlexLayout.columnItem`.
   *Propuesta (CSS):* mínimo automático = min(alto declarado, alto del contenido).

14. **Pista en porcentaje sobre un tamaño indefinido** (filas con el contenedor en
    `Height: auto`, o al calcular intrínsecos): vale 0. Contradice el §3.1 ("un valor que
    depende de una base indefinida se comporta como `auto`").
    *Dónde:* `layout/grid/TrackSizing`.
    *Propuesta (CSS):* tratarla como `auto`.

15. **Un ítem que ocupa varias pistas y alguna es flexible no aporta nada a su tamaño**
    (simplificación que ya traía `TrackSizing`). Un ítem largo que cruza una columna `1fr`
    puede desbordar su área.
    *Dónde:* `layout/grid/TrackSizing.distributeExcess`.
    *Propuesta (CSS):* repartir su exceso entre las pistas flexibles que ocupa, en proporción a
    su fracción.

21. **Con `Decoration: slice` no se reservan el borde y el relleno inferiores:** si el
    contenido cabe y solo el borde o el relleno inferiores se salen de la página, la caja
    termina igualmente y desborda (se pintan en el margen de la página).
    *Dónde:* `layout/Slice`.
    *Propuesta:* reservarlos también con `slice`, como ya se hace con `clone`
    (`Slice.contentRemaining`). Puede cambiar algún caso golden.

38. **Numeración en dos fases:** en la primera, `{page}` y `{pages}` se miden como `00`
    (reservan dos cifras); en la segunda, solo se rehacen las líneas que los contienen. Los
    saltos de línea y los altos de la primera fase no cambian, así que con 100 páginas o más el
    número puede desbordar su línea.
    *Dónde:* `layout/inline/PageNumbers`, `InlineLayout.number`, `fragment/Paginator`.
    *Propuesta:* si el total tiene más de dos cifras, volver a paginar midiendo con esas cifras
    hasta que el total no cambie.

## Aplazadas al hito de fuentes

29. **Fuentes en PDF:** solo las 14 estándar y los alias que reconoce PDFBox (`Arial`,
    `TimesNewRoman`…); una fuente desconocida es un error al maquetar con `PdfMetrics`, no una
    validación del árbol (el SVG admite cualquiera). La especificación (§4.6) habla también de
    fuentes registradas, que quedan pendientes. Los caracteres fuera de WinAnsi se pintan y se
    miden como `?`.
    *Dónde:* `layout/inline/PdfMetrics`, `paint/PdfCanvas`.

30. **Línea base aproximada:** la de `StackingContext` (línea centrada con el 80 % del cuerpo
    por encima), también en PDF; no se usan el ascendente ni el descendente reales de la fuente.
    *Dónde:* `paint/StackingContext`.
    *Alternativa:* calcularla con las métricas de la fuente, que el hito de fuentes necesita de
    todos modos.
