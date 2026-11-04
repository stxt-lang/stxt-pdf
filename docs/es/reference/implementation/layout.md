# El layout

Cómo calcula el motor la posición y el tamaño de cada caja (package `layout` y `fragment`). El comportamiento está en las
páginas de comportamiento común y de cada componente, y aquí, cómo está hecho.

## `Engine` y `LayoutContext`

`Engine` despacha por `Style.layout` al algoritmo correspondiente (un `EnumMap`, y la tabla, que es un grid, tiene el suyo).
Los algoritmos no conocen el motor: dependen de **`LayoutContext`** (`layout`, `intrinsic`, `contentIntrinsic`,
`measurer`, `natural`, `imageRatio`, `arranged`), que `Engine` implementa y que un test puede sustituir por un doble. **Toda llamada recursiva de
layout o de tamaños intrínsecos pasa por `context.layout(...)` / `context.intrinsic(...)`**, nunca directamente al
algoritmo, para usar la `LayoutCache` (sin ella, `Stack` y `Grid` anidados crecen de forma exponencial).

`Engine.layOutDocument` es la entrada: valida las fuentes, las imágenes y los idiomas de los guiones, pagina con el
`Paginator`, comprueba los
desbordamientos con `Overflow` y el módulo mínimo de los códigos con `ModuleSizes`, y devuelve un `LaidOutDocument`. Al terminar (también si falla) suelta todo lo que memoriza.
El protocolo con el `Paginator` (`reservePageNumbers`, `assume`, `headings`, `number`, `overlays()`) es de package. Lo que
ese protocolo fija vive en dos objetos del `Engine` que `InlineLayout` recibe en su constructor y solo lee: `TextValues`
(el número de prueba de `{page}` y `{pages}`, las páginas supuestas, los números y los títulos corridos) y
`LineBreakingMemo` (el reparto en líneas memorizado). Los algoritmos no guardan estado del documento.

> **Decisión:** los algoritmos de layout dependen de la interfaz `LayoutContext` y no de `Engine`. El protocolo de la
> paginación se queda en `Engine`, de package, porque moverlo a otra clase no quitaba ninguna dependencia; lo que fija
> va en `TextValues` y `LineBreakingMemo`, que el motor inyecta en `InlineLayout`, y así el algoritmo se prueba con
> referencias sin un `Engine` entero. *Alternativa descartada:* `InlineLayout` con ese estado como campos, que el motor
> tenía que conocer por su tipo concreto dentro del mapa de algoritmos.

> **Decisión:** el protocolo de la paginación no es público: quien manejara un `Engine` desde fuera podría romper la
> numeración de otro documento. *Alternativa descartada:* un objeto de sesión por documento.

> **Decisión:** `layOutDocument` suelta la caché, el reparto en líneas y las páginas supuestas al terminar, así que un motor
> guardado no retiene el árbol del último documento y uno reutilizado da lo mismo que uno nuevo. No cuesta nada medible: la
> caché solo sirve dentro de un documento.

## `LayoutAlgorithm`

Tiene dos métodos: `contentIntrinsic(box)`, los anchos más estrecho y natural del contenido (sin borde, relleno, margen
ni `Width`, e independientes del espacio), y `layout(box, space, from)`. Los de la caja de margen los da
`context.intrinsic(box)` con `BoxModel.intrinsic`, y los del contenido, `context.contentIntrinsic(box)` (el mínimo automático de un ítem de `Stack`).

## `ConstraintSpace` y `LayoutResult`

**`ConstraintSpace`** es la entrada inmutable del layout y parte de la clave de la caché: ancho disponible, alto (vacío =
indefinido), tamaños impuestos por el padre, bases de porcentajes, espacio restante en la página desde el borde superior de
la caja (vacío = sin fragmentación), si la caja empieza la página y si se mide el alto de su contenido ignorando su `Height`
(`measuringContentHeight()`).

**`LayoutResult`**: el `Fragment`, el `BreakToken` (`null` = la caja ha terminado), la calidad del corte
(de `BreakAppeal.PERFECT_BREAK` a `FORCED`), las `PendingOverlay` que suben hasta su contenedor de referencia (con la posición
estática relativa a la caja de contenido de quien devuelve el resultado) y los `AdjoiningMargins`.

## `BoxModel`

Margen, borde, relleno, ancho de la caja de borde y alto si es definido, a partir del `ConstraintSpace`. Es la parte común de
todos los algoritmos. `childSpace()` da el espacio de los hijos en flujo y `BoxModel.intrinsic(...)` pasa de tamaños
intrínsecos del contenido a los de la caja de margen. Los márgenes `auto` valen 0: el centrado lo hace el padre, que es quien
coloca al hijo. El dimensionado de una imagen está en `BoxModel.replaced`, para que las sondas de `Stack`, `Grid` y los posicionados midan
bien la imagen: se usa siempre `BoxModel.resolve(box, space, context)`, que le pasa el tamaño natural. Primero se aplica el
máximo y luego el mínimo. Si chocan, gana el mínimo (`Sizing.constrain`).

## Fusión de márgenes

Cada caja devuelve en `AdjoiningMargins` los márgenes de sus descendientes que salen por arriba o por abajo (y si está vacía y
deja pasar los suyos). El padre los fusiona con los márgenes propios del hijo, que resuelve él. `MarginStrut` es inmutable y
guarda el mayor positivo y el más negativo por separado. `Box.isolatesMargins()` dice qué cajas lo cortan. La cuenta del
apilado (el cursor, los márgenes que esperan bajo él, los que salen de la caja por arriba y la colocación horizontal de
un hijo con sus márgenes laterales) la lleva `Stacking`, que usan `BlockLayout` con los hijos de un bloque y
`FootnoteLayout` con las notas del área de notas, para que una regla de fusión o de centrado sea la misma en los dos.

Los márgenes que salen de un hijo por arriba (el margen superior de su primer descendiente, si es mayor que los de
alrededor) lo mueven hacia abajo respecto a donde lo pondría su propio margen. `BlockLayout` los **predice antes de
maquetarlo** con `LeadingMargins`, una función pura del árbol: recorre la cadena de primeros descendientes en flujo mientras
cada caja esté abierta por arriba (flujo de bloque, sin borde ni relleno arriba, sin aislar los márgenes) y acumula sus
márgenes superiores, más los dos márgenes de los hijos vacíos que preceden a cada uno, resolviendo los porcentajes contra el
ancho de contenido de cada nivel, y salta las cajas laterales y las fuera de flujo. Es una predicción: un texto cuenta como
no vacío (si tiene líneas solo se sabe al maquetarlo) y un bloque como vacío solo si no tiene hijos en flujo. El hijo se
coloca donde dice la predicción y lo que decide son los `AdjoiningMargins` que devuelve su layout: si coinciden, un solo
layout; si difieren y lo mueven, lo que le queda de página y lo que tiene al lado cambian, y se maqueta otra vez en su
posición solo si el resultado puede cambiar: si sube (tiene más sitio), si lo que colocó no cabe en lo que queda desde
ahí o si tiene cajas laterales al lado (`Run.holdsAt`). Si baja, cabe y no hay nada al lado, el primer layout vale, aunque el
hijo quedara cortado: con menos sitio nada nuevo cabe y lo colocado sigue cabiendo, así que los algoritmos deciden lo
mismo. Una predicción que se pasa (un margen negativo que al final no sale) no corta antes del hijo: si donde lo pone no
queda sitio, se parte de su propio margen, como antes. Hasta la 0.9.0 se maquetaba en la posición de su propio margen y se
repetía siempre al moverlo; después, solo cuando el primer layout no valía más abajo, lo que dejaba dos layouts por
capítulo más largo que la página en la página donde empieza.

> **Decisión:** los márgenes previstos son una predicción con red y no sustituyen a los que devuelve el layout: un texto
> solo se sabe vacío al maquetarlo (un título corrido o una referencia pueden dejarlo sin líneas), así que un sustituto no
> podría ser exacto sin maquetar. *Alternativa descartada:* reemplazar `adjoining.top()` por la predicción y quitar
> `holdsAt`, que cambiaría la salida en esos casos.

## Grid

`TrackSizing.size` reparte el espacio libre entre las pistas según un `Distribution`: `EQUAL`, a partes iguales entre las
`auto`, en un `Grid`, y `CONTENT`, en proporción al contenido de cada columna, en una `Table` (`GridLayout.arrange` lo
elige por el papel de la caja, [Table](../components/table.md)).

> **Decisión:** un ítem de grid estirado se maqueta siempre otra vez con su alto fijado, aunque mida lo mismo que al
> medirlo, para que sus hijos tengan la base definida que promete la regla del alto definido. Cuesta un layout más por ítem
> estirado. *Alternativa descartada:* repetirlo solo si algún descendiente usa porcentajes verticales.

## Cajas posicionadas

Los algoritmos no maquetan los hijos `Overlay` y `Page overlay`. Los devuelven como `PendingOverlay` con su posición estática, y
los hijos `Frame` se desplazan con `FrameOffset` después de colocarlos. `Engine.layout` resuelve con `OverlayLayout`
(sobre `OverlayEquation`) los `Overlay` cuyo contenedor de referencia es la caja recién maquetada (sus fragmentos cuelgan
del de esa caja). Los `Page overlay` suben hasta la raíz y el `Paginator` los coloca en `Page.pageOverlays` de cada página, solo en las
que incluye su `Pages`.

## Cajas giradas en flujo

`Engine.layout` maqueta aparte una caja con `Style.rotatesInFlow()` (un `Block`, `Text`, `Paragraph`, `Image` o `Barcode` con
`Rotate`): con `ConstraintSpace.fixed` y el ancho de `turnedWidth` (su `Width` resuelto o, con `auto`, su ancho natural más
borde y relleno), sin fragmentación y sin `from`. El resultado se envuelve con `Fragment.turned`: el fragmento mide el
rectángulo que envuelve su caja de borde girada sobre su centro, y `turned` guarda esa caja sin girar, centrada en él. Los
algoritmos de los padres no saben nada del giro: ven una caja del tamaño del rectángulo, que no se parte (`BEFORE` si no
cabe y no está al principio de la página) y cuyos márgenes de dentro no salen (`AdjoiningMargins.NONE`: aísla los
márgenes). `Engine.intrinsic` hace lo mismo con la base de porcentajes vacía: su ancho más estrecho y su
ancho natural son el ancho del rectángulo más los márgenes. Es contenedor de referencia de sus `Overlay`
(`Box.referenceContainer`), que se resuelven sobre la caja sin girar antes de envolverla.

## Fragmentos

Un **`Fragment`** tiene su posición relativa a la caja de **contenido** del padre. `StxtDump` la pasa a coordenadas absolutas
de página. Lleva su propio `border` y `padding`, que en un fragmento partido no son los de la caja: se usan siempre los del
fragmento para llegar a su caja de contenido. Lleva también el `margin` usado (con la base de porcentajes de su layout) y el
desplazamiento de `Frame` ya sumado a su posición (`offsetX`/`offsetY`), que `Overflow` necesita. La geometría solo
existe en los fragmentos: `Box` no guarda posición ni tamaño.

En una caja girada en flujo, `x`, `y`, `width` y `height` son los del rectángulo que ocupa, y `turned`, su caja de borde
sin girar relativa a él. Sus hijos y sus líneas se refieren a la caja sin girar: quien baja a ellos usa
`Fragment.borderBox(x, y)`, `contentWidth()` y `contentHeight()`, que la tienen en cuenta, y nunca `width` y `height`.

> **Decisión:** la geometría usada solo existe en los fragmentos: una caja se maqueta muchas veces (sondas, medidas,
> estirado, cada página) y la caché devuelve resultados sin volver a escribirla, así que «el último layout» de una caja no
> correspondería a ningún fragmento. El margen usado va en el fragmento, con la base de porcentajes del layout que lo
> produjo. *Alternativa descartada:* que `Overflow` resuelva los márgenes con el ancho de contenido del padre, que no es la
> base de los ítems de grid.

> **Decisión:** `Overflow` comprueba un `Frame` en su sitio con el desplazamiento que usó el layout, guardado en el
> fragmento, en lugar de recalcularlo: en un padre partido entre páginas la base de un porcentaje vertical no es el alto del
> fragmento, y recalcularlo rechazaba documentos válidos.

## Fragmentación

Cada algoritmo coloca lo que cabe en `fragmentainerRemaining` y devuelve el corte con su calidad. El padre lo compara con los
cortes entre sus hijos y se queda con el **último de mayor calidad** (`BreakRules.best`). Relajar las reglas en el orden de
[Cortes](../common/breaks.md#elección-del-corte) sale solo de esa comparación: por eso un hijo que no puede respetar viudas y
huérfanas devuelve igualmente un corte (`VIOLATES_WIDOWS`) en lugar de `BEFORE`.

**`BreakToken`** (sealed, records con `equals`): `Block`, `Line`, `Container`, con el alto ya consumido por la caja (un
`Height` definido es el total de todos los fragmentos, y un fragmento cortado solo consume lo que ocupa su contenido), y
`BEFORE`: «no cabe nada, corta antes de mí», que nunca se pasa como `from`. El `Paginator` detecta la falta de progreso
comparando tokens. El token de la raíz no acumula alto: cada página tiene el área útil entera.

`Slice` calcula el alto, el borde y el relleno de cada fragmento (`Edges: open` y `closed`), y `Slice.contentRemaining`
da lo que queda al contenido. Un fragmento de una caja con alto definido que no termina llega al final de la página pero
solo consume lo que ocupa su contenido, y lo mismo vale hacia arriba: `LayoutResult.occupied(from)` da lo que un fragmento
ocupa para su padre (lo consumido por su token en ese fragmento, y no su alto), y `BlockLayout` (en la `Mark` del hijo
partido), `UnitBreaker` (en `Split`) y `ColumnsLayout` (en `Fill`: el mayor de sus columnas) lo pasan a `Slice.of` como
`occupied`, aparte del alto visible del contenido.
Stack y grid cortan entre unidades (líneas, ítems o filas, que son `fragment.Unit`) con `UnitBreaker`: se
maquetan enteros sin fragmentar (`UnitBreaker.Arranged`: ítems colocados y `Units`) y `UnitBreaker.fragment` se queda con lo
que cabe en la página. Ese reparto sin fragmentar (la colocación, las pistas, los ítems maquetados a su tamaño, las
franjas y, en una tabla, la cabecera, el pie y las líneas de `Merge borders`) depende de la caja y de su espacio sin
fragmentación (`ConstraintSpace.unfragmented`, que conserva `atFragmentainerStart`), no de lo que queda de página ni del
token: se calcula una vez por contenedor y espacio (`LayoutContext.arranged`, un memo más de la `LayoutCache`, por caja,
espacio y tipo de lo memoizado, que `clear` y `Engine.forget` vacían como el resto) y cada fragmento solo lo corta. Lo
memoizado es inmutable (`Arranged` y `Repeat` copian sus listas). Hasta la 0.9.0 cada fragmento de una tabla la colocaba y
medía entera otra vez, y las unidades de un grid se calculaban recorriendo todos los ítems por cada fila (ahora, una
pasada por los ítems); `ArrangementCacheTest` cuenta los repartos. Una unidad que no cabe en lo que queda de página parte sus ítems en paralelo en esa página
(`Units.split`, de `Split rows` y `Split items`, y solo donde `Units.splittable` lo admite, que no es el caso de las líneas
de un `Stack` horizontal con `Wrap`; con `false`, solo la que no cabe ni al principio de página), repitiendo su layout con `ConstraintSpace.fragmented` y el espacio guardado en `UnitBreaker.Placed`;
`UnitBreaker.splitOnThisPage` compara la calidad de ese corte con la del mejor corte entre unidades (`BreakRules.cutUnits`)
y se queda con el último de los mejores, y el `BreakToken.Container` lleva el token de cada ítem. Con `atFragmentainerStart` hay que colocar algo aunque desborde (garantía de progreso). Las tablas repiten
cabecera y pie con `UnitBreaker.Repeat`. Las franjas de las filas (`Box.rows()`, `Element.ROW`) son `UnitBreaker.Band`:
`GridLayout.arrange` las crea con el ancho de las columnas, `TableLayout` coloca las de la cabecera y el pie en el
`Repeat` y deja las del cuerpo en su unidad, y `UnitBreaker` las pone en cada fragmento delante de los ítems (enteras,
recortadas con `splitBands` si su grupo se parte, o la de la última unidad en la continuación del grupo). Son fragmentos
sin hijos: el pintado las trata como cualquier caja en flujo. Con `Merge borders`
([Table](../components/table.md#bordes-fusionados)), `UnitBreaker` apunta además qué muestra cada fragmento de cada unidad
(`UnitBreaker.UnitPiece`: entera, recortada o la continuación de su grupo, como las franjas) y se lo pasa con sus hijos a
`Repeat.rules`; `TableLayout` añade las filas de la cabecera y del pie y `layout.table.MergedBorders` calcula las líneas
de ese fragmento sobre las columnas de `GridLayout.arrangeWithColumns`, que van en `Fragment.rules`. Los cortes forzados son `PAGE` o `COLUMN`
(`BreakRules.forcedBetween` devuelve el tipo). El de página tiene la calidad `BreakAppeal.FORCED_PAGE`, por encima de `PERFECT_BREAK`.

> **Decisión:** el memo del reparto vive en `LayoutContext` y `LayoutCache`, no dentro de cada algoritmo: así se vacía con
> la caché (`clear` al terminar y `Engine.forget` al volver a paginar, que alcanza al contenedor como antepasado del texto
> que cambia) y los algoritmos siguen sin estado. El tipo forma parte de la clave porque la tabla memoiza su preparación
> (cuerpo, cabecera y pie) encima del reparto de su grid, los dos para la misma caja. *Alternativa descartada:* un campo en
> cada algoritmo, que `Engine.forget` no alcanzaría y que un `Engine` reutilizado arrastraría.

El contenido de un `Columns` va en una caja generada, el flujo de columnas (`Element.COLUMN_FLOW`), su único hijo: cada
columna es un fragmento de esa caja, maquetado con los algoritmos de siempre. Las columnas solo están al principio del
contenedor de fragmentos (`atFragmentainerStart`) si lo está el `Columns`, y los cortes entre columnas no cuentan para el
padre: el corte de página tiene la calidad del de la última columna.

> **Decisión:** el flujo de columnas es una caja generada. *Alternativa descartada:* que el `Columns` apile los hijos él
> mismo, repitiendo el flujo de bloque.

`ColumnsLayout` maqueta el flujo de columnas una vez por columna con `context.layout`, con el alto de la columna como
`fragmentainerRemaining` y el token de la columna anterior como `from`, y cada columna es un fragmento suyo. El equilibrado es
una búsqueda binaria del alto mínimo, con precisión de 0,001 pt, que rechaza los altos con cortes entre columnas peores
que los del reparto en orden.

## Texto

`InlineLayout` reparte el texto con `LineBreaker` (voraz o Knuth-Plass) o, con `Preformatted: true` o `wrap`, con `PreformattedText`.
El texto de una caja es siempre un solo párrafo (las líneas en blanco son un error al leerlo, salvo en uno preformateado,
donde son contenido): `BreakRules.cutLines` aplica viudas y huérfanas a todas sus líneas y solo la última no se justifica.
`InlineLayout.runs` sustituye las referencias por su valor (el que el `Paginator` dejó en `TextValues`) y aplica `TextTransform` a los tramos **antes** de repartirlos,
con el `Case` y el `Small caps` de cada tramo con estilo (`TextFormat.textCase`, `smallCaps`). `LineBreaker` mide cada
carácter con el interletraje de su tramo (`TextFormat.letterSpacing`, en `advance`) y quita a una línea el del último
carácter (`trailing`, `Paragraph.width`), que es el de su tramo. Un tramo que es una imagen (`TextFormat.image`, con el
texto `TextRun.IMAGE`, U+FFFC) mide en `advance` lo que da `TextMeasurer.imageWidth`: tan alto como el tamaño de la línea
y tan ancho como su proporción, que `TextValues.WithImages` lee del fichero a través de `LayoutContext.imageRatio`
(así también con `FixedMetrics` la imagen mide lo que es), y `PageNumbers.probing` delega. `Images.validate` comprueba
antes las imágenes de los tramos como las de las `Image` (formato, límites), con la línea del texto.
`TextLine.runs` guarda cada tramo colocado, con el espaciado de `justify` incluido. `MarkerLayout` coloca el marcador de un
`List item` en su primer fragmento, con la línea base de su primera línea (`TextMeasurer.vertical`); un marcador con imagen
mide `Height` o el `Size` del ítem de alto y lo que da su proporción (`LayoutContext.natural`) de ancho, lleva el espacio
que lo separa del ítem como relleno derecho del fragmento, y se centra en las mayúsculas (`VerticalMetrics.capHeight`) de
la tipografía del ítem sobre esa línea base.

**Cajas laterales** ([Block](../components/block.md#cajas-laterales-side)). `BlockLayout` coloca un hijo con `Side` entero,
a su lado y en la posición del flujo, sin mover el cursor, y lo apunta como un `Beside` (la banda que ocupa y lo que quita
por cada lado, con sus márgenes). A cada hijo que rodea las cajas (`Box.wrapsBesideSideBoxes`: textos no preformateados,
`Block`, encabezados, `Markdown`, listas e ítems de lista sin fondo ni borde, que no sean cajas laterales ni lleven `Clear`,
posición, `Rotate` u `Overflow: clip`; las listas, solo junto a cajas derechas) le pasa en el
`ConstraintSpace` las **exclusiones** (`model.Exclusion`: banda y lo que quita por cada lado, medidas desde la caja de
borde del hijo), que forman parte de la clave de la caché; los demás hijos bajan hasta el final de las cajas laterales. Un
`Block` que recibe exclusiones las trata como cajas laterales heredadas para sus propios hijos, sin que cuenten para su
alto. `InlineLayout` las pasa a la caja de contenido y `LineWidths` da a cada línea su caja de línea (desde dónde empieza
y cuánto mide, con `varying()` como la primera línea de ancho entero); `LineBreaker` reparte con el ancho de cada línea,
el voraz línea a línea y Knuth-Plass con el número de línea en el estado de la programación dinámica (hasta `varying()`),
y una palabra que no cabe sola en una línea corta la deja vacía (`Line.isEmpty`) y pasa a la siguiente. Cada `Line` lleva la
posición del texto donde empieza (`start`), y el `BreakToken.Line` lleva las exclusiones y la posición inicial del reparto
con que se colocó el fragmento: el fragmento siguiente, si hay exclusiones en alguno de los dos, vuelve a repartir el
resto del texto desde la posición de su primera línea con las suyas (normalmente ninguna: las cajas laterales acaban con
la página), y el reparto memorizado se indexa por exclusiones y posición. Las viudas de un texto cortado junto a una
caja se cuentan con las líneas que tendrá el resto a ancho entero. `TextLine.left` y `right` guardan la caja de línea para
volver a colocar la línea al numerar las páginas.

**Letra capitular** ([Text](../components/text.md#letra-capitular-drop-cap)). `InlineLayout` quita la primera letra de los
tramos de un `Paragraph` con `Drop cap` en todos los fragmentos (así las posiciones del texto no cambian), la compone al
cuerpo que da la altura de mayúsculas de la fuente (`VerticalMetrics.capHeight`, de la tabla `OS/2`, o 0,7 sin ella) y la
coloca como la primera `TextLine` del primer fragmento, con la base en la de la línea `N − 1`; las `N` primeras líneas
reciben su ancho como una exclusión más (`LineWidths.withDropCap`), el párrafo mide al menos `N` líneas y `cutLines` recibe
`Orphans` ≥ `N`. `TextPainter` no escribe el espacio invisible tras la línea de la capitular, para que el texto que se
lee siga siendo una palabra.

**Guiones.** Con `Hyphenate: true`, `InlineLayout` pasa a `LineBreaker` el `Hyphenator` del idioma de la caja
(`Hyphenations.forLocale`, package `layout.inline.hyphenation`): el algoritmo de Liang sobre un trie con los patrones
del recurso del idioma, con una caché por palabra. `LineBreaker.words` da a cada palabra sus puntos de corte (los de sus
guiones blandos, que quita del texto, o los de los patrones sobre sus letras, que son las de `Hyphenator.isLetter`: las
de Unicode y, si los patrones lo tienen, el apóstrofo), y los dos repartos trabajan sobre las
**posiciones** del texto (`LineBreaker.Paragraph`): el inicio de cada palabra y cada punto de corte. Una línea va de
una posición a otra; si acaba en un punto de corte lleva el guion, medido con el formato de la letra anterior, y la
`Line` (y la `TextLine` colocada) lo dice con `hyphenated`. El reparto voraz prueba, cuando una palabra no cabe, sus
puntos de corte de mayor a menor; Knuth-Plass suma a los deméritos la penalización de TeX por guion (50²) y por dos
guiones seguidos (10 000). Los anchos siguen siendo sumas: los de las palabras enteras, los espacios y las partes de una
palabra partida, cada uno medido una vez. `Hyphenations.validate` comprueba antes del layout, como las fuentes, que cada
caja con texto y `Hyphenate: true` está en un idioma con patrones.

**Saltos de línea forzados.** El salto de un Markdown (`\` al final de la línea) llega al texto como el carácter U+2028
(`TextRun.LINE_SEPARATOR`), que `MarkdownBuilder` escribe en su lugar. `LineBreaker.words` lo trata como un espacio que
separa palabras y marca la palabra de antes (`Word.forced`); la posición donde empieza la siguiente es un final
obligatorio (`Paragraph.forced`). El reparto voraz cierra ahí la línea, y Knuth-Plass no deja que ninguna línea lo
cruce y no da maldad a la que acaba en él, como a la última. La `Line` lo dice con `forced`, e `InlineLayout` no la
justifica. El ancho natural del texto es el de su línea más ancha entre saltos.

> **Decisión:** el idioma se comprueba al empezar el layout y no al construir el árbol, como las fuentes: `tree` no
> depende de `layout`, donde viven los patrones. *Alternativa descartada:* los patrones en `model`.

> **Decisión:** los dos repartos comparten las posiciones y las sumas de `Paragraph`, en lugar de un reparto por
> átomos (partes de palabra) con sumas de prefijos: con ligaduras una palabra entera no mide lo que suman sus partes,
> y así cada línea se mide como se pinta.

**Encoger.** Con `Overflow: shrink` ([Desbordamiento](../common/overflow.md#encoger-el-texto-shrink)),
`InlineLayout.shrink` reparte el texto al `Size` de la caja y, mientras alguna línea mida más que el ancho disponible (con
la tolerancia de `Overflow`, `Tolerance.MODEL`) o las líneas sumen más que el límite de alto (`InlineLayout.limit`: la caja de
contenido del `BoxModel.height()` definido o, sin él, del `maxHeight()`), vuelve a repartirlo a un tamaño 0,5 pt menor, con
`Min size` como último paso. El límite forma parte de la clave del reparto memorizado (`BrokenKey`), porque la misma caja se
mide sin alto (`measuringContentHeight`) y se maqueta después estirada. Las líneas guardan el tamaño al que se repartieron
(`TextLine.fontSize`, el que usan `LineBreaker.place`, `Engine.number` y el pintado), y el interlineado sale de
`TypographyStyle.usedLineHeight(size)`. Los tamaños intrínsecos (`contentIntrinsic`) se miden al `Size`.

**Medida.** Un `TextMeasurer` tiene que ser **aditivo entre palabras** (el ancho de un texto es la suma de los de sus palabras
y espacios): los dos repartos en líneas suman. `PdfMetrics` mide con `FontFile.width(texto, tamaño, ligaduras, kerning)`,
que aplica `GSUB` y los pares de kerning palabra a palabra si se piden; un par no cruza un espacio, así que la suma sigue
siendo exacta. La parte de una palabra partida se mide junto con su guion, como se pinta: con kerning, la última letra y
el guion son un par. `Engine.layOutDocument` llama antes a `validate(root)`, que en `PdfMetrics`
comprueba que existen las fuentes y tienen todos los caracteres.

**Texto largo.** Cada página maqueta el texto desde otra línea, así que la `LayoutCache` no ayuda. `InlineLayout` memoriza
el reparto en líneas por caja y ancho disponible en `LineBreakingMemo`, que es del `Engine` como la caché (sin esa
memoria, el coste crece con el cuadrado de las páginas, y lo vigila `LongTextTest`). El reparto voraz suma los avances de las palabras y los espacios de la línea en curso y solo mide la línea
entera cuando la suma queda a menos de `Tolerance.MODEL` del ancho disponible.

> **Decisión:** el ancho de un texto es aditivo entre palabras y el reparto voraz suma, midiendo la línea entera solo cerca
> del límite: la suma en coma flotante puede diferir en unos 10⁻¹³ pt de la medida de la línea, y sin esa comprobación algún
> corte justo en el límite cambiaba. Así el reparto es exactamente el de medir siempre la línea. *Alternativa descartada:*
> fiarse de la suma (cambia cortes en el límite) o medir en milésimas enteras, que ataría `TextMeasurer` a la forma de
> `FontFile`.

> **Decisión:** la línea base de un marcador de lista se calcula al maquetar con las métricas del medidor (con
> `FixedMetrics`, la aproximación 0,8/0,2, y con `PdfMetrics`, la `hhea` de la fuente) y no al pintar: el marcador es un
> fragmento normal, con su rectángulo en el volcado. El SVG pinta siempre con la aproximación, así que un marcador de otro
> tamaño que su primera línea puede quedar ligeramente desplazado en el SVG de un documento maquetado con las métricas
> reales.

## Paginación

`Paginator` hace una pasada de flujo por sección (`flow`) con **una sola convergencia** de las referencias, y calcula con
`Numbering` el número impreso de cada página (el contador, `restart` con su `Start` y `none`), el último de su tanda
(`{pages}`) y su `Page number format`, con el que los escribe (`label`, `totalLabel`). `Page` lleva su `PageSetup`, su
sección, su `printed` (0 = sin número) y su estilo. El `PageSetup` de una página es el de su sección pasado por
`forPage(número)`: con `Mirror margins`, en las páginas físicas pares lleva el margen izquierdo y el derecho
intercambiados, y como el área útil mide lo mismo en las dos caras, el flujo se maqueta una sola vez y el pintado, el
volcado y `Overflow` no saben de caras. Un `Page overlay` del documento se pone con `PendingOverlay(box, 0, 0)`
en cada página de cada sección que lo hereda, y `Pages.includes` se evalúa con las páginas de su alcance.

**Páginas en blanco.** Tras el flujo, `blankAfter` mira qué secciones dejan la siguiente, con `Begin on: odd`, en una
página física par. Esas acaban con una página en blanco: `Numbering` la cuenta como una página más de la sección, `pagesOf`
le reserva su número físico y `pages` la añade como una `Page` con `blank`, un fragmento vacío de la caja de la sección y
sin `Page overlay`. No forma parte del flujo de la sección, así que el `Pages` de sus `Page overlay` no la ve.

**`{page}` y `{pages}`.** `InlineLayout` mide los marcadores como un número de prueba (`00` al principio, el medidor de
`TextValues`) y las líneas los conservan. Al terminar de paginar, el `Paginator` llama a `Engine.number`
(`PageNumbers.number`, con el medidor real), que rehace solo esas líneas con los valores de cada página ya escritos. Si algún número impreso no cabe en el de prueba (`Numbering.probe`:
tantos caracteres como el más largo, con `M` o `m` si hay romanos), se vuelve a paginar con uno que lo cubre
(`PageNumbers.cover`), que solo crece.

**Referencias.** `Engine.assume` fija las páginas supuestas en `TextValues` (la primera vez, `00`), se pagina, se mira en qué página ha
quedado el primer fragmento de cada caja referida (`Box.key()`) y se vuelve a paginar si alguna difiere, hasta 10 pasadas que
cambien algo. Si no convergen, es `REFERENCE_UNSTABLE`. Las pasadas por crecer las cifras de `{pages}` no cuentan, porque
solo crecen. Con un medidor aditivo la paginación converge siempre: el error es una red de seguridad.

> **Decisión:** `{page of: id}` se sustituye antes de repartir el texto en líneas y no en un hueco reservado como `{page}`:
> el marcador tiene espacios y el reparto lo partiría en tres palabras, y con la convergencia no hace falta un segundo
> paso. Como la primera pasada supone `00`, con referencias a páginas siempre hay al menos dos. *Alternativa descartada:*
> quedarse con la última pasada sin avisar.

> **Decisión:** un `Id` que no existe (`REFERENCE_UNKNOWN`) y la página de una caja de un `Page overlay`
> (`REFERENCE_NOT_PLACEABLE`) se detectan al construir el árbol (`References`), porque dependen solo de su estructura. La
> de una caja que cae en una página sin número se detecta al paginar. `{number of: id}` sale del árbol y no depende de la paginación.

> **Decisión:** con `{pages}` y 100 páginas o más, o con números romanos, se vuelve a paginar reservando el ancho del
> número más largo, en lugar de dejar que el número real se salga del hueco reservado. En romanos se reserva una `M` por
> letra, la más ancha de las que escriben un romano. *Alternativa descartada:* medir cada número real en su línea, que
> cambiaría los saltos de línea de una página a otra.

**Volver a paginar no vacía la caché.** Cuando el número de prueba crece (`Engine.reservePageNumbers`) o cambia
la página de una referencia (`Engine.assume`), el `Paginator` pasa los textos que cambian de ancho y el motor olvida solo lo
que la `LayoutCache` y el reparto en líneas (`LineBreakingMemo`) guardan de ellos y de sus antepasados (`Engine.forget`), también el reparto de
un `Stack`, un `Grid` o una `Table` que los contenga. La caché y el reparto en líneas guardan todo por caja primero (un mapa
por caja con el resto de la clave), así que olvidar cuesta lo que las cajas olvidadas y no lo que hay guardado: un
`{heading 1}` en un `Page overlay` olvida su texto en cada página donde cambia el capítulo. Lo vigilan
`PaginationCacheTest`, `ArrangementCacheTest` y `LayoutCacheTest`.

> **Decisión:** volver a paginar solo olvida lo que depende de los textos que cambian, porque el resultado de una caja
> depende solo de su subárbol y de la clave (caja, espacio y token): los algoritmos no leen nada de fuera, salvo el estilo,
> que no cambia. Si un algoritmo nuevo leyera algo de fuera del subárbol, tendría que entrar en la clave. Un documento de 100
> páginas pasó de costar el doble que uno de 99 a costar lo mismo. *Alternativa descartada:* vaciar la caché entera.

**Notas al pie.** `FootnoteLayout` maqueta cada página de una sección con notas ([Footnote](../components/footnote.md)):
el flujo con `ConstraintSpace.forPage(pageSetup, reserva)`, que quita a la raíz la altura reservada al pie, y las notas de
las llamadas que caen en sus líneas (`TextFormat.footnote` en los tramos de las `TextLine`) apiladas en el área de notas
de la sección (`Section.footnotes()`, la caja generada `Footnotes`, hija de la sección fuera de su flujo). Como el cuerpo
es función de la reserva y las notas dependen del cuerpo, la reserva se busca por sondas: sin reserva; si las notas no
caben, la altura entera de esas notas (en la que las de cualquier reserva mayor caben, porque menos sitio es menos
flujo), salvo que no deje sitio al cuerpo (lo coloca forzado por la garantía de progreso y desborda: entonces, la mayor
reserva que no lo fuerza, por bisección); y desde una que cabe, hacia abajo hasta lo que sus notas necesitan y, donde
una menor trae una llamada que ya no cabe, por bisección entre las dos (`PRECISION`, 0,001 pt). Unas notas que no caben ni
con la reserva entera son `FOOTNOTE_TOO_TALL`, y una reserva que no asienta en 60 sondas (`MAX_PROBES`), un
`InternalException`. Cada nota se maqueta con
`context.layout` como cualquier bloque, así que la caché responde a las sondas repetidas (`FootnoteLayout` recibe el
`LayoutContext`, no el motor), y se apilan con `Stacking` como los hijos de un bloque: una nota que baja por los
márgenes que salen de ella por arriba se maqueta otra vez con lo que le queda; solo la última puede quedar
partida, con un corte sin violaciones (`BreakAppeal.PERFECT_BREAK`) y solo si el flujo continúa, y su resto
(`FootnoteLayout.Carry`) encabeza el área de la página siguiente. El área mide lo que miden sus notas y se ancla al final
del área útil: el sobrante de la reserva queda entre el texto y el filete. `Paginator.flow` guarda el fragmento del
área de cada página (`Flow.footnotes`), que pasa a `Page.footnotes`, numerado con `{page}` como el flujo, y `pagesOf`
también lo recorre: una nota tiene `Id` y se puede referir.

> **Decisión:** la reserva se busca por sondas sobre la página entera, con los algoritmos de layout intactos, en lugar de
> reservar línea a línea dentro del reparto, como TeX con sus inserciones: los algoritmos no leen nada de fuera de su
> subárbol y la caché sigue valiendo. El precio son unas sondas más por página con notas (tres en el caso corriente,
> hasta una veintena cuando una llamada queda justo en el límite). *Alternativa descartada:* un presupuesto de notas en
> `ConstraintSpace`, que entra en la clave de la caché y cambia todos los algoritmos.

> **Decisión:** una nota se parte solo si su corte no viola viudas, huérfanas ni `Keep`, y solo si el flujo sigue en otra
> página; si no, la línea que la llama pasa entera a la página siguiente. *Alternativa descartada:* partirla por la
> fuerza, que deja una llamada con una nota de una línea, o una página solo con el resto de la nota.

**Títulos corridos.** `RunningHeads` recorre las páginas en orden físico y guarda el encabezado de cada nivel en vigor: el
último que ha empezado (el `Fragment` con `first()` de una caja con `headingLevel() > 0`, con el texto de
`Box.headingText()`, que ya lleva el número), hasta que empieza uno de un nivel superior, que vacía los de debajo. Para
cada página da el valor de cada referencia del documento: `"2"`, el encabezado en vigor, y `"2: first"`, el primero de ese
nivel que empieza en la página o, si no empieza ninguno, el mismo que `"2"`; `pages` se lo pasa al motor
(`Engine.headings`) antes de colocar los `Page overlay` de la página, solo cuando difiere del de la anterior; el motor lo
guarda en `TextValues` como las páginas supuestas y olvida lo que la caché y el reparto en líneas tienen de los textos
que llevan un título corrido y de sus antepasados, así que esos `Page overlay` se maquetan de nuevo en cada página que
cambia, y el resto sale de la caché. El flujo ya está paginado y no se toca: un `Page overlay` no ocupa espacio.

> **Decisión:** el título corrido se sustituye en el texto antes de repartirlo en líneas, como `{page of: id}`, y no en un
> hueco reservado como `{page}`: su ancho cambia de una página a otra y no hay un número de prueba que lo cubra. El precio
> es maquetar el `Page overlay` una vez por página en lugar de una, solo si lleva alguno. *Alternativa descartada:*
> reservar el ancho del encabezado más largo, que dejaría huecos o desbordaría.

## Desbordamientos

`Overflow` recorre el resultado paginado y señala al culpable más interior ([Desbordamiento](../common/overflow.md)),
usando el `PageSetup` de cada página. Lo que contiene una caja con `Overflow: clip` no se comprueba contra ella (ni sus
líneas ni sus hijos) y, con el indicador `clipped` que baja por sus descendientes, tampoco contra la página; cada caja de
dentro se sigue comprobando contra su propio padre. Lo girado se comprueba como se pinta, por el rectángulo que lo
envuelve, con los giros de sus antepasados en un `Turns` ([El pintado](painting.md#giro)). `Engine.layout` maqueta una caja con `Overflow: clip` como una girada:
sin fragmentación, y `BEFORE` si no cabe en lo que queda de la página. Donde un texto podría encoger, `textOverflow` y
`tooTall` lo dicen, y donde ya encoge y no cabe a `Min size`, también. Las tolerancias son dos, con nombre: `Tolerance.EPSILON` (10⁻⁹), la de la aritmética del
layout, y `Tolerance.MODEL` (0,01 pt), lo que una caja puede salirse sin desbordar y la precisión de los volcados. Se
compara con `Tolerance.fits(size, space)` y `Tolerance.same(a, b)`, nunca con un `1e-9` propio.

> **Decisión:** los desbordamientos se buscan sobre el resultado paginado y no en cada algoritmo al colocar: así no cuentan
> las pasadas de medida, en las que una caja se maqueta con un espacio provisional. Solo dos se detectan al maquetar,
> porque sin sitio no se avanza: `TABLE_REPEAT_OVERFLOW` y los cortes de columna de más de un `Columns`.

> **Decisión:** dos tolerancias con nombre y no una: con `EPSILON` en `Overflow` el motor sería más estricto y una caja que se
> sale 0,005 pt sería un error. *Alternativa descartada:* una sola tolerancia, con ese cambio de comportamiento.

Después de `Overflow`, y salvo que quien convierte quite `Check.BARCODE_SIZE`, `ModuleSizes` recorre las mismas páginas
(flujo, área de notas y `Page overlay`) y, en cada fragmento con `Box.modules()`, compara la caja de contenido con
`Modules.minimumSize()`, el tamaño en el que los módulos miden `Modules.minModule()`, con la tolerancia `Tolerance.MODEL`:
el ancho y el alto en una matriz, y solo el ancho en una fila estirada (`Modules.fit()`). El primero que no llega es
`BARCODE_TOO_SMALL`, con la línea de su caja ([Barcode](../components/barcode.md#módulo-mínimo)). El mínimo lo pone el
generador en `Modules`, así que `ModuleSizes` no sabe de formatos.

> **Decisión:** el módulo mínimo se comprueba sobre el resultado paginado, como los desbordamientos, y no al medir la imagen
> (`Images.natural`), porque un código se maqueta con tamaños provisionales al medir un `Stack` o un `Grid`.
> *Alternativa descartada:* comprobarlo en `ImageLayout`, que daría errores por pasadas de medida.

## Hilos

Un `Engine` **no** se comparte entre hilos (caché del layout, reparto en líneas y páginas supuestas sin sincronizar): uno por
conversión, que es lo que hace `Converter`. Un `Engine` guardado se puede reutilizar en secuencia. Ver
[Uso desde Java](api.md#hilos).
