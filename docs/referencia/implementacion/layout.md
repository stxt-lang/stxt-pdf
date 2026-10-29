# El layout

Cómo calcula el motor la posición y el tamaño de cada caja (package `layout` y `fragment`). El comportamiento está en las
páginas de comportamiento común y de cada componente; aquí, cómo está hecho.

## `Engine` y `LayoutContext`

`Engine` despacha por `Style.layout` al algoritmo correspondiente (un `EnumMap`; la tabla, que es un grid, tiene el suyo).
Los algoritmos no conocen el motor: dependen de **`LayoutContext`** (`layout`, `intrinsic`, `contentIntrinsic`,
`measurer`, `natural`), que `Engine` implementa y que un test puede sustituir por un doble. **Toda llamada recursiva de
layout o de tamaños intrínsecos pasa por `context.layout(...)` / `context.intrinsic(...)`**, nunca directamente al
algoritmo, para usar la `LayoutCache` (sin ella, flex y grid anidados crecen de forma exponencial).

`Engine.layOutDocument` es la entrada: valida las fuentes y las imágenes, pagina con el `Paginator`, comprueba los
desbordamientos con `Overflow` y devuelve un `LaidOutDocument`. Al terminar (también si falla) suelta todo lo que memoriza.
El protocolo con el `Paginator` (`reservePageDigits`, `assume`, `number`, `absolute()`) es de package.

> **Decisión:** los algoritmos de layout dependen de la interfaz `LayoutContext` y no de `Engine`; el protocolo de la
> paginación se queda en `Engine`, de package, porque moverlo a otra clase no quitaba ninguna dependencia.

> **Decisión:** el protocolo de la paginación no es público: quien manejara un `Engine` desde fuera podría romper la
> numeración de otro documento. *Alternativa descartada:* un objeto de sesión por documento.

> **Decisión:** `layOutDocument` suelta la caché, el reparto en líneas y las páginas supuestas al terminar, así que un motor
> guardado no retiene el árbol del último documento y uno reutilizado da lo mismo que uno nuevo. No cuesta nada medible: la
> caché solo sirve dentro de un documento.

## `LayoutAlgorithm`

`contentIntrinsic(box)` (min/max-content del contenido, sin borde, relleno, margen ni `Width`; independiente del espacio) y
`layout(box, space, from)`. Los de la caja de margen los da `context.intrinsic(box)` con `BoxModel.intrinsic`; los del
contenido, `context.contentIntrinsic(box)` (el mínimo automático de un ítem de `Flex`).

## `ConstraintSpace` y `LayoutResult`

**`ConstraintSpace`** es la entrada inmutable del layout y parte de la clave de la caché: ancho disponible, alto (vacío =
indefinido), tamaños impuestos por el padre, bases de porcentajes, espacio restante en la página desde el borde superior de
la caja (vacío = sin fragmentación), si la caja empieza la página y si se mide el alto de su contenido ignorando su `Height`
(`measuringContentHeight()`).

**`LayoutResult`**: el `Fragment`, el `BreakToken` (`null` = la caja ha terminado), la calidad del corte
(`BreakAppeal.PERFECT_BREAK` … `FORCED`), las `PendingAbsolute` que suben hasta su contenedor de referencia (con la posición
estática relativa a la caja de contenido de quien devuelve el resultado) y los `AdjoiningMargins`.

## `BoxModel`

Margen, borde, relleno, ancho de la caja de borde y alto si es definido, a partir del `ConstraintSpace`. Es la parte común de
todos los algoritmos; `childSpace()` da el espacio de los hijos en flujo y `BoxModel.intrinsic(...)` pasa de tamaños
intrínsecos del contenido a los de la caja de margen. Los márgenes `auto` valen 0: el centrado lo hace el padre, que es quien
coloca al hijo. El dimensionado de una imagen está en `BoxModel.replaced`, para que las sondas de flex, grid y absolute midan
bien la imagen: se usa siempre `BoxModel.resolve(box, space, context)`, que le pasa el tamaño natural. Primero se aplica el
máximo y luego el mínimo; si chocan, gana el mínimo (`Sizing.constrain`).

## Colapso de márgenes

Cada caja devuelve en `AdjoiningMargins` los márgenes de sus descendientes que salen por arriba o por abajo (y si está vacía y
deja pasar los suyos); el padre los colapsa con los márgenes propios del hijo, que resuelve él. `MarginStrut` es inmutable y
guarda el mayor positivo y el más negativo por separado. `Box.establishesBlockFormattingContext()` dice qué cajas lo cortan.

## Cajas posicionadas

Los algoritmos no maquetan los hijos `Absolute` y `Fixed`; los devuelven como `PendingAbsolute` con su posición estática, y
los hijos `Relative` se desplazan con `RelativeOffset` después de colocarlos. `Engine.layout` resuelve con `AbsoluteLayout`
(sobre `AbsoluteEquation`) los `Absolute` cuyo contenedor de referencia es la caja recién maquetada (sus fragmentos cuelgan
del de esa caja); los `Fixed` suben hasta la raíz y el `Paginator` los coloca en `Page.fixed` de cada página, solo en las
que incluye su `Pages`.

## Fragmentos

Un **`Fragment`** tiene su posición relativa a la caja de **contenido** del padre; `StxtDump` la pasa a coordenadas absolutas
de página. Lleva su propio `border` y `padding`, que en un fragmento partido no son los de la caja: se usan siempre los del
fragmento para llegar a su caja de contenido. Lleva también el `margin` usado (con la base de porcentajes de su layout) y el
desplazamiento de `Relative` ya sumado a su posición (`offsetX`/`offsetY`), que `Overflow` necesita. La geometría solo
existe en los fragmentos: `Box` no guarda posición ni tamaño.

> **Decisión:** la geometría usada solo existe en los fragmentos: una caja se maqueta muchas veces (sondas, medidas,
> estirado, cada página) y la caché devuelve resultados sin volver a escribirla, así que «el último layout» de una caja no
> correspondería a ningún fragmento. El margen usado va en el fragmento, con la base de porcentajes del layout que lo
> produjo. *Alternativa descartada:* que `Overflow` resuelva los márgenes con el ancho de contenido del padre, que no es la
> base de los ítems de grid.

> **Decisión:** `Overflow` comprueba un `Relative` en su sitio con el desplazamiento que usó el layout, guardado en el
> fragmento, en lugar de recalcularlo: en un padre partido entre páginas la base de un porcentaje vertical no es el alto del
> fragmento, y recalcularlo rechazaba documentos válidos.

## Fragmentación

Cada algoritmo coloca lo que cabe en `fragmentainerRemaining` y devuelve el corte con su calidad; el padre lo compara con los
cortes entre sus hijos y se queda con el **último de mayor calidad** (`BreakRules.best`). Relajar las reglas en el orden de
[Cortes](../comun/cortes.md#elección-del-corte) sale solo de esa comparación: por eso un hijo que no puede respetar viudas y
huérfanas devuelve igualmente un corte (`VIOLATES_WIDOWS`) en lugar de `BEFORE`.

**`BreakToken`** (sealed, records con `equals`): `Block`, `Line`, `Container`, con el alto ya consumido por la caja (un
`Height` definido es el total de todos los fragmentos; un fragmento cortado solo consume lo que ocupa su contenido), y
`BEFORE`: «no cabe nada, corta antes de mí», que nunca se pasa como `from`. El `Paginator` detecta la falta de progreso
comparando tokens. El token de la raíz no acumula alto: cada página tiene el área útil entera.

`Slice` calcula el alto, el borde y el relleno de cada fragmento (`Decoration: slice` y `clone`; `Slice.contentRemaining` da
lo que queda al contenido). Flex y grid cortan entre unidades (líneas, ítems o filas; `fragment.Unit`) con `UnitBreaker`: se
maquetan enteros sin fragmentar (`UnitBreaker.Arranged`: ítems colocados y `Units`) y `UnitBreaker.fragment` se queda con lo
que cabe en la página. Una unidad que no cabe ni al principio de página parte sus ítems en paralelo, repitiendo su layout con
`ConstraintSpace.fragmented` y el espacio guardado en `UnitBreaker.Placed`, y el `BreakToken.Container` lleva el token de
cada ítem. Con `atFragmentainerStart` hay que colocar algo aunque desborde (garantía de progreso). Las tablas repiten
cabecera y pie con `UnitBreaker.Repeat`. Los cortes forzados son `PAGE` o `COLUMN` (`BreakRules.forcedBetween` devuelve el
tipo); el de página tiene la calidad `BreakAppeal.FORCED_PAGE`, por encima de `PERFECT_BREAK`.

`MulticolLayout` maqueta el flujo de columnas una vez por columna con `context.layout`, con el alto de la columna como
`fragmentainerRemaining` y el token de la columna anterior como `from`; cada columna es un fragmento suyo. El equilibrado es
una búsqueda binaria del alto mínimo.

## Texto

`InlineLayout` reparte el texto con `LineBreaker` (voraz o Knuth-Plass) o, con `pre` y `pre-wrap`, con `PreformattedText`.
`InlineLayout.runs` sustituye las referencias por su valor y aplica `TextTransform` a los tramos **antes** de repartirlos.
`TextLine.runs` guarda cada tramo colocado, con el espaciado de `justify` incluido. `MarkerLayout` coloca el marcador de un
`List item` en su primer fragmento, con la línea base de su primera línea (`TextMeasurer.vertical`).

**Medida.** Un `TextMeasurer` tiene que ser **aditivo entre palabras** (el ancho de un texto es la suma de los de sus palabras
y espacios): los dos repartos en líneas suman. `PdfMetrics` mide con `FontFile.width(texto, tamaño, ligaduras)`, que aplica
`GSUB` palabra a palabra si hay ligaduras. `Engine.layOutDocument` llama antes a `validate(root)`, que en `PdfMetrics`
comprueba que existen las fuentes y tienen todos los caracteres.

**Texto largo.** Cada página maqueta el texto desde otra línea, así que la `LayoutCache` no ayuda; `InlineLayout` memoriza
el reparto en líneas por caja y ancho disponible (si no, el coste crece con el cuadrado de las páginas; lo vigila
`LongTextTest`). El reparto voraz suma los avances de las palabras y los espacios de la línea en curso y solo mide la línea
entera cuando la suma queda a menos de `Tolerance.MODEL` del ancho disponible.

> **Decisión:** el ancho de un texto es aditivo entre palabras y el reparto voraz suma, midiendo la línea entera solo cerca
> del límite: la suma en coma flotante puede diferir en unos 10⁻¹³ pt de la medida de la línea, y sin esa comprobación algún
> corte justo en el límite cambiaba. Así el reparto es exactamente el de medir siempre la línea. *Alternativa descartada:*
> fiarse de la suma (cambia cortes en el límite) o medir en milésimas enteras, que ataría `TextMeasurer` a la forma de
> `FontFile`.

> **Decisión:** la línea base de un marcador de lista se calcula al maquetar con las métricas del medidor (con
> `FixedMetrics`, la aproximación 0,8/0,2; con `PdfMetrics`, la `hhea` de la fuente), no al pintar: el marcador es un
> fragmento normal, con su rectángulo en el volcado. El SVG pinta siempre con la aproximación, así que un marcador de otro
> tamaño que su primera línea puede quedar ligeramente desplazado en el SVG de un documento maquetado con las métricas
> reales.

## Paginación

`Paginator` hace una pasada de flujo por sección (`flow`) con **una sola convergencia** de las referencias, y calcula con
`Numbering` el número impreso de cada página (el contador, `restart` y `none`) y el último de su tanda (`{pages}`); `Page`
lleva su `Media`, su sección y su `printed` (0 = sin número). Un `Fixed` del documento se pone con `PendingAbsolute(box, 0, 0)`
en cada página de cada sección que lo hereda, y `Pages.includes` se evalúa con las páginas de su alcance.

**`{page}` y `{pages}`.** `InlineLayout` mide los marcadores como `00` y las líneas los conservan; al terminar de paginar, el
`Paginator` llama a `Engine.number`, que rehace solo esas líneas con los valores de cada página (`PageNumbers`).

**Referencias.** `Engine.assume` fija las páginas supuestas (la primera vez, `00`), se pagina, se mira en qué página ha
quedado el primer fragmento de cada caja referida (`Box.key()`) y se vuelve a paginar si alguna difiere, hasta 10 pasadas que
cambien algo; si no convergen, `REFERENCE_UNSTABLE`. Las pasadas por crecer las cifras de `{pages}` no cuentan, porque
solo crecen. Con un medidor aditivo la paginación converge siempre: el error es una red de seguridad.

> **Decisión:** `{page of: id}` se sustituye antes de repartir el texto en líneas, no en un hueco reservado como `{page}`:
> el marcador tiene espacios y el reparto lo partiría en tres palabras, y con la convergencia no hace falta un segundo
> paso. Como la primera pasada supone `00`, con referencias a páginas siempre hay al menos dos. *Alternativa descartada:*
> quedarse con la última pasada sin avisar.

> **Decisión:** un `Id` que no existe (`REFERENCE_UNKNOWN`) y la página de una caja de un `Fixed`
> (`REFERENCE_NOT_PLACEABLE`) se detectan al construir el árbol (`References`), porque dependen solo de su estructura; la
> de una caja que cae en una página sin número, al paginar. `{number of: id}` sale del árbol y no depende de la paginación.

> **Decisión:** con `{pages}` y 100 páginas o más se vuelve a paginar reservando las cifras del total, en lugar de dejar
> que el número real se salga del hueco reservado.

**Volver a paginar no vacía la caché.** Cuando el total de páginas necesita otra cifra (`Engine.reservePageDigits`) o cambia
la página de una referencia (`Engine.assume`), el `Paginator` pasa los textos que cambian de ancho y el motor olvida solo lo
que la `LayoutCache` y el reparto en líneas guardan de ellos y de sus antepasados (`Engine.forget`). Lo vigila
`PaginationCacheTest`.

> **Decisión:** volver a paginar solo olvida lo que depende de los textos que cambian, porque el resultado de una caja
> depende solo de su subárbol y de la clave (caja, espacio y token): los algoritmos no leen nada de fuera, salvo el estilo,
> que no cambia. Si un algoritmo nuevo leyera algo de fuera del subárbol, tendría que entrar en la clave. Un documento de 100
> páginas pasó de costar el doble que uno de 99 a costar lo mismo. *Alternativa descartada:* vaciar la caché entera.

## Desbordamientos

`Overflow` recorre el resultado paginado y señala al culpable más interior ([Desbordamiento](../comun/desbordamiento.md)),
usando el `Media` de cada página. Las tolerancias son dos, con nombre: `Tolerance.EPSILON` (10⁻⁹), la de la aritmética del
layout, y `Tolerance.MODEL` (0,01 pt), lo que una caja puede salirse sin desbordar y la precisión de los volcados. Se
compara con `Tolerance.fits(size, space)` y `Tolerance.same(a, b)`, nunca con un `1e-9` propio.

> **Decisión:** los desbordamientos se buscan sobre el resultado paginado, no en cada algoritmo al colocar: así no cuentan
> las pasadas de medida, en las que una caja se maqueta con un espacio provisional. Solo dos se detectan al maquetar,
> porque sin sitio no se avanza: `TABLE_REPEAT_OVERFLOW` y los cortes de columna de más de un `Multicol`.

> **Decisión:** dos tolerancias con nombre, no una: con `EPSILON` en `Overflow` el motor sería más estricto y una caja que se
> sale 0,005 pt sería un error. *Alternativa descartada:* una sola tolerancia, con ese cambio de comportamiento.

## Hilos

Un `Engine` **no** se comparte entre hilos (caché del layout, reparto en líneas y páginas supuestas sin sincronizar): uno por
conversión, que es lo que hace `Converter`. Un `Engine` guardado se puede reutilizar en secuencia. Ver
[Uso desde Java](api.md#hilos).
