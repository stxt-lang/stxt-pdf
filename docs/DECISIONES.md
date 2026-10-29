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
efecto o contradictorias son errores (§6). El catálogo `test/errors/`
tiene un caso por código.

51. **El alto definido se decide al leer el documento**, con una regla fija (§3.6), no al
    maquetar. Es más estricta que el motor y que CSS: un ítem de flex estirado no da un alto
    definido a sus hijos, aunque el motor sí lo conoce al maquetarlos.
    *Dónde:* `tree/ModelValidator.definiteHeight` y `validatePercentages`.
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
    *Dónde:* `tree/ModelValidator.validateMargins`.

54. **Mínimos y máximos solo se comparan en las mismas unidades** (puntos con puntos,
    porcentaje con porcentaje). Si no se pueden comparar al leer, al maquetar sigue ganando el
    mínimo.
    *Dónde:* `tree/ModelValidator.validateMinMax`, `layout/Sizing.constrain`.
    *Alternativa:* comparar también al maquetar y dar un error de layout.

55. **Formas de valor por propiedad:** `Width: 1fr`, `Max width: max-content`, `Gap: auto`,
    un `minmax()` fuera de las pistas o un número negativo fuera de `Margin` y `Offset` son
    errores (`VALUE_NOT_ALLOWED`); antes se comportaban como `auto` o se aceptaban. Sigue la
    columna *Valores* del §4.
    *Dónde:* `tree/ValueReader.value`, `sides`, `tracks`.

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
    *Dónde:* `tree/ModelValidator.validateContainer`.

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
    *Dónde:* schema; `tree/BoxTreeBuilder.buildContent` y `PropertyReader.item`.
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
    *Dónde:* schema; `tree/PropertyKey.internal` traduce los cortos al nombre interno.
    *Alternativa:* renombrar `Wrap` de `Flex` (por ejemplo, `Multiline`) para poder usar `Wrap`
    en `Typography`.

76. **Fuera de los grupos queda lo que define al elemento**: `Id`, `Isolate`, `Offset`,
    `Layer`, las propiedades de `Flex` y `Grid` y las de `Item` y `Cell`. Un grupo
    `Position` (`Offset`, `Layer`) se descartó porque solo existe en los elementos
    posicionados, que ya lo dicen con su nombre.

77. **Los errores señalan la línea de la propiedad**, dentro del grupo, nunca la del grupo.

## Solo modo paginado (2026-10-27)

78. **Se elimina el modo continuo y, con él, `Type` de `Media`.** El medio es siempre una
    secuencia de páginas: `Width` y `Height` son obligatorios (lo exige el schema, con
    `TOO_FEW_CHILDREN`) y el área útil tiene siempre alto definido, así que un porcentaje
    vertical en una caja de primer nivel o en un `Relative` de primer nivel es válido.
    Desaparecen `MEDIA_HEIGHT_REQUIRED` y `MEDIA_HEIGHT_CONTINUOUS`. El `ConstraintSpace` sigue
    admitiendo `fragmentainerRemaining` vacío para las cajas que no se parten (ítems de flex,
    pasadas de medida). *Dónde:* schema, `tree/Media`, `tree/BoxTreeBuilder`,
    `fragment/Paginator`, `layout/Overflow`, `paint/SvgCanvas`, `paint/PdfCanvas`.
    *Alternativa:* conservar el modo continuo para pantalla, a costa de un caso especial en el
    paginador, en el desbordamiento y en el alto definido del área útil.

## Hito 11: reglas reutilizables (2026-10-27)

79. **Prioridad fija en lugar de especificidad.** De más a menos fuerte: el elemento,
    `For id`, `For class` (entre clases, la que va después en `Styles`), `Element`, la
    herencia y el valor por defecto. Una regla gana a la herencia, como en CSS cualquier
    regla que se aplica al elemento gana a lo heredado. *Dónde:* `StyleRules.apply`.
    *Alternativa:* especificidad calculada y selectores compuestos, como CSS; más potente,
    pero menos predecible y sin necesidad todavía.

80. **Lo que aporta una regla se valida como si lo declarara el elemento.** Una regla por
    tipo puede dar un error en un elemento concreto (un `Margin: auto` en un ítem de flex),
    con la línea de la regla. Los valores de las reglas se comprueban aunque no se apliquen.
    *Alternativa:* ignorar las reglas sin efecto cuando vienen de una regla; habría que saber
    de dónde viene cada valor en las 11 comprobaciones que usan `Style.isDeclared`.

81. **Reglas sin aplicar.** Un `Element`, un `For class` o un `For id` sin elementos se permiten
    (las reglas se reutilizan; el `For id` dejó de ser error, ver 228); una clase de `Class` sin su
    regla y dos reglas con el mismo selector son errores. *Alternativa:* tratarlos todos igual
    (todos error, o ninguno).

82. **Las reglas solo admiten `Box`, `Typography` y `Break`**, no las propiedades propias de
    un elemento (`Gap`, `Columns`, `Offset`…), que el schema no podría validar porque el tipo
    va en el valor de `Element`. *Alternativa:* admitirlas y validarlas en el builder.

## Hito 12: encabezados y párrafos (2026-10-27)

83. **El motor evita siempre el corte dentro y después de un encabezado**, en lugar de dejarlo
    al autor con `Break`. Son `avoid` normales (se relajan si no hay otro corte), y el
    encabezado y sus `Text` no admiten `Break`. *Dónde:* `BoxTreeBuilder.element`.
    *Alternativa:* como CSS, un valor por defecto (`break-after: avoid`) que el autor puede
    cambiar; contradice que no haya estilos implícitos (T1).

84. **Los saltos de nivel son un error** (`HEADING_LEVEL`), y el primer encabezado tiene que
    ser `Heading 1`. Se comprueba en orden del documento, en todo el árbol. *Dónde:*
    `BoxTreeBuilder.heading`. *Alternativa:* permitirlos y colgar el marcador del último de
    nivel menor, como hacen los navegadores.

85. **Un encabezado es un bloque con hijos `Text`**, no un `Text` con otro nombre; su texto en
    el índice es el de sus `Text` unido por espacios. Deja sitio a los tramos con formato (T4)
    sin cambiar el elemento. *Alternativa:* el texto en el valor o en `Content`, como `Text`.

86. **`Paragraph` es un solo párrafo**: una línea en blanco en su `Content` es un error
    (`PARAGRAPH_BLANK_LINE`), aunque en un `Text` separa párrafos. *Alternativa:* permitirlo,
    con un `P` en el PDF que contiene varios párrafos.

87. **(Resuelta después en la misma sesión: `Title` y `Language` opcionales en `Document`,
    que van a la información del documento, a XMP `dc:title`, a `DisplayDocTitle` y al `Lang`
    del catálogo; sin ellos, el PDF no los declara. No se declara conformidad PDF/UA.)**
    **PDF etiquetado sin idioma.** El árbol de estructura tiene un elemento por caja
    (`Document`, `H1`…`H6`, `P`, `Span`, `Div`) y los fondos, bordes y `Fixed` son
    *artifacts*, pero el documento no declara `Lang` (el modelo no tiene dónde) ni título, que
    PDF/UA exige. *Dónde:* `paint/PdfStructure`. *Alternativa:* una propiedad de idioma (y de
    título) en `Document`; se puede añadir sin cambiar el resto.

88. **Un encabezado dentro de un `Fixed` es un error** (`HEADING_IN_FIXED`): el `Fixed` se
    repite en cada página y es *artifact*, así que no tendría marcador ni etiqueta.
    *Alternativa:* permitirlo como un bloque sin significado.

## Hito 13: listas (2026-10-27)

89. **Un único elemento `List` con `Marker` obligatorio**, en lugar de `ul`/`ol` o de un valor
    por defecto. `Marker`, `Start` y `Number` son propiedades del elemento: no se heredan ni
    van en `Styles` (R6), así que cada lista anidada declara su marcador. *Dónde:* schema,
    `BoxTreeBuilder.element`. *Alternativa:* `Marker` en `Typography` (heredable y en las
    reglas), como `list-style-type`.

90. **El marcador es texto en la fuente del ítem**, sin `circle` ni `square`, porque IBM Plex
    Sans no tiene `◦` ni `▪` y no hay fuente de sustitución. *Dónde:* `tree/ListMarkers`.
    *Alternativa:* dibujar `disc`, `circle` y `square` como formas, con un primitivo nuevo en
    `Canvas`; el marcador dejaría de depender de la fuente.

91. **La sangría es el `Padding` izquierdo de la `List`** y el espacio que separa el marcador
    del contenido es el espacio final de su texto (`"1. "`), como en CSS; sin `Indent` ni
    `Gap`. Un marcador más ancho que la sangría es un error (`LIST_MARKER_OVERFLOW`, en
    `Overflow`), no un desbordamiento tolerado. *Alternativa:* una propiedad `Indent` con un
    ancho de marcador fijo.

92. **La primera línea se busca bajando por el primer hijo en flujo** de cada bloque hasta un
    texto, y si no se llega (un `Flex`, un `Grid` o un bloque vacío al principio) es un error
    (`LIST_ITEM_WITHOUT_LINE`), comprobado ya al construir el árbol. CSS mira también la línea
    base de flex y grid y salta los bloques vacíos. *Dónde:* `ListBuilder.markerLine`,
    `layout/block/MarkerLayout`. *Alternativa:* exportar la línea base de flex y grid, o alinear
    el marcador con el borde superior del ítem.

93. **La línea base se calcula al maquetar con `TextMeasurer.vertical`**, no al pintar: el
    marcador es un fragmento normal (con su `Rect` en el volcado). Con `FixedMetrics` es la
    aproximación 0,8/0,2 y con `PdfMetrics` la `hhea` de la fuente, la misma del PDF; el SVG
    pinta siempre con la aproximación, así que un marcador de otro tamaño que su primera
    línea puede quedar ligeramente desplazado en el SVG de un documento maquetado con las
    métricas reales. *Alternativa:* alinear en el pintado, con las métricas del lienzo.

94. **El marcador va en el primer fragmento del ítem** y nunca se repite; no se busca la
    primera línea en los fragmentos siguientes, porque un bloque que no coloca su primera línea
    devuelve `BEFORE` y no deja un fragmento sin ella. *Dónde:* `BlockLayout.withMarker`.

95. **`dash` se etiqueta con `ListNumbering: None`**, porque el PDF no tiene un valor para el
    guion; la lista sigue siendo `L` con `Lbl` y `LBody`. *Dónde:* `paint/PdfStructure`.
    *Alternativa:* `Unordered` (PDF 2.0), si se declara la versión 2.0.

## Hito 14: tablas (2026-10-27)

96. **El envoltorio de los ítems de `Grid` pasa a llamarse `Area`** (antes `Cell`, decisiones
    63 y 64), para dejar `Cell` a la celda de tabla, que sí es una caja y se puede nombrar en
    `Element`. `Area` es el término de CSS (`grid-area`) y admite los mismos valores que tenía
    `Cell` (`Area: side`, `Area: 1 1 1 2`); el nombre casa con `Areas`, cuyas áreas nombra. No
    cambia el layout ni ningún resultado. La clase interna `layout/grid/Cell` (la colocación
    leída) conserva su nombre. *Dónde:* schema, `BoxTreeBuilder` (`Style.itemArea`, línea
    `item.area`). *Alternativa:* conservar `Cell` en `Grid` y llamar `Table cell` a la de la
    tabla, como `List item`; más largo en lo que más se repite de una tabla, y con dos
    "celdas" distintas en el namespace.

97. **El `Break` de una `Row` es el de sus celdas**, porque la fila no es una caja: se copia a
    cada celda (con la línea del `Break` de la fila) y el corte entre filas sale de las reglas
    de corte del grid (`BreakRules.between`). Su `Typography` se aplica como herencia, antes
    de las reglas, así que `Element: Cell` le gana. *Dónde:* `BoxTreeBuilder.element` y
    `TableBuilder` (las celdas). *Alternativa:* que la `Row` sea una caja con su propio `Box` (fondo de fila),
    pintada detrás de sus celdas.

98. **Los márgenes de una `Cell` son los de un ítem de grid**: separan la caja de la celda de
    su área, como en `Grid`. HTML los ignora en las celdas. *Dónde:* `GridLayout`, sin código
    propio. *Alternativa:* error si una celda declara `Margin`.

99. **`Span: 1 1` explícito se admite**, aunque no tenga efecto, como `Start: 1` en una lista:
    es el valor por defecto escrito, no una contradicción. *Dónde:* `TableBuilder.span`.
    *Alternativa:* error de valor sin efecto.

100. **El error de cabecera y pie (`TABLE_REPEAT_OVERFLOW`) es solo cuando no dejan sitio al
     cuerpo** al principio de una página, no cuando no caben con una fila entera del cuerpo
     (como decía TB9): una fila más alta que el sitio que dejan se parte en paralelo, como
     decide TB7 para cualquier fila, y si no se puede partir es un `PAGE_OVERFLOW`. Así una
     celda con un párrafo largo en una tabla con cabecera no es un error. Se detecta al
     maquetar, no en `Overflow`, porque sin sitio la tabla no avanza. *Dónde:*
     `layout/table/TableLayout`. *Alternativa:* el error de TB9 al pie de la letra, con la
     primera fila del cuerpo de cada página entera.

101. **El pie va detrás de la última fila del cuerpo de cada página**, no pegado al final del
     área útil: el fragmento de la tabla mide lo que su contenido. *Dónde:* `UnitBreaker.Repeat`.
     *Alternativa:* estirar el último fragmento de cada página hasta el final del área útil.

102. **Las cajas `Absolute` y `Fixed` de las celdas que se repiten solo van en el primer
     fragmento de la tabla**, como las del propio contenedor; en los demás, la cabecera y el
     pie se repiten sin ellas. *Dónde:* `UnitBreaker.Run.repeated`. *Alternativa:* repetirlas
     con su fila.

103. **La cabecera y el pie repetidos son *artifacts*** salvo en la primera página en que sale
     cada celda: el árbol de estructura tiene una sola `THead` y una sola `TFoot`, y un lector
     de pantalla no lee la cabecera en cada página. Las `TH` llevan `Scope: Column`, que TB10 no
     decía. *Dónde:* `paint/PdfStructure` (`repeated`, `mark`). *Alternativa:* marcar cada
     repetición como contenido de la misma celda.

## Calidad y arquitectura (2026-10-27)

104. **La geometría usada solo existe en los fragmentos**: `Box` ya no guarda `x`, `y`,
     `width`, `height`, `margin`, `border` ni `padding` "del último layout". Una caja se maqueta
     muchas veces (sondas, medidas, estirado, cada página) y la caché devuelve resultados sin
     volver a escribirla, así que ese estado no correspondía a ningún fragmento. El margen usado,
     que `Overflow` necesita para el desbordamiento horizontal, va en `Fragment.margin`, con la
     base de porcentajes del layout que produjo el fragmento (el área en grid, la caja de
     contenido del padre en bloque y flex). *Dónde:* `fragment/Fragment`, `layout/Overflow`,
     `tree/Box`. *Alternativa:* que `Overflow` resuelva los márgenes con el ancho de contenido
     del padre, que no es la base de los ítems de grid.

105. **`Overflow` comprueba un `Relative` en su sitio con el desplazamiento que usó el layout**,
     guardado en `Fragment.offsetX`/`offsetY`, en lugar de volver a calcularlo. Lo recalculaba
     con el alto del fragmento como base de los porcentajes verticales, y en un padre partido
     entre páginas esa base no es su `Height` entero: rechazaba documentos válidos (un
     `Offset: 10%` dentro de un bloque de 1500 pt que se parte). *Dónde:* `fragment/Fragment`,
     `layout/UnitBreaker.Placed`, `layout/Overflow`. *Alternativa:* que `Overflow` resuelva el
     porcentaje con el `Height` del padre, que necesita las bases de todos sus antepasados.

106. **Una caja que declara su ancho (`Width` o `Min width`) es la culpable de sus hijos que
     solo la llenan** (§5.12): al buscar el culpable más interior de un desbordamiento
     horizontal, `Overflow` solo baja a un hijo de una caja así si ese hijo se sale también de
     ella. Antes bajaba a cualquier hijo que pasara del contenedor original, y señalaba un
     `Block` con ancho `auto` (o el `Text` de un `List item` con `Width`) en lugar del `Width`
     que lo causaba. En una caja de ancho `auto` sigue bajando, porque su ancho sale de su
     contenido. *Dónde:* `layout/Overflow.tooWide`. *Alternativa:* comparar siempre con la caja
     del padre inmediato, que en una caja `auto` señala la caja y no lo que la ensancha.

107. **Los desbordamientos verticales señalan la propiedad que fija el alto** (§5.12), no solo
     `Height`, `Max height` y `Line height`. En `BOX_OVERFLOW`, si la caja no declara `Height`
     ni `Max height`: su `Aspect ratio` o, en un grid, sus `Rows` (unas filas `auto` crecerían
     con su contenido). En `PAGE_OVERFLOW`, por este orden: el `Padding` o el `Border` si el
     relleno y el borde del fragmento ya no caben en la página (antes se señalaba el `Height`,
     que sí se puede partir); el `Line height` o, sin él, el `Size` de una línea de texto; el
     `Height`. Para eso `Line height` se hereda con su línea, como las propiedades de la fuente
     (`Style.LINED_PROPERTIES`). *Dónde:* `layout/Overflow` (`tooTall`, `pageOverflow`),
     `tree/Style`. *Límite:* una caja cuyo alto impone el padre (un ítem estirado a una fila
     fija) se señala a sí misma, no el `Rows` del grid; buscarlo exige subir por los padres.
     *Alternativa:* la línea de la caja, como antes.

108. **Los packages dependen en un solo sentido**, y lo comprueba `ArchitectureTest`: `model`
     no depende de nada; `tree`, solo de `model`; y no hay ciclos. Por eso la colocación de
     grid (`Area`, `Cell`, `GridAreas`), `FontFace` y sus enums (`FontWeight`, `FontStyle`)
     están en `model`; las calidades de corte, en `fragment/BreakAppeal`; y el `Paginator`, en
     `layout`. `Style` guarda `Areas` y la colocación de cada ítem ya interpretados: el valor de
     un `Area` se interpreta al leer el envoltorio, con las `Areas` del grid padre, y no al
     validar la caja; la tabla da a cada celda su `Cell` directamente. Un documento con dos
     errores puede dar ahora primero el de `Area` (`GRID_PLACEMENT`), que se encuentra antes que
     los de las propiedades del elemento. *Dónde:* `model`, `tree/PropertyReader.item`,
     `tree/TableBuilder`, `layout/grid/GridLayout`, `paint/PdfStructure`. *Alternativa:*
     guardar el texto y volver a interpretarlo en cada layout, como antes.

109. **Un ítem con la fila fija y sin hueco en ella es un error** (§5.8, decidido con el usuario):
     `GRID_PLACEMENT` con la línea del `Area`. Antes la búsqueda de columna no tenía límite y el
     motor acababa en `ArrayIndexOutOfBoundsException` tras dos mil millones de vueltas. CSS
     crearía columnas implícitas, pero aquí la rejilla tendría que crecer a mitad de la
     colocación, con los automáticos anteriores ya colocados con menos columnas. Además, los
     ítems con solo la fila o solo la columna fija se colocan en orden del árbol con los
     automáticos (el paso 3 del §5.8), no antes como en CSS, y el de fila fija toma la primera
     columna libre, no la siguiente a los anteriores de esa fila. *Dónde:* `layout/grid/Placement`,
     `layout/grid/GridLayout.noRoom`. *Alternativa:* la columna implícita de CSS.

## Hito 15: bloque en columnas (2026-10-27)

110. **El contenido de un `Multicol` va en una caja generada, el flujo de columnas**
     (`Element.COLUMNS`), su único hijo: cada columna es un fragmento de esa caja, maquetado
     con los algoritmos de siempre. Crea un contexto de formato de bloque (los márgenes no
     salen de las columnas, como en CSS). *Dónde:* `tree/BoxTreeBuilder.columns`,
     `layout/multicol/MulticolLayout`, `paint/PdfStructure` (que la salta). *Alternativa:* que
     `MulticolLayout` apile los hijos él mismo, repitiendo el flujo de bloque.

111. **El alto de las columnas no es definido**: un porcentaje vertical dentro de un
     `Multicol` es `INDEFINITE_PERCENTAGE`, aunque el `Multicol` tenga `Height`. El alto de
     cada columna sale del reparto, no se conoce al leer. *Dónde:* `tree/BoxTreeBuilder.columns`.
     *Alternativa:* tomar como base el alto de contenido del `Multicol` cuando es definido,
     como CSS.

112. **`Column width` y `Column gap` solo en puntos**, y `Column gap` vale 0 por defecto (en
     CSS, `1em` y admite porcentajes). Como el resto del modelo, sin unidades relativas a la
     fuente. *Dónde:* `tree/PropertyReader.multicol`. *Alternativa:* porcentajes del ancho de
     contenido en `Column gap`.

113. **El equilibrado no incumple reglas que el reparto en orden respeta.** Se busca el alto
     mínimo (búsqueda binaria con precisión de 0,001 pt, que no cambia los cortes) en que el
     contenido termina en las columnas sin pasar del alto y sin cortes entre columnas peores
     que los del reparto en orden (perfectos, si no tiene cortes): un bloque con
     `Inside: avoid` no se parte y un párrafo de tres líneas con viudas y huérfanas de 2 no se
     reparte. Solo se equilibra el último fragmento, como `column-fill: balance`; no hay
     `balance-all`. *Dónde:* `MulticolLayout.Run.balance`. *Alternativa:* el equilibrado de
     los navegadores, que minimiza la falta de espacio y puede incumplir reglas.

114. **Sin página ni alto que llenar, las columnas se equilibran siempre**, también con
     `Fill: auto`: un `Multicol` que es un ítem de flex o está en un `Absolute` no tiene nada
     que llenar en orden. No es un error porque no se sabe al leer: el mismo ítem se parte en
     paralelo si no cabe en una página. *Dónde:* `MulticolLayout.Run.layOut`. *Alternativa:*
     llenar en orden con un alto infinito, que dejaría todo en la primera columna.

115. **El contenido que no cabe en las columnas y no puede seguir en otra página es
     `BOX_OVERFLOW`, detectado al maquetar**: cuando el `Height` (o `Max height`) del
     `Multicol` termina en esta página, en la línea de esa propiedad, o cuando, sin página ni
     alto, hay más cortes de columna que columnas, en la línea de `Count`. *Dónde:*
     `MulticolLayout.Run.tooTall` y `tooManyColumnBreaks`. *Alternativa:* las columnas de
     desbordamiento de CSS, a un lado del contenedor; o una última columna sin límite y que lo
     detecte `Overflow`.

116. **Un corte forzado de página tiene una calidad propia, `BreakAppeal.FORCED_PAGE`**, por
     encima de `PERFECT_BREAK`, para que atraviese las columnas: el `Multicol` deja de
     llenarlas y pasa el corte a su padre. El de columna es un corte perfecto. Si una caja
     fuerza los dos, gana el de página; de un `Multicol`, el `Before` y el `After` de sus
     hijos de los extremos solo salen si son de página. Límite: en los ítems de flex y grid
     partidos en paralelo, el corte del grupo es el peor de los ítems, así que un corte de
     página dentro de uno de ellos actúa como de columna. *Dónde:* `fragment/BreakRules`
     (`forcedBetween` devuelve el tipo), `fragment/Unit`, `layout/block/BlockLayout`.
     *Alternativa:* un campo en `LayoutResult` que diga si el corte es de página.

117. **Las columnas de una página empiezan donde el `Multicol`:** solo están "al principio de
     página" (y deben colocar algo aunque no quepa) si lo está el `Multicol`. Si la primera no
     puede colocar nada, el `Multicol` corta antes de él; si no puede otra, el resto pasa a la
     página siguiente, porque las demás columnas tienen el mismo alto. La calidad del corte de
     página es la de la última columna; los cortes entre columnas no cuentan para el padre.
     *Dónde:* `MulticolLayout.Run.fill`. *Alternativa:* tratar cada columna como el principio
     de un fragmentainer, que forzaría contenido que no cabe en la página.

118. **Tamaños intrínsecos:** con `Count` y sin `Column width`, min-content = `Count`
     columnas del min-content del contenido más los huecos; con `Column width`, una columna. El
     max-content es `Count` columnas (una sin `Count`) del max-content del contenido o de
     `Column width` si es mayor, más los huecos. *Dónde:* `MulticolLayout.contentIntrinsic`.
     *Alternativa:* las reglas de CSS Multicol (min-content con `column-width` = el menor entre
     él y el del contenido).

119. **La línea entre columnas no puede ser más ancha que el hueco** (`COLUMN_RULE_WIDTH`):
     en CSS no ocupa sitio y se pinta encima de las columnas. Se pinta con el fondo del
     `Multicol`, solo entre columnas con contenido, desde arriba de la caja de contenido y con
     el alto de la columna más alta del fragmento; siempre continua (sin `column-rule-style`).
     *Dónde:* `paint/StackingContext.paintRules`, `tree/ModelValidator.validateMulticol`.
     *Alternativa:* la línea de CSS, con estilos y sin límite de grosor.

## Hito 16: tramos con formato dentro del texto (2026-10-27)

120. **La negrita y la cursiva de un tramo se suman a las de la caja** (R1): un tramo es un
     conjunto de marcas (`TextFormat`: negrita, cursiva, subrayado, tachado), no un peso y un
     estilo propios, así que no puede quitar la negrita de un texto en negrita. Es lo que hace
     Markdown (`**` dentro de un encabezado en negrita sigue en negrita) y deja que `Styles`
     cambie la fuente de la caja sin tocar los tramos. Familia, tamaño, color y `Line height`
     son los de la caja: la altura de las líneas no cambia. *Dónde:* `model/TextFormat.face`.
     *Alternativa:* tramos con `Weight` y `Style` explícitos, como un `<span>` de CSS.

121. **El texto de la caja sigue siendo una cadena** (`Box.text`) y los tramos son una
     partición suya (`Box.runs`, `null` sin formato): las líneas del documento, los mensajes,
     los marcadores del PDF y `{page}` siguen trabajando sobre el texto entero. Un texto sin
     formato es un único tramo y se mide y se pinta igual que antes (los PNG no cambian).
     *Dónde:* `tree/Box.runs()`. *Alternativa:* guardar solo los tramos y derivar el texto.

122. **El espacio entre dos palabras tiene el formato de su primer espacio en blanco** (R2): un
     subrayado cubre los espacios de dentro de su tramo pero no el espacio entre dos tramos que
     no lo llevan. El espacio que se pierde en el salto de línea no se pinta. Con `justify`, el
     espacio extra va en el tramo de su espacio. *Dónde:* `layout/inline/LineBreaker.paragraphs`
     y `place`. *Alternativa:* el formato del tramo de la palabra anterior, o el subrayado
     continuo entre tramos subrayados.

123. **Una palabra con partes de varios tramos es una sola palabra** (R3) y mide la suma de sus
     partes, sin kerning entre ellas (tampoco se aplica dentro de un tramo). *Dónde:*
     `LineBreaker.Word`. *Alternativa:* permitir el salto entre dos tramos.

124. **Subrayado y tachado con las métricas del TTF** (R2): `post` (`underlinePosition`,
     `underlineThickness`) y `OS/2` (`yStrikeoutPosition`, `yStrikeoutSize`), leídas como la
     parte de arriba de la raya (lo que dice la especificación OpenType); con la variante de cada
     tramo, así que la raya de la negrita es más gruesa. En SVG, una aproximación cercana a IBM
     Plex Sans. Se pintan como rectángulos del color del texto, el subrayado antes del texto y
     el tachado después (el orden de CSS), y como *artifacts* en el PDF etiquetado (R5).
     *Dónde:* `resources/FontFile.decoration`, `model/DecorationMetrics`,
     `paint/StackingContext.paintDecorations`. *Alternativa:* el subrayado centrado en la
     posición (la definición de PostScript), o el atributo `TextDecorationType` en la estructura.

125. **Los tramos no se vuelcan en el §7** (R4) ni tienen elemento propio en el PDF etiquetado
     (R5): son contenido del mismo `P`, `Span` o `H1`, en el mismo contenido marcado. Se prueban
     con tests de layout y de pintado. *Dónde:* `output/StxtDump`, `paint/PdfStructure`.
     *Alternativa:* volcar las líneas con sus tramos; un `Span` por tramo en el PDF.

126. **La variante de cada tramo debe existir** (R6): `FONT_VARIANT_MISSING` con la línea del
     tramo (la del carácter en que empieza) y el mensaje `(texto con formato bold)`; los
     caracteres de cada tramo se comprueban en su variante (`GLYPH_MISSING`). *Dónde:*
     `layout/inline/PdfMetrics.check`. *Alternativa:* un código propio para la variante de un
     tramo.

## Hito 17: elemento `Markdown` (2026-10-27)

127. **El Markdown se traduce a nodos STXT, no a cajas**: `MarkdownBuilder` genera `Heading N`,
     `Paragraph`, `List` y `Table` con su línea, y `BoxTreeBuilder` los construye como cualquier
     otro elemento. Así las reglas de `Styles`, la validación del §6, el PDF etiquetado y los
     marcadores no tienen ningún caso especial. Lo generado se valida además contra el schema,
     como red de seguridad. *Dónde:* `tree/MarkdownBuilder.translate`,
     `tree/BoxTreeBuilder.markdown`. *Alternativa:* construir las cajas directamente desde el
     árbol de commonmark-java.

128. **Parser: commonmark-java 0.30.1** con las extensiones de tablas y tachado de GFM y la de
     subrayado (`ins`). El tachado exige dos tildes (`~x~` queda como texto), como en el resto
     de la sintaxis, que dobla el delimitador. *Dónde:* `MarkdownBuilder.PARSER`, `pom.xml`.
     *Alternativa:* un parser propio del subconjunto; el tachado con una tilde, como GFM.

129. **El nivel de una lista se cuenta por tipo** (M2): el de una lista con viñetas es el número
     de listas con viñetas que la contienen, y las numeradas cuentan aparte. Una lista numerada
     dentro de una con viñetas usa el primer marcador de `Numbering`. `Start` solo se genera si
     el primer número no es 1. *Dónde:* `MarkdownBuilder.Translator.list`. *Alternativa:* un
     nivel común a los dos tipos, como el anidamiento de `ul` y `ol` en CSS.

130. **Las propiedades que no se usan no son un error** (K6): un `Bullet` sin listas con
     viñetas, o un `Table columns` sin tablas, se aceptan, aunque el §6 rechaza en general lo que
     no tiene efecto. El contenido de un Markdown cambia a menudo y obligaría a quitar y poner
     las propiedades; su valor sí se comprueba siempre. *Dónde:* `MarkdownBuilder.Translator`.
     *Alternativa:* el error de siempre de lo que no tiene efecto.

131. **Las filas de una tabla con celdas de menos o de más siguen a GFM**: se completan con
     celdas vacías y las que sobran se descartan, sin error, a diferencia de una `Table` escrita
     en STXT (`TABLE_ROW_CELLS`). commonmark-java no conserva las celdas que sobran. *Dónde:*
     `MarkdownBuilder.Translator.rows`. *Alternativa:* contar las celdas de cada línea en el
     texto y dar el error.

132. **Las líneas son las del principio de cada bloque** (K5): los errores de un párrafo de
     varias líneas señalan la primera, y los de una celda, la de su fila (commonmark-java no da
     la línea de cada celda). Los del marcador de una lista señalan `Bullet` o `Numbering`, y los
     de las columnas, `Table columns`. *Dónde:* `MarkdownBuilder.Translator.line`.
     *Alternativa:* posiciones dentro del bloque (`IncludeSourceSpans.BLOCKS_AND_INLINES`).

133. **Los textos vacíos usan `TEXT_CONTENT`** (un `Markdown` sin contenido, un encabezado `#`
     sin texto), y lo que el subconjunto no puede traducir, `MARKDOWN_UNSUPPORTED`: también un
     encabezado dentro de un ítem de lista (como en un `List item`) y una tabla sin filas debajo
     de la cabecera (una `Table` necesita su `Body`). Un `Bullet` o un `Numbering` con un
     marcador del otro tipo es `VALUE_NOT_ALLOWED`. *Dónde:* `MarkdownBuilder`. *Alternativa:*
     códigos propios para cada caso.

134. **El `Markdown` es un `Div` en el PDF etiquetado**, como un `Block`, y lo generado cuelga de
     él. *Dónde:* `paint/PdfStructure.type`. *Alternativa:* que no tenga elemento propio y lo
     generado cuelgue de su padre.

## Hito 18: imágenes (2026-10-28)

Decisiones I1–I10 de la hoja de ruta, todas con la propuesta aceptada: elemento `Image` con la
ruta relativa a `images/`, PNG y JPEG, 96 píxeles por pulgada, tamaño natural también en flujo
de bloque, `Fit` con `contain` por defecto, sin cortes, `Alt` opcional, imágenes solas en su
párrafo de Markdown y tres errores nuevos. Las interpretaciones que salieron al implementarlas:

135. **La proporción es la de la caja de contenido**: el relleno y el borde van fuera y no
     deforman la imagen. En las demás cajas, `Aspect ratio` se aplica a la caja de borde (una
     simplificación anterior). Un `Aspect ratio` declarado en una `Image` sustituye a la del
     fichero y se aplica igual, a la caja de contenido. *Dónde:* `layout/BoxModel.replaced`,
     `layout/image/ImageLayout.contentIntrinsic`. *Alternativa:* la caja de borde, como el resto.

136. **El alto de una imagen es siempre definido**, porque sale de la proporción: un flex no la
     estira en el eje cruzado de una fila, ni un grid en vertical (en CSS, `align-self: stretch`
     la estira y la deforma). En horizontal sí se estira (un `flex column`, un área de grid), y el
     alto la sigue. *Dónde:* `BoxModel.replaced` (da siempre `height`), que flex y grid consultan
     antes de estirar. *Alternativa:* estirarla y dejar que `Fit` la coloque.

137. **Mínimos y máximos con los dos ejes `auto`**: se limita el ancho, el alto sale de la
     proporción y se limita, y si el alto ha cambiado, el ancho se vuelve a deducir de él (y se
     vuelve a limitar). Es una aproximación de la tabla de CSS 2.1 §10.4; si los límites no se
     pueden cumplir a la vez con la proporción, gana el ancho y `Fit` coloca la imagen. *Dónde:*
     `BoxModel.replaced`. *Alternativa:* la tabla completa de CSS.

138. **Los tamaños intrínsecos son un mínimo de verdad**: min-content y max-content son el ancho
     natural, o el que sale de un `Height` en puntos (un `Height` en porcentaje no cuenta, porque
     no hay containing block). En una pista `1fr` o en un ítem flex sin `Min width`, la imagen
     no encoge por debajo; si no cabe, es un desbordamiento. CSS deja encoger las imágenes con
     ancho en porcentaje (*compressible replaced elements*). *Dónde:* `ImageLayout.contentIntrinsic`.
     *Alternativa:* min-content 0 con `Width` en porcentaje, como CSS.

139. **`Margin: 0 auto` centra una `Image` sin `Width`**, porque mide su ancho natural y no
     ocupa todo el ancho: es la excepción a `MARGIN_AUTO_WITHOUT_EFFECT` en flujo de bloque.
     *Dónde:* `tree/ModelValidator.validateMargins`. *Alternativa:* exigir `Width` o `Max width`
     como en cualquier caja.

140. **Errores de las imágenes:** una `Image` sin ruta es `IMAGE_UNKNOWN` al construir el árbol
     (no hace falta la carpeta); que el fichero exista y sea PNG o JPEG se comprueba antes de
     maquetar, en todos los motores, también con `FixedMetrics`, porque el tamaño natural hace
     falta para el layout (`new Engine()` lee las imágenes de la carpeta de recursos por
     defecto). Un `Alt` vacío es `VALUE_NOT_ALLOWED`; en Markdown, `![](ruta)` es una imagen
     decorativa (sin `Alt`), y un título (`![a](ruta "t")`), que no tendría efecto,
     `MARKDOWN_UNSUPPORTED`. *Dónde:* `BoxTreeBuilder.image` y `alt`, `layout/image/Images`,
     `MarkdownBuilder.Translator.image`. *Alternativa:* comprobar los ficheros solo con la salida
     PDF; aceptar el `Alt` vacío como decorativa.

141. **`PAGE_OVERFLOW` de una imagen sin `Height` señala su `Width`**, del que sale el alto; sin
     ninguno de los dos, la línea de la `Image`. *Dónde:* `layout/Overflow.pageOverflow`.
     *Alternativa:* siempre la línea de la `Image`.

142. **PDF etiquetado:** el `BBox` del `Figure` es la caja de contenido de la imagen (no la
     parte visible con `contain`), y una imagen con `Alt` dentro de un `Fixed` o en las filas
     repetidas de una tabla es un *artifact* (en la tabla, salvo la primera vez), como el resto
     de su contenido. *Dónde:* `paint/PdfStructure.figureBox`, `PdfCanvas.image`.
     *Alternativa:* el rectángulo dibujado como `BBox`.

143. **Las imágenes de prueba están en la carpeta de recursos del repositorio**
     (`resources/images/test/`), junto a la fuente, porque los tests de layout, el catálogo de
     errores y los casos golden usan la carpeta por defecto. Las genera Java2D y no tienen
     licencia de terceros. *Alternativa:* una carpeta de recursos solo para los tests.

144. **Los casos de prueba están en `test/`, en la raíz del repositorio**, no en
     `src/test/resources`: `golden/`, `errors/`, `examples/` y `spec/` (el ejemplo del §10, que
     antes era `examples/report.stxt`). Los tests los leen por ruta de disco
     (`Path.of("test", …)`) y no del classpath, así que no hay que reconfigurar Maven ni
     Eclipse. Los tests Java siguen en `src/test/java` y los recursos (fuentes e imágenes), en
     `resources/`. *Dónde:* `GoldenTest`, `ErrorCatalogTest`, `ExamplesTest`. *Alternativa:*
     también mover los tests Java a `test/java`, que obliga a cambiar `testSourceDirectory`.

145. **El catálogo de errores tiene un fichero por caso**, un documento STXT de verdad, en
     `test/errors/<grupo>/<código>.<descripción>.stxt`, con un grupo por familia de `ErrorCode`
     (los códigos de `stxt-core`, en `schema`). Las tres primeras líneas son comentarios
     (`# Case:`, `# Code:`, `# Line:`) y la línea esperada es la del propio fichero, cabecera
     incluida (la primera del documento es la 4), así que se puede abrir y validar en el editor
     y no hay que contar desde un bloque incrustado. *Dónde:* `ErrorCatalogTest`. *Alternativa:*
     un fichero por grupo con los documentos incrustados en `Document >>`, como antes.

146. **Los ejemplos se maquetan con las métricas reales** (`PdfMetrics`) y la carpeta
     `resources/`, no con `FixedMetrics` como los golden, y su `expected.stxt` es una
     **instantánea** que genera el motor (`-Dexamples.update=true`) y se revisa mirando las
     imágenes, no un resultado calculado a mano: con texto real y fuentes reales, calcularlo a
     mano no tiene sentido. Un `expected.stxt` que falta es un error del test. *Dónde:*
     `ExamplesTest`. *Alternativa:* comparar solo las imágenes, que no detecta cambios de un
     punto.

147. **Las imágenes de los ejemplos se versionan y el PDF no**, como en los golden: el PDF se
     regenera en cada ejecución y está en `.gitignore` (`test/examples/**/*.pdf`); las
     `page-N.png`, a 110 ppp y sin contornos, se versionan para que un cambio se vea en el diff
     y no se reescriben si los píxeles cambian menos de la tolerancia del antialiasing (16 por
     canal). Primero se versionaban también los PDF, para poder abrirlos al navegar por el
     repositorio; se descartó porque cambian en cada ejecución (fechas e identificadores).
     *Dónde:* `golden/ReferenceFiles`, `ExamplesTest`, `.gitignore`. *Alternativa:* versionar el
     PDF y reescribirlo solo si cambian las imágenes.

148. **Se añaden IBM Plex Serif y Mono** (los TTF de IBM/plex, con su licencia OFL, en
     `resources/fonts/`) a Plex Sans, para poder escribir documentos con serif y con
     monoespaciada, y **las imágenes de los ejemplos las genera un programa**
     (`tools/ExampleImages.java`, con Java2D y las mismas fuentes) y se versionan en
     `resources/images/examples/`: no hay ficheros de terceros ni derechos que respetar, y el
     autor puede cambiarlas retocando el programa. *Alternativa:* imágenes de bancos libres, que
     habría que citar y que no se pueden regenerar.

149. **Una portada sin cabecera ni pie, y un fondo detrás de todo, se hacen con lo que hay**: un
     `Absolute` con `Layer` alta y del tamaño de la página tapa lo que repiten los `Fixed`, y un
     `Fixed` con `Layer` negativa queda detrás del contenido en todas las páginas. No hay
     páginas con contenido propio (primera página distinta, páginas con nombre). *Dónde:*
     ejemplos 04, 06, 09 y 10. *Alternativa:* añadir al motor una forma de declararlo.

150. **Los números de página del índice del informe anual están escritos a mano**
     (ejemplo 04): el motor no tiene referencias a páginas. Si el texto cambia, hay que
     revisarlos. *Alternativa:* un nodo de referencia que se resuelva al terminar de paginar,
     como `{page}` y `{pages}`.

## Hito 22: referencias a páginas e índice (2026-10-28)

Decisiones X1–X12 de la hoja de ruta: `{page of: id}` y `{number of: id}`, resueltas paginando
hasta que converjan (X3), un elemento `Contents` que genera el índice, y un `Padding` por nivel
en sus entradas. Las interpretaciones que salieron al implementarlas:

151. **La referencia se sustituye antes de repartir el texto en líneas, no después**, a
     diferencia de `{page}` y `{pages}`. El marcador `{page of: id}` tiene espacios, y el
     reparto en líneas lo partiría en tres palabras. Con convergencia (X3) tampoco hace falta un
     segundo paso: el texto con el valor supuesto se mide, se reparte y se pinta tal cual. Las
     líneas ya no contienen la referencia. *Dónde:* `layout/inline/InlineLayout.runs`,
     `model/Reference`. *Alternativa:* un marcador sin espacios, resuelto en dos fases como
     `{page}`.

152. **La página de una caja es la de su primer fragmento**, también en las filas repetidas de
     una tabla (la primera de sus páginas). Se busca en los fragmentos del resultado, en orden de
     página y de documento. *Dónde:* `layout/Paginator.pagesOf`. *Alternativa:* la del último
     fragmento.

153. **El número de prueba de la primera pasada tiene las cifras de `{page}`** (`00`), así que
     siempre hay al menos una segunda pasada si hay referencias a páginas, aunque la página real
     tenga dos cifras: se compara el texto supuesto con el real. El máximo es de 10 pasadas que
     cambian alguna página; las pasadas por crecer las cifras de `{pages}` no cuentan, porque solo
     crecen. Como un número solo cambia de ancho al cambiar de cifras, la paginación converge
     siempre salvo con un medidor de texto que dé anchos distintos a cifras de la misma longitud
     (`REFERENCE_UNSTABLE` es una red de seguridad, sin caso en el catálogo; la prueba
     `PageReferencesTest` la fuerza con un medidor de pega). *Dónde:* `layout/Paginator.paginate`.
     *Alternativa:* quedarse con la última pasada sin avisar.

154. **Dos errores se detectan al construir el árbol**, no al maquetar: un `Id` que no existe o
     una referencia sin `Id` (`REFERENCE_UNKNOWN`) y la página de una caja de un `Fixed`
     (`REFERENCE_NOT_PLACEABLE`), porque dependen solo de la estructura del documento. Se
     comprueban todas las referencias del árbol, también las de un `Fixed`, un `Markdown` o una
     `Cell`. *Dónde:* `tree/References.validate`. *Alternativa:* comprobarlo al paginar, con
     la línea del texto igualmente.

155. **Las referencias se validan como cifras en las métricas reales**: un texto con
     `{page of: id}` necesita en su fuente las cifras, no las letras del marcador. *Dónde:*
     `layout/inline/PdfMetrics.check`.

156. **Solo se numeran los ítems de las listas numeradas**, y el número es el del marcador sin su
     sufijo (`3`, `c`, `IV`). Los encabezados no llevan número en el modelo, así que no hay nada
     que referir; un `{number of: id}` de otra caja es `REFERENCE_NOT_NUMBERED`. Un número
     jerárquico («4.2») se escribe con dos referencias. La propuesta del hito hablaba de
     encabezados «si el encabezado lleva» número: hoy ninguno lo lleva. *Dónde:*
     `tree/ListMarkers.number`, `Box.number`, `tree/References.validate`. *Alternativa:*
     numerar los encabezados (una propiedad nueva) o añadir contadores genéricos.

157. **`{number of: id}` no usa la paginación**: el número sale del árbol, así que se sustituye
     desde la primera pasada, igual que las páginas supuestas. No se puede referir a un ítem de
     un `Markdown`, porque lo generado no lleva `Id`. *Dónde:* `layout/Paginator.numbers`.
     *Alternativa:* dar `Id` a lo generado.

158. **Las entradas del índice son cajas generadas al final de la construcción**, cuando se
     conocen todos los encabezados (también los de un `Markdown` y los posteriores al índice).
     Cada una es un `Flex` con tres cajas (título, guía, número) que cuelgan del `Contents`;
     el número es un `{page of: …}` como el que escribiría el autor, así que no hay un mecanismo
     aparte para las páginas del índice. Un encabezado sin `Id` recibe un nombre generado
     (`Box.anchor`, `(heading 3)`), que solo existe para las referencias. *Dónde:*
     `tree/ContentsBuilder`, `Box.key`. *Alternativa:* dar `Id` a lo generado, que saldría en
     los volcados.

159. **Las entradas se nombran en `Styles` con `Element: Contents entry N` aunque no se
     escriban**: un `Element` generado y «solo para selector» (`Element.selectable()`), que el
     schema no tiene. El schema solo define el `Contents`; los títulos, las guías y los números
     son cajas de la entrada y heredan de ella. *Dónde:* `tree/Element`, `StyleRules.apply`.
     *Alternativa:* una regla por parte (`Contents title`…), más fina y más larga de escribir.

160. **La guía se pinta con rectángulos rellenos, sin texto**: puntos de 0,09 veces el cuerpo
     cada medio cuerpo, pegados al número, o una línea de 0,05; sobre la línea base de la
     entrada. Así llena exactamente el hueco, sin caracteres seleccionables ni conteo de puntos
     en el texto del PDF, y es un *artifact*. Los puntos de una guía de texto serían la letra
     «.» repetida, con un ancho que habría que calcular al maquetar. *Dónde:*
     `paint/Leaders`, `StackingContext.paintLeader`. *Alternativa:* guía de texto.

161. **Un `Contents` sin ningún encabezado de sus niveles es un error** (`CONTENTS_EMPTY`), en la
     línea de política del §6: un índice sin entradas no tiene efecto. `Levels` son siempre dos
     niveles (no se admite uno solo) para no dudar entre «hasta el 2» y «solo el 2». *Dónde:*
     `tree/ContentsBuilder`. *Alternativa:* un índice vacío, o `Levels: 2` como «hasta el 2».

162. **Con título largo, el número queda en la primera línea**: la entrada es un flex sin
     alineación por línea base, y el título se parte. *Alternativa:* alinear el número con la
     última línea, que pediría una alineación `baseline` que no existe todavía.

## Hito 23: enlaces

163. **El enlace de una caja es una propiedad del elemento, no de un grupo** (`Link`, como `Id`):
     no va en `Box`, `Typography` ni `Break`, no se hereda y no se puede poner en las reglas de
     `Styles`. Un enlace por regla de clase haría que todas las cajas con esa clase llevaran al
     mismo sitio, que casi nunca se quiere, y heredado, un `Typography` con `Link` en `Document`
     enlazaría todo el documento. Se admite en todos los elementos de caja salvo `Markdown`
     (sus enlaces van en su contenido) y `Contents` (sus entradas ya llevan el suyo). *Dónde:*
     schema, `Style.link`, `tree/BoxTreeBuilder.link`. *Alternativa:* una propiedad de
     `Typography`, o solo en `Text`, `Paragraph`, `Cell` e `Image`.

164. **Un enlace no tiene aspecto por defecto**, salvo en Markdown, donde el tramo sale subrayado
     (L3): una caja con `Link` se ve igual que sin él, y el autor le da color y subrayado con su
     estilo. En Markdown no hay otra manera de dar estilo a una parte de una línea, así que se
     subraya, sin color (el color es el del texto). *Dónde:* `tree/MarkdownBuilder.link`.
     *Alternativa:* azul y subrayado siempre, o una regla `Element: Link`.

165. **`#id` solo lleva a cajas con `Id` y a los encabezados de un índice**, y no a las de un
     `Fixed` (que están en todas las páginas): `LINK_UNKNOWN` para las dos cosas, con mensajes
     distintos, en lugar de un código más. Un encabezado de un `Markdown` no tiene `Id`, así que
     solo lo enlaza su entrada del índice (la entrada usa el mismo nombre generado que las
     referencias, decisión 158). *Dónde:* `tree/LinkTargets`. *Alternativa:* un código propio
     para el `Fixed` (`LINK_NOT_PLACEABLE`), como `REFERENCE_NOT_PLACEABLE`.

166. **`LINK_SCHEME` cubre también el destino mal escrito**: vacío, `https://` sin servidor,
     `mailto:` sin destinatario, con espacios o una ruta relativa. Un código solo para «no es una
     URL» no añadía nada: la corrección es la misma. El mensaje dice cuál es el problema. *Dónde:*
     `model/Links.problem`. *Alternativa:* `LINK_INVALID` aparte.

167. **Los enlaces de Markdown son `[texto](destino)` y `<destino>`; con título o con una
     definición aparte (`[x][ref]` más `[ref]: destino`) son `MARKDOWN_UNSUPPORTED`**, como el
     título de una imagen (decisión 140). El destino se escribe en el propio enlace. Una imagen
     dentro de un enlace (`[![a](b.png)](url)`) sigue siendo error: una imagen va sola en su
     párrafo; para una imagen clicable está `Image` con `Link`. *Dónde:*
     `tree/MarkdownBuilder`. *Alternativa:* admitir las definiciones (commonmark-java ya las
     resuelve).

168. **Un enlace de Markdown es un `TextFormat` más** (`TextFormat.link`): dos enlaces con
     destinos distintos no se unen en un tramo aunque el resto del formato sea igual, y un
     destino igual en dos tramos seguidos sí (una parte en negrita dentro de un enlace es una
     sola zona). No cambia cómo se miden las palabras ni las líneas. *Dónde:* `model/TextFormat`,
     `layout/inline/LineBreaker`. *Alternativa:* una lista de enlaces aparte del texto.

169. **La zona de un enlace se calcula al pintar, no al maquetar**: `StackingContext.paintLinks`
     recorre los fragmentos y sus líneas, y llama a `Canvas.link`. Una caja con `Link` es clicable
     en su caja de borde (una zona por fragmento, también el `Fixed` y las filas repetidas de una
     tabla); un tramo, en el alto de la línea y el ancho del tramo (una zona por línea). El layout
     no sabe nada de los enlaces, y el SVG de depuración los ignora. *Dónde:*
     `paint/StackingContext`, `paint/Canvas`. *Alternativa:* guardar las zonas en el fragmento.

170. **Los destinos internos se resuelven después de pintar todas las páginas**: un enlace puede
     ir hacia delante. `PdfCanvas.write` crea todas las páginas antes de pintar ninguna,
     `PdfDestinations` busca la esquina del primer fragmento de cada caja con nombre (la misma
     que usan los marcadores, que antes buscaban solo los encabezados) y `PdfLinks` los asigna.
     *Dónde:* `paint/PdfCanvas`, `paint/PdfDestinations`, `paint/PdfLinks`. *Alternativa:*
     destinos con nombre en el catálogo del PDF.

171. **(Sustituida por la 252.) PDF etiquetado: el `Link` solo contiene su `OBJR`**, no el texto marcado: un `Link` hijo
     del elemento de la caja con la anotación y su `Alt`, y no un elemento que envuelve el
     contenido del tramo. Para envolverlo habría que partir el contenido marcado de un `P` en
     varios `MCID` por cada tramo con enlace. El texto del enlace llega a quien lo lee por
     el `Alt` del `Link` y el `Contents` de la anotación. Lo que no es contenido (un `Fixed`, una
     imagen decorativa y las filas repetidas de una tabla desde su segunda página) no tiene
     `Link`, pero sí anotación. *Dónde:* `paint/PdfStructure.link`. *Alternativa:* el `Link` con
     el contenido marcado del tramo dentro, que es lo más completo para la accesibilidad.
     **Deuda pendiente, anotada en la sección «Accesibilidad» de `TODO.md`:** hay que hacerlo,
     porque la accesibilidad es básica en este proyecto.

172. **Solo se admiten `https`, `http`, `mailto` y `#id`**: ni `tel:`, ni `ftp:`, ni rutas
     relativas, ni `javascript:` ni `file:` (L6). El motor no abre nada y un PDF no debería
     llevar un enlace que ejecute o lea algo en quien lo abre. *Dónde:* `model/Links`.
     *Alternativa:* cualquier URI, con una lista de esquemas prohibidos.

173. **El `Alt` y el `Contents` de un enlace es el texto de la caja** (o el `Alt` de la imagen):
     si es un `Block` con varios textos, los une con un espacio; el número de página de una
     entrada de índice no cuenta. Sin texto (un `Block` solo con fondo), el propio destino.
     Las referencias (`{page of: …}`) y marcadores (`{page}`) del texto salen sin sustituir.
     *Dónde:* `paint/StackingContext.describe`. *Alternativa:* un `Alt` propio en `Link`.

174. **Las reglas de posición son valores de `Element`, no elementos nuevos** (B1, hito 25):
     `Head cell`, `Body cell`, `Foot cell`, `Odd row cell` y `Even row cell` se escriben en
     `Element:` y se aplican además de la regla `Element: Cell`; el elemento de la caja sigue
     siendo `Cell`. `Odd row cell` y `Even row cell` cuentan solo las filas del **cuerpo**, desde
     la primera como la 1: la cabecera y el pie no entran en la cebra. La fila de una celda con
     `Span` de varias filas es la primera de las que ocupa (B5). *Dónde:* `tree/CellSelector`,
     `tree/CellPosition`, `tree/StyleRules`. *Alternativas:* que `Row` admita `Box` y una regla
     `Element: Row` (no cubre el `Typography` de la celda), o no aplicar la cebra a celdas de
     varias filas.

175. **`For column: n` o `For column: n m`**: el valor es una columna o un rango de menor a mayor,
     ambas incluidas (no una lista), y se aplica a las celdas que **empiezan** en una de ellas, en
     cualquier grupo de la tabla; una celda con `Span` de columnas es de la columna donde empieza.
     Dos `For column` con el mismo rango son `STYLE_DUPLICATE`; si se solapan, se aplican en el
     orden de `Styles`, como las clases. No es error que una regla no se aplique a ninguna celda
     (la tabla puede tener menos columnas). *Dónde:* `tree/StyleRules`. *Alternativa:* `n m` como
     lista de columnas sueltas.

176. **`Column align` es una propiedad nueva de `Table`, no un hijo de `Columns`** (B2): `Columns`
     es la lista de pistas, un nodo que comparten `Grid` y `Table` con una sola definición en el
     schema, y un hijo suyo habría obligado a prohibirlo en el `Grid`. Da una alineación por
     columna (`left`, `center`, `right`, `justify`), tantas como `Columns` (`TABLE_COLUMN_ALIGN`),
     y es el valor por defecto más bajo de `Align` en las celdas: **por encima de la regla
     `Element: Cell`** (que es el tipo, más general que una columna concreta) **y por debajo de**
     las reglas de posición, `For column`, clase, `Id` y lo que declara la celda. Así una cabecera
     `Head cell` centrada gana a la alineación de su columna, y un `Element: Cell` con `Align` no
     deja la propiedad sin efecto. La línea de la alineación es la del `Column align`.
     *Dónde:* `tree/TableBuilder.columnAlign`, `tree/StyleRules.apply`. *Alternativa:* por debajo
     también de `Element: Cell`, o por encima de las reglas de `Styles`.

177. **Prioridad de las reglas de una celda** (B3), de menos a más: `Element: Cell`,
     `Column align`, reglas de posición (grupo y luego fila), `For column`, `For class`, `For id`
     y lo propio de la celda. Las tablas de `Markdown` reciben las mismas reglas; su `Align` sale
     de la fila de separación, que lo declara cada celda y por tanto gana a todas (B4).
     *Dónde:* `tree/StyleRules.apply`. *Alternativa:* que la posición gane a la columna.

178. **Las reglas de posición y de columna tampoco dan `Break` a una celda**: se aplica el mismo
     `STYLE_NOT_ALLOWED` que a `Element: Cell`, con el `Break` de la regla (los cortes de una
     celda son los de su `Row`). *Dónde:* `tree/StyleRules.noBreak`.

179. **Ejemplos reescritos con las reglas** (hito 25): `Class: th` pasa a `Element: Head cell`, y
     `Class: num` a `Column align` por tabla (y a `Foot cell` con `Align: right` en el pie de la
     factura, que tiene celdas con `Span`). El pie de totales sigue necesitando `Class: total`
     en sus celdas. La cabecera de las tablas de Markdown (informe anual, manual) sale ahora con el
     estilo de `Head cell`, y la celda vacía de la esquina del horario (ejemplo 11), que antes
     no llevaba clase, también: la barra de cabecera queda entera. El ejemplo 05 estrena la
     cebra con `Even row cell`.

180. **`Pages` en `Fixed`, no páginas con nombre** (N1, hito 24): una propiedad con `all` (por
     defecto), `first`, `not-first`, `odd`, `even` y `last`, solo en `Fixed` (no en `Styles`, como
     `Offset` y `Layer`). Cuenta las páginas desde la 1 en el orden físico, y `odd`/`even` son las
     impares y pares de ese número. En un documento de una sola página, `first` y `last` coinciden.
     *Dónde:* schema, `tree/Style.Pages`, `tree/BoxTreeBuilder`, `layout/Paginator`.
     *Alternativa:* páginas con nombre como `@page :first` de CSS, más general y más pesado.

181. **Sin margen distinto en la primera página** (N2): no hay `First margin`. El `Paginator` sigue
     maquetando todas las páginas con el mismo área útil, y la portada a sangre sigue siendo un
     `Absolute` con `Offset` negativo y el bloque de 1 pt con `Break: After: page` que fuerza el
     salto (un `Absolute` no ocupa sitio). Lo que desaparece del truco es el `Layer` que tapaba el
     pie. La primera hoja del contrato (07) deja el mismo hueco que las demás donde va la
     cabecera. *Alternativa:* `First margin` en `Media`, con áreas útiles distintas por página.

182. **`{page}` y `{pages}` siguen siendo la página física** (N3): la portada cuenta, y la primera
     hoja interior de un informe con portada es «Página 2 de 7». No hay `Start` en `Media`.
     *Alternativa:* una numeración que empiece donde se diga.

183. **Un `Fixed` con `Pages` no reserva sitio donde sale ni donde no** (N4): el flujo es el mismo
     con él o sin él, como hoy. El `Paginator` lo filtra al colocarlo en cada página; un `Fixed`
     que no sale en ninguna (`not-first` en un documento de una sola página) es válido y no se
     comprueba, y su `BOX_OVERFLOW` solo se mira en las páginas donde sale. Un `Fixed` dentro de
     otro sale solo donde salen los dos, cada uno con su `Pages`. *Dónde:* `layout/Paginator`.
     *Alternativa:* que empuje el flujo, que rompe el modelo de `Fixed` como caja fuera de flujo.

184. **Sin páginas en blanco para doble cara** (N5): `Break: Before: odd-page` queda fuera del
     hito y anotado en `TODO.md`. *Alternativa:* `odd-page` y `even-page` en `Before` y `After`.

185. **Ejemplos con `Pages`** (hito 24): el pie de 04, 06 y 09 y el logotipo de la cabecera de 07
     pasan a `Pages: not-first`; las portadas de 04 y 09 pierden el `Layer: 5` (el pie ya no sale
     debajo, así que sus PNG no cambian); el boletín (05) gana un titulillo corrido desde la
     segunda página. La carta (02) es de una sola página y no cambia: su membrete ya es un bloque
     de la primera y no hay segunda hoja que enseñar.

## Hito 26: formato dentro de la línea (2026-10-28)

186. **`Inline: markdown` como propiedad de `Typography`** (F1, F3): el texto es literal salvo que
     se pida, y se pide con una propiedad que se hereda, en `Document`, en una regla de `Styles` o
     en el propio elemento; `Inline: none` vuelve al literal. Los documentos que ya existen no
     cambian. *Dónde:* schema, `tree/Style.Inline`, `tree/PropertyReader`.
     *Alternativa:* hijos `Run` con su propio `Typography` (más STXT, mucho más largo), o una
     propiedad solo del elemento (se repetiría en cada texto, justo lo que quitó el hito 25).

187. **La misma sintaxis y el mismo subconjunto que `Markdown`** (F2, F4): `**x**`, `*x*`, `++x++`,
     `~~x~~` y `[x](destino)`, con los escapes de CommonMark (`\*`). Se reutiliza el parser y el
     `Translator` de `MarkdownBuilder` en modo `inlineOnly` (`MarkdownBuilder.inline`), así que un
     enlace con título o con un destino mal escrito da los mismos errores. *Dónde:*
     `tree/MarkdownBuilder`. *Alternativa:* un parser en línea propio, o admitir también listas y
     encabezados (que ya hace `Markdown`).

188. **Solo párrafos** (F4, F5): un texto con `Inline: markdown` que empieza como un encabezado,
     una lista, una cita, código o una tabla es `MARKDOWN_UNSUPPORTED`, con el mensaje propio de
     este modo y la línea del párrafo; hay que escapar el primer carácter (`1\.`, `\-`, `\#`) si
     es literal. Varios párrafos (línea en blanco entre ellos) se unen con `\n\n`, que es lo que ya
     entiende el reparto en líneas. *Alternativa:* tratar lo que no es un párrafo como texto literal
     (silencioso, contra la política del §6).

189. **`Box.text` es el texto sin marcas y el texto deja de ser un bloque** (F2): lo que ven el
     reparto en líneas, el PDF etiquetado, las referencias (`{page}`, `{page of: id}`) y el índice es
     el texto interpretado. Como el texto ya no se corresponde línea a línea con el del
     documento, `Box.textBlock` es falso y los errores del texto (fuente sin un carácter,
     referencia desconocida, `LINK_UNKNOWN`) señalan la línea donde empieza. *Dónde:*
     `tree/BoxTreeBuilder.inlineFormat`. *Alternativa:* llevar la correspondencia de líneas de cada
     párrafo, más precisa pero más código para un caso poco frecuente.

190. **No actúa dentro de un `Markdown`** (F1): sus textos ya vienen interpretados y lo escapado
     debe seguir siendo literal; `Inline: markdown` heredado de `Document` no los vuelve a pasar
     por el parser. Una caja de `Contents` genera sus entradas sin pasar por `element`, así que
     tampoco. *Dónde:* `BuildContext.insideMarkdown`. *Alternativa:* un error por declararla dentro
     de un `Markdown` (se hereda, así que sería habitual).

191. **Qué admite:** `Text`, `Paragraph` (también como hijo de un `Heading`) y `Cell` con valor;
     una `Cell` con elementos, no. `Image.Alt` y el resto de valores siguen siendo literales.
     *Alternativa:* también `Alt`, `Title` y `Language` (no tienen formato que pintar).

192. **Ejemplos** (hito 26): las partes del contrato (07), la leyenda del manual (08) y la carta de
     la presidenta (04) pasan de `Markdown` a `Paragraph` con la regla `Element: Paragraph` y
     `Inline: markdown`, sin cambiar un píxel; la factura (01), el currículum (03) y la carta del
     restaurante (06) ganan una negrita o una cursiva dentro de una frase. Los pasos numerados
     del manual y las cláusulas del contrato siguen siendo `Markdown` porque son listas.

193. **`Language` obligatorio en `Document`** (hito 27, Y4; revisa la 87). El idioma es una
     característica de accesibilidad (PDF/UA exige el `Lang` del catálogo, y un lector de pantalla
     lo necesita para leer el contenido), así que deja de ser opcional: un `Document` sin
     `Language` es `TOO_FEW_CHILDREN` (lo detecta el schema, con la línea del `Document`). Además
     es la configuración regional que usará `Transform`. Todos los documentos de prueba, los
     ejemplos y los de la especificación declaran `Language: es`. `Title` sigue siendo opcional.
     *Dónde:* schema, `docs/ESPECIFICACION.md` §2 y §4.1. *Alternativa:* dejarlo opcional y
     usar `Locale.ROOT` si falta.

194. **`Transform` se aplica a los tramos antes de repartir en líneas** (Y1). Cambia lo que mide
     el texto (`ß` → `SS`), así que no puede ser solo de pintado: `InlineLayout.runs` transforma
     los tramos (después de sustituir las referencias) y las líneas llevan el texto ya
     transformado; `Box.text` es el escrito. Los marcadores `{page}` y `{pages}` no se
     transforman, porque `PageNumbers` los sustituye al terminar de paginar. *Dónde:*
     `layout/inline/TextTransform`, `layout/inline/InlineLayout`. *Alternativa:* transformar al
     construir el árbol (`Box.text` ya en mayúsculas), que perdería el texto escrito para el PDF
     etiquetado.

195. **`capitalize` solo sube la primera letra de cada palabra** (Y1). Una palabra empieza tras un
     espacio o un salto de línea, aunque el espacio sea el final de otro tramo, y el resto de la
     palabra se deja como está (`iPhone` da `IPhone`, no `Iphone`), como en CSS. *Dónde:*
     `TextTransform.capitalize`. *Alternativa:* pasar el resto de la palabra a minúscula.

196. **El interletraje lo lleva cada carácter menos el último de la línea** (Y2). Un espacio también
     lo lleva, y el ancho de una línea es lo que suman sus caracteres menos un interletraje, así
     que `right` y `center` alinean con la última letra y no con un hueco. Con `justify`, el
     sobrante que reparte es lo que queda después del interletraje. Puede ser negativo (texto más
     apretado) y no se comprueba que el avance del carácter siga siendo positivo. *Dónde:*
     `LineBreaker.advance`, `LineBreaker.width` y `LineBreaker.place`. *Alternativa:* el de CSS,
     que también lo pone tras el último carácter; o rechazar valores negativos.

197. **Versalitas sintetizadas al 80 %** (Y3). Cada minúscula que tiene mayúscula distinta pasa a
     mayúscula y se mide y se pinta al 80 % del `Size` (`TextFormat.SMALL_CAPS_SCALE`); los
     espacios, las cifras, los signos y las mayúsculas no se reducen. No se usa la característica
     `smcp` de la fuente (ninguna de las incluidas la tiene) y no hay error si falta. El
     subrayado y el tachado, la altura de línea y la línea base son los del `Size` de la caja, no
     los de las versalitas. Se aplica después de `Transform`. *Dónde:* `TextTransform.emit`,
     `model/TextFormat`. *Alternativa:* usar `smcp` si la fuente la tiene.

198. **`Small caps` con `Transform: uppercase` no es un error** aunque no tenga efecto (ninguna
     minúscula que reducir): son propiedades heredables y un documento puede declarar `Small
     caps` en `Document` y `uppercase` en un solo rótulo. *Dónde:* `TextTransform`. *Alternativa:*
     un error (la política de §6 para lo que no tiene efecto), que obligaría a repetir las reglas.

199. **Interletraje en el PDF con ajustes de `TJ`** (Y5, resuelta sin preguntar). Las fuentes son
     Type 0 Identity-H, donde `Tw` no actúa, y `justify` ya usa `TJ`; con interletraje, cada
     carácter va en su cadena con el desplazamiento entre ellas. El texto sigue siendo
     seleccionable, aunque el extractor de un lector puede insertar espacios si la separación es
     grande (`TODO.md`). *Dónde:* `paint/PdfCanvas.spaced`. *Alternativa:* el operador `Tc`.

200. **`ActualText` solo si el texto escrito es fijo** (Y1). Un elemento con `Transform` o `Small
     caps` lleva `ActualText` con el texto escrito (`Box.text`) en el PDF etiquetado, para que un
     lector de pantalla lea «Emisor» y no «EMISOR», salvo si el texto contiene `{page}`, `{pages}` o
     una referencia, que cambian con la página: ahí se lee lo pintado. *Dónde:*
     `paint/PdfStructure.build`. *Alternativa:* dar el texto sustituido de cada fragmento
     (más código, y el `ActualText` de un elemento partido en páginas no es por fragmento).

201. **`Letter spacing` solo en puntos** (Y2). Un número; un porcentaje o un texto es
     `VALUE_NOT_ALLOWED`. *Dónde:* `tree/ValueReader.signedPoints`. *Alternativa:* una fracción
     del `Size` (`0.05em` de CSS).

202. **Ejemplos** (hito 27): los rótulos de los ejemplos 01, 03, 04, 05, 06, 07, 08, 09 y 10 se
     escriben en minúscula y las reglas de `Styles` (o el `Typography` del texto, si es uno solo)
     los ponen en mayúsculas con `Transform: uppercase`, casi todos con 0,6 a 4 pt de interletraje.
     Ningún volcado cambia (el interletraje no cambia el alto de las líneas) y los PNG solo se
     diferencian en los rótulos. «CIF B-…», «Nº» y las siglas se quedan escritos como están. Los
     marcadores del PDF de los encabezados ahora dicen «Experiencia» y no «EXPERIENCIA».

203. **Texto preformateado con una propiedad `White space`** (hito 29, W1): `normal`, `pre` y
     `pre-wrap` en `Typography`, que se hereda y vale en `Styles`, y actúa en cualquier texto
     (`Text`, `Paragraph`, `Cell` con valor). Solo esos tres valores de los seis de CSS (sin
     `nowrap`, `pre-line` ni `break-spaces`). *Dónde:* schema, `tree/Style.WhiteSpace`,
     `layout/inline/PreformattedText`. *Alternativa:* un elemento `Preformatted`, que no cabría
     en una `Cell` con valor y no se heredaría.

204. **Todo el texto preformateado es un solo párrafo** (W5): las líneas en blanco son líneas y no
     separan párrafos, así que las viudas y huérfanas se cuentan sobre el bloque entero y un
     bloque largo se parte entre líneas como cualquier texto. En CSS no hay párrafos en un `<pre>`
     y las viudas y huérfanas cuentan líneas del bloque, igual. *Dónde:* `InlineLayout.breakText`.
     *Alternativa:* un párrafo por línea de origen (no se podría cortar sin violar las viudas).

205. **`pre` no parte y desborda** (W2): una línea más ancha que la caja es `TEXT_OVERFLOW`, con el
     mensaje de `pre` (sugiere `pre-wrap`); `pre-wrap` parte entre palabras y una palabra más
     ancha que la caja sigue desbordando, como en el texto normal (en CSS tampoco se parte sin
     `overflow-wrap`). *Dónde:* `layout/Overflow.textOverflow`. *Alternativa:* recortar, contra la
     política del §6.

206. **Los espacios del corte de `pre-wrap` se pierden** (CSS los deja colgando): no se ven y así
     una línea no desborda por culpa de sus espacios finales. La sangría de antes de la primera
     palabra sí se conserva, y los espacios de dentro de una línea, también. El parser de
     `stxt-core` ya quita los espacios al final de cada línea de un `Content >>`. *Dónde:*
     `PreformattedText.wrap`. *Alternativa:* dejarlos colgando (medirlos pero no contarlos).

207. **Las líneas preformateadas no se justifican**: `justify` se trata como `left` en vez de ser
     un error, porque `Align` se hereda y es normal declararlo en `Document`; un bloque de código
     heredaría el error. En CSS, `justify` sí justifica un `pre-wrap` (menos la última línea de
     cada línea de origen). *Dónde:* `InlineLayout.layout`. *Alternativa:* justificar las líneas
     partidas de `pre-wrap`; error en los dos modos.

208. **`Tab size` es un entero de columnas con tabuladores de columna** (W3, ampliada por el
     usuario: «una propiedad que diga cuántos espacios, 4 por defecto»): cada tabulación lleva a la
     siguiente columna múltiplo de `Tab size`, contando caracteres desde el principio de la línea,
     como `tab-size` de CSS con un entero y como los editores; así `a⇥b` y `abcd⇥e` alinean en una
     fuente monoespaciada. Por defecto 4 (el de los ficheros STXT) y no 8 (el de CSS). Un valor
     menor que 1 es `VALUE_NOT_ALLOWED`. *Dónde:* `PreformattedText.sourceLines`. *Alternativa:*
     sustituir cada tabulación por exactamente `Tab size` espacios.

209. **Un salto de línea final no añade una línea vacía**, y el `\r` se ignora. Un `Content >>` no
     termina en salto de línea (el parser lo quita), pero el texto de un `Inline: markdown` o el
     de un `Text` generado sí podría. *Dónde:* `PreformattedText.sourceLines`.

210. **`SvgCanvas` conserva los espacios con `xml:space="preserve"`** cuando un texto tiene dos
     espacios seguidos o empieza o acaba en espacio; el SVG es de depuración, y los textos
     normales no cambian. *Dónde:* `paint/SvgCanvas.text`.

211. **Código en Markdown con `Code font`** (hito 29, W4): una propiedad de `Markdown`, como `Bullet`
     (obligatoria solo si hay código: `MARKDOWN_PROPERTY_MISSING` con la línea del código, K6), no una
     propiedad de `Typography`, así que un texto con `Inline: markdown` no tiene código (sigue siendo
     `MARKDOWN_UNSUPPORTED`). *Dónde:* schema, `tree/MarkdownBuilder`. *Alternativa:* `Code font` en
     `Typography` (heredable, y valdría también en `Inline: markdown`).

212. **El código en línea es un tramo con otra familia** (`TextFormat.family`, `null` = la de la caja),
     no un elemento: se suma a la negrita y la cursiva del texto que lo rodea, puede ir dentro de un
     enlace y no cambia el alto de la línea ni la línea base, que son los de la fuente de la caja.
     Como en CSS, el espacio en blanco de un párrafo se colapsa también dentro del código en línea.
     *Dónde:* `model/TextFormat`, `layout/inline/PdfMetrics` (una familia desconocida es `FONT_UNKNOWN`
     con la línea del texto). *Alternativa:* un código en línea que conserve los espacios, o con la
     altura de línea de su fuente.

213. **Un bloque de código es un `Text` con `White space: pre`** y la fuente de `Code font`, con el
     código en `Content >>` en la línea de las vallas (o la anterior, si está sangrado), de modo
     que los errores de sus líneas señalan la correcta. No tiene `Id` ni `Class`, y las reglas de
     `Styles` solo lo alcanzan con `Element: Text`, que también es el `Text` de cada encabezado de
     un Markdown: un bloque de código no se puede estilar aparte (fondo, relleno) sin tocar los
     encabezados. *Dónde:* `MarkdownBuilder.codeBlock`. *Alternativa:* un elemento generado que las
     reglas `Element` nombren (`Code block`), como las entradas de un `Contents`. **Sustituida por la 285
     (hito 34): ahora es ese elemento.**

214. **La etiqueta de lenguaje de las vallas es un error** (```` ```java ````): no hay resaltado y no
     tendría efecto, como el título de un enlace o de una imagen. *Dónde:* `MarkdownBuilder.codeBlock`
     (`MARKDOWN_UNSUPPORTED`). *Alternativa:* ignorarla.

215. **Ejemplos** (hito 29): el IBAN y el BIC de la factura (01) son código en línea de un `Markdown` con
     `Code font` (el IBAN, en negrita: `**`…`**`), y el terminal del manual (08) es un solo `Text` con
     `White space: pre` en lugar de un `Grid` de doce `Area`; el borne `N` de un párrafo del manual es
     código en línea. El volcado de la factura no cambia y el del manual solo cambia en el alto del
     terminal (de 89 a 98 pt: la línea en blanco bajo el título mide 13 pt y el margen de antes, 4). Los
     valores de la tabla de especificaciones del manual siguen con `Class: mono`: una tabla de
     Markdown no admite columnas de ancho fijo.

216. **`Radius` es un radio por esquina, de 1 a 4 valores** (arriba izquierda, arriba derecha, abajo
     derecha, abajo izquierda, el abreviado de CSS). Un % horizontal es del ancho de la caja de borde y
     uno vertical, de su alto, así que `50%` da un círculo o una elipse; si los radios de un lado suman
     más que el lado, todos se reducen por el mismo factor (CSS). No hay dos radios por esquina
     (`10px / 20px`): la elipse se pide con %. *Dónde:* `model/Corners`, `model/Radii`,
     `tree/ValueReader.radius`. *Alternativa:* un solo valor, o la sintaxis `rx / ry` de CSS.

217. **Los radios dentro de la caja pierden el grosor del borde.** El fondo (en la caja de relleno) tiene
     los radios de la caja de borde menos el borde de cada lado, y una `Image` los de su caja de contenido
     (menos borde y relleno); una esquina que se queda sin radio es cuadrada (`Radii.inset`). Los radios de
     una caja partida con `Decoration: slice` solo están en las esquinas de su primer fragmento (arriba) y
     de su último (abajo), y los porcentajes se resuelven sobre el tamaño **del fragmento**, no el de la caja
     entera: un fragmento muy bajo reduce sus radios. *Dónde:* `paint/StackingContext.radii`.
     *Alternativa:* resolver los % contra la caja entera, que el layout no conoce al pintar.

218. **`Border color` admite de 1 a 4 colores** (arriba, derecha, abajo, izquierda), sin propiedades por
     lado. Los lados se encuentran en la diagonal de la esquina como en CSS; con esquinas redondeadas la
     diagonal sigue hacia dentro más allá de la esquina de relleno (dos veces el radio de dentro), para
     cubrir el trozo de anillo que entra en la caja de relleno (`Shapes.sidePolygon`). *Alternativa:*
     `Border top color`… (cuatro nombres más en el schema).

219. **Sin sombras** (`Shadow`). Se descartaron en el hito 28: eran lo más caro (PDFBox no desenfoca, habría
     que aproximar con capas semitransparentes) y lo que menos aporta. Las fichas con sombra del catálogo
     (09) siguen con su borde. *Alternativa:* `Shadow: x y desenfoque color`, exterior y única, con capas.

220. **`Background` admite `linear(ángulo, color, color…)`**, con el ángulo en grados como en CSS (0 hacia
     arriba, 90 hacia la derecha, 180 hacia abajo) y los colores repartidos a partes iguales; no hay paradas
     con posición, ni radiales, ni repetidos. La línea pasa por el centro y mide `|w·sen| + |h·cos|`, como
     en CSS. Es un sombreado axial de PDF (función de tipo 2, o de tipo 3 con más de dos colores) recortado
     a la caja; si algún color no es opaco, las opacidades van en una máscara suave de luminosidad con el
     mismo degradado en grises, porque un sombreado de PDF no tiene opacidad por color. *Dónde:*
     `model/Paint`, `paint/PdfCanvas.gradient`. *Alternativa:* solo dos colores, o paradas con posición.

221. **Los colores admiten opacidad** (`#RGBA`, `#RRGGBBAA`) en todas las propiedades de color, también en
     `Color` (texto) y `Rule color`, con un estado gráfico de PDF por opacidad (`ca`); sin propiedad
     `Opacity`. Un color liso con opacidad se pinta dentro de un `q`/`Q` propio. *Dónde:* `model/Colors`,
     `paint/PdfCanvas.painting`. *Alternativa:* `Opacity` por caja (que afectaría también a lo de dentro).

222. **Salida SVG de depuración:** las cajas redondeadas son `<path>`, el degradado un `linearGradient` en
     coordenadas de la página con `stop-opacity`, y el borde de un solo color un anillo (`evenodd`) o, con
     colores distintos, uno por lado con un `clipPath` (como en el PDF); la imagen redondeada, un `clipPath`
     alrededor de su `<svg>`. Una caja lisa y cuadrada sigue siendo un `<rect>`. *Alternativa:* `rx` y
     `ry` de `<rect>`, que no sirve para radios distintos por esquina.

223. **En el PDF, un borde liso, opaco y de esquinas cuadradas se pinta como antes** (un rectángulo por
     lado, los verticales encima), no como anillo: así no cambia ningún píxel de los PNG de los casos y los
     ejemplos, porque el anillo par-impar deja un antialiasing distinto en el borde de la caja. El anillo
     se usa con esquinas redondas, con opacidad y con colores distintos. *Dónde:*
     `paint/PdfCanvas.squareBorder`. *Alternativa:* el anillo siempre (cambian los PNG existentes).

224. **`Canvas` cambia:** `border(Rect, Insets, Radii, Sides<String>)` (un color por lado, ya resuelto)
     sustituye al de un color, `fill(Rect, Radii, Paint)` pinta el fondo de una caja (`background(Rect,
     String)` queda para los rectángulos lisos: guías, líneas entre columnas y rayas de texto) e
     `image(Box, Rect, Radii)` recibe las esquinas con que recortar. `Style.background` pasa a ser un
     `Paint` y `Style.borderColor` un `Sides<String>`. *Alternativa:* conservar las firmas y añadir
     métodos nuevos con implementaciones por defecto.

225. **Ejemplos** (hito 28): ocho documentos usan lo nuevo, y solo cambia un volcado: el del informe anual (04),
     por la caja `cover-fade` (un `Absolute` de 595 × 90 con `linear(180, #12315A00, #12315A)` sobre el pie de
     la foto de la portada, que funde el horizonte con el panel azul; el título sigue debajo de la foto, así
     que no hace falta el contraste que pedía la hoja de ruta). Currículum (03): foto circular y barras de
     nivel redondeadas; manual (08): insignias circulares y avisos con las esquinas de la derecha
     redondeadas; factura (01): igual en las tarjetas; horario (11): celdas redondeadas; catálogo (09): fichas,
     etiqueta «Novedad» y esquinas de arriba de la foto; carta (06): marcos y recuadro con esquinas; certificado
     (10): adornos de las esquinas circulares. El marco doble de la carta y los tres del certificado siguen
     siendo cajas distintas (una caja solo tiene un borde): lo que ganan es el radio. Las fichas del catálogo
     no tienen sombra (decisión 219). *Alternativa:* quitar los `Fixed` de los marcos y dejar uno solo.

## Documento de estilos aparte (2026-10-28)

No es un hito de la hoja de ruta: sale del primer punto de `TODO.md`. Interpretaciones y simplificaciones:

226. **La tipografía base va en `Element: Document`**, una regla más de `Styles` que solo admite
     `Typography` (con `Box` o `Break` es `STYLE_NOT_ALLOWED`), y `Document` deja de admitir `Typography`
     (un `Typography` suelto es `CHILD_NOT_DECLARED`, sin compatibilidad hacia atrás: todos los documentos de
     test, los ejemplos y los de la especificación se han migrado). Así la base de la herencia está en el
     mismo sitio que todo el estilo y se puede mover al documento de estilos aparte. *Dónde:*
     `tree/StyleRules.element` y `applyDocument`, schema. *Alternativa:* un `Typography:` propio de
     `Styles`, sin selector.

227. **La prioridad entre los dos orígenes es por nivel**: el orden de siempre (`For id` > `For class` >
     `For column` > posición de la celda > `Element`) no cambia, y dentro de cada nivel van antes las reglas
     del documento de estilos aparte y después las del documento, así que gana el documento. Sacar el
     `Styles` de un documento a un fichero no cambia ningún resultado (los PNG y los volcados de los once
     ejemplos son los mismos). En las reglas de posición de una celda el orden es: las del documento aparte
     (por posición) y luego las del documento (por posición). *Dónde:* `tree/StyleRules.apply`.
     *Alternativa:* todas las reglas del fichero valen menos que cualquiera del documento, aunque sea de un
     nivel inferior.

228. **Cada origen es estricto por separado.** Dos reglas con el mismo selector son `STYLE_DUPLICATE`
     dentro de un origen, no entre los dos; una clase de `Class` puede tener su `For class` en cualquiera de
     los dos. Un `For id` sin elemento **no es nunca un error**, ni en el documento aparte (sirve para varios
     documentos) ni en el propio (el elemento puede llegar después, de una plantilla: se seguirá
     https://stxt.dev/stxt-transform-ref#s4). Se quitó `STYLE_UNKNOWN_ID` y su caso del catálogo; antes del
     cambio, el documento sí lo exigía. *Alternativa:* exigirlo, lo que obliga a un fichero de estilos por
     documento y impide estilar elementos que rellenará una plantilla.

229. **Los errores de una regla del documento aparte llevan su línea y lo dicen** (`línea 8 de los estilos:
     …`), con el mismo código. Lo que se detecta después de construir el árbol (una fuente que no existe, un
     desbordamiento) señala la línea de la propiedad como siempre, sin decir en qué fichero está, porque
     `Style.line` solo guarda un número. *Dónde:* `InvalidModelException.inSource`, `StyleRules.located`.
     *Alternativa:* guardar el origen de cada línea en `Style` y el nombre del fichero (que el motor no
     conoce: recibe el árbol, no la ruta).

230. **El documento no nombra su documento de estilos ni el motor lo busca**: lo lee quien llama
     (`DocumentReader.readStyles`, de raíz `Styles` con el namespace; otra raíz es `DOCUMENT_ROOT`) y lo
     pasa al maquetar (`BoxTreeBuilder.build(documento, estilos)`, `Engine.layOutDocument(documento,
     estilos)`, y `Converter.builder().styles(estilos)` desde la decisión 294, donde antes era `PdfWriter.write`; `null` si no hay en
     las dos primeras). Los tests de ejemplos y de
     casos golden usan el `styles.stxt` de la carpeta si existe. El mismo nodo `Styles` del schema sirve de
     hijo de `Document` y de raíz, porque un schema acepta cualquier nodo como raíz.
     *Alternativa:* una propiedad `Styles file` en `Document`.

231. **Mensajes de `TEXT_WITHOUT_FONT` y `TEXT_WITHOUT_FONT_SIZE`:** dicen ahora «de un antecesor o de Styles
     (Element: Document)» en vez de «o de Document».

## Hito 30: rotación y papel con nombre (2026-10-28)

Las decisiones O1–O5 de la hoja de ruta se tomaron con un cuestionario; estas son las interpretaciones que
salieron al implementarlas.

232. **`Paper`, no `Size`.** La propuesta era `Size: A4`, pero `Size` ya es el tamaño de la fuente
     (`Typography`) y cada nombre tiene una sola definición en el namespace. Los tamaños son los de ISO 216
     redondeados a puntos enteros, como se usan siempre (A4 = 595 × 842, A3 = 842 × 1191, A5 = 420 × 595,
     A6 = 298 × 420, B4 = 709 × 1001, B5 = 499 × 709), y Letter y Legal, exactos. *Dónde:* `tree/Media.Paper`,
     schema. *Alternativa:* los valores exactos con decimales (A4 = 595,28 × 841,89), que no coinciden con los
     documentos que ya escriben 595 × 842.

233. **El tamaño del medio se declara de una sola forma, y los errores son `MEDIA_SIZE`.** En el schema,
     `Paper`, `Orientation`, `Width` y `Height` pasan a ser opcionales (como mucho uno); el motor exige `Paper` o
     `Width` y `Height`, nunca las dos cosas, y `Orientation` solo con `Paper`. No hay código nuevo: es el
     tamaño del medio. El caso del catálogo «medio sin alto» pasa de `TOO_FEW_CHILDREN` (del schema) a
     `MEDIA_SIZE`, con la línea del `Width`. *Dónde:* `BoxTreeBuilder.media`. *Alternativa:* un código propio
     (`MEDIA_PAPER`), o dejar que `Paper` sustituya a `Width` y `Height` en silencio.

234. **`Rotate` es una propiedad del elemento**, como `Offset`: no va en `Styles` ni en un grupo, no se
     hereda (aunque lo que contiene la caja gira con ella) y solo la admiten `Relative`, `Absolute`, `Fixed` e
     `Image`. Un `Text` no gira: se gira el bloque que lo contiene (`CHILD_NOT_DECLARED`). *Dónde:* schema,
     `BoxTreeBuilder.element`, `Style.rotation`. *Alternativa:* en `Box`, para poder darlo con una clase.

235. **Ángulo:** un `NUMBER` del schema de −360 a 360 grados; `0` (que no gira nada) y lo de fuera son
     `VALUE_NOT_ALLOWED`. 360 y −360 se admiten aunque dejen la caja como estaba. *Dónde:*
     `ValueReader.angle`. *Alternativa:* normalizar a (−180, 180] o rechazar las vueltas enteras.

236. **`Rotate origin`:** una o dos palabras clave en cualquier orden (`top left` = `left top`); el eje que
     no se nombra es `center`, y `center center` vale. Dos del mismo eje, o algo que no es una palabra clave
     (puntos, porcentajes), es `VALUE_NOT_ALLOWED`; sin `Rotate`, `ROTATE_ORIGIN_WITHOUT_ROTATE`. *Dónde:*
     `model/Rotation.origin`, `ModelValidator.validateRotation`.

237. **Lo girado se comprueba como se pinta.** El rectángulo que envuelve la caja de borde girada, con los
     giros de sus ancestros compuestos, debe estar dentro de la página física (`BOX_OVERFLOW` en la línea de
     su `Rotate`). Se usa la posición pintada, con el `Offset` de un `Relative` incluido, aunque el
     `Relative` se compruebe en su sitio sin él: lo que importa es que lo girado se vea. Un `Absolute` o un
     `Fixed` girado se comprueba solo así (puede salirse sin girar y caber girado); uno sin girar dentro de
     una caja girada, también así, en la línea del elemento. Las cajas en flujo de dentro de una girada se
     siguen comprobando contra su padre sin girar, que es lo que las contiene. *Dónde:* `layout/Overflow`.
     *Alternativa:* comprobar solo la caja girada y no lo que cuelga de ella.

238. **Un `Relative` girado no se parte.** `Engine.layout` lo maqueta sin fragmentación y, si no cabe en lo
     que queda de página, devuelve `BEFORE` (como una imagen); al principio de una página se coloca, y si es
     más alto que el área útil, `PAGE_OVERFLOW` en la línea de su `Rotate` (lo que impide partirlo es el
     giro, no un hijo). Los cortes forzados (`Before: page`) de sus descendientes no tienen efecto, como
     dentro de un `Absolute`. `Inside` y `Decoration` en él son `BREAK_IN_ROTATED`, también desde una regla.
     *Alternativa:* partirlo y girar cada fragmento sobre su propio centro (un sello partido en dos páginas no
     tiene sentido), o un error para los cortes forzados de dentro.

239. **Pintado.** El giro envuelve todo el contexto de apilamiento de la caja girada (también sus
     posicionados y los contextos con `Layer` de dentro, que se pintan dentro del giro); una `Image` en flujo
     se gira en sus dos pasos (fondo y borde, y contenido). El orden de pintado no cambia. En PDF es un
     `q … cm … Q` (el texto sigue siendo texto y el PDF etiquetado no cambia); en SVG, un
     `<g transform="rotate(…)">`. El contorno de depuración también gira. *Dónde:* `StackingContext.paint`,
     `Canvas.beginRotation`, `PdfCanvas`, `SvgCanvas`.

240. **Enlaces, figuras y destinos.** La anotación de un enlace girado tiene como `Rect` el rectángulo que
     envuelve la zona girada y como `QuadPoints` la zona girada, en sentido antihorario desde abajo a la
     izquierda; el `BBox` de una `Figure` girada y el destino (marcadores y enlaces internos) de una caja
     girada, o de dentro de una girada, son los del rectángulo que la envuelve. *Dónde:* `PdfCanvas.link`,
     `PdfCanvas.image`, `PdfDestinations`. *Alternativa:* solo el envolvente, sin `QuadPoints`.

241. **El volcado (§7) no cambia:** el `Rect` de una caja girada es el de sin girar, como el de un enlace o un
     fondo, porque el giro no cambia el layout. Todos los documentos de muestra en A4 (los ejemplos salvo la
     carta del restaurante, 06, que es de 420 × 700, `test/spec/report.stxt` y los ejemplos de la
     especificación y del README) usan `Paper: A4`; los tests y los casos golden siguen con `Width` y
     `Height`, porque prueban medidas concretas.

## Hito 31: imágenes vectoriales (SVG) (2026-10-28)

Las decisiones V1–V6 de la hoja de ruta se tomaron con un cuestionario (todas con la propuesta); estas son las
interpretaciones que salieron al implementarlas.

242. **El SVG se reconoce por el contenido: un fichero que empieza por `<`** (tras la marca de orden de bytes
     de UTF-8 y los espacios). Un XML que no es SVG (raíz distinta, o sin el espacio de nombres de SVG) es
     `IMAGE_FORMAT` con un mensaje propio. El mensaje de los que no son PNG, JPEG ni SVG pasó a decirlo
     (`no es PNG, JPEG ni SVG`). *Dónde:* `ImageFile.read`. *Alternativa:* exigir la extensión `.svg`, contra
     lo que ya se hace con PNG y JPEG (§5.18).

243. **Subconjunto cerrado y estricto, con la línea del SVG en el mensaje.** Elementos: `svg` (sin anidar), `g`,
     `defs`, `title`, `desc`, `rect`, `circle`, `ellipse`, `line`, `polyline`, `polygon`, `path`,
     `linearGradient`, `radialGradient`, `stop`, `clipPath`, `use`, `text` y `tspan`. Todo lo demás (también
     `style`, `symbol`, `metadata`, `switch` y los elementos de otros espacios de nombres, como los de
     Inkscape), un atributo de otro espacio de nombres (salvo `xlink` y `xml`), un manejador `on…` y las
     propiedades `filter`, `mask` y `marker*` (del atributo o de `style`) son `IMAGE_FORMAT`: la política es
     la del resto del motor, nada se ignora en silencio. El precio es que un SVG exportado de un editor suele
     necesitar limpiarse. *Dónde:* `SvgInspector`. *Alternativa:* ignorar lo que JSVG ignora, o admitir
     `metadata` y los atributos de editor (`inkscape:`) como inertes.

244. **Referencias: solo internas.** Un `href` (también `xlink:href`) y un `url(…)` (en un atributo o en
     `style`) que no empiecen por `#` son errores; un `href` solo tiene sentido en `use` y en los degradados.
     Un DTD, una entidad o un `XInclude` también (el parser SAX los desactiva), y JSVG carga con
     `ResourcePolicy.DENY_ALL` por si algo se le escapara a la validación. *Dónde:* `SvgInspector`,
     `PdfImages.draw`.

245. **Tamaño natural.** `width` y `height` con unidad absoluta (`px`, `pt`, `pc`, `in`, `cm`, `mm`; sin
     unidad, píxeles) o, si faltan, el `viewBox`; con un solo lado y `viewBox`, el otro sale de la
     proporción; en `%` o `em` no hay tamaño natural. Después, a 96 ppp como los PNG. `ImageFile.pixelWidth()`
     pasó de `int` a `double`. *Dónde:* `SvgInspector.Handler.size`/`result`. *Alternativa:* exigir `width`,
     `height` y `viewBox`.

246. **Texto: la primera familia de `font-family`, sin sustitución.** Solo cuenta la primera familia de la
     lista (`'IBM Plex Sans', sans-serif` es `IBM Plex Sans`) y no puede ser genérica (`sans-serif`…), porque
     habría que sustituirla; `font-weight` solo `normal`/`400` o `bold`/`700` y `font-style` solo
     `normal` o `italic`, propios o heredados de un `g`; la variante debe existir en `fonts/` y tener todos los
     caracteres del texto. Sin `font-family` es un error (no hay fuente por defecto, §5.11). *Dónde:*
     `ImageFile.checkText`. *Alternativa:* probar las familias de la lista en orden, como CSS.

247. **El texto de un SVG no es texto del PDF.** La propuesta (V4) suponía que con `pdfbox-graphics2d` quedaría
     seleccionable, pero JSVG dibuja los glifos como contornos (`fill(Shape)`), así que en el PDF son rutas
     con la forma de la fuente de `fonts/`: se ve igual y no se puede seleccionar ni se extrae. Para que JSVG
     encuentre la fuente por su familia hay que darla de alta en AWT (`GraphicsEnvironment.registerFont`),
     lo que es global a la JVM; solo se hace si algún SVG tiene texto, una vez por fichero. Lo habitual en
     un SVG de un documento accesible es que su texto vaya en el documento y el SVG sea un dibujo, y `Alt`
     da el texto alternativo. *Dónde:* `PdfImages.registerFonts`. *Alternativa:* una `Output` propia de JSVG
     (`TextOutput`) que escriba el texto con `drawString` y un `PdfBoxGraphics2DFontTextDrawer` con las
     fuentes registradas (en `TODO.md`). *Cambiada por la 308:* ya no se da nada de alta en AWT.

248. **Un SVG es un formulario del PDF y se escala a su caja.** `PdfBoxGraphics2D` dibuja el SVG a su tamaño
     natural en puntos (un formulario de `file.width()` × `file.height()`), una vez por fichero, y
     `PdfCanvas.image` lo coloca con una matriz `cm` de escala a donde dice `ImageFit` (`contain`, `cover` y
     `fill` valen igual que con un PNG; `fill` deforma el dibujo) dentro del recorte de la caja de contenido
     con sus esquinas. El PDF etiquetado trata la imagen igual: `Figure` con `Alt`, *artifact* sin él.
     *Dónde:* `PdfCanvas.image`, `PdfImages.drawing`.

249. **Un formulario hereda el estado gráfico, y los guiones del modo `debug` se le colaban.** Al revisar el PNG
     del caso golden, los trazos del SVG salían a trazos discontinuos: el modo `debug` deja activo el patrón
     de guiones del contorno de las cajas, y `pdfbox-graphics2d` no fija los guiones si el trazo no los
     tiene. `PdfCanvas.image` los restablece (`[] 0 d`) dentro de su `q … Q` antes del `Do`. Los demás
     parámetros (ancho, remate, unión, colores) los escribe el dibujo en cada trazo. *Dónde:*
     `PdfCanvas.image`. *Alternativa:* envolver cada contorno de depuración en su propio `q … Q`.

250. **Los ejemplos con SVG y el generador.** Los logotipos, las firmas, el sello, los tres gráficos, el
     emblema, el adorno, el termostato y el esquema de conexión de los ejemplos pasan a SVG con el mismo
     tamaño natural que tenían los PNG (así ningún volcado cambia): `tools/ExampleImages.java` tiene una
     clase `Svg` con lo justo de Java2D (rellenos, trazos, un degradado lineal y texto) y escribe las
     mismas figuras que antes dibujaba en un `BufferedImage`. Las fotos, la pantalla del móvil (08) y el
     icono del horario (11) siguen siendo PNG o JPEG. El recorte `AlphaComposite.Clear` del anillo de
     clientes pasó a ser una resta de áreas. *Dónde:* `tools/ExampleImages.java`,
     `resources/images/examples/`.

251. **Dependencias nuevas:** JSVG 2.2.0 (`com.github.weisj:jsvg`, sin dependencias propias) y
     `pdfbox-graphics2d` 3.0.5 (`de.rototor.pdfbox:graphics2d`, sobre el PDFBox que ya se usa). *Alternativa
     descartada:* Apache Batik, que arrastra decenas de jars.

252. **El `Link` del PDF etiquetado envuelve lo que se pulsa** (sustituye a la decisión 171). Una caja con
     `Link` tiene un `Link` hijo de su elemento que contiene su texto y los elementos de sus hijos
     (`Span → Link → texto`, `TOCI → Link → título, número`) y recibe una `OBJR` por fragmento; una
     imagen es la excepción y su `Link` es el padre de su `Figure`, porque una `Figure` no contiene a su
     `Link`. Un enlace dentro del texto (Markdown, `Inline: markdown`) es un `Link` hijo del elemento del
     texto con el contenido marcado de sus palabras: el texto se pinta en secuencias de contenido
     marcado (`MCID`) seguidas con el mismo destino, así que el párrafo queda `P → [texto, Link, texto]`.
     *Dónde:* `paint/PdfStructure`, `paint/StackingContext.paintText`, `paint/Canvas.beginLinkText`,
     `paint/PdfCanvas`. *Alternativa:* dejarlo como estaba (un `Link` con solo la `OBJR`), que no cumple
     PDF/UA.

253. **Un enlace de varias líneas es un solo `Link`** por enlace y fragmento, con un `MCID` y una `OBJR`
     por línea (las anotaciones siguen siendo una por línea, que es lo que el visor necesita para el
     rectángulo). Un lector de pantalla anuncia un enlace, no uno por línea. Dos enlaces con el mismo
     destino y seguidos, sin texto entre ellos, son uno. *Dónde:* `paint/StackingContext.paintText`.
     *Alternativa:* un `Link` por línea.

254. **Sin `Alt` si el `Link` contiene algo que leer.** Con `Alt`, un lector de pantalla lo leería en lugar
     del contenido del enlace. Solo lleva `Alt` el `Link` sin texto ni figura dentro (un bloque vacío con
     `Link` y un fondo) y el suelto de la decisión 255; el texto de la anotación (`Contents`), que PDF/UA
     exige, se queda siempre. *Dónde:* `paint/PdfStructure.hasContent`, `emptyLinks`.

255. **Los enlaces de lo que no es contenido van a un `Link` suelto del `Document`**, con su `Alt` y sin
     contenido marcado: un `Fixed`, una imagen decorativa con `Link` y las filas repetidas de una tabla desde
     su segunda página. La decisión 171 los dejaba sin etiquetar y la anotación incumplía PDF/UA (7.18.5-1:
     una anotación `Link` debe estar en un `Link`). El pie de cada página con su web se anuncia en cada una
     (es lo que una persona que ve la página también encuentra). *Dónde:* `paint/PdfStructure.outside`.
     *Alternativa:* marcar las anotaciones como ocultas, que las quita del ratón.

256. **`Tabs: S` en las páginas con anotaciones**, para que el visor recorra los enlaces en el orden de la
     estructura (PDF/UA, 7.18.3-1). *Dónde:* `paint/PdfCanvas.write`.

257. **veraPDF en los tests** (`org.verapdf:validation-model` 1.28.2, alcance `test`): `ExamplesTest`
     valida el PDF de cada ejemplo con el perfil PDF/UA-1 y falla con cualquier regla incumplida salvo las
     de `KNOWN_UA_FAILURES`, hoy solo la 5-1 (el XMP no lleva el identificador `pdfuaid:part`, es decir, no
     se afirma la conformidad). Así una regresión de accesibilidad en la estructura rompe el build.
     *Dónde:* `golden/PdfUa`, `examples/ExamplesTest`. *Alternativa:* validar a mano de vez en cuando.

258. **Un `Text` de bloque es un `P` en el PDF etiquetado**, no un `Span`: un `Span` es texto en línea, un lector
     de pantalla no distingue dónde acaba y PDF/UA-2 no admite un `Span` directo bajo `Document` o `Div`.
     Solo es un `Span` el `Text` hijo de un encabezado (parte del texto del `H1`…`H6`) y las dos piezas de una
     entrada de índice, que ya lo eran. Un `Text` con varios párrafos (línea en blanco) es un solo `P`: el
     contenido marcado no se parte por párrafos. Solo cambia la etiqueta: ningún volcado ni PNG. *Dónde:*
     `paint/PdfStructure.type`. *Alternativa:* dejar `Span` y que lo decida una propiedad del `Text`.

259. **`Language` también en `Typography`**: el idioma de una caja que no está en el del documento (se hereda y
     vale en `Styles`). Reutiliza el nombre y la definición de `Language` de `Document` (misma etiqueta BCP 47,
     mismo `VALUE_NOT_ALLOWED`) y fija `Style.locale`, así que también cambia las reglas de las letras de
     `Transform` de lo que contiene. En el PDF etiquetado es el `Lang` del elemento de la caja, solo donde
     difiere del de la caja de fuera (el árbol de estructura lo hereda). `Element: Document` no lo admite
     (`STYLE_NOT_ALLOWED`): el idioma del documento es uno solo y es el de `Document`. No hay idioma por tramo
     dentro de una línea ni en Markdown. *Dónde:* schema, `tree/PropertyReader.typography`, `tree/StyleRules`,
     `paint/PdfStructure.build`. *Alternativa:* `Language` por tramo (`TextFormat`), que parte el contenido
     marcado.

260. **Espacio invisible al final de las líneas partidas.** El corte de línea se come el espacio entre la última
     palabra y la primera de la línea siguiente, y en el contenido del PDF las líneas son textos separados: un
     lector que los une (lector de pantalla, extracción, búsqueda) pegaba las palabras («vientoprestado»,
     «elde»). `StackingContext.paintText` añade un espacio al último tramo de cada línea que no es la última de
     su fragmento, si `White space` es `normal`; no cambia el ancho de la línea (el espacio sale del borde
     derecho, invisible), el layout ni los PNG. En `pre` y `pre-wrap` no, que no se come espacios y sus líneas
     son líneas. *Dónde:* `paint/StackingContext.paintText`. *Alternativa:* dejarlo a la geometría, como
     PDFTextStripper.

261. **El PDF declara PDF/UA-1 si tiene `Title`.** El XMP lleva `pdfuaid:part` = 1 (regla 5-1 de veraPDF) en los
     documentos con `Title`; sin él no se declara nada (ni siquiera hay XMP), porque sin título no se cumple la
     7.1 y no se puede afirmar. Es una afirmación que veraPDF solo comprueba en parte (no el orden de lectura
     ni el sentido de los textos), así que se apoya en el repaso a mano de las decisiones 252–260 y en los
     huecos que quedan apuntados en `TODO.md` (cabeceras de fila, títulos reales de portada e índice, idioma
     por tramo): un documento puede ser PDF/UA-1 según veraPDF y mejorable para un lector de pantalla.
     `ExamplesTest.KNOWN_UA_FAILURES` queda vacío. *Dónde:* `paint/PdfStructure.xmp`. *Alternativa:* no
     declararlo.

262. **`Title` es obligatorio en `Document`** (exactamente uno, como `Language` y `Media`). El visor muestra el
     título en lugar del nombre del fichero y PDF/UA-1 (7.1) lo exige; con la decisión 261 todo PDF declara
     PDF/UA-1, y un documento sin título no podría afirmarlo. Falta de `Title` es `TOO_FEW_CHILDREN` (línea de
     `Document`). Todos los documentos de tests, golden, catálogo de errores y ejemplos de la documentación lo
     llevan (`Title: Prueba` o `Informe` tras `Language`; en el catálogo, la línea esperada se desplazó una
     cuando el error cae después). *Dónde:* schema (`Min: 1`), `tree/Metadata`, `paint/PdfStructure`.
     *Alternativa:* dejarlo opcional y no declarar PDF/UA sin él.

263. **`Header: true` en una `Cell`**: la celda es la cabecera de su fila (el concepto de una factura, la hora de
     un horario), `TH` con `Scope: Row` en el PDF etiquetado, para que un lector de pantalla la anuncie con cada
     celda de la fila. Se escribe en cada celda, no en la tabla, que es lo más flexible (la etiqueta de un pie de
     totales, o la tercera columna de una agenda) a costa de repetirlo; es del elemento y no va en `Styles`
     (como `Span` y `Link`). Vale en `Body` y `Foot`; en `Head` es un error (`TABLE_HEADER`, línea del `Header`)
     porque no tendría efecto: sus celdas ya son `TH` con `Scope: Column`. Las tablas de `Markdown` no tienen
     cabeceras de fila (GFM no las tiene). Los ejemplos 01, 04, 05, 07, 08, 09 y 11 la usan. *Dónde:* schema,
     `tree/Style.header`, `tree/BoxTreeBuilder`, `paint/PdfStructure`. *Alternativa:* `Header columns: N` en la
     `Table`, que no repite pero solo sirve para columnas enteras.

264. **`In contents: false` en un encabezado** lo deja fuera de los índices (`Contents`) sin dejar de ser un
     encabezado: sigue siendo `H1`…`H6`, marcador, parte de la jerarquía (la comprueba en orden del documento) y
     destino de `#id`. Es una propiedad del elemento (`Heading 1`…`6`), no de `Styles`, por defecto `true` y no
     se hereda. El nombre no puede ser `Contents` (ya es un elemento y cada nombre del namespace tiene una sola
     definición). Los títulos de la portada y del índice de los ejemplos 04 y 09 pasan de `Text` a `Heading 1`
     con `In contents: false`, sin cambiar un píxel; la regla `For class: page-title`, que solo repetía
     `Element: Heading 1`, se quita. *Dónde:* schema, `tree/Style.inContents`, `tree/BoxTreeBuilder`,
     `tree/ContentsBuilder.collect`. *Alternativa:* `Levels` por encabezado, que no sirve cuando la portada y los
     capítulos son del mismo nivel.

265. **`Section` es el único contenido de `Document`** (hito 32). Un documento tiene `Media`, `Title`,
     `Language`, `Styles`, `Fixed` y una o más `Section` (`Min: 1`); el flujo y los demás elementos posicionados
     van dentro. No hay sección implícita para el contenido suelto: así la separación está siempre a la vista, y
     migrar un documento es mecánico (envolver). *Dónde:* schema. *Alternativa:* aceptar contenido suelto como
     una sección implícita.

266. **El `Id` de una sección es un hijo `Id`**, no el valor: en este schema el valor de un elemento es su
     contenido (la ruta de `Image`, la colocación de `Area`) y el `Id` ya es una sola definición para todo el
     namespace (`Link: #id`, `{page of: id}`, `For id`). *Alternativa:* `Section: nombre`.

267. **`Media` de una sección hereda por grupos del `Media` del documento:** el tamaño (`Paper` +
     `Orientation`, o `Width` + `Height`) es un grupo y `Margin` otro; cada uno se hereda entero o se declara
     entero. Así `Orientation: landscape` sola da el papel del documento apaisado, y si el documento declara
     `Width` y `Height` esa sección no puede girarlo (`MEDIA_SIZE`). *Dónde:* `tree/BoxTreeBuilder.media`.
     *Alternativa:* `Media` completo en cada sección.

268. **`Fixed` del documento y de la sección.** El del documento sale en todas las secciones (con el área útil
     de cada página), el de la sección solo en las suyas, y `Pages` cuenta dentro del alcance donde se declara.
     `Inherit fixed: false` deja una sección sin los del documento. Que sobre (sin `Fixed` en el documento) no es
     un error, como las propiedades que sobran de `Markdown` (decisión 130): una plantilla puede ponerlos
     después. *Dónde:* `layout/Paginator`, `tree/Style.pages`. *Alternativa:* `Pages` siempre físico.

269. **La numeración es un contador por documento:** `Page numbering: continue` (por defecto), `restart` (vuelve a
     1) o `none` (la página no tiene número y el contador no avanza). `{page}` es el valor del contador y
     `{pages}`, el último de su tanda, la que empieza en cada `restart`: con una portada `none`, la primera página
     interior dice «1 de N». El nombre no puede ser `Numbering` (ya es una propiedad de `Markdown`).
     *Dónde:* `tree/Style`, `layout/Paginator`. *Alternativa:* un `Start` para empezar en otro número (no se hace
     por ahora, `TODO.md`).

270. **Lo que se imprime manda.** `{page of: id}` y las entradas de los índices dan el número del contador, no el
     de la página física, para que el índice y el pie coincidan. Una referencia a una caja de una sección `none`
     es `REFERENCE_NOT_PLACEABLE`, y un texto con `{page}` o `{pages}` que cae en una página sin número (un
     `Fixed` del documento en una portada) es `PAGE_NUMBER_UNAVAILABLE`, con la línea del texto: nada de dejarlo
     en blanco. Los marcadores y los enlaces siguen la página física; las `PageLabels` del PDF llevan la
     numeración impresa. *Alternativa:* dejar el hueco vacío.

271. **Una `Section` admite `Id`, `Class`, `Media`, `Page numbering`, `Inherit fixed` y su contenido.** Las reglas
     `For class` y `For id` que le llegan solo pueden dar `Typography`, que hereda su contenido (un anexo con otro
     `Language`), como `Element: Document` pero por sección; con `Box` o `Break` es `STYLE_NOT_ALLOWED`. Sin
     `Background` de página ni `Start`. *Dónde:* `tree/StyleRules`.

272. **Una sección sin contenido es `SECTION_EMPTY`** (línea de la sección): sin elementos, o solo con `Fixed`
     (que no ocupan sitio), no tiene ninguna página, y una página en blanco que nadie escribió es un valor por
     defecto silencioso. Un `Absolute` sí cuenta: una portada hecha solo de cajas posicionadas es una sección
     válida con su página. *Alternativa:* una página en blanco.

273. **Una sección no se vuelca como caja** (§7) aunque tenga `Id`: es la página, no una caja; se vuelcan sus
     cajas con `Id` con las coordenadas de la página de su sección. Su `Id` sí es un destino de `Link` y de
     `{page of: id}` (la primera página de la sección). *Alternativa:* volcar el rango de páginas.

274. **El documento sigue siendo uno** para la jerarquía de encabezados, `Contents` y `Styles`: se comprueban en
     el orden del documento, atravesando las secciones (un `Heading 1` en cada sección es correcto). Cada
     sección es la raíz de su layout (una `Box` sin padre, como la raíz anónima de antes) y cuelga de la raíz del
     documento, que ya no se maqueta. *Dónde:* `tree/BoxTreeBuilder`, `layout/Paginator`.

275. **Una sección es la raíz de su layout, sin padre, y cuelga de la raíz del documento sin ser hija.** Es una `Box`
     (`Element.SECTION`) como la raíz anónima de un documento de una sola sección, así que el layout, la caché,
     `establishesBlockFormattingContext`, subir por `parent` hasta el containing block de un `Fixed` y todo lo que
     ya miraba `parent == null` siguen igual. Se añade a `root.children` (no con `addChild`) para que los recorridos
     del árbol entero (referencias, enlaces, índices, fuentes, imágenes, `Id`, el `PdfStructure`) la vean sin
     tocarlos. Lo que se rompe es la invariante «el padre de mi hijo soy yo»: solo vale para las secciones y
     para nadie más. *Dónde:* `tree/BoxTreeBuilder.build`, `layout/Paginator`. *Alternativa:* pasar a los
     recorridos la lista de secciones.

276. **Un `Fixed` del documento se coloca en `(0, 0)` del área útil de cada página** (su posición estática, ya que no
     está en ningún flujo) y se pinta antes que los de la sección: el orden de apilamiento entre un `Fixed` del
     documento y uno de una sección se decide sin árbol común (los del documento, primero). Los dos escriben en la
     misma lista de `Page.fixed`. *Dónde:* `layout/Paginator`, `paint/StackingContext.compareTreeOrder`.

277. **`{page of}` y los pies usan el contador; los marcadores, enlaces y `Pages` del documento, la página física.**
     Se calcula una vez por pasada de paginación (`Numbering`), con la tanda de cada página para `{pages}`; el
     contador no avanza en una sección `none`, así que una sección `continue` detrás de una portada empieza en 1.
     Un `Fixed` de una sección cuenta sus `Pages` con las páginas de la sección, aunque esas páginas sean las
     4 a 6 del documento. *Dónde:* `layout/Paginator.Numbering`.

278. **El PDF escribe un `Sect` por sección y las `PageLabels` solo si alguna página no es su número físico.** Un `Sect`
     lleva `Lang` si su idioma cambia respecto al del documento (el de la raíz). Las etiquetas: `decimal` desde el
     número impreso donde el contador salta, y sin estilo (etiqueta vacía) en las páginas sin número; un documento
     de una sola sección que numera como siempre no cambia ni un byte de estructura. Las páginas del PDF tienen el
     tamaño del `Media` de su sección. *Dónde:* `paint/PdfCanvas.write` y `labels`, `paint/PdfStructure`.

279. **La portada de los ejemplos 04 y 09 es una `Section` con `Relative`.** Era un `Absolute` que salía al margen y un
     bloque de 1 pt con `Break: After: page`; ahora es una sección con `Margin: 0`, `Page numbering: none` e
     `Inherit fixed: false`, y un `Relative` del tamaño de la página que hace de containing block de sus
     `Absolute` (lo que era el propio `Absolute`). Los volcados solo pierden el `cover-spacer`; los PNG de la
     portada no cambian y los de las páginas interiores dicen «Página 1 de 6» y el índice, los números impresos. El
     ejemplo 07 (contrato) no cambia: no tiene portada y su `Pages: not-first` sigue valiendo.

280. **`Numbered` es una propiedad de `Typography`, booleana y heredable** (hito 33). Una propiedad propia del
     encabezado no se podría escribir en una regla `Element: Heading 2`, porque las reglas solo admiten `Box`,
     `Typography` y `Break`; y de `Typography` sale gratis la herencia: `Element: Document` numera todo, una regla
     por nivel o por clase numera lo que nombre, y `Numbered: false` en el `Typography` de un encabezado lo deja
     fuera (la portada, el título del índice). Se llama `Numbered` y no `Numbering`, que ya es de `Markdown`
     (marcadores de las listas numeradas): cada nombre tiene una sola definición en el namespace. Un `Text` o un
     `Block` que hereda `Numbered: true` no hace nada: solo lo lee un encabezado. *Dónde:* schema, `tree/Style`,
     `tree/PropertyReader`. *Alternativa:* `Numbering: decimal` en cada regla, con formato por nivel.

281. **Solo decimal con puntos (`1`, `1.1`, `1.1.1`).** Un contador por nivel que sube con cada encabezado numerado
     de ese nivel y vuelve a 0 con cualquier encabezado de un nivel superior, numerado o no. Cuenta a través de las
     `Section` y de los `Markdown` (el documento es uno, decisión 274). Quedan fuera los formatos por nivel
     (romanos, letras para los anexos) y los prefijos y sufijos propios (`Anexo A.`, `§ 4.2`).
     *Dónde:* `tree/HeadingNumbers`. *Alternativa:* un formato por nivel con los valores de `Marker`.

282. **Los encabezados sin numerar no aportan nivel al número de los de debajo.** El título de un documento es un
     `Heading 1` sin número y sus secciones, `Heading 2` numerados: son `1`, `2`, `3` (y los `Heading 3`, `2.1`),
     no un error ni `0.1`. El número de un encabezado de nivel N son los contadores de los niveles 1 a N que
     están numerados; un nuevo encabezado de un nivel superior, numerado o no, reinicia los contadores de debajo.
     *Revisada el mismo día:* la primera versión era un error (`HEADING_NUMBER_PARENT`, un numerado necesita que
     su padre lo esté), pero el ejemplo 07 (un título y sus secciones) es el caso normal y chocaba con ella.
     *Dónde:* `tree/HeadingNumbers.next`. *Alternativa:* el error, que obliga a numerar el título o a quitarlo de los
     encabezados.

283. **El número es parte del texto del primer `Text` del encabezado**, escrito delante con un espacio, no una caja
     generada como el marcador de una lista. Así lo miden el layout y el reparto en líneas, lo ven el índice, los
     marcadores y el PDF etiquetado sin tocar nada, y un encabezado largo envuelve bajo el número (no hay sangría
     francesa). Consecuencias asumidas: el espacio es normal y puede cortar la línea justo después del número;
     `Transform` no lo cambia (son dígitos), `Letter spacing` y `Small caps` sí lo afectan; `ActualText` del PDF
     etiquetado lo incluye. *Dónde:* `tree/HeadingNumbers.prefix`. *Alternativa:* una caja `HEADING_NUMBER`
     colgada a la izquierda, como `MarkerLayout`, que alinearía los títulos pero obligaría a un hueco fijo.

284. **`{number of: id}` de un encabezado da su número completo (`4.2`)**, el mismo que lleva escrito, y no el de su
     último nivel como el de un `List item`. Es el uso que pidió el usuario («la sección 4.2»). Un encabezado sin
     numerar sigue siendo `REFERENCE_NOT_NUMBERED`. Los encabezados de un `Markdown` se numeran pero no se pueden
     referir (no llevan `Id`). *Dónde:* `tree/References.validate`, `Box.number`.

## Hito 34: el bloque de código como elemento generado (2026-10-28)

285. **El bloque de código de un `Markdown` es el elemento generado `Code block`** (`Element.CODE_BLOCK`), que las
     reglas `Element` nombran como las entradas de un `Contents` y que no se escribe en el documento (elegido por
     cuestionario; las alternativas eran un elemento también escribible y una clase automática `code`, que
     reservaría un nombre de clase al autor). Sigue siendo un texto (`Layout.INLINE`, lleva texto, un `P` en el
     PDF etiquetado) con la fuente de `Code font` y `White space: pre`, sin caja envoltorio: su `Box` (fondo,
     relleno, márgenes) lo da `Element: Code block`. **`Element: Text` ya no lo alcanza**, y un `Text` escrito a
     mano con `White space: pre` tampoco es un `Code block`. `MarkdownBuilder` marca el nodo `Text` que genera
     (`Translation.codeBlocks`) y `BoxTreeBuilder.element` le da el elemento; el nodo sigue validando contra el
     schema como un `Text`. *Dónde:* `tree/Element`, `tree/MarkdownBuilder`, `tree/BuildContext`,
     `tree/BoxTreeBuilder`, `paint/PdfStructure`. *Alternativa:* un `Code block` también escribible.

## Hito 35: `Class` en la `Row` (2026-10-28)

286. **El `Class` de una `Row` aplica sus reglas `For class` a cada una de sus celdas** (elegido por cuestionario; las
     alternativas eran que `Row` admitiera además un `Box`, que se repetiría en cada celda —un `Border` o un
     `Padding` de fila saldría entre celdas—, y selectores por posición de fila, `For row` o `Last row cell`, que
     identifican la fila de totales por su sitio y no por lo que es). La fila no pasa a ser una caja: no tiene
     `Box` ni `Id` (no se puede enlazar a una fila ni darle estilo con `For id`). *Dónde:* schema (`Row`),
     `tree/CellPosition.rowClass`, `tree/TableBuilder`, `tree/StyleRules.classRules`. *Alternativa:* `Box` en la
     `Row`; un fondo de fila se da a cada celda.

287. **Prioridad de la clase de la fila** (decidida sin preguntar, por coherencia): por encima de `For column`, de
     las reglas de posición y de `Column align`, y por debajo de las clases de la propia celda y de su `For id`;
     es decir, una clase de la fila es una regla de clase, pero más débil que la de la celda, que es más
     específica. La fila de totales gana a la cebra y a la columna; una celda suelta puede romperlo con su
     clase. El `Typography` de la `Row` sigue siendo herencia (como antes): una regla de la clase de la propia
     fila lo pisa, igual que `Element: Cell` lo pisa. *Alternativa:* poner la clase de la fila por debajo de
     `For column` (como una regla de posición).

288. **Una regla de la clase de una `Row` no puede dar `Break`** a sus celdas (`STYLE_NOT_ALLOWED`, igual que la
     de la clase de una celda): los cortes de una fila son los de su `Row`, que los declara ella. Se decidió sin
     preguntar para no abrir un segundo camino al `Break` de la fila. *Alternativa:* permitirlo y tomarlo como el
     `Break` de la fila (sería el único caso en que una regla da `Break` a algo que no lo admite).

## Ligaduras (2026-10-28, ROADMAP-QA-ARQ.md, QA-03 y PF-01)

289. **`Ligatures` en `Typography`, heredable, `false` por defecto** (decidido con el usuario: con y sin
     ligaduras, y sin por defecto). Antes PDFBox aplicaba la tabla `GSUB` al pintar mientras el motor medía un
     glifo por carácter (decisión 48): en IBM Plex Sans y Serif cambiaban `fi`, `fl`, `ffi` y `ffl` y el texto
     no medía lo que se pintaba. Ahora, sin la opción, se desactiva `GSUB` al incrustar la fuente
     (`TrueTypeFont.setEnableGsub(false)`, API pública de fontbox) y el pintado de texto es entre 17 y 30 veces
     más rápido; con ella, el texto se mide con los mismos glifos que se escriben. Cambia el aspecto de los
     documentos existentes (las `fi` de Plex Serif dejan de ser ligadura): se regeneraron los PNG afectados,
     revisados a ojo. *Dónde:* schema, `tree/Style.ligatures`, `tree/PropertyReader`, `model/FontFace`,
     `resources/FontFile`, `paint/PdfCanvas.pdfFont`. *Alternativa:* medir siempre con ligaduras, o quitarlas
     siempre sin opción.

290. **La opción viaja en `FontFace`** (`ligatures`, cuarto componente; `base()` es la fuente sin ella): así la
     usan sin cambios las firmas de `TextMeasurer`, `Canvas.text` y la caché de fuentes del PDF, y un fichero TTF
     se identifica igual con y sin ligaduras (`FontLibrary.find` normaliza). `PdfCanvas` incrusta una fuente
     distinta por cada combinación usada. *Alternativa:* un parámetro más en `TextMeasurer.width` y en `Canvas.text`.

291. **Con `Letter spacing` distinto de 0 no hay ligaduras** (como en CSS), aunque `Ligatures` sea `true`:
     `Style.fontFace()` lo resuelve. Además el pintado ya partía el texto carácter a carácter para el interletraje,
     de modo que PDFBox no tenía dónde ligar. *Alternativa:* medir con ligaduras y pintar sin ellas (descuadra).

292. **Se mide por palabras, como PDFBox aplica `GSUB`** (parte el texto en los espacios en blanco `\s`): una
     ligadura no cruza un espacio ni un cambio de tramo, que se miden y se pintan por separado. Se aplica el
     mismo `GsubWorker` de fontbox (`ccmp`, `liga` y `clig` para escritura latina) para que lo medido sea lo
     escrito, y se memoriza el ancho de cada palabra. Una palabra con un carácter sin glifo se mide como siempre
     (el error de glifo ausente ya lo da `PdfMetrics.validate`). *Alternativa:* leer solo la característica
     `liga` de la tabla, con otro algoritmo que el de PDFBox.

293. **El SVG de depuración no distingue `Ligatures`**: el navegador aplica las ligaduras de la fuente como
     quiera. Solo es el lienzo de los tests; el PDF es la salida de verdad. *Alternativa:* `font-variant-ligatures`
     en el texto del SVG.

294. **`Converter` sustituye a `PdfWriter.write`** (ROADMAP-QA-ARQ.md, AR-08 y TS-05; decidido con el usuario: sin
     código fuera del repositorio, así que se hace el refactor completo y no se deja nada obsoleto).
     `PdfWriter` tenía cuatro sobrecargas de `write` cuyos parámetros (estilos que podían ser `null`, `Resources`,
     un `boolean debug` suelto) se multiplicaban con cada opción nueva, solo producía PDF y no decía nada de
     hilos. Ahora la configuración es un objeto inmutable (`Converter.builder()`: `resources`, `styles`, `debug`),
     seguro entre hilos porque cada conversión crea su propio `Engine`, con `layOut`, `toPdf` (de un documento o
     de uno ya maquetado) y `toSvg`. Los estilos nunca son `null`: si no hay, no se llama a `styles`. Vive en un
     package nuevo, `convert`, encima de todos los demás y del que no depende nadie (`ArchitectureTest`).
     `PdfCanvas.write` y `Engine` siguen siendo públicos como capa de bajo nivel. *Dónde:*
     `convert/Converter`; se eliminó `paint/PdfWriter`. *Alternativa:* conservar `PdfWriter.write` como atajo
     o marcarlo `@Deprecated`.

295. **Todos los errores del motor son un `StxtPdfException`** (ROADMAP-QA-ARQ.md, AR-09). Antes no tenían padre común
     (`InvalidModelException`, `ResourceException`, la `ParseException` de `stxt-core`, `IllegalStateException`,
     `UncheckedIOException`), y un servicio no podía distinguir un documento inválido (4xx) de un fallo del servidor
     (5xx) sin conocerlos todos. Lleva `kind()` (`INVALID_DOCUMENT`, `RESOURCE`, `LIMIT`, `INTERNAL`), `code()` y
     `line()` (`NO_LINE` = 0). Vive en `model`, que no depende de nada, para que `tree` y `resources` lo extiendan.
     *Dónde:* `model/StxtPdfException`, `model/InternalException`. *Alternativa:* una interfaz o un `Result` en lugar
     de una excepción.

296. **`code()` es el código en texto y el enum pasa a `errorCode()`** en `InvalidModelException`. Hacía falta un
     `code()` común a todos, y los errores de la librería tienen un conjunto abierto de códigos que no caben en
     `ErrorCode`. Los tests comparan `e.errorCode()` con el enum y el catálogo compara `e.code()` en texto.
     *Alternativa:* añadir cada código de `stxt-core` a `ErrorCode`, que ata el motor a la lista de la librería.

297. **Los errores de `stxt-core` salen como `DocumentSyntaxException`** (en `tree`, con el código, la línea y el
     mensaje de la librería, que va como causa) desde `DocumentReader` y `DocumentSchema.validate`. Un
     `LimitException` de la librería (documento anidado más de 100 niveles o con una línea de más de 10 000
     caracteres) es un `LIMIT` y no un documento inválido. El fichero que no se puede leer es un `ResourceException`;
     un fallo del schema que lleva el propio motor, un `InternalException`. *Alternativa:* dejar pasar la excepción
     de la librería.

298. **`PAGE_LIMIT` es un `ErrorCode` con `Kind.LIMIT`, sin línea** (antes un `IllegalStateException` del
     `Paginator`). Cada código sabe su `kind()` (`INVALID_DOCUMENT` por defecto), para que los límites de QA-06 con
     línea (una imagen demasiado grande, un SVG enorme) puedan ser también un `InvalidModelException`. «Sin
     progreso» (la garantía del §5.10 que no se cumple) y «enlace sin destino» son `InternalException`: son
     defectos del motor. *Dónde:* `tree/ErrorCode`, `layout/Paginator`, `paint/PdfLinks`, caso
     `test/errors/limits/PAGE_LIMIT.too-many-pages.stxt`. *Alternativa:* un `LimitExceededException` aparte.

299. **`Converter` es la frontera de los errores**: deja pasar los `StxtPdfException` y las `IOException` del
     destino, y convierte cualquier otra excepción de ejecución en un `InternalException` con el original como
     causa, así que «una sola `catch`» vale también para un defecto del motor o de PDFBox. `PdfCanvas.write` saca
     como `IOException` las `UncheckedIOException` de las primitivas del lienzo (que no pueden declararla).
     Los `IllegalStateException` de mal uso interno (`ConstraintSpace`, `Style.line`) se quedan como están.
     *Alternativa:* envolver también la `IOException` del destino, que no es un error del motor.

300. **El protocolo de la paginación deja de ser público en `Engine`** (ROADMAP-QA-ARQ.md, TS-01):
     `reservePageDigits`, `assume`, `number` y `absolute()` son de package, porque solo los usa el `Paginator`.
     Quien compartía o manejaba un `Engine` desde fuera ya no puede romper la numeración de otro documento. Lo
     público es `layOutDocument`, `layout`, `intrinsic`, `contentIntrinsic`, `measurer` y `natural`. *Alternativa:*
     moverlos a un objeto de sesión por documento (`PaginationSession`, AR-04).

301. **`layOutDocument` suelta lo que memoriza al terminar** (también si falla): vacía la caché, el reparto en
     líneas y las páginas supuestas de las referencias. Un motor guardado ya no retiene el árbol de cajas del último
     documento, y el resultado de uno reutilizado es idéntico al de uno nuevo (`EngineContractTest`, con los once
     ejemplos). Costó nada medible: la caché solo sirve dentro de un documento. *Alternativa:* dejarlo y documentar
     que no se guarde.

302. **Contrato de hilos escrito, no solo observado** (TS-01): `Engine` no se comparte; `Converter`, `Resources` y
     `DocumentReader` sí; un `LaidOutDocument` se puede pintar desde varios hilos (los fragmentos son `record`) y las
     cajas son de solo lectura una vez construidas. El schema compartido de `stxt-core` se apoya en dos hechos de la
     versión 1.0.3, comprobados en su bytecode: `SchemaProviderMemory.getSchema` solo lee de un `LinkedHashMap` y
     `SchemaValidator` solo escribe campos en su constructor. Queda anotado en el `pom.xml` y en `DocumentSchema`
     que un cambio de versión obliga a repasarlo. *Alternativa:* una copia del schema por hilo.

303. **`Limits` y sus valores por defecto, «conservadores»** (ROADMAP-QA-ARQ.md, QA-06; los valores los eligió el
     usuario entre tres juegos medidos): 2000 páginas, 25 millones de píxeles por PNG o JPEG, 20 000 elementos y 64
     niveles por SVG. Se miden con `tools/qa/BombsProbe.java`: un PNG con transparencia de 144 Mpx (547 KB en disco)
     revienta un heap de 512 MB y tarda 13 s con 1,5 GB; un PNG de 900 Mpx de 106 KB tarda 23 s; un SVG de 300 000
     rectángulos tarda 5,4 s; 20 000 niveles de SVG dan un `StackOverflowError` (un `Error` que ni el `Converter`
     atrapa); una cadena de `use` de 10⁹ elementos agota un heap de 512 MB tras 51 s. Un valor por defecto más alto
     (el juego «medidos» era 10 000 páginas, 100 Mpx, 100 000 elementos y 256 niveles) deja a un servicio expuesto a
     cada una de estas entradas; el usuario prefirió pecar de estricto, y todo se sube con `Converter.builder().limits`.
     *Dónde:* `model/Limits`. *Alternativa:* los límites como propiedades del sistema, o como constantes sin configurar.

304. **Los límites son un parámetro de la conversión y no del recurso**: `Limits` llega a `Engine`, `Images`,
     `Paginator` y `PdfCanvas.write` (cambia su firma), no a `Resources` ni a `ImageFile`, que se comparten y guardan en
     caché por carpeta. `ImageFile` solo cuenta (píxeles de la cabecera, elementos y niveles del SVG) y `Images.validate`
     compara con los límites de cada conversión. Dos `Converter` con límites distintos pueden compartir `Resources`.
     *Alternativa:* los límites dentro de `Resources`, que ataría la caché de imágenes a un único juego de límites.

305. **El recuento de elementos de un SVG cuenta lo que dibuja, no lo que pesa el fichero**: cada elemento cuenta 1 y un
     `<use>` suma además todo lo que repite, de modo que un SVG de 1 KB con 5 niveles de `<use>` de 10 copias cada uno
     son 111 111 elementos. Es un tope por exceso de lo que cuenta JSVG (cuenta también lo que está en `defs`), así que
     lo que cumple el límite no falla al dibujar (`LimitsTest.anSvgAtItsLimitsIsDrawnByJsvg`). Se calcula sin recursión
     (una pila explícita) porque un SVG hostil puede anidar o encadenar decenas de miles de niveles, satura en 2⁵⁰ y un
     `<use>` circular es `IMAGE_FORMAT`. *Alternativa:* dejar que JSVG cuente al dibujar, que falla tarde, sin línea y sin
     tipo.

306. **JSVG trae límites ocultos que rechazaban SVG legítimos**: 2000 elementos y 30 niveles. Un SVG de 3000 rectángulos
     pasaba la validación del motor y fallaba al pintar con una `IOException` genérica, con un volcado de pila en el
     registro. Ahora `PdfImages` da a JSVG los `Limits` del motor y, si aun así no carga un SVG, lanza un
     `ImageFormatException` (kind `RESOURCE`, ya no una `IOException`). Cambia el comportamiento: los SVG de entre 2000 y
     20 000 elementos, que antes fallaban, ahora se dibujan. *Alternativa:* dejar los límites de JSVG.

307. **Los límites del texto STXT no se vuelven a exponer**: 10 MB, 100 niveles y 10 000 caracteres por línea son los de
     `stxt-core`, ya dan un error `LIMIT` desde `DocumentReader` y se pueden cambiar con sus propios métodos si hiciera
     falta. `Limits` solo tiene lo que el motor comprueba por su cuenta. *Alternativa:* un `DocumentReader` con `Limits`.

### Revisión de QA, sesión 4: el texto de los SVG sin estado de la JVM (ROADMAP-QA-ARQ.md, QA-02 y TS-03)

308. **El texto de un SVG se dibuja con el fichero que se comprobó, sin dar nada de alta en AWT.** Medido: con la JVM
     en turco, AWT tiene `IBM Plex Sans` dada de alta pero `new Font(familia)` devuelve `Dialog` (pasa la `I` a
     minúscula con el idioma), así que JSVG dibujaba el texto con otra fuente sin avisar y se reescribían 12 PNG; y el
     registro es de toda la JVM, así que una segunda carpeta de recursos con otra «IBM Plex Sans» se dibujaba con la de
     la primera. Ahora `SvgInspector` apunta la fuente de cada `<text>` y `<tspan>` (`ImageFile.textFonts`), y al
     dibujar `SvgFonts` (un `DomProcessor` de JSVG) le pone una familia clave, `stxt-font-` y la huella SHA-256 del
     fichero (`FontFile.digest`), con su peso y su estilo, y la da `PlatformSupport.customFont` con `Font.createFont`.
     El formulario PDF de los diez SVG con texto del repositorio es el mismo, byte a byte, que antes en inglés y que
     ahora en turco: no cambia ningún PNG. La clave es la huella del contenido porque JSVG guarda las fuentes en una
     caché de toda la JVM cuya clave no incluye el `PlatformSupport`: así lo que guarda vale para cualquier carpeta con
     ese mismo fichero, y crece con los ficheros distintos, no con las conversiones. *Dónde:* `paint/SvgFonts`,
     `PdfImages.draw`, `ImageFile.textFonts`, `SvgInspector`. *Alternativa:* que el motor convierta el texto en
     contornos con sus propias fuentes, sin JSVG; obligaba a reimplementar la disposición del texto de SVG
     (`text-anchor`, listas de `x` e `y`, `dx`, `rotate`, `textLength`…, que el subconjunto admite) o a recortarlo.

309. **Un texto que repite un `<use>` conserva la fuente de su sitio en el fichero.** En SVG, lo que repite un `<use>`
     hereda las propiedades del `<use>`; el motor comprueba cada texto con la herencia de sus antepasados en el fichero,
     y `SvgFonts` le pone esa fuente explícita, de modo que lo que se dibuja es lo que se comprobó. Antes, un texto
     Regular de un `<defs>` repetido dentro de un `g` en negrita se comprobaba en Regular y se dibujaba en negrita.
     *Dónde:* `SvgFonts.apply`. *Alternativa:* comprobar cada texto en el contexto de cada `<use>` que lo repite (y
     dibujarlo con una fuente por contexto, que el DOM de JSVG no permite expresar).

310. **Los SVG con texto se dibujan de uno en uno.** La caché de fuentes de JSVG (`FontResolver`) es un `HashMap`
     estático sin sincronizar, y los glifos de cada fuente también se guardan sin sincronizar: dos hilos dibujando texto
     a la vez podían romperla. `PdfImages` dibuja un SVG con texto dentro de un cerrojo de clase (`JSVG_FONTS`); cada
     fichero se dibuja una vez por conversión. Medido con `ConcurrencyProbe` (8 hilos × 10 rondas × 11 ejemplos): el
     mismo tiempo que antes (7,8 a 10,1 s en los dos casos, en la misma máquina) y los mismos bytes que en secuencial.
     *Dónde:* `PdfImages.draw`. *Alternativa:* `FontResolver.resolveWithoutCache`, que JSVG no deja elegir.

### Revisión de QA, sesión 5: la caché de la paginación (ROADMAP-QA-ARQ.md, PF-03, PF-04, AR-10 y AR-11)

311. **Volver a paginar solo olvida lo que depende de los textos que cambian.** El `Paginator` repite la paginación
     cuando el total necesita una cifra más (`{page}`/`{pages}` a partir de la página 100) o cuando cambia la página de
     un `{page of: id}`, y antes vaciaba la caché entera: un documento de 100 páginas costaba el doble que uno de 99.
     Ahora pasa al motor los textos que cambian de ancho (los que tienen los marcadores, o una referencia cuyo valor
     cambia) y `Engine.forget` olvida en la `LayoutCache` y en el reparto en líneas solo esas cajas y sus antepasados.
     Se apoya en que el resultado de una caja depende solo de su subárbol y de la clave (caja, espacio y token): los
     algoritmos no leen nada de fuera, salvo el estilo, que no cambia. Si un algoritmo nuevo leyera algo de fuera del
     subárbol, tendría que entrar en la clave. Medido con `ScaleProbe`: de 46 a 23 layouts por página con 100 a 657
     páginas, y de 57 a 33 con un índice y una referencia por capítulo. *Dónde:* `Engine.forget`,
     `LayoutCache.forget`, `InlineLayout.forget`, `Paginator.paginate`. *Alternativa:* repetir solo los `Fixed`
     (no sirve para las referencias del cuerpo).

312. **Dos tolerancias con nombre, no una.** `Tolerance.EPSILON` (1e-9) es la de la aritmética y la usa el layout;
     `Tolerance.MODEL` (0,01 pt) es lo que una caja puede salirse sin desbordar (`Overflow`) y la precisión con que se
     comparan los volcados (`ReferenceFiles`). Con `EPSILON` en `Overflow` pasan todos los tests, pero el motor sería más
     estricto: una caja que se sale 0,005 pt (un `Width` con decimales, lo que acumulan las medidas de las fuentes en
     milésimas) pasaría a ser un error. *Dónde:* `model/Tolerance`, `Overflow`, `Leaders` (que tenía su propio `1e-9`).
     *Alternativa:* una sola tolerancia, `EPSILON`, con ese cambio de comportamiento.

### Revisión de QA, sesión 6: los tests de referencia (ROADMAP-QA-ARQ.md, QA-07 y QA-08)

313. **Un PNG de referencia que cambia hace fallar el test** (lo decidió el usuario). Antes `GoldenTest` y
     `ExamplesTest` reescribían las imágenes versionadas dentro de `test/` y un cambio solo se veía si alguien miraba
     `git status`; en la CI no fallaba nada. Ahora el PDF y las imágenes nuevas van siempre a `target/reference/`, se
     comparan con las versionadas (con la tolerancia del antialiasing, `PIXEL_TOLERANCE`) y una que cambia, falta o
     sobra hace fallar el caso con la ruta de las nuevas. `-Dgolden.update=true` y `-Dexamples.update=true` aceptan el
     cambio (reescriben solo lo que cambia; la segunda, también el `expected.stxt`). `mvn test` no modifica ningún
     fichero versionado y la CI sube `target/reference` cuando falla. *Dónde:* `golden/ReferenceFiles` (`writeActual`,
     `checkImages`), `GoldenTest`, `ExamplesTest`. *Alternativa:* seguir reescribiéndolas y que la CI compruebe el
     árbol, que avisa pero no dice qué página ni deja las imágenes a mano.

### Revisión de QA, sesión 7: encapsulación del modelo (ROADMAP-QA-ARQ.md, AR-02 y AR-03)

314. **El árbol de cajas se cierra al construirlo y solo `tree` lo escribe.** `Box` tiene los campos privados, se lee
     con métodos (`text()`, `children()`, `formattedRuns()`…) y se escribe con métodos `set…` del package; `Style`
     tiene los campos del package y un método de lectura por propiedad. Al terminar, `BoxTreeBuilder.build` llama a
     `Box.close`, que recorre el árbol (también los marcadores, que no son hijos): los hijos pasan a una lista
     inmutable, los que están en flujo se calculan una vez y escribir en una caja cerrada es un
     `IllegalStateException`. Así «el layout no escribe en el árbol» deja de ser una convención y un
     `LaidOutDocument` se puede leer desde varios hilos sin depender de que nadie escriba. Los tests que ponen en el
     árbol tramos que el documento no puede escribir (`TextRuns`) lo hacen antes de cerrarlo, con el `finish` de un
     `build` del package; los de otros packages que necesitan un estilo hecho a mano usan `StyleEditor`, un ayudante
     de los tests en el package `tree`. Los campos de `Style` siguen siendo mutables dentro de `tree`: partirlo en
     registros inmutables es la etapa 3 de AR-02. *Dónde:* `tree/Box`, `tree/Style`, `BoxTreeBuilder.build`,
     `test/…/tree/StyleEditor`, `BoxTest`. *Alternativa:* accesores sin cierre (la garantía seguiría siendo una
     convención dentro de `tree`) o un `StyleBuilder` aparte (más código para el mismo efecto mientras `Style` no
     se parta).

### Revisión de QA, sesión 8: el constructor del árbol (ROADMAP-QA-ARQ.md, AR-01)

315. **Las familias de elementos son un enum, no una interfaz con una clase por familia.** `BoxTreeBuilder.element`
     es el orden común a todos los elementos y delega lo propio en `ElementReader`, un enum con una constante por
     familia (`TEXT`, `IMAGE`, `CELL`, `TABLE`, `GRID`, `FLEX`, `LIST`, `MULTICOL`, `MARKDOWN`, `CONTENTS` y
     `CONTAINER`) que sobrescribe solo lo que necesita: `layout`, `wrapper`, `property`, `check` y `content`. Las
     familias son pocas, están cerradas (las da el schema) y comparten casi todo, así que un enum con métodos por
     defecto es más corto que seis clases y un registro, y `ElementReader.of` las elige con un `switch` sobre
     `Element`. El orden de las comprobaciones y los mensajes no cambian (el catálogo de errores lo vigila); solo se
     mueven dos pasos sin efecto: un `Contents` se apunta en el contexto al rellenar su contenido, y el contexto de
     `Fixed` envuelve también el contenido de los textos, las imágenes y las celdas, que nunca son `Fixed`. Las
     propiedades sueltas y el grupo `Typography` son tablas `Map<String, Property>` en lugar de `switch`. *Dónde:*
     `tree/ElementReader`, `BoxTreeBuilder.element` y `properties`, `PropertyReader.single`, `tree/MediaReader`.
     *Alternativa:* la interfaz `ElementReader` con una clase por familia de la propuesta de AR-01, o dejar las
     familias como métodos privados del builder (no baja de 350 líneas ni separa lo propio de cada familia).
