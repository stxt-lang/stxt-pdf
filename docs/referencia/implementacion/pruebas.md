# Pruebas

```bash
mvn test                                   # compila, ejecuta todos los tests y deja la cobertura en target/site/jacoco/index.html
mvn test -Dtest=ValueParserTest            # una clase
mvn test -Dtest='TrackSizingTest#nombre'   # un método
```

`mvn test` no escribe nada en `test/`: lo que generan los tests va a `target/`.

## Tests unitarios y de layout

JUnit 5. Un test por pieza pura, en el mismo package que la clase (`src/test/java/dev/stxt/boxes/...`). Los nombres de los
métodos de test están en inglés y en camelCase (`autoMarginsCenter`).

Los documentos de test se leen con `DocumentReader`, que valida contra el schema: un test de una regla del modelo comprueba
así que el schema no la detecta antes. Los tests de un algoritmo maquetan documentos pequeños con `LayoutFixture` (A4: área
útil de 515 × 762 pt desde (40, 40); `rect(id)` da la caja de borde en coordenadas de página). La fixture declara en
`Element: Document` `Font: IBM Plex Sans` y `Size: 10` (`Docs.mergeStyles` junta los `Styles` del test con ese y envuelve el
contenido en una `Section`). Con `FixedMetrics`, cada carácter mide 5 pt con `Size: 10`. `layOutPaged(alto, …)` pagina (con
180 pt de alto, el área útil mide 100) y `rect(página, id)` / `has(página, id)` miran una página concreta. Un test de
layout no puede desbordar: los desbordamientos van al catálogo de errores.

## Tests de referencia (`GoldenTest`)

Cada carpeta de `test/golden/NN-nombre/` tiene `input.stxt` y `expected.stxt` ([el volcado](#el-volcado)); los rectángulos
se comparan con una tolerancia de 0,01 pt. El resultado esperado se calcula a mano siguiendo la referencia, como en el
[ejemplo completo](../comun/ejemplo-completo.md), y no se actualiza nunca. Cada caso escribe un SVG de depuración en
`target/golden-svg/`, y `output.pdf` y un `page-N.png` por página (con los contornos de las cajas y las métricas reales) en
`target/reference/golden/NN-nombre/`. Cada imagen se compara con el `page-N.png` versionado en la carpeta del caso: si
cambia más allá del antialiasing, que varía entre máquinas (16 por canal), o hay páginas de más o de menos, el caso **falla**
con la ruta de las imágenes nuevas. Si el cambio es el que se busca, se acepta con `-Dgolden.update=true`, que reescribe solo
las que cambian. La CI sube `target/reference` cuando falla.

> **Decisión:** una imagen de referencia que cambia hace fallar el test, y solo se acepta a mano: así un cambio no se queda
> sin ver en `git status`. *Alternativa descartada:* reescribirlas en cada ejecución y que la CI compruebe el árbol, que
> avisa pero no dice qué página ni deja las imágenes a mano.

## Ejemplos (`ExamplesTest`)

Cada carpeta de `test/examples/NN-nombre/` es un documento completo (`input.stxt`, con su `styles.stxt`) con un `page-N.png`
por página (versionados, sin contornos, a 110 ppp), el `output.pdf` y un `expected.stxt` con el volcado de las cajas con
`Id`. Se maquetan con las **métricas reales** y la carpeta `resources/`, y el PDF se valida con veraPDF (PDF/UA-1). El volcado
no se calcula a mano: es una instantánea que se genera con `-Dexamples.update=true`, que también acepta las imágenes que
cambian y reescribe el `output.pdf`, y se revisa mirando las imágenes. Con `-Dexamples.only=NN-nombre` se prueba un solo
ejemplo. Las imágenes de los ejemplos están en `resources/images/examples/` y las genera `java tools/ExampleImages.java`
(Java2D, sin ficheros de terceros).

> **Decisión:** los ejemplos se maquetan con las métricas reales y su volcado es una instantánea que se revisa mirando las
> imágenes: con texto y fuentes reales, calcularlo a mano no tiene sentido. *Alternativa descartada:* comparar solo las
> imágenes, que no detecta cambios de un punto.

> **Decisión:** el `output.pdf` de cada ejemplo se versiona para poder abrirlo desde el repositorio, pero solo se escribe con
> `-Dexamples.update=true` y no se compara: lleva la fecha y un identificador que cambian en cada ejecución. Lo que se vigila
> son las imágenes y el volcado.

> **Decisión:** las imágenes de los ejemplos las genera un programa (`tools/ExampleImages.java`): no hay ficheros de
> terceros ni derechos que respetar, y se pueden cambiar retocando el programa. Las fuentes son IBM Plex Sans, Serif y Mono,
> con su licencia OFL. *Alternativa descartada:* imágenes de bancos libres, que habría que citar y que no se pueden
> regenerar.

## Documentos de la documentación (`SpecificationDocumentsTest`)

Cada documento completo (`Document (dev.stxt.boxes):` en un bloque de código) del [manual](../../manual/README.md) y de la
referencia se maqueta con las métricas reales y se escribe en PDF, que debe cumplir PDF/UA-1 según veraPDF; el PDF queda en
`target/reference/docs/`. El [ejemplo completo](../comun/ejemplo-completo.md) debe ser `test/spec/report.stxt` sin su cabecera
de comentarios. Así ningún ejemplo de la documentación deja de funcionar sin que se note.

## Catálogo de errores (`ErrorCatalogTest`)

`test/errors/<grupo>/<código>.<descripción>.stxt` es un documento STXT de verdad, un fichero por caso, en la carpeta de su
familia (`document`, `schema`, `values`, `structure`, `headings`, `lists`, `tables`, `columns`, `markdown`, `images`,
`references`, `links`, `contents`, `sections`, `styles`, `combinations`, `overflow`, `fonts`, `limits`; los códigos de
`stxt-core` van en `schema`). Las tres primeras líneas son comentarios `# Case:` (descripción), `# Code:` y `# Line:`, y la
línea esperada es la del propio fichero, cabecera incluida (la primera línea del documento es la 4). Se maqueta con las
métricas reales. `everyCodeHasACase` exige un caso por cada `ErrorCode` salvo los de `UNCOVERED`. Los tests de layout prueban
los casos válidos; los inválidos van al catálogo.

> **Decisión:** el catálogo tiene un fichero por caso, un documento STXT de verdad, para poder abrirlo y validarlo en el
> editor sin contar líneas desde un bloque incrustado. *Alternativa descartada:* un fichero por grupo con los documentos
> incrustados.

## Propiedades, determinismo y mutación

- **Propiedades:** `ValueParserPropertiesTest`, `LineBreakerPropertiesTest` y `FragmentationPropertiesTest` prueban
  propiedades con miles de entradas al azar de un `Random` con semilla fija (un fallo se repite; sin librerías nuevas).
- **Determinismo:** `CrossJvmDeterminismTest` comprueba que una JVM hija da el mismo PDF de cada ejemplo y golden.
- **Hilos:** `ConcurrentConversionTest` y `EngineContractTest` ([Uso desde Java](api.md#hilos)).
- **Arquitectura:** `ArchitectureTest` comprueba las dependencias entre packages ([Arquitectura](../arquitectura.md)).
- **Mutación:** el perfil `pit` del `pom.xml` pasa PIT por `layout` y `fragment`
  (`mvn -Ppit test-compile org.pitest:pitest-maven:mutationCoverage`, ≈ 13 min; informe en `target/pit-reports/`). No está en
  la CI: es una pasada puntual para ver si los asserts miden algo. Deja fuera los tests que escriben en carpetas compartidas
  de `target/` o arrancan otra JVM.
- **Rendimiento:** los tests con `@Tag("perf")` no van en `mvn test`; se ejecutan con
  `mvn test -Dgroups=perf -DexcludedGroups=none -Dtest=…`. Las sondas de medición están en [`tools/qa/`](../../../tools/qa).

## Integración continua

`.github/workflows/ci.yml` ejecuta `mvn test` con JDK 17 y 21 y con la JVM en turco (que comprueba que el idioma de la JVM no
cambia nada y que el árbol queda limpio), y, en la ejecución diaria y a mano, en Windows.

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

- Solo se vuelcan las cajas con `Id`; las descendientes con `Id` se anidan dentro.
- El marcador de un `List item` con `Id` se vuelca dentro del ítem como `Marker: 3.`, con su `Rect`.
- Un `Absolute` se anida dentro de su contenedor de referencia, detrás de sus hijos en flujo, y no dentro de su padre.
- Las columnas de un `Multicol` no tienen fragmento: sus hijos se anidan dentro del `Multicol`, y una caja partida entre dos
  columnas sale dos veces en la misma página.
- Las coordenadas son las de la página de su sección; una `Section` no se vuelca como caja, y los `Fixed` del documento
  salen antes que los de la sección.
- Las cajas sin `Id` pueden incluirse para depurar.
- No se vuelcan las líneas de texto, los tramos, los enlaces, el tipo de elemento, el aspecto (fondos, bordes, esquinas) ni
  el giro: no cambian el layout, y el `Rect` de una caja girada es el de sin girar.

> **Decisión:** los tramos no se vuelcan: se prueban con tests de layout y de pintado. *Alternativa descartada:* volcar las
> líneas con sus tramos.

> **Decisión:** el `Rect` de una caja girada es el de sin girar, como el de un enlace o un fondo, porque el giro no cambia el
> layout.
