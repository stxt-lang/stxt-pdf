# Pruebas

```bash
mvn test                                   # compila, ejecuta todos los tests y deja la cobertura en target/site/jacoco/index.html
mvn test -Dtest=ValueParserTest            # una clase
mvn test -Dtest='TrackSizingTest#nombre'   # un método
mvn -Pquick test                           # el bucle de desarrollo: sin los tests lentos ni la cobertura
```

`mvn test` no escribe nada en `test/`: lo que generan los tests va a `target/`.

Los tests de integración lentos llevan `@Tag("slow")`: los ejemplos (`ExamplesTest.examples`), los documentos de la
documentación (`DocumentationExamplesTest.documentationDocuments`), las conversiones desde varios hilos
(`ConcurrentConversionTest`) y los que arrancan otra JVM (`CrossJvmDeterminismTest` y
`PdfImagesTest.theLanguageOfTheJvmDoesNotChangeTheText`). Suman más de la mitad de lo que tarda la suite. El perfil
`quick` los deja fuera, junto con JaCoCo, para que el informe de `target/site/jacoco/` sea siempre el de una suite
completa. Es para las vueltas de un cambio: antes de darlo por terminado se pasa `mvn test`, que es lo que pasa la CI.

## Tests unitarios y de layout

JUnit 5. Un test por pieza pura, en el mismo package que la clase (`src/test/java/dev/stxt/pdf/...`). Los nombres de los
métodos de test están en inglés y en camelCase (`autoMarginsCenter`).

Los documentos de test se leen con `DocumentReader`, que valida contra el schema: un test de una regla del modelo comprueba
así que el schema no la detecta antes. Los tests de un algoritmo maquetan documentos pequeños con `LayoutFixture` (A4, con un
área útil de 515 × 762 pt desde (40, 40), y `rect(id)` da la caja de borde en coordenadas de página). La fixture declara en
`For element: Document` `Font: IBM Plex Sans` y `Size: 10` (`Docs.mergeStyles` junta los `Styles` del test con ese y envuelve el
contenido en una `Section`). Con `FixedMetrics`, cada carácter mide 5 pt con `Size: 10`. `layOutPaged(alto, ...)` pagina (con
180 pt de alto, el área útil mide 100) y `rect(página, id)` / `has(página, id)` miran una página concreta. Un test de
layout no puede desbordar: los desbordamientos van al catálogo de errores.

Los casos de prueba que no son código están en `test/` (`golden/`, `errors/`, `examples/`, `spec/` y `resources/`), que
los tests leen por ruta de disco y no del classpath. `test/resources/` es una carpeta de recursos con una sola fuente, IBM
Plex Sans SemiBold en `.otf` con contornos CFF, para probar esas fuentes (`FontLibraryTest`, `FontFileTest` y
`PdfCanvasTest`) sin ponerlas en la carpeta por defecto, donde se incrustarían enteras en los PDF de los ejemplos. Las imágenes de los tests están en la carpeta de recursos del repositorio,
`resources/images/test/` (`wide.png` 150 × 75 pt, `tall.jpg` 90 × 120, `square.png` 30 × 30, `shapes.svg` 150 × 75 y
`label.svg` 120 × 36, y `drawing.gif`, `broken.png` y los `bad-*.svg` para los errores), porque los tests de layout, el
catálogo de errores y los casos golden usan la carpeta por defecto.

## Tests de referencia (`GoldenTest`)

Cada carpeta de `test/golden/NN-nombre/` tiene `input.stxt` y `expected.stxt` ([el volcado](#el-volcado)). Los rectángulos
se comparan con una tolerancia de 0,01 pt. El resultado esperado se calcula a mano siguiendo la referencia, como en el
[ejemplo completo](../common/complete-example.md), y no se actualiza nunca. Cada caso escribe un SVG de depuración en
`target/golden-svg/`, y `output.pdf` y un `page-N.png` por página (con los contornos de las cajas y las métricas reales) en
`target/reference/golden/NN-nombre/`. Cada imagen se compara con el `page-N.png` versionado en la carpeta del caso: si
cambia más allá del antialiasing, que varía entre máquinas (16 por canal), o hay páginas de más o de menos, el caso **falla**
con la ruta de las imágenes nuevas. Si el cambio es el que se busca, se acepta con `-Dgolden.update=true`, que reescribe solo
las que cambian. La CI sube `target/reference` cuando falla.

> **Decisión:** una imagen de referencia que cambia hace fallar el test, y solo se acepta a mano: así un cambio no se queda
> sin ver en `git status`. *Alternativa descartada:* reescribirlas en cada ejecución y que la CI compruebe el árbol, que
> avisa pero no dice qué página ni deja las imágenes a mano.

## Ejemplos (`ExamplesTest`)

Cada carpeta de `test/examples/NN-nombre/` es un documento completo escrito en dos idiomas, cada uno en su subcarpeta:
castellano (`es/`), el original, e inglés (`en/`), su espejo. Las dos versiones comparten el `styles.stxt` de la carpeta del
ejemplo. Cada idioma tiene su `input.stxt`, la imagen de su primera página (`page-1.png`, versionada, sin contornos, a 110
ppp), el `output.pdf` y un `expected.stxt` con el volcado de las cajas con `Id`. Se maquetan con las **métricas reales** y la carpeta `resources/`, se comprueba el contraste del texto, y el PDF se
valida con veraPDF, como PDF/UA-1, escrito también como PDF/UA-2 y escrito como PDF/A-3a. El
volcado no se calcula a mano: es una instantánea que se genera con `-Dexamples.update=true`, que también acepta el
`output.pdf` y la portada si cambia, y se revisa mirando las imágenes: el test escribe las de todas las páginas en
`target/reference/examples/NN-nombre/<idioma>/` y, cuando el PDF no es el versionado, el versionado y sus páginas en el
`before/` de esa carpeta, para comparar el antes y el después. `-Dexamples.only` limita la prueba a un ejemplo
(`01-invoice`), a un idioma (`en`) o a los dos (`01-invoice/en`). Un test comprueba además que la versión inglesa es espejo
de la castellana: los mismos nodos en el mismo orden, los mismos `Id` y `Style`, la misma imagen en su carpeta de idioma y
`Language: en`. Solo cambian los textos. Las imágenes de los ejemplos están en `resources/images/examples/` y las genera
`java tools/ExampleImages.java` (Java2D, sin ficheros de terceros). Las que llevan texto (los gráficos del informe, las
figuras del manual y el sello del certificado) se generan una vez por idioma, en `es/` y `en/`.

> **Decisión:** cada ejemplo está en los dos idiomas de la documentación, con un único `styles.stxt` para los dos: el
> documento lleva el contenido y los estilos son los mismos en cualquier idioma. *Alternativa descartada:* un árbol de
> ejemplos por idioma (`test/examples/es/`, `test/examples/en/`), que duplica los estilos y obliga a mantenerlos iguales.

> **Decisión:** los ejemplos se maquetan con las métricas reales y su volcado es una instantánea que se revisa mirando las
> imágenes: con texto y fuentes reales, calcularlo a mano no tiene sentido. *Alternativa descartada:* comparar solo las
> imágenes, que no detecta cambios de un punto.

> **Decisión:** el `output.pdf` de cada ejemplo se versiona para poder abrirlo desde el repositorio y se compara byte a
> byte: el mismo documento da los mismos bytes ([El PDF](pdf.md#páginas-fuentes-y-metadatos)), así que
> un `output.pdf` distinto es un cambio de verdad, y el nuevo queda en `target/reference/`. De las imágenes solo se
> versiona la primera página de cada idioma, la que enseñan las galerías de los README, comparada como una golden; las
> demás solo se escriben en `target/`, para mirarlas. *Alternativas descartadas:* no comparar el PDF, que dejó en el
> repositorio PDF que ya no eran los que daba el motor; versionar todas las páginas, que no comprobaba nada que el PDF no
> compruebe (la imagen sale de él) y sumaba unos 8 MB al historial con cada aceptación; bajarlas a 72 ppp, que solo
> ahorraba la mitad; versionar el SHA-256 del PDF en lugar del fichero, que quita el PDF que el README abre; y Git LFS,
> cuya cuota agota una CI que descarga los ficheros en cada ejecución.

> **Decisión:** cada ejemplo se valida en una sola variante de PDF/A, la 3a. La variante solo cambia la identificación
> del XMP (`pdfaid`), no el contenido; las reglas del nivel A incluyen las del B, y sin adjuntos la parte 3 comprueba lo
> mismo que la 2. Que cada variante se declara y cumple su perfil lo comprueba `PdfATest`. *Alternativa descartada:* las
> cuatro variantes en cada ejemplo, que repetía las mismas reglas sobre el mismo contenido cuatro veces;
> y una variante en un solo ejemplo, que dejaba sin comprobar en los demás las reglas que dependen del contenido (SVG,
> páginas de PDF, degradados, opacidades).

> **Decisión:** las imágenes de los ejemplos las genera un programa (`tools/ExampleImages.java`): no hay ficheros de
> terceros ni derechos que respetar, y se pueden cambiar retocando el programa. Las fuentes son IBM Plex Sans, Serif y Mono,
> con su licencia OFL. *Alternativa descartada:* imágenes de bancos libres, que habría que citar y que no se pueden
> regenerar.

## Documentos de la documentación (`DocumentationExamplesTest`)

Cada documento completo (`Document (dev.stxt.pdf):` en un bloque de código) del [manual](../../manual/README.md) y de la
referencia se maqueta con las métricas reales, se comprueba su contraste y se escribe en PDF, que debe cumplir PDF/UA-1 y,
escrito como PDF/UA-2, PDF/UA-2 según veraPDF. El PDF queda
en `target/reference/docs/`. El [ejemplo completo](../common/complete-example.md) debe ser `test/spec/report.stxt` sin su cabecera
de comentarios. Así ningún ejemplo de la documentación deja de funcionar sin que se note.

## Catálogo de errores (`ErrorCatalogTest`)

`test/errors/<grupo>/<código>.<descripción>.stxt` es un documento STXT de verdad, un fichero por caso, en la carpeta de su
familia (`document`, `schema`, `values`, `structure`, `headings`, `lists`, `tables`, `columns`, `markdown`, `images`,
`references`, `links`, `contents`, `sections`, `styles`, `combinations`, `overflow`, `fonts` y `limits`, y los códigos
de `stxt-core` van en `schema`). Las tres primeras líneas son comentarios `# Case:` (descripción), `# Code:` y `# Line:`, y la
línea esperada es la del propio fichero, cabecera incluida (la primera línea del documento es la 4). Se maqueta con las
métricas reales. `everyCodeHasACase` exige un caso por cada `ErrorCode` salvo los de `UNCOVERED`. Los tests de layout prueban
los casos válidos, y los inválidos van al catálogo.

> **Decisión:** el catálogo tiene un fichero por caso, un documento STXT de verdad, para poder abrirlo y validarlo en el
> editor sin contar líneas desde un bloque incrustado. *Alternativa descartada:* un fichero por grupo con los documentos
> incrustados.

## Propiedades, determinismo y mutación

- **Propiedades:** `ValueParserPropertiesTest`, `LineBreakerPropertiesTest` y `FragmentationPropertiesTest` prueban
  propiedades con miles de entradas al azar de un `Random` con semilla fija (un fallo se repite) y sin librerías nuevas.
- **Determinismo:** `CrossJvmDeterminismTest` comprueba que una JVM hija da el mismo PDF, byte a byte, de cada ejemplo y
  golden, y `PdfIdTest`, que el identificador sale del contenido.
- **Hilos:** `ConcurrentConversionTest` y `EngineContractTest` ([Uso desde Java](api.md#hilos)).
- **Cancelación:** `CancellationTest` (las `Cancellation` de serie) y `ConversionCancellationTest`, que comprueba que el
  motor pregunta en cada fase y que, pedida en cualquiera de ellas, para allí sin envolver el error y sin escribir nada
  ([Uso desde Java](api.md#cancelar-una-conversión)).
- **Arquitectura:** `ArchitectureTest` comprueba las dependencias entre packages ([Arquitectura](../architecture.md)).
- **API pública:** `PublicApiTest` lee la tabla de «Lo que se promete» de [Uso desde Java](api.md#lo-que-se-promete) y
  comprueba que cada tipo y cada miembro prometido existe y es público, que ninguna firma prometida expone un tipo del
  motor que no lo esté ni uno de otra librería que no sea de `stxt-core` o el `PDDocument` y el `PDPage` de PDFBox, y
  que la tabla de `docs/en/` dice lo mismo.
- **Mutación:** el perfil `pit` del `pom.xml` pasa PIT por `layout` y `fragment`
  (`mvn -Ppit test-compile org.pitest:pitest-maven:mutationCoverage`, unos 13 min en Linux y cerca de una hora en
  Windows, con el informe en
  `target/pit-reports/`). No está en
  la CI: es una pasada puntual para ver si los asserts miden algo. Deja fuera los tests que escriben en carpetas compartidas
  de `target/` o arrancan otra JVM.
- **Rendimiento:** los tests con `@Tag("perf")` no van en `mvn test` ni en la CI: son sondas que se ejecutan a mano,
  como las de [`tools/qa/`](../../../../tools/qa), e imprimen sus medidas sin afirmar nada sobre ellas. Se ejecutan con
  `mvn test -Dgroups=perf -DexcludedGroups=none -Dtest=...`. Lo que sí vigila la suite es la complejidad: cuánto
  trabajo cuesta el doble de documento (`LongTextTest`, `LayoutCacheTest`, `PaginationCacheTest`).
  Los de `@Tag("slow")` sí van en `mvn test`, y solo los deja fuera `-Pquick` (ver el principio de esta página).

## Integración continua

`.github/workflows/ci.yml` ejecuta `mvn test` con JDK 17 y 21 y con la JVM en turco (que comprueba que el idioma de la JVM no
cambia nada y que el árbol queda limpio), y, en la ejecución diaria y a mano, en Windows. veraPDF (`PdfUa`) valida con el
locale neutro como predeterminado de la JVM, porque en 1.28.2 pasa a minúsculas los tipos de valor de los esquemas de
extensión XMP con el locale predeterminado y en turco `Integer`, el tipo de `pdfuaid:part`, se convierte en un tipo que no
conoce: todo PDF/A fallaría 6.6.2.3.1 y 6.6.2.3.3 con un PDF que es byte a byte el de cualquier otro locale.

## El volcado

El motor puede volcar el resultado en STXT (`StxtDump`), que es el formato de los `expected.stxt`. Cada `Rect` es la caja de
borde en **coordenadas absolutas de página**: `x y ancho alto`, en pt, con hasta dos decimales.

```
Result:
    Page: 1
        Fragment: header
            Rect: 40 10 515 20
        Fragment: body
            Rect: 40 80 515 300
            Fragment: side
                Rect: 40 80 150 120
```

Los volcados no tienen namespace y se leen con `STXT.rawParser()`.

- Solo se vuelcan las cajas con `Id`, y las descendientes con `Id` se anidan dentro.
- El marcador de un `List item` con `Id` se vuelca dentro del ítem como `Marker: 3.` (o `Marker image: ruta`, con una
  imagen), con su `Rect`.
- Las líneas de los bordes fusionados de una `Table` con `Id` se vuelcan detrás de su `Rect`, una por línea y en el
  orden en que se pintan: `Rule: x y ancho alto color`, en coordenadas de la página.
- Un `Overlay` se anida dentro de su contenedor de referencia, detrás de sus hijos en flujo, y no dentro de su padre.
- Las columnas de un `Columns` no tienen fragmento: sus hijos se anidan dentro del `Columns`, y una caja partida entre dos
  columnas sale dos veces en la misma página.
- Las coordenadas son las de la página de su sección. Una `Section` no se vuelca como caja, y los `Page overlay` del documento
  salen antes que los de la sección.
- Las cajas sin `Id` pueden incluirse para depurar.
- No se vuelcan las líneas de texto, los tramos, los enlaces, el tipo de elemento, el aspecto (fondos, bordes, esquinas) ni
  el giro. El `Rect` de un posicionado girado es el de sin girar. El de una caja girada en flujo es el rectángulo que
  ocupa, y los de sus descendientes, sin girar.

> **Decisión:** los tramos no se vuelcan: se prueban con tests de layout y de pintado. *Alternativa descartada:* volcar las
> líneas con sus tramos.

> **Decisión:** el `Rect` de un posicionado girado es el de sin girar, como el de un enlace o un fondo, porque su giro no
> cambia el layout. El de una caja girada en flujo es el rectángulo que ocupa, que es lo que su giro cambia en el layout.
