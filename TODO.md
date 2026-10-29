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

### Documentación

- **Reducir los detalles técnicos de la referencia**, para que el manual baste para crear documentos y la referencia
  quede para las reglas exactas.
- **Repasar las decisiones de la referencia** (los bloques «Decisión»): están todas aplicadas, pero solo las de los hitos
  1 a 7 se repasaron juntas; las demás se tomaron al implementar cada hito.

### Accesibilidad

- Verificar los PDF con lectores de pantalla reales (NVDA, JAWS, VoiceOver): hoy los comprueba veraPDF (PDF/UA-1) y un
  lector simulado.
- El `Link` suelto de un `Fixed` (un pie con una web) se anuncia al final de la estructura, no en su sitio de lectura.
- Idioma por tramo dentro de una línea (una palabra en inglés en un párrafo español): hoy el idioma es de la caja.
- Contraste de color entre el texto y su fondo: el motor conoce `Color` y `Background`, pero no lo que hay detrás de una
  imagen.
- PDF/UA-2 y PDF 2.0: `TOCI` con `Reference` y `Ref`, destinos de estructura, `ListNumbering` para las listas con guion.
- `Alt` de las imágenes: hoy una `Image` sin `Alt` es decorativa sin aviso. Si los olvidos resultan un problema, hacer
  `Alt` obligatorio con `Alt:` vacío para las decorativas.

### Estilos

- Los errores que se detectan después de construir el árbol (una fuente que no existe, un desbordamiento) señalan la línea
  de una regla del documento de estilos aparte sin decir que es de ese fichero.

### Texto

- Código en línea en un texto con `Inline: markdown` (no hay `Code font` fuera de `Markdown`; podría ser de `Typography`).
- `justify` en `pre-wrap`, y que el código en línea use la altura de línea de su fuente en vez de la de la caja.
- Un texto con `Inline: markdown` no admite listas ni encabezados, y hay que escapar el primer carácter si empieza como una.
- Versalitas con la característica `smcp` de la fuente, en lugar de mayúsculas al 80 %.
- Un texto con interletraje grande puede extraerse del PDF con espacios entre las letras.

### Encabezados y secciones

- Numeración de los encabezados: formatos por nivel (romanos, letras para los anexos), prefijos y sufijos propios
  (`Anexo A.`), número colgado a la izquierda y poder referir y enlazar los encabezados de un `Markdown`.
- Secciones: empezar en página impar con una página en blanco (doble cara), un número inicial (`Start: 5`), otros formatos
  de número (romanos en el prefacio), un `Background` de página y una página apaisada que no sea una sección entera.

### Tablas

- Una fila no tiene `Box` (un borde o un fondo de fila se da a cada celda, y un `Border` se repite entre celdas) ni `Id`
  (no se puede enlazar a una fila ni darle estilo con `For id`).

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

### Giro

- Girar un `Text` o un `Block` directamente, `Rotate origin` con medidas, que una caja girada ocupe en el layout su
  rectángulo envolvente y que un `Relative` girado se pueda partir.
