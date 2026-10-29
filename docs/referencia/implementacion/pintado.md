# El pintado

Cómo se dibuja un documento maquetado (package `paint`). Qué se dibuja está en [Capas y giro](../comun/capas-y-giro.md),
[La caja](../comun/caja.md#fondo-borde-y-esquinas) y [El PDF](../comun/pdf.md).

## `StackingContext` y `Canvas`

`StackingContext` recorre los fragmentos de una página en el orden de pintado (contextos de apilamiento, `Layer`, fondos
antes que texto) y llama a un `Canvas`. Resuelve los radios de cada fragmento (`Radii.resolve` sobre su caja de borde; con
`Decoration: slice`, `forFragment(first, last)` deja solo las esquinas de su lado) y llama a `Canvas.fill` (el fondo, con
`radii.inset(borde)`), `Canvas.border` (un color por lado, ya resuelto) e `image` (con los radios de la caja de contenido);
`Canvas.background(Rect, String)` queda para los rectángulos lisos (guías, líneas entre columnas, rayas del texto). La
línea base la calcula con `Canvas.vertical(font)` (`hhea` en PDF, la aproximación 0,8/0,2 en SVG). `Shapes` da el camino de
una caja redondeada (cuartos de elipse como Bézier) y el trapecio de cada lado del borde.

El giro envuelve el contexto de la caja girada en `Canvas.beginRotation`/`endRotation`. Las zonas de los enlaces las calcula
`StackingContext.paintLinks` al pintar (la caja de borde de cada fragmento con `Link`; el rectángulo de cada tramo con enlace
de cada línea, uniendo los seguidos con el mismo destino) y llama a `Tagging.link`. `StackingContext.paintText` parte el
texto en secuencias de tramos con el mismo destino y añade el espacio invisible al final de las líneas partidas.

`Canvas` solo dibuja y cada lienzo da su `Tagging` con `Canvas.tagging()` (`PdfTagging` en el PDF y `Tagging.NONE` en el
SVG): el texto marcado del PDF etiquetado y los enlaces.

> **Decisión:** `Canvas` solo dibuja y cada lienzo da su `Tagging` con un método explícito, no con `instanceof`: un lienzo
> nuevo tiene que decir qué hace con el etiquetado. Las primitivas no declaran `IOException` y la convierte un único punto
> del PDF. *Alternativa descartada:* primitivas con `throws IOException`, que obligan a todos los lienzos por un fallo que
> solo da el PDF.

> **Decisión:** la zona de un enlace se calcula al pintar, no al maquetar: el layout no sabe nada de los enlaces, y el SVG de
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
| `PdfImages` | Las imágenes: PNG y JPEG incrustados, SVG dibujados como vectores |
| `SvgFonts` | Las fuentes de `fonts/` con que se dibuja el texto de un SVG |
| `PdfStructure` | El PDF etiquetado y los marcadores |
| `PdfLinks`, `PdfDestinations` | Las anotaciones de enlace y la esquina de cada caja con nombre (para marcadores y enlaces internos) |
| `ImageFit`, `Leaders`, `Shapes` | Dónde se dibuja una imagen según `Fit`, la guía de un índice y los caminos de las cajas |

> **Decisión:** los destinos internos se resuelven después de pintar todas las páginas, porque un enlace puede ir hacia
> delante: se crean todas las páginas antes de pintar ninguna y se busca la esquina del primer fragmento de cada caja con
> nombre, la misma de los marcadores. *Alternativa descartada:* destinos con nombre en el catálogo del PDF.

### Texto

Las fuentes son Type 0 Identity-H (cada carácter ocupa dos bytes), así que `justify` y el interletraje se escriben con
ajustes de `TJ` entre los caracteres, no con `Tw` ni `Tc`. `PdfFonts.font` incrusta una fuente por combinación con
`TrueTypeFont.setEnableGsub(font.ligatures())`: sin ligaduras PDFBox no aplica `GSUB` (un glifo por carácter, de 17 a 30
veces más rápido) y lo medido es lo pintado.

El ancho de cada glifo se redondea a milésimas del cuerpo, que es lo que el PDF escribe en la tabla de anchos de la fuente
incrustada: así maquetar y pintar miden lo mismo con fuentes de cualquier `unitsPerEm`. La línea base sale del ascendente y
el descendente de la tabla `hhea`.

> **Decisión:** sin kerning: aplicar el de las tablas `kern`/`GPOS` obligaría a colocar cada glifo por separado en el PDF.

> **Decisión:** el ascendente y el descendente salen de `hhea`, no de `OS/2` (`sTypo*`, ni cuando la fuente marca
> `USE_TYPO_METRICS`; ni `usWin*`). En IBM Plex Sans, `hhea` da 1025 y 275 por 1000, igual que `usWin*`; `sTypo*`, 780 y
> 220.

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

> **Decisión:** la opción de las ligaduras viaja en `FontFace` (su cuarto componente; `base()` es la fuente sin ella): así
> la usan sin cambios `TextMeasurer`, `Canvas.text` y la caché de fuentes del PDF, y un fichero se identifica igual con y
> sin ligaduras. *Alternativa descartada:* un parámetro más en `TextMeasurer.width` y en `Canvas.text`.

> **Decisión:** cada TTF se lee una vez, al indexar la carpeta: lo que se incrusta y se dibuja sale de esos bytes
> (`FontFile.content()`), así que es lo que se midió aunque el fichero cambie o desaparezca, y pintar no puede fallar con una
> `IOException` de una fuente. *Alternativa descartada:* una copia propia de los bytes, que duplica la memoria, o leer el
> fichero en cada conversión.

### Bordes, fondos y degradados

Un borde liso, opaco y de esquinas cuadradas son cuatro rectángulos; si no, un anillo par-impar, recortado a cada trapecio
con colores distintos. El degradado es un sombreado axial (función de tipo 2, o de tipo 3 con más de dos colores) recortado
a la caja, con una máscara suave de luminosidad si algún color no es opaco, porque un sombreado de PDF no tiene opacidad por
color; la opacidad de un color liso es un estado gráfico `ca` dentro de un `q`/`Q` propio.

> **Decisión:** un borde liso, opaco y de esquinas cuadradas se pinta como cuatro rectángulos, no como un anillo, porque el
> anillo par-impar deja un antialiasing distinto en el borde de la caja y cambiaría las imágenes de referencia. El anillo se
> usa con esquinas redondas, con opacidad y con colores distintos.

> **Decisión:** `Canvas.border` recibe un color por lado, ya resuelto, `Canvas.fill` pinta el fondo de una caja con sus
> radios e `image` recibe las esquinas con que recortar; `background(Rect, String)` queda para los rectángulos lisos.

### Giro

`PdfCanvas` escribe un `cm` dentro de `q`/`Q` y guarda el giro compuesto para los `QuadPoints` de los enlaces y el `BBox` de
las figuras. Las coordenadas de `Rotation` son las de la página, con la y hacia abajo: un ángulo positivo gira en el sentido
de las agujas del reloj (en el PDF, con el origen abajo, el ángulo cambia de signo).

> **Decisión:** la anotación de un enlace girado tiene como `Rect` el rectángulo que envuelve la zona girada y como
> `QuadPoints` la zona girada; el `BBox` de una figura girada y el destino de una caja girada son los del rectángulo que la
> envuelve. *Alternativa descartada:* solo el envolvente, sin `QuadPoints`.

### Imágenes

`PdfImages` incrusta el PNG y el JPEG con PDFBox y el SVG con JSVG 2.2.0 (`ResourcePolicy.DENY_ALL`) dibujando en un
`PdfBoxGraphics2D` (`pdfbox-graphics2d` 3.0.5), que da un formulario del tamaño natural, una vez por fichero.
`PdfCanvas.image` lo coloca con una matriz `cm` de escala a donde manda `ImageFit` y restablece los guiones antes de
dibujarlo. JSVG dibuja el texto como contornos.

> **Decisión:** un formulario hereda el estado gráfico, y el modo de depuración deja activo el patrón de guiones de los
> contornos: `PdfCanvas.image` restablece los guiones (`[] 0 d`) dentro de su `q … Q` antes de dibujar el formulario.

> **Decisión:** JSVG y `pdfbox-graphics2d` para los SVG, sin dependencias propias. *Alternativa descartada:* Apache Batik, que
> arrastra decenas de jars.

**Nada se da de alta en AWT.** `SvgInspector` apunta la fuente de cada `<text>` y `<tspan>` con la herencia del fichero
(`ImageFile.textFonts`) y `SvgFonts`, el `DomProcessor` de JSVG, le pone una familia clave (`stxt-font-` y la huella del
contenido, `FontFile.digest`) con su peso y su estilo explícitos, y la da `PlatformSupport.customFont` con `Font.createFont`.
La caché de fuentes de JSVG es de toda la JVM y no está sincronizada: un SVG con texto se dibuja dentro de
`PdfImages.JSVG_FONTS`.

> **Decisión:** la clave de cada fuente es la huella de su contenido porque JSVG guarda las fuentes en una caché de toda la
> JVM cuya clave no incluye el `PlatformSupport`: así lo que guarda vale para cualquier carpeta con ese mismo fichero y crece
> con los ficheros distintos, no con las conversiones.

> **Decisión:** los SVG con texto se dibujan de uno en uno, dentro de un cerrojo de clase, porque la caché de fuentes de
> JSVG es un `HashMap` estático sin sincronizar. Medido con ocho hilos: el mismo tiempo que sin cerrojo. *Alternativa
> descartada:* resolver las fuentes sin caché, que JSVG no deja elegir.

### PDF etiquetado

`PdfStructure` construye el árbol de estructura con el contenido marcado (`Tagging.beginText`/`endText`; fondos, bordes y
`Fixed` son *artifacts*) y los marcadores. `PdfStructure.type` es un `switch` exhaustivo sobre `Element.Role`, así que un
elemento nuevo con un papel que ya existe no toca `paint`. `PdfStructure.build` crea el `Link` de una caja con `Link` como
hijo de su elemento y padre de su contenido, y `PdfStructure.markLink` el de un enlace de texto; `PdfStructure.outside` el
`Link` suelto de lo que no es contenido.

> **Decisión:** `Element.Role` es el papel de una caja, compartido por varios elementos, y es lo único que el pintado mira:
> los nombres de las etiquetas del PDF están en `PdfStructure`. *Alternativa descartada:* métodos del PDF en `Element`, que
> ponen el PDF en `tree`.

## El SVG de depuración

`SvgCanvas` escribe el SVG de los tests: `<path>` para las cajas redondeadas, `linearGradient` y `clipPath`, y `<rect>` para
lo liso y cuadrado; un `<g transform>` para el giro; las imágenes, como `data:`. No pinta enlaces ni distingue las ligaduras.

> **Decisión:** el SVG conserva los espacios con `xml:space="preserve"` cuando un texto tiene dos espacios seguidos o empieza
> o acaba en espacio, y escribe las líneas con `\n` en cualquier sistema.
