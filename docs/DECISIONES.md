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
- **6 y 7:** el mínimo automático de un ítem flex es el tamaño de su contenido, nunca mayor
  que su `Width` (o `Height`) declarado, como en CSS (§5.7). Los algoritmos dan ahora los
  intrínsecos del contenido (`contentIntrinsic`), y `ConstraintSpace.measuringContentHeight()`
  mide el alto del contenido sin el `Height` de la caja.
- **15:** lo que le falta a un ítem que cruza alguna fracción se reparte entre las fracciones
  que ocupa, en proporción a su `fr`, como en CSS (§5.8; `TrackSizing.distributeToFlexible`).
  Solo crecen las fracciones con mínimo intrínseco, y se usa el min-content del ítem.

## Hito 8: fuentes

Resueltas por el hito:

- **29:** solo se usan las fuentes TTF de `resources/fonts`, incrustadas en el PDF; ya no hay
  14 fuentes estándar ni sustitución por `?` (§5.11).
- **30:** la línea base sale del ascendente y el descendente reales de la fuente (§5.4).

Decisiones nuevas:

42. **`Font` obligatoria y sin valor por defecto:** el texto sin `Font` (propia o heredada) es
    un error del modelo, también con `FixedMetrics`. Como solo se hereda de antecesoras y las
    cajas de primer nivel no tienen ninguna declarada, cada caja de primer nivel con texto
    declara la suya (el ejemplo del §10 la repite cinco veces).
    *Dónde:* `tree/BoxTreeBuilder.buildContent`.
    *Alternativa:* admitir propiedades de texto heredables en `Document` para todo el
    documento. Pendiente de la conversación sobre la política de errores.

43. **Un carácter que la fuente no tiene es un error**, con la línea de la caja; no se pinta el
    glifo `.notdef` ni se busca otra fuente. Los espacios en blanco se comprueban como espacio y
    `{page}`/`{pages}` como las diez cifras.
    *Dónde:* `layout/inline/PdfMetrics.validate`.
    *Alternativa:* pintar `.notdef` (el ancho ya se mide así) o una lista de fuentes de
    sustitución.

44. **Las comprobaciones de fuentes solo se hacen con las métricas reales.** Con
    `FixedMetrics` (tests de layout, SVG) no se mira la carpeta de fuentes: basta con que
    `Font` esté declarada. Los errores de la carpeta (no existe, fichero ilegible, fuente
    variable, duplicados) son `ResourceException` sin línea, porque no son del documento.
    *Dónde:* `TextMeasurer.validate`, llamado desde `Engine.layOutDocument`.
    *Alternativa:* validar siempre las fuentes al construir el árbol, lo que obligaría a tener
    la carpeta también para maquetar sin PDF.

45. **Pesos 400 y 700 solamente:** `normal` es el fichero con `usWeightClass` 400 y `bold`, el
    de 700. Los ficheros de otros pesos se ignoran sin error.
    *Dónde:* `resources/FontLibrary.read`.
    *Alternativa:* `Font weight` numérico (100 a 900), o tratar como error un fichero que no
    se puede usar.

46. **Familia exacta:** `Font` se compara con mayúsculas y espacios; si solo difiere en
    mayúsculas, el error lo sugiere (`¿Querías decir 'IBM Plex Sans'?`). La familia es la
    tipográfica (`nameID` 16) y, si falta, la 1.
    *Dónde:* `resources/FontLibrary.find`.
    *Alternativa:* comparar sin distinguir mayúsculas.

47. **Línea base con `hhea`:** ascendente y descendente de la tabla `hhea`. No se usan los de
    `OS/2` (`sTypo*`, ni cuando la fuente marca `USE_TYPO_METRICS`; ni `usWin*`). En IBM Plex
    Sans, `hhea` da 1025 y 275 por 1000, igual que `usWin*`; `sTypo*` da 780 y 220.
    *Dónde:* `resources/FontFile`, `model/VerticalMetrics`.
    *Alternativa:* `sTypo*` cuando la fuente marca `USE_TYPO_METRICS`.

48. **Anchos como los del PDF, sin kerning ni ligaduras:** el avance de cada glifo se redondea
    a milésimas del cuerpo, que es lo que escribe PDFBox en el array `W` de la fuente
    incrustada; así maquetar y pintar miden lo mismo con fuentes de cualquier `unitsPerEm`.
    *Dónde:* `resources/FontFile.width`.
    *Alternativa:* aplicar el kerning de `kern`/`GPOS`, que PDFBox no aplica al pintar: habría
    que posicionar cada glifo con `TJ`.

49. **Justificación con `TJ`:** con la codificación Identity-H de las fuentes incrustadas cada
    carácter ocupa dos bytes y el operador `Tw` de PDF no tiene efecto, así que el espacio extra
    se añade tras cada espacio en un `TJ`. En SVG sigue siendo `word-spacing`.
    *Dónde:* `paint/PdfCanvas.justified`.

50. **Enlaces simbólicos dentro de la carpeta:** se admiten si su destino, una vez resuelto,
    está dentro de la carpeta de recursos; si apunta fuera es un error, también al explorar
    `fonts/`.
    *Dónde:* `resources/Resources.checkInside`.
    *Alternativa:* rechazar cualquier enlace.
