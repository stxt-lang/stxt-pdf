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
    *Dónde:* schema; `tree/PropertyReader.INTERNAL` traduce los cortos al nombre interno.
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

81. **Reglas sin aplicar.** Un `Element` o un `For class` sin elementos se permiten (las
    reglas se reutilizan); un `For id` sin su elemento, una clase de `Class` sin su regla y
    dos reglas con el mismo selector son errores. *Alternativa:* tratarlos todos igual
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
