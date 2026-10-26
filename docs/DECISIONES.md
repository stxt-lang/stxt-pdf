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

Resueltas en el código después del repaso:

- **2:** las posicionadas se pintan en orden del documento (§5.6).
- **14:** una pista en porcentaje sobre un tamaño indefinido es `auto` (§5.8).
- **41** (encontrada al resolver la 14): los porcentajes de las pistas se resolvían contra la
  caja de contenido menos los huecos; ahora, contra la caja de contenido, como dice el §3.6.
- **21:** con `slice`, si solo el borde y el relleno inferiores no caben, `Engine.layout` repite
  el layout reservándolos (§5.10). No se reservan siempre, como con `clone`, para no dejar un
  hueco al final de cada página cuando la caja se corta de todos modos.
- **38:** con 100 páginas o más, el `Paginator` vuelve a paginar reservando las cifras del total
  (§5.10).

## Cambiar el código

Pendientes de implementar, una a una. Al resolver cada una, actualiza la especificación y
quítala de aquí.

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

15. **Un ítem que ocupa varias pistas y alguna es flexible no aporta nada a su tamaño**
    (simplificación que ya traía `TrackSizing`). Un ítem largo que cruza una columna `1fr`
    puede desbordar su área.
    *Dónde:* `layout/grid/TrackSizing.distributeExcess`.
    *Propuesta (CSS):* repartir su exceso entre las pistas flexibles que ocupa, en proporción a
    su fracción.

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
