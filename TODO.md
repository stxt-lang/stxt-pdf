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
- Revisar que la lectura del pdf se cachea correctamente y no se hace cada vez que se genera un pdf

## Notas de Claude

### Nombres

- Segunda vuelta: los valores que vienen de la maquetación web (`fr`, `minmax`, `min-content`, `max-content`,
  `fit-content`, `min`, `max`, `clamp`, `repeat`).
- Pendiente de decidir si `Paragraph` y `Text` se quedan los dos: hoy solo se diferencian en que `Paragraph` no admite
  líneas en blanco y en que las reglas `For element` los distinguen.

### PDF como imagen

- De las reglas de PDF/UA sobre las fuentes de la página solo se comprueban dos: incrustadas y con `ToUnicode`
  (`Check.PDF_IMAGE_FONTS`). Las demás (anchos coherentes con el programa de la fuente, `CIDToGIDMap`, codificaciones de
  las TrueType simbólicas, `.notdef`…) no: un PDF de otro programa aún puede dar un documento que veraPDF rechace.
- Las capas del PDF de origen reciben un `Name` si no lo tienen, pero una configuración con `AS` (PDF/UA-1, 7.10) se
  copia tal cual: no está comprobado si veraPDF la rechaza.
- Las marcas de contenido se quitan del contenido de la página y de sus formularios, no de los patrones de mosaico ni de
  los glifos de las fuentes Type 3.
- `tools/ExamplePdfs.java` no da los mismos bytes en cada ejecución (el identificador y las fechas del PDF): regenerar
  cambia los binarios aunque no cambie nada, así que solo se suben los ficheros nuevos o los que cambian de verdad.
