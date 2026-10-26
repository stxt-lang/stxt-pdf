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
