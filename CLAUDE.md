# CLAUDE.md

Reglas para trabajar en este repositorio. Lo que hace el motor y cómo está hecho está en la documentación; aquí solo
está cómo se trabaja.

## Qué es

stxt-pdf es un motor de layout y generación de PDF a partir de documentos escritos en [STXT](https://stxt.dev): los
documentos se maquetan en páginas de tamaño fijo y se escriben en un PDF con las fuentes incrustadas, etiquetado
(PDF/UA-1, o PDF/UA-2 como opción) y con marcadores. Todas las medidas están en puntos tipográficos (1 pt = 1/72 in;
A4 = 595 × 842).

## Documentación

| Dónde | Qué | Para quién |
|---|---|---|
| [`README.md`](README.md) | Índice: qué es, ejemplos, enlaces | Todos |
| [`docs/manual/`](docs/manual/README.md) | Cómo se escribe un documento: comportamiento común y una página por componente | Quien crea PDF: solo sabe STXT y qué es un PDF |
| [`docs/referencia/`](docs/referencia/README.md) | **La fuente de verdad del comportamiento**: comportamiento común, una página por componente, las decisiones de diseño, la [arquitectura](docs/referencia/arquitectura.md) y los [detalles de implementación](docs/referencia/README.md#implementación) | Quien necesita la regla exacta, y quien trabaja en el motor |
| [`TODO.md`](TODO.md) | Lo pendiente | El autor (su apartado) y Claude (el suyo) |

- **Léete la [arquitectura](docs/referencia/arquitectura.md)** antes de tocar el código, y la página de implementación de la
  parte que vayas a cambiar ([árbol](docs/referencia/implementacion/arbol.md), [layout](docs/referencia/implementacion/layout.md),
  [pintado](docs/referencia/implementacion/pintado.md), [el PDF por dentro](docs/referencia/implementacion/pdf.md),
  [uso desde Java](docs/referencia/implementacion/api.md),
  [pruebas](docs/referencia/implementacion/pruebas.md)).
- **El manual no menciona CSS ni HTML**: explica los conceptos como si no existieran. La referencia tampoco: el modelo se
  inspiró en CSS, pero no lo es ni lo pretende. Todo debe ser claro, definido y sencillo, sin interpretaciones.
- **El manual y la referencia dicen lo mismo**; si discrepan, manda la referencia. Un cambio de comportamiento se escribe en
  las dos.
- **Las decisiones** de interpretación (un hueco de la referencia, una simplificación, una alternativa descartada) se
  escriben en la página de la referencia a la que afectan, en un bloque `> **Decisión:** … *Alternativa descartada:* …`.
- **`TODO.md`:** el primer apartado es del autor y no se toca. El segundo es de Claude: apunta ahí lo que salga al trabajar
  (lo que queda de algo, una mejora vista de pasada) y quita lo que se haga.
- **Las referencias desde el código** a la documentación se escriben con la ruta dentro de `docs/`:
  `(referencia/componentes/grid.md)`.
- **Ejemplos de la documentación:** cada documento completo (`Document (dev.stxt.pdf):` en un bloque de código) del
  README, del manual y de la referencia se maqueta y se valida con veraPDF en `DocumentationExamplesTest`. Cada página de
  componente del manual tiene al menos uno. El [ejemplo completo](docs/referencia/comun/ejemplo-completo.md) es
  `test/spec/report.stxt`.

**Idioma:** la documentación, los comentarios, el javadoc y los mensajes de error están en **castellano**. Los **términos
técnicos están en inglés**: identificadores Java (packages, clases, métodos, campos, variables, tests) y nombres de nodos y
valores STXT. El contenido de `Text` en los documentos puede estar en cualquier idioma.

## Comandos

Java 17 y Maven (`dev.stxt:stxt-pdf`, package raíz `dev.stxt.pdf`).

```bash
mvn test                                   # compila, ejecuta todos los tests y deja la cobertura (JaCoCo) en target/site/jacoco/index.html
mvn test -Dtest=ValueParserTest            # una clase
mvn test -Dtest='TrackSizingTest#nombre'   # un método
mvn test -Dtest=GoldenTest -Dgolden.update=true        # acepta los PNG que cambian en los casos golden
mvn test -Dtest=ExamplesTest -Dexamples.update=true    # acepta el volcado, el PDF y los PNG de los ejemplos
```

`mvn test` no escribe nada en `test/`: si cambia una imagen de referencia, el test falla y deja las nuevas en
`target/reference/`. Míralas antes de aceptarlas. Más en [pruebas](docs/referencia/implementacion/pruebas.md).

Eclipse: el repositorio incluye `.project`, `.classpath` y `.settings/` (proyecto `stxt-pdf`, Java + m2e, Java 17,
UTF-8). Si cambian las dependencias: *Maven → Update Project*.

## Convenciones del código

- **Dependencias entre packages en un solo sentido** (`model` ← `tree` ← `fragment` ← `layout` ← `paint` ← `convert`), sin
  ciclos; `ArchitectureTest` tiene escrito qué puede usar cada package y cada algoritmo de `layout`. Una dependencia nueva
  se decide y se apunta allí. Un tipo que necesitan `tree` y `layout` va en `model`.
- **El árbol de cajas solo se escribe en `tree`** y se cierra al construirlo. Fuera se lee con métodos del nombre de la
  propiedad, y las de un grupo, a través de él (`style.typography().fontSize()`). Una propiedad nueva va en el grupo de su
  sitio en el documento, con su lectura y su `set…`.
- **La geometría solo existe en los fragmentos**; `Box` no guarda posición ni tamaño.
- **Toda llamada recursiva de layout pasa por `LayoutContext`** (`context.layout`, `context.intrinsic`), para usar la caché.
- **Fuera de `tree`, se pregunta el papel de una caja (`Element.Role`), nunca su elemento.**
- **Las entradas y salidas del layout son `record` con listas copiadas.**
- **Comparar medidas** con `Tolerance.fits`, `Tolerance.same` o `Tolerance.EPSILON`, nunca con un `1e-9` propio;
  `Tolerance.MODEL` (0,01 pt) es la de lo que se ve.
- **Min y max:** primero el máximo y luego el mínimo; si chocan, gana el mínimo (`Sizing.constrain`).
- **Valores especificados frente a usados:** `Value` conserva lo que escribió el autor; se convierte a puntos con
  `resolve(...)` durante el layout.
- **Enums de los valores** (`model`): cada constante se llama como su valor en el documento (`TextAlign.JUSTIFY` ↔
  `justify`, `Marker.LOWER_ALPHA` ↔ `lower-alpha`), porque se leen con `Enum.valueOf`.
- **Errores:** todo error que puede ver quien convierte es un `StxtPdfException` (los del modelo, `InvalidModelException`
  con su `ErrorCode` y la línea del nodo culpable: la de la propiedad, no la de su caja). Un `IllegalStateException` solo
  para un mal uso interno.
- **Nombres STXT:** en inglés, y dentro de un grupo, el nombre corto (`Size`, `Before`) si no choca con otro nodo del
  namespace; cada nombre tiene una sola definición en todo el schema. Las propiedades de varias palabras van separadas por
  espacio y con mayúscula solo la primera (`Line height`).
- **Términos del dominio en castellano en la documentación** («área útil», «caja de borde», «viudas»); en el código, su
  equivalente inglés (`pageArea`, `borderBox`, `widows`).
- Indentación de 4 espacios en Java; tabuladores en los ficheros `.stxt`. Comentarios y javadoc a 120 columnas.

## Tests

- JUnit 5. Un test por pieza pura, en el mismo package que la clase. Los nombres de los métodos de test están en inglés y
  en camelCase (`autoMarginsCenter`).
- Los tests de un algoritmo maquetan documentos pequeños con `LayoutFixture` (con `FixedMetrics`: cada carácter mide 5 pt
  con `Size: 10`). Un test de layout no puede desbordar: los casos inválidos van al catálogo de errores.
- **Catálogo de errores:** un fichero `test/errors/<grupo>/<código>.<descripción>.stxt` por caso; cada `ErrorCode` tiene
  al menos uno.
- **Golden:** el `expected.stxt` de un caso de `test/golden/` se calcula a mano siguiendo la referencia, nunca se genera.
- **Ejemplos:** un ejemplo nuevo de `test/examples/` es un documento real, con sentido, y se añade con su fila en su
  `README.md`. Sus imágenes las genera `tools/ExampleImages.java`: si necesitas una, añádela al generador en vez de subir un
  binario.
- Cada corrección de un fallo lleva su test, que falla sin ella.

## Al cambiar el comportamiento

1. **La referencia manda:** si el código y la referencia discrepan, aclara con el usuario cuál es el correcto antes de
   cambiar ninguno de los dos. Un cambio de comportamiento se habla antes con el usuario, y no se generaliza una regla nueva
   sin comentarlo.
2. **Una propiedad nueva o cambiada:** el schema `.stxt` (en su grupo o en cada elemento que la admite), su tabla y su
   explicación en la página de la referencia y del manual, el resumen del schema en
   [estructura](docs/referencia/comun/estructura.md#schema), el grupo de `Style` que le toca, su lectura (`PropertyReader`,
   o `ElementReader` si es propia de un elemento) y, si aplica, su validación (`ModelValidator`).
3. **Una regla de validación nueva:** su `ErrorCode`, su fila en la tabla de [errores](docs/referencia/comun/errores.md) y un
   caso en el catálogo.
4. **Un elemento nuevo:** su página de componente en el manual (con un documento completo) y en la referencia, y su fila en
   los índices.
5. **Cada decisión de interpretación**, en un bloque «Decisión» de la página de la referencia a la que afecta.
6. Si algo queda pendiente, apúntalo en tu apartado de `TODO.md`.
