# Ideas varias

- ~~Índice!!~~ (hito 22)
- Float? Algo más que haga falta?
- Más pdf friendly: Tamaños de páginas estilo A4, Letter, etc. + rotación
- Renombre de elementos para que no sea tan "css copy" y más "css inspired". Es como un repensado de css
- Revisar ThreadSafe
- Revisar rendimiento
- Como extender fácil? Valorar alternativas
- Httpservice para probar, que se pueda ejecutar en maven, etc. 
- Mirar como api permite a partir de stxt crear otro, y con cualquier página de esto usarlo dircto en pdfbox, para poder poner listeners, e integrar en otros lados
- Revisión código desde punto de vista de QA y Arquitectura: SOLID, DRY, KISS
- Un docker para esto

## Huecos del motor detectados al escribir los ejemplos (hito 21)

- **Texto preformateado**: los espacios se colapsan y Markdown no admite código, así que un
  bloque de terminal con columnas alineadas (ejemplo 08) usa un grid. Un `Text` con
  `White space: pre` (o un `Preformatted`) en monoespaciada lo resolvería.
- ~~**Referencias a páginas e índice automático**~~ (hito 22: `{page of: id}`, `{number of: id}`
  y `Contents`).
- **Excluir un encabezado del índice**: un `Contents` lista todos los encabezados de sus niveles,
  así que la portada y el título del propio índice (ejemplos 04 y 09) son un `Text` en vez de un
  `Heading 1`, y pierden su marcador en el PDF. Algo como una propiedad `Contents: false` en el
  encabezado, o una clase de exclusión.
- **Numerar los encabezados** (`1`, `1.1`…): hoy `{number of: id}` solo da el número de un ítem de
  una lista numerada, así que «la sección 4.2» no se puede escribir de un encabezado.
- **Primera página distinta / páginas con nombre**: una portada sin cabecera ni pie se hace con
  un `Absolute` de `Layer` alta que tapa los `Fixed` (ejemplos 04 y 09). Falta una forma de
  decir en qué páginas va un `Fixed`.
- **Enlaces** (a una URL o a un `Id` del documento) y anotaciones del PDF.
- **Formato dentro de la línea fuera de Markdown**: en `Text`, `Paragraph` y `Cell` los
  asteriscos son literales, así que una negrita suelta en un texto corto obliga a usar `Markdown`.
- **Estilo de las tablas**: una fila no tiene `Box`, así que un fondo alternado (cebra) exige una
  clase en cada celda, y la cabecera de una tabla de Markdown no se puede estilar. Algo como
  `Element: Head cell` o un selector por posición en `Styles`.
- **Aspecto de las cajas**: no hay esquinas redondeadas, sombras, degradados, ni un `Border color`
  distinto por lado. Los ejemplos se arreglan con rectángulos, filetes y fondos planos.
- **Texto**: no hay interletraje (`letter-spacing`), versalitas ni `text-transform`; los títulos
  en mayúsculas van escritos en mayúsculas.
- **Imágenes vectoriales (SVG)**: los gráficos de los ejemplos son PNG generados; con SVG
  quedarían nítidos a cualquier tamaño.
- **Rotación**: nada se puede girar, ni un sello ni un texto vertical.
