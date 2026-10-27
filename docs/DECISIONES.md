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

42. ~~**`Font` obligatoria y sin valor por defecto**~~, resuelta tras la conversación sobre
    errores: `Font` y `Font size` son obligatorias en el texto y se pueden declarar en
    `Document` (§4.6), así que ya no hay que repetirlas en cada caja de primer nivel.

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

## Política de errores

Errores con código estable (`ErrorCode`) y la línea del nodo culpable; las combinaciones sin
efecto o contradictorias son errores (§6). El catálogo `src/test/resources/errors/errors.stxt`
tiene un caso por código.

51. **El alto definido se decide al leer el documento**, con una regla fija (§3.6), no al
    maquetar. Es más estricta que el motor y que CSS: un ítem de flex estirado no da un alto
    definido a sus hijos, aunque el motor sí lo conoce al maquetarlos.
    *Dónde:* `tree/BoxTreeBuilder.definiteHeight` y `validatePercentages`.
    *Alternativa:* detectar los porcentajes sin base durante el layout final, distinguiéndolo de
    las pasadas de medida (grid mide sus ítems sin base y luego los maqueta con el alto del área).

52. **Un ítem de grid estirado se maqueta siempre con su alto fijado**, aunque mida lo mismo
    que al medirlo, para que sus hijos tengan la base definida que promete la regla 51. Cuesta
    un layout más por ítem estirado.
    *Dónde:* `layout/grid/GridLayout.finalSpace`.
    *Alternativa:* repetirlo solo si algún descendiente usa porcentajes verticales.

53. **`Margin: auto` horizontal en flujo de bloque necesita `Width` o `Max width`**: con ancho
    `auto` la caja ocupa toda la línea y los márgenes valdrían 0. También son error los
    verticales en flujo de bloque y los de las `absolute` sin inicio, tamaño y fin. Amplían la
    regla acordada (márgenes `auto` en ítems de flex y grid) con el mismo criterio.
    *Dónde:* `tree/BoxTreeBuilder.validateMargins`.

54. **Mínimos y máximos solo se comparan en las mismas unidades** (puntos con puntos,
    porcentaje con porcentaje). Si no se pueden comparar al leer, al maquetar sigue ganando el
    mínimo.
    *Dónde:* `tree/BoxTreeBuilder.validateMinMax`, `layout/Sizing.constrain`.
    *Alternativa:* comparar también al maquetar y dar un error de layout.

55. **Formas de valor por propiedad:** `Width: 1fr`, `Max width: max-content`, `Gap: auto`,
    un `minmax()` fuera de las pistas o un número negativo fuera de `Margin` y `Offset` son
    errores (`VALUE_NOT_ALLOWED`); antes se comportaban como `auto` o se aceptaban. Sigue la
    columna *Valores* del §4.
    *Dónde:* `tree/BoxTreeBuilder.value`, `sides`, `tracks`.

56. **Se para en el primer error.** Acumularlos tiene el problema de los errores en cascada
    (una caja mal definida provoca otros en sus hijos).
    *Alternativa:* devolver la lista de errores independientes.

57. **Líneas de los errores de fuentes:** una familia desconocida señala la línea de su `Font`,
    aunque esté en una antecesora; una variante que falta, la del texto, y el mensaje nombra
    dónde se declaran `Font`, `Font weight` y `Font style`; un carácter que falta, la suya
    dentro del bloque `Text >>`.
    *Dónde:* `layout/inline/PdfMetrics.check`, `tree/Box.textLineAt`.

58. **El hueco entre filas de un flex `row` sin `Wrap` no se comprueba**: no se usa, así que
    `Gap: 5%` en un flex de una línea sin alto definido no es un error.
    *Dónde:* `tree/BoxTreeBuilder.validateContainer`.

59. **Sin código propio lo que ya rechaza el schema** (antes, el template): `Main axis: stretch` y
    `Break inside: page` son `VALUE_NOT_ALLOWED`; un bloque `>>` donde van hijos lo rechaza el
    schema con su código de `stxt-core`, también sin `DocumentReader` (decisión 73).

60. ~~**Solo las propiedades de la fuente en `Document`**~~, resuelta en el hito 10: `Document`
    admite el `Typography` entero, que es todo lo que se hereda.

61. **Los desbordamientos se comprueban sobre el resultado final**, una vez paginado, y no
    durante el layout: así no cuentan las pasadas de medida. Cuentan las líneas y los hijos en
    flujo que no caben en la caja de contenido de su padre (a lo ancho con sus márgenes
    horizontales; a lo alto, el borde inferior), el flujo que no cabe en la página y las
    `absolute` y `fixed` que salen de la página física. No cuentan el desplazamiento de las
    `relative`, los márgenes negativos (espacio que gana el autor) ni el borde superior (los
    márgenes verticales colapsan).
    *Dónde:* `layout/Overflow`, llamado desde `Engine.layOutDocument`.
    *Alternativa:* detectarlo en cada algoritmo al colocar, con el riesgo de confundir medidas
    provisionales con el resultado.

62. **El culpable de un desbordamiento:** se baja hasta lo más interior que se sale. A lo ancho
    se señala su `Width` (o `Min width`, o la caja); si es una palabra, la línea del documento
    de esa palabra (la primera vez que aparece en el texto de la caja). A lo alto, el `Height`
    (o `Max height`) del contenedor que se queda corto. Si no cabe en la página, el `Height` o
    el `Line height` de lo más interior que pasa del final.
    *Dónde:* `layout/Overflow.tooWide`, `tooTall`, `pageOverflow`, `textOverflow`.

## Hito 9: elementos en lugar de `Box` (2026-10-27)

El documento deja de ser un árbol de `Box` con `Layout` y `Position` y pasa a escribirse con
elementos (`Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute`, `Fixed`) y los envoltorios
`Item` y `Cell`, validados con un `@stxt.schema` en lugar del `@stxt.template` (§2, §9). Se
sustituye el formato anterior por completo. El motor no cambia: `BoxTreeBuilder` traduce cada
elemento a una `Box` con su `Layout` y su `Position`. Desaparecen los códigos que pasan a ser
estructura del schema (`INLINE_WITH_BOXES`, `CONTAINER_MISMATCH`, `ITEM_MISMATCH`,
`OFFSET_ON_STATIC`, `LAYER_WITHOUT_EFFECT`, `BREAK_NOT_ALLOWED`) y las cajas anónimas, y se
añaden `SINGLE_ELEMENT` y `TEXT_CONTENT`. Los casos golden no cambian su `expected.stxt`; sus
imágenes solo ganan el contorno del `Text` que ahora va dentro de la cabecera, el pie y el sello.

63. **`Item` y `Cell` son envoltorios, no cajas**, con exactamente un elemento en flujo dentro
    (`SINGLE_ELEMENT`, porque el schema solo puede limitar cada tipo de elemento a uno). Las
    dimensiones, márgenes e `Id` van en el elemento; el envoltorio solo lleva lo que depende del
    contenedor. Así cada ítem sigue siendo una única `Box`, como ahora.
    *Dónde:* schema; `tree/BoxTreeBuilder.buildContent` e `item`.
    *Alternativa:* que `Cell` e `Item` sean cajas de bloque con varios hijos; añadiría una caja
    por ítem, y un flex dentro de una celda ya no se estiraría al alto del área.

64. **Todo hijo en flujo de un `Flex` es un `Item`, y de un `Grid`, un `Cell`**, aunque no
    declare nada (`Cell:` sin valor se coloca automáticamente). La estructura es siempre la
    misma y el schema puede comprobarla.
    *Alternativa:* admitir elementos directamente en `Flex` y `Grid` y el envoltorio solo
    cuando hace falta; menos líneas, pero dos formas de escribir lo mismo.

65. **`Relative` es un elemento, no una propiedad**, a diferencia de lo hablado (desplazamiento
    `relative` como propiedad de cualquier elemento). Como elemento, `Offset` y `Layer` solo
    existen en los elementos posicionados y en `Item`/`Cell`, y el schema detecta su mal uso
    sin reglas propias; un `Relative` sin `Offset` marca el containing block de los `Absolute`.
    *Alternativa:* una propiedad (`Offset` en cualquier elemento en flujo); haría falta otra
    forma de marcar el containing block sin desplazar y mantener `LAYER_WITHOUT_EFFECT`.

66. **Los elementos posicionados son bloques.** Un flex o un grid posicionado se escribe
    dentro (`Absolute` > `Flex`), con una caja más que antes.
    *Alternativa:* que `Relative`, `Absolute` y `Fixed` sean envoltorios de un elemento, como
    `Cell`; se leería peor en el caso habitual (un sello, una cabecera).

67. **`Absolute` y `Fixed` no admiten el grupo `Break`** (`Before`, `After`, `Inside` y
    `Decoration`), porque no se parten ni están en el flujo de la página. `Widows` y `Orphans`
    van en `Typography` desde el hito 10 y se admiten, porque se heredan. Antes solo era un
    error el corte forzado (`BREAK_NOT_ALLOWED`); `avoid` y `Decoration` se aceptaban sin
    efecto. Sus descendientes sí admiten `Break` (el schema no distingue dónde está un
    `Block`), aunque tampoco se parten.
    *Dónde:* schema.

68. **El texto de un `Text` va en el valor o en `Content >>`**, uno de los dos
    (`TEXT_CONTENT`). `Content` es `BLOCK`, así que `Content: algo` es un error del schema: el
    texto corto va en el valor. Un `Text` vacío es un error.
    *Alternativa:* `Content` de tipo `TEXT` (en línea o en bloque); habría dos formas de
    escribir el texto corto.

69. **`Isolate` solo en `Block` y `Relative`**: en los demás no tendría efecto (`Absolute`,
    `Fixed` y los ítems ya crean un contexto de formato de bloque; `Flex`, `Grid` y `Text` no
    tienen hijos en flujo de bloque). Un `Block` que es ítem lo admite aunque no tenga efecto,
    porque el schema no sabe si está en un `Item`.
    *Dónde:* schema.

70. **Un `Document` sin elementos es válido** (una página vacía). Antes el template exigía al
    menos una `Box`, y el schema no puede exigir "al menos uno" entre varios tipos.
    *Alternativa:* una regla del builder con su código.

71. **`Fixed` se admite en cualquier contenedor**, como antes cualquier `Box` podía ser
    `fixed`; su posición estática es la de su sitio en el documento.
    *Alternativa:* solo como hijo de `Document`, que es donde tiene sentido una cabecera o un
    pie.

72. ~~**El schema no tiene grupos de hijos**~~, resuelta en el hito 10 con los grupos `Box`,
    `Typography` y `Break`: cada elemento declara tres grupos en lugar de unas 30 propiedades.

73. **`BoxTreeBuilder` valida contra el schema al empezar**, aunque el documento venga de
    `DocumentReader`: la estructura solo está definida en el schema y el builder la da por
    buena. Un árbol construido sin validar (`STXT.rawParser()`) da los mismos errores que el
    lector, con los códigos de `stxt-core`, en lugar de interpretar mal una propiedad fuera de
    su sitio. Sustituye a una copia de la tabla de hijos en el builder (`CHILDREN`) y al código
    `DOCUMENT_STRUCTURE`, que desaparecen. Cuesta validar dos veces lo que viene del lector.
    *Dónde:* `tree/DocumentSchema`, `tree/BoxTreeBuilder.build`.
    *Alternativa:* generar la tabla del builder a partir del schema, o no comprobar nada sin
    validar.

## Hito 10: grupos `Box`, `Typography` y `Break` (2026-10-27)

Las propiedades de cada caja pasan a tres grupos, hijos del elemento (§4): `Box` (modelo de
caja), `Typography` (texto) y `Break` (cortes de página). El schema baja de 749 a 512 líneas, la
mayoría descripciones y valores de los ENUM. El motor no cambia: `BoxTreeBuilder` guarda cada
propiedad con su nombre interno de siempre (`font-size`, `break-inside`), así que `Style.line`,
`Overflow` y `PdfMetrics` no cambian.

74. **Lo que se hereda va en `Typography` y lo que no, en `Box` o `Break`.** Por eso
    `Background` y `Border color` van en `Box` (no son geometría, pero no se heredan) y
    `Widows` y `Orphans` en `Typography` (son de los párrafos y se heredan), no en `Break`.
    *Alternativa:* un cuarto grupo de pintado (`Background`, `Border color`), o `Widows` y
    `Orphans` en `Break`; en los dos casos un grupo mezclaría heredables y no heredables.

75. **Nombres cortos dentro de los grupos** (`Size`, `Weight`, `Style`, `Align`, `Before`,
    `After`, `Inside`) salvo donde chocan con otro nodo del namespace: `Text wrap` no puede ser
    `Wrap`, que es el booleano de `Flex`. Los de `Box` ya eran cortos y se conservan.
    *Dónde:* schema; `tree/BoxTreeBuilder.INTERNAL` traduce los cortos al nombre interno.
    *Alternativa:* renombrar `Wrap` de `Flex` (por ejemplo, `Multiline`) para poder usar `Wrap`
    en `Typography`.

76. **Fuera de los grupos queda lo que define al elemento**: `Id`, `Isolate`, `Offset`,
    `Layer`, las propiedades de `Flex` y `Grid` y las de `Item` y `Cell`. Un grupo
    `Position` (`Offset`, `Layer`) se descartó porque solo existe en los elementos
    posicionados, que ya lo dicen con su nombre.

77. **Los errores señalan la línea de la propiedad**, dentro del grupo, nunca la del grupo.
