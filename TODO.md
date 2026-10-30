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

### PDF como imagen

- Solo se comprueba que las fuentes de la página estén incrustadas. Las demás reglas de PDF/UA sobre las fuentes (un
  `ToUnicode` en cada una, anchos coherentes…) no se miran: un PDF de otro programa puede dar un documento que veraPDF
  rechace. Habría que decidir si se comprueban al leerlo o se deja a quien convierte.
- `LayerUtility` copia también el contenido opcional (`OCProperties`) del PDF de origen al catálogo; PDF/UA-1 pide un
  `Name` en cada configuración (7.10) y no se comprueba.
- Las marcas de contenido se quitan del contenido de la página y de sus formularios, no de los patrones de mosaico ni de
  los glifos de las fuentes Type 3.
- No hay límite para un PDF (páginas, tamaño, complejidad del contenido): se abre entero con PDFBox. Quizá un
  `maxPdfBytes` en `Limits`.
- `tools/ExamplePdfs.java` no da los mismos bytes en cada ejecución (el identificador y las fechas del PDF): regenerar
  `letterhead.pdf` cambia el binario aunque no cambie nada.
