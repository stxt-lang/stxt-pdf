# El layout

Cómo calcula el motor la posición y el tamaño de cada caja (package `layout` y `fragment`). El comportamiento está en las
páginas de comportamiento común y de cada componente, y aquí, cómo está hecho.

## `Engine` y `LayoutContext`

`Engine` despacha por `Style.layout` al algoritmo correspondiente (un `EnumMap`, y la tabla, que es un grid, tiene el suyo).
Los algoritmos no conocen el motor: dependen de **`LayoutContext`** (`layout`, `intrinsic`, `contentIntrinsic`,
`measurer`, `natural`), que `Engine` implementa y que un test puede sustituir por un doble. **Toda llamada recursiva de
layout o de tamaños intrínsecos pasa por `context.layout(...)` / `context.intrinsic(...)`**, nunca directamente al
algoritmo, para usar la `LayoutCache` (sin ella, `Stack` y `Grid` anidados crecen de forma exponencial).

`Engine.layOutDocument` es la entrada: valida las fuentes y las imágenes, pagina con el `Paginator`, comprueba los
desbordamientos con `Overflow` y devuelve un `LaidOutDocument`. Al terminar (también si falla) suelta todo lo que memoriza.
El protocolo con el `Paginator` (`reservePageNumbers`, `assume`, `number`, `absolute()`) es de package.

> **Decisión:** los algoritmos de layout dependen de la interfaz `LayoutContext` y no de `Engine`. El protocolo de la
> paginación se queda en `Engine`, de package, porque moverlo a otra clase no quitaba ninguna dependencia.

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
guarda el mayor positivo y el más negativo por separado. `Box.isolatesMargins()` dice qué cajas lo cortan.

## Grid

> **Decisión:** un ítem de grid estirado se maqueta siempre otra vez con su alto fijado, aunque mida lo mismo que al
> medirlo, para que sus hijos tengan la base definida que promete la regla del alto definido. Cuesta un layout más por ítem
> estirado. *Alternativa descartada:* repetirlo solo si algún descendiente usa porcentajes verticales.

## Cajas posicionadas

Los algoritmos no maquetan los hijos `Overlay` y `Page overlay`. Los devuelven como `PendingOverlay` con su posición estática, y
los hijos `Frame` se desplazan con `FrameOffset` después de colocarlos. `Engine.layout` resuelve con `OverlayLayout`
(sobre `OverlayEquation`) los `Overlay` cuyo contenedor de referencia es la caja recién maquetada (sus fragmentos cuelgan
del de esa caja). Los `Page overlay` suben hasta la raíz y el `Paginator` los coloca en `Page.fixed` de cada página, solo en las
que incluye su `Pages`.

## Cajas giradas en flujo

`Engine.layout` maqueta aparte una caja con `Style.rotatesInFlow()` (un `Block`, `Text`, `Paragraph` o `Image` con
`Rotate`): con `ConstraintSpace.fixed` y el ancho de `turnedWidth` (su `Width` resuelto o, con `auto`, su ancho natural más
borde y relleno), sin fragmentación y sin `from`. El resultado se envuelve con `Fragment.turned`: el fragmento mide el
rectángulo que envuelve su caja de borde girada sobre su centro, y `turned` guarda esa caja sin girar, centrada en él. Los
algoritmos de los padres no saben nada del giro: ven una caja del tamaño del rectángulo, que no se parte (`BEFORE` si no
cabe y no está al principio de la página) y cuyos márgenes de dentro no salen (`AdjoiningMargins.NONE`: aísla los
márgenes). `Engine.intrinsic` hace lo mismo con la base de porcentajes vacía: su ancho más estrecho y su
ancho natural son el ancho del rectángulo más los márgenes. Es contenedor de referencia de sus `Overlay`
(`Box.outOfFlowContainingBlock`), que se resuelven sobre la caja sin girar antes de envolverla.

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
da lo que queda al contenido. Stack y grid cortan entre unidades (líneas, ítems o filas, que son `fragment.Unit`) con `UnitBreaker`: se
maquetan enteros sin fragmentar (`UnitBreaker.Arranged`: ítems colocados y `Units`) y `UnitBreaker.fragment` se queda con lo
que cabe en la página. Una unidad que no cabe ni al principio de página parte sus ítems en paralelo, repitiendo su layout con
`ConstraintSpace.fragmented` y el espacio guardado en `UnitBreaker.Placed`, y el `BreakToken.Container` lleva el token de
cada ítem. Con `atFragmentainerStart` hay que colocar algo aunque desborde (garantía de progreso). Las tablas repiten
cabecera y pie con `UnitBreaker.Repeat`. Las franjas de las filas (`Box.rows()`, `Element.ROW`) son `UnitBreaker.Band`:
`GridLayout.arrange` las crea con el ancho de las columnas, `TableLayout` coloca las de la cabecera y el pie en el
`Repeat` y deja las del cuerpo en su unidad, y `UnitBreaker` las pone en cada fragmento delante de los ítems (enteras,
recortadas con `splitBands` si su grupo se parte, o la de la última unidad en la continuación del grupo). Son fragmentos
sin hijos: el pintado las trata como cualquier caja en flujo. Los cortes forzados son `PAGE` o `COLUMN`
(`BreakRules.forcedBetween` devuelve el tipo). El de página tiene la calidad `BreakAppeal.FORCED_PAGE`, por encima de `PERFECT_BREAK`.

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
`InlineLayout.runs` sustituye las referencias por su valor y aplica `TextTransform` a los tramos **antes** de repartirlos.
`TextLine.runs` guarda cada tramo colocado, con el espaciado de `justify` incluido. `MarkerLayout` coloca el marcador de un
`List item` en su primer fragmento, con la línea base de su primera línea (`TextMeasurer.vertical`).

**Medida.** Un `TextMeasurer` tiene que ser **aditivo entre palabras** (el ancho de un texto es la suma de los de sus palabras
y espacios): los dos repartos en líneas suman. `PdfMetrics` mide con `FontFile.width(texto, tamaño, ligaduras)`, que aplica
`GSUB` palabra a palabra si hay ligaduras. `Engine.layOutDocument` llama antes a `validate(root)`, que en `PdfMetrics`
comprueba que existen las fuentes y tienen todos los caracteres.

**Texto largo.** Cada página maqueta el texto desde otra línea, así que la `LayoutCache` no ayuda. `InlineLayout` memoriza
el reparto en líneas por caja y ancho disponible (sin esa memoria, el coste crece con el cuadrado de las páginas, y lo
vigila `LongTextTest`). El reparto voraz suma los avances de las palabras y los espacios de la línea en curso y solo mide la línea
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
sección, su `printed` (0 = sin número) y su estilo. Un `Page overlay` del documento se pone con `PendingOverlay(box, 0, 0)`
en cada página de cada sección que lo hereda, y `Pages.includes` se evalúa con las páginas de su alcance.

**Páginas en blanco.** Tras el flujo, `blankAfter` mira qué secciones dejan la siguiente, con `Begin on: odd`, en una
página física par. Esas acaban con una página en blanco: `Numbering` la cuenta como una página más de la sección, `pagesOf`
le reserva su número físico y `pages` la añade como una `Page` con `blank`, un fragmento vacío de la caja de la sección y
sin `Page overlay`. No forma parte del flujo de la sección, así que el `Pages` de sus `Page overlay` no la ve.

**`{page}` y `{pages}`.** `InlineLayout` mide los marcadores como un número de prueba (`00` al principio) y las líneas
los conservan. Al terminar de paginar, el `Paginator` llama a `Engine.number`, que rehace solo esas líneas con los
valores de cada página ya escritos (`PageNumbers`). Si algún número impreso no cabe en el de prueba (`Numbering.probe`:
tantos caracteres como el más largo, con `M` o `m` si hay romanos), se vuelve a paginar con uno que lo cubre
(`PageNumbers.cover`), que solo crece.

**Referencias.** `Engine.assume` fija las páginas supuestas (la primera vez, `00`), se pagina, se mira en qué página ha
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
que la `LayoutCache` y el reparto en líneas guardan de ellos y de sus antepasados (`Engine.forget`). Lo vigila
`PaginationCacheTest`.

> **Decisión:** volver a paginar solo olvida lo que depende de los textos que cambian, porque el resultado de una caja
> depende solo de su subárbol y de la clave (caja, espacio y token): los algoritmos no leen nada de fuera, salvo el estilo,
> que no cambia. Si un algoritmo nuevo leyera algo de fuera del subárbol, tendría que entrar en la clave. Un documento de 100
> páginas pasó de costar el doble que uno de 99 a costar lo mismo. *Alternativa descartada:* vaciar la caché entera.

## Desbordamientos

`Overflow` recorre el resultado paginado y señala al culpable más interior ([Desbordamiento](../common/overflow.md)),
usando el `PageSetup` de cada página. Las tolerancias son dos, con nombre: `Tolerance.EPSILON` (10⁻⁹), la de la aritmética del
layout, y `Tolerance.MODEL` (0,01 pt), lo que una caja puede salirse sin desbordar y la precisión de los volcados. Se
compara con `Tolerance.fits(size, space)` y `Tolerance.same(a, b)`, nunca con un `1e-9` propio.

> **Decisión:** los desbordamientos se buscan sobre el resultado paginado y no en cada algoritmo al colocar: así no cuentan
> las pasadas de medida, en las que una caja se maqueta con un espacio provisional. Solo dos se detectan al maquetar,
> porque sin sitio no se avanza: `TABLE_REPEAT_OVERFLOW` y los cortes de columna de más de un `Columns`.

> **Decisión:** dos tolerancias con nombre y no una: con `EPSILON` en `Overflow` el motor sería más estricto y una caja que se
> sale 0,005 pt sería un error. *Alternativa descartada:* una sola tolerancia, con ese cambio de comportamiento.

## Hilos

Un `Engine` **no** se comparte entre hilos (caché del layout, reparto en líneas y páginas supuestas sin sincronizar): uno por
conversión, que es lo que hace `Converter`. Un `Engine` guardado se puede reutilizar en secuencia. Ver
[Uso desde Java](api.md#hilos).
