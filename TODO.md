# TODO

Dos apartados: el primero es del autor del proyecto y solo lo toca él; el segundo es de Claude, que apunta ahí lo que va
saliendo al trabajar y solo toca ese.

## Notas del autor

- Float? Algo más que haga falta?
- Renombre de elementos para que no sea tan "css copy" y más "css inspired". Es como un repensado de css
- Como extender fácil? Valorar alternativas
- Httpservice para probar, que se pueda ejecutar en maven, etc.
- Mirar como api permite a partir de stxt crear otro, y con cualquier página de esto usarlo dircto en pdfbox, para poder
  poner listeners, e integrar en otros lados
- Un docker para esto

## Notas de Claude

### Aspecto de las cajas

- Sombras (descartadas: PDFBox no desenfoca y habría que aproximar con capas), degradados con paradas con posición,
  radiales o repetidos, y dos radios por esquina.

### Imágenes

- El texto de un SVG se dibuja como contornos y no se puede seleccionar: haría falta una salida propia de JSVG que escriba
  el texto con `drawString` en el `PdfBoxGraphics2D`.
- El SVG de depuración incrusta el SVG como `data:`, y un navegador no tiene las fuentes de `fonts/`.
- En el subconjunto de SVG no hay `pattern`, `mask`, `filter`, `marker`, `image` ni hojas de estilo, y un SVG no puede
  enlazar a otro fichero.
