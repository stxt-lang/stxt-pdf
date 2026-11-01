# El pintado

Cómo se dibuja un documento maquetado (package `paint`). Qué se dibuja está en [Capas y giro](../common/layers-and-rotation.md),
[La caja](../common/box.md#fondo-borde-y-esquinas) y [El PDF](../common/pdf.md).

## `StackingContext` y `Canvas`

`StackingContext` recorre los fragmentos de una página en el orden de pintado (contextos de apilamiento, `Layer`, fondos
antes que texto) y llama a un `Canvas`. Resuelve los radios de cada fragmento (`Radii.resolve` sobre su caja de borde, y con
`Edges: open`, `forFragment(first, last)` deja solo las esquinas de su lado) y llama a `Canvas.fill` (el fondo, con
`radii.inset(borde)`), `Canvas.border` (un color por lado, ya resuelto) e `image` (con los radios de la caja de contenido).
`Canvas.background(Rect, String)` queda para los rectángulos lisos (guías, líneas entre columnas, rayas del texto). La
línea base la calcula con `Canvas.vertical(font)` (`hhea` en PDF, la aproximación 0,8/0,2 en SVG). `Shapes` da el camino de
una caja redondeada (cuartos de elipse como Bézier) y el trapecio de cada lado del borde.

Cada `Piece` lleva en `frames` la cadena de giros y recortes de sus ancestros, de la página hacia dentro, y cada paso del
pintado se la aplica con `Canvas.beginRotation`/`endRotation` y `beginClip`/`endClip`, y después el giro de la propia caja:
una caja girada se pinta girada con todo lo que contiene, sea un contexto o gire en flujo. Una caja con esquinas
redondeadas añade a lo que tiene en flujo (también a un `Frame`) el recorte por la curva de su caja de relleno
(`Frame.Kind.CORNERS`), y una caja con `Overflow: clip` ([Desbordamiento](../common/overflow.md#recortar-clip)), el recorte
por su caja de relleno a todo lo que contiene (`Frame.Kind.WINDOW`). Un `Overlay` hereda los giros y las ventanas, no las
esquinas, y un `Page overlay`, nada. El fondo, el borde y la imagen se pintan con todos los recortes (`Clips.ALL`). El texto
se pinta con los giros y las ventanas (`Clips.WINDOWS`), cada línea con su tamaño (`TextLine.fontSize`, el de la caja o el
que `Overflow: shrink` le dejó), y el contorno de depuración (`Canvas.outlines()`), sin recortes (`Clips.NONE`), para que se
vea la caja entera. El paso de los fondos de una caja que no pinta nada, y el del contenido de una caja sin texto ni
imagen, no recortan. Las zonas de los enlaces las calcula
`StackingContext.paintLinks` al pintar (la caja de borde de cada fragmento con `Link`, y el rectángulo de cada tramo con
enlace de cada línea, uniendo los seguidos con el mismo destino) y llama a `Tagging.link`. `StackingContext.paintText` parte el
texto en secuencias de tramos con el mismo destino y añade el espacio invisible al final de las líneas partidas.

`Canvas` solo dibuja y cada lienzo da su `Tagging` con `Canvas.tagging()` (`PdfTagging` en el PDF y `Tagging.NONE` en el
SVG): el texto marcado del PDF etiquetado y los enlaces.

> **Decisión:** `Canvas` solo dibuja y cada lienzo da su `Tagging` con un método explícito y no con `instanceof`: un lienzo
> nuevo tiene que decir qué hace con el etiquetado. Las primitivas no declaran `IOException` y la convierte un único punto
> del PDF. *Alternativa descartada:* primitivas con `throws IOException`, que obligan a todos los lienzos por un fallo que
> solo da el PDF.

> **Decisión:** la zona de un enlace se calcula al pintar y no al maquetar: el layout no sabe nada de los enlaces, y el SVG de
> depuración los ignora. *Alternativa descartada:* guardar las zonas en el fragmento.

## El PDF

| Clase | Qué hace |
|---|---|
| `PdfDocumentWriter` | El documento: crea todas las páginas antes de pintar, pinta cada una, escribe la estructura, los marcadores, los destinos y las etiquetas de página, y guarda. Su `write` es la entrada |
| `PdfCanvas` | El dibujo de una página |
| `PdfContent` | El flujo de una página con el origen abajo: caminos, color, giros y `io`, el único punto que convierte las `IOException` |
| `PdfTagging` | El contenido marcado y las anotaciones de una página |
| `PdfFonts` | Las fuentes incrustadas y los argumentos de `TJ` |
| `PdfGradients` | Los sombreados de los degradados y sus máscaras suaves |
| `PdfImages` | Las imágenes: PNG y JPEG incrustados, SVG dibujados como vectores, páginas de PDF copiadas |
| `PdfPages` | La copia de una página de otro PDF en un formulario, sin su etiquetado |
| `SvgFonts` | Las fuentes de `fonts/` con que se dibuja el texto de un SVG |
| `PdfStructure` | El PDF etiquetado y los marcadores |
| `PdfMetadata` | El título, el idioma, el paquete XMP (PDF/UA, PDF/A y el XMP de los adjuntos) y el perfil de salida de PDF/A |
| `PdfAttachments` | Los ficheros adjuntos de PDF/A-3: las especificaciones de fichero, el árbol `EmbeddedFiles` y el array `AF` |
| `PdfLinks`, `PdfDestinations` | Las anotaciones de enlace y la esquina de cada caja con nombre (para marcadores y enlaces internos) |
| `ImageFit`, `Leaders`, `Shapes` | Dónde se dibuja una imagen según `Fit`, la guía de un índice y los caminos de las cajas |

> **Decisión:** los destinos internos se resuelven después de pintar todas las páginas, porque un enlace puede ir hacia
> delante: se crean todas las páginas antes de pintar ninguna y se busca la esquina del primer fragmento de cada caja con
> nombre, la misma de los marcadores. *Alternativa descartada:* destinos con nombre en el catálogo del PDF.

### Texto

Las fuentes son Type 0 Identity-H (cada carácter ocupa dos bytes), así que `justify`, el interletraje y el kerning se
escriben con ajustes de `TJ` entre los caracteres y no con `Tw` ni `Tc`. `PdfFonts.font` incrusta una fuente por combinación con
`TrueTypeFont.setEnableGsub(font.ligatures())`: sin ligaduras PDFBox no aplica `GSUB` (un glifo por carácter, de 17 a 30
veces más rápido) y lo medido es lo pintado.

El ancho de cada glifo se redondea a milésimas del cuerpo, que es lo que el PDF escribe en la tabla de anchos de la fuente
incrustada: así maquetar y pintar miden lo mismo con fuentes de cualquier `unitsPerEm`. La línea base sale del ascendente y
el descendente de la tabla `hhea`.

Con `Kerning: true`, `FontFile.kerning(texto, ligaduras)` da, para cada posición del texto, el ajuste en milésimas del
cuerpo que va antes del carácter que empieza en ella, y `PdfFonts.spaced` parte el texto en esas posiciones y escribe el
ajuste en `TJ`, con el signo cambiado (en `TJ` un número positivo acerca). La fuente incrustada es la misma con y sin
kerning: `PdfFonts.font` la busca sin esa opción. Los pares los lee `KerningPairs` la primera vez que se piden: de
`GPOS`, los *lookups* de ajuste de pares (tipo 2, formatos 1 y 2, también dentro de extensiones, tipo 9) de las
características `kern`; si no los hay, la tabla `kern`, a través de fontbox. Una tabla que no se puede leer es un
`ResourceException`.

> **Decisión:** con ligaduras, solo hay par donde la palabra se puede partir sin cambiar sus glifos: donde lo que `GSUB`
> deja de las dos partes, una tras otra, son los glifos de la palabra entera. PDFBox aplica `GSUB` a cada trozo de `TJ`
> por separado, así que partir dentro de una ligadura la desharía. Si los trozos de todas las posiciones con kerning
> juntos no dan los glifos de la palabra, la palabra va sin kerning. *Alternativa descartada:* escribir los glifos uno
> a uno sin pasar por `showText`, que obligaría a rehacer el `ToUnicode` de las ligaduras.

> **Decisión:** el ascendente y el descendente salen de `hhea` y no de `OS/2`: ni de `sTypo*`, aunque la fuente
> marque `USE_TYPO_METRICS`, ni de `usWin*`. En IBM Plex Sans, `hhea` da 1025 y 275 por 1000, igual que `usWin*`, y
> `sTypo*`, 780 y 220.

> **Decisión:** la justificación y el interletraje van con `TJ`: con la codificación Identity-H, `Tw` no tiene efecto. En el
> SVG son `word-spacing` y `letter-spacing`.

Con `Ligatures: true`, `FontFile.width` aplica `GSUB` palabra a palabra con el mismo procesador que usa PDFBox al pintar
(`ccmp`, `liga` y `clig` para escritura latina), así que una ligadura no cruza un espacio ni un cambio de tramo y lo
medido son los glifos que se escriben.

> **Decisión:** el mismo procesador de `GSUB` que PDFBox. *Alternativa descartada:* leer solo la característica `liga`
> de la tabla, con otro algoritmo que el del PDF.

El subrayado y el tachado usan las métricas de la variante del tramo: `underlinePosition` y `underlineThickness` de la
tabla `post`, y `yStrikeoutPosition` y `yStrikeoutSize` de `OS/2`, medidos desde la línea base hasta la parte de arriba de
la raya, que es lo que dice la especificación OpenType. Se pintan como *artifacts*.

> **Decisión:** las opciones de las ligaduras y del kerning viajan en `FontFace` (sus componentes cuarto y quinto, y
> `base()` es la fuente sin ellas): así las usan sin cambios `TextMeasurer`, `Canvas.text` y la caché de fuentes del
> PDF, y un fichero se identifica igual con y sin ellas. *Alternativa descartada:* un parámetro más en `TextMeasurer.width` y en `Canvas.text`.

> **Decisión:** cada fichero de fuente se lee una vez, al indexar la carpeta: lo que se incrusta y se dibuja sale de esos bytes
> (`FontFile.content()`), así que es lo que se midió aunque el fichero cambie o desaparezca, y pintar no puede fallar con una
> `IOException` de una fuente. *Alternativa descartada:* una copia propia de los bytes, que duplica la memoria, o leer el
> fichero en cada conversión.

### Bordes, fondos y degradados

Un borde liso, opaco y de esquinas cuadradas son cuatro rectángulos. Si no, es un anillo par-impar, recortado a cada trapecio
con colores distintos. El degradado es un sombreado axial (función de tipo 2, o de tipo 3 con más de dos colores) recortado
a la caja, con una máscara suave de luminosidad si algún color no es opaco, porque un sombreado de PDF no tiene opacidad por
color. La opacidad de un color liso es un estado gráfico `ca` dentro de un `q`/`Q` propio.

> **Decisión:** un borde liso, opaco y de esquinas cuadradas se pinta como cuatro rectángulos y no como un anillo, porque el
> anillo par-impar deja un antialiasing distinto en el borde de la caja y cambiaría las imágenes de referencia. El anillo se
> usa con esquinas redondas, con opacidad y con colores distintos.

> **Decisión:** `Canvas.border` recibe un color por lado, ya resuelto, `Canvas.fill` pinta el fondo de una caja con sus
> radios e `image` recibe las esquinas con que recortar. `background(Rect, String)` queda para los rectángulos lisos.

### Giro

`PdfCanvas` escribe un `cm` dentro de `q`/`Q` y guarda el giro compuesto para los `QuadPoints` de los enlaces y el `BBox` de
las figuras. Las coordenadas de `Rotation` son las de la página, con la y hacia abajo: un ángulo positivo gira en el sentido
de las agujas del reloj (en el PDF, con el origen abajo, el ángulo cambia de signo).

> **Decisión:** la anotación de un enlace girado tiene como `Rect` el rectángulo que envuelve la zona girada y como
> `QuadPoints` la zona girada. El `BBox` de una figura girada y el destino de una caja girada son los del rectángulo que la
> envuelve. *Alternativa descartada:* solo el envolvente, sin `QuadPoints`.

### Imágenes

`PdfImages` incrusta el PNG y el JPEG con PDFBox y el SVG con JSVG 2.2.0 (`ResourcePolicy.DENY_ALL`) dibujando en un
`PdfBoxGraphics2D` (`pdfbox-graphics2d` 3.0.5), que da un formulario del tamaño natural, una vez por fichero.
`PdfCanvas.image` lo coloca con una matriz `cm` de escala a donde manda `ImageFit` y restablece los guiones antes de
dibujarlo. JSVG dibuja el texto como contornos.

> **Decisión:** el texto de un SVG son contornos y no texto del PDF, porque JSVG no permite otra cosa. *Alternativa
> descartada:* que el motor dibujara el texto de los SVG con sus propias fuentes, que obligaba a reimplementar la
> disposición del texto de SVG.

> **Decisión:** un formulario hereda el estado gráfico, y el modo de depuración deja activo el patrón de guiones de los
> contornos: `PdfCanvas.image` restablece los guiones (`[] 0 d`) dentro de su `q ... Q` antes de dibujar el formulario.

La página de un PDF la copia `PdfPages` con `LayerUtility.importPageAsForm` de PDFBox, que copia su contenido y sus
recursos (los compartidos entre páginas, una vez) y no sus anotaciones. El formulario lleva el recorte de la página como
`BBox` y una matriz propia que lo gira según `Rotate` y lo lleva al origen (`PdfPages.placement`), así que mide lo que
`ImageFile.width(page)` y se coloca igual que un SVG. Después se le quita el etiquetado del PDF de origen: las marcas de
contenido (`BMC`, `BDC`, `EMC`, `MP`, `DP`) de su contenido y del de los formularios, los patrones de mosaico y los glifos
de las fuentes Type 3 que usa, salvo las de contenido opcional (`/OC`), y las entradas `StructParent` y `StructParents`. Los PDF de origen se abren una vez por fichero y se
cierran después de guardar. `PdfInspector` (`resources`) lee al cargar el documento el tamaño de cada página, con el
mismo giro (`PdfInspector.rotation`), y lo que impide dibujarla igual, por separado para cada `Check`: una anotación que
se ve que no es un enlace (`PDF_IMAGE_ANNOTATIONS`) y una fuente que no cumple PDF/UA (`PDF_IMAGE_FONTS`). En los recursos
de la página y en los de sus formularios, sus patrones y sus fuentes Type 3 busca una fuente sin incrustar, sin `ToUnicode`
(salvo una simple que no es simbólica), una TrueType con una codificación que PDF/UA no admite o una CIDFont de tipo 2 sin
`CIDToGIDMap`. Después, `PdfGlyphs` (un `PDFStreamEngine` de PDFBox) recorre el contenido de la página, sus formularios,
los glifos Type 3 que dibuja y sus patrones de mosaico, y mira una vez cada código de cada fuente: que no sea `.notdef`, que
diga qué carácter es y que su ancho sea el del programa de fuente. `Images` lanza el error de las que no se han quitado.
`LayerUtility` copia también las capas (`OCProperties`) al catálogo, y `PdfPages` pone el nombre del fichero a cada
configuración sin `Name` (la `D` y las de `Configs`) y le quita sus `AS`: PDF/UA pide lo primero y no admite lo segundo
(ISO 14289-1, 7.10).

> **Decisión:** el etiquetado del PDF de origen se quita del contenido copiado: sus `MCID` no son de este documento, y un
> *artifact* o un contenido marcado dentro de la figura de la `Image` incumple PDF/UA. Las marcas de contenido opcional se
> conservan porque deciden lo que se ve. *Alternativa descartada:* copiar el contenido tal cual.

> **Decisión:** la matriz del giro es propia y no la de `LayerUtility`, que conserva el tamaño sin girar y deforma una
> página girada 90°.

Una imagen generada, como un `Barcode` (`Box.modules()`), no pasa por `PdfImages`: `PdfCanvas.modules` coloca la matriz
con `ImageFit` (`Modules.fit()`: `inside` si tiene varias filas, `stretch` si tiene una) y escribe un rectángulo por cada
tramo de módulos oscuros seguidos de cada fila (`Modules.runs`), todos en un mismo trazado que se rellena una vez (`f`) con
el color de la tipografía, dentro del recorte de las esquinas y del contenido marcado de su figura.

> **Decisión:** un solo trazado y un solo relleno, y no un relleno por módulo: dos rectángulos rellenados por separado
> dejan ver una línea fina entre ellos en algunos visores por el suavizado, que en un QR se lee como un módulo claro.

> **Decisión:** JSVG y `pdfbox-graphics2d` para los SVG, sin dependencias propias. *Alternativa descartada:* Apache Batik, que
> arrastra decenas de jars.

**Nada se da de alta en AWT.** `SvgInspector` apunta la fuente de cada `<text>` y `<tspan>` con la herencia del fichero
(`ImageFile.textFonts`) y `SvgFonts`, el `DomProcessor` de JSVG, le pone una familia clave (`stxt-font-` y la huella del
contenido, `FontFile.digest`) con su peso y su estilo explícitos, y la da `PlatformSupport.customFont` con `Font.createFont`.
La caché de fuentes de JSVG es de toda la JVM y no está sincronizada: un SVG con texto se dibuja dentro de
`PdfImages.JSVG_FONTS`.

> **Decisión:** la clave de cada fuente es la huella de su contenido porque JSVG guarda las fuentes en una caché de toda la
> JVM cuya clave no incluye el `PlatformSupport`: así lo que guarda vale para cualquier carpeta con ese mismo fichero y crece
> con los ficheros distintos y no con las conversiones.

> **Decisión:** los SVG con texto se dibujan de uno en uno, dentro de un cerrojo de clase, porque la caché de fuentes de
> JSVG es un `HashMap` estático sin sincronizar. Medido con ocho hilos: el mismo tiempo que sin cerrojo. *Alternativa
> descartada:* resolver las fuentes sin caché, que JSVG no deja elegir.

### PDF etiquetado

`PdfStructure` construye el árbol de estructura con el contenido marcado (`Tagging.beginText`/`endText`, y los fondos, los
bordes y los `Page overlay` son *artifacts*) y los marcadores. `PdfStructure.type` es un `switch` exhaustivo sobre `Element.Role`, así que un
elemento nuevo con un papel que ya existe no toca `paint`. `PdfStructure.build` crea el `Link` de una caja con `Link` como
hijo de su elemento y padre de su contenido, `PdfStructure.markLink`, el de un enlace de texto, y
`PdfStructure.outside`, el `Link` suelto de lo que no es contenido.

> **Decisión:** `Element.Role` es el papel de una caja, compartido por varios elementos, y es lo único que el pintado mira:
> los nombres de las etiquetas del PDF están en `PdfStructure`. *Alternativa descartada:* métodos del PDF en `Element`, que
> ponen el PDF en `tree`.

## El contraste

`ContrastCheck` es un `Canvas` que no dibuja: apunta, en el orden de pintado, la zona y los colores de cada fondo
(`fill` y `background`) y la zona de cada imagen, en coordenadas de la página (con el giro aplicado). En cada `text` mira
el punto de la primera letra (un cuarto del cuerpo a la derecha y un tercio por encima de la línea base, donde no llega el
subrayado) y recorre lo apuntado que lo contiene: un color opaco sustituye lo de debajo, uno translúcido se mezcla con cada
color posible de debajo y una imagen lo deja sin conocer hasta que un color opaco la tapa. La caja del texto la sabe por su
`Tagging` (`beginText`). Los bordes no se apuntan y las esquinas redondeadas cuentan como rectas. Lo usa
`Converter.layOut`, sobre las páginas ya maquetadas ([El PDF](../common/pdf.md#contraste)).

## El SVG de depuración

`SvgCanvas` escribe el SVG de los tests: `<path>` para las cajas redondeadas, `linearGradient` y `clipPath`, y `<rect>` para
lo liso y cuadrado, un `<g transform>` para el giro, los módulos de un `Barcode` como un `<path>` con un rectángulo por
tramo y las imágenes como `data:` (la página de un PDF, dibujada en un PNG a
144 ppp con `PDFRenderer`, porque un navegador no dibuja un PDF dentro de un SVG). No pinta enlaces ni distingue las ligaduras.
Con `Kerning` y la carpeta de recursos, escribe los ajustes de los pares como `dx`, con `font-kerning: none` para que el
visor no sume los suyos.

> **Decisión:** el SVG conserva los espacios con `xml:space="preserve"` cuando un texto tiene dos espacios seguidos o empieza
> o acaba en espacio, y escribe las líneas con `\n` en cualquier sistema.
