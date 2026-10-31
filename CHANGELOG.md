# Change Log

Los cambios de `dev.stxt:stxt-pdf`, de la versión más nueva a la más vieja.

El formato sigue [Keep a Changelog](http://keepachangelog.com/). Mientras la versión empiece por `0.`, los nombres del
namespace `dev.stxt.pdf` y la API de Java pueden cambiar de una versión menor a otra.

## [0.8.0] - 2026-10-31

Primera versión publicada en Maven Central.

- Lee documentos del namespace `dev.stxt.pdf`, los valida contra su `@stxt.schema` con `stxt-core` 1.0.3, los maqueta en
  páginas de tamaño fijo y escribe un PDF etiquetado (PDF/UA-1, o PDF/UA-2 con `pdfUa(2)`), con las fuentes
  incrustadas y un marcador por encabezado.
- Elementos: `Document`, `Section`, `Block`, `Text`, `Paragraph`, `Heading 1` a `Heading 6`, `List`, `Table`, `Stack`,
  `Grid`, `Columns`, `Frame`, `Overlay`, `Page overlay`, `Image` (PNG, JPEG, SVG y una página de un PDF), `Markdown`
  y `Table of contents`.
- Estilos reutilizables (`Styles`, `For element`, `For style`), en el documento o en un fichero aparte.
- Referencias y enlaces internos, numeración de encabezados y de páginas, giro, esquinas redondeadas y columnas
  equilibradas.
- Errores con código y línea (`InvalidModelException` con su `ErrorCode`), comprobación de contraste WCAG AA
  (`CONTRAST_LOW`) y límites para entradas hostiles (`PAGE_LIMIT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX`,
  `PDF_TOO_LARGE`).
- El jar no incluye fuentes: se leen de la carpeta de recursos (`resources/` del directorio de trabajo, o la que se
  pase con `Converter.builder().resources(...)`).
