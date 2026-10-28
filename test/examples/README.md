# Ejemplos

Documentos completos, como los que escribiría un autor, para ver lo que da el motor con las
fuentes y las imágenes reales de [`resources/`](../../resources) y **sin contornos de
depuración**. A diferencia de los casos [golden](../golden), que comprueban un algoritmo con un
resultado calculado a mano, un ejemplo es un documento con sentido, con su contenido y su estilo.

Cada carpeta tiene:

| Fichero | Qué es |
|---|---|
| `input.stxt` | El documento. Valida con el schema y se maqueta sin errores. |
| `output.pdf` | El PDF (etiquetado, con marcadores y las fuentes incrustadas). |
| `page-N.png` | Una imagen por página del PDF, a 110 ppp. |
| `expected.stxt` | El volcado ([§7](../../docs/ESPECIFICACION.md#7-formato-de-resultados)) de las cajas con `Id`, con las métricas reales. |

Los ejemplos los ejecuta `ExamplesTest` con `mvn test`: comprueba que el documento se lee y se
maqueta y que el volcado no cambia (tolerancia de 0,01 pt). Las imágenes y el PDF solo se
reescriben si cambian los píxeles de alguna página, así que si aparecen modificados en git, el
resultado ha cambiado: míralos antes de dar el cambio por bueno. El volcado esperado no se
calcula a mano, es una instantánea de lo que da el motor, que se revisa mirando las imágenes.

```bash
mvn test -Dtest=ExamplesTest                                   # todos
mvn test -Dtest=ExamplesTest -Dexamples.only=01-invoice        # uno
mvn test -Dtest=ExamplesTest -Dexamples.update=true            # regenera los expected.stxt
```

Las imágenes están en [`resources/images/examples/`](../../resources/images/examples) y las
genera [`tools/ExampleImages.java`](../../tools/ExampleImages.java) con Java2D, sin ficheros de
terceros (`java tools/ExampleImages.java`). Las fuentes son IBM Plex Sans, Serif y Mono (SIL
Open Font License). Los nombres, las empresas y los datos de los documentos son inventados.

## Los once documentos

| | Documento | Qué enseña |
|---|---|---|
| [![](01-invoice/page-1.png)](01-invoice/output.pdf) | **[01 · Factura](01-invoice)**<br>Una página A4 | Logotipo y datos de las partes en un grid, tira de condiciones, tabla con cabecera, cuerpo y **pie de totales**, pie de página fijo con `{page}` y `{pages}`. Todo el estilo en `Styles`. |
| [![](02-letter/page-1.png)](02-letter/output.pdf) | **[02 · Carta comercial](02-letter)**<br>Una página A4 | Membrete con logotipo, cuerpo en **Markdown** justificado con **Knuth-Plass** (`Text wrap: pretty`), negrita, cursiva y subrayado dentro de la línea, firma escaneada. |
| [![](03-resume/page-1.png)](03-resume/output.pdf) | **[03 · Currículum](03-resume)**<br>Una página A4, sin márgenes | Grid de dos columnas a **página completa**, foto, barras de nivel con anchos en porcentaje, línea de tiempo en un grid y cifras en un `Flex`. |
| [![](04-annual-report/page-1.png)](04-annual-report/output.pdf) | **[04 · Informe anual](04-annual-report)**<br>7 páginas A4 | **Portada a sangre** (un `Absolute` con `Layer` que tapa el margen y el pie), contenido, cifras clave, **gráficos** como imágenes, texto en **dos columnas** con filete, tabla financiera con cabecera y pie, calendario con celdas de varias columnas (`Span`) y secciones que empiezan en página nueva. |
| [![](05-newsletter/page-1.png)](05-newsletter/output.pdf) | **[05 · Boletín](05-newsletter)**<br>2 páginas A4 | Aire de periódico: cabecera de portada, foto recortada con `Fit: cover`, artículos a **tres columnas** con cortes equilibrados, foto al lado del texto, agenda en tabla y anuncios en una rejilla. |
| [![](06-menu/page-1.png)](06-menu/output.pdf) | **[06 · Carta de restaurante](06-menu)**<br>2 páginas, 420 × 700 | **Tamaño de página propio**, fondo y marco doble con `Fixed` de **`Layer` negativa** (detrás del contenido en todas las páginas), platos con precio en un `Flex`, adornos e imagen centrada. |
| [![](07-contract/page-1.png)](07-contract/output.pdf) | **[07 · Contrato](07-contract)**<br>3 páginas A4 | Cabecera y pie fijos con rúbricas, `Reunidos` y `Exponen` con numeración romana, doce **cláusulas con tres niveles de numeración** escritas en Markdown, anexo con tabla de honorarios y **bloque de firmas que no se parte**. |
| [![](08-manual/page-1.png)](08-manual/output.pdf) | **[08 · Manual](08-manual)**<br>4 páginas A4 | Figura con **números superpuestos** (un `Relative` con `Absolute` encima), leyenda al lado, avisos con color, pasos numerados, esquema de conexión, **bloque de terminal** en monoespaciada, tablas de especificaciones y de averías, y marcadores en el PDF. |
| [![](09-catalog/page-1.png)](09-catalog/output.pdf) | **[09 · Catálogo](09-catalog)**<br>4 páginas A4 | Portada a sangre, **fichas de producto** en una rejilla de dos columnas con foto recortada y etiqueta «Novedad» superpuesta, tabla de maderas y página de pedidos. |
| [![](10-certificate/page-1.png)](10-certificate/output.pdf) | **[10 · Certificado](10-certificate)**<br>Una página A4 apaisada | Fondo y **tres marcos concéntricos** con adornos en las esquinas, nombre como protagonista, dos firmas y un sello con cintas, todo centrado. |
| [![](11-timetable/page-1.png)](11-timetable/output.pdf) | **[11 · Horario](11-timetable)**<br>Una página A4 apaisada | Tabla con las celdas separadas por un `Gap`, **colores por clase**, recreo y comedor que ocupan los cinco días con `Span`, leyenda en un `Flex` con salto de línea y notas en Markdown. |

## Lo que se aprende al escribirlos

Qué se resolvió bien con lo que hay y qué se echa en falta (apuntado en [`TODO.md`](../../TODO.md)):

- **Una portada sin cabecera ni pie** se hace con un `Absolute` de `Layer` alta y del tamaño de la
  página, que tapa lo que repiten los `Fixed`. Funciona, pero es un truco.
- **El texto con formato** solo se escribe en `Markdown`; en `Text` y `Paragraph` los asteriscos
  son literales. Los ejemplos mezclan los dos según lo que necesitan.
- **Los espacios se colapsan**: no hay texto preformateado, así que un bloque de terminal con
  columnas alineadas (ejemplo 08) usa un grid.
- **El contenido** (índice) lleva los números de página escritos a mano.
