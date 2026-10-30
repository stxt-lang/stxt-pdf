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

### Accesibilidad

- Verificar los PDF con lectores de pantalla reales (NVDA, JAWS, VoiceOver): hoy los comprueba veraPDF (PDF/UA-1) y un
  lector simulado.
- Idioma por tramo dentro de una línea (una palabra en inglés en un párrafo español): hoy el idioma es de la caja.
- PDF/UA-2 y PDF 2.0: `TOCI` con `Reference` y `Ref`, destinos de estructura, `ListNumbering` para las listas con guion.
- `Alt` de las imágenes: hoy una `Image` sin `Alt` es decorativa sin aviso. Si los olvidos resultan un problema, hacer
  `Alt` obligatorio con `Alt:` vacío para las decorativas.

### Aspecto de las cajas

- Sombras (descartadas: PDFBox no desenfoca y habría que aproximar con capas), degradados con paradas con posición,
  radiales o repetidos, y dos radios por esquina.
- Las esquinas redondeadas no recortan a los hijos que no son una `Image`.

### Imágenes

- El texto de un SVG se dibuja como contornos y no se puede seleccionar: haría falta una salida propia de JSVG que escriba
  el texto con `drawString` en el `PdfBoxGraphics2D`.
- El SVG de depuración incrusta el SVG como `data:`, y un navegador no tiene las fuentes de `fonts/`.
- En el subconjunto de SVG no hay `pattern`, `mask`, `filter`, `marker`, `image` ni hojas de estilo, y un SVG no puede
  enlazar a otro fichero.
