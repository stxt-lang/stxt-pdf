# Hoja de ruta: revisión de QA y arquitectura (SOLID, DRY, KISS), hilos y rendimiento

Resultado de la revisión del código de 2026-10-28 (sesión 0, ver el [registro](#registro-de-sesiones)) y plan para
corregirlo en varias sesiones separadas. Es independiente de [`ROADMAP.md`](ROADMAP.md) (hitos de funcionalidad,
terminados): aquí no se añade comportamiento nuevo, salvo las decisiones que se marcan como abiertas.

Todo lo que dice este documento como **medido** o **verificado** se ha ejecutado de verdad; lo que no, está marcado
como *no verificado*. Los programas con que se midió están en [`tools/qa/`](../tools/qa/README.md) para repetirlo.

## Cómo usar este documento

**Al empezar una sesión:**

1. Lee el [resumen](#resumen), la [línea base](#línea-base-medida) y el [registro de sesiones](#registro-de-sesiones).
2. Elige la siguiente [sesión del plan](#plan-de-sesiones). Cada una se puede hacer sola: sus tareas tienen
   evidencia, propuesta y criterio de aceptación.
3. Ejecuta `mvn test` para partir de verde (1388 tests tras la sesión 6) y comprueba que `git status` queda limpio.

**Al terminar una sesión:**

1. Marca las tareas hechas (`[x]`) y actualiza la columna *Estado* de la tabla.
2. Si una tarea cambia una decisión de interpretación o una diferencia con la especificación, apúntala en
   [`DECISIONES.md`](DECISIONES.md).
3. Repite la medición que corresponda (`tools/qa/`) y cambia la línea base si ha cambiado.
4. Añade una entrada al [registro de sesiones](#registro-de-sesiones): fecha, qué se hizo, en qué se quedó y el
   siguiente paso concreto.

**Reglas para todas las sesiones:** ningún cambio de este documento puede modificar un PNG ni un `expected.stxt`
de `test/` (son refactorizaciones o correcciones sin cambio visible, salvo la tarea que lo diga); un cambio de
comportamiento de la especificación se habla antes con el usuario; cada corrección de un fallo lleva su test, que
falla sin ella.

**Estados:** `pendiente` · `decidir` (hay una decisión abierta para el usuario) · `en curso` · `hecho`.
**Prioridad:** `Alta` (fallo o riesgo real hoy), `Media` (deuda que frena o riesgo condicionado), `Baja` (limpieza).
**Esfuerzo:** `S` (menos de una hora), `M` (una sesión corta), `L` (una sesión entera o más).

## Resumen

El código está en muy buen estado: 1314 tests en verde, cobertura de instrucciones del 93 al 100 % por package,
dependencias entre packages en un solo sentido (`model` ← `tree` ← `fragment` ← `layout` ← `paint`; verificado
con los `import`: `tree` no importa nada de `layout`, `fragment` ni `paint`, y `layout` no importa `paint`),
casi nada de código copiado (ver [DRY](#dry)) y un modelo inmutable en el layout. Lo que se ha encontrado:

- **Un fallo real**: la numeración en mayúsculas de las listas depende del idioma de la JVM (QA-01); con
  `-Duser.language=tr` fallan 5 tests y el número romano `IV` sale como `İV`.
- **Un problema de coherencia y de rendimiento a la vez**: PDFBox aplica las ligaduras de la fuente (GSUB) al
  pintar y el motor mide sin ellas (QA-03). Es además el 70-75 % del tiempo en documentos de texto largo.
- **Hilos**: la conversión es segura entre hilos **si cada conversión tiene su `Engine`**, que es lo que hace
  `PdfWriter`; ocho hilos convirtiendo 880 documentos dieron los mismos bytes que en secuencial. Pero `Engine` es
  público, mutable y no es seguro, nada lo dice, y hay un estado global de JVM (`registerFont`) y dos cerrojos en la
  ruta caliente que cuestan un 20 % de rendimiento (TS-01…TS-04).
- **Arquitectura**: dos clases dios (`Style` con 70 campos públicos mutables y `BoxTreeBuilder.element` de 162
  líneas y complejidad ≈ 67), la interfaz `Canvas` con 11 métodos por defecto que son del PDF, y los algoritmos
  atados a la clase concreta `Engine`. Nada de esto es un fallo; frena cambiar cosas con seguridad.
- **Falta de red de seguridad fuera de los tests unitarios**: no hay integración continua (no existe `.github/`),
  ni prueba de hilos, ni de idiomas, ni de límites.

## Estado

| ID | Tarea | Prio | Esf. | Estado |
|---|---|---|---|---|
| QA-01 | `ListMarkers`: mayúsculas con el `Locale` de la JVM | Alta | S | hecho |
| QA-02 | El texto de un SVG cambia con el idioma de la JVM | Media | M | hecho |
| QA-03 | PDFBox aplica GSUB (ligaduras) y el motor mide sin ellas | Alta | M | hecho |
| QA-04 | `Rotation.CENTER` es un array público mutable | Baja | S | hecho |
| QA-05 | Mensaje de «la palabra no cabe» vuelca la palabra entera | Baja | S | hecho |
| QA-06 | Límites y entradas hostiles | Media | M | hecho |
| QA-07 | Los tests de referencia ensucian el árbol y no fallan si cambia un PNG | Media | M | hecho |
| QA-08 | Tests que faltan: hilos, idioma, determinismo, propiedades, mutación | Media | M | en curso |
| QA-09 | Integración continua | Alta | S | hecho |
| TS-01 | Contrato de hilos documentado y comprobable | Alta | S | hecho |
| TS-02 | Test de conversión concurrente | Alta | S | hecho |
| TS-03 | `registerFont` de AWT es global a la JVM | Media | M | hecho |
| TS-04 | Cerrojos en la ruta caliente (`FontFile`, `Resources`) | Media | S | hecho |
| TS-05 | Fachada reutilizable y segura entre hilos (`Converter`) | Media | M | hecho |
| PF-01 | Coste del GSUB de PDFBox | Alta | M | hecho |
| PF-02 | Medir el texto sin pasar por `TrueTypeFont` en cada carácter | Media | S | hecho |
| PF-03 | Vaciar toda la caché al subir las cifras de `{pages}` o las referencias | Media | M | hecho |
| PF-04 | Listas y recorridos que se recalculan por llamada | Baja | S | hecho |
| PF-05 | Fuentes leídas dos veces y arranque en frío | Baja | S | pendiente |
| PF-06 | El reparto en líneas mide la línea entera por cada palabra | Media | M | hecho |
| AR-01 | `BoxTreeBuilder.element` (SRP, OCP, KISS) | Alta | L | hecho |
| AR-02 | `Style`: 70 campos públicos mutables | Alta | L | hecho |
| AR-03 | `Box` mutable y con campos públicos | Media | M | hecho |
| AR-04 | `Engine`: algoritmos atados a la clase concreta (DIP, OCP) | Media | M | hecho |
| AR-05 | `Canvas`: interfaz demasiado ancha (ISP) | Media | M | hecho |
| AR-06 | `PdfCanvas` (760 líneas) hace demasiado | Media | M | hecho |
| AR-07 | Conocimiento de `Element` repartido por `layout` y `paint` | Media | M | hecho |
| AR-08 | Superficie pública: 4 sobrecargas de `PdfWriter.write` y ninguna fachada | Media | M | hecho |
| AR-09 | Modelo de errores sin tipo común | Media | M | hecho |
| AR-10 | Dos tolerancias | Baja | S | hecho |
| AR-11 | `Paginator.paginate` (96 líneas) | Baja | S | hecho |
| AR-12 | `ArchitectureTest` solo detecta ciclos | Baja | S | hecho |
| AR-13 | Limpieza: referencias a «módulo N», excepción como control de flujo | Baja | S | hecho |

## Línea base medida

Máquina de la sesión 0: 4 núcleos, 16 GB, OpenJDK 17, `-Djava.awt.headless=true`. Los tiempos varían de una máquina
a otra; lo que importa son las proporciones. Repetir con `tools/qa/`.

| Medida | Resultado |
|---|---|
| `mvn test` | 1314 tests, 0 fallos. Cobertura de instrucciones por package: del 93,2 % (`layout.multicol`) al 100 % (`output`); de ramas, del 75 % (`stxt`) al 98 % (`fragment`) |
| Los 11 ejemplos, secuencial | 1.ª ronda en frío 2654 ms; en caliente 633 a 955 ms (≈ 58 ms por documento) |
| 8 hilos × 10 rondas × 11 ejemplos (880 conversiones, 4 núcleos) | `Resources` compartido **16 217 ms**; uno por hilo **13 417 ms**; 0 diferencias de bytes con la secuencial en los dos casos |
| Texto largo (`Paragraph` de 330 caracteres), 153 páginas | leer 66 ms, layout 923 ms, pintar 2413 ms |
| Ídem, 1483 páginas (25 600 párrafos) | layout ≈ 6,5 s, pintar 15 a 21 s; cabe en `-Xmx256m` |
| 2000 páginas casi vacías (salto de página forzado) | 528 ms en total |
| `Contents` + `{page of: …}` con 1600 capítulos (1626 páginas) | 1549 ms |
| `showText` de PDFBox, 2400 líneas | con GSUB **855 a 1508 ms**; sin GSUB **48 a 126 ms** (17 a 30 veces); mismo tamaño de PDF |
| Perfil (JFR) del texto largo | ≈ 1/3 de las muestras en `fontbox.ttf.gsub` (expresiones regulares por llamada); `FontFile.width` 13 % |
| Reparto de tiempo con `{page}` en un `Fixed` | al pasar de 99 a 100 páginas el layout se hace dos veces (PF-03): 23 → 46 fallos de caché por página |
| Tras la sesión 2, texto largo | 153 páginas: layout 386 ms, pintar 232 ms (antes 954 y 2275); 657 páginas: layout 1,5 s, pintar 0,8 s (antes 3,2 y 9,3). Con `Ligatures: true`: layout 588 ms, pintar ≈ 2 s. 8 hilos × 10 rondas × 11 ejemplos: 6,8 a 7,0 s (antes 13,4 a 16,2 s) |
| Entradas hostiles | anidado > 100 niveles y línea > 10 000 caracteres: `LimitException` de `stxt-core` (bien); `Columns: repeat(100000, 1fr)`: 182 ms; 2000 páginas: 0,5 s |
| Bombas de imagen (`BombsProbe`, JVM de 512 MB) | PNG sin transparencia: 64 Mpx en 4,6 s, 144 Mpx en 4,3 s, 900 Mpx (106 KB) en 23 s; con transparencia: 64 Mpx en 5,3 s, 144 Mpx (547 KB) `OutOfMemoryError` (13 s con 1,5 GB); JPEG de 400 Mpx: 0,5 s. SVG con los límites de JSVG subidos: 300 000 rectángulos 5,4 s (PDF de 4 MB), 100 000 `<use>` 1,8 s, 2000 niveles bien, **20 000 niveles `StackOverflowError`**, 10⁹ elementos por `<use>` `OutOfMemoryError` a los 51 s. Con los límites por defecto de JSVG (2000 elementos y 30 niveles) falla incluso un SVG legítimo de 3000 rectángulos |
| Tras la sesión 4, SVG con texto | Formulario PDF de los 10 SVG con texto idéntico en `en` y en `tr` (antes, en `tr`, de 1 a 19 KB menos: dibujados con `Dialog`; `AwtFontProbe` muestra la causa); `mvn test` en turco deja el árbol limpio. `ConcurrencyProbe` en la misma máquina, antes y después: 7,7 a 10,0 s y 7,8 a 10,1 s, 0 diferencias de bytes; 1.ª ronda en frío 2,1 a 2,2 s y 1,9 a 2,1 s |
| Tras la sesión 5, paginación | `ScaleProbe`, layouts por página: 96 páginas 22,8; 100 páginas de 46,0 a 23,0; 153 de 45,8 a 22,9; 657 de 43,0 a 21,5; índice de 163 páginas de 57,1 a 33,0. Layout: 657 páginas de 1459 a 794 ms; 153 de 394 a 283; índice de 364 a 225. Perfil del layout (JFR, pilas enteras): `LineBreaker.breakParagraphs` 71,5 %, `FontFile.width` 21,6 %, `TextRun.Joiner` ≈ 19 %, `PdfMetrics.validate` ≈ 8 % (PF-06) |
| Tras PF-06, layout | `ScaleProbe`, medianas de tres: 657 páginas de 743 a 430 ms, 153 de 197 a 96, 100 de 167 a 103, índice de 163 páginas de 246 a 164; 15 layouts seguidos de las 657 páginas, de ≈ 575 a ≈ 235 ms en caliente. El reparto en líneas pasa del 71,5 % a ≈ 24 % del perfil |

## QA

### QA-01 · `ListMarkers` usa `toUpperCase()` con el idioma de la JVM · Alta · S

**Evidencia (verificada).** `tree/ListMarkers.java:42` y `:44` llaman a `alpha(number).toUpperCase()` y
`roman(number).toUpperCase()` sin `Locale`. En una JVM turca, `"i".toUpperCase()` es `İ` (U+0130). Con
`mvn test -DargLine="-Duser.language=tr -Duser.country=TR"` fallan 5 tests por esta única causa:
`ListMarkersTest.romanNumerals` (`MCMXC?V`), `ListMarkersTest.theNumberHasNoSuffix…`, `ListsTest.markersWithAHyphen`,
`NumberReferencesTest.anItemNumberIsWrittenAsItsMarkerDoes` y el golden `10-lists` (`I.` sale `İ.`). En producción,
un servidor turco daría además `GLYPH_MISSING` si la fuente no tiene `İ`.

**Propuesta.** `toUpperCase(Locale.ROOT)` en las dos líneas. Buscar el resto de llamadas sin `Locale`:
`grep -rnE "to(Upper|Lower)Case\(\)" src/main` (solo hay estas dos; el resto usa `Locale.ROOT` o el `Language` del
documento). `ImageFile.java:140` usa `String.format("%04X")` sin `Locale`: ponerle también `Locale.ROOT`.

**Hecho (sesión 1).** `toUpperCase(Locale.ROOT)` en `ListMarkers` y `Locale.ROOT` en `ImageFile`; `ListMarkersTest` pone la JVM en turco en cada test (sin la corrección falla, comprobado). La suite entera pasa con `-Duser.language=tr` (1321 tests).

**Aceptación.** Los 5 tests pasan con `-Duser.language=tr`; un test de `ListMarkersTest` fija `Locale.setDefault`
a turco en un `@BeforeEach`/`@AfterEach` (o usa el `@DefaultLocale` equivalente) para que lo guarde sin CI;
QA-09 añade la matriz de idiomas.

### QA-02 · Un SVG con texto sale distinto según el idioma de la JVM · Media · M · hecho

**Evidencia (verificada).** Con `-Duser.language=tr -Duser.country=TR`, `ExamplesTest` pasa (los volcados coinciden)
pero **12 PNG versionados se reescriben** (en la sesión 1 también `golden/24-svg/page-2`; el conjunto exacto varía de una ejecución a otra: `01-invoice/page-1`, `02-letter/page-1`, `04-annual-report/page-3,4,6`,
`06-menu/page-1`, `07-contract/page-1..3`, `08-manual/page-1,2`, `10-certificate/page-1`). Comparando los streams del
PDF de `01-invoice` en `en` y en `tr`, solo cambia el formulario del SVG del logotipo (`nogal.svg`, 15 122 bytes
frente a 10 160: otra forma de dibujar el texto) y la tabla de glifos de las fuentes del subconjunto. La causa no
está aislada: la hipótesis es que JSVG o AWT buscan la familia por el nombre en minúsculas con el `Locale` por defecto
(`IBM Plex Serif` → `ıbm plex serıf`) y no encuentran la fuente que `PdfImages.registerFonts` acaba de dar de alta;
el motor, en cambio, promete «sin sustitución de fuentes» (§5.11).

**Propuesta.** (1) Reproducir con un SVG mínimo con texto y `-Duser.language=tr`, mirando qué fuente elige JSVG.
(2) Si es AWT/JSVG: dibujar el texto del SVG con un `Font` creado por el motor desde el TTF (no por nombre de
familia), o convertir el texto en contornos al leer el SVG (`ImageFile`), o fijar el `Locale` solo durante el dibujo
(`Locale.setDefault` es global y NO es una opción en un servidor). (3) Si no hay arreglo limpio: documentar en
§5.18 que el SVG con texto necesita una JVM con un idioma sin la `i` turca, y hacer que el motor **falle** (no
sustituya) cuando detecte que la fuente elegida no es la pedida.

**Aceptación.** `ExamplesTest` con `-Duser.language=tr` deja `git status` limpio, o el motor rechaza el SVG con
un error claro.

**Hecho (sesión 4).** Causa aislada con una sonda: AWT tiene `IBM Plex Sans` dada de alta y en la lista de familias,
pero con la JVM arrancada en turco `new Font(familia)` devuelve `Dialog` (cambiar el `Locale` en caliente no lo
reproduce). JSVG 2.2.0 no deja inyectar su resolución de fuentes (`FontResolver` es estático), pero, si la familia no
está en AWT, pide la fuente a `PlatformSupport.customFont`: `SvgFonts` reescribe con un `DomProcessor` la fuente de
cada `<text>` y `<tspan>` a una familia clave del fichero (la huella de su contenido) y la crea con `Font.createFont`,
sin registro ni búsqueda por nombre (decisiones 308 a 310). Los diez SVG con texto dan el mismo formulario PDF, byte a
byte, en inglés, en turco y con el código anterior en inglés. La suite entera en turco deja el árbol limpio, y el job
de la CI en turco ya lo comprueba. Test: `PdfImagesTest.theLanguageOfTheJvmDoesNotChangeTheText` (dos JVM hijas).

### QA-03 · PDFBox aplica GSUB y el motor mide sin ligaduras · Alta · M · hecho

**Evidencia (verificada con `GsubGlyphsProbe`).** `PdfCanvas.text` llama a `PDPageContentStream.showText`, que en
PDFBox 3.0.8 aplica la tabla GSUB de la fuente (`PDAbstractContentStream.encodeForGsub`). Las fuentes que incluye el
repositorio tienen la característica `liga`: con IBM Plex Sans cambian al pintar `fi`, `ffi` y `office`; con IBM Plex
Serif, `fi`, `fl`, `ffi`, `ffl`, `office` y `affluent`; con Plex Mono no cambia nada. Pero `FontFile.width` suma el
avance de cada carácter por separado y su javadoc lo dice («No se aplican el kerning ni las ligaduras»). Es decir: **lo
que se pinta no es lo que se midió** en esos pares. Si el avance del glifo de la ligadura difiere de la suma de los dos,
las líneas justificadas, centradas o alineadas a la derecha quedan descuadradas una fracción de punto, y las guías de
`Contents` y los subrayados (que se miden por tramo) no coinciden con los glifos.

Además PDFBox lanza `IllegalStateException: could not find the glyphId…` si recibe un carácter que la fuente no
tiene mientras GSUB está activo (caso `ǅ`); hoy no se llega porque `PdfMetrics.validate` lo rechaza antes con
`GLYPH_MISSING`, pero un `Engine` con `FixedMetrics` pintado con `PdfCanvas` sí podría.

**Hecho (sesión 2), con la decisión del usuario: las dos opciones, elegible, y sin ligaduras por defecto.**
Nueva propiedad `Ligatures` (`true`/`false`, `false` por defecto) en `Typography`, heredable y válida en `Styles`
(decisiones 289 a 293 de `DECISIONES.md`, §4.4 y §5.4 de la especificación). Sin ella, `PdfCanvas` carga el TTF con
`setEnableGsub(false)` (API pública de fontbox, no el truco de quitar la tabla) y PDFBox escribe un glifo por
carácter; con ella, `FontFile.width(…, ligatures)` mide cada palabra con el mismo `GsubWorker` que PDFBox aplica al
pintar. La opción viaja en `FontFace` (cuarto componente), y `Letter spacing` distinto de 0 la desactiva. Se
regeneraron 22 PNG (solo cambia dónde había `fi`/`fl`, revisado). Test: `LigaturesTest` (anchos, valor por defecto,
herencia con el documento, interletraje y glifos escritos en el PDF). **Cambia el aspecto de los documentos
existentes** (por defecto, sin la ligadura `fi` de Plex Serif).

**Decisión que se tomó (para el registro).**

- **A (recomendada). Sin ligaduras en el PDF**: quitar la tabla GSUB al incrustar. Mide = pinta, el pintado del texto
  pasa a ser 17 a 30 veces más rápido (PF-01) y es coherente con «el texto se mide por carácter». Pierde `fi` como
  ligadura. Cómo: `PDType0Font.load(doc, ttf, true)` con un `TrueTypeFont` que devuelva `NO_DATA_FOUND` en
  `getGsubData()` (probado quitando `"GSUB"` de `getTableMap()`, que es un truco sobre una estructura interna de
  fontbox: investigar una subclase de `TTFParser`/`TrueTypeFont` o una opción pública antes de dejarlo así).
- **B. Medir con ligaduras**: aplicar la tabla `liga` en `FontFile.width` (y en el reparto en líneas, porque una
  ligadura es un glifo que no se parte). Fiel a la tipografía, pero es mucho trabajo y hay que decidir qué hacer con
  `Letter spacing` (se desactivan las ligaduras en CSS cuando es distinto de 0) y con `justify`.

**Aceptación (A).** Un test que pinta `fi` con Plex Sans y comprueba que el contenido del PDF tiene 2 glifos y que
su ancho es el medido; `GsubCostProbe` muestra el cambio; PNG de ejemplos sin cambios (si cambian por el avance,
revisarlos uno a uno y anotarlo en `DECISIONES.md`).

### QA-04 · `Rotation.CENTER` es mutable · Baja · S

`model/Rotation.java:23`: `public static final double[] CENTER = {0.5, 0.5}`. Cualquier llamador puede escribir en
él y cambiar el centro de giro de todos los documentos de la JVM (hoy `BoxTreeBuilder.element` solo lo lee, pero es
público). **Hecho (sesión 1):** `Rotation.CENTER` es ahora un `double` (0,5, el mismo en los dos ejes). Queda, si se quiere, que `Rotation.origin` y `ValueReader.rotateOrigin` devuelvan un `record Origin(double x, double y)` en vez de un `double[]` (son arrays nuevos en cada llamada, sin riesgo).

**Propuesta original:** dos constantes `double` (`CENTER_X`, `CENTER_Y`) o un `record Origin(double x, double y)
{ static final Origin CENTER }`. **Aceptación:** compila sin ningún `double[]` público ni `static final` de array en `src/main`
(`grep -rnE "static final [a-zA-Z]+\[\]" src/main`: hoy quedan `ImageFile.PNG`/`JPEG`, `ListMarkers.ROMAN_*` y
`PdfStructure.TABLE_GROUPS`, privados y de solo lectura por convención; valen).

### QA-05 · El mensaje de «la palabra no cabe» no se acota · Baja · S

Una palabra de 1000 caracteres produce un error de línea 13 que **contiene los 1000 caracteres** (medido con
`HostileInputsProbe`). **Propuesta:** acotar a ≈ 40 caracteres con `…` y la longitud total («la palabra de 1000
caracteres 'xxxxxxxx…' no cabe…»), en `Messages`. **Hecho (sesión 1):** `Messages.excerpt` (40 caracteres Unicode como mucho, y la longitud total); lo usan los dos mensajes de `Overflow.textOverflow`. `MessagesTest` comprueba que el mensaje de una palabra de 1000 y otra de 2000 caracteres mide lo mismo salvo las cifras.

**Aceptación:** el caso del catálogo de errores correspondiente
sigue pasando y un test comprueba que el mensaje no crece con la palabra.

### QA-06 · Límites y entradas hostiles · Media · M · hecho

**Lo verificado** (`HostileInputsProbe`): anidar más de 100 niveles y las líneas de más de 10 000 caracteres los
rechaza `stxt-core` con `LimitException` (no con `StackOverflowError`); `repeat(100000, 1fr)` se resuelve en 182 ms;
2000 páginas en 0,5 s.

**Lo que falta o no está verificado.**

1. ~~`Paginator.MAX_PAGES = 10_000` lanza `IllegalStateException` genérica~~ **Hecho (sesión 3, AR-09):** ahora es
   `ErrorCode.PAGE_LIMIT` (`Kind.LIMIT`, sin línea), con su caso en `test/errors/limits/`. Queda hacer configurable el
   número con `Limits`.
2. **No verificado:** un PNG o JPEG pequeño en bytes pero enorme en píxeles (*decompression bomb*). `PdfImages.bitmap`
   usa `PDImageXObject.createFromFileByContent`, que decodifica a `BufferedImage`. Comprobar el tamaño con la cabecera
   (lo hace `ImageFile` para el tamaño natural) y fijar un máximo de píxeles configurable.
3. **No verificado:** SVG con muchos elementos o con `<use>` anidados (JSVG los dibuja); límite de elementos y de
   profundidad en `SvgInspector` (es SAX, así que el recuento es barato).
4. No hay límite de tamaño de documento ni de tiempo. Para el servicio HTTP de `TODO.md` hace falta una
   `Limits` (páginas, píxeles por imagen, elementos de SVG, tamaño del documento) pasada en las opciones (AR-08).

**Aceptación.** `Limits` con valores por defecto razonables; un caso del catálogo de errores por cada límite;
`HostileInputsProbe` ampliada con la bomba de píxeles y el SVG enorme.

**Hecho (sesión 3), decisiones 303 a 307.** Se midió primero con una sonda nueva, `tools/qa/BombsProbe.java` (cada caso en
una JVM de 512 MB), y los valores por defecto los eligió el usuario entre tres juegos medidos («conservadores»).

*Lo medido* (ver la línea base): (1) **PNG y JPEG**: PDFBox incrusta casi todos los PNG sin decodificarlos, así que no
siempre hay una bomba de memoria, pero sí de tiempo (900 Mpx en 23 s) y, con transparencia y 144 Mpx, un `OutOfMemoryError`
en 512 MB (con 1,5 GB tarda 13 s); el JPEG se incrusta tal cual. (2) **SVG**: no era una bomba sino un **defecto**: JSVG
trae límites ocultos (2000 elementos, 30 niveles) y un SVG legítimo de 3000 rectángulos pasaba la validación del
motor y fallaba al pintar con una `IOException` genérica; además 20 000 niveles dan `StackOverflowError`, que ni el
`Converter` atrapa, y una cadena de `use` de 10⁹ elementos agota un heap de 512 MB tras 51 s.

*Lo hecho:* `model/Limits` (`maxPages` 2000, `maxImagePixels` 25 Mpx, `maxSvgElements` 20 000, `maxSvgDepth` 64, con
`withMax…`) en `Converter.builder().limits(…)`, que llega a `Engine`, `Images`, `Paginator` y `PdfCanvas.write`. Códigos
nuevos `IMAGE_TOO_LARGE` y `SVG_TOO_COMPLEX` (`Kind.LIMIT`, con la línea de la `Image`), comprobados **antes de
maquetar** con la cabecera (`ImageFile.pixels()`) y con el recuento de `SvgInspector` (sin recursión; cada `<use>` cuenta
lo que repite; satura; un `<use>` circular es `IMAGE_FORMAT`). `PdfImages` da a JSVG los mismos límites, de modo que lo que
el motor acepta se dibuja (`LimitsTest` lo comprueba justo en el límite) y un SVG que JSVG no carga sale como
`ImageFormatException`, no como `IOException`. Casos nuevos en `test/errors/limits/` (con `limit-huge.png`, de 35 KB, y
`limit-uses.svg` y `limit-depth.svg` en `resources/images/test/`), `LimitsTest` (9 casos) y los de recuento en
`SvgImageTest`. Cambia el comportamiento: los SVG de entre 2000 y 20 000 elementos, que antes fallaban, ahora se dibujan;
una conversión de más de 2000 páginas (antes 10 000) falla. §6.2 de la especificación y apartado «Límites» del README.

*Sin hacer:* un límite de **tiempo** (una conversión sigue sin tener tope de segundos; para un servicio, un
`Future.get(timeout)` y cancelar), el tamaño del texto del documento (ya lo limita `stxt-core` a 10 MB) y la ampliación de
`HostileInputsProbe` (la cubre `BombsProbe`).

### QA-07 · Los tests de referencia ensucian el árbol · Media · M · hecho

**Evidencia (verificada).** `GoldenTest` y `ExamplesTest` escriben `output.pdf` y los `page-N.png` **dentro de
`test/`** (versionado): un test que pasa deja `git status` sucio si cambian los píxeles (pasó con `tr`, QA-02). La
política actual es deliberada («si aparece modificado en git, el resultado ha cambiado; míralo», `CLAUDE.md`), pero
depende de que alguien mire: en integración continua, un PNG que cambia **no hace fallar nada**. Además los tests
dependen del directorio de trabajo (`resources/`, `test/`) y no se pueden ejecutar en paralelo ni con el árbol de
solo lectura.

**Propuesta (a decidir).** Escribir siempre en `target/` y comparar con el versionado: igual dentro de
`PIXEL_TOLERANCE` → pasa; distinto → **falla** con la ruta de los dos PNG, salvo `-Dexamples.update=true` /
`-Dgolden.update=true` (ya existe el primero para `expected.stxt`). Así el CI vigila los PNG y el árbol queda limpio.
**Aceptación:** `mvn test` no modifica ningún fichero versionado; un cambio de layout hace fallar el test con un
mensaje que dice cómo aceptarlo.

**Hecho (sesión 6).** La propuesta, con la decisión del usuario (decisión 313): `ReferenceFiles.writeActual` escribe el
PDF y las imágenes en `target/reference/{golden,examples}/<caso>/` y `checkImages` las compara con las versionadas; una
que cambia, falta o sobra hace fallar el caso con la ruta de las nuevas y la propiedad que lo acepta
(`-Dgolden.update=true`, `-Dexamples.update=true`). Probado a mano (un PNG de `01-block-flow` sustituido por el de
otro caso: «page-1.png cambia en 3120 píxeles…») y con `ReferenceFilesTest`. Ya no se escribe ningún PDF en `test/`
(fuera del `.gitignore`), y la CI sube `target/reference` si falla.

### QA-08 · Tests que faltan · Media · M

La cobertura de líneas es muy alta (el informe de JaCoCo no es el problema); lo que no se prueba es lo que la
cobertura no ve:

1. **Hilos** → TS-02.
2. **Idioma** → QA-01, QA-02 y QA-09.
3. **Determinismo**: el mismo documento dos veces da los mismos bytes (quitando `CreationDate`, `ModDate` y `ID`;
   medido con `ConcurrencyProbe`: determinista).
4. **Propiedades/*fuzzing*** de las piezas puras con más ramas sin cubrir: `ValueReader` (83 % de instrucciones),
   `ValueParser` (87 %), `AbsoluteEquation` (89 %), `ImageFile` (89 %), `MulticolLayout.Run` (92 %). Por ejemplo:
   `ValueParser.list(texto)` nunca lanza nada que no sea `IllegalArgumentException`; el reparto en líneas nunca pierde
   ni duplica caracteres; la suma de alturas de los fragmentos de una caja es su alto; el layout paginado de un
   documento es idempotente.
5. **Mutación** (PIT, `pitest-maven`) sobre `layout.*` y `fragment`: con el 96 % de cobertura, solo dirá si los
   asserts miden algo. Una pasada puntual, no en CI.
6. **Documentos de la especificación**: `DocumentReaderTest` ya maqueta los ejemplos del §10; falta que lo haga con
   `PdfMetrics` y PDF/UA (los once ejemplos ya pasan veraPDF; los de la especificación no).

**Sesión 6: hechos los puntos 3 y 4** (los 1 y 2 ya lo estaban; quedan el 5 y el 6, fuera del plan de la sesión).
(3) `CrossJvmDeterminismTest`: el PDF de los 11 ejemplos y los 28 golden es el mismo en una JVM hija, donde cambian los
`identityHashCode` (y con ellos el orden de los `IdentityHashMap`/`HashSet` de cajas); dentro de una JVM ya lo vigilaba
`ConcurrentConversionTest`. (4) Propiedades con `Random` de semilla fija, sin dependencias nuevas:
`ValueParserPropertiesTest` (50 000 textos al azar: `value` y `list` solo lanzan `IllegalArgumentException`; acepta
unos 2200), `LineBreakerPropertiesTest` (3000 textos y anchos, `auto` y `pretty`: ninguna palabra se pierde, se repite
ni cambia de orden, los párrafos son los del texto, ninguna línea vacía ni con espacios en los extremos y toda línea de
más de una palabra cabe) y `FragmentationPropertiesTest` (150 documentos y alturas de página: los fragmentos de cada
párrafo suman su alto entero y van en páginas seguidas). No han encontrado ningún fallo. Comprobado que detectan uno:
perder la última línea de un párrafo y repetir una línea al cambiar de página hacen fallar el suyo.

### QA-09 · Integración continua · Alta · S

No existe `.github/`. **Propuesta:** `.github/workflows/ci.yml` con JDK 17 (y 21 como matriz), `mvn -B test`, y una
segunda ejecución con `-DargLine="-Duser.language=tr -Duser.country=TR"` (QA-01/02); fallar si `git status --porcelain`
no está vacío tras los tests (QA-07); subir `target/site/jacoco` y `target/surefire-reports` como artefactos;
trabajo nocturno aparte con `-Dgroups=perf -DexcludedGroups=none` y con `tools/qa/`. Nota de entorno: dentro de esta
sesión, `mvn` solo funcionó con red (el plugin de JaCoCo no estaba en la caché local; `-o` falla).
**Hecho (sesión 1):** `.github/workflows/ci.yml` con tres trabajos: `test` (JDK 17 y 21, `mvn -B test`, comprueba que el árbol versionado queda limpio y sube los informes), `locale` (la suite con la JVM en turco; **no** comprueba el árbol hasta QA-02) y `perf` (`@Tag("perf")`, solo programado y a mano). Primera ejecución en GitHub (commit `9d544cc`): los tres trabajos en verde, también el del árbol limpio (el antialiasing de la máquina de GitHub no reescribe ningún PNG).

**Aceptación:** el commit siguiente al cambio muestra el workflow en verde.

## Hilos (ThreadSafe de la conversión)

### Qué es seguro y qué no

Verificado leyendo el código y con `ConcurrencyProbe` (8 hilos, 10 rondas, los 11 ejemplos, comparando con la
conversión secuencial: 0 diferencias, con `Resources` compartido y con uno por hilo):

| Pieza | Compartible entre hilos | Por qué |
|---|---|---|
| `DocumentSchema` (estático) | Sí | Se carga una vez en una constante; el uso concurrente de `SchemaValidator` sobre el mismo `SchemaProviderMemory` de `stxt-core` funcionó en las 880 conversiones, pero **es un contrato de otra librería** que el repositorio solo da por cierto (TS-01) |
| `DocumentReader` | Sí | Crea un `Parser` por lectura |
| `Resources`, `FontLibrary`, `ImageLibrary` | Sí | Carga perezosa con `synchronized`; `FontLibrary` es inmutable; `ImageLibrary.find` es `synchronized` |
| `FontFile` | Sí | Mide sin cerrojos con los avances precalculados (TS-04, sesión 2); `digest` se calcula una vez, sin cerrojo |
| `Box`, `Style`, `LaidOutDocument` | Sí (solo lectura) | Fuera de `tree` solo se leen; el árbol se cierra al construirlo, con sus estilos, y escribir en una caja o un estilo cerrados es un error (AR-02, AR-03, sesiones 7 y 10) |
| **`Engine`** | **No** | `LayoutCache` (`HashMap`/`IdentityHashMap` sin sincronizar), `InlineLayout` (`broken`, `measurer`, `pages`, `numbers` reasignados sin `volatile`) y contadores; `layOutDocument`, `assume` y `reservePageDigits` mutan estado compartido |
| `Paginator`, `BuildContext`, `PdfCanvas`, `PdfImages`, `PdfStructure`, `PdfLinks` | No (uno por conversión) | Estado de la conversión; los crea `PdfCanvas.write` |
| La caché de fuentes de JSVG (`FontResolver`) | Global, sin sincronizar | Los SVG con texto se dibujan dentro de `PdfImages.JSVG_FONTS`; ya no se da nada de alta en AWT (TS-03, sesión 4) |

`PdfWriter.write` crea un `Engine` y un `PdfCanvas` nuevos en cada llamada, así que **la ruta recomendada es
segura**. El riesgo está en que `Engine` es `public final class` con constructores públicos, sin una palabra sobre
hilos, y que `reservePageDigits` y `assume` (el protocolo interno del `Paginator`) también son públicos. Quien
comparta un `Engine` entre hilos (por ejemplo, para «no reconstruir» los algoritmos) corrompe un `HashMap` sin
excepción clara. Además, un `Engine` reutilizado en secuencia conserva la caché del último documento hasta la
siguiente llamada (`reservePageDigits` la vacía al empezar): retiene todo el árbol de cajas.

### TS-01 · Contrato de hilos documentado · Alta · S · hecho

Escribir en el javadoc de `Engine`, `PdfCanvas`, `PdfWriter`, `Resources`, `DocumentReader`, `LaidOutDocument`,
`Box` y `Style` qué se puede compartir, y en el `README.md` un apartado «Uso desde varios hilos»: *un `Engine` por
conversión; `Resources` y `DocumentReader` se comparten; un `LaidOutDocument` debería poder pintarse desde varios hilos
mientras nadie modifique las cajas (*no verificado*: lo comprueba TS-02); la JVM debe ir con `-Djava.awt.headless=true` si hay SVG con texto*.
Hacer package-private (o mover a un objeto de sesión) `Engine.reservePageDigits`, `assume`, `number` y `absolute`,
que solo usa el `Paginator` (mismo package). Vaciar la caché al terminar `layOutDocument`. Fijar la versión de
`stxt-core` que se ha probado concurrentemente (hoy `1.0.3`) y escribir a quien la mantiene el contrato que se
asume (seguridad del `SchemaValidator` compartido) o proteger el uso con una copia por hilo si no lo da.
**Aceptación:** javadoc y README; `Engine` sin métodos públicos de protocolo interno; sin cambio visible.

**Hecho (sesión 3), decisiones 300 a 302.** Contrato en el javadoc de `Engine`, `Resources`, `LaidOutDocument`, `Box`,
`Style`, `BoxTreeBuilder`, `DocumentSchema`, `PdfCanvas` y `PdfImages.registerFonts` (`Converter` y `DocumentReader` ya lo
decían), y un apartado «Uso desde varios hilos» en el README con una tabla de qué se comparte. `Engine.reservePageDigits`,
`assume`, `number` y `absolute()` pasan a ser de package, y `layOutDocument` vacía la caché, el reparto en líneas y las
páginas supuestas al terminar (antes un `Engine` guardado retenía el árbol del último documento: 351 entradas tras uno
de los ejemplos). `EngineContractTest`: el protocolo no es público, la caché queda vacía (falla sin la limpieza) y un
motor reutilizado da lo mismo que uno nuevo con los once ejemplos. El schema compartido de `stxt-core` 1.0.3 se
revisó en su bytecode (`getSchema` solo lee, `SchemaValidator` solo escribe en el constructor) y la versión queda
anotada en el `pom.xml`. **Sin hacer:** avisar a quien mantiene `stxt-core` del contrato que se asume (no se puede
hacer desde aquí).

### TS-02 · Test de conversión concurrente · Alta · S

Pasar `tools/qa/ConcurrencyProbe.java` a un test JUnit (`concurrency/ConcurrentConversionTest`): N hilos (4 por
defecto, `@Timeout`), cada uno convierte todos los ejemplos con su `Engine` y compara el PDF normalizado con el de la
conversión secuencial; una variante con `Resources` compartido y otra con uno por hilo; otra que paginan el mismo
`LaidOutDocument` a PDF desde varios hilos (comprueba que el árbol de cajas se lee sin escribir). Etiquetarlo `@Tag("slow")`
si pasa de unos segundos y ejecutarlo en CI (QA-09). **Hecho (sesión 1):** `concurrency/ConcurrentConversionTest` (4 hilos × 3 rondas × los 11 ejemplos, ≈ 14 s): un `Resources` por hilo, uno compartido y un mismo `LaidOutDocument` pintado desde varios hilos (esto último confirma que el árbol se lee sin escribir), más un test de determinismo. Probado con sabotaje: con `pages`/`numbers` de `InlineLayout` convertidos en `static` fallan los dos primeros. Un `static` en `LayoutCache` **no** lo detecta (las cajas son distintas en cada conversión): el test cubre estado compartido que cambia el resultado, no un `HashMap` compartido; para eso sirve TS-01 (que `Engine` no sea compartible).

**Aceptación:** pasa; falla si se introduce a propósito un
`static` mutable en la ruta (probarlo una vez a mano).

### TS-03 · `GraphicsEnvironment.registerFont` es global a la JVM · Media · M · hecho

`PdfImages.registerFonts` da de alta en AWT las fuentes de `fonts/` (JSVG busca por familia). Es un efecto de toda la
JVM: dos `Resources` con carpetas distintas pero la **misma familia** (dos clientes con su propia «IBM Plex Sans»)
comparten el registro y AWT se queda con el primero; el SVG del segundo se dibuja con la fuente del primero, sin
error. `registerFont` devuelve `false` si ya hay una fuente con ese nombre y el código no mira el resultado. Es
el mismo origen que QA-02 (el SVG con texto es el único punto donde el motor deja de controlar la fuente).
**Propuesta:** no usar AWT por nombre: convertir el texto del SVG en contornos con el `FontFile` del motor al
validar, o pasar a JSVG un `FontResolver`/`Font` propio por `Resources` (comprobar si JSVG 2.2.0 lo permite), de modo
que no haya ningún estado de JVM. **Aceptación:** dos `Resources` con la misma familia y ficheros distintos dibujan
cada uno el suyo (test); `PdfImages.REGISTERED` desaparece.

**Hecho (sesión 4).** Con QA-02: nada se da de alta en AWT y `REGISTERED` y `registerFonts` desaparecen. La caché de
fuentes de JSVG sigue siendo de toda la JVM y su clave no incluye el `PlatformSupport`; por eso la familia clave es la
huella SHA-256 del fichero (`FontFile.digest`), que es igual para el mismo fichero en dos carpetas y distinta para dos
ficheros. Esa caché es además un `HashMap` sin sincronizar: los SVG con texto se dibujan dentro de un cerrojo
(`PdfImages.JSVG_FONTS`), sin coste medible (decisión 310). Tests: `PdfImagesTest.twoFoldersWithTheSameFamilyDrawEachItsOwnFile`
(una copia de IBM Plex Serif que se declara «IBM Plex Sans» se dibuja como Serif), y además
`theFontOfStyleAndOfTheAncestorsIsTheOneDrawn` y `aTextRepeatedByAUseKeepsTheFontOfItsPlaceInTheFile` (decisión 309).

### TS-04 · Cerrojos en la ruta caliente · Media · S · hecho

`FontFile.width` es `synchronized` y `Resources.fonts()` también, y se llaman para **cada palabra medida** (varias
veces: mínimo y máximo intrínseco, reparto en líneas, `place`). Medido: 880 conversiones en 4 núcleos con `Resources`
compartido tardan un **20 % más** (16,2 s frente a 13,4 s) que con uno por hilo. **Propuesta:** (1) al abrir la
fuente, precomputar los avances por glifo en un `int[]` inmutable (ver PF-02) y quitar el cerrojo de `width`;
(2) `Resources.fonts()` y `images()` con un campo `volatile` y doble comprobación, o una carga garantizada en el
constructor; (3) `ImageLibrary` con `ConcurrentHashMap.computeIfAbsent`. **Hecho (sesión 2):** (1) sin `synchronized` en `FontFile.width`; (2) `Resources.fonts()`/`images()` con `volatile` y comprobación doble; (3) `ImageLibrary` con `ConcurrentHashMap`. Queda un `synchronized (ttf)` en el camino raro de un glifo inexistente y otro en la primera petición del `GsubWorker`. `ConcurrencyProbe` (ahora en cuatro fases alternadas, porque el orden de las dos distorsionaba por el calentamiento de la JVM): compartir `Resources` es igual o más rápido que uno por hilo (6,8 s frente a 7,0 s; antes 16,2 s frente a 13,4 s, con el sesgo del orden).

**Aceptación:** `ConcurrencyProbe` muestra
que compartir `Resources` ya no es más lento que no compartirlo.

### TS-05 · Fachada reutilizable y segura entre hilos · Media · M · hecho

Ver AR-08: un `Converter` inmutable (con `Resources`, estilos opcionales y límites) cuyo `convert(...)` crea su
`Engine` y es seguro entre hilos por construcción; sustituye a «hay que saber que no se comparte el `Engine`». Es
la base del servicio HTTP de `TODO.md`. **Hecho (sesión 3)** junto con AR-08: `convert/Converter` (decisión 294 de
`DECISIONES.md`). `ConcurrentConversionTest.threadsSharingTheConvertersGetTheSamePdf` comparte un mismo `Converter`
por ejemplo entre 4 hilos. Los `Limits` de QA-06 están ya en el `Builder` (`limits(…)`).

## Rendimiento

El tiempo se va en el **pintado** (70-75 % en documentos de texto largo), no en el layout, y dentro del pintado en
GSUB. El layout escala bien (casi lineal; el salto al pasar de 99 páginas es PF-03).

### PF-01 · Coste del GSUB de PDFBox · Alta · M · hecho

Medido con `GsubCostProbe`: 2400 `showText` tardan **855 a 1508 ms con GSUB y 48 a 126 ms sin él**, con un PDF del
mismo tamaño; en el perfil, ≈ 1/3 de las muestras están en `org.apache.fontbox.ttf.gsub.*`
(`CompoundCharacterTokenizer`, `GlyphArraySplitterRegexImpl`: compilan expresiones regulares en cada llamada). La
opción A de QA-03 lo resuelve de golpe; si el usuario elige B (ligaduras), hay que aplicar la tabla una vez por
cadena y cachear el resultado por `(fuente, texto)`. **Hecho (sesión 2):** con el valor por defecto (`Ligatures: false`) pintar 3200 párrafos baja de 2190 ms a **232 ms** (≈ 9,5 veces; 12 800 párrafos: 7,3 s → 0,8 s). Con `Ligatures: true` sigue costando lo de antes (≈ 2 s): es el coste de GSUB en PDFBox, que ya solo se paga si se pide. Si algún día hace falta rapidez *con* ligaduras, se puede codificar el texto con el `GsubWorker` del motor (ya se aplica al medir, con memoria por palabra) y escribir los glifos con `showGlyphs`/`TJ` en vez de `showText`: queda como mejora opcional.

**Aceptación:** `ScaleProbe` con 3200 párrafos: pintar baja de
≈ 2,4 s a < 0,6 s.

### PF-02 · `FontFile.width` · Media · S · hecho

Cada carácter hace `cmap.getGlyphId` (búsqueda en `CmapSubtable`) y `ttf.getAdvanceWidth` (que busca la tabla `hmtx` en un
`HashMap` cada vez: `TrueTypeFont.getTable`), dentro de un `synchronized`. `FontFile.width` es el 13 % de las muestras
propias. **Propuesta:** al abrir la fuente, leer `hmtx` a un `short[]`/`int[]` por glifo ya redondeado como hoy
(`Math.round(avance × 1000 / unitsPerEm)`) y un `char → avance` perezoso para el plano básico (`int[65536]` es 256 KB
por fuente: mejor por bloques de 256 o un `HashMap<Character,Integer>`); quita el cerrojo (TS-04). **Hecho (sesión 2):** `FontFile` calcula al abrir el avance de cada glifo (`int[]`) y memoriza el de cada carácter del plano básico (`int[65536]`, escrituras idempotentes sin cerrojo). `FontFileTest` compara los 63 488 caracteres de las 12 fuentes con lo que contesta fontbox (idénticos) y mide desde 4 hilos. Layout de 12 800 párrafos: 3,2 s → 1,2 s; 3200: 954 → 512 ms. Los 22 PNG que cambiaron en esta sesión son solo por QA-03, no por esto (los anchos son los mismos).

**Aceptación:** los
anchos son idénticos bit a bit (test que recorre todos los glifos de las 12 fuentes contra la implementación anterior
una vez); `ScaleProbe`: layout −15 % o mejor.

### PF-03 · Vaciar toda la caché al subir las cifras o las referencias · Media · M · hecho

`Paginator.paginate` repite el layout del documento entero cuando (a) el total de páginas necesita más cifras de las
reservadas (`PageNumbers.DIGITS = 2`: a partir de la página 100) o (b) cambia una página supuesta de `{page of: id}`;
y `Engine.reservePageDigits` y `Engine.assume` hacen `cache.clear()`. Medido: con `{page}` en un `Fixed`,
77 páginas → 23 fallos de caché por página; 153 páginas → 46 (el doble), y el layout pasa de 273 ms a 923 ms. Es
trabajo tirado: solo cambia el ancho de los textos que contienen esos marcadores o referencias (un pie de página),
y el cuerpo del documento da el mismo resultado. **Propuesta:** (1) invalidar solo las entradas de la caché de las
cajas cuyo texto (o el de un descendiente) contiene `{page}`, `{pages}` o una `Reference`; la información sale del
árbol (`References.sites`, `Paginator.hasPageNumbers`, que se calculan una vez al construir: PF-04). (2) Las
cajas con marcadores suelen ser `Fixed`, que ya se colocan fuera del flujo: puede bastar con repetir solo esa parte.
**Aceptación:** `ScaleProbe` con `{page}` y 153 páginas: los fallos de caché por página no se duplican al cruzar la
página 100; los golden de los hitos 7, 22 y 32 no cambian.

**Hecho (sesión 5).** La propuesta (1): el `Paginator` calcula una vez los textos con `{page}`/`{pages}` y, en cada
pasada, los que tienen un `{page of}` cuyo valor cambia, y `Engine.reservePageDigits`/`assume` olvidan solo esas cajas y
sus antepasados en la `LayoutCache` y en el reparto en líneas (`Engine.forget`, decisión 311). `ScaleProbe` (ahora con
los fallos por página, 96 y 100 páginas a los dos lados de la frontera y dos documentos con índice): 2100 párrafos
(100 páginas) de 46,0 a 23,0 fallos por página (96 páginas: 22,8); 153 páginas de 45,8 a 22,9; 657 de 43,0 a 21,5;
índice de 163 páginas de 57,1 a 33,0. Layout: 657 páginas de 1459 a 794 ms, 153 de 394 a 283, índice de 364 a 225.
Ningún golden, ejemplo ni PNG cambia. Test: `PaginationCacheTest` (con la caché vaciada entera calcula el doble de
layouts y falla).

### PF-04 · Cálculos que se repiten · Baja · S · hecho

`Box.inFlowChildren()` crea una lista nueva con un `stream` en cada llamada (`BlockLayout`, `FlexLayout`,
`GridLayout`, dos veces en `BreakRules`); `Paginator.hasPageNumbers` y `withPageNumbers` recorren el árbol entero;
`pagesOf`/`firstPages` recorren todos los fragmentos de todas las páginas en cada pasada. **Propuesta:** calcular
`inFlowChildren` al terminar de construir (AR-03) y las marcas del documento (`hasPageNumbers`, destinos de
referencia) en `BoxTreeBuilder.Result`. Es una mejora pequeña: medirla antes con JFR para no optimizar a ciegas.

**Hecho (sesión 5): medido, sin cambios.** JFR con las pilas enteras (`jfr print --stack-depth 64`; con las 5 de por
defecto casi nada se atribuye) sobre 40 layouts de los documentos de 153 y 163 páginas de `ScaleProbe`, 481 muestras:
`Box.inFlowChildren` 0,2 %, `pagesOf`/`firstPages` 0 %, `withPageNumbers` 0,2 % (desde PF-03 se recorre el árbol una
vez), `References.sites`/`byId` 0,6 %. No compensa tocarlos. El tiempo está en el reparto en líneas: ver PF-06.

### PF-05 · Fuentes leídas dos veces y arranque en frío · Baja · S

`FontLibrary` parsea cada TTF una vez (para las métricas) y `PdfCanvas.pdfFont` vuelve a leerlo con
`PDType0Font.load(doc, file)` en **cada** conversión. Guardar los bytes del fichero en `FontFile` y pasarlos a
PDFBox (`PDType0Font.load(doc, InputStream, true)`) ahorra la lectura de disco, no el análisis. El arranque en frío
de los 11 ejemplos es 2,6 s (frente a 0,6 s en caliente): JIT, escaneo de `fonts/`, AWT para los SVG. Para un servicio,
calentar al arrancar con un documento mínimo. **No verificado** cuánto aporta cada parte.

### PF-06 · El reparto en líneas vuelve a construir y medir la línea entera por cada palabra · Media · M

**Evidencia (medida en la sesión 5, PF-04).** En el perfil de 40 layouts de 153 y 163 páginas, `LineBreaker.breakParagraphs`
es el **71,5 %** de las muestras, y dentro, `FontFile.width` el 21,6 % y el `StringBuilder` de `TextRun.Joiner` cerca
del 19 %. Con `Text wrap: normal`, por cada palabra que se añade a la línea se construye la línea entera
(`LineBreaker.line`) y se mide entera (`width`): coste cuadrático en las palabras de cada línea (unas 15). El reparto
`pretty` (`optimal`) ya usa sumas acumuladas de los avances de las palabras y los espacios. Además,
`PdfMetrics.validate` (comprobar los glifos de cada texto antes de maquetar) es cerca del 8 %.
**Propuesta.** Medir la línea en curso con la suma acumulada (como `optimal`) y medir la línea entera solo cuando la
suma queda a menos de `Tolerance.MODEL` del ancho disponible, para que las decisiones sean exactamente las de hoy.
**Aceptación.** El mismo reparto (ningún PNG ni volcado cambia) y `ScaleProbe` con el layout al menos un 30 % más rápido.

**Hecho (después de la sesión 10), en cuatro commits.** (1) `LineBreaker.greedy` lleva la suma de los avances de las
palabras y los espacios de la línea en curso, cada uno medido una vez, y construye y mide la línea entera solo cuando la
suma queda a menos de `Tolerance.MODEL` del ancho disponible (la propuesta). Hace falta: sin esa medida exacta, el
redondeo de la suma cambia algún corte justo en el límite (lo detecta el test). `TextMeasurer` dice ahora que su ancho es
aditivo entre palabras, que `pretty` ya suponía (decisión 321). (2) `LineBreaker.paragraphs` recorre el texto en vez de
buscar `\S+` y cortar cada hueco, y una palabra de un solo tramo no pasa por `TextRun.Joiner`. (3) `PdfMetrics.validate`
busca `{page}`, `{pages}` y las referencias solo donde hay una llave, y `FontFile.has` memoriza los caracteres del plano
básico. (4) `PdfMetrics` recuerda la última fuente (buscarla en cada palabra era el 12 %) y un texto sin formato no busca
su tramo. Tests nuevos en `LineBreakerPropertiesTest`: el reparto es el del algoritmo de antes (3000 textos con formato,
métricas reales, ligaduras, interletraje y anchos justo en el límite) y las palabras, las de la expresión regular (20 000
textos con todos los espacios en blanco). Ningún PDF, PNG, SVG ni volcado cambia. **Medido** (`ScaleProbe`, tres veces
alternando con el código de antes; medianas del layout): 657 páginas de 743 a 430 ms (−42 %), 153 de 197 a 96 (−51 %),
100 de 167 a 103 (−38 %), índice de 163 páginas de 246 a 164 (−33 %); en caliente (15 layouts seguidos de las 657
páginas), de unos 575 a unos 235 ms. Lo que queda del layout de texto largo en el perfil: el reparto (≈ 24 %, ya
lineal), `FontFile.width` (≈ 10 %), construir las líneas (`LineBreaker.line`) y `PdfMetrics.validate` (≈ 4 %).

## Arquitectura

### Lo que está bien (no tocar)

- **Capas**: `model` ← `tree` ← `fragment` ← `layout` ← `paint`, sin ciclos y sin saltos (verificado con los `import`).
- **Algoritmos puros y probados** (`LineBreaker`, `FlexResolver`, `TrackSizing`, `Placement`, `Columns`,
  `AbsoluteEquation`, `BreakRules`), cada uno con su test; entradas y salidas del layout inmutables.
- <a id="dry"></a>**DRY**: una búsqueda de trozos repetidos de 7 líneas normalizadas en `src/main` da **0 coincidencias**;
  con 4 líneas, 5 (todas en `PdfCanvas` y `PropertyReader`). No hay trabajo de DRY a nivel de texto; los puntos de
  DRY que quedan son conceptuales (AR-07, AR-10).
- Errores con código, línea y catálogo (un fichero por caso, y `everyCodeHasACase`).

### AR-01 · `BoxTreeBuilder.element` · Alta · L

**Evidencia (medida).** `element(...)` tiene 162 líneas y una complejidad ciclomática ≈ 67 (la siguiente es 31); la
clase, 607 líneas. Hace, en un solo método: comprobar la posición en `Multicol`, la jerarquía de encabezados, heredar,
aplicar reglas, leer propiedades con un `switch` de 20 casos (más tres `if` previos por tipo: `FLEX`, `GRID`/`TABLE`,
`MULTICOL`), siete validaciones por `kind == Element.X`, crear la caja, el marcador de lista, y despachar el
contenido por tipo (texto, imagen, celda, tabla, `Multicol`, `Markdown`, genérico). Añadir un elemento obliga a
tocarlo en cuatro sitios. Rompe SRP (hace de todo), OCP (cada elemento nuevo lo modifica) y KISS.

**Propuesta.** Sin cambiar ninguna regla ni mensaje: (1) una interfaz `ElementReader` por familia
(`TextElement`, `ImageElement`, `TableElement`, `MulticolElement`, `MarkdownElement`, `ContainerElement`) elegida
desde `Element` (un campo o un `EnumMap<Element, ElementReader>`); cada una lee sus propiedades propias, valida y
rellena el contenido. (2) El `switch (name)` de propiedades sueltas pasa a una tabla
`Map<String, PropertyHandler>` en `PropertyReader`, que ya agrupa `typography`, `box`, `flex`, `grid`…
(3) `element` queda como el orden: validar posición → herencia → reglas → propiedades → caja → contenido. (4) Quitar las
variables locales `id`, `rotate`, `rotateOrigin` que se arrastran por el método: un pequeño `ElementProps` mutable
y privado del builder. **Hacer por pasos**, un commit por cada familia de elemento, con `mvn test` en verde y
**ningún PNG ni volcado cambiado** en cada uno. **Aceptación:** ningún método de `tree` con más de 60 líneas ni
complejidad > 20; `BoxTreeBuilder` < 350 líneas.

**Sesión 8: hecho**, en cuatro commits verdes y sin ningún PNG, volcado ni PDF cambiado (byte a byte, sin fechas ni ID):
(a) `MediaReader` lee `Media`, `Title` y `Language` (`media`, complejidad 22, partido en `media` y `paper`);
(b) las propiedades sueltas de los elementos y el grupo `Typography` (un `switch` de complejidad 21) son tablas
`Map<String, Property>` de `PropertyReader`, y `BoxTreeBuilder.properties` las lee y pone el giro en el estilo (el
`ElementProps` propuesto no hizo falta: las variables quedan dentro de ese método); (c) `ElementReader`, un enum con una
constante por familia en lugar de una interfaz y una clase por familia (las familias son pocas y cerradas, y un enum
se elige con un `switch` sin registro aparte), con `layout`, `wrapper`, `property`, `check` y `content`; (d) para
cumplir la aceptación en todo `tree`, `TableBuilder.table` (80 líneas, complejidad 21) se parte en `table`, `rows`,
`freeColumn` y `occupy`, y `ModelValidator.validateMargins` (21) saca `validateOutOfFlowMargins`. Resultado:
`element` 42 líneas y complejidad 8 (antes 162 y 67), `BoxTreeBuilder` 347 líneas (antes 617), el método más largo
de `tree` 44 líneas y la complejidad más alta 18. Decisión 315.

### AR-02 · `Style` · Alta · L

**Evidencia (medida).** 363 líneas, **70 campos `public` no `final`** de todas las familias (caja, tipografía, flex, grid,
ítem, lista, enlace, imagen, índice, `Multicol`), todos mutables, más un `Map` de líneas por propiedad. Cualquier
clase puede escribir cualquier campo en cualquier momento (el layout no lo hace hoy, pero nada lo impide) y ningún
algoritmo ve qué usa: `FlexLayout` recibe un `Style` con 70 propiedades y lee 12. Es la causa de que `Box` y `Style`
no se puedan compartir con garantías entre hilos (TS-01) y de que `tree`, `layout` y `paint` se acoplen a una clase
enorme. Rompe SRP e ISP.

**Propuesta (por etapas, cada una verde).**
1. *Accesores sin cambio de forma*: añadir métodos de lectura (`style.fontSize()`) y migrar los `layout`/`paint` a
   ellos con un refactor mecánico; los campos siguen existiendo.
2. *Esconder los campos* (`private`/package-private) y que solo `tree` pueda escribir (un `StyleBuilder` o los
   métodos `set…` en el package `tree`).
3. *Partir por familias* en registros inmutables: `TypographyStyle` (el único heredable: `Style.inheriting` copia
   solo esto), `BoxStyle`, `FlexStyle`, `GridStyle`, `ItemStyle`, `ListStyle`, `ContentsStyle`, `MulticolStyle`,
   `ImageStyle`; `Style` es el agregado inmutable. Los enums que hoy viven en `Style` (`Layout`, `Position`, `TextAlign`,
   `Marker`…) se mueven al package `model` donde los usan `tree` y `layout`; si no, `model` no puede nombrarlos y
   `Style` seguirá siendo un cajón de sastre.
4. *Congelar*: tras construir el árbol, `Box`/`Style` ya no cambian (AR-03).
**Aceptación:** ningún campo público no `final` en `Style`; `FlexLayout` depende de `FlexStyle` y `BoxStyle`, no
del agregado; volcados y PNG sin cambios.

**Sesión 7: hechas las etapas 1 y 2.** Los 80 campos son del package `tree` y cada uno tiene su método de lectura con
el mismo nombre (`style.fontSize()`); el constructor, `inheriting` y `declared` también son del package. Las 233
lecturas de fuera de `tree` se migraron con los errores de `javac` como guía (sin buscar y reemplazar a ciegas). Los
cuatro tests de otros packages que escribían un estilo a mano (`BoxModelTest`, `LineBreakerTest`, `BreakRulesTest`,
`UnitBreakerTest`) usan `StyleEditor`, un ayudante de los tests en el package `tree`. Quedan las etapas 3 y 4
(sesión 10).

**Sesión 10: hechas las etapas 3 y 4.** Los 17 enums pasan a `model` (`Fill` de `Multicol` es `ColumnFill`; decisión
319). `Style` se parte en diez grupos, uno por commit: `FlexStyle`, `GridStyle`, `ItemStyle`, `ListStyle`,
`ContentsStyle`, `ImageStyle`, `MulticolStyle`, `BreakStyle`, `BoxStyle` y `TypographyStyle`, que es el único que se
hereda (`Style.inheriting` copia el grupo del padre en vez de 19 campos) y lleva `fontFace()` y `usedLineHeight()`. El
hueco es de `FlexStyle` y de `GridStyle`, cada uno el suyo. En `Style` quedan las diez propiedades del elemento (§4.2
y §4.9), los grupos y las líneas: de 731 líneas a 302. Las lecturas y escrituras (unas 600) se migraron con los
errores de `javac` como guía, como en la sesión 7. **Congelar (etapa 4):** los campos de `Style` y de los grupos son
privados y se escriben con un `set…` del package que falla si el estilo está cerrado (`StyleGroup`); `Box.close`
cierra el estilo con sus grupos. Los grupos no son `record`: el builder escribe las propiedades de una en una
(decisión 318). Aceptación: ningún campo de `Style` ni de sus grupos deja de ser privado (`BoxTest`); `FlexLayout` lee
`flex()`, `box()` y el `item()` de sus hijos (y `isOutOfFlow()`); el PDF de los ejemplos y los golden y los SVG de los
golden, iguales byte a byte (sin fechas ni ID). `ScaleProbe`, tres veces alternando con el principio de la sesión:
dentro del ruido (657 páginas: layout de 687 a 771 ms antes y de 730 a 833 ahora; índice de 163 páginas: de 253 a 280
antes y de 229 a 259 ahora).

### AR-03 · `Box` · Media · M

`Box` (180 líneas) tiene `text`, `runs`, `image`, `anchor`, `number`, `marker`, `headRows`, `footRows`, `textLine`,
`textBlock` y `parent` públicos y no finales, y `children` es una `ArrayList` pública. La documentación dice que «`Box`
es mutable solo mientras `BoxTreeBuilder` construye el árbol; el layout no la escribe nunca»: es una convención, no
una garantía, y `LaidOutDocument.root()` la entrega a cualquier cliente. **Propuesta:** campos privados con
accesores; los mutadores package-private en `tree`; `children()` devuelve una vista no modificable; `inFlowChildren`
se calcula una vez al cerrar el árbol (PF-04); `BoxTreeBuilder.Result` marca el árbol como cerrado. Se hace junto con AR-02.

**Sesión 7: hecho.** Campos privados con métodos de lectura (`id()`, `children()`, `text()`, `formattedRuns()` para los
tramos crudos, porque `runs()` ya daba un tramo sin formato…), constructor y escritura del package (`addChild`,
`addSection` para las secciones, `setText`, `setRuns`, `prependText`, `setMarker`…). `BoxTreeBuilder.build` cierra el
árbol al terminar (`Box.close`, recursivo y también sobre los marcadores): los hijos pasan a `List.copyOf`,
`inFlowChildren` se calcula una vez y cualquier escritura posterior es un `IllegalStateException`. `TextRuns` pone sus
tramos con el `finish` de un `build` del package, antes de cerrar. `BoxTest` vigila el cierre y que ni `Box` ni
`Style` tengan campos públicos. Decisión 314.

### AR-04 · `Engine` y los algoritmos · Media · M

**Evidencia.** 8 algoritmos (`layout/*/…Layout`) importan la clase concreta `Engine` (para `engine.layout`,
`engine.intrinsic`, `engine.measurer()`, `engine.natural`) y `Engine` construye los 8 en su constructor: dependencias
circulares entre clase concreta y detalle (DIP), un `switch` por `Style.Layout` más una excepción a mano
(`box.element == Element.TABLE ? table : grid`) cada vez que se añade un algoritmo (OCP), y un `Engine` que a la vez
despacha, valida (`measurer.validate`, `images.validate`), numera páginas, guarda las páginas supuestas y vacía
cachés (SRP). Para probar un algoritmo hay que construir el motor entero (los tests lo hacen con `LayoutFixture`).

**Propuesta.** (1) Interfaz `LayoutContext` (`layout`, `intrinsic`, `contentIntrinsic`, `measurer`, `natural`) que
implementa `Engine`; los algoritmos dependen de ella y se pueden probar con un doble. (2) El despacho en un
`EnumMap<Style.Layout, LayoutAlgorithm>` y la tabla a un algoritmo propio registrado (`TableLayout` envuelve a
`GridLayout`, ya lo hace), sin el `if` por `Element`. (3) Sacar de `Engine` lo que es de la paginación
(`reservePageDigits`, `assume`, `number`, `absolute`) a un `PaginationSession`/package-private (TS-01). (4) `Engine`
conserva solo `layOutDocument` y el despacho. **Aceptación:** ningún `…Layout` importa `Engine`; `ArchitectureTest`
(AR-12) lo comprueba.

**Sesión 9: hecho.** `LayoutContext` (`layout`, `intrinsic`, `contentIntrinsic`, `measurer`, `natural`) lo
implementa `Engine`; los ocho algoritmos, `BoxModel.resolve` y `UnitBreaker` la reciben (el campo se llama `context`).
El despacho es un `EnumMap<Style.Layout, LayoutAlgorithm>` más la tabla, elegida por su elemento (la excepción que
AR-07 permite). El punto (3) ya lo cumplía TS-01 (el protocolo de la paginación es de package) y no se movió a otra
clase: separarlo no quitaba ninguna dependencia. `ImageLayoutTest` prueba `ImageLayout` con un doble.

### AR-05 · `Canvas` · Media · M

La interfaz mezcla dos cosas: primitivas de dibujo (`background`, `fill`, `border`, `text`, `vertical`) y, con
**11 métodos `default` que no hacen nada**, las del PDF etiquetado y los enlaces (`beginText(Box)`, `beginLinkText`,
`image`, `link`, `beginRotation`, `endRotation`, `endText`…) que `SvgCanvas` ignora. `StackingContext` las llama todas
sin saber si el lienzo las entiende: un tercer lienzo (por ejemplo un PNG) heredaría 11 métodos vacíos sin darse
cuenta de que debería implementar alguno. ISP. **Propuesta:** `Canvas` con el dibujo; `TaggedCanvas`/`LinkCanvas`
(o un solo `Annotating`) como interfaces opcionales que `StackingContext` obtiene con `canvas instanceof`/un método
`Optional<…> tagging()`. **Aceptación:** `SvgCanvas` no tiene métodos vacíos heredados; golden SVG sin cambios.

**Sesión 9: hecho.** `Canvas` solo tiene métodos abstractos de dibujo (más `tagging()`); `Tagging` (`beginText`,
`beginLinkText`, `endText`, `link`) es aparte: `PdfCanvas` da su `PdfTagging` y `SvgCanvas`, `Tagging.NONE`, y declara
sus métricas aproximadas en lugar de heredarlas. Un método explícito en lugar de `instanceof`: cada lienzo dice lo
que hace con el etiquetado. Los 54 SVG de los golden, iguales.

### AR-06 · `PdfCanvas` · Media · M

760 líneas y cuatro responsabilidades: (1) orquestar el documento (`write` estático: páginas, etiquetas de página
`labels`, destinos, estructura, guardado); (2) las primitivas del lienzo; (3) sombreados y máscaras suaves
(`gradient`, `opacityMask`, `axial`, `stitched`, ≈ 130 líneas de diccionarios COS); (4) fuentes (`pdfFont`) y el texto
con `TJ` (`spaced`, `justified`). Y **12 bloques idénticos** `try { … } catch (IOException e) { throw new
UncheckedIOException(e); }`. **Propuesta:** `PdfDocumentWriter` (1), `PdfGradients` (3), `PdfFonts` (4: caché de
`PDFont` y carga, que también resuelve PF-05) y `PdfCanvas` (2); un único punto de conversión de
`IOException` (un `interface Io { void run() throws IOException; }` con `io(() -> …)`) o que los métodos de
`Canvas` declaren `throws IOException`, que es lo honesto. **Aceptación:** `PdfCanvas` < 350 líneas; PDF byte a byte
igual (normalizado) en los 11 ejemplos antes y después, con `ConcurrencyProbe`.

**Sesión 9: hecho.** `PdfDocumentWriter` (el documento; su `write` sustituye a `PdfCanvas.write`), `PdfCanvas` (318
líneas, el dibujo de una página), `PdfContent` (el flujo de la página con el origen abajo, el color, los giros y `io`,
el único punto que convierte las `IOException`), `PdfTagging` (contenido marcado y anotaciones), `PdfFonts` (fuentes
y `TJ`) y `PdfGradients`. Se mantienen las primitivas sin `throws IOException` (cambiarlo obligaría a todos los
lienzos y a `StackingContext` a declararlo para un fallo que solo puede dar el PDF). PF-05 no se tocó. El PDF de los
ejemplos y los golden, igual byte a byte.

### AR-07 · Conocimiento de `Element` fuera de `tree` · Media · M

Hay 12 comparaciones `x.element == Element.Y` en `layout` y `paint` (`Engine`, `Overflow`, `StackingContext`,
`PdfStructure` ×8). `PdfStructure.build` (59 líneas, complejidad 22) y `type(Box)` son una cadena de `if` por tipo
de elemento; añadir un elemento obliga a tocar `PdfStructure`, `Element` y, a menudo, `StackingContext`. DRY
conceptual y OCP. **Propuesta:** que `Element` (que ya da `layout()`, `position()`, `hasText()`, `headingLevel()`) lo
describa todo: `pdfRole()` (etiqueta y si es contenedor), `structural()`, `decorative()`; `PdfStructure` consulta, no
compara. **Aceptación:** ninguna comparación con una constante de `Element` en `paint` ni `layout`, salvo el despacho.

**Sesión 9: hecho.** En lugar de `pdfRole()`, `structural()` y `decorative()` en `Element`, un solo `Element.Role`
(el papel de la caja: sección, encabezado, párrafo, lista, celda, figura, guía, flujo de columnas, bloque…), que
comparten varios elementos: así `tree` no sabe nombres de etiquetas del PDF y `PdfStructure.type` es un `switch`
exhaustivo sobre `Role`. `PdfStructure.build` saca `attributes` (de complejidad 22 a menos de 20). `ArchitectureTest`
comprueba que solo `Engine` nombra una constante de `Element` fuera de `tree`. Decisión 316.

### AR-08 · Superficie pública · Media · M · hecho

`PdfWriter.write` tiene **4 sobrecargas** (con y sin estilos, con y sin `Resources`, con `boolean debug`);
`Engine.layOutDocument` 3; `SvgCanvas.page` 2; los estilos aparte son un parámetro que puede ser `null`. No hay un
objeto que represente «una configuración de conversión». **Propuesta:** `StxtPdf` (fachada) con `Options`
(`Resources`, `Limits` de QA-06, `debug`, estilos) y un `Converter` inmutable y seguro entre hilos (TS-05):
`Converter.convert(String|Path documento, OutputStream)`, `layOut(...)` (para pintar a PDF y a SVG a la vez) y la
lectura del `Styles` aparte. Las clases actuales quedan como la capa de bajo nivel documentada, y las sobrecargas
de `PdfWriter`, delegando en `Converter` y marcadas `@Deprecated`. Es la base de la API HTTP y de «cómo extender fácil»
de `TODO.md`.

**Hecho (sesión 3), con la decisión del usuario: refactor completo, sin `@Deprecated`.** No hay código fuera del
repositorio que use `PdfWriter`, así que se eliminó (`git rm`) en vez de dejarlo delegando. `convert/Converter`
(`create()` o `builder()` con `resources`, `styles` —de un nodo, un texto o un fichero; nunca `null`— y `debug`) da
`layOut(InlineNode)`, `toPdf(InlineNode | LaidOutDocument, OutputStream)` y `toSvg(LaidOutDocument)`. Package nuevo
`convert`, la capa de arriba (`ArchitectureTest` exige que nada dependa de él). Migrados GoldenTest, StylesFileTest,
PdfCanvasTest, BoxLooksPaintTest, ConcurrentConversionTest y `tools/qa/ConcurrencyProbe`; `PdfBytes` (helper de test)
compartido. README, `CLAUDE.md` y especificación al día. Sin cambio en ningún PNG ni volcado. Quedan **sin hacer** de
esta tarea: `StxtPdf` como nombre de fachada (se usó `Converter`), `Limits` (QA-06) y `layOut(String|Path)` (quien
lee usa `DocumentReader`, que ya es público y seguro entre hilos).

### AR-09 · Modelo de errores · Media · M · hecho

Un cliente que convierte un documento debe poder distinguir «el documento es inválido» (4xx) de «el servidor falla»
(5xx), y hoy las excepciones no tienen un tipo común: `dev.stxt.exceptions.ParseException`/`ValidationException`
(de `stxt-core`), `InvalidModelException`, `ResourceException`/`ImageFormatException` (todas `RuntimeException`
sin padre común), `IllegalStateException` (9 usos: «sin progreso» y «máximo de páginas» del `Paginator`, enlaces sin
destino…), `UncheckedIOException` (12 en `PdfCanvas`), `IOException` y `java.awt.FontFormatException` convertidas
(`PdfImages`). **Propuesta:** `StxtPdfException` (runtime) con `kind()` (`INVALID_DOCUMENT`, `RESOURCE`, `LIMIT`,
`INTERNAL`), `line()` y `code()`; `InvalidModelException` y `ResourceException` la extienden; `DocumentReader` envuelve
las excepciones de `stxt-core` (conservando la causa); los `IllegalStateException` que son fallos del documento pasan
a `LIMIT`, y los que son errores de programación, a `INTERNAL`. **Aceptación:** una sola `catch (StxtPdfException)` basta
en un servicio; el catálogo de errores sigue verde; la tabla de §6 de la especificación incluye el nuevo `kind`.

**Hecho (sesión 3), decisiones 295 a 299.** `StxtPdfException` (en `model`, con `Kind`, `code()` en texto, `line()` y
`NO_LINE`) extendida por `InvalidModelException` (`errorCode()` da ahora el enum), `DocumentSyntaxException`
(`tree`; los errores de `stxt-core`, con su causa), `ResourceException` (`RESOURCE`; `ImageFormatException` es
`IMAGE_FORMAT`) e `InternalException`. `DocumentReader` y `DocumentSchema` convierten los errores de la librería (un
`LimitException` es `LIMIT`); `Converter` los deja pasar, convierte cualquier otra excepción de ejecución en
`INTERNAL` y no toca la `IOException` del destino; `PdfCanvas.write` saca como `IOException` las
`UncheckedIOException` de las primitivas. `PAGE_LIMIT` (`Kind.LIMIT`, sin línea) sustituye al
`IllegalStateException` del máximo de páginas, con su caso en un grupo nuevo del catálogo (`test/errors/limits/`); «sin
progreso» y «enlace sin destino» son `InternalException`. §6.1 de la especificación (tipos, `kind` y qué contesta
un servicio) y el apartado «Errores» del README. `ConverterErrorsTest` cubre cada `kind` y que el envoltorio de
`INTERNAL` hace falta (falla sin él). **No se tocó** el resto de `IllegalArgumentException`/`IllegalStateException`
de mal uso interno (`ConstraintSpace`, `Style.line`, `ValueParser`…), que el `Converter` ya convierte en `INTERNAL`
si se escapan; ni el control de flujo con `Placement.NoRoom` (AR-13).

### AR-10 · Dos tolerancias · Baja · S · hecho

`Overflow.TOLERANCE = 0.01` frente a `Tolerance.EPSILON` del resto del motor; el comentario admite que «ningún test
depende hoy de la diferencia». `CLAUDE.md` pide usar `Tolerance` y dice que `Overflow` es la excepción. Unificar
(probar con `EPSILON` y ver si algún golden/ejemplo cambia) o dar nombre a las dos en `Tolerance`
(`LAYOUT`, `OVERFLOW`) con su razón. DRY.

**Hecho (sesión 5).** Con `EPSILON` en `Overflow` pasan los 1381 tests, pero unificar haría el motor más estricto (una
caja que se sale 0,005 pt sería un error), y eso es un cambio de comportamiento. Se quedan las dos con nombre y su
razón en `Tolerance`: `EPSILON` (aritmética, el layout) y `MODEL` (0,01 pt, lo que se ve: `Overflow` y la comparación de
los volcados en `ReferenceFiles`); `Leaders` deja su `1e-9` propio (decisión 312).

### AR-11 · `Paginator.paginate` · Baja · S · hecho

96 líneas, complejidad 21, con tres fases (convergencia, segunda fase de numeración, colocación de los `Fixed`)
y los registros privados `Queued` y `Numbering`. Extraer `converge(...)`, `placeFixed(...)` y `Numbering` a su
fichero. Antes de tocarlo, PF-03 (que lo modifica). Sin cambio de comportamiento.

**Hecho (sesión 5).** `paginate` (11 líneas) prepara y encadena `converge` (las pasadas hasta que no cambia nada, 41
líneas; devuelve un `Flow` con los resultados y la numeración), `pages` (la segunda fase, 32) y `placeFixed` (los
`Fixed` de una página, 17); `Numbering` está en su fichero. El PDF de los 11 ejemplos, los 28 golden y dos documentos
grandes de `ScaleProbe` es el mismo, byte a byte (sin fechas ni `ID`), que antes de la sesión 5.

### AR-12 · `ArchitectureTest` solo detecta ciclos · Baja · S

Hoy comprueba que no hay ciclos entre packages de primer nivel. Añadir **reglas de dirección** (hoy se cumplen):
`model` no importa nada; `tree` solo `model` (y `stxt-core`); `fragment` solo `model` y `tree`; `layout` no importa
`paint`; `paint` no importa `stxt`; dentro de `layout`, los subpackages de algoritmos no se importan entre sí salvo
los dos casos actuales (`block` → `inline`/`position`; `table` → `grid`; `flex`/`grid` → `position`); y, tras AR-04,
ningún `…Layout` importa `Engine`. Las reglas se escriben como datos en el test (tabla de permitidos).

**Sesión 9: hecho**: `ALLOWED` (packages), `ALLOWED_IN_LAYOUT` (algoritmos), `noLayoutAlgorithmUsesTheEngine` (AR-04)
y `onlyTheDispatchNamesAnElement` (AR-07); comprobado a mano que las reglas fallan con una dependencia nueva.

### AR-13 · Limpieza · Baja · S

- 14 javadoc nombran un «módulo N» de un documento que no está en el repositorio (`CLAUDE.md` pide no usarlos):
  `grep -rn "módulo [0-9]" src/main`. Sustituir por la sección de la especificación o quitar.
- `Placement.NoRoom` (`extends RuntimeException`) se usa como control de flujo en `GridLayout.arrange` (`catch` en
  `GridLayout.java:73`): valorar devolver un resultado (`Optional`/valor) en vez de lanzar.
- `Box.describe()` y `Box.toString()` duplican la forma de nombrar una caja (`StxtDump`, mensajes): una función.

**Sesión 10: hecho.** Los 13 javadoc y el comentario de `TrackSizingTest` remiten a la sección de la especificación
(`§3`, `§3.6`, `§5.4` a `§5.9`). `Placement.place` devuelve un `Placement.Result` (`Placed` con las áreas o `NoRoom`, un
`record` con el primer ítem que no cabe) y `GridLayout` lo convierte en `GRID_PLACEMENT`, sin `try`. La duplicación
estaba entre `Box.describe()` (volcados) y `Messages.name` (mensajes): queda solo `Box.describe()`, con la forma de los
mensajes (`main (línea 12)`, `de la línea 12`, `(área útil)`), y `StxtDump` escribe el `Id`, que es lo que define el §7;
ningún mensaje ni volcado cambia.

## Plan de sesiones

Cada sesión deja `mvn test` en verde, `git status` limpio y un commit por tarea. Orden pensado para que lo barato y de
más riesgo vaya primero y el refactor grande se haga con red de seguridad.

| Sesión | Tareas | Resultado |
|---|---|---|
| **1. Correcciones y red de seguridad** | QA-01, QA-04, QA-05, QA-09, TS-02 | Fallo del idioma corregido; CI con matriz de idiomas; test de hilos |
| **2. Ligaduras y rendimiento del pintado** (hecha) | QA-03 + PF-01, PF-02, TS-04 | Mide = pinta; pintar ≈ 9,5 veces más rápido por defecto; sin cerrojos calientes |
| **3. Contrato público** (hecha) | TS-01, AR-09, AR-08 + TS-05, QA-06 | `Converter`, errores con tipo común, límites, README de hilos |
| **4. SVG y el estado global** (hecha) | QA-02, TS-03 | Sin `registerFont`; el idioma de la JVM no cambia el PDF |
| **5. Caché de la paginación** (hecha) | PF-03, PF-04, AR-11, AR-10 | Documentos de más de 99 páginas sin trabajo doble |
| **6. Tests de referencia** (hecha) | QA-07, QA-08 (propiedades, determinismo) | `mvn test` no ensucia el árbol y vigila los PNG |
| **7. Encapsulación del modelo** (hecha) | AR-03, AR-02 (etapas 1 y 2) | `Box`/`Style` con accesores y sin campos públicos |
| **8. Constructor del árbol** (hecha) | AR-01 | `BoxTreeBuilder.element` partido por familias |
| **9. Algoritmos y lienzos** (hecha) | AR-04, AR-12, AR-05, AR-06, AR-07 | `LayoutContext`, `Canvas` estrecho, `PdfCanvas` partido |
| **10. Partir `Style`** (hecha) | AR-02 (etapas 3 y 4), AR-13 | Un grupo por familia, cerrado con el árbol; árbol congelado |

Las sesiones 7 a 10 son refactorizaciones grandes: cada tarea puede ocupar una sesión entera y conviene hacerlas con
`ConcurrencyProbe` (comparación del PDF) y los PNG como red.

## Decisiones abiertas

1. ~~**Ligaduras (QA-03/PF-01)**~~ **Resuelta (sesión 2):** las dos, con `Ligatures` en `Typography`, heredable, sin
   ligaduras por defecto.
2. ~~**Política de los PNG de referencia (QA-07)**~~ **Resuelta (sesión 6):** falla el test cuando cambia un PNG,
   salvo con `-Dgolden.update=true` o `-Dexamples.update=true`.
3. ~~**Fachada (AR-08)**~~ **Resuelta (sesión 3):** `PdfWriter` se eliminó y lo sustituye `Converter` (sin código externo
   que migrar, no se dejó nada obsoleto).
4. ~~**SVG con texto (QA-02/TS-03)**~~ **Resuelta (sesión 4):** ni lo uno ni lo otro: JSVG sigue dibujando, pero con el
   fichero de `fonts/` que le da el motor (`PlatformSupport.customFont`), sin registro en AWT ni búsqueda por nombre.

## Registro de sesiones

| Fecha | Sesión | Qué se hizo | Siguiente paso |
|---|---|---|---|
| 2026-10-28 | 0 · Revisión | Lectura del núcleo (`Engine`, `LayoutCache`, `InlineLayout`, `Paginator`, `BoxTreeBuilder`, `Box`, `Style`, `PdfCanvas`, `PdfImages`, recursos); `mvn test` (1314 verdes, cobertura por package); mediciones con `tools/qa/` (hilos, escala, JFR, GSUB, entradas hostiles, `tr`); este documento. **No se ha cambiado código de `src/`.** | Sesión 1 del plan |
| 2026-10-28 | 1 · Correcciones y red de seguridad | QA-01 (`Locale.ROOT`; la suite pasa en turco), QA-04, QA-05 (`Messages.excerpt`), QA-09 (`.github/workflows/ci.yml`) y TS-02 (`ConcurrentConversionTest`). 1321 tests en verde con el idioma por defecto y con `tr`; con `tr` siguen reescribiéndose PNG (QA-02). CI en verde en GitHub (JDK 17, 21 y turco). | Sesión 2 (decidir ligaduras: QA-03) |
| 2026-10-28 | 2 · Ligaduras y rendimiento del pintado | `Ligatures` en `Typography` (heredable, `false` por defecto; schema, §4.4/§5.4/§9, decisiones 289 a 293), medida con ligaduras con el `GsubWorker` de fontbox y pintado con `setEnableGsub`; PF-02 y TS-04 (`FontFile` sin cerrojos con avances precalculados; `Resources`/`ImageLibrary` sin `synchronized` calientes). 22 PNG regenerados (revisados: solo `fi`/`fl`). Suite verde. Medido: pintar ≈ 9,5× más rápido, layout ≈ 2× más rápido. | Sesión 3 (TS-01, AR-09, AR-08 + TS-05, QA-06) |
| 2026-10-28 | 3 · Contrato público (en curso) | AR-08 y TS-05 (commit `a25b014`): `Converter` (inmutable, seguro entre hilos, `layOut`/`toPdf`/`toSvg`) en el package `convert`; `PdfWriter` eliminado y sus llamadores migrados; decisión 294. Nuevos `ConverterTest` (9) y un test de `Converter` compartido entre hilos. | Resto de la sesión 3: TS-01 (contrato de hilos en javadoc/README y `Engine` sin protocolo público), AR-09 (`StxtPdfException`), QA-06 (`Limits` en el `Builder`) |
| 2026-10-28 | 3 · Contrato público (en curso) | AR-09: `StxtPdfException` con `kind`/`code`/`line` para todos los errores del motor; `DocumentSyntaxException` para los de `stxt-core`; `PAGE_LIMIT` (primera parte de QA-06); `Converter` como frontera (`INTERNAL`); §6.1 de la especificación, README, decisiones 295 a 299. Tests nuevos: `ConverterErrorsTest` y el caso `PAGE_LIMIT`. | Resto de la sesión 3: TS-01 (contrato de hilos y `Engine` sin protocolo público) y QA-06 (`Limits` configurable, píxeles de imagen, elementos de SVG) |
| 2026-10-28 | 3 · Contrato público (en curso) | TS-01: contrato de hilos en javadoc y README, protocolo de la paginación de `Engine` de package, `layOutDocument` suelta su estado al terminar; `EngineContractTest`; decisiones 300 a 302. | Último punto de la sesión 3: QA-06 (`Limits` configurable en el `Builder` del `Converter`, píxeles por imagen y elementos de SVG) |
| 2026-10-29 | 3 · Contrato público (terminada) | QA-06: `Limits` (2000 páginas, 25 Mpx, 20 000 elementos y 64 niveles de SVG, elegidos por el usuario tras medirlos con `BombsProbe`) en `Converter.builder().limits(…)`; `IMAGE_TOO_LARGE` y `SVG_TOO_COMPLEX` antes de maquetar; JSVG con los mismos límites (corrige que rechazara SVG legítimos de más de 2000 elementos con una `IOException`); casos del catálogo, `LimitsTest`, §6.2 de la especificación; decisiones 303 a 307. | Sesión 4: SVG y estado global (QA-02, TS-03). Decidir antes la decisión abierta 4 (contornos hechos por el motor o JSVG con sus límites) |
| 2026-10-29 | 4 · SVG y el estado global (hecha) | QA-02 y TS-03: causa aislada (AWT da `Dialog` para `IBM Plex Sans` en una JVM en turco); `SvgFonts` da a JSVG el fichero comprobado con una familia clave (huella SHA-256) y `PlatformSupport.customFont`, sin `registerFont`; `ImageFile.textFonts`, `FontFile.digest`; cerrojo para la caché global de JSVG; un texto de un `<use>` conserva la fuente de su sitio. `PdfImagesTest` (4 tests, 3 fallan sin el cambio) y `SvgImageTest`; §5.18, decisiones 308 a 310; la CI en turco comprueba el árbol. 1379 tests en verde con el idioma por defecto y en turco; ningún PNG ni volcado cambia. | Sesión 5: caché de la paginación (PF-03, PF-04, AR-11, AR-10) |
| 2026-10-29 | 5 · Caché de la paginación (hecha) | PF-03: volver a paginar olvida solo los textos que cambian y sus antepasados (`Engine.forget`, decisión 311; `PaginationCacheTest`; `ScaleProbe` con fallos por página e índice): la mitad de layouts a partir de la página 100. PF-04: medido con JFR, sin cambios (< 1 %); de ahí sale PF-06 (el reparto en líneas, 71 % del layout). AR-10: `Tolerance.EPSILON` y `Tolerance.MODEL` con nombre (decisión 312). AR-11: `paginate` partido en `converge`, `pages` y `placeFixed`; `Numbering` en su fichero. El PDF de los ejemplos, los golden y dos documentos grandes es el mismo byte a byte que antes. 1381 tests en verde. | Sesión 6: tests de referencia (QA-07, QA-08). Decidir antes la decisión abierta 2 (¿falla el test si cambia un PNG?). PF-06 queda pendiente fuera del plan |
| 2026-10-29 | 6 · Tests de referencia (hecha) | QA-07 con la decisión del usuario (un PNG que cambia hace fallar el test; decisión 313): imágenes y PDF en `target/reference/`, `ReferenceFiles.checkImages`, `-Dgolden.update`/`-Dexamples.update`, la CI sube `target/reference` si falla; `ReferenceFilesTest`. QA-08 puntos 3 y 4: `CrossJvmDeterminismTest` y las propiedades de `ValueParser`, `LineBreaker` y la fragmentación (sin fallos encontrados; comprobado con dos mutaciones a mano). 1388 tests en verde; `mvn test` no cambia ningún fichero versionado. | Sesión 7: encapsulación del modelo (AR-03, AR-02 etapas 1 y 2). QA-08 puntos 5 (PIT, una pasada) y 6 (documentos de la especificación con `PdfMetrics` y PDF/UA) pendientes |
| 2026-10-29 | 7 · Encapsulación del modelo (hecha) | AR-02 etapas 1 y 2 (commit `84a461c`): los 80 campos de `Style` son del package `tree` y se leen con un método del mismo nombre; 233 lecturas migradas siguiendo los errores de `javac`; `StyleEditor` para los tests de otros packages. AR-03: `Box` con campos privados, escritura del package y el árbol cerrado al construirlo (`Box.close`, decisión 314); 827 lecturas migradas igual; `BoxTest` (4). 1392 tests en verde; el PDF de los ejemplos y los golden es el mismo byte a byte (sin fechas ni ID). | Sesión 8: constructor del árbol (AR-01) |
| 2026-10-29 | 8 · Constructor del árbol (hecha) | AR-01 en cuatro commits: `MediaReader`; tablas de propiedades en `PropertyReader` y `BoxTreeBuilder.properties`; `ElementReader` (una constante por familia: layout, envoltorio, propiedades del tipo, comprobaciones y contenido) con `element` como el orden común; `TableBuilder.table` y `ModelValidator.validateMargins` partidos. `element` de 162 a 42 líneas y de complejidad 67 a 8; `BoxTreeBuilder` de 617 a 347 líneas; ningún método de `tree` pasa de 44 líneas ni de complejidad 18. Decisión 315. 1392 tests en verde; el PDF de los ejemplos y los golden, el mismo byte a byte. | Sesión 9: algoritmos y lienzos (AR-04, AR-12, AR-05, AR-06, AR-07) |
| 2026-10-29 | 9 · Algoritmos y lienzos (hecha) | AR-04 (`LayoutContext`, despacho con `EnumMap`, un doble en `ImageLayoutTest`), AR-12 (reglas de dirección como datos en `ArchitectureTest`), AR-05 (`Canvas` solo dibuja; `Tagging` aparte, `Canvas.tagging()`), AR-06 (`PdfCanvas` de 788 a 318 líneas: `PdfDocumentWriter`, `PdfContent`, `PdfTagging`, `PdfFonts`, `PdfGradients`; un solo `io`) y AR-07 (`Element.Role`; solo el despacho de `Engine` nombra un elemento fuera de `tree`). Decisión 316. 1398 tests en verde; los PDF de los ejemplos y los golden, iguales byte a byte, y los 54 SVG de los golden, iguales. | Sesión 10: partir `Style` (AR-02 etapas 3 y 4) y AR-13. Fuera del plan: PF-05, PF-06, QA-08 (5 y 6) y `UnitBreaker.Run.layOut` (complejidad 22) |
| 2026-10-29 | Fuera del plan · Tests en Windows | `mvn test` fallaba en Windows (1 fallo y 4 errores) y no en la CI, que es solo Linux: `SvgCanvas` con `%n` (ahora `\n`), `SvgFonts` dejaba abierto el TTF (ahora crea la fuente con su contenido) y `LimitsTest` usaba un enlace simbólico (ahora copia la fuente). Decisión 317. | Valorar `windows-latest` en la CI |
| 2026-10-29 | 10 · Partir `Style` (hecha) | AR-13 (tres commits: el javadoc remite a la especificación; `Placement.place` devuelve `Placed` o `NoRoom`; `Box.describe()` es la única forma de nombrar una caja, sin `Messages.name`). AR-02 etapa 3: los enums a `model` y `Style` partido en diez grupos, uno por commit; etapa 4: `Style` y sus grupos se cierran con la caja (`StyleGroup`). Decisiones 318 a 320. 1400 tests en verde; los 39 PDF y los 54 SVG, iguales byte a byte; `ScaleProbe` dentro del ruido. | Plan terminado. Fuera del plan: PF-05, PF-06, QA-08 (5 y 6), `UnitBreaker.Run.layOut` (complejidad 22) y `windows-latest` en la CI |
| 2026-10-29 | Fuera del plan · PF-06 | El reparto voraz suma los avances de las palabras y mide la línea entera solo cerca del límite; las palabras sin expresión regular; `validate` sin buscar marcadores en cada carácter y con los glifos memorizados; la última fuente recordada. Decisión 321. 1402 tests en verde; los 39 PDF y los 54 SVG, iguales byte a byte. Layout de 33 a 51 % más rápido en `ScaleProbe` (657 páginas: de 743 a 430 ms). | Quedan PF-05, QA-08 (5 y 6), `UnitBreaker.Run.layOut` (complejidad 22) y `windows-latest` en la CI |
